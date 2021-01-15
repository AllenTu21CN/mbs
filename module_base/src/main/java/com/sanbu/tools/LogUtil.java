package com.sanbu.tools;

import android.util.Log;

public class LogUtil {

    public static final String COMM_TAG = "DaPanJi";

    private static boolean OS_ANDROID = true;

    private static int mLogLevel = Log.VERBOSE;

    private static int mLogPublishLevel = Log.WARN;

    private static boolean mIsEnabledPublish = false;

    public static void initEnv(boolean isAndroidOS) {
        OS_ANDROID = isAndroidOS;
    }

    public static void setLogLevel(int level) {
        mLogLevel = level;
    }

    public static int getLogLevel() {
        return mLogLevel;
    }

    public static void setLogPublishLevel(int level) {
        mLogPublishLevel = level;
        if (mLogPublishLevel < mLogLevel)
            mLogPublishLevel = mLogLevel;
    }

    public static boolean isEnabledVerbose() {
        return mLogLevel <= Log.VERBOSE;
    }

    public static boolean isEnabledDebug() {
        return mLogLevel <= Log.DEBUG;
    }

    public static void v(String text) {
        v(COMM_TAG, text);
    }

    public static void v(String tag, String text) {
        if (mLogLevel <= Log.VERBOSE) {
            _v(tag, text);
            if (mIsEnabledPublish)
                publishLog(Log.VERBOSE, tag, text);
        }
    }

    public static void v(String tag, String subTag, String text) {
        v(tag, subTag + ":: " + text);
    }

    public static void v(String tag, StringBuilder subTags, String text) {
        subTags.append(":: ").append(text);
        v(tag, subTags.toString());
    }

    public static void d(String text) {
        d(COMM_TAG, text);
    }

    public static void d(String tag, String text) {
        if (mLogLevel <= Log.DEBUG) {
            _d(tag, text);
            if (mIsEnabledPublish)
                publishLog(Log.DEBUG, tag, text);
        }
    }

    public static void d(String tag, String subTag, String text) {
        d(tag, subTag + ":: " + text);
    }

    public static void d(String tag, StringBuilder subTags, String text) {
        subTags.append(":: ").append(text);
        d(tag, subTags.toString());
    }

    public static void i(String text) {
        i(COMM_TAG, text);
    }

    public static void i(String tag, String text) {
        if (mLogLevel <= Log.INFO) {
            _i(tag, text);
            if (mIsEnabledPublish)
                publishLog(Log.INFO, tag, text);
        }
    }

    public static void i(String tag, String subTag, String text) {
        i(tag, subTag + ":: " + text);
    }

    public static void i(String tag, StringBuilder subTags, String text) {
        subTags.append(":: ").append(text);
        i(tag, subTags.toString());
    }

    public static void w(String text) {
        w(COMM_TAG, text);
    }

    public static void w(String tag, String text) {
        if (mLogLevel <= Log.WARN) {
            _w(tag, text);
            if (mIsEnabledPublish)
                publishLog(Log.WARN, tag, text);
        }
    }

    public static void w(String tag, String subTag, String text) {
        w(tag, subTag + ":: " + text);
    }

    public static void w(String tag, StringBuilder subTags, String text) {
        subTags.append(":: ").append(text);
        w(tag, subTags.toString());
    }

    public static void w(Throwable tr) {
        w(COMM_TAG, tr.getMessage(), tr);
    }

    public static void w(String text, Throwable tr) {
        w(COMM_TAG, text, tr);
    }

    public static void w(String tag, String text, Throwable tr) {
        if (mLogLevel <= Log.WARN) {
            _w(tag, text, tr);
            if (mIsEnabledPublish)
                publishLog(Log.WARN, tag, text, tr);
        }
    }

    public static void w(String tag, String subTag, String text, Throwable tr) {
        w(tag, subTag + ":: " + text, tr);
    }

    public static void w(String tag, StringBuilder subTags, String text, Throwable tr) {
        subTags.append(":: ").append(text);
        w(tag, subTags.toString(), tr);
    }

    public static void e(String text) {
        e(COMM_TAG, text);
    }

    public static void e(String tag, String text) {
        if (mLogLevel <= Log.ERROR) {
            _e(tag, text);
            if (mIsEnabledPublish)
                publishLog(Log.ERROR, tag, text);
        }
    }

    public static void e(String tag, String subTag, String text) {
        e(tag, subTag + ":: " + text);
    }

    public static void e(String tag, StringBuilder subTags, String text) {
        subTags.append(":: ").append(text);
        e(tag, subTags.toString());
    }

    public static void e(Throwable tr) {
        e(COMM_TAG, tr.getMessage(), tr);
    }

    public static void e(String text, Throwable tr) {
        e(COMM_TAG, text, tr);
    }

    public static void e(String tag, String text, Throwable tr) {
        if (mLogLevel <= Log.ERROR) {
            _e(tag, text, tr);
            if (mIsEnabledPublish)
                publishLog(Log.ERROR, tag, text, tr);
        }
    }

    public static void e(String tag, String subTag, String text, Throwable tr) {
        e(tag, subTag + ":: " + text, tr);
    }

    public static void e(String tag, StringBuilder subTags, String text, Throwable tr) {
        subTags.append(":: ").append(text);
        e(tag, subTags.toString(), tr);
    }

    public static void log(final int level, String tag, String text) {
        log(level, tag, text, null);
    }

    public static void log(final int level, String tag, String text, Throwable tr) {
        switch (level) {
            case Log.VERBOSE:
                v(tag, text);
                break;
            case Log.DEBUG:
                d(tag, text);
                break;
            case Log.INFO:
                i(tag, text);
                break;
            case Log.WARN:
                if (tr == null)
                    w(tag, text);
                else
                    w(tag, text, tr);
                break;
            case Log.ERROR:
            default:
                if (tr == null)
                    e(tag, text);
                else
                    e(tag, text, tr);
                break;
        }
    }

    private static void publishLog(int level, String tag, String text) {
        publishLog(level, tag, text, null);
    }

    private static void publishLog(int level, String tag, String text, Throwable tr) {
        if (mLogPublishLevel <= level) {
            LogMessage info = new LogMessage();
            info.setTag(tag);
            info.setLevel(level);
            info.setContent(text);
            info.setThrowable(tr);
            // EventBus.getDefault().post(info);
        }
    }

    public static StringBuilder Tags(String tag1, String... tags) {
        StringBuilder builder = new StringBuilder(tag1);
        builder.append("::");
        for (String tag : tags) {
            builder.append(tag);
            builder.append("::");
        }
        return builder;
    }

    public static class LogMessage {
        String tag;
        int level;
        String content;
        Throwable throwable;

        public String getTag() {
            return tag;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }

        public int getLevel() {
            return level;
        }

        public void setLevel(int level) {
            this.level = level;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public Throwable getThrowable() {
            return throwable;
        }

        public void setThrowable(Throwable throwable) {
            this.throwable = throwable;
        }
    }

    private static void _v(String tag, String text) {
        if (OS_ANDROID)
            Log.v(tag, text);
        else
            System.out.println("[VERBOSE][" + tag + "]: " + text);
    }

    private static void _d(String tag, String text) {
        if (OS_ANDROID)
            Log.d(tag, text);
        else
            System.out.println("[DEBUG][" + tag + "]: " + text);
    }

    private static void _i(String tag, String text) {
        if (OS_ANDROID)
            Log.i(tag, text);
        else
            System.out.println("[INFO][" + tag + "]: " + text);
    }

    private static void _w(String tag, String text) {
        if (OS_ANDROID)
            Log.w(tag, text);
        else
            System.out.println("[WARN][" + tag + "]: " + text);
    }

    private static void _w(String tag, String text, Throwable throwable) {
        if (OS_ANDROID) {
            Log.w(tag, text, throwable);
        } else {
            System.out.println("[WARN][" + tag + "]: " + text);
            System.out.println(throwable.getLocalizedMessage());
        }
    }

    private static void _e(String tag, String text) {
        if (OS_ANDROID)
            Log.e(tag, text);
        else
            System.out.println("[ERROR][" + tag + "]: " + text);
    }

    private static void _e(String tag, String text, Throwable throwable) {
        if (OS_ANDROID) {
            Log.e(tag, text, throwable);
        } else {
            System.out.println("[ERROR][" + tag + "]: " + text);
            System.out.println(throwable.getLocalizedMessage());
        }
    }
}
