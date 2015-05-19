package com.kojimahome.music21;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.os.RemoteException;
import android.net.Uri;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.MediaController;
import com.kojimahome.music21.MusicUtils.Defs;
import com.kojimahome.music21.MusicUtils.ServiceToken;

//import com.kojimahome.music21.PolicyManager;

//import com.kojimahome.music21.MusicUtils.*;







import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Locale;

/**
 * A view containing controls for a MediaPlayer. Typically contains the
 * buttons like "Play/Pause", "Rewind", "Fast Forward" and a progress
 * slider. It takes care of synchronizing the controls with the state
 * of the MediaPlayer.
 * <p>
 * The way to use this class is to instantiate it programatically.
 * The MediaController will create a default set of controls
 * and put them in a window floating above your application. Specifically,
 * the controls will float above the view specified with setAnchorView().
 * The window will disappear if left idle for three seconds and reappear
 * when the user touches the anchor view.
 * <p>
 * Functions like show() and hide() have no effect when MediaController
 * is created in an xml layout.
 * 
 * MediaController will hide and
 * show the buttons according to these rules:
 * <ul>
 * <li> The "previous" and "next" buttons are hidden until setPrevNextListeners()
 *   has been called
 * <li> The "previous" and "next" buttons are visible but disabled if
 *   setPrevNextListeners() was called with null listeners
 * <li> The "rewind" and "fastforward" buttons are shown unless requested
 *   otherwise by using the MediaController(Context, boolean) constructor
 *   with the boolean set to false
 * </ul>
 */
@SuppressLint("WrongViewCast")
public class MyMediaController extends Dialog implements MusicUtils.Defs {

    private MediaPlayerControl  mPlayer;
    private Context             mContext;
    private View                mAnchor;
    private WindowManager       mWindowManager;
    private Window              mWindow;
    private View                mDecor;
    private ProgressBar         mProgress;
    private TextView            mEndTime, mCurrentTime;
    private boolean             mShowing;
    private boolean             mDragging;
    private static final int    sDefaultTimeout = 7200000;
    private static final int    FADE_OUT = 1;
    private static final int    SHOW_PROGRESS = 2;
    private static final int 	BPOINT_HALF_REACHED = 3;
    private static final int 	BPOINT_REACHED = 4;
    private static final int    PREV_NEXT_RECEIVED = 5;
    private static final int    SPEED_STOP = 6;
    private static final int    SPEED_START = 7;
	private static final int 	MODE_PRIVATE = 0;
    private boolean             mUseFastForward;
    private boolean             mFromXml;
    private boolean             mListenersSet;
//    private View.OnClickListener mNextListener, mPrevListener;
    StringBuilder               mFormatBuilder;
    Formatter                   mFormatter;
    private ImageButton         mPauseButton;
    private ImageButton         mFfwdButton;
    private ImageButton         mRewButton;
    private ImageButton         mNextButton;
    private ImageButton         mPrevButton;
    private View		        mRoot;
    private FrameLayout			mRootLayout;
    private ImageButton mAbRepeatingButton;
    private ImageButton mSpeedButton;
    private Button mJumpButtonCenter;
    private Button mAPosTime;
    private Button mBPosTime;
    private ImageButton mTopBottom;
    private SharedPreferences mPreferences;
    private static AlertDialog mSpeedDialog;
    private static int mAPos = 0;
    private static int mBPos = 0;
    private static int mSpeed = 1;
    private static int mSpeedRunmsecs = 200;
    private static int mSpeedPausemsecs = 0;
    private static boolean mPausedForSpeed = false;
    private static boolean mAbEdited = false;
    private static long mAbRecRowId = -1;
    private Toast mToast;
//    private static int mAbRepeatingState = 0; //ABREPEATING_NOT
    private ABDbAdapter mABDbHelper;
    private static boolean mPickingABPos;
    private static boolean mABWideBar = true;
    private Cursor mAbCursor = null;
    private static int mPosInAbList = -1;
    private static boolean mAbListTraverseMode = false;
    
    private static final int RECORD_ABPOINTS = 0;
    private static final int BROWSE_ABPOINTS = 1;
    
    public static final long CHECK_BPOINT_DURATION = 3000;
    
    private static final String TAG = "MyMediaController";
    public static final int ABREPEATING_NOT = 0;
    public static final int ABREPEATING_WAITING = 1;
    public static final int ABREPEATING_NOW = 2;
    public static final String MEDIA_BUTTON_ACTION_VIDEO = "com.kojimahome.music21.mediabuttonvideo";
    private static boolean mTopControl = false;
    private boolean mAbSelectedfromList = false;
    private boolean mUpdatePausePlayCalledAfterSeek = false;
    private boolean mTempPauseMightOccur = false;
    private static int mAbRepeatingState = ABREPEATING_NOT;
    
    private BroadcastReceiver mUnplugReceiver = null;
    private AudioManager mAudioManager;
    private static boolean mWasPlaying = false;
    private TelephonyManager teleMgr = null;
    private static int mPausedPosition = 0;
    private static boolean mKeyeventTooClose = false;
    
    OrientationEventListener myOrientationEventListener;

    
//    private FrameLayout         mRoot = new FrameLayout(mContext) {
//      @Override
//      public void onFinishInflate() {
//          if (mRoot != null)
//              initControllerView(this);
//      }
//    };
    
//    public MyMediaController(Context context, AttributeSet attrs) {
//        super(context, attrs);
//        mRoot = this;
//        mContext = context;
//        mUseFastForward = true;
//        mFromXml = true;
//    }

//    @Override
//    public void onFinishInflate() {
//        if (mRoot != null)
//            initControllerView(mRoot);
//    }

//    public MyMediaController(Context context, boolean useFastForward) {
//        super(context);
//        mContext = context;
//        mUseFastForward = useFastForward;
////        initFloatingWindow();
//    }
    
    public static void clearAfterRelease() {
    	mAPos = 0;
    	mBPos = 0;
    	mAbRepeatingState = ABREPEATING_NOT;
    	mAbListTraverseMode = false;
    	mPosInAbList = -1;
    }

    public MyMediaController(Context context) {
        super(context);

        mContext = context;
        mRootLayout = new FrameLayout(mContext) {
          @Override
          public void onFinishInflate() {
              if (mRoot != null)
                  initControllerView(this);
          }
        };
        mRoot = mRootLayout;
        mUseFastForward = true;
        initFloatingWindow();
        
        mABDbHelper = new ABDbAdapter(context);
        
        mPreferences = mContext.getSharedPreferences("ABRepeatVideo", MODE_PRIVATE);
        if (mPreferences.getBoolean("abinitialzed", false) == false) {
        	Editor ed = mPreferences.edit();
        	ed.putLong("jumpdistone", 7500);
        	ed.putLong("jumpdisttwo", 5000);
        	ed.putLong("jumpdistthree", 2500);
        	ed.putLong("abshiftone", 2000);
        	ed.putLong("abshifttwo", 1000);
        	ed.putLong("abshiftthree", 200);
        	ed.putBoolean("abinitialzed", true);
        	ed.commit();
        }
        registerHeadsetUnplugListener();
        
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
	        mAudioManager.registerMediaButtonEventReceiver(new ComponentName(context.getPackageName(),
	                MediaButtonIntentReceiverVideo.class.getName()));
        }
        teleMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        teleMgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        
        mPosInAbList = -1;
        mAbListTraverseMode = false;
        
        IntentFilter commandFilter = new IntentFilter();
        commandFilter.addAction(MediaPlaybackService.SERVICECMD);
        context.registerReceiver(mIntentReceiver, commandFilter);
        
//        myOrientationEventListener
//        = new OrientationEventListener(mContext, SensorManager.SENSOR_DELAY_NORMAL){
//
//         @Override
//         public void onOrientationChanged(int arg0) {
////          hide();
//         }};
         
//         if (myOrientationEventListener.canDetectOrientation()){
//            myOrientationEventListener.enable();
//         }
    }

    private void initFloatingWindow() {

        mWindowManager = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
//        mWindow = PolicyManager.makeNewWindow(mContext);

        mWindow = getWindow();
//        mWindowManager = mWindow.getWindowManager();
        mWindow.setWindowManager(mWindowManager, null, null);
        mWindow.requestFeature(Window.FEATURE_NO_TITLE);
        mDecor = mWindow.getDecorView();
        mDecor.setOnTouchListener(mTouchListener);
//        LayoutInflater inflater =
//        (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//        View view = inflater.inflate(R.layout.media_controller, null);
        mWindow.setContentView(mRootLayout);
        mWindow.setBackgroundDrawableResource(android.R.color.transparent);
        
        // While the media controller is up, the volume control keys should
        // affect the media stream type
        mWindow.setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mRootLayout.setFocusable(true);
        mRootLayout.setFocusableInTouchMode(true);
        mRootLayout.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        mRootLayout.requestFocus();
    }

    private OnTouchListener mTouchListener = new OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (mShowing) {
                    hide();
                }
            }
            return false;
        }
    };
    
    public void setMediaPlayer(MediaPlayerControl player) {
        mPlayer = player;
     // Only when debugged
//        if (LocalDebugMode.debugMode) {
//        	Log.i("@@@VideoDebug", "iupdatePausePlay called in setMediaPlayer");
//    	}
        updatePausePlay();
//      if (myOrientationEventListener.canDetectOrientation()){
//      myOrientationEventListener.enable();
//   }
    }

    /**
     * Set the view that acts as the anchor for the control view.
     * This can for example be a VideoView, or your Activity's main view.
     * @param view The view to which to anchor the controller when it is visible.
     */
    public void setAnchorView(View view) {
        mAnchor = view;

        FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.FILL_PARENT
        );
        
        mRootLayout.removeAllViews();
        View v = makeControllerView();
        mRootLayout.addView(v, frameParams);
        
    }

    /**
     * Create the view that holds the widgets that control playback.
     * Derived classes can override this to create their own.
     * @return The controller view.
     * @hide This doesn't work as advertised
     */
    
    public View makeControllerView() {
        LayoutInflater inflate = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mRoot = inflate.inflate(R.layout.media_controller, null);

        initControllerView(mRoot);

        return mRoot;
    }
    
    public void registerHeadsetUnplugListener() {
        if (mUnplugReceiver == null) {
            mUnplugReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (action.equals(Intent.ACTION_HEADSET_PLUG)
                    		&&(intent.getIntExtra("state", -1) == 0)&&(mPlayer != null)) {
                    	if (isPlaying()) {
                    		pause();
                    		setPauseButtonImage();
//                            mPausedByTransientLossOfFocus = false;
                        }
//                        Log.i(LOGTAG,"ACTION_HEADSET_PLUG Received: " + intent.getIntExtra("state", -1));
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO &&
                    		action.equals(AudioManager.ACTION_SCO_AUDIO_STATE_CHANGED)
                    		&&(intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, 0) 
                    				== AudioManager.SCO_AUDIO_STATE_DISCONNECTED)&&(mPlayer != null)) {
                    	if (isPlaying()) {
                    		pause();
                    		setPauseButtonImage();
//                            mPausedByTransientLossOfFocus = false;
                        }
//  This is not working for unknown reasons, but other listeners are takeing care of it.
//                    	Log.i(LOGTAG,"ACTION_SCO_AUDIO_STATE_CHANGED Received: " + intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE , -1));
                    } else if (action.equals(MyMediaController.MEDIA_BUTTON_ACTION_VIDEO)
                    		&&(intent.getStringExtra(MediaPlaybackService.CMDNAME).equals(MediaPlaybackService.CMDTOGGLEPAUSE))
                    		&&(mPlayer != null)) {
                    	if (isPlaying()) {
                    		pause();
                    		setPauseButtonImage();
//                            mPausedByTransientLossOfFocus = false;
                        }
                    }
                }
            };
            IntentFilter iFilter = new IntentFilter();
            iFilter.addAction(Intent.ACTION_HEADSET_PLUG);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) iFilter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_CHANGED);
            iFilter.addAction(MyMediaController.MEDIA_BUTTON_ACTION_VIDEO);
            mContext.registerReceiver(mUnplugReceiver, iFilter);
        }
    }
    
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String cmd = intent.getStringExtra("command");
            if (MediaPlaybackService.AB_DB_MODIFIED.equals(cmd)) {
                clearAbCursor();
            }
        }
    };
    
    @Override
    protected void onStop() {
    	
    	if (mUnplugReceiver != null) {
            if (mContext != null) mContext.unregisterReceiver(mUnplugReceiver);
            mUnplugReceiver = null;
        }
    	mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
    	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
	        mAudioManager.registerMediaButtonEventReceiver(new ComponentName(mContext.getPackageName(),
	                MediaButtonIntentReceiver.class.getName()));
    	}
        if ((teleMgr != null)&&(mPhoneStateListener != null)) {
        teleMgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        if (teleMgr != null)teleMgr = null;
        
    	super.onStop();
    }
    
    private void initControllerView(View v) {
        mPauseButton = (ImageButton) v.findViewById(R.id.pause);
        if (mPauseButton != null) {
            mPauseButton.requestFocus();
            mPauseButton.setOnClickListener(mPauseListener);
            mPauseButton.setLongClickable(true);
            mPauseButton.setOnLongClickListener(mPauseLongListener);
        }
        
        mAbRepeatingButton = (ImageButton) v.findViewById(R.id.abrepeat);
        if (mAbRepeatingButton != null) {
        	mAbRepeatingButton.requestFocus();
        	mAbRepeatingButton.setImageResource(R.drawable.apoint);
            mAbRepeatingButton.setOnClickListener(mAbRepeatListener);
            mAbRepeatingButton.setLongClickable(true);
            mAbRepeatingButton.setOnLongClickListener(mAbRepeatingLongListner);
        }
        
        mSpeedButton = (ImageButton) v.findViewById(R.id.speed);
        if (mSpeedButton != null) {
        	mSpeedButton.requestFocus();
        	mSpeedButton.setImageResource(R.drawable.ic_speed_1);
        	mSpeed = 1;
            mSpeedButton.setOnClickListener(mSpeedListener);
            mSpeedButton.setLongClickable(false);
//            mSpeedButton.setOnLongClickListener(mSpeedLongListner);
        }
        
        mAPosTime = (Button) v.findViewById(R.id.apostime);
        if (mAPosTime != null) {
        	mAPosTime.requestFocus();
        	mAPosTime.setClickable(true);
            mAPosTime.setOnClickListener(mAPosTimeClickListner);
            mAPosTime.setLongClickable(true);
            mAPosTime.setOnLongClickListener(mAPosTimeLongListner);
        }
        
        mJumpButtonCenter = (Button) v.findViewById(R.id.jumpbuttoncenter);
        if (mJumpButtonCenter != null) {
        	mJumpButtonCenter.requestFocus();
        	mJumpButtonCenter.setClickable(true);
            mJumpButtonCenter.setOnClickListener(mJumpButtonCenterClickListner);
            mJumpButtonCenter.setLongClickable(true);
            mJumpButtonCenter.setOnLongClickListener(mJumpButtonCenterLongListner);
        }
        
        mBPosTime = (Button) v.findViewById(R.id.bpostime);
        if (mBPosTime != null) {
        	mBPosTime.requestFocus();
        	mBPosTime.setClickable(true);
            mBPosTime.setOnClickListener(mBPosTimeClickListner);
            mBPosTime.setLongClickable(true);
            mBPosTime.setOnLongClickListener(mBPosTimeLongListner);   
        }
        
        mTopBottom = (ImageButton) v.findViewById(R.id.topbottom);
        if (mTopBottom != null) {
        	mTopBottom.requestFocus();
        	mTopBottom.setClickable(true);
            mTopBottom.setOnClickListener(mTopBottomClickListner);
            mTopBottom.setLongClickable(true);
//            mTopBottom.setOnLongClickListener(mTopBottomLongListner);   
        }

        setJumpButtonContents();
        setAbRepeatingButtonImage();
        
        mFfwdButton = (ImageButton) v.findViewById(R.id.ffwd);
        if (mFfwdButton != null) {
            mFfwdButton.setOnClickListener(mFfwdListener);
            if (!mFromXml) {
                mFfwdButton.setVisibility(mUseFastForward ? View.VISIBLE : View.GONE);
            }
        }

        mRewButton = (ImageButton) v.findViewById(R.id.rew);
        if (mRewButton != null) {
            mRewButton.setOnClickListener(mRewListener);
            if (!mFromXml) {
                mRewButton.setVisibility(mUseFastForward ? View.VISIBLE : View.GONE);
            }
        }

        // By default these are hidden. They will be enabled when setPrevNextListeners() is called 
        mNextButton = (ImageButton) v.findViewById(R.id.next);
        if (mNextButton != null && !mFromXml && !mListenersSet) {
            mNextButton.setVisibility(View.GONE);
        }
        mPrevButton = (ImageButton) v.findViewById(R.id.prev);
        if (mPrevButton != null && !mFromXml && !mListenersSet) {
            mPrevButton.setVisibility(View.GONE);
        }

        mProgress = (ProgressBar) v.findViewById(R.id.mediacontroller_progress);
        if (mProgress != null) {
            if (mProgress instanceof SeekBar) {
                SeekBar seeker = (SeekBar) mProgress;
                seeker.setOnSeekBarChangeListener(mSeekListener);
            }
            mProgress.setMax(1000);
        }

        mEndTime = (TextView) v.findViewById(R.id.time);
        mCurrentTime = (TextView) v.findViewById(R.id.time_current);
        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());

        installPrevNextListeners();
    }

    public void setJumpButtonContents(){
    	float jumpdistance;
    	String durationformat;
    	StringBuilder sFormatBuilder = new StringBuilder();
    	Formatter sFormatter = new Formatter(sFormatBuilder, Locale.getDefault());
    	Object[] sTimeArgs = new Object[5];
    	final Object[] timeArgs = sTimeArgs;
    	int apos;
    	int bpos;
    	
    	
    	switch (mAbRepeatingState) {
    	case ABREPEATING_NOT:
    	
        mJumpButtonCenter.requestFocus(); 
        mJumpButtonCenter.setShadowLayer(3, 0, 0, R.color.button_text_shadow_color);
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(1);
        df.setMinimumFractionDigits(1);
        jumpdistance = (((float) mPreferences.getLong("jumpdisttwo", 5000))/1000);
        if (jumpdistance < 0) {
        	mJumpButtonCenter.setText(df.format(-jumpdistance));
            mJumpButtonCenter
            .setCompoundDrawablesWithIntrinsicBounds(null, mContext.getResources().getDrawable(R.drawable.jump_forward), null, null);
        } else {
        	mJumpButtonCenter.setText(df.format(jumpdistance));
            mJumpButtonCenter
            .setCompoundDrawablesWithIntrinsicBounds(null, mContext.getResources().getDrawable(R.drawable.jump_backward), null, null);
        }
        mJumpButtonCenter.setVisibility(View.VISIBLE);

        mAPosTime.requestFocus();    
        mAPosTime.setShadowLayer(3, 0, 0, R.color.button_text_shadow_color);
        jumpdistance = (((float) mPreferences.getLong("jumpdistone", 7500))/1000);
        if (jumpdistance < 0) {
        	mAPosTime.setText(df.format(-jumpdistance));
        	mAPosTime
            .setCompoundDrawablesWithIntrinsicBounds(null, mContext.getResources().getDrawable(R.drawable.jump_forward), null, null);
        } else {
        	mAPosTime.setText(df.format(jumpdistance));
        	mAPosTime
            .setCompoundDrawablesWithIntrinsicBounds(null, mContext.getResources().getDrawable(R.drawable.jump_backward), null, null);
        }
        mAPosTime.setVisibility(View.VISIBLE);
        
        mBPosTime.requestFocus();
        mBPosTime.setShadowLayer(3, 0, 0, R.color.button_text_shadow_color);
        jumpdistance = (((float) mPreferences.getLong("jumpdistthree", 2500))/1000);
        if (jumpdistance < 0) {
        	mBPosTime.setText(df.format(-jumpdistance));
        	mBPosTime
            .setCompoundDrawablesWithIntrinsicBounds(null, mContext.getResources().getDrawable(R.drawable.jump_forward), null, null);
        } else {
        	mBPosTime.setText(df.format(jumpdistance));
        	mBPosTime
            .setCompoundDrawablesWithIntrinsicBounds(null, mContext.getResources().getDrawable(R.drawable.jump_backward), null, null);
        }
        mBPosTime.setVisibility(View.VISIBLE);
        break;
    	case ABREPEATING_WAITING:
    		apos = getAPos()/1000;
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
    		.setCompoundDrawablesWithIntrinsicBounds(null, mContext.getResources().getDrawable(R.drawable.apostime_or_bookmark),
    				null, null);
    		mAPosTime.setVisibility(View.VISIBLE);
    		mJumpButtonCenter
    		.setCompoundDrawablesWithIntrinsicBounds(null, mContext.getResources().getDrawable(R.drawable.ic_bookmark_flat), null, null);
    		mJumpButtonCenter.setText("B Mark");
//    		mJumpButtonCenter.setVisibility(View.INVISIBLE);
    		mBPosTime
    		.setCompoundDrawablesWithIntrinsicBounds(null, mContext.getResources().getDrawable(R.drawable.cancel_flat), null, null);
    		mBPosTime.setText("Cancel");
    		mBPosTime.setVisibility(View.VISIBLE);
    	break;
    	case ABREPEATING_NOW:
    		bpos = getBPos()/1000;
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
    		.setCompoundDrawablesWithIntrinsicBounds(null, mContext.getResources().getDrawable(R.drawable.bpostime),
    				null, null);
    		mBPosTime.setVisibility(View.VISIBLE);
    		mJumpButtonCenter.setText("AB");
    		mJumpButtonCenter
    		.setCompoundDrawablesWithIntrinsicBounds(null, mContext.getResources().getDrawable(R.drawable.record),
    				null, null);
    		apos = getAPos()/1000;
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
    		.setCompoundDrawablesWithIntrinsicBounds(null, mContext.getResources().getDrawable(R.drawable.apostime),
    				null, null);
    		mJumpButtonCenter.setVisibility(View.VISIBLE);
    	break;
    	}
    }
    
    private View.OnClickListener mAbRepeatListener = new View.OnClickListener() {
        public void onClick(View v) {
        
        	String durationformat;
        	StringBuilder sFormatBuilder = new StringBuilder();
        	Formatter sFormatter = new Formatter(sFormatBuilder, Locale.getDefault());
        	Object[] sTimeArgs = new Object[5];
        	final Object[] timeArgs = sTimeArgs;
        	int apos;
        	int bpos;
        	
                switch (mAbRepeatingState) {
                	case ABREPEATING_NOT:
                		mAbRepeatingButton.setImageResource(R.drawable.bpoint);
                		setAPos();
                		apos = getAPos()/1000;
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
                		.setCompoundDrawablesWithIntrinsicBounds(null, mContext.getResources().getDrawable(R.drawable.apostime_or_bookmark),
                				null, null);
                		mAPosTime.setVisibility(View.VISIBLE);
                		mJumpButtonCenter
                		.setCompoundDrawablesWithIntrinsicBounds(null, mContext.getResources().getDrawable(R.drawable.ic_bookmark_flat), null, null);
                		mJumpButtonCenter.setText("B Mark");
//                		mJumpButtonCenter.setVisibility(View.INVISIBLE);
                		mBPosTime
                		.setCompoundDrawablesWithIntrinsicBounds(null, mContext.getResources().getDrawable(R.drawable.cancel_flat), null, null);
                		mBPosTime.setText("Cancel");
                		mBPosTime.setVisibility(View.VISIBLE);
                		break;
                	case ABREPEATING_WAITING:
                		if (setBPos()) {
                			mAbRepeatingButton.setImageResource(R.drawable.delpoints);
                    		bpos = getBPos()/1000;
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
                    		.setCompoundDrawablesWithIntrinsicBounds(null, mContext.getResources().getDrawable(R.drawable.bpostime),
                    				null, null);
                    		mBPosTime.setVisibility(View.VISIBLE);
                    		mJumpButtonCenter.setText("AB");
                    		mJumpButtonCenter
                    		.setCompoundDrawablesWithIntrinsicBounds(null, mContext.getResources().getDrawable(R.drawable.record),
                    				null, null);
                    		mAPosTime
                    		.setCompoundDrawablesWithIntrinsicBounds(null, mContext.getResources().getDrawable(R.drawable.apostime),
                    				null, null);
                    		mJumpButtonCenter.setVisibility(View.VISIBLE);
                    		
                			
                		} else {
                            showToast(R.string.startendtooclose);
                		}
                		break;
                	case ABREPEATING_NOW:
                		clearABPos();
                		mAbRepeatingButton.setImageResource(R.drawable.apoint_or_bookmark);
                		setJumpButtonContents();
                		showToast(R.string.abpos_cleared);
//                		mAPosTime.setVisibility(View.INVISIBLE);
//                		mBPosTime.setVisibility(View.INVISIBLE);
                		break;
                	default:
                		mAbRepeatingButton.setImageResource(R.drawable.apoint_or_bookmark);
                }
                show(sDefaultTimeout);
        }
    };
    
private View.OnLongClickListener mAbRepeatingLongListner = new View.OnLongClickListener() {
		
		@Override
		public boolean onLongClick(View v) {
			
					switch (mAbRepeatingState) {
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
			    
			return true;
    	}
    	
    };
    
    private void showAbList(){
		
		Intent intent = new Intent();
        intent.setClass(mContext, ABPosPickerActivity.class);
//        intent.putExtra(ABDbAdapter.KEY_MUSICTITLE, mPlayer.getTrackName());
        intent.putExtra(ABDbAdapter.KEY_MUSICTITLE, "");
        intent.putExtra(ABDbAdapter.KEY_MUSICPATH, mPlayer.getPath());
        intent.putExtra(ABDbAdapter.KEY_MUSICDURATION, (long) mPlayer.getDuration());
        ((Activity) mContext).startActivityForResult(intent, BROWSE_ABPOINTS);
        mPickingABPos = true;
        pause();
        refreshNow();
        setPauseButtonImage();
        setAbRepeatingButtonImage();
	}
    
    private void setPauseButtonImage() {
            if (isPlaying()) {
                mPauseButton.setImageResource(android.R.drawable.ic_media_pause);
            } else {
            	// Only when debugged
//                if (LocalDebugMode.debugMode) {
//                	Log.i("@@@VideoDebug", "ic_media_play set in setPauseButtonImage");
//            	}
                mPauseButton.setImageResource(android.R.drawable.ic_media_play);
            }
            setPrevPauseNextToTravarseMode(mAbListTraverseMode);
            if (mTopControl) {
            	mTopBottom.setImageResource(R.drawable.go_down);
            } else {
            	mTopBottom.setImageResource(R.drawable.go_up);
            }
    }
    
    private void setAbRepeatingButtonImage() {
		setPrevPauseNextToTravarseMode(mAbListTraverseMode);
		switch (mAbRepeatingState) {
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
    }
    
    private void setPrevPauseNextToTravarseMode (boolean abmode) {
    	if (abmode) {
    		if (mPrevButton != null) {
    			mPrevButton.setVisibility(View.VISIBLE);
    			mPrevButton.getDrawable().setColorFilter(0xFF00FFFF, PorterDuff.Mode.MULTIPLY);
    			if (mRewButton != null) mRewButton.setVisibility(View.GONE);
    		}
    		if (mPauseButton != null) 
    			mPauseButton.getDrawable().setColorFilter(0xFF00FFFF, PorterDuff.Mode.MULTIPLY);
    		if (mNextButton != null) {
    			mNextButton.setVisibility(View.VISIBLE);
    			mNextButton.getDrawable().setColorFilter(0xFF00FFFF, PorterDuff.Mode.MULTIPLY);
    			if (mFfwdButton != null) mFfwdButton.setVisibility(View.GONE);
    		}
    	} else {
    		if (mPrevButton != null) {
    			mPrevButton.getDrawable().clearColorFilter();
    			mPrevButton.setVisibility(View.GONE);
    			if (mRewButton != null) mRewButton.setVisibility(View.VISIBLE);
    		}
    		if (mPauseButton != null) 
    			mPauseButton.getDrawable().clearColorFilter();
    		if (mNextButton != null) {
    			mNextButton.getDrawable().clearColorFilter();
    			mNextButton.setVisibility(View.GONE);
    			if (mFfwdButton != null) mFfwdButton.setVisibility(View.VISIBLE);
    		}
    		
    	}
    	if (mPrevButton != null) 
			mPrevButton.invalidate();
    	if (mPauseButton != null) 
			mPauseButton.invalidate();
    	if (mNextButton != null) 
			mNextButton.invalidate();
    }
    
    private long refreshNow() {
        
        	int mDuration = mPlayer.getDuration();
//            long pos = mPosOverride < 0 ? mPlayer.getCurrentPosition() : mPosOverride;
            long pos = mPlayer.getCurrentPosition();
            long remaining = 1000 - (pos % 1000);
            if ((pos >= 0) && (mDuration > 0)) {
                mCurrentTime.setText(MusicUtils.makeTimeString(mContext, pos / 1000));
                
                if (isPlaying()) {
                    mCurrentTime.setVisibility(View.VISIBLE);
                } else {
                    // blink the counter
                    int vis = mCurrentTime.getVisibility();
                    mCurrentTime.setVisibility(vis == View.INVISIBLE ? View.VISIBLE : View.INVISIBLE);
                    remaining = 500;
                }

//                mProgress.setProgress((int) (1000 * pos / mDuration));
                if ((mAbRepeatingState == MediaPlaybackService.ABREPEATING_NOW) && mABWideBar) {
                	mProgress.setProgress(safeLongToInt (1000 * (pos - mAPos) / (mBPos - mAPos)));
                } else {
                	mProgress.setProgress(safeLongToInt (1000 * pos / mDuration));
                }
                
                switch (mAbRepeatingState){
                case MediaPlaybackService.ABREPEATING_NOW:
                  	if (mABWideBar) {
                  		mProgress.setSecondaryProgress(safeLongToInt (1000));
                  	} else {
                  		mProgress.setSecondaryProgress(safeLongToInt (1000 * mBPos / mDuration));
                  	}
//                	mProgress.setSecondaryProgress((int) (1000 * mBPos / mDuration));
                	mAPosTime.setVisibility(View.VISIBLE);
                	mBPosTime.setVisibility(View.VISIBLE);
                	break;
                case MediaPlaybackService.ABREPEATING_WAITING:
                	mProgress.setSecondaryProgress(0);
                	mAPosTime.setVisibility(View.VISIBLE);
                	mBPosTime.setVisibility(View.VISIBLE);
                	break;
                case MediaPlaybackService.ABREPEATING_NOT:
                	mProgress.setSecondaryProgress(0);
//                	mAPosTime.setVisibility(View.INVISIBLE);
//                	mBPosTime.setVisibility(View.INVISIBLE);
                	break;
            	default:
            		mProgress.setSecondaryProgress(0);
            		mAPosTime.setVisibility(View.INVISIBLE);
                	mBPosTime.setVisibility(View.INVISIBLE);
                }
                	
                
                
                if (mAbRepeatingState == MediaPlaybackService.ABREPEATING_NOW) {
//                    mProgress.setSecondaryProgress((int) (1000 * mBPos / mDuration));
                    if (mABWideBar) {
                    	mProgress.setSecondaryProgress(safeLongToInt (1000));
                    } else {
                    	mProgress.setSecondaryProgress(safeLongToInt (1000 * mBPos / mDuration));
                    }
                    } else {
                    	mProgress.setSecondaryProgress(0);
                    }
            } else {
                mCurrentTime.setText("--:--");
                mProgress.setProgress(1000);
            }
            // return the number of milliseconds until the next full second, so
            // the counter can be updated at just the right time
            return remaining;
        
    }
    
    private View.OnClickListener mSpeedListener = new View.OnClickListener() {
    	@Override
		public void onClick(View v) {
    		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
    		View vw = LayoutInflater.from(mContext).inflate(R.layout.dialog_layout,
    				null);
    		builder
    				.setIcon(R.drawable.ic_speed_1).setTitle("SlowMotion").setView(vw);

    		ListView lv = (ListView) vw.findViewById(R.id.listview);
    		ArrayAdapter<CharSequence> adapter = ArrayAdapter
    				.createFromResource(mContext, R.array.speed,
    						android.R.layout.simple_list_item_single_choice);

    		lv.setOnItemClickListener(mItemSelectedListner);
    		lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    		lv.setAdapter(adapter);
    		mSpeedDialog = builder.create();
    		mSpeedDialog.show();
    		lv.setItemChecked(getSpeedIdx(), true);
    	}
    };
    
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
    				onSpeedSelected(selected_speed);
//    				Editor ed = mPreferences.edit();
//    	        	ed.putInt(DICTIONARY, idx);
//    	        	ed.commit();
//    	        	
//    	        	loadPage();

    	        	mSpeedDialog.hide();

    			}
    		
    	};
    
    private View.OnClickListener mAPosTimeClickListner = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
//				boolean abshiftpicker_called = false;
				switch (mAbRepeatingState) {
            	case ABREPEATING_NOT:
            		long jumpdist = mPreferences.getLong("jumpdistone", 7500);
        			jump(jumpdist);
        			break;	
            	case MediaPlaybackService.ABREPEATING_WAITING:
            		seek(mAPos);
            		break;
            	case MediaPlaybackService.ABREPEATING_NOW:
//            		long jumpdist2 = mPreferences.getLong("jumpdisttwo", 5000);
            		new ABShiftPickerDialog(v.getContext(), true,
            				mAShiftDistSetListener,
                            R.string.adjust_apoint, R.drawable.apoint).show();
//            		abshiftpicker_called = true;
            		break;
				}
//				if (abshiftpicker_called) {
//					hide();
//				} else {
					show(sDefaultTimeout);
//				}
			
		}
	};
    
private View.OnClickListener mJumpButtonCenterClickListner = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
				String mWhereClause = "";
				AlertDialog.Builder mybuilder;
				switch (mAbRepeatingState) {
            	case ABREPEATING_NOT:
            		long jumpdist = mPreferences.getLong("jumpdisttwo", 5000);
        			jump(jumpdist);
        			break;	
            	case ABREPEATING_WAITING:
            		mABDbHelper.open();
                	long rowid = mABDbHelper.createABPos("Bookmark@ "+mAPosTime.getText(),
                			getAPos(), (long) 0, mPlayer.getPath(), 
                			"", mPlayer.getDuration());
//        					mService.getTrackName(), mService.duration());
                	mABDbHelper.close();
                	clearAbCursor();
                	clearABPos();
                    setAbRepeatingButtonImage();
                    setJumpButtonContents();
                    showToast(R.string.bookmark_recorded);
            		break;
            	case ABREPEATING_NOW:
            		mABDbHelper.open();
            		mybuilder = new AlertDialog.Builder(mContext);
            		if (MusicUtils.getBooleanPref(mContext, MusicUtils.Defs.ASK_IF_OVERWRITE, true)&&mAbEdited) {
            			mybuilder.setTitle(R.string.ab_edited);
                    	mybuilder.setMessage(R.string.ab_update_or_new);
                    	mybuilder.setPositiveButton(R.string.ab_update, new android.content.DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								try {
									mABDbHelper.updateABPos(mAbRecRowId, mAPos, mBPos);
									mAbEdited = false;
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
				                			getAPos(), getBPos(), mPlayer.getPath(), 
				                			"", mPlayer.getDuration());
									mAbRecRowId = rowid;
									mAbEdited = false;
				                	mABDbHelper.close();
//		                    	sendDbModifiedtoService();
				                	clearAbCursor();
		                    	showToast(R.string.abpos_recorded);
								} catch (Exception e) {
									
								}
								
							}});
                    	
//                    	mybuilder.setIcon(R.drawable.ic_dialog_time);
                    	mybuilder.setNegativeButton(android.R.string.cancel, null);
                    	mybuilder.setCancelable(true);
                    	mybuilder.show();
            		} else {
            			mWhereClause = ABDbAdapter.KEY_MUSICTITLE + "=\"" + "" + "\""
                        		+ " AND " + ABDbAdapter.KEY_MUSICDURATION + ">=" + Long.toString(mPlayer.getDuration() - 1) 
                        		+ " AND " + ABDbAdapter.KEY_MUSICDURATION + "<=" + Long.toString(mPlayer.getDuration() + 1)
    		            		+ " AND " + ABDbAdapter.KEY_APOS + "=" + Long.toString(getAPos())
    		            		+ " AND " + ABDbAdapter.KEY_BPOS + "=" + Long.toString(getBPos());
                    	
                    	if (MusicUtils.getBooleanPref(mContext, MusicUtils.Defs.ASK_IF_DUPLICATED, true)&&mABDbHelper.checkIfABPosExists(mWhereClause)) {
                    		mybuilder.setTitle(R.string.same_ab_exists);
                        	mybuilder.setMessage(R.string.want_to_continue);
                        	mybuilder.setPositiveButton(android.R.string.yes, new android.content.DialogInterface.OnClickListener() {
    							@Override
    							public void onClick(DialogInterface dialog,
    									int which) {
    								try {
    									long rowid = mABDbHelper.createABPos(mAPosTime.getText()+" "+mBPosTime.getText(),
    				                			getAPos(), getBPos(), mPlayer.getPath(), 
    				                			"", mPlayer.getDuration());
    									mAbRecRowId = rowid;
    									mAbEdited = false;
    				                	mABDbHelper.close();
//    		                    	sendDbModifiedtoService();
    				                	clearAbCursor();
    		                    	showToast(R.string.abpos_recorded);
    								} catch (Exception e) {
    									
    								}
    								
    							}});
//                        	mybuilder.setIcon(R.drawable.ic_dialog_time);
                        	mybuilder.setNegativeButton(android.R.string.cancel, null);
                        	mybuilder.setCancelable(true);
                        	mybuilder.show();
                    		
                    	} else {
                    		rowid = mABDbHelper.createABPos(mAPosTime.getText()+" "+mBPosTime.getText(),
                        			getAPos(), getBPos(), mPlayer.getPath(), 
                        			"", mPlayer.getDuration());
                        	mABDbHelper.close();
                        	mAbRecRowId = rowid;
							mAbEdited = false;
//                        	sendDbModifiedtoService();
							clearAbCursor();
                        	showToast(R.string.abpos_recorded);
                    	}
            		}
            		
            		
//                	mABDbHelper.createABPos(mAPosTime.getText()+" "+mBPosTime.getText(),
//                			getAPos(), getBPos(), mPlayer.getPath(), 
////                			getTrackName(), mPlayer.getDuration());
//                			"", mPlayer.getDuration());
//                	mABDbHelper.close();
//                	clearAbCursor();
//                	showToast(R.string.abpos_recorded);
            		break;
				}
				show(sDefaultTimeout);
		}
	};
    
private View.OnClickListener mBPosTimeClickListner = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
//			boolean abshiftpicker_called = false;
				switch (mAbRepeatingState) {
            	case ABREPEATING_NOT:
            		long jumpdist = mPreferences.getLong("jumpdistthree", 2500);
        			jump(jumpdist);
        			break;	
            	case MediaPlaybackService.ABREPEATING_WAITING:
            		clearABPos();
            		mAbRepeatingButton.setImageResource(R.drawable.apoint_or_bookmark);
            		setJumpButtonContents();
            		showToast(R.string.abpos_cleared);
            		break;
            	case MediaPlaybackService.ABREPEATING_NOW:
            		new ABShiftPickerDialog(v.getContext(), true,
            				mBShiftDistSetListener,
                            R.string.adjust_bpoint, R.drawable.bpoint).show();
//            		abshiftpicker_called = true;
            		break;
				}
//				if (abshiftpicker_called) {
//					hide();
//				} else {
					show(sDefaultTimeout);
//				}
		}
	};
	
private View.OnClickListener mTopBottomClickListner = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			
				if (mTopControl) {
					mTopControl = false;
				} else {
					mTopControl = true;
				}
				hide();
				mHandler.removeCallbacks(redrawControl);
				mHandler.postDelayed(redrawControl, 600);
//				hide();
//				show(sDefaultTimeout);
		}
	};
	
private View.OnLongClickListener mAPosTimeLongListner = new View.OnLongClickListener() {
		
		@Override
		public boolean onLongClick(View v) {
			boolean consumed = false;
					switch (mAbRepeatingState) {
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
	            		new ABShiftPickerDialog(v.getContext(), true,
	            				mAShiftDistSetListener,
	                            R.string.adjust_apoint, R.drawable.apoint).show();
	            		consumed = true;
	            		break;
	            	case MediaPlaybackService.ABREPEATING_NOW:
	            		
	            		break;
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
            }
    };
	
private View.OnLongClickListener mJumpButtonCenterLongListner = new View.OnLongClickListener() {
		
		@Override
		public boolean onLongClick(View v) {
			boolean consumed = false;
					switch (mAbRepeatingState) {
	            	case MediaPlaybackService.ABREPEATING_NOT:
	            		long jumpdist = mPreferences.getLong("jumpdisttwo", 5000);
	            		new RealNumberPickerDialog(v.getContext(),
	        					mJumpDistSetTwoListner,
	                            safeLongToInt (jumpdist),
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
				if (consumed == true) {
					return true;
				} else {
					return false;
				}
		}
	};
    
	RealNumberPickerDialog.OnNumberSetListener mJumpDistSetTwoListner =
        new RealNumberPickerDialog.OnNumberSetListener() {
            public void onNumberSet(int jumpdist) {
            	Editor ed = mPreferences.edit();
            	ed.putLong("jumpdisttwo", (long) jumpdist);
            	ed.commit();
            	setJumpButtonContents();
            }
    };
    
private View.OnLongClickListener mBPosTimeLongListner = new View.OnLongClickListener() {
		
		@Override
		public boolean onLongClick(View v) {
			boolean consumed = false;
					switch (mAbRepeatingState) {
	            	case MediaPlaybackService.ABREPEATING_NOT:
	            		long jumpdist = mPreferences.getLong("jumpdistthree", 2500);
	            		new RealNumberPickerDialog(v.getContext(),
	        					mJumpDistSetThreeListner,
	                            safeLongToInt (jumpdist),
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
            }
    };
    
	ABShiftPickerDialog.OnShiftDistSetListener mAShiftDistSetListener =
    	new ABShiftPickerDialog.OnShiftDistSetListener() {
    		public void onShiftDistSet (long shiftdist, boolean checkmodeon) {
    			shiftAPos((long) shiftdist);
    			setAPosTimeText (getAPos());
    			if (checkmodeon == true){
    				seek(getAPos());
    			}
    		}
    		public void onABPauseSet(long abpause) {
    			
    		}
    		
    };
	
	ABShiftPickerDialog.OnShiftDistSetListener mBShiftDistSetListener =
    	new ABShiftPickerDialog.OnShiftDistSetListener() {
    		public void onShiftDistSet (long shiftdist, boolean checkmodeon) {
    			shiftBPos((long) shiftdist);
    			long newbpos =getBPos();
    			setBPosTimeText (newbpos);
    			if (checkmodeon == true){
    				long apos = getAPos();
    				long jumpto;
    				jumpto = ((newbpos - CHECK_BPOINT_DURATION) < apos) ? apos : (newbpos - CHECK_BPOINT_DURATION);
    				seek(safeLongToInt (jumpto));
    			}
    		}
    		public void onABPauseSet(long abpause) {
    			
    		}
    };
	
    private void setAPosTimeText (long apostime) {    	
    	mAPosTime.setText(MusicUtils.makeTimeString(mContext, apostime/1000));	
    }
    
    private void setBPosTimeText (long bpostime) {    	
    	mBPosTime.setText(MusicUtils.makeTimeString(mContext, bpostime/1000));	
    }
    
	public void jump(long jumpdist) {
		int currentpos;
    	int newpos;
    	mUpdatePausePlayCalledAfterSeek = true;
    	mTempPauseMightOccur = true;
        if (mPlayer != null) {
        	currentpos = mPlayer.getCurrentPosition();
        	newpos = currentpos - safeLongToInt (jumpdist);
            if (newpos < 0) newpos = 0;
            if (newpos > mPlayer.getDuration()) newpos = mPlayer.getDuration();
            mPlayer.seekTo(newpos);
        }
    }
	
    public void setAPos() {
        mAPos = mPlayer.getCurrentPosition();
        mAbRepeatingState = ABREPEATING_WAITING;
    }
    
    public boolean setBPos() {
    	int pos = mPlayer.getCurrentPosition();
    	if ((pos - mAPos) < 500) {
    		return false;
    	} else {
    		mBPos = pos;
    		seek(mAPos);
    		sendBPointReachedMessage();
    		mAbRepeatingState = ABREPEATING_NOW;
    		return true;
    	}
    }
    
    public void shiftAPos(long delta) {
    	if (mAbRepeatingState == ABREPEATING_NOW) {
    		if ((mAPos + delta) < 0) {
    			mAPos = 0;
    		} else {
    			if ((mAPos + delta) < (mBPos - 500)) {
    				mAPos += delta;
    			} else {
    				mAPos = mBPos - 500;
    			}
    		}
    	}
    	if (mAbRepeatingState == ABREPEATING_WAITING) {
    		if ((mAPos + delta) < 0) {
    			mAPos = 0;
    		} else {
    			if ((mAPos + delta) < (mPlayer.getDuration() - 1000)) {
    				mAPos += delta;
    			} else {
    				mAPos = mPlayer.getDuration() - 1000;
    			}
    		}
    	}
    	if (mAbRecRowId != -1) mAbEdited = true;
    	refreshBPointReachedMessages();
    }
    
    public void shiftBPos(long delta) {
    	if (mAbRepeatingState == ABREPEATING_NOW) {
    		if ((mBPos + delta) > (mPlayer.getDuration() - 500)) {
    			mBPos = mPlayer.getDuration() - 500;
    		} else {
    			if ((mBPos + delta) > (mAPos + 500)) {
    				mBPos += delta;
    			} else {
    				mAPos = mBPos - 500;
    			}
    		}
//    		if ((mBPos + delta > (mAPos + 500)) && (mBPos + delta < mPlayer.duration())) {
//    			mBPos += delta;
//    		}
    	}
    	if (mAbRecRowId != -1) mAbEdited = true;
    	refreshBPointReachedMessages();
    }
    
    public void refreshBPointReachedMessages() {
    	if ((isPlaying() == true) && (mAbRepeatingState == ABREPEATING_NOW))  {
        sendBPointReachedMessage();
    	}
    }
    
    private void sendBPointReachedMessage() {
    	long cur_pos = mPlayer.getCurrentPosition();
//    	if ((cur_pos >= mAPos) && (cur_pos <= mBPos)) {
    	if (cur_pos != 0) {
    		clearBPointReachedMessage();
		if (mBPos - cur_pos > 500) {
		    mHandler.sendEmptyMessageDelayed(BPOINT_HALF_REACHED, ((mBPos - cur_pos)/2));
//		    Log.i(LOGTAG, "BPOINT HALF REACHED MSG SENT " + cur_pos + "ms " + mBPos + "ms");
		 // Only when debugged
//            if (LocalDebugMode.debugMode) {
//            	Log.i("@@@VideoDebug", "BPOINT HALF REACHED MSG SENT cur_pos " + cur_pos + "ms mAPos " + mAPos + "ms mBPos " + mBPos + "ms");
//        	}
		} else {
			mHandler.sendEmptyMessageDelayed(BPOINT_REACHED, (mBPos - cur_pos)*mSpeed);
//			Log.i(LOGTAG, "BPOINT REACHED SENT " + cur_pos + "ms " + mBPos + "ms");
			// Only when debugged
//            if (LocalDebugMode.debugMode) {
//            	Log.i("@@@VideoDebug", "BPOINT REACHED SENT cur_pos " + cur_pos + "ms mAPos " + mAPos + "ms mBPos " + mBPos + "ms");
//        	}
		} 
    	}
//    	} else {
//    		clearABPos(); // something wrong
//    	}
    }
    
    private void clearBPointReachedMessage() {
//    	Log.i("@@@VideoDebug", "BPOINT REACHED MSGS CLEARED");
    	mHandler.removeMessages(BPOINT_HALF_REACHED);
    	mHandler.removeMessages(BPOINT_REACHED);
    }
    
    public void setAbPoints (long apos, long bpos) {
    	if ((bpos - apos) < 500) {
    		return;
    	} else {
    		mAPos = safeLongToInt (apos);
    		mBPos = safeLongToInt (bpos);
    		seek(safeLongToInt (apos));
    		sendBPointReachedMessage();
    		mAbRepeatingState = ABREPEATING_NOW;
    		return;
    	}
    }
    
    public int getAPos() {
    	return mAPos;
    }
    
    public int getBPos() {
    	return mBPos;
    }
    
    public void clearABPos() {
        mAPos = 0;
        mBPos = 0;
        clearBPointReachedMessage();
//        Log.i(LOGTAG, "BPOINT MSGS DELETED-2");
        mAbRepeatingState = ABREPEATING_NOT;
    }
    
    private void showToast(int resid) {
        if (mToast == null) {
            mToast = Toast.makeText(mContext, "", Toast.LENGTH_SHORT);
        }
        mToast.setText(resid);
        mToast.show();
    }
    
    /**
     * Show the controller on screen. It will go away
     * automatically after 3 seconds of inactivity.
     */
    public void show() {
        show(sDefaultTimeout);
    }

    /**
     * Disable pause or seek buttons if the stream cannot be paused or seeked.
     * This requires the control interface to be a MediaPlayerControlExt
     */
    private void disableUnsupportedButtons() {
        try {
            if (mPauseButton != null && !mPlayer.canPause()) {
                mPauseButton.setEnabled(false);
            }
            if (mRewButton != null && !mPlayer.canSeekBackward()) {
                mRewButton.setEnabled(false);
            }
            if (mFfwdButton != null && !mPlayer.canSeekForward()) {
                mFfwdButton.setEnabled(false);
            }
        } catch (IncompatibleClassChangeError ex) {
            // We were given an old version of the interface, that doesn't have
            // the canPause/canSeekXYZ methods. This is OK, it just means we
            // assume the media can be paused and seeked, and so we don't disable
            // the buttons.
        }
    }
    
    /**
     * Show the controller on screen. It will go away
     * automatically after 'timeout' milliseconds of inactivity.
     * @param timeout The timeout in milliseconds. Use 0 to show
     * the controller until hide() is called.
     */
    public void show(int timeout) {

        if (!mShowing && mAnchor != null && mDecor.getWindowToken() == null) {
            setProgress();
            if (mPauseButton != null) {
                mPauseButton.requestFocus();
            }
            disableUnsupportedButtons();

            int [] anchorpos = new int[2];
            mAnchor.getLocationInWindow(anchorpos);

            WindowManager.LayoutParams p = new WindowManager.LayoutParams();
            p.gravity = Gravity.TOP;
            p.width = mAnchor.getWidth();
            p.height = LayoutParams.WRAP_CONTENT;
            p.x = 0;
            if (mTopControl) {
            	p.y = 0;
            } else {
            	p.y = anchorpos[1] + mAnchor.getHeight() - p.height;
            }
//            p.y = anchorpos[1] + mAnchor.getHeight() - p.height;
//            p.y = 0;
            p.format = PixelFormat.TRANSLUCENT;
            p.type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL;
            p.flags |= WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
            p.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
            p.token = null;
            p.windowAnimations = 0; // android.R.style.DropDownAnimationDown;
//            Log.i(TAG, "Before addView: " + mDecor.getWindowToken());
            
//            if (p.token == null) {
//            	if (mDecor != null) {
//            		p.token = mDecor.getWindowToken();
//            	}
//            }
            
            mWindowManager.addView(mDecor, p);
//            Log.i(TAG, "After addView");
            mShowing = true;
            mTempPauseMightOccur = false; // just make sure
        }
     // Only when debugged
//        if (LocalDebugMode.debugMode) {
//        	Log.i("@@@VideoDebug", "updatePausePlay called in show");
//    	}
        updatePausePlay();
        setNavBarVisibility();
//        if (MusicUtils.getBooleanPref(mContext,VIDEO_NAVBAR_AUTOHIDE, true)) {
//            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                    | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
//        }
        // cause the progress bar to be updated even if mShowing
        // was already true.  This happens, for example, if we're
        // paused with the progress bar showing the user hits play.
        mHandler.sendEmptyMessage(SHOW_PROGRESS);

        Message msg = mHandler.obtainMessage(FADE_OUT);
        if (timeout != 0) {
            mHandler.removeMessages(FADE_OUT);
            mHandler.sendMessageDelayed(msg, timeout);
        }
    }
    
    private Runnable redrawControl = new Runnable() {
    	public void run() {
//    	Log.i(TAG, "redrawControl mShowing: " + mShowing);
//    	Log.i(TAG, "redrawControl mAnchor: " + mAnchor);
//    	Log.i(TAG, "redrawControl mDecor.getWindowToken(): " + mDecor.getWindowToken());

        if (!mShowing && mAnchor != null && mDecor.getWindowToken() == null) {
//        if (mAnchor != null) {
            setProgress();
            if (mPauseButton != null) {
                mPauseButton.requestFocus();
            }
            disableUnsupportedButtons();

            int [] anchorpos = new int[2];
            mAnchor.getLocationInWindow(anchorpos);

            WindowManager.LayoutParams p = new WindowManager.LayoutParams();
            p.gravity = Gravity.TOP;
            p.width = mAnchor.getWidth();
            p.height = LayoutParams.WRAP_CONTENT;
            p.x = 0;
            if (mTopControl) {
            	p.y = 0;
            } else {
            	p.y = anchorpos[1] + mAnchor.getHeight() - p.height;
            }
//            p.y = anchorpos[1] + mAnchor.getHeight() - p.height;
//            p.y = 0;
            p.format = PixelFormat.TRANSLUCENT;
            p.type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL;
            p.flags |= WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
            p.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
//            p.flags |= WindowManager.LayoutParams.FLAG_SPLIT_TOUCH;
            p.flags |= 0x00800000;  //Ice Cream Sandwich
            p.token = null;
            p.windowAnimations = 0; // android.R.style.DropDownAnimationDown;
//            Log.i(TAG, "Before addView: " + mDecor.getWindowToken());
            Log.i(TAG, "redrawControl remove and add View");
//            mWindowManager.removeView(mDecor);
            mWindowManager.addView(mDecor, p);
//            Log.i(TAG, "After addView");
            mShowing = true;
            mTempPauseMightOccur = false; // just make sure
        }
     // Only when debugged
//        if (LocalDebugMode.debugMode) {
//        	Log.i("@@@VideoDebug", "updatePausePlay called in show");
//    	}
        updatePausePlay();
        setNavBarVisibility();
//        if (MusicUtils.getBooleanPref(mContext,VIDEO_NAVBAR_AUTOHIDE, true)) {
//            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                    | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
//        }
        // cause the progress bar to be updated even if mShowing
        // was already true.  This happens, for example, if we're
        // paused with the progress bar showing the user hits play.
//        mHandler.sendEmptyMessage(SHOW_PROGRESS);
//
//        Message msg = mHandler.obtainMessage(FADE_OUT);
//        if (timeout != 0) {
//            mHandler.removeMessages(FADE_OUT);
//            mHandler.sendMessageDelayed(msg, timeout);
//        }
    	}};

    private void setNavBarVisibility() {
//        if (MusicUtils.getBooleanPref(mContext,VIDEO_NAVBAR_AUTOHIDE, true)) {
//            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                    | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
//        }
        if (MusicUtils.getBooleanPref(mContext,VIDEO_NAVBAR_AUTOHIDE, true)) {
            getWindow().getDecorView().setSystemUiVisibility(
                      View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                    | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
           );
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                      View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().getDecorView().setFitsSystemWindows(true);
        }
    }
    
    public boolean isShowing() {
        return mShowing;
    }

    /**
     * Remove the controller from the screen.
     */
    public void hide() {
        if (mAnchor == null)
            return;

        if (mShowing) {
            try {
//            	if (LocalDebugMode.debugMode) {
//                	Log.i("@@@VideoDebug", "removemessage in hide called");
//            	}
                mHandler.removeMessages(SHOW_PROGRESS);
                mWindowManager.removeView(mDecor);
            } catch (IllegalArgumentException ex) {
                Log.w("MediaController", "already removed");
            }
            mShowing = false;
        }
    }
    
    private void MakeAbCursor() {
    	if (mABDbHelper == null) mABDbHelper = new ABDbAdapter(mContext);
        mABDbHelper.open();
    	clearAbCursor();
        Cursor[] cs;
        // Use ArrayList for the moment, since we don't know the size of
        // Cursor[]. If the length of Corsor[] larger than really used,
        // a NPE will come up when access the content of Corsor[].
        ArrayList<Cursor> cList = new ArrayList<Cursor>();

        Cursor c;

//        	mWhereClause = ABDbAdapter.KEY_MUSICPATH + "=\"" + mMusicPath + "\""
//        		+ " AND " + ABDbAdapter.KEY_MUSICDURATION + "=" + Long.toString(mMusicDuration);
//        	mWhereClause = ABDbAdapter.KEY_MUSICTITLE + "=\"" + mMusicTitle + "\""
//    		+ " AND " + ABDbAdapter.KEY_MUSICDURATION + "=" + Long.toString(mMusicDuration);
        	String mWhereClause = ABDbAdapter.KEY_MUSICTITLE + "=\"" + "" + "\""
    		+ " AND " + ABDbAdapter.KEY_MUSICDURATION + ">=" + Long.toString(mPlayer.getDuration() - 1) 
    		+ " AND " + ABDbAdapter.KEY_MUSICDURATION + "<=" + Long.toString(mPlayer.getDuration() + 1);
        	String mOrderBy;
        	
        	switch (MusicUtils.getIntPref(mContext, "abposdisporder", ABPosDispOrderActivity.CREATION_NORMAL)) {
        	case ABPosDispOrderActivity.CREATION_NORMAL:
        		mOrderBy = ABDbAdapter.KEY_ROWID + " asc";
    			break;
    		case ABPosDispOrderActivity.CREATION_REVERSE:
    			mOrderBy = ABDbAdapter.KEY_ROWID + " desc";
    			break;
    		case ABPosDispOrderActivity.APOS_NORMAL:
    			mOrderBy = ABDbAdapter.KEY_APOS + " asc";
    			break;
    		case ABPosDispOrderActivity.APOS_REVERSE:
    			mOrderBy = ABDbAdapter.KEY_APOS + " desc";
    			break;
    		case ABPosDispOrderActivity.NAME_NORMAL:
    			mOrderBy = ABDbAdapter.KEY_ABTITLE + " asc";
    			break;
    		case ABPosDispOrderActivity.NAME_REVERSE:
    			mOrderBy = ABDbAdapter.KEY_ABTITLE + " desc";
    			break;
    		default:
    			mOrderBy = ABDbAdapter.KEY_ABTITLE + " asc";
        	}
        	
        	c = mABDbHelper.fetchABPos(mWhereClause, mOrderBy);
        	if (c != null) 
        		cList.add(c);
        	

        // Get the ArrayList size.
        int size = cList.size();
        if (0 == size) {
            // If no video/audio/SDCard exist, return.
            mAbCursor = null;
            return;
        }
        
        mAbRecRowId = -1;
        mAbEdited = false;

        // The size is known now, we're sure each item of Cursor[] is not null.
        cs = new Cursor[size];
        cs = cList.toArray(cs);
        mAbCursor = new SortCursor(cs, ABDbAdapter.KEY_APOS);
    }
    
    private void clearAbCursor () {
    	if (mAbCursor != null) {
    		mAbCursor.close();
    		mAbCursor = null;
    		mPosInAbList = -1;
    		mAbRecRowId = -1;
            mAbEdited = false;
    		if (mAbListTraverseMode) {
	    		mAbListTraverseMode = false;
	    		setPrevPauseNextToTravarseMode(mAbListTraverseMode);
	    		clearABPos();
				MusicUtils.playAlert(mContext);
	    		showToast(R.string.abpos_cleared);
    		}
    	}
    }
    
    private boolean currentAbPoints () {
//    	Log.i(LOGTAG, "currentAbPoints Called");
    	if (mAbCursor == null) {
			MakeAbCursor();
		}
    	if (mAbCursor != null) {
    		if (mAbCursor.getCount() < 1) return false;
    		Log.i(TAG, "in currentAbPoints mPosInAbList:" + mPosInAbList);
    		if (mPosInAbList >= 0) {
    			mAbCursor.moveToPosition(mPosInAbList);
    		} else {
    			mAbCursor.moveToFirst();
    		}
    		long apos = mAbCursor.getLong(mAbCursor.getColumnIndex(ABDbAdapter.KEY_APOS));
    		long bpos = mAbCursor.getLong(mAbCursor.getColumnIndex(ABDbAdapter.KEY_BPOS));

    		seek(safeLongToInt (apos));
    		setAPos();
    		if (bpos != 0) {
    			seek(safeLongToInt (bpos));
    			setBPos();
    		}
    		String abtitle = mAbCursor.getString(mAbCursor.getColumnIndex(ABDbAdapter.KEY_ABTITLE));
    		Toast.makeText(mContext, abtitle, Toast.LENGTH_LONG).show();
    		return true;
    	}	
    	return false;
    }
    
    private boolean prevAbPoints () {
    	if (mAbCursor == null) {
			MakeAbCursor();
		}
    	if (mAbCursor != null) {
    		if (mAbCursor.getCount() < 1) return false;
    		if (!mAbCursor.moveToPrevious()){
    			mAbCursor.moveToLast();
    		}
    		long apos = mAbCursor.getLong(mAbCursor.getColumnIndex(ABDbAdapter.KEY_APOS));
    		long bpos = mAbCursor.getLong(mAbCursor.getColumnIndex(ABDbAdapter.KEY_BPOS));
    		mAbRecRowId = mAbCursor.getLong(mAbCursor.getColumnIndex(ABDbAdapter.KEY_ROWID));
    		mAbEdited = false;
    		
    		if (bpos != 0) {
    			setAbPoints(apos, bpos);
    		} else {
    			if (MusicUtils.getBooleanPref(mContext, MusicUtils.Defs.NEW_BM,
    					false)) {
    				seek(safeLongToInt (apos));
            		setAPos();
    			} else {
    				clearABPos();
    				seek(safeLongToInt (apos));
    			}
    		}
    		String abtitle = mAbCursor.getString(mAbCursor.getColumnIndex(ABDbAdapter.KEY_ABTITLE));
    		Toast.makeText(mContext, abtitle, Toast.LENGTH_LONG).show();
//    		notifyChange(MediaPlaybackService.ABPOS_SET_BY_BT);
    		return true;
    	}	
    	return false;
    }
    
    private boolean nextAbPoints () {
    	if (mAbCursor == null) {
			MakeAbCursor();
		}
    	if (mAbCursor != null) {
    		if (mAbCursor.getCount() < 1) return false;
    		if (!mAbCursor.moveToNext()){
    			mAbCursor.moveToFirst();
    		}
    		long apos = mAbCursor.getLong(mAbCursor.getColumnIndex(ABDbAdapter.KEY_APOS));
    		long bpos = mAbCursor.getLong(mAbCursor.getColumnIndex(ABDbAdapter.KEY_BPOS));
    		mAbRecRowId = mAbCursor.getLong(mAbCursor.getColumnIndex(ABDbAdapter.KEY_ROWID));
    		mAbEdited = false;
    		
    		if (bpos != 0) {
    			setAbPoints(apos, bpos);
    		} else {
    			if (MusicUtils.getBooleanPref(mContext, MusicUtils.Defs.NEW_BM,
    					false)) {
    				seek(safeLongToInt (apos));
            		setAPos();
    			} else {
    				clearABPos();
    				seek(safeLongToInt (apos));
    			}
    		}
    		String abtitle = mAbCursor.getString(mAbCursor.getColumnIndex(ABDbAdapter.KEY_ABTITLE));
    		Toast.makeText(mContext, abtitle, Toast.LENGTH_LONG).show();
//    		notifyChange(MediaPlaybackService.ABPOS_SET_BY_BT);
    		return true;
    	}	
    	return false;
    }
    
    private View.OnLongClickListener mPauseLongListener = new View.OnLongClickListener() {
    	@Override
        public boolean onLongClick(View v) {
    		switchModes();
            return true;
        }
    };
    
    private void switchModes () {
    		
    		if (mAbListTraverseMode) {
    			mAbListTraverseMode = false;
    			statusChanged(false);
    			clearABPos();
    			MusicUtils.playAlert(mContext);
        		showToast(R.string.abpos_cleared);
    		} else {
        		if (currentAbPoints()) {
        			mAbListTraverseMode = true;
        			statusChanged(true);
	    			MusicUtils.playAlert(mContext,"canopus");
	    			showToast(R.string.abmode_set);
	    		} else {
	    			MusicUtils.playAlert(mContext);   
	    			showToast(R.string.abmode_not_set);
	    		}
    		}
    		setAbRepeatingButtonImage();
			setJumpButtonContents();
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int pos;
            switch (msg.what) {
                case FADE_OUT:
                    hide();
//                    msg = obtainMessage(B_REACHED);
//                    sendMessageDelayed(msg, 5000);
                    break;
                case SHOW_PROGRESS:
//                	if (LocalDebugMode.debugMode) {
//                    	Log.i("@@@VideoDebug", "SHOW_PROGRESS in Handler: mDra " + mDragging + " mSho " + mShowing 
//                    			+ " isPla " + isPlaying() + " mPla " + (mPlayer != null) + " mTemp " + mTempPauseMightOccur);
//                	}
                    pos = setProgress();
                    if (!mDragging && mShowing) {
                    	if (isPlaying()) {
                        msg = obtainMessage(SHOW_PROGRESS);
//                        if (LocalDebugMode.debugMode) {
//                        	Log.i("@@@VideoDebug", "sendMessageDelayed called in Handler");
//                    	}
                        removeMessages(SHOW_PROGRESS);
                        sendMessageDelayed(msg, 1000 - (pos % 1000));
                    	} else {
                    		if (mTempPauseMightOccur) {
//                    			if (LocalDebugMode.debugMode) {
//                                	Log.i("@@@VideoDebug", "sendMessageDelayed-long called in Handler");
//                            	}
                    			msg = obtainMessage(SHOW_PROGRESS);
                    			removeMessages(SHOW_PROGRESS);
                                sendMessageDelayed(msg, 2000 - (pos % 1000));
                                mTempPauseMightOccur = false;
                    		}
                    	}
                    }
                    break;
                case BPOINT_HALF_REACHED:
//                	Log.i("@@@VideoDebug", "BPOINT HALF REACHED MSG RECEIVED " + mPlayer.getCurrentPosition() + "ms");
                	sendBPointReachedMessage();
            		break;
                case BPOINT_REACHED:
//                	Log.i("@@@VideoDebug", "BPOINT REACHED MSG RECEIVED " + (mPlayer.getCurrentPosition()- mBPos) + "ms");
                	if (mAbRepeatingState == ABREPEATING_NOW) {
                		seek(mAPos);
//                		sendBPointReachedMessage();
                	}
                	break;
                case SPEED_STOP:
//                	Log.i("SPEED","STOP in Handler");
                	mHandler.sendEmptyMessageDelayed(SPEED_START, mSpeedPausemsecs);
                	mPlayer.pause();
                	mPausedForSpeed = true;
                	break;
                case SPEED_START:
//                	Log.i("SPEED","START in Handler");
                	mHandler.sendEmptyMessageDelayed(SPEED_STOP, mSpeedRunmsecs);
                	mPlayer.start();
                	mPausedForSpeed = false;
                	break;
                case PREV_NEXT_RECEIVED:
                	mKeyeventTooClose = false;
                	break;
                default:
                    break;
            }
        }
    };

    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours   = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }
    
    public long seek(int pos) {
    	// Only when debugged
//        if (LocalDebugMode.debugMode) {
//        	Log.i("@@@VideoDebug", "pos: " + pos + " seek in MyMediaController");
//    	}
    	int duration;
    	int returncode = pos;
        if (mPlayer != null) {
            if (pos < 0) pos = 0;
            duration = mPlayer.getDuration();
            if ((duration > 0) && (pos > duration)) pos = duration;
            mUpdatePausePlayCalledAfterSeek = true;
            mTempPauseMightOccur = true;
            mPlayer.seekTo(pos);
            if (mAbRepeatingState == ABREPEATING_NOW) {
                sendBPointReachedMessage();
            }
            return returncode;
        }
        return -1;
    }

    private int setProgress() {
//    	Log.i("SPEED", "setProgress called");
        if (mPlayer == null || mDragging) {
            return 0;
        }
        int position = mPlayer.getCurrentPosition();
        int duration = mPlayer.getDuration();
        if (mProgress != null) {
            if (duration > 0) {
                // use long to avoid overflow
                long pos;
                if ((mAbRepeatingState == ABREPEATING_NOW) && mABWideBar) {
                	if (mBPos <= mAPos) return 0;
                	pos = 1000L * (position - mAPos) / (mBPos - mAPos);
                } else {
                	if (duration == 0) return 0;
                	pos = 1000L * position / duration;
                }
                mProgress.setProgress( safeLongToInt (pos));
            }
            int percent = mPlayer.getBufferPercentage();
            if ((mAbRepeatingState != ABREPEATING_NOW)) {
//            if (LocalDebugMode.debugMode) {
//            	Log.i("@@@VideoDebug", "setSecondary percent " + percent);
//        	}
            mProgress.setSecondaryProgress(percent * 10);
            } else {
            	if (duration > 0) {
            		if (mABWideBar) {
            			mProgress.setSecondaryProgress(safeLongToInt (1000L));
            		} else {
            			mProgress.setSecondaryProgress(safeLongToInt (1000L * mBPos / duration));
            		}
            	}
            }
        }

        if (mEndTime != null)
            mEndTime.setText(stringForTime(duration));
        if (mCurrentTime != null)
            mCurrentTime.setText(stringForTime(position));


        return position;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
//        show(sDefaultTimeout);
        return true;
    }

    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
//        show(sDefaultTimeout);
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (event.getRepeatCount() == 0 && event.getAction() == KeyEvent.ACTION_DOWN && (
                keyCode ==  KeyEvent.KEYCODE_HEADSETHOOK ||
                keyCode ==  KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
                keyCode ==  KeyEvent.KEYCODE_SPACE)) {
            doPauseResume();
            show(sDefaultTimeout);
            if (mPauseButton != null) {
                mPauseButton.requestFocus();
            }
            return true;
        } else if (keyCode ==  KeyEvent.KEYCODE_MEDIA_STOP) {
            if (isPlaying()) {
                pause();
             // Only when debugged
//                if (LocalDebugMode.debugMode) {
//                	Log.i("@@@VideoDebug", "iupdatePausePlay called in dispatchKeyEvent");
//            	}
                updatePausePlay();
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
                keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            // don't show the controls for volume adjustment
            return super.dispatchKeyEvent(event);
        } else if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
            hide();

            return true;
        } else if (keyCode ==  KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
        	if (!mKeyeventTooClose) {
        		rewind();
        		mKeyeventTooClose = true;
        		mHandler.sendEmptyMessageDelayed(PREV_NEXT_RECEIVED, 500);
        	}
        	return true;
        } else if (keyCode ==  KeyEvent.KEYCODE_MEDIA_NEXT) {
        	if (!mKeyeventTooClose) {
        		forward();
        		mKeyeventTooClose = true;
        		mHandler.sendEmptyMessageDelayed(PREV_NEXT_RECEIVED, 500);
        	}
        	return true;
        } else {
            show(sDefaultTimeout);
        }
        return super.dispatchKeyEvent(event);
    }

    private View.OnClickListener mPauseListener = new View.OnClickListener() {
        public void onClick(View v) {
            doPauseResume();
            setAbRepeatingButtonImage();
			setJumpButtonContents();
            show(sDefaultTimeout);
        }
    };

    private void updatePausePlay() {
        if (mRoot == null || mPauseButton == null)
            return;
        if (mPlayer == null) {
        	// Only when debugged
//            if (LocalDebugMode.debugMode) {
//            	Log.i("@@@VideoDebug", "mPlayer is null in updatePausePlay");
//        	}
        }
        if (mTopBottom != null) {
	        if (mTopControl) {
	        	mTopBottom.setImageResource(R.drawable.go_down);
	        } else {
	        	mTopBottom.setImageResource(R.drawable.go_up);
	        }
        }
        if (isPlaying()) {
            mPauseButton.setImageResource(R.drawable.ic_media_pause);
        } else {
            if (mUpdatePausePlayCalledAfterSeek == false) {
            	// Only when debugged
//            	if (LocalDebugMode.debugMode) {
//                	Log.i("@@@VideoDebug", "ic_media_play set in updatePausePlay");
//            	}
            mPauseButton.setImageResource(R.drawable.ic_media_play);
            } else {
            	mUpdatePausePlayCalledAfterSeek = false;
            }
        }
        setAbRepeatingButtonImage();
		setJumpButtonContents();
    }

    private void doPauseResume() {
//    	Log.i("SPEED","Pause Button");
        if (isPlaying()) {
        	if (mAbRepeatingState == ABREPEATING_NOW) clearBPointReachedMessage();
            pause();
            mPausedForSpeed = false;
        } else {
            start();
            if (mAbRepeatingState == ABREPEATING_NOW) sendBPointReachedMessage();
        }
     // Only when debugged
//        if (LocalDebugMode.debugMode) {
//        	Log.i("@@@VideoDebug", "iupdatePausePlay called in doPauseResume");
//    	}
        updatePausePlay();
    }

    // There are two scenarios that can trigger the seekbar listener to trigger:
    //
    // The first is the user using the touchpad to adjust the posititon of the
    // seekbar's thumb. In this case onStartTrackingTouch is called followed by
    // a number of onProgressChanged notifications, concluded by onStopTrackingTouch.
    // We're setting the field "mDragging" to true for the duration of the dragging
    // session to avoid jumps in the position in case of ongoing playback.
    //
    // The second scenario involves the user operating the scroll ball, in this
    // case there WON'T BE onStartTrackingTouch/onStopTrackingTouch notifications,
    // we will simply apply the updated position without suspending regular updates.
    private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
        public void onStartTrackingTouch(SeekBar bar) {
//          if (LocalDebugMode.debugMode) {
//        	Log.i("@@@VideoDebug", "onStartTrackingTouch called");
//    	}
            show(3600000);

            mDragging = true;

            // By removing these pending progress messages we make sure
            // that a) we won't update the progress while the user adjusts
            // the seekbar and b) once the user is done dragging the thumb
            // we will post one of these messages to the queue again and
            // this ensures that there will be exactly one message queued up.
            mHandler.removeMessages(SHOW_PROGRESS);
        }

        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
//          if (LocalDebugMode.debugMode) {
//        	Log.i("@@@VideoDebug", "onProgressChanged called");
//    	}
            if (!fromuser) {
                // We're not interested in programmatically generated changes to
                // the progress bar's position.
                return;
            }

            long duration = mPlayer.getDuration();
//            long newposition = (duration * progress) / 1000L;
            long newposition;
            if ((mAbRepeatingState == ABREPEATING_NOW)&&mABWideBar) {
            	newposition = mAPos + ((mBPos - mAPos) * progress) / 1000L;
            } else {
            	newposition = (duration * progress) / 1000L;
            }
//            if ((mAbRepeatingState == ABREPEATING_NOW)&&isPlaying()) 
//            	sendBPointReachedMessage();
            boolean wasPlaying = isPlaying();
            mUpdatePausePlayCalledAfterSeek = true;
            mTempPauseMightOccur = true;
            mPlayer.seekTo( safeLongToInt (newposition));
            if ((mAbRepeatingState == ABREPEATING_NOW)&&((newposition < mAPos)||(newposition > mBPos))) {
            	clearABPos();
        		mAbRepeatingButton.setImageResource(R.drawable.apoint_or_bookmark);
        		setJumpButtonContents();
        		showToast(R.string.abpos_cleared);
            }
            if ((mAbRepeatingState == ABREPEATING_NOW)&&wasPlaying) 
            	sendBPointReachedMessage();
//            Log.i("@@@VideoDebug", "After sendBPointReachedMessage in ProgressChanged");
            if (mCurrentTime != null)
                mCurrentTime.setText(stringForTime( safeLongToInt (newposition)));
        }

        public void onStopTrackingTouch(SeekBar bar) {
//          if (LocalDebugMode.debugMode) {
//        	Log.i("@@@VideoDebug", "onStopTrackingTouch called");
//    	}
            mDragging = false;
            setProgress();
         // Only when debugged
//            if (LocalDebugMode.debugMode) {
//            	Log.i("@@@VideoDebug", "updatePausePlay called in onStopTrackingTouch");
//        	}
            if (mUpdatePausePlayCalledAfterSeek == false) {
            updatePausePlay();
            }
            show(sDefaultTimeout);

            // Ensure that progress is properly updated in the future,
            // the call to show() does not guarantee this because it is a
            // no-op if we are already showing.
            mHandler.sendEmptyMessage(SHOW_PROGRESS);
        }
    };

//    @Override
    public void setEnabled(boolean enabled) {
        if (mPauseButton != null) {
            mPauseButton.setEnabled(enabled);
        }
        if (mFfwdButton != null) {
            mFfwdButton.setEnabled(enabled);
        }
        if (mRewButton != null) {
            mRewButton.setEnabled(enabled);
        }
        if (mNextButton != null) {
            mNextButton.setEnabled(enabled && mNextListener != null);
        }
        if (mPrevButton != null) {
            mPrevButton.setEnabled(enabled && mPrevListener != null);
        }
        if (mProgress != null) {
            mProgress.setEnabled(enabled);
        }
        
        
        disableUnsupportedButtons();
        mRoot.setEnabled(enabled);
    }

    private View.OnClickListener mRewListener = new View.OnClickListener() {
        public void onClick(View v) {
        	rewind();
        }
    };
    
    
    
    private void rewind () {
    	int pos = mPlayer.getCurrentPosition();
        pos -= 5000; // milliseconds
        mUpdatePausePlayCalledAfterSeek = true;
        mTempPauseMightOccur = true;
        boolean wasPlaying = isPlaying();
        mPlayer.seekTo(pos);
        setProgress();
        if ((mAbRepeatingState == ABREPEATING_NOW)&&((pos < mAPos)||(pos > mBPos))) {
        	clearABPos();
    		mAbRepeatingButton.setImageResource(R.drawable.apoint_or_bookmark);
    		setJumpButtonContents();
    		showToast(R.string.abpos_cleared);
        }
        if ((mAbRepeatingState == ABREPEATING_NOW)&&wasPlaying)
        	sendBPointReachedMessage();
        show(sDefaultTimeout);
    }

    private View.OnClickListener mFfwdListener = new View.OnClickListener() {
        public void onClick(View v) {
        	forward();
        }
    };
    
    private void forward() {
    	int pos = mPlayer.getCurrentPosition();
        pos += 15000; // milliseconds
        mUpdatePausePlayCalledAfterSeek = true;
        mTempPauseMightOccur = true;
        boolean wasPlaying = isPlaying();
        mPlayer.seekTo(pos);
        setProgress();
        if ((mAbRepeatingState == ABREPEATING_NOW)&&((pos < mAPos)||(pos > mBPos))) {
        	clearABPos();
    		mAbRepeatingButton.setImageResource(R.drawable.apoint_or_bookmark);
    		setJumpButtonContents();
    		showToast(R.string.abpos_cleared);
        }
        if ((mAbRepeatingState == ABREPEATING_NOW)&&wasPlaying)
        	sendBPointReachedMessage();
        show(sDefaultTimeout);
    }

    private void installPrevNextListeners() {
        if (mNextButton != null) {
            mNextButton.setOnClickListener(mNextListener);
            mNextButton.setEnabled(mNextListener != null);
        }

        if (mPrevButton != null) {
            mPrevButton.setOnClickListener(mPrevListener);
            mPrevButton.setEnabled(mPrevListener != null);
        }
    }
    
    private View.OnClickListener mPrevListener = new View.OnClickListener() {
		public void onClick(View v) {
				int abstate = mAbRepeatingState;
				if (mABWideBar) {
					long apos = mAPos;
					if (mAbListTraverseMode) {
						if ((mPlayer.getCurrentPosition() - apos) < 2000) {
							prevAbPoints();
						} else {
							mPlayer.seekTo(safeLongToInt(apos));
							start();
						}
						setAbRepeatingButtonImage();
						setJumpButtonContents();
					} else {
						if (abstate != MediaPlaybackService.ABREPEATING_NOT) {
							if ((mPlayer.getCurrentPosition() - apos) < 2000) {
								clearABPos();
								setAbRepeatingButtonImage();
								setJumpButtonContents();
								showToast(R.string.abpos_cleared);
								prev();
							} else {
								mPlayer.seekTo(safeLongToInt(apos));
								start();
							}
						} else {
							if (mPlayer.getCurrentPosition() < 2000) {
								prev();								
							} else {
								mPlayer.seekTo(0);
								start();
							}
						}
					}
				} else {
					if (mPlayer.getCurrentPosition() < 2000) {
						prev();
					} else {
						mPlayer.seekTo(0);
						start();
					}
					if (abstate != MediaPlaybackService.ABREPEATING_NOT) {
						clearABPos();
						setAbRepeatingButtonImage();
						setJumpButtonContents();
						showToast(R.string.abpos_cleared);
					}
				}
		}
	};

    private View.OnClickListener mNextListener = new View.OnClickListener() {
        public void onClick(View v) {
				int abstate = mAbRepeatingState;
				if (mABWideBar) {
					if (mAbListTraverseMode) {
						nextAbPoints();	
						setAbRepeatingButtonImage();
						setJumpButtonContents();
					} else {
						if (abstate != MediaPlaybackService.ABREPEATING_NOT) {
							clearABPos();
							setAbRepeatingButtonImage();
							setJumpButtonContents();
							showToast(R.string.abpos_cleared);
						}
						next();
					}
				} else {
					next();
					if (abstate != MediaPlaybackService.ABREPEATING_NOT) {
						clearABPos();
						setAbRepeatingButtonImage();
						setJumpButtonContents();
						showToast(R.string.abpos_cleared);
					}
				}
        }
    };
    
    private void prev() {
    	
    }
    
    private void next() {
    	
    }
    
    private void start() {
//    	Log.i("SPEED","start called");
    	if (slowDown() == true) {
    		mHandler.removeMessages(SPEED_START);
    		mHandler.removeMessages(SPEED_STOP);
    		mHandler.sendEmptyMessageDelayed(SPEED_STOP,mSpeedRunmsecs);
    	}
    	mPlayer.start();
    	mPausedForSpeed = false;
    }
    
    private void pause() {
//    	Log.i("SPEED","pause called");
    	if (slowDown() == true) {
	    	mHandler.removeMessages(SPEED_START);
			mHandler.removeMessages(SPEED_STOP);
    	}
    	mPlayer.pause();
    	mPausedForSpeed = false;
    }
    
    private boolean isPlaying() {
    	return (mPlayer.isPlaying()||mPausedForSpeed);
    }

    public void setPrevNextListeners(View.OnClickListener next, View.OnClickListener prev) {
        mNextListener = next;
        mPrevListener = prev;
        mListenersSet = true;

        if (mRoot != null) {
            installPrevNextListeners();
            
            if (mNextButton != null && !mFromXml) {
                mNextButton.setVisibility(View.VISIBLE);
            }
            if (mPrevButton != null && !mFromXml) {
                mPrevButton.setVisibility(View.VISIBLE);
            }
        }
    }
    
    
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
    	@Override
    	public void onCallStateChanged (int state, String incomingNumber) {
    		super.onCallStateChanged(state, incomingNumber);
    		if (mPlayer != null) {
    		switch (state) {
    		case TelephonyManager.CALL_STATE_IDLE:
    			if(!isPlaying() && mWasPlaying) {
              	  mWasPlaying = false;
              	  mPlayer.seekTo(mPausedPosition);
              	  start();
                }
    			break;
    		case TelephonyManager.CALL_STATE_RINGING:
    		case TelephonyManager.CALL_STATE_OFFHOOK:
    		default:
              if(isPlaying()) {
            	  mWasPlaying = true;
            	  pause();
            	  mPausedPosition = mPlayer.getCurrentPosition();
              }
    		}
    		}
    	}
    	
    };
    
    public void onAbPointsSelected(Intent intent) {
    	long apos = intent.getLongExtra(ABDbAdapter.KEY_APOS, 0);
    	long bpos = intent.getLongExtra(ABDbAdapter.KEY_BPOS, 0);
    	mAbRecRowId = intent.getLongExtra(ABDbAdapter.KEY_ROWID, -1);
    	mAbEdited = false;
    	mPosInAbList = intent.getIntExtra(ABDbAdapter.KEY_POSITION, -1);
    	mAbSelectedfromList = true;
        	if (mAbRepeatingState != MediaPlaybackService.ABREPEATING_NOT) {
        		clearABPos();
        	}
        	
//        	seek((int) apos);
        	
        	if (bpos != 0) {
//        	setAPos();
        	mAPos = safeLongToInt (apos);
        	mAPosTime.setText(MusicUtils.makeTimeString(mContext, apos / 1000));
        	mAPosTime
    		.setCompoundDrawablesWithIntrinsicBounds(null, mContext.getResources().getDrawable(R.drawable.apostime),
    				null, null);
        	mAPosTime.setVisibility(View.VISIBLE);
        	
//        	seek((int) bpos);
//        	setBPos();
        	if ((bpos - mAPos) > 500) {
        		mBPos =safeLongToInt( bpos);
        		seek(mAPos);
        		sendBPointReachedMessage();
        		mAbRepeatingState = ABREPEATING_NOW;
        	}
            mBPosTime.setText(MusicUtils.makeTimeString(mContext, bpos / 1000));
            mBPosTime
    		.setCompoundDrawablesWithIntrinsicBounds(null, mContext.getResources().getDrawable(R.drawable.bpostime),
    				null, null);
            mBPosTime.setVisibility(View.VISIBLE);
            mJumpButtonCenter.setText("AB");
    		mJumpButtonCenter
    		.setCompoundDrawablesWithIntrinsicBounds(null, mContext.getResources().getDrawable(R.drawable.record),
    				null, null);
    		mJumpButtonCenter.setVisibility(View.VISIBLE);
    		
    		start();
        	seek(safeLongToInt (apos));
        	setAbRepeatingButtonImage();
            
        	} else {
        		if (MusicUtils.getBooleanPref(mContext, MusicUtils.Defs.NEW_BM, false)) {
        			String durationformat;
                	StringBuilder sFormatBuilder = new StringBuilder();
                	Formatter sFormatter = new Formatter(sFormatBuilder, Locale.getDefault());
                	Object[] sTimeArgs = new Object[5];
                	final Object[] timeArgs = sTimeArgs;
        			mAbRepeatingButton.setImageResource(R.drawable.bpoint);
        			seek(safeLongToInt (apos));
//            		setAPos();
        			mAPos = safeLongToInt (apos);
        	        mAbRepeatingState = ABREPEATING_WAITING;
            		long apossec = getAPos()/1000;
            		durationformat = 
            			apossec < 3600 ? "%2$d:%5$02d" : "%1$d:%3$02d:%5$02d";
            		sFormatBuilder.setLength(0);
                    timeArgs[0] = apossec / 3600;
                    timeArgs[1] = apossec / 60;
                    timeArgs[2] = (apossec / 60) % 60;
                    timeArgs[3] = apossec;
                    timeArgs[4] = apossec % 60;
            		mAPosTime.setText(sFormatter.format(durationformat, timeArgs).toString());
            		mAPosTime
            		.setCompoundDrawablesWithIntrinsicBounds(null, mContext.getResources().getDrawable(R.drawable.apostime_or_bookmark),
            				null, null);
            		mAPosTime.setVisibility(View.VISIBLE);
            		mJumpButtonCenter
            		.setCompoundDrawablesWithIntrinsicBounds(null, mContext.getResources().getDrawable(R.drawable.ic_bookmark_flat), null, null);
            		mJumpButtonCenter.setText("B Mark");
//            		mJumpButtonCenter.setVisibility(View.INVISIBLE);
            		mBPosTime
            		.setCompoundDrawablesWithIntrinsicBounds(null, mContext.getResources().getDrawable(R.drawable.cancel_flat), null, null);
            		mBPosTime.setText("Cancel");
            		mBPosTime.setVisibility(View.VISIBLE);
        		} else {
        			setJumpButtonContents();
        		}
//        		setJumpButtonContents();
        		start();
            	seek(safeLongToInt(apos));
            	setAbRepeatingButtonImage();
        	}
        	
//        	start();
//        	seek((int) apos);
//        	setAbRepeatingButtonImage();

    }
    
    public void onSpeedSelected(int speed) {
    	mSpeed = speed;
    	switch (speed) {
    	case 8:
    		mSpeedRunmsecs   =  25;
    		mSpeedPausemsecs = 175;
    		mSpeedButton.setImageResource(R.drawable.ic_speed_8);
    		break;
    	case 4:
    		mSpeedRunmsecs   =  50;
    		mSpeedPausemsecs = 150;
    		mSpeedButton.setImageResource(R.drawable.ic_speed_4);
    		break;
    	case 2:
    		mSpeedRunmsecs   = 100;
    		mSpeedPausemsecs = 100;
    		mSpeedButton.setImageResource(R.drawable.ic_speed_2);
    		break;
    	case 1:
    		mSpeedRunmsecs   = 200;
    		mSpeedPausemsecs =   0;
    		mSpeedButton.setImageResource(R.drawable.ic_speed_1);
    		break;
    	default:
    		mSpeedRunmsecs   = 200;
    		mSpeedPausemsecs =   0;
    		mSpeedButton.setImageResource(R.drawable.ic_speed_1);
    		
    	}
    	if (isPlaying()) {
    		mHandler.removeMessages(SPEED_START);
    		mHandler.removeMessages(SPEED_STOP);
    		start();
    	} else {
    		pause();
    	}
    	
    }
    
    public int getSpeedIdx() {
    	int idx = 3;
    	switch (mSpeed) {
    	case 8:
    		idx = 0;
    		break;
    	case 4:
    		idx = 1;
    		break;
    	case 2:
    		idx = 2;
    		break;
    	case 1:
    		idx = 3;
    		break;
    	}
    	
    	return idx;
    }
    
    private boolean slowDown() {
    	if (mSpeed == 1) return false;
    	return true;
    }
    
    private void statusChanged(boolean abmodeset) {
    	if (abmodeset) {
    		setPrevPauseNextToTravarseMode(mAbListTraverseMode);
    	} else {
    		setAbRepeatingButtonImage();
        	setJumpButtonContents();
    	}
    }
    
    public boolean ifAbSelectedFromList () {
    	if (mAbSelectedfromList == true) {
    		mAbSelectedfromList = false;
    		return true;
    	} else {
    		return false;
    	}
    }
    
    public void refreshController () {
    	if (mShowing) {
    		hide();
    	}
    }

    public interface MediaPlayerControl {
        void    start();
        void    pause();
        int     getDuration();
        int     getCurrentPosition();
        void    seekTo(int pos);
        boolean isPlaying();
        int     getBufferPercentage();
        boolean canPause();
        boolean canSeekBackward();
        boolean canSeekForward();
        String getPath();
    }
    
    public static int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE) {
        	return Integer.MIN_VALUE;
        }
        if (l > Integer.MAX_VALUE) {
        	return Integer.MAX_VALUE;
        }
        return (int) l;
    }
}
