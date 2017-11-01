package sanp.mp100.test.utils;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sanp.avalon.libs.SimpleTesting;
import sanp.avalon.libs.base.utils.FileUtils;
import sanp.avalon.libs.base.utils.HardwareStatisHelper;
import sanp.avalon.libs.base.utils.LogManager;
import sanp.avalon.libs.media.base.AVDefines;
import sanp.mp100.MP100Application;
import sanp.mp100.integration.RBUtil;
import sanp.mpx.mc.MediaController;
import sanp.mpx.mc.MediaEngine;
import sanp.mpx.mc.ScreenLayout;

/**
 * Created by Tuyj on 2017/7/17.
 */

public class ProductionTesting implements MediaController.Observer {

    private static final String TAG = "ProductionTesting";

    public enum TestingType {
        PRODUCTION_TESTING_UNKNOW,
        PRODUCTION_TESTING_CAPTURE,
        PRODUCTION_TESTING_MEDIACOMPOSITIVE,
        PRODUCTION_TESTING_ENCODED,
    }

    // 编解码统计信息
    public static class CodingStatis {
        public int decodingWidth;          //解码分辨率-宽
        public int decodingHeight;         //解码分辨率-高
        public int decodingBitrateKbps;    //解码带宽(kbps)
        public float decodingFps;          //解码帧率

        public int encodingWidth;          //编码分辨率-宽
        public int encodingHeight;         //编码分辨率-高
        public int encodingBitrateKbps;    //编码带宽(kbps)
        public float encodingFps;          //编码帧率

        public float mixFps;               //合成帧率

        public CodingStatis() {
            clear();
        }

        public void clear() {
            decodingWidth = 0;
            decodingHeight = 0;
            decodingBitrateKbps = 0;
            decodingFps = 0;
            encodingWidth = 0;
            encodingHeight = 0;
            encodingBitrateKbps = 0;
            encodingFps = 0;
            mixFps = 0;
        }
    }

    // 硬件统计信息
    public static class HardwareStatis {
        public float cupUsingRate;      //CPU使用率
        public float cpuTemperature;    //CPU温度
        public float memUsingRate;      //内存使用率
    }
    public interface Callback {
        void onSourceStatis(int id, float fps, int kbps);

        void onOutputStatis(int id, float fps, int kbps);

        void onVideoRendererStatis(float fps);

        void onHardwareStatis(TestingType type, HardwareStatis statis);

        void onOutputAdded(TestingType type, int id, String url, int result, int width, int height);

        void onSourceResolutionChanged(int id, int width, int height);
    }

    public static final int CAPTURE_DEVICE_NONE = 0x00;
    public static final int CAPTURE_DEVICE_CAMERA0 = 0x01;
    public static final int CAPTURE_DEVICE_CAMERA1 = 0x02;
    public static final int CAPTURE_DEVICE_BOTH = CAPTURE_DEVICE_CAMERA0 | CAPTURE_DEVICE_CAMERA1;

    public enum VideoFormat {
        FORMAT_1080P_4M_30FPS,
        FORMAT_1080P_2M_30FPS,
        FORMAT_1080P_2M_25FPS,
        FORMAT_1080P_1M_10FPS,
        FORMAT_720P_2M_30FPS,
        FORMAT_720P_1M_30FPS,
        FORMAT_720P_1M_25FPS,
        FORMAT_720P_1M_10FPS,
        FORMAT_480P_1M_30FPS,
        FORMAT_480P_512K_25FPS,
    }

    public static final VideoFormat DEFAULT_1080P_FORMAT = VideoFormat.FORMAT_1080P_2M_25FPS;
    public static final VideoFormat DEFAULT_720P_FORMAT = VideoFormat.FORMAT_720P_1M_25FPS;
    public static final VideoFormat DEFAULT_480P_FORMAT = VideoFormat.FORMAT_480P_512K_25FPS;

    private static final String MP4_PATH =  MP100Application.TMP_FILE_PATH;

    private static Map<VideoFormat, String> gDecodingFiles = new HashMap<VideoFormat, String>() {{
        // put(VideoFormat.FORMAT_1080P_2M_25FPS, "rtsp://10.1.83.200:5000/main.h264");
        put(VideoFormat.FORMAT_1080P_2M_25FPS, MP4_PATH + "/test_1080p_24fps_2mbps_32secs.mp4?loop=true");
        put(VideoFormat.FORMAT_720P_1M_25FPS, MP4_PATH + "/test_720p_24fps_1mbps_17secs.mp4?loop=true");
    }};
    private static Map<Integer, String> gCaptureUrls = new HashMap<Integer, String>() {{
        put(CAPTURE_DEVICE_CAMERA0, "capture://none?type=video&id=0");
        put(CAPTURE_DEVICE_CAMERA1, "capture://none?type=video&id=1");
    }};
    private static Map<VideoFormat, MediaEngine.VideoSinkConfig> gFormatConfigs = new HashMap<VideoFormat, MediaEngine.VideoSinkConfig>() {{
        put(VideoFormat.FORMAT_1080P_4M_30FPS, new MediaEngine.VideoSinkConfig(MediaEngine.VideoSinkConfig.MIME_TYPE_H264, 1920, 1088, 30, 4 * 1024 * 1024, 10));
        put(VideoFormat.FORMAT_1080P_2M_30FPS, new MediaEngine.VideoSinkConfig(MediaEngine.VideoSinkConfig.MIME_TYPE_H264, 1920, 1088, 30, 2 * 1024 * 1024, 10));
        put(VideoFormat.FORMAT_1080P_2M_25FPS, new MediaEngine.VideoSinkConfig(MediaEngine.VideoSinkConfig.MIME_TYPE_H264, 1920, 1088, 25, 2 * 1024 * 1024, 10));
        put(VideoFormat.FORMAT_1080P_1M_10FPS, new MediaEngine.VideoSinkConfig(MediaEngine.VideoSinkConfig.MIME_TYPE_H264, 1920, 1088, 10, 1 * 1024 * 1024, 10));
        put(VideoFormat.FORMAT_720P_2M_30FPS, new MediaEngine.VideoSinkConfig(MediaEngine.VideoSinkConfig.MIME_TYPE_H264, 1280, 720, 30, 2 * 1024 * 1024, 10));
        put(VideoFormat.FORMAT_720P_1M_30FPS, new MediaEngine.VideoSinkConfig(MediaEngine.VideoSinkConfig.MIME_TYPE_H264, 1280, 720, 30, 1 * 1024 * 1024, 10));
        put(VideoFormat.FORMAT_720P_1M_25FPS, new MediaEngine.VideoSinkConfig(MediaEngine.VideoSinkConfig.MIME_TYPE_H264, 1280, 720, 25, 1 * 1024 * 1024, 10));
        put(VideoFormat.FORMAT_720P_1M_10FPS, new MediaEngine.VideoSinkConfig(MediaEngine.VideoSinkConfig.MIME_TYPE_H264, 1280, 720, 10, 512 * 1024, 10));
        put(VideoFormat.FORMAT_480P_1M_30FPS, new MediaEngine.VideoSinkConfig(MediaEngine.VideoSinkConfig.MIME_TYPE_H264, 856, 480, 30, 1 * 1024 * 1024, 10));
        put(VideoFormat.FORMAT_480P_512K_25FPS, new MediaEngine.VideoSinkConfig(MediaEngine.VideoSinkConfig.MIME_TYPE_H264, 856, 480, 25, 512 * 1024, 10));
    }};
    private static String gEncodingFilePath = MP100Application.HOME_EXTERNAL_PATH + "/pt";

    private static final String ENCODING_MEDIA_TEST = gEncodingFilePath + "/test-0.mp4" + "?loop=true";

    private static ProductionTesting gProductionTesting = null;

    public static ProductionTesting getInstance() {
        if (gProductionTesting == null) {
            synchronized (ProductionTesting.class) {
                if (gProductionTesting == null) {
                    gProductionTesting = new ProductionTesting();
                }
            }
        }
        return gProductionTesting;
    }

    private MediaController mMediaController = null;
    private Callback mCallback = null;
    private TestingType mTestingType = TestingType.PRODUCTION_TESTING_UNKNOW;
    private float mLastTotalCpuTime = 0;
    private float mLastProcessCpuTime = 0;
    private Thread mStatisThread = null;
    private boolean mRunning = false;
    private Context mContext = null;
    private Map<Integer, VideoFormat> mOutputs = new HashMap<>();

    private ProductionTesting() {
    }

    public void entryTesting() {
        new File(gEncodingFilePath).mkdir();
        mMediaController = MediaController.getInstance();
        mMediaController.removeObserver(RBUtil.getInstance());
        mMediaController.clean();
        mOutputs.clear();
        mMediaController.enableDisplayTestingItems(true);
        mMediaController.addObserver(this);
        startStatisThread();
    }

    public void exitTesting() {
        stopStatisThread();
        mMediaController.removeObserver(this);
        mMediaController.clean();
        mOutputs.clear();
        mMediaController.enableDisplayTestingItems(false);
        cleanTmpDir();
    }

    public void setContext(Context context) {
        mContext = context;
    }

    public void setCallback(Callback cb) {
        mCallback = cb;
    }

    public void cleanTmpDir() {
        FileUtils.deleteDir(gEncodingFilePath);
    }

    /**
     * 结束媒体测试
     */
    public void stopMediaTesting() {
        mMediaController.clean();
        mOutputs.clear();
        mCallback = null;
        mTestingType = TestingType.PRODUCTION_TESTING_UNKNOW;
    }

    public void clean() {
        mMediaController.clean();
        mOutputs.clear();
    }

    /**
     * 开始采集测试: HDMI/Camera - 显示
     */
    public void startCaptureTesting(int captureDevices) {
        startMediaCompositiveTesting(captureDevices, DEFAULT_1080P_FORMAT, 0, DEFAULT_1080P_FORMAT, 0, TestingType.PRODUCTION_TESTING_CAPTURE);
    }

    /**
     * 开始多媒体综合测试: 采集(设备值) + 解码*数量 -> 合成 -> 显示 + 编码*数量
     */
    public void startMediaCompositiveTesting(int captureDevices, VideoFormat decodingFormat, int decodingCnt, VideoFormat encodingFormat, int encodingCnt) {
        startMediaCompositiveTesting(captureDevices, decodingFormat, decodingCnt, encodingFormat, encodingCnt, TestingType.PRODUCTION_TESTING_MEDIACOMPOSITIVE);
    }

    public void startMediaCompositiveTesting(int captureDevices, VideoFormat decodingFormat, int decodingCnt, List<VideoFormat> encodings) {
        startMediaCompositiveTesting(captureDevices, decodingFormat, decodingCnt, encodings, TestingType.PRODUCTION_TESTING_MEDIACOMPOSITIVE);
    }

    private void startMediaCompositiveTesting(int captureDevices, VideoFormat decodingFormat, int decodingCnt, VideoFormat encodingFormat, int encodingCnt, TestingType type) {
        List<VideoFormat> formats = new ArrayList<>();
        for(int i = 0 ; i < encodingCnt ; ++i) formats.add(encodingFormat);
        startMediaCompositiveTesting(captureDevices, decodingFormat, decodingCnt, formats, type);
    }

    private void startMediaCompositiveTesting(int captureDevices, VideoFormat decodingFormat, int decodingCnt,List<VideoFormat> encodings, TestingType type) {
        mTestingType = type;
        // 添加采集源
        if ((captureDevices & CAPTURE_DEVICE_CAMERA0) != 0) {
              mMediaController.addSource(gCaptureUrls.get(CAPTURE_DEVICE_CAMERA0),  Arrays.asList(AVDefines.DataType.VIDEO), MediaController.RECOMMENDED_REOPEN_CNT);
        }
        if ((captureDevices & CAPTURE_DEVICE_CAMERA1) != 0) {
             mMediaController.addSource(gCaptureUrls.get(CAPTURE_DEVICE_CAMERA1),  Arrays.asList(AVDefines.DataType.VIDEO), MediaController.RECOMMENDED_REOPEN_CNT);
        }

        // 添加解码源
        String decodingFile = gDecodingFiles.get(decodingFormat);
        if (decodingFile == null)
            throw new RuntimeException("non-supported decoding format:" + decodingFormat);
        for (int i = 0; i < decodingCnt; ++i) {
             mMediaController.addSource(decodingFile,  Arrays.asList(AVDefines.DataType.VIDEO), MediaController.RECOMMENDED_REOPEN_CNT);
        }

        // 添加编码输出
        int i = 0;
        for(VideoFormat format: encodings) {
            MediaEngine.VideoSinkConfig video = gFormatConfigs.get(format);
            int id = mMediaController.addOutput(gEncodingFilePath + "/test-" + i + ".mp4", video, MediaController.RECOMMENDED_REOPEN_CNT);
            if(id >= 0)
                mOutputs.put(id, format);
            video.bitrate += 10;
            ++i;
        }
    }

    /**
     * 开始编码回显测试1:解码-编码(test-0.mp4)
     */
    public void startEncodedTesting1() {
        startMediaCompositiveTesting(CAPTURE_DEVICE_NONE, DEFAULT_1080P_FORMAT, 1, DEFAULT_1080P_FORMAT, 1, TestingType.PRODUCTION_TESTING_ENCODED);
    }

    /**
     * 开始编码回显测试2:解码(test-0.mp4)--显示
     */
    public void startEncodedTesting2() {
        mTestingType = TestingType.PRODUCTION_TESTING_ENCODED;
        mMediaController.addSource(ENCODING_MEDIA_TEST,  Arrays.asList(AVDefines.DataType.VIDEO), MediaController.RECOMMENDED_REOPEN_CNT);
    }

    private void startStatisThread() {
        if (mStatisThread == null) {
            mStatisThread = new Thread(new Runnable() {
                public void run() {
                    doStatis();
                }
            }, "ProductionTesting Thread");
            mRunning = true;
            mStatisThread.start();
        }
    }

    private void stopStatisThread() {
        mRunning = false;
        if (mStatisThread != null) {
            try {
                mStatisThread.join();
                //mStatisThread.interrupt();
            } catch (InterruptedException e) {
                LogManager.e("Be interrupted while join statis thread");
                LogManager.e(e);
            }
            mStatisThread = null;
        }
    }

    private void doStatis() {
        while (mRunning) {
            if (mCallback != null) {
                if (mLastTotalCpuTime == 0 || mLastProcessCpuTime == 0) {
                    mLastTotalCpuTime = HardwareStatisHelper.getTotalCpuTime();
                    mLastProcessCpuTime = HardwareStatisHelper.getAppCpuTime();
                } else {
                    float totalCpuTime = HardwareStatisHelper.getTotalCpuTime();
                    float processCpuTime = HardwareStatisHelper.getAppCpuTime();
                    float cpuRate = 100 * (processCpuTime - mLastProcessCpuTime)
                            / (totalCpuTime - mLastTotalCpuTime);
                    mLastTotalCpuTime = totalCpuTime;
                    mLastProcessCpuTime = processCpuTime;
                    float memRate = 0;
                    if (mContext != null){
                        memRate = HardwareStatisHelper.getUsedPercentValue(mContext);
                    }
                    HardwareStatis hstatis = new HardwareStatis();
                    hstatis.cupUsingRate = cpuRate;
                    hstatis.cpuTemperature = HardwareStatisHelper.getDeviceTemperature();
                    hstatis.memUsingRate = memRate;
                    mCallback.onHardwareStatis(mTestingType, hstatis);
                }
            }
            try {
                Thread.sleep(5000/*ms*/);
            } catch (InterruptedException e) {
                LogManager.e(e);
                break;
            }
        }
    }

    @Override
    public void onSourceAdded(int id, String url, int result) {
        LogManager.d(TAG, "onSourceAdded :" + id);
        if (result == 0) {
            mMediaController.addSource2Scene(id);
            mMediaController.autoShow(ScreenLayout.LayoutMode.SYMMETRICAL, ScreenLayout.FillPattern.FILL_PATTERN_ADAPTING);
        }
    }
    @Override
    public void onSourceLost(int id, String url, int result) {
        mMediaController.cleanSceneSource(id);
        mMediaController.autoShow(ScreenLayout.LayoutMode.SYMMETRICAL, ScreenLayout.FillPattern.FILL_PATTERN_ADAPTING);
    }

    /**
     * 输入源分辨率改变回调
     *
     * @param id     source id
     * @param width  宽
     * @param height 高
     */
    @Override
    public void onSourceResolutionChanged(int id, int width, int height) {
        if (mCallback != null) {
            mCallback.onSourceResolutionChanged(id, width, height);
        } else {
            LogManager.d(TAG, "onSourceResolutionChanged id: " + id + " width: " + width + " height: " + height);
        }
    }

    /**
     * 输入源编码统计
     *
     * @param id   源id
     * @param fps  源帧率
     * @param kbps 源码率
     */
    @Override
    public void onSourceStatistics(int id, float fps, int kbps) {
        if (mCallback != null) {
            mCallback.onSourceStatis(id, fps, kbps);
        } else {
            LogManager.d(TAG, "onSourceStatistics id: " + id + " fps: " + fps + " kbps: " + kbps);
        }
    }

    @Override
    public void onOutputAdded(int id, String url, int result) {
        LogManager.d(TAG, "onOutputAdded :" + id);
        if(!mOutputs.containsKey(id)) {
            LogManager.w(TAG, "logical error: unknow output id-" + id);
            return;
        }
        VideoFormat format = mOutputs.get(id);
        MediaEngine.VideoSinkConfig video = gFormatConfigs.get(format);
        if (mCallback != null) {
            mCallback.onOutputAdded(mTestingType, id, url, result, video.width, video.height);
        } else {
            LogManager.i(String.format("Output(id-%d url-%s) added", id, url));
        }
    }

    @Override
    public void onOutputLost(int id, String url, int result) {
    }

    /**
     * 输出编码数据统计
     *
     * @param id   输出id
     * @param fps  输出帧率
     * @param kbps 输出码率
     */
    @Override
    public void onOutputStatistics(int id, float fps, int kbps) {
        if (mCallback != null) {
            mCallback.onOutputStatis(id, fps, kbps);
        } else {
            LogManager.d(TAG, "onOutputStatistics id: " + id + " fps: " + fps + " kbps: " + kbps);
        }
    }

    /**
     * 合成数据统计
     *
     * @param fps          帧率
     * @param droppedFrame 丢失帧率
     */
    @Override
    public void onVideoRendererStatistics(float fps, long droppedFrame) {
        if (mCallback != null) {
            CodingStatis cstatis = new CodingStatis();
            if (mTestingType == TestingType.PRODUCTION_TESTING_MEDIACOMPOSITIVE) {
                cstatis.decodingWidth = 0;
                cstatis.decodingHeight = 0;
                cstatis.decodingBitrateKbps = 0;
                cstatis.decodingFps = 0;
                cstatis.encodingWidth = 0;
                cstatis.encodingHeight = 0;
                cstatis.encodingBitrateKbps = 0;
                cstatis.encodingFps = 0;
                cstatis.mixFps = fps;
            } else if (mTestingType == TestingType.PRODUCTION_TESTING_ENCODED) {
                cstatis.decodingWidth = 0;
                cstatis.decodingHeight = 0;
                cstatis.decodingBitrateKbps = 0;
                cstatis.decodingFps = 0;
                cstatis.mixFps = fps;
            } else if (mTestingType == TestingType.PRODUCTION_TESTING_CAPTURE) {
                cstatis.mixFps = fps;
            }
            mCallback.onVideoRendererStatis(fps);
        } else {
            LogManager.d(String.format("fps: %.2f dropped: %d", fps, droppedFrame));
        }
    }

    public static class Tester implements SimpleTesting.Tester {
        ProductionTesting mProductionTesting = null;
        private int mTestingStep = -1;

        public void start(Object obj) {
            mProductionTesting = ProductionTesting.getInstance();
            mProductionTesting.setContext((Context) obj);
            mProductionTesting.entryTesting();
            mProductionTesting.startMediaCompositiveTesting(CAPTURE_DEVICE_NONE, DEFAULT_1080P_FORMAT, 2, DEFAULT_1080P_FORMAT, 1);
            mTestingStep = 4;
        }

        public void next() {
            if (mTestingStep == 4) {
                mProductionTesting.stopMediaTesting();
            } else if (mTestingStep == 3) {
                mProductionTesting.cleanTmpDir();
            } else {
                if (mProductionTesting != null) {
                    mProductionTesting.exitTesting();
                    mProductionTesting = null;
                }
                return;
            }
            --mTestingStep;
        }
    }

}
