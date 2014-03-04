package com.netaporter.salad.metrics.actor.admin.spray

import spray.http.HttpHeaders.{ `Cache-Control`, `Content-Type` }
import spray.http._
import spray.http.HttpResponse
import spray.routing.RequestContext
import com.netaporter.salad.metrics.actor.admin.RetrieveMetricsActor
import spray.httpx.marshalling.{ ToResponseMarshaller, ToResponseMarshallingContext, Marshaller, BasicMarshallers }
import spray.http.CacheDirectives.{ `must-revalidate`, `no-cache`, `no-store` }

/**
 * Created by d.tootell@london.net-a-porter.com on 03/02/2014.
 */
object OutputMetricsMessages {
  // Output the metrics to the given request context
  case class OutputMetrics(ctx: RequestContext)
}

abstract class OutputMetricsActor extends RetrieveMetricsActor {

  override def otherMetricsMessageHandler: Receive = {
    case OutputMetricsMessages.OutputMetrics(ctx: RequestContext) => {
      val json = converter.toJsonString(metricsRegistry)
      ctx.complete(HttpResponse(StatusCodes.OK, HttpEntity(ContentTypes.`application/json`, json),
        List(`Cache-Control`(`must-revalidate`, `no-store`, `no-cache`))))

    }
  }

}
