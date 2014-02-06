package com.netaporter.salad.metrics.util

import java.util.concurrent.CountDownLatch
import com.netaporter.salad.metrics.actor.factory.MetricsActorFactory
import akka.actor.{ Props, ActorRef, ActorSystem }
import com.netaporter.salad.metrics.actor.metrics.{ MetricsEventActor, YammerMetricsRegistry }
import com.netaporter.salad.metrics.messages.MetricEventMessage.{ MeterEvent, NanoTimeEvent, IncCounterEvent, DecCounterEvent }

/**
 * Created by d.tootell@london.net-a-porter.com on 06/02/2014.
 */
class AtomicCounterMetricsActoryFactory(latch: CountDownLatch) extends MetricsActorFactory {
  override def eventActor(system: ActorSystem): ActorRef = {
    system.actorOf(Props(new CounterMetricEventActor(latch) with YammerMetricsRegistry))
  }

  override def eventTellAdminActor(system: ActorSystem): ActorRef = {
    MetricsActorFactory.eventTellAdminActor(system)
  }
  override def eventAskAdminActor(system: ActorSystem): ActorRef = {
    MetricsActorFactory.eventAskAdminActor(system)
  }

  abstract class CounterMetricEventActor(val latch: CountDownLatch) extends MetricsEventActor {
    override def handleDecCounter(message: DecCounterEvent): Unit = {
      latch.countDown()
      super.handleDecCounter(message);
    }

    override def handleIncCounter(message: IncCounterEvent): Unit = {
      latch.countDown()
      super.handleIncCounter(message)
    }

    override def handleTimerEvent(message: NanoTimeEvent): Unit = {
      latch.countDown()
      super.handleTimerEvent(message)
    }

    override def handleMeterEvent(message: MeterEvent): Unit = {
      latch.countDown()
      super.handleMeterEvent(message)
    }
  }

}

