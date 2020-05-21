package com.sanbu.tools;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class WebSocketClient extends WebSocketListener {

    public static class Config extends HttpClient.Config {

    }

    public interface ConnectionObserver {
        void onChanged(boolean connected, int code, String reason);
    }

    public interface MessageHandler {
        void onWSMessage(String text);
        void onWSBinary(ByteString bytes);
    }

    private HttpClient mClient;
    private ConnectionObserver mObserver;
    private MessageHandler mHandler;
    private String mUrl;
    private WebSocket mWebSocket;
    private Object mLock = new Object();

    public WebSocketClient() {
        this(null);
    }

    public WebSocketClient(Config config) {
        mClient = HttpClient.getInstance(config);
    }

    public void connect(String wsUrl, ConnectionObserver observer, MessageHandler handler) {
        synchronized (mLock) {
            if (mObserver != null) {
                LogUtil.w("had been connecting/connected");
                return;
            }

            mUrl = wsUrl;
            mObserver = observer;
            mHandler = handler;

            Request request = HttpClient.buildGet(mUrl);
            mClient.getClient().newWebSocket(request, this);
        }
    }

    public void close() {
        synchronized (mLock) {
            if (mWebSocket != null) {
                if (mWebSocket.close(1000, "closed by client")) {
                    for (int i = 0 ; i < 10 ; ++i) {
                        if (mWebSocket.queueSize() == 0)
                            break;
                        try { Thread.sleep(20); } catch (InterruptedException e) {}
                    }
                }
                mWebSocket = null;
            }

            if (mClient != null) {
                mClient.getClient().dispatcher().cancelAll();
                mClient = null;
            }

            mUrl = null;
            mHandler = null;
            mObserver = null;
        }
    }

    public boolean send(String text) {
        synchronized (mLock) {
            if (mWebSocket == null)
                return false;
            return mWebSocket.send(text);
        }
    }

    public boolean send(ByteString bytes) {
        synchronized (mLock) {
            if (mWebSocket == null)
                return false;
            return mWebSocket.send(bytes);
        }
    }

    /*
    public HttpClient getHttpClient() {
        return mClient;
    }
    */

    ///////////////// implementation of WebSocketListener

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        LogUtil.v("WebSocketClient onOpen");
        synchronized (mLock) {
            mWebSocket = webSocket;
            if (mObserver != null)
                mObserver.onChanged(true, 0, null);
        }
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        synchronized (mLock) {
            try {
                if (mHandler != null)
                    mHandler.onWSMessage(text);
            } catch (Exception e) {
                LogUtil.e("WebSocketClient, onWSMessage error", e);
            }
        }
    }

    @Override
    public void onMessage(WebSocket webSocket, ByteString bytes) {
        synchronized (mLock) {
            try {
                if (mHandler != null)
                    mHandler.onWSBinary(bytes);
            } catch (Exception e) {
                LogUtil.e("WebSocketClient, onWSBinary error", e);
            }
        }
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        LogUtil.v("WebSocketClient onClosing");
        synchronized (mLock) {
            mWebSocket = null;
            if (mObserver != null)
                mObserver.onChanged(false, code, reason);
        }
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        LogUtil.v("WebSocketClient onClosed");
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        LogUtil.v("WebSocketClient onFailure");
        int code = response == null ? -1 : response.code();
        String message = response == null ? "" : response.message();
        message += ": " + (t == null ? "none" : t.getMessage());
        synchronized (mLock) {
            mWebSocket = null;
            if (mObserver != null)
                mObserver.onChanged(false, code, message);
        }
    }
}
