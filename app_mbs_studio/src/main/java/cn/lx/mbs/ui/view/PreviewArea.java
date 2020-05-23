package mbs.studio.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.Paint;
import android.util.Log;
import android.util.TypedValue;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.studio.R;

public class PreviewArea {
    private static final String TAG = PreviewArea.class.getSimpleName();
    private static final int SCENE_COUNT = 9;

    private Activity mActivity;
    private TextView mPreviewAreaLabel;
    private SurfaceView mPreviewSurfaceView;
    private LinearLayout mSceneButtonGroup;
    private SceneButton[] mSceneButtons = new SceneButton[SCENE_COUNT];

    private int mActiveSceneIndex = -1;

    public PreviewArea(Activity activity) {
        mActivity = activity;
    }

    public void init() {
        // Adjust label text size
        mPreviewAreaLabel = mActivity.findViewById(R.id.preview_area_label);
        mPreviewAreaLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX, Utils.PX(28.0f));

        // Adjust margin
        mPreviewSurfaceView = mActivity.findViewById(R.id.preview_surface_view);
        Utils.setMargins(mPreviewSurfaceView, 0, Utils.PX(12.0f), 0, 0);

        // Create and add scene switch buttons
        mSceneButtonGroup = mActivity.findViewById(R.id.scene_button_group);
        int btnMarginTop = Utils.PX(4.0f);
        int btnMarginLeftRight = Utils.PX(10.0f);
        for (int i = 0; i < SCENE_COUNT; i++) {
            SceneButton btn = new SceneButton(mSceneButtonGroup.getContext(), i);
            btn.setText(String.valueOf(i + 1));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
            lp.weight = 1;
            switch (i) {
                case 0 :
                    lp.setMargins(0, btnMarginTop, btnMarginLeftRight, 0);
                    break;
                case (SCENE_COUNT - 1) :
                    lp.setMargins(btnMarginLeftRight, btnMarginTop, 0, 0);
                    break;
                default :
                    lp.setMargins(btnMarginLeftRight, btnMarginTop, btnMarginLeftRight, 0);
                    break;
            }
            btn.setLayoutParams(lp);

            btn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // TODO:
                    if (v instanceof SceneButton) {
                        SceneButton btn = (SceneButton) v;
                        Log.d(TAG, "Scene button clicked! Button Id:" + btn.getText());
                        if (mActiveSceneIndex != btn.getSceneId()) {
                            if (0 <= mActiveSceneIndex && mActiveSceneIndex < SCENE_COUNT) {
                                mSceneButtons[mActiveSceneIndex].setActive(false);
                            }
                            btn.setActive(true);
                            mActiveSceneIndex = btn.getSceneId();
                        } else {
                            if (btn.isActive()) {
                                btn.setActive(false);
                                mActiveSceneIndex = -1;
                            } else {
                                btn.setActive(true);
                                mActiveSceneIndex = btn.getSceneId();
                            }
                        }
                    }
                }
            });

            mSceneButtonGroup.addView(btn, i);
            mSceneButtons[i] = btn;
        }
    }

    private class SceneButton extends GlowButton {
        private int mSceneId;
        private boolean mActive = false;

        private Paint mPaint;

        public SceneButton(Context context, int sceneId) {
            super(context, null);

            mSceneId = sceneId;

            //setBackground(mActivity.getDrawable(R.drawable.common_button_bg));
            setTextColor(mActivity.getColor(R.color.primaryTextColor));
            setTextSize(TypedValue.COMPLEX_UNIT_PX, Utils.PX(40.0f));
            //setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
            //setBackgroundDrawable(null);
            setPadding(0, 0, 0, Utils.PX(8));

            //mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            //setLayerType(LAYER_TYPE_SOFTWARE, mPaint);
            setLightColor(COLOR_OFF);
            setLightHeight(Utils.PX(8));
            setLightPosition(POSITION_BOTTOM);
        }

        public int getSceneId() { return mSceneId; }

        public boolean isActive() { return mActive; }

        public void setActive(boolean active) {
            mActive = active;
            setLightColor(mActive ? COLOR_ON : COLOR_OFF);

            if (mActive) {
                //setBackgroundColor(mActivity.getColor(R.color.commonButtonPressedBackgroundColor));
                //setTextColor(mActivity.getColor(R.color.commonButtonPressedTextColor));
            } else {
                //setBackground(mActivity.getDrawable(R.drawable.common_button_bg));
                //setTextColor(mActivity.getColor(R.color.commonButtonNormalTextColor));
            }

            invalidate();
        }

        /*@Override
        protected void onDraw(Canvas canvas) {
            final int padding = 3;
            // Background and shadow
            mPaint.setShadowLayer(6, 0, 0, 0x33000000);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(mActivity.getColor(mActive ? R.color.commonButtonPressedBackgroundColor : R.color.commonButtonNormalBackgroundColor));
            canvas.drawRoundRect(padding, padding, getWidth() - padding, getHeight() - padding, 6.0f, 6.0f, mPaint);

            // Border
            mPaint.clearShadowLayer();
            //if (!mActive) {
                mPaint.setStyle(Paint.Style.STROKE);
                mPaint.setColor(0xFFEEEEEE);
                mPaint.setStrokeWidth(2.0f);
                canvas.drawRoundRect(padding, padding, getWidth() - padding, getHeight() - padding, 6.0f, 6.0f, mPaint);
            //}

            // Text
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setTextAlign(Paint.Align.CENTER);
            mPaint.setTextSize(Utils.PX(40.0f));
            mPaint.setColor(mActivity.getColor(R.color.commonButtonNormalTextColor));
            int xPos = (getWidth() / 2);
            int yPos = (int) ((getHeight() / 2) - ((mPaint.descent() + mPaint.ascent()) / 2)) ;
            canvas.drawText((String)getText(), xPos, yPos, mPaint);

            //super.onDraw(canvas);
        }*/
    }
}
