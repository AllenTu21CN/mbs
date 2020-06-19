package cn.lx.mbs.ui.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sanbu.tools.EventPub;
import com.sanbu.tools.ToastUtil;

import androidx.appcompat.widget.AppCompatButton;
import androidx.constraintlayout.widget.ConstraintLayout;

import cn.lx.mbs.Events;
import cn.lx.mbs.R;
import cn.lx.mbs.support.MBS;
import cn.lx.mbs.support.structures.SRId;
import cn.lx.mbs.support.structures.SRState;
import cn.lx.mbs.ui.MainActivity;
import cn.sanbu.avalon.endpoint3.structures.jni.TransitionDesc;
import cn.sanbu.avalon.endpoint3.structures.jni.TransitionMode;

public class ControlArea {
    private Activity mActivity;
    private ConstraintLayout mSelfLayout;
    private LinearLayout mTransitionGroup1;
    private Button mCutButton;
    private Button mFadeButton;
    private Button mWipeButton;
    private Button mFtbButton;
    private SeekBar mTransitionDurationSeekBar;
    private TextView mTransitionDurationTextView;
    private RecordingButton mRecordingButton;
    private SwitchButton mEnableStreamingSwitch;
    private SwitchButton mEnableRecordingSwitch;
    private ImageButton mInfoButton;
    private ImageButton mSettingsButton;

    private int mTransitionDurationMs;

    public ControlArea(Activity activity) {
        mActivity = activity;
    }

    public void init() {
        // Adjust margins
        mSelfLayout = mActivity.findViewById(R.id.area_control);
        Utils.adjustMargins(mSelfLayout);
        for (int i = 0; i < mSelfLayout.getChildCount(); i++) {
            Utils.adjustMargins(mSelfLayout.getChildAt(i));
        }

        //mTransitionGroup1 = mActivity.findViewById(R.id.transition_group_1);
        //Utils.setSize(mTransitionGroup1, ViewGroup.LayoutParams.WRAP_CONTENT, Utils.PX(120));

        final int transitionBtnTextSize = Utils.PX(28);
        final int transitionBtnWidth = Utils.PX(160);
        final int transitionBtnHeight = Utils.PX(80);

        mCutButton = mActivity.findViewById(R.id.cut_button);
        mFadeButton = mActivity.findViewById(R.id.fade_button);
        mWipeButton = mActivity.findViewById(R.id.wipe_button);
        mFtbButton = mActivity.findViewById(R.id.ftb_button);

        Button[] transitionButtons = new Button[] { mCutButton, mFadeButton, mWipeButton, mFtbButton};
        for (Button btn : transitionButtons) {
            btn.setTextSize(TypedValue.COMPLEX_UNIT_PX, transitionBtnTextSize);
            Utils.setSize(btn, transitionBtnWidth, transitionBtnHeight);
        }

        mCutButton.setOnClickListener(view -> {
            MBS.getInstance().switchPVW2PGM(TransitionDesc.buildEmpty(), null);
        });

        mFadeButton.setOnClickListener(view -> {
            MBS.getInstance().switchPVW2PGM(new TransitionDesc(TransitionMode.fade, (float)mTransitionDurationMs / 1000.0f), null);
        });

        mWipeButton.setOnClickListener(view -> {
            MBS.getInstance().switchPVW2PGM(new TransitionDesc(TransitionMode.wipeDown, (float)mTransitionDurationMs / 1000.0f), null);
        });

        mFtbButton.setOnClickListener(view -> {
            MBS.getInstance().switchPVW2PGM(new TransitionDesc(TransitionMode.flyeye, (float)mTransitionDurationMs / 1000.0f), null);
        });

        /*Utils.setMargins(mCutButton, 0, Utils.PX(36), Utils.PX(18), 0);
        Utils.setMargins(mFadeButton, Utils.PX(18), 0, 0, 0);
        Utils.setMargins(mWipeButton, 0, Utils.PX(18), Utils.PX(18), 0);
        Utils.setMargins(mFtbButton, Utils.PX(18), 0, 0, 0);*/

        mTransitionDurationSeekBar = mActivity.findViewById(R.id.transition_duration_seekbar);
        //Utils.setMargins(mTransitionDurationSeekBar, 0, Utils.PX(36), 0, 0);
        mTransitionDurationSeekBar.setProgressBarHeight(Utils.PX(8));
        mTransitionDurationSeekBar.setThumbSize(Utils.PX(30));
        Utils.setSize(mTransitionDurationSeekBar, Utils.PX(400), ViewGroup.LayoutParams.WRAP_CONTENT);

        mTransitionDurationTextView = mActivity.findViewById(R.id.transition_duration_label);
        mTransitionDurationTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, Utils.PX(24));

        mTransitionDurationSeekBar.setMax(20);
        mTransitionDurationSeekBar.incrementProgressBy(1);
        mTransitionDurationSeekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int i, boolean b) {
                i = i + 1;
                if (i <= 10) {
                    mTransitionDurationMs = i * 100; // 0.1s-1.0s, step 0.1s
                } else if (i <= 15) {
                    mTransitionDurationMs = (i - 10) * 200 + 1000; // 1.2s-2.0s, step 0.2s
                } else {
                    mTransitionDurationMs = (i - 15) * 500 + 2000; // 2.5s-5s, step 0.5s
                }

                mTransitionDurationTextView.setText(String.format("%.1fs", (float)mTransitionDurationMs / 1000.0f));
            }

            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {

            }
        });

        /*
        mWipeButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, Utils.PX(32));
        mWipeButton.setWidth(Utils.PX(160));
        //mWipeButton.setHeight(Utils.PX(80));


        mFtbButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, Utils.PX(32));
        mFtbButton.setWidth(Utils.PX(160));
        //mFtbButton.setHeight(Utils.PX(80));


        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(mActivity,
                R.array.transition_duration_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mTransitionDurationSpinner.setAdapter(adapter);*/

        int recordingButtonSize = Utils.PX(100);
        mRecordingButton = mActivity.findViewById(R.id.recording_button);
        Utils.setSize(mRecordingButton, recordingButtonSize, recordingButtonSize);
        mRecordingButton.setData(this);

        mEnableStreamingSwitch = mActivity.findViewById(R.id.enable_streaming);
        mEnableRecordingSwitch = mActivity.findViewById(R.id.enable_recording);
        mEnableStreamingSwitch.setTextSize(TypedValue.COMPLEX_UNIT_PX, Utils.PX(28));
        mEnableStreamingSwitch.setThumbSize(Utils.PX(32));
        mEnableRecordingSwitch.setTextSize(TypedValue.COMPLEX_UNIT_PX, Utils.PX(28));
        mEnableRecordingSwitch.setThumbSize(Utils.PX(32));

        /*ShapeDrawable thumbDrawable = new ShapeDrawable(new OvalShape());
        thumbDrawable.getPaint().setStyle(Paint.Style.FILL);
        thumbDrawable.getPaint().setColor(0xffffffff);
        int thumbSize = Utils.PX(32);
        thumbDrawable.setIntrinsicWidth(thumbSize);
        thumbDrawable.setIntrinsicHeight(thumbSize);
        LayerDrawable switchThumbDrawable = new LayerDrawable(new Drawable[]{ thumbDrawable });
        int padding = Utils.PX(4);
        switchThumbDrawable.setLayerInset(0, padding, padding, padding, padding);

        mEnableStreamingSwitch.setThumbDrawable(switchThumbDrawable);
        mEnableRecordingSwitch.setThumbDrawable(switchThumbDrawable);*/

        mInfoButton = mActivity.findViewById(R.id.info_button);
        Utils.setSize(mInfoButton, Utils.PX(48), Utils.PX(48));

        mInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO:
                if (mActivity instanceof MainActivity) {
                    MainActivity ma = (MainActivity) mActivity;
                    ma.hideSystemUI();
                }
            }
        });

        mSettingsButton = mActivity.findViewById(R.id.settings_button);
        Utils.setSize(mSettingsButton, Utils.PX(48), Utils.PX(48));

        mSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mActivity instanceof MainActivity) {
                    MainActivity ma = (MainActivity) mActivity;
                    ma.showSettingsDialog();
                }
            }
        });
    }

    public static class TransitionButton extends AppCompatButton {
        private Paint mPaint;

        public TransitionButton(Context context, AttributeSet attrs) {
            super(context, attrs);

            //setBackground(mActivity.getDrawable(R.drawable.common_button_bg));
            //setTextColor(mActivity.getColor(R.color.commonButtonNormalTextColor));
            //setTextSize(TypedValue.COMPLEX_UNIT_PX, Utils.PX(36.0f));
            //setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
            setBackgroundDrawable(getContext().getDrawable(R.drawable.common_button_bg));
            setPadding(0, 0, 0, 0);

            mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            setLayerType(LAYER_TYPE_SOFTWARE, mPaint);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            /*final int padding = 3;
            // Background and shadow
            mPaint.setShadowLayer(6, 0, 0, 0x33000000);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(getContext().getColor(R.color.commonButtonNormalBackgroundColor));
            canvas.drawRoundRect(padding, padding, getWidth() - padding, getHeight() - padding, 6.0f, 6.0f, mPaint);

            // Border
            mPaint.clearShadowLayer();
            //if (!mActive) {
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(0xFF000000);
            mPaint.setStrokeWidth(2.0f);
            canvas.drawRoundRect(padding, padding, getWidth() - padding, getHeight() - padding, 6.0f, 6.0f, mPaint);
            //}

            // Text
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setTextAlign(Paint.Align.CENTER);
            mPaint.setTextSize(getTextSize());
            mPaint.setColor(getContext().getColor(R.color.commonButtonNormalTextColor));
            int xPos = (getWidth() / 2);
            int yPos = (int) ((getHeight() / 2) - ((mPaint.descent() + mPaint.ascent()) / 2)) ;
            canvas.drawText((String)getText(), xPos, yPos, mPaint);*/

            super.onDraw(canvas);
        }
    }

    public static class RecordingButton extends AppCompatButton {

        private boolean mIsRecording;
        private Paint mPaint;
        private ControlArea mParent;
        private Handler mHandler;

        public RecordingButton(Context context, AttributeSet attrs) {
            super(context, attrs);
            mHandler = new Handler();

            setBackgroundDrawable(null);
            setPadding(0, 0, 0, 0);
            mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

            setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    mIsRecording = !mIsRecording;
                    invalidate();

                    EventPub.getDefaultPub().post(Events.SR_SWITCH_CHANGED, mIsRecording ? 1 : 0);

                    if (mIsRecording) {
                        if (mParent.mEnableRecordingSwitch.isChecked()) {
                            MBS.getInstance().switchSRState(SRId.Recording, SRState.Start, result -> {
                                if (!result.isSuccessful())
                                    mHandler.post(() -> ToastUtil.show("Start recording failed", false));
                            });
                        }

                        if (mParent.mEnableStreamingSwitch.isChecked()) {
                            MBS.getInstance().switchSRState(SRId.Streaming, SRState.Start, result -> {
                                if (!result.isSuccessful())
                                    mHandler.post(() -> ToastUtil.show("Start steaming failed", false));
                            });
                        }
                    } else {
                        MBS.getInstance().switchSRState(SRId.Streaming, SRState.Stop, null);
                        MBS.getInstance().switchSRState(SRId.Recording, SRState.Stop, null);
                    }
                }
            });
        }

        public void setData(ControlArea parent) {
            mParent = parent;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float centerX = getWidth() / 2.0f;
            float centerY = getHeight() / 2.0f;
            float outerCircleRadius = getHeight() / 2.0f;
            float innerCircleRadius = getHeight() / 2.5f;
            float innerSquareSize = getHeight() / 2.3f;
            float innerSquareRadius = 4.0f;

            // Outer circle
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(Color.WHITE);
            canvas.drawCircle(centerX, centerY, outerCircleRadius, mPaint);

            mPaint.setColor(Color.RED);
            if (mIsRecording) {
                // Inner rectangle
                float left = (getWidth() - innerSquareSize) / 2.0f;
                float top = (getHeight() - innerSquareSize) / 2.0f;
                canvas.drawRoundRect(left, top, left + innerSquareSize, top + innerSquareSize, innerSquareRadius, innerSquareRadius, mPaint);
            } else {
                // Inner circle
                canvas.drawCircle(centerX, centerY, innerCircleRadius, mPaint);
            }

        }
    }
}
