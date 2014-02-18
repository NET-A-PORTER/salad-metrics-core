package com.netaporter.salad.metrics.actor.metrics

import spray.routing.{ ValidationRejection, HttpService }
import _root_.spray.testkit.ScalatestRouteTest
import akka.actor.{ Props, ActorSystem, ActorRef }
import com.netaporter.salad.metrics.util.{ AtomicCounterMetricsActoryFactory, ActorSys }

import org.scalatest.{ ParallelTestExecution, fixture, OptionValues, Matchers }
import com.netaporter.salad.metrics.spray.metrics.MetricsDirectiveFactory
import com.netaporter.salad.metrics.messages.MetricAdminMessage.{ MetricsResponse, MetricsRequest }
import scala.concurrent.duration.Duration
import java.util.concurrent.{ CountDownLatch, TimeUnit }
import com.netaporter.salad.metrics.actor.factory.MetricsActorFactory

/**
 * Created by d.tootell@london.net-a-porter.com on 03/02/2014.
 */
class FailureMetricsEventActorSpec extends fixture.WordSpec with Matchers with ScalatestRouteTest with fixture.UnitFixture
    with ParallelTestExecution with HttpService with OptionValues {
  def actorRefFactory = system // connect the DSL to the test ActorSystem
  def makeEventActor(factory: MetricsActorFactory)(implicit system: ActorSystem): ActorRef = factory.eventActor()
  def makeAdminActor(factory: MetricsActorFactory)(implicit system: ActorSystem): ActorRef = factory.eventTellAdminActor()
  def makeReturningAdminActor(factory: MetricsActorFactory)(implicit system: ActorSystem): ActorRef = factory.eventAskAdminActor()

  def rejectionRoute(metric: ActorRef, adminActor: ActorRef,
    timerName: String) = {
    val factory = MetricsDirectiveFactory(metric)
    val time = factory.timer(timerName).time
    val requestCounter = factory.counterWithMethod("all").all.count

    val metrics = time & requestCounter

    metrics {
      get {
        pathSingleSlash { ctx =>
          {
            ctx.reject(ValidationRejection("Restricted!"))
          }
        }
      }
    }
  }

  def failureRoute(metric: ActorRef, adminActor: ActorRef,
    timerName: String) = {
    val factory = MetricsDirectiveFactory(metric)
    val time = factory.timer(timerName).time
    val requestCounter = factory.counterWithMethod("all").failures.count

    val metrics = time & requestCounter

    metrics {
      get {
        pathSingleSlash { ctx =>
          {
            ctx.failWith(new NullPointerException("lkdjlj"))
          }

        }
      }
    }
  }

  "Metrics Event Actor" should {
    "should capture rejections" in new ActorSys {
      val latch = new CountDownLatch(2);
      val factory = new AtomicCounterMetricsActoryFactory(latch)
      val metricActor = makeEventActor(factory)
      val adminActor = makeAdminActor(factory)
      val returningStringActor = makeReturningAdminActor(factory)

      Get() ~> rejectionRoute(metricActor, adminActor, "mytimer1") ~> check {
        assert(rejection.getClass == classOf[ValidationRejection])
      }

      Get() ~> rejectionRoute(metricActor, adminActor, "mytimer2") ~> check {
        assert(rejection.getClass == classOf[ValidationRejection])
      }

      try {
        val ok = latch.await(1, TimeUnit.SECONDS)
        assert(ok)
      } catch {
        case e: Exception => {
          fail("Exception waiting for metrics messages to be processed")
        }

      }
      returningStringActor ! MetricsRequest

      expectMsgAllClassOf(Duration(1, TimeUnit.SECONDS), classOf[MetricsResponse]) foreach { msg =>
        msg match {
          case MetricsResponse(s: String) => {

            assert(s.contains("mytimer2"))
            assert(s.contains("mytimer1"))
            assert(s.contains("\"all.GET.rejections\":{\"count\":2"))

          }
        }
      }

    }
  }

}
