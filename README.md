# SALAD Metrics Core

Contains shared metrics library code used by the LAD API and the Search API .

## Implementation Overview ##

The library is an extension(^tm) of: https://github.com/spray/spray/pull/359
When we say extension we mean an adaption of this pull request.

The main differences between this library's implementation of the Metrics
implementation and that of https://github.com/spray/spray/pull/359, is:

The Metrics (Timer, Counter, Meter) are sent to an Actor.  It is the actor
that is responsible for recording the metric into the Yammer MetricsRegistry (http://metrics.codahale.com/)

## Simple usage ##

The pull request https://github.com/spray/spray/pull/359 defines a set of spray routing aware metrics directives.
These directives either record events as they come into the route (A counter), or wrap the current spray directive
what could be considered kind of like an aspect.  The aspect is "around" the existing directive call and at the end of
the method records the event (how long the event took), i.e. a Timer.

Here is an example that times GET("/bob") and counts the number of requests to GET("/bob")

    // Get the metrics spray routing aware directive factory
    val factory: MetricsDirectiveFactory = MetricsDirectiveFactory()

    // Create a timer for timing GET("/bob") requests
    val time = factory.timer("bobrequests").time

    // Create a counter for counting GET("/bob") requests
    val bobrequests = factory.counter("bobrequests").all.count

    // Join the metrics together saying, I'm monitoring both the time and num of requests for GET("/bob")
    val bobMetrics = time & requestCounter


    // use the metrics in your routing

    path("bob") {
     get {
         bobMetrics {
             complete {
                 <h1>Say hello to spray</h1>
             }
         }
     }
    }


## Outputting the JSON metrics ##

The codehale metrics library includes a metrics servlet:  (http://metrics.codahale.com/manual/servlets/#metricsservlet)
As this metrics servlet is very java and servlet specific, a custom actor has be created that works with the MetricRegisty
that the MetricsDirectiveFactory() is using.

    // get the admin actor that outputs the json
    val metricsOutputActor = factory.defaultMetricsActorFactory.eventTellAdminActor()

    // Set a path to for handling ("/admin/metrics") that outputs json.
    //
    // {"version":"3.0.0","gauges":{},"counters":{"bobrequests.successes":{"count":1}},"histograms":{},"meters":{},
    // "timers":{"bobrequests":{"count":1,"max":0.054916000000000006,"mean":0.054916000000000006,"min":0.054916000000000006,
    // "p50":0.054916000000000006,"p75":0.054916000000000006,"p95":0.054916000000000006,"p98":0.054916000000000006,
    // "p99":0.054916000000000006,"p999":0.054916000000000006,"stddev":0.0,"m15_rate":0.0,"m1_rate":0.0,"m5_rate":0.0,
    // "mean_rate":4.291145650065655,"duration_units":"seconds","rate_units":"calls/second"}}}

    path("admin" / "metrics") {
      get {
          ctx => metricsOutputActor ! OutputMetrics(ctx)
      }
    }
