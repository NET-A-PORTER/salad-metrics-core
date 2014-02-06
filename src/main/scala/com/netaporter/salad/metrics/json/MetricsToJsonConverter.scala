package com.netaporter.salad.metrics.json

import com.codahale.metrics.MetricRegistry

/**
 * Created by d.tootell@london.net-a-porter.com on 03/02/2014.
 */
trait MetricsToJsonConverter {
  /**
   * Given a MetricRegistry, converts the contains stats into a json String
   * @param reg
   * @return
   */
  def toJsonString(reg: MetricRegistry): String

}
