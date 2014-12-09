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

import java.text.DecimalFormat;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import com.kojimahome.music21.NumberPicker;

import com.kojimahome.music21.R;

/**
 * A dialog that prompts the user for the message deletion limits.
 */
public class NumberPickerDialog extends AlertDialog implements OnClickListener {
    private long mInitialNumber;

    private static final String NUMBER = "number";
    private static final String SUB_NUMBER = "subnumber";

    /**
     * The callback interface used to indicate the user is done filling in
     * the time (they clicked on the 'Set' button).
     */
    public interface OnNumberSetListener {

        /**
         * @param number The number that was set.
         */
        void onNumberSet(int number);
    }

    private final NonWrapNumberPicker mSecPicker;
    private final NonWrapNumberPicker mSubSecPicker;
    private final OnNumberSetListener mCallback;

    /**
     * @param context Parent.
     * @param callBack How parent is notified.
     * @param number The initial number.
     */
    public NumberPickerDialog(Context context,
            OnNumberSetListener callBack,
            int number,
            int rangeMin,
            int rangeMax,
            int title) {
        this(context, 0x7f090001,
                callBack, number, rangeMin, rangeMax, title);
    }
    

    /**
     * @param context Parent.
     * @param theme the theme to apply to this dialog
     * @param callBack How parent is notified.
     * @param number The initial number.
     */
    public NumberPickerDialog(Context context,
            int theme,
            OnNumberSetListener callBack,
            long number,
            long rangeMin,
            long rangeMax,
            int title) {
    	super(context);
//        super(context, theme);
        mCallback = callBack;
        mInitialNumber = number;

        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(1);
        df.setMinimumFractionDigits(1);
        setTitle(df.format(((float) mInitialNumber)/1000));
        setIcon(R.drawable.ic_dialog_time);

        setButton(context.getText(android.R.string.ok), this);
        setButton2(context.getText(android.R.string.cancel), (OnClickListener) null);

        LayoutInflater inflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.number_picker_dialog, null);
        setView(view);
        mSecPicker = (NonWrapNumberPicker) view.findViewById(R.id.sec_picker);

        // initialize state
        mSecPicker.setRange((int) rangeMin/1000, (int) rangeMax/1000);
        mSecPicker.setCurrent((int) number / 1000);
        mSecPicker.setSpeed(200);   
        
        mSubSecPicker = (NonWrapNumberPicker) view.findViewById(R.id.sub_sec_picker);

        // initialize state
        mSubSecPicker.setRange((int) 0, (int) 9);
        mSubSecPicker.setCurrent((int) (number % 1000)/100);
        mSubSecPicker.setSpeed(200);   
        
    }

    public void onClick(DialogInterface dialog, int which) {
        if (mCallback != null) {
            mSecPicker.clearFocus();
            mCallback.onNumberSet((mSecPicker.getCurrent()*1000)+(mSubSecPicker.getCurrent()*100));
        }
    }

    @Override
    public Bundle onSaveInstanceState() {
        Bundle state = super.onSaveInstanceState();
        state.putInt(NUMBER, mSecPicker.getCurrent());
        state.putInt(SUB_NUMBER, mSubSecPicker.getCurrent());
        return state;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        int number = savedInstanceState.getInt(NUMBER);
        mSecPicker.setCurrent(number);
        int subnumber = savedInstanceState.getInt(SUB_NUMBER);
        mSubSecPicker.setCurrent(subnumber);
    }

    public static class NonWrapNumberPicker extends NumberPicker {

        public NonWrapNumberPicker(Context context) {
            this(context, null);
        }

        public NonWrapNumberPicker(Context context, AttributeSet attrs) {
            this(context, attrs, 0);
        }

        @SuppressWarnings({"UnusedDeclaration"})
        public NonWrapNumberPicker(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs);
        }

        @Override
        protected void changeCurrent(int current) {
            // Don't wrap. Pin instead.
            if (current > getEndRange()) {
                current = getBeginRange();
            } else if (current < getBeginRange()) {
                current = getEndRange();
            }
            super.changeCurrent(current);
        }

    }

}
