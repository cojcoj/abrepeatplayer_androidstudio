package com.kojimahome.music21;

import java.text.DecimalFormat;
import java.util.Formatter;
import java.util.Locale;

import com.kojimahome.music21.MusicUtils.ServiceToken;

import wei.mark.standout.ui.Window;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class WidgetsWindow extends MultiWindow {
	
	private static final String LOGTAG = "WidgetsWindow";
    private static final int REFRESH = 1;
    private static final int QUIT = 2;
    private static final int GET_ALBUM_ART = 3;
    private static final int ALBUM_ART_DECODED = 4;
    private static final float INITIAL_WIDTH_DIP = 300;
    private static final float INITIAL_HEIGHT_DIP = 198;
    private static final float ABSHIFT_WIDTH_DIP = 300;
    private static final float ABSHIFT_HEIGHT_DIP = 225;
    private static final float DIALOG_WIDTH_DIP = 300;
    private static final float DIALOG_HEIGHT_DIP = 130;
    
    private static int INITIAL_WIDTH;
    private static int INITIAL_HEIGHT;
    private static int ABSHIFT_WIDTH;
    private static int ABSHIFT_HEIGHT;
    private static int DIALOG_WIDTH;
    private static int DIALOG_HEIGHT;
	
	public static final int DATA_CHANGED_TEXT = 0;
	public static final String INITIALIZE_WIDGETSWINDOW = "com.kojimahome.music21.initializewidgetswin";
	private static Context mContext;
	private static boolean mHiddenByABPosPicker = false;
	private long mAPos = 0;
    private long mBPos = 0;
    private int mAbRepeatingState = MediaPlaybackService.ABREPEATING_NOT;
	private static boolean mAbListTraverseMode = false;
	private static long mAbRecRowId = -1;
	private static boolean mAbEdited = false;
	private static long mLastClick = 0;
    private IMediaPlaybackService mService = null;
    private ServiceToken mToken;
	private TextView mMarqueeText;
	private ImageButton mPrevButton;
	private ImageButton mPauseButton;
	private ImageButton mNextButton;
	
	private LinearLayout mDialogLayout;
	private TextView mDialogText;
	private Button mDialogOK;
	private Button mDialogNeutral;
	private Button mDialogCancel;
	
	private ImageButton mAbRepeatingButton;
    private Button mJumpButtonCenter;
    private Button mAPosTime;
    private Button mBPosTime;
    private TextView mCurrentTime;
    private TextView mTotalTime;
    
//    private boolean mCheckModeOn = false;
    private Button mCheckModeButton;
    private Button mJumpZeroButton;
    private Button mLeftMostButton;
    private Button mLeftMidButton;
    private Button mLeftButton;
    private Button mRightButton;
    private Button mRightMidButton;
    private Button mRightMostButton;
    private Button mOkButton;
    private Boolean mPauseSetting = false;
    private LinearLayout mAbshiftLayout;
    private LinearLayout mMusicButtonsLayout;
    private int mShiftType = MediaPlaybackService.SHIFT_APOS;
    private int mId;
    private static boolean DOUBLECLICK_MODE_SWITCH = MusicUtils.Defs.DOUBLECLICK_MODE_SWITCH;
    
    private String mArtistName;
    private String mAlbumnName;
    private String mTrackName;
    private long mDuration;
    private String mMusicPath;
    private String mMusicData;
    
    private DecimalFormat df;
    
    private SharedPreferences mPreferences;
    
    private ABDbAdapter mABDbHelper;
	
	@Override
	public void createAndAttachView(final int id, FrameLayout frame) {
		mContext = this;
		mId = id;
		
		INITIAL_WIDTH = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
				INITIAL_WIDTH_DIP, getResources().getDisplayMetrics());
		INITIAL_HEIGHT = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
				INITIAL_HEIGHT_DIP, getResources().getDisplayMetrics());
		ABSHIFT_WIDTH = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
				ABSHIFT_WIDTH_DIP, getResources().getDisplayMetrics());
		ABSHIFT_HEIGHT = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
				ABSHIFT_HEIGHT_DIP, getResources().getDisplayMetrics());
		DIALOG_WIDTH = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
				DIALOG_WIDTH_DIP, getResources().getDisplayMetrics());
		DIALOG_HEIGHT = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
				DIALOG_HEIGHT_DIP, getResources().getDisplayMetrics());
		
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.widgets, frame, true);
		
		mPreferences = getSharedPreferences("ABRepeat", MODE_PRIVATE);
		
		df = new DecimalFormat();
        df.setMaximumFractionDigits(1);
        df.setMinimumFractionDigits(1);

		mMarqueeText = (TextView) view.findViewById(R.id.marqueetext);
		mPrevButton = (ImageButton) view.findViewById(R.id.prev);
//      mPrevButton.getDrawable().setColorFilter(0xFF00FFFF, PorterDuff.Mode.MULTIPLY);
		
		mPauseButton = (ImageButton) view.findViewById(R.id.pause);
		mNextButton = (ImageButton) view.findViewById(R.id.next);
		mAbRepeatingButton = (ImageButton) view.findViewById(R.id.abrepeat);
		mJumpButtonCenter = (Button) view.findViewById(R.id.jumpbuttoncenter);
		mAPosTime = (Button) view.findViewById(R.id.apostime);
        mBPosTime = (Button) view.findViewById(R.id.bpostime);
        mCurrentTime = (TextView) view.findViewById(R.id.currenttime);
        mTotalTime = (TextView) view.findViewById(R.id.totaltime);
        
        mDialogLayout = (LinearLayout) view.findViewById(R.id.dialoglayout);
        mDialogText = (TextView) view.findViewById(R.id.dialog_text);
        mDialogOK = (Button) view.findViewById(R.id.dialog_ok);
        mDialogNeutral = (Button) view.findViewById(R.id.dialog_neutral);
        mDialogCancel = (Button) view.findViewById(R.id.dialog_cancel);
        
        mAbshiftLayout = (LinearLayout) view.findViewById(R.id.abshiftlayout);
        mMusicButtonsLayout = (LinearLayout) view.findViewById(R.id.musicbuttonslayout);
        
        if (mABDbHelper == null) mABDbHelper = new ABDbAdapter(this);
        mABDbHelper.open();
        
		mPrevButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Intent i = new Intent(mContext, MediaPlaybackService.class);
	            i.setAction(MediaPlaybackService.SERVICECMD);
	            i.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDPREVIOUS);
	            mContext.startService(i);
			}
		}	
		);
		
		mPauseButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Intent i = new Intent(mContext, MediaPlaybackService.class);
	            i.setAction(MediaPlaybackService.SERVICECMD);
	            i.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDTOGGLEPAUSE);
	            mContext.startService(i);
	            long currenttime = System.currentTimeMillis();
	            if (currenttime - mLastClick < 900) {
//	            	if (DOUBLECLICK_MODE_SWITCH) {
	            	if (MusicUtils.getBooleanPref(mContext, MusicUtils.Defs.DOUBLECLICK_MODE_SWITCH_MAIN, false)) {
		            	Intent i2 = new Intent(mContext, MediaPlaybackService.class);
			            i2.setAction(MediaPlaybackService.SERVICECMD);
			            i2.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDSTOP);
			            mContext.startService(i2);
	            	}
	            } 
	            mLastClick = currenttime;
			}
		}	
		);
		
		mPauseButton.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View arg0) {
				Intent i = new Intent(mContext, MediaPlaybackService.class);
	            i.setAction(MediaPlaybackService.SERVICECMD);
	            i.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDSTOP);
	            mContext.startService(i);
	            return true;
			}
		}	
		);
		
		mNextButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Intent i = new Intent(mContext, MediaPlaybackService.class);
	            i.setAction(MediaPlaybackService.SERVICECMD);
	            i.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDNEXT);
	            mContext.startService(i);
			}
		}	
		);
		
		mAbRepeatingButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Intent i = new Intent(mContext, MediaPlaybackService.class);
	            i.setAction(MediaPlaybackService.SERVICECMD);
	            i.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDABREPEAT);
	            mContext.startService(i);
			}
		}	
		);
		
		mAbRepeatingButton.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				switch (mAbRepeatingState) {
				case MediaPlaybackService.ABREPEATING_NOT:
				case MediaPlaybackService.ABREPEATING_WAITING:
				case MediaPlaybackService.ABREPEATING_NOW:
				default:
					showAbList();
				}
				return true;
			}
			
		}
		);
		
		mAPosTime.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				switch (mAbRepeatingState) {
				case MediaPlaybackService.ABREPEATING_NOT:
				case MediaPlaybackService.ABREPEATING_WAITING:
					Intent i = new Intent(mContext, MediaPlaybackService.class);
		            i.setAction(MediaPlaybackService.SERVICECMD);
		            i.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDAPOSTIME);
		            mContext.startService(i);
		            break;
				case MediaPlaybackService.ABREPEATING_NOW:
					openAbShiftLayout(id, MediaPlaybackService.SHIFT_APOS, R.string.adjust_apoint, R.drawable.apoint);
//					mAbshiftLayout.setVisibility(View.VISIBLE);
//					mMusicButtonsLayout.setVisibility(View.GONE);
//					getWindow(id).edit().setSize(ABSHIFT_WIDTH, ABSHIFT_HEIGHT).commit();
//					mShiftType = MediaPlaybackService.SHIFT_APOS;
//					setTitle(id, getResources().getString(R.string.adjust_apoint));
//					setIcon(id, R.drawable.apoint);
				default:
				}
			}
		}	
		);
		
		mBPosTime.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
	            switch (mAbRepeatingState) {
				case MediaPlaybackService.ABREPEATING_NOT:
				case MediaPlaybackService.ABREPEATING_WAITING:
					Intent i = new Intent(mContext, MediaPlaybackService.class);
		            i.setAction(MediaPlaybackService.SERVICECMD);
		            i.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDBPOSTIME);
		            mContext.startService(i);
		            break;
				case MediaPlaybackService.ABREPEATING_NOW:
					openAbShiftLayout(id, MediaPlaybackService.SHIFT_BPOS, R.string.adjust_bpoint, R.drawable.bpoint);
//					mAbshiftLayout.setVisibility(View.VISIBLE);
//					mMusicButtonsLayout.setVisibility(View.GONE);
//					getWindow(id).edit().setSize(ABSHIFT_WIDTH, ABSHIFT_HEIGHT).commit();
//					mShiftType = MediaPlaybackService.SHIFT_BPOS;
//					setTitle(id, getResources().getString(R.string.adjust_bpoint));
//					setIcon(id, R.drawable.bpoint);
				default:
				}
			}
		}	
		);
		
		mJumpButtonCenter.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				String mWhereClause = "";
//            	mDialogText.setText(R.string.want_to_continue);
            	if (mAbEdited&&(mAbRecRowId == -1)) {
            		mAbEdited = false;
            	}
				switch (mAbRepeatingState) {
				case MediaPlaybackService.ABREPEATING_WAITING:
					if (MusicUtils.getBooleanPref(mContext, MusicUtils.Defs.ASK_IF_OVERWRITE, true)&&mAbEdited) {
						mDialogOK.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {
								Intent i = new Intent(mContext, MediaPlaybackService.class);
					            i.setAction(MediaPlaybackService.SERVICECMD);
					            i.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDJMPCENTER);
					            i.putExtra("update", true);
					            mContext.startService(i);	
					            closeDialogLayout();
							}	
						});
						mDialogNeutral.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {
								Intent i = new Intent(mContext, MediaPlaybackService.class);
					            i.setAction(MediaPlaybackService.SERVICECMD);
					            i.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDJMPCENTER);
					            i.putExtra("update", false);
					            mContext.startService(i);	
					            closeDialogLayout();
							}	
						});
						mDialogOK.setText(R.string.ab_update);
						mDialogNeutral.setText(R.string.ab_new);
						mDialogNeutral.setVisibility(View.VISIBLE);
						mDialogText.setText(R.string.ab_update_or_new);
						openDialogLayout(mId, R.string.ab_edited);
					} else {
						mWhereClause = ABDbAdapter.KEY_MUSICTITLE + "=\"" + mTrackName + "\""
	                    		+ " AND " + ABDbAdapter.KEY_MUSICDURATION + ">=" + Long.toString(mDuration - 1) 
	                    		+ " AND " + ABDbAdapter.KEY_MUSICDURATION + "<=" + Long.toString(mDuration + 1)
			            		+ " AND " + ABDbAdapter.KEY_APOS + "=" + Long.toString(mAPos)
			            		+ " AND " + ABDbAdapter.KEY_BPOS + "=" + Long.toString(0);
						if (MusicUtils.getBooleanPref(mContext, MusicUtils.Defs.ASK_IF_DUPLICATED, true)&&mABDbHelper.checkIfABPosExists(mWhereClause)) {
							mDialogOK.setOnClickListener(new OnClickListener() {
								@Override
								public void onClick(View v) {
									Intent i = new Intent(mContext, MediaPlaybackService.class);
						            i.setAction(MediaPlaybackService.SERVICECMD);
						            i.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDJMPCENTER);
						            mContext.startService(i);	
						            closeDialogLayout();
								}	
							});
							mDialogNeutral.setVisibility(View.GONE);
							mDialogOK.setText(android.R.string.ok);
							mDialogText.setText(R.string.want_to_continue);
							openDialogLayout(mId, R.string.same_ab_exists);
							
						} else {
							Intent i = new Intent(mContext, MediaPlaybackService.class);
				            i.setAction(MediaPlaybackService.SERVICECMD);
				            i.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDJMPCENTER);
				            mContext.startService(i);
						}
					}
//					mWhereClause = ABDbAdapter.KEY_MUSICTITLE + "=\"" + mTrackName + "\""
//                    		+ " AND " + ABDbAdapter.KEY_MUSICDURATION + ">=" + Long.toString(mDuration - 1) 
//                    		+ " AND " + ABDbAdapter.KEY_MUSICDURATION + "<=" + Long.toString(mDuration + 1)
//		            		+ " AND " + ABDbAdapter.KEY_APOS + "=" + Long.toString(mAPos)
//		            		+ " AND " + ABDbAdapter.KEY_BPOS + "=" + Long.toString(0);
//					if (MusicUtils.getBooleanPref(mContext, MusicUtils.Defs.ASK_IF_DUPLICATED, true)&&mABDbHelper.checkIfABPosExists(mWhereClause)) {
//						mDialogOK.setOnClickListener(new OnClickListener() {
//							@Override
//							public void onClick(View v) {
//								Intent i = new Intent(mContext, MediaPlaybackService.class);
//					            i.setAction(MediaPlaybackService.SERVICECMD);
//					            i.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDJMPCENTER);
//					            mContext.startService(i);	
//					            closeDialogLayout();
//							}	
//						});
//						mDialogNeutral.setVisibility(View.GONE);
//						mDialogOK.setText(android.R.string.ok);
//						openDialogLayout(mId, R.string.same_ab_exists);
//						
//					} else {
//						Intent i = new Intent(mContext, MediaPlaybackService.class);
//			            i.setAction(MediaPlaybackService.SERVICECMD);
//			            i.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDJMPCENTER);
//			            mContext.startService(i);
//					}
					break;
				case MediaPlaybackService.ABREPEATING_NOW:
					if (MusicUtils.getBooleanPref(mContext, MusicUtils.Defs.ASK_IF_OVERWRITE, true)&&mAbEdited) {
						mDialogOK.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {
								Intent i = new Intent(mContext, MediaPlaybackService.class);
					            i.setAction(MediaPlaybackService.SERVICECMD);
					            i.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDJMPCENTER);
					            i.putExtra("update", true);
					            mContext.startService(i);	
					            closeDialogLayout();
							}	
						});
						mDialogNeutral.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {
								Intent i = new Intent(mContext, MediaPlaybackService.class);
					            i.setAction(MediaPlaybackService.SERVICECMD);
					            i.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDJMPCENTER);
					            i.putExtra("update", false);
					            mContext.startService(i);	
					            closeDialogLayout();
							}	
						});
						mDialogOK.setText(R.string.ab_update);
						mDialogNeutral.setText(R.string.ab_new);
						mDialogNeutral.setVisibility(View.VISIBLE);
						mDialogText.setText(R.string.ab_update_or_new);
						openDialogLayout(mId, R.string.ab_edited);
					} else {
						mWhereClause = ABDbAdapter.KEY_MUSICTITLE + "=\"" + mTrackName + "\""
	                    		+ " AND " + ABDbAdapter.KEY_MUSICDURATION + ">=" + Long.toString(mDuration - 1) 
	                    		+ " AND " + ABDbAdapter.KEY_MUSICDURATION + "<=" + Long.toString(mDuration + 1)
			            		+ " AND " + ABDbAdapter.KEY_APOS + "=" + Long.toString(mAPos)
			            		+ " AND " + ABDbAdapter.KEY_BPOS + "=" + Long.toString(mBPos);
						if (MusicUtils.getBooleanPref(mContext, MusicUtils.Defs.ASK_IF_DUPLICATED, true)&&mABDbHelper.checkIfABPosExists(mWhereClause)) {
							mDialogOK.setOnClickListener(new OnClickListener() {
								@Override
								public void onClick(View v) {
									Intent i = new Intent(mContext, MediaPlaybackService.class);
						            i.setAction(MediaPlaybackService.SERVICECMD);
						            i.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDJMPCENTER);
						            mContext.startService(i);	
						            closeDialogLayout();
								}	
							});
							mDialogNeutral.setVisibility(View.GONE);
							mDialogOK.setText(android.R.string.ok);
							mDialogText.setText(R.string.want_to_continue);
							openDialogLayout(mId, R.string.same_ab_exists);
						} else {
							Intent i = new Intent(mContext, MediaPlaybackService.class);
				            i.setAction(MediaPlaybackService.SERVICECMD);
				            i.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDJMPCENTER);
				            mContext.startService(i);
						}
					}
//					mWhereClause = ABDbAdapter.KEY_MUSICTITLE + "=\"" + mTrackName + "\""
//                    		+ " AND " + ABDbAdapter.KEY_MUSICDURATION + ">=" + Long.toString(mDuration - 1) 
//                    		+ " AND " + ABDbAdapter.KEY_MUSICDURATION + "<=" + Long.toString(mDuration + 1)
//		            		+ " AND " + ABDbAdapter.KEY_APOS + "=" + Long.toString(mAPos)
//		            		+ " AND " + ABDbAdapter.KEY_BPOS + "=" + Long.toString(mBPos);
//					if (MusicUtils.getBooleanPref(mContext, MusicUtils.Defs.ASK_IF_DUPLICATED, true)&&mABDbHelper.checkIfABPosExists(mWhereClause)) {
//						mDialogOK.setOnClickListener(new OnClickListener() {
//							@Override
//							public void onClick(View v) {
//								Intent i = new Intent(mContext, MediaPlaybackService.class);
//					            i.setAction(MediaPlaybackService.SERVICECMD);
//					            i.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDJMPCENTER);
//					            mContext.startService(i);	
//					            closeDialogLayout();
//							}	
//						});
//						mDialogNeutral.setVisibility(View.GONE);
//						openDialogLayout(mId, R.string.same_ab_exists);
//					} else {
//						Intent i = new Intent(mContext, MediaPlaybackService.class);
//			            i.setAction(MediaPlaybackService.SERVICECMD);
//			            i.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDJMPCENTER);
//			            mContext.startService(i);
//					}
					break;
				case MediaPlaybackService.ABREPEATING_NOT:
				default:
					Intent i = new Intent(mContext, MediaPlaybackService.class);
		            i.setAction(MediaPlaybackService.SERVICECMD);
		            i.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDJMPCENTER);
		            mContext.startService(i);
				}
//				Intent i = new Intent(mContext, MediaPlaybackService.class);
//	            i.setAction(MediaPlaybackService.SERVICECMD);
//	            i.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDJMPCENTER);
//	            mContext.startService(i);
			}
		}	
		);
		
		mJumpButtonCenter.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				switch (mAbRepeatingState) {
				case MediaPlaybackService.ABREPEATING_NOT:
				case MediaPlaybackService.ABREPEATING_WAITING:
				case MediaPlaybackService.ABREPEATING_NOW:
				default:
					showAbList();
				}
				return true;
			}
			
		}		
		);
		
		mDialogCancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
	            closeDialogLayout();
			}	
		}
		);
		
		mDialogOK.setText(android.R.string.ok);
		mDialogCancel.setText(android.R.string.cancel);
		
		setAbShiftLayoutButtons(mContext, view);
		
//		mToken = MusicUtils.bindToService(mContext, osc);
//        if (mToken == null) {
//            // something went wrong
//            mHandler.sendEmptyMessage(QUIT);
//        }
		IntentFilter f = new IntentFilter();
        f.addAction(MediaPlaybackService.PLAYSTATE_CHANGED);
        f.addAction(MediaPlaybackService.META_CHANGED);
        f.addAction(MediaPlaybackService.PLAYBACK_COMPLETE);
        f.addAction(MediaPlaybackService.ABPOS_CLEARD_BY_BT);
        f.addAction(MediaPlaybackService.ABPOS_SET_BY_BT);
        f.addAction(ABPosPickerActivity.ACTION_ABPOSPICKER_CLOSED);
        registerReceiver(mStatusListener, new IntentFilter(f));
        
        AudioManager mAudioManager;
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
	        mAudioManager.registerMediaButtonEventReceiver(new ComponentName(getPackageName(),
	                MediaButtonIntentReceiver.class.getName()));
        }
//        Log.i(LOGTAG,"ButtonEventRcv Registered:"+ this.getPackageName() +"  " + MediaButtonIntentReceiver.class.getName());
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
//		Log.i(LOGTAG, "Start Cmd received");
		if (intent != null) {
//			Log.i(LOGTAG, "Intent is not null");
			if (intent.getAction().equals(INITIALIZE_WIDGETSWINDOW)) {
//				Log.i(LOGTAG, "Initialize WidgetsWindow Trackinfo");
				setPauseButtonImage(intent.getBooleanExtra(MediaPlaybackService.PLAY_STATE, false));
				updateTrackInfo(intent);
				setABJumpButtonsForABRepeat();
			}			
		}		
		return super.onStartCommand(intent, flags, startId);
	}
	
	public class MyListener implements android.content.DialogInterface.OnClickListener {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			// TODO Auto-generated method stub
			
		}
		
	}

	@Override
	public StandOutLayoutParams getParams(int id, Window window) {
		INITIAL_WIDTH = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
				INITIAL_WIDTH_DIP, getResources().getDisplayMetrics());
		INITIAL_HEIGHT = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
				INITIAL_HEIGHT_DIP, getResources().getDisplayMetrics());
		ABSHIFT_WIDTH = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
				ABSHIFT_WIDTH_DIP, getResources().getDisplayMetrics());
		ABSHIFT_HEIGHT = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
				ABSHIFT_HEIGHT_DIP, getResources().getDisplayMetrics());
		DIALOG_WIDTH = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
				DIALOG_WIDTH_DIP, getResources().getDisplayMetrics());
		DIALOG_HEIGHT = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
				DIALOG_HEIGHT_DIP, getResources().getDisplayMetrics());
		return new StandOutLayoutParams(id, INITIAL_WIDTH, INITIAL_HEIGHT,
				StandOutLayoutParams.RIGHT, StandOutLayoutParams.BOTTOM);
	}

	@Override
	public String getAppName() {
		return "AB Repeat";
	}

	@Override
	public int getThemeStyle() {
		return android.R.style.Theme_Light;
	}
	
	private BroadcastReceiver mStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
//            Log.i(LOGTAG, "StatusListner intent play_state:"+ intent.getBooleanExtra(MediaPlaybackService.PLAY_STATE, false));
            boolean playing = intent.getBooleanExtra(MediaPlaybackService.PLAY_STATE, false);
            if (action.equals(MediaPlaybackService.META_CHANGED)) {
                // redraw the artist/title info and
                // set new max for progress bar
                updateTrackInfo(intent);
                setPauseButtonImage(playing);
                setABJumpButtonsForABRepeat();
//                queueNextRefresh(1);
            } else if (action.equals(MediaPlaybackService.PLAYBACK_COMPLETE)) {
                    setPauseButtonImage(playing);
                    setABJumpButtonsForABRepeat();
            } else if (action.equals(MediaPlaybackService.PLAYSTATE_CHANGED)) {
                setPauseButtonImage(playing);
                setABJumpButtonsForABRepeat();
            } else if (action.equals(MediaPlaybackService.ABPOS_CLEARD_BY_BT)) {
//            	Log.i(LOGTAG,"ABPOS_CLEARD_BY_BT Received");
            	updateTrackInfo(intent);
//            	mAbListTraverseMode = false;
            	setABJumpButtonsForABRepeat();
            	setJumpButtonContents();
            } else if (action.equals(MediaPlaybackService.ABPOS_SET_BY_BT)) {
//            	Log.i(LOGTAG,"ABPOS_SET_BY_BT Received");
            	updateTrackInfo(intent);
//            	mAbListTraverseMode = true;
            	setABJumpButtonsForABRepeat ();
            } else if (action.equals(ABPosPickerActivity.ACTION_ABPOSPICKER_CLOSED)) {
            	if (mHiddenByABPosPicker) {
            		show(mId);
            		NotificationManager mNotificationManager = (NotificationManager) 
            		getSystemService(Context.NOTIFICATION_SERVICE);
            		mNotificationManager.cancel(mHiddenNotificationId);
//            		Log.i(LOGTAG,"Notify ID:"+(getClass().hashCode() + mId));
            		mHiddenByABPosPicker = false;
            	}
            }
        }
    };
    
    private void setPauseButtonImage(boolean play_state) {
        try {
            if (play_state) {
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
        setPrevPauseNextToTravarseMode(mAbListTraverseMode);
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
    
    private void updateTrackInfo(Intent i) {
      mArtistName = i.getStringExtra("artist");
      if (mArtistName == null) mArtistName = "";
      mAlbumnName = i.getStringExtra("album");
      if (mAlbumnName == null) mAlbumnName = "";
      mTrackName = i.getStringExtra("track");
      if (mTrackName == null) mTrackName = "";
      mMusicPath = i.getStringExtra(ABDbAdapter.KEY_MUSICPATH);
      if (mMusicPath == null) mMusicPath = "";
      mMusicData = i.getStringExtra(ABDbAdapter.KEY_MUSIC_DATA);
      if (mMusicData == null) mMusicData = "";
      
      mMarqueeText.setText(mTrackName + " : " + mAlbumnName + " : " + mArtistName);
      mDuration = i.getLongExtra(ABDbAdapter.KEY_MUSICDURATION, 0);
      mTotalTime.setText(MusicUtils.makeTimeString(this, mDuration / 1000));
      mAbRepeatingState = i.getIntExtra(MediaPlaybackService.AB_STATE, 
    		  MediaPlaybackService.ABREPEATING_NOT);
      mAPos = i.getLongExtra(ABDbAdapter.KEY_APOS, 0);
      mBPos = i.getLongExtra(ABDbAdapter.KEY_BPOS, 0);
      mAbListTraverseMode = i.getBooleanExtra(MediaPlaybackService.AB_LIST_TRAVERSE_MODE, false);
      mAbRecRowId = i.getLongExtra(MediaPlaybackService.AB_REC_ROWID, -1);
      mAbEdited = i.getBooleanExtra(MediaPlaybackService.AB_EDITED, false);
    }
    
    public void setAbRepeatingButtonImage() {
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
    	
    	
//    	try {
//    		if (mService != null) {
//    			Log.i(LOGTAG,"TraverseMode2488:"+mService.abListTraverseMode());
    			setPrevPauseNextToTravarseMode(mAbListTraverseMode);
            switch (mAbRepeatingState) {
            	case MediaPlaybackService.ABREPEATING_WAITING:
//            		Log.i(LOGTAG,"ABREPEATING:WAITING");
            		mAbRepeatingButton.setImageResource(R.drawable.bpoint);
//            		mService.setAPos();
            		apos = mAPos/1000;
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
//            			mService.setABPause(mPreferences.getLong("abpause", 0));
            			mAbRepeatingButton.setImageResource(R.drawable.delpoints);
                		bpos = mBPos/1000;
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
                		apos = mAPos/1000;
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
            }
//            }}
//        } catch (RemoteException ex) {
//        }
    }
    
    private void setAbShiftLayoutButtons(Context context, View view) {
    	mLeftMostButton = (Button) view.findViewById(R.id.leftmostbutton);
        mLeftMidButton = (Button) view.findViewById(R.id.leftmidbutton);
        mLeftButton = (Button) view.findViewById(R.id.leftbutton);
        mRightButton = (Button) view.findViewById(R.id.rightbutton);
        mRightMidButton = (Button) view.findViewById(R.id.rightmidbutton);
        mRightMostButton = (Button) view.findViewById(R.id.rightmostbutton);
        
        mCheckModeButton = (Button) view.findViewById(R.id.checkmodebutton);
        mJumpZeroButton = (Button) view.findViewById(R.id.jumpzerobutton);
        mOkButton = (Button) view.findViewById(R.id.ok_button);
        
        mLeftMostButton.setCompoundDrawablesWithIntrinsicBounds(null, 
        		context.getResources().getDrawable(R.drawable.jump3), null, null);
        mLeftMidButton.setCompoundDrawablesWithIntrinsicBounds(null, 
        		context.getResources().getDrawable(R.drawable.jump2), null, null);
        mLeftButton.setCompoundDrawablesWithIntrinsicBounds(null, 
        		context.getResources().getDrawable(R.drawable.jump1), null, null);
        mRightButton.setCompoundDrawablesWithIntrinsicBounds(null, 
        		context.getResources().getDrawable(R.drawable.rjump1), null, null);
        mRightMidButton.setCompoundDrawablesWithIntrinsicBounds(null, 
        		context.getResources().getDrawable(R.drawable.rjump2), null, null);
        mRightMostButton.setCompoundDrawablesWithIntrinsicBounds(null, 
        		context.getResources().getDrawable(R.drawable.rjump3), null, null);
        
        mCheckModeButton.setCompoundDrawablesWithIntrinsicBounds(null, 
        		context.getResources().getDrawable(R.drawable.checkmode_off), null, null);
        
        mJumpZeroButton.setCompoundDrawablesWithIntrinsicBounds(null, 
        		(mPauseSetting) ? context.getResources().getDrawable(R.drawable.pause0)
        				: context.getResources().getDrawable(R.drawable.jump_zero), null, null);
        
        mLeftMostButton.setShadowLayer(3, 0, 0, R.color.button_text_shadow_color);
        mLeftMidButton.setShadowLayer(3, 0, 0, R.color.button_text_shadow_color);
        mLeftButton.setShadowLayer(3, 0, 0, R.color.button_text_shadow_color);
        mRightButton.setShadowLayer(3, 0, 0, R.color.button_text_shadow_color);
        mRightMidButton.setShadowLayer(3, 0, 0, R.color.button_text_shadow_color);
        mRightMostButton.setShadowLayer(3, 0, 0, R.color.button_text_shadow_color);
        
        mLeftMostButton.setText(df.format(((float) mPreferences.getLong("abshiftone", 2000))/1000));
        mLeftMidButton.setText(df.format(((float) mPreferences.getLong("abshifttwo", 1000))/1000));
        mLeftButton.setText(df.format(((float) mPreferences.getLong("abshiftthree", 200))/1000));
        mRightButton.setText(df.format(((float) mPreferences.getLong("abshiftthree", 200))/1000));
        mRightMidButton.setText(df.format(((float) mPreferences.getLong("abshifttwo", 1000))/1000));
        mRightMostButton.setText(df.format(((float) mPreferences.getLong("abshiftone", 2000))/1000));
        
        mCheckModeButton.setText("Check");
        mJumpZeroButton.setText((mPauseSetting) ? 
        		df.format(((float) mPreferences.getLong("abpause", 0))/1000) :"0");
        
        mLeftMostButton.setVisibility(View.VISIBLE);
        mLeftMidButton.setVisibility(View.VISIBLE);
        mLeftButton.setVisibility(View.VISIBLE);
        mRightButton.setVisibility(View.VISIBLE);
        mRightMidButton.setVisibility(View.VISIBLE);
        mRightMostButton.setVisibility(View.VISIBLE);
        
        mCheckModeButton.setVisibility(View.VISIBLE);
        mJumpZeroButton.setVisibility((mPauseSetting) ? View.VISIBLE : View.INVISIBLE);
        
        mLeftMostButton.setOnClickListener(mLeftMostClickListener);
        mLeftMidButton.setOnClickListener(mLeftMidClickListener);
        mLeftButton.setOnClickListener(mLeftClickListener);
        mRightButton.setOnClickListener(mRightClickListener);
        mRightMidButton.setOnClickListener(mRightMidClickListener);
        mRightMostButton.setOnClickListener(mRightMostClickListener);
        
        mCheckModeButton.setOnClickListener(mCheckModeClickListener);
        mJumpZeroButton.setOnClickListener(mJumpZeroClickListener);
        mOkButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
//				mAbshiftLayout.setVisibility(View.GONE);
//				mMusicButtonsLayout.setVisibility(View.VISIBLE);
//				getWindow(mId).edit().setSize(INITIAL_WIDTH, INITIAL_HEIGHT).commit();
//				setTitle(mId, "AB Repeat");
//				setIcon(mId, R.drawable.favicon36);
				closeAbShiftLayout();
			}
		}	
		);
        

//        mLeftMostButton.setLongClickable(true);
//        mLeftMidButton.setLongClickable(true);
//        mLeftButton.setLongClickable(true);
//        mRightButton.setLongClickable(true);
//        mRightMidButton.setLongClickable(true);
//        mRightMostButton.setLongClickable(true);
//        
//        mLeftMostButton.setOnLongClickListener(mLeftMostLongListener);
//        mLeftMidButton.setOnLongClickListener(mLeftMidLongListener);
//        mLeftButton.setOnLongClickListener(mLeftLongListener);
//        mRightButton.setOnLongClickListener(mRightLongListener);
//        mRightMidButton.setOnLongClickListener(mRightMidLongListener);
//        mRightMostButton.setOnLongClickListener(mRightMostLongListener);
    }
    
    private View.OnClickListener mLeftMostClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			Intent i = new Intent(mContext, MediaPlaybackService.class);
            i.setAction(MediaPlaybackService.SERVICECMD);
            i.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDSHIFTABPOS);
            i.putExtra(MediaPlaybackService.SHIFT_DISTANCE, 
            		(- mPreferences.getLong("abshiftone", 2000)));
            i.putExtra(MediaPlaybackService.SHIFT_TYPE, mShiftType);
            i.putExtra(MediaPlaybackService.CHECKMODEON, getCheckMode());
            mContext.startService(i);
            if (!getCheckMode()) closeAbShiftLayout();
		}
    	
    };
    
    private View.OnClickListener mLeftMidClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			Intent i = new Intent(mContext, MediaPlaybackService.class);
            i.setAction(MediaPlaybackService.SERVICECMD);
            i.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDSHIFTABPOS);
            i.putExtra(MediaPlaybackService.SHIFT_DISTANCE, 
            		(- mPreferences.getLong("abshifttwo", 1000)));
            i.putExtra(MediaPlaybackService.SHIFT_TYPE, mShiftType);
            i.putExtra(MediaPlaybackService.CHECKMODEON, getCheckMode());
            mContext.startService(i);
            if (!getCheckMode()) closeAbShiftLayout();
		}
    	
    };
    
    private View.OnClickListener mLeftClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			Intent i = new Intent(mContext, MediaPlaybackService.class);
            i.setAction(MediaPlaybackService.SERVICECMD);
            i.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDSHIFTABPOS);
            i.putExtra(MediaPlaybackService.SHIFT_DISTANCE, 
            		(- mPreferences.getLong("abshiftthree", 200)));
            i.putExtra(MediaPlaybackService.SHIFT_TYPE, mShiftType);
            i.putExtra(MediaPlaybackService.CHECKMODEON, getCheckMode());
            mContext.startService(i);
            if (!getCheckMode()) closeAbShiftLayout();
		}
    	
    };
    
    private View.OnClickListener mRightMostClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			Intent i = new Intent(mContext, MediaPlaybackService.class);
            i.setAction(MediaPlaybackService.SERVICECMD);
            i.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDSHIFTABPOS);
            i.putExtra(MediaPlaybackService.SHIFT_DISTANCE, 
            		(mPreferences.getLong("abshiftone", 2000)));
            i.putExtra(MediaPlaybackService.SHIFT_TYPE, mShiftType);
            i.putExtra(MediaPlaybackService.CHECKMODEON, getCheckMode());
            mContext.startService(i);
            if (!getCheckMode()) closeAbShiftLayout();
		}
    	
    };
    
    private View.OnClickListener mRightMidClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			Intent i = new Intent(mContext, MediaPlaybackService.class);
            i.setAction(MediaPlaybackService.SERVICECMD);
            i.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDSHIFTABPOS);
            i.putExtra(MediaPlaybackService.SHIFT_DISTANCE, 
            		(mPreferences.getLong("abshifttwo", 1000)));
            i.putExtra(MediaPlaybackService.SHIFT_TYPE, mShiftType);
            i.putExtra(MediaPlaybackService.CHECKMODEON, getCheckMode());
            mContext.startService(i);
            if (!getCheckMode()) closeAbShiftLayout();
		}
    	
    };
    
    private View.OnClickListener mRightClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			Intent i = new Intent(mContext, MediaPlaybackService.class);
            i.setAction(MediaPlaybackService.SERVICECMD);
            i.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDSHIFTABPOS);
            i.putExtra(MediaPlaybackService.SHIFT_DISTANCE, 
            		(mPreferences.getLong("abshiftthree", 200)));
            i.putExtra(MediaPlaybackService.SHIFT_TYPE, mShiftType);
            i.putExtra(MediaPlaybackService.CHECKMODEON, getCheckMode());
            mContext.startService(i);
            if (!getCheckMode()) closeAbShiftLayout();
		}
    	
    };
    
    private View.OnClickListener mCheckModeClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
//			if (getCheckMode() == true) {
//				setCheckMode(false);
//				mCheckModeButton.setCompoundDrawablesWithIntrinsicBounds(null, 
//		        		mContext.getResources().getDrawable(R.drawable.checkmode_off), null, null);
//				if (mPauseSetting) {
//					mJumpZeroButton.setCompoundDrawablesWithIntrinsicBounds(null, 
//			        		mContext.getResources().getDrawable(R.drawable.pause0), null, null);
//					mJumpZeroButton.setText(df.format(((float) mPreferences.getLong("abpause", 0))/1000));
//				} else {
//					mJumpZeroButton.setVisibility(View.INVISIBLE);
//				}
////				setButton(mContext.getText(android.R.string.cancel), (OnClickListener) null);
//			} else {
//				setCheckMode(true);
//				mCheckModeButton.setCompoundDrawablesWithIntrinsicBounds(null, 
//		        		mContext.getResources().getDrawable(R.drawable.checkmode_on), null, null);
//				if (mPauseSetting) {
//					mJumpZeroButton.setCompoundDrawablesWithIntrinsicBounds(null, 
//			        		mContext.getResources().getDrawable(R.drawable.jump_zero), null, null);
//					mJumpZeroButton.setText("0");
//				} else {
//					mJumpZeroButton.setVisibility(View.VISIBLE);
//				}
////				setButton(mContext.getText(android.R.string.ok), (OnClickListener) null);
//			}
			if (getCheckMode() == true) {
				setCheckMode(false);
			} else {
				setCheckMode(true);
			}
			setCheckModeandJumpZeroButton();
		}
    	
    };
    
    private void setCheckModeandJumpZeroButton() {
    	if (getCheckMode() == false) {
//			setCheckMode(false);
			mCheckModeButton.setCompoundDrawablesWithIntrinsicBounds(null, 
	        		mContext.getResources().getDrawable(R.drawable.checkmode_off), null, null);
			if (mPauseSetting) {
				mJumpZeroButton.setCompoundDrawablesWithIntrinsicBounds(null, 
		        		mContext.getResources().getDrawable(R.drawable.pause0), null, null);
				mJumpZeroButton.setText(df.format(((float) mPreferences.getLong("abpause", 0))/1000));
			} else {
				mJumpZeroButton.setVisibility(View.INVISIBLE);
			}
//			setButton(mContext.getText(android.R.string.cancel), (OnClickListener) null);
		} else {
//			setCheckMode(true);
			mCheckModeButton.setCompoundDrawablesWithIntrinsicBounds(null, 
	        		mContext.getResources().getDrawable(R.drawable.checkmode_on), null, null);
			mJumpZeroButton.setCompoundDrawablesWithIntrinsicBounds(null, 
	        		mContext.getResources().getDrawable(R.drawable.jump_zero), null, null);
			mJumpZeroButton.setText("0");
			if (mPauseSetting) {
//				mJumpZeroButton.setCompoundDrawablesWithIntrinsicBounds(null, 
//		        		mContext.getResources().getDrawable(R.drawable.jump_zero), null, null);
//				mJumpZeroButton.setText("0");
			} else {
				mJumpZeroButton.setVisibility(View.VISIBLE);
			}
//			setButton(mContext.getText(android.R.string.ok), (OnClickListener) null);
		}    	
    }
    
    private View.OnClickListener mJumpZeroClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			if (getCheckMode()) {
				Intent i = new Intent(mContext, MediaPlaybackService.class);
	            i.setAction(MediaPlaybackService.SERVICECMD);
	            i.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDSHIFTABPOS);
	            i.putExtra(MediaPlaybackService.SHIFT_DISTANCE, 0);
	            i.putExtra(MediaPlaybackService.SHIFT_TYPE, mShiftType);
	            i.putExtra(MediaPlaybackService.CHECKMODEON, getCheckMode());
	            mContext.startService(i);
			}
		}
    	
    };
    
    private void notifyChange() {
    	Intent i = new Intent(mContext, MediaPlaybackService.class);
        i.setAction(MediaPlaybackService.SERVICECMD);
        i.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDNOTIFYCHANGE);
        mContext.startService(i);
    }
    
    private void showAbList(){
		Intent intent = new Intent();
        intent.setClass(this, ABPosPickerActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(ABDbAdapter.KEY_MUSICTITLE, mTrackName);
        intent.putExtra(ABDbAdapter.KEY_MUSIC_DATA, mMusicData);
        intent.putExtra(ABDbAdapter.KEY_MUSICPATH, mMusicPath);
        intent.putExtra(ABDbAdapter.KEY_MUSICDURATION, mDuration);
        intent.putExtra(ABPosPickerActivity.CALLEDFROMFLOATPAD, true);
        this.startActivity(intent);
        Intent i2 = new Intent(mContext, MediaPlaybackService.class);
        i2.setAction(MediaPlaybackService.SERVICECMD);
        i2.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDPAUSE);
        mContext.startService(i2);
        hide(mId);
        mHiddenByABPosPicker = true;
	}
    
    private void openAbShiftLayout(int id, int shift_type, int title_resource, int icon_resource) {
    	mAbshiftLayout.setVisibility(View.VISIBLE);
		mMusicButtonsLayout.setVisibility(View.GONE);
		getWindow(id).edit().setSize(ABSHIFT_WIDTH, ABSHIFT_HEIGHT).commit();
		mShiftType = shift_type;
		setTitle(id, getResources().getString(title_resource));
		setIcon(id, icon_resource);
		setCheckModeandJumpZeroButton();
    }
    
    private void closeAbShiftLayout() {
    	mAbshiftLayout.setVisibility(View.GONE);
		mMusicButtonsLayout.setVisibility(View.VISIBLE);
		getWindow(mId).edit().setSize(INITIAL_WIDTH, INITIAL_HEIGHT).commit();
		setTitle(mId, "AB Repeat");
		setIcon(mId, R.drawable.favicon36);
    }
    
    private void openDialogLayout(int id, int title_resource) {
    	mDialogLayout.setVisibility(View.VISIBLE);
		mMusicButtonsLayout.setVisibility(View.GONE);
		mMarqueeText.setVisibility(View.GONE);
		getWindow(mId).edit().setSize(DIALOG_WIDTH, DIALOG_HEIGHT).commit();
		setTitle(mId, getResources().getString(title_resource));
//		setIcon(mId, R.drawable.favicon36);
    }
    
    private void closeDialogLayout() {
    	mDialogLayout.setVisibility(View.GONE);
		mMusicButtonsLayout.setVisibility(View.VISIBLE);
		mMarqueeText.setVisibility(View.VISIBLE);
		getWindow(mId).edit().setSize(INITIAL_WIDTH, INITIAL_HEIGHT).commit();
		setTitle(mId, "AB Repeat");
//		setIcon(mId, R.drawable.favicon36);
    }
    
    private boolean getCheckMode() {
    	return MusicUtils.getBooleanPref(mContext, MusicUtils.Defs.CHECK_MODE, false);
    }
    
    private void setCheckMode(boolean checkmode) {
    	MusicUtils.setBooleanPref(mContext, MusicUtils.Defs.CHECK_MODE, checkmode);
    }
    
//	private ServiceConnection osc = new ServiceConnection() {
//		public void onServiceConnected(ComponentName classname, IBinder obj) {
//			String durationformat;
//			StringBuilder sFormatBuilder = new StringBuilder();
//			Formatter sFormatter = new Formatter(sFormatBuilder,
//					Locale.getDefault());
//			Object[] sTimeArgs = new Object[5];
//			final Object[] timeArgs = sTimeArgs;
//			Long apos;
//			Long bpos;
//			mService = IMediaPlaybackService.Stub.asInterface(obj);
////			startPlayback();
//		}
//		public void onServiceDisconnected(ComponentName classname) {
//			mService = null;
//		}
//	};
    
//    ABShiftPickerDialog.OnShiftDistSetListener mAShiftDistSetListener =
//    	new ABShiftPickerDialog.OnShiftDistSetListener() {
//    		public void onShiftDistSet (long shiftdist, boolean checkmodeon) {
////    			try {
////    			mService.shiftAPos((long) shiftdist);
////    			setAPosTimeText (mService.getAPos());
////    			if (checkmodeon == true){
////    				mService.seek(mService.getAPos());
////    			}
////    			} catch (RemoteException ex) {
////    			}
//    		}
//    		public void onABPauseSet(long abpause) {
//    			try {
//    				mService.setABPause(mPreferences.getLong("abpause", 0));
//    			} catch (RemoteException ex) {
//    			}
//    		}
//    		
//    };
	
}