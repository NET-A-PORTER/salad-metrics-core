package com.netaporter.salad.metrics.actor.metrics

/**
 * Created by d.tootell@london.net-a-porter.com on 05/02/2014.
 */
trait MetricsRegistry[T] {
  val metricsRegistry: T
}
