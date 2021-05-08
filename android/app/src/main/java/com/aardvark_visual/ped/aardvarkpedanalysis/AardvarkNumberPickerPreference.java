/*
 *  Copyright (c) 2014-2015 Aardvark Visual Software Inc,
 *
 *  All Rights Reserved.
 *
 *
 */

package com.aardvark_visual.ped.aardvarkpedanalysis;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.NumberPicker;

import com.aardvark_visual.ped.R;

public class AardvarkNumberPickerPreference extends DialogPreference {

    // Value types

    private static final int UNDEFINED = 0;
    private static final int INTEGER   = 1;
    private static final int BOOLEAN   = 2;
    private static final int STRING    = 3;
    private static final int FLOAT     = 4;

    private static final int  MIN_VALUE_IDX           = 0;
    private static final int  MAX_VALUE_IDX           = 9;
    private static final boolean WRAP_SELECTOR_WHEEL = false;
    private static final int VALUE_TYPE = UNDEFINED;

    private int mValueType = FLOAT;

    private String[]                mAvailableValueLabels = { "0.0", "1.0", "2.0", "3.0", "4.0", "5.0", "6.0", "7.0", "8.0"};
    private double[]                mAvailableFloatValues = { 0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
    private String[]                mAvailableStringValues = null;
    private int[]                   mAvailableIntegerValues = null;
    private boolean[]               mAvailableBooleanValues = null;

    private int                     mSelectedValueIdx = 4;
    private int                     mMinValueIdx = 0;
    private int                     mMaxValueIdx = 8;
    private final boolean           mWrapSelectorWheel;
    private NumberPicker            mNumberPicker;

    public AardvarkNumberPickerPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AardvarkNumberPickerPreference);
        String[] available_values = null;
        int resource_id = 0;


        mWrapSelectorWheel = a.getBoolean(R.styleable.AardvarkNumberPickerPreference_setWrapSelectorWheel, AardvarkNumberPickerPreference.WRAP_SELECTOR_WHEEL);
        mValueType   = a.getInt(R.styleable.AardvarkNumberPickerPreference_valueType, AardvarkNumberPickerPreference.VALUE_TYPE);

        resource_id = a.getResourceId(R.styleable.AardvarkNumberPickerPreference_valueLabels, 0);
        mAvailableValueLabels = a.getResources().getStringArray(resource_id);

        resource_id = a.getResourceId(R.styleable.AardvarkNumberPickerPreference_availableValues, 0);
        available_values = a.getResources().getStringArray(resource_id);

        this.setValues(mValueType, available_values);

        mMinValueIdx = 0;
        mMaxValueIdx = available_values.length-1;

        mMinValueIdx = a.getInt(R.styleable.AardvarkNumberPickerPreference_minValue, mMinValueIdx);
        mMaxValueIdx = a.getInt(R.styleable.AardvarkNumberPickerPreference_maxValue, mMaxValueIdx);

        a.recycle();
    }

    @Override
    protected void onSetInitialValue(final boolean restoreValue, final Object defaultValue) {

        if (restoreValue || defaultValue == null) {
            String valstr = this.getPersistedString(null);
            if (valstr != null) {
                mSelectedValueIdx = Integer.parseInt(valstr);
                if (mSelectedValueIdx > mAvailableValueLabels.length-1) {
                    mSelectedValueIdx = mAvailableValueLabels.length;
                }
            }
            else {
                mSelectedValueIdx = 0;
            }
        }
        else {
            String valstr = defaultValue.toString();
            mSelectedValueIdx = Integer.parseInt(valstr);
            if (mSelectedValueIdx > mAvailableValueLabels.length-1) {
                mSelectedValueIdx = mAvailableValueLabels.length;
            }
        }
        this.updateSummary();
    }

    @Override
    protected Object onGetDefaultValue(final TypedArray a, final int index) {
       TypedValue val = a.peekValue(index);

       // The only logical thing here is a value index, not a value 
       int aInt = a.getInt(index, 0);
        switch(val.type){
        case TypedValue.TYPE_FLOAT:
        case TypedValue.TYPE_FRACTION:
        case TypedValue.TYPE_NULL:
        case TypedValue.TYPE_REFERENCE:
        case TypedValue.TYPE_STRING:
        default:
            System.err.println("invalid value for default value");
            return 0;

        case TypedValue.TYPE_INT_BOOLEAN:
        case TypedValue.TYPE_INT_COLOR_ARGB4:
        case TypedValue.TYPE_INT_COLOR_ARGB8:
        case TypedValue.TYPE_INT_COLOR_RGB8:
        case TypedValue.TYPE_INT_DEC:
        case TypedValue.TYPE_INT_HEX:
        case TypedValue.TYPE_LAST_INT:
            mValueType = INTEGER;
            aInt = a.getInt(index, 0);
            return (Object)aInt;
        }
    }


    protected Object getValue() {
        switch(mValueType){
        case INTEGER:
             return (Object)mAvailableIntegerValues[mSelectedValueIdx];
    
        case BOOLEAN:
             return (Object)mAvailableBooleanValues[mSelectedValueIdx];

        case STRING:
             return (Object)mAvailableStringValues[mSelectedValueIdx];

        case FLOAT:
             return (Object)mAvailableFloatValues[mSelectedValueIdx];
             
        default:

             // 2DO: throw an exception here

             return (Object)(-1);
        }
    }

    public void setDisplayedValues (String[] displayedValues) {
        mAvailableValueLabels = displayedValues.clone();

    }


    public void setMaxValueIdx(int max) {
        mMaxValueIdx = max;
        if (max > mAvailableValueLabels.length-1) {
            mMaxValueIdx = mAvailableValueLabels.length-1;
        }
    }

    public void setMinValueIdx(int min) {
        mMinValueIdx = min;
        if (min < 0) {
            mMinValueIdx = 0;
        }
    }

    public void setValues(int valueType, String[] values) {
        
        mMinValueIdx = 0;
        mMaxValueIdx = values.length-1;

        switch(mValueType = valueType){
        case INTEGER:
             mAvailableIntegerValues = new int[values.length];
             for (int i = 0; i < values.length; i++ ) {
                 mAvailableIntegerValues[i] = Integer.parseInt(values[i]);
             }
             break;
    
        case BOOLEAN:
             mAvailableBooleanValues = new boolean[values.length];
             for (int i = 0; i < values.length; i++ ) {
                 mAvailableBooleanValues[i] = Boolean.parseBoolean(values[i]);
             }
             break;

        case STRING:
             mAvailableStringValues = values.clone();
             break;

        case FLOAT:
             mAvailableFloatValues = new double[values.length];
             for (int i = 0; i < values.length; i++ ) {
                 mAvailableFloatValues[i] = Double.parseDouble(values[i]);
             }
             break;
             
        default:

             // 2DO: throw an exception here

             break;
        }
    }


    @Override
    protected void onPrepareDialogBuilder(final Builder builder) {
        super.onPrepareDialogBuilder(builder);

        mNumberPicker = new NumberPicker(this.getContext());
        mNumberPicker.setMinValue(mMinValueIdx);
        mNumberPicker.setMaxValue(mMaxValueIdx);
        mNumberPicker.setValue(mSelectedValueIdx);
        mNumberPicker.setWrapSelectorWheel(mWrapSelectorWheel);
        mNumberPicker.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        mNumberPicker.setDisplayedValues( mAvailableValueLabels);
        mNumberPicker.setFocusable(false);
        mNumberPicker.setClickable(false);
        mNumberPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

        final LinearLayout linearLayout = new LinearLayout(this.getContext());
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        linearLayout.setGravity(Gravity.CENTER);
        linearLayout.addView(mNumberPicker);

        builder.setView(linearLayout);
    }

    @Override
    protected void onDialogClosed(final boolean positiveResult) {
        if (positiveResult && this.shouldPersist()) {
            mSelectedValueIdx = mNumberPicker.getValue();
            this.persistString(Integer.toString(mSelectedValueIdx));
            this.updateSummary();
            String value = null;
            switch(mValueType){
            case INTEGER:
                 value = Integer.toString(mAvailableIntegerValues[mSelectedValueIdx]);
                 break;
        
            case BOOLEAN:
                 value = Boolean.toString(mAvailableBooleanValues[mSelectedValueIdx]);
                 break;
    
            case STRING:
                 value = mAvailableStringValues[mSelectedValueIdx];
                 break;
    
            case FLOAT:
                 value = Double.toString(mAvailableFloatValues[mSelectedValueIdx]);
                 break;
                 
            default:
    
                 // 2DO: throw an exception here?
    
                 break;
            }
            callChangeListener(value);
        }
    }

    private void updateSummary() {
        super.setSummary(super.getTitle() + " " + mAvailableValueLabels[mSelectedValueIdx]);
    }
}


