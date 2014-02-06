package com.netaporter.salad.metrics.actor.admin.spray

import spray.http.HttpHeaders.`Content-Type`
import spray.http._
import spray.http.HttpResponse
import spray.routing.RequestContext
import com.netaporter.salad.metrics.actor.admin.RetrieveMetricsActor

/**
 * Created by d.tootell@london.net-a-porter.com on 03/02/2014.
 */
object OutputMetricsMessages {
  // Might use this later if more completes..
  //  val marshaller = Marshaller.of[String](ContentTypes.`application/json`) {(value, contentType, ctx) =>
  //    ctx.marshalTo(value,`Content-Type`(contentType))
  //  }

  // Output the metrics to the given request context
  case class OutputMetrics(ctx: RequestContext)
}

abstract class OutputMetricsActor extends RetrieveMetricsActor {

  override def otherMetricsMessageHandler: Receive = {
    case OutputMetricsMessages.OutputMetrics(ctx: RequestContext) => {
      //      ctx.complete(converter.toJsonString(reg))(OutputMetricsMessages.marshaller)
      ctx.complete(HttpResponse(StatusCodes.OK, converter.toJsonString(metricsRegistry), List(`Content-Type`(ContentTypes.`application/json`))))
    }
  }

}
