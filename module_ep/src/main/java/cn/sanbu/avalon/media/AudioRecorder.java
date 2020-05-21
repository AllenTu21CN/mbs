package cn.sanbu.avalon.media;

import android.media.AudioRecord;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by huangzy on 2018/8/2.
 */

public class AudioRecorder {

    private static final String TAG = AudioRecorder.class.getName();

    private static final long DefaultThreadExitTimeoutMs = 3000;
    private static final ByteOrder DefaultOrder = ByteOrder.LITTLE_ENDIAN;
    private static final AudioRecorder instance = new AudioRecorder();

    public static final AudioRecorder getInstance() {
        return instance;
    }

    private AudioRecord mRecord;
    private Thread mRecordingThread;
    private boolean mRecording = false;
    //private int mframeDurationMs = -1;
    private int mFrameSizeInByte = -1;

    private AudioRecorder() {

    }

    public int open() {
        return open(
                AudioSupporting.DEFAULT_AUDIO_SOURCE,
                AudioSupporting.HZ_RECOMMENDED,
                AudioSupporting.CHANNEL_RECOMMENDED,
                AudioSupporting.DEFAULT_SAMPLE_FORMAT,
                AudioSupporting.FRAME_DURATION_MS_RECOMMENDED);
    }

    public int open(int audioSrc, int sampleRate, int channelMode, int sampleFormat, int frameDurationMs) {
        synchronized (this) {
            if (mRecord == null) {
                int mChannelsCnt = AudioSupporting.ChannelMode2Cnt(channelMode);
                int mSampleWidthInBytes = AudioSupporting.SampleFormat2Bytes(sampleFormat);
                int mSampleCntOfFrame = sampleRate * frameDurationMs / 1000 * mChannelsCnt;
                int minRecSizeInByte = AudioRecord.getMinBufferSize(sampleRate, channelMode, sampleFormat);

                //mframeDurationMs = frameDurationMs;
                mFrameSizeInByte = mSampleCntOfFrame * mSampleWidthInBytes;
                mRecord = new AudioRecord(audioSrc, sampleRate, channelMode, sampleFormat, minRecSizeInByte);
            }
        }
        return 0;
    }

    public int resume() {
        synchronized (this) {
            if (mRecordingThread != null)
                return 0;
            mRecordingThread = new Thread(new Runnable() {
                public void run() {
                    doCaptureLoop();
                }
            }, "AudioCapturer Thread");
            mRecordingThread.start();
        }
        return 0;
    }

    public int pause() {
        synchronized (this) {
            mRecording = false;
            if (mRecordingThread != null) {
                try {
                    if (mRecord != null)
                        mRecord.stop();
                } catch (IllegalStateException e) {
                    Log.w(TAG, "audio record stop failed:",e);
                    //e.printStackTrace();
                }
                try {
                    mRecordingThread.join(DefaultThreadExitTimeoutMs);
                } catch (InterruptedException e) {
                    Log.e(TAG, e.getMessage(), e);
                    return -1;
                }
                if (mRecordingThread.isAlive()) {
                    Log.w(TAG, "wait audio recording thread exit timeout, interrupt it rudely");
                    mRecordingThread.interrupt();
                }
                mRecordingThread = null;
            }
        }
        return 0;
    }

    public int close() {
        synchronized (this) {
            if (mRecord != null) {
                pause();
                mRecord.release();
                //mframeDurationMs = -1;
                mRecord = null;
            }
        }
        return 0;
    }

    private void doCaptureLoop() {
        mRecord.startRecording();
        mRecording = true;
        Log.i(TAG, "Start to record voice");
        ByteBuffer pcmFrame = allocPcmFrame();
        while (mRecording) {
            int ret = mRecord.read(pcmFrame, mFrameSizeInByte);
            if (ret < 0) {
                Log.e(TAG, "AudioRecord read error: " + ret);
                break;
            } else if (ret != mFrameSizeInByte) {
                Log.w(TAG, String.format("Capturing data is not enough: %d(%d)", ret, mFrameSizeInByte));
                continue;
            }
            // pcmFrame ==> onData
            long ptsUs = System.nanoTime() / 1000;
            byte[] bs = new byte[pcmFrame.remaining()];
            pcmFrame.get(bs, 0, bs.length);
            onData(bs, 0, bs.length, ptsUs);
            pcmFrame.clear();
        }
        mRecording = false;
        //mRecord.stop(); call it while pause
        Log.i(TAG, "Exit to audio capture loop");
    }

    private ByteBuffer allocPcmFrame() {
        ByteBuffer PcmFrame = ByteBuffer.allocateDirect(mFrameSizeInByte);
        PcmFrame.order(DefaultOrder);
        return PcmFrame;
    }

    private native int onData(byte[] bs, int offset, int length, long ptsUs);
}

