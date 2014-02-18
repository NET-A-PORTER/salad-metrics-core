package com.netaporter.salad.metrics.actor.factory

import akka.actor.{ Props, ActorRef, ActorSystem }
import com.netaporter.salad.metrics.actor.metrics.{ YammerMetricsRegistry, MetricsEventActor }
import com.netaporter.salad.metrics.actor.admin.spray.OutputMetricsActor
import com.netaporter.salad.metrics.actor.admin.RetrieveMetricsActor

/**
 * Created by d.tootell@london.net-a-porter.com on 06/02/2014.
 */
trait MetricsActorFactory {
  def eventActor()(implicit system: ActorSystem): ActorRef
  def eventTellAdminActor()(implicit system: ActorSystem): ActorRef
  def eventAskAdminActor()(implicit system: ActorSystem): ActorRef
}

object MetricsActorFactory extends MetricsActorFactory {
  override def eventActor()(implicit system: ActorSystem): ActorRef = system.actorOf(Props(new MetricsEventActor with YammerMetricsRegistry))
  override def eventTellAdminActor()(implicit system: ActorSystem): ActorRef = system.actorOf(Props(new OutputMetricsActor with YammerMetricsRegistry))
  override def eventAskAdminActor()(implicit system: ActorSystem): ActorRef = system.actorOf(Props(new RetrieveMetricsActor with YammerMetricsRegistry))
}
