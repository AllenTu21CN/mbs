package cn.sanbu.avalon.endpoint3.structures.jni;

import com.sanbu.tools.CompareHelper;

public class EPFixedConfig {

    public boolean sip_enable;          // 使能SIP协议
    public boolean h323_enable;         // 使能H323协议
    public boolean sbs_enable;          // 使能SBS协议
    public int sip_port;                // SIP协议端口
    public int h323_port;               // H323协议端口
    public int sbs_port;                // SBS协议端口
    public int h245_port_min;           // H245(H323子协议)协议最小端口
    public int h245_port_max;           // H245(H323子协议)协议最大端口
    public int rtp_port_min;            // RTP最大传输端口
    public int rtp_port_max;            // RTP最小传输端口
    public String nat_ip;               // NAT服务器地址
    public String sbs_over_protocol;    // SBS传输协议
    public int crash_print_num;         // C/C++(Native)层崩溃处理机制: 崩溃栈日志打印(0: 不打印, -1: 一直打印, >0: 打印次数)
    public boolean crash_exit;          // C/C++(Native)层崩溃处理机制: 崩溃后应用是否自动退出
    public String log_level;            // Native层日志可输出最低基本("FATAL", "ERROR", "WARNING", "INFO", "DEBUG", "VERBOSE")
    public String background_color;     // 绘制的背景颜色(RGB码或颜色英文名)

    public EPFixedConfig(boolean sip_enable, boolean h323_enable, boolean sbs_enable, int sip_port,
                         int h323_port, int sbs_port, int h245_port_min, int h245_port_max,
                         int rtp_port_min, int rtp_port_max, String nat_ip, String sbs_over_protocol,
                         int crash_print_num, boolean crash_exit, String log_level, String background_color) {
        this.sip_enable = sip_enable;
        this.h323_enable = h323_enable;
        this.sbs_enable = sbs_enable;
        this.sip_port = sip_port;
        this.h323_port = h323_port;
        this.sbs_port = sbs_port;
        this.h245_port_min = h245_port_min;
        this.h245_port_max = h245_port_max;
        this.rtp_port_min = rtp_port_min;
        this.rtp_port_max = rtp_port_max;
        this.nat_ip = nat_ip;
        this.sbs_over_protocol = sbs_over_protocol;
        this.crash_print_num = crash_print_num;
        this.crash_exit = crash_exit;
        this.log_level = log_level;
        this.background_color = background_color;
    }

    public EPFixedConfig(EPFixedConfig other) {
        this(other.sip_enable, other.h323_enable, other.sbs_enable,
                other.sip_port, other.h323_port, other.sbs_port,
                other.h245_port_min, other.h245_port_max,
                other.rtp_port_min, other.rtp_port_max, other.nat_ip,
                other.sbs_over_protocol, other.crash_print_num,
                other.crash_exit, other.log_level, other.background_color);
    }

    public boolean isEqual(EPFixedConfig other) {
        return ((this.sip_enable == other.sip_enable) &&
                (this.h323_enable == other.h323_enable) &&
                (this.sbs_enable == other.sbs_enable) &&
                (this.sip_port == other.sip_port) &&
                (this.h323_port == other.h323_port) &&
                (this.sbs_port == other.sbs_port) &&
                (this.h245_port_min == other.h245_port_min) &&
                (this.h245_port_max == other.h245_port_max) &&
                (this.rtp_port_min == other.rtp_port_min) &&
                (this.rtp_port_max == other.rtp_port_max) &&
                (CompareHelper.isEqual(this.nat_ip, other.nat_ip)) &&
                (CompareHelper.isEqual(this.sbs_over_protocol, other.sbs_over_protocol)) &&
                (this.crash_print_num == other.crash_print_num) &&
                (this.crash_exit == other.crash_exit) &&
                (CompareHelper.isEqual(this.log_level, other.log_level)) &&
                (CompareHelper.isEqual(this.background_color, other.background_color))
        );
    }
}
