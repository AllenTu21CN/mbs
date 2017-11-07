package sanp.mp100.test.ui.view;

import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import sanp.tools.utils.ScreenUtils;
import sanp.mp100.R;
import sanp.mp100.test.utils.ProductionTesting;

/**
 * Created by zhangxd on 2017/7/21
 * 编解码信息封装 view.
 */

public class MediaCodecItemView extends LinearLayout {

    private static final String TAG = "MediaCodecItemVIew";

    private static final String FORMAT_HIGH = "1920*1080";

    private static final String FORMAT_MIDDLE = "1280*720";

    private static final String FORMAT_LOW = "856*480";


    private LinearLayout mRootLayout;

    private TextView mPxType;

    private TextView mBitType;

    private TextView mFpsType;

    private TextView mPxTxt;

    private TextView mBitTxt;

    private TextView mFpsTxt;

    public MediaCodecItemView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public MediaCodecItemView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    public MediaCodecItemView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView();
    }

    public MediaCodecItemView(Context context) {
        super(context);
        initView();
    }

    private void initView() {
        View mView = LayoutInflater.from(getContext()).inflate(R.layout.media_codec_item, null);
        mRootLayout = (LinearLayout) mView.findViewById(R.id.root_view);
        mPxType = (TextView) mView.findViewById(R.id.px_type);
        mBitType = (TextView) mView.findViewById(R.id.bit_type);
        mFpsType = (TextView) mView.findViewById(R.id.fbps_type);
        mPxTxt = (TextView) mView.findViewById(R.id.px_rate);
        mBitTxt = (TextView) mView.findViewById(R.id.bit_rate);
        mFpsTxt = (TextView) mView.findViewById(R.id.fbps_rate);
        addView(mView);
        scaleViewSize();
    }

    public void setPxText(String pxText) {
        String text = mPxType.getText().toString();
        if (TextUtils.isEmpty(text)) {
            mPxTxt.setText(pxText);
            mPxTxt.setVisibility(View.INVISIBLE);
        }
    }

    public void setCodecStatisText(String bitTxt, String fpsTxt) {
        mBitTxt.setText(bitTxt);
        mFpsTxt.setText(fpsTxt);
    }

    public void setCodecType(boolean decodding) {
        if (mPxTxt.getVisibility() == View.INVISIBLE) {
            mPxTxt.setVisibility(View.VISIBLE);
        }
        if (decodding) {
            mPxType.setText(getContext().getString(R.string.decoding_px));
            mBitType.setText(getContext().getString(R.string.decoding_bit_rate));
            mFpsType.setText(getContext().getString(R.string.decoding_frame_rate));
        } else {
            mPxType.setText(getContext().getString(R.string.encoding_px));
            mBitType.setText(getContext().getString(R.string.encoding_bit_rate));
            mFpsType.setText(getContext().getString(R.string.encoding_frame_rate));
        }
    }

    public void setEnCodingTxt(ProductionTesting.VideoFormat videoFormat) {
        if (videoFormat == ProductionTesting.DEFAULT_1080P_FORMAT) {
            mPxTxt.setText(FORMAT_HIGH);
        } else if (videoFormat == ProductionTesting.DEFAULT_720P_FORMAT) {
            mPxTxt.setText(FORMAT_MIDDLE);
        } else {
            mPxTxt.setText(FORMAT_LOW);
        }
    }

    private void scaleViewSize() {
        int textSize = ScreenUtils.getScreenWidth(getContext()) / 50;
        int textWidth = ScreenUtils.getScreenWidth(getContext()) / 8;
        int count = mRootLayout.getChildCount();
        for (int i = 0; i < count; i++) {
            View view = mRootLayout.getChildAt(i);
            if (view instanceof TextView) {
                ((TextView) view).setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
                LayoutParams lp = (LayoutParams) view.getLayoutParams();
                lp.width = textWidth;
                view.setLayoutParams(lp);
            }
        }
    }
}
