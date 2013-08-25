package com.nomachetejuggling.ssl;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.Months;
import org.joda.time.Weeks;
import org.joda.time.Years;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.nomachetejuggling.ssl.model.Exercise;
import com.nomachetejuggling.ssl.model.MuscleGroups;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.os.Environment;
import android.util.Log;

public class Util {

	public static File getLogStorageDir(Context context) {
		File dir = Environment.getExternalStorageDirectory();
		File myDir = new File(dir, "/SimpleHealthSuite/Strength/Logs");
		if (!myDir.mkdirs()) {
			Log.w("Util", "Directory not created");
		}
		return myDir;
	}

	public static File getExerciseFile(Context context) {
		File dir = Environment.getExternalStorageDirectory();
		File myDir = new File(dir, "/SimpleHealthSuite/Strength");
		if (!myDir.mkdirs()) {
			Log.w("Util", "Directory not created");
		}
		return new File(myDir, "exerciseList.json");
	}

	public static String getRelativeDate(LocalDate today, LocalDate previousDate) {
		//These calculations are relatively expensive, so we'll only do the ones we absolutely need to
		int years = Years.yearsBetween(previousDate, today).getYears();
		if (years > 1) {
			return years + " Years Ago";
		} else if (years == 1) {
			return "One Year Ago";
		} else {
			int months = Months.monthsBetween(previousDate, today).getMonths();
			if (months > 1) {
				return months + " Months Ago";
			} else if (months == 1) {
				return "1 Month Ago";
			} else {
				int weeks = Weeks.weeksBetween(previousDate, today).getWeeks();
				if (weeks > 1) {
					return weeks + " Weeks Ago";
				} else if (weeks == 1) {
					return "1 Week Ago";
				} else {
					int days = Days.daysBetween(previousDate, today).getDays();
					if (days > 1) {
						return days + " Days Ago";
					} else if (days == 1) {
						return "Yesterday";
					} else {
						return "Previously";
					}
				}
			}
		}
	}

	public static MuscleGroups loadMuscleGroups(Resources resources) {
		Gson gson=new Gson(); 
		Type type = new TypeToken<Map<String, List<String>>>(){}.getType();
		try {
			InputStream stream = resources.openRawResource(R.raw.muscles);
		
			Map<String, List<String>> myMap = gson.fromJson(new InputStreamReader(stream), type);
		
			return new MuscleGroups(myMap);
		} catch(NotFoundException e) {
			Log.e("IO", "Somehow the raw resource couldn't be found...", e);
			return new MuscleGroups(new HashMap<String, List<String>>());
		}
	}

	public static String join(String[] s, String separator, String ifEmpty)
	{
	  if (s.length==0) return ifEmpty;
	
	  StringBuilder out=new StringBuilder();
	  out.append(s[0]);
	
	  for (int x=1;x<s.length;++x)
	    out.append(separator).append(s[x]);
	
	  return out.toString();
	}

	static Activity getActivityFromContext(Context context) {
		while (context instanceof ContextWrapper && !(context instanceof Activity)) {
	        context = ((ContextWrapper) context).getBaseContext();
	    }
	    if (!(context instanceof Activity)) {
	        throw new IllegalStateException("The Context is not an Activity.");
	    }
	
	    return (Activity) context;
	}

}
