package com.nomachetejuggling.ssl;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ListView;

public class ExerciseList extends Activity {
	private ListView listView;
	
	static final int ADD_EXERCISE_REQUEST = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_exercise_list);

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
             Intent data) {
         if (requestCode == ADD_EXERCISE_REQUEST) {
             if (resultCode == RESULT_OK) {
                 Log.i("stuff", "junk");
             }
         }
     }

}
