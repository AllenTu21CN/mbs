package sanp.tools.utils;

import org.xutils.common.Callback;
import org.xutils.http.RequestParams;
import org.xutils.x;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import sanp.tools.callback.XutilsCallback;

/**
 * Created by Tom on 2016/11/7.
 */
public class UpDownManger {

    public static void upLoadRequest(String path, String url, HashMap<String, String> mParams, final XutilsCallback callback) {
        RequestParams params = new RequestParams(url);
        params.setMultipart(true);
        params.addBodyParameter("file", new File(path));
        if (mParams != null && mParams.size() > 0) {
            Iterator iter = mParams.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                String key = (String) entry.getKey();
                String val = (String) entry.getValue();
                params.addBodyParameter(key, val);
            }
        }
        x.http().post(params, new Callback.CommonCallback<String>() {
            @Override
            public void onSuccess(String result) {
                LogManager.i("上传结果" + result);
                callback.callback(result, true);
            }

            @Override
            public void onError(Throwable ex, boolean isOnCallback) {
                LogManager.e("上传失败");
                callback.callback("", false);
            }

            @Override
            public void onCancelled(CancelledException cex) {
            }

            @Override
            public void onFinished() {
            }
        });
    }


    /**
     * 根据URL得到输入流
     *
     * @param urlStr
     * @return
     * @throws MalformedURLException
     * @throws IOException
     */
    public static InputStream getInputStreamFromUrl(String urlStr)
            throws MalformedURLException, IOException {
        URL url = new URL(urlStr);
        HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
        InputStream inputStream = urlConn.getInputStream();
        return inputStream;
    }

    public static void downLoadRequest(final String path, final String fileName, final String picUrl, final XutilsCallback callback) {
        LogManager.d("下载------" + picUrl + "\n图片名称----- " + fileName);
        new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream inputStream = null;
                try {
                    FileUtils fileUtils = new FileUtils();
                    if (fileUtils.isFileExist(path + fileName)) {
                        inputStream = getInputStreamFromUrl(picUrl);
                        File resultFile = fileUtils.writeToSDFromInput(path,fileName, inputStream);
                        if (resultFile == null) {
                            callback.callback("",false);
                        }
                    } else {
                        inputStream = getInputStreamFromUrl(picUrl);
                        File resultFile = fileUtils.writeToSDFromInput(path,fileName, inputStream);
                        if (resultFile == null) {
                            callback.callback("",false);
                        }
                    }
                } catch (Exception e) {
                   LogManager.e(e);
                    callback.callback("",false);
                } finally {
                    try {
                        inputStream.close();
                    } catch (Exception e) {
                       LogManager.e(e);
                    }
                }
                callback.callback("",true);
            }
        }).start();
    }
}