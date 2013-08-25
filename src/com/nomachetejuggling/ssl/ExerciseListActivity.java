package com.nomachetejuggling.ssl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.nomachetejuggling.ssl.model.Exercise;
import com.nomachetejuggling.ssl.model.MuscleGroups;

// -- Release 1.1
//TODO: muscles are a json file too, current becomes defaults.  no ui to edit, but can edit by hand

// -- Future Release
//FUTURE: Edit name of exercise
//FUTURE: check mark on exercises done today (in list view) (this is tricky because that file is slow to load)
//FUTURE: Load exercise list async
//FUTURE: replace dialog progress bar with simple progress bar in log area
//FUTURE: Metric/Imperial setting (this should change increment from 5 to 1)
//FUTURE: "workout summary" feature with all of current day's stuff.  datepicker for other dates.
//FUTURE: (maybe) full historical record for an exercise to see improvement.  should this be part of larger suite?
//FUTURE: Filter should be a navigation dropdown, not a button

public class ExerciseListActivity extends ListActivity {
	private ExerciseAdapter exerciseAdapter;
	private ArrayList<Exercise> allExercises;
	private ArrayList<Exercise> displayExercises;
	private boolean dirty;

	private MuscleGroups muscleGroups;
	private String filter;

	private static final int ADD_EXERCISE_REQUEST = 0;
	private static final int EDIT_EXERCISE_REQUEST = 1;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_exercise_list);
		
		allExercises = new ArrayList<Exercise>();
		displayExercises = new ArrayList<Exercise>();
		dirty = false;
		
		exerciseAdapter = new ExerciseAdapter(this, R.layout.list_exercises, R.id.line1, displayExercises);
		setListAdapter(exerciseAdapter);
		
		registerForContextMenu(getListView());

		if(savedInstanceState != null) {
			muscleGroups = (MuscleGroups) savedInstanceState.getSerializable("muscleGroups");
			filter = savedInstanceState.getString("filter");
		} else {
			muscleGroups = Util.loadMuscleGroups(getResources());
			filter = "All";
		}	
	}	

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		
		savedInstanceState.putSerializable("muscleGroups", this.muscleGroups);
		savedInstanceState.putString("filter", filter);
	}

	@Override
	protected void onStop() {
		super.onStop();
		saveExercises();
	}

	@Override
	protected void onStart() {
		super.onStart();
		loadExercises();
		displayExercises();
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		Exercise selectedExercise = ((ExerciseAdapter)this.getListAdapter()).getItem(info.position);

	    menu.setHeaderTitle(selectedExercise.name);
		
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.exercise_list_context, menu);
		
		MenuItem favorite = (MenuItem) menu.findItem(R.id.favoriteContextMenu);
		MenuItem unfavorite = (MenuItem) menu.findItem(R.id.unfavoriteContextMenu);
		
		favorite.setVisible(!selectedExercise.favorite);
		unfavorite.setVisible(selectedExercise.favorite);
		
		super.onCreateContextMenu(menu, v, menuInfo); 
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.exercise_list, menu);

		MenuItem item = menu.findItem(R.id.pick_action_provider);
		ExerciseListFilterActionProvider provider = (ExerciseListFilterActionProvider) item.getActionProvider();
		provider.setMuscleGroups(this.muscleGroups);

		return true;
	}
	
	

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.add_exercise:
			Intent intent = new Intent(this, AddActivity.class);
			intent.putExtra("muscles", this.muscleGroups.getMuscles().toArray(new String[] {}));
			startActivityForResult(intent, ADD_EXERCISE_REQUEST);
			return true;
		case R.id.action_settings:
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		}
		return false;
	}
	
	
	@Override
    protected void onListItemClick(ListView l, View v, int position, long id) { 
		Exercise exercise = allExercises.get(position);
		logExercise(exercise);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
	    // Here's how you can get the correct item in onContextItemSelected()
	    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
	    Exercise selectedExercise = ((ExerciseAdapter) getListAdapter()).getItem(info.position);
	    
	    switch(item.getItemId()) {
	    	case R.id.logContextMenu :
	    		logExercise(selectedExercise);
	    		return true;
	    	case R.id.deleteContextMenu :
	    		deleteExercise(selectedExercise);
	    		return true;
	    	case R.id.favoriteContextMenu :
	    		markFavorite(selectedExercise, true);
	    		this.exerciseAdapter.notifyDataSetChanged();
	    		return true;
	    	case R.id.unfavoriteContextMenu :
	    		markFavorite(selectedExercise, false);
	    		this.exerciseAdapter.notifyDataSetChanged();
	    		return true;
	    	case R.id.editContextMenu :
	    		Intent intent = new Intent(this, AddActivity.class);
				intent.putExtra("muscles", this.muscleGroups.getMuscles().toArray(new String[] {}));
				intent.putExtra("exercise", selectedExercise);
				startActivityForResult(intent, EDIT_EXERCISE_REQUEST);
				return true;
	    }
	    return false;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		if (requestCode == ADD_EXERCISE_REQUEST) {
			if (resultCode == RESULT_OK) {
				Bundle extras = intent.getExtras();
				if (extras != null) {
					Exercise newExercise = (Exercise) extras.getSerializable("newExercise");
					addExercise(newExercise);
				}
			}
		} else if(requestCode == EDIT_EXERCISE_REQUEST) {
			if(resultCode == RESULT_OK) {
				Bundle extras = intent.getExtras();
				if (extras != null) {
					Exercise editedExercise = (Exercise) extras.getSerializable("newExercise");
					modifyExercise(editedExercise);
				}
			}
		}
	}

	private void modifyExercise(Exercise editedExercise) {
		for(Exercise exercise: allExercises) {
			if(exercise.name.equals(editedExercise.name)) {
				//Modify the existing one, because there might be handles to it elsewhere
				exercise.copyFrom(editedExercise);
				this.dirty = true;
				saveExercises();
				this.displayExercises();
			}
		}
	}
	
	public void selectFilter(CharSequence title) {
		this.filter = title.toString();
		displayExercises();
	}
	

	protected void markFavorite(Exercise exercise, boolean favorite) {
		if(favorite != exercise.favorite) {
			exercise.favorite = favorite;
			dirty = true;
			saveExercises();
		}
	}
	
	private void addExercise(Exercise newExercise) {
		allExercises.add(newExercise);
		displayExercises();
		dirty = true;
		saveExercises();
	}
	
	private void logExercise(Exercise exercise) {
		Intent intent = new Intent(ExerciseListActivity.this, LogActivity.class);
		intent.putExtra("exercise", exercise);
		startActivity(intent);
	}
	
	private void deleteExercise(final Exercise exercise) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(Html.fromHtml("Are you sure you want to delete '"+StringEscapeUtils.escapeHtml3(exercise.name)+"'? <br/><small>(Note: this will not delete any logs)</small>"))
		       .setCancelable(false)
		       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   allExercises.remove(exercise);
		        	   dirty = true;
		       			displayExercises();
		    		saveExercises();
		           }
		       })
		       .setNegativeButton("No", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                dialog.cancel();
		           }
		       });
		AlertDialog alert = builder.create();
		alert.show();
	}	

	private void saveExercises() {
		if (dirty == false)
			return;

		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		String json = gson.toJson(allExercises);
		File file = Util.getExerciseFile(this.getApplicationContext());
		Log.d("IO", "Writing to " + file.getAbsolutePath() + "\n" + json);
		try {
			FileUtils.write(file, json, "UTF-8");
		} catch (IOException e) {
			Toast.makeText(getApplicationContext(), getString(R.string.error_cannot_save_exercises), Toast.LENGTH_SHORT).show();
		}
		dirty = false;
	}

	private void displayExercises() {
		ActionBar ab = getActionBar();
		
		displayExercises.clear();
		if (this.filter == null || this.filter.equals("All")) {
			displayExercises.addAll(allExercises);
			ab.setSubtitle(null);
		} else if(filter.equals("Favorites")) {
			for (Exercise exercise : allExercises) {
				if (exercise.favorite) displayExercises.add(exercise);
			}
			ab.setSubtitle("Favorites");
		} else {
			for (Exercise exercise : allExercises) {
				if (muscleGroups.contains(filter, exercise)) {
					displayExercises.add(exercise);
				}
			}
			ab.setSubtitle(filter);
		}
		this.exerciseAdapter.notifyDataSetChanged();

	}

	private void loadExercises() {
		File file = Util.getExerciseFile(this.getApplicationContext());
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		Type collectionType = new TypeToken<Collection<Exercise>>() {}.getType();
		List<Exercise> exercisesRead = new ArrayList<Exercise>();
		String json;
		try {
			json = FileUtils.readFileToString(file, "UTF-8");
			Log.d("IO", "Start Reading from " + file.getAbsolutePath() + "\n" + json);

			exercisesRead = gson.fromJson(json, collectionType);
		} catch (IOException e) {
			InputStream raw = getResources().openRawResource(R.raw.exerciselist_default);
			exercisesRead = gson.fromJson(new InputStreamReader(raw), collectionType);
			this.dirty = true; //Save this on exit
		}
		
		Collections.sort(exercisesRead);
		allExercises.clear();
		allExercises.addAll(exercisesRead);
		
	}

	private static class ExerciseAdapter extends ArrayAdapter<Exercise> {
		Context mContext;
		
		public ExerciseAdapter(Context context, int layout, int resId, List<Exercise> items) {
			super(context, layout, resId, items);
			mContext = context;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				row = LayoutInflater.from(getContext()).inflate(R.layout.list_exercises, parent, false);
			}
			final Exercise item = getItem(position);
			TextView text = (TextView) row.findViewById(R.id.line1);
			text.setText(item.name);
			
			OnClickListener toggleFavoriteListener = new OnClickListener(){
		        public void onClick(View v) {
		        	ExerciseListActivity activity = (ExerciseListActivity)Util.getActivityFromContext(mContext);
		        	CheckBox checkBox = (CheckBox) v;
		        	activity.markFavorite(item, checkBox.isChecked());
		        }
			};
			
			CheckBox favoriteCheckBox = (CheckBox) row.findViewById(R.id.favoriteCheckbox);
			favoriteCheckBox.setChecked(item.favorite);
			favoriteCheckBox.setOnClickListener(toggleFavoriteListener);

			TextView muscleListView = (TextView) row.findViewById(R.id.muscleList);
			if (item.muscles != null && item.muscles.length > 0) {
				muscleListView.setText(Util.join(item.muscles, ", ", ""));
				muscleListView.setVisibility(View.VISIBLE);
			} else {
				muscleListView.setText("");
				muscleListView.setVisibility(View.GONE);
			}
			return row;
		}
	}

}
