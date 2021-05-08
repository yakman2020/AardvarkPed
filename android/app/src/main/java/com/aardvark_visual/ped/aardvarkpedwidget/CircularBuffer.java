
/*
 *  Copyright (c) 2014-2015 Aardvark Visual Software Inc,
 *
 *  All Rights Reserved.
 *
 */

package com.aardvark_visual.ped.aardvarkped;

class CircularBuffer<T> {
     public final T InvalidResult;
     public CircularBuffer(int size, T invalid) {
               mSize  = size;
               mArray = (T[])new Object[size];
               mNumSamples = 0;
               mBufTailIdx = 0;
               InvalidResult = invalid;
          };
 
     public
     T elementAt(int idx) {
              int i = idx%mSize;
              while (i < 0) i += mSize; // Mod operation leaves negative negative

              return mArray[i];
         };
     
     public
     void add(T value) {
              mArray[mBufTailIdx++] = value;
              mBufTailIdx %= mSize;
              mNumSamples++;
              if (mNumSamples > mSize) {
                  mNumSamples = mSize;
              }
         };
          
     public
     T back() {
             if (mNumSamples == 0) {
                 return InvalidResult;
             }
             return mArray[mBufTailIdx-1];
         };
          
     public
     int first() {
             if (mNumSamples < mSize) {
                 return 0;
             }
             return mBufTailIdx;
        };

     public
     int last() {
             if (mNumSamples == 0 ) {
                return -1; // Invalid index
             }
             if (mBufTailIdx == 0 ) {
                 return mNumSamples-1; // wrap around cheaply. 
                                       // If mBufTailIdx == 0 and mNumSamples != 0, 
                                       // then the buffer is fully primed.
             }
             return mBufTailIdx-1;
        };

     public
     int size() {
             return mNumSamples;
        };

     public
     int maxsize() {
             return mSize;
        };

     public
     void clear() {
            mNumSamples = 0;
            fill(InvalidResult);
          
        };

     public
     void fill(final T val) {
            for (int i = 0; i < mSize; i++ ) {
                mArray[i] = val;
            }
        };


   protected T[] mArray;
   protected int mSize;
   protected int mNumSamples;

   protected int mBufTailIdx;
};


