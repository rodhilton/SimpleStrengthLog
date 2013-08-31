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

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.text.Html;
import android.util.Log;
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.nomachetejuggling.ssl.model.Exercise;
import com.nomachetejuggling.ssl.model.LogEntry;

public class LogActivity extends Activity {

	private List<LogEntry> currentLogs = null;
	private List<LogEntry> previousLogs = null;
	private Exercise currentExercise;
	private CountDownTimer restTimer;
	private int restSecsLeft = -1;
	private int weightScale = 5;
	private boolean metric = false;
	private boolean spinnersAlreadySet = false;
	
	private static final int NUM_WEIGHT_VALUES = 200;
	private static final int NUM_REP_VALUES = 50;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_log);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        boolean keepScreenOnSetting = settings.getBoolean("screenOn", true);
        
        if(keepScreenOnSetting) {
        	getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        	Log.i("SETTINGS", "Keeping screen on, per request");
        } else {
        	Log.i("SETTINGS", "Not keeping screen on");
        }
        
        this.metric = settings.getBoolean("metricUnits", false);
        this.weightScale = metric ? 1 : 5;
        
		currentLogs = new ArrayList<LogEntry>();
		previousLogs = new ArrayList<LogEntry>();
		
		currentExercise = (Exercise) getIntent().getExtras().getSerializable("exercise");

		TextView weightPickerLabel = (TextView) findViewById(R.id.weightPickerLabel);
		weightPickerLabel.setText(getResources().getString( metric ? R.string.weightPickerLabelMetric : R.string.weightPickerLabelImperial));
		
		NumberPicker weightPicker = (NumberPicker) findViewById(R.id.weightPicker);
		weightPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

		String[] displayedValues = new String[NUM_WEIGHT_VALUES];
		//String weightUnits = metric ? "kg" : "lbs";
		// Populate the array
		for (int i = 0; i < NUM_WEIGHT_VALUES; i++) 
			displayedValues[i] = String.valueOf(weightScale * (i + 1));

		weightPicker.setMinValue(0);
		weightPicker.setMaxValue(displayedValues.length - 1);
		weightPicker.setDisplayedValues(displayedValues);
		weightPicker.setWrapSelectorWheel(false);

		NumberPicker repsPicker = (NumberPicker) findViewById(R.id.repsPicker);
		repsPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
		String[] displayedReps = new String[NUM_REP_VALUES];
		// Populate the array
		for (int i = 0; i < NUM_REP_VALUES; i++)
			displayedReps[i] = (i + 1)+"";

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
		
		findViewById(R.id.buttonBar).setVisibility(View.INVISIBLE);
		findViewById(R.id.currentLogsLayout).setVisibility(View.INVISIBLE);
		findViewById(R.id.previousLogsLayout).setVisibility(View.INVISIBLE);
		findViewById(R.id.logLoadProgress).setVisibility(View.VISIBLE);
		
		new LoadLogData(this, currentExercise, dir).execute();
	}

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

		savedInstanceState.putInt("WeightPickerPosition", weightPicker.getValue());
		savedInstanceState.putInt("RepsPickerPosition", repsPicker.getValue());
		
		if(this.restTimer != null) {
			restTimer.cancel();
			savedInstanceState.putInt("RestTimeRemaining", this.restSecsLeft);
		} else {
			savedInstanceState.putInt("RestTimeRemaining", -1);
		}
		
		Button undoButton = (Button) findViewById(R.id.undoButton);
		savedInstanceState.putBoolean("UndoEnabled", undoButton.isEnabled());
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		
		int weightPickerPosition = savedInstanceState.getInt("WeightPickerPosition");
		int repsPickerPosition = savedInstanceState.getInt("RepsPickerPosition");

		NumberPicker weightPicker = (NumberPicker) findViewById(R.id.weightPicker);
		NumberPicker repsPicker = (NumberPicker) findViewById(R.id.repsPicker);

		weightPicker.setValue(weightPickerPosition);
		repsPicker.setValue(repsPickerPosition);
		spinnersAlreadySet = true;
		
		if(savedInstanceState.containsKey("RestTimeRemaining")) {
			restFor(savedInstanceState.getInt("RestTimeRemaining"));
		}
		
		Button undoButton = (Button) findViewById(R.id.undoButton);
		undoButton.setEnabled(savedInstanceState.getBoolean("UndoEnabled"));
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

	public void loadCurrentLogs(LoadLogData.Output output) {
		if(output.currentLogs != null) {
			currentLogs.clear();
			currentLogs.addAll(output.currentLogs);
		}

		if(output.previousLogs != null) {
			previousLogs.clear();
			previousLogs.addAll(output.previousLogs);
		}

		if(output.previousDate != null) {
			TextView textView = (TextView) findViewById(R.id.previousLogLabel);
			String relative = Util.getRelativeDate(new LocalDate(), output.previousDate);
			textView.setText(relative + ":");
		}

		if(!spinnersAlreadySet) {
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
				int weightPosition = (lastEntry.weight / weightScale) - 1;
				int repsPosition = (lastEntry.reps) - 1;
		
				NumberPicker weightPicker = (NumberPicker) findViewById(R.id.weightPicker);
				NumberPicker repsPicker = (NumberPicker) findViewById(R.id.repsPicker);
		
				weightPicker.setValue(weightPosition);
				repsPicker.setValue(repsPosition);
	
			}
		}
		this.showCurrentLogs();
		findViewById(R.id.buttonBar).setVisibility(View.VISIBLE);
		findViewById(R.id.currentLogsLayout).setVisibility(View.VISIBLE);
		findViewById(R.id.previousLogsLayout).setVisibility(View.VISIBLE);
		findViewById(R.id.logLoadProgress).setVisibility(View.GONE);
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


	private LogEntry currentEntry() {
		NumberPicker weightPicker = (NumberPicker) findViewById(R.id.weightPicker);
		NumberPicker repsPicker = (NumberPicker) findViewById(R.id.repsPicker);

		LogEntry log = new LogEntry();
		log.exercise = currentExercise.name;
		log.weight = ((weightPicker.getValue() + 1) * weightScale);
		log.reps = (repsPicker.getValue() + 1);
		log.time = new LocalTime().toString(ISODateTimeFormat.time());
		return log;
	}

	private void restFor(int restTime) {
		if(restTime <= 0) return;
		
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
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
	        boolean vibrateAfterRest = settings.getBoolean("vibrateAfterRest", true);	        
	        boolean soundAfterRest = settings.getBoolean("soundAfterRest", true);

	        if(soundAfterRest) {
	        	MediaPlayer mPlayer = MediaPlayer.create(this.getApplicationContext(), R.raw.fight);
	        	mPlayer.setVolume(.5f, .5f);  //Let's not go crazy here, probably listening to music.
	        	mPlayer.start();
	        }
	        
	        if(vibrateAfterRest) {
	        	Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
	        	v.vibrate(new long[]{160, 228, 13,228, 13,228, 13,228, 13,228}, -1);
	        }
	      
		}
	}

	private static class LoadLogData extends AsyncTask<Void, Void, LoadLogData.Output> {
		public static class Output {
			List<LogEntry> currentLogs = new ArrayList<LogEntry>();
			List<LogEntry> previousLogs = new ArrayList<LogEntry>();
			LocalDate previousDate = null;
		}
		private LogActivity act;
		private File dir;
		private Exercise currentExercise;

		public LoadLogData(LogActivity act, Exercise currentExercise, File dir) {
			this.act = act;
			this.dir = dir;
			this.currentExercise = currentExercise;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected Output doInBackground(Void... params) {		
			Output output = new Output();
						
			String today = new LocalDate().toString("yyyy-MM-dd");

			try {
				File[] files = dir.listFiles();

				Arrays.sort(files, new Comparator<File>() {
					public int compare(File f1, File f2) {
						return f2.getName().compareTo(f1.getName());
					}
				});
				
				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				Type collectionType = new TypeToken<Collection<LogEntry>>() {}.getType();
				DateTimeFormatter pattern = DateTimeFormat.forPattern("yyyy-MM-dd");
				
				output.currentLogs = new ArrayList<LogEntry>();
				output.previousLogs = new ArrayList<LogEntry>();
				
				for (int i = 0; i < 50 && i < files.length && output.previousLogs.size() == 0; i++) {
					File file = files[i];
					String json = FileUtils.readFileToString(file, "UTF-8");
					List<LogEntry> logs = gson.fromJson(json,collectionType);
					if (file.getName().equals(today + ".json")) {
						output.currentLogs = logs;
					} else {
						for (LogEntry entry : logs) {
							if (entry.exercise.equals(currentExercise.name)) {
								output.previousLogs = logs;
								output.previousDate = LocalDate.parse(file.getName().substring(0,10), pattern);
							}
						}
					}
				}

				return output;
			} catch (IOException e) {
				Log.e("IO", "Problem", e);
				return new Output();
			}

		}

		@Override
		protected void onPostExecute(Output output) {
			super.onPostExecute(output);
			act.loadCurrentLogs(output);
		}

	}
	
}
