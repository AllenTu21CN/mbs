package sanp.tools.utils;

import android.util.Log;

import org.greenrobot.eventbus.EventBus;

/**
 * 日志打印类 发布时建议关闭日志打印
 */
public class LogManager {
    /**
     * 日志打印开关
     */
    private final static boolean IS_DEBUG_ON = true;
    public final static String APP_TAG = "MPX";

    public final static String LOG_TAG = "";

    public static void v(String text) {
        if (IS_DEBUG_ON) {
            Log.v(APP_TAG, text);
            sendMsgToDebug(APP_TAG,text);
        }
    }

    public static void a(String text) {
        if (IS_DEBUG_ON) {
            Log.i("ACTION", LOG_TAG+text);
            sendMsgToDebug(APP_TAG,text);
        }
    }

    public static void v(String tag, String text) {
        if (IS_DEBUG_ON) {
            Log.v(tag, LOG_TAG+text);
            sendMsgToDebug(APP_TAG,text);
        }
    }

    public static void i(String text) {
        if (IS_DEBUG_ON) {
            Log.i(APP_TAG, LOG_TAG+text);
            sendMsgToDebug(APP_TAG,text);
        }
    }

    public static void i(String tag, String text) {
        if (IS_DEBUG_ON) {
            Log.i(tag, LOG_TAG+text);
            sendMsgToDebug(APP_TAG,text);
        }
    }

    public static void d(String text) {
        if (IS_DEBUG_ON) {
            Log.d(APP_TAG, LOG_TAG+text);
            sendMsgToDebug(APP_TAG,text);
        }
    }

    public static void d(String tag, String text) {
        if (IS_DEBUG_ON) {
            Log.d(tag, LOG_TAG+text);
            sendMsgToDebug(tag,text);
        }
    }

    public static void w(String text) {
        if (IS_DEBUG_ON) {
            Log.w(APP_TAG, LOG_TAG+text);
            sendMsgToDebug(APP_TAG,text);
        }
    }

    public static void w(String tag, String text) {
        if (IS_DEBUG_ON) {
            Log.w(tag, LOG_TAG+text);
            sendMsgToDebug(tag,text);
        }
    }

    public static void e(String tag, String text) {
        if (IS_DEBUG_ON) {
            Log.e(tag, text);
            sendMsgToDebug(tag,text);
        }
    }

    public static void e(String text) {
        Log.e(APP_TAG, text);
        sendMsgToDebug(APP_TAG,text);
    }


    public static boolean isStart = false;

    private static void sendMsgToDebug(String log,String text) {
        if (!isStart) {
            return;
        }
        DebugInfo info = new DebugInfo();
        info.setTag(log);
        info.setContent(text);
        EventBus.getDefault().post(info);
    }

    public static void e(Throwable tr) {
        Log.e(APP_TAG, tr.getMessage(), tr);
    }

    public static void e(String text, Throwable tr) {
        Log.e(APP_TAG, text, tr);
    }

    public static void e(String tag, String text, Throwable tr) {
        Log.e(tag, text, tr);
    }


    public static class DebugInfo {
        String tag;
        String content;

        public String getTag() {
            return tag;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

}
