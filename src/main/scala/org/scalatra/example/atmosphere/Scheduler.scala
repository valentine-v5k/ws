package org.scalatra.example.atmosphere

import akka.actor.Cancellable

import scala.concurrent.duration._
import scala.language.postfixOps


class Scheduler(action: => Unit) {

  import system.dispatcher

  private lazy val system = akka.actor.ActorSystem()

  def start(delay: FiniteDuration, interval: FiniteDuration)(action: => Unit): Cancellable = {
    system.scheduler.schedule(delay, interval)(action)
  }

}
