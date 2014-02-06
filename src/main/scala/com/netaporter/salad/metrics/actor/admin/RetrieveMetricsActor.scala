package com.netaporter.salad.metrics.actor.admin

import com.codahale.metrics.MetricRegistry
import akka.actor.{ ActorLogging, Actor }

import com.netaporter.salad.metrics.json.{ JacksonMetricsToJsonConverter, MetricsToJsonConverter }
import akka.util.Timeout
import java.util.concurrent.TimeUnit
import com.netaporter.salad.metrics.messages.MetricAdminMessage.{ MetricsResponse, MetricsRequest, Noop, EMPTY_METRICS_RESPONSE }
import com.netaporter.salad.metrics.actor.metrics.MetricsRegistry

abstract class RetrieveMetricsActor(val converter: MetricsToJsonConverter)
    extends AbstractAdminMetricsActor
    with Actor
    with ActorLogging
    with MetricsRegistry[MetricRegistry] {

  def this() = this(new JacksonMetricsToJsonConverter())

  import context._
  def configuration = system.settings.config

  // This timeout is just for resource cleanup.
  // Make sure it is 10% longer than spray can's request timeout.
  implicit val askTimeout = Timeout(
    (configuration.getMilliseconds("spray.can.client.request-timeout") * 11) / 10, TimeUnit.MILLISECONDS)

  // to be defined in subclassing actor
  def otherMetricsMessageHandler: Receive = {
    case Noop => sender ! EMPTY_METRICS_RESPONSE
  }

  // generic message handler
  def askForMetricsMessageHandler: Receive = {
    case MetricsRequest => {
      if (metricsRegistry == null) sender ! EMPTY_METRICS_RESPONSE
      else sender ! MetricsResponse(converter.toJsonString(metricsRegistry))
    }
    case _ => {
      sender ! EMPTY_METRICS_RESPONSE
    }

  }

}