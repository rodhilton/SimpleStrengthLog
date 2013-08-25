package com.nomachetejuggling.ssl;

import com.nomachetejuggling.ssl.model.MuscleGroups;

import android.content.Context;
import android.view.ActionProvider;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.SubMenu;
import android.view.View;

public class ExerciseListFilterActionProvider extends ActionProvider implements OnMenuItemClickListener {

	private Context mContext;
	private MuscleGroups muscleGroups;

	public ExerciseListFilterActionProvider(Context context) {
		super(context);
		mContext = context;
	}

	@Override
	public View onCreateActionView() {		
		return null;
	}

	@Override
	public boolean hasSubMenu() {
		return true;
	}

	@Override
	public void onPrepareSubMenu(SubMenu subMenu) {
		subMenu.clear();
		subMenu.add("All").setOnMenuItemClickListener(this);
		subMenu.add("Favorites").setOnMenuItemClickListener(this);;
		for(String muscleGroup: muscleGroups.getMuscleGroups()) {
			subMenu.add(muscleGroup).setOnMenuItemClickListener(this);;
		}		
	}

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		ExerciseListActivity parent = (ExerciseListActivity) Util.getActivityFromContext(mContext);
		parent.selectFilter(item.getTitle());
		return true;
	}
	
	public void setMuscleGroups(MuscleGroups muscleGroups) {
		this.muscleGroups = muscleGroups;	
	}
}