package com.netaporter.salad.metrics.util

import java.util.concurrent.atomic.AtomicInteger
import akka.actor.ActorSystem
import akka.testkit.{ TestKit, ImplicitSender }

/**
 * Created by d.tootell@london.net-a-porter.com on 03/02/2014.
 */
object ActorSys {
  val uniqueId = new AtomicInteger(0)
}

class ActorSys(name: String) extends TestKit(ActorSystem(name)) with ImplicitSender with DelayedInit with (() => Unit) {
  def this() = this("TestSystem%05d".format(ActorSys.uniqueId.getAndIncrement))

  def shutdown(): Unit = system.shutdown()

  override def apply() {
    try theBody()
    finally shutdown()
  }

  private var theBody: () => Unit = _
  final def delayedInit(body: => Unit) {
    theBody = (() => body)
  }
}
