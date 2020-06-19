package cn.lx.mbs.ui.view;

import android.app.Activity;
import android.os.Handler;
import android.os.SystemClock;
import com.sanbu.tools.LogUtil;
import android.util.TypedValue;
import android.view.SurfaceView;
import android.widget.TextView;

import com.sanbu.tools.EventPub;

import cn.lx.mbs.Events;
import cn.lx.mbs.R;

public class ProgramArea {

    private static final String TAG = ProgramArea.class.getSimpleName();

    private Activity mActivity;
    private TextView mProgramAreaLabel;
    private SurfaceView mProgramSurfaceView;
    private TextView mProductionClockLabel;
    private TextView mVideoInfoLabel;
    private TextView mAudioInfoLabel;

    private long MillisecondTime, StartTime, TimeBuff, UpdateTime = 0L ;
    private Handler handler;
    private int Seconds, Minutes, Hours, MilliSeconds ;

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

        handler = new Handler();

        EventPub.getDefaultPub().subscribe(Events.SR_SWITCH_CHANGED, TAG, (evtId, arg1, arg2, obj) -> {
            if (arg1 == 0) {
                // stopped
                LogUtil.i(TAG, "RecordingStop!");
                handler.removeCallbacks(UpdateTimeLabel);
                MillisecondTime = 0L ;
                StartTime = 0L ;
                TimeBuff = 0L ;
                UpdateTime = 0L ;
                Seconds = 0 ;
                Minutes = 0 ;
                Hours = 0;
                MilliSeconds = 0 ;
            } else if (arg1 == 1) {
                // started
                LogUtil.i(TAG, "RecordingStart!");
                if (StartTime == 0)
                    StartTime = SystemClock.uptimeMillis();
                handler.postDelayed(UpdateTimeLabel, 0);
            } else if (arg1 == -1) {
                // paused
                handler.removeCallbacks(UpdateTimeLabel);
            }

            return false;
        });
    }

    private Runnable UpdateTimeLabel = new Runnable() {
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

            handler.postDelayed(UpdateTimeLabel, 100);
        }
    };
}
