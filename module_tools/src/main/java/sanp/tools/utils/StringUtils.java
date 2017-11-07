/**
 *
 */
package sanp.tools.utils;

import android.text.TextUtils;

/**
 * @author wujp
 */
public class StringUtils {

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
        {
            if (TextUtils.isEmpty(hexString))
                throw new IllegalArgumentException("this hexString must not be empty");
            hexString = hexString.toLowerCase();
            final byte[] byteArray = new byte[hexString.length() / 2];
            int k = 0;
            for (int i = 0; i < byteArray.length; i++) {//因为是16进制，最多只会占用4位，转换成字节需要两个16进制的字符，高位在先
                byte high = (byte) (Character.digit(hexString.charAt(k), 16) & 0xff);
                byte low = (byte) (Character.digit(hexString.charAt(k + 1), 16) & 0xff);
                byteArray[i] = (byte) (high << 4 | low);
                k += 2;
            }
            return byteArray;
        }
    }


}