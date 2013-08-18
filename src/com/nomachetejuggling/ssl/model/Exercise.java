package com.nomachetejuggling.ssl.model;

import java.io.Serializable;

public class Exercise implements Serializable, Comparable<Exercise> {
	private static final long serialVersionUID = -848012361873659700L;
	
	private String name;
	
	public Exercise() {
		
	}

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	
	@Override
	public String toString() {
		return "Exercise("+name+")";
	}

	@Override
	public int compareTo(Exercise arg0) {
		return this.getName().trim().toLowerCase().compareTo(arg0.getName().trim().toLowerCase());
	}
}
