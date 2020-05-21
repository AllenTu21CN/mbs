package com.sanbu.modules;

import android.app.Presentation;
import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.sanbu.board.Qualcomm;
import com.sanbu.board.Rockchip;
import com.sanbu.tools.LogUtil;
import com.sanbu.tools.R;

public class MultiScreenPresentation extends Presentation {

    private static final String TAG = MultiScreenPresentation.class.getSimpleName();

    public interface Callback extends SurfaceHolder.Callback {
        void onDisplayRemoved(int displayId);
    }

    private static Handler gHandler;
    private static Context gContext;
    private static Callback gCallback;
    private static MultiScreenPresentation gCurrent = null;

    private static DisplayManager.DisplayListener gListener = new DisplayManager.DisplayListener() {
        @Override
        public void onDisplayAdded(int displayId) {
            LogUtil.d(TAG, "onDisplayAdded: #" + displayId);
            if (!isMultiple()) {
                if (gHandler != null)
                    gHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            checkAndCreate((DisplayManager) gContext.getSystemService(Context.DISPLAY_SERVICE), gContext);
                        }
                    }, 1000);
            } else {
                LogUtil.w(TAG, "!!! logical error: got another display");
            }
        }

        @Override
        public void onDisplayRemoved(int displayId) {
            LogUtil.d(TAG, "onDisplayRemoved: #" + displayId);
            if (gCallback != null)
                gCallback.onDisplayRemoved(displayId);
            if (gHandler != null)
                gHandler.removeCallbacksAndMessages(null);
        }

        @Override
        public void onDisplayChanged(int displayId) {
            LogUtil.d(TAG, "onDisplayChanged: #" + displayId);
        }
    };

    public static boolean isMultiple() {
        synchronized (MultiScreenPresentation.class) {
            return gCurrent != null;
        }
    }

    public static void init(Context context, Callback callback) {
        gHandler = new Handler();
        gContext = context;
        gCallback = callback;
        DisplayManager displayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);

        // check init the extra display
        checkAndCreate(displayManager, context);

        // register the listener
        displayManager.registerDisplayListener(gListener, null);
    }

    public static void release() {
        synchronized (MultiScreenPresentation.class) {
            if (gContext == null)
                return;

            // unregister the listener
            DisplayManager displayManager = (DisplayManager) gContext.getSystemService(Context.DISPLAY_SERVICE);
            displayManager.unregisterDisplayListener(gListener);

            // dismiss the extra display
            if (gCurrent != null)
                gCurrent.dismiss();

            gHandler.removeCallbacksAndMessages(null);
            gHandler = null;
            gContext = null;
            gCallback = null;
        }
    }

    private static void checkAndCreate(DisplayManager displayManager, Context context) {
        Display[] plays = displayManager.getDisplays();
        if (plays.length >= 2) {
            MultiScreenPresentation differentDisplay = new MultiScreenPresentation(context, plays[1]);
            differentDisplay.show();
        }
    }

    private SurfaceView mSurfaceView;

    public MultiScreenPresentation(Context outerContext, Display display) {
        super(outerContext, display);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LogUtil.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.second_screen_view);
        mSurfaceView = findViewById(R.id.surfaceView);
        SurfaceHolder surfaceHolder = mSurfaceView.getHolder();
        surfaceHolder.setFormat(PixelFormat.TRANSPARENT);
        surfaceHolder.addCallback(gCallback);

        synchronized (MultiScreenPresentation.class) {
            gCurrent = this;
        }
    }

    @Override
    public void onDisplayRemoved() {
        LogUtil.d(TAG, "onDisplayRemoved");
        super.onDisplayRemoved();

        synchronized (MultiScreenPresentation.class) {
            gCurrent = null;
        }
    }

    @Override
    public void show() {
        /*
        if (Rockchip.is3BUEdition())
            getWindow().setType(WindowManager.LayoutParams.TYPE_PHONE);
        else if (Qualcomm.isVIA820())
            getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        */
        super.show();
    }
}
