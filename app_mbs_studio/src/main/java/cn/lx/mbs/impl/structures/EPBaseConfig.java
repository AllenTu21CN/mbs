package cn.lx.mbs.impl.structures;

import com.sanbu.tools.CompareHelper;
import cn.sanbu.avalon.endpoint3.structures.CallingProtocol;

public class EPBaseConfig {
    public CallingProtocol defaultProtocol; // 首选信令
    public boolean enabledRx;               // 允许被叫
    public boolean autoAnswer;              // 自动应答
    public boolean autoRxMute;              // 自动被叫静音
    public boolean enabledAGC;              // AGC使能

    public EPBaseConfig(CallingProtocol defaultProtocol, boolean enabledRx,
                        boolean autoAnswer, boolean autoRxMute, boolean enabledAGC) {
        this.enabledRx = enabledRx;
        this.autoAnswer = autoAnswer;
        this.autoRxMute = autoRxMute;
        this.defaultProtocol = defaultProtocol;
        this.enabledAGC = enabledAGC;
    }

    public EPBaseConfig(EPBaseConfig other) {
        this(other.defaultProtocol, other.enabledRx,
                other.autoAnswer, other.autoRxMute, other.enabledAGC);
    }

    public boolean isEqual(EPBaseConfig other) {
        return (other != null &&
                CompareHelper.isEqual(defaultProtocol, other.defaultProtocol) &&
                enabledRx == other.enabledRx &&
                autoAnswer == other.autoAnswer &&
                autoRxMute == other.autoRxMute &&
                enabledAGC == other.enabledAGC);
    }
}
