package com.kojimahome.music21;

import android.os.Bundle;
import android.view.View;
import android.app.Activity;
import android.content.Intent;

public class HelpPageActivity extends Activity {
	public static final String LAYOUT = "layout";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        int viewresid = intent.getIntExtra(LAYOUT, 0);
        if (viewresid != 0) setContentView(viewresid);
    }
    
    public void onClickBluetooth01 (View v) {
    	MusicUtils.playAlert(this,"canopus");
    }
    
    public void onClickBluetooth02 (View v) {
    	MusicUtils.playAlert(this);
    }
    
    public void onClickreleaseNoteButton (View v) {
    	Intent intent = new Intent();
        intent.setClass(this, NewFeaturesActivity.class);
        startActivity(intent);
    }

}
