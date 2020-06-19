package cn.lx.mbs.support.core;

import android.os.Handler;

import com.sanbu.base.Callback;
import com.sanbu.base.Result;
import com.sanbu.tools.StringUtil;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CoreUtils
{
    public static final String TAG = "ep_mbs";

    ///////////////// util functions

    public static void callbackSuccess(Callback callback) {
        callbackSuccess(null, callback);
    }

    public static void callbackSuccess(Handler handler, Callback callback) {
        callbackResult(handler, callback, Result.SUCCESS);
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

    // 填充格式化名称
    public static String fillFormatName(String format, String username, String title) {
        /*
        将format中的'%T'替换成title; 将'%N'替换成username;
        将'%t/.../' 替换成当前时间，时间格式是//之间的部分，如%t/yyyyMMdd/表示替换成时间20200116，若仅仅'%t'，表示使用默认格式yyyyMMdd.HHmmss
        * */
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd.HHmmss");
        final Date date = new Date();

        if (StringUtil.isEmpty(format))
            return "";
        if (username == null)
            username = "";
        if (title == null)
            title = "";

        // replace all %N and %T
        format = format.replace("%N", username);
        format = format.replace("%T", title);

        // replace all %t
        int index;
        while ((index = format.indexOf("%t")) >= 0) {
            String pattern = null;
            if (index + 2 < format.length()) {
                char f1 = format.charAt(index + 2);
                if (f1 == '/') {
                    index = index + 3;
                    int f2 = format.indexOf('/', index);
                    if (f2 > index)
                        pattern = format.substring(index, f2);
                }
            }

            if (pattern == null)
                format = format.replaceFirst("%t", sdf.format(date));
            else
                format = format.replaceFirst("%t/" + pattern + "/", new SimpleDateFormat(pattern).format(date));
        }
        return format;
    }
}
