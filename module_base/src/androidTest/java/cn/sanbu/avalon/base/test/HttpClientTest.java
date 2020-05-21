package cn.sanbu.avalon.base.test;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;


import okhttp3.Request;

import com.sanbu.base.Callback;
import com.sanbu.base.Result;
import com.sanbu.tools.HttpClient;

@RunWith(AndroidJUnit4.class)
public class HttpClientTest {
    @Test
    public void testHttpClient() throws Exception {
        Request request = HttpClient.buildGet("http://news.baidu.com/", null);
        HttpClient.getDefault().call(request, new Callback() {
            @Override
            public void done(Result result) {

            }
        });
    }
}