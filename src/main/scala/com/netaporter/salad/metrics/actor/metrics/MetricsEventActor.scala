package com.netaporter.salad.metrics.actor.metrics

import com.netaporter.salad.metrics.messages.MetricEventMessage.{ MeterEvent, NanoTimeEvent, IncCounterEvent, DecCounterEvent }
import com.codahale.metrics.{ Timer, MetricRegistry }
import java.util.concurrent.TimeUnit

/**
 * Created by d.tootell@london.net-a-porter.com on 05/02/2014.
 */
abstract class MetricsEventActor extends AbstractMetricsEventActor with MetricsRegistry[MetricRegistry] {

  override def handleDecCounter(message: DecCounterEvent): Unit = {
    metricsRegistry.counter(message.metricname).inc()
  }

  override def handleIncCounter(message: IncCounterEvent): Unit = {
    metricsRegistry.counter(message.metricname).inc()
  }

  override def handleTimerEvent(message: NanoTimeEvent): Unit = {
    val timer: Timer = metricsRegistry.timer(message.metricname)
    timer.update(message.elapsedNanoTime, TimeUnit.NANOSECONDS)
  }

  override def handleMeterEvent(message: MeterEvent): Unit = {
    metricsRegistry.meter(message.metricname).mark()
  }
}
