package com.kojimahome.music21;

import java.util.Calendar;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;

public class SleepTimerActivity extends Activity {
	private static String LOGTAG = "SleepTimerActivity";
	public static final String ALARM_TIME = "com.learnerstechlab.music21.alarm_time";
	private static final int RENEW_REMAINS = 0;
	public static final int BUTTON1_ID = 1;
	public static final int BUTTON2_ID = 2;
	public static final String BUTTON1_MINUTES = "button1_minutes";
	public static final String BUTTON2_MINUTES = "button2_minutes";
	public static final int AFTER_MODE = 0;
	public static final int AT_MODE = 1;
	public static final String BUTTON1_PRESET_MODE = "button1_alarm_preset_mode";
	public static final String BUTTON2_PRESET_MODE = "button2_alarm_preset_mode";
	public static final int BUTTON1_DEFAULT_MIN = 30;
	public static final int BUTTON2_DEFAULT_MIN = 60;
	private static Context mContext;
	private static AlarmManager mAM;
	private static Calendar cal;
	private static TimePicker mTimePicker;
	private static boolean mFromTimePicker = false;
	private static int mTimerHour = 0;
	private static int mTimerMinute = 0;
	private static Button mPresetButton1;
	private static Button mPresetButton2;
	private static Button mSetClearButton;
	private static Button mCloseButton;
	private static TextView mCurrentTimer;
	private static TextView mTimeRemains;
	private static long mSavedTime = 0L;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		cal = Calendar.getInstance();

		bindViews();

		bindListeners();

	}

	@Override
	public void onResume() {
		super.onResume();
		mSavedTime = MusicUtils.getLongPref(mContext, ALARM_TIME, 0L);
		if (mSavedTime < System.currentTimeMillis()) { // timedout while power
														// down
			MusicUtils.setLongPref(mContext, ALARM_TIME, 0L);
			mSavedTime = 0L;
		} else { // not timedout yet, let's set timer if it disappeared
			if (PendingIntent
					.getService(mContext, 12345, new Intent(mContext,
							MediaPlaybackService.class),
							(PendingIntent.FLAG_NO_CREATE)) == null) {
				Intent intent = new Intent(mContext, MediaPlaybackService.class);
				intent.setAction(MediaPlaybackService.SERVICECMD);
				intent.putExtra(MediaPlaybackService.CMDNAME,
						MediaPlaybackService.CMDPAUSE);
				intent.putExtra(ALARM_TIME, true);
				PendingIntent pendingIntent = PendingIntent.getService(
						mContext, 12345, intent,
						(PendingIntent.FLAG_CANCEL_CURRENT));
				mAM.set(AlarmManager.RTC_WAKEUP, mSavedTime, pendingIntent);
			}
		}
		// now mSavedTime and the Timer are consistent
		if (mSavedTime != 0L) {
			setButtonsAlarmExists();
			mHandler.sendEmptyMessageDelayed(RENEW_REMAINS, 1000);
			// Log.i(LOGTAG,"msgsent in onResume");
		} else {
			setButtonsNoAlarmExists();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		mHandler.removeMessages(RENEW_REMAINS);
		// Log.i(LOGTAG,"msgremoved in onPause");
	}

	private void bindViews() {
		setContentView(R.layout.sleep_timer_activity);
		mAM = (AlarmManager) getSystemService(Activity.ALARM_SERVICE);
		mPresetButton1 = (Button) findViewById(R.id.preset_button_1);
		mPresetButton2 = (Button) findViewById(R.id.preset_button_2);
		mTimePicker = (TimePicker) findViewById(R.id.timepicker);
		mSetClearButton = (Button) findViewById(R.id.set_clear);
		mCloseButton = (Button) findViewById(R.id.close);
		mTimePicker.setIs24HourView(true);
		mCurrentTimer = (TextView) findViewById(R.id.currenttimer);
		mTimeRemains = (TextView) findViewById(R.id.timeremains);
	}

	private void bindListeners() {
		mPresetButton1.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				int delta = MusicUtils.getIntPref(mContext, BUTTON1_MINUTES,
						BUTTON1_DEFAULT_MIN);
				int preset_mode = MusicUtils.getIntPref(mContext,
						BUTTON1_PRESET_MODE, AFTER_MODE);
				switch (preset_mode) {
				case AT_MODE:
					mTimePicker.setCurrentHour(delta / 60);
					mTimePicker.setCurrentMinute(delta % 60);
					mFromTimePicker = true;
					break;
				case AFTER_MODE:
				default:
					if (cal == null)
						cal = Calendar.getInstance();
					cal.setTimeInMillis(System.currentTimeMillis());
					cal.add(Calendar.MINUTE, delta);
					mTimePicker.setCurrentHour(cal.get(Calendar.HOUR_OF_DAY));
					mTimePicker.setCurrentMinute(cal.get(Calendar.MINUTE));
					mFromTimePicker = false;
					break;
				}
			}
		});
		mPresetButton2.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				int delta = MusicUtils.getIntPref(mContext, BUTTON2_MINUTES,
						BUTTON2_DEFAULT_MIN);
				int preset_mode = MusicUtils.getIntPref(mContext,
						BUTTON2_PRESET_MODE, AFTER_MODE);
				switch (preset_mode) {
				case AT_MODE:
					mTimePicker.setCurrentHour(delta / 60);
					mTimePicker.setCurrentMinute(delta % 60);
					mFromTimePicker = true;
					break;
				case AFTER_MODE:
				default:
					if (cal == null)
						cal = Calendar.getInstance();
					cal.setTimeInMillis(System.currentTimeMillis());
					cal.add(Calendar.MINUTE, delta);
					mTimePicker.setCurrentHour(cal.get(Calendar.HOUR_OF_DAY));
					mTimePicker.setCurrentMinute(cal.get(Calendar.MINUTE));
					mFromTimePicker = false;
					break;
				}
			}
		});
		mPresetButton1.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				HourMinuteDialog mDialog = new HourMinuteDialog(mContext,
						mButtonChangedListener, BUTTON1_ID);
				mDialog.show();
				return true;
			}
		});
		mPresetButton2.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				HourMinuteDialog mDialog = new HourMinuteDialog(mContext,
						mButtonChangedListener, BUTTON2_ID);
				mDialog.show();
				return true;
			}
		});

		mSetClearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(mContext, MediaPlaybackService.class);
				intent.setAction(MediaPlaybackService.SERVICECMD);
				intent.putExtra(MediaPlaybackService.CMDNAME,
						MediaPlaybackService.CMDPAUSE);
				intent.putExtra(ALARM_TIME, true);
				PendingIntent pendingIntent = PendingIntent.getService(
						mContext, 12345, intent,
						(PendingIntent.FLAG_CANCEL_CURRENT));
				if (mSavedTime == 0L) {
					if (mFromTimePicker) {
						mSavedTime = calcFromTimePicker(
								mTimePicker.getCurrentHour(),
								mTimePicker.getCurrentMinute());
					} else {
						mSavedTime = cal.getTimeInMillis();
					}
					mAM.set(AlarmManager.RTC_WAKEUP, mSavedTime, pendingIntent);
					MusicUtils.setLongPref(mContext, ALARM_TIME, mSavedTime);
					setButtonsAlarmExists();
					mHandler.sendEmptyMessageDelayed(RENEW_REMAINS, 1000);
				} else {
					mAM.cancel(pendingIntent);
					mSavedTime = 0L;
					MusicUtils.setLongPref(mContext, ALARM_TIME, 0L);
					setButtonsNoAlarmExists();
					mHandler.removeMessages(RENEW_REMAINS);
				}
			}
		});

		mCloseButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});

		mTimePicker
				.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
					@Override
					public void onTimeChanged(TimePicker view, int hourOfDay,
							int minute) {
						mFromTimePicker = true;
						if (mSavedTime != 0L) {
							if (hourOfDay != mTimerHour) {
								mTimePicker.setCurrentHour(mTimerHour);
							}
							if (minute != mTimerMinute) {
								mTimePicker.setCurrentMinute(mTimerMinute);
							}
						}
					}
				});
	}

	private void setButtonsAlarmExists() {
		mPresetButton1.setVisibility(View.INVISIBLE);
		mPresetButton2.setVisibility(View.INVISIBLE);
		mSetClearButton.setText(R.string.clear);
		mCurrentTimer.setText(R.string.current_timer);
		mTimeRemains.setText(MusicUtils.makeTimeString(mContext,
				(mSavedTime - System.currentTimeMillis()) / 1000));
		cal.setTimeInMillis(mSavedTime);
		mTimerHour = cal.get(Calendar.HOUR_OF_DAY);
		mTimePicker.setCurrentHour(mTimerHour);
		mTimerMinute = cal.get(Calendar.MINUTE);
		mTimePicker.setCurrentMinute(mTimerMinute);
	}

	private void setButtonsNoAlarmExists() {
		mPresetButton1.setVisibility(View.VISIBLE);
		mPresetButton2.setVisibility(View.VISIBLE);
		setPresetButtonTexts();

		mSetClearButton.setText(R.string.set);
		mCurrentTimer.setText(R.string.no_timer_set);
		mTimeRemains.setText("");
		mTimePicker.setCurrentHour(0);
		mTimePicker.setCurrentMinute(0);
	}

	private void setPresetButtonTexts() {
		String prefix;
		switch (MusicUtils
				.getIntPref(mContext, BUTTON1_PRESET_MODE, AFTER_MODE)) {
		case AFTER_MODE:
			prefix = "After ";
			break;
		case AT_MODE:
			prefix = "At ";
			break;
		default:
			prefix = "";
			break;
		}
		mPresetButton1.setText(prefix
				+ MusicUtils.makeMinuteTimeString(mContext, (MusicUtils
						.getIntPref(mContext, BUTTON1_MINUTES,
								BUTTON1_DEFAULT_MIN) * 60)));

		switch (MusicUtils
				.getIntPref(mContext, BUTTON2_PRESET_MODE, AFTER_MODE)) {
		case AFTER_MODE:
			prefix = "After ";
			break;
		case AT_MODE:
			prefix = "At ";
			break;
		default:
			prefix = "";
			break;
		}
		mPresetButton2.setText(prefix
				+ MusicUtils.makeMinuteTimeString(mContext, (MusicUtils
						.getIntPref(mContext, BUTTON2_MINUTES,
								BUTTON2_DEFAULT_MIN) * 60)));
	}

	private long calcFromTimePicker(int hour, int minute) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
		int currentMinute = calendar.get(Calendar.MINUTE);
		if (hour > currentHour) {
			calendar.set(Calendar.HOUR_OF_DAY, hour);
			calendar.set(Calendar.MINUTE, minute);
		} else if (hour < currentHour) {
			calendar.add(Calendar.DAY_OF_MONTH, 1);
			calendar.set(Calendar.HOUR_OF_DAY, hour);
			calendar.set(Calendar.MINUTE, minute);
		} else { // hour == currentHour
			if (minute > currentMinute) {
				calendar.set(Calendar.MINUTE, minute);
			} else {
				calendar.add(Calendar.DAY_OF_MONTH, 1);
				calendar.set(Calendar.MINUTE, minute);
			}
		}
		return calendar.getTimeInMillis();
	}

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case RENEW_REMAINS:
				long secsremains = (mSavedTime - System.currentTimeMillis()) / 1000;
				if (secsremains > 0) {
					mTimeRemains.setText(MusicUtils.makeTimeString(mContext,
							secsremains));
					mHandler.sendEmptyMessageDelayed(RENEW_REMAINS, 1000);
				} else {
					setButtonsNoAlarmExists();
				}
				break;
			}
		}
	};

	public HourMinuteDialog.onHourMinuteSetListener mButtonChangedListener = new HourMinuteDialog.onHourMinuteSetListener() {

		@Override
		public void onHourMinuteSet() {
			setPresetButtonTexts();
		}

	};

}
