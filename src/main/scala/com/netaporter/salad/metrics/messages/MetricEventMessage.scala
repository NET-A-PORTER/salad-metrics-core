package com.netaporter.salad.metrics.messages

import com.codahale.metrics.Gauge

/**
 * Created by d.tootell@london.net-a-porter.com on 05/02/2014.
 */
trait MetricEventMessage {
  def metricname: String
}

object MetricEventMessage {
  case class IncCounterEvent(metricname: String) extends MetricEventMessage
  case class DecCounterEvent(metricname: String) extends MetricEventMessage
  case class NanoTimeEvent(metricname: String, elapsedNanoTime: Long) extends MetricEventMessage
  case class MeterEvent(metricname: String) extends MetricEventMessage
  case class GaugeEvent[T](metricname: String, takeReading: () => T) extends MetricEventMessage
}
