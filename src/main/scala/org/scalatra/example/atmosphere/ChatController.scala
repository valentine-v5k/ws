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
import scala.concurrent.duration._
import scala.language.postfixOps

class ChatController extends ScalatraServlet with ScalateSupport with JValueResult
with JacksonJsonSupport with SessionSupport with AtmosphereSupport {

  case class Action(author: String, message: String, time: String)

  private final val topic = new Topic
  private final val scheduler = new Scheduler()

  scheduler.start(3 seconds, 5 seconds) {
    topic.alert(("author" -> "System") ~ ("message" -> "x") ~ ("time" -> time))
  }

  implicit protected val jsonFormats: Formats = DefaultFormats

  get("/") {
    contentType = "text/html"
    ssp("/index")
  }

  atmosphere("/the-chat") {
    new AtmosphereClient {
      private final val client = new AtmosphereSubscriber(this)

      def receive: AtmoReceive = {
        case Connected =>
          topic.subscribe(client)
          println("Client %s is connected" format uuid)
          broadcast(write(Action("Someone", "joined the room", time)), Everyone)

        case Disconnected(ClientDisconnected, _) =>
          topic.unsubscribe(client)
          broadcast(write(Action("Someone", "has left the room", time)), Everyone)

        case Disconnected(ServerDisconnected, _) =>
          topic.unsubscribe(client)
          println("Server disconnected the client %s" format uuid)

        case JsonMessage(json) =>
          println("Got message %s from %s".format((json \ "message").extract[String], (json \ "author").extract[String]))
          val msg = json merge ("time" -> time.toString: JValue)
          broadcast(msg, Others)

        case _ => throw new RuntimeException("Unsupported inbound message")
      }
    }
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
