package com.kojimahome.music21;

import java.util.Calendar;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TimePicker;

public class HourMinuteDialog extends AlertDialog {
	private static Context mContext;
	private static int mHour;
	private static int mMinute;
	private static TimePicker mTimePicker;
	private static RadioButton mRadioButtonAfter;
	private static RadioButton mRadioButtonAt;
	private static int buttonid;
	private static int mAfterOrAt = SleepTimerActivity.AFTER_MODE;
	int mDelta = 0;

	public interface onHourMinuteSetListener {
		void onHourMinuteSet();
	}

	private onHourMinuteSetListener mCallback;

	public HourMinuteDialog(Context context, onHourMinuteSetListener callback,
			int bid) {
		super(context);
		mContext = context;
		mCallback = callback;
		buttonid = bid;
		LayoutInflater inflater = (LayoutInflater) mContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.hourmunite_dialog, null);
		setView(view);

		mTimePicker = (TimePicker) view.findViewById(R.id.time_picker_00);
		mTimePicker.setIs24HourView(true);
		switch (buttonid) {
		case SleepTimerActivity.BUTTON1_ID:
			mDelta = MusicUtils.getIntPref(mContext,
					SleepTimerActivity.BUTTON1_MINUTES,
					SleepTimerActivity.BUTTON1_DEFAULT_MIN);
			break;
		case SleepTimerActivity.BUTTON2_ID:
			mDelta = MusicUtils.getIntPref(mContext,
					SleepTimerActivity.BUTTON2_MINUTES,
					SleepTimerActivity.BUTTON2_DEFAULT_MIN);
			break;
		default:
			break;
		}
		mTimePicker.setCurrentHour(mDelta / 60);
		mTimePicker.setCurrentMinute(mDelta % 60);

		setTitle("TITLE");
		setIcon(R.drawable.ic_sleep_timer);
		setButton(AlertDialog.BUTTON_POSITIVE,
				context.getText(android.R.string.ok), new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mDelta = mTimePicker.getCurrentHour()*60 
						+ mTimePicker.getCurrentMinute();
						switch (buttonid) {
						case SleepTimerActivity.BUTTON1_ID:
							MusicUtils.setIntPref(mContext,
									SleepTimerActivity.BUTTON1_MINUTES,
									mDelta);
							MusicUtils.setIntPref(mContext,
									SleepTimerActivity.BUTTON1_PRESET_MODE,
									mAfterOrAt);
							break;
						case SleepTimerActivity.BUTTON2_ID:
							MusicUtils.setIntPref(mContext,
									SleepTimerActivity.BUTTON2_MINUTES,
									mDelta);
							MusicUtils.setIntPref(mContext,
									SleepTimerActivity.BUTTON2_PRESET_MODE,
									mAfterOrAt);
							break;
						default:
							break;
						}
						mCallback.onHourMinuteSet();
						cancel();
					}
				});

		setButton(AlertDialog.BUTTON_NEGATIVE,
				context.getText(android.R.string.cancel),
				new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						cancel();
					}
				});

		mRadioButtonAfter = (RadioButton) view.findViewById(R.id.radio_after);
		mRadioButtonAt = (RadioButton) view.findViewById(R.id.radio_at);
		switch (buttonid) {
		case SleepTimerActivity.BUTTON1_ID:
			mAfterOrAt = MusicUtils.getIntPref(mContext,
					SleepTimerActivity.BUTTON1_PRESET_MODE,
					SleepTimerActivity.AFTER_MODE);
			break;
		case SleepTimerActivity.BUTTON2_ID:
			mAfterOrAt = MusicUtils.getIntPref(mContext,
					SleepTimerActivity.BUTTON2_PRESET_MODE,
					SleepTimerActivity.AFTER_MODE);
			break;
		default:
			mAfterOrAt = SleepTimerActivity.AFTER_MODE;
			break;
		}
		switch (mAfterOrAt) {
		case SleepTimerActivity.AT_MODE:
			mRadioButtonAt.setChecked(true);
			break;
		case SleepTimerActivity.AFTER_MODE:
		default:
			mRadioButtonAfter.setChecked(true);
			break;
		}
		mRadioButtonAfter.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mAfterOrAt = SleepTimerActivity.AFTER_MODE;
			}
		});
		mRadioButtonAt.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mAfterOrAt = SleepTimerActivity.AT_MODE;
			}
		});
	}
}
