package sanp.avalon.libs.network.protocol;

import android.media.MediaFormat;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

import sanp.avalon.libs.base.utils.LogManager;

public class RTMPPushClient {
    static{
        System.loadLibrary("MPX_JNI");
    }

    static public final int WRITING_FLAG_KEYFRAME = 0x00000001;
    
    /*
    static private final int RTMP_EVT_CONNECTED    = 0;
    static private final int RTMP_EVT_DISCONNECTED = 1;
    */
    static private final int RTMP_EVT_CONNECTION_BROKEN = 2;

    private static int g_cur_id = 0;

    public interface Callback {
        void onConnectionBroken(int err);
    }

    private int m_id = -1;
    private Callback m_callback = null;

    public static RTMPPushClient create(Callback cb) {
        RTMPPushClient client = new RTMPPushClient(g_cur_id, cb);
        ++g_cur_id;
        return client;
    }
    
    private RTMPPushClient(int id, Callback cb) {
        m_id = id;
        m_callback = cb;
    }
    
    public int init(String url) { return init(m_id, url); }
    public int close() { return close(m_id); }
    public int connect() { return connect(m_id); }
    public int disconnect() { return disconnect(m_id); }
    
    public void write(int stream_index, byte []datas, int offset, int length, long pts_us, int flag) { 
        write(m_id, stream_index, datas, offset, length, pts_us, flag); 
    }
    public int addStream(MediaFormat format, byte[] extradata) {
        Map<String, String> fmp = new HashMap<>();

        String media_type = "";
        String codec_type = "";
        String mime = format.getString(MediaFormat.KEY_MIME);
        int bitrate = format.getInteger(MediaFormat.KEY_BIT_RATE);
        
        if(mime.startsWith("audio")) {

            media_type = MEDIA_TYPE_NAME_AUDIO;
            if(mime.equals(MediaFormat.MIMETYPE_AUDIO_AAC)) {
                codec_type = CODEC_NAME_AAC;
            } else if(mime.equals(MediaFormat.MIMETYPE_AUDIO_G711_ALAW)) {
                codec_type = CODEC_NAME_G711A;
            } else if(mime.equals(MediaFormat.MIMETYPE_AUDIO_G711_MLAW)) {
                codec_type = CODEC_NAME_G711U;
            } else {
                LogManager.e("non-support codec:" + mime);
                return -1;
            }

            int channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            int sample_rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            fmp.put(FORMAT_KEY_NAME_CHANNELS, channels+"");
            fmp.put(FORMAT_KEY_NAME_SAMPLE_RATE, sample_rate+"");
            
        } else if(mime.startsWith("video")) {

            media_type = MEDIA_TYPE_NAME_VIDEO;
            if(mime.equals(MediaFormat.MIMETYPE_VIDEO_AVC)) {
                codec_type = CODEC_NAME_H264;
            } else if(mime.equals(MediaFormat.MIMETYPE_VIDEO_HEVC)) {
                codec_type = CODEC_NAME_H265;
            } else if(mime.equals(MediaFormat.MIMETYPE_VIDEO_VP8)) {
                codec_type = CODEC_NAME_VP8;
            } else {
                LogManager.e("non-support codec:" + mime);
                return -1;
            }
            
            int width = format.getInteger(MediaFormat.KEY_WIDTH);
            int height = format.getInteger(MediaFormat.KEY_HEIGHT);
            fmp.put(FORMAT_KEY_NAME_WIDTH, width+"");
            fmp.put(FORMAT_KEY_NAME_HEIGHT, height+"");
            
        } else {
            LogManager.e("non-support format:" + mime);
            return -1;
        }
        
        fmp.put(FORMAT_KEY_NAME_MEDIA_TYPE, media_type);
        fmp.put(FORMAT_KEY_NAME_CODEC_TYPE, codec_type);
        fmp.put(FORMAT_KEY_NAME_BIT_RATE, bitrate+"");
        
        JSONObject object = new JSONObject(fmp);
        String format_json = object.toString();
        int stream_id = addStream(m_id, format_json, extradata);
        return stream_id;
    }
    
    public void onEvent(int event, int result) {
        if(m_callback == null) {
            LogManager.w("RTMPPushClient onEvent: " + event + " with result " + result);
            return;
        }
        switch(event) {
        case RTMP_EVT_CONNECTION_BROKEN:
            m_callback.onConnectionBroken(result);
            break;
        default :
            throw new RuntimeException("Unknown event " + event);
        }
    }

    private native int init(int obj_id, String url);
    private native int close(int obj_id);
    private native int addStream(int obj_id, String format_json, byte[] extradata);
    private native int connect(int obj_id);
    private native int disconnect(int obj_id);
    private native int write(int obj_id, int stream_index, byte []datas, int offset, int length, long pts_us, int flag);
    
    private static final String FORMAT_KEY_NAME_CODEC_TYPE      = "codec_type";
    private static final String FORMAT_KEY_NAME_MEDIA_TYPE      = "media_type";
    private static final String FORMAT_KEY_NAME_BIT_RATE        = "bit_rate";
    private static final String FORMAT_KEY_NAME_CHANNELS        = "channels";
    private static final String FORMAT_KEY_NAME_SAMPLE_RATE     = "sample_rate";
    //private static final String FORMAT_KEY_NAME_SAMPLE_FMT      = "sample_fmt";
    private static final String FORMAT_KEY_NAME_WIDTH           = "width";
    private static final String FORMAT_KEY_NAME_HEIGHT          = "height";
    //private static final String FORMAT_KEY_NAME_PIX_FMT         = "pix_fmt";
    
    private static final String MEDIA_TYPE_NAME_AUDIO = "audio";
    private static final String MEDIA_TYPE_NAME_VIDEO = "video";
    
    private static final String CODEC_NAME_AAC   = "AAC";
    private static final String CODEC_NAME_G711A = "G711A";
    private static final String CODEC_NAME_G711U = "G711U";
    private static final String CODEC_NAME_H264  = "H264";
    private static final String CODEC_NAME_H265  = "H265";
    private static final String CODEC_NAME_VP8   = "VP8";
    
    private static final String PIXEL_FORMAT_NAME_I420  = "I420";
    private static final String PIXEL_FORMAT_NAME_YV12  = "YV12";
    private static final String PIXEL_FORMAT_NAME_NV12  = "NV12";
    private static final String PIXEL_FORMAT_NAME_NV21  = "NV21";
    private static final String PIXEL_FORMAT_NAME_UYVY  = "UYVY";
    private static final String PIXEL_FORMAT_NAME_YUY2  = "YUY2";
    private static final String PIXEL_FORMAT_NAME_RGB24 = "RGB24";
    private static final String PIXEL_FORMAT_NAME_RGB32 = "RGB32";
    private static final String PIXEL_FORMAT_NAME_ARGB  = "ARGB";
}
