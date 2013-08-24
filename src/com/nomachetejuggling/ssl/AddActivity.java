package com.nomachetejuggling.ssl;

import java.util.ArrayList;
import java.util.List;

import com.nomachetejuggling.ssl.model.Exercise;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.support.v4.app.NavUtils;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;

//TODO: only allow up to a certain value for rest time.
//TODO: basic validation.  non-empty name, min and max vals for rest time, no negs

public class AddActivity extends Activity {
	
	private String[] availableMuscles;
	private String[] currentMuscles;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add);
		setupActionBar();
	
		availableMuscles = getIntent().getExtras().getStringArray("muscles");
		
		if(savedInstanceState != null) {
			if(savedInstanceState.containsKey("currentMuscles")) {
				setCurrentMuscles(savedInstanceState.getStringArray("currentMuscles"));
			} else {
				setCurrentMuscles(new String[]{});
			}
		}
	}

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.add, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		case R.id.action_save: 
			saveExercise();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putSerializable("currentMuscles", this.currentMuscles);
	}
	
	public void saveExercise() {
		TextView nameText = (TextView) this.findViewById(R.id.nameText);
		TextView restText = (TextView) this.findViewById(R.id.restTimeText);
		
		Exercise newExercise = new Exercise();
		newExercise.name=(nameText.getText().toString());
		newExercise.restTime=Integer.parseInt(restText.getText().toString());
		newExercise.muscles = this.currentMuscles;
		
		Intent intent = new Intent();
		intent.putExtra("newExercise",newExercise);
		setResult(RESULT_OK,intent);
		finish();	
	}
	
	private void setCurrentMuscles(String[] muscles) {
    	currentMuscles = muscles;
    	
    	Button musclesButton = (Button) findViewById(R.id.musclesButton);
    	musclesButton.setText(Util.join(currentMuscles, ", ", getString(R.string.setTagsButton)));
	}
	
	public void clickTags(View view) {
		
		final ArrayList<String> mSelectedMuscles = new ArrayList<String>();
		if(currentMuscles != null) {
			for(String currentMuscle: currentMuscles) {
				mSelectedMuscles.add(currentMuscle);
			}
		}
		
		//Super inefficient but it's a small list, hopefully we can get away with it.
		boolean[] checkedItems = new boolean[availableMuscles.length];	
		for(int i=0;i<checkedItems.length;i++) {
			checkedItems[i] = mSelectedMuscles.contains(availableMuscles[i]);
		}
		
		AlertDialog.Builder ab = new AlertDialog.Builder(this);
		ab.setTitle("Select Muscles Worked");
		ab.setMultiChoiceItems(availableMuscles, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
	         @Override
	         public void onClick(DialogInterface dialog, int which, boolean isChecked) {
	             if (isChecked) {
	            	 mSelectedMuscles.add(availableMuscles[which]);
	             } else if (mSelectedMuscles.contains(which)) { 
	            	 mSelectedMuscles.remove(availableMuscles[which]);
	             }
	         }
	     });
		ab.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
            	String[] muscles = mSelectedMuscles.toArray(new String[]{});
            	setCurrentMuscles(muscles);
            }
        });
		
        ab.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
            	
            }
        });
		
		ab.create().show();

	}

}
