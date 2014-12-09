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
import android.view.KeyEvent;

/**
 * 
 */
public class MediaButtonIntentReceiverVideo extends BroadcastReceiver {
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String intentAction = intent.getAction();
        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intentAction)) {
//        	Intent i = new Intent(context, MyMediaController.class);
        	Intent i = new Intent();
            i.setAction(MyMediaController.MEDIA_BUTTON_ACTION_VIDEO);
            i.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDTOGGLEPAUSE);
            context.sendBroadcast(i);
        } else if (Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
            KeyEvent event = (KeyEvent)
                    intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            
            if (event == null) {
                return;
            }

            int keycode = event.getKeyCode();
            int action = event.getAction();
            
            if ((keycode == KeyEvent.KEYCODE_HEADSETHOOK)
            		||(keycode == 126) // KeyEvent.KEYCODE_MEDIA_PLAY:
            		||(keycode == 127) // KeyEvent.KEYCODE_MEDIA_PAUSE:
            		||(keycode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)) {
            	if (action == KeyEvent.ACTION_DOWN) {
//                    Intent i = new Intent(context, MyMediaController.class);
                    Intent i = new Intent();
                    i.setAction(MyMediaController.MEDIA_BUTTON_ACTION_VIDEO);
                    i.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDTOGGLEPAUSE);
                    context.sendBroadcast(i);
	            }
	            if (isOrderedBroadcast()) {
	                abortBroadcast();
	            }
            }

        }
    }
}
