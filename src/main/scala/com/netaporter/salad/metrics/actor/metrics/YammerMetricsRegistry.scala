package com.netaporter.salad.metrics.actor.metrics

import com.codahale.metrics.MetricRegistry

object YammerMetricsRegistry {
  val metricsRegistry = new MetricRegistry
}

/**
 * Created by d.tootell@london.net-a-porter.com on 06/02/2014.
 */
trait YammerMetricsRegistry extends MetricsRegistry[MetricRegistry] {
  override val metricsRegistry: MetricRegistry = YammerMetricsRegistry.metricsRegistry
}
