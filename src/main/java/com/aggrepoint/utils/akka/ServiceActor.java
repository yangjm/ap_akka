package com.aggrepoint.utils.akka;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.UntypedAbstractActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import scala.concurrent.duration.Duration;

public abstract class ServiceActor extends UntypedAbstractActor {
	/** 接收客户方获取服务的请求 */
	public static final String TOPIC_SERVICE_DISCOVERY = ServiceActor.class.getName() + ".service_discovery";
	/** 将本服务广播给客户方 */
	public static final String TOPIC_SERVICE_UP = ServiceActor.class.getName() + ".service_up";

	@Autowired
	ActorSystem system;

	/** 启动时间 */
	long startTime;

	@Override
	public void preStart() {
		startTime = System.currentTimeMillis();

		// 订阅服务发现请求
		ActorRef mediator = DistributedPubSub.get(system).mediator();
		mediator.tell(new DistributedPubSubMediator.Subscribe(TOPIC_SERVICE_DISCOVERY, getSelf()), getSelf());

		// 订阅节点启动
		Cluster.get(system).subscribe(getSelf(), ClusterEvent.initialStateAsEvents(), MemberUp.class);
	}

	@Override
	public void postStop() {
		DistributedPubSub.get(system).mediator().tell(
				new DistributedPubSubMediator.Unsubscribe(ServiceActor.TOPIC_SERVICE_DISCOVERY, getSelf()), getSelf());

		Cluster.get(system).unsubscribe(getSelf());
	}

	public boolean onReceive(Logger logger, Object message, String clientRole, int serviceType, long delay)
			throws Throwable {
		if (message instanceof MemberUp) { // 自己启动，或者其他节点启动
			MemberUp m = (MemberUp) message;

			if (m.member().hasRole(clientRole)) { // 延迟一些时间等待前端pubsub准备好，将本服务告知集群中的前端节点
				logger.info("Found node " + m.member().address() + " with role " + clientRole + " is up");

				system.scheduler().scheduleOnce(Duration.create(delay, TimeUnit.MILLISECONDS), () -> {
					logger.info("Publishing service address " + getSelf().path());

					DistributedPubSub.get(system).mediator().tell(new DistributedPubSubMediator.Publish(
							TOPIC_SERVICE_UP, new AkkaServiceUpMessage(serviceType, startTime)), getSelf());
				}, getContext().system().dispatcher());
				return true;
			}
		} else if (message instanceof AkkaServiceDiscoveryMessage) {
			AkkaServiceDiscoveryMessage msg = (AkkaServiceDiscoveryMessage) message;
			if (msg.has(serviceType)) {
				logger.info("Received lookup from {}", getSender().path());
				getSender().tell(new AkkaServiceUpMessage(serviceType, startTime), getSelf());
			}
		}

		return false;
	}
}
