package com.sanbu.tools;

import com.google.gson.Gson;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by huangzy on 2018/8/13.
 */

public class GzipUtil {

    public static <T> T unJsonGzipSerialize(byte[] data,Class<T> classOfT) throws IOException {
        InputStreamReader reader = null;
        GZIPInputStream gzip = null;
        ByteArrayInputStream bis = null;
        try{
            bis = new ByteArrayInputStream(data);
            gzip = new GZIPInputStream(bis);
            reader = new InputStreamReader(gzip);
            return new Gson().fromJson(reader,classOfT);
        }catch (IOException e){
            throw e;
        } finally {
            if (reader != null){
                reader.close();
            }
            if (gzip != null){
                gzip.close();
            }
            if (bis != null){
                bis.close();
            }

        }
    }

    public static byte[] jsonGzipSerialize(Object obj,int level) throws IOException{
        String objStr = new Gson().toJson(obj);
        byte[] data = objStr.getBytes(Charset.forName("UTF-8"));
        return gzip(data,level);
    }

    public static byte[] ungzip(byte[] data) throws IOException {
        ByteArrayInputStream bis = null;
        GZIPInputStream gzip = null;
        ByteArrayOutputStream bos = null;
        try {
            bis = new ByteArrayInputStream(data);
            gzip = new GZIPInputStream(bis);
            byte[] buf = new byte[1024];
            int num = -1;
            bos = new ByteArrayOutputStream();
            while ((num = gzip.read(buf)) != -1) {
                bos.write(buf, 0, num);
            }
            byte[] ret = bos.toByteArray();
            bos.flush();
            return ret;
        }catch (IOException e){
            throw e;
        } finally {
            if (gzip != null){
                gzip.close();
            }
            if (bis != null){
                bis.close();
            }
            if (bos != null){
                //Log.i("",new String(bos.toByteArray()));
                bos.close();
            }
        }
    }

    public static byte[] gzip(byte[] data,final int level) throws IOException {
        ByteArrayOutputStream bos = null;
        GZIPOutputStream gzip = null;
        try{
            bos = new ByteArrayOutputStream();
            gzip = new GZIPOutputStream(bos){
                {
                    def.setLevel(level);
                }
            };
            gzip.write(data);
            gzip.finish();
            byte[] ret = bos.toByteArray();
            return ret;
        }catch (IOException e){
            throw e;
        }finally {
            if (gzip != null){
                gzip.close();
            }
            if (bos != null){
                bos.close();
            }
        }
    }


}
