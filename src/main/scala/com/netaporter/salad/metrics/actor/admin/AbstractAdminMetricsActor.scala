package com.netaporter.salad.metrics.actor.admin

import akka.actor.Actor.Receive

/**
 * Created by d.tootell@london.net-a-porter.com on 06/02/2014.
 */
abstract trait AbstractAdminMetricsActor {
  MetricsRegistry =>

  def askForMetricsMessageHandler: Receive
  def otherMetricsMessageHandler: Receive

  def receive: Receive = otherMetricsMessageHandler orElse askForMetricsMessageHandler
}
