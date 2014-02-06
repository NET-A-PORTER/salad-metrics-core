package com.netaporter.salad.metrics.actor.admin

import org.scalatest._
import akka.actor.{ ActorSystem, ActorRef }
import com.netaporter.salad.metrics.util.ActorSys
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import com.fasterxml.jackson.databind.{ JsonNode, ObjectMapper }
import com.fasterxml.jackson.core.{ JsonProcessingException }
import java.io.IOException
import com.netaporter.salad.metrics.messages.MetricAdminMessage.{ MetricsResponse, MetricsRequest }
import com.netaporter.salad.metrics.actor.factory.MetricsActorFactory

/**
 * Created by d.tootell@london.net-a-porter.com on 03/02/2014.
 */
class RetrieveMetricsActorSpec extends fixture.WordSpec with Matchers with fixture.UnitFixture with ParallelTestExecution {

  def makeActor(system: ActorSystem): ActorRef = MetricsActorFactory.eventAskAdminActor(system)

  val mapper = new ObjectMapper();

  def parseJson(json: String): Option[JsonNode] = {
    try {
      Some(mapper.readTree(json))
    } catch {
      case e: JsonProcessingException => {
        None
      }
      case e: IOException => {
        None
      }
    }
  }

  "Retrieve Metrics Actor" should {
    "be able to handle MetricsRequest message" in new ActorSys {
      val actor = makeActor(system)

      actor ! MetricsRequest

      expectMsgAnyClassOf(Duration(1, TimeUnit.SECONDS), classOf[MetricsResponse])
    }
  }

  "Retrieve Metrics Actor" should {
    "return a metrics result that is valid json" in new ActorSys {
      val actor = makeActor(system)

      actor ! MetricsRequest

      expectMsgAllClassOf(Duration(1, TimeUnit.SECONDS), classOf[MetricsResponse]) foreach { msg =>
        msg match {
          case MetricsResponse(s: String) => {
            parseJson(s) match {
              case None => fail
              case Some(j: JsonNode) => {
                // nothing json is good
              }

            }
          }
        }
      }
    }
  }

  "Retrieve Metrics Actor" should {
    "return a json body that contains timers, meters, counters and gauges" in new ActorSys {
      val actor = makeActor(system)

      actor ! MetricsRequest

      expectMsgAllClassOf(Duration(1, TimeUnit.SECONDS), classOf[MetricsResponse]) foreach { msg =>
        msg match {
          case MetricsResponse(s: String) => {
            assert(s.contains("meters"))
            assert(s.contains("gauges"))
            assert(s.contains("timers"))
            assert(s.contains("counters"))
          }
        }
      }
    }
  }
}
