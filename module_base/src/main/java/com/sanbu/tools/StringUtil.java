package com.sanbu.tools;

import java.util.Arrays;

public class StringUtil {

    public static boolean isEmpty(String s) {
        return s == null || s.trim().equals("");
    }

    public static int containsCharNum(String string, char matchChar) {
        int num = 0;
        char[] ch = string.toCharArray();
        for (char c : ch) {
            if (c == matchChar) {
                num++;
            }
        }
        return num;
    }

    public static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.length() == 0)
            throw new IllegalArgumentException("this hexString must not be empty");
        hexString = hexString.replace(" ", "");
        hexString = hexString.toLowerCase();
        final byte[] byteArray = new byte[hexString.length() / 2];
        int k = 0;
        for (int i = 0; i < byteArray.length; i++) {
            byte high = (byte) (Character.digit(hexString.charAt(k), 16) & 0xff);
            byte low = (byte) (Character.digit(hexString.charAt(k + 1), 16) & 0xff);
            byteArray[i] = (byte) (high << 4 | low);
            k += 2;
        }
        return byteArray;
    }

    public static String bytesToHexString(byte[] src, int offset, int max) {
        return bytesToHexString(src, offset, max, "");
    }

    public static String bytesToHexString(byte[] src, int offset, int max, String appendingSuffix) {
        if (src == null || src.length <= 0)
            return "";

        int len = max;
        if (offset + len > src.length)
            len = src.length - offset;
        if (len <= 0)
            return "";

        StringBuilder stringBuilder = new StringBuilder("");
        for (int i = offset; i < (offset+len); i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv).append(appendingSuffix);
        }
        return stringBuilder.toString();
    }

    public static String join(CharSequence delimiter, CharSequence... elements) {
        return join(delimiter, Arrays.asList(elements));
    }

    public static String join(CharSequence delimiter,
                              Iterable<? extends CharSequence> elements) {
        StringBuilder builder = new StringBuilder();
        CharSequence seg = "";
        for (CharSequence cs: elements) {
            builder.append(seg).append(cs);
            seg = delimiter;
        }
        return builder.toString();
    }

    public static boolean isNumeric(String str) {
        if(isEmpty(str)) {
            return false;
        }
        for(int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))){
                return false;
            }
        }
        return true;
    }
}