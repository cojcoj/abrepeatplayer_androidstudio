package com.kojimahome.music21;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class ABPosDispOrderActivity extends Activity {
	private static RadioGroup mRadioGroup;
	public static final int CREATION_NORMAL = 1;
	public static final int CREATION_REVERSE = 2;
	public static final int APOS_NORMAL = 3;
	public static final int APOS_REVERSE = 4;
	public static final int NAME_NORMAL = 5;
	public static final int NAME_REVERSE = 6;
	private static int mOrderOld;
	private static int mOrder;
	
	public void onCreate(Bundle icicle) 
	{
		super.onCreate(icicle);
		setContentView(R.layout.abpos_disp_order_activity);
		mRadioGroup = (RadioGroup) findViewById(R.id.radioGroup1);
		
		mOrderOld = mOrder = MusicUtils.getIntPref(this, "abposdisporder", CREATION_NORMAL);
		RadioButton radiobutton = null;
		
		switch (mOrderOld) {
		case CREATION_NORMAL:
			radiobutton = (RadioButton) findViewById(R.id.radio0);
			break;
		case CREATION_REVERSE:
			radiobutton = (RadioButton) findViewById(R.id.radio1);
			break;
		case APOS_NORMAL:
			radiobutton = (RadioButton) findViewById(R.id.radio2);
			break;
		case APOS_REVERSE:
			radiobutton = (RadioButton) findViewById(R.id.radio3);
			break;
		case NAME_NORMAL:
			radiobutton = (RadioButton) findViewById(R.id.radio4);
			break;
		case NAME_REVERSE:
			radiobutton = (RadioButton) findViewById(R.id.radio5);
			break;
		default:
			radiobutton = null;
		}
		if (radiobutton != null) {
			mRadioGroup.check(radiobutton.getId());
		}
	}
	
	public void CreateNormalClicked (View view) {
		RadioButton radiobutton = (RadioButton) view;
		mRadioGroup.check(radiobutton.getId());
		mOrder = CREATION_NORMAL;
	}
	
	public void CreateReverseClicked (View view) {
		RadioButton radiobutton = (RadioButton) view;
		mRadioGroup.check(radiobutton.getId());
		mOrder = CREATION_REVERSE;
	}
	
	public void APosNormalClicked (View view) {
		RadioButton radiobutton = (RadioButton) view;
		mRadioGroup.check(radiobutton.getId());
		mOrder = APOS_NORMAL;
	}
	
	public void APosReverseClicked (View view) {
		RadioButton radiobutton = (RadioButton) view;
		mRadioGroup.check(radiobutton.getId());
		mOrder = APOS_REVERSE;
	}
	
	public void NameNormalClicked (View view) {
		RadioButton radiobutton = (RadioButton) view;
		mRadioGroup.check(radiobutton.getId());
		mOrder = NAME_NORMAL;
	}
	
	public void NameReverseClicked (View view) {
		RadioButton radiobutton = (RadioButton) view;
		mRadioGroup.check(radiobutton.getId());
		mOrder = NAME_REVERSE;
	}
	
	public void OkClicked (View view) {
		if (mOrderOld != mOrder) {
			MusicUtils.setIntPref(this, "abposdisporder", mOrder);
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
