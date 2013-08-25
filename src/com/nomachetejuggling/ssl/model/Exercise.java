package com.nomachetejuggling.ssl.model;

import java.io.Serializable;

public class Exercise implements Serializable, Comparable<Exercise> {
	private static final long serialVersionUID = -848012361873659720L;
	
	public String name;
	public int restTime = 90;
	public String[] muscles = new String[]{};
	public boolean favorite = false;

	
	@Override
	public String toString() {
		return "Exercise("+name+")";
	}

	@Override
	public int compareTo(Exercise arg0) {
		return name.trim().toUpperCase().compareTo(arg0.name.trim().toUpperCase());
	}

	public void copyFrom(Exercise editedExercise) {
		this.name = editedExercise.name;
		this.restTime = editedExercise.restTime;
		this.muscles = editedExercise.muscles;
		this.favorite = editedExercise.favorite;
		
	}	
}
