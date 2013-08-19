package com.nomachetejuggling.ssl.model;

import java.io.Serializable;

public class LogEntry implements Serializable {
	private static final long serialVersionUID = 5995809185162465374L;
	
	public Exercise exercise;
	public int weight;
	public int reps;
	
	@Override
	public String toString() {
		int oneRM=(int)(((weight*reps)/30.0)+weight);
		return weight+"x"+reps+" (1RM="+oneRM+")";
	}

}
