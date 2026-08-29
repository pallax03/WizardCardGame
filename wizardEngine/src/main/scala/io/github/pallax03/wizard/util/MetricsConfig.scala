package io.github.pallax03.wizard.util

import io.vertx.core.VertxOptions
import io.vertx.micrometer.{MicrometerMetricsOptions, VertxPrometheusOptions}

object MetricsConfig:
  private val prometheusOptions = VertxPrometheusOptions()
    .setEnabled(true)
  
  private val metricsOptions = new MicrometerMetricsOptions()
    .setPrometheusOptions(prometheusOptions)
    .setEnabled(true)

  val vertxOptions: VertxOptions = new VertxOptions().setMetricsOptions(metricsOptions)
