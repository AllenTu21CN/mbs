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

import io.crossbar.autobahn.wamp.types.CallResult;

import sanp.avalon.libs.base.utils.LogManager;
import sanp.mp100.R;
import sanp.mp100.integration.BusinessPlatform;
import sanp.mp100.integration.BusinessPlatformPostman;

public class BusinessPlatformTestActivity extends AppCompatActivity implements View.OnClickListener, BusinessPlatform.Observer {

    //    private static final String websocketURL = "ws://10.1.0.75:8080";
    private static final String websocketURL1 = "ws://192.168.1.109:8080";
    private static final String realm1 = "device-hub.samples";
    private static final String websocketURL2 = "ws://lbblscy.3322.org:8085/device-hub";
    private static final String realm2 = "ebp.daemon";

    private BusinessPlatform mBusinessPlatform;

    private TextView mMsgText;
    private String mMsg = "";

    private Button mBtnConnect;
    private Button mBtnDisConnect;

    private Button mBtnGetProvinces;
    private Button mBtnGetProvinces2;
    private Button mBtnGetLessonTimetable;
    private Button mBtnGetLessonTimetable2;

    private Handler mHandler = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_business_platform_test);

        mBusinessPlatform = BusinessPlatform.getInstance();
        mBusinessPlatform.addStateObserver(this);

        mMsgText = (TextView) findViewById(R.id.text_result);
        mMsgText.setText(mMsg);

        findViewById(R.id.button_clean_msg).setOnClickListener(this);

        mBtnGetProvinces = (Button) findViewById(R.id.btn_getProvinces);
        mBtnGetProvinces.setOnClickListener(this);

        mBtnGetProvinces2 = (Button) findViewById(R.id.btn_getProvinces2);
        mBtnGetProvinces2.setOnClickListener(this);

        mBtnGetLessonTimetable = (Button) findViewById(R.id.btn_getLessonTimetable);
        mBtnGetLessonTimetable.setOnClickListener(this);

        mBtnGetLessonTimetable2 = (Button) findViewById(R.id.btn_getLessonTimetable2);
        mBtnGetLessonTimetable2.setOnClickListener(this);

        mBtnConnect = (Button) findViewById(R.id.btn_connect);
        mBtnConnect.setOnClickListener(this);

        mBtnDisConnect = (Button) findViewById(R.id.btn_disconnect);
        mBtnDisConnect.setOnClickListener(this);

        BusinessPlatformPostman.State state = mBusinessPlatform.connectingState();
        if(state == BusinessPlatformPostman.State.NONE) {
            mBtnConnect.setEnabled(true);
            mBtnDisConnect.setEnabled(false);
            enableInvoke(false, false);
        } else if(state == BusinessPlatformPostman.State.READY) {
            mBtnConnect.setEnabled(false);
            mBtnDisConnect.setEnabled(true);
            enableInvoke(true, false);
        } else {
            mBtnConnect.setEnabled(false);
            mBtnDisConnect.setEnabled(false);
            enableInvoke(false, false);
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
            case R.id.btn_disconnect:
                disconnectRemote();
                break;
            case R.id.btn_getProvinces:
                invokeFuncGetProvinces(mBtnGetProvinces);
                break;
            case R.id.btn_getProvinces2:
                invokeFuncGetProvinces2(mBtnGetProvinces2);
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

        // finally, provide everything to a Client instance and connect
        LogManager.e("call connect");
        BusinessPlatform.ConnectionSettings connectionSettings = new BusinessPlatform.ConnectionSettings();
        connectionSettings.WebSocketURL = websocketURL2;
        connectionSettings.Realm = realm2;
        int ret = mBusinessPlatform.connect(connectionSettings);
        LogManager.e("after connect: " + ret);

        if(ret != 0)
            mBtnConnect.setEnabled(true);
    }

    private void disconnectRemote() {
        mBtnDisConnect.setEnabled(false);

        LogManager.e("call disconnect");
        int ret = mBusinessPlatform.disconnect();
        LogManager.e("after disconnect" );

        if(ret != 0)
            mBtnDisConnect.setEnabled(true);
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
            setBtnEnable(mBtnGetProvinces, able);
            setBtnEnable(mBtnGetProvinces2, able);
            setBtnEnable(mBtnGetLessonTimetable, able);
            setBtnEnable(mBtnGetLessonTimetable2, able);
        } else {
            mBtnGetProvinces.setEnabled(able);
            mBtnGetProvinces2.setEnabled(able);
            mBtnGetLessonTimetable.setEnabled(able);
            mBtnGetLessonTimetable2.setEnabled(able);
        }
    }

    @Override
    public void onConnectingState(boolean connected) {
        if(connected) {
            LogManager.i("Has connected with BusinessPlatform");
            setBtnEnable(mBtnConnect, false);
            setBtnEnable(mBtnDisConnect, true);
            enableInvoke(true, true);
        } else {
            LogManager.i("Disconnect from BusinessPlatform");
            setBtnEnable(mBtnConnect, true);
            setBtnEnable(mBtnDisConnect, false);
            enableInvoke(false, true);
        }
    }

    @Override
    public void onActivatingState(boolean activated) {

    }

    @Override
    public void onBindingState(boolean bound) {

    }
}
