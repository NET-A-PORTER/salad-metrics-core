package com.netaporter.salad.metrics.actor.metrics

import _root_.spray.routing.HttpService
import _root_.spray.testkit.ScalatestRouteTest
import akka.actor.ActorRef
import com.netaporter.salad.metrics.util.ActorSys

import org.scalatest.{ ParallelTestExecution, fixture, OptionValues, Matchers }
import com.netaporter.salad.metrics.spray.metrics.MetricsDirectiveFactory
import com.netaporter.salad.metrics.messages.MetricAdminMessage.{ MetricsResponse, MetricsRequest }
import scala.concurrent.duration.Duration
import java.util.concurrent.{ CountDownLatch, TimeUnit }
import com.netaporter.salad.metrics.actor.factory.MetricsActorFactory
import com.netaporter.salad.metrics.messages.MetricEventMessage.NanoTimeEvent
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Await

/**
 * Created by d.tootell@london.net-a-porter.com on 03/02/2014.
 */
class MetricsExampleRoutingSpec extends fixture.WordSpec with Matchers with ScalatestRouteTest with fixture.UnitFixture
    with ParallelTestExecution with HttpService with OptionValues {
  def actorRefFactory = system // connect the DSL to the test ActorSystem
  def makeEventActor(factory: MetricsActorFactory): ActorRef = factory.eventActor()
  def makeAdminActor(factory: MetricsActorFactory): ActorRef = factory.eventTellAdminActor()
  def makeReturningAdminActor(factory: MetricsActorFactory): ActorRef = factory.eventAskAdminActor()

  def smallRoute(factory: MetricsDirectiveFactory) = {

    // Create a timer for timing GET("/bob") requests
    val bobTimings = factory.timer("bobrequests").time

    // Create a counter for counting GET("/bob") requests
    val bobRequestsCounter = factory.counter("bobrequests").all.count

    // Join the metrics together saying, I'm monitoring both the time and num of requests for GET("/bob")
    val bobMetrics = bobTimings & bobRequestsCounter

    path("bob") {
      get {
        bobMetrics {
          complete {
            <h1>Say hello to spray</h1>
          }
        }
      }
    }

  }

  "MetricsDirectiveFactory" should {
    "Record timing and counter events for GET /bob" in new ActorSys {

      // Get the metrics spray routing aware directive factory
      val factory: MetricsDirectiveFactory = MetricsDirectiveFactory()

      val latch: CountDownLatch = new CountDownLatch(1);

      Get("/bob") ~> smallRoute(factory) ~> check {
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

      val getMetricsActor: ActorRef = factory.defaultMetricsActorFactory.eventTellAdminActor()

      implicit val askTimeout = Timeout(2, TimeUnit.SECONDS)

      val metricsStringFuture = getMetricsActor ? MetricsRequest

      val metricsResponse = Await.result(metricsStringFuture, Duration(2, TimeUnit.SECONDS)).asInstanceOf[MetricsResponse]
      val metricResponseString: String = metricsResponse.response

      metricResponseString should include regex "timers\":\\{.*\"bobrequests\":\\{\"count\":(1|2)"
      metricResponseString should include regex "\"bobrequests.successes\":\\{\"count\":(1|2)"
    }
  }

}
