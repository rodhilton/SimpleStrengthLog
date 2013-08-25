package com.nomachetejuggling.ssl;

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.nomachetejuggling.ssl.model.Exercise;

public class AddActivity extends Activity {
	
	private String[] availableMuscles;
	private String[] currentMuscles;	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add);
		setupActionBar();
	
		availableMuscles = getIntent().getExtras().getStringArray("muscles");
	
		
		if(savedInstanceState != null) { //Restore
			if(savedInstanceState.containsKey("currentMuscles")) {
				setCurrentMuscles(savedInstanceState.getStringArray("currentMuscles"));
			} else {
				setCurrentMuscles(new String[]{});
			}
		} else if(getIntent().getExtras().containsKey("exercise")) { //Edit
			Exercise exercise = (Exercise) getIntent().getExtras().getSerializable("exercise");
			
			EditText nameText = (EditText) findViewById(R.id.nameText);
			nameText.setText(exercise.name);
			nameText.setEnabled(false); //Renaming has huge cascading effects, it's not allowed for now
			
			EditText restTimeText = (EditText) findViewById(R.id.restTimeText);
			restTimeText.setText(""+exercise.restTime);
			restTimeText.requestFocus();
			
			CheckBox favoriteCheckBox = (CheckBox) findViewById(R.id.favoriteCheckBoxAdd);
			favoriteCheckBox.setChecked(exercise.favorite);
			
			setCurrentMuscles(exercise.muscles);
			
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
		} else { // Add
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		}
	}

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
		boolean valid = true;
		
		TextView nameText = (TextView) this.findViewById(R.id.nameText);
		TextView restText = (TextView) this.findViewById(R.id.restTimeText);
		
		if( nameText.getText().toString().length() == 0 ) { 
			nameText.setError( "Exercise name is required!" );
			valid = false;
		}
		
		if(!StringUtils.isNumeric(restText.getText())) {
			restText.setError("Rest time must be a number");
			valid = false;
		}
		
		int restTime = Integer.parseInt(restText.getText().toString());
		if(restTime <= 0 || restTime > 300) {
			restText.setError("Rest time must be between 1 and 300");
			valid = false;
		}
		
		CheckBox favoriteCheckBox = (CheckBox) findViewById(R.id.favoriteCheckBoxAdd);		
		
		if(valid) {
			Exercise newExercise = new Exercise();
			newExercise.name=nameText.getText().toString();
			newExercise.restTime=restTime;
			newExercise.muscles = this.currentMuscles;
			newExercise.favorite = favoriteCheckBox.isChecked();
			
			Intent intent = new Intent();
			intent.putExtra("newExercise",newExercise);
			setResult(RESULT_OK,intent);
			finish();	
		}
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
