package com.netaporter.salad.metrics.messages

/**
 * Created by d.tootell@london.net-a-porter.com on 05/02/2014.
 */
trait MetricEventMessage {
  def metricname: String
}

object MetricEventMessage {
  case class IncCounterEvent(val metricname: String) extends MetricEventMessage
  case class DecCounterEvent(val metricname: String) extends MetricEventMessage
  case class NanoTimeEvent(val metricname: String, elapsedNanoTime: Long) extends MetricEventMessage
  case class MeterEvent(val metricname: String) extends MetricEventMessage
}
