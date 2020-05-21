package com.sanbu.tools;

import android.util.Base64;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class AESUtil {

    public enum Pattern {
        ECB,
        CBC,
        CFB,
        OFB
    }

    public static Key convertKey(String aesKey) throws UnsupportedEncodingException {
        return new SecretKeySpec(aesKey.getBytes("utf-8"), "AES");
    }

    public static byte[] decrypt(Pattern pattern, Key aesKey, byte[] ciphertext)
            throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("AES/" + pattern.name() + "/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, aesKey);
        return cipher.doFinal(ciphertext);
    }

    public static String decrypt(String aesKey, String ciphertext, String inCodec/*base64/utf-8*/,
                                 String outCharsetName) throws Exception {
        return decrypt(Pattern.ECB, aesKey, ciphertext, inCodec, outCharsetName);
    }

    public static String decrypt(Pattern pattern, String aesKey, String ciphertext, String inCodec/*base64/utf-8*/,
                                 String outCharsetName) throws Exception {
        byte[] in;
        if (inCodec.toLowerCase().equals("base64"))
            in = Base64.decode(ciphertext, Base64.DEFAULT);
        else
            in = ciphertext.getBytes(inCodec);

        byte[] out = decrypt(pattern, convertKey(aesKey), in);
        return new String(out, outCharsetName);
    }

    // import org.myapache.commons.codec.binary.Base64;
    //
    // public static String decrypt(Pattern pattern, String aesKey, String ciphertext, String inCodec/*base64/utf-8*/,
    //                              String outCharsetName) throws Exception {
    //     byte[] in;
    //     if (inCodec.toLowerCase().equals("base64"))
    //         in = Base64.decodeBase64(ciphertext);
    //     else
    //         in = ciphertext.getBytes(inCodec);
    //
    //     byte[] out = decrypt(pattern, convertKey(aesKey), in);
    //     return new String(out, outCharsetName);
    // }
}
