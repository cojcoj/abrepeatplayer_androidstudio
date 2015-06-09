package com.kojimahome.music21;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

public class PrefsActivityVideo extends PreferenceActivity implements MusicUtils.Defs {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		PreferenceManager prefMgr = getPreferenceManager();
        prefMgr.setSharedPreferencesName(this.getPackageName());
        prefMgr.setSharedPreferencesMode(MODE_WORLD_READABLE);
		addPreferencesFromResource(R.xml.abrepeat_prefs_video);
        int orientation = MusicUtils.getIntPref(getApplicationContext(), VIDEO_SCRN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        switch (orientation) {
            case ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                break;
            case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
        }

        ListPreference listpref = (ListPreference) findPreference("video_scrn_lock_auto");
        listpref.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
//                        Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
////orientation will contain Surface.ROTATION_0 (no rotation), Surface.ROTATION_90, Surface.ROTATION_180 or Surface.ROTATION_270
//                        int x = Surface.ROTATION_0;
//                        int rotation = display.getRotation();
//                        Log.i("PrefsActivityVideo", "rotation:" + rotation);
//                        Log.i("PrefsActivityVideo", "selection:" + newValue.toString());
                        switch (Integer.valueOf(newValue.toString())) {
                            case ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED:
                                MusicUtils.setIntPref(getApplicationContext(), VIDEO_SCRN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                                break;
                            case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:
                                MusicUtils.setIntPref(getApplicationContext(), VIDEO_SCRN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                                break;
                            case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
                                MusicUtils.setIntPref(getApplicationContext(), VIDEO_SCRN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                                break;
                        }
                        return true;
                    }
                }

        );

	}

//    void setRotation (int rotation) {
//        switch (rotation) {
//            case ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED:
//                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
//                break;
//            case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
//                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//                break;
//            case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:
//                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//                break;
//            case ActivityInfo.SCREEN_ORIENTATION_USER:
//                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
//                break;
//            case ActivityInfo.SCREEN_ORIENTATION_BEHIND:
//                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_BEHIND);
//                break;
//        }
//    }

}
