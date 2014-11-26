package com.netaporter.salad.metrics.actor.metrics

import com.netaporter.salad.metrics.messages.MetricEventMessage._
import com.codahale.metrics.{ Gauge, Timer, MetricRegistry }
import java.util.concurrent.TimeUnit
import com.netaporter.salad.metrics.messages.MetricEventMessage.DecCounterEvent
import com.netaporter.salad.metrics.messages.MetricEventMessage.NanoTimeEvent
import com.netaporter.salad.metrics.messages.MetricEventMessage.MeterEvent
import com.netaporter.salad.metrics.messages.MetricEventMessage.IncCounterEvent

/**
 * Created by d.tootell@london.net-a-porter.com on 05/02/2014.
 */
abstract class MetricsEventActor extends AbstractMetricsEventActor with MetricsRegistry[MetricRegistry] {

  override def handleDecCounter(message: DecCounterEvent): Unit = {
    metricsRegistry.counter(message.metricname).dec()
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

  override def handleGaugeEvent[T](message: GaugeEvent[T]): Unit = {
    val gauge = new Gauge[T] { def getValue = message.takeReading() }
    metricsRegistry.register(message.metricname, gauge)
  }
}
