package sanp.avalon.libs.media.audio;

/**
 * Created by Tuyj on 2017/7/27.
 */

public class PCMWave {
    public interface AmplitudeCallback {
        void onAmplitude(long amplitude, long max);
    }

    public static short getMaxAmplitude(byte []frame, int offset, int size, int channels) {
        int step = 2 * channels;
        short amplitude = 0;
        for(int i = offset ; i <= size-step ; i += step) {
            short sample = (short) ((((short)frame[i+1]) << 8) | (short)frame[i]);
            if(sample < 0)
                sample = (short) (0 - sample);
            if (sample > amplitude)
                amplitude = sample;
        }
        return amplitude;
    }
}
