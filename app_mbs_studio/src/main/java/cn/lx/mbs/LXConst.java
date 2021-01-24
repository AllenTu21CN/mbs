package cn.lx.mbs;

import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import com.google.gson.Gson;
import com.sanbu.media.AACProfile;
import com.sanbu.media.AudioFormat;
import com.sanbu.media.AudioSamplerate;
import com.sanbu.media.Bandwidth;
import com.sanbu.media.CodecType;
import com.sanbu.media.H264Profile;
import com.sanbu.media.Resolution;
import com.sanbu.media.VideoFormat;
import com.sanbu.tools.MessageBox;

import com.sanbu.base.Callback;
import com.sanbu.base.Result;
import com.sanbu.base.Tuple;
import com.sanbu.tools.FileSaveUtil;
import com.sanbu.tools.LogUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.lx.mbs.support.structures.AudioCodec;
import cn.lx.mbs.support.structures.MixMode;
import cn.lx.mbs.support.structures.RecProp;
import cn.sanbu.avalon.endpoint3.EPConst;
import cn.sanbu.avalon.endpoint3.structures.RecFileFormat;
import cn.sanbu.avalon.endpoint3.structures.jni.EPFixedConfig;
import cn.sanbu.avalon.endpoint3.structures.jni.RecSplitMode;
import cn.sanbu.avalon.endpoint3.structures.jni.Reconnecting;
import cn.sanbu.avalon.endpoint3.utils.SysResChecking;

public class LXConst {

    public static final String TAG = "app_mbs";

    ////////////////////////////////// paths

    public static final String APP_EXTERNAL_PATH = Environment.getExternalStorageDirectory() + "/lx/mbs";
    public static final String SP_PATH = APP_EXTERNAL_PATH + "/preference";
    public static final String TEMP_PATH = APP_EXTERNAL_PATH + "/tmp";
    public static final String LOG_PATH = APP_EXTERNAL_PATH + "/log";
    public static final String DB_PATH = APP_EXTERNAL_PATH + "/db";
    public static final String UPGRADE_PATH = APP_EXTERNAL_PATH + "/upgrade";
    public static final String RESOURCE_PATH = APP_EXTERNAL_PATH + "/resource";
    public static final String IMAGE_PATH = RESOURCE_PATH + "/image";
    public static final String MEDIA_PATH = APP_EXTERNAL_PATH + "/media";

    ////////////////////////////////// SP

    public static final String SP_COMMON_NAMESPACE = "cn.lx.mbs.comm";
    public static final String SP_COMMON_FILE_PATH = SP_PATH + "/" + SP_COMMON_NAMESPACE + ".xml";

    public static final String SP_PERSONALIZED_NAMESPACE = "cn.lx.mbs.per";
    public static final String SP_PERSONALIZED_FILE_PATH = SP_PATH + "/" + SP_PERSONALIZED_NAMESPACE + ".xml";

    public static final String SP_KEY_CORE_SOURCES = "core_sources";
    public static final String SP_KEY_CORE_SR_AUDIO_FORMAT = "core_sr_audio_format";
    public static final String SP_KEY_CORE_SR_S_VIDEO_FORMAT = "core_sr_s_video_format";
    public static final String SP_KEY_CORE_SR_R_VIDEO_FORMAT = "core_sr_r_video_format";
    public static final String SP_KEY_CORE_SR_S_URLS = "core_sr_s_urls";
    public static final String SP_KEY_CORE_SR_R_PROP = "core_sr_r_prop";

    ///////////////// resources and assets

    // 内置的背景图片
    public static final String BG_IMAGE_LOADING = IMAGE_PATH + "/loading_720p.png";
    public static final String BG_IMAGE_NONE = IMAGE_PATH + "/none_720p.png";

    // 测试MP4
    public static final String TEST_MP4 = MEDIA_PATH + "/test.mp4";

    // 须复制到存储的资源列表
    public static final Map<String/*toFile*/, Tuple<Integer/*resId*/, FileSaveUtil.Action>> FIXED_RESOURCE = new HashMap<String, Tuple<Integer, FileSaveUtil.Action>>() {{
        put(BG_IMAGE_LOADING, new Tuple<>(R.raw.loading_720p, FileSaveUtil.Action.Smart));
        put(BG_IMAGE_NONE, new Tuple<>(R.raw.none_720p, FileSaveUtil.Action.Smart));
        put(TEST_MP4, new Tuple<>(R.raw.test, FileSaveUtil.Action.Simple));
    }};

    // 须复制到存储的Assert列表
    public static final Map<String/*toFile*/, Tuple<String/*asset*/, FileSaveUtil.Action>> BUILD_IN_ASSETS = new HashMap<String, Tuple<String, FileSaveUtil.Action>>() {{
    }};

    ////////////////////////////////// 静态定义

    // 非实时工作队列线程名
    public static final String CHILD_THREAD_NAME = "RTWorker@MBS";

    // 必须要独占的系统资源
    public static final List<SysResChecking.Port> REQUIRED_PORTS = Arrays.asList(
            new SysResChecking.Port("tcp", "40000"),
            new SysResChecking.Port("tcp", "30001")
    );

    // 通过LogUtil允许输出的日志级别
    public static final int LOG_UTIL_ENABLED_OUTPUT_LEVEL = Log.DEBUG;

    // 是否记录操作日志
    private static final boolean LOG_ACTION = true;
    // 操作日志级别
    private static final int LOG_ACTION_LEVEL = Log.INFO;
    // 是否记录操作参数
    private static final boolean LOG_ACTION_PARAMS = true;

    // 消息盒中最多记录数
    public static final int MAX_MESSAGE_BOX_SIZE = 100;

    // 是否使用内部NoSignal
    public static final boolean USING_INTERNAL_NO_SIGNAL_IMG = false;

    // 网络重连参数
    public static final Reconnecting SOURCE_RECONNECTING = new Reconnecting(
            -1, 3000, 8000, 100);

    ////////////////////////////////// 默认值

    // 默认推流录制音频格式
    public static final AudioFormat DEFAULT_SR_AUDIO_FORMAT = new AudioFormat(CodecType.AAC,
            AudioSamplerate.HZ_48K, 2, Bandwidth._128K, AACProfile.LC);

    // 默认推流视频格式
    public static final VideoFormat DEFAULT_SR_S_VIDEO_FORMAT = new VideoFormat(CodecType.H264,
            H264Profile.Main, Resolution.RES_720P, 30, Bandwidth._2M, 5);

    // 默认录制视频格式
    public static final VideoFormat DEFAULT_SR_R_VIDEO_FORMAT = new VideoFormat(CodecType.H264,
            H264Profile.Main, Resolution.RES_1080P, 30, Bandwidth._4M, 10);

    // 默认录制属性
    public static final RecProp DEFAULT_SR_REC_PROP = new RecProp(MEDIA_PATH, RecFileFormat.MP4,
            "%N-%T-%t", RecSplitMode.BySize, 512 * 1024 * 1024
    );

    // 默认EP固定配置
    public static final EPFixedConfig DEFAULT_EP_FIXED_CONFIG = new EPFixedConfig(
            true, true, true,
            15060, 11720, 17070, 20000, 21999,
            20000, 21999, "", "",
            1, true, "INFO", "#0F0F0F"
    );

    // 默认MIC的混音模式(输出)
    public static final MixMode DEFAULT_MIC_MIX_MODE_4_OUTPUT = MixMode.On;

    // 默认呼叫音频格式
    public static final List<AudioCodec> DEFAULT_CALLING_AUDIO_CODECS = Arrays.asList(AudioCodec.G711A, AudioCodec.G711U);

    ///////////////// util functions

    public static void callbackSuccess(Callback callback) {
        callbackSuccess(null, callback);
    }

    public static void callbackSuccess(Handler handler, Callback callback) {
        callbackResult(handler, callback, Result.SUCCESS);
    }

    public static void callbackError(Callback callback, String action, int code, String message, String hint) {
        callbackError(null, callback, action, code, message, hint);
    }

    public static void callbackError(Handler handler, Callback callback, String action, int code, String message, String hint) {
        LogUtil.w(TAG, String.format("%scode: %d message: %s", (action == null ? "" : action + ", "), code, message));
        callbackResult(handler, callback, new Result(code, hint));
    }

    public static void callbackResult(Callback callback, Result result) {
        callbackResult(null, callback, result);
    }

    public static void callbackResult(Handler handler, Callback callback, Result result) {
        if (callback == null)
            return;
        if (handler == null)
            callback.done(result);
        else
            handler.post(() -> callback.done(result));
    }

    public static void logAction(String subTag, String action, Object... params) {
        logAction(TAG, subTag, action, params);
    }

    public static void logAction(String tag, String subTag, String action, Object... params) {
        if (LOG_ACTION) {
            String message = subTag + ", " + action;

            if (LOG_ACTION_PARAMS) {
                message += ": ";
                for (Object param : params)
                    message += new Gson().toJson(param) + ", ";
            } else {
                message += " ...";
            }

            if (message.length() > EPConst.LOG_LINE_BUFFER_LEN) {
                int offset = 0;
                int left = message.length();
                int line = 0;
                int max = (left / EPConst.LOG_LINE_BUFFER_LEN) + (left % EPConst.LOG_LINE_BUFFER_LEN == 0 ? 0 : 1);

                LogUtil.log(LOG_ACTION_LEVEL, tag, "logAction size: "+ left);
                do {
                    ++line;
                    int len = Math.min(left, EPConst.LOG_LINE_BUFFER_LEN);
                    String sub = "line " + line + "/" + max + ": " + message.substring(offset, offset + len);
                    offset += len;
                    left -= len;
                    LogUtil.log(LOG_ACTION_LEVEL, tag, sub);
                } while(left > 0);
            } else {
                LogUtil.log(LOG_ACTION_LEVEL, tag, message);
            }
        }
    }

    public static MessageBox gMessageBox = new MessageBox(MAX_MESSAGE_BOX_SIZE);

}
