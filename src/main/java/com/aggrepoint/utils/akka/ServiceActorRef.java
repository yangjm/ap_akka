package com.aggrepoint.utils.akka;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aggrepoint.utils.TwoValues;

import akka.actor.ActorRef;
import akka.actor.Address;

public class ServiceActorRef {
	private static final Logger logger = LoggerFactory.getLogger(ServiceActorRef.class);

	/** 可用的服务，按启动时间从近到远排序 */
	List<TwoValues<ActorRef, Long>> reachable = new ArrayList<>();
	/** 不可用的服务 */
	List<TwoValues<ActorRef, Long>> unreachable = new ArrayList<>();

	/** true表示使用最新的服务，false表示轮流使用可用的服务 */
	boolean useLatest;
	int nextSvc = 0;
	int svcType;

	public ServiceActorRef(int svcType, boolean useLatest) {
		this.svcType = svcType;
		this.useLatest = useLatest;
	}

	public ActorRef getActor() {
		if (reachable.size() == 0)
			return null;

		if (useLatest)
			return reachable.get(0).getOne();
		return reachable.get((nextSvc++) % reachable.size()).getOne();
	}

	protected void sort() {
		reachable.sort((v1, v2) -> v2.getTwo().compareTo(v1.getTwo()));
	}

	protected TwoValues<ActorRef, Long> find(List<TwoValues<ActorRef, Long>> list, ActorRef actor) {
		Optional<TwoValues<ActorRef, Long>> find = list.stream().filter(p -> p.getOne().equals(actor)).findFirst();
		if (find.isPresent())
			return find.get();
		return null;
	}

	/** 0: 已有一样的actor 1 - 添加到备用 2 - 添加到使用 -1 不匹配 */
	public int addImpl(ActorRef actor, long startTime) {
		TwoValues<ActorRef, Long> current = find(reachable, actor);
		if (current != null)
			return 0;
		current = find(unreachable, actor);
		if (current != null)
			return 0;

		TwoValues<ActorRef, Long> add = new TwoValues<ActorRef, Long>(actor, startTime);
		reachable.add(add);
		sort();
		return reachable.get(0) == add ? 2 : 1;
	}

	/** -1 不匹配 */
	public int add(AkkaServiceUpMessage upmsg, ActorRef actor) {
		if (upmsg.getType() != svcType)
			return -1;

		int ret = addImpl(actor, upmsg.getStartTime());
		switch (ret) {
		case 0:
			logger.info("Received service already known: {}", actor.path());
			break;
		case 1:
			if (useLatest)
				logger.info("Backup service added: {}", actor.path());
			else
				logger.info("Service added: {}", actor.path());
			break;
		case 2:
			if (useLatest)
				logger.info("Primary service added: {}", actor.path());
			else
				logger.info("Service added: {}", actor.path());
			break;
		}
		return ret;
	}

	/** 地址停止服务 */
	public boolean remove(Address address) {
		List<TwoValues<ActorRef, Long>> r = reachable.stream().filter(p -> p.getOne().path().address().equals(address))
				.collect(Collectors.toList());
		List<TwoValues<ActorRef, Long>> ur = unreachable.stream()
				.filter(p -> p.getOne().path().address().equals(address)).collect(Collectors.toList());

		r.addAll(ur);
		if (r.size() == 0)
			return false;

		logger.info("Services removed: [{}]",
				r.stream().map(tw -> tw.getOne().path().toString()).collect(Collectors.joining(",")));

		return true;
	}

	/** 地址不可用 */
	public boolean unreachable(Address address) {
		List<TwoValues<ActorRef, Long>> un = reachable.stream().filter(p -> p.getOne().path().address().equals(address))
				.collect(Collectors.toList());
		if (un.size() == 0)
			return false;

		logger.info("Services unreachable: [{}]",
				un.stream().map(tw -> tw.getOne().path().toString()).collect(Collectors.joining(",")));

		reachable.removeAll(un);
		unreachable.addAll(un);
		return true;
	}

	/** 地址恢复可用 */
	public boolean reachable(Address address) {
		List<TwoValues<ActorRef, Long>> un = unreachable.stream()
				.filter(p -> p.getOne().path().address().equals(address)).collect(Collectors.toList());
		if (un.size() == 0)
			return false;

		logger.info("Services reachable: [{}]",
				un.stream().map(tw -> tw.getOne().path().toString()).collect(Collectors.joining(",")));

		unreachable.removeAll(un);
		reachable.addAll(un);
		sort();
		return true;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(" reachable: [");
		sb.append(reachable.stream().map(p -> p.getOne().path().toString()).collect(Collectors.joining(", ")));
		sb.append("]");
		if (unreachable.size() > 0) {
			sb.append(" unreachable: [");
			sb.append(unreachable.stream().map(p -> p.getOne().path().toString()).collect(Collectors.joining(", ")));
			sb.append("]");
		}
		return sb.toString();
	}
}
