package com.kojimahome.music21;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class WelcomeActivity extends Activity {
	
	public void onCreate(Bundle icicle) 
	{
		super.onCreate(icicle);
		setContentView(R.layout.welcome_activity);
		if (MusicUtils.donated(this)) {
//        	TextView tv = (TextView) findViewById(R.id.donation);
        	Button bv = (Button) findViewById(R.id.donationbutton);
//        	tv.setVisibility(View.INVISIBLE);
        	bv.setVisibility(View.INVISIBLE);
        }
	}
	
	public void buttonMethod01(View mybutton) {
		Intent intent = new Intent();
        intent.setClass(this, HelpListActivity.class);
        startActivity(intent);
    }
	
	public void buttonMethod02(View mybutton) {
//		startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("http://www.youtube.com/watch?v=ohzBg9YuqlU")));
		startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(getResources().getString(R.string.introduction_video_url))));
    }
	
	public void buttonMethod03(View mybutton) {
		Intent intent = new Intent();
		intent.setClass(this, HelpPageActivity.class);
		intent.putExtra(HelpPageActivity.LAYOUT, R.layout.float_pad_help_activity);
        startActivity(intent);
    }
	
	public void buttonMethod04(View mybutton) {
		Intent intent = new Intent();
		intent.setClass(this, HelpPageActivity.class);
		intent.putExtra(HelpPageActivity.LAYOUT, R.layout.list_traverse_help_activity);
        startActivity(intent);
    }
	
	public void onClickDonationButton(View mybutton) {
		try {
        	Intent intent = new Intent(Intent.ACTION_VIEW);
        	intent.setData(Uri.parse("market://details?id=com.learnerstechlab.abdonation5"));
        	startActivity(intent);
        	} catch (ActivityNotFoundException e) {
        		try {
        		Intent intent2 = new Intent(Intent.ACTION_VIEW);
        		intent2.setData(Uri.parse("http://play.google.com/store/apps/details?id=com.learnerstechlab.abdonation5"));
            	startActivity(intent2);
        		} catch (ActivityNotFoundException e2) {
        			Toast.makeText(this, "Apps access Google Play Not installed.", Toast.LENGTH_LONG).show();
        		}
        	}
    }
	
	public void onClickCommentButton(View mybutton) {
		try {
        	Intent intent = new Intent(Intent.ACTION_VIEW);
        	intent.setData(Uri.parse("market://details?id=com.kojimahome.music21"));
        	startActivity(intent);
        	} catch (ActivityNotFoundException e) {
        		try {
        		Intent intent2 = new Intent(Intent.ACTION_VIEW);
        		intent2.setData(Uri.parse("http://play.google.com/store/apps/details?id=com.kojimahome.music21"));
            	startActivity(intent2);
        		} catch (ActivityNotFoundException e2) {
        			Toast.makeText(this, "Apps access Google Play Not installed.", Toast.LENGTH_LONG).show();
        		}
        	}
    }
	
	public void buttonOk(View mybutton) {
		finish();
    }

}
