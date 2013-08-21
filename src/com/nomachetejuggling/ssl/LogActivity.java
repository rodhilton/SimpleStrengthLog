package com.nomachetejuggling.ssl;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.joda.time.LocalDate;
import org.joda.time.Period;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import com.nomachetejuggling.ssl.model.Exercise;
import com.nomachetejuggling.ssl.model.LogEntry;

import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.PowerManager;
import android.os.Vibrator;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.app.NavUtils;


//TODO: UI fix
//TODO: load previous logs from file
//TODO: prepopulate spinners to last value
//TODO: handle pause/resume
//TODO: undo button to undo previously logged entry
//TODO: still loading kinda slow and sluggish.
//TODO: "current logs"->"Today:", "previous logs"->"yesterday"|"2 days ago"|"last week"|"last month" etc, use joda
//TODO: remove  android:configChanges

public class LogActivity extends Activity {	
	
	private List<LogEntry> currentLogs;
	private Exercise currentExercise;
	private boolean resting;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_log);
		// Show the Up button in the action bar.
		setupActionBar();
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		currentLogs = new ArrayList<LogEntry>();
		resting=false;
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
		    ab.setSubtitle(currentExercise.name); 
		  }
		
		File dir = Util.getLogStorageDir(this.getApplicationContext());
		File file = new File(dir, new LocalDate().toString("yyyy-MM-dd")+".json");
		new PostTask(this).execute(file);
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
		
		this.persistCurrentLogs();		
	}
	
	public void logSetAndRest(View view) {
		NumberPicker weightPicker = (NumberPicker) findViewById(R.id.weightPicker);
		NumberPicker repsPicker = (NumberPicker) findViewById(R.id.repsPicker);	
		
		LogEntry log = new LogEntry();
		log.exercise = currentExercise;
		log.weight = ((weightPicker.getValue()+1)*5);
		log.reps = (repsPicker.getValue()+1);
		this.currentLogs.add(log);
		
		this.persistCurrentLogs();
		
		final ProgressBar timerProgressBar = (ProgressBar) findViewById(R.id.restTimerBar);
		timerProgressBar.setMax(currentExercise.restTime);
		timerProgressBar.setProgress(0);
		
		final Button saveButton = (Button) findViewById(R.id.saveButton);
		final Button saveAndRestButton = (Button) findViewById(R.id.saveAndRestButton);
		final Button undoButton = (Button) findViewById(R.id.undoButton);
		
		saveButton.setEnabled(false);
		saveAndRestButton.setText(currentExercise.restTime+"s");
		saveAndRestButton.setEnabled(false);
		undoButton.setEnabled(false);
		timerProgressBar.setVisibility(View.VISIBLE);
		this.resting=true;
		
		
		new CountDownTimer(currentExercise.restTime*1000, 1000) {

		     public void onTick(long millisUntilFinished) {
		    	 if(!resting) {
		    		 this.cancel();
		    		 this.onFinish();
		    	 }
		    	 
		    	 int secsLeft = (int) (millisUntilFinished/1000);
		    	 timerProgressBar.setProgress(currentExercise.restTime-secsLeft);
		    	 Period period = new Period(millisUntilFinished);
		    	 
		    	 saveAndRestButton.setText(String.format("%02d:%02d", period.getMinutes(), period.getSeconds()));
		     }

		     public void onFinish() {
		    	 stopResting();
		     }
		  }.start();
		
	}
	
	private void stopResting() {
		final ProgressBar timerProgressBar = (ProgressBar) findViewById(R.id.restTimerBar);
		final Button saveButton = (Button) findViewById(R.id.saveButton);
		final Button saveAndRestButton = (Button) findViewById(R.id.saveAndRestButton);
		final Button undoButton = (Button) findViewById(R.id.undoButton);
		
		timerProgressBar.setVisibility(View.INVISIBLE);
		saveAndRestButton.setText(getString(R.string.saveAndRestButtonLabel));
		
   	 	saveButton.setEnabled(true);
		saveAndRestButton.setEnabled(true);
		undoButton.setEnabled(true);
		this.resting=false;
		
		Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		// Vibrate for 500 milliseconds
		v.vibrate(500);
		
		MediaPlayer mPlayer = MediaPlayer.create(this.getApplicationContext(), R.raw.fight);
//		mPlayer.setVolume(f, .4f);
		mPlayer.start();
	}
	
	@Override
	public void onBackPressed() {
		//This isnt working for some reason... press back and it kills the timer and stuff but the countdown keeps ticking
//		if(resting) {
//			resting=false;
//		} else {
//			super.onBackPressed();
//		}
		super.onBackPressed();
	}
	
	public void loadCurrentLogs(List<LogEntry> readLogs) {
		currentLogs.clear();
		currentLogs.addAll(readLogs);
		this.showCurrentLogs();
	}
	
	public void persistCurrentLogs() {
		File dir = Util.getLogStorageDir(this.getApplicationContext());
		File file = new File(dir, new LocalDate().toString("yyyy-MM-dd")+".json");
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		String json = gson.toJson(currentLogs);
		Log.d("IO", "Writing to " + file.getAbsolutePath() + "\n" + json);
		try {
			FileUtils.write(file, json, "UTF-8");
		} catch(IOException e) {
			Toast.makeText(
		
				getApplicationContext(), 
				getString(R.string.error_cannot_save_log), 
				Toast.LENGTH_SHORT).show();
		}
		
		showCurrentLogs();
	}

	private void showCurrentLogs() {
		StringBuilder sb = new StringBuilder();
		for(int i=0;i<this.currentLogs.size();i++) {
			LogEntry logEntry = this.currentLogs.get(i);
			if(logEntry.exercise.equals(currentExercise)) {
				sb.append(logEntry.toString());
				//if(i!=this.currentLogs.size()-1) {
					sb.append("\n");
				//}
			}
		}
		
		TextView currentLogs = (TextView)findViewById(R.id.currentLogsView);
		currentLogs.setText(sb);
		
		ScrollView scrollView = (ScrollView)findViewById(R.id.currentLogsScroll);
		scrollView.fullScroll(View.FOCUS_DOWN);
	}
	
	private static class PostTask extends AsyncTask<File, Void, List<LogEntry>> {

    	private static ProgressDialog dialog;
		private LogActivity act;


		public PostTask(LogActivity act) {
			 this.act = act;
		}

		@Override
    	protected void onPreExecute() {
    		super.onPreExecute();
    		dialog = ProgressDialog.show(act, "hrf", "brf");
    	}
    	
		@Override
		protected List<LogEntry> doInBackground(File... params) {
			File file=params[0];
			
			try {
				String json = FileUtils.readFileToString(file, "UTF-8");
				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				Log.d("IO", "Start Reading from " + file.getAbsolutePath() + "\n" + json);

				Type collectionType = new TypeToken<Collection<LogEntry>>() {}.getType();
				List<LogEntry> currentLogsRead = gson.fromJson(json, collectionType);
				return currentLogsRead;			
			} catch (IOException e) {
				//Ignore, a missing file is either an unmounted SD Card (unrecoverable) or a first-time run.
				return new ArrayList<LogEntry>();
			}
		
		}    	
    	
    	@Override
    	protected void onPostExecute(List<LogEntry> result) {
    		super.onPostExecute(result);
    		act.loadCurrentLogs(result);
    		dialog.hide();
    	}
		
    }

}
