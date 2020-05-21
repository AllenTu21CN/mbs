package cn.sanbu.avalon.media;

import android.graphics.SurfaceTexture;


public class OutputSurfaceTexture {
    private String TAG = "OutputSurfaceTexture";
    public OutputSurfaceTexture(int texName) {
        mSurfaceTexture = new SurfaceTexture(texName);
        mTextureID = texName;
        setOnFrameAvailableListener();
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    private void setOnFrameAvailableListener() {
        mSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                synchronized (mUpdateSurfaceLock) {
                    mUpdateSurface = true;
                }
            }
        });
    }

    public void updateTexture() {
//                mSurfaceTexture.updateTexImage();

        synchronized (mUpdateSurfaceLock) {
            if (mUpdateSurface) {
//                Log.i(TAG, "Update surface texture : " + mTextureID);
                // TODO: 保证与draw的同步
                mSurfaceTexture.updateTexImage();
                mUpdateSurface = false;
            }
        }
    }

    private SurfaceTexture mSurfaceTexture;
    private Object mUpdateSurfaceLock = new Object();
    private boolean mUpdateSurface;
    private int mTextureID;
}
