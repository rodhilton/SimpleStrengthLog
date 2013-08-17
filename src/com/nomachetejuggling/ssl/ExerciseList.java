package com.nomachetejuggling.ssl;

import java.util.ArrayList;

import com.nomachetejuggling.ssl.model.Exercise;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

public class ExerciseList extends Activity {
	private ListView listView;
	ArrayAdapter<String> aa;
	ArrayList<String> exercises;
	
	static final int ADD_EXERCISE_REQUEST = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_exercise_list);

		ListView listView = (ListView) this.findViewById(R.id.exerciseList);
		exercises = new ArrayList<String>();
		aa = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,  
				exercises);
        listView.setAdapter(aa);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.exercise_list, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case R.id.add_exercise: 
				startActivityForResult(new Intent(this, AddActivity.class), ADD_EXERCISE_REQUEST);
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
                 if(extras != null);
                 Exercise newExercise = (Exercise)extras.getSerializable("newExercise");
                 Log.i("newExercise", newExercise.toString());
                 
                 exercises.add(newExercise.getName());
                 this.aa.notifyDataSetChanged();
             }
         }
     }

}
