package org.scalatra.example.atmosphere

import org.json4s.JValue
import org.scalatra.atmosphere.{AtmosphereClient, JsonMessage}

import scala.concurrent.ExecutionContext

class AtmosphereSubscriber(client: AtmosphereClient)(implicit executionContext: ExecutionContext) extends Subscriber {

  override def broadcast(jsonValue: JValue): Unit = client ! new JsonMessage(jsonValue)

  override def toString: String = client.uuid
}
