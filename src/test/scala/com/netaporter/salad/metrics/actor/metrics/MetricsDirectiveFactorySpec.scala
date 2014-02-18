package com.netaporter.salad.metrics.actor.metrics

import _root_.spray.routing.HttpService
import _root_.spray.testkit.ScalatestRouteTest
import akka.actor.{ ActorSystem, ActorRef }
import com.netaporter.salad.metrics.util.{ AtomicCounterMetricsActoryFactory, ActorSys }

import com.netaporter.salad.metrics.actor.admin.spray.OutputMetricsMessages.OutputMetrics
import org.scalatest.{ ParallelTestExecution, fixture, OptionValues, Matchers }
import com.netaporter.salad.metrics.spray.metrics.MetricsDirectiveFactory
import com.netaporter.salad.metrics.messages.MetricAdminMessage.{ MetricsResponse, MetricsRequest }
import scala.concurrent.duration.Duration
import java.util.concurrent.{ CountDownLatch, TimeUnit }
import com.netaporter.salad.metrics.actor.factory.MetricsActorFactory
import com.netaporter.salad.metrics.messages.MetricEventMessage.{ NanoTimeEvent, MeterEvent }
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Await

/**
 * Created by d.tootell@london.net-a-porter.com on 03/02/2014.
 */
class MetricsDirectiveFactorySpec extends fixture.WordSpec with Matchers with ScalatestRouteTest with fixture.UnitFixture
    with ParallelTestExecution with HttpService with OptionValues {
  def actorRefFactory = system // connect the DSL to the test ActorSystem
  def makeEventActor(factory: MetricsActorFactory): ActorRef = factory.eventActor()
  def makeAdminActor(factory: MetricsActorFactory): ActorRef = factory.eventTellAdminActor()
  def makeReturningAdminActor(factory: MetricsActorFactory): ActorRef = factory.eventAskAdminActor()

  def smallRoute(factory: MetricsDirectiveFactory) = {

    val time = factory.timer.time

    val metrics = time

    metrics {
      get {
        complete {
          <h1>Say hello to spray</h1>
        }
      }
    }

  }

  "MetricsDirectiveFactory" should {
    "Send Metrics Events to a given actor" in new ActorSys {

      val metricsFactory: MetricsDirectiveFactory = MetricsDirectiveFactory(testActor)

      Get() ~> smallRoute(metricsFactory) ~> check {
        assert(responseAs[String].contains("<h1>Say hello to spray</h1>"))
      }

      val msg: NanoTimeEvent = expectMsgAnyClassOf(Duration(1, TimeUnit.SECONDS), classOf[NanoTimeEvent])

      assert((msg.elapsedNanoTime / 1000000) < 2000)

    }
  }

  "MetricsDirectiveFactory" should {
    "Send Metrics Events to a default actor" in new ActorSys {

      val metricsFactory: MetricsDirectiveFactory = MetricsDirectiveFactory()

      val latch: CountDownLatch = new CountDownLatch(1);

      Get() ~> smallRoute(metricsFactory) ~> check {
        assert(responseAs[String].contains("<h1>Say hello to spray</h1>"))
        latch.countDown();
      }

      try {
        val ok = latch.await(2000, TimeUnit.MILLISECONDS)
        assert(ok, "Spray has not returned response.  Therefore cannot check if timer metric has recorded")
      } catch {
        case e: Exception => {
          fail("Should have obtained response from spray before checking if timer metric has recorded")
        }
      }

      val getMetricsActor: ActorRef = metricsFactory.defaultMetricsActorFactory.eventTellAdminActor()

      implicit val askTimeout = Timeout(2, TimeUnit.SECONDS)

      val metricsStringFuture = getMetricsActor ? MetricsRequest

      val metricsResponse = Await.result(metricsStringFuture, Duration(2, TimeUnit.SECONDS)).asInstanceOf[MetricsResponse]
      val metricResponseString: String = metricsResponse.response

      metricResponseString should include("timers\":{\"/.GET\":{\"count\":1")
    }
  }

}
