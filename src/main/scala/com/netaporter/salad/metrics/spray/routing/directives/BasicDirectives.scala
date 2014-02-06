/*
 * Copyright (C) 2011-2013 spray.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//package com.netaporter.salad.spray.routing
package com.netaporter.salad.metrics.spray.routing.directives

import scala.util.control.NonFatal
import shapeless._
import spray.http._
import spray.routing._
import spray.http.HttpRequest
import spray.http.HttpResponse
import spray.routing.RequestContext
import akka.actor.Status.Failure

// Basically provides what could be also known as a AOP around context for
// spray routes
trait BasicDirectives {

  def around(before: RequestContext ⇒ (RequestContext, Any ⇒ Any)): Directive0 =
    mapInnerRoute { inner ⇒
      ctx ⇒
        val (ctxForInnerRoute, after) = before(ctx)
        try inner(ctxForInnerRoute.withRouteResponseMapped(after))
        catch { case NonFatal(ex) ⇒ after(Failure(ex)) }
    }

  def mapInnerRoute(f: Route ⇒ Route): Directive0 = new Directive0 {
    def happly(inner: HNil ⇒ Route) = f(inner(HNil))
  }

  def mapRequestContext(f: RequestContext ⇒ RequestContext): Directive0 =
    mapInnerRoute { inner ⇒ ctx ⇒ inner(f(ctx)) }

  def mapRequest(f: HttpRequest ⇒ HttpRequest): Directive0 =
    mapRequestContext(_.withRequestMapped(f))

  def routeRouteResponse(f: PartialFunction[Any, Route]): Directive0 =
    mapRequestContext(_.withRouteResponseRouting(f))

  def mapRouteResponse(f: Any ⇒ Any): Directive0 =
    mapRequestContext(_.withRouteResponseMapped(f))

  def mapRouteResponsePF(f: PartialFunction[Any, Any]): Directive0 =
    mapRequestContext(_.withRouteResponseMappedPF(f))

  def mapRejections(f: List[Rejection] ⇒ List[Rejection]): Directive0 =
    mapRequestContext(_.withRejectionsMapped(f))

  def mapHttpResponsePart(f: HttpResponsePart ⇒ HttpResponsePart): Directive0 =
    mapRequestContext(_.withHttpResponsePartMapped(f))

  def mapHttpResponse(f: HttpResponse ⇒ HttpResponse): Directive0 =
    mapRequestContext(_.withHttpResponseMapped(f))

  def mapHttpResponseEntity(f: HttpEntity ⇒ HttpEntity): Directive0 =
    mapRequestContext(_.withHttpResponseEntityMapped(f))

  def mapHttpResponseHeaders(f: List[HttpHeader] ⇒ List[HttpHeader]): Directive0 =
    mapRequestContext(_.withHttpResponseHeadersMapped(f))

  /**
   * A Directive0 that always passes the request on to its inner route
   * (i.e. does nothing with the request or the response).
   */
  def noop: Directive0 = Directive.Empty

  /**
   * A simple alias for the `noop` directive.
   */
  def pass: Directive0 = noop

  /**
   * Injects the given value into a directive.
   */
  def provide[T](value: T): Directive1[T] = hprovide(value :: HNil)

  /**
   * Injects the given values into a directive.
   */
  def hprovide[L <: HList](values: L): Directive[L] = new Directive[L] {
    def happly(f: L ⇒ Route) = f(values)
  }

  /**
   * Extracts a single value using the given function.
   */
  def extract[T](f: RequestContext ⇒ T): Directive1[T] =
    hextract(ctx ⇒ f(ctx) :: HNil)

  /**
   * Extracts a number of values using the given function.
   */
  def hextract[L <: HList](f: RequestContext ⇒ L): Directive[L] = new Directive[L] {
    def happly(inner: L ⇒ Route) = ctx ⇒ inner(f(ctx))(ctx)
  }
}

object BasicDirectives extends BasicDirectives