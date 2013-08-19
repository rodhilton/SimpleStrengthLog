package com.nomachetejuggling.ssl;

import java.util.ArrayList;
import java.util.List;

import com.nomachetejuggling.ssl.model.Exercise;
import com.nomachetejuggling.ssl.model.LogEntry;

import android.os.Build;
import android.os.Bundle;
import android.app.ActionBar;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.TextView;
import android.support.v4.app.NavUtils;

public class LogActivity extends Activity {	
	
	private List<LogEntry> currentLogs;
	private Exercise currentExercise;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_log);
		// Show the Up button in the action bar.
		setupActionBar();
		currentLogs = new ArrayList<LogEntry>();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		currentExercise = (Exercise)getIntent().getExtras().getSerializable("exercise");
		Log.i("LogActivity", "Exercise: "+currentExercise.toString());
		
		int numValues = 100;
		int PICKER_RANGE = 5;
		
		NumberPicker weightPicker = (NumberPicker) findViewById(R.id.weightPicker);
		
		String[] displayedValues  = new String[numValues];
		//Populate the array
		for(int i=0; i<numValues; i++)
		    displayedValues[i] = String.valueOf(PICKER_RANGE * (i+1))+" lbs";
		
		weightPicker.setMinValue(0); 
		weightPicker.setMaxValue(displayedValues.length-1);
		weightPicker.setDisplayedValues(displayedValues);
		weightPicker.setWrapSelectorWheel(false);
		
		int maxReps=20;
		NumberPicker repsPicker = (NumberPicker) findViewById(R.id.repsPicker);
		String[] displayedReps  = new String[maxReps];
		//Populate the array
		for(int i=0; i<maxReps; i++)
			displayedReps[i] = (i+1)+" reps";
		
		repsPicker.setMinValue(0); 
		repsPicker.setMaxValue(displayedReps.length-1);
		repsPicker.setDisplayedValues(displayedReps);
		repsPicker.setWrapSelectorWheel(false);
		
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
		    ActionBar ab = getActionBar();
		    ab.setTitle("Log");
		    ab.setSubtitle(currentExercise.getName()); 
		  }
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
	
	public void logSet(View view) {
		NumberPicker weightPicker = (NumberPicker) findViewById(R.id.weightPicker);
		NumberPicker repsPicker = (NumberPicker) findViewById(R.id.repsPicker);	
		
		LogEntry log = new LogEntry();
		log.exercise = currentExercise;
		log.weight = ((weightPicker.getValue()+1)*5);
		log.reps = (repsPicker.getValue()+1);
		this.currentLogs.add(log);
		
		//String logEntry = ((weightPicker.getValue()+1)*5)+"x"+(repsPicker.getValue()+1);
		StringBuilder sb = new StringBuilder();
		for(int i=0;i<this.currentLogs.size();i++) {
			LogEntry logEntry = this.currentLogs.get(i);
			sb.append(logEntry.toString());
			//if(i!=this.currentLogs.size()-1) {
				sb.append("\n");
			//}
		}
		
		TextView currentLogs = (TextView)findViewById(R.id.currentLogsView);
		currentLogs.setText(sb);
		
		ScrollView scrollView = (ScrollView)findViewById(R.id.currentLogsScroll);
		scrollView.fullScroll(View.FOCUS_DOWN);
	}

}
