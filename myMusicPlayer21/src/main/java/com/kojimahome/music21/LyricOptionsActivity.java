package com.kojimahome.music21;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class LyricOptionsActivity extends Activity {
	private static RadioGroup mRadioGroup;
	public static final int AUTO = 1;
	public static final int ID3TAG = 2;
//	public static final int MUSIX = 3;
	public static final String LYRICOPTIONS = "lyricoptions";
	private static int mOptionOld;
	private static int mOption;
	
	public void onCreate(Bundle icicle) 
	{
		super.onCreate(icicle);
		setContentView(R.layout.lyric_options_activity);
		mRadioGroup = (RadioGroup) findViewById(R.id.radioGroup1);
		
		mOptionOld = mOption = MusicUtils.getIntPref(this, LYRICOPTIONS, AUTO);
		RadioButton radiobutton = null;
		
		switch (mOptionOld) {
		case AUTO:
			radiobutton = (RadioButton) findViewById(R.id.radio0);
			break;
		case ID3TAG:
			radiobutton = (RadioButton) findViewById(R.id.radio1);
			break;
//		case MUSIX:
//			radiobutton = (RadioButton) findViewById(R.id.radio2);
//			break;
		default:
			radiobutton = null;
		}
		if (radiobutton != null) {
			mRadioGroup.check(radiobutton.getId());
		}
	}
	
	public void AutoClicked (View view) {
		RadioButton radiobutton = (RadioButton) view;
		mRadioGroup.check(radiobutton.getId());
		mOption = AUTO;
	}
	
	public void Id3TagClicked (View view) {
		RadioButton radiobutton = (RadioButton) view;
		mRadioGroup.check(radiobutton.getId());
		mOption = ID3TAG;
	}
	
//	public void MusixClicked (View view) {
//		RadioButton radiobutton = (RadioButton) view;
//		mRadioGroup.check(radiobutton.getId());
//		mOption = MUSIX;
//	}
	
	public void OkClicked (View view) {
		if (mOptionOld != mOption) {
			MusicUtils.setIntPref(this, LYRICOPTIONS, mOption);
			setResult(RESULT_OK, null);
		} else {
			setResult(RESULT_CANCELED, null);
		}
		finish();
		return;
	}
	
	public void CancelClicked (View view) {
		setResult(RESULT_CANCELED, null);
		finish();
		return;
	}

}
