/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kojimahome.music21;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import com.kojimahome.music21.R;

/**
 * 
 */
public class MediaButtonIntentReceiver extends BroadcastReceiver {

	private static final int MSG_LONGPRESS_TIMEOUT = 1;
	private static final int LONG_PRESS_DELAY = 1000;

	private static long mLastClickTime = 0;
	private static boolean mDown = false;
	private static Context mContext;
	private static boolean mPauseClicked = false;
	// private static boolean mLaunched = false;
	private static final String LOGTAG = "MediaButtonIntentReceiver";

	private static Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_LONGPRESS_TIMEOUT:
				Intent i = new Intent(mContext, MediaPlaybackService.class);
				i.setAction(MediaPlaybackService.SERVICECMD);
				// This is used to switch between AB Repeat and normal play
				// modes
				i.putExtra(MediaPlaybackService.CMDNAME,
						MediaPlaybackService.CMDSTOP);
				mContext.startService(i);
				// Log.i(LOGTAG, "LogPressTimeout");
				// if (!mLaunched) {
				// Context context = (Context)msg.obj;
				// Intent i = new Intent();
				// i.putExtra("autoshuffle", "true");
				// i.setClass(context, MusicBrowserActivity.class);
				// i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
				// Intent.FLAG_ACTIVITY_CLEAR_TOP);
				// context.startActivity(i);
				// mLaunched = true;
				// }
				break;
			}
		}
	};

	@Override
	public void onReceive(Context context, Intent intent) {
		String intentAction = intent.getAction();

		// KeyEvent ev = (KeyEvent)
		// intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
		// int kcode = ev.getKeyCode();
		// int act = ev.getAction();
		// String sAct = "";
		// String sCode = "";
		// if (act == 0) {
		// sAct = "DOWN ";
		// }
		// if (act == 1) {
		// sAct = "UP   ";
		// }
		// if (kcode == 85) {
		// sCode = "Pause ";
		// }
		// if (kcode == 86) {
		// sCode = "Stop  ";
		// }
		// if ( sAct.equals("")||sCode.equals("")) {
		// Log.i(LOGTAG, "onReceive: Others");
		// } else {
		// Log.i(LOGTAG, "onReceive: " + sCode + sAct);
		// }
		mContext = context;
		
		Log.i(LOGTAG,"intentAction:" + intentAction);

		if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intentAction)) {
			Intent i = new Intent(context, MediaPlaybackService.class);
			i.setAction(MediaPlaybackService.SERVICECMD);
			i.putExtra(MediaPlaybackService.CMDNAME,
					MediaPlaybackService.CMDPAUSE);
			context.startService(i);
		} else if (Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
			KeyEvent event = (KeyEvent) intent
					.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

			if (event == null) {
				return;
			}

			int keycode = event.getKeyCode();
			int action = event.getAction();
			long eventtime = event.getEventTime();

			// single quick press: pause/resume.
			// double press: next track
			// long press: start auto-shuffle mode.

			String command = null;
			switch (keycode) {
			case KeyEvent.KEYCODE_MEDIA_STOP:
				// Using this for toggle betweeen AB repeat and normal play
				// modes
				command = MediaPlaybackService.CMDSTOP;
				break;
			case 126: // KeyEvent.KEYCODE_MEDIA_PLAY:
			case 127: // KeyEvent.KEYCODE_MEDIA_PAUSE:
				keycode = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
			case KeyEvent.KEYCODE_HEADSETHOOK:
			case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
				command = MediaPlaybackService.CMDTOGGLEPAUSE;
				break;
			case KeyEvent.KEYCODE_MEDIA_NEXT:
				command = MediaPlaybackService.CMDNEXT;
				break;
			case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
				command = MediaPlaybackService.CMDPREVIOUS;
				break;
			}

			if (command != null) {
				if (action == KeyEvent.ACTION_DOWN) {
					if (mDown) {
//						 Log.i(LOGTAG, "mDown true in if");
						if (MediaPlaybackService.CMDTOGGLEPAUSE.equals(command)
								&& mLastClickTime != 0
								&& eventtime - mLastClickTime > LONG_PRESS_DELAY) {
							mHandler.sendMessage(mHandler.obtainMessage(
									MSG_LONGPRESS_TIMEOUT, context));
//							 Log.i(LOGTAG, "MsgSent to Handler");
						}
					} else {
//						 Log.i(LOGTAG, "mDown false");
						// if this isn't a repeat event

						// The service may or may not be running, but we need to
						// send it
						// a command.
						Intent i = new Intent(context,
								MediaPlaybackService.class);
						i.setAction(MediaPlaybackService.SERVICECMD);
						if (eventtime - mLastClickTime < 800) {
//							 Log.i(LOGTAG, MediaPlaybackService.CMDNEXT +
//							 " is sent to service -00");
							if (keycode == KeyEvent.KEYCODE_HEADSETHOOK) {
								i.putExtra(MediaPlaybackService.CMDNAME,
										MediaPlaybackService.CMDNEXT);
							} else if (keycode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
								if (MusicUtils.getBooleanPref(mContext, MusicUtils.Defs.DOUBLECLICK_MODE_SWITCH_BT, true)) {
									Intent i2 = new Intent(context,
											MediaPlaybackService.class);
									i2.setAction(MediaPlaybackService.SERVICECMD);
									i2.putExtra(MediaPlaybackService.CMDNAME,
											MediaPlaybackService.CMDSTOP);
									context.startService(i2);
								}
//								Log.i(LOGTAG, i2.getStringExtra(MediaPlaybackService.CMDNAME) +
//								 " is sent to service line176");
								i.putExtra(MediaPlaybackService.CMDNAME,
										MediaPlaybackService.CMDTOGGLEPAUSE);
							} 
//							Log.i(LOGTAG, i.getStringExtra(MediaPlaybackService.CMDNAME) +
//							 " is sent to service line179");
							context.startService(i);
							mLastClickTime = 0;
						} else {
//							 Log.i(LOGTAG, command +
//							 " is sent to service line185");
							i.putExtra(MediaPlaybackService.CMDNAME, command);
							context.startService(i);
							mLastClickTime = eventtime;
						}

						// mLaunched = false;
						mDown = true;
					}
				} else {
					mHandler.removeMessages(MSG_LONGPRESS_TIMEOUT);
					mDown = false;
//					 Log.i(LOGTAG, "MsgRemoved");
				}
				if (isOrderedBroadcast()) {
					abortBroadcast();
				}
			}
		}
	}
}
