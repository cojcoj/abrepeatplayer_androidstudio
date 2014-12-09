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

import com.kojimahome.music21.IMediaPlaybackService;
import com.kojimahome.music21.R;
import com.kojimahome.music21.IMediaPlaybackService.Stub;
import com.kojimahome.music21.R.id;
import com.kojimahome.music21.MusicUtils.ServiceToken;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.Debug;

public class MusicBrowserActivity extends Activity
    implements MusicUtils.Defs {

    private ServiceToken mToken;

    public MusicBrowserActivity() {
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle icicle) {
//    	if (LocalDebugMode.debugMode) {
//    		android.os.Debug.waitForDebugger();
//    	}
        super.onCreate(icicle);
        
        int activeTab = MusicUtils.getIntPref(this, "activetab", R.id.artisttab);
        if (activeTab != R.id.artisttab
                && activeTab != R.id.albumtab
                && activeTab != R.id.songtab
                && activeTab != R.id.playlisttab) {
            activeTab = R.id.artisttab;
        }
        MusicUtils.activateTab(this, activeTab);
        
        String shuf = getIntent().getStringExtra("autoshuffle");
        if ("true".equals(shuf)) {
            mToken = MusicUtils.bindToService(this, autoshuffle);
        }
//        try {
        try {
        	int versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        	int oldVersionCode = MusicUtils.getIntPref(this, "versioncode", 0);
        	if (oldVersionCode == 0) {
        		Intent intent = new Intent();
		        intent.setClass(this, WelcomeActivity.class);
		        startActivity(intent);
		        MusicUtils.setIntPref(this, "versioncode", versionCode);
        	} else if ((oldVersionCode/10) < (versionCode/10)) {
        		Intent intent = new Intent();
		        intent.setClass(this, NewFeaturesActivity.class);
		        startActivity(intent);
		        MusicUtils.setIntPref(this, "versioncode", versionCode);
        	}
		} catch (NameNotFoundException e) {
		}
    }

    @Override
    public void onDestroy() {
        if (mToken != null) {
            MusicUtils.unbindFromService(mToken);
        }
        super.onDestroy();
    }

    private ServiceConnection autoshuffle = new ServiceConnection() {
        public void onServiceConnected(ComponentName classname, IBinder obj) {
            // we need to be able to bind again, so unbind
            try {
                unbindService(this);
            } catch (IllegalArgumentException e) {
            }
            IMediaPlaybackService serv = IMediaPlaybackService.Stub.asInterface(obj);
            if (serv != null) {
                try {
                    serv.setShuffleMode(MediaPlaybackService.SHUFFLE_AUTO);
                } catch (RemoteException ex) {
                }
            }
        }

        public void onServiceDisconnected(ComponentName classname) {
        }
    };

}

