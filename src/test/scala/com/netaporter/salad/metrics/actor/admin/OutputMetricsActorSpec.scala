package com.netaporter.salad.metrics.actor.admin

import _root_.spray.http.ContentTypes
import _root_.spray.http.HttpHeaders.`Cache-Control`
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
        entity.toOption.value.contentType should equal(ContentTypes.`application/json`)

        withClue("Cache-Control header should be present.") {
          header("Cache-Control") should be('defined)
        }

        header("Cache-Control").value.value should include("must-revalidate")
        header("Cache-Control").value.value should include("no-cache")
        header("Cache-Control").value.value should include("no-store")

        header("Cache-Control").value.value should not include ("no-storedasfls")

      }
    }
  }

}
