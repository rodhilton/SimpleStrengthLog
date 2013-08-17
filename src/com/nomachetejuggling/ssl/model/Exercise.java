package com.nomachetejuggling.ssl.model;

import java.io.Serializable;

public class Exercise implements Serializable {
	private static final long serialVersionUID = -8480123618738659700L;
	
	private String name;
	public Exercise() {
		
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return "Exercise("+name+")";
	}
}
