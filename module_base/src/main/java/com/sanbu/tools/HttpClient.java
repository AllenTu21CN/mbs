package com.sanbu.tools;

import com.sanbu.base.BaseError;
import com.sanbu.base.Callback;
import com.sanbu.base.Result;
import com.sanbu.base.Tuple;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.ConnectionSpec;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.TlsVersion;

public class HttpClient {

    private static final String TAG = HttpClient.class.getSimpleName();

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public static final int CODE_SUCCESS = 200;

    public static class Config {
        public boolean usingHTTPS;
        public int connectTimeoutMS;
        public int readTimeoutMS;
        public int writeTimeoutMS;
        public int pingIntervalMS;

        public Config() {
            usingHTTPS = false;
            connectTimeoutMS = -1;
            readTimeoutMS = -1;
            writeTimeoutMS = -1;
            pingIntervalMS = -1;
        }

        public Config setUsingHttps(boolean usingHttps) {
            this.usingHTTPS = usingHttps;
            return this;
        }

        public boolean isEqual(Config other) {
            return (this.usingHTTPS == other.usingHTTPS &&
                    this.connectTimeoutMS == other.connectTimeoutMS &&
                    this.readTimeoutMS == other.readTimeoutMS &&
                    this.writeTimeoutMS == other.writeTimeoutMS &&
                    this.pingIntervalMS == other.pingIntervalMS);
        }
    }

    public interface OnDownloadListener {
        void onDownloaded(Result result); // success-File
        void onDownloading(int progress);
    }

    private static final Config gDefaultConfig = new Config();
    private static final Config gDefaultSSLConfig = new Config().setUsingHttps(true);
    private static List<Tuple<Config, HttpClient>> gConfigedClients = new LinkedList<>();

    public static HttpClient getDefault() {
        return getInstance(gDefaultConfig);
    }

    public static HttpClient getDefaultSSL() {
        return getInstance(gDefaultSSLConfig);
    }

    public static HttpClient getSpecsSSL() {
        return getInstance(gDefaultSSLConfig, true);
    }

    public static HttpClient getDefault(boolean useSSL) {
        return useSSL?getInstance(gDefaultConfig):getInstance(gDefaultConfig);
    }

    public static HttpClient getInstance(Config config) {
        return getInstance(config, false);
    }

    public static HttpClient getInstance(Config config, boolean specs) {
        if (config == null)
            config = gDefaultConfig;

        synchronized (HttpClient.class) {
            for (Tuple<Config, HttpClient> pair: gConfigedClients) {
                if (pair.first.isEqual(config))
                    return pair.second;
            }
            HttpClient instance;
            if (specs)
                instance = new HttpClient(config, true);
            else
                instance = new HttpClient(config);
            gConfigedClients.add(new Tuple(config, instance));
            return instance;
        }
    }

    private OkHttpClient mClient;

    /* !!!
     * OkHttpClients should be shared:
     * OkHttp performs best when you create a single {@code OkHttpClient} instance and reuse it for
     * all of your HTTP calls. This is because each client holds its own connection pool and thread
     * pools. Reusing connections and threads reduces latency and saves memory. Conversely, creating a
     * client for each request wastes resources on idle pools.
    */

    private HttpClient(Config config) {
        mClient = buildHttpClient(config);
    }

    private HttpClient(Config config, boolean specs) {
        mClient = buildHttpClient(config, specs);
    }

    public OkHttpClient getClient() {
        return mClient;
    }

    public Request call(Request request, final Callback callback) {
        mClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                LogUtil.e(TAG, "call-fail", e);
                callback.done(new Result(-1, null, e));
            }
            @Override
            public void onResponse(Call call, Response response) {
                try {
                    callback.done(new Result(response.code(), null, response.body().string()));
                } catch (IOException e) {
                    callback.done(new Result(-1, null, e));
                }
            }
        });

        return request;
    }

    public void download(final String targetUrl, final String localDir, final OnDownloadListener listener) {
        File dir = new File(localDir);
        if (!dir.exists() && !dir.mkdirs()) {
            if (listener != null)
                listener.onDownloaded(new Result(BaseError.INVALID_PARAM, "can not mkdir: " + localDir));
            return;
        }

        Request request = new Request.Builder().url(targetUrl).build();
        mClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                try {
                    LogUtil.e(TAG, "download-fail", e);
                    if (listener != null)
                        listener.onDownloaded(new Result(-1, null, e));
                } catch (Exception exp) {
                    exp.printStackTrace();
                }
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                InputStream is = null;
                byte[] buf = new byte[2048];
                int len;
                FileOutputStream fos = null;
                Result result;

                try {
                    File dir = new File(localDir);
                    dir.mkdirs();
                    String fullPath = dir.getAbsolutePath();

                    is = response.body().byteStream();
                    long total = response.body().contentLength();

                    File file = new File(fullPath, new File(targetUrl).getName());
                    fos = new FileOutputStream(file);

                    long sum = 0;
                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                        sum += len;
                        if (listener != null) {
                            int progress = (int) (sum * 1.0f / total * 100);
                            listener.onDownloading(progress);
                        }
                    }

                    fos.flush();
                    result = Result.buildSuccess(file);
                } catch (Exception e) {
                    LogUtil.e(TAG, "download-error", e);
                    result = new Result(-1, null, e);
                } finally {
                    try {
                        if (is != null)
                            is.close();
                    } catch (IOException e) {
                    }
                    try {
                        if (fos != null)
                            fos.close();
                    } catch (IOException e) {
                    }
                }

                if (listener != null)
                    listener.onDownloaded(result);
            }
        });
    }

    ///////////////// util functions

    public static Request buildGet(String url) {
        return buildGet(url, null);
    }

    public static Request buildGet(String url, Map<String, String> params) {
        return new Request.Builder()
                .url(encodeParams2Url(url, params))
                .build();
    }

    public static Request buildGet(String url, Map<String, String> params, Map<String, String> extraHeaders) {
        Request.Builder builder = new Request.Builder();
        builder.url(encodeParams2Url(url, params));
        if (extraHeaders != null) {
            for (Map.Entry<String, String> entry: extraHeaders.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        return builder.build();
    }

    // CONTENT_TYPE = application/x-www-form-urlencoded
    public static Request buildPost(String url, Map<String, String> params) {
        FormBody.Builder builder = new FormBody.Builder();
        if (params != null && params.size() > 0) {
            for (Map.Entry<String, String> entry: params.entrySet()) {
                builder.add(entry.getKey(), entry.getValue());
            }
        }
        RequestBody body = builder.build();

        return new Request.Builder()
                .url(url)
                .post(body)
                .build();
    }

    public static Request buildPost(String url, MediaType type, String content, Map<String, String> extraHeaders) {
        Request.Builder builder = new Request.Builder();
        builder.url(url);
        builder.post(RequestBody.create(type, content));
        if (extraHeaders != null) {
            for (Map.Entry<String, String> entry: extraHeaders.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        return builder.build();
    }

    private static OkHttpClient buildHttpClient(Config config) {
        return buildHttpClient(config, false);
    }

    private static OkHttpClient buildHttpClient(Config config, boolean specs) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if (config != null) {
            if (config.usingHTTPS)
                configAsHttps(builder);
            if (config.connectTimeoutMS >= 0)
                builder.connectTimeout(config.connectTimeoutMS, TimeUnit.MILLISECONDS);
            if (config.readTimeoutMS >= 0)
                builder.readTimeout(config.readTimeoutMS, TimeUnit.MILLISECONDS);
            if (config.writeTimeoutMS >= 0)
                builder.writeTimeout(config.writeTimeoutMS, TimeUnit.MILLISECONDS);
            if (config.pingIntervalMS >= 0)
                builder.pingInterval(config.pingIntervalMS, TimeUnit.MILLISECONDS);
        }
        if (specs) {
            ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.COMPATIBLE_TLS)
                    .tlsVersions(TlsVersion.TLS_1_1, TlsVersion.TLS_1_0)
                    .build();
            builder.connectionSpecs(Collections.singletonList(spec));
        }
        return builder.build();
    }

    private static OkHttpClient.Builder configAsHttps(OkHttpClient.Builder builder) {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[0];
                        }
                    }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            HostnameVerifier verifier = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            return builder.sslSocketFactory(sslSocketFactory)
                    .hostnameVerifier(verifier);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String encodeParams2Url(String url, Map<String, String> params) {
        if (params == null || params.size() == 0)
            return url;
        String separator = url.contains("?") ? "&" : "?";
        for (Map.Entry<String, String> entry: params.entrySet()) {
            url += separator + entry.getKey() + "=" + entry.getValue();
            separator = "&";
        }
        return url;
    }

}
