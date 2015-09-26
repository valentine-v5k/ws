package org.scalatra.example.atmosphere

import org.json4s.JValue

import scala.collection.mutable.ListBuffer

class Topic {

  private val clients = ListBuffer[Subscriber]()

  def subscribe(client: Subscriber): Unit = {
    clients.+=:(client)
  }

  def alert(jValue: JValue): Unit = {
    clients.foreach { client =>
      client.broadcast(jValue)
    }
  }
}
