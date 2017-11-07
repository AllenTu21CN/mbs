package sanp.avalon.libs.network.protocol;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.media.MediaFormat;

import sanp.tools.utils.LogManager;
import sanp.avalon.libs.media.base.AVChannel;
import sanp.avalon.libs.media.base.AVPacket;
import sanp.avalon.libs.media.base.MediaInfo;
import sanp.avalon.libs.media.video.VideoDecoderWithChannel;

public class RTSPSource implements RTSPClient.Callback {

    public interface Callback {
        void onConnectionBroken(int err);
    };

    public static final String MEDIAFORMAT_VIDEO_EXT_KEY = "video.ext";

    private static final boolean RTP_OVER_TCP = true;
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int STREAM_TIMEOUT_MS = 5000;
    private static final int  BUFFERED_PACKET_COUNT = 3;
    private static final long WAIT_PKT_TIMEOUT_MS = 16; // = 1000ms/60fps
    
    private String          m_url = "";
    private RTSPClient      m_rtsp_client = null;
    private boolean         m_track_selected = false;
    private AVChannel[]     m_reading_ques = null;
    private long            m_start_pts = -1;
    private long            m_start_timestamp = -1;
    private List<MediaFormat> m_formats = new ArrayList<>();
    private Map<Integer, Integer> m_MID2TID = new HashMap<>();

    private Callback m_cb = null;

    public RTSPSource(String url) {
        m_url = url;
        m_rtsp_client = RTSPClient.create(this);
        if(m_rtsp_client.init() != 0) {
            throw new RuntimeException("RTSPClient init failed!");
        }
    }

    public void release() {
        disconnect();

        if(m_rtsp_client != null) {
            m_rtsp_client.release();
            m_rtsp_client = null;
        }
        m_track_selected = false;
    }

    public int connect(Callback cb) {
        m_cb = cb;
        int ret = m_rtsp_client.connect(m_url, RTP_OVER_TCP, CONNECT_TIMEOUT_MS, STREAM_TIMEOUT_MS);
        if(ret != 0)
            return ret;

        loadMedia();

        int cnt = m_formats.size();
        m_reading_ques = new AVChannel[cnt];
        for(int i = 0 ; i < cnt ; ++i) {
            m_reading_ques[i] = null;
        }
        return 0;
    }

    public void disconnect() {
        if(m_rtsp_client != null)
            m_rtsp_client.stop();
        m_reading_ques = null;
        m_formats.clear();
        m_MID2TID.clear();
        m_cb = null;
    }

    public int reconnect() {
        if(m_reading_ques == null || m_formats.size() == 0)
            throw new RuntimeException("logical error");

        // stop first
        if(m_rtsp_client != null) {
            m_rtsp_client.stop();
            m_rtsp_client.release();
            m_rtsp_client = null;
        }
        m_MID2TID.clear();

        // create again
        m_rtsp_client = RTSPClient.create(this);
        int ret = m_rtsp_client.init();
        if(ret != 0)
            throw new RuntimeException("RTSPClient init failed!");

        // connect again
        ret = m_rtsp_client.connect(m_url, RTP_OVER_TCP, CONNECT_TIMEOUT_MS, STREAM_TIMEOUT_MS);
        if(ret != 0) {
            return ret;
        }
        List<MediaFormat> old_formats = m_formats;
        m_formats = new ArrayList<>();
        loadMedia();

        // check media stream format
        if(m_formats.size() != old_formats.size())
            throw new RuntimeException("Media format had been changed");
        for(int i = 0 ; i < m_formats.size() ; ++i) {
            if(m_formats.get(i).getString(MediaFormat.KEY_MIME) != old_formats.get(i).getString(MediaFormat.KEY_MIME))
                throw new RuntimeException("Media format had been changed");
        }

        // play again
        start();
        return 0;
    }

    public int start() {
        if(!m_track_selected) {
            throw new RuntimeException("selectTrack first");
        }
        m_start_pts = -1;
        m_start_timestamp = -1;
        return m_rtsp_client.play();
    }

    private void loadMedia() {
        MediaInfo[] medias = new MediaInfo[3];
        medias[0] = m_rtsp_client.getAudioInfo();
        medias[1] = m_rtsp_client.getVideoInfo();
        medias[2] = m_rtsp_client.getVideoExtInfo();
        int i = 0;
        for(int j = 0 ; j < 3 ; ++j) {
            MediaInfo info = medias[j];
            if(info != null) {
                MediaFormat format = info.convert();
                if(format == null) {
                    LogManager.e(String.format("RTSPClient get %s Format failed", info.typeName()));
                    continue;
                }
                if(j == 2)
                    format.setInteger(MEDIAFORMAT_VIDEO_EXT_KEY, 1);
                m_formats.add(format);
                m_MID2TID.put(info.id, i);
                ++i;
            }
        }
        if(m_formats.size() == 0) {
            throw new RuntimeException("RTSPClient has no media on this url!");
        }
    }
    
    public MediaFormat[] enumTracks() {
        return m_formats.toArray(new MediaFormat[m_formats.size()]);
    }
    
    public void selectTrack(Integer track_id) {
        if(m_reading_ques[track_id] == null) {
            m_track_selected = true;
            m_reading_ques[track_id] = new AVChannel(
                    m_formats.get(track_id),
                    BUFFERED_PACKET_COUNT);
        }
    }
    
    public AVChannel getTrackChannel(int track_id) {
        return m_reading_ques[track_id];
    }

    @Override
    public void onConnectionBroken(int err) {
        if(m_cb != null)
            m_cb.onConnectionBroken(err);
        else
            LogManager.i("RTSPSource.onConnectionBroken " + err);
    }
    
    @Override
    public void onIncomingFrame(int media_id, byte []datas, long timestamp_us, int has_start_code) {
        Integer track_id = m_MID2TID.get(media_id);
        if(track_id == null) {
            LogManager.i("logical error");
            return;
        }
        
        AVChannel ch = m_reading_ques[track_id];
        if(ch == null)
            return;
        
        AVPacket pkt = null;
        do{
            try {
                pkt = ch.pollIdlePacket(WAIT_PKT_TIMEOUT_MS);
                if(pkt == null) {
                    LogManager.i("rtsp rx channel has no ilde item");
                    return;
                }
                
                pkt.reset();
                if(has_start_code > 0) {
                    ByteBuffer dst = pkt.getPayload();
                    if(dst.capacity() < datas.length) {
                        LogManager.w("pkt space is not enough");
                        break;
                    }
                    dst.put(datas);
                    dst.flip();
                } else {
                    if(!VideoDecoderWithChannel.addStartCode(datas, pkt.getPayload())) {
                        LogManager.w("pkt space is not enough");
                        break;
                    }
                }
                if(m_start_pts < 0)
                    m_start_pts = System.nanoTime() / 1000;
                if(m_start_timestamp < 0)
                    m_start_timestamp = timestamp_us;
                long pts = timestamp_us - m_start_timestamp + m_start_pts;
                pkt.setPts(pts);
                // pkt.setPts(timestamp_us);
                ch.putBusyPacket(pkt);
                pkt = null;
            } catch (InterruptedException e) {
                LogManager.w("thread Interrupted");
            }
        } while(false);
        
        if(pkt != null) {
            ch.putIdlePacket(pkt);
        }
    }
}
