package sanp.javalon.media.base;

import android.media.MediaFormat;

public class AudioInfo extends MediaInfo {
    public int sampleRate = -1;
    public int channelCount = -1;
    public int Profile = -1;

    public AudioInfo(int id, int values[]) {
        if(!CodecId2Mime.containsKey(values[0]))
            throw new RuntimeException("Non-supported codec type: " + values[0]);
        if(!ProfileId2AndroidId.containsKey(values[3]))
            throw new RuntimeException("Non-supported profile value: " + values[3]);
        String mime = CodecId2Mime.get(values[0]);
        int sampleRate = values[1];
        int channelCount = values[2];
        int profile = ProfileId2AndroidId.get(values[3]);
        init0(id, mime, sampleRate, channelCount, profile);
    }

    public AudioInfo(int id, String mime, int sampleRate, int channelCount, int profile) {
        init0(id, mime, sampleRate, channelCount, profile);
    }

    public String typeName() {
        return "audio";
    }

    public MediaFormat convert() {
        MediaFormat format = MediaFormat.createAudioFormat(mime, sampleRate, channelCount);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, Profile);
        return format;
    }

    private void init0(int id, String mime, int sampleRate, int channelCount, int profile) {
        this.sampleRate = sampleRate;
        this.channelCount = channelCount;
        Profile = profile;
        init(id, mime);
    }
}
