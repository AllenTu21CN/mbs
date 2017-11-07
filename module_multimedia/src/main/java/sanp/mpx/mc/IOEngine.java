package sanp.mpx.mc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.media.MediaFormat;
import android.util.Pair;

import sanp.tools.utils.LogManager;
import sanp.avalon.libs.base.Tuple3;
import sanp.avalon.libs.media.base.AVChannel;
import sanp.avalon.libs.media.base.AVDefines.DataType;
import sanp.avalon.libs.media.format.MP4Muxer;
import sanp.avalon.libs.media.format.MP4Reader;
import sanp.avalon.libs.network.protocol.RTSPSource;
import sanp.avalon.libs.network.protocol.RTMPSink;

public class IOEngine {

    public enum IOFlags {
        IO_NULL,
        IO_RD,
        IO_WR,
    }

    public class TrackInfo {
        public int trackId = -1;
        public MediaFormat format = null;
        public IOFlags flag = IOFlags.IO_NULL;
        public DataType type = DataType.UNKNOWN;
        TrackInfo() {}
        TrackInfo(int trackId, MediaFormat format, IOFlags flag, DataType type) {
            this.trackId = trackId;
            this.format = format;
            this.flag = flag;
            this.type = type;
        }
    }

    public class IOChannel {
        public int sessionID = -1;
        public int trackID = -1;
        public AVChannel rChannel = null;
        public AVChannel wChannel = null;
        IOChannel(int SID, int TID) {
            sessionID = SID;
            trackID = TID;
        } 
        IOChannel(int SID, int TID, AVChannel r, AVChannel w) {
            sessionID = SID;
            trackID = TID;
            rChannel = r;
            wChannel = w;
        }
    }

    abstract static class IOSession {

        public static class asyncActionCallback {
            private Object m_params;
            public asyncActionCallback(Object extParams) {
                m_params = extParams;
            }
            public Object getExtParams() { return m_params; }
            public void onOpening(int err) {
                throw new RuntimeException("logical error");
            }
            public void onStarting(int err) {
                throw new RuntimeException("logical error");
            }
        }

        public interface Observer {
            void onBroken(IOFlags flag, int id, String url, int err);
        }

        public int sessionID = -1;
        protected String  m_name;
        protected String  m_url;
        protected IOFlags m_flag;
        protected Observer m_ob;

        public IOSession(int id, String name, IOFlags flag, String url, Observer ob) {
            sessionID = id;
            m_name = name;
            m_url = url;
            m_flag = flag;
            m_ob = ob;
        }
        public abstract void close();
        public abstract void asyncOpen(asyncActionCallback cb);
        public void asyncReOpen(asyncActionCallback cb) {
            throw new RuntimeException(m_name + "(" + sessionID + ") don't support this action");
        }
        public abstract void asyncStart(asyncActionCallback cb);
        public abstract void stop();
        public TrackInfo[] getTrackInfos() {
            throw new RuntimeException(m_name + "(" + sessionID + ") don't support this action");
        }
        public IOChannel getTrackIO(int track_id) {
            throw new RuntimeException(m_name + "(" + sessionID + ") don't support this action");
        }
        public int addTrack(DataType type, MediaFormat format) {
            throw new RuntimeException(m_name + "(" + sessionID + ") don't support this action");
        }
        public int addTrack(DataType type, MediaFormat format, byte[] extradata) {
            throw new RuntimeException(m_name + "(" + sessionID + ") don't support this action");
        }
        public boolean retryForOpenFailed() {
            return false;
        }
    }
    
    class MP4MuxerSession extends IOSession {
        private MP4Muxer m_muxer = null;
        private Map<Integer, DataType> m_types = null;

        MP4MuxerSession(int id, String url, Observer ob) {
            super(id, "MP4MuxerSession", IOFlags.IO_WR, url, ob);
            m_types = new HashMap<>();
            try {
                m_muxer = new MP4Muxer(m_url);
            } catch (IOException e) {
                LogManager.w("create MP4Muxer failed:");
                e.printStackTrace();
                m_muxer = null;
            }
        }

        @Override
        public void close() {
            stop();
            m_types = null;
            if(m_muxer != null) {
                m_muxer.release();
                m_muxer = null;
            }
        }

        @Override
        public void asyncOpen(asyncActionCallback cb) {
            if (m_muxer != null) {
                cb.onOpening(0);
            }else {
                cb.onOpening(-1);
            }
        }

        @Override
        public void asyncStart(asyncActionCallback cb) {
            if(m_muxer == null)
                throw new RuntimeException("Logical error");
            m_muxer.start();
            cb.onStarting(0);
        }
        
        @Override
        public void stop() {
            if(m_muxer != null)
                m_muxer.stop();
        }

        @Override
        public TrackInfo[] getTrackInfos() {
            Map<Integer, MediaFormat> tracks = m_muxer.enumTracks();
            int cnt = tracks.size();
            TrackInfo[] tracks_info = new TrackInfo[cnt];

            int i = 0;
            for (Map.Entry<Integer, MediaFormat> entry : tracks.entrySet()) {
                int trackId = entry.getKey();
                tracks_info[i] = new TrackInfo(trackId, entry.getValue(), m_flag, m_types.get(trackId));
                ++i;
            }
            return tracks_info;
        }
        
        @Override
        public IOChannel getTrackIO(int track_id) {
            return new IOChannel(sessionID, track_id, null, m_muxer.getChannel());
        }
        
        @Override
        public int addTrack(DataType type, MediaFormat format) {
            int trackId = m_muxer.addTrack(format);
            m_types.put(trackId, type);
            return trackId;
        }
    }
    
    class MP4ExtractorSession extends IOSession {
        private MP4Reader m_reader = null;
        private boolean m_loop_play = false;
        private TrackInfo[] m_tracks_info = null;
        
        MP4ExtractorSession(int id, String url, boolean loop, Observer ob) {
            super(id, "MP4ExtractorSession", IOFlags.IO_RD, url, ob);
            m_loop_play = loop;
        }

        @Override
        public void close() {
            stop();
            m_tracks_info = null;
            if(m_reader != null) {
                m_reader.release();
                m_reader = null;
            }
        }

        @Override
        public void asyncOpen(asyncActionCallback cb) {
            if(m_reader != null)
                throw new RuntimeException("Logical error");
            try {
                m_reader = new MP4Reader(m_url, m_loop_play);
                loadTrackInfo();
                cb.onOpening(0);
            } catch (IOException e) {
                LogManager.w("create MP4Reader failed:");
                e.printStackTrace();
                cb.onOpening(-1);
            }
        }

        @Override
        public void asyncStart(asyncActionCallback cb) {
            if(m_reader == null)
                throw new RuntimeException("Logical error");
            m_reader.start();
            cb.onStarting(0);
        }
        
        @Override
        public void stop() {
            if(m_reader != null)
                m_reader.stop();
        }

        @Override
        public TrackInfo[] getTrackInfos() {
            return m_tracks_info;
        }
        
        @Override
        public IOChannel getTrackIO(int track_id) {
            m_reader.selectTrack(track_id);
            return new IOChannel(sessionID, track_id, m_reader.getTrackChannel(track_id), null);
        }

        private void loadTrackInfo() {
            TrackInfo audio = null;
            TrackInfo video = null;
            TrackInfo video_ext = null;
            MediaFormat[] tracks = m_reader.enumTracks();
            int cnt = tracks.length;
            for(int i = 0 ; i < cnt ; ++i) {
                MediaFormat format = tracks[i];
                String name = format.getString(MediaFormat.KEY_MIME);
                if(name.startsWith("video")) {
                    if(video == null)
                        video = new TrackInfo(i, format, m_flag, DataType.VIDEO);
                    else if(video_ext == null)
                        video_ext = new TrackInfo(i, format, m_flag, DataType.VIDEO_EXT);
                } else if(name.startsWith("audio")) {
                    if(audio == null)
                        audio = new TrackInfo(i, format, m_flag, DataType.AUDIO);
                }
            }
            m_tracks_info = new TrackInfo[cnt];
            if(audio != null)
                m_tracks_info[audio.trackId] = audio;
            if(video != null)
                m_tracks_info[video.trackId] = video;
            if(video_ext != null)
                m_tracks_info[video_ext.trackId] = video_ext;
        }
    }

    class RTSPClientSession extends IOSession implements RTSPSource.Callback {
        private RTSPSource m_source = null;
        private TrackInfo[] m_tracks_info = null;

        RTSPClientSession(int id, String url, Observer ob) {
            super(id, "RTSPClientSession", IOFlags.IO_RD, url, ob);
        }

        @Override
        public boolean retryForOpenFailed() {
            return true;
        }

        @Override
        public void close() {
            stop();
            m_tracks_info = null;
            if(m_source != null) {
                m_source.release();
                m_source = null;
            }
        }

        @Override
        public void asyncOpen(asyncActionCallback cb) {
            if(m_source != null)
                throw new RuntimeException("Logical error");
            m_source = new RTSPSource(m_url);
            int ret = m_source.connect(this);
            if(ret == 0)
                loadTrackInfo();

            cb.onOpening(ret);
        }

        @Override
        public void asyncReOpen(asyncActionCallback cb) {
            if(m_source == null)
                throw new RuntimeException("Logical error");
            int ret = m_source.reconnect();
            cb.onOpening(ret);
        }

        @Override
        public void asyncStart(asyncActionCallback cb) {
            if(m_source == null)
                throw new RuntimeException("Logical error");
            int ret = m_source.start();
            cb.onStarting(ret);
        }
        
        @Override
        public void stop() {
            if(m_source != null)
                m_source.disconnect();
        }

        @Override
        public TrackInfo[] getTrackInfos() {
            return m_tracks_info;
        }
        
        @Override
        public IOChannel getTrackIO(int track_id) {
            if(m_source == null)
                return null;
            m_source.selectTrack(track_id);
            return new IOChannel(sessionID, track_id, m_source.getTrackChannel(track_id), null);
        }

        @Override
        public void onConnectionBroken(int err) {
            if(m_ob != null)
                m_ob.onBroken(m_flag, sessionID, m_url, err);
            else
                LogManager.i("RTSPClientSession.onConnectionBroken: " + sessionID + "(" + m_url + "): " + err);
        }

        private void loadTrackInfo() {
            MediaFormat[] tracks = m_source.enumTracks();
            int cnt = tracks.length;
            m_tracks_info = new TrackInfo[cnt];
            for(int i = 0 ; i < cnt ; ++i) {
                MediaFormat format = tracks[i];
                DataType type = DataType.UNKNOWN;
                String name = format.getString(MediaFormat.KEY_MIME);
                if(name.startsWith("video")) {
                    if(format.containsKey(RTSPSource.MEDIAFORMAT_VIDEO_EXT_KEY))
                        type = DataType.VIDEO_EXT;
                    else
                        type = DataType.VIDEO;
                } else if(name.startsWith("audio")) {
                    type = DataType.AUDIO;
                }
                m_tracks_info[i] = new TrackInfo(i, format, m_flag, type);
            }
        }
    }
    
    class RTMPSinkSession extends IOSession implements RTMPSink.Callback {
        private RTMPSink m_sink = null;
        private asyncActionCallback m_cb = null;
        private Map<Integer, DataType> m_types = null;

        RTMPSinkSession(int id, String url, Observer ob) {
            super(id, "RTMPSinkSession", IOFlags.IO_WR, url, ob);
            m_sink = new RTMPSink(m_url);
            m_types = new HashMap<>();
        }

        @Override
        public boolean retryForOpenFailed() {
            return true;
        }

        @Override
        public void close() {
            stop();
            m_types = null;
            if(m_sink != null) {
                m_sink.release();
                m_sink = null;
            }
        }

        @Override
        public void asyncOpen(asyncActionCallback cb) {
            if(m_sink == null)
                throw new RuntimeException("Logical error");
            m_cb = cb;
            m_sink.connect(this);
        }

        @Override
        public void asyncReOpen(asyncActionCallback cb) {
            if(m_sink == null)
                throw new RuntimeException("Logical error");
            m_cb = cb;
            m_sink.reconnect();
        }

        @Override
        public void asyncStart(asyncActionCallback cb) {
            // Do nothing
            cb.onStarting(0);
        }

        @Override
        public void stop() {
            // Do nothing
        }

        @Override
        public TrackInfo[] getTrackInfos() {
            Map<Integer, MediaFormat> tracks = m_sink.enumTracks();
            int cnt = tracks.size();
            TrackInfo[] tracks_info = new TrackInfo[cnt];

            int i = 0;
            for (Map.Entry<Integer, MediaFormat> entry: tracks.entrySet()) {
                int trackId = entry.getKey();
                tracks_info[i] = new TrackInfo(trackId, entry.getValue(), m_flag, m_types.get(trackId));
                ++i;
            }
            return tracks_info;
        }
        
        @Override
        public IOChannel getTrackIO(int track_id) {
            return new IOChannel(sessionID, track_id, null, m_sink.getChannel());
        }
        
        @Override
        public int addTrack(DataType type, MediaFormat format) {
            int trackId = m_sink.addTrack(format);
            m_types.put(trackId, type);
            return trackId;
        }
        
        @Override
        public int addTrack(DataType type, MediaFormat format, byte[] extradata) {
            int trackId = m_sink.addTrack(format, extradata);
            m_types.put(trackId, type);
            return trackId;
        }

        @Override
        public void onConnected() {
            if(m_cb != null)
                m_cb.onOpening(0);
            else
                LogManager.i("RTMPSinkSession.onConnected: " + sessionID + "(" + m_url + ")");
        }

        @Override
        public void onConnectFailed(int err) {
            if(m_cb != null)
                m_cb.onOpening(err);
            else
                LogManager.i("RTMPSinkSession.onConnectFailed: " + sessionID + "(" + m_url + "): " + err);
        }

        @Override
        public void onConnectionBroken(int err) {
            if(m_ob != null)
                m_ob.onBroken(m_flag, sessionID, m_url, err);
            else
                LogManager.i("RTMPSinkSession.onConnectionBroken: " + sessionID + "(" + m_url + "): " + err);
        }
    }

    class DummySession extends IOSession {
        DummySession(int id) {
            super(id, "DummySession", IOFlags.IO_NULL, "", null);
        }

        @Override
        public void close() {
        }
        @Override
        public void asyncOpen(asyncActionCallback cb) {
            throw new RuntimeException(m_name + "(" + sessionID + ") don't support this action");
        }
        @Override
        public void asyncStart(asyncActionCallback cb) {
            throw new RuntimeException(m_name + "(" + sessionID + ") don't support this action");
        }
        @Override
        public void stop() {
        }
    }

    class ExtraReadingSession extends IOSession {
        private Map<Integer, AVChannel> m_channels = null;
        private TrackInfo[] m_tracks_info = null;

        ExtraReadingSession(int id, String url, Map<Integer, Tuple3<DataType, MediaFormat, AVChannel>> tracks) {
            super(id, "ExtraReadingSession", IOFlags.IO_RD, url, null);

            m_channels = new HashMap<>();
            m_tracks_info = new TrackInfo[tracks.size()];
            int i = 0;
            for(int track_id: tracks.keySet()) {
                Tuple3<DataType, MediaFormat, AVChannel> track = tracks.get(track_id);
                m_channels.put(track_id, track.third);
                m_tracks_info[i++] = new TrackInfo(track_id, track.second, m_flag, track.first);
            }
        }

        @Override
        public void close() {
            stop();
            m_channels = null;
            m_tracks_info = null;
        }

        @Override
        public void asyncOpen(asyncActionCallback cb) {
            cb.onOpening(0);
        }

        @Override
        public void asyncStart(asyncActionCallback cb) {
            cb.onStarting(0);
        }

        @Override
        public void stop() {
            // nothing
        }

        @Override
        public TrackInfo[] getTrackInfos() {
            return m_tracks_info;
        }

        @Override
        public IOChannel getTrackIO(int track_id) {
            AVChannel ch = m_channels.get(track_id);
            if(ch == null)
                return null;
            return new IOChannel(sessionID, track_id, ch, null);
        }
    }

    class ExtraWritingSession extends IOSession {
        private Map<Integer, AVChannel> m_channels = null;
        private Map<DataType, Pair<Integer, MediaFormat>> m_tracks = null;

        ExtraWritingSession(int id, String url, Map<Integer, Pair<DataType, AVChannel>> tracks) {
            super(id, "ExtraWritingSession", IOFlags.IO_WR, url, null);

            m_channels = new HashMap<>();
            m_tracks = new HashMap<>();
            for(int track_id: tracks.keySet()) {
                Pair<DataType, AVChannel> track = tracks.get(track_id);
                m_channels.put(track_id, track.second);
                m_tracks.put(track.first, new Pair(track_id, null));
            }
        }

        @Override
        public void close() {
            stop();
        }

        @Override
        public void asyncOpen(asyncActionCallback cb) {
            cb.onOpening(0);
        }

        @Override
        public void asyncStart(asyncActionCallback cb) {
            cb.onStarting(0);
        }

        @Override
        public void stop() {
            // do nothing
        }

        @Override
        public TrackInfo[] getTrackInfos() {
            List<TrackInfo> tracks_info = new ArrayList<>();
            for(Map.Entry<DataType, Pair<Integer, MediaFormat>> entry: m_tracks.entrySet()) {
                DataType type = entry.getKey();
                int trackId = entry.getValue().first;
                MediaFormat format = entry.getValue().second;
                if(format != null)
                    tracks_info.add(new TrackInfo(trackId, format, m_flag, type));
            }
            return (TrackInfo[]) tracks_info.toArray();
        }

        @Override
        public IOChannel getTrackIO(int track_id) {
            AVChannel ch = m_channels.get(track_id);
            if(ch == null)
                return null;
            return new IOChannel(sessionID, track_id, null, ch);
        }

        @Override
        public int addTrack(DataType type, MediaFormat format) {
            Pair<Integer, MediaFormat> track = m_tracks.get(type);
            if(track == null)
                return -1;
            m_tracks.put(type, new Pair(track.first, format));
            return track.first;
        }
    }

    private int m_session_counter;
    private Map<Integer, IOSession> m_sessions; 
    private Pattern m_url_mp4_pattern;
    
    public IOEngine() {
        m_session_counter = 0;
        m_sessions = new HashMap<>();
        m_url_mp4_pattern = Pattern.compile("^(.+\\.mp4)(\\?.+)*$", Pattern.CASE_INSENSITIVE);
    }

    public int addDummySession() {
        m_sessions.put(m_session_counter, new DummySession(m_session_counter));
        return m_session_counter++;
    }

    /**
     * Add a session, it is first of all actions.
     * @param url refer to RFC1738
     *  For MP4Muxer, it looks like: "file:///e:/test.mp4"
     *  For MP4Extractor, it looks like: "http://10.1.0.75/test-3.mp4?loop=true"
     *  For RTSPClient, it looks like: "rtsp://10.3.0.114:5000/main.h264"
     *  For RTMPPushClient, it looks like: "rtmp://..."
     * @return this session id
     * @throws RuntimeException
     * */
    public int addSession(String url, IOFlags flag) {
        return addSession(url, flag, null, null);
    }
    public int addSession(String url, IOFlags flag, IOSession.Observer ob) {
        return addSession(url, flag, ob, null);
    }
    public int addSession(String url, IOFlags flag, IOSession.Observer ob, Object extra) {
        Matcher mp4_matcher = m_url_mp4_pattern.matcher(url);
        if(mp4_matcher.matches()) {
            url = mp4_matcher.group(1);
            String ps = mp4_matcher.group(2);
            if(flag == IOFlags.IO_RD) {
                boolean loop = false;
                if(ps != null)
                    ps = ps.substring(1);
                else
                    ps = "";
                String[] params = ps.split("&");
                for(String param: params) {
                    if(param.startsWith("loop")) {
                        loop = param.split("=")[1].equals("true");
                    }
                }
                
                IOSession session = new MP4ExtractorSession(m_session_counter, url, loop, ob);
                m_sessions.put(m_session_counter, session);
                return m_session_counter++;
            } else if(flag == IOFlags.IO_WR) {
                IOSession session = new MP4MuxerSession(m_session_counter, url, ob);
                m_sessions.put(m_session_counter, session);
                return m_session_counter++;
            } else {
                throw new RuntimeException("invalid IOFlag for MP4");
            }
        } else if(url.startsWith("rtsp://")) {
            if(flag != IOFlags.IO_RD) {
                throw new RuntimeException("RTSPClient just support IO_RD mode");
            }
            IOSession session = new RTSPClientSession(m_session_counter, url, ob);
            m_sessions.put(m_session_counter, session);
            return m_session_counter++;
        } else if(url.startsWith("rtmp://")) {
            if(flag == IOFlags.IO_WR) {
                IOSession session = new RTMPSinkSession(m_session_counter, url, ob);
                m_sessions.put(m_session_counter, session);
                return m_session_counter++;
            }
            throw new RuntimeException("just support RTMP with IO_WR mode");
        } else if(url.startsWith("sip:") || url.startsWith("h323:")) {
            if(flag == IOFlags.IO_WR) {
                IOSession session = new ExtraWritingSession(m_session_counter, url, (Map<Integer, Pair<DataType, AVChannel>>) extra);
                m_sessions.put(m_session_counter, session);
                return m_session_counter++;
            } else if(flag == IOFlags.IO_RD) {
                IOSession session = new ExtraReadingSession(m_session_counter, url, (Map<Integer, Tuple3<DataType, MediaFormat, AVChannel>>) extra);
                m_sessions.put(m_session_counter, session);
                return m_session_counter++;
            }
        }
        throw new RuntimeException(String.format("unknow url:%s", url));
    }
    
    public void removeSession(int session_id) {
        IOSession session = m_sessions.remove(session_id);
        if(session != null)
            session.close();
    }

    public boolean retryForOpenFailed(int session_id) {
        return m_sessions.get(session_id).retryForOpenFailed();
    }

    public void closeSession(int session_id) {
        m_sessions.get(session_id).close();
    }

    public void asyncOpenSession(int session_id, IOSession.asyncActionCallback cb) {
        m_sessions.get(session_id).asyncOpen(cb);
    }

    public void asyncReOpenSession(int session_id, IOSession.asyncActionCallback cb) {
        m_sessions.get(session_id).asyncReOpen(cb);
    }

    /**
     * Start the session to read/write datas.
     *  Please call `getTrackIO` or `addTrack` before this action.
     * */
    public void asyncStartSession(int session_id, IOSession.asyncActionCallback cb) {
        m_sessions.get(session_id).asyncStart(cb);
    }

    /**
     * Stop the session to read/write datas.
     * */
    public void stopSession(int session_id) {
        m_sessions.get(session_id).stop();
    }
    
    /**
     * Get all tracks's information in the session.
     * */
    public TrackInfo[] getTrackInfos(int session_id) {
        return m_sessions.get(session_id).getTrackInfos();
    }

    /**
     * Get the special track IO-channels in the session.
     * */
    public IOChannel getTrackIO(int session_id, int track_id) {
        return m_sessions.get(session_id).getTrackIO(track_id);
    }
    
    /**
     * Add a only-writing-able track to the session.
     * @return the new track id
     * */
    public int addTrack(int session_id, DataType type, MediaFormat format) {
        return m_sessions.get(session_id).addTrack(type, format);
    }
    public int addTrack(int session_id, DataType type, MediaFormat format, byte[] extradata) {
        return m_sessions.get(session_id).addTrack(type, format, extradata);
    }
}








