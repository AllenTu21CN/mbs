package sanp.mp100.ui.test;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.crossbar.autobahn.wamp.types.CallResult;

import sanp.avalon.libs.base.utils.LogManager;
import sanp.mp100.R;
import sanp.mp100.integration.BusinessPlatform;
import sanp.mp100.integration.BusinessPlatformPostman;
import sanp.mp100.integration.BusinessPlatformPostman.State;

public class BusinessPlatformPostmanTestActivity extends AppCompatActivity implements View.OnClickListener {

    //    private static final String websocketURL = "ws://10.1.0.75:8080";
    private static final String websocketURL1 = "ws://192.168.1.109:8080";
    private static final String realm1 = "device-hub.samples";
    private static final String websocketURL2 = "ws://lbblscy.3322.org:8085/device-hub";
    private static final String realm2 = "ebp.daemon";

    private BusinessPlatform mBusinessPlatform;
    private BusinessPlatformPostman mBusinessPlatformPostman;

    private TextView mMsgText;
    private String mMsg = "";

    private Button mBtnConnect;
    private Button mBtnConnect2;
    private Button mBtnDisConnect;

    private Button mBtnSum;
    private Button mBtnSum2;
    private Button mBtnGetProvinces;
    private Button mBtnGetProvinces2;
    private Button mBtnGetCities;
    private Button mBtnGetCities2;
    private Button mBtnGetLessonTimetable;
    private Button mBtnGetLessonTimetable2;

    private Handler mHandler = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_business_platform_postman_test);

        mBusinessPlatform = BusinessPlatform.getInstance();
        mBusinessPlatformPostman = mBusinessPlatform.getmPlatformPostman();

        mMsgText = (TextView) findViewById(R.id.text_result);
        mMsgText.setText(mMsg);

        findViewById(R.id.button_clean_msg).setOnClickListener(this);

        mBtnSum = (Button) findViewById(R.id.btn_sum);
        mBtnSum.setOnClickListener(this);

        mBtnSum2 = (Button) findViewById(R.id.btn_sum2);
        mBtnSum2.setOnClickListener(this);

        mBtnGetProvinces = (Button) findViewById(R.id.btn_getProvinces);
        mBtnGetProvinces.setOnClickListener(this);

        mBtnGetProvinces2 = (Button) findViewById(R.id.btn_getProvinces2);
        mBtnGetProvinces2.setOnClickListener(this);

        mBtnGetCities = (Button) findViewById(R.id.btn_getCities);
        mBtnGetCities.setOnClickListener(this);

        mBtnGetCities2 = (Button) findViewById(R.id.btn_getCities2);
        mBtnGetCities2.setOnClickListener(this);

        mBtnGetLessonTimetable = (Button) findViewById(R.id.btn_getLessonTimetable);
        mBtnGetLessonTimetable.setOnClickListener(this);

        mBtnGetLessonTimetable2 = (Button) findViewById(R.id.btn_getLessonTimetable2);
        mBtnGetLessonTimetable2.setOnClickListener(this);

        mBtnConnect = (Button) findViewById(R.id.btn_connect);
        mBtnConnect.setOnClickListener(this);

        mBtnConnect2 = (Button) findViewById(R.id.btn_connect2);
        mBtnConnect2.setOnClickListener(this);

        mBtnDisConnect = (Button) findViewById(R.id.btn_disconnect);
        mBtnDisConnect.setOnClickListener(this);

        State state = mBusinessPlatformPostman.state();
        if(state == State.NONE) {
            mBtnConnect.setEnabled(true);
            mBtnConnect2.setEnabled(true);
            mBtnDisConnect.setEnabled(false);
            enableInvoke(false, false);
        } else if(state == State.CONNECTING) {
            throw new RuntimeException("TODO: !!!! 1");
        } else if(state == State.READY) {
            mBtnConnect.setEnabled(false);
            mBtnConnect2.setEnabled(false);
            mBtnDisConnect.setEnabled(true);
            enableInvoke(true, false);
        } else if(state == State.DISCONNECTING) {
            throw new RuntimeException("TODO: !!!! 2");
        }

        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 0:
                        ((Button)msg.obj).setEnabled(msg.arg1!=0);
                        break;
                    case 1:
                        mMsgText.setText(mMsg);
                        break;
                    default:
                        break;
                }
            }
        };
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_clean_msg:
                mMsg = "";
                mMsgText.setText(mMsg);
                break;
            case R.id.btn_connect:
                connectRemote();
                break;
            case R.id.btn_connect2:
                connectRemote2();
                break;
            case R.id.btn_disconnect:
                disconnectRemote();
                break;
            case R.id.btn_sum:
                invokeFuncSum(mBtnSum, "");
                break;
            case R.id.btn_sum2:
                invokeFuncSum2(mBtnSum2, "2");
                break;
            case R.id.btn_getProvinces:
                invokeFuncGetProvinces(mBtnGetProvinces);
                break;
            case R.id.btn_getProvinces2:
                invokeFuncGetProvinces2(mBtnGetProvinces2);
                break;
            case R.id.btn_getCities:
                invokeFuncGetCities(mBtnGetCities);
                break;
            case R.id.btn_getCities2:
                invokeFuncGetCities2(mBtnGetCities2);
                break;
            case R.id.btn_getLessonTimetable:
                invokeFuncGetLessonTimetable(mBtnGetLessonTimetable);
                break;
            case R.id.btn_getLessonTimetable2:
                invokeFuncGetLessonTimetable2(mBtnGetLessonTimetable2);
                break;
        }
    }

    private void connectRemote() {
        mBtnConnect.setEnabled(false);
        mBtnConnect2.setEnabled(false);

        // finally, provide everything to a Client instance and connect
        LogManager.e("call connect");
        int ret = mBusinessPlatformPostman.syncConnect(websocketURL2, realm2, this::onBusinessPlatformStateChanged);
        LogManager.e("after connect: " + ret);

        onBusinessPlatformStateChanged(ret == 0 ? 1 : 0, null, null);
    }

    private void connectRemote2() {
        mBtnConnect.setEnabled(false);
        mBtnConnect2.setEnabled(false);

        // finally, provide everything to a Client instance and connect
        LogManager.e("call connect");
        int ret = mBusinessPlatformPostman.asyncConnect(websocketURL2, realm2, this::onBusinessPlatformStateChanged);
        LogManager.e("after connect: " + ret);

        if(ret != 0) {
            mBtnConnect.setEnabled(true);
            mBtnConnect2.setEnabled(true);
        }
    }

    private void disconnectRemote() {
        mBtnDisConnect.setEnabled(false);

        // finally, provide everything to a Client instance and connect
        LogManager.e("call leave");
        int ret = mBusinessPlatformPostman.disconnect();
        LogManager.e("after leave: " + ret);

        if(ret != 0)
            mBtnDisConnect.setEnabled(true);
    }

    private void invokeFuncSum(Button btn, String suffix) {
        String procedureName = "sum" + suffix;
        LogManager.e("invoke " + procedureName);
        btn.setEnabled(false);

        List<Object> args = new ArrayList<>();
        for(int i = 1; i < 4 ; ++i)
            args.add(i);

        try {
            CallResult result = mBusinessPlatformPostman.syncInvoke(procedureName, args, null);
            LogManager.i("Sum: " + result.results.get(0));
            mMsg += result.results.get(0) + "\n";
            mMsgText.setText(mMsg);
        } catch (InterruptedException | InternalError e) {
            e.printStackTrace();
        }

        btn.setEnabled(true);
        LogManager.e("after " + procedureName);
    }

    private void invokeFuncSum2(Button btn, String suffix) {
        btn.setEnabled(false);

        String procedureName = "sum" + suffix;
        List<Object> params = new ArrayList<>();
        for(int i = 1; i < 4 ; ++i)
            params.add(i);

        LogManager.e("invoke " + procedureName);
        int ret = mBusinessPlatformPostman.asyncInvoke(
                procedureName,
                params,
                null,
                (value, args, kwargs) -> {
                    if(value == 0) {
                        LogManager.i("Sum: " + args.get(0));
                        mMsg += args.get(0) + "\n";
                        flushMsgView();
                    } else {
                        LogManager.e("invokeFuncSum2 fail: " + kwargs.get("message"));
                    }
                    setBtnEnable(btn, true);
                }
        );
        LogManager.e("after " + procedureName + ": " + ret);
    }

    private void invokeFuncGetProvinces(Button btn) {
        LogManager.e("invokeFuncGetProvinces");
        btn.setEnabled(false);

        try {
            List<BusinessPlatform.Province> provinces = mBusinessPlatform.getAreaProvinces();
            mMsg += "getAreaProvinces relust:\n";
            for(BusinessPlatform.Province item: provinces) {
                mMsg += item.id + "," + item.province + "\n";
            }
            mMsg += "----\n";
            mMsgText.setText(mMsg);
        } catch (InterruptedException | InternalError e) {
            e.printStackTrace();
        }

        btn.setEnabled(true);
        LogManager.e("after invokeFuncGetProvinces");
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
                        flushMsgView();
                    } else {
                        LogManager.e("invokeFuncGetProvinces2 fail: " + kwargs.get("message"));
                    }
                    setBtnEnable(btn, true);
                }
        );
        LogManager.e("after invokeFuncGetProvinces2: " + ret);
    }

    private void invokeFuncGetCities(Button btn) {
        LogManager.e("invokeFuncGetCities");
        btn.setEnabled(false);

        try {
            List<BusinessPlatform.City> cities = mBusinessPlatform.getAreaCitiesByProvince("广东省");
            mMsg += "getAreaCitiesByProvince relust:\n";
            for(BusinessPlatform.City item: cities) {
                mMsg += item.id + "," + item.city + "\n";
            }
            mMsg += "----\n";
            mMsgText.setText(mMsg);
        } catch (InterruptedException | InternalError e) {
            e.printStackTrace();
        }

        btn.setEnabled(true);
        LogManager.e("after invokeFuncGetCities");
    }

    private void invokeFuncGetCities2(Button btn) {
        btn.setEnabled(false);

        LogManager.e("invokeFuncGetCities2");
        int ret = mBusinessPlatform.getAreaCitiesByProvince("广东省",
                (value, args, kwargs) -> {
                    if(value == 0) {
                        mMsg += "getAreaCitiesByProvince relust:\n";
                        for(Object item: args) {
                            BusinessPlatform.City city = (BusinessPlatform.City) item;
                            mMsg += city.id + "," + city.city + "\n";
                        }
                        mMsg += "----\n";
                        flushMsgView();
                    } else {
                        LogManager.e("invokeFuncGetCities2 fail: " + kwargs.get("message"));
                    }
                    setBtnEnable(btn, true);
                }
        );
        LogManager.e("after invokeFuncGetCities2: " + ret);
    }

    private void invokeFuncGetLessonTimetable(Button btn) {
        LogManager.e("invokeFuncGetLessonTimetable");
        btn.setEnabled(false);

        try {
            List<BusinessPlatform.TimeTable> tables = mBusinessPlatform.getLessonTimetable(1, "2017-10-16", "2017-10-22");
            mMsg += "getLessonTimetable relust:\n";
            for(BusinessPlatform.TimeTable item: tables) {
                mMsg += item.id + "," + item.type + "," + item.subject_name + "," + item.title + "," + item.date + "\n";
            }
            mMsg += "----\n";
            mMsgText.setText(mMsg);
        } catch (InterruptedException | InternalError e) {
            e.printStackTrace();
        }

        btn.setEnabled(true);
        LogManager.e("after invokeFuncGetLessonTimetable");
    }

    private void invokeFuncGetLessonTimetable2(Button btn) {
        btn.setEnabled(false);

        LogManager.e("invokeFuncGetLessonTimetable2");
        int ret = mBusinessPlatform.getLessonTimetable(1, "2017-10-16", "2017-10-22",
                (value, args, kwargs) -> {
                    if(value == 0) {
                        mMsg += "getLessonTimetable relust:\n";
                        for(Object item: args) {
                            BusinessPlatform.TimeTable table = (BusinessPlatform.TimeTable) item;
                            mMsg += table.id + "," + table.type + "," + table.subject_name + "," + table.title + "," + table.date + "\n";
                        }
                        mMsg += "----\n";
                        flushMsgView();
                    } else {
                        LogManager.e("invokeFuncGetLessonTimetable2 fail: " + kwargs.get("message"));
                    }
                    setBtnEnable(btn, true);
                }
        );
        LogManager.e("after invokeFuncGetLessonTimetable2: " + ret);
    }

    private void setBtnEnable(Button btn, boolean able) {
        mHandler.sendMessage(Message.obtain(mHandler, 0, able?1:0, 0, btn));
    }

    private void flushMsgView() {
        mHandler.sendMessage(Message.obtain(mHandler, 1));
    }

    private void enableInvoke(boolean able, boolean async) {
        if(async) {
            setBtnEnable(mBtnSum, able);
            setBtnEnable(mBtnSum2, able);
            setBtnEnable(mBtnGetProvinces, able);
            setBtnEnable(mBtnGetProvinces2, able);
            setBtnEnable(mBtnGetCities, able);
            setBtnEnable(mBtnGetCities2, able);
            setBtnEnable(mBtnGetLessonTimetable, able);
            setBtnEnable(mBtnGetLessonTimetable2, able);
        } else {
            mBtnSum.setEnabled(able);
            mBtnSum2.setEnabled(able);
            mBtnGetProvinces.setEnabled(able);
            mBtnGetProvinces2.setEnabled(able);
            mBtnGetCities.setEnabled(able);
            mBtnGetCities2.setEnabled(able);
            mBtnGetLessonTimetable.setEnabled(able);
            mBtnGetLessonTimetable2.setEnabled(able);
        }
    }

    private void onBusinessPlatformStateChanged(int connected, List<Object> useless1, Map<String, Object> useless2) {
        if(connected == 0) {
            LogManager.i("Disconnect from BusinessPlatform");
            setBtnEnable(mBtnConnect, true);
            setBtnEnable(mBtnConnect2, true);
            setBtnEnable(mBtnDisConnect, false);
            enableInvoke(false, true);
        } else {
            LogManager.i("Has connected with BusinessPlatform");
            setBtnEnable(mBtnConnect, false);
            setBtnEnable(mBtnConnect2, false);
            setBtnEnable(mBtnDisConnect, true);
            enableInvoke(true, true);
        }
    }
}
