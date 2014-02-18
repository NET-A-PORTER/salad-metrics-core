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

package com.netaporter.salad.metrics.spray.metrics

import akka.actor.{ ActorSystem, ActorRef }
import spray.routing.Directive0

import spray.routing.Rejected
import com.netaporter.salad.metrics.messages.MetricEventMessage.MeterEvent
import spray.routing.RequestContext
import akka.actor.Status.Failure
import spray.http.HttpResponse
import com.netaporter.salad.metrics.messages.MetricEventMessage.IncCounterEvent
import com.netaporter.salad.metrics.messages.MetricEventMessage.NanoTimeEvent
import com.netaporter.salad.metrics.actor.factory.MetricsActorFactory

/**
 * Provides helper methods for metrics.
 */
object MetricHelpers {
  /**
   * Given a [[spray.routing.RequestContext]], will construct a valid string for
   * the metric name using the HTTP method and the HTTP path.  e.g.
   * `com.mycompany.mynoun.GET`.
   *
   * @param ctx The [[spray.routing.RequestContext]] for which the name should
   * be generated.
   *
   * @return The generated name for the metric.
   */
  def metricName(ctx: RequestContext): String = {
    val methodName = ctx.request.method.name
    val routeName = ctx.request.uri.path.toString()
    routeName + "." + methodName
  }

  /**
   * Given a [[spray.routing.RequestContext]], will construct a valid string for
   * the metric name using the HTTP method and the given string
   */
  def metricNameWithMethod(ctx: RequestContext, prefix: String): String = {
    val methodName = ctx.request.method.name
    return prefix + "." + methodName
  }
}

/**
 * The CounterMetric object holds helpers for code that implements Counters.
 */
object CounterMetric {
  // Declares the function type that increments a given metric
  type CountIncrementer = (String, ActorRef) ⇒ Unit

  // Increments nothing at all
  val nilIncrementer: CountIncrementer = { (_, _) ⇒ () }

  /**
   * Increments the counter specified by "prefix.postfix" in the given
   * MetricRegistry.
   *
   * @param postfix
   *   The identifier for the counter is defined with some prefix and some
   *   postfix.  The postfix is something like 'successes' or 'failures'.  The
   *   prefix may be much more dynamically determined.
   * @param prefix
   *   The prefix of the counter metric identifier.  This may be a constant
   *   defined elsewhwere or dynamically computed by the route path.
   * @param metricsActor
   *   The instance of an actor against which we will send the IncCounter request
   */
  def inc(postfix: String)(prefix: String, metricsActor: ActorRef): Unit =
    metricsActor ! IncCounterEvent(prefix + "." + postfix)
  //    metricRegistry.counter(prefix + "." + postfix).inc()

  // An instance of the [[inc]] function for successes
  val incSuccesses = inc("successes") _

  // An instance of the [[inc]] function for failures
  val incFailures = inc("failures") _

  // An instance of the [[inc]] function for rejections
  val incRejections = inc("rejections") _

  // An instance of the [[inc]] function for exceptions
  val incExceptions = inc("exceptions") _
}

/**
 * The CounterBase trait provides helpers for derivations that implement
 * specific types of Counter metrics.
 */
sealed trait CounterBase {
  import CounterMetric.CountIncrementer

  // The instance of the MetricRegistry that holdes this counter metric
  val metricsEventActor: ActorRef

  // The incrementer for the counter that counts failing metrics
  val handleFailures: CountIncrementer

  // The incrementer for the counter that counts rejecting metrics
  val handleRejections: CountIncrementer

  // The incrementer for the counter that counts excepting metrics
  val handleExceptions: CountIncrementer

  // The incrementer for the counter that counts successful metrics
  val handleSuccesses: CountIncrementer

  /**
   * The [[spray.routing.directives.BasicDirectives#around]] directive requires that the
   * caller return a function that will process what happens <i>after</i> the
   * specific [[spray.routing.Route]] completes.  This method builds that
   * function.
   *
   * @param key
   *   The prefix that we can send to any of the incrementation handlers.
   *
   * @return
   *   The function that can deal with the result of the
   *   [[spray.routing.Route]]'s evaluation.
   */
  def buildAfter(key: String): Any ⇒ Any = { possibleRsp: Any ⇒
    possibleRsp match {
      case rsp: HttpResponse ⇒
        if (rsp.status.isFailure) handleFailures(key, metricsEventActor)
        else handleSuccesses(key, metricsEventActor)
      case Rejected(_) ⇒
        handleRejections(key, metricsEventActor)
      case Failure(_) ⇒
        handleExceptions(key, metricsEventActor)
    }
    possibleRsp
  }
}

/**
 * Provides a builder that can provide a new [[spray.routing.Directive]], which
 *
 * will count successful, failed, rejected or excepted operations in a given
 * [[spray.routing.Route]]
 *
 * The actual identifiers for this counter will be, given a specific
 * `prefix`:
 *
 * - {prefix}.successes
 * - {prefix}.failures
 * - {prefix}.rejections
 * - {prefix}.exceptions
 *
 * @constructor Create the counter metric with a specific prefix.
 * @param prefix
 *   The user-specific designation for this counter's Id.
 * @param metricsEventActor
 *   The instance of the actor that is recieveing the IncCounter message requests
 * @param handleFailures
 *   A function that will increment `{prefix}.failures`. Defaults to
 *   [[com.netaporter.salad.metrics.spray.metrics.CounterMetric.CountIncrementer#nilIncrementer]].
 * @param handleRejections
 *   A function that will increment `{prefix}.rejections`. Defaults to
 *   [[com.netaporter.salad.metrics.spray.metrics.CounterMetric.CountIncrementer#nilIncrementer]].
 * @param handleExceptions
 *   A function that will increment `{prefix}.exceptions`. Defaults to
 *   [[com.netaporter.salad.metrics.spray.metrics.CounterMetric.CountIncrementer#nilIncrementer]].
 * @param handleSuccesses
 *   A function that will increment `{prefix}.successes`. Defaults to
 *   [[com.netaporter.salad.metrics.spray.metrics.CounterMetric.CountIncrementer#incSuccesses]].
 */
case class CounterMetric(
    prefix: String,
    metricsEventActor: ActorRef,
    includeMethod: Boolean = false,
    handleFailures: CounterMetric.CountIncrementer = CounterMetric.nilIncrementer,
    handleRejections: CounterMetric.CountIncrementer = CounterMetric.nilIncrementer,
    handleExceptions: CounterMetric.CountIncrementer = CounterMetric.nilIncrementer,
    handleSuccesses: CounterMetric.CountIncrementer = CounterMetric.incSuccesses) extends CounterBase {

  import CounterMetric._
  import MetricHelpers._
  import com.netaporter.salad.metrics.spray.routing.directives.BasicDirectives._

  /**
   * This is the instance of the [[spray.routing.Directive]] that you can use in
   * your [[spray.routing.Route]].
   */
  val count: Directive0 = around { ctx ⇒ (ctx, if (includeMethod) buildAfter(metricNameWithMethod(ctx, prefix)) else buildAfter(prefix)) }

  /**
   * Returns a new instance of the CounterMetric that will <i>not</i> count
   * successes. Any other counting aspect will remain as it was.
   */
  def noSuccesses: CounterMetric = copy(handleSuccesses = nilIncrementer)

  /**
   * Returns a new instance of the CounterMetric that will count rejections. Any
   * other counting aspect will remain as it was.
   */
  def rejections: CounterMetric = copy(handleRejections = incRejections)

  /**
   * Returns a new instance of the CounterMetric that will count failures. Any
   * other counting aspect will remain as it was.
   */
  def failures: CounterMetric = copy(handleFailures = incFailures)

  /**
   * Returns a new instance of the CounterMetric that will count exceptions. Any
   * other counting aspect will remain as it was.
   */
  def exceptions: CounterMetric = copy(handleExceptions = incExceptions)

  /**
   * Returns a new instance of the CounterMetric that will count successes,
   * failures, rejections and exceptions.
   */
  def all: CounterMetric = copy(handleFailures = incFailures,
    handleRejections = incRejections,
    handleExceptions = incExceptions,
    handleSuccesses = incSuccesses)
}

/**
 * Provides a builder that can provide a new [[spray.routing.Directive]], which
 * will count successful, failed, rejected or excepted operations in a given
 * [[spray.routing.Route]].
 *
 * The actual identifiers for this counter will be, depending on the incoming
 * URL (e.g. `/path/to/route` translates to `path.to.route`):
 *
 * - {path.to.route}.successes
 * - {path.to.route}.failures
 * - {path.to.route}.rejections
 * - {path.to.route}.exceptions
 *
 * @constructor Create the counter metric that will use the route path for the
 * metric name.
 * @param metricsEventActor
 *   The instance of the MetricRegistry that holds the counter metric.
 * @param handleFailures
 *   A function that will increment `{prefix}.failures`. Defaults to
 *   [[com.netaporter.salad.metrics.spray.metrics.CounterMetric.CountIncrementer#nilIncrementer]].
 * @param handleRejections
 *   A function that will increment `{prefix}.rejections`. Defaults to
 *   [[com.netaporter.salad.metrics.spray.metrics.CounterMetric.CountIncrementer#nilIncrementer]].
 * @param handleExceptions
 *   A function that will increment `{prefix}.exceptions`. Defaults to
 *   [[com.netaporter.salad.metrics.spray.metrics.CounterMetric.CountIncrementer#nilIncrementer]].
 * @param handleSuccesses
 *   A function that will increment `{prefix}.successes`. Defaults to
 *   [[com.netaporter.salad.metrics.spray.metrics.CounterMetric.CountIncrementer#incSuccesses]].
 */
case class CounterMetricByUri(metricsEventActor: ActorRef,
    handleFailures: CounterMetric.CountIncrementer = CounterMetric.nilIncrementer,
    handleRejections: CounterMetric.CountIncrementer = CounterMetric.nilIncrementer,
    handleExceptions: CounterMetric.CountIncrementer = CounterMetric.nilIncrementer,
    handleSuccesses: CounterMetric.CountIncrementer = CounterMetric.incSuccesses) extends CounterBase {

  import CounterMetric._
  import MetricHelpers._
  import com.netaporter.salad.metrics.spray.routing.directives.BasicDirectives._

  /**
   * This is the instance of the [[spray.routing.Directive]] that you can use in
   * your [[spray.routing.Route]].
   */
  val count: Directive0 = around { ctx ⇒
    (ctx, buildAfter(metricName(ctx)))
  }

  /**
   * Returns a new instance of the CounterMetric that will <i>not</i> count
   * successes. Any other counting aspect will remain as it was.
   */
  def noSuccesses: CounterMetricByUri = copy(handleSuccesses = nilIncrementer)

  /**
   * Returns a new instance of the CounterMetric that will count rejections. Any
   * other counting aspect will remain as it was.
   */
  def failures: CounterMetricByUri = copy(handleFailures = incFailures)

  /**
   * Returns a new instance of the CounterMetric that will count failures. Any
   * other counting aspect will remain as it was.
   */
  def rejections: CounterMetricByUri = copy(handleRejections = incRejections)

  /**
   * Returns a new instance of the CounterMetric that will count exceptions. Any
   * other counting aspect will remain as it was.
   */
  def exceptions: CounterMetricByUri = copy(handleExceptions = incExceptions)

  /**
   * Returns a new instance of the CounterMetric that will count successes,
   * failures, rejections and exceptions.
   */
  def all: CounterMetricByUri = copy(handleFailures = incFailures,
    handleRejections = incRejections,
    handleExceptions = incExceptions,
    handleSuccesses = incSuccesses)
}

/**
 * The TimerBase trait provides helpers for derivations that implement specific
 * types of Timer metrics.
 */
sealed trait TimerBase {

  val metricsEventActor: ActorRef
  /**
   * The [[spray.routing.directives.BasicDirectives#around]] directive requires that the
   * caller return a function that will process what happens <i>after</i> the
   * specific [[spray.routing.Route]] completes.  This method builds that
   * function.
   *
   * @param timerName
   *   The name of the event
   *
   * @param startNanoTime
   *   The start nano time of the context that was originally started in the `before` part
   *   of the [[spray.routing.directives.BasicDirectives#around]]
   *   [[spray.routing.Directive]].
   *
   * @return
   *   The function that will stop the timer on any result whatsoever.
   */
  def buildAfter(timerName: String, startNanoTime: Long): Any ⇒ Any = { possibleRsp: Any ⇒
    possibleRsp match {
      case _ ⇒
        metricsEventActor ! NanoTimeEvent(timerName, System.nanoTime() - startNanoTime)
    }
    possibleRsp
  }
}

/**
 * Provides a Timer metric that will record times on the timer under the name
 * given.
 *
 * @constructor Create the timer metric with a specific name.
 * @param timerName
 *   The name for this particular timer.
 * @param metricsEventActor
 *   The instance of an ActorRef to whcih we send the elapsed time of a event (nanos)
 */
case class TimerMetric(timerName: String, metricsEventActor: ActorRef) extends TimerBase {
  import com.netaporter.salad.metrics.spray.routing.directives.BasicDirectives._

  /**
   * This is the instance of the [[spray.routing.Directive]] that you can use in
   * your [[spray.routing.Route]].
   */
  val time: Directive0 =
    around { ctx ⇒
      val startTime = System.nanoTime();
      (ctx, buildAfter(timerName, startTime))
    }
}

/**
 * Provides a Timer metric that will record times on the timer under the name
 * given.
 *
 * @constructor Create the timer metric with a specific name.
 *
 * @param metricsEventActor
 *   The instance of an ActorRef to whcih we send the elapsed time of a event (nanos)
 */
case class TimerMetricByUri(metricsEventActor: ActorRef) extends TimerBase {
  import com.netaporter.salad.metrics.spray.routing.directives.BasicDirectives._
  import MetricHelpers._
  /**
   * This is the instance of the [[spray.routing.Directive]] that you can use in
   * your [[spray.routing.Route]].
   */
  val time: Directive0 =
    around { ctx ⇒
      val startTime = System.nanoTime();
      (ctx, buildAfter(metricName(ctx), startTime))
    }
}

sealed trait BaseMeterMetric {
  val metricsEventActor: ActorRef
  /**
   * The [[spray.routing.directives.BasicDirectives#around]] directive requires that the
   * caller return a function that will process what happens <i>after</i> the
   * specific [[spray.routing.Route]] completes.  This method builds that
   * function.
   *
   * @param timerName
   *   The name of the event
   *
   *
   * @return
   *   The function that will stop the timer on any result whatsoever.
   */
  def buildAfter(timerName: String): Any ⇒ Any = { possibleRsp: Any ⇒
    possibleRsp match {
      case _ ⇒
        metricsEventActor ! MeterEvent(timerName)
    }
    possibleRsp
  }
}
/**
 * Provides a Meter metric that will mark a specific event under a Meter of a
 * specific name.
 *
 * @constructor Create the meter metric with a specific name.
 * @param meterName
 *   The name under which the meter exists.
 * @param metricsEventActor
 *   The instance of the Actor to which a Meter event should be sent
 */
case class MeterMetric(meterName: String, metricsEventActor: ActorRef) {
  import com.netaporter.salad.metrics.spray.routing.directives.BasicDirectives._

  /**
   * This is the instance of the [[spray.routing.Directive]] that you can use in
   * your [[spray.routing.Route]].
   */
  val meter: Directive0 = mapRequestContext { ctx ⇒
    metricsEventActor ! MeterEvent(meterName)
    ctx
  }
}

/**
 * Provides a Meter metric that will mark a specific event under a Meter of a
 * specific name, when the request has completed and is being sent to the user
 *
 * @constructor Create the meter metric with a specific name.
 * @param meterName
 *   The name under which the meter exists.
 * @param metricsEventActor
 *   The instance of the Actor to which a Meter event should be sent
 */
case class MeterMetricOut(meterName: String, metricsEventActor: ActorRef) extends BaseMeterMetric {
  import com.netaporter.salad.metrics.spray.routing.directives.BasicDirectives._

  /**
   * This is the instance of the [[spray.routing.Directive]] that you can use in
   * your [[spray.routing.Route]].
   */
  val meter: Directive0 = around { ctx ⇒
    (ctx, buildAfter(meterName))
  }
}

case class MeterMetricByUri(metricsEventActor: ActorRef) extends BaseMeterMetric {
  import com.netaporter.salad.metrics.spray.routing.directives.BasicDirectives._
  import MetricHelpers._
  /**
   * This is the instance of the [[spray.routing.Directive]] that you can use in
   * your [[spray.routing.Route]].
   */
  val meter: Directive0 = around { ctx ⇒
    (ctx, buildAfter(metricName(ctx)))
  }
}

/**
 * Provides a Meter metric that will mark a specific event under a Meter that
 * has an identifier that matches the current [[spray.routing.Route]] path.
 *
 * @constructor Create the meter metric that will use the current path route for
 * the metric name.
 * // * @param metricsEventActor
 *   The instance of the Actor in which send meter events.
 */
//case class MeterMetricByUri(metricRegistry: MetricRegistry) {
//  import MetricHelpers._
//  import spray.routing.directives.BasicDirectives._
//
//  /**
//   * This is the instance of the [[spray.routing.Directive]] that you can use in
//   * your [[spray.routing.Route]].
//   */
//  val meter: Directive0 = mapRequestContext { ctx ⇒
//    metricRegistry.meter(metricName(ctx)).mark()
//    ctx
//  }
//}

/**
 * Provides an entry point to creating metric accumulators specific to the Coda
 * Hale Metrics library (http://metrics.codahale.com).
 *
 * ==Overview==
 *
 * This trait is intended to be used to construct objects that provide
 * [[spray.routing.Directive]]s, which can then be used to instrument your
 * [[spray.routing.Route]]s with metrics accumulators.  You would create these
 * instances, and then join them together however you like in order to ease how
 * your code is instrumented.
 *
 * ==Usage==
 *
 * {{{
 * import com.codahale.metrics.MetricRegistry
 *
 * class MyApp extends Directives {
 *   val metricRegistry = new MetricRegistry()
 *   val metricFactory = CodaHaleMetricsDirectiveFactory(metricRegistry)
 *
 *   // Creates a counter that measures failures only under the name of the
 *   // path to the current route
 *   val counter = metricFactory.counter.failures.noSuccesses.count
 *
 *   // Creates a timer that measures everything under the name of the
 *   // path to the current route
 *   val timer = metricFactory.timer.time
 *
 *   // Joins the two metrics into a single directive
 *   val measure = counter | timer
 *
 *   val apiRoute =
 *     path("something") {
 *       measure {
 *         get {
 *           // do the thing
 *         }
 *       }
 *     }
 * }
 * }}}
 */
trait MetricsDirectiveFactory {

  val defaultMetricsActorFactory: MetricsActorFactory

  // The instance of the MetricRegistry in which you want to store your metrics
  val metricsEventActor: ActorRef

  /**
   * Creates an instance of a [[CounterMetric]] with a specific prefix name that
   * counts successes by default.
   *
   * @param counterPrefix
   *   The prefix of the counter's identifier.
   *
   * @return
   *   The instance of the [[CounterMetric]] you can use to count events.
   */
  def counter(counterPrefix: String): CounterMetric = new CounterMetric(counterPrefix, metricsEventActor)
  def counterWithMethod(counterPrefix: String): CounterMetric = new CounterMetric(counterPrefix, metricsEventActor, true)

  /**
   * Creates an instance of a [[CounterMetric]] with a specific prefix name that
   * counts all types of events.
   *
   * @param counterPrefix
   *   The prefix of the counter's identifier.
   *
   * @return
   *   The instance of the [[CounterMetric]] you can use to count events.
   */
  def allCounter(counterPrefix: String): CounterMetric = new CounterMetric(counterPrefix, metricsEventActor).all

  /**
   * Creates an instance of a [[CounterMetric]] that counts successes by
   * default under an identifier unique to the path to the current route..
   *
   * @return
   *   The instance of the [[CounterMetric]] you can use to count events.
   */
  def counter: CounterMetricByUri = new CounterMetricByUri(metricsEventActor)

  /**
   * Creates an instance of a [[TimerMetric]] that measures events with a
   * specific name.
   *
   * @param timerName
   *   The name of the timer in which measured events should be recorded.
   *
   * @return
   *   The instance of the [[TimerMetric]] you can use to measure events.
   */
  def timer(timerName: String): TimerMetric = new TimerMetric(timerName, metricsEventActor)

  /**
   * Creates an instance of a [[TimerMetric]] that measures events with a
   * name specific to the path to the current route.
   *
   * @return
   *   The instance of the [[TimerMetric]] you can use to measure events.
   */
  def timer: TimerMetricByUri = new TimerMetricByUri(metricsEventActor)

  /**
   * Creates an instance of a [[MeterMetric]] that measures events with a
   * specific name.
   *
   * @param meterName
   *   The name of the meter in which measured events should be recorded.
   *
   * @return
   *   The instance of the [[MeterMetric]] you can use to measure events.
   */
  def meter(meterName: String): MeterMetric = new MeterMetric(meterName, metricsEventActor)

  /**
   * Creates an instance of a [[MeterMetric]] that measures events with a
   * name specific to the path to the current route.
   *
   * @return
   *   The instance of the [[MeterMetric]] you can use to measure events.
   */
  def meter: MeterMetricByUri = new MeterMetricByUri(metricsEventActor)
}

/**
 * Provides construction for an instance of the CodaHaleMetricsDirectiveFactory.
 */
object MetricsDirectiveFactory {

  /**
   * Constructs an instance of the Metrics factory that sends metrics events to the
   * given Actor.  The metrics event message are that of those found in
   * {@link MetricEventMessage}
   *
   *
   * @param actor
   *   The instance of the Actor that will be sent events to record
   *   metrics.
   * @return
   *   The instance of the CodaHaleMetricsDirectiveFactory to be used.
   */
  def apply(actor: ActorRef) = new MetricsDirectiveFactory {
    override val defaultMetricsActorFactory = MetricsActorFactory
    override val metricsEventActor = actor
  }

  /**
   * Constructs an instance of the Metrics factory that send metrics to
   * Yammer metrics actor.  That records metrics events using the yammer codehale
   * metrics library.
   */
  def apply()(implicit system: ActorSystem) = new MetricsDirectiveFactory {
    override val defaultMetricsActorFactory = MetricsActorFactory
    override val metricsEventActor: ActorRef = defaultMetricsActorFactory.eventActor()
  }
}