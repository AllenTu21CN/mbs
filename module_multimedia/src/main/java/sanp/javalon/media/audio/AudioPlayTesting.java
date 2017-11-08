package sanp.javalon.media.audio;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;

import sanp.multimedia.R;
import sanp.test.SimpleTesting;
import sanp.tools.utils.LogManager;

/**
 * Created by Tuyj on 2017/7/25.
 */

public class AudioPlayTesting {
    
    static private AudioPlayTesting mAudioPlayTesting = null;
    public static AudioPlayTesting getInstance() {
        if (mAudioPlayTesting == null) {
            synchronized (AudioPlayTesting.class) {
                if (mAudioPlayTesting == null) {
                    mAudioPlayTesting = new AudioPlayTesting();
                }
            }
        }
        return mAudioPlayTesting;
    }

    private MediaPlayer mMediaPlayer = null;
    private Visualizer mVisualizer = null;
    private PCMWave.AmplitudeCallback mVisualizerCallback = null;

    public void start(Context context, PCMWave.AmplitudeCallback callback) {
        if(mMediaPlayer == null) {
            mVisualizerCallback = callback;
            mMediaPlayer = MediaPlayer.create(context, R.raw.sample);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setLooping(true);
            /*
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                }
            });
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                }
            });
            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    return false;
                }
            });
            mMediaPlayer.prepareAsync();
            */
            mMediaPlayer.start();
            if(mVisualizerCallback != null)
                initVisualizer();
        }
    }

    public void stop() {
        if(mVisualizer != null) {
            mVisualizer.release();
            mVisualizer = null;
        }
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    public void pause() {
        if (mMediaPlayer != null)
            mMediaPlayer.pause();
    }

    public void resume () {
        if (mMediaPlayer != null)
            mMediaPlayer.start();
    }

    private void initVisualizer() {
        mVisualizer = new Visualizer(mMediaPlayer.getAudioSessionId());
        mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[0]);
        mVisualizer.setDataCaptureListener(
                new Visualizer.OnDataCaptureListener() {
                    @Override
                    public void onFftDataCapture(Visualizer visualizer,
                                                 byte[] fft, int samplingRate) {
                    }
                    @Override
                    public void onWaveFormDataCapture(Visualizer visualizer,
                                                      byte[] waveform, int samplingRate) {
                        long avg = 0;
                        for(byte value: waveform) {
                            avg += value;
                        }
                        avg = avg / waveform.length + 128;
                        mVisualizerCallback.onAmplitude(avg, 256);
                    }
                }, Visualizer.getMaxCaptureRate() / 2, true, false);
        mVisualizer.setEnabled(true);
    }

    static public class Tester implements SimpleTesting.Tester, PCMWave.AmplitudeCallback {
        private int mTestingStep = 5;
        private AudioPlayTesting mAudioPlayTesting = null;
        public void start(Object obj) {
            mAudioPlayTesting = AudioPlayTesting.getInstance();
            mAudioPlayTesting.start((Context) obj, this);
            --mTestingStep;
        }
        public void next() {
            if (mTestingStep == 0) {
                if(mAudioPlayTesting != null) {
                    mAudioPlayTesting.stop();
                    mAudioPlayTesting = null;
                }
                return;
            }
            if(mTestingStep == 4) {
                mAudioPlayTesting.pause();
            } else if (mTestingStep == 3) {
                mAudioPlayTesting.resume();
            } else if (mTestingStep == 2) {
            } else if (mTestingStep == 1) {
            }
            --mTestingStep;
        }

        public void onAmplitude(long amplitude, long max) {
            LogManager.i("amplitude: " + (amplitude * 100) / max + "%");
        }
    }
}
