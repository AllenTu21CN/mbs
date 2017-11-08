package sanp;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

import sanp.multimedia.R;
import sanp.tools.utils.FileSaveUtils;
import sanp.tools.utils.LogManager;

import sanp.javalon.network.protocol.RTMPBandwidthTest;
import sanp.test.MediaTesting;

/**
 * Created by Tuyj on 2017/11/7.
 */

public class InitMultimedia {

    public static String AppHomePath = null;
    public static String ModuleHomePath = null;

    private static final Map<String, Integer> VIDEO_SAMPLE_FILES = new HashMap<String, Integer>() {{
        put(MediaTesting.TEST_1080P_24FPS_2MBPS_32SECS, R.raw.test_1080p_24fps_2mbps_32secs);
        put(MediaTesting.TEST_720P_24FPS_1MBPS_17SECS, R.raw.test_720p_24fps_1mbps_17secs);
    }};

    private static final Map<String, Integer> SPEED_SAMPLE_FILES = new HashMap<String, Integer>() {{
        put(RTMPBandwidthTest.SPEED_SAMPLE_H264, R.raw.speed0_h264);
        put(RTMPBandwidthTest.SPEED_SAMPLE_H264_IDX, R.raw.speed0_h264_idx);
        put(RTMPBandwidthTest.SPEED_SAMPLE_H264_SPS, R.raw.speed0_h264_sps);
    }};

    public static void init(Context context, String appHomePath) {
        AppHomePath = appHomePath;
        ModuleHomePath = AppHomePath + "/mm";
        saveVideoSamplesToStorage(context);
        saveSpeedSamplesToStorage(context);
    }

    private static void saveVideoSamplesToStorage(Context context) {
        for(String filename: VIDEO_SAMPLE_FILES.keySet()) {
            try {
                FileSaveUtils.saveToSDCard(context, ModuleHomePath, filename, VIDEO_SAMPLE_FILES.get(filename));
            } catch (Throwable e) {
                LogManager.e("saveVideoSamplesToStorage error " + e);
            }
        }
    }

    private static void saveSpeedSamplesToStorage(Context context) {
        for(String filename: SPEED_SAMPLE_FILES.keySet()) {
            try {
                FileSaveUtils.saveToSDCard(context, ModuleHomePath, filename, SPEED_SAMPLE_FILES.get(filename));
            } catch (Throwable e) {
                LogManager.e("saveSpeedSamplesToStorage error " + e);
            }
        }
    }
}
