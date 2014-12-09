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

import java.util.Formatter;
import java.util.Locale;
import java.io.IOException;
import java.text.*;

import wei.mark.standout.StandOutWindow;

import com.kojimahome.music21.IMediaPlaybackService;
import com.kojimahome.music21.R;
import com.kojimahome.music21.IMediaPlaybackService.Stub;
import com.kojimahome.music21.R.drawable;
import com.kojimahome.music21.R.id;
import com.kojimahome.music21.R.layout;
import com.kojimahome.music21.R.string;
import com.kojimahome.music21.MusicUtils.Defs;
import com.kojimahome.music21.MusicUtils.ServiceToken;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;
//import com.musixmatch.lyrics.musiXmatchLyricsConnector;



import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.text.Layout;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;


public class MediaPlaybackActivity extends Activity implements MusicUtils.Defs,
    View.OnTouchListener, View.OnLongClickListener
{
	private static final String LOGTAG = "MediaPlaybackActivity";
    private static final int USE_AS_RINGTONE = CHILD_MENU_BASE;
//    private static final int ADJUST_ABPOINTS = CHILD_MENU_BASE + 1;
    private static final int ADJUST_APOINT = CHILD_MENU_BASE +2;
    private static final int ADJUST_BPOINT = CHILD_MENU_BASE +3;
    private static final int RECORD_ABPOINTS = CHILD_MENU_BASE +4;
    private static final int BROWSE_ABPOINTS = CHILD_MENU_BASE +5;
    private static final int HELP_MENU = CHILD_MENU_BASE +18;
    private static final int HELP_ABREPEAT = CHILD_MENU_BASE +19;
    private static final int HELP_ADJUST_AB = CHILD_MENU_BASE +20;
    private static final int HELP_INTERVAL_AB = CHILD_MENU_BASE +21;
    private static final int HELP_JUMP = CHILD_MENU_BASE +22;
    private static final int HELP_BOOKMARK = CHILD_MENU_BASE +23;
    private static final int HELP_MANAGE = CHILD_MENU_BASE +24;
    private final static int LYRIC = CHILD_MENU_BASE +25;
    private final static int LYRIC_OPTIONS = CHILD_MENU_BASE +26;
    public static final long CHECK_BPOINT_DURATION = 3000;
    
    private static Context mContext;
    private boolean mOneShot = false;
    private boolean mSeeking = false;
    private boolean mDeviceHasDpad;
    private long mStartSeekPos = 0;
    private long mLastSeekEventTime;
    private IMediaPlaybackService mService = null;
    private RepeatingImageButton mPrevButton;
    private ImageButton mPauseButton;
    private RepeatingImageButton mNextButton;
    private ImageButton mAbRepeatingButton;
    private Button mJumpButtonCenter;
    private ImageButton mRepeatButton;
    private ImageButton mShuffleButton;
    private ImageButton mQueueButton;
    private Worker mAlbumArtWorker;
    private AlbumArtHandler mAlbumArtHandler;
    private Toast mToast;
    private int mTouchSlop;
    private ServiceToken mToken;
    private ABDbAdapter mABDbHelper;
    private Intent mABPickResultIntent = null;
    private SharedPreferences mPreferences;
    private boolean mPickingABPos = false;
    private boolean mABWideBar = true;
    private static long mLastPauseClick = 0;
    
//    private musiXmatchLyricsConnector lyricsPlugin= null;

    public MediaPlaybackActivity()
    {
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle)
    {
//    	if (LocalDebugMode.debugMode) {
//    		android.os.Debug.waitForDebugger();
//    	}
        super.onCreate(icicle);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        
//        if (lyricsPlugin == null) {
//	        lyricsPlugin = new musiXmatchLyricsConnector(this);
//	        lyricsPlugin.setTestMode(true);
//        }
        mContext = this;
        
        mAlbumArtWorker = new Worker("album art worker");
        mAlbumArtHandler = new AlbumArtHandler(mAlbumArtWorker.getLooper());
        
        mABDbHelper = new ABDbAdapter(this);
        
        mPreferences = getSharedPreferences("ABRepeat", MODE_PRIVATE);
        if (mPreferences.getBoolean("abinitialzed", false) == false) {
        	Editor ed = mPreferences.edit();
        	ed.putLong("jumpdistone", 7500);
        	ed.putLong("jumpdisttwo", 5000);
        	ed.putLong("jumpdistthree", 2500);
        	ed.putLong("abshiftone", 2000);
        	ed.putLong("abshifttwo", 1000);
        	ed.putLong("abshiftthree", 200);
        	ed.putBoolean("abwidebar", true);
        	ed.putBoolean("abinitialzed", true);
        	ed.commit();
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.audio_player);

        mCurrentTime = (TextView) findViewById(R.id.currenttime);
        mTotalTime = (TextView) findViewById(R.id.totaltime);
        mAPosTime = (Button) findViewById(R.id.apostime);
        mBPosTime = (Button) findViewById(R.id.bpostime);
        mProgress = (ProgressBar) findViewById(android.R.id.progress);
        mAlbum = (ImageView) findViewById(R.id.album);
        mArtistName = (TextView) findViewById(R.id.artistname);
        mAlbumName = (TextView) findViewById(R.id.albumname);
        mTrackName = (TextView) findViewById(R.id.trackname);

        View v = (View)mArtistName.getParent(); 
        v.setOnTouchListener(this);
        v.setOnLongClickListener(this);

        v = (View)mAlbumName.getParent();
        v.setOnTouchListener(this);
        v.setOnLongClickListener(this);

        v = (View)mTrackName.getParent();
        v.setOnTouchListener(this);
        v.setOnLongClickListener(this);
        
        mPrevButton = (RepeatingImageButton) findViewById(R.id.prev);
        mPrevButton.setOnClickListener(mPrevListener);
        mPrevButton.setRepeatListener(mRewListener, 260);
//        mPrevButton.getDrawable().setColorFilter(0xFF00FFFF, PorterDuff.Mode.MULTIPLY);
        mPauseButton = (ImageButton) findViewById(R.id.pause);
        mPauseButton.requestFocus();
        mPauseButton.setOnClickListener(mPauseListener);
        mPauseButton.setOnLongClickListener(mPauseLongListener);
        mAbRepeatingButton = (ImageButton) findViewById(R.id.abrepeat);
        mAbRepeatingButton.requestFocus();
        mAbRepeatingButton.setImageResource(R.drawable.apoint_or_bookmark);
        mAbRepeatingButton.setOnClickListener(mAbRepeatListener);
        mAbRepeatingButton.setLongClickable(true);
        mAbRepeatingButton.setOnLongClickListener(mAbRepeatingLongListner);
        mNextButton = (RepeatingImageButton) findViewById(R.id.next);
        mNextButton.setOnClickListener(mNextListener);
        mNextButton.setRepeatListener(mFfwdListener, 260);
        seekmethod = 1;
        
        
        
        mJumpButtonCenter = (Button) findViewById(R.id.jumpbuttoncenter);
        
        mJumpButtonCenter.setClickable(true);
        mJumpButtonCenter.setOnClickListener(mJumpButtonCenterClickListner);
        mJumpButtonCenter.setLongClickable(true);
        mJumpButtonCenter.setOnLongClickListener(mJumpButtonCenterLongListner);
        mAPosTime.setClickable(true);
        mAPosTime.setOnClickListener(mAPosTimeClickListner);
        mAPosTime.setLongClickable(true);
        mAPosTime.setOnLongClickListener(mAPosTimeLongListner);
        mBPosTime.setClickable(true);
        mBPosTime.setOnClickListener(mBPosTimeClickListner);
        mBPosTime.setLongClickable(true);
        mBPosTime.setOnLongClickListener(mBPosTimeLongListner);   
        
        setJumpButtonContents();
        
        mDeviceHasDpad = (getResources().getConfiguration().navigation ==
            Configuration.NAVIGATION_DPAD);
        
        mQueueButton = (ImageButton) findViewById(R.id.curplaylist);
        mQueueButton.setOnClickListener(mQueueListener);
        mShuffleButton = ((ImageButton) findViewById(R.id.shuffle));
        mShuffleButton.setOnClickListener(mShuffleListener);
        mRepeatButton = ((ImageButton) findViewById(R.id.repeat));
        mRepeatButton.setOnClickListener(mRepeatListener);
        
        if (mProgress instanceof SeekBar) {
            SeekBar seeker = (SeekBar) mProgress;
            seeker.setOnSeekBarChangeListener(mSeekListener);
        }
        mProgress.setMax(1000);
        
        if (icicle != null) {
            mOneShot = icicle.getBoolean("oneshot");
        } else {
            mOneShot = getIntent().getBooleanExtra("oneshot", false);
        }

        mTouchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
    }
    
    int mInitialX = -1;
    int mLastX = -1;
    int mTextWidth = 0;
    int mViewWidth = 0;
    boolean mDraggingLabel = false;
    
    TextView textViewForContainer(View v) {
        View vv = v.findViewById(R.id.artistname);
        if (vv != null) return (TextView) vv;
        vv = v.findViewById(R.id.albumname);
        if (vv != null) return (TextView) vv;
        vv = v.findViewById(R.id.trackname);
        if (vv != null) return (TextView) vv;
        return null;
    }
    
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        TextView tv = textViewForContainer(v);
        if (tv == null) {
            return false;
        }
        if (action == MotionEvent.ACTION_DOWN) {
            v.setBackgroundColor(0xff606060);
            mInitialX = mLastX = (int) event.getX();
            mDraggingLabel = false;
        } else if (action == MotionEvent.ACTION_UP ||
                action == MotionEvent.ACTION_CANCEL) {
            v.setBackgroundColor(0);
            if (mDraggingLabel) {
                Message msg = mLabelScroller.obtainMessage(0, tv);
                mLabelScroller.sendMessageDelayed(msg, 1000);
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (mDraggingLabel) {
                int scrollx = tv.getScrollX();
                int x = (int) event.getX();
                int delta = mLastX - x;
                if (delta != 0) {
                    mLastX = x;
                    scrollx += delta;
                    if (scrollx > mTextWidth) {
                        // scrolled the text completely off the view to the left
                        scrollx -= mTextWidth;
                        scrollx -= mViewWidth;
                    }
                    if (scrollx < -mViewWidth) {
                        // scrolled the text completely off the view to the right
                        scrollx += mViewWidth;
                        scrollx += mTextWidth;
                    }
                    tv.scrollTo(scrollx, 0);
                }
                return true;
            }
            int delta = mInitialX - (int) event.getX();
            if (Math.abs(delta) > mTouchSlop) {
                // start moving
                mLabelScroller.removeMessages(0, tv);
                
                // Only turn ellipsizing off when it's not already off, because it
                // causes the scroll position to be reset to 0.
                if (tv.getEllipsize() != null) {
                    tv.setEllipsize(null);
                }
                Layout ll = tv.getLayout();
                // layout might be null if the text just changed, or ellipsizing
                // was just turned off
                if (ll == null) {
                    return false;
                }
                // get the non-ellipsized line width, to determine whether scrolling
                // should even be allowed
                mTextWidth = (int) tv.getLayout().getLineWidth(0);
                mViewWidth = tv.getWidth();
                if (mViewWidth > mTextWidth) {
                    tv.setEllipsize(TruncateAt.END);
                    v.cancelLongPress();
                    return false;
                }
                mDraggingLabel = true;
                tv.setHorizontalFadingEdgeEnabled(true);
                v.cancelLongPress();
                return true;
            }
        }
        return false; 
    }

    Handler mLabelScroller = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            TextView tv = (TextView) msg.obj;
            int x = tv.getScrollX();
            x = x * 3 / 4;
            tv.scrollTo(x, 0);
            if (x == 0) {
                tv.setEllipsize(TruncateAt.END);
            } else {
                Message newmsg = obtainMessage(0, tv);
                mLabelScroller.sendMessageDelayed(newmsg, 15);
            }
        }
    };
    
    public boolean onLongClick(View view) {

        CharSequence title = null;
        String mime = null;
        String query = null;
        String artist;
        String album;
        String song;
        long audioid;
        
        try {
            artist = mService.getArtistName();
            album = mService.getAlbumName();
            song = mService.getTrackName();
            audioid = mService.getAudioId();
        } catch (RemoteException ex) {
            return true;
        } catch (NullPointerException ex) {
            // we might not actually have the service yet
            return true;
        }

//        if (MediaStore.UNKNOWN_STRING.equals(album) &&
//                MediaStore.UNKNOWN_STRING.equals(artist) &&  
        if (       
                song != null &&
                song.startsWith("recording")) {
            // not music
            return false;
        }

        if (audioid < 0) {
            return false;
        }

        Cursor c = MusicUtils.query(this,
                ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, audioid),
                new String[] {MediaStore.Audio.Media.IS_MUSIC}, null, null, null);
        boolean ismusic = true;
        if (c != null) {
            if (c.moveToFirst()) {
                ismusic = c.getInt(0) != 0;
            }
            c.close();
        }
        if (!ismusic) {
            return false;
        }

//        boolean knownartist =
//            (artist != null) && !MediaStore.UNKNOWN_STRING.equals(artist);
//
//        boolean knownalbum =
//            (album != null) && !MediaStore.UNKNOWN_STRING.equals(album);
        
        boolean knownartist =
            (artist != null);

        boolean knownalbum =
            (album != null);
        
        if (knownartist && view.equals(mArtistName.getParent())) {
            title = artist;
            query = artist;
            mime = MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE;
        } else if (knownalbum && view.equals(mAlbumName.getParent())) {
            title = album;
            if (knownartist) {
                query = artist + " " + album;
            } else {
                query = album;
            }
            mime = MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE;
        } else if (view.equals(mTrackName.getParent()) || !knownartist || !knownalbum) {
//            if ((song == null) || MediaStore.UNKNOWN_STRING.equals(song)) {
//                // A popup of the form "Search for null/'' using ..." is pretty
//                // unhelpful, plus, we won't find any way to buy it anyway.
//                return true;
//            }

            title = song;
            if (knownartist) {
                query = artist + " " + song;
            } else {
                query = song;
            }
            mime = "audio/*"; // the specific type doesn't matter, so don't bother retrieving it
        } else {
            throw new RuntimeException("shouldn't be here");
        }
        title = getString(R.string.mediasearch, title);

        Intent i = new Intent();
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.setAction(MediaStore.INTENT_ACTION_MEDIA_SEARCH);
        i.putExtra(SearchManager.QUERY, query);
        if(knownartist) {
            i.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, artist);
        }
        if(knownalbum) {
            i.putExtra(MediaStore.EXTRA_MEDIA_ALBUM, album);
        }
        i.putExtra(MediaStore.EXTRA_MEDIA_TITLE, song);
        i.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, mime);

        startActivity(Intent.createChooser(i, title));
        return true;
    }

    private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
        public void onStartTrackingTouch(SeekBar bar) {
            mLastSeekEventTime = 0;
            mFromTouch = true;
//            try {
//            	mService.clearABPos();
//            	setAbRepeatingButtonImage();
//            } catch (RemoteException ex) {
//            }
        }
        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (!fromuser || (mService == null)) return;
            long now = SystemClock.elapsedRealtime();
            if ((now - mLastSeekEventTime) > 250) {
                mLastSeekEventTime = now;
//                mPosOverride = mDuration * progress / 1000;
                try {
                	long aPosition = mService.getAPos();
                	long bPosition = mService.getBPos(); 
                	int abStatus = mService.abRepeatingState();
                	if ((abStatus == MediaPlaybackService.ABREPEATING_NOW) && mABWideBar) {
                    	mPosOverride = aPosition + ((bPosition - aPosition) * progress / 1000);
                    } else {
                    	mPosOverride = mDuration * progress / 1000;
                    }
                	switch (abStatus) {
                		case MediaPlaybackService.ABREPEATING_NOW:
                			if ((mPosOverride < aPosition) || (mPosOverride > bPosition)) {
                				mService.clearABPos();
                            	setAbRepeatingButtonImage();
                            	setJumpButtonContents();
                            	showToast(R.string.abpos_cleared);
                			}
                			break;
                		case MediaPlaybackService.ABREPEATING_WAITING:
//                			if (mPosOverride < aPosition) {
//                				mService.clearABPos();
//                            	setAbRepeatingButtonImage();
//                            	setJumpButtonContents();
//                            	showToast(R.string.abpos_cleared);
//                			}
                			break;
                		case MediaPlaybackService.ABREPEATING_NOT:
                	}
                    mService.seek(mPosOverride);
                } catch (RemoteException ex) {
                }

                // trackball event, allow progress updates
                if (!mFromTouch) {
                    refreshNow();
                    mPosOverride = -1;
                }
            }
        }
        public void onStopTrackingTouch(SeekBar bar) {
            mPosOverride = -1;
            mFromTouch = false;
        }
    };
    
    private View.OnClickListener mQueueListener = new View.OnClickListener() {
        public void onClick(View v) {
            startActivity(
                    new Intent(Intent.ACTION_EDIT)
                    .setClass(MediaPlaybackActivity.this, TrackBrowserActivity.class )
                    .setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track")
                    .putExtra("playlist", "nowplaying")
            );
        }
    };
    
    private View.OnClickListener mShuffleListener = new View.OnClickListener() {
        public void onClick(View v) {
            toggleShuffle();
        }
    };

    private View.OnClickListener mRepeatListener = new View.OnClickListener() {
        public void onClick(View v) {
            cycleRepeat();
        }
    };

    private View.OnClickListener mPauseListener = new View.OnClickListener() {
        public void onClick(View v) {        	
            doPauseResume();
            long currenttime = System.currentTimeMillis();
//            Log.i(LOGTAG, "currenttime - mLastPauseClick:" + (currenttime - mLastPauseClick));
            if (currenttime - mLastPauseClick < 900) {
//            	if (DOUBLECLICK_MODE_SWITCH) switchModes();
            	if (MusicUtils.getBooleanPref(mContext, DOUBLECLICK_MODE_SWITCH_MAIN, false)) switchModes();
            }
            mLastPauseClick = currenttime;
        }
    };
    
    private View.OnLongClickListener mPauseLongListener = new View.OnLongClickListener() {
    	@Override
        public boolean onLongClick(View v) {
    		switchModes();
            return true;
        }
    };
    
    private void switchModes () {
    	try {
    		
    		if (mService.abListTraverseMode()) {
    			mService.setAbListTraverseMode(false);
    			mService.clearABPos();
    			MusicUtils.playAlert(mContext);
        		showToast(R.string.abpos_cleared);
    		} else {
        		if (mService.currentAbPoints()) {
        			mService.setAbListTraverseMode(true);
	    			MusicUtils.playAlert(mContext,"canopus");
	    			showToast(R.string.abmode_set);
	    		} else {
	    			MusicUtils.playAlert(mContext);   
	    			showToast(R.string.abmode_not_set);
	    		}
    		}
    		
		} catch (RemoteException ex) {
        }
    }
    
    private View.OnClickListener mAbRepeatListener = new View.OnClickListener() {
        public void onClick(View v) {
        
        	String durationformat;
        	StringBuilder sFormatBuilder = new StringBuilder();
        	Formatter sFormatter = new Formatter(sFormatBuilder, Locale.getDefault());
        	Object[] sTimeArgs = new Object[5];
        	final Object[] timeArgs = sTimeArgs;
        	Long apos;
        	Long bpos;
        	
        	
        	try {
        		if (mService != null) {
                switch (mService.abRepeatingState()) {
                	case MediaPlaybackService.ABREPEATING_NOT:
                		mAbRepeatingButton.setImageResource(R.drawable.bpoint);
                		mService.setAPos();
                		apos = mService.getAPos()/1000;
                		durationformat = 
                           apos < 3600 ? "%2$d:%5$02d" : "%1$d:%3$02d:%5$02d";
                		sFormatBuilder.setLength(0);
                        timeArgs[0] = apos / 3600;
                        timeArgs[1] = apos / 60;
                        timeArgs[2] = (apos / 60) % 60;
                        timeArgs[3] = apos;
                        timeArgs[4] = apos % 60;
                		mAPosTime.setText(sFormatter.format(durationformat, timeArgs).toString());
                		mAPosTime
                		.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.apostime_or_bookmark),
                				null, null);
                		mAPosTime.setVisibility(View.VISIBLE);
                		mJumpButtonCenter
                		.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.ic_bookmark_flat), null, null);
                		mJumpButtonCenter.setText("B Mark");
//                		mJumpButtonCenter.setVisibility(View.INVISIBLE);
                		mBPosTime
                		.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.cancel_flat), null, null);
                		mBPosTime.setText("Cancel");
                		mBPosTime.setVisibility(View.VISIBLE);
                		break;
                	case MediaPlaybackService.ABREPEATING_WAITING:
                		if (mService.setBPos()) {
                			mService.setABPause(mPreferences.getLong("abpause", 0));
                			mAbRepeatingButton.setImageResource(R.drawable.delpoints);
                    		bpos = mService.getBPos()/1000;
                    		durationformat = 
                               bpos < 3600 ? "%2$d:%5$02d" : "%1$d:%3$02d:%5$02d";
                    		sFormatBuilder.setLength(0);
                            timeArgs[0] = bpos / 3600;
                            timeArgs[1] = bpos / 60;
                            timeArgs[2] = (bpos / 60) % 60;
                            timeArgs[3] = bpos;
                            timeArgs[4] = bpos % 60;
                    		mBPosTime.setText(sFormatter.format(durationformat, timeArgs).toString());
                    		mBPosTime
                    		.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.bpostime),
                    				null, null);
                    		mBPosTime.setVisibility(View.VISIBLE);
                    		mJumpButtonCenter.setText("AB");
                    		mJumpButtonCenter
                    		.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.record),
                    				null, null);
                    		mAPosTime
                    		.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.apostime),
                    				null, null);
                    		mJumpButtonCenter.setVisibility(View.VISIBLE);
                    		
                			
                		} else {
                            showToast(R.string.startendtooclose);
                		}
                		break;
                	case MediaPlaybackService.ABREPEATING_NOW:
                		mService.clearABPos();
                		mAbRepeatingButton.setImageResource(R.drawable.apoint_or_bookmark);
                		setJumpButtonContents();
                		showToast(R.string.abpos_cleared);
//                		mAPosTime.setVisibility(View.INVISIBLE);
//                		mBPosTime.setVisibility(View.INVISIBLE);
                		break;
                	default:
                		mAbRepeatingButton.setImageResource(R.drawable.apoint_or_bookmark);
                }}
            } catch (RemoteException ex) {
            }

        }
    };
    
    private View.OnLongClickListener mAbRepeatingLongListner = new View.OnLongClickListener() {
		
		@Override
		public boolean onLongClick(View v) {
			try {
				if (mService != null) {
					switch (mService.abRepeatingState()) {
	            	case MediaPlaybackService.ABREPEATING_NOT:
	            		showAbList();
	        			break;	
	            	case MediaPlaybackService.ABREPEATING_WAITING:
	            		showAbList();
	            		break;
	            	case MediaPlaybackService.ABREPEATING_NOW:
	            		showAbList();	            		
	            		break;
					}
			    }
				} catch (RemoteException ex) {
				}
			return true;
    	}
    	
    };
    
    private View.OnClickListener mJumpButtonCenterClickListner = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			String mWhereClause = "";
			try {
			if (mService != null) {
				if (mService.getAbEdited()&&(mService.getAbRecRowId()==-1)) {
        			mService.setAbEdited(false);
        		}
				AlertDialog.Builder mybuilder;
				switch (mService.abRepeatingState()) {
            	case MediaPlaybackService.ABREPEATING_NOT:
            		long jumpdist = mPreferences.getLong("jumpdisttwo", 5000);
        			mService.jump(jumpdist);
        			break;	
            	case MediaPlaybackService.ABREPEATING_WAITING:
            		mABDbHelper.open();
            		
            		mybuilder = new AlertDialog.Builder(mContext);
            		if (MusicUtils.getBooleanPref(mContext, ASK_IF_OVERWRITE, true)&&mService.getAbEdited()) {
            			mybuilder.setTitle(R.string.ab_edited);
                    	mybuilder.setMessage(R.string.ab_update_or_new);
                    	mybuilder.setPositiveButton(R.string.ab_update, new android.content.DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								try {
									mABDbHelper.updateABPos(mService.getAbRecRowId(), mService.getAPos(), mService.getBPos());
									mService.setAbEdited(false);
				                	mABDbHelper.close();
		                    	sendDbModifiedtoService();
		                    	showToast(R.string.abpos_updated);
								} catch (Exception e) {
									
								}
								
							}});
                    	mybuilder.setNeutralButton(R.string.ab_new, new android.content.DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								try {
									long rowid = mABDbHelper.createABPos(mAPosTime.getText()+" "+mBPosTime.getText(),
				                			mService.getAPos(), mService.getBPos(), mService.getPath(), 
				                			mService.getTrackName(), mService.duration());
									mService.setAbRecRowId(rowid);
									mService.setAbEdited(false);
				                	mABDbHelper.close();
		                    	sendDbModifiedtoService();
		                    	showToast(R.string.abpos_recorded);
								} catch (Exception e) {
									
								}
								
							}});
                    	
//                    	mybuilder.setIcon(R.drawable.ic_dialog_time);
                    	mybuilder.setNegativeButton(android.R.string.cancel, null);
                    	mybuilder.setCancelable(true);
                    	mybuilder.show();
            		} else {
            			mWhereClause = ABDbAdapter.KEY_MUSICTITLE + "=\"" + mService.getTrackName() + "\""
                        		+ " AND " + ABDbAdapter.KEY_MUSICDURATION + ">=" + Long.toString(mService.duration() - 1) 
                        		+ " AND " + ABDbAdapter.KEY_MUSICDURATION + "<=" + Long.toString(mService.duration() + 1)
    		            		+ " AND " + ABDbAdapter.KEY_APOS + "=" + Long.toString(mService.getAPos())
    		            		+ " AND " + ABDbAdapter.KEY_BPOS + "=" + Long.toString(0);
                    	
                    	if (MusicUtils.getBooleanPref(mContext, ASK_IF_DUPLICATED, true)&&mABDbHelper.checkIfABPosExists(mWhereClause)) {
                    		mybuilder = new AlertDialog.Builder(mContext);
                    		mybuilder.setTitle(R.string.same_ab_exists);
                        	mybuilder.setMessage(R.string.want_to_continue);
                        	mybuilder.setPositiveButton(android.R.string.yes, new android.content.DialogInterface.OnClickListener() {
    							@Override
    							public void onClick(DialogInterface dialog,
    									int which) {
    								try {
    								long rowid = mABDbHelper.createABPos("Bookmark@ "+mAPosTime.getText(),
    		                    			mService.getAPos(), (long) 0, mService.getPath(), 
    		                    			mService.getTrackName(), mService.duration());
    								mService.setAbRecRowId(rowid);
    								mService.setAbEdited(false);
    		                    	mABDbHelper.close();
    		                    	sendDbModifiedtoService();
    		                    	mService.clearABPos();
    		                        setAbRepeatingButtonImage();
    		                        setJumpButtonContents();
    		                        showToast(R.string.bookmark_recorded);
    								} catch (Exception e) {
    									
    								}
    								
    							}});
//                        	mybuilder.setIcon(R.drawable.ic_dialog_time);
                        	mybuilder.setNegativeButton(android.R.string.cancel, null);
                        	mybuilder.setCancelable(true);
                        	mybuilder.show();
                    		
                    	} else {
                        	long rowid = mABDbHelper.createABPos("Bookmark@ "+mAPosTime.getText(),
                        			mService.getAPos(), (long) 0, mService.getPath(), 
                        			mService.getTrackName(), mService.duration());
                        	mService.setAbRecRowId(rowid);
    						mService.setAbEdited(false);
                        	mABDbHelper.close();
                        	sendDbModifiedtoService();
                        	mService.clearABPos();
                            setAbRepeatingButtonImage();
                            setJumpButtonContents();
                            showToast(R.string.bookmark_recorded);
                    	}
            		}
            		break;
            	case MediaPlaybackService.ABREPEATING_NOW:
            		mABDbHelper.open();
            		mybuilder = new AlertDialog.Builder(mContext);
            		if (MusicUtils.getBooleanPref(mContext, ASK_IF_OVERWRITE, true)&&mService.getAbEdited()) {
            			mybuilder.setTitle(R.string.ab_edited);
                    	mybuilder.setMessage(R.string.ab_update_or_new);
                    	mybuilder.setPositiveButton(R.string.ab_update, new android.content.DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								try {
									mABDbHelper.updateABPos(mService.getAbRecRowId(), mService.getAPos(), mService.getBPos());
									mService.setAbEdited(false);
				                	mABDbHelper.close();
//		                    	sendDbModifiedtoService();
		                    	showToast(R.string.abpos_updated);
								} catch (Exception e) {
									
								}
								
							}});
                    	mybuilder.setNeutralButton(R.string.ab_new, new android.content.DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								try {
									long rowid = mABDbHelper.createABPos(mAPosTime.getText()+" "+mBPosTime.getText(),
				                			mService.getAPos(), mService.getBPos(), mService.getPath(), 
				                			mService.getTrackName(), mService.duration());
									mService.setAbRecRowId(rowid);
									mService.setAbEdited(false);
				                	mABDbHelper.close();
		                    	sendDbModifiedtoService();
		                    	showToast(R.string.abpos_recorded);
								} catch (Exception e) {
									
								}
								
							}});
                    	
//                    	mybuilder.setIcon(R.drawable.ic_dialog_time);
                    	mybuilder.setNegativeButton(android.R.string.cancel, null);
                    	mybuilder.setCancelable(true);
                    	mybuilder.show();
            		} else {
            			mWhereClause = ABDbAdapter.KEY_MUSICTITLE + "=\"" + mService.getTrackName() + "\""
                        		+ " AND " + ABDbAdapter.KEY_MUSICDURATION + ">=" + Long.toString(mService.duration() - 1) 
                        		+ " AND " + ABDbAdapter.KEY_MUSICDURATION + "<=" + Long.toString(mService.duration() + 1)
    		            		+ " AND " + ABDbAdapter.KEY_APOS + "=" + Long.toString(mService.getAPos())
    		            		+ " AND " + ABDbAdapter.KEY_BPOS + "=" + Long.toString(mService.getBPos());
                    	
                    	if (MusicUtils.getBooleanPref(mContext, ASK_IF_DUPLICATED, true)&&mABDbHelper.checkIfABPosExists(mWhereClause)) {
                    		mybuilder.setTitle(R.string.same_ab_exists);
                        	mybuilder.setMessage(R.string.want_to_continue);
                        	mybuilder.setPositiveButton(android.R.string.yes, new android.content.DialogInterface.OnClickListener() {
    							@Override
    							public void onClick(DialogInterface dialog,
    									int which) {
    								try {
    									long rowid = mABDbHelper.createABPos(mAPosTime.getText()+" "+mBPosTime.getText(),
    				                			mService.getAPos(), mService.getBPos(), mService.getPath(), 
    				                			mService.getTrackName(), mService.duration());
    									mService.setAbRecRowId(rowid);
    									mService.setAbEdited(false);
    				                	mABDbHelper.close();
    		                    	sendDbModifiedtoService();
    		                    	showToast(R.string.abpos_recorded);
    								} catch (Exception e) {
    									
    								}
    								
    							}});
//                        	mybuilder.setIcon(R.drawable.ic_dialog_time);
                        	mybuilder.setNegativeButton(android.R.string.cancel, null);
                        	mybuilder.setCancelable(true);
                        	mybuilder.show();
                    		
                    	} else {
                    		long rowid = mABDbHelper.createABPos(mAPosTime.getText()+" "+mBPosTime.getText(),
                        			mService.getAPos(), mService.getBPos(), mService.getPath(), 
                        			mService.getTrackName(), mService.duration());
                        	mABDbHelper.close();
                        	mService.setAbRecRowId(rowid);
							mService.setAbEdited(false);
                        	sendDbModifiedtoService();
                        	showToast(R.string.abpos_recorded);
                    	}
            		}
            		break;
				}
		    }
			} catch (RemoteException ex) {
          }
			
		}
	};
    
    private View.OnClickListener mAPosTimeClickListner = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			try {
			if (mService != null) {
				switch (mService.abRepeatingState()) {
            	case MediaPlaybackService.ABREPEATING_NOT:
            		long jumpdist = mPreferences.getLong("jumpdistone", 7500);
        			mService.jump(jumpdist);
        			break;	
            	case MediaPlaybackService.ABREPEATING_WAITING:
            		long ap = mService.getAPos();
            		mService.seek(ap);
            		break;
            	case MediaPlaybackService.ABREPEATING_NOW:
//            		long jumpdist2 = mPreferences.getLong("jumpdisttwo", 5000);
            		new ABShiftPickerDialog(v.getContext(),
            				mAShiftDistSetListener,
                            R.string.adjust_apoint, R.drawable.apoint, true).show();
            		break;
				}
		    }
			} catch (RemoteException ex) {
          }
			
		}
	};
    
    private View.OnClickListener mBPosTimeClickListner = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			try {
			if (mService != null) {
				switch (mService.abRepeatingState()) {
            	case MediaPlaybackService.ABREPEATING_NOT:
            		long jumpdist = mPreferences.getLong("jumpdistthree", 2500);
        			mService.jump(jumpdist);
        			break;	
            	case MediaPlaybackService.ABREPEATING_WAITING:
            		mService.clearABPos();
            		mAbRepeatingButton.setImageResource(R.drawable.apoint_or_bookmark);
            		setJumpButtonContents();
            		showToast(R.string.abpos_cleared);
            		break;
            	case MediaPlaybackService.ABREPEATING_NOW:
            		new ABShiftPickerDialog(v.getContext(),
            				mBShiftDistSetListener,
                            R.string.adjust_bpoint, R.drawable.bpoint).show();
            		break;
				}
		    }
			} catch (RemoteException ex) {
          }
			
		}
	};
	
    private View.OnLongClickListener mJumpButtonCenterLongListner = new View.OnLongClickListener() {
		
		@Override
		public boolean onLongClick(View v) {
			boolean consumed = false;
			try {
				if (mService != null) {
					switch (mService.abRepeatingState()) {
	            	case MediaPlaybackService.ABREPEATING_NOT:
	            		long jumpdist = mPreferences.getLong("jumpdisttwo", 5000);
	            		new RealNumberPickerDialog(v.getContext(),
	        					mJumpDistSetTwoListner,
	                            (int) jumpdist,
	                                0,
	                            99900,
	                            R.string.forward_200msec).show();
	            		consumed = true;
	        			break;	
	            	case MediaPlaybackService.ABREPEATING_WAITING:
	            		break;
	            	case MediaPlaybackService.ABREPEATING_NOW:
	            		showAbList();
	            		consumed = true;
	            		break;
					}
			    }
				} catch (RemoteException ex) {
				}
				if (consumed == true) {
					return true;
				} else {
					return false;
				}
		}
	};
	
	private void showAbList(){
		try {
		Intent intent = new Intent();
        intent.setClass(this, ABPosPickerActivity.class);
        intent.putExtra(ABDbAdapter.KEY_MUSICTITLE, mService.getTrackName());
        intent.putExtra(ABDbAdapter.KEY_MUSICPATH, mService.getPath());
        intent.putExtra(ABDbAdapter.KEY_MUSICDURATION, mService.duration());
        intent.putExtra(ABDbAdapter.KEY_MUSIC_DATA, mService.getData());
        startActivityForResult(intent, BROWSE_ABPOINTS);
        mPickingABPos = true;
        mService.pause();
        refreshNow();
        setPauseButtonImage();
        setAbRepeatingButtonImage();
		} catch (RemoteException ex) {
		}
	}
	
	RealNumberPickerDialog.OnNumberSetListener mJumpDistSetTwoListner =
        new RealNumberPickerDialog.OnNumberSetListener() {
            public void onNumberSet(int jumpdist) {
            	Editor ed = mPreferences.edit();
            	ed.putLong("jumpdisttwo", (long) jumpdist);
            	ed.commit();
            	setJumpButtonContents();
            	notifyChange();
            }
    };
    
    ABShiftPickerDialog.OnShiftDistSetListener mAShiftDistSetListener =
    	new ABShiftPickerDialog.OnShiftDistSetListener() {
    		public void onShiftDistSet (long shiftdist, boolean checkmodeon) {
    			try {
    			mService.shiftAPos((long) shiftdist);
    			setAPosTimeText (mService.getAPos());
    			if (checkmodeon == true){
    				mService.seek(mService.getAPos());
    			}
    			} catch (RemoteException ex) {
    			}
    		}
    		public void onABPauseSet(long abpause) {
    			try {
    				mService.setABPause(mPreferences.getLong("abpause", 0));
    			} catch (RemoteException ex) {
    			}
    		}
    		
    };
    
    ABShiftPickerDialog.OnShiftDistSetListener mBShiftDistSetListener =
    	new ABShiftPickerDialog.OnShiftDistSetListener() {
    		public void onShiftDistSet (long shiftdist, boolean checkmodeon) {
    			try {
    			mService.shiftBPos((long) shiftdist);
    			long newbpos =mService.getBPos();
    			setBPosTimeText (newbpos);
    			if (checkmodeon == true){
    				long apos = mService.getAPos();
    				long jumpto;
    				jumpto = ((newbpos - CHECK_BPOINT_DURATION) < apos) ? apos : (newbpos - CHECK_BPOINT_DURATION);
    				mService.seek(jumpto);
    			}
    			} catch (RemoteException ex) {
    			}
    		}
    		public void onABPauseSet(long abpause) {
    			
    		}
    };
    
    private void setAPosTimeText (long apostime) {    	
    	mAPosTime.setText(MusicUtils.makeTimeString(this, apostime/1000));	
    }
    
    private void setBPosTimeText (long bpostime) {    	
    	mBPosTime.setText(MusicUtils.makeTimeString(this, bpostime/1000));	
    }
    
   
    
private View.OnLongClickListener mAPosTimeLongListner = new View.OnLongClickListener() {
		
		@Override
		public boolean onLongClick(View v) {
			boolean consumed = false;
			try {
				if (mService != null) {
					switch (mService.abRepeatingState()) {
	            	case MediaPlaybackService.ABREPEATING_NOT:
	            		long jumpdist = mPreferences.getLong("jumpdistone", 7500);
	            		new RealNumberPickerDialog(v.getContext(),
	        					mJumpDistSetOneListner,
	                            (int) jumpdist,
	                            0,
	                            99900,
	                            R.string.forward_200msec).show();
	            				consumed = true;
	        			break;	
	            	case MediaPlaybackService.ABREPEATING_WAITING:
	            		new ABShiftPickerDialog(v.getContext(),
	            				mAShiftDistSetListener,
	                            R.string.adjust_apoint, R.drawable.apoint, false).show();
	            				consumed = true;
	            		break;
	            	case MediaPlaybackService.ABREPEATING_NOW:
	            		
	            		break;
					}
			    }
				} catch (RemoteException ex) {
				}
			if (consumed == true) {
				return true;
			} else {
				return false;
			}
		}
	};
	
	RealNumberPickerDialog.OnNumberSetListener mJumpDistSetOneListner =
        new RealNumberPickerDialog.OnNumberSetListener() {
            public void onNumberSet(int jumpdist) {
            	Editor ed = mPreferences.edit();
            	ed.putLong("jumpdistone", (long) jumpdist);
            	ed.commit();
            	setJumpButtonContents();
            	notifyChange();
            }
    };
    
private View.OnLongClickListener mBPosTimeLongListner = new View.OnLongClickListener() {
		
		@Override
		public boolean onLongClick(View v) {
			boolean consumed = false;
			try {
				if (mService != null) {
					switch (mService.abRepeatingState()) {
	            	case MediaPlaybackService.ABREPEATING_NOT:
	            		long jumpdist = mPreferences.getLong("jumpdistthree", 2500);
	            		new RealNumberPickerDialog(v.getContext(),
	        					mJumpDistSetThreeListner,
	                            (int) jumpdist,
	                            0,
	                            99900,
	                            R.string.forward_200msec).show();
	            		consumed = true;
	        			break;	
	            	case MediaPlaybackService.ABREPEATING_WAITING:
	            		break;
	            	case MediaPlaybackService.ABREPEATING_NOW:
	            		
	            		break;
					}
			    }
				} catch (RemoteException ex) {
				}
				if (consumed == true) {
					return true;
				} else {
					return false;
				}
		}
	};
	
	RealNumberPickerDialog.OnNumberSetListener mJumpDistSetThreeListner =
        new RealNumberPickerDialog.OnNumberSetListener() {
            public void onNumberSet(int jumpdist) {
            	Editor ed = mPreferences.edit();
            	ed.putLong("jumpdistthree", (long) jumpdist);
            	ed.commit();
            	setJumpButtonContents();
            	notifyChange();
            }
    };

	private View.OnClickListener mPrevListener = new View.OnClickListener() {		
		public void onClick(View v) {
//			MusicUtils.logToFile("Prev Clicked");
			if (mService == null)
				return;
			try {
				int abstate = mService.abRepeatingState();
				if (mABWideBar) {
					long apos = mService.getAPos();
					if (mService.abListTraverseMode()) {
						if ((mService.position() - apos) < 2000) {
							mService.prevAbPoints();
						} else {
							mService.seek(apos);
							mService.play();
						}						
					} else {
						if (abstate != MediaPlaybackService.ABREPEATING_NOT) {
							if (MusicUtils.getBooleanPref(mContext, PREV_BEHAVIOUR_OLD, false)) {
								if ((mService.position() - apos) < 2000) {
									mService.clearABPos();
									setAbRepeatingButtonImage();
									setJumpButtonContents();
									showToast(R.string.abpos_cleared);
									mService.prev();
								} else {
									mService.seek(apos);
									mService.play();
								}
							} else {
								mService.seek(apos);
								mService.play();
							}
//							if ((mService.position() - apos) < 2000) {
//								mService.clearABPos();
//								setAbRepeatingButtonImage();
//								setJumpButtonContents();
//								showToast(R.string.abpos_cleared);
//								mService.prev();
//							} else {
//								mService.seek(apos);
//								mService.play();
//							}
						} else {
							if (mService.position() < 2000) {
								mService.prev();								
							} else {
								mService.seek(0);
								mService.play();
							}
						}
					}
				} else {
					if (mService.position() < 2000) {
						mService.prev();
					} else {
						mService.seek(0);
						mService.play();
					}
					if (abstate != MediaPlaybackService.ABREPEATING_NOT) {
						mService.clearABPos();
						setAbRepeatingButtonImage();
						setJumpButtonContents();
						showToast(R.string.abpos_cleared);
					}
				}
			} catch (RemoteException ex) {
			}
		}
	};

    private View.OnClickListener mNextListener = new View.OnClickListener() {
        public void onClick(View v) {
			if (mService == null)
				return;
			try {
				int abstate = mService.abRepeatingState();
				if (mABWideBar) {
					if (mService.abListTraverseMode()) {
						mService.nextAbPoints();					
					} else {
						if (abstate != MediaPlaybackService.ABREPEATING_NOT) {
							mService.clearABPos();
							setAbRepeatingButtonImage();
							setJumpButtonContents();
							showToast(R.string.abpos_cleared);
						}
						mService.next();
					}
				} else {
					mService.next();
					if (abstate != MediaPlaybackService.ABREPEATING_NOT) {
						mService.clearABPos();
						setAbRepeatingButtonImage();
						setJumpButtonContents();
						showToast(R.string.abpos_cleared);
					}
				}
			} catch (RemoteException ex) {
			}
        }
    };

    private RepeatingImageButton.RepeatListener mRewListener =
        new RepeatingImageButton.RepeatListener() {
        public void onRepeat(View v, long howlong, int repcnt) {
            scanBackward(repcnt, howlong);
        }
    };
    
    private RepeatingImageButton.RepeatListener mFfwdListener =
        new RepeatingImageButton.RepeatListener() {
        public void onRepeat(View v, long howlong, int repcnt) {
            scanForward(repcnt, howlong);
        }
    };
   
    @Override
    public void onStop() {
        paused = true;
        if (mService != null && mOneShot && getChangingConfigurations() == 0 && mPickingABPos == false) {
            try {
                mService.stop();
            } catch (RemoteException ex) {
            }
        }
        mHandler.removeMessages(REFRESH);
        unregisterReceiver(mStatusListener);
        MusicUtils.unbindFromService(mToken);
        mService = null;
//        Log.i(LOGTAG, "onStop");
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("oneshot", mOneShot);
        super.onSaveInstanceState(outState);
    }
    
    @Override
    public void onStart() {
//    	Log.i(LOGTAG, "onStart");
        super.onStart();
        paused = false;

        mToken = MusicUtils.bindToService(this, osc);
        if (mToken == null) {
            // something went wrong
            mHandler.sendEmptyMessage(QUIT);
        }
        
        IntentFilter f = new IntentFilter();
        f.addAction(MediaPlaybackService.PLAYSTATE_CHANGED);
        f.addAction(MediaPlaybackService.META_CHANGED);
        f.addAction(MediaPlaybackService.PLAYBACK_COMPLETE);
        f.addAction(MediaPlaybackService.ABPOS_CLEARD_BY_BT);
        f.addAction(MediaPlaybackService.ABPOS_SET_BY_BT);
        registerReceiver(mStatusListener, new IntentFilter(f));
        updateTrackInfo();
        long next = refreshNow();
        queueNextRefresh(next);
    }
    
    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
        mOneShot = intent.getBooleanExtra("oneshot", false);
    }
    
    @Override
    public void onResume() {
//    	if (!lyricsPlugin.getIsBound())
//    		lyricsPlugin.doBindService();
//    	Log.i(LOGTAG, "onResume");
        super.onResume();
//        if (!lyricsPlugin.getIsBound())
//    		lyricsPlugin.doBindService();
        
        
        updateTrackInfo();
        setPauseButtonImage();
        setAbRepeatingButtonImage();
        setABJumpButtonsForABRepeat ();
        mABWideBar = mPreferences.getBoolean("abwidebar", true);
    }
    
    @Override
    public void onDestroy()
    {
//    	if (lyricsPlugin.getIsBound())
//    		lyricsPlugin.doUnbindService();
    	
        mAlbumArtWorker.quit();
//        Log.i(LOGTAG, "onDestroy");
        super.onDestroy();
        //System.out.println("***************** playback activity onDestroy\n");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // Don't show the menu items if we got launched by path/filedescriptor, or
        // if we're in one shot mode. In most cases, these menu items are not
        // useful in those modes, so for consistency we never show them in these
        // modes, instead of tailoring them to the specific file being played.
        if (MusicUtils.getCurrentAudioId() >= 0 && !mOneShot) {
  
//  			SubMenu sub = menu.addSubMenu(2, ADJUST_APOINT, 0,
//  					R.string.adjust_apoint).setIcon(R.drawable.apoint);
//  			sub = menu.addSubMenu(2, ADJUST_BPOINT, 0,
//  					R.string.adjust_bpoint).setIcon(R.drawable.bpoint);
//  			menu.add(2, RECORD_ABPOINTS, 0, R.string.record_abpoints).setIcon(R.drawable.ic_abpos_save);
//  		    menu.setGroupVisible(2, false);
  		    
//  		    menu.add(0, HELP_MENU, 0, R.string.ab_help_menu).setIcon(R.drawable.ic_menu_abpos_help);
//  		    menu.add(0, BROWSE_ABPOINTS, 0, R.string.browse_abpoints).setIcon(R.drawable.ic_menu_abpos_library);
  		    SubMenu sub = menu.addSubMenu(0, HELP_MENU, 0,
  		    		R.string.ab_help_menu).setIcon(R.drawable.ic_menu_abpos_help);
            sub = menu.addSubMenu(0, ADD_TO_PLAYLIST, 0,
                    R.string.add_to_playlist).setIcon(android.R.drawable.ic_menu_add);
  		    
            menu.add(0, GOTO_START, 0, R.string.goto_start).setIcon(R.drawable.ic_menu_music_library);
            menu.add(0, PARTY_SHUFFLE, 0, R.string.party_shuffle); // icon will be set in onPrepareOptionsMenu()
            
            // these next two are in a separate group, so they can be shown/hidden as needed
            // based on the keyguard state
            menu.add(1, USE_AS_RINGTONE, 0, R.string.ringtone_menu_short)
                    .setIcon(R.drawable.ic_menu_set_as_ringtone);
            menu.add(1, DELETE_ITEM, 0, R.string.delete_item)
                    .setIcon(R.drawable.ic_menu_delete);
//            menu.add(0, SWITCH_PROGRESS_BAR, 0, R.string.ab_progress_bar_old);
            menu.add(0, LYRIC, 0, R.string.lyric).setIcon(R.drawable.ic_menu_lyric);
//            menu.add(0, LYRIC_OPTIONS, 0, R.string.lyric_options).setIcon(R.drawable.ic_menu_lyric);
            menu.add(0, DONATION, 0, R.string.ab_donation).setIcon(R.drawable.ic_menu_donation);
            menu.add(0, FLOAT_PAD, 0, R.string.ab_float_pad).setIcon(R.drawable.ic_menu_donation);
            menu.add(0, SLEEP_TIMER, 0, R.string.sleep_timer).setIcon(R.drawable.ic_sleep_timer);
            menu.add(0, EXPORT_AB_SOUND, 0, R.string.export_ab_sound)
            		.setIcon(R.drawable.ic_menu_set_as_ringtone);
            menu.add(0, AB_SETTINGS, 0, R.string.ab_prefs)
    		.setIcon(R.drawable.ic_menu_music_library);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mService == null) return false;
        MenuItem item = menu.findItem(PARTY_SHUFFLE);
        if (item != null) {
            int shuffle = MusicUtils.getCurrentShuffleMode();
            if (shuffle == MediaPlaybackService.SHUFFLE_AUTO) {
                item.setIcon(R.drawable.ic_menu_party_shuffle);
                item.setTitle(R.string.party_shuffle_off);
            } else {
                item.setIcon(R.drawable.ic_menu_party_shuffle);
                item.setTitle(R.string.party_shuffle);
            }
        }

        item = menu.findItem(ADD_TO_PLAYLIST);
        if (item != null) {
            SubMenu sub = item.getSubMenu();
            MusicUtils.makePlaylistMenu(this, sub);
        }
        
//        item = menu.findItem(HELP_MENU);
//        SubMenu sub = item.getSubMenu();
//        sub.clear();
//    	sub.add(0, HELP_ABREPEAT, 0, R.string.ab_help_ab_sub);
//    	sub.add(0, HELP_ADJUST_AB, 0, R.string.ab_help_adjust_ab_sub);
//    	sub.add(0, HELP_INTERVAL_AB, 0, R.string.ab_help_interval_ab_sub);
//    	sub.add(0, HELP_JUMP, 0, R.string.ab_help_jump_sub);
//    	sub.add(0, HELP_BOOKMARK, 0, R.string.ab_help_bookmark_sub);
//    	sub.add(0, HELP_MANAGE, 0, R.string.ab_help_manage_sub);
    	
    	item = menu.findItem(DONATION);
    	if (MusicUtils.donated(getApplicationContext())) {
    		item.setVisible(false);
    	} else {
    		item.setVisible(true);
    	}

//    	item = menu.findItem(SWITCH_PROGRESS_BAR);
//    	if (item != null) {
//    		if (mABWideBar) {
//    			item.setTitle(R.string.ab_progress_bar_old);
//    		} else {
//    			item.setTitle(R.string.ab_progress_bar_new);
//    		}
//    		
//    	}
    	
//        try {
//        	if (mService != null) {
//        		long aposition = mService.getAPos();
//        		long bposition = mService.getBPos();
//        		long duration = mService.duration();
//        	if (mService.abRepeatingState() == MediaPlaybackService.ABREPEATING_NOW) {
//        	  item = menu.findItem(ADJUST_APOINT);
//              if (item != null) {
//                  SubMenu sub = item.getSubMenu();
//                  sub.clear();
//                  if ((aposition + 1000) < (bposition - 500)) {
//                	  sub.add(0, FWD_APOINT_2000, 0, R.string.forward_2000msec);
//                  }
//                  if ((aposition + 500) < (bposition - 500)) {
//                	  sub.add(0, FWD_APOINT_1000, 0, R.string.forward_1000msec);
//                  }
//                  if ((aposition + 100) < (bposition - 500)) {
//                	  sub.add(0, FWD_APOINT_200, 0, R.string.forward_200msec);
//                  }
//                  if ((aposition - 100) > 0) {
//                	  sub.add(0, BWD_APOINT_200, 0, R.string.backward_200msec);
//                  }
//                  if ((aposition - 500) > 0) {
//                	  sub.add(0, BWD_APOINT_1000, 0, R.string.backward_1000msec);
//                  }
//                  if ((aposition - 1000) > 0) {
//                	  sub.add(0, BWD_APOINT_2000, 0, R.string.backward_2000msec);
//                  }
//              }
//              item = menu.findItem(ADJUST_BPOINT);
//              if (item != null) {
//                  SubMenu sub = item.getSubMenu();
//                  sub.clear();
//                  if ((bposition + 1000) < duration - 300) {
//                	  sub.add(0, FWD_BPOINT_2000, 0, R.string.forward_2000msec);
//                  }
//                  if ((bposition + 500) < duration - 300) {
//                	  sub.add(0, FWD_BPOINT_1000, 0, R.string.forward_1000msec);
//                  }
//                  if ((bposition + 100) < duration - 300) {
//                	  sub.add(0, FWD_BPOINT_200, 0, R.string.forward_200msec);
//                  }
//                  if ((bposition - 100) > (aposition + 500)) {
//                	  sub.add(0, BWD_BPOINT_200, 0, R.string.backward_200msec);
//                  }
//                  if ((bposition - 500) > (aposition + 500)) {
//                	  sub.add(0, BWD_BPOINT_1000, 0, R.string.backward_1000msec);
//                  }
//                  if ((bposition - 1000) > (aposition + 500)) {
//                	  sub.add(0, BWD_BPOINT_2000, 0, R.string.backward_2000msec);
//                  }
//              }
//              menu.setGroupVisible(2, true);
//    		} else {
//    		  menu.setGroupVisible(2, false);
//    		}
//        	}
//        } catch (RemoteException ex) {
//        }

        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        menu.setGroupVisible(1, !km.inKeyguardRestrictedInputMode());

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        try {
            switch (item.getItemId()) {
                case GOTO_START:
                    intent = new Intent();
                    intent.setClass(this, MusicBrowserActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                    break;
                case USE_AS_RINGTONE: {
                    // Set the system setting to make this the current ringtone
                    if (mService != null) {
                        MusicUtils.setRingtone(this, mService.getAudioId());
                    }
                    return true;
                }
                case PARTY_SHUFFLE:
                    MusicUtils.togglePartyShuffle();
                    setShuffleButtonImage();
                    break;
                    
                case NEW_PLAYLIST: {
                    intent = new Intent();
                    intent.setClass(this, CreatePlaylist.class);
                    startActivityForResult(intent, NEW_PLAYLIST);
                    return true;
                }

                case PLAYLIST_SELECTED: {
                    long [] list = new long[1];
                    list[0] = MusicUtils.getCurrentAudioId();
                    long playlist = item.getIntent().getLongExtra("playlist", 0);
                    MusicUtils.addToPlaylist(this, list, playlist);
                    return true;
                }
                
                case DELETE_ITEM: {
                    if (mService != null) {
                        long [] list = new long[1];
                        list[0] = MusicUtils.getCurrentAudioId();
                        Bundle b = new Bundle();
                        b.putString("description", getString(R.string.delete_song_desc,
                                mService.getTrackName()));
                        b.putLongArray("items", list);
                        intent = new Intent();
                        intent.setClass(this, DeleteItems.class);
                        intent.putExtras(b);
                        startActivityForResult(intent, -1);
                    }
                    return true;
                }
                case RECORD_ABPOINTS:                	
//                	mABDbHelper.open();
//                	
//                	String mWhereClause = ABDbAdapter.KEY_MUSICTITLE + "=\"" + mService.getTrackName() + "\""
//                    		+ " AND " + ABDbAdapter.KEY_MUSICDURATION + ">=" + Long.toString(mService.duration() - 1) 
//                    		+ " AND " + ABDbAdapter.KEY_MUSICDURATION + "<=" + Long.toString(mService.duration() + 1);
//                	
//                	if (mABDbHelper.checkIfABPosExists(mWhereClause)) {
//                		
//                	} else {
//                		mABDbHelper.createABPos(mAPosTime.getText()+" "+mBPosTime.getText(),
//                    			mService.getAPos(), mService.getBPos(), mService.getPath(), 
//                    			mService.getTrackName(), mService.duration());
//                    	mABDbHelper.close();
//                    	showToast(R.string.abpos_recorded);               		
//                	}
                	
//                	mABDbHelper.createABPos(mAPosTime.getText()+" "+mBPosTime.getText(),
//                			mService.getAPos(), mService.getBPos(), mService.getPath(), 
//                			mService.getTrackName(), mService.duration());
//                	mABDbHelper.close();
//                	showToast(R.string.abpos_recorded);
                	break;
                case HELP_MENU:
                	intent = new Intent();
                    intent.setClass(this, HelpListActivity.class);
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
                case HELP_INTERVAL_AB:
//                	intent = new Intent();
//                    intent.setClass(this, IntervalABHelpActivity.class);
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
                case FLOAT_PAD:
                	
                	if (!StandOutWindow.isRunning) {
                		Log.i(LOGTAG, "Floating control starting");
	                	StandOutWindow.closeAll(this, WidgetsWindow.class);
	                	StandOutWindow.show(this, WidgetsWindow.class,
	            				StandOutWindow.DEFAULT_ID);
	                	sendInitialDataToWidgetsWindow();
//	                	Log.i(LOGTAG, "Floating control started");
                	} else {
                		showToast(R.string.abpos_only_one_window);
//                		Log.i(LOGTAG, "Floating control not started");
                	}
                	break;
                case LYRIC:
//                	int mOption = MusicUtils.getIntPref(this, LyricOptionsActivity.LYRICOPTIONS, LyricOptionsActivity.AUTO);
                	int mOption = LyricOptionsActivity.ID3TAG;
                	String filename;
                	String ext;
                	switch (mOption) {
                	case LyricOptionsActivity.AUTO:
                		intent = new Intent();
//                    	String filename;
                    	if (mService != null) {
                    		filename = mService.getData();
                    		if (filename == null) filename = "";
                    	} else {
                    		filename = "";
                    	}
                        ext = MusicUtils.getExtension(filename);
                        if ((ext != null) && (ext.equals("mp3"))) {
                	        try {
                	            Mp3File mp3 = new Mp3File(filename, false);
                	            ID3v2 tag = mp3.getId3v2Tag();
                	            String lyric = tag.getUnsynchLyrics();
                	            if ((lyric == null)||(lyric.length() == 0)) {
//                	            	musixSearch();
                	            	break;
                	            }
                	            String lyric1 = lyric.replace("\r", "\n");
                	            intent.putExtra(LyricActivity.LYRIC, lyric1);
                	            } catch (Exception e) {
                	            	Log.e("LyricActivity","Lyric Tag Error: " + e);
//                	            	musixSearch();
                	            	break;
                	            }
                        } else {
//                        	intent.putExtra(LyricActivity.LYRIC, getString(R.string.not_mp3));
//                        	musixSearch();
                        	break;
                        }
                        intent.setClass(this, LyricActivity.class);
                        startActivity(intent);
                		break;
                	case LyricOptionsActivity.ID3TAG:
                		intent = new Intent();
//                    	String filename;
                    	if (mService != null) {
                    		filename = mService.getData();
                    		if (filename == null) filename = "";
                    	} else {
                    		filename = "";
                    	}
                        ext = MusicUtils.getExtension(filename);
                        if ((ext != null) && (ext.equals("mp3"))) {
                	        try {
                	            Mp3File mp3 = new Mp3File(filename, false);
                	            ID3v2 tag = mp3.getId3v2Tag();
                	            String lyric = tag.getUnsynchLyrics();
                	            String lyric1 = lyric.replace("\r", "\n");
                	            intent.putExtra(LyricActivity.LYRIC, lyric1);
                	            } catch (Exception e) {
                	            	Log.e("LyricActivity","Lyric Tag Error: " + e);
                	            	intent.putExtra(LyricActivity.LYRIC, getString(R.string.no_id3tag));
                	            }
                        } else {
                        	intent.putExtra(LyricActivity.LYRIC, getString(R.string.not_mp3));
                        }
                        intent.putExtra(ABDbAdapter.KEY_MUSICDURATION, mService.duration());
                        intent.putExtra(ABDbAdapter.KEY_MUSICTITLE, mService.getTrackName());
                        intent.putExtra(ABDbAdapter.KEY_MUSIC_DATA, filename);
                        intent.setClass(this, LyricActivity.class);
                        startActivity(intent);
                		break;
                	}
                	break;
                case LYRIC_OPTIONS:
                	intent = new Intent();
                    intent.setClass(this, LyricOptionsActivity.class);
                    intent.putExtra(ABDbAdapter.KEY_MUSICTITLE, mService.getTrackName());
                    intent.putExtra(ABDbAdapter.KEY_MUSICPATH, mService.getPath());
                    intent.putExtra(ABDbAdapter.KEY_MUSICDURATION, mService.duration());
                    startActivity(intent);
                    break;
                case SLEEP_TIMER:
                	intent = new Intent();
                    intent.setClass(this, SleepTimerActivity.class);
                    startActivity(intent);
                	break;
                case BROWSE_ABPOINTS:
                	intent = new Intent();
                    intent.setClass(this, ABPosPickerActivity.class);
                    intent.putExtra(ABDbAdapter.KEY_MUSICTITLE, mService.getTrackName());
                    intent.putExtra(ABDbAdapter.KEY_MUSICPATH, mService.getPath());
                    intent.putExtra(ABDbAdapter.KEY_MUSICDURATION, mService.duration());
                    intent.putExtra(ABDbAdapter.KEY_MUSIC_DATA, mService.getData());
                    startActivityForResult(intent, BROWSE_ABPOINTS);
                    mService.pause();
                    refreshNow();
                    setPauseButtonImage();
                    setAbRepeatingButtonImage();
                	break;
                case EXPORT_AB_SOUND:
                	try {
                        Intent intent2 = new Intent(Intent.ACTION_EDIT,
                                Uri.parse(mService.getData()));
                        intent2.putExtra("was_get_content_intent",
//                                mWasGetContentIntent);
                        		false);
                        if (mService.abRepeatingState()!=MediaPlaybackService.ABREPEATING_NOT) {
	                        intent2.putExtra(com.ringdroid.RingdroidEditActivity.APOS,
	                        		mService.getAPos());
	                        intent2.putExtra(com.ringdroid.RingdroidEditActivity.BPOS,
	                        		mService.getBPos());
                        }
//                        intent�Q.putExtra(com.ringdroid.RingdroidEditActivity.ABTITLE,
//                        		mCursor.getString(mCursor.getColumnIndex(ABDbAdapter.KEY_ABTITLE)));
                        intent2.setClassName(
//                                "com.ringdroid",
                        		"com.kojimahome.music21",
                        "com.ringdroid.RingdroidEditActivity");
                        startActivityForResult(intent2, EXPORT_AB_SOUND);
                    } catch (Exception e) {
                        Log.e(LOGTAG, "Couldn't start ringdroid editor");
                    }
                	break;
                case AB_SETTINGS:
                	intent = new Intent();
                    intent.setClass(this, PrefsActivity.class);
                    startActivity(intent);
                	break;
                
            }
        } catch (RemoteException ex) {
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void sendInitialDataToWidgetsWindow() {
    	try {
    	Intent i = new Intent();
    	i.setClass(mContext, WidgetsWindow.class);
    	i.setAction(WidgetsWindow.INITIALIZE_WIDGETSWINDOW);
        i.putExtra("artist", mService.getArtistName());
        i.putExtra("album",mService.getAlbumName());
        i.putExtra("track", mService.getTrackName());
//        for lyric activity
        i.putExtra(ABDbAdapter.KEY_MUSIC_DATA, mService.getData());
        i.putExtra(ABDbAdapter.KEY_MUSICTITLE, mService.getTrackName());
        i.putExtra(ABDbAdapter.KEY_MUSICDURATION, mService.duration());
        i.putExtra(MediaPlaybackService.PLAY_STATE, mService.isPlaying());
        i.putExtra(MediaPlaybackService.AB_STATE, mService.abRepeatingState());
        i.putExtra(ABDbAdapter.KEY_APOS, mService.getAPos());
        i.putExtra(ABDbAdapter.KEY_BPOS, mService.getBPos());
        i.putExtra(MediaPlaybackService.AB_LIST_TRAVERSE_MODE, mService.abListTraverseMode());
        startService(i);
    	} catch (RemoteException ex) {
    		Log.e(LOGTAG, "RemoteException:" + ex);
        } 
    }
    
//    private void musixSearch() {
//    	String artist = null;
//        String track = null;
//        try {
//			if (mService != null) {
//				artist = mService.getArtistName(); 
//	    		if (artist == null) artist = "";
//	    		track  = mService.getTrackName();
//	    		if (track == null) track = "";
//			} else {
//				artist = ""; 
//				track  = "";
//			}
//        } catch (Exception ex) {
//        	artist = ""; 
//        	track  = "";
//        }
////        Log.i("MediaPlaybackActivity", "Musix Artist: " + artist + "  Track: " + track);
////		if (lyricsPlugin.getIsBound())
////			try {
////				lyricsPlugin.startLyricsActivity(artist,track);
////			} catch (RemoteException e) {
////				lyricsPlugin.downloadLyricsPlugin();
////			}
////		else lyricsPlugin.downloadLyricsPlugin();
//    }
//    
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode != RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case NEW_PLAYLIST:
                Uri uri = intent.getData();
                if (uri != null) {
                    long [] list = new long[1];
                    list[0] = MusicUtils.getCurrentAudioId();
                    int playlist = Integer.parseInt(uri.getLastPathSegment());
                    MusicUtils.addToPlaylist(this, list, playlist);
                }
                break;
            case BROWSE_ABPOINTS:
            	mABPickResultIntent = intent;
            	long apos = intent.getLongExtra(ABDbAdapter.KEY_APOS, 0);
            	long bpos = intent.getLongExtra(ABDbAdapter.KEY_BPOS, 0);
            	int currentposition = intent.getIntExtra(ABDbAdapter.KEY_POSITION, -1);
            	try {
                	if (mService != null) {
                	if (mService.abRepeatingState() != MediaPlaybackService.ABREPEATING_NOT) {
                		mService.clearABPos();
                	}
                		
                	mService.seek(bpos);
                	mService.setBPos();
                    mBPosTime.setText(MusicUtils.makeTimeString(this, bpos / 1000));
                    mBPosTime.setVisibility(View.VISIBLE);
                    
                    mService.seek(apos);
                	mService.setAPos();
                	mService.setABPause(mPreferences.getLong("abpause", 0));
                	mAPosTime.setText(MusicUtils.makeTimeString(this, apos / 1000));
                	mAPosTime.setVisibility(View.VISIBLE);
                	mService.play();
                	mService.setPosInAbList(currentposition);
                	mService.setAbRecRowId(intent.getLongExtra(ABDbAdapter.KEY_ROWID, -1));
                	mService.setAbEdited(false);
                	}
            	} catch (RemoteException ex) {
                }
            	break;
        }
    }
    private final int keyboard[][] = {
        {
            KeyEvent.KEYCODE_Q,
            KeyEvent.KEYCODE_W,
            KeyEvent.KEYCODE_E,
            KeyEvent.KEYCODE_R,
            KeyEvent.KEYCODE_T,
            KeyEvent.KEYCODE_Y,
            KeyEvent.KEYCODE_U,
            KeyEvent.KEYCODE_I,
            KeyEvent.KEYCODE_O,
            KeyEvent.KEYCODE_P,
        },
        {
            KeyEvent.KEYCODE_A,
            KeyEvent.KEYCODE_S,
            KeyEvent.KEYCODE_D,
            KeyEvent.KEYCODE_F,
            KeyEvent.KEYCODE_G,
            KeyEvent.KEYCODE_H,
            KeyEvent.KEYCODE_J,
            KeyEvent.KEYCODE_K,
            KeyEvent.KEYCODE_L,
            KeyEvent.KEYCODE_DEL,
        },
        {
            KeyEvent.KEYCODE_Z,
            KeyEvent.KEYCODE_X,
            KeyEvent.KEYCODE_C,
            KeyEvent.KEYCODE_V,
            KeyEvent.KEYCODE_B,
            KeyEvent.KEYCODE_N,
            KeyEvent.KEYCODE_M,
            KeyEvent.KEYCODE_COMMA,
            KeyEvent.KEYCODE_PERIOD,
            KeyEvent.KEYCODE_ENTER
        }

    };

    private int lastX;
    private int lastY;

    private boolean seekMethod1(int keyCode)
    {
        if (mService == null) return false;
        for(int x=0;x<10;x++) {
            for(int y=0;y<3;y++) {
                if(keyboard[y][x] == keyCode) {
                    int dir = 0;
                    // top row
                    if(x == lastX && y == lastY) dir = 0;
                    else if (y == 0 && lastY == 0 && x > lastX) dir = 1;
                    else if (y == 0 && lastY == 0 && x < lastX) dir = -1;
                    // bottom row
                    else if (y == 2 && lastY == 2 && x > lastX) dir = -1;
                    else if (y == 2 && lastY == 2 && x < lastX) dir = 1;
                    // moving up
                    else if (y < lastY && x <= 4) dir = 1; 
                    else if (y < lastY && x >= 5) dir = -1; 
                    // moving down
                    else if (y > lastY && x <= 4) dir = -1; 
                    else if (y > lastY && x >= 5) dir = 1; 
                    lastX = x;
                    lastY = y;
                    try {
                        mService.seek(mService.position() + dir * 5);
                    } catch (RemoteException ex) {
                    }
                    refreshNow();
                    return true;
                }
            }
        }
        lastX = -1;
        lastY = -1;
        return false;
    }

    private boolean seekMethod2(int keyCode)
    {
        if (mService == null) return false;
        for(int i=0;i<10;i++) {
            if(keyboard[0][i] == keyCode) {
                int seekpercentage = 100*i/10;
                try {
                    mService.seek(mService.duration() * seekpercentage / 100);
                } catch (RemoteException ex) {
                }
                refreshNow();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        try {
            switch(keyCode)
            {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    if (!useDpadMusicControl()) {
                        break;
                    }
                    if (mService != null) {
                        if (!mSeeking && mStartSeekPos >= 0) {
                            mPauseButton.requestFocus();
                            if (mStartSeekPos < 1000) {
                                mService.prev();
                            } else {
                                mService.seek(0);
                            }
                        } else {
                            scanBackward(-1, event.getEventTime() - event.getDownTime());
                            mPauseButton.requestFocus();
                            mStartSeekPos = -1;
                        }
                    }
                    mSeeking = false;
                    mPosOverride = -1;
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if (!useDpadMusicControl()) {
                        break;
                    }
                    if (mService != null) {
                        if (!mSeeking && mStartSeekPos >= 0) {
                            mPauseButton.requestFocus();
                            mService.next();
                        } else {
                            scanForward(-1, event.getEventTime() - event.getDownTime());
                            mPauseButton.requestFocus();
                            mStartSeekPos = -1;
                        }
                    }
                    mSeeking = false;
                    mPosOverride = -1;
                    return true;
            }
        } catch (RemoteException ex) {
        }
        return super.onKeyUp(keyCode, event);
    }

    private boolean useDpadMusicControl() {
        if (mDeviceHasDpad && (mPrevButton.isFocused() ||
                mNextButton.isFocused() ||
                mPauseButton.isFocused())) {
            return true;
        }
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        int direction = -1;
        int repcnt = event.getRepeatCount();

        if((seekmethod==0)?seekMethod1(keyCode):seekMethod2(keyCode))
            return true;

        switch(keyCode)
        {
/*
            // image scale
            case KeyEvent.KEYCODE_Q: av.adjustParams(-0.05, 0.0, 0.0, 0.0, 0.0,-1.0); break;
            case KeyEvent.KEYCODE_E: av.adjustParams( 0.05, 0.0, 0.0, 0.0, 0.0, 1.0); break;
            // image translate
            case KeyEvent.KEYCODE_W: av.adjustParams(    0.0, 0.0,-1.0, 0.0, 0.0, 0.0); break;
            case KeyEvent.KEYCODE_X: av.adjustParams(    0.0, 0.0, 1.0, 0.0, 0.0, 0.0); break;
            case KeyEvent.KEYCODE_A: av.adjustParams(    0.0,-1.0, 0.0, 0.0, 0.0, 0.0); break;
            case KeyEvent.KEYCODE_D: av.adjustParams(    0.0, 1.0, 0.0, 0.0, 0.0, 0.0); break;
            // camera rotation
            case KeyEvent.KEYCODE_R: av.adjustParams(    0.0, 0.0, 0.0, 0.0, 0.0,-1.0); break;
            case KeyEvent.KEYCODE_U: av.adjustParams(    0.0, 0.0, 0.0, 0.0, 0.0, 1.0); break;
            // camera translate
            case KeyEvent.KEYCODE_Y: av.adjustParams(    0.0, 0.0, 0.0, 0.0,-1.0, 0.0); break;
            case KeyEvent.KEYCODE_N: av.adjustParams(    0.0, 0.0, 0.0, 0.0, 1.0, 0.0); break;
            case KeyEvent.KEYCODE_G: av.adjustParams(    0.0, 0.0, 0.0,-1.0, 0.0, 0.0); break;
            case KeyEvent.KEYCODE_J: av.adjustParams(    0.0, 0.0, 0.0, 1.0, 0.0, 0.0); break;

*/

            case KeyEvent.KEYCODE_SLASH:
                seekmethod = 1 - seekmethod;
                return true;

            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (!useDpadMusicControl()) {
                    break;
                }
                if (!mPrevButton.hasFocus()) {
                    mPrevButton.requestFocus();
                }
                scanBackward(repcnt, event.getEventTime() - event.getDownTime());
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (!useDpadMusicControl()) {
                    break;
                }
                if (!mNextButton.hasFocus()) {
                    mNextButton.requestFocus();
                }
                scanForward(repcnt, event.getEventTime() - event.getDownTime());
                return true;

            case KeyEvent.KEYCODE_S:
                toggleShuffle();
                return true;

            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_SPACE:
                doPauseResume();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    
    private void scanBackward(int repcnt, long delta) {
        if(mService == null) return;
        try {
            if(repcnt == 0) {
                mStartSeekPos = mService.position();
                mLastSeekEventTime = 0;
                mSeeking = false;
            } else {
                mSeeking = true;
                if (delta < 5000) {
                    // seek at 10x speed for the first 5 seconds
                    delta = delta * 10; 
                } else {
                    // seek at 40x after that
                    delta = 50000 + (delta - 5000) * 40;
                }
                long newpos = mStartSeekPos - delta;
                if (newpos < 0) {
                    // move to previous track
                	if (mService.abRepeatingState() != MediaPlaybackService.ABREPEATING_NOT) {
                		mService.clearABPos();
                        setAbRepeatingButtonImage();
                        setJumpButtonContents();
                            showToast(R.string.abpos_cleared);
                        }
                    mService.prev();
                    long duration = mService.duration();
                    mStartSeekPos += duration;
                    newpos += duration;
                }
                if (((delta - mLastSeekEventTime) > 250) || repcnt < 0){
//                	if ((mService.abRepeatingState()!=MediaPlaybackService.ABREPEATING_NOT) && (newpos < mService.getAPos())) {
//                    	mService.clearABPos();
//                        setAbRepeatingButtonImage();
//                        setJumpButtonContents();
//                            showToast(R.string.abpos_cleared);
//                	}
                    mService.seek(newpos);
                    mLastSeekEventTime = delta;
                }
                if (repcnt >= 0) {
                    mPosOverride = newpos;
                } else {
                    mPosOverride = -1;
                }
                refreshNow();
            }
//            mService.clearABPos();
//            setAbRepeatingButtonImage();
        } catch (RemoteException ex) {
        }
    }

    private void scanForward(int repcnt, long delta) {
        if(mService == null) return;
        try {
            if(repcnt == 0) {
                mStartSeekPos = mService.position();
                mLastSeekEventTime = 0;
                mSeeking = false;
            } else {
                mSeeking = true;
                if (delta < 5000) {
                    // seek at 10x speed for the first 5 seconds
                    delta = delta * 10; 
                } else {
                    // seek at 40x after that
                    delta = 50000 + (delta - 5000) * 40;
                }
                long newpos = mStartSeekPos + delta;
                long duration = mService.duration();
                if (newpos >= duration) {
                    // move to next track
                	if (mService.abRepeatingState() != MediaPlaybackService.ABREPEATING_NOT) {
                		mService.clearABPos();
                        setAbRepeatingButtonImage();
                        setJumpButtonContents();
                            showToast(R.string.abpos_cleared);
                        }
                    mService.next();
                    mStartSeekPos -= duration; // is OK to go negative
                    newpos -= duration;
                }
                if (((delta - mLastSeekEventTime) > 250) || repcnt < 0){
                	if ((mService.abRepeatingState()==MediaPlaybackService.ABREPEATING_NOW) && (newpos > mService.getBPos())) {
                		if (mService.abRepeatingState() != MediaPlaybackService.ABREPEATING_NOT) {
                		mService.clearABPos();
                        setAbRepeatingButtonImage();
                        setJumpButtonContents();
                            showToast(R.string.abpos_cleared);
                        }
                	}
                    mService.seek(newpos);
                    mLastSeekEventTime = delta;
                }
                if (repcnt >= 0) {
                    mPosOverride = newpos;
                } else {
                    mPosOverride = -1;
                }
                refreshNow();
            }
//            mService.clearABPos();
//            setAbRepeatingButtonImage();
        } catch (RemoteException ex) {
        }
    }
    
    private void doPauseResume() {
        try {
            if(mService != null) {
                if (mService.isPlaying() || mService.getInABPause()) {
                    mService.pause();
                } else {
                    mService.play();
                }
                refreshNow();
                setPauseButtonImage();
                setAbRepeatingButtonImage();
            }
        } catch (RemoteException ex) {
        }
    }
    
    private void toggleShuffle() {
        if (mService == null) {
            return;
        }
        try {
            int shuffle = mService.getShuffleMode();
            if (shuffle == MediaPlaybackService.SHUFFLE_NONE) {
                mService.setShuffleMode(MediaPlaybackService.SHUFFLE_NORMAL);
                if (mService.getRepeatMode() == MediaPlaybackService.REPEAT_CURRENT) {
                    mService.setRepeatMode(MediaPlaybackService.REPEAT_ALL);
                    setRepeatButtonImage();
                }
                showToast(R.string.shuffle_on_notif);
            } else if (shuffle == MediaPlaybackService.SHUFFLE_NORMAL ||
                    shuffle == MediaPlaybackService.SHUFFLE_AUTO) {
                mService.setShuffleMode(MediaPlaybackService.SHUFFLE_NONE);
                showToast(R.string.shuffle_off_notif);
            } else {
                Log.e("MediaPlaybackActivity", "Invalid shuffle mode: " + shuffle);
            }
            setShuffleButtonImage();
        } catch (RemoteException ex) {
        }
    }
    
    private void cycleRepeat() {
        if (mService == null) {
            return;
        }
        try {
            int mode = mService.getRepeatMode();
            if (mode == MediaPlaybackService.REPEAT_NONE) {
                mService.setRepeatMode(MediaPlaybackService.REPEAT_ALL);
                showToast(R.string.repeat_all_notif);
            } else if (mode == MediaPlaybackService.REPEAT_ALL) {
                mService.setRepeatMode(MediaPlaybackService.REPEAT_CURRENT);
                if (mService.getShuffleMode() != MediaPlaybackService.SHUFFLE_NONE) {
                    mService.setShuffleMode(MediaPlaybackService.SHUFFLE_NONE);
                    setShuffleButtonImage();
                }
                showToast(R.string.repeat_current_notif);
            } else {
                mService.setRepeatMode(MediaPlaybackService.REPEAT_NONE);
                showToast(R.string.repeat_off_notif);
            }
            setRepeatButtonImage();
        } catch (RemoteException ex) {
        }
        
    }
    
    private void showToast(int resid) {
        if (mToast == null) {
            mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        }
        mToast.setText(resid);
        mToast.show();
    }

    private void startPlayback() {

        if(mService == null)
            return;
        Intent intent = getIntent();
        String filename = "";
        Uri uri = intent.getData();
        if (uri != null && uri.toString().length() > 0) {
            // If this is a file:// URI, just use the path directly instead
            // of going through the open-from-filedescriptor codepath.
            String scheme = uri.getScheme();
            if ("file".equals(scheme)) {
                filename = uri.getPath();
            } else {
                filename = uri.toString();
            }
//            Log.i("MediaPlaybackActivity", "Id3 filename1: " + filename);
            try {
                if (! ContentResolver.SCHEME_CONTENT.equals(scheme) ||
                        ! MediaStore.AUTHORITY.equals(uri.getAuthority())) {
                    mOneShot = true;
                }
                mService.stop();
//                Log.i("MediaPlaybackActivity", "Id3 filename2: " + filename);
                mService.openFile(filename, mOneShot);
                mService.clearABPos();
                setAbRepeatingButtonImage();
                mService.play();
                setIntent(new Intent());
            } catch (Exception ex) {
                Log.d("MediaPlaybackActivity", "couldn't start playback: " + ex);
            }
        }
        
        updateTrackInfo();
        long next = refreshNow();
        queueNextRefresh(next);
        
//        try {
//        	Log.i("MediaPlaybackActivity", "Id3 filename3: " + filename);
//	        Mp3File mp3 = new Mp3File(filename);
//	        ID3v2 tag = mp3.getId3v2Tag();
//	        String lyric = tag.getUnsynchLyrics();
//	        if (lyric != null) {
//		        String lyric1 = lyric.replace("\r", "\n");
//		        Log.i("MediaPlaybackActivity", "Id3 Lyric: " + lyric1);
//	        } else {
//	        	Log.i("MediaPlaybackActivity", "Id3 Lyric: NULL");
//	        }
////        textv.setText(lyric1);
//        } catch (Exception ex) {
//        	Log.d("MediaPlaybackActivity", "Id3 error: " + ex);
//        }
    }

	private ServiceConnection osc = new ServiceConnection() {
		public void onServiceConnected(ComponentName classname, IBinder obj) {
			String durationformat;
			StringBuilder sFormatBuilder = new StringBuilder();
			Formatter sFormatter = new Formatter(sFormatBuilder,
					Locale.getDefault());
			Object[] sTimeArgs = new Object[5];
			final Object[] timeArgs = sTimeArgs;
			Long apos;
			Long bpos;
			mService = IMediaPlaybackService.Stub.asInterface(obj);
			startPlayback();
			if (mABPickResultIntent != null) {
				mPickingABPos = false;
				apos = mABPickResultIntent
						.getLongExtra(ABDbAdapter.KEY_APOS, 0);
				bpos = mABPickResultIntent
						.getLongExtra(ABDbAdapter.KEY_BPOS, 0);
				int currentposition = mABPickResultIntent.getIntExtra(ABDbAdapter.KEY_POSITION, -1);
				try {
					if (mService != null) {
						if (mService.abRepeatingState() != MediaPlaybackService.ABREPEATING_NOT) {
							mService.clearABPos();
						}

						mService.seek(apos);

						if (bpos != 0) {
							mService.setAPos();
							apos = apos / 1000;
							durationformat = apos < 3600 ? "%2$d:%5$02d"
									: "%1$d:%3$02d:%5$02d";
							sFormatBuilder.setLength(0);
							timeArgs[0] = apos / 3600;
							timeArgs[1] = apos / 60;
							timeArgs[2] = (apos / 60) % 60;
							timeArgs[3] = apos;
							timeArgs[4] = apos % 60;
							mAPosTime.setText(sFormatter.format(durationformat,
									timeArgs).toString());
							mAPosTime.setCompoundDrawablesWithIntrinsicBounds(
									null,
									getResources().getDrawable(
											R.drawable.apostime), null, null);
							mAPosTime.setVisibility(View.VISIBLE);

							mService.seek(bpos);
							mService.setBPos();
							mService.setABPause(mPreferences.getLong("abpause",
									0));
							bpos = bpos / 1000;
							durationformat = bpos < 3600 ? "%2$d:%5$02d"
									: "%1$d:%3$02d:%5$02d";
							sFormatBuilder.setLength(0);
							timeArgs[0] = bpos / 3600;
							timeArgs[1] = bpos / 60;
							timeArgs[2] = (bpos / 60) % 60;
							timeArgs[3] = bpos;
							timeArgs[4] = bpos % 60;
							mBPosTime.setText(sFormatter.format(durationformat,
									timeArgs).toString());
							mBPosTime.setCompoundDrawablesWithIntrinsicBounds(
									null,
									getResources().getDrawable(
											R.drawable.bpostime), null, null);
							mBPosTime.setVisibility(View.VISIBLE);
							mJumpButtonCenter.setText("AB");
							mJumpButtonCenter
									.setCompoundDrawablesWithIntrinsicBounds(
											null,
											getResources().getDrawable(
													R.drawable.record), null,
											null);
							mJumpButtonCenter.setVisibility(View.VISIBLE);
						} else {
							if (MusicUtils.getBooleanPref(mContext, NEW_BM,
									false)) {
								mAbRepeatingButton
										.setImageResource(R.drawable.bpoint);
								mService.setAPos();
								apos = mService.getAPos() / 1000;
								durationformat = apos < 3600 ? "%2$d:%5$02d"
										: "%1$d:%3$02d:%5$02d";
								sFormatBuilder.setLength(0);
								timeArgs[0] = apos / 3600;
								timeArgs[1] = apos / 60;
								timeArgs[2] = (apos / 60) % 60;
								timeArgs[3] = apos;
								timeArgs[4] = apos % 60;
								mAPosTime.setText(sFormatter.format(
										durationformat, timeArgs).toString());
								mAPosTime
										.setCompoundDrawablesWithIntrinsicBounds(
												null,
												getResources()
														.getDrawable(
																R.drawable.apostime_or_bookmark),
												null, null);
								mAPosTime.setVisibility(View.VISIBLE);
								mJumpButtonCenter
										.setCompoundDrawablesWithIntrinsicBounds(
												null,
												getResources()
														.getDrawable(
																R.drawable.ic_bookmark_flat),
												null, null);
								mJumpButtonCenter.setText("B Mark");
								mBPosTime
										.setCompoundDrawablesWithIntrinsicBounds(
												null,
												getResources().getDrawable(
														R.drawable.cancel_flat),
												null, null);
								mBPosTime.setText("Cancel");
								mBPosTime.setVisibility(View.VISIBLE);
							} else {
								setJumpButtonContents();
							}
							// setJumpButtonContents();
						}

						mService.play();
						mService.setPosInAbList(currentposition);
						mService.setAbRecRowId(mABPickResultIntent.getLongExtra(ABDbAdapter.KEY_ROWID, -1));
	                	mService.setAbEdited(false);

					}
				} catch (RemoteException ex) {
				}
				mABPickResultIntent = null;
			}
			try {
				// Assume something is playing when the service says it is,
				// but also if the audio ID is valid but the service is paused.
				if (mService.getAudioId() >= 0 || mService.isPlaying()
						|| mService.getPath() != null) {
					// something is playing now, we're done
					if (mOneShot || mService.getAudioId() < 0) {
						mRepeatButton.setVisibility(View.INVISIBLE);
						mShuffleButton.setVisibility(View.INVISIBLE);
						mQueueButton.setVisibility(View.INVISIBLE);
						setRepeatButtonImage();
						setShuffleButtonImage();
					} else {
						mRepeatButton.setVisibility(View.VISIBLE);
						mShuffleButton.setVisibility(View.VISIBLE);
						mQueueButton.setVisibility(View.VISIBLE);
						setRepeatButtonImage();
						setShuffleButtonImage();
					}
					setPauseButtonImage();
					setAbRepeatingButtonImage();
					setABJumpButtonsForABRepeat();
					return;
				}
			} catch (RemoteException ex) {
			}
			// Service is dead or not playing anything. If we got here as part
			// of a "play this file" Intent, exit. Otherwise go to the Music
			// app start screen.
			if (getIntent().getData() == null) {
				Intent intent = new Intent(Intent.ACTION_MAIN);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				intent.setClass(MediaPlaybackActivity.this,
						MusicBrowserActivity.class);
				startActivity(intent);
			}
			finish();
		}

		public void onServiceDisconnected(ComponentName classname) {
			mService = null;
		}
	};
    
    private void setRepeatButtonImage() {
        if (mService == null) return;
        try {
            switch (mService.getRepeatMode()) {
                case MediaPlaybackService.REPEAT_ALL:
                    mRepeatButton.setImageResource(R.drawable.ic_mp_repeat_all_btn);
                    break;
                case MediaPlaybackService.REPEAT_CURRENT:
                    mRepeatButton.setImageResource(R.drawable.ic_mp_repeat_once_btn);
                    break;
                default:
                    mRepeatButton.setImageResource(R.drawable.ic_mp_repeat_off_btn);
                    break;
            }
        } catch (RemoteException ex) {
        }
    }
    
    private void setShuffleButtonImage() {
        if (mService == null) return;
        try {
            switch (mService.getShuffleMode()) {
                case MediaPlaybackService.SHUFFLE_NONE:
                    mShuffleButton.setImageResource(R.drawable.ic_mp_shuffle_off_btn);
                    break;
                case MediaPlaybackService.SHUFFLE_AUTO:
                    mShuffleButton.setImageResource(R.drawable.ic_mp_partyshuffle_on_btn);
                    break;
                default:
                    mShuffleButton.setImageResource(R.drawable.ic_mp_shuffle_on_btn);
                    break;
            }
        } catch (RemoteException ex) {
        }
    }
    
    private void setPauseButtonImage() {
        try {
            if (mService != null && mService.isPlaying()) {
                mPauseButton.setImageResource(android.R.drawable.ic_media_pause);
            } else {
            	if (mService != null && mService.getInABPause()) {
            		mPauseButton.setImageResource(android.R.drawable.ic_media_pause);
            	} else {
            		mPauseButton.setImageResource(android.R.drawable.ic_media_play);
            	}
            }
        } catch (RemoteException ex) {
        }
    }
    
    private void setAbRepeatingButtonImage() {
    	try {
    		if (mService != null) {
//    			Log.i(LOGTAG,"TraverseMode2364:"+mService.abListTraverseMode());
    			setPrevPauseNextToTravarseMode(mService.abListTraverseMode());
    			switch (mService.abRepeatingState()) {
            		case MediaPlaybackService.ABREPEATING_NOT:
            			mAbRepeatingButton.setImageResource(R.drawable.apoint_or_bookmark);
            			break;
            		case MediaPlaybackService.ABREPEATING_WAITING:
            			mAbRepeatingButton.setImageResource(R.drawable.bpoint);
            			break;
            		case MediaPlaybackService.ABREPEATING_NOW:
            			mAbRepeatingButton.setImageResource(R.drawable.delpoints);
            			break;
            		default:
            			mAbRepeatingButton.setImageResource(R.drawable.apoint_or_bookmark);
    			}
    			} else {
    				mAbRepeatingButton.setImageResource(R.drawable.apoint_or_bookmark);
    		}
        } catch (RemoteException ex) {
        }
    }
    
    private void setPrevPauseNextToTravarseMode (boolean abmode) {
    	if (abmode) {
    		mPrevButton.getDrawable().setColorFilter(0xFF00FFFF, PorterDuff.Mode.MULTIPLY);
			mPauseButton.getDrawable().setColorFilter(0xFF00FFFF, PorterDuff.Mode.MULTIPLY);
			mNextButton.getDrawable().setColorFilter(0xFF00FFFF, PorterDuff.Mode.MULTIPLY);
    	} else {
    		mPrevButton.getDrawable().clearColorFilter();
			mPauseButton.getDrawable().clearColorFilter();
			mNextButton.getDrawable().clearColorFilter();
    	}
    	mPrevButton.invalidate();
    	mPauseButton.invalidate();
    	mNextButton.invalidate();
    }
    
    private ImageView mAlbum;
    private TextView mCurrentTime;
    private TextView mTotalTime;
    private Button mAPosTime;
    private Button mBPosTime;
    private TextView mArtistName;
    private TextView mAlbumName;
    private TextView mTrackName;
    private ProgressBar mProgress;
    private long mPosOverride = -1;
    private boolean mFromTouch = false;
    private long mDuration;
    private int seekmethod;
    private boolean paused;

    private static final int REFRESH = 1;
    private static final int QUIT = 2;
    private static final int GET_ALBUM_ART = 3;
    private static final int ALBUM_ART_DECODED = 4;

    public void setJumpButtonContents(){
    	float jumpdistance;
        mJumpButtonCenter.requestFocus(); 
        mJumpButtonCenter.setShadowLayer(3, 0, 0, R.color.button_text_shadow_color);
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(1);
        df.setMinimumFractionDigits(1);
        
        jumpdistance = (((float) mPreferences.getLong("jumpdisttwo", 5000))/1000);
        if (jumpdistance < 0) {
        	mJumpButtonCenter.setText(df.format(-jumpdistance));
            mJumpButtonCenter
            .setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.jump_forward), null, null);
        } else {
        	mJumpButtonCenter.setText(df.format(jumpdistance));
            mJumpButtonCenter
            .setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.jump_backward), null, null);
        }
        mJumpButtonCenter.setVisibility(View.VISIBLE);

        mAPosTime.requestFocus();    
        mAPosTime.setShadowLayer(3, 0, 0, R.color.button_text_shadow_color);
        jumpdistance = (((float) mPreferences.getLong("jumpdistone", 7500))/1000);
        if (jumpdistance < 0) {
        	mAPosTime.setText(df.format(-jumpdistance));
        	mAPosTime
            .setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.jump_forward), null, null);
        } else {
        	mAPosTime.setText(df.format(jumpdistance));
        	mAPosTime
            .setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.jump_backward), null, null);
        }
        mAPosTime.setVisibility(View.VISIBLE);
        
        mBPosTime.requestFocus();
        mBPosTime.setShadowLayer(3, 0, 0, R.color.button_text_shadow_color);
        jumpdistance = (((float) mPreferences.getLong("jumpdistthree", 2500))/1000);
        if (jumpdistance < 0) {
        	mBPosTime.setText(df.format(-jumpdistance));
        	mBPosTime
            .setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.jump_forward), null, null);
        } else {
        	mBPosTime.setText(df.format(jumpdistance));
        	mBPosTime
            .setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.jump_backward), null, null);
        }
        mBPosTime.setVisibility(View.VISIBLE);
    }
    
    public void setABJumpButtonsForABRepeat () {
    	String durationformat;
    	StringBuilder sFormatBuilder = new StringBuilder();
    	Formatter sFormatter = new Formatter(sFormatBuilder, Locale.getDefault());
    	Object[] sTimeArgs = new Object[5];
    	final Object[] timeArgs = sTimeArgs;
    	Long apos;
    	Long bpos;
    	
    	
    	try {
    		if (mService != null) {
//    			Log.i(LOGTAG,"TraverseMode2488:"+mService.abListTraverseMode());
    			setPrevPauseNextToTravarseMode(mService.abListTraverseMode());
            switch (mService.abRepeatingState()) {
            	case MediaPlaybackService.ABREPEATING_WAITING:
//            		Log.i(LOGTAG,"ABREPEATING:WAITING");
            		mAbRepeatingButton.setImageResource(R.drawable.bpoint);
//            		mService.setAPos();
            		apos = mService.getAPos()/1000;
            		durationformat = 
                       apos < 3600 ? "%2$d:%5$02d" : "%1$d:%3$02d:%5$02d";
            		sFormatBuilder.setLength(0);
                    timeArgs[0] = apos / 3600;
                    timeArgs[1] = apos / 60;
                    timeArgs[2] = (apos / 60) % 60;
                    timeArgs[3] = apos;
                    timeArgs[4] = apos % 60;
            		mAPosTime.setText(sFormatter.format(durationformat, timeArgs).toString());
            		mAPosTime
            		.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.apostime_or_bookmark),
            				null, null);
            		mAPosTime.setVisibility(View.VISIBLE);
            		mJumpButtonCenter
            		.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.ic_bookmark_flat), null, null);
            		mJumpButtonCenter.setText("B Mark");
//            		mJumpButtonCenter.setVisibility(View.INVISIBLE);
            		mBPosTime
            		.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.cancel_flat), null, null);
            		mBPosTime.setText("Cancel");
            		mBPosTime.setVisibility(View.VISIBLE);
            		break;
            	case MediaPlaybackService.ABREPEATING_NOW:
//            		Log.i(LOGTAG,"ABREPEATING:NOW");
//            		if (mService.setBPos()) {
            			mService.setABPause(mPreferences.getLong("abpause", 0));
            			mAbRepeatingButton.setImageResource(R.drawable.delpoints);
                		bpos = mService.getBPos()/1000;
                		durationformat = 
                           bpos < 3600 ? "%2$d:%5$02d" : "%1$d:%3$02d:%5$02d";
                		sFormatBuilder.setLength(0);
                        timeArgs[0] = bpos / 3600;
                        timeArgs[1] = bpos / 60;
                        timeArgs[2] = (bpos / 60) % 60;
                        timeArgs[3] = bpos;
                        timeArgs[4] = bpos % 60;
                		mBPosTime.setText(sFormatter.format(durationformat, timeArgs).toString());
                		mBPosTime
                		.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.bpostime),
                				null, null);
                		mBPosTime.setVisibility(View.VISIBLE);
                		mJumpButtonCenter.setText("AB");
                		mJumpButtonCenter
                		.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.record),
                				null, null);
                		apos = mService.getAPos()/1000;
                		durationformat = 
                           apos < 3600 ? "%2$d:%5$02d" : "%1$d:%3$02d:%5$02d";
                		sFormatBuilder.setLength(0);
                        timeArgs[0] = apos / 3600;
                        timeArgs[1] = apos / 60;
                        timeArgs[2] = (apos / 60) % 60;
                        timeArgs[3] = apos;
                        timeArgs[4] = apos % 60;
                		mAPosTime.setText(sFormatter.format(durationformat, timeArgs).toString());
                		mAPosTime
                		.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.apostime),
                				null, null);
                		mJumpButtonCenter.setVisibility(View.VISIBLE);
                		
            			
//            		} else {
//                        showToast(R.string.startendtooclose);
//            		}
            		break;
            	case MediaPlaybackService.ABREPEATING_NOT:
//            		Log.i(LOGTAG,"ABREPEATING:NOT");
//            		Log.i(LOGTAG, "AB_NOT in setABJumpButtonsForABRepeat");
//            		mService.clearABPos();
            		mAbRepeatingButton.setImageResource(R.drawable.apoint_or_bookmark);
            		setJumpButtonContents();
//            		showToast(R.string.abpos_cleared);
//            		mAPosTime.setVisibility(View.INVISIBLE);
//            		mBPosTime.setVisibility(View.INVISIBLE);
            		break;
            	default:
            		mAbRepeatingButton.setImageResource(R.drawable.apoint_or_bookmark);
            }}
        } catch (RemoteException ex) {
        }
    }

    
    private void notifyChange() {
    	Intent i = new Intent(mContext, MediaPlaybackService.class);
        i.setAction(MediaPlaybackService.SERVICECMD);
        i.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDNOTIFYCHANGE);
        mContext.startService(i);
    }
    
    private void queueNextRefresh(long delay) {
        if (!paused) {
            Message msg = mHandler.obtainMessage(REFRESH);
            mHandler.removeMessages(REFRESH);
            mHandler.sendMessageDelayed(msg, delay);
        }
    }

    private long refreshNow() {
        if(mService == null)
            return 500;
        try {
            long pos = mPosOverride < 0 ? mService.position() : mPosOverride;
            long remaining = 1000 - (pos % 1000);
            if ((pos >= 0) && (mDuration > 0)) {
                mCurrentTime.setText(MusicUtils.makeTimeString(this, pos / 1000));
                
                if (mService.isPlaying()) {
                    mCurrentTime.setVisibility(View.VISIBLE);
                } else {
                    // blink the counter
                    int vis = mCurrentTime.getVisibility();
                    mCurrentTime.setVisibility(vis == View.INVISIBLE ? View.VISIBLE : View.INVISIBLE);
                    remaining = 500;
                }

//                mProgress.setProgress((int) (1000 * pos / mDuration));
                
                switch (mService.abRepeatingState()){
                case MediaPlaybackService.ABREPEATING_NOW:
                	if (mABWideBar) {
                		long apos = mService.getAPos();
                		long bpos = mService.getBPos();
                		mProgress.setProgress((int) (1000 * (pos - apos) / (bpos - apos)));
                    	mProgress.setSecondaryProgress(1000);
                	} else {
                		mProgress.setProgress((int) (1000 * pos / mDuration));
                    	mProgress.setSecondaryProgress((int) (1000 * mService.getBPos() / mDuration));
                	}
                	mAPosTime.setVisibility(View.VISIBLE);
                	mBPosTime.setVisibility(View.VISIBLE);
                	break;
                case MediaPlaybackService.ABREPEATING_WAITING:
                	mProgress.setProgress((int) (1000 * pos / mDuration));
                	mProgress.setSecondaryProgress(0);
                	mAPosTime.setVisibility(View.VISIBLE);
                	mBPosTime.setVisibility(View.VISIBLE);
                	break;
                case MediaPlaybackService.ABREPEATING_NOT:
                	mProgress.setProgress((int) (1000 * pos / mDuration));
                	mProgress.setSecondaryProgress(0);
//                	mAPosTime.setVisibility(View.INVISIBLE);
//                	mBPosTime.setVisibility(View.INVISIBLE);
                	break;
            	default:
            		mProgress.setProgress((int) (1000 * pos / mDuration));
            		mProgress.setSecondaryProgress(0);
            		mAPosTime.setVisibility(View.INVISIBLE);
                	mBPosTime.setVisibility(View.INVISIBLE);
                }
            } else {
                mCurrentTime.setText("--:--");
                mProgress.setProgress(1000);
            }
            // return the number of milliseconds until the next full second, so
            // the counter can be updated at just the right time
            return remaining;
        } catch (RemoteException ex) {
        }
        return 500;
    }
    
    private void sendDbModifiedtoService() {
//    	Log.i(LOGTAG, "DB Modified Msg Sent");
    	Intent intent = new Intent();
        intent.setAction(MediaPlaybackService.SERVICECMD);
        intent.putExtra("command", MediaPlaybackService.AB_DB_MODIFIED);
        sendBroadcast(intent);
    }
    
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ALBUM_ART_DECODED:
                    mAlbum.setImageBitmap((Bitmap)msg.obj);
                    mAlbum.getDrawable().setDither(true);
                    break;

                case REFRESH:
                    long next = refreshNow();
                    queueNextRefresh(next);
                    break;
                    
                case QUIT:
                    // This can be moved back to onCreate once the bug that prevents
                    // Dialogs from being started from onCreate/onResume is fixed.
                    new AlertDialog.Builder(MediaPlaybackActivity.this)
                            .setTitle(R.string.service_start_error_title)
                            .setMessage(R.string.service_start_error_msg)
                            .setPositiveButton(R.string.service_start_error_button,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            finish();
                                        }
                                    })
                            .setCancelable(false)
                            .show();
                    break;

                default:
                    break;
            }
        }
    };

    private BroadcastReceiver mStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
//            Log.i(LOGTAG,"StatusListner:" + action);
            if (action.equals(MediaPlaybackService.META_CHANGED)) {
                // redraw the artist/title info and
                // set new max for progress bar
                updateTrackInfo();
                setPauseButtonImage();
                setAbRepeatingButtonImage();
                setJumpButtonContents();
                queueNextRefresh(1);
            } else if (action.equals(MediaPlaybackService.PLAYBACK_COMPLETE)) {
                if (mOneShot) {
                    finish();
                } else {
                    setPauseButtonImage();
                    setAbRepeatingButtonImage();
                    setJumpButtonContents();
                }
            } else if (action.equals(MediaPlaybackService.PLAYSTATE_CHANGED)) {
                setPauseButtonImage();
                setAbRepeatingButtonImage();
            } else if (action.equals(MediaPlaybackService.ABPOS_CLEARD_BY_BT)) {
//            	Log.i(LOGTAG,"ABPOS_CLEARD_BY_BT Received");
            	setAbRepeatingButtonImage();
            	setJumpButtonContents();
            } else if (action.equals(MediaPlaybackService.ABPOS_SET_BY_BT)) {
//            	Log.i(LOGTAG,"ABPOS_SET_BY_BT Received");
            	setABJumpButtonsForABRepeat ();
            }
        }
    };

    private static class AlbumSongIdWrapper {
        public long albumid;
        public long songid;
        AlbumSongIdWrapper(long aid, long sid) {
            albumid = aid;
            songid = sid;
        }
    }
    
    private void updateTrackInfo() {
        if (mService == null) {
            return;
        }    
        
        try {
            String path = mService.getPath();
            if (path == null) {
                finish();
                return;
            }
            
//            AssetFileDescriptor fd = null;
//            try {
//                ContentResolver resolver = this.getContentResolver();
//                fd = resolver.openAssetFileDescriptor(uri, "r");
//                if (fd == null) {
//                    return;
//                }
//                // Note: using getDeclaredLength so that our behavior is the same
//                // as previous versions when the content provider is returning
//                // a full file.
//                
//                if (fd.getDeclaredLength() < 0) {
////                    setDataSource(fd.getFileDescriptor());
//                } else {
////                    setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getDeclaredLength());
//                }
//                return;
//            } catch (SecurityException ex) {
//            } catch (IOException ex) {
//            } finally {
//                if (fd != null) {
//                    fd.close();
//                }
//            }
            
            
            
            
//            try {
//            MP3File mp3 = new MP3File(Environment.getExternalStorageDirectory() +
//            		"/MLKDream_64kb.mp3");
//
////    	    Log.i("MediaPlaybackActivity",("Lyric: " + mp3.getID3v2Tag().getFrame("USLT")));
//    	    
//    	    String buff = mp3.getID3v2Tag().getFrame("USLT").toString();
//
//            } catch (Exception e) {
//            	Log.e("MediaPlaybackActivity","Lyric Tag Error: " + e);
//            }
            
            long songid = mService.getAudioId(); 
            
//            Log.i("MediaPlaybackActivity", "Media DATA: "+mService.getData());
            if (songid < 0 && path.toLowerCase().startsWith("http://")) {
                // Once we can get album art and meta data from MediaPlayer, we
                // can show that info again when streaming.
                ((View) mArtistName.getParent()).setVisibility(View.INVISIBLE);
                ((View) mAlbumName.getParent()).setVisibility(View.INVISIBLE);
                mAlbum.setVisibility(View.GONE);
                mTrackName.setText(path);
                mAlbumArtHandler.removeMessages(GET_ALBUM_ART);
                mAlbumArtHandler.obtainMessage(GET_ALBUM_ART, new AlbumSongIdWrapper(-1, -1)).sendToTarget();
            } else {
                ((View) mArtistName.getParent()).setVisibility(View.VISIBLE);
                ((View) mAlbumName.getParent()).setVisibility(View.VISIBLE);
                String artistName = mService.getArtistName();
//                if (MediaStore.UNKNOWN_STRING.equals(artistName)) {
//                    artistName = getString(R.string.unknown_artist_name);
//                }
                mArtistName.setText(artistName);
                String albumName = mService.getAlbumName();
                long albumid = mService.getAlbumId();
//                if (MediaStore.UNKNOWN_STRING.equals(albumName)) {
//                    albumName = getString(R.string.unknown_album_name);
//                    albumid = -1;
//                }
                mAlbumName.setText(albumName);
                mTrackName.setText(mService.getTrackName());
                mAlbumArtHandler.removeMessages(GET_ALBUM_ART);
                mAlbumArtHandler.obtainMessage(GET_ALBUM_ART, new AlbumSongIdWrapper(albumid, songid)).sendToTarget();
                mAlbum.setVisibility(View.VISIBLE);
            }
            mDuration = mService.duration();
            mTotalTime.setText(MusicUtils.makeTimeString(this, mDuration / 1000));
            
            if (mService.abRepeatingState() == MediaPlaybackService.ABREPEATING_NOW) {
            mAPosTime.setText(MusicUtils.makeTimeString(this, mService.getAPos() / 1000));
            mAPosTime
    		.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.apostime),
    				null, null);
            mBPosTime.setText(MusicUtils.makeTimeString(this, mService.getBPos() / 1000));
            mBPosTime
    		.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.bpostime),
    				null, null);
            mJumpButtonCenter.setText("AB");
            mJumpButtonCenter
    		.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.record),
    				null, null);
            }
            if (mService.abRepeatingState() == MediaPlaybackService.ABREPEATING_WAITING) {
                mAPosTime.setText(MusicUtils.makeTimeString(this, mService.getAPos() / 1000));
                mAPosTime
        		.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.apostime_or_bookmark),
        				null, null);
                mJumpButtonCenter
        		.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.ic_bookmark_flat), null, null);
        		mJumpButtonCenter.setText("B Mark");
        		mBPosTime
        		.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.cancel_flat), null, null);
        		mBPosTime.setText("Cancel");
        		mBPosTime.setVisibility(View.VISIBLE);
//                mJumpButtonCenter.setVisibility(View.INVISIBLE);
            }
            
        } catch (RemoteException ex) {
            finish();
        }
    }
    
    public class AlbumArtHandler extends Handler {
        private long mAlbumId = -1;
        
        public AlbumArtHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg)
        {
            long albumid = ((AlbumSongIdWrapper) msg.obj).albumid;
            long songid = ((AlbumSongIdWrapper) msg.obj).songid;
            if (msg.what == GET_ALBUM_ART && (mAlbumId != albumid || albumid < 0)) {
                // while decoding the new image, show the default album art
                Message numsg = mHandler.obtainMessage(ALBUM_ART_DECODED, null);
                mHandler.removeMessages(ALBUM_ART_DECODED);
                mHandler.sendMessageDelayed(numsg, 300);
                Bitmap bm = MusicUtils.getArtwork(MediaPlaybackActivity.this, songid, albumid);
                if (bm == null) {
                    bm = MusicUtils.getArtwork(MediaPlaybackActivity.this, songid, -1);
                    albumid = -1;
                }
                if (bm != null) {
                    numsg = mHandler.obtainMessage(ALBUM_ART_DECODED, bm);
                    mHandler.removeMessages(ALBUM_ART_DECODED);
                    mHandler.sendMessage(numsg);
                }
                mAlbumId = albumid;
            }
        }
    }
    
    private static class Worker implements Runnable {
        private final Object mLock = new Object();
        private Looper mLooper;
        
        /**
         * Creates a worker thread with the given name. The thread
         * then runs a {@link android.os.Looper}.
         * @param name A name for the new thread
         */
        Worker(String name) {
            Thread t = new Thread(null, this, name);
            t.setPriority(Thread.MIN_PRIORITY);
            t.start();
            synchronized (mLock) {
                while (mLooper == null) {
                    try {
                        mLock.wait();
                    } catch (InterruptedException ex) {
                    }
                }
            }
        }
        
        public Looper getLooper() {
            return mLooper;
        }
        
        public void run() {
            synchronized (mLock) {
                Looper.prepare();
                mLooper = Looper.myLooper();
                mLock.notifyAll();
            }
            Looper.loop();
        }
        
        public void quit() {
            mLooper.quit();
        }
    }
}

