package com.netaporter.salad.metrics.actor.metrics

import akka.actor.{ ActorLogging, Actor }
import com.netaporter.salad.metrics.messages.MetricEventMessage._
import com.netaporter.salad.metrics.messages.MetricEventMessage.DecCounterEvent
import com.netaporter.salad.metrics.messages.MetricEventMessage.NanoTimeEvent
import com.netaporter.salad.metrics.messages.MetricEventMessage.MeterEvent
import com.netaporter.salad.metrics.messages.MetricEventMessage.IncCounterEvent

/**
 * Created by d.tootell@london.net-a-porter.com on 05/02/2014.
 */
abstract trait AbstractMetricsEventActor extends Actor with ActorLogging {
  MetricsRegistry =>

  def handleIncCounter(message: IncCounterEvent)
  def handleDecCounter(message: DecCounterEvent)
  def handleTimerEvent(message: NanoTimeEvent)
  def handleMeterEvent(message: MeterEvent)
  def handleGaugeEvent[T](message: GaugeEvent[T])

  def receive = {
    case m: IncCounterEvent => handleIncCounter(m)
    case m: DecCounterEvent => handleDecCounter(m)
    case m: NanoTimeEvent => handleTimerEvent(m)
    case m: MeterEvent => handleMeterEvent(m)
    case m: GaugeEvent[_] => handleGaugeEvent(m)
  }
}
