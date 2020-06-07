package cn.sanbu.avalon.base.test;

import androidx.test.runner.AndroidJUnit4;

import com.sanbu.base.Callback;
import com.sanbu.base.Result;
import com.sanbu.tools.HttpClient;
import com.sanbu.tools.LogUtil;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Request;
import okio.Buffer;
import okio.BufferedSink;

@RunWith(AndroidJUnit4.class)
public class HttpTest {

    @Test
    public void testHTTP() {
        getToken();
    }

    private static void getToken() {
        final String url = "http://yun.3bu.cn:88/middlecenter/oauth2/authorize";
        final HashMap<String, String> params = new HashMap() {{
            put("client_id", "6b4cfaea-7016-11e5-bd19-68f728833c05");
            put("response_type", "code");
            put("redirect_uri", "/oauth2/client/oauth_callback");
            put("username", "zgadmin");
            put("password", "Mm123456");
        }};

        final Request request = HttpClient.buildPost(url, params);
        onRequest(request, false, true);

        HttpClient.getDefault().call(request, new Callback() {
            @Override
            public void done(Result result) {
                onResponse(request, result, true);

                if (result.code == HttpClient.CODE_SUCCESS && result.data != null) {
                    try {
                        JSONObject dataObj = new JSONObject((String) result.data);
                        String access_token = dataObj.optString("access_token");
                        LogUtil.w("access_token: " + access_token);
                        getTenantList(access_token);
                    } catch (JSONException e) {
                        LogUtil.w("JSONObject parse error" + e.getLocalizedMessage());
                    }
                } else {
                    LogUtil.w("request error: " + result.getMessage());
                }
            }
        });
    }

    public static void getTenantList(final String token) {
        final String url = "http://yun.3bu.cn:88/middlecenter/service/oauth_api/v1/foreign/lesson/query/tenant";
        Map<String, String> params = new HashMap() {{
            put("access_token", token);
            put("client_id", "6b4cfaea-7016-11e5-bd19-68f728833c05");
            put("pageIndex", "1");
            put("pageSize", "1000");
        }};

        final Request request = HttpClient.buildPost(url, params);
        onRequest(request, true, true);

        HttpClient.getDefault().call(request, new Callback() {
            @Override
            public void done(Result rsp) {
                onResponse(request, rsp, true);
                if (rsp.code == HttpClient.CODE_SUCCESS && rsp.data != null) {
                    try {
                        JSONObject object = new JSONObject((String) rsp.data);
                        String listArray = object.getString("rows");
                        LogUtil.w("tenantList: " + listArray);
                        return;
                    } catch (JSONException e) {
                        rsp = new Result(-1, "JSONException", e);
                    }
                }
                onRequestFailure("getTenantList", rsp);
            }
        });
    }

    private static void onRequest(Request request, boolean printBody, boolean isVerbose) {
        String body = "***";
        if (printBody && request.method().equals("POST")) {
            BufferedSink sink = new Buffer();
            try {
                request.body().writeTo(sink);
                body = sink.toString();
            } catch (IOException e) {
                e.printStackTrace();
                body = "N/A";
            }
        }
        LogUtil.w("plt4http_message, send: " + request.toString() + " { body = " + body + "}");
    }

    private static void onResponse(Request request, Result result, boolean isVerbose) {
        String response = "Response{code=" + result.code + ", message=" + result.message;
        if (result.data != null)
            response += ", data=" + result.data.toString();
        response += "}";
        LogUtil.w("plt4http_message, recv: " + response + " for " + request);
    }

    private static void onRequestFailure(String action, Result result) {
        LogUtil.w(action + " failed: " + result.getError());
    }
}
