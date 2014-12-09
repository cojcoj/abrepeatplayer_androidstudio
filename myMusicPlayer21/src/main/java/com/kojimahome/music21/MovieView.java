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

import com.kojimahome.music21.R;








import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

/**
 * This activity plays a video from a specified URI.
 */
public class MovieView extends NoSearchActivity  {
    private static final String TAG = "MovieView";

  public final static int CHILD_MENU_BASE = 0;

  private static final int BROWSE_ABPOINTS = CHILD_MENU_BASE +1 ;
  private static final int HELP_MENU = CHILD_MENU_BASE +18;
  private static final int HELP_ABREPEAT = CHILD_MENU_BASE +19;
  private static final int HELP_ADJUST_AB = CHILD_MENU_BASE +20;
  private static final int HELP_JUMP = CHILD_MENU_BASE +21;
  private static final int HELP_BOOKMARK = CHILD_MENU_BASE +22;
  private static final int HELP_MANAGE = CHILD_MENU_BASE +23;
  private static final int DONATION = CHILD_MENU_BASE +24;
  private static final int SPEED = CHILD_MENU_BASE +25;
    
    private MovieViewControl mControl;
    private boolean mFinishOnCompletion;
    private boolean mResumed = false;  // Whether this activity has been resumed.
    private boolean mFocused = false;  // Whether this window has focus.
    private boolean mControlResumed = false;  // Whether the MovieViewControl is resumed.
    
    private static AlertDialog mDictDialog;
    

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.movie_view);
        View rootView = findViewById(R.id.root);
        Intent intent = getIntent();
        mControl = new MovieViewControl(rootView, this, intent.getData()) {
            @Override
            public void onCompletion() {
                if (mFinishOnCompletion) {
                    finish();
                }
            }
        };
        if (intent.hasExtra(MediaStore.EXTRA_SCREEN_ORIENTATION)) {
            int orientation = intent.getIntExtra(
                    MediaStore.EXTRA_SCREEN_ORIENTATION,
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            if (orientation != getRequestedOrientation()) {
                setRequestedOrientation(orientation);
            }
        }
        mFinishOnCompletion = intent.getBooleanExtra(
                MediaStore.EXTRA_FINISH_ON_COMPLETION, true);
        MyMediaController.clearAfterRelease();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

  		    menu.addSubMenu(0, HELP_MENU, 0,
  		    		R.string.ab_help_menu).setIcon(R.drawable.ic_menu_abpos_help);
  		  menu.add(0, DONATION, 0, R.string.ab_donation).setIcon(R.drawable.ic_menu_donation);
  		  menu.add(0, SPEED, 0, "SlowMotion").setIcon(R.drawable.ic_speed_1);
  		  

            return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        MenuItem item;
        
//        item = menu.findItem(HELP_MENU);
//        SubMenu sub = item.getSubMenu();
//        sub.clear();
//    	sub.add(0, HELP_ABREPEAT, 0, R.string.ab_help_ab_sub);
//    	sub.add(0, HELP_ADJUST_AB, 0, R.string.ab_help_adjust_ab_sub);
//    	sub.add(0, HELP_JUMP, 0, R.string.ab_help_jump_sub);
//    	sub.add(0, HELP_BOOKMARK, 0, R.string.ab_help_bookmark_sub);
//    	sub.add(0, HELP_MANAGE, 0, R.string.ab_help_manage_sub);
    	
    	item = menu.findItem(DONATION);
    	if (MusicUtils.donated(getApplicationContext())) {
    		item.setVisible(false);
    	} else {
    		item.setVisible(true);
    	}


        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        menu.setGroupVisible(1, !km.inKeyguardRestrictedInputMode());

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
            switch (item.getItemId()) {
                case HELP_MENU:
                	intent = new Intent();
                    intent.setClass(this, HelpListActivityVideo.class);
                    startActivity(intent);
                	break;
                case HELP_ABREPEAT:
//                	intent = new Intent();
//                    intent.setClass(this, ABHelpActivity.class);
//                    startActivity(intent);
                	break;
                case HELP_ADJUST_AB:
//                	intent = new Intent();
//                    intent.setClass(this, AdjustABHelpActivity.class);
//                    startActivity(intent);
                	break;
                case HELP_JUMP:
//                	intent = new Intent();
//                    intent.setClass(this, JumpHelpActivity.class);
//                    startActivity(intent);
                	break;
                case HELP_BOOKMARK:
//                	intent = new Intent();
//                    intent.setClass(this, BookmarkHelpActivity.class);
//                    startActivity(intent);
                	break;
                case HELP_MANAGE:
//                	intent = new Intent();
//                    intent.setClass(this, ManageListHelpActivity.class);
//                    startActivity(intent);
                	break;
                case DONATION:
                	try {
                	intent = new Intent(Intent.ACTION_VIEW);
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
                	break;
                case SPEED:
                	AlertDialog.Builder builder = new AlertDialog.Builder(this);
            		View vw = LayoutInflater.from(this).inflate(R.layout.dialog_layout,
            				null);
            		builder
            				.setIcon(R.drawable.ic_speed_1).setTitle("SlowMotion").setView(vw);

            		ListView lv = (ListView) vw.findViewById(R.id.listview);
            		ArrayAdapter<CharSequence> adapter = ArrayAdapter
            				.createFromResource(this, R.array.speed,
            						android.R.layout.simple_list_item_single_choice);

            		lv.setOnItemClickListener(mItemSelectedListner);
            		lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            		lv.setAdapter(adapter);
            		mDictDialog = builder.create();
            		mDictDialog.show();
            		lv.setItemChecked(mControl.getSpeedIdx(), true);
//            		lv.setItemChecked(mPreferences.getInt(DICTIONARY, DEFAULT_DICT), true);
                	break;

                
            }

        return super.onOptionsItemSelected(item);
    }
    
    AdapterView.OnItemClickListener mItemSelectedListner =
    		new AdapterView.OnItemClickListener() {

    			@Override
    			public void onItemClick(AdapterView<?> arg0, View arg1, int idx,
    					long arg3) {
    				int selected_speed = 1;
    				switch (idx) {
    				case 0:
    					selected_speed = 8;
    					break;
    				case 1:
    					selected_speed = 4;
    					break;
    				case 2:
    					selected_speed = 2;
    					break;
    				case 3:
    					selected_speed = 1;
    					break;
    				}
    				mControl.onSpeedSelected(selected_speed);
//    				Editor ed = mPreferences.edit();
//    	        	ed.putInt(DICTIONARY, idx);
//    	        	ed.commit();
//    	        	
//    	        	loadPage();

    	        	mDictDialog.hide();

    			}
    		
    	};
    
    @Override
    public void onPause() {
        super.onPause();
        mResumed = false;
        if (mControlResumed) {
            mControl.onPause();
            mControlResumed = false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mResumed = true;
        if (mFocused && mResumed && !mControlResumed) {
            mControl.onResume();
            mControlResumed = true;
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        mFocused = hasFocus;
        if (mFocused && mResumed && !mControlResumed) {
            mControl.onResume();
            mControlResumed = true;
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode != RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case BROWSE_ABPOINTS:
            	if (mControl != null) {
            	mControl.onAbPointsSelected(intent);
            	}
            	break;
        }
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
    	if (mControl != null) {
    		mControl.onConfigurationChanged();
    	}
    }
    
}
