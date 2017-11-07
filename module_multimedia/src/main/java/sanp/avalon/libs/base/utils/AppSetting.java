package sanp.avalon.libs.base.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by zhangxd on 2017/10/13.
 */

public class AppSetting {

    private static Context mContext;

    private static SharedPreferences preferences;

    private static final String FILE_NAME = "app_setting";

    private static final String CALL_PROTOCAL = "call_protocal";

    private static final String WLAN_CALL_RATE = "wlan_call_rate";

    private static final String LTE_CALL_RATE = "lte_call_rate";

    private static final String AUDIO_FORMAT = "audio_format";

    private static final String VIDEO_FORMAT = "video_format";

    private static final String H323 = "H.323";

    private static final String SIP = "SIP";

    private static final String wlan_call_rate_default = "512knps";

    private static final String lte_call_rate_default = "512knps";

    private static Set<String> mAudioFormatSet = new HashSet<>();

    static {
        mAudioFormatSet.add("AAC");
        mAudioFormatSet.add("G711u");
        mAudioFormatSet.add("G711A");
    }

    private static Set<String> mVideoFormatSet = new HashSet<>();

    static {
        mVideoFormatSet.add("H.263");
        mVideoFormatSet.add("H.264");
        mVideoFormatSet.add("H.265");
    }


    public static void setContext(Context context) {
        mContext = context;
    }


    public static void setSipCall(boolean open) {
        SharedPreferences preferences = mContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(SIP, open);
        editor.commit();
    }

    /**
     * @return 返回呼叫协议
     */
    public static boolean isSipCallOpen() {
        SharedPreferences preferences = mContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        boolean open = preferences.getBoolean(SIP, false);
        return open;
    }

    public static void setH323Call(boolean open) {
        SharedPreferences preferences = mContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(H323, open);
        editor.commit();
    }

    /**
     * @return 返回呼叫协议
     */
    public static boolean isH323Open() {
        SharedPreferences preferences = mContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        boolean open = preferences.getBoolean(H323, false);
        return open;
    }



    /**
     * @param rate wlan呼叫速率
     */
    public static void setWlanCallrate(String rate) {
        SharedPreferences preferences = mContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(WLAN_CALL_RATE, rate);
        editor.commit();
    }

    public static String getWlanCallRate() {
        SharedPreferences preferences = mContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        String protocal = preferences.getString(WLAN_CALL_RATE, wlan_call_rate_default);
        return protocal;
    }

    /**
     * @param rate 3G/4G呼叫速率
     */
    public static void setLteCallrate(String rate) {
        SharedPreferences preferences = mContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(LTE_CALL_RATE, rate);
        editor.commit();
    }

    public static String getLteCallrate() {
        SharedPreferences preferences = mContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        String protocal = preferences.getString(LTE_CALL_RATE, lte_call_rate_default);
        return protocal;
    }

    /**
     * @param formatArr 音频格式
     */
    public static void setAudioFormat(String[] formatArr) {
        SharedPreferences preferences = mContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        Set<String> stringSet = new HashSet<>();
        Collections.addAll(stringSet, formatArr);
        editor.putStringSet(AUDIO_FORMAT, stringSet);
    }

    public static Set getAudioFormat() {
        SharedPreferences preferences = mContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        return preferences.getStringSet(AUDIO_FORMAT, mAudioFormatSet);
    }

    /**
     * @param formatArr 视频格式
     */
    public static void setVideoFormat(String[] formatArr) {
        SharedPreferences preferences = mContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        Set<String> stringSet = new HashSet<>();
        Collections.addAll(stringSet, formatArr);
        editor.putStringSet(VIDEO_FORMAT, stringSet);
    }

    public static Set getVideoFormat() {
        SharedPreferences preferences = mContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        return preferences.getStringSet(VIDEO_FORMAT, mAudioFormatSet);
    }


}
