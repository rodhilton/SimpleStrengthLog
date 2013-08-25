package com.nomachetejuggling.ssl;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.nomachetejuggling.ssl.model.Exercise;
import com.nomachetejuggling.ssl.model.MuscleGroups;

//TODO: Long press to Edit/Delete exercise
//TODO: Favorite (part of long press menu, later a separate star)
//TODO: load file in a thread, it can be pretty slow
//TODO: default file of exercises (must have favorite feature first)
//TODO: "workout summary" feature with all of current day's stuff.  datepicker for other dates.
//TODO: (maybe) full historical record for an exercise to see improvement.  should this be part of larger suite?
//TODO: setting for lbs/kg ? (should it change range?)
//TODO: replace dialog progress bar on load with one inside log area?
//TODO: Context menu on long press: ["Log" (default click), "Favorite/Unfavorite" (secondary), "Edit", "Delete"]

public class ExerciseListActivity extends Activity {
	ExerciseAdapter exerciseAdapter;
	ArrayList<Exercise> allExercises;
	ArrayList<Exercise> displayExercises;
	boolean dirty;

	MuscleGroups muscleGroups;
	private String filter;

	static final int ADD_EXERCISE_REQUEST = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_exercise_list);

		ListView listView = (ListView) this.findViewById(R.id.exerciseList);
		allExercises = new ArrayList<Exercise>();
		displayExercises = new ArrayList<Exercise>();

		listView.setEmptyView(findViewById(android.R.id.empty));

		exerciseAdapter = new ExerciseAdapter(this, R.layout.list_exercises, R.id.line1, displayExercises);
		listView.setAdapter(exerciseAdapter);

		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				Exercise exercise = allExercises.get(arg2);
				Intent intent = new Intent(ExerciseListActivity.this, LogActivity.class);
				intent.putExtra("exercise", exercise);
				startActivity(intent);
			}
		});
		dirty = false;

		this.muscleGroups = Util.loadMuscleGroups(getResources());
		if (savedInstanceState != null) {
			this.filter = savedInstanceState.getString("filter");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.exercise_list, menu);

		MenuItem item = menu.findItem(R.id.pick_action_provider);
		CustomActionProvider provider = (CustomActionProvider) item.getActionProvider();
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
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		if (requestCode == ADD_EXERCISE_REQUEST) {
			if (resultCode == RESULT_OK) {
				Bundle extras = intent.getExtras();
				if (extras != null) {
					Exercise newExercise = (Exercise) extras.getSerializable("newExercise");
					Log.i("newExercise", newExercise.toString());

					allExercises.add(newExercise);
					displayExercises();
					dirty = true;
					saveExercises();
				}
			}
		}
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putSerializable("muscleGroups", this.muscleGroups);
		savedInstanceState.putString("filter", filter);
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		this.muscleGroups = (MuscleGroups) savedInstanceState.getSerializable("muscleGroups");
	}

	@Override
	protected void onStop() {
		super.onStop();
		saveExercises();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onStart() {
		super.onStart();
		loadExercises();
		displayExercises();
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
		displayExercises.clear();
		if (this.filter == null || this.filter.equals("All")) {
			displayExercises.addAll(allExercises);
		} else {
			for (Exercise exercise : allExercises) {
				if (muscleGroups.contains(filter, exercise)) {
					displayExercises.add(exercise);
				}
			}
		}
		this.exerciseAdapter.notifyDataSetChanged();

	}

	private void loadExercises() {
		File file = Util.getExerciseFile(this.getApplicationContext());

		String json;
		try {
			json = FileUtils.readFileToString(file, "UTF-8");
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			Log.d("IO", "Start Reading from " + file.getAbsolutePath() + "\n" + json);

			Type collectionType = new TypeToken<Collection<Exercise>>() {
			}.getType();
			List<Exercise> exercisesRead = gson.fromJson(json, collectionType);
			Collections.sort(exercisesRead);
			allExercises.clear();
			allExercises.addAll(exercisesRead);
		} catch (IOException e) {
			// Ignore, a missing file is either an unmounted SD Card
			// (unrecoverable) or a first-time run.
		}
	}

	private static class ExerciseAdapter extends ArrayAdapter<Exercise> {
		public ExerciseAdapter(Context context, int layout, int resId, List<Exercise> items) {
			super(context, layout, resId, items);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				row = LayoutInflater.from(getContext()).inflate(R.layout.list_exercises, parent, false);
			}
			Exercise item = getItem(position);
			TextView text = (TextView) row.findViewById(R.id.line1);
			text.setText(item.name);

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

	public void selectFilter(CharSequence title) {
		this.filter = title.toString();
		displayExercises();
	}

}
