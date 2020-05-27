package cn.lx.mbs.ui.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.Checkable;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import cn.lx.mbs.R;

public class AudioMixerArea {
    private Activity mActivity;

    private ConstraintLayout mSelfLayout;
    private AudioMixerItem mOutputItem;
    private AudioMixerItem mLocalMicItem;
    private AudioMixerItem[] mInputItems = new AudioMixerItem[4];

    public static class OnAfvOffButton extends GlowButton implements View.OnClickListener {
        public static final int STATE_OFF = 0;
        public static final int STATE_ON = 1;
        public static final int STATE_AFV = 2;

        public int mState = STATE_OFF;

        public OnAfvOffButton(Context context, AttributeSet attrs) {
            super(context, attrs);
            super.setOnClickListener(this);

            setPadding(0, 0, 0, 0);
            setTextColor(getContext().getColor(R.color.primaryTextColor));

            updateStyle();
        }

        public void setState(int state) {
            mState = state % 3;
            updateStyle();
        }

        @Override
        public void onClick(View v) {
            mState = (mState + 1) % 3;
            updateStyle();
        }

        private void updateStyle() {
            switch (mState) {
                case STATE_OFF :
                    setText("Off");
                    setLightColor(COLOR_OFF);
                    //setBackground(getContext().getDrawable(R.drawable.audio_mixer_btn_off_bg));
                    break;
                case STATE_ON :
                    setText("On");
                    setLightColor(COLOR_ON);
                    //setBackground(getContext().getDrawable(R.drawable.audio_mixer_btn_on_bg));
                    break;
                case STATE_AFV :
                    setText("Afv");
                    setLightColor(COLOR_YELLOW);
                    //setBackground(getContext().getDrawable(R.drawable.audio_mixer_btn_afv_bg));
                    break;
            }
        }
    } // End of OnAfvOffButton

    public static class SoloButton extends GlowButton implements Checkable, View.OnClickListener {
        private boolean mChecked = false;

        public SoloButton(Context context, AttributeSet attrs) {
            super(context, attrs);
            super.setOnClickListener(this);

            /*Drawable icon = getContext().getDrawable(R.drawable.ic_headset_black_24dp);
            icon.setBounds(Utils.PX(16), 2, Utils.PX(24 + 16), Utils.PX(24 + 2));
            setCompoundDrawables(icon, null, null, null);
            setCompoundDrawableTintList(getContext().getColorStateList(R.color.video_source_dynamic_button_tint));
            setCompoundDrawableTintMode(PorterDuff.Mode.SRC_IN);*/

            setPadding(0, 0, 0, 0);
            setTextColor(getContext().getColor(R.color.primaryTextColor));
            updateStyle();
        }

        @Override
        public boolean isChecked() {
            return mChecked;
        }

        @Override
        public void setChecked(boolean checked) {
            mChecked = checked;
            updateStyle();
        }

        @Override
        public void toggle() {
            setChecked(!mChecked);
        }

        @Override
        public void onClick(View v) {
            setChecked(!isChecked());
            updateStyle();
        }

        private void updateStyle() {
            setText("Solo");
            /*setBackground(getContext().getDrawable(
                    isChecked()
                    ? R.drawable.audio_mixer_btn_on_bg
                    : R.drawable.audio_mixer_btn_off_bg));*/
            setLightColor(isChecked() ? getContext().getColor(R.color.colorPrimaryOrange) : 0xFF1B1B1B);
        }
    } // End of class SoloButton

    public static class VolumeSlider extends SeekBar {
        public VolumeSlider(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        public void setThumbSize(int size) {
            int thumbSize = size;
            /**
             * <layer-list xmlns:android="http://schemas.android.com/apk/res/android">
             *     <item>
             *         <shape android:shape="oval">
             *             <solid android:color="@color/primaryTextColor"/>
             *             <size android:width="30px" android:height="30px"/>
             *         </shape>
             *     </item>
             * </layer-list>
             */
            ShapeDrawable thumb = new ShapeDrawable(new RectShape());
            thumb.getPaint().setStyle(Paint.Style.FILL);
            thumb.getPaint().setColor(0xffffffff);
            thumb.setIntrinsicWidth(Utils.PX(10));
            thumb.setIntrinsicHeight(thumbSize);

            LayerDrawable drawable = new LayerDrawable(new Drawable[]{ thumb });
            setThumb(drawable);
        }
    }

    public static class VUMeter extends View {

        //private int mLineWidth;
        //private int[] mPaddings = new int[] { 1, 0, 1, 0 };
        //private int mSpacing = 1;

        private float mLeftChannelDb = 0.7f;
        private float mRightChannelDb = 0.8f;

        private Paint mPaint;
        private LinearGradient mLinearGradient;

        //private Timer mTestTimer = new Timer(true);

        public VUMeter(Context context, AttributeSet attrs) {
            super(context, attrs);

            //mLineWidth = Utils.PX(6);
            //mSpacing = mLineWidth + 4 + mSpacing;

            //setBackgroundDrawable(null);
            setPadding(0, 0, 0, 0);

            // TODO:
            mPaint = new Paint(0);
            mPaint.setAntiAlias(false);
            //setLayerType(LAYER_TYPE_SOFTWARE, mPaint);

            /*mTestTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    update(new Random().nextFloat(), new Random().nextFloat());
                }
            }, 0, 100);*/
        }

        public void update(float left, float right) {
            mLeftChannelDb = left;
            mRightChannelDb = right;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            // TODO:
            int canvasWidth = getWidth();
            int canvasHeight = getHeight();
            int indicatorSpacing = (0 == canvasHeight % 2) ? 2 : 1;
            int indicatorBarSize = (canvasHeight - 6 - indicatorSpacing) / 2;

            if (null == mLinearGradient) {
                mLinearGradient = new LinearGradient(0, 0, canvasWidth, 0,
                        0xFFDDF23E, 0xFFF74A00, Shader.TileMode.MIRROR);
            }

            // Container border
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(0xFF000000);
            mPaint.setShader(null);
            mPaint.setStrokeWidth(1.0f);
            canvas.drawRect(1, 1, canvasWidth - 1, canvasHeight - 2, mPaint);

            // Indicator bars
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setShader(mLinearGradient);
            // Left channel
            int leftBarLength = (int)((canvasWidth - 5) * mLeftChannelDb);
            canvas.drawRect(2, 3, leftBarLength + 2, 3 + indicatorBarSize, mPaint);
            // Right channel
            int rightBarLength = (int)((canvasWidth - 5) * mRightChannelDb);
            canvas.drawRect(2, 3 + indicatorBarSize + indicatorSpacing, rightBarLength + 2, canvasHeight - 3, mPaint);
        }
    } // End of class VUMeter

    class AudioMixerItem {
        final static int DYNAMIC_BUTTON_COUNT = 4;

        ConstraintLayout mSelf;
        TextView mLabelTextView;
        VUMeter mVUMeter;
        Button[] mButtons = new Button[DYNAMIC_BUTTON_COUNT];
        VolumeSlider mVolumeSlider;

        AudioMixerItem(View view) {
            if (view instanceof ConstraintLayout) {
                mSelf = (ConstraintLayout) view;
            }
        }

        void init() {
            // Adjust margins
            Utils.adjustMargins(mSelf);
            for (int i = 0; i < mSelf.getChildCount(); i++) {
                Utils.adjustMargins(mSelf.getChildAt(i));
            }

            // Adjust label text size
            mLabelTextView = mSelf.findViewById(R.id.label);
            mLabelTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, Utils.PX(20));

            // Create VU meter
            mVUMeter = mSelf.findViewById(R.id.vu_meter);

            // Create dynamic buttons
            // TODO:
            mButtons[0] = mSelf.findViewById(R.id.dynamic_button_1);
            mButtons[1] = mSelf.findViewById(R.id.dynamic_button_2);
            mButtons[0].setTextSize(TypedValue.COMPLEX_UNIT_PX, Utils.PX(24));
            mButtons[1].setTextSize(TypedValue.COMPLEX_UNIT_PX, Utils.PX(24));
            //Utils.setSize(mButtons[0], Utils.PX(100), WRAP_CONTENT);
            //mButtons[2] = mSelf.findViewById(R.id.dynamic_button_3);
            //mButtons[3] = mSelf.findViewById(R.id.dynamic_button_4);

            mVolumeSlider = mSelf.findViewById(R.id.volume_seekbar);
            mVolumeSlider.setProgressBarHeight(Utils.PX(8));
            mVolumeSlider.setThumbSize(Utils.PX(30));
        }

        void setLabel(String label) {
            mLabelTextView.setText(label);
        }
    } // End of class AudioMixerItem

    class OutputItem extends AudioMixerItem {
        OutputItem(View view) {
            super(view);
        }

        @Override
        void init() {
            super.init();

            setLabel("OUT");
            mButtons[0].setVisibility(View.GONE);
        }
    }

    public AudioMixerArea(Activity activity) {
        mActivity = activity;
    }

    public void init() {
        // Adjust margins
        mSelfLayout = mActivity.findViewById(R.id.area_audio_mixer);
        Utils.adjustMargins(mSelfLayout);

        // Adjust label text size
        mSelfLayout = mActivity.findViewById(R.id.area_audio_mixer);

        mOutputItem = new OutputItem(mSelfLayout.findViewById(R.id.audio_mixer_output));
        mOutputItem.init();

        mLocalMicItem = new AudioMixerItem(mSelfLayout.findViewById(R.id.audio_mixer_mic));
        mLocalMicItem.init();
        mLocalMicItem.setLabel("MIC");

        mInputItems[0] = new AudioMixerItem(mSelfLayout.findViewById(R.id.audio_mixer_input_1));
        mInputItems[1] = new AudioMixerItem(mSelfLayout.findViewById(R.id.audio_mixer_input_2));
        mInputItems[2] = new AudioMixerItem(mSelfLayout.findViewById(R.id.audio_mixer_input_3));
        mInputItems[3] = new AudioMixerItem(mSelfLayout.findViewById(R.id.audio_mixer_input_4));
        for (int i = 0; i < mInputItems.length; i++) {
            mInputItems[i].init();
            mInputItems[i].setLabel(String.format("IN-%d", i + 1));
        }

    }
}
