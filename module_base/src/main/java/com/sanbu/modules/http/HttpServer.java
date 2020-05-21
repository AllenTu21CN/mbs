package com.sanbu.modules.http;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLServerSocketFactory;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by huangzy on 2018/8/9.
 */

public class HttpServer extends NanoHTTPD {

    private Map<String,Handler> handlers = new ConcurrentHashMap<>();

    public HttpServer(int port) {
        super(port);
    }

    public HttpServer(String hostname, int port) {
        super(hostname, port);
    }

    public static SSLServerSocketFactory makeSSLSocketFactory(InputStream keystoreStream, char[] passphrase,String type) throws IOException {
        try {
            KeyStore keystore = KeyStore.getInstance(type);
            keystore.load(keystoreStream, passphrase);
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keystore, passphrase);
            return NanoHTTPD.makeSSLSocketFactory(keystore,keyManagerFactory);
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        } finally {
            if (keystoreStream != null) {
                keystoreStream.close();
            }
        }
    }

    public static SSLServerSocketFactory makeSSLSocketFactory(InputStream keystoreStream, char[] passphrase) throws IOException {
        return makeSSLSocketFactory(keystoreStream, passphrase,"BKS");
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri= session.getUri();
        Handler handler = handlers.get(uri);
        if (handler != null){
               return handler.ServeHTTP(session);
        }
        return newFixedLengthResponse(Response.Status.NOT_FOUND,MIME_PLAINTEXT,"404 page not found");
    }

    public void Handle(String pattern,Handler handler){
        if (wasStarted()) return;
        handlers.put(pattern,handler);
    }

    public interface Handler{
        Response ServeHTTP(IHTTPSession session);
    }

}
