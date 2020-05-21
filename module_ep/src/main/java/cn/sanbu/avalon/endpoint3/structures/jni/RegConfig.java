package cn.sanbu.avalon.endpoint3.structures.jni;

import com.sanbu.tools.CompareHelper;
import com.sanbu.tools.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class RegConfig {
    public static class H323 {
        public String address;          // GK地址
        public int port;                // GK端口
        public String username;         // 分机号
        public String verifname;        // 认证名称
        public String password;         // 认证密码
        public List<String> aliases;    // 别名

        public H323(String address, int port, String ext_number,
                    String username, String password, List<String> aliases) {
            this.address = address;
            this.port = port;
            this.username = ext_number;
            this.verifname = username;
            this.password = password;
            this.aliases = aliases;
        }

        public H323(H323 other) {
            this(other.address, other.port, other.username,
                    other.verifname, other.password, new ArrayList<>(other.aliases));
        }

        public String getExtNumber() {
            return username;
        }

        public String getAuthName() {
            return verifname;
        }

        public boolean isValid() {
            return (!StringUtil.isEmpty(address) && port > 0 && username != null);
        }

        public boolean isEqual(H323 other) {
            return (CompareHelper.isEqual(address, other.address) && port == other.port &&
                    CompareHelper.isEqual(username, other.username) &&
                    CompareHelper.isEqual(verifname, other.verifname) &&
                    CompareHelper.isEqual(password, other.password) &&
                    CompareHelper.isEqual(aliases, other.aliases, (src, dst) -> {
                        List<String> listSrc = (List<String>) src;
                        List<String> listDst = (List<String>) dst;
                        if (listSrc.size() != listDst.size())
                            return false;
                        for (int i = 0 ; i < listSrc.size() ; ++i) {
                            if (!CompareHelper.isEqual(listSrc.get(i), listDst.get(i)))
                                return false;
                        }
                        return true;
                    }));
        }
    }

    public static class SIP {
        public String address;      // SIP信令服务器地址
        public int port;            // 服务器端口
        public String domain;       // SIP域
        public String username;     // 用户名
        public String password;     // 密码
        public List<String> aliases;// 别名

        public SIP(String address, int port, String domain,
                   String username, String password, List<String> aliases) {
            this.address = address;
            this.port = port;
            this.domain = domain;
            this.username = username;
            this.password = password;
            this.aliases = aliases;
        }

        public SIP(SIP other) {
            this(other.address, other.port, other.domain,
                    other.username, other.password, new ArrayList<>(other.aliases));
        }

        public boolean isValid() {
            return (!StringUtil.isEmpty(address) && port > 0 && username != null);
        }

        public boolean isEqual(SIP other) {
            return (CompareHelper.isEqual(address, other.address) && port == other.port &&
                    CompareHelper.isEqual(domain, other.domain) &&
                    CompareHelper.isEqual(username, other.username) &&
                    CompareHelper.isEqual(password, other.password) &&
                    CompareHelper.isEqual(aliases, other.aliases, (src, dst) -> {
                        List<String> listSrc = (List<String>) src;
                        List<String> listDst = (List<String>) dst;
                        if (listSrc.size() != listDst.size())
                            return false;
                        for (int i = 0 ; i < listSrc.size() ; ++i) {
                            if (!CompareHelper.isEqual(listSrc.get(i), listDst.get(i)))
                                return false;
                        }
                        return true;
                    }));
        }
    }
}
