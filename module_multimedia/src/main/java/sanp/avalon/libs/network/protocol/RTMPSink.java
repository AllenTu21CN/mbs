package sanp.avalon.libs.network.protocol;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import android.media.MediaFormat;

import sanp.avalon.libs.base.utils.LogManager;
import sanp.avalon.libs.media.base.AVChannel;
import sanp.avalon.libs.media.base.AVDefines.DataFlag;
import sanp.avalon.libs.media.base.AVDefines.DataType;
import sanp.avalon.libs.media.base.AVPacket;

public class RTMPSink implements Runnable, RTMPPushClient.Callback {

    public interface Callback {
        void onConnected();
        void onConnectFailed(int err);
        void onConnectionBroken(int err);
    };

    private static final long THREAD_EXIT_TIMEOUT_MS = 1000;
    private static final int  BUFFERED_PACKET_COUNT = 5;
    private static final long WAIT_PKT_TIMEOUT_MS = 80;
    
    private boolean m_track_added = false;
    private boolean m_connected = false;
    private Callback m_cb = null;

    private String m_url = "";
    private RTMPPushClient m_rtmp_client = null;
    private Thread m_thread = null;
    private AVChannel m_writing_que = new AVChannel(BUFFERED_PACKET_COUNT);
    private Map<Integer, MediaFormat> m_formats = new HashMap<>();
    private Map<Integer, byte[]> m_extradatas = new HashMap<>();
    private Map<Integer, Integer> m_track2stream_IDs = new HashMap<>();
    
    private int track_id_ref = 0;
    
    public RTMPSink(String url) {
        m_url = url;
        m_rtmp_client = RTMPPushClient.create(this);
        if(m_rtmp_client.init(url) != 0) {
            throw new RuntimeException("RTMPPushClient init failed!");
        }
    }
    
    public void release() {
        disconnect();
        
        if(m_rtmp_client != null) {
            m_rtmp_client.close();
            m_rtmp_client = null;
        }
        
        m_writing_que = null;
        
        if(m_formats != null) {
            m_formats.clear();
            m_formats = null;
        }

        if(m_extradatas != null) {
            m_extradatas.clear();
            m_extradatas = null;
        }

        if(m_track2stream_IDs != null) {
            m_track2stream_IDs.clear();
            m_track2stream_IDs = null;
        }
    }

    public int addTrack(MediaFormat format) {
        if(m_connected) {
            throw new RuntimeException("addTrack MUST be before connect");
        }
        
        m_formats.put(track_id_ref, format);
        m_extradatas.put(track_id_ref, null);
        m_track_added = false;
        return track_id_ref++;
    }
    
    public int addTrack(MediaFormat format, byte[] extradata) {
        if(m_connected) {
            throw new RuntimeException("addTrack MUST be before connect");
        }

        byte[] myextradata = extradata.clone();
        int stream_id = m_rtmp_client.addStream(format, myextradata);
        if(stream_id < 0)
            return stream_id;
        
        m_formats.put(track_id_ref, format);
        m_extradatas.put(track_id_ref, myextradata);
        m_track2stream_IDs.put(track_id_ref, stream_id);
        if(m_track2stream_IDs.size() == m_formats.size())
            m_track_added = true;
        return track_id_ref++;
    }

    public int cfgTrack(int track_id, byte[] extradata) {
        if(m_track2stream_IDs.containsKey(track_id)) {
            throw new RuntimeException("extradata had been added while calling addTrack");
        }
        if(!m_formats.containsKey(track_id)) {
            throw new RuntimeException("call addTrack first");
        }

        byte[] myextradata = extradata.clone();
        MediaFormat format = m_formats.get(track_id);
        int stream_id = m_rtmp_client.addStream(format, extradata);
        if(stream_id < 0)
            return stream_id;

        m_extradatas.put(track_id, myextradata);
        m_track2stream_IDs.put(track_id, stream_id);
        if(m_track2stream_IDs.size() == m_formats.size())
            m_track_added = true;
        return track_id;
    }

    public Map<Integer, MediaFormat> enumTracks() {
        return m_formats;
    }

    public void connect(Callback cb) {
        if(m_formats.size() == 0) {
            throw new RuntimeException("addTrack first");
        }

        m_cb = cb;
        boolean connected = true;
        if(m_track_added) {
            int ret = m_rtmp_client.connect();
            if(ret != 0) {
                m_cb.onConnectFailed(ret);
                return;
            }
        } else {
            LogManager.i("RTMPSink(" + m_url + ") formats lost some extradata, delay to connect to server");
        }

        if(m_thread == null) {
            m_thread = new Thread(this, "RTMPSink");
            m_connected = true;
            m_thread.start();
        }
        if(connected)
            m_cb.onConnected();
    }

    public void disconnect() {
        if(m_thread != null) {
            m_connected = false;
            AVPacket pkt = m_writing_que.pollIdlePacket();
            if(pkt != null) {
                pkt.reset();
                m_writing_que.offerBusyPacket(pkt);
            }
            try {  m_thread.join(THREAD_EXIT_TIMEOUT_MS); } catch (InterruptedException e) {LogManager.e(e); }
            m_thread = null;
        }

        if(m_rtmp_client != null)
            m_rtmp_client.disconnect();
         m_cb = null;
    }

    public void reconnect() {
        // backup
        Callback cb = m_cb;

        // close client first
        disconnect();
        if(m_rtmp_client != null) {
            m_rtmp_client.close();
            m_rtmp_client = null;
        }
        m_writing_que.clearBusyPackets();
        m_track2stream_IDs.clear();
        m_track_added = false;

        // create again
        m_rtmp_client = RTMPPushClient.create(this);
        if(m_rtmp_client.init(m_url) != 0)
            throw new RuntimeException("RTMPPushClient init failed!");

        // add stream again
        flushTracks();

        // connect again
        connect(cb);
    }

    public AVChannel getChannel() {
        return m_writing_que;
    }

    class BreakoutThread extends Exception {}
    private void checkState() throws BreakoutThread {
        if(!m_connected) throw new BreakoutThread();
    }
    
    private void resetPkt(AVPacket pkt) {
        pkt.reset();
        m_writing_que.putIdlePacket(pkt);
    }

    private void flushTracks() {
        for(int track_id: m_formats.keySet()) {
            MediaFormat format = m_formats.get(track_id);
            byte[] extradata = m_extradatas.get(track_id);

            int stream_id = m_rtmp_client.addStream(format, extradata);
            if(stream_id < 0)
                throw new RuntimeException("m_rtmp_client addStream failed");

            m_track2stream_IDs.put(track_id, stream_id);
            if(m_track2stream_IDs.size() == m_formats.size())
                m_track_added = true;
        }
    }

    @Override
    public void run() {
        
        LogManager.i("RTMPSink thread started!");
        AVPacket pkt = null;
        long first_video_pts = -1;
        long last_video_pts = -1;
        boolean is_wait_iframe = true;
        boolean is_weak = false;
        long encoded_frames = 0;

        try {
            while(true) {
                checkState();
                
                pkt = m_writing_que.pollBusyPacket(WAIT_PKT_TIMEOUT_MS);
                if(pkt == null)
                    continue;
                checkState();

                if(is_weak) {
                    resetPkt(pkt);
                    continue;
                }

                ByteBuffer payload = pkt.getPayload();
                int track_id = pkt.getTrackIndex();

                if(!m_track_added && pkt.getDataFlag() == DataFlag.CODEC_SPECIFIC_DATA) {
                    int position = payload.position();
                    byte[] extradata = new byte[payload.limit()-position];
                    payload.get(extradata);
                    payload.position(position);
                    cfgTrack(track_id, extradata);
                    if(m_track_added) {
                        int ret = m_rtmp_client.connect();
                        if(ret != 0) {
                            m_cb.onConnectionBroken(ret);
                            is_weak = true;
                        }
                        first_video_pts = -1;
                        checkState();
                    }

                    // RTMP is not interested in this frame, ignore it.
                    // In some case, maybe it is necessary.
                    resetPkt(pkt);
                    continue;
                }

                if(m_track_added) {
                    if(is_wait_iframe) {
                        if(pkt.getMediaType() != DataType.VIDEO || !pkt.isKeyFrame()) {
                            resetPkt(pkt);
                            continue;
                        }
                        is_wait_iframe = false;
                    }

                    int flag = 0;
                    long pts = pkt.getPts();
                    if(pkt.getMediaType() == DataType.VIDEO) {
                        if(first_video_pts == -1)
                            first_video_pts = pts;
                        if(pkt.isKeyFrame())
                            flag |= RTMPPushClient.WRITING_FLAG_KEYFRAME;
                        ++encoded_frames;
                    } else {
                        if(first_video_pts == -1 || pts < first_video_pts) {
                            resetPkt(pkt);
                            continue;
                        }
                    }
                    pts = pts - first_video_pts;
                    if(pkt.getMediaType() == DataType.VIDEO) {
                        pts = pts / 1000;
                        if(last_video_pts != -1 && pts <= last_video_pts) {
                            pts = last_video_pts + 1;
                            LogManager.w("!!! fix encoded frame pts");
                        }
                        last_video_pts = pts;
                        pts = pts * 1000;
                    }
                    int stream_id = m_track2stream_IDs.get(track_id);
                    m_rtmp_client.write(stream_id, payload.array(), payload.position(), payload.remaining(), pts, flag);
                    //LogManager.i("id-" + stream_id + " pts-" + pts);
                }
                resetPkt(pkt);
            }
        } catch (BreakoutThread | InterruptedException e) {
            if(pkt != null) {
                resetPkt(pkt);
            }
        }
        LogManager.i("RTMPSink thread exit...! with " + encoded_frames + " frames");
    }

    @Override
    public void onConnectionBroken(int err) {
        if(m_cb != null)
            m_cb.onConnectionBroken(err);
        else
            LogManager.i("RTMPSink.onConnectionBroken " + err);
    }
}
