package cn.lx.mbs.ui.model;

import com.google.gson.Gson;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class VideoSourcesDataModel {

    public static class VideoSourceConfig {
        public static final int TYPE_NONE = 0;
        public static final int TYPE_LOCAL_CAMERA = 1;
        public static final int TYPE_REMOTE_CAMERA = 5;
        public static final int TYPE_RTSP = 2;
        public static final int TYPE_RTMP = 3;
        public static final int TYPE_FILE = 4;

        public class LocalCameraConfig {
            public String cameraId;
            public int captureWidth;
            public int captureHeight;

            // TODO: AE AF Stability...
        }

        public class RemoteCameraConfig {
            // TODO:
            public String host;
            public int port;
        }

        public class RtspConfig {
            public String url;
            public boolean useTcp;
            public String extraOptions;
        }

        public class RtmpConfig {
            public String url;
        }

        public class FileConfig {
            public String path;
            public boolean loop;
        }

        public String alias = "Untitled";
        public int type = TYPE_NONE;

        public LocalCameraConfig localCameraConfig = new LocalCameraConfig();
        public RemoteCameraConfig remoteCameraConfig = new RemoteCameraConfig();
        public RtspConfig rtspConfig = new RtspConfig();
        public RtmpConfig rtmpConfig = new RtmpConfig();
        public FileConfig fileConfig = new FileConfig();

    } // End of VideoSourceConfig

    private List<VideoSourceConfig> list = new LinkedList<>();

    public VideoSourcesDataModel() { }

    public VideoSourceConfig getItem(int index) {
        try {
            return list.get(index);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public int size() {
        return list.size();
    }

    public void add(VideoSourceConfig item) {
        list.add(item);
    }

    public void remove(int index) {
        list.remove(index);
    }

    public void clear() {
        list.clear();
    }

    public boolean fromJson(String json) {
        Gson gson = new Gson();

        VideoSourceConfig[] data = gson.fromJson(json, VideoSourceConfig[].class);
        if (data != null) {
            list.clear();
            list = Arrays.asList(data);
            return true;
        }

        return false;
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(toArray());
    }

    public VideoSourceConfig[] toArray() {
        return list.toArray(new VideoSourceConfig[list.size()]);
    }

}
