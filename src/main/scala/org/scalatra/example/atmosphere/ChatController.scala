package org.scalatra.example.atmosphere

import java.util.Date

import org.json4s.JsonDSL._
import org.json4s._
import org.json4s.jackson.Serialization.write
import org.scalatra._
import org.scalatra.atmosphere._
import org.scalatra.json.{JValueResult, JacksonJsonSupport}
import org.scalatra.scalate.ScalateSupport

import scala.concurrent.ExecutionContext.Implicits.global

class ChatController extends ScalatraServlet with ScalateSupport with JValueResult
with JacksonJsonSupport with SessionSupport with AtmosphereSupport {

  private final val topic = new Topic

  implicit protected val jsonFormats: Formats = DefaultFormats

  get("/") {
    contentType = "text/html"
    ssp("/index")
  }

  get("/x/:msg") {
    val json: JValue = ("author" -> "system") ~ ("message" -> params("msg"))
    topic.alert(json)
  }

  case class Action(author: String, message: String, time: String)

  atmosphere("/the-chat") {
    val client = new AtmosphereClient {
      def receive: AtmoReceive = {
        case Connected =>
          println("Client %s is connected" format uuid)
          broadcast(write(Action("Someone", "joined the room", time)), Everyone)

        case Disconnected(ClientDisconnected, _) =>
          broadcast(write(Action("Someone", "has left the room", time)), Everyone)

        case Disconnected(ServerDisconnected, _) =>
          println("Server disconnected the client %s" format uuid)

        case _: TextMessage =>
          send(write(Action("system", "Only json is allowed", time)))

        case JsonMessage(json) =>
          println("Got message %s from %s".format((json \ "message").extract[String], (json \ "author").extract[String]))
          val msg = json merge ("time" -> time.toString: JValue)
          broadcast(msg, SkipSelf) // by default a broadcast is to everyone but self
        //  send(msg) // also send to the sender
      }
    }
    topic.subscribe(new AtmosphereSubscriber(client))
    client
  }

  private def time: String = new Date().getTime.toString


  error {
    case t: Throwable => t.printStackTrace()
  }

  notFound {
    // remove content type in case it was set through an action
    contentType = null
    // Try to render a ScalateTemplate if no route matched
    findTemplate(requestPath) map { path =>
      contentType = "text/html"
      layoutTemplate(path)
    } orElse serveStaticResource() getOrElse resourceNotFound()
  }
}
