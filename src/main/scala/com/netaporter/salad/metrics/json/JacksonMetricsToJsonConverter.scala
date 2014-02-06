package com.netaporter.salad.metrics.json

import com.codahale.metrics.MetricRegistry
import com.fasterxml.jackson.databind.ObjectMapper
import com.codahale.metrics.json.MetricsModule
import java.util.concurrent.TimeUnit
import com.netaporter.salad.metrics.io.ThreadUnsafeByteArrayOutputStream

/**
 * Created by d.tootell@london.net-a-porter.com on 03/02/2014.
 */
class JacksonMetricsToJsonConverter(rate: TimeUnit, duration: TimeUnit, showSamples: Boolean) extends MetricsToJsonConverter {
  def this() = this(TimeUnit.SECONDS, TimeUnit.SECONDS, false);

  val mapper = new ObjectMapper().registerModule(new MetricsModule(rate, duration, showSamples));

  override def toJsonString(reg: MetricRegistry): String = {
    val baos = new ThreadUnsafeByteArrayOutputStream(1024)
    mapper.writer().writeValue(baos, reg)
    baos.toString
  }
}
