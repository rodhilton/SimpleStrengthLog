package com.nomachetejuggling.ssl;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import com.nomachetejuggling.ssl.model.Exercise;
import com.nomachetejuggling.ssl.model.LogEntry;

import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.NumberPicker.OnValueChangeListener;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.app.NavUtils;

//TODO: clean this code up, lots of duplication
//FIXME: currently if you change the scrollers, then rotate, it loses your value and restores based on log.  need to have a listener to mark scrollers dirty and save/restore from it if there

public class LogActivity extends Activity {

	private List<LogEntry> currentLogs = null;
	private List<LogEntry> previousLogs = null;
	private Exercise currentExercise;
	private CountDownTimer restTimer;
	private int restSecsLeft = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_log);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        boolean keepScreenOnSetting = settings.getBoolean("screenOn", false);
        
        if(keepScreenOnSetting) {
        	getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        	Log.i("SETTINGS", "Keeping screen on, per request");
        } else {
        	Log.i("SETTINGS", "Not keeping screen on");
        }
        
		currentLogs = new ArrayList<LogEntry>();
		previousLogs = new ArrayList<LogEntry>();
		
		currentExercise = (Exercise) getIntent().getExtras().getSerializable("exercise");

		int numValues = 100;
		int PICKER_RANGE = 5;

		NumberPicker weightPicker = (NumberPicker) findViewById(R.id.weightPicker);
		weightPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

		String[] displayedValues = new String[numValues];
		// Populate the array
		for (int i = 0; i < numValues; i++) displayedValues[i] = String.valueOf(PICKER_RANGE * (i + 1)) + " lbs";

		weightPicker.setMinValue(0);
		weightPicker.setMaxValue(displayedValues.length - 1);
		weightPicker.setDisplayedValues(displayedValues);
		weightPicker.setWrapSelectorWheel(false);

		int maxReps = 20;
		NumberPicker repsPicker = (NumberPicker) findViewById(R.id.repsPicker);
		repsPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
		String[] displayedReps = new String[maxReps];
		// Populate the array
		for (int i = 0; i < maxReps; i++)
			displayedReps[i] = (i + 1) + " reps";

		repsPicker.setMinValue(0);
		repsPicker.setMaxValue(displayedReps.length - 1);
		repsPicker.setDisplayedValues(displayedReps);
		repsPicker.setWrapSelectorWheel(false);

		OnValueChangeListener onValueChangeListener = new OnValueChangeListener() {

			@Override
			public void onValueChange(NumberPicker arg0, int arg1, int arg2) {
				showCurrentLogs();
			}

		};
		weightPicker.setOnValueChangedListener(onValueChangeListener);
		repsPicker.setOnValueChangedListener(onValueChangeListener);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			ActionBar ab = getActionBar();
			ab.setTitle(currentExercise.name);
			if(currentExercise.muscles != null && currentExercise.muscles.length > 0) {
				ab.setSubtitle(Util.join(currentExercise.muscles, ", ", null));
			}
		}
		
		File dir = Util.getLogStorageDir(getApplicationContext());
		
		new LoadLogsTask(this).execute(currentExercise, dir);
	}
//
//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		return true;
//	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			if(this.restTimer != null) {
				this.restTimer.cancel();
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void clickLogSet(View view) {
		LogEntry log = currentEntry();
		this.currentLogs.add(log);
		
		Button undoButton = (Button) findViewById(R.id.undoButton);
		undoButton.setEnabled(true);

		this.persistCurrentLogs();
	}
	
	public void clickLogSetAndRest(View view) {
		clickLogSet(view);		
		restFor(currentExercise.restTime);
	}
	
	public void clickUndo(View view) {
		Button undoButton = (Button) findViewById(R.id.undoButton);
		undoButton.setEnabled(false);
		
		this.currentLogs.remove(this.currentLogs.size()-1);
		this.persistCurrentLogs();
	}
	
	@Override
	public void onBackPressed() {
		if(this.restTimer != null) {
			this.restTimer.cancel();
			this.restTimer = null;
			stopResting(true);
		} else {
			super.onBackPressed();
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);

		NumberPicker weightPicker = (NumberPicker) findViewById(R.id.weightPicker);
		NumberPicker repsPicker = (NumberPicker) findViewById(R.id.repsPicker);

		savedInstanceState.putInt("WeightPickerPosition",
				weightPicker.getValue());
		savedInstanceState.putInt("RepsPickerPosition", repsPicker.getValue());
		
		if(this.restTimer != null) {
			restTimer.cancel();
			savedInstanceState.putInt("RestTimeRemaining", this.restSecsLeft);
		}
		
		Button undoButton = (Button) findViewById(R.id.undoButton);
		savedInstanceState.putBoolean("UndoEnabled", undoButton.isEnabled());
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		// Restore UI state from the savedInstanceState.
		// This bundle has also been passed to onCreate.
		int weightPickerPosition = savedInstanceState.getInt("WeightPickerPosition");
		int repsPickerPosition = savedInstanceState.getInt("RepsPickerPosition");

		NumberPicker weightPicker = (NumberPicker) findViewById(R.id.weightPicker);
		NumberPicker repsPicker = (NumberPicker) findViewById(R.id.repsPicker);

		weightPicker.setValue(weightPickerPosition);
		repsPicker.setValue(repsPickerPosition);
		
		if(savedInstanceState.containsKey("RestTimeRemaining")) {
			restFor(savedInstanceState.getInt("RestTimeRemaining"));
		}
		
		Button undoButton = (Button) findViewById(R.id.undoButton);
		undoButton.setEnabled(savedInstanceState.getBoolean("UndoEnabled"));
	}

	private void loadCurrentLogs(LogSet logSet) {
		currentLogs.clear();
		currentLogs.addAll(logSet.currentLogs);

		previousLogs.clear();
		previousLogs.addAll(logSet.previousLogs);

		if(logSet.previousDate != null) {
			TextView textView = (TextView) findViewById(R.id.previousLogLabel);
			String relative = Util.getRelativeDate(new LocalDate(),
					logSet.previousDate);
			textView.setText(relative + ":");
		}

		LogEntry lastEntry = null;
		if(currentLogs.size() > 0) {
			for(int i=currentLogs.size()-1;i>=0 && lastEntry == null;i--) {
				LogEntry entry = currentLogs.get(i);
				if(entry.exercise.equals(currentExercise.name)) {
					lastEntry = entry;
				}
			}
		} 
		
		if( lastEntry == null && previousLogs.size() > 0){
			for(int i=previousLogs.size()-1;i>=0 && lastEntry == null;i--) {
				LogEntry entry = previousLogs.get(i);
				if(entry.exercise.equals(currentExercise.name)) {
					lastEntry = entry;
				}
			}
		}
		
		if(lastEntry != null) {
			int weightPosition = (lastEntry.weight / 5) - 1;
			int repsPosition = (lastEntry.reps) - 1;
	
			NumberPicker weightPicker = (NumberPicker) findViewById(R.id.weightPicker);
			NumberPicker repsPicker = (NumberPicker) findViewById(R.id.repsPicker);
	
			weightPicker.setValue(weightPosition);
			repsPicker.setValue(repsPosition);

		}
		this.showCurrentLogs();
	}

	private void persistCurrentLogs() {
		File dir = Util.getLogStorageDir(this.getApplicationContext());
		File file = new File(dir, new LocalDate().toString("yyyy-MM-dd") + ".json");
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		String json = gson.toJson(currentLogs);
		Log.d("IO", "Writing to " + file.getAbsolutePath() + "\n" + json);
		try {
			FileUtils.write(file, json, "UTF-8");
		} catch (IOException e) {
			Toast.makeText(getApplicationContext(),
					getString(R.string.error_cannot_save_log),
					Toast.LENGTH_SHORT).show();
		}

		showCurrentLogs();
	}

	private void showCurrentLogs() {
		List<LogEntry> logs = this.currentLogs;

		StringBuilder sb = builderForLogs(logs);

		LogEntry currentEntry = currentEntry();

		int currentTextColor=getResources().getColor(R.color.currentLogEntry);
		String hexColor = String.format("#%06X", (0xFFFFFF & currentTextColor));
		
		sb.append("<font color='"+hexColor+"'><i>" + formatEntry(currentEntry)+ "</i></font>");

		TextView currentLogs = (TextView) findViewById(R.id.currentLogsView);
		currentLogs.setText(Html.fromHtml(sb.toString()));

		final ScrollView scrollView = (ScrollView) findViewById(R.id.currentLogsScroll);
		scrollView.post(new Runnable() {
			@Override
			public void run() {
				scrollView.fullScroll(ScrollView.FOCUS_DOWN);
			}
		});

		StringBuilder sb2 = builderForLogs(this.previousLogs);
		TextView prevLogs = (TextView) findViewById(R.id.previousLogsView);
		prevLogs.setText(Html.fromHtml(sb2.toString()));

		final ScrollView scrollView2 = (ScrollView) findViewById(R.id.previousLogsScroll);
		scrollView2.post(new Runnable() {
			@Override
			public void run() {
				scrollView2.fullScroll(ScrollView.FOCUS_DOWN);
			}
		});

	}
	
	private String formatEntry(LogEntry entry) {
		int oneRM=entry.oneRepMax();
		return ""+entry.weight+"x"+entry.reps+" <small>(1RM="+oneRM+")</small><br/>";
	}

	private StringBuilder builderForLogs(List<LogEntry> logs) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < logs.size(); i++) {
			LogEntry logEntry = logs.get(i);
			if (logEntry.exercise.equals(currentExercise.name)) {
				sb.append(formatEntry(logEntry));
			}
		}
		return sb;
	}


	private static class LogSet {
		public List<LogEntry> currentLogs = new ArrayList<LogEntry>();
		public List<LogEntry> previousLogs = new ArrayList<LogEntry>();
		public LocalDate previousDate = null;
	}

	private static class LoadLogsTask extends AsyncTask<Object, Void, LogSet> {

		private static ProgressDialog dialog;
		private LogActivity act;

		public LoadLogsTask(LogActivity act) {
			this.act = act;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			dialog = ProgressDialog.show(act, "Please wait", "Loading logs");
		}

		@Override
		protected LogSet doInBackground(Object... params) {
			Exercise currentExercise = (Exercise)params[0];
			File dir = (File)params[1];
			
			String today = new LocalDate().toString("yyyy-MM-dd");

			try {
				File[] files = dir.listFiles();

				Arrays.sort(files, new Comparator<File>() {
					public int compare(File f1, File f2) {
						return f2.getName().compareTo(f1.getName());
					}
				});
				
				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				LogSet logSet = new LogSet();
				Type collectionType = new TypeToken<Collection<LogEntry>>() {}.getType();
				DateTimeFormatter pattern = DateTimeFormat.forPattern("yyyy-MM-dd");
				
				for (int i = 0; i < 50 && i < files.length && logSet.previousLogs.size() == 0; i++) {
					File file = files[i];
					String json = FileUtils.readFileToString(file, "UTF-8");
					List<LogEntry> logs = gson.fromJson(json,collectionType);
					if (file.getName().equals(today + ".json")) {
						logSet.currentLogs = logs;
					} else {
						for (LogEntry entry : logs) {
							if (entry.exercise.equals(currentExercise.name)) {
								logSet.previousLogs = logs;
								logSet.previousDate = LocalDate.parse(file.getName().substring(0,10), pattern);
							}
						}
					}
				}

				return logSet;
			} catch (IOException e) {
				Log.e("IO", "Problem", e);
				return new LogSet();
			}

		}

		@Override
		protected void onPostExecute(LogSet result) {
			super.onPostExecute(result);
			act.loadCurrentLogs(result);
			dialog.dismiss();
		}

	}


	private LogEntry currentEntry() {
		NumberPicker weightPicker = (NumberPicker) findViewById(R.id.weightPicker);
		NumberPicker repsPicker = (NumberPicker) findViewById(R.id.repsPicker);

		LogEntry log = new LogEntry();
		log.exercise = currentExercise.name;
		log.weight = ((weightPicker.getValue() + 1) * 5);
		log.reps = (repsPicker.getValue() + 1);
		log.time = new LocalTime().toString(ISODateTimeFormat.time());
		return log;
	}

	private void restFor(int restTime) {
		Button saveButton = (Button) findViewById(R.id.saveButton);
		Button saveAndRestButton = (Button) findViewById(R.id.saveAndRestButton);
		Button undoButton = (Button) findViewById(R.id.undoButton);

		saveButton.setEnabled(false);
		saveAndRestButton.setText(currentExercise.restTime + "s");
		saveAndRestButton.setEnabled(false);
		undoButton.setEnabled(false);
		
		ProgressBar timerProgressBar = (ProgressBar) findViewById(R.id.restTimerBar);
		timerProgressBar.setMax(currentExercise.restTime);
		timerProgressBar.setProgress(0);	
		timerProgressBar.setVisibility(View.VISIBLE);
		this.restTimer = createRestTimer(this, restTime);
		this.restTimer.start();
	}
	
	private CountDownTimer createRestTimer(final LogActivity activity, int secsRemaining) {
		return new CountDownTimer(secsRemaining * 1000, 1000) {

			public void onTick(long millisUntilFinished) {				
				activity.restSecsLeft = (int) (millisUntilFinished / 1000);
				ProgressBar timerProgressBar = (ProgressBar) findViewById(R.id.restTimerBar);
				timerProgressBar.setProgress(currentExercise.restTime - restSecsLeft);
				Period period = new Period(millisUntilFinished);

				Button saveAndRestButton = (Button) findViewById(R.id.saveAndRestButton);
				saveAndRestButton.setText(String.format("%02d:%02d", period.getMinutes(), period.getSeconds()));
			}

			public void onFinish() {
				stopResting(false);
			}
		};
	}

	private void stopResting(boolean cancelled) {
		final ProgressBar timerProgressBar = (ProgressBar) findViewById(R.id.restTimerBar);
		final Button saveButton = (Button) findViewById(R.id.saveButton);
		final Button saveAndRestButton = (Button) findViewById(R.id.saveAndRestButton);
		final Button undoButton = (Button) findViewById(R.id.undoButton);

		timerProgressBar.setVisibility(View.INVISIBLE);
		saveAndRestButton.setText(getString(R.string.saveAndRestButtonLabel));

		this.restSecsLeft = -1;
		
		saveButton.setEnabled(true);
		saveAndRestButton.setEnabled(true);
		undoButton.setEnabled(true);
		
		if(!cancelled) {	
			Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
			v.vibrate(new long[]{160, 228, 13,228, 13,228, 13,228, 13,228}, -1);
	
			MediaPlayer mPlayer = MediaPlayer.create(this.getApplicationContext(),
					R.raw.fight);
			mPlayer.setVolume(.6f, .6f);  //Let's not go crazy here, probably listening to music.
			mPlayer.start();
		}
	}

	
}
