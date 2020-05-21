package com.sanbu.tools;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Util {
	
    private static final char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6','7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
    private static final int MAX_SOURCE_SIZE = 50*1024*1024;

    public static String getFileMD5(File file) {
    	if (!file.exists()) {
            LogUtil.e("File is not exists: " + file.getName());
            return null;
        }

    	long length = file.length();
		if (length > MAX_SOURCE_SIZE) {
            LogUtil.e("File is too big, " + (length / 1024 / 1024) + "M > " + (MAX_SOURCE_SIZE / 1024 / 1024) + "M");
            return null;
        }

        MessageDigest messagedigest;
        try {
            messagedigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            LogUtil.e("MD5Util MessageDigest get MD5 failed", e);
            return null;
        }

        String md5 = null;
        FileInputStream in = null;

        try {
            in = new FileInputStream(file);
			FileChannel ch = in.getChannel();
			MappedByteBuffer byteBuffer = ch.map(FileChannel.MapMode.READ_ONLY, 0, length);
			messagedigest.update(byteBuffer);  
			md5 = bufferToHex(messagedigest.digest());
		} catch (Throwable throwable) {
            LogUtil.e("getFileMD5 failed", throwable);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return md5;
    }

    public static String getResourceMD5(Context context, int resourceId) {
        String md5 = null;
        InputStream in = null;

        try {
            in = context.getResources().openRawResource(resourceId);
            md5 = getStreamMD5(in);
        } catch (Throwable throwable) {
            LogUtil.e("getResourceMD5 failed", throwable);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return md5;
    }

    public static String getAssetMD5(Context context, String assetFile) {
        String md5 = null;
        InputStream in = null;

        try {
            in = context.getAssets().open(assetFile);
            md5 = getStreamMD5(in);
        } catch (Throwable throwable) {
            LogUtil.e("getAssetMD5 failed", throwable);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return md5;
    }

    private static String getStreamMD5(InputStream in) throws IOException {
        MessageDigest messagedigest;
        try {
            messagedigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            LogUtil.e("MD5Util MessageDigest get MD5 failed", e);
            return null;
        }

        int length = in.available();
        if (length > MAX_SOURCE_SIZE) {
            LogUtil.e("Resource is too big, " + (length / 1024 / 1024) + "M > " + (MAX_SOURCE_SIZE / 1024 / 1024) + "M");
            return null;
        }

        byte []buffer = new byte[length];
        in.read(buffer);

        messagedigest.update(buffer);
        return bufferToHex(messagedigest.digest());
    }

    private static String bufferToHex(byte bytes[]) {
        return bufferToHex(bytes, 0, bytes.length);  
    }  
  
    private static String bufferToHex(byte bytes[], int offset, int length) {
        StringBuffer stringbuffer = new StringBuffer(2 * length);
        int k = offset + length;
        for (int l = offset; l < k; l++) {
            appendHexPair(bytes[l], stringbuffer);  
        }  
        return stringbuffer.toString();  
    }  
  
    private static void appendHexPair(byte bt, StringBuffer stringbuffer) {
        char c0 = hexDigits[(bt & 0xf0) >> 4];  
        char c1 = hexDigits[bt & 0xf];  
        stringbuffer.append(c0);  
        stringbuffer.append(c1);  
    }
}