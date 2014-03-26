package com.netaporter.salad.metrics.actor.factory

import akka.actor.{ ActorRefFactory, Props, ActorRef }
import com.netaporter.salad.metrics.actor.metrics.{ YammerMetricsRegistry, MetricsEventActor }
import com.netaporter.salad.metrics.actor.admin.spray.OutputMetricsActor
import com.netaporter.salad.metrics.actor.admin.RetrieveMetricsActor

/**
 * Created by d.tootell@london.net-a-porter.com on 06/02/2014.
 */
trait MetricsActorFactory {
  def eventActor()(implicit factory: ActorRefFactory): ActorRef
  def eventTellAdminActor()(implicit factory: ActorRefFactory): ActorRef
  def eventAskAdminActor()(implicit factory: ActorRefFactory): ActorRef
}

object MetricsActorFactory extends MetricsActorFactory {
  override def eventActor()(implicit factory: ActorRefFactory): ActorRef = factory.actorOf(Props(new MetricsEventActor with YammerMetricsRegistry))
  override def eventTellAdminActor()(implicit factory: ActorRefFactory): ActorRef = factory.actorOf(Props(new OutputMetricsActor with YammerMetricsRegistry))
  override def eventAskAdminActor()(implicit factory: ActorRefFactory): ActorRef = factory.actorOf(Props(new RetrieveMetricsActor with YammerMetricsRegistry))
}
