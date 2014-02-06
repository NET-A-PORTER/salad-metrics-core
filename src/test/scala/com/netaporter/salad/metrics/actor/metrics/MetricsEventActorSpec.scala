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
import com.netaporter.salad.metrics.messages.MetricEventMessage.{ MeterEvent, NanoTimeEvent, IncCounterEvent, DecCounterEvent }

/**
 * Created by d.tootell@london.net-a-porter.com on 03/02/2014.
 */
class MetricsEventActorSpec extends fixture.WordSpec with Matchers with ScalatestRouteTest with fixture.UnitFixture
    with ParallelTestExecution with HttpService with OptionValues {
  def actorRefFactory = system // connect the DSL to the test ActorSystem
  def makeEventActor(factory: MetricsActorFactory, system: ActorSystem): ActorRef = factory.eventActor(system)
  def makeAdminActor(factory: MetricsActorFactory, system: ActorSystem): ActorRef = factory.eventTellAdminActor(system)
  def makeReturningAdminActor(factory: MetricsActorFactory, system: ActorSystem): ActorRef = factory.eventAskAdminActor(system)

  def smallRoute(metric: ActorRef, adminActor: ActorRef,
    timerName: String = "allrequests") = {
    val factory = MetricsDirectiveFactory(metric)
    val time = factory.timer(timerName).time
    val requestCounter = factory.counterWithMethod("all").count
    val pathCounter = factory.counter.all.count
    val rootCounter = factory.counter.all.count

    val metrics = time & requestCounter

    metrics {
      get {
        rootCounter {
          pathSingleSlash { ctx =>
            {
              adminActor ! OutputMetrics(ctx)
            }
          }
        }
      } ~
        pathCounter {
          get {
            path("bob") { ctx =>
              adminActor ! OutputMetrics(ctx)
            }
          } ~
            post { ctx =>
              adminActor ! OutputMetrics(ctx)
            }
        }
    }
  }

  "Output Metrics Actor" should {
    "record timer events and requests counts for /" in new ActorSys {
      val latch = new CountDownLatch(4);
      val factory = new AtomicCounterMetricsActoryFactory(latch)
      val metricActor = makeEventActor(factory, system)
      val adminActor = makeAdminActor(factory, system)
      val returningStringActor = makeReturningAdminActor(factory, system)

      Get() ~> smallRoute(metricActor, adminActor, "mytimer1") ~> check {
        assert(responseAs[String].contains("counters"))
      }

      Get() ~> smallRoute(metricActor, adminActor, "mytimer2") ~> check {
        assert(responseAs[String].contains("counters"))
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
            System.out.println("STRING:::" + s)

            assert(s.contains("mytimer2"))
            assert(s.contains("mytimer1"))
            assert(s.contains("\"/.GET.successes\":{\"count\":2"))

          }
        }
      }

    }
  }

  "Output Metrics Actor" should {
    "record timer events and GET and POST requests counts for /bob" in new ActorSys {
      val latch = new CountDownLatch(4);
      val factory = new AtomicCounterMetricsActoryFactory(latch)
      val metricActor = makeEventActor(factory, system)
      val adminActor = makeAdminActor(factory, system)
      val returningStringActor = makeReturningAdminActor(factory, system)

      Get("/bob") ~> smallRoute(metricActor, adminActor) ~> check {
        assert(responseAs[String].contains("counters"))
      }

      Post("/bob") ~> smallRoute(metricActor, adminActor) ~> check {
        assert(responseAs[String].contains("counters"))
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
            System.out.println("STRING:::" + s)

            assert(s.contains("\"/bob.GET.successes\":{\"count\":1"))
            assert(s.contains("\"/bob.POST.successes\":{\"count\":1"))

          }
        }
      }

    }
  }
}
