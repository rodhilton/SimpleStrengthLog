package com.nomachetejuggling.ssl;

import java.util.ArrayList;

import com.nomachetejuggling.ssl.model.Exercise;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.support.v4.app.NavUtils;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;

//TODO: only allow up to a certain value for rest time.  
public class AddActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add);
		// Show the Up button in the action bar.
		setupActionBar();
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
	
	public void saveExercise() {
		TextView nameText = (TextView) this.findViewById(R.id.nameText);
		TextView restText = (TextView) this.findViewById(R.id.restTimeText);
		
		Exercise newExercise = new Exercise();
		newExercise.name=(nameText.getText().toString());
		newExercise.restTime=Integer.parseInt(restText.getText().toString());
		
		Intent intent = new Intent();
		intent.putExtra("newExercise",newExercise);
		setResult(RESULT_OK,intent);
		finish();	
	}
	
	public void clickTags(View view) {
		final String items[] = {"Movie","Music","Book"};
		
		final ArrayList<Integer> mSelectedItems = new ArrayList();  // Where we track the selected items
		
		AlertDialog.Builder ab = new AlertDialog.Builder(this);
		ab.setTitle("Choose Tags");
		ab.setMultiChoiceItems(R.array.musclegroups, null, new DialogInterface.OnMultiChoiceClickListener() {
	         @Override
	         public void onClick(DialogInterface dialog, int which,
	                 boolean isChecked) {
	             if (isChecked) {
	                 // If the user checked the item, add it to the selected items
	                 mSelectedItems.add(which);
	             } else if (mSelectedItems.contains(which)) {
	                 // Else, if the item is already in the array, remove it 
	                 mSelectedItems.remove(Integer.valueOf(which));
	             }
	         }
	     });
		ab.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK, so save the mSelectedItems results somewhere
                // or return them to the component that opened the dialog
                
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
