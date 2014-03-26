package com.netaporter.salad.metrics.actor.cache

import akka.testkit.{ TestProbe, ImplicitSender, TestActorRef, TestKit }
import akka.actor.{ Actor, ActorSystem }
import com.netaporter.salad.metrics.cache.CacheMetrics
import spray.caching.LruCache
import org.scalatest.{ OneInstancePerTest, Matchers, FlatSpecLike }
import com.netaporter.salad.metrics.messages.MetricEventMessage.GaugeEvent
import scala.concurrent.Future

class CacheMetricsSpec
    extends TestKit(ActorSystem())
    with FlatSpecLike
    with Matchers
    with OneInstancePerTest
    with ImplicitSender {

  import system.dispatcher

  val eventProbe = TestProbe()

  val cacheActor = TestActorRef(new Actor with CacheMetrics {
    override val eventActor = eventProbe.ref
    val cache = cacheMetrics(LruCache[Int](maxCapacity = 5, initialCapacity = 5), "test-cache")
    def receive = {
      case i: Int => cache.apply(i, () => Future.successful(i))
    }
  })

  val gauge = eventProbe.expectMsgType[GaugeEvent[Double]]

  "CacheMetrics" should "start with hit ratio 0.0" in {
    cacheActor ! 'hitRatio
    expectHitRatio(0.0d)
  }

  it should "have the correct hit ratio after a few requests" in {
    // Miss
    cacheActor ! 1
    cacheActor ! 'hitRatio
    expectHitRatio(0.0d)

    // Hit
    cacheActor ! 1
    cacheActor ! 'hitRatio
    expectHitRatio(0.5d)

    // Hit
    cacheActor ! 1
    cacheActor ! 'hitRatio
    expectHitRatio(0.66d)

    // Hit
    cacheActor ! 1
    cacheActor ! 'hitRatio
    expectHitRatio(0.75d)

    // Miss
    cacheActor ! 2
    cacheActor ! 'hitRatio
    expectHitRatio(3d / 5d)
  }

  def expectHitRatio(expected: Double) {
    val actual = gauge.takeReading()
    actual should equal(expected +- 0.01)
  }
}
