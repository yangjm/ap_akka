package com.aggrepoint.utils.akka;

import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import akka.actor.Actor;
import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.IndirectActorProducer;
import akka.actor.Props;
import akka.cluster.singleton.ClusterSingletonManager;
import akka.cluster.singleton.ClusterSingletonManagerSettings;

/**
 * 创建Spring容器管理的Actor
 */
@Component
public class ActorCreator {
	@Autowired
	private ApplicationContext context;
	@Autowired
	private ActorSystem system;

	public static class SpringActorProducer<T extends Actor> implements IndirectActorProducer {
		private ApplicationContext applicationContext;
		private Class<T> actorClass;
		private Consumer<T> init;

		public SpringActorProducer(ApplicationContext applicationContext, Class<T> actorClass, Consumer<T> init) {
			this.applicationContext = applicationContext;
			this.actorClass = actorClass;
			this.init = init;
		}

		@Override
		public Actor produce() {
			T a = (T) applicationContext.getBean(actorClass);
			if (init != null)
				init.accept(a);
			return a;
		}

		@Override
		public Class<? extends Actor> actorClass() {
			return actorClass;
		}
	}

	/**
	 * 创建/user下的actor
	 */
	public <T extends Actor> ActorRef create(Class<T> actorClass, Consumer<T> init, String name) {
		return system.actorOf(Props.create(SpringActorProducer.class, context, actorClass, init), name);
	}

	public <T extends Actor> ActorRef create(Class<T> actorClass) {
		return system.actorOf(Props.create(SpringActorProducer.class, context, actorClass, null));
	}

	public <T extends Actor> ActorRef create(Class<T> actorClass, Consumer<T> init) {
		return system.actorOf(Props.create(SpringActorProducer.class, context, actorClass, init));
	}

	public <T extends Actor> ActorRef create(Class<T> actorClass, String name) {
		return system.actorOf(Props.create(SpringActorProducer.class, context, actorClass, null), name);
	}

	public <T extends Actor> ActorRef create(Class<T> actorClass, String name, Consumer<T> init) {
		return system.actorOf(Props.create(SpringActorProducer.class, context, actorClass, init), name);
	}

	/**
	 * @param nodeRole
	 *            运行的node的角色
	 * @param actorClass
	 *            actor类
	 * @param init
	 *            初始化方法
	 * @param name
	 *            actor名称
	 * @param terminationMessage
	 *            终止消息
	 * @return
	 */
	public <T extends Actor> ActorRef singleton(String nodeRole, Class<T> actorClass, Consumer<T> init, String name,
			Object terminationMessage) {
		final ClusterSingletonManagerSettings settings = ClusterSingletonManagerSettings.create(system);
		if (!StringUtils.isEmpty(nodeRole))
			settings.withRole(nodeRole);

		return system.actorOf(
				ClusterSingletonManager.props(Props.create(SpringActorProducer.class, context, actorClass, init),
						terminationMessage, settings),
				name);
	}

	/**
	 * 创建actor下的actor
	 */
	public <T extends Actor> ActorRef create(ActorContext actorContext, Class<T> actorClass, Consumer<T> init,
			String name) {
		return actorContext.actorOf(Props.create(SpringActorProducer.class, context, actorClass, init), name);
	}

	public <T extends Actor> ActorRef create(ActorContext actorContext, Class<T> actorClass) {
		return actorContext.actorOf(Props.create(SpringActorProducer.class, context, actorClass, null));
	}

	public <T extends Actor> ActorRef create(ActorContext actorContext, Class<T> actorClass, Consumer<T> init) {
		return actorContext.actorOf(Props.create(SpringActorProducer.class, context, actorClass, init));
	}

	public <T extends Actor> ActorRef create(ActorContext actorContext, Class<T> actorClass, String name) {
		return actorContext.actorOf(Props.create(SpringActorProducer.class, context, actorClass, null), name);
	}

	public <T extends Actor> ActorRef create(ActorContext actorContext, Class<T> actorClass, String name,
			Consumer<T> init) {
		return actorContext.actorOf(Props.create(SpringActorProducer.class, context, actorClass, init), name);
	}
}
