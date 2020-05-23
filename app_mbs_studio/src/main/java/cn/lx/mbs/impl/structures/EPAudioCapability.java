package cn.lx.mbs.impl.structures;

import com.sanbu.tools.CompareHelper;

import java.util.LinkedList;
import java.util.List;

// 音频编码能力
public class EPAudioCapability {
    public List<AudioCodec> codecs;

    public EPAudioCapability(List<AudioCodec> codecs) {
        this.codecs = codecs;
    }

    public EPAudioCapability(EPAudioCapability other) {
        this(new LinkedList<>(other.codecs));
    }

    public boolean isEqual(EPAudioCapability other) {
        return (other != null &&
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
}
