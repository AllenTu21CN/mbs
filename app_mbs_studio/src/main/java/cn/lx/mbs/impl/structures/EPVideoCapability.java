package cn.lx.mbs.impl.structures;

import com.sanbu.tools.CompareHelper;

import java.util.ArrayList;
import java.util.List;

import cn.sanbu.avalon.endpoint3.structures.Bandwidth;
import cn.sanbu.avalon.endpoint3.structures.jni.CodecType;
import cn.sanbu.avalon.endpoint3.structures.H264Profile;
import cn.sanbu.avalon.endpoint3.structures.Resolution;

// 视频编码能力
public class EPVideoCapability {
    public Bandwidth maxBandwidth;     // 最大带宽
    public Resolution maxResolution;   // 最大分辨率
    public int maxFramerate;           // 最大帧率
    public H264Profile profile;        // 编码Profile
    public int iFrameIntervalSec;      // I帧间隔(单位:秒)
    public List<CodecType> codecs;     // 编码候选

    public EPVideoCapability(Bandwidth maxBandwidth, Resolution maxResolution, int maxFramerate,
                             H264Profile profile, int iFrameIntervalSec, List<CodecType> codecs) {
        this.maxBandwidth = maxBandwidth;
        this.maxResolution = maxResolution;
        this.maxFramerate = maxFramerate;
        this.profile = profile;
        this.iFrameIntervalSec = iFrameIntervalSec;
        this.codecs = codecs;
    }

    public EPVideoCapability(EPVideoCapability other) {
        this(other.maxBandwidth, other.maxResolution, other.maxFramerate,
                other.profile, other.iFrameIntervalSec, new ArrayList<>(other.codecs));
    }

    public boolean isEqual(EPVideoCapability other) {
        return (other != null &&
                CompareHelper.isEqual(maxBandwidth, other.maxBandwidth) &&
                CompareHelper.isEqual(maxResolution, other.maxResolution) &&
                maxFramerate == other.maxFramerate &&
                CompareHelper.isEqual(profile, other.profile) &&
                iFrameIntervalSec == other.iFrameIntervalSec &&
                CompareHelper.isEqual(codecs, other.codecs, (src, dst) -> {
                    if (codecs.size() != other.codecs.size())
                        return false;
                    for (int i = 0 ; i < codecs.size() ; ++i) {
                        if (codecs.get(i) != other.codecs.get(i))
                            return false;
                    }
                    return true;
                })
        );
    }

    public boolean isValid() {
        return (maxBandwidth != null &&
                maxResolution != null && maxResolution != Resolution.RES_UNKNOWN &&
                maxFramerate >= 10 &&
                profile != null && profile != H264Profile.Unspecified &&
                iFrameIntervalSec > 0 &&
                codecs != null && codecs.size() > 0
        );
    }
}
