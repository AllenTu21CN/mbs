package cn.sanbu.avalon.media;

import android.util.Log;

public class Transcoder {
    //static {
    //    System.loadLibrary(MediaJni.EP_ANDROID_LIB);

    //}

    // Transcoder callbacks
    public interface Callback {
        //give transcoding process range in [0-100]
        void onProcess(int id, int process);

        //call if transcoding finished
        void onFinish(int id, int result, String message);
    }

    //give transcoding process range in [0-100]
    public void onProcess(int id, int process) {
        Log.i("onProcess", String.valueOf(process));
        if (m_cb == null) {
            Log.e("onProcess", "Callback is null");
            return;
        }
        m_cb.onProcess(id, process);
    }

    //call if transcoding finished
    public void onFinish(int id, int result, String message) {
        Log.i("onFinish", String.valueOf(result) + message);
        if (m_cb == null) {
            Log.e("onFinish", "Callback is null");
            return;
        }
        m_cb.onFinish(id, result, message);
    }

    //if failed  return a negtive value
    //if success return a postive value represent transcoder client's id;
    public int configure(String configuration) {
        Log.i("Configure", "configure transcoder.");
        int id = jniConfigure(configuration);
        if (id < 0) {
            Log.i("Configure", "configure transcoder faild.");
            return -1;
        }

        return id;
    }

    //set callback
    public void setCallback(Callback cb) {
        m_cb = cb;
    }

    //start transcoding
    public int start(int id) {
        return jniStartTranscode(id);
    }

    //cancel transcoding
    public int cancel(int id) {
        return jniCancel(id);
    }

    //release client
    public void release(int id) {
        jniRelease(id);
    }

    //call back for transcoding
    private Callback m_cb = null;
    //Singleton class
    private Transcoder() { jniEnvInit(); }

    private static final Transcoder m_instance = new Transcoder();
    public static final Transcoder getInstance() { return m_instance; }

    //Init jni environment
    private native int jniEnvInit();

    //Configure transcoding task
    private native int jniConfigure(String configuration);

    //Start transcoding
    private native int jniStartTranscode(int id);

    //Cancel transcoding
    private native int jniCancel(int id);

    //Release transcoding client
    private native void jniRelease(int id);
}