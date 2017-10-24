package sanp.avalon.libs.media.base;

import android.media.MediaFormat;

public class VideoInfo extends MediaInfo {
    public int width = -1;
    public int height = -1;
    public int fps = -1;
    public int bitrate = -1;

    public VideoInfo(int id, int values[]) {
        if(!CodecId2Mime.containsKey(values[0]))
            throw new RuntimeException("Non-supported codec type: " + values[0]);
        String mime = CodecId2Mime.get(values[0]);
        int width = values[1];
        int height = values[2];
        int fps = 25; // TODO: can't get framerate from values[3]
        int bitrate = 2048;// TODO: can't get bitrate from values[4]
        init0(id, mime, width, height, fps, bitrate);
    }

    public VideoInfo(int id, String mime, int width, int height, int fps, int bitrate) {
        init0(id, mime, width, height, fps, bitrate);
    }

    public String typeName() {
        return "video";
    }

    public MediaFormat convert() {
        MediaFormat format = MediaFormat.createVideoFormat(mime, width, height);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        return format;
    }

    private void init0(int id, String mime, int width, int height, int fps, int bitrate) {
        this.width = width;
        this.height = height;
        this.fps = fps;
        this.bitrate = bitrate;
        init(id, mime);
    }
}
