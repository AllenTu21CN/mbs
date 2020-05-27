package cn.lx.mbs.ui.view;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.util.Size;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.ArrayList;

public class Utils {
    private static final float BASE_RESOLUTION_WIDTH = 2560;
    private static final float BASE_RESOLUTION_HEIGHT = 1600;
    private static float mPixelSizeFactor = 1.0f;

    public static void init(Activity activity) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        //int width = displayMetrics.widthPixels;
        mPixelSizeFactor = (float)height / BASE_RESOLUTION_HEIGHT;
    }

    public static int PX(float px) {
        return px > 1.0 ? (int)(px * mPixelSizeFactor) : (int)px;
    }

    public static void setSize(View v, int width, int height) {
        if (v.getLayoutParams() instanceof ViewGroup.LayoutParams) {
            ViewGroup.LayoutParams p = v.getLayoutParams();
            p.width = width;
            p.height = height;
            v.setLayoutParams(p);
        }
    }


    public static void setMargins(View v, int left, int top, int right, int bottom) {
        if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            p.setMargins(left, top, right, bottom);
            v.requestLayout();
        }
    }

    public static void adjustAll(ViewGroup root) {
        final int childCount = root.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = root.getChildAt(i);

            adjustMargins(child);
            adjustPaddings(child);
            adjustSize(child);

            if (child instanceof ViewGroup) {
                adjustAll((ViewGroup) child);
                continue;
            }

            if (child instanceof TextView) {
                adjustTextSize((TextView) child);
            }
        }
    }

    public static void adjustMargins(View v) {
        if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            int left = Utils.PX(p.leftMargin);
            int top = Utils.PX(p.topMargin);
            int right = Utils.PX(p.rightMargin);
            int bottom = Utils.PX(p.bottomMargin);
            p.setMargins(left, top, right, bottom);
            v.requestLayout();
        }
    }

    public static void adjustPaddings(View v) {
        int left = PX(v.getPaddingLeft());
        int top = PX(v.getPaddingTop());
        int right = PX(v.getPaddingRight());
        int bottom = PX(v.getPaddingBottom());

        v.setPadding(left, top, right, bottom);
    }

    public static void adjustSize(View v) {
        if (v.getLayoutParams() instanceof ConstraintLayout.LayoutParams) {
            ConstraintLayout.LayoutParams p = (ConstraintLayout.LayoutParams) v.getLayoutParams();
            p.width = PX(p.width);
            p.height = PX(p.height);
            v.setLayoutParams(p);
        } else if (v.getLayoutParams() instanceof TableRow.LayoutParams) {
            TableRow.LayoutParams p = (TableRow.LayoutParams) v.getLayoutParams();
            p.width = PX(p.width);
            p.height = PX(p.height);
            v.setLayoutParams(p);
        }
    }

    public static Size getSize(View v) {
        ViewGroup.LayoutParams p = v.getLayoutParams();
        return new Size(p.width, p.height);
    }

    public static void adjustTextSize(TextView v) {
        float px = v.getTextSize();
        v.setTextSize(TypedValue.COMPLEX_UNIT_PX, PX(px));
    }

    public static ArrayList<View> getViewsByTag(ViewGroup root, String tag){
        ArrayList<View> views = new ArrayList<>();
        final int childCount = root.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = root.getChildAt(i);
            if (child instanceof ViewGroup) {
                views.addAll(getViewsByTag((ViewGroup) child, tag));
            }

            final Object tagObj = child.getTag();
            if (tagObj != null && tagObj.equals(tag)) {
                views.add(child);
            }

        }

        return views;
    }

    public static Bitmap generateCheckerBoardBitmap(int canvasWidth, int canvasHeight, int cellWidth, int cellHeight) {
        Bitmap image = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);

        // Draw checkerborad's background
        canvas.drawARGB(0xFF, 0xFF, 0xFF, 0xFF);

        // Draw gray cells
        Paint paint = new Paint();
        paint.setColor(0xFFCCCCCC);
        paint.setStyle(Paint.Style.FILL);
        for (int y = 0; y < canvasHeight; y = y + cellHeight) {
            for (int x = 0; x < canvasWidth; x = x + cellWidth * 2) {
                float left = (0 == (y / cellHeight) % 2) ? x : x + cellWidth;
                float top = y;
                float right = left + cellWidth;
                float bottom = top + cellHeight;
                canvas.drawRect(left, top, right, bottom, paint);
            }
        }

        return image;
    }
}
