package cn.lx.mbs.ui.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import com.sanbu.tools.LogUtil;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.MotionEventCompat;

import static android.view.MotionEvent.INVALID_POINTER_ID;

public class OverlayEditableView extends View {

    public interface OnPositionChangeListener {
        public void onPositionChanged(int left, int top, int right, int bottom, int centerX, int centerY);
    }

    private static final String TAG = OverlayEditableView.class.getSimpleName();

    private String mText;
    private Bitmap mImage;

    private Paint mPainter = new Paint();
    private Rect mCanvasRect = new Rect();
    //private Rect mContentRect = new Rect();
    private Rect mDrawingRect = new Rect();

    private ScaleGestureDetector mScaleDetector;
    private float mScaleFactor = 1.f;

    private float mLastTouchX;
    private float mLastTouchY;

    private OnPositionChangeListener mOnPositionChangeListener;

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor *= detector.getScaleFactor();
            LogUtil.d(TAG, "Scale gesture detected, scale factor=" + mScaleFactor);

            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 10.0f));

            setScaleFactor(mScaleFactor);

            return true;
        }
    }

    public OverlayEditableView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mPainter.setAntiAlias(true);

        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    public void setOnPositionChangeListener(OnPositionChangeListener listener) {
        mOnPositionChangeListener = listener;
    }

    public void setImageBitmap(Bitmap image) {
        mImage = image;
    }

    public void setText(String text) {
        mText = text;
    }

    public void setScaleFactor(float scaleFactor) {
        setScaleX(scaleFactor);
        setScaleY(scaleFactor);

        notifyPositionChanged();
        //invalidate();
        /*ViewGroup.LayoutParams layoutParams = getLayoutParams();
        float centerX = getX() + layoutParams.width / 2;
        float centerY = getY() + layoutParams.height / 2;

        // Resize
        layoutParams.width = (int)(layoutParams.width * scaleFactor);
        layoutParams.height = (int)(layoutParams.height * scaleFactor);
        setLayoutParams(layoutParams);

        // Keep center stable
        setX(centerX);
        setY(centerY);*/
    }

    public void setRotateAngle(float angle) {
        //animate().rotationBy(angle).setDuration(0).setInterpolator(new LinearInterpolator()).start();
        setRotation(angle);
    }

    public void setOpacity(float opacity) {
        setAlpha(opacity);
    }

    protected void notifyPositionChanged() {
        getDrawingRect(mDrawingRect);
        int left = (int)getX();
        int top = (int)getY();
        int right = left + getWidth();
        int bottom = top + getHeight();
        int centerX = (left + right) / 2;
        int centerY = (top + bottom) / 2;

        float scaledWidth = getWidth() * getScaleX();
        float scaledHeight = getHeight() * getScaleY();
        left = centerX - (int)(scaledWidth / 2.f);
        top = centerY - (int)(scaledHeight / 2.f);
        right = centerX + (int)(scaledWidth / 2.f);
        bottom = centerY + (int)(scaledHeight / 2.f);

        //LogUtil.d(TAG, String.format("Position changed: left=%d, top=%d, right=%d, bottom=%d",
        //        left, top, right, bottom));
        if (mOnPositionChangeListener != null) {
            mOnPositionChangeListener.onPositionChanged(left, top, right, bottom, centerX, centerY);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final float blockWidth = Utils.PX(10);
        final int borderPadding = (int)blockWidth / 2;

        canvas.getClipBounds(mCanvasRect);
        /*mContentRect.set(
                mCanvasRect.left + borderPadding,
                mCanvasRect.top + borderPadding,
                mCanvasRect.right - borderPadding,
                mCanvasRect.bottom - borderPadding);*/

        // TODO:
        if (mImage != null) {
            canvas.drawBitmap(mImage, null, mCanvasRect, mPainter);
        }

        if (mText != null && !mText.isEmpty()) {
            // TODO: Draw text
        }

        /*/ Draw border
        mPainter.setStyle(Paint.Style.STROKE);
        mPainter.setColor(0xFF000000);
        mPainter.setStrokeWidth(Utils.PX(2));
        canvas.drawRect(mContentRect, mPainter);

        //mPainter.setColor(0xFF000000);
        //mPainter.setStrokeWidth(Utils.PX(2));
        //canvas.drawRect(dstRect, mPainter);

        // Draw corner blocks
        mPainter.setColor(0xFFFFFFFF);
        mPainter.setStyle(Paint.Style.FILL);
        canvas.drawRect(
                mCanvasRect.left,
                mCanvasRect.top,
                mCanvasRect.left + blockWidth,
                mCanvasRect.top + blockWidth,
                mPainter);
        canvas.drawRect(
                mCanvasRect.right - blockWidth,
                mCanvasRect.top,
                mCanvasRect.right,
                mCanvasRect.top + blockWidth,
                mPainter);
        canvas.drawRect(
                mCanvasRect.left,
                mCanvasRect.bottom - blockWidth,
                mCanvasRect.left + blockWidth,
                mCanvasRect.bottom,
                mPainter);
        canvas.drawRect(
                mCanvasRect.right - blockWidth,
                mCanvasRect.bottom - blockWidth,
                mCanvasRect.right,
                mCanvasRect.bottom,
                mPainter);

        mPainter.setColor(0xFF000000);
        mPainter.setStyle(Paint.Style.STROKE);
        canvas.drawRect(
                mCanvasRect.left,
                mCanvasRect.top,
                mCanvasRect.left + blockWidth,
                mCanvasRect.top + blockWidth,
                mPainter);
        canvas.drawRect(
                mCanvasRect.right - blockWidth,
                mCanvasRect.top,
                mCanvasRect.right,
                mCanvasRect.top + blockWidth,
                mPainter);
        canvas.drawRect(
                mCanvasRect.left,
                mCanvasRect.bottom - blockWidth,
                mCanvasRect.left + blockWidth,
                mCanvasRect.bottom,
                mPainter);
        canvas.drawRect(
                mCanvasRect.right - blockWidth,
                mCanvasRect.bottom - blockWidth,
                mCanvasRect.right,
                mCanvasRect.bottom,
                mPainter);
         */
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // Let the ScaleGestureDetector inspect all events.
        mScaleDetector.onTouchEvent(ev);

        if (ev.getPointerCount() == 1) {
            final int action = ev.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN : {
                    final float x = ev.getRawX();
                    final float y = ev.getRawY();
                    //LogUtil.d(TAG, "Down event, x=" + x + ", y=" + y);

                    // Remember where we started (for dragging)
                    mLastTouchX = x;
                    mLastTouchY = y;

                    break;
                }

                case MotionEvent.ACTION_MOVE : {
                    final float x = ev.getRawX();
                    final float y = ev.getRawY();

                    // Calculate the distance moved
                    final float dx = x - mLastTouchX;
                    final float dy = y - mLastTouchY;
                    //LogUtil.d(TAG, "Move event, distance moved: dx=" + dx + ", dy=" + dy);

                    float posX = this.getX() + dx;
                    float posY = this.getY() + dy;
                    setX(posX);
                    setY(posY);

                    notifyPositionChanged();

                    // Remember this touch position for the next move event
                    mLastTouchX = x;
                    mLastTouchY = y;

                    break;
                }

                case MotionEvent.ACTION_POINTER_UP : {
                    mLastTouchX = ev.getRawX();
                    mLastTouchY = ev.getRawY();
                    break;
                }
            }
        }

        return true;
    }
}
