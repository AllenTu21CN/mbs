package cn.sanbu.avalon.media;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Log;

/**
 * Created by huangzy on 2018/8/2.
 */

public class AudioPlayer {

    private static final String TAG = AudioPlayer.class.getName();

    private static final AudioPlayer instance = new AudioPlayer();
    public static final AudioPlayer getInstance() { return instance; }
    private AudioTrack mTrack;
    private int mFrameSizeInByte;

    private AudioPlayer(){
        //Log.d(TAG,"AudioPlayer init" );
    }

    public int open() {
        Log.d(TAG,"AudioPlayer open" );
        return open(
                AudioSupporting.STREAM_MODE_MEDIA,
                AudioSupporting.HZ_RECOMMENDED,
                AudioSupporting.CHANNEL_RECOMMENDED,
                AudioSupporting.DEFAULT_SAMPLE_FORMAT,
                AudioSupporting.FRAME_DURATION_MS_RECOMMENDED);

    }

    public int open(int audioDst, int sampleRate, int channelMode, int sampleFormat, int frameDurationMs) {
        synchronized (this) {
            if (mTrack != null) {
                return 0;
            }

            int minPlaySizeInByte = AudioTrack.getMinBufferSize(sampleRate, channelMode, sampleFormat);
            if (minPlaySizeInByte == AudioTrack.ERROR_BAD_VALUE) {
                throw new RuntimeException(String.format("AudioTrack.getMinBufferSize invalid parameter: rate-%d mode-%d format-%d",
                                                            sampleRate, channelMode, sampleFormat));
            }

            int mChannelsCnt = AudioSupporting.ChannelMode2Cnt(channelMode);
            int mSampleWidthInBytes = AudioSupporting.SampleFormat2Bytes(sampleFormat);
            int mSampleCntOfFrame = sampleRate * frameDurationMs / 1000 * mChannelsCnt;
            mFrameSizeInByte = mSampleCntOfFrame * mSampleWidthInBytes;
            minPlaySizeInByte = mFrameSizeInByte > minPlaySizeInByte ? mFrameSizeInByte : minPlaySizeInByte;

            mTrack = new AudioTrack.Builder()
                            .setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build())
                            .setAudioFormat(new AudioFormat.Builder()
                                .setEncoding(sampleFormat)
                                .setSampleRate(sampleRate)
                                .setChannelMask(channelMode)
                                .build())
                            .setBufferSizeInBytes(minPlaySizeInByte * 4)
                            .build();
            mTrack.play();
        }
        Log.i(TAG,"AudioPlayer =====open finish" );
        return 0;
    }

    public int write(byte[] bs, int offset, int length, long ptsUs) {
//        long t1 = System.currentTimeMillis();
        if(mTrack == null) return 0;
        int ret = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            ret = mTrack.write(bs, offset, length, AudioTrack.WRITE_NON_BLOCKING);
        } else {
            ret = mTrack.write(bs, offset, length);
        }
        if(ret < 0) {
            //Log.e(TAG,"AudioCapturer.PlaybackTesting.AudioTrack write error: " + ret);
        } else if(ret != mFrameSizeInByte) {
            //Log.w(TAG, String.format("AudioCapturer.PlaybackTesting.AudioTrack write %d(%d)", ret, mFrameSizeInByte));
        }
//        long t2 = System.currentTimeMillis();
//        long td = t2-t1;
//        if (td > 1){
//            Log.e(TAG,String.format("time:%d",t2-t1));
//        }
        return ret;
    }

    public int close() {
        synchronized (this){
            if (mTrack != null){
                mTrack.flush();
                mTrack.release();
                mTrack = null;
            }
        }
        return 0;
    }
}
