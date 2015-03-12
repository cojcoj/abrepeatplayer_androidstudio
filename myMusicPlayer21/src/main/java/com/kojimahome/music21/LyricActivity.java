package com.kojimahome.music21;

import com.mpatric.mp3agic.*;



import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Picture;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView.PictureListener;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

public class LyricActivity extends Activity {
	public static final String LOGTAG = "LyricActivity";
	public static final String LYRIC = "lyric";
//	public static final String POSX = "posx";
	public static final String POSY = "posy";
//	public static final String CUR_FONT_SIZE = "fontsize";
	public static final String CUR_SCALE = "currentscale";
//	public static final String SCROLL_POSITION_RATIO = "scroll_position_ratio";
	public static final String SCROLL_POSITION = "scroll_position";
//	private static final float FONT_ENLARGE_FACTOR = LyricTextView.FONT_ENLARGE_FACTOR;
	private static final int ENLARGE_FONT = 0;
	private static final int SHRINK_FONT = 1;
	private static final int FONT_SIZE = 2;
	private static final int INITIAL_FONT_SIZE_DIP = 17;
	private static final int MAX_FONT_SIZE = 140;
	private static final int MIN_FONT_SIZE = 8;
//	private static ScrollView mScrollView;
//	public static int mPosX;
//	public static float mScrollRatio;
	public static int mScrollPos;
//	public static int mPosY;
	private static Long duration_intent;
	private static String title_intent;
	
	private static LyricWebView textv;
	private static Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lyric_layout);
        
        mContext = this;
        
        textv = (LyricWebView) findViewById(R.id.tv);
        textv.getSettings().setJavaScriptEnabled(true);
		textv.getSettings().setAllowFileAccess(true);
//		textv.getSettings().setPluginsEnabled(true);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) textv.getSettings().setPluginState(PluginState.ON);
		textv.getSettings().setSupportZoom(true);
		textv.getSettings().setBuiltInZoomControls(true);
		textv.setPictureListener(new WebView.PictureListener() {
			@Override
			public void onNewPicture(WebView view, Picture picture) {
				// TODO Auto-generated method stub
				textv.scrollTo(0, MusicUtils.getIntPref(mContext, SCROLL_POSITION, 0));
			}
	    });
		      
        Intent intent = getIntent();
        displayLyric(this, intent);
    }
    
    protected void onSaveInstanceState(Bundle outState) {
    	    super.onSaveInstanceState(outState);
//    	    outState.putIntArray("ARTICLE_SCROLL_POSITION",
//    	            new int[]{ mScrollView.getScrollX(), mScrollView.getScrollY()});
//    	    Log.i(LOGTAG, "onSaveScrollY:" + (float)(mScrollView.getScrollY()));
//    	    Log.i(LOGTAG, "onSaveHeight:" + (float)(textv.getHeight()));
//    	    Log.i(LOGTAG, "onSaveRatio:" + (float)(mScrollView.getScrollY()/(float)textv.getHeight()));
//    	    Log.i(LOGTAG, "textv.getScrollY() onSaveInstanceState:"+textv.getScrollY());
//    	    outState.putFloat(SCROLL_POSITION_RATIO,(float)(textv.getScrollY()/(float)textv.getContentHeight()));
    	    outState.putInt(SCROLL_POSITION,textv.getScrollY());
//    	    outState.putFloat(CUR_FONT_SIZE, textv.getTextSize());
    	    outState.putInt(CUR_SCALE, (int) (100*textv.getScale()));
    	}
    
    @Override
    public void onStart() {
    	super.onStart();
    	IntentFilter f = new IntentFilter();
//        f.addAction(MediaPlaybackService.PLAYSTATE_CHANGED);
        f.addAction(MediaPlaybackService.META_CHANGED);
//        f.addAction(MediaPlaybackService.PLAYBACK_COMPLETE);
        registerReceiver(mStatusListener, new IntentFilter(f));
    }

    
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
//        float initial_font_size = (float) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
//        		INITIAL_FONT_SIZE_DIP, getResources().getDisplayMetrics());
        int initial_font_size = 0;
//        textv.setTextSize(TypedValue.COMPLEX_UNIT_PX, savedInstanceState.getFloat(CUR_FONT_SIZE, initial_font_size));
        textv.setInitialScale(savedInstanceState.getInt(CUR_SCALE, initial_font_size));
//        final int position = (int) (((float)textv.getHeight()) * savedInstanceState.getFloat(SCROLL_POSITION_RATIO));
//        if(position != null)
//        	mScrollRatio = savedInstanceState.getFloat(SCROLL_POSITION_RATIO);
        	mScrollPos = savedInstanceState.getInt(SCROLL_POSITION);
        	MusicUtils.setIntPref(this, SCROLL_POSITION, mScrollPos);
//            textv.post(new Runnable() {
//                public void run() {
////                    textv.scrollTo(0, (int) (((float)textv.getContentHeight()) * mScrollRatio));
//                    textv.scrollTo(0, mScrollPos);
//                    Log.i(LOGTAG, "mScrollPos onRestoreInstanceState:" + mScrollPos);
//                }
//            });
    }
    
    @Override
    protected void onStop () {
    	unregisterReceiver(mStatusListener);
    	
//    	Log.i(LOGTAG, "textv.getScrollY() onStop:"+textv.getScrollY());
    	MusicUtils.setIntPref(this, SCROLL_POSITION, textv.getScrollY());
    	MusicUtils.setIntPref(this, CUR_SCALE, (int) (100*textv.getScale()));
    	MusicUtils.setStringPref(this, ABDbAdapter.KEY_MUSICTITLE, title_intent);
    	MusicUtils.setLongPref(this, ABDbAdapter.KEY_MUSICDURATION, duration_intent);
    	super.onStop();
    }
    
    public static String getExtension(String s)
    {
    String ext = null;
    int i = s.lastIndexOf('.');
    if (i > 0 && i < s.length() - 1)
    ext = s.substring(i+1).toLowerCase();
    if(ext == null)
    return "";
    return ext;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//    	menu.add(0, ENLARGE_FONT, 0, R.string.font_enlarge).setIcon(R.drawable.ic_font_enlarge);
//    	menu.add(0, SHRINK_FONT, 0, R.string.font_shrink).setIcon(R.drawable.ic_font_shrink);
//    	menu.add(0, FONT_SIZE, 0, "Font Size");
    	return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
//    	float old_size = textv.getScale();
//    	if ((old_size * FONT_ENLARGE_FACTOR) > MAX_FONT_SIZE) {
//    		menu.getItem(ENLARGE_FONT).setEnabled(false);
//    	} else {
//    		menu.getItem(ENLARGE_FONT).setEnabled(true);
//    	}
//    	if ((old_size / FONT_ENLARGE_FACTOR) < MIN_FONT_SIZE) {
//    		menu.getItem(SHRINK_FONT).setEnabled(false);
//    	} else {
//    		menu.getItem(SHRINK_FONT).setEnabled(true);
//    	}
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	float old_size = textv.getScale();
    	float new_size;
    	switch (item.getItemId()) {
    	case ENLARGE_FONT:
//    		new_size = old_size * FONT_ENLARGE_FACTOR;
//    		if (new_size < MAX_FONT_SIZE) {
//    			mScrollRatio = (float)(mScrollView.getScrollY()/(float)textv.getHeight());
//    			textv.setTextSize(TypedValue.COMPLEX_UNIT_PX, new_size);
////    			mScrollView.scrollTo(0, (int) (((float)textv.getHeight()) * mScrollRatio));
//    			mScrollView.post(new Runnable() {
//                    public void run() {
//                        mScrollView.scrollTo(0, (int) (((float)textv.getHeight()) * mScrollRatio));
//                    }
//                });
//    		}
    		break;
    	case SHRINK_FONT:
//    		new_size = old_size / FONT_ENLARGE_FACTOR;
//    		if (new_size > MIN_FONT_SIZE) {
//    			mScrollRatio = (float)(mScrollView.getScrollY()/(float)textv.getHeight());
//    			textv.setTextSize(TypedValue.COMPLEX_UNIT_PX, new_size);
////    			mScrollView.scrollTo(0, (int) (((float)textv.getHeight()) * mScrollRatio));
//    			mScrollView.post(new Runnable() {
//                    public void run() {
//                        mScrollView.scrollTo(0, (int) (((float)textv.getHeight()) * mScrollRatio));
//                    }
//                });
//    		}
    		break;
    	case FONT_SIZE:
    		break;
    	}	
    	return true;
    }
    
    private BroadcastReceiver mStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
//        	Log.i(LOGTAG, "textv.getScrollY() onReceive:"+textv.getScrollY());
//        	MusicUtils.setFloatPref(context, SCROLL_POSITION_RATIO, (float)(textv.getScrollY()/(float)textv.getContentHeight()));
        	MusicUtils.setIntPref(context, SCROLL_POSITION, textv.getScrollY());
        	MusicUtils.setIntPref(context, CUR_SCALE, (int) (100*textv.getScale()));
        	MusicUtils.setStringPref(context, ABDbAdapter.KEY_MUSICTITLE, title_intent);
        	MusicUtils.setLongPref(context, ABDbAdapter.KEY_MUSICDURATION, duration_intent);
        	displayLyric(context, intent);
        }
    };
    
    private String getLyric(String filename) {
    	String lyric1;
    	String ext = MusicUtils.getExtension(filename);
    	if ((ext != null) && (ext.equals("mp3"))) {
	    	try {
	            Mp3File mp3 = new Mp3File(filename, false);
	            ID3v2 tag = mp3.getId3v2Tag();
	            String lyric = tag.getUnsynchLyrics();
//	            lyric1 = lyric.replace("\r", "\n");
	            lyric1 = lyric.replace("\r", "<br>").replace("\n", "<br>");
	            } catch (Exception e) {
	            	Log.e("LyricActivity","Lyric Tag Error: " + e);
	            	lyric1 = getString(R.string.no_id3tag);
	            }
    	} else {
    		lyric1 = getString(R.string.no_id3tag);
    	}
    	return lyric1;
    }
    
    private void displayLyric(Context context, Intent intent) {
        String filename = intent.getStringExtra(ABDbAdapter.KEY_MUSIC_DATA);
        String lyric = "";
        if (filename != null) lyric = getLyric(filename);
        duration_intent = intent.getLongExtra(ABDbAdapter.KEY_MUSICDURATION, 0);
        title_intent = intent.getStringExtra(ABDbAdapter.KEY_MUSICTITLE);
        if (title_intent == null) title_intent = "";
        if (title_intent.equals("")) {
        	setTitle(getString(R.string.lyric));
        } else {
        	setTitle(title_intent);
        }
        Long duration_pref = MusicUtils.getLongPref(context, ABDbAdapter.KEY_MUSICDURATION, 0);
        String title_pref = MusicUtils.getStringPref(context, ABDbAdapter.KEY_MUSICTITLE, "");
//        textv.setText(lyric);
        final String mimeType = "text/html";
        final String encoding = "UTF-8";
        textv.loadDataWithBaseURL("", lyric, mimeType, encoding, "");
//        float initial_font_size = (float) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
//        		INITIAL_FONT_SIZE_DIP, getResources().getDisplayMetrics());
        int initial_font_size = 0;
        textv.setInitialScale(MusicUtils.getIntPref(context, CUR_SCALE, initial_font_size));
//        Log.i(LOGTAG, "PrefScale:" + MusicUtils.getIntPref(context, CUR_SCALE, initial_font_size));
//        mScrollRatio = MusicUtils.getFloatPref(context, SCROLL_POSITION_RATIO, 0);
        mScrollPos = MusicUtils.getIntPref(context, SCROLL_POSITION, 0);
//        Log.i(LOGTAG, "ScrollPosition:" + MusicUtils.getIntPref(context, SCROLL_POSITION, 0));
        if((duration_intent.equals(duration_pref))&&(title_intent.equals(title_pref))) {;
//        	textv.setInitialScrollRatio(mScrollPos);       	
        } else {
        	MusicUtils.setIntPref(this, SCROLL_POSITION, 0);
        }
    }


//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.activity_main, menu);
//        return true;
//    }
}