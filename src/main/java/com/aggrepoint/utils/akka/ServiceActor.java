package com.aggrepoint.utils.akka;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import akka.actor.ActorSystem;
import akka.actor.UntypedAbstractActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import scala.concurrent.duration.Duration;

public abstract class ServiceActor extends UntypedAbstractActor {
	@Autowired
	ActorSystem system;

	/** 启动时间 */
	long startTime;

	@Override
	public void preStart() {
		startTime = System.currentTimeMillis();

		// 启动Mediator
		DistributedPubSub.get(system).mediator();

		Cluster.get(system).subscribe(getSelf(), ClusterEvent.initialStateAsEvents(), MemberUp.class);
	}

	public boolean onReceive(Logger logger, Object message, String clientRole, String topic, int serviceType,
			long delay) throws Throwable {
		if (message instanceof MemberUp) {
			MemberUp m = (MemberUp) message;

			if (m.member().hasRole(clientRole)) { // 延迟一些时间等待前端pubsub准备好，将本服务告知集群中的前端节点
				logger.info("Found node " + m.member().address() + " with role " + clientRole + " is up");

				system.scheduler().scheduleOnce(Duration.create(delay, TimeUnit.MILLISECONDS), () -> {
					logger.info("Publishing service address " + getSelf().path());

					DistributedPubSub.get(system).mediator().tell(new DistributedPubSubMediator.Publish(topic,
							new AkkaServiceUpMessage(serviceType, startTime)), getSelf());
				}, getContext().system().dispatcher());
				return true;
			}
		}

		return false;
	}
}
