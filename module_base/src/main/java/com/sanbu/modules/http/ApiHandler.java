package com.sanbu.modules.http;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;

/**
 * Created by huangzy on 2018/8/9.
 */

public class ApiHandler implements HttpServer.Handler {


    private static final String TAG = ApiHandler.class.getName();
    private static final String CONTENT_TYPE = "application/json";
    private static final String JSON_RPC = "2.0";

    private Api mApi;
    private Gson mGson;

    public ApiHandler(Api api) {
        mApi = api;
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Error.class, new ErrorSerializer());
        mGson = builder.create();
    }

    @Override
    public Response ServeHTTP(IHTTPSession session) {
        Response response = serveHTTP(session);
        if (response == null) {
            response = NanoHTTPD.newFixedLengthResponse(Status.METHOD_NOT_ALLOWED, NanoHTTPD.MIME_PLAINTEXT, null);
        }
        return response;
    }

    public Response serveHTTP(IHTTPSession session) {
        if (session.getMethod() != Method.POST)
            return NanoHTTPD.newFixedLengthResponse(Status.METHOD_NOT_ALLOWED, NanoHTTPD.MIME_PLAINTEXT, "Method must be POST");
        String ContentType = session.getHeaders().get("content-type");
        if (ContentType == null || ContentType.indexOf(ApiHandler.CONTENT_TYPE) < 0)
            return NanoHTTPD.newFixedLengthResponse(Status.METHOD_NOT_ALLOWED, NanoHTTPD.MIME_PLAINTEXT, "Content-Type must be application/json");
        Response resp;
        try {
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            if (files.get("postData") == null)
                return NanoHTTPD.newFixedLengthResponse(Status.METHOD_NOT_ALLOWED, NanoHTTPD.MIME_PLAINTEXT, "Body is null");
            JsonRequest request = mGson.fromJson(files.get("postData"), JsonRequest.class);
            if (!JSON_RPC.equals(request.jsonrpc))
                return NanoHTTPD.newFixedLengthResponse(Status.METHOD_NOT_ALLOWED, NanoHTTPD.MIME_PLAINTEXT, "JSON_RPC must be 2.0");
            JsonResponse response = withApi(request);
            if (response == null) {
                response = BuildError(Error.NoResponseError);
            }
            response.id = request.id;
            response.jsonrpc = request.jsonrpc;
            String txt = mGson.toJson(response).toString();
            resp = NanoHTTPD.newFixedLengthResponse(Status.OK, CONTENT_TYPE, txt);
        } catch (JsonSyntaxException | JsonIOException e) {
            resp = NanoHTTPD.newFixedLengthResponse(Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT, "JSON Parse error");
            Log.d(TAG, "serveHTTP: ", e);
        } catch (Exception e) {
            resp = NanoHTTPD.newFixedLengthResponse(Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, Status.INTERNAL_ERROR.name());
            Log.d(TAG, "serveHTTP: ", e);
        }
        return resp;
    }


    private JsonResponse withApi(JsonRequest request) {
        if (mApi == null) {
            return BuildError(Error.MethodNotFound);
        }
        try {
            switch (request.method) {
                case "ep_call":
                    if (request.params == null) {
                        return BuildError(Error.MissingParams);
                    }
                    JsonElement url = request.params.getAsJsonObject().get("url");
                    if (url == null) {
                        return BuildError(Error.MissingParams);
                    }
                    return mApi.epCall(url.getAsString());
                case "ep_release":
                    return mApi.epRelease();
                case "ep_open_ext_channel":
                    return mApi.epOpenExtChannel();
                case "ep_close_ext_channel":
                    return mApi.epCloseExtChannel();
                case "ep_configure":
                    if (request.params == null) {
                        return BuildError(Error.MissingParams);
                    }
                    return mApi.epConfigure(request.params);
                case "ep_info":
                    return mApi.epInfo();
                case "ep_reboot":
                    return mApi.epReboot();
                case "ep_power_off":
                    return mApi.epPowerOff();
                default:
                    return BuildError(Error.MethodNotFound);
            }
        } catch (IllegalStateException e) {
            Log.d(TAG, e.getMessage(), e);
            return BuildError(Error.InvalidParams);
        } catch (Exception e) {
            Log.d(TAG, e.getMessage(), e);
            return BuildError(Error.InternalError);
        }

    }


    public interface Api {

        JsonResponse epCall(String url);

        JsonResponse epRelease();

        JsonResponse epOpenExtChannel();

        JsonResponse epCloseExtChannel();

        JsonResponse epConfigure(JsonElement params);

        JsonResponse epInfo();

        JsonResponse epReboot();

        JsonResponse epPowerOff();
    }

    private static class JsonRequest {
        String jsonrpc;
        String method;
        JsonElement params;
        int id;
    }

    public static class JsonResponse {
        int id;
        String jsonrpc;
        Object result;
        Error error;
    }

    public static JsonResponse BuildSuccess() {
        JsonResponse resp = new JsonResponse();
        resp.result = true;
        return resp;
    }

    public static JsonResponse BuildResult(Object result) {
        JsonResponse resp = new JsonResponse();
        resp.result = result;
        return resp;
    }

    public static JsonResponse BuildError(Error err) {
        JsonResponse resp = new JsonResponse();
        resp.error = err;
        return resp;
    }

    public class ErrorSerializer implements JsonSerializer<Error>, JsonDeserializer<Error> {

        @Override
        public JsonElement serialize(Error src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            json.addProperty("code", src.code);
            json.addProperty("message", src.name());
            return json;
        }

        @Override
        public Error deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            if (obj != null && obj.has("code")) {
                int code = obj.get("code").getAsInt();
                for (int i = 0; i < Error.values().length; i++) {
                    if (code == Error.values()[i].code) return Error.values()[i];
                }
            }
            return null;
        }
    }

    public enum Error {
        ParseError(-32700),
        InvalidRequest(-32600),
        MethodNotFound(-32601),
        InvalidParams(-32602),
        InternalError(-32603),
        MissingParams(-32604),
        NoResponseError(-32005);
        public int code;

        Error(int code) {
            this.code = code;
        }

    }
}
