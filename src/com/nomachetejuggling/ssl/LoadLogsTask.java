package com.nomachetejuggling.ssl;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.nomachetejuggling.ssl.model.Exercise;
import com.nomachetejuggling.ssl.model.LogEntry;
import com.nomachetejuggling.ssl.model.LogSet;

public class LoadLogsTask extends AsyncTask<Object, Void, LogSet> {

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

