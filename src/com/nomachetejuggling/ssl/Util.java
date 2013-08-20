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
		if(!myDir.mkdirs()) {
			Log.w("Util", "Directory not created");
		}
	    return new File(myDir, "exerciseList.json");
	}

}
