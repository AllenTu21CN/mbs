package sanp.avalon.libs.media.audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.NoiseSuppressor;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import sanp.avalon.libs.SimpleTesting;
import sanp.avalon.libs.base.utils.LogManager;

/**
 * Created by Tuyj on 2017/6/9.
 */

public class AudioCapturer {

    static private AudioCapturer gAudioCapturer = null;
    public static AudioCapturer getInstance() {
        if(gAudioCapturer == null) {
            synchronized (AudioCapturer.class) {
                if(gAudioCapturer == null)
                    gAudioCapturer = new AudioCapturer();
            }
        }
        return gAudioCapturer;
    }
    public static void releaseInstance() {
        synchronized (AudioCapturer.class) {
            if(gAudioCapturer != null) {
                gAudioCapturer.close();
                gAudioCapturer = null;
            }
        }
    }

    public interface Callback {
        void onData(ByteBuffer pcmFrame, Format format, long ptsUs);
    }

    public static final class Supporting {
        static public final int DEFAULT_AUDIO_SOURCE = MediaRecorder.AudioSource.DEFAULT;
        static public final int DEFAULT_SAMPLE_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

        static public final int HZ_48000 = 48000;
        static public final int HZ_44100 = 44100;
        static public final int HZ_16000 = 16000;
        static public final int HZ_8000  = 8000;
        static public final int HZ_RECOMMENDED = HZ_44100;

        static public final int CHANNEL_STEREO = AudioFormat.CHANNEL_IN_STEREO;
        static public final int CHANNEL_MONO = AudioFormat.CHANNEL_IN_MONO;
        static public final int CHANNEL_RECOMMENDED = CHANNEL_STEREO;

        static public final int FRAME_DURATION_MS_10 = 10;
        static public final int FRAME_DURATION_MS_20 = 20;
        static public final int FRAME_DURATION_MS_25 = 25;
        static public final int FRAME_DURATION_MS_40 = 40;
        static public final int FRAME_DURATION_MS_RECOMMENDED = FRAME_DURATION_MS_20;

        static public final int TESTING_METHOD_PLAYBACK = 1;
        static public final int TESTING_METHOD_TOFILE = 2;
        static public final int TESTING_METHOD_NONE = -1;

        static public final int TESTING_PLAYBACK_MODE_CALL = AudioManager.STREAM_VOICE_CALL;
        static public final int TESTING_PLAYBACK_MODE_MEDIA = AudioManager.STREAM_MUSIC;
        static public final int TESTING_PLAYBACK_MODE_NONE = -1;
    }

    static private final long DefaultThreadExitTimeoutMs = 1000;
    static private final boolean DefaultPlaybackTestingHDMI = true;
    static private final String DefaultFileTestingPath = "/sdcard/voice_%d_%d.pcm";
    static private final int DefaultVolumeDebugDurationMs = 50;
    static private final ByteOrder DefaultOrder = ByteOrder.LITTLE_ENDIAN;

    public Format PCMFormat = null;
    private int mAudioSource = -1;
    private int mSampleRate = -1;
    private int mChannelMode = -1;
    private int mChannelsCnt = -1;
    private int mSampleFormat = -1;
    private int mSampleWidthInBytes = -1;
    private int mFrameDurationMs = -1;
    private int mSampleCntOfFrame = -1;
    private int mFrameSizeInByte = -1;

    private ByteBuffer mPcmFrame = null;
    private boolean mRecording = false;
    private AudioRecord mAudioRecord = null;
    private Thread mRecordingThread = null;
    private List<Callback> mCallbacks = new ArrayList<>();
    private Testing mTesting = null;

    private NoiseSuppressor mNoiseSuppressor = null;
    private AutomaticGainControl mAutomaticGainControl = null;

    private PCMWave.AmplitudeCallback mAmplitudeCallback = null;
    private int mAmplitudeCallbackIntervalMS = DefaultVolumeDebugDurationMs;

    private AudioCapturer() {
    }

    public int init() {
        return init(
                Supporting.DEFAULT_AUDIO_SOURCE,
                Supporting.HZ_RECOMMENDED,
                Supporting.CHANNEL_RECOMMENDED,
                Supporting.DEFAULT_SAMPLE_FORMAT,
                Supporting.FRAME_DURATION_MS_RECOMMENDED);
    }

    public int init(int audioSource, int sampleRate, int channelMode, int sampleFormat, int frameDurationMs) {
        if(mAudioRecord != null) {
            LogManager.w("had inited");
            return 0;
        }
        mAudioSource = audioSource;
        mSampleRate = sampleRate;
        mChannelMode = channelMode;
        mSampleFormat = sampleFormat;
        mFrameDurationMs = frameDurationMs;

        mChannelsCnt = ChannelMode2Cnt(mChannelMode);
        mSampleWidthInBytes = SampleFormat2Bytes(mSampleFormat);
        mSampleCntOfFrame = mSampleRate * mFrameDurationMs / 1000 * mChannelsCnt;
        mFrameSizeInByte = mSampleCntOfFrame * mSampleWidthInBytes;
        mPcmFrame = ByteBuffer.allocateDirect(mFrameSizeInByte);
        mPcmFrame.order(DefaultOrder);
        PCMFormat = new Format(mSampleRate, mChannelsCnt, mSampleWidthInBytes, mFrameDurationMs);

        int minRecSizeInByte = AudioRecord.getMinBufferSize(mSampleRate, mChannelMode, mSampleFormat);
        if (minRecSizeInByte == AudioRecord.ERROR_BAD_VALUE) {
            LogManager.e(String.format("AudioRecord.getMinBufferSize invalid parameter: rate-%d channel_mode-%d sample_format-%d",
                    mSampleRate, mChannelMode, mSampleFormat));
            return -1;
        }
        minRecSizeInByte = minRecSizeInByte > mFrameSizeInByte ? minRecSizeInByte : mFrameSizeInByte;

        mAudioRecord = new AudioRecord(mAudioSource, mSampleRate, mChannelMode, mSampleFormat, minRecSizeInByte);
        if (mAudioRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {
            LogManager.e("AudioRecord initialize failed");
            mAudioRecord.release();
            mAudioRecord = null;
            return -1;
        }

        return 0;
    }

    public boolean inited() {
        return (mAudioRecord != null);
    }

    public int close() {
        if(mTesting != null)
            enableTesting(false, Supporting.TESTING_METHOD_NONE, Supporting.TESTING_PLAYBACK_MODE_NONE);
        pause();
        if(mNoiseSuppressor != null) {
            mNoiseSuppressor.release();
            mNoiseSuppressor = null;
        }
        if(mAutomaticGainControl != null) {
            mAutomaticGainControl.release();
            mAutomaticGainControl = null;
        }
        if(mAudioRecord != null) {
            mAudioRecord.release();
            mAudioRecord = null;
        }
        mCallbacks.clear();
        return 0;
    }

    public int resume() {
        if(mRecording && mRecordingThread != null)
            return 0;
        mRecordingThread = new Thread(new Runnable() {
            public void run() {
                doCaptureLoop();
            }
        }, "AudioCapturer Thread");
        mRecordingThread.start();
        return 0;
    }

    public int pause() {
        mRecording = false;
        if(mRecordingThread != null) {
            try {
                mRecordingThread.join(DefaultThreadExitTimeoutMs);
                //mRecordingThread.interrupt();
            } catch (InterruptedException e) {
                LogManager.e("Be interrupted while join capturer thread");
               LogManager.e(e);
            }
            mRecordingThread = null;
        }
        return 0;
    }

    public boolean setNSEnabled(boolean enabled) {
        if(!NoiseSuppressor.isAvailable()) {
            LogManager.w("NoiseSuppressor is not available");
            return false;
        }
        if(mAudioRecord == null) {
            LogManager.w("init first");
            return false;
        }
        if(mNoiseSuppressor == null) {
            mNoiseSuppressor = NoiseSuppressor.create(mAudioRecord.getAudioSessionId());
            if(mNoiseSuppressor == null) {
                LogManager.w("NoiseSuppressor is not available");
                return false;
            }
        }
        mNoiseSuppressor.setEnabled(enabled);
        LogManager.i("setNSEnabled " + enabled);
        return true;
    }

    public boolean setAGCEnabled(boolean enabled) {
        if(!AutomaticGainControl.isAvailable()) {
            LogManager.w("AutomaticGainControl is not available");
            return false;
        }
        if(mAudioRecord == null) {
            LogManager.w("init first");
            return false;
        }
        if(mAutomaticGainControl == null) {
            mAutomaticGainControl = AutomaticGainControl.create(mAudioRecord.getAudioSessionId());
            if(mAutomaticGainControl == null) {
                LogManager.w("AutomaticGainControl is not available");
                return false;
            }
        }
        mAutomaticGainControl.setEnabled(enabled);
        LogManager.i("setAGCEnabled " + enabled);
        return true;
    }

    public void setAmplitudeCallback(PCMWave.AmplitudeCallback callback) {
        setAmplitudeCallback(callback, DefaultVolumeDebugDurationMs);
    }

    public void setAmplitudeCallback(PCMWave.AmplitudeCallback callback, int intervalMS) {
        mAmplitudeCallbackIntervalMS = intervalMS;
        mAmplitudeCallback = callback;
    }

    public void clearAmplitudeCallback() {
        mAmplitudeCallback = null;
        mAmplitudeCallbackIntervalMS = DefaultVolumeDebugDurationMs;
    }

    public void addCallback(Callback cb) {
        synchronized(mCallbacks) {
            if(!mCallbacks.contains(cb))
                mCallbacks.add(cb);
            if(mCallbacks.size() == 1)
                resume();
        }
    }

    public void removeCallback(Callback cb) {
        removeCallback(cb, true);
    }

    public void removeCallback(Callback cb, boolean allowedStop) {
        synchronized(mCallbacks) {
            mCallbacks.remove(cb);
            if(allowedStop && mCallbacks.size() == 0)
                pause();
        }
    }

    public void enableTesting(boolean on, int method, int playbackMode) {
        if(mAudioRecord == null) {
            LogManager.e("init first");
            return;
        }
        if(on) {
            if(mTesting != null) {
                removeCallback(mTesting, false);
                mTesting.stop();
                mTesting = null;
            }
            if(method == Supporting.TESTING_METHOD_PLAYBACK) {
                mTesting = new PlaybackTesting(mSampleRate, mChannelMode, mSampleFormat, mFrameSizeInByte, playbackMode);
            } else if(method == Supporting.TESTING_METHOD_TOFILE) {
                String filePath = String.format(DefaultFileTestingPath, mSampleRate, mChannelsCnt);
                mTesting = new Debug2FileTesting(filePath, mFrameSizeInByte);
            } else {
                LogManager.e("unknow testing method");
                return;
            }
            try {
                mTesting.start();
            } catch (Exception e) {
               LogManager.e(e);
                mTesting.stop();
                mTesting = null;
                return;
            }
            addCallback(mTesting);
        } else {
            if(mTesting != null) {
                removeCallback(mTesting);
                mTesting.stop();
                mTesting = null;
            }
        }
    }

    private void doCaptureLoop() {
        mAudioRecord.startRecording();
        mRecording = true;
        LogManager.i("Start to record voice");
        int volumeDebugDurationMs = 0;
        while (mRecording) {
            mPcmFrame.clear();
            int ret = mAudioRecord.read(mPcmFrame, mFrameSizeInByte);
            if(ret < 0) {
                LogManager.e("AudioRecord read error: " + ret);
                break;
            } else if(ret != mFrameSizeInByte) {
                LogManager.w(String.format("Capturing data is not enough: %d(%d)", ret, mFrameSizeInByte));
                continue;
            }

            int pos = mPcmFrame.position();
            long ptsUs = System.nanoTime() / 1000;
            synchronized(mCallbacks) {
                for(Callback cb: mCallbacks) {
                    cb.onData(mPcmFrame, PCMFormat, ptsUs);
                    mPcmFrame.position(pos);
                }
            }
            if(mAmplitudeCallback != null) {
                volumeDebugDurationMs += mFrameDurationMs;
                if(volumeDebugDurationMs >= mAmplitudeCallbackIntervalMS) {
                    short amplitude = (short) (PCMWave.getMaxAmplitude(mPcmFrame.array(), mPcmFrame.position(), mFrameSizeInByte, mChannelsCnt) / 256);
                    if(mAmplitudeCallback != null)
                        mAmplitudeCallback.onAmplitude(amplitude, (short) 128);
                    volumeDebugDurationMs = 0;
                }
            }
        }
        mRecording = false;
        mAudioRecord.stop();
        LogManager.i("Exit to audio capture loop");
    }

    static public int ChannelMode2Cnt(int mode) {
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
                LogManager.e("Invalid channel mode " + mode);
                return -1;
        }
    }

    static public int SampleFormat2Bytes(int format) {
        switch (format) {
            case AudioFormat.ENCODING_PCM_8BIT:
                return 1;
            case AudioFormat.ENCODING_PCM_16BIT:
                return 2;
            case AudioFormat.ENCODING_PCM_FLOAT:
                return 4;
            default:
                LogManager.e("Invalid sample format " + format);
                return -1;
        }
    }

    public static class Format {
        public int SampleRate         = -1;
        public int ChannelsCnt        = -1;
        public int SampleWidthInBytes = -1;
        public int FrameDurationMs    = -1;
        public Format() {
        }
        public Format(int rate, int channels, int width, int duration) {
            SampleRate = rate;
            ChannelsCnt = channels;
            SampleWidthInBytes = width;
            FrameDurationMs = duration;
        }
        public void fromOther(Format other) {
            SampleRate = other.SampleRate;
            ChannelsCnt = other.ChannelsCnt;
            SampleWidthInBytes = other.SampleWidthInBytes;
            FrameDurationMs = other.FrameDurationMs;
        }
    }

    private abstract class Testing implements Callback {
        abstract void start();
        abstract void stop();
    }

    private class PlaybackTesting extends Testing {
        private int mSampleRate = -1;
        private int mChannelMode = -1;
        private int mSampleFormat = -1;
        private int mFrameSizeInByte = -1;
        private int mPlayMode = -1;
        private AudioTrack mAudioTrack = null;

        public PlaybackTesting(int sampleRate, int channelMode, int sampleFormat, int frameSizeInByte, int playMode) {
            mSampleRate = sampleRate;
            mChannelMode = channelMode;
            mSampleFormat = sampleFormat;
            mFrameSizeInByte = frameSizeInByte;
            mPlayMode = playMode;
        }
        public void start() {
            if(mAudioTrack != null)
                return;
            int minPlaySizeInByte = AudioTrack.getMinBufferSize(mSampleRate, mChannelMode, mSampleFormat);
            if (minPlaySizeInByte == AudioTrack.ERROR_BAD_VALUE)
                throw new RuntimeException(String.format("AudioTrack.getMinBufferSize invalid parameter: rate-%d mode-%d format-%d",
                        mSampleRate, mChannelMode, mSampleFormat));
            minPlaySizeInByte = mFrameSizeInByte > minPlaySizeInByte ? mFrameSizeInByte : minPlaySizeInByte;

            mAudioTrack = new AudioTrack(mPlayMode, mSampleRate, mChannelMode, mSampleFormat, minPlaySizeInByte, AudioTrack.MODE_STREAM);
            if (mAudioTrack.getState() == AudioTrack.STATE_UNINITIALIZED)
                throw new RuntimeException("AudioTrack initialize failed");
            mAudioTrack.play();
            LogManager.i("Start to PlaybackTesting");
        }
        public void stop() {
            if(mAudioTrack != null) {
                mAudioTrack.release();
                mAudioTrack = null;
                LogManager.i("End of PlaybackTesting");
            }
        }
        public void onData(ByteBuffer pcmFrame, Format format, long ptsUs) {
            if(mAudioTrack == null)
                return;
            int ret = mAudioTrack.write(pcmFrame, mFrameSizeInByte, AudioTrack.WRITE_NON_BLOCKING);
            if(ret < 0) {
                LogManager.e("AudioTrack write error: " + ret);
            } else if(ret != mFrameSizeInByte) {
                LogManager.w(String.format("AudioTrack write %d(%d)", ret, mFrameSizeInByte));
            }
        }
    }

    private class Debug2FileTesting extends Testing {
        private int mFrameSizeInByte = -1;
        private byte mPcmData[] = null;
        private String mFilePath = null;
        private FileOutputStream mOutFile = null;

        public Debug2FileTesting(String filePath, int frameSizeInByte) {
            mFilePath = filePath;
            mFrameSizeInByte = frameSizeInByte;
            mPcmData = new byte[mFrameSizeInByte];
        }
        public void start() {
            if(mOutFile != null)
                return;
            try {
                mOutFile = new FileOutputStream(mFilePath);
                LogManager.i("Start to Debug2FileTesting");
            } catch (FileNotFoundException e) {
                LogManager.e("Create FileOutputStream failed");
               LogManager.e(e);
                mOutFile = null;
            }
        }
        public void stop() {
            if(mOutFile != null) {
                try { mOutFile.close(); } catch (IOException e) {LogManager.e(e); }
                mOutFile = null;
                LogManager.i("End of Debug2FileTesting");
            }
        }
        public void onData(ByteBuffer pcmFrame, Format format, long ptsUs) {
            if(mOutFile == null)
                return;
            pcmFrame.get(mPcmData);
            try { mOutFile.write(mPcmData, 0, mFrameSizeInByte); } catch (IOException e) {LogManager.e(e); }
        }
    }

    static public class Tester implements SimpleTesting.Tester, PCMWave.AmplitudeCallback {
        static private final int testingMethod = AudioCapturer.Supporting.TESTING_METHOD_TOFILE;
        static private final int playbackMode = AudioCapturer.Supporting.TESTING_PLAYBACK_MODE_MEDIA;
        private int mTestingStep = 5;
        private AudioCapturer mCapturer = null;
        public void start(Object obj) {
            mCapturer = AudioCapturer.getInstance();
            if (mCapturer.init() != 0)
                return;
            mCapturer.enableTesting(true, testingMethod, playbackMode);
            --mTestingStep;
        }
        public void next() {
            if (mTestingStep == 0) {
                AudioCapturer.releaseInstance();
                mCapturer = null;
                return;
            }
            if(mTestingStep == 4) {
                mCapturer.setAmplitudeCallback(this);
            } else if (mTestingStep == 3) {
                mCapturer.clearAmplitudeCallback();
            } else if (mTestingStep == 2) {
            } else if (mTestingStep == 1) {
            }
            --mTestingStep;
        }

        public void onAmplitude(long amplitude, long max) {
            LogManager.i("amplitude: " + (amplitude * 100) / max + "%");
        }

        /*
        private AudioManager mAudioManager = (AudioManager) ((Context)obj).getSystemService(Context.AUDIO_SERVICE);
        if(testingMethod == Supporting.TESTING_METHOD_PLAYBACK && DefaultPlaybackTestingHDMI) {
            LogManager.w("ATTACHED: " + mAudioManager.getParameters("attached_output_devices"));
            LogManager.w("DEFAULT: " + mAudioManager.getParameters("default_output_device"));
            LogManager.w("PRE: " + mAudioManager.getParameters("audio_devices_out_active"));
            mAudioManager.setParameters("audio_devices_out_active=AUDIO_HDMI");
            LogManager.w("POST: " + mAudioManager.getParameters("audio_devices_out_active"));
        }
        */
        /*
        if(testingMethod == Supporting.TESTING_METHOD_PLAYBACK && DefaultPlaybackTestingHDMI) {
            if(mAudioManager != null) {
                mAudioManager.setParameters("audio_devices_out_active=AUDIO_DEVICE_OUT_SPEAKER");
            }
        }
        */
        /*
        mAudioManager.setMode(AudioManager.MODE_IN_CALL);
        mAudioManager.setSpeakerphoneOn(false);
        LogManager.w("isSpeakerphoneOn: " + mAudioManager.isSpeakerphoneOn());
        LogManager.w("isWiredHeadsetOn: " + mAudioManager.isWiredHeadsetOn());
        LogManager.w("getMode: " + mAudioManager.getMode());
        */
    }

    /*
    private void initVisualizer() {
        mVisualizer = new Visualizer(mAudioTrack.getAudioSessionId());
        mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
        LogManager.w("Visualizer setCaptureSize " + mVisualizer.getCaptureSize());
        mVisualizer.setDataCaptureListener(
                new Visualizer.OnDataCaptureListener()
                {
                    @Override
                    public void onFftDataCapture(Visualizer visualizer,
                                                 byte[] fft, int samplingRate)
                    {
                    }
                    @Override
                    public void onWaveFormDataCapture(Visualizer visualizer,
                                                      byte[] waveform, int samplingRate)
                    {
                        long avg1 = 0;
                        short max = 0;
                        for(byte value: waveform) {
                            avg1 += value;
                            if(value < 0)
                                value = (byte) (0 - value);
                            if (value > max)
                                max = value;
                        }
                        avg1 = avg1 / waveform.length + 128;
                        LogManager.i(String.format("onWaveFormDataCapture avg1:%d max:%d", avg1, max));
                    }
                }, Visualizer.getMaxCaptureRate() / 2, true, false);
        mVisualizer.setEnabled(true);
    }
    */
}
