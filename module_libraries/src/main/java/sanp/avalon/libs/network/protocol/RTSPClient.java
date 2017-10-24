package sanp.avalon.libs.network.protocol;

import android.media.MediaFormat;

import sanp.avalon.libs.media.base.AudioInfo;
import sanp.avalon.libs.media.base.VideoInfo;
import sanp.avalon.libs.base.utils.LogManager;

public class RTSPClient {
    
    static{
        System.loadLibrary("MPX_JNI");
    }

    static public final int MEDIA_TYPE_VIDEO = 0;
    static public final int MEDIA_TYPE_AUDIO = 1;
    static public final int MEDIA_TYPE_VIDEO_EXT = 2;

    /*
    static private final int RTSP_EVT_CONNECTED    = 0;
    static private final int RTSP_EVT_DISCONNECTED = 1;
    static private final int RTSP_EVT_PLAYED       = 2;
    */
    static private final int RTSP_EVT_CONNECTION_BROKEN = 3;

    public interface Callback {
        void onConnectionBroken(int err);
        void onIncomingFrame(int media_id, byte []datas, long timestamp_us, int has_start_code);
    }
    
    private Callback m_callback;
    private int m_id = -1;
    private static int g_cur_id = 0;
    
    public static RTSPClient create(Callback callback) {
        RTSPClient client = new RTSPClient(g_cur_id, callback);
        ++g_cur_id;
        return client;
    }
    
    private RTSPClient(int id, Callback callback) {
        m_callback = callback;
        m_id = id;
    }
    
    public int init() { return init(m_id); }
    public int init(int rtsp_port, int transport_start_port) { return init(m_id, rtsp_port, transport_start_port); }
    public int release() { return release(m_id); }
    public int connect(String url, boolean overTcp, int connect_timeout_ms, int stream_timeout_ms) { return connect(m_id, url, overTcp?1:0, connect_timeout_ms, stream_timeout_ms); }
    public int play() { return play(m_id); }
    public int pause() { return pause(m_id); }
    public int stop() { return stop(m_id); }
    public AudioInfo getAudioInfo() {
        int[] infos = getAudioInfo(m_id);
        if(infos == null)
            return null;
        return new AudioInfo(MEDIA_TYPE_AUDIO, infos);
    }
    public VideoInfo getVideoInfo() {
        int[] infos = getVideoInfo(m_id);
        if(infos == null)
            return null;
        return new VideoInfo(MEDIA_TYPE_VIDEO, infos);
    }
    public VideoInfo getVideoExtInfo() {
        return null;
    }
    public VideoInfo getDummyVideoInfo() {
        return new VideoInfo(MEDIA_TYPE_VIDEO, MediaFormat.MIMETYPE_VIDEO_AVC, 1920, 1080, 25, 2 * 1024 * 1024);
    }
    
    public void onEvent(int event, int result) {
        if(m_callback == null) {
            LogManager.w("RTSPClient onEvent: " + event + " with result " + result);
            return;
        }
        switch(event) {
        case RTSP_EVT_CONNECTION_BROKEN:
            m_callback.onConnectionBroken(result);
            break;
        default :
            throw new RuntimeException("Unknown event " + event);
        }
    }
    
    public void onIncomingFrame(int media_id, byte []datas, long timestamp_us, int has_start_code) {
        m_callback.onIncomingFrame(media_id, datas, timestamp_us, has_start_code);
    }
    
    private native int init(int obj_id);
    private native int init(int obj_id, int rtsp_port, int transport_start_port);
    private native int release(int obj_id);
    
    private native int connect(int obj_id, String url, int rtp_over_tcp, int connect_timeout_ms, int stream_timeout_ms);
    private native int play(int obj_id);
    private native int pause(int obj_id);
    private native int stop(int obj_id);
    
    private native int[] getAudioInfo(int obj_id);
    private native int[] getVideoInfo(int obj_id);
    private native int[] getVideoExtInfo(int obj_id);
}
