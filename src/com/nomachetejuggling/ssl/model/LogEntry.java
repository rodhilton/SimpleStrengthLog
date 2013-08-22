package com.nomachetejuggling.ssl.model;

import java.io.Serializable;

public class LogEntry implements Serializable {
	private static final long serialVersionUID = 5995809185162465374L;
	
	public Exercise exercise;
	public int weight;
	public int reps;
	
	public int oneRepMax() {
		return (int)(((weight*reps)/30.0)+weight);
	}

}
