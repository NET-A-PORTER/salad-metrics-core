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
    val cache = LruCacheWithMetrics[Int](metricsName = "test-cache", maxCapacity = 5, initialCapacity = 5)
    def receive = {
      case i: Int => cache.apply(i, () => Future.successful(i))
    }
  })

  val hitRatioGauge = eventProbe.expectMsgType[GaugeEvent[Double]]
  val fillRatioGauge = eventProbe.expectMsgType[GaugeEvent[Double]]

  "CacheMetrics" should "name the caches correctly" in {
    hitRatioGauge.metricname should equal("test-cache.hit-ratio")
    fillRatioGauge.metricname should equal("test-cache.fill-ratio")
  }

  it should "start with hit ratio 0.0" in {
    expectHitRatio(0.0d)
  }

  it should "have the correct hit ratio after a few requests" in {
    // Miss
    cacheActor ! 1
    expectHitRatio(0.0d)

    // Hit
    cacheActor ! 1
    expectHitRatio(0.5d)

    // Hit
    cacheActor ! 1
    expectHitRatio(0.66d)

    // Hit
    cacheActor ! 1
    expectHitRatio(0.75d)

    // Miss
    cacheActor ! 2
    expectHitRatio(3d / 5d)
  }

  it should "start with usage percent of 0.0" in {
    expectFillRatio(0.0d)
  }

  it should "have the correct usage percent after a few requests" in {
    //Max capacity is 5

    // Miss
    cacheActor ! 1
    expectFillRatio(0.2d)

    // Hit
    cacheActor ! 1
    expectFillRatio(0.2d)

    // Hit
    cacheActor ! 1
    expectFillRatio(0.2d)

    // Miss
    cacheActor ! 2
    expectFillRatio(0.4d)

    // Miss
    cacheActor ! 3
    expectFillRatio(0.6d)
  }

  def expectHitRatio(expected: Double) {
    val actual = hitRatioGauge.takeReading()
    actual should equal(expected +- 0.01)
  }

  def expectFillRatio(expected: Double) {
    val actual = fillRatioGauge.takeReading()
    actual should equal(expected +- 0.01)
  }
}
