package cn.lx.mbs.impl;

import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import com.google.gson.Gson;
import com.sanbu.tools.MessageBox;

import com.sanbu.base.Callback;
import com.sanbu.base.Result;
import com.sanbu.base.Tuple;
import com.sanbu.tools.FileSaveUtil;
import com.sanbu.tools.LogUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.sanbu.avalon.endpoint3.EPConst;
import cn.sanbu.avalon.endpoint3.utils.SysResChecking;

public class Const {

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

    ////////////////////////////////// SP

    public static final String SP_NAMESPACE = "cn.lx.mbs";

    public static final String SP_KEY_EP_BASE_CONFIG = "ep_base_config";
    public static final String SP_KEY_EP_FIXED_CONFIG = "ep_fixed_config";
    public static final String SP_KEY_EP_VIDEO_CAPABILITY = "ep_video_capability";
    public static final String SP_KEY_EP_AUDIO_CAPABILITY = "ep_audio_capability";
    public static final String SP_KEY_EP_H323_REG_STATE = "ep_h323_reg_state";
    public static final String SP_KEY_EP_SIP_REG_STATE = "ep_sip_reg_state";
    public static final String SP_KEY_EP_H323_REG_CONFIG = "ep_h323_reg_config";
    public static final String SP_KEY_EP_SIP_REG_CONFIG = "ep_sip_reg_config";

    ///////////////// resources and assets

    // 须复制到存储的资源列表
    public static final Map<String/*toFile*/, Tuple<Integer/*resId*/, FileSaveUtil.Action>> FIXED_RESOURCE = new HashMap<String, Tuple<Integer, FileSaveUtil.Action>>() {{
    }};

    // 须复制到存储的Assert列表
    public static final Map<String/*toFile*/, Tuple<String/*asset*/, FileSaveUtil.Action>> BUILD_IN_ASSETS = new HashMap<String, Tuple<String, FileSaveUtil.Action>>() {{
    }};

    ///////////////// DB

    public static final String DB_NAME = "mbs.db";
    public static final int DB_VERSION = 1;

    // 数据库表类
    public static final List<Class> DB_TABLE_LIST = new ArrayList<Class>() {{
    }};

    ////////////////////////////////// 默认值

    // 必须要独占的系统资源
    public static final List<SysResChecking.Port> REQUIRED_PORTS = Arrays.asList(
            // new SysResChecking.Port("tcp", "40000")
    );
    public static final List<Integer> REQUIRED_CAMERAS = Arrays.asList(0, 1);

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
