package mbs.studio.view;

import android.app.Activity;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.util.TypedValue;
import android.view.SurfaceView;
import android.widget.TextView;

import mbs.studio.Event;
import com.example.studio.R;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class ProgramArea {
    private Activity mActivity;
    private TextView mProgramAreaLabel;
    private SurfaceView mProgramSurfaceView;
    private TextView mProductionClockLabel;
    private TextView mVideoInfoLabel;
    private TextView mAudioInfoLabel;

    private long MillisecondTime, StartTime, TimeBuff, UpdateTime = 0L ;
    private Handler handler;
    private int Seconds, Minutes, Hours, MilliSeconds ;
    public Runnable runnable = new Runnable() {
        public void run() {
            MillisecondTime = SystemClock.uptimeMillis() - StartTime;
            UpdateTime = TimeBuff + MillisecondTime;
            Seconds = (int) (UpdateTime / 1000);
            Hours = Seconds / 3600;
            Minutes = Seconds / 60 - Hours * 60;
            Seconds = Seconds % 60;
            MilliSeconds = (int) (UpdateTime % 1000);

            mProductionClockLabel.setText(String.format("%02d", Hours) + ":"
                    + String.format("%02d", Minutes) + ":"
                    + String.format("%02d", Seconds) + ":"
                    + String.format("%03d", MilliSeconds));

            handler.postDelayed(this, 0);
        }

    };

    public ProgramArea(Activity activity) {
        mActivity = activity;
    }

    public void init() {
        // Adjust label text size
        mProgramAreaLabel = mActivity.findViewById(R.id.program_area_label);
        mProgramAreaLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX, Utils.PX(28.0f));

        // Adjust margin
        mProgramSurfaceView = mActivity.findViewById(R.id.program_surface_view);
        Utils.setMargins(mProgramSurfaceView, 0, Utils.PX(12.0f), 0, 0);

        // Adjust production clock text size
        mProductionClockLabel = mActivity.findViewById(R.id.production_clock_label);
        mProductionClockLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX, Utils.PX(72.0f));

        // Adjust video/audio information text size
        mVideoInfoLabel = mActivity.findViewById(R.id.video_info_label);
        mVideoInfoLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX, Utils.PX(28.0f));

        mAudioInfoLabel = mActivity.findViewById(R.id.audio_info_label);
        mAudioInfoLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX, Utils.PX(28.0f));

        EventBus.getDefault().register(this);

        handler = new Handler();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRecordingStartEvent(Event.RecordingStartEvent event) {
        Log.i("ProgramArea", "Recieve RecordingStartEvent!");

        StartTime = SystemClock.uptimeMillis();
        handler.postDelayed(runnable, 0);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRecordingStopEvent(Event.RecordingStopEvent event) {
        Log.i("ProgramArea", "Recieve RecordingStopEvent!");
        handler.removeCallbacks(runnable);
        MillisecondTime = 0L ;
        StartTime = 0L ;
        TimeBuff = 0L ;
        UpdateTime = 0L ;
        Seconds = 0 ;
        Minutes = 0 ;
        Hours = 0;
        MilliSeconds = 0 ;
    }
}
