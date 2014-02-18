package com.netaporter.salad.metrics.actor.metrics

import _root_.spray.routing.HttpService
import _root_.spray.testkit.ScalatestRouteTest
import akka.actor.ActorRef
import com.netaporter.salad.metrics.util.ActorSys

import org.scalatest.{ ParallelTestExecution, fixture, OptionValues, Matchers }
import com.netaporter.salad.metrics.spray.metrics.MetricsDirectiveFactory
import com.netaporter.salad.metrics.messages.MetricAdminMessage.{ MetricsRequest, MetricsResponse }
import scala.concurrent.duration.Duration
import java.util.concurrent.{ CountDownLatch, TimeUnit }
import com.netaporter.salad.metrics.actor.factory.MetricsActorFactory
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Await
import com.netaporter.salad.metrics.actor.admin.spray.OutputMetricsMessages.OutputMetrics
import spray.http.StatusCodes.{ OK, NotFound }

/**
 * Created by d.tootell@london.net-a-porter.com on 03/02/2014.
 */
class MetricsExampleAdminEndpointSpec extends fixture.WordSpec with Matchers with ScalatestRouteTest with fixture.UnitFixture
    with ParallelTestExecution with HttpService with OptionValues {
  def actorRefFactory = system // connect the DSL to the test ActorSystem
  def makeEventActor(factory: MetricsActorFactory): ActorRef = factory.eventActor()
  def makeAdminActor(factory: MetricsActorFactory): ActorRef = factory.eventTellAdminActor()
  def makeReturningAdminActor(factory: MetricsActorFactory): ActorRef = factory.eventAskAdminActor()

  def smallRoute(factory: MetricsDirectiveFactory) = {

    // get the admin actor that outputs the json
    val metricsOutputActor = factory.defaultMetricsActorFactory.eventTellAdminActor()

    // get the admin actor that outputs the json
    val metricsAskActor = factory.defaultMetricsActorFactory.eventAskAdminActor()

    // Create a timer for timing GET("/bob") requests
    val bobTimings = factory.timer("bobrequests").time

    // Create a counter for counting GET("/bob") requests
    val bobRequestsCounter = factory.counter("bobrequests").all.count

    // Join the metrics together saying, I'm monitoring both the time and num of requests for GET("/bob")
    val bobMetrics = bobTimings & bobRequestsCounter

    implicit val askTimeout = Timeout(2000, TimeUnit.MILLISECONDS)

    path("admin" / "metrics" / "ask") {
      get {
        onSuccess(metricsAskActor ? MetricsRequest) {
          case MetricsResponse(json: String) =>
            complete(OK, json)
        }
      }
    } ~
      path("admin" / "metrics") {
        get {
          ctx => metricsOutputActor ! OutputMetrics(ctx)
        }
      } ~
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
    "Record timing and counter events for GET /bob, and output admin metrics" in new ActorSys {

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

      Get("/admin/metrics") ~> smallRoute(factory) ~> check {
        val responseBody = responseAs[String]
        responseBody should include regex "timers\":\\{.*\"bobrequests\":\\{\"count\":(1|2)"
        responseBody should include regex "\"bobrequests.successes\":\\{\"count\":(1|2)"
      }

      Get("/admin/metrics/ask") ~> smallRoute(factory) ~> check {
        val responseBody = responseAs[String]
        responseBody should include regex "timers\":\\{.*\"bobrequests\":\\{\"count\":(1|2)"
        responseBody should include regex "\"bobrequests.successes\":\\{\"count\":(1|2)"
      }
    }
  }

}
