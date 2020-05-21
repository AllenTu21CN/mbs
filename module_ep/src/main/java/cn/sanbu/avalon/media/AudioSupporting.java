package cn.sanbu.avalon.media;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.util.Log;

/**
 * Created by huangzy on 2018/8/2.
 */

public final class AudioSupporting {
    private static final String TAG = AudioSupporting.class.getName();

    public static final int DEFAULT_AUDIO_SOURCE = MediaRecorder.AudioSource.DEFAULT;
    public static final int DEFAULT_SAMPLE_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    public static final int HZ_48000 = 48000;
    public static final int HZ_44100 = 44100;
    public static final int HZ_16000 = 16000;
    public static final int HZ_8000 = 8000;
    public static final int HZ_RECOMMENDED = HZ_44100;

    public static final int CHANNEL_STEREO = AudioFormat.CHANNEL_IN_STEREO;
    public static final int CHANNEL_MONO = AudioFormat.CHANNEL_IN_MONO;
    public static final int CHANNEL_RECOMMENDED = CHANNEL_STEREO;

    public static final int FRAME_DURATION_MS_10 = 10;
    public static final int FRAME_DURATION_MS_20 = 20;
    public static final int FRAME_DURATION_MS_25 = 25;
    public static final int FRAME_DURATION_MS_40 = 40;
    public static final int FRAME_DURATION_MS_RECOMMENDED = FRAME_DURATION_MS_20;

    public static final int STREAM_MODE_MEDIA = AudioManager.STREAM_MUSIC;

    public static int ChannelMode2Cnt(int mode) {
        switch (mode) {
            case AudioFormat.CHANNEL_IN_DEFAULT: // AudioFormat.CHANNEL_CONFIGURATION_DEFAULT
            case AudioFormat.CHANNEL_IN_MONO:
            case AudioFormat.CHANNEL_CONFIGURATION_MONO:
                return 1;
            case AudioFormat.CHANNEL_IN_STEREO:
            case AudioFormat.CHANNEL_CONFIGURATION_STEREO:
            case (AudioFormat.CHANNEL_IN_FRONT | AudioFormat.CHANNEL_IN_BACK):
                return 2;
            case AudioFormat.CHANNEL_INVALID:
            default:
                Log.e(TAG, "Invalid channel mode " + mode);
                return -1;
        }
    }

    public static  int SampleFormat2Bytes(int format) {
        switch (format) {
            case AudioFormat.ENCODING_PCM_8BIT:
                return 1;
            case AudioFormat.ENCODING_PCM_16BIT:
                return 2;
            case AudioFormat.ENCODING_PCM_FLOAT:
                return 4;
            default:
                Log.e(TAG, "Invalid sample format " + format);
                return -1;
        }
    }
}
