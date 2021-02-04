package com.sanbu.network;

import com.sanbu.tools.CompareHelper;
import com.sanbu.tools.LogUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CallingUrl {
    public CallingProtocol protocol;
    public String username;
    public String address;
    public int port;
    public String confId;

    public CallingUrl() {
        protocol = CallingProtocol.Unknown;
        port = -1;
    }

    public CallingUrl(CallingProtocol protocol, String username,
                      String address, int port, String confId) {
        this.protocol = protocol;
        this.username = username;
        this.address = address;
        this.port = port;
        this.confId = confId;
    }

    public String toString() {
        String url = "";

        if (protocol != CallingProtocol.Unknown)
            url += protocol.prefix;

        if (username != null) {
            url += username;
            if (address != null)
                url += "@";
        }

        if (address != null)
            url += address;

        if (port > 0)
            url += ":" + port;

        if (confId != null)
            url += "##" + confId;

        return url;
    }

    public boolean isEqual(CallingUrl other) {
        return (CompareHelper.isEqual(protocol, other.protocol) &&
                CompareHelper.isEqual(username, other.username) &&
                CompareHelper.isEqual(address, other.address) &&
                CompareHelper.isEqual(confId, other.confId) &&
                port == other.port);
    }

    public static CallingUrl parse(String callingUrl) {
        try {
            CallingUrl result = new CallingUrl();
            String url = callingUrl;

            // 1. strip "sip|h323|sbs:" -> protocol
            result.protocol = CallingProtocol.fromUrl(url);
            if (result.protocol != CallingProtocol.Unknown)
                url = url.split(":", 2)[1];

            // 2. strip "@" -> username
            String[] tokens = url.split("@", 2);
            if (tokens.length == 2) {
                result.username = tokens[0];
                url = tokens[1];
            }

            /* 3. catch following cases with regex -> address/port/confId
                <address>
                <address>:<port>
                <address>##<confId>
                <address>:<port>##<confId>
            * */
            String regex = "^((?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)|(?:(?:[a-zA-Z0-9-]+\\.){0,5}[a-zA-Z0-9-][a-zA-Z0-9-]+\\.[a-zA-Z]{2,63}?))(?:\\:(\\d+))?(?:##(\\d+))?$";
            Matcher m = Pattern.compile(regex).matcher(url);
            if (m.find()) {
                result.address = m.group(1);
                if (m.group(2) != null)
                    result.port = Integer.valueOf(m.group(2));
                result.confId = m.group(3);
                return result;
            }

            /* 4. catch following cases with regex
                <username>
                <username>:<confId>
            * */
            regex = "^(\\+?[\\w]+)(?:\\:(\\d+))?$";
            m = Pattern.compile(regex).matcher(url);
            if (m.find()) {
                if (result.username != null)
                    throw new RuntimeException("logical error");
                result.username = m.group(1);
                result.confId = m.group(2);
                return result;
            }

            return null;
        } catch (Exception e) {
            LogUtil.e("CallingUrl parse(url=" + callingUrl + ") error:", e);
            return null;
        }
    }
}
