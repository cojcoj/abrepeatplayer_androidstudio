package com.kojimahome.music21;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class PrefsActivityVideo extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		PreferenceManager prefMgr = getPreferenceManager();
        prefMgr.setSharedPreferencesName(this.getPackageName());
        prefMgr.setSharedPreferencesMode(MODE_WORLD_READABLE);
		addPreferencesFromResource(R.xml.abrepeat_prefs_video);
	}

}
