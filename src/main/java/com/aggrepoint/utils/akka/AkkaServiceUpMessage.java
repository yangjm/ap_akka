package com.aggrepoint.utils.akka;

import java.io.Serializable;

public class AkkaServiceUpMessage implements Serializable {
	private static final long serialVersionUID = 1L;

	private int type;
	private long startTime;

	public AkkaServiceUpMessage(int type, long startTime) {
		this.type = type;
		this.startTime = startTime;
	}

	public int getType() {
		return type;
	}

	public long getStartTime() {
		return startTime;
	}
}
