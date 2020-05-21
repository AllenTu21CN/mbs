package com.sanbu.ui;

import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;

public class KeyHoldingHelper {

    public interface Receiver {
        void onKeyDown(int keyCode, KeyEvent event);
    }

    private Receiver mReceiver;
    private Handler mHandler;
    private Runnable mReSend = new Runnable() {
        @Override
        public void run() {
            KeyHoldingHelper.this.sendLoop();
        }
    };

    private boolean mOnKeyHolding = false;
    private int mLastKeyCode = -1;
    private KeyEvent mLastEvent;

    public KeyHoldingHelper(Handler mainHandler, Receiver receiver) {
        mReceiver = receiver;
        mHandler = mainHandler;
    }

    public void release() {
        if (mHandler != null) {
            mHandler.removeCallbacks(mReSend);
            mReceiver = null;
            mHandler = null;
            mOnKeyHolding = false;
            mLastKeyCode = -1;
            mLastEvent = null;
        }
    }

    public void onKeyDown(int keyCode, KeyEvent event) {
        if (mOnKeyHolding)
            return;

        mOnKeyHolding = true;
        mLastKeyCode = keyCode;
        mLastEvent = event;
        mHandler.postDelayed(mReSend, 500);
    }

    public void onKeyUp(int keyCode, KeyEvent event) {
        mOnKeyHolding = false;
        mLastKeyCode = -1;
        mLastEvent = null;
        mHandler.removeCallbacksAndMessages(null);
    }

    private void sendLoop() {
        if (mLastKeyCode >= 0 && mLastEvent != null && mReceiver != null) {
            mReceiver.onKeyDown(mLastKeyCode, mLastEvent);
            mHandler.postDelayed(mReSend, 500);
        }
    }
}
