package com.nomachetejuggling.ssl;

import java.io.File;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nomachetejuggling.ssl.model.Exercise;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class Util {

	public static File getLogStorageDir(Context context, String albumName) {
	    // Get the directory for the app's private pictures directory. 
	    File file = new File(context.getExternalFilesDir(
	            Environment.DIRECTORY_PICTURES), albumName);
	    if (!file.mkdirs()) {
	        Log.e("blahrg", "Directory not created");
	    }
	    return file;
	}
	
	public static File getExerciseFile(Context context) {
		File dir = context.getExternalFilesDir(null);
		if(!dir.mkdirs()) {
			Log.e("blahrg", "Directory not created");
		}
//		if (!file.getParentFile().mkdirs()) {
//	        Log.e("blahrg", "Directory not created");
//	    }
	    return new File(dir, "exercise_list.json");
	}

}
