package com.netaporter.salad.metrics.messages

/**
 * Created by d.tootell@london.net-a-porter.com on 05/02/2014.
 */
object MetricAdminMessage {
  val EMPTY_METRICS_RESPONSE = new MetricsResponse("{}")
  // Ask the actor for metrics
  case object MetricsRequest
  // Response for the metrics request.
  case class MetricsResponse(response: String)

  // Does nothing.
  sealed case class Noop()
}
