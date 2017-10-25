package sanp.mp100;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.crossbar.autobahn.wamp.types.CallResult;

import sanp.avalon.libs.base.utils.LogManager;
import sanp.mp100.integration.BusinessPlatform;

public class BusinessPlatformTestActivity extends AppCompatActivity implements View.OnClickListener {

    //    private static final String websocketURL = "ws://10.1.0.75:8080";
    private static final String websocketURL1 = "ws://192.168.1.109:8080";
    private static final String realm1 = "device-hub.samples";
    private static final String websocketURL2 = "ws://lbblscy.3322.org:8085/device-hub";
    private static final String realm2 = "ebp.daemon";

    private BusinessPlatform mBusinessPlatform;

    private TextView mMsgText;
    private String mMsg = "";

    private Button mBtnSum;
    private Button mBtnSum2;
    private Button mBtnGetProvinces;
    private Button mBtnConnect;
    private Button mBtnDisConnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_business_platform_test);

        mBusinessPlatform = BusinessPlatform.getInstance();

        mMsgText = (TextView) findViewById(R.id.text_result);
        mMsgText.setText(mMsg);

        mBtnSum = (Button) findViewById(R.id.btn_sum);
        mBtnSum.setOnClickListener(this);

        mBtnSum2 = (Button) findViewById(R.id.btn_sum2);
        mBtnSum2.setOnClickListener(this);

        mBtnGetProvinces = (Button) findViewById(R.id.btn_getProvinces);
        mBtnGetProvinces.setOnClickListener(this);

        mBtnConnect = (Button) findViewById(R.id.btn_connect);
        mBtnConnect.setOnClickListener(this);

        mBtnDisConnect = (Button) findViewById(R.id.btn_disconnect);
        mBtnDisConnect.setOnClickListener(this);

        mBtnConnect.setEnabled(true);
        mBtnDisConnect.setEnabled(false);
        enableInvoke(false);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_connect:
                //connectRemote();
                connectRemote2();
                break;
            case R.id.btn_disconnect:
                disconnectRemote();
                break;
            case R.id.btn_sum:
                invokeFuncSum2(mBtnSum, "");
                break;
            case R.id.btn_sum2:
                invokeFuncSum2(mBtnSum2, "2");
                break;
            case R.id.btn_getProvinces:
                //invokeFuncGetProvinces(mBtnGetProvinces);
                invokeFuncGetProvinces2(mBtnGetProvinces);
                break;
        }
    }

    private void connectRemote() {
        mBtnConnect.setEnabled(false);

        // finally, provide everything to a Client instance and connect
        LogManager.e("call connect");
        int ret = mBusinessPlatform.syncConnect(websocketURL1, realm1, this::onBusinessPlatformStateChanged);
        LogManager.e("after connect: " + ret);

        if(ret < 0)
            mBtnConnect.setEnabled(true);
    }

    private void connectRemote2() {
        mBtnConnect.setEnabled(false);

        // finally, provide everything to a Client instance and connect
        LogManager.e("call connect");
        int ret = mBusinessPlatform.asyncConnect(websocketURL2, realm2, this::onBusinessPlatformStateChanged);
        LogManager.e("after connect: " + ret);

        if(ret < 0)
            mBtnConnect.setEnabled(true);
    }

    private void disconnectRemote() {
        mBtnDisConnect.setEnabled(false);

        // finally, provide everything to a Client instance and connect
        LogManager.e("call leave");
        int ret = mBusinessPlatform.disconnect();
        LogManager.e("after leave: " + ret);

        if(ret < 0)
            mBtnDisConnect.setEnabled(true);
    }

    private void invokeFuncSum(Button btn, String suffix) {
        btn.setEnabled(false);

        String procedureName = "sum" + suffix;
        List<Object> args = new ArrayList<>();
        for(int i = 1; i < 4 ; ++i)
            args.add(i);

        LogManager.e("invoke " + procedureName);
        CallResult result = mBusinessPlatform.syncInvoke(procedureName, args, null);
        if (result != null) {
            LogManager.i("Sum: " + result.results.get(0));
            mMsg += result.results.get(0) + "\n";
            mMsgText.setText(mMsg);
        }
        LogManager.e("after " + procedureName);

        btn.setEnabled(true);
    }

    private void invokeFuncSum2(Button btn, String suffix) {
        btn.setEnabled(false);

        String procedureName = "sum" + suffix;
        List<Object> params = new ArrayList<>();
        for(int i = 1; i < 4 ; ++i)
            params.add(i);

        LogManager.e("invoke " + procedureName);
        int ret = mBusinessPlatform.asyncInvoke(
                procedureName,
                params,
                null,
                (value, args, kwargs) -> {
                    if(value == 0) {
                        LogManager.i("Sum: " + args.get(0));
                        mMsg += args.get(0) + "\n";
                        mMsgText.setText(mMsg);
                    } else {
                        LogManager.e("invokeFuncSum2 fail: " + kwargs.get("message"));
                    }
                    btn.setEnabled(true);
                }
        );
        LogManager.e("after " + procedureName + ": " + ret);
    }

    private void invokeFuncGetProvinces(Button btn) {
        btn.setEnabled(false);

        LogManager.e("invokeFuncGetProvinces");
        List<BusinessPlatform.Province> provinces = mBusinessPlatform.getAreaProvinces();
        if(provinces != null) {
            mMsg += "getAreaProvinces relust:\n";
            for(BusinessPlatform.Province item: provinces) {
                mMsg += item.id + "," + item.province + "\n";
            }
            mMsg += "----\n";
            mMsgText.setText(mMsg);
        }
        LogManager.e("after invokeFuncGetProvinces");

        btn.setEnabled(true);
    }

    private void invokeFuncGetProvinces2(Button btn) {
        btn.setEnabled(false);

        LogManager.e("invokeFuncGetProvinces2");
        int ret = mBusinessPlatform.getAreaProvinces(
                (value, args, kwargs) -> {
                    if(value == 0) {
                        mMsg += "getAreaProvinces relust:\n";
                        for(Object item: args) {
                            BusinessPlatform.Province province = (BusinessPlatform.Province) item;
                            mMsg += province.id + "," + province.province + "\n";
                        }
                        mMsg += "----\n";
                        mMsgText.setText(mMsg);
                    } else {
                        LogManager.e("invokeFuncGetProvinces2 fail: " + kwargs.get("message"));
                    }
                    btn.setEnabled(true);
                }
        );
        LogManager.e("after invokeFuncGetProvinces2: " + ret);
    }

    private void enableInvoke(boolean able) {
        mBtnSum.setEnabled(able);
        mBtnSum2.setEnabled(able);
        mBtnGetProvinces.setEnabled(able);
    }

    private void onBusinessPlatformStateChanged(int connected, List<Object> useless1, Map<String, Object> useless2) {
        if(connected == 0) {
            LogManager.i("Disconnect from BusinessPlatform");
            mBtnConnect.setEnabled(true);
            mBtnDisConnect.setEnabled(false);
            enableInvoke(false);
        } else {
            LogManager.i("Has connected with BusinessPlatform");
            mBtnConnect.setEnabled(false);
            mBtnDisConnect.setEnabled(true);
            enableInvoke(true);
        }
    }
}
