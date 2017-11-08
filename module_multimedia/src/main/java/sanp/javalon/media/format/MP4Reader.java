package sanp.javalon.media.format;

import java.io.IOException;

import android.media.MediaExtractor;
import android.media.MediaFormat;

import sanp.tools.utils.LogManager;
import sanp.tools.utils.SpeedController;
import sanp.javalon.media.base.AVChannel;
import sanp.javalon.media.base.AVPacket;

public class MP4Reader implements Runnable {
    
    private static final int  BUFFERED_PACKET_COUNT = 3;
    private static final long THREAD_EXIT_TIMEOUT_MS = 1000;
    private static final long WAIT_PKT_TIMEOUT_MS = 33; // = 1000ms/60fps*2
    
    private String          m_file = "";
    private long            m_seek_time_us = -1;
    private Thread          m_reading_thread;
    private MediaExtractor  m_extractor;
    private long            m_pts_adding_us = 0;
    private boolean         m_loop_play = false;
    private boolean         m_started= false;
    private boolean         m_track_selected = false;
    private SpeedController m_speed_controller;
    private AVChannel[]     m_reading_ques;

    public MP4Reader(String file, boolean loop) throws IOException {

        m_file = file;
        m_loop_play = loop;
        m_reading_thread = null;
        m_reading_ques = null;

        m_extractor = new MediaExtractor();
        m_extractor.setDataSource(m_file.toString());
        
        int cnt = m_extractor.getTrackCount();
        m_reading_ques = new AVChannel[cnt];
        for(int i = 0 ; i < cnt ; ++i) {
            m_reading_ques[i] = null;  
        }

        m_speed_controller = new SpeedController();
        m_reading_thread = new Thread(this, String.format("MP4Reader[%s]", file));
    }
    
    public void release() {
        stop();
        m_extractor.release();
        m_extractor = null;
        m_speed_controller = null;
        m_reading_ques = null;
        m_reading_thread = null;
    }
    
    public MediaFormat[] enumTracks() {
        int cnt = m_extractor.getTrackCount();
        MediaFormat[] tracks = new MediaFormat[cnt];
        for (int i = 0; i < cnt; i++) {
            tracks[i] = m_extractor.getTrackFormat(i);
        }
        return tracks;
    }
    
    public void selectTrack(int track_id) {
        if(m_reading_ques[track_id] == null) {
            m_extractor.selectTrack(track_id);
            m_track_selected = true;
            m_reading_ques[track_id] = new AVChannel(
                    m_extractor.getTrackFormat(track_id),
                    BUFFERED_PACKET_COUNT);
        }
    }
    
    public void seek(long timeUs) {
        if(m_started) {
            m_extractor.seekTo(timeUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        } else {
            m_seek_time_us = timeUs;
        }
    }
    
    public AVChannel getTrackChannel(int track_id) {
        return m_reading_ques[track_id];
    }
    
    public void start() {
        if(m_started)
            return;
        if(!m_track_selected) {
            throw new RuntimeException("selectTrack first");
        }
        
        if(m_seek_time_us >= 0) {
            m_extractor.seekTo(m_seek_time_us, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
            m_seek_time_us = -1;
        }

        m_started = true;
        m_reading_thread.start();
    }
    
    public void stop() {
        if(!m_started)
            return;

        m_started = false;
        stopThread();
        m_seek_time_us = 0;
        m_pts_adding_us = 0;
        m_speed_controller.reset();
    }
    
    private void stopThread() {
        m_started = false;
        for (AVChannel ch: m_reading_ques) {
            if(ch != null) {
                AVPacket pkt = ch.pollIdlePacket();
                if(pkt != null) {
                    pkt.reset();
                    ch.offerBusyPacket(pkt);
                }
            }
        }
        try {  m_reading_thread.join(THREAD_EXIT_TIMEOUT_MS); } catch (InterruptedException e) {LogManager.e(e); }
        for (AVChannel ch: m_reading_ques) {
            if(ch != null) {
                //TODO: ch.reset();
            }
        }
    }
    
    @Override
    public void run() {
        LogManager.i(String.format("Start to read mp4(%s)", m_file));

        int track_id = -1;
        long last_sample_pts_us = 0;
        long curr_pts_us = 0;
        AVPacket pkt = null;
        while (m_started) {
            
            track_id = m_extractor.getSampleTrackIndex();
            if(track_id == -1) {
                if(m_loop_play) {
                    m_extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
                    m_pts_adding_us += last_sample_pts_us;
                    LogManager.i("Mp4 loop to beginning");
                } else {
                    LogManager.i("End of mp4");
                    break;
                }
            } else {
                AVChannel ch = m_reading_ques[track_id];
                if(ch == null) {
                    LogManager.i("logical error");
                    continue;
                }
                
                try {
                    pkt = ch.pollIdlePacket(WAIT_PKT_TIMEOUT_MS);
                    if(pkt == null) {
                        LogManager.i("mp4 tx channel has no ilde item");
                        continue;
                    }
                } catch (InterruptedException e) {
                    LogManager.w("thread Interrupted");
                    break;
                }
                
                m_extractor.readSampleData(pkt.getPayload(), 0);
                last_sample_pts_us = m_extractor.getSampleTime();
                curr_pts_us = last_sample_pts_us + m_pts_adding_us;
                pkt.setPts(curr_pts_us);
                ch.putBusyPacket(pkt);
                
                m_extractor.advance();
                m_speed_controller.preRender(curr_pts_us);
            }
        }
        
        // Mark end of stream
        for(AVChannel ch: m_reading_ques) {
            if(ch != null) {
                pkt = ch.pollIdlePacket();
                if(pkt != null) {
                    pkt.reset();
                    ch.offerBusyPacket(pkt);
                }
            }
        }
        //TODO: marking for end of mp4
        LogManager.i("Mp4Player reading thread exit...!");
    }
}

