package sanp.avalon.libs.base.db;

/**
 * Created by Tom on 2017/5/17.
 */

public class VideoInfo {
    String videoUrl;
    String videoTitle;
    String videoContent;
    String videoRole;

    public String getVideoRole() {
        return videoRole;
    }

    public void setVideoRole(String videoRole) {
        this.videoRole = videoRole;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getVideoTitle() {
        return videoTitle;
    }

    public void setVideoTitle(String videoTitle) {
        this.videoTitle = videoTitle;
    }

    public String getVideoContent() {
        return videoContent;
    }

    public void setVideoContent(String videoContent) {
        this.videoContent = videoContent;
    }
}
