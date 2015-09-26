package org.scalatra.example.atmosphere

import org.json4s.JValue

trait Subscriber {

  def broadcast(jsonValue: JValue)

}
