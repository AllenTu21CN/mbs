package com.sanbu.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;

/**
 * 文件管理类
 */
public class FileUtil {

    /**
     * 获取文件夹或文件大小
     * @param filePath
     * @return
     */
    public static long getAutoFileOrFilesSize(String filePath){
        File file = new File(filePath);
        long blockSize=0;
        try {
            if(file.isDirectory()) {
                blockSize = getFileSizes(file);
            } else {
                blockSize = getFileSize(file);
            }
        } catch (Exception e) {
           LogUtil.e(e);
        }
        return blockSize;
    }

    /**** 复制单个文件
    * @param oldPath String 原文件路径 如：c:/fqf.txt
    * @param newPath String 复制后路径 如：f:/fqf.txt
    * @return boolean
    */
    public static void copyFile(String oldPath, String newPath) {
        try {
            int bytesum = 0;
            int byteread = 0;
            File oldfile = new File(oldPath);
            File newfile = new File(newPath);
            if (oldfile.exists()) { //文件存在时
                if(!newfile.getParentFile().exists()) {
                    newfile.getParentFile().mkdirs();
                }
                InputStream inStream = new FileInputStream(oldPath); //读入原文件
                FileOutputStream fs = new FileOutputStream(newPath);
                byte[] buffer = new byte[1444];
                int length;
                while ( (byteread = inStream.read(buffer)) != -1) {
                    bytesum += byteread; //字节数 文件大小
                    fs.write(buffer, 0, byteread);
                }
                inStream.close();
            }
        }
        catch (Exception e) {
           LogUtil.e("复制单个文件操作出错。" + e);
        }
    }

    /**
     * 复制整个文件夹内容
     * @param oldPath String 原文件路径 如：c:/fqf
     * @param newPath String 复制后路径 如：f:/fqf/ff
     * @return boolean
     */
    public static void copyFolder(String oldPath, String newPath) {

        try {
            (new File(newPath)).mkdirs(); //如果文件夹不存在 则建立新文件夹
            File a=new File(oldPath);
            String[] file=a.list();
            File temp=null;
            for (int i = 0; i < file.length; i++) {
                if(oldPath.endsWith(File.separator)){
                    temp=new File(oldPath+file[i]);
                }
                else{
                    temp=new File(oldPath+File.separator+file[i]);
                }

                if(temp.isFile()){
                    FileInputStream input = new FileInputStream(temp);
                    FileOutputStream output = new FileOutputStream(newPath + "/" +
                            (temp.getName()).toString());
                    byte[] b = new byte[1024 * 5];
                    int len;
                    while ( (len = input.read(b)) != -1) {
                        output.write(b, 0, len);
                    }
                    output.flush();
                    output.close();
                    input.close();
                }
                if(temp.isDirectory()){//如果是子文件夹
                    copyFolder(oldPath+"/"+file[i],newPath+"/"+file[i]);
                }
            }
        }
        catch (Exception e) {
           LogUtil.e("复制整个文件夹内容操作出错。" + e);
        }
    }

    public static boolean deleteDir(String path) {
        return deleteDir(new File(path));
    }

    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (int i = 0; i < children.length; i++) {
                    boolean success = deleteDir(new File(dir, children[i]));
                    if (!success) {
                        return false;
                    }
                }
            }
        }
        return dir.delete();
    }

    /**
     * 将文本文件中的内容读入到buffer中
     * @param buffer buffer
     * @param filePath 文件路径
     */
    public static void readLinesToBuffer(StringBuffer buffer, String filePath) throws IOException {
        readToBuffer(buffer, filePath);
    }

    public static void readToBuffer(StringBuffer buffer, String filePath) throws IOException {
        InputStream is = new FileInputStream(filePath);
        String line; // 用来保存每行读取的内容
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        line = reader.readLine();
        while (line != null) {
            buffer.append(line);
            buffer.append("\n"); // 添加换行符
            line = reader.readLine();
        }
        reader.close();
        is.close();
    }

    public static String readToText(String file) {
        File target = new File(file);
        if (!target.exists() || !target.isFile())
            return null;

        InputStream is = null;
        BufferedReader reader = null;
        char buffer[] = new char[1024];

        try {
            StringBuffer sb = new StringBuffer();
            is = new FileInputStream(target);
            reader = new BufferedReader(new InputStreamReader(is));

            int len = 0;
            while ((len = reader.read(buffer)) > 0)
                sb.append(buffer, 0, len);

            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (reader != null)
                    reader.close();
                if (is != null)
                    is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean writeToFile(String filePath, String data) {
        File file = new File(filePath);
        if (!file.exists()) {
            try {
                if (!(file.createNewFile())) {
                    LogUtil.w(String.format("create file[%s] failed", filePath));
                    return false;
                }
            } catch (IOException e) {
                LogUtil.w(String.format("create file[%s] failed2", filePath), e);
                return false;
            }
        } else if (!file.isFile()) {
            LogUtil.e(filePath + " is not a file");
            return false;
        }

        FileWriter fw = null;
        try {
            fw = new FileWriter(file);
            fw.write(data);
            fw.close();
            return true;
        } catch (Exception e) {
            LogUtil.e("file write error", e);
            return false;
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 转换文件大小
     * @param fileS
     * @return
     */
    public static String FormatFileSize(long fileS)
    {
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        String wrongSize="0B";
        if(fileS==0){
            return wrongSize;
        }
        if (fileS < 1024){
            fileSizeString = df.format((double) fileS) + "B";
        }
        else if (fileS < 1048576){
            fileSizeString = df.format((double) fileS / 1024) + "KB";
        }
        else if (fileS < 1073741824){
            fileSizeString = df.format((double) fileS / 1048576) + "MB";
        }
        else{
            fileSizeString = df.format((double) fileS / 1073741824) + "GB";
        }
        return fileSizeString;
    }

    /**
     * 获取指定文件大小
     * @param file
     * @return
     * @throws Exception
     */
    private static long getFileSize(File file) throws Exception
    {
        long size = 0;
        if (file.exists()){
            size = file.length();
        }
        else{
            file.createNewFile();
        }
        return size;
    }

    /**
     * 获取指定文件夹
     * @param f
     * @return
     * @throws Exception
     */
    private static long getFileSizes(File f) throws Exception
    {
        long size = 0;
        File flist[] = f.listFiles();
        for (int i = 0; i < flist.length; i++){
            if (flist[i].isDirectory()){
                size = size + getFileSizes(flist[i]);
            }
            else{
                size =size + getFileSize(flist[i]);
            }
        }
        return size;
    }

    /**
     * 从raw包下读取数据
     * @param input
     * @return
     */
    public static String readFileFromRaw(InputStream input) {
        try {
//            InputStreamReader inputReader = new InputStreamReader(context.getResources().openRawResource(rawName));
            InputStreamReader inputReader = new InputStreamReader(input);
            BufferedReader bufReader = new BufferedReader(inputReader);
            String line = "";
            String result = "";
            while ((line = bufReader.readLine()) != null)
                result += line;
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}

