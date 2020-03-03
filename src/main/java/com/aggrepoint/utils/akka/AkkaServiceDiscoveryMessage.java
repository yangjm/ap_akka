package com.aggrepoint.utils.akka;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class AkkaServiceDiscoveryMessage implements Serializable {
	private static final long serialVersionUID = 1L;

	private Set<Integer> types = new HashSet<>();

	public AkkaServiceDiscoveryMessage(int... types) {
		if (types.length > 0)
			for (int t : types)
				this.types.add(t);
	}

	public boolean has(int type) {
		return types.contains(type);
	}
}
