/*
 *  文件名称 : RSAUtils.java <br>
 *  项目名称 : middleware.cas<br>
 *  建立人员 : WuJianPing<br>
 *  建立时间 : 2016年6月20日 上午11:10:52
 *  Copyright (c) 3bu.cn
 */
package com.sanbu.tools;

import android.util.Base64;

import java.io.IOException;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

/**
 * RSA工具类
 *
 * @author huangzy
 */
public class RSAUtil {

    public static final String KEY_PUBLIC = "public";

    public static final String KEY_PRIVATE = "private";

    /**
     * 生成公钥和私钥
     *
     * @throws NoSuchAlgorithmException
     */
    public static HashMap<String, Object> getKeys() throws NoSuchAlgorithmException {
        HashMap<String, Object> map = new HashMap<String, Object>();
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(1024);
        KeyPair keyPair = keyPairGen.generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        map.put(KEY_PUBLIC, publicKey);
        map.put(KEY_PRIVATE, privateKey);
        return map;
    }

    //将byte数组变成RSAPublicKey
    public static RSAPublicKey bytes2PublicKey(byte[] buf) {
        buf = decodeBase64(buf);
        byte size = buf[0];
        byte size2 = buf[1];
        byte[] b1 = new byte[size];
        System.arraycopy(buf, 2, b1, 0, b1.length);
        byte[] b2 = new byte[size2];
        System.arraycopy(buf, b1.length + 2, b2, 0, b2.length);
        BigInteger B1 = new BigInteger(b1);
        BigInteger B2 = new BigInteger(b2);
        RSAPublicKeySpec spec = new RSAPublicKeySpec(B1, B2);//存储的就是这两个大整形数
        KeyFactory keyFactory;
        PublicKey pk = null;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
            pk = keyFactory.generatePublic(spec);
        } catch (Exception e) {
           LogUtil.e(e);
        }
        return (RSAPublicKey) pk;
    }

    public static PrivateKey getPrivateKey(final String priKey) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        PrivateKey privateKey = null;
        PKCS8EncodedKeySpec priPKCS8;

        priPKCS8 = new PKCS8EncodedKeySpec(
                decodeBase64(priKey)
        );
        KeyFactory keyf = KeyFactory.getInstance("RSA");
        privateKey = keyf.generatePrivate(priPKCS8);

        return privateKey;
    }

    public static PublicKey getPubKey(final String pubKey) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        PublicKey publicKey = null;

        //自己的公钥(测试)
        java.security.spec.X509EncodedKeySpec bobPubKeySpec = new java.security.spec.X509EncodedKeySpec(
                decodeBase64(pubKey)
        );
        // RSA对称加密算法
        KeyFactory keyFactory;
        keyFactory = KeyFactory.getInstance("RSA");
        // 取公钥匙对象
        publicKey = keyFactory.generatePublic(bobPubKeySpec);

        return publicKey;
    }


    /**
     * 使用模和指数生成RSA公钥
     * 注意：【此代码用了默认补位方式，为RSA/None/PKCS1Padding，不同JDK默认的补位方式可能不同，如Android默认是RSA/None/NoPadding】
     *
     * @param modulus  模
     * @param exponent 指数
     * @return
     */
    public static RSAPublicKey getPublicKey(String modulus, String exponent) {
        try {
            BigInteger b1 = new BigInteger(modulus);
            BigInteger b2 = new BigInteger(exponent);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPublicKeySpec keySpec = new RSAPublicKeySpec(b1, b2);
            return (RSAPublicKey) keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
           LogUtil.e(e);
            return null;
        }
    }

    /**
     * 使用模和指数生成RSA私钥
     * 注意：【此代码用了默认补位方式，为RSA/None/PKCS1Padding，不同JDK默认的补位方式可能不同，如Android默认是RSA/None/NoPadding】
     *
     * @param modulus  模
     * @param exponent 指数
     * @return
     */
    public static RSAPrivateKey getPrivateKey(String modulus, String exponent) {
        try {
            BigInteger b1 = new BigInteger(modulus);
            BigInteger b2 = new BigInteger(exponent);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(b1, b2);
            return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
           LogUtil.e(e);
            return null;
        }
    }


    /**
     * 密钥加密
     * @param data
     * @param key
     * @return
     * @throws Exception
     */
    public static String encryptByKey(String data, Key key)
            throws Exception {
        Cipher cipher = getInstanceCipher();
        cipher.init(Cipher.ENCRYPT_MODE, key);
        // 模长
        int key_len = ((java.security.interfaces.RSAKey) key).getModulus().bitLength() / 8;
        // 加密数据长度 <= 模长-11
////使用byte 解码会出现很多0字符
//		byte []bytes = data.getBytes();
//		byte[][] array = splitArray(bytes, key_len - 11);
//		List<byte[]> list =new ArrayList<byte[]>();
//		int len=0;
//		for(byte [] buf : array){
//			byte[] bs = cipher.doFinal(buf);
//			list.add(bs);
//			len+=bs.length;
//		}
////使用byte
//使用string
        String[] parts = splitString(data, key_len - 11);
        List<byte[]> list =new ArrayList<byte[]>();
        int len=0;
        for(String s : parts){
            byte[] bs = cipher.doFinal(s.getBytes("utf-8"));
            list.add(bs);
            len+=bs.length;
        }
//使用string

        byte []all = new byte[len];
        int index = 0;
        for(byte [] buf : list){
            System.arraycopy(buf, 0, all, index, buf.length);
            index += buf.length;
        }
        byte[] base64 = encodeBase64(all);
        return new String(base64);
    }

    /**
     * 密钥加密
     * @param bytes
     * @param key
     * @return
     * @throws Exception
     */
    public static String encryptByKey(byte[] bytes, Key key)
            throws Exception {
        Cipher cipher = getInstanceCipher();
        cipher.init(Cipher.ENCRYPT_MODE, key);
        // 模长
        int key_len = ((java.security.interfaces.RSAKey) key).getModulus().bitLength() / 8;
        // 加密数据长度 <= 模长-11
        byte[][] datas = splitArray(bytes, key_len - 11);
        String mi = "";
        //如果明文长度大于模长-11则要分组加密
        for (byte[] bt : datas) {
            mi += new String(cipher.doFinal(bt));
        }
        return mi;
    }
    /**
     * 私钥解密
     *
     * @param data
     * @param privateKey
     * @return
     * @throws Exception
     */
    public static String decryptByPrivateKey(String data, RSAPrivateKey privateKey)
            throws Exception {

        byte[] decode = decodeBase64(data);
        final byte[] out = decryptByPrivateKey(decode, privateKey);
        return new String(out, "utf-8");
    }

    /**
     * 私钥解密
     *
     * @param bytes
     * @param privateKey
     * @return
     * @throws Exception
     */
    public static byte[] decryptByPrivateKey(Cipher cipher,byte[] bytes, RSAPrivateKey privateKey)
            throws Exception {

        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        //模长
        int key_len = privateKey.getModulus().bitLength() / 8;
        byte[] bcd = bytes;// ASCII_To_BCD(bytes, bytes.length);
        //System.err.println(bcd.length);
        //如果密文长度大于模长则要分组解密
        byte[][] arrays = splitArray(bcd, key_len);
        byte[][] rltArrs = new byte[arrays.length][];
        int index = 0;
        int length = 0;
        for (byte[] arr : arrays) {
            rltArrs[index] = cipher.doFinal(arr);
            length += rltArrs[index].length;
            index++;
        }
        byte[] rlt = new byte[length];
        int pos = 0;
        for (int i = 0; i < rltArrs.length; i++) {
            int arrLen = rltArrs[i].length;
            System.arraycopy(rltArrs[i], 0, rlt, pos, arrLen);
            pos += arrLen;
        }

        return rlt;
    }

    /**
     * 私钥解密
     *
     * @param bytes
     * @param privateKey
     * @return
     * @throws Exception
     */
    public static byte[] decryptByPrivateKey(byte[] bytes, RSAPrivateKey privateKey)
            throws Exception {
        Cipher cipher = getInstanceCipher();
        return decryptByPrivateKey(cipher,bytes,privateKey);
    }

    /**
     * 公钥解密
     *
     * @param data
     * @param publicKey
     * @return
     * @throws Exception
     */
    public static String decryptByPublicKey(String data, RSAPublicKey publicKey)
            throws Exception {

        byte[] decode = decodeBase64(data);
        final byte[] out = decryptByPublicKey(decode, publicKey);
        return new String(out);
    }

    /**
     * 公钥解密
     *
     * @param bytes
     * @param publicKey
     * @return
     * @throws Exception
     */
    public static byte[] decryptByPublicKey(byte[] bytes, RSAPublicKey publicKey)
            throws Exception {
        Cipher cipher = getInstanceCipher();
        cipher.init(Cipher.DECRYPT_MODE, publicKey);
        //模长
        int key_len = publicKey.getModulus().bitLength() / 8;
        byte[] bcd = bytes;// ASCII_To_BCD(bytes, bytes.length);
        //System.err.println(bcd.length);
        //如果密文长度大于模长则要分组解密
        byte[][] arrays = splitArray(bcd, key_len);
        byte[][] rltArrs = new byte[arrays.length][];
        int index = 0;
        int length = 0;
        for (byte[] arr : arrays) {
            rltArrs[index] = cipher.doFinal(arr);
            length += rltArrs[index].length;
            index++;
        }
        byte[] rlt = new byte[length];
        int pos = 0;
        for (int i = 0; i < rltArrs.length; i++) {
            int arrLen = rltArrs[i].length;
            System.arraycopy(rltArrs[i], 0, rlt, pos, arrLen);
            pos += arrLen;
        }

        return rlt;
    }

    /**
     * 拆分字符串
     */
    public static String[] splitString(String string, int len) {
        int x = string.length() / len;
        int y = string.length() % len;
        int z = 0;
        if (y != 0) {
            z = 1;
        }
        String[] strings = new String[x + z];
        String str = "";
        for (int i = 0; i < x + z; i++) {
            if (i == x + z - 1 && y != 0) {
                str = string.substring(i * len, i * len + y);
            } else {
                str = string.substring(i * len, i * len + len);
            }
            strings[i] = str;
        }
        return strings;
    }

    /**
     * BCD转字符串
     */
    public static String bcd2Str(byte[] bytes) {
        char temp[] = new char[bytes.length * 2];
        char val;
        for (int i = 0; i < bytes.length; i++) {
            val = (char) (((bytes[i] & 0xf0) >> 4) & 0x0f);
            temp[i * 2] = (char) (val > 9 ? val + 'A' - 10 : val + '0');

            val = (char) (bytes[i] & 0x0f);
            temp[i * 2 + 1] = (char) (val > 9 ? val + 'A' - 10 : val + '0');
        }
        return new String(temp);
    }

    /**
     * BCD转字符串
     */
    public static byte[] bcd2Bytes(byte[] bytes) {
        byte temp[] = new byte[bytes.length * 2];
        byte val;
        for (int i = 0; i < bytes.length; i++) {
            val = (byte) (((bytes[i] & 0xf0) >> 4) & 0x0f);
            temp[i * 2] = (byte) (val > 9 ? val + 'A' - 10 : val + '0');

            val = (byte) (bytes[i] & 0x0f);
            temp[i * 2 + 1] = (byte) (val > 9 ? val + 'A' - 10 : val + '0');
        }
        return temp;
    }


    /**
     * ASCII码转BCD码
     */
    public static byte[] ASCII_To_BCD(byte[] ascii, int asc_len) {
        byte[] bcd = new byte[asc_len / 2];
        int j = 0;
        for (int i = 0; i < (asc_len + 1) / 2; i++) {
            bcd[i] = asc_to_bcd(ascii[j++]);
            bcd[i] = (byte) (((j >= asc_len) ? 0x00 : asc_to_bcd(ascii[j++])) + (bcd[i] << 4));
        }
        return bcd;
    }

    public static byte asc_to_bcd(byte asc) {
        byte bcd;

        if ((asc >= '0') && (asc <= '9'))
            bcd = (byte) (asc - '0');
        else if ((asc >= 'A') && (asc <= 'F'))
            bcd = (byte) (asc - 'A' + 10);
        else if ((asc >= 'a') && (asc <= 'f'))
            bcd = (byte) (asc - 'a' + 10);
        else
            bcd = (byte) (asc - 48);
        return bcd;
    }

    /**
     * 拆分数组
     */
    public static byte[][] splitArray(byte[] data, int len) {
        int x = data.length / len;
        int y = data.length % len;
        int z = 0;
        if (y != 0) {
            z = 1;
        }
        byte[][] arrays = new byte[x + z][];
        byte[] arr;
        for (int i = 0; i < x + z; i++) {
            arr = new byte[len];
            if (i == x + z - 1 && y != 0) {
                System.arraycopy(data, i * len, arr, 0, y);
            } else {
                System.arraycopy(data, i * len, arr, 0, len);
            }
            arrays[i] = arr;
        }
        return arrays;
    }

//	private static String toHexString(byte[] b) {
//		StringBuilder sb = new StringBuilder(b.length * 2);
//		for (int i = 0; i < b.length; i++) {
//			sb.append(HEXCHAR[(b[i] & 0xf0) >>> 4]);
//			sb.append(HEXCHAR[b[i] & 0x0f]);
//		}
//		return sb.toString();
//	}

    public static String getKeyString(Key key) throws Exception {
        byte[] keyBytes = key.getEncoded();
        String s = encodeBase64String(keyBytes);
        return s;
    }

    public static Cipher getInstanceCipher() throws NoSuchAlgorithmException, NoSuchPaddingException {
        synchronized (Cipher.class) {
            return Cipher.getInstance("RSA/ECB/PKCS1Padding");
        }
    }

    private static byte[] decodeBase64(String buf) {
        return Base64.decode(buf, Base64.DEFAULT);
    }

    private static byte[] decodeBase64(byte[] buf) {
        return Base64.decode(buf, Base64.DEFAULT);
    }

    private static byte[] encodeBase64(byte[] buf) {
        return Base64.encode(buf, Base64.DEFAULT);
    }

    private static String encodeBase64String(byte[] buf) {
        return Base64.encodeToString(buf, Base64.DEFAULT);
    }

    // import org.myapache.commons.codec.binary.Base64;
    // private static byte[] decodeBase64(String buf) {
    //     return Base64.decodeBase64(buf);
    // }
    // private static byte[] encodeBase64(byte[] buf) {
    //     return Base64.encodeBase64(buf, Base64.DEFAULT);
    // }
    // private static String encodeBase64String(byte[] buf) {
    //     return Base64.encodeBase64String(buf, Base64.DEFAULT);
    // }

    private static final String LAUNCH_KEY =
            "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCvIz3nDzhZ1XlJ\n" +
                    "HtCjYxKwUUFDDNcwYrHE9BUV9Te/bD39GZXardVKADvpXhRiY3FFq3MvMrXGcmdf\n" +
                    "JdIP4qN6dam9f7txCm54vuho/12h11ZaCimtU+E9ZWmu2HZ0eScu3nGZtf4d4Ggd\n" +
                    "bkV0s0pk2dwYwt2w0cJ6lL+HnN/6fwi4YH/ALDlM7/mMoQHlwZXlF0fXZFK7woq9\n" +
                    "1XA7CIKZk9YVhbgTQzHVbXiO8qHsdHldZTg8fVeN/p4JxQwqL+uwpoeDUsgEVH/w\n" +
                    "Y/uNQvuBh91RI920oX/YUzXLrmKtHjBEFOtbbh4Pv5hPrfrkSWJbEsnMwd3wEIlP\n" +
                    "fGEXyDt3AgMBAAECggEAY+EGSYJ9kYXT0GZ5d+RlYRZF0LY+9oIifX7xk38wVsOl\n" +
                    "KnVv57FhRP2TLUiQ+Xdavu/DFbSmw9C0sSeBf7uxnYIC1mZFAHeBfuzo4BBaVpTW\n" +
                    "0yyejhGbD4eJWMRr8YqyVOFZCd8nV+SFm9sqUx54m8E+6wQep9tIPN4dyHonpdPh\n" +
                    "CyQhxaas381mZ5FC7abhRsg/yphhxVdugJZH8rLHnLJ3qjh6TEChCvClLG3CJHKp\n" +
                    "HsuToV9h4QqCv6pXvdjaydPhqwFoMhRn539Xp0HEN/K8g+TTrvwwngwDHkDrB80H\n" +
                    "tgmCd69oSyUtqWUxleqafJrEu33VXlwGS7CQ3Jc2KQKBgQDZsAS/QfPynzVlb1RF\n" +
                    "rwPngdOzOf9dOTz/FPLACTcym1ngRYtZLZ+wElzk6RHcesuLsTFq8aXpDlBgDuvb\n" +
                    "kuvQqNt4u9SPoaPpiWuGCh/b+p/w4xaqoxUboGuKmc5399XqzBSgObt1OOTCpvAu\n" +
                    "Izr9CNZSIRFr5pzQB2sB5pZmjQKBgQDN9h+z9b9S9N2nR5H+cvaKFLQX4vMosPI4\n" +
                    "g3YZeLY4d/gGQpw4hWkc5jKt+9i2XCdfg9qh2mntTrX9hl8+0qWonJuPVpley04O\n" +
                    "veHTxx2XpEikY6+Y3fj48l8TLNxP4k2712D/gwe1u6w7YIq1pKZq6nUsEirlF+3f\n" +
                    "i3YFfXTbEwKBgAFxCvcBRs5Kg54CWLqFaC82SuKa9bf2UxMVXm4rIXRyVuwHSd7z\n" +
                    "UGVoGbliWb3uCj8Ik10z8HdUou+f6avwkyM0mw93nva8iUtYn5+pnYBlbn2340SK\n" +
                    "A+/E5jFqx1VADOibJV/SQg2KrOklFd4YWaGnV1P/6A+g3VT7V5gRoNlZAoGAB8wG\n" +
                    "fTiTap+MeJW0CC7v+GA6RSE5VVQFh3Aqm2I/e7nG8O26nkUmqopoZr++/4BTmUET\n" +
                    "mMb89ZfiOdvJZUZyMQTkurNrkPQfW12C3BKQozn76gVAE4hrsXjzcnVjTb7idHb9\n" +
                    "3A5oz09wgLrx5Vh7WFbR4r45U3zpa2oLKMAMGXMCgYB//ywnPq+W6hCTpjb+ldN7\n" +
                    "b39MyaGAj2I7DgYytSveEXToJ2EcWkswOCpIXNKYaUUu0QUG4TgfM1H1TNW4hGl3\n" +
                    "dmGeDzI/LElNdmRoK0ABpM0PNq9H9BokqehzO4Epf7GOjarH4uXpeje5laA4zDRl\n" +
                    "5n9X0wf8D7L7zjhmgclk7w==\n";

    public static String decryptByString(String pubstr) throws Exception{
        String decryptStr = null;

        RSAPrivateKey privateKey = (RSAPrivateKey) RSAUtil.getPrivateKey(LAUNCH_KEY);
        //        RSAPrivateKey privateKey = (RSAPrivateKey) RSAUtil.getPrivateKey(context.getResources().openRawResource(R.raw.key).toString());
        byte[] decode = android.util.Base64.decode(pubstr, android.util.Base64.URL_SAFE | android.util.Base64.NO_PADDING);
        byte[] bs = RSAUtil.decryptByPrivateKey(decode, privateKey);
        decryptStr =  new String(GzipUtil.ungzip(bs));
        return decryptStr;
    }

}
