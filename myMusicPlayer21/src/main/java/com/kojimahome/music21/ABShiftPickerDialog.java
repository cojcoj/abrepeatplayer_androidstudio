package com.kojimahome.music21;

import java.text.DecimalFormat;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.kojimahome.music21.NumberPicker;

import android.widget.Button;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.kojimahome.music21.R;

public class ABShiftPickerDialog extends AlertDialog implements OnClickListener {

    /**
     * The callback interface used to indicate the user is done filling in
     * the time (they clicked on the 'Set' button).
     */
    public interface OnShiftDistSetListener {

        /**
         * @param number The number that was set.
         */
        void onShiftDistSet(long number, boolean checkmodeon);
        void onABPauseSet(long abpause);
    }

    private final OnShiftDistSetListener mCallback;
//    private boolean mCheckModeOn = false;
    private final Button mCheckModeButton;
    private final Button mJumpZeroButton;
    private final Button mLeftMostButton;
    private final Button mLeftMidButton;
    private final Button mLeftButton;
    private final Button mRightButton;
    private final Button mRightMidButton;
    private final Button mRightMostButton;
    
//    private final Button mSpaceHolder1Button;
//    private final Button mSpaceHolder2Button;
    
    private SharedPreferences mPreferences;
    private Context mContext;
    private Boolean mPauseSetting;

    /**
     * @param context Parent.
     * @param callBack How parent is notified.
     * @param number The initial number.
     */
    public ABShiftPickerDialog(Context context,
            OnShiftDistSetListener callBack,
            int number,
            int icon) {
//        this(context, 0x7f090001,
        		this(context, false,
        callBack, number, icon, false);
    }

    public ABShiftPickerDialog(Context context,
            OnShiftDistSetListener callBack,
            int number,
            int icon, boolean pauseSetting) {
//        this(context, 0x7f090001,
        		this(context, false,
        callBack, number, icon, pauseSetting);
    }
    public ABShiftPickerDialog(Context context,
    		boolean transparent,
            OnShiftDistSetListener callBack,
            int number,
            int icon) {
//        this(context, 0x7f090001,
        		this(context, transparent,
        callBack, number, icon, false);
    }
    
    /**
     * @param context Parent.
     * @param theme the theme to apply to this dialog
     * @param callBack How parent is notified.
     * @param number The initial number.
     */
    public ABShiftPickerDialog(Context context,
//            int theme,
    		boolean transparent,
            OnShiftDistSetListener callBack,
            int title,
            int icon, boolean pauseSetting) {
    	super(context);
    	if (transparent) {
    		context.setTheme(R.style.TransparentTheme);
    	}
    	mContext = context;
    	mPauseSetting = pauseSetting;
        mCallback = callBack;
//        android.R.style.them
        mPreferences = context.getSharedPreferences("ABRepeat", Context.MODE_PRIVATE);

        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(1);
        df.setMinimumFractionDigits(1);
        setTitle(title);
        setIcon(icon);
        
        setButton(context.getText(android.R.string.ok), (OnClickListener) null);
//        setButton2("Check Mode", (OnClickListener) null);
//        setButton(-3, "-3", (DialogInterface.OnClickListener) null);
//        setButton(-2, "-2", (DialogInterface.OnClickListener) null);
//        setButton(-1, "-1", (DialogInterface.OnClickListener) null);
//        
//        
//        mCheckModeButton = getButton (-2);
//        mCheckModeButton.setBackgroundResource(android.R.color.background_light);

        LayoutInflater inflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = (transparent) ? inflater.inflate(R.layout.abshift_picker_dialog_tr, null)
        							: inflater.inflate(R.layout.abshift_picker_dialog, null);
        setView(view);

        mLeftMostButton = (Button) view.findViewById(R.id.leftmostbutton);
        mLeftMidButton = (Button) view.findViewById(R.id.leftmidbutton);
        mLeftButton = (Button) view.findViewById(R.id.leftbutton);
        mRightButton = (Button) view.findViewById(R.id.rightbutton);
        mRightMidButton = (Button) view.findViewById(R.id.rightmidbutton);
        mRightMostButton = (Button) view.findViewById(R.id.rightmostbutton);
        
        mCheckModeButton = (Button) view.findViewById(R.id.checkmodebutton);
        mJumpZeroButton = (Button) view.findViewById(R.id.jumpzerobutton);
        
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
        if (getCheckMode()) {
        	mCheckModeButton.setCompoundDrawablesWithIntrinsicBounds(null, 
            		context.getResources().getDrawable(R.drawable.checkmode_on), null, null);
        } else {
        	mCheckModeButton.setCompoundDrawablesWithIntrinsicBounds(null, 
            		context.getResources().getDrawable(R.drawable.checkmode_off), null, null);
        }
        
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
//        mJumpZeroButton.setText((mPauseSetting) ? 
//        		df.format(((float) mPreferences.getLong("abpause", 0))/1000) :"0");
        setCheckModeandJumpZeroButton();
        
        mLeftMostButton.setVisibility(View.VISIBLE);
        mLeftMidButton.setVisibility(View.VISIBLE);
        mLeftButton.setVisibility(View.VISIBLE);
        mRightButton.setVisibility(View.VISIBLE);
        mRightMidButton.setVisibility(View.VISIBLE);
        mRightMostButton.setVisibility(View.VISIBLE);
        
//        mCheckModeButton.setVisibility(View.VISIBLE);
//        mJumpZeroButton.setVisibility((mPauseSetting||getCheckMode()) ? View.VISIBLE : View.INVISIBLE);
        
        mLeftMostButton.setOnClickListener(mLeftMostClickListener);
        mLeftMidButton.setOnClickListener(mLeftMidClickListener);
        mLeftButton.setOnClickListener(mLeftClickListener);
        mRightButton.setOnClickListener(mRightClickListener);
        mRightMidButton.setOnClickListener(mRightMidClickListener);
        mRightMostButton.setOnClickListener(mRightMostClickListener);
        
        mCheckModeButton.setOnClickListener(mCheckModeClickListener);
        mJumpZeroButton.setOnClickListener(mJumpZeroClickListener);

        mLeftMostButton.setLongClickable(true);
        mLeftMidButton.setLongClickable(true);
        mLeftButton.setLongClickable(true);
        mRightButton.setLongClickable(true);
        mRightMidButton.setLongClickable(true);
        mRightMostButton.setLongClickable(true);
        
        mLeftMostButton.setOnLongClickListener(mLeftMostLongListener);
        mLeftMidButton.setOnLongClickListener(mLeftMidLongListener);
        mLeftButton.setOnLongClickListener(mLeftLongListener);
        mRightButton.setOnLongClickListener(mRightLongListener);
        mRightMidButton.setOnLongClickListener(mRightMidLongListener);
        mRightMostButton.setOnLongClickListener(mRightMostLongListener);
        
    }

public void refreshButtons() {
	DecimalFormat df = new DecimalFormat();
    df.setMaximumFractionDigits(1);
    df.setMinimumFractionDigits(1);
	
    mJumpZeroButton.setText(df.format(((float) mPreferences.getLong("abpause", 0))/1000));
    mLeftMostButton.setText(df.format(((float) mPreferences.getLong("abshiftone", 2000))/1000));
    mLeftMidButton.setText(df.format(((float) mPreferences.getLong("abshifttwo", 1000))/1000));
    mLeftButton.setText(df.format(((float) mPreferences.getLong("abshiftthree", 200))/1000));
    mRightButton.setText(df.format(((float) mPreferences.getLong("abshiftthree", 200))/1000));
    mRightMidButton.setText(df.format(((float) mPreferences.getLong("abshifttwo", 1000))/1000));
    mRightMostButton.setText(df.format(((float) mPreferences.getLong("abshiftone", 2000))/1000));
}
    
public void onClick(DialogInterface dialog, int which) {
        if (mCallback != null) {
//            mSecPicker.clearFocus();
//            mCallback.onNumberSet((mSecPicker.getCurrent()*1000)+(mSubSecPicker.getCurrent()*100));
        }
    }
    
	
private View.OnLongClickListener mLeftMostLongListener = new View.OnLongClickListener() {
	
	@Override
	public boolean onLongClick(View v) {
			long shiftdist = mPreferences.getLong("abshiftone", 2000);
			new NumberPickerDialog (v.getContext(),
					mABShiftDistSetOneListener,
					(int) shiftdist,
					0,
					99900,
					R.string.forward_200msec).show();
		return true;
	}
};

private View.OnLongClickListener mLeftMidLongListener = new View.OnLongClickListener() {
	
	@Override
	public boolean onLongClick(View v) {
			long shiftdist = mPreferences.getLong("abshifttwo", 1000);
			new NumberPickerDialog (v.getContext(),
					mABShiftDistSetTwoListener,
					(int) shiftdist,
					0,
					99900,
					R.string.forward_200msec).show();
		return true;
	}
};

private View.OnLongClickListener mLeftLongListener = new View.OnLongClickListener() {
	
	@Override
	public boolean onLongClick(View v) {
			long shiftdist = mPreferences.getLong("abshifthree", 200);
			new NumberPickerDialog (v.getContext(),
					mABShiftDistSetThreeListener,
					(int) shiftdist,
					0,
					99900,
					R.string.forward_200msec).show();
		return true;
	}
};

private View.OnLongClickListener mRightLongListener = new View.OnLongClickListener() {
	
	@Override
	public boolean onLongClick(View v) {
			long shiftdist = mPreferences.getLong("abshiftthree", 200);
			new NumberPickerDialog (v.getContext(),
					mABShiftDistSetThreeListener,
					(int) shiftdist,
					0,
					99900,
					R.string.forward_200msec).show();
		return true;
	}
};

private View.OnLongClickListener mRightMidLongListener = new View.OnLongClickListener() {
	
	@Override
	public boolean onLongClick(View v) {
			long shiftdist = mPreferences.getLong("abshifttwo", 1000);
			new NumberPickerDialog (v.getContext(),
					mABShiftDistSetTwoListener,
					(int) shiftdist,
					0,
					99900,
					R.string.forward_200msec).show();
		return true;
	}
};

private View.OnLongClickListener mRightMostLongListener = new View.OnLongClickListener() {
	
	@Override
	public boolean onLongClick(View v) {
			long shiftdist = mPreferences.getLong("abshiftone", 2000);
			new NumberPickerDialog (v.getContext(),
					mABShiftDistSetOneListener,
					(int) shiftdist,
					0,
					99900,
					R.string.forward_200msec).show();
		return true;
	}
};

NumberPickerDialog.OnNumberSetListener mABPauseDistSetListener =
	new NumberPickerDialog.OnNumberSetListener() {	
		@Override
		public void onNumberSet(int shiftdist) {
			Editor ed = mPreferences.edit();
        	ed.putLong("abpause", (long) shiftdist);
        	ed.commit();
        	mCallback.onABPauseSet(shiftdist);
        	refreshButtons();
		}
	};

NumberPickerDialog.OnNumberSetListener mABShiftDistSetOneListener =
	new NumberPickerDialog.OnNumberSetListener() {	
		@Override
		public void onNumberSet(int shiftdist) {
			Editor ed = mPreferences.edit();
        	ed.putLong("abshiftone", (long) shiftdist);
        	ed.commit();
        	refreshButtons();
		}
	};
	
NumberPickerDialog.OnNumberSetListener mABShiftDistSetTwoListener =
	new NumberPickerDialog.OnNumberSetListener() {	
		@Override
		public void onNumberSet(int shiftdist) {
			Editor ed = mPreferences.edit();
        	ed.putLong("abshifttwo", (long) shiftdist);
        	ed.commit();
        	refreshButtons();
		}
	};

NumberPickerDialog.OnNumberSetListener mABShiftDistSetThreeListener =
	new NumberPickerDialog.OnNumberSetListener() {	
		@Override
		public void onNumberSet(int shiftdist) {
			Editor ed = mPreferences.edit();
        	ed.putLong("abshiftthree", (long) shiftdist);
        	ed.commit();
        	refreshButtons();
		}
	};
		
private View.OnClickListener mLeftMostClickListener = new View.OnClickListener() {
				
		@Override
		public void onClick(View v) {
			mCallback.onShiftDistSet(- mPreferences.getLong("abshiftone", 2000), getCheckMode());
			if (getCheckMode() == false) {
				 cancel();
				}
		}
	};
			
private View.OnClickListener mLeftMidClickListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			mCallback.onShiftDistSet(- mPreferences.getLong("abshifttwo", 1000), getCheckMode());
			if (getCheckMode() == false) {
				 cancel();
				}
		}
	};
	
private View.OnClickListener mLeftClickListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			mCallback.onShiftDistSet(- mPreferences.getLong("abshiftthree", 200), getCheckMode());
			if (getCheckMode() == false) {
				 cancel();
				}
		}
	};
	
private View.OnClickListener mRightClickListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			mCallback.onShiftDistSet( mPreferences.getLong("abshiftthree", 200), getCheckMode());
			if (getCheckMode() == false) {
				 cancel();
				}
		}
	};
	
private View.OnClickListener mRightMidClickListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			mCallback.onShiftDistSet( mPreferences.getLong("abshifttwo", 1000), getCheckMode());
			if (getCheckMode() == false) {
				 cancel();
				}
		}
	};
	
private View.OnClickListener mRightMostClickListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			mCallback.onShiftDistSet( mPreferences.getLong("abshiftone", 2000), getCheckMode());
			if (getCheckMode() == false) {
			 cancel();
			}
		}
	};
	
private View.OnClickListener mJumpZeroClickListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			if (mPauseSetting&&!getCheckMode()) {
				long pausedist = mPreferences.getLong("abpause", 0);
				new NumberPickerDialog (v.getContext(),
						mABPauseDistSetListener,
						(int) pausedist,
						0,
						99900,
						R.string.forward_200msec).show();
			} else {
				mCallback.onShiftDistSet( 0, getCheckMode());
				if (getCheckMode() == false) {
				 cancel();
				}
			}
		}
	};
	
private View.OnClickListener mCheckModeClickListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
//			DecimalFormat df = new DecimalFormat();
//	        df.setMaximumFractionDigits(1);
//	        df.setMinimumFractionDigits(1);
//	        
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
	
    @Override
    public Bundle onSaveInstanceState() {
        Bundle state = super.onSaveInstanceState();
//        state.putInt(NUMBER, mSecPicker.getCurrent());
//        state.putInt(SUB_NUMBER, mSubSecPicker.getCurrent());
        return state;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

    }
    
    private void setCheckModeandJumpZeroButton() {
		DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(1);
        df.setMinimumFractionDigits(1);
        
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
//				mJumpZeroButton.setText("0");
				mJumpZeroButton.setVisibility(View.VISIBLE);
			}
//			setButton(mContext.getText(android.R.string.ok), (OnClickListener) null);
		}
    }
    
    private boolean getCheckMode() {
    	return MusicUtils.getBooleanPref(mContext, MusicUtils.Defs.CHECK_MODE, false);
    }
    
    private void setCheckMode(boolean checkmode) {
    	MusicUtils.setBooleanPref(mContext, MusicUtils.Defs.CHECK_MODE, checkmode);
    }

}
