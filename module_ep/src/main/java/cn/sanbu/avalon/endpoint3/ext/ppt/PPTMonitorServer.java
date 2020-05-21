package cn.sanbu.avalon.endpoint3.ext.ppt;

import com.sanbu.tools.LogUtil;
import com.sanbu.tools.StringUtil;

import cn.sanbu.avalon.endpoint3.EPConst;
import cn.sanbu.avalon.endpoint3.structures.TransProtocol;
import cn.sanbu.avalon.endpoint3.ext.ExtEvent;
import cn.sanbu.avalon.endpoint3.ext.ExtJni;

public class PPTMonitorServer {

    private static final String TAG = PPTMonitorServer.class.getSimpleName();

    public interface EventListener {
        void onPPTEvent(ExtEvent event);
    }

    // handler
    private int mJniObject = -1;
    private EventListener mListener;

    public PPTMonitorServer() {

    }

    public int init(int listenPort, TransProtocol protocol, EventListener listener) {
        if (!ExtJni.isInited())
            throw new RuntimeException("call ExtJni.initEnv first");
        if (mJniObject >= 0)
            return 0;

        mJniObject = jniCreateServer(listenPort, protocol == TransProtocol.UDP);
        if (mJniObject < 0)
            return mJniObject;

        mListener = listener;
        return 0;
    }

    public void release() {
        if (mJniObject >= 0) {
            jniReleaseServer(mJniObject);
            mJniObject = -1;
        }
        mListener = null;
    }

    ///////////////// callback for JNI

    public void onConnecting(int id, String ip, int port, int error) {
        LogUtil.i(EPConst.TAG, TAG, String.format("onConnecting: id[%d] ip[%s] port[%d] error[%d]", id, ip, port, error));
        if (id != mJniObject) {
            LogUtil.d(EPConst.TAG, TAG, "ignore expired onConnecting");
            return;
        }
    }

    public void onPPTEvent(int id, byte[] data) {
        if (id != mJniObject) {
            LogUtil.v(EPConst.TAG, TAG, "ignore expired onPPTEvent");
            return;
        }

        ExtEvent event = ExtEvent.fromBin(2, data, 0, data.length);
        if (event == null) {
            LogUtil.i(EPConst.TAG, TAG, "onPPTEvent, unknown data: [" + StringUtil.bytesToHexString(data, 0, 10, " ") + "]");
            return;
        } else if (LogUtil.isEnabledVerbose()) {
            LogUtil.v(EPConst.TAG, TAG, "onPPTEvent: [" + StringUtil.bytesToHexString(data, 0, 10, " ") + "]");
        }

        LogUtil.v(EPConst.TAG, TAG, "onPPTEvent: " + event.name());
        if (mListener != null)
            mListener.onPPTEvent(event);
    }

    ///////////////// JNI functions

    private native int jniCreateServer(int port, boolean is_udp);

    private native void jniReleaseServer(int object_id);
}
