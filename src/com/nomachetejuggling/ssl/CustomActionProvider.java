package com.nomachetejuggling.ssl;

import com.nomachetejuggling.ssl.model.MuscleGroups;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.util.Log;
import android.view.ActionProvider;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.SubMenu;
import android.view.View;
import android.widget.Toast;

public class CustomActionProvider extends ActionProvider implements OnMenuItemClickListener {

	static final int LIST_LENGTH = 3;

	Context mContext;

	private MuscleGroups muscleGroups;

	public CustomActionProvider(Context context) {
		super(context);
		mContext = context;
	}

	@Override
	public View onCreateActionView() {		
		return null;
	}

	@Override
	public boolean onPerformDefaultAction() {
		return super.onPerformDefaultAction();
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
		ExerciseListActivity parent = (ExerciseListActivity) getActivity();
		parent.selectFilter(item.getTitle());
		return true;
	}
	
	private Activity getActivity() {
        // Gross way of unwrapping the Activity so we can get the FragmentManager
        Context context = mContext;
        while (context instanceof ContextWrapper && !(context instanceof Activity)) {
            context = ((ContextWrapper) context).getBaseContext();
        }
        if (!(context instanceof Activity)) {
            throw new IllegalStateException("The MediaRouteActionProvider's Context " +
                    "is not an Activity.");
        }

        return (Activity) context;
    }

	public void setMuscleGroups(MuscleGroups muscleGroups) {
		this.muscleGroups = muscleGroups;
		
	}
}