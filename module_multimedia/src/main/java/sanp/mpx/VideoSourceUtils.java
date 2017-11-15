package sanp.mpx;

import java.util.ArrayList;
import java.util.List;

import sanp.tools.utils.LogManager;

/**
 * Created by Tuyj on 2017/6/20.
 */

public class VideoSourceUtils {

    public enum SourceType {
        UNDEFINED, // 未定义
        HDMI_IN,
        INTEGRATED_CAMERA,  // 内置
        USB_CAMERA,
        IP_CAMERA,
    };

    public class Source {

        public SourceType Type;
        public String Name;
        public String Url;

        public Source(SourceType type, String name, String url) {
            Type = type;
            Name = name;
            Url = url;
        }

        public SourceType getType() {
            return Type;
        }

        public String getName() {
            return Name;
        }

        public void setName(String name) {
            Name = name;
        }

        public String getUrl() {
            return Url;
        }

        public void setUrl(String url) {
            Url = url;
        }
    }

    public interface Callback {
        void onSourceList(List<Source> sources);
    };

    static private VideoSourceUtils mVideoSourceUtils = null;
    static public VideoSourceUtils getInstance() {
        synchronized (VideoSourceUtils.class) {
            if(mVideoSourceUtils == null)
                mVideoSourceUtils = new VideoSourceUtils();
        }
        return mVideoSourceUtils;
    }

    private int mScanIntervalMs = -1;
    private Callback mCallback = null;
    private boolean mStarted = false;
    private Thread mScanThread = null;
    private List<Source> mSources = new ArrayList<>();

    public void start(int scanIntervalMs) {
        start(scanIntervalMs, null);
    }

    public void start(int scanIntervalMs, Callback cb) {
        if(mStarted)
            return;
        mCallback = cb;
        mScanIntervalMs = scanIntervalMs;
        mStarted = true;
        mScanThread = new Thread(new Runnable() {
            public void run() { scanLoop(); }
        }, "VideoSourceUtils scan thread");
        mScanThread.start();
    }

    public void stop() {
        mStarted = false;
        if(mScanThread != null) {
            try {
                mScanThread.join(mScanIntervalMs*3);
                //mOutputThread.interrupt();
            } catch (InterruptedException e) {
               LogManager.e(e);
            }
            mScanThread = null;
        }
    }

    public void getSourceList(List<Source> sources) {
        sources.clear();
        synchronized (mSources) {
            for(Source source: mSources)
                sources.add(new Source(source.Type, source.Name, source.Url));
        }
    }

    private void scanLoop() {
        LogManager.i("Start to VideoSourceUtils scan loop");
        while (mStarted) {
            try { Thread.sleep(mScanIntervalMs); } catch (InterruptedException e) {LogManager.e(e); break;}
            synchronized (mSources) {
                mSources.clear();
                LogManager.w("!!!!!!TODO: Magical code in VideoSourceUtils.scanLoop");
                mSources.add(new Source(SourceType.IP_CAMERA, "网络摄像机-200", "rtsp://10.1.83.200:5000/main.h264"));
                mSources.add(new Source(SourceType.IP_CAMERA, "网络摄像机-201", "rtsp://10.1.83.201:5000/main.h264"));
                mSources.add(new Source(SourceType.HDMI_IN, "HDMI-IN", "capture://none?type=video&id=0&name=hdmi_in&description=HDMI-IN"));
                if(mCallback != null)
                    mCallback.onSourceList(mSources);
            }
        }
        mStarted = false;
        LogManager.i("Exit to VideoSourceUtils scan loop");
    }
}
