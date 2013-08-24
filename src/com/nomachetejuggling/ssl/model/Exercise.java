package com.nomachetejuggling.ssl.model;

import java.io.Serializable;

public class Exercise implements Serializable, Comparable<Exercise> {
	private static final long serialVersionUID = -848012361873659700L;
	
	public String name;
	public int restTime;
	public String[] muscles = new String[]{};

	public Exercise() {
		
	}
	
	@Override
	public String toString() {
		return "Exercise("+name+")";
	}

	@Override
	public int compareTo(Exercise arg0) {
		return this.name.trim().toLowerCase().compareTo(arg0.name.trim().toLowerCase());
	}
	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Exercise other = (Exercise) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}
