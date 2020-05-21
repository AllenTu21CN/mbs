package cn.sanbu.avalon.endpoint3;

import android.util.Log;

import com.google.gson.Gson;
import com.sanbu.tools.LogUtil;

import cn.sanbu.avalon.endpoint3.structures.AudioInputDevice;
import cn.sanbu.avalon.endpoint3.structures.AudioOutputDevice;

public class EPConst {

    public static final String TAG = "endpoint3";

    public static final String EP_PROPERTY_BAND_WIDTH            = "bandwidth";
    public static final String EP_PROPERTY_AUDIO_CAPABILITIES    = "audio_capabilities";
    public static final String EP_PROPERTY_VIDEO_CAPABILITIES    = "video_capabilities";
    public static final String EP_PROPERTY_AGC_SWITCH            = "agc_switch";

    public static final String LOCAL_VIDEO_CAPTURE0   = "device://video/0";
    public static final String LOCAL_VIDEO_CAPTURE1   = "device://video/1";

    public static final AudioInputDevice LOCAL_DEFAULT_AUDIO_CAPTURE  = new AudioInputDevice(0, "device://audio/0",
            "Default Microphone", "默认的麦克风");
    public static final AudioOutputDevice LOCAL_DEFAULT_AUDIO_SPEAKER  = new AudioOutputDevice(0, "device://audio/0",
            "Default Speaker", "默认的扬声器");

    public static final float EP_DEFAULT_AUDIO_VOLUME = 1.0f;

    // __android_log_print buffer size
    public static final int LOG_PREFIX_STR_MAX_LEN = 128;
    public static final int LOG_TIME_STR_MAX_LEN = 32;
    public static final int LOG_LINE_BUFFER_LEN = 1024 - LOG_PREFIX_STR_MAX_LEN - LOG_TIME_STR_MAX_LEN;

    public static boolean isCapture(String url) {
        return url.startsWith("device://");
    }

    private static final boolean LOG_ACTION = true;
    private static final int LOG_ACTION_LEVEL = Log.DEBUG;
    private static final boolean LOG_ACTION_PARAMS = true;

    public static void logAction(String action, int result, Object... params) {
        logAction(TAG, LOG_ACTION_LEVEL, action, result, params);
    }

    public static void logAction(String tag, int level, String action, int result, Object... params) {
        if (LOG_ACTION) {
            String message = action;

            if (LOG_ACTION_PARAMS) {
                message += "(";
                for (Object param : params)
                    message += new Gson().toJson(param) + ", ";
                message += ")";
            } else {
                message += "(...)";
            }

            message += ", ret: " + result;

            if (message.length() > LOG_LINE_BUFFER_LEN) {
                int offset = 0;
                int left = message.length();
                int line = 0;
                int max = (left / LOG_LINE_BUFFER_LEN) + (left % LOG_LINE_BUFFER_LEN == 0 ? 0 : 1);

                LogUtil.log(level, tag, "logAction size: "+ left);
                do {
                    ++line;
                    int len = Math.min(left, LOG_LINE_BUFFER_LEN);
                    String sub = "line " + line + "/" + max + ": " + message.substring(offset, offset + len);
                    offset += len;
                    left -= len;
                    LogUtil.log(level, tag, sub);
                } while(left > 0);
            } else {
                LogUtil.log(level, tag, message);
            }
        }
    }
}
