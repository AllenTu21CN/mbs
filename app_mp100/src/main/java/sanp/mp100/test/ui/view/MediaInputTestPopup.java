package sanp.mp100.test.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.TextView;

import sanp.tools.utils.LogManager;
import sanp.tools.utils.ScreenUtils;
import sanp.mp100.R;
import sanp.mp100.test.utils.ProductionTesting;

/**
 * Created by zhangxd on 2017/7/20
 * 视频输入测试.
 */

public class MediaInputTestPopup extends PopupWindow implements ProductionTesting.Callback {

    private static final String TAG = "MediaInputTestPopup";

    private Context mContext;

    private View mView;

    private TextView mediaInputType;

    private TextView mUseStatus;

    private TextView mFps;

    private TextView mCurPx;

    private TextView maxPx;

    public MediaInputTestPopup(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public MediaInputTestPopup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
    }

    public MediaInputTestPopup(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mContext = context;
    }


    public MediaInputTestPopup(Context context) {
        mContext = context;
        mView = LayoutInflater.from(mContext).inflate(R.layout.multi_media_view, null);
        int width = ScreenUtils.getScreenWidth(mContext) / 3;
        int height = ScreenUtils.getScreenWidth(mContext) / 3;
        setWidth(width);
        setHeight(height);
        setContentView(mView);
        initView();
        ProductionTesting.getInstance().setContext(context);
    }

    private void initView() {
        mView = LayoutInflater.from(mContext).inflate(R.layout.media_input_test, null);
        mediaInputType = (TextView) mView.findViewById(R.id.media_input_type_result);
        mUseStatus = (TextView) mView.findViewById(R.id.use_status_result);
        mFps = (TextView) mView.findViewById(R.id.frame_bps_result);
        mCurPx = (TextView) mView.findViewById(R.id.current_fbps_result);
        maxPx = (TextView) mView.findViewById(R.id.max_fbps_result);
        setContentView(mView);
    }

    public void startMediaInputTest(int type) {
        showAtLocation(mView, Gravity.CENTER, 0, 0);
        ProductionTesting.getInstance().setCallback(this);
        ProductionTesting.getInstance().startCaptureTesting(type);
    }

    @Override
    public void onSourceStatis(int id, float fps, int kbps) {
        LogManager.d(TAG, "onSourceStatis " + fps + "fps" + "kbps:" + kbps);
    }

    @Override
    public void onOutputStatis(int id, float fps, int kbps) {
        LogManager.d(TAG, "onOutputStatis " + fps + "fps" + "kbps:" + kbps);
    }

    @Override
    public void onVideoRendererStatis(float fps) {

    }

    @Override
    public void onHardwareStatis(ProductionTesting.TestingType type, ProductionTesting.HardwareStatis statis) {

    }

    @Override
    public void onOutputAdded(ProductionTesting.TestingType type, int id, String url, int result, int width, int height) {

    }

    @Override
    public void onSourceResolutionChanged(int id, int width, int height) {
        LogManager.d(TAG, "onSourceResolutionChanged width" + width + "height:" + height);
    }

    public void dismissPopup() {
        dismiss();
        ProductionTesting.getInstance().stopMediaTesting();
    }
}
