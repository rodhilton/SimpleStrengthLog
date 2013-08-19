package com.nomachetejuggling.ssl;

import com.nomachetejuggling.ssl.model.Exercise;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.support.v4.app.NavUtils;

public class LogActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_log);
		// Show the Up button in the action bar.
		setupActionBar();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		Exercise exercise = (Exercise)getIntent().getExtras().getSerializable("exercise");
		Log.i("LogActivity", "Exercise: "+exercise.toString());
		
		TextView titleView = (TextView)findViewById(R.id.exerciseName);
		titleView.setText(exercise.getName());
		
		int numValues = 100;
		int PICKER_RANGE = 5;
		
		NumberPicker weightPicker = (NumberPicker) findViewById(R.id.weightPicker);
		
		String[] displayedValues  = new String[numValues];
		//Populate the array
		for(int i=0; i<numValues; i++)
		    displayedValues[i] = String.valueOf(PICKER_RANGE * (i+1));
		
		weightPicker.setMinValue(0); 
		weightPicker.setMaxValue(displayedValues.length-1);
		weightPicker.setDisplayedValues(displayedValues);
		weightPicker.setWrapSelectorWheel(false);
	}

	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar() {

		getActionBar().setDisplayHomeAsUpEnabled(true);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.log, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
