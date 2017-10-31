package sanp.mp100.test.ui.view;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sanp.avalon.libs.base.utils.LogManager;
import sanp.avalon.libs.base.utils.ScreenUtils;
import sanp.mp100.R;
import sanp.mp100.test.utils.ProductionTesting;

/**
 * Created by zhangxd on 2017/7/17.
 */

public class MediaCodingPopup extends PopupWindow implements ProductionTesting.Callback {

    public static final String TAG = "MediaCodingPopup";

    private static final int MSG_SHOW_CODING_RESULT = 0x01;

    private static final int MSG_REMOVE_OUTPUT = 0x02;

    private static final int MSG_SHOW_HARDWARE_RESULT = 0x03;

    private static final int MSG_SHOW_PX_CHANGE = 0x04;

    private static final int MSG_SHOW_COMPLEX_FBPS = 0x05;

    private static final int MSG_DELAY = 5000;

    private Context mContext;

    private View mView;
    /**
     * 合成输出帧率
     */
    private TextView mMixedFps;

    private TextView mCpuUseRate;

    private TextView mCpuTemp;

    private TextView memUseRate;

    private TextView mEncodingTxt;

    private LinearLayout mainLayout;

    private ProductionTesting.VideoFormat mEnCodeFormat;

    private Map<Integer, View> mViewMap = new HashMap<>();

    private DecimalFormat df = new DecimalFormat("0.0");

    public MediaCodingPopup(Context context) {
        mContext = context;
        mView = LayoutInflater.from(mContext).inflate(R.layout.multi_media_view, null);
        setContentView(mView);
        initView();
        int width = (ScreenUtils.getScreenWidth(mContext) * 4) / 5;
        int height = ScreenUtils.getScreenWidth(mContext) / 2;
        setWidth(width);
        setHeight(height);
        ProductionTesting.getInstance().setContext(context);
        ProductionTesting.getInstance().setCallback(this);
    }


    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_SHOW_CODING_RESULT:
                    Bundle bundle = (Bundle) msg.obj;
                    setCodecStatis(bundle);
                    break;
                case MSG_SHOW_HARDWARE_RESULT:
                    showHardWareText((ProductionTesting.HardwareStatis) msg.obj);
                    break;
                case MSG_REMOVE_OUTPUT:
                    onMediaCodingShow();
                    ProductionTesting.getInstance().clean();
                    ProductionTesting.getInstance().startEncodedTesting2();
                    break;
                case MSG_SHOW_PX_CHANGE:
                    Bundle pxBundle = (Bundle) msg.obj;
                    setPxStatis(pxBundle);
                    break;
                case MSG_SHOW_COMPLEX_FBPS:
                    Bundle mixBundle = (Bundle) msg.obj;
                    setMixedFps(mixBundle);
                    break;
                default:
                    break;
            }
        }
    };

    public void initView() {
        mMixedFps = (TextView) mView.findViewById(R.id.output_frame_rate);
        mEncodingTxt = (TextView) mView.findViewById(R.id.encoding);
        mainLayout = (LinearLayout) mView.findViewById(R.id.main_coding);
        mCpuUseRate = (TextView) mView.findViewById(R.id.cpu_use_rate);
        mCpuTemp = (TextView) mView.findViewById(R.id.cpu_temp);
        memUseRate = (TextView) mView.findViewById(R.id.mem_use_rate);
        scaleScreenSize(mainLayout);
    }

    public void showPopup() {
        showAtLocation(mView, Gravity.CENTER, 0, 0);
    }

    public static void startMediaCodingTest(int captureformat, ProductionTesting.VideoFormat deformat, int decoCount, List<ProductionTesting.VideoFormat> encodings) {
        ProductionTesting.getInstance().startMediaCompositiveTesting(captureformat, deformat, decoCount, encodings);
    }

    /**
     * 测试编码效果，解码-编码-解码-显示
     */
    public void startEnCodingTest() {
        showAtLocation(mView, Gravity.CENTER, 0, 0);
        onEnCodingTestShow();
        ProductionTesting.getInstance().startEncodedTesting1();
    }


    /**
     * 编码效果测试中界面显示
     */
    private void onEnCodingTestShow() {
        mainLayout.setVisibility(View.GONE);
        mEncodingTxt.setVisibility(View.VISIBLE);
    }

    private void onMediaCodingShow() {
        mEncodingTxt.setVisibility(View.GONE);
        mainLayout.setVisibility(View.VISIBLE);
    }

    public void dismissPopup() {
        dismiss();
        mViewMap.clear();
        ProductionTesting.getInstance().stopMediaTesting();
    }


    @Override
    public void onHardwareStatis(ProductionTesting.TestingType type, ProductionTesting.HardwareStatis statis) {
        Message msg = Message.obtain();
        msg.obj = statis;
        msg.what = MSG_SHOW_HARDWARE_RESULT;
        handler.sendMessage(msg);
    }

    /**
     * @param statis 硬件相关参数，如CPU,内存
     */
    private void showHardWareText(ProductionTesting.HardwareStatis statis) {
        String cpurate = df.format(statis.cupUsingRate);
        String cputemp = df.format(statis.cpuTemperature);
        String memrate = df.format(statis.memUsingRate);
        LogManager.d(TAG, "cpu use: " + cpurate + "cpu temp :" + cputemp + " memUse: " + memrate);
        StringBuilder mCpuUsingRateBuilder = new StringBuilder(cpurate);
        mCpuUsingRateBuilder.append("%");
        mCpuUseRate.setText(mCpuUsingRateBuilder.toString());
        StringBuilder mCpuTempBuilder = new StringBuilder(cputemp);
        mCpuTempBuilder.append("℃");
        mCpuTemp.setText(mCpuTempBuilder.toString());
        StringBuilder memUsingRateBuilder = new StringBuilder(memrate);
        memUsingRateBuilder.append("%");
        memUseRate.setText(memUsingRateBuilder.toString());

    }

    @Override
    public void onOutputAdded(ProductionTesting.TestingType type, int id, String url, int result, int width, int height) {
        if (type == ProductionTesting.TestingType.PRODUCTION_TESTING_ENCODED) {
            handler.sendEmptyMessageDelayed(MSG_REMOVE_OUTPUT, MSG_DELAY);
        }
        sendPxShowMessage(id, width, height);
    }

    /**
     * @param id     解码id
     * @param width  宽
     * @param height 高
     */
    @Override
    public void onSourceResolutionChanged(int id, int width, int height) {
        LogManager.d(TAG, "onSourceResolutionChanged id: " + id + " width: " + width + " height: " + height);
        sendPxShowMessage(id, width, height);
    }

    private void sendPxShowMessage(int id, int width, int height) {
        Bundle bundle = new Bundle();
        bundle.putInt("id", id);
        bundle.putInt("width", width);
        bundle.putInt("height", height);
        Message msg = Message.obtain();
        msg.obj = bundle;
        msg.what = MSG_SHOW_PX_CHANGE;
        handler.sendMessage(msg);
    }

    @Override
    public void onSourceStatis(int id, float fps, int kbps) {
        LogManager.d(TAG, "onSourceStatis id: " + id + " fps: " + fps + " kbps: " + kbps);
        sendCodecMessage(id, fps, kbps, true);
    }

    @Override
    public void onOutputStatis(int id, float fps, int kbps) {
        LogManager.d(TAG, "onOutputStatis id: " + id + " fps: " + fps + " kbps: " + kbps);
        sendCodecMessage(id, fps, kbps, false);
    }

    @Override
    public void onVideoRendererStatis(float fps) {
        Message message = Message.obtain();
        Bundle bundle = new Bundle();
        bundle.putFloat("fps", fps);
        message.obj = bundle;
        message.what = MSG_SHOW_COMPLEX_FBPS;
        handler.sendMessage(message);
    }

    /**
     * @param id       编解码信息id
     * @param fbps     帧率
     * @param kbps     码率
     * @param decoding 是否是解码
     */
    private void sendCodecMessage(int id, float fbps, float kbps, boolean decoding) {
        Message msg = Message.obtain();
        Bundle bundle = new Bundle();
        bundle.putInt("id", id);
        bundle.putFloat("fps", fbps);
        bundle.putFloat("kbps", kbps);
        if (decoding) {
            bundle.putBoolean("decoding", true);
        } else {
            bundle.putBoolean("decoding", false);
        }
        msg.what = MSG_SHOW_CODING_RESULT;
        msg.obj = bundle;
        handler.sendMessage(msg);
    }

    private void setCodecStatis(Bundle bundle) {
        if (mViewMap == null) {
            return;
        }
        int id = bundle.getInt("id");
        float fps = bundle.getFloat("fps");
        float kbps = bundle.getFloat("kbps");
        String fpsStr = df.format(fps);
        String kbpsStr = df.format(kbps);
        boolean decoding = bundle.getBoolean("decoding", true);
        MediaCodecItemView itemView;
        if (mViewMap.containsKey(id)) {
            itemView = (MediaCodecItemView) mViewMap.get(id);
        } else {
            itemView = new MediaCodecItemView(mContext);
            mainLayout.addView(itemView, getLayoutParams());
            mViewMap.put(id, itemView);
        }
        if (decoding) {
            itemView.setCodecType(true);
        } else {
            itemView.setCodecType(false);
           // itemView.setEnCodingTxt(mEnCodeFormat);
        }
        StringBuilder mDecodingBpsBuilder = new StringBuilder(kbpsStr);
        mDecodingBpsBuilder.append(" ").append("kbps");
        itemView.setCodecStatisText(mDecodingBpsBuilder.toString(), fpsStr);
    }

    private void setPxStatis(Bundle bundle) {
        if (mViewMap == null) {
            return;
        }
        int id = bundle.getInt("id");
        int width = bundle.getInt("width");
        int height = bundle.getInt("height");
        MediaCodecItemView itemView;
        if (mViewMap.containsKey(id)) {
            itemView = (MediaCodecItemView) mViewMap.get(id);
        } else {
            itemView = new MediaCodecItemView(mContext);
            mainLayout.addView(itemView, getLayoutParams());
            mViewMap.put(id, itemView);
        }
        StringBuilder mdecodingBuilder = new StringBuilder();
        mdecodingBuilder.append(width).append("*").append(height);
        itemView.setPxText(mdecodingBuilder.toString());
    }

    private void setMixedFps(Bundle bundle) {
        float fps = bundle.getFloat("fps");
        String fpsStr = df.format(fps);
        mMixedFps.setText(fpsStr);
    }

    private LinearLayout.LayoutParams getLayoutParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 0);
        lp.weight = 1;
        lp.setLayoutDirection(LinearLayout.HORIZONTAL);
        return lp;
    }

    private void scaleScreenSize(ViewGroup viewGroup) {
        int textWidth = ScreenUtils.getScreenWidth(mContext) / 8;
        float textSize = ScreenUtils.getScreenWidth(mContext) / 50;
        int count = viewGroup.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = viewGroup.getChildAt(i);
            if (child instanceof ViewGroup) {
                scaleScreenSize((ViewGroup) child);
            } else if (child instanceof TextView) {
                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) child.getLayoutParams();
                layoutParams.width = textWidth;
                ((TextView) child).setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
            }
        }
    }

}
