package com.nomachetejuggling.ssl.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MuscleGroups implements Serializable {
	private static final long serialVersionUID = -2281995191088855472L;
	
	private Map<String, List<String>> muscleGroupToMuscles;
	private ArrayList<String> cachedMuscleGroups;
	private ArrayList<String> cachedMuscles;

	public MuscleGroups(Map<String, List<String>> muscleGroupToMuscles) {
		this.muscleGroupToMuscles = muscleGroupToMuscles;
	}
	
	synchronized public ArrayList<String> getMuscleGroups() {
		if(this.cachedMuscleGroups == null) {
			this.cachedMuscleGroups = cacheMuscleGroups();
		}
		return this.cachedMuscleGroups;
	}
	
	synchronized public ArrayList<String> getMuscles() {
		if(this.cachedMuscles == null) {
			this.cachedMuscles = cacheMuscles();
		}
		return this.cachedMuscles;
	}
	
	private ArrayList<String> cacheMuscleGroups() {
		ArrayList<String> groups = new ArrayList<String>();
		groups.addAll(muscleGroupToMuscles.keySet());
		Collections.sort(groups);
		return groups;
	}
	
	private ArrayList<String> cacheMuscles() {
		Set<String> muscles = new HashSet<String>();
		for(List<String> someMuscles: muscleGroupToMuscles.values()) {
			muscles.addAll(someMuscles);		
		}
		ArrayList<String> muscleList = new ArrayList<String>();
		muscleList.addAll(muscles);
		Collections.sort(muscleList);
		return muscleList;
	}
}
