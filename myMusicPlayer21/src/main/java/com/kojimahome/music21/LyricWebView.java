package com.kojimahome.music21;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.WebView;

public class LyricWebView extends WebView {
	private static final String TAG = "LyricWebView";
//	private static float initialScrollRatio;
	private static int initialScrollPos;
	private static Context mContext;

	public LyricWebView(Context context) {
		super(context);
		mContext = context;
	}
	
	public LyricWebView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
	}
	
	public LyricWebView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
	}
	
	@Override
	public void onScrollChanged(int l, int t, int oldl, int oldt) {
		super.onScrollChanged(l, t, oldl, oldt);
		MusicUtils.setIntPref(mContext, LyricActivity.SCROLL_POSITION, this.getScrollY());
    	MusicUtils.setIntPref(mContext, LyricActivity.CUR_SCALE, (int) (100*this.getScale()));
//    	MusicUtils.setStringPref(mContext, ABDbAdapter.KEY_MUSICTITLE, title_intent);
//    	MusicUtils.setLongPref(mContext, ABDbAdapter.KEY_MUSICDURATION, duration_intent);
	}
	
//	public LyricWebView(Context context, AttributeSet attrs, int defStyle,
//			boolean privateBrowsing) {
//		super(context, attrs, defStyle, privateBrowsing);
//		// TODO Auto-generated constructor stub
//	}	
//	public void setInitialScrollRatio (int initialratio) {
//		initialScrollPos = initialratio;
//	}
//	
//	@Override
//	protected void onLayout(boolean changed, int l, int t, int r, int b) {
//		 // set initial scroll to
//		scrollTo(0, (int)(initialScrollPos*3));
//		super.onLayout(changed, l, t, r, b);
//		Log.i(TAG,"initialScrollPos onLayout:" + (int)(initialScrollPos*getScale()));
//		scrollTo(0, (int)(initialScrollPos*3));
//		 super.onLayout(changed, l, t, r, b);
//		 }
//	@Override
//	protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
//		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//		Log.i(TAG,"ContentHeight onMeasure:" + getContentHeight());
//		scrollTo(0, (int) (((float)getContentHeight())*initialScrollRatio));
//	}

}
