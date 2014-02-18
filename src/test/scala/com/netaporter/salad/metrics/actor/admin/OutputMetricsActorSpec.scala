package com.netaporter.salad.metrics.actor.admin

import _root_.spray.routing.HttpService
import _root_.spray.testkit.ScalatestRouteTest
import akka.actor.{ ActorSystem, ActorRef }
import com.netaporter.salad.metrics.util.ActorSys

import com.netaporter.salad.metrics.actor.admin.spray.OutputMetricsMessages.OutputMetrics
import org.scalatest.{ OptionValues, Matchers }
import com.netaporter.salad.metrics.actor.factory.MetricsActorFactory

/**
 * Created by d.tootell@london.net-a-porter.com on 03/02/2014.
 */
class OutputMetricsActorSpec extends RetrieveMetricsActorSpec with ScalatestRouteTest with HttpService with Matchers with OptionValues {
  def actorRefFactory = system // connect the DSL to the test ActorSystem
  override def makeActor()(implicit system: ActorSystem): ActorRef = MetricsActorFactory.eventTellAdminActor()(system)

  def smallRoute(ref: ActorRef) = {
    get {
      pathSingleSlash { ctx =>
        {
          ref ! OutputMetrics(ctx)
        }
      }
    }
  }

  "Output Metrics Actor" should {
    "be able to send json results to RequestContext" in new ActorSys {
      val actor = makeActor()
      Get() ~> smallRoute(actor) ~> check {
        assert(responseAs[String].contains("counters"))
      }
    }
  }

  "Output Metrics Actor" should {
    "be setting the response header to json" in new ActorSys {
      val actor = makeActor()
      Get() ~> smallRoute(actor) ~> check {
        println(header("Content-Type"))
        //        assert((header("Content-Type")).isDefined)
        header("Content-Type").value.value should include("application/json")
        headers.count(_.name == "Content-Type") should equal(1)

      }
    }
  }

}
