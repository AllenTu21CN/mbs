package sanp.avalon.libs.base.bean.localbean;

/**
 * Created by Vald on 2017/6/6.
 */

public class audio {
    private String puttype;            //input输入output输出
    private String origin;            //来源
    private String volume;            //音量
    private String samplingdelay;     //采样延迟

    public String getPuttype() {
        return puttype;
    }

    public void setPuttype(String puttype) {
        this.puttype = puttype;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getVolume() {
        return volume;
    }

    public void setVolume(String volume) {
        this.volume = volume;
    }

    public String getSamplingdelay() {
        return samplingdelay;
    }

    public void setSamplingdelay(String samplingdelay) {
        this.samplingdelay = samplingdelay;
    }
}
