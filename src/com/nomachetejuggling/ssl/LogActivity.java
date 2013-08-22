package com.nomachetejuggling.ssl;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;

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

//TODO: prepopulate spinners to last value
//TODO: handle pause/resume
//TODO: undo button to undo previously logged entry
//TODO: still loading kinda slow and sluggish.

public class LogActivity extends Activity {

	private List<LogEntry> currentLogs = null;
	private List<LogEntry> previousLogs = null;
	private Exercise currentExercise;
	private boolean resting;
	private String previousFileName = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_log);
		// Show the Up button in the action bar.
		setupActionBar();
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		currentLogs = new ArrayList<LogEntry>();
		previousLogs = new ArrayList<LogEntry>();

		resting = false;
	}

	@Override
	protected void onStart() {
		super.onStart();
		currentExercise = (Exercise) getIntent().getExtras().getSerializable(
				"exercise");
		Log.i("LogActivity", "Exercise: " + currentExercise.toString());

		int numValues = 100;
		int PICKER_RANGE = 5;

		NumberPicker weightPicker = (NumberPicker) findViewById(R.id.weightPicker);

		String[] displayedValues = new String[numValues];
		// Populate the array
		for (int i = 0; i < numValues; i++)
			displayedValues[i] = String.valueOf(PICKER_RANGE * (i + 1))
					+ " lbs";

		weightPicker.setMinValue(0);
		weightPicker.setMaxValue(displayedValues.length - 1);
		weightPicker.setDisplayedValues(displayedValues);
		weightPicker.setWrapSelectorWheel(false);

		int maxReps = 20;
		NumberPicker repsPicker = (NumberPicker) findViewById(R.id.repsPicker);
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
			ab.setTitle("Log");
			ab.setSubtitle(currentExercise.name);
		}
		
		File dir = Util.getLogStorageDir(getApplicationContext());
		
		new PostTask(this).execute(currentExercise, dir);
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
		// getMenuInflater().inflate(R.menu.log, menu);
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
		log.weight = ((weightPicker.getValue() + 1) * 5);
		log.reps = (repsPicker.getValue() + 1);
		this.currentLogs.add(log);

		this.persistCurrentLogs();
	}

	private LogEntry currentEntry() {
		NumberPicker weightPicker = (NumberPicker) findViewById(R.id.weightPicker);
		NumberPicker repsPicker = (NumberPicker) findViewById(R.id.repsPicker);

		LogEntry log = new LogEntry();
		log.exercise = currentExercise;
		log.weight = ((weightPicker.getValue() + 1) * 5);
		log.reps = (repsPicker.getValue() + 1);
		return log;
	}

	public void logSetAndRest(View view) {
		LogEntry log = currentEntry();
		this.currentLogs.add(log);

		this.persistCurrentLogs();

		final ProgressBar timerProgressBar = (ProgressBar) findViewById(R.id.restTimerBar);
		timerProgressBar.setMax(currentExercise.restTime);
		timerProgressBar.setProgress(0);

		final Button saveButton = (Button) findViewById(R.id.saveButton);
		final Button saveAndRestButton = (Button) findViewById(R.id.saveAndRestButton);
		final Button undoButton = (Button) findViewById(R.id.undoButton);

		saveButton.setEnabled(false);
		saveAndRestButton.setText(currentExercise.restTime + "s");
		saveAndRestButton.setEnabled(false);
		undoButton.setEnabled(false);
		timerProgressBar.setVisibility(View.VISIBLE);
		this.resting = true;

		new CountDownTimer(currentExercise.restTime * 1000, 1000) {

			public void onTick(long millisUntilFinished) {
				if (!resting) {
					this.cancel();
					this.onFinish();
				}

				int secsLeft = (int) (millisUntilFinished / 1000);
				timerProgressBar.setProgress(currentExercise.restTime
						- secsLeft);
				Period period = new Period(millisUntilFinished);

				saveAndRestButton.setText(String.format("%02d:%02d",
						period.getMinutes(), period.getSeconds()));
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
		this.resting = false;

		Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		// Vibrate for 500 milliseconds
		v.vibrate(500);

		MediaPlayer mPlayer = MediaPlayer.create(this.getApplicationContext(),
				R.raw.fight);
		// mPlayer.setVolume(f, .4f);
		mPlayer.start();
	}

	@Override
	public void onBackPressed() {
		// This isnt working for some reason... press back and it kills the
		// timer and stuff but the countdown keeps ticking
		// if(resting) {
		// resting=false;
		// } else {
		// super.onBackPressed();
		// }
		super.onBackPressed();
	}

	public void loadCurrentLogs(LogSet logSet) {
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

		// Start the pickers at the previous value
		LogEntry lastEntry = null;
		if(currentLogs.size() > 0) {
			lastEntry = currentLogs.get(currentLogs.size()-1);
		} else if(previousLogs.size() > 0){
			lastEntry = previousLogs.get(previousLogs.size()-1);
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

	public void persistCurrentLogs() {
		File dir = Util.getLogStorageDir(this.getApplicationContext());
		File file = new File(dir, new LocalDate().toString("yyyy-MM-dd")
				+ ".json");
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

		// int
		// currentTextColor=getResources().getColor(R.color.currentLogEntry);
		sb.append("<br/><font color='#aaaaaa'><i>" + formatEntry(currentEntry)+ "</i></font>");

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
		return ""+entry.weight+"x"+entry.reps+" &nbsp; (1RM="+oneRM+")";
	}

	private StringBuilder builderForLogs(List<LogEntry> logs) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < logs.size(); i++) {
			LogEntry logEntry = logs.get(i);
			if (logEntry.exercise.equals(currentExercise)) {
				if(i==logs.size()-1) {
					sb.append(formatEntry(logEntry));
				} else {
					sb.append(formatEntry(logEntry));
					sb.append("<br/>");
				}
			}
		}
		return sb;
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);

		NumberPicker weightPicker = (NumberPicker) findViewById(R.id.weightPicker);
		NumberPicker repsPicker = (NumberPicker) findViewById(R.id.repsPicker);

		savedInstanceState.putInt("WeightPickerPosition",
				weightPicker.getValue());
		savedInstanceState.putInt("RepsPickerPosition", repsPicker.getValue());
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
	}

	private static class LogSet {
		public List<LogEntry> currentLogs = new ArrayList<LogEntry>();
		public List<LogEntry> previousLogs = new ArrayList<LogEntry>();
		public LocalDate previousDate = null;
	}

	private static class PostTask extends AsyncTask<Object, Void, LogSet> {

		private static ProgressDialog dialog;
		private LogActivity act;

		public PostTask(LogActivity act) {
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
			File file = new File(dir, today + ".json");

			try {
				String json = FileUtils.readFileToString(file, "UTF-8");
				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				Log.d("IO", "Start Reading from " + file.getAbsolutePath()
						+ "\n" + json);

				Type collectionType = new TypeToken<Collection<LogEntry>>() {}.getType();
				List<LogEntry> currentLogsRead = gson.fromJson(json,collectionType);

				LogSet logSet = new LogSet();
				logSet.currentLogs = currentLogsRead;

				// Now search for previous log with this exercise
				File[] files = dir.listFiles();

				Arrays.sort(files, new Comparator<File>() {
					public int compare(File f1, File f2) {
						return f2.getName().compareTo(f1.getName());
					}
				});

				// Check the last 50 files to find one with entries for this
				// exercise
				for (int i = 0; i < 50 && i < files.length && logSet.previousLogs.size() == 0; i++) {
					File prevFile = files[i];
					if (!prevFile.getName().equals(today + ".json")) {
						String jsonPrev = FileUtils.readFileToString(files[i], "UTF-8");
						List<LogEntry> prevLogsRead = gson.fromJson(jsonPrev,collectionType);
						for (LogEntry entry : prevLogsRead) {
							if (entry.exercise.name.equals(currentExercise.name)) {
								logSet.previousLogs = prevLogsRead;
								logSet.previousDate = LocalDate.parse(prevFile.getName().substring(0,10), DateTimeFormat.forPattern("yyyy-MM-dd"));
							}
						}
					}
				}

				return logSet;
			} catch (IOException e) {
				// Ignore, a missing file is either an unmounted SD Card
				// (unrecoverable) or a first-time run.
				return new LogSet();
			}

		}

		@Override
		protected void onPostExecute(LogSet result) {
			super.onPostExecute(result);
			act.loadCurrentLogs(result);
			dialog.hide();
		}

	}

}
