package com.nomachetejuggling.ssl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.nomachetejuggling.ssl.model.Exercise;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

//TODO: Long press to Edit/Delete exercise
//TODO: Actual exercise logging
//TODO: Favorite (part of long press menu, later a separate star)

public class ExerciseList extends Activity {
	ExerciseAdapter exerciseAdapter;
	ArrayList<Exercise> exercises;

	static final int ADD_EXERCISE_REQUEST = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_exercise_list);

		ListView listView = (ListView) this.findViewById(R.id.exerciseList);
		exercises = new ArrayList<Exercise>();

		listView.setEmptyView(findViewById(android.R.id.empty));

		exerciseAdapter = new ExerciseAdapter(this, R.layout.list_exercises, R.id.line1, exercises);
		listView.setAdapter(exerciseAdapter);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.exercise_list, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.add_exercise:
			startActivityForResult(new Intent(this, AddActivity.class),
					ADD_EXERCISE_REQUEST);
			return true;
		case R.id.action_settings:
			return true;
		}
		return false;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		if (requestCode == ADD_EXERCISE_REQUEST) {
			if (resultCode == RESULT_OK) {
				Bundle extras = intent.getExtras();
				if (extras != null) {
					Exercise newExercise = (Exercise) extras
							.getSerializable("newExercise");
					Log.i("newExercise", newExercise.toString());

					exercises.add(newExercise);
					exerciseAdapter.notifyDataSetChanged();
					saveExercises();
				}
			}
		}
	}

	@Override
	protected void onStop() {
		super.onStop(); // Always call the superclass method first
		saveExercises();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onStart() {
		super.onStart(); // Always call the superclass method first
		loadExercises();
		exerciseAdapter.notifyDataSetChanged();
	}

	// TODO: Deal with the card not being mounted
	private void saveExercises() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		String json = gson.toJson(exercises);
		File file = Util.getExerciseFile(this.getApplicationContext());
		Log.i("IO", "Writing to " + file.getAbsolutePath() + "\n" + json);
		try {
			PrintWriter printWriter = new PrintWriter(file);
			printWriter.write(json);
			printWriter.close();
		} catch (FileNotFoundException e) {
			Log.e("file", "blah", e);
		}
	}

	private void loadExercises() {
		File file = Util.getExerciseFile(this.getApplicationContext());

		String json;
		try {
			json = FileUtils.readFileToString(file, "UTF-8");
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			Log.i("IO", "Start Reading from " + file.getAbsolutePath() + "\n" + json);

			Type collectionType = new TypeToken<Collection<Exercise>>() {
			}.getType();
			List<Exercise> exercisesRead = gson.fromJson(json, collectionType);
			Collections.sort(exercisesRead);
			exercises.clear();
			exercises.addAll(exercisesRead);
		} catch (IOException e) {
			Log.e("file", "problem reading file", e);
		}
	}

	private static class ExerciseAdapter extends ArrayAdapter<Exercise> {
		public ExerciseAdapter(Context context, int layout, int resId,
				List<Exercise> items) {
			super(context, layout, resId, items);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				row = LayoutInflater.from(getContext()).inflate(
						R.layout.list_exercises, parent, false);
			}
			Exercise item = getItem(position);
			TextView text = (TextView) row.findViewById(R.id.line1);
			text.setText(item.getName());
			return row;
		}
	}

}
