package cn.lx.mbs.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;


import java.util.LinkedList;
import java.util.List;

import cn.lx.mbs.R;
import cn.lx.mbs.ui.model.SceneOverlayDataModel;
import cn.lx.mbs.ui.model.VideoSourcesDataModel;
import cn.lx.mbs.ui.view.AudioMixerArea;
import cn.lx.mbs.ui.view.ControlArea;
import cn.lx.mbs.ui.view.BaseDialog;
import cn.lx.mbs.ui.view.OverlayArea;
import cn.lx.mbs.ui.view.PreviewArea;
import cn.lx.mbs.ui.view.ProgramArea;
import cn.lx.mbs.ui.view.SettingsDialog;
import cn.lx.mbs.ui.view.SettingsFragment;
import cn.lx.mbs.ui.view.Utils;
import cn.lx.mbs.ui.view.VideoSourcesArea;
import cn.lx.mbs.ui.view.VideoSourcesManageDialog;
import cn.sanbu.avalon.media.CameraHelper;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int PERMISSION_REQUEST_CODE = 1;

    private PreviewArea mPreviewArea;
    private ProgramArea mProgramArea;
    private ControlArea mControlArea;
    private OverlayArea mOverlayArea;
    private AudioMixerArea mAudioMixerArea;
    private VideoSourcesArea mVideoSourcesArea;
    private SettingsFragment mSettingsFragment;
    private SettingsDialog mSettingsDialog;
    private BaseDialog mVideoSourceManageDialog;

    private VideoSourcesDataModel mVideoSourcesDataModel = new VideoSourcesDataModel();
    private SceneOverlayDataModel[] mSceneOverlayDataModel = new SceneOverlayDataModel[10];

    public VideoSourcesDataModel getVideoSourcesDataModel() {
        return mVideoSourcesDataModel;
    }

    public SceneOverlayDataModel getSceneOverlayDataModel(int index) { return mSceneOverlayDataModel[index]; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkPermissions();

        keepScreenOn();

        generateTestData();

        String[] cameraIdList = CameraHelper.getInstance().getCameraIdList();
        for (String id : cameraIdList) {
            CameraCharacteristics info = CameraHelper.getInstance().getCameraCharacteristics(id);
            Log.i(TAG, "Camera ID=" + id + ", Characteristics=" + info.toString());
        }

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        hideSystemUI();

        setContentView(R.layout.activity_main);

        Utils.init(this);

        mPreviewArea = new PreviewArea(this);
        mPreviewArea.init();

        mProgramArea = new ProgramArea(this);
        mProgramArea.init();

        mControlArea = new ControlArea(this);
        mControlArea.init();

        mOverlayArea = new OverlayArea(this);
        mOverlayArea.init();

        mAudioMixerArea = new AudioMixerArea(this);
        mAudioMixerArea.init();

        mVideoSourcesArea = new VideoSourcesArea(this);
        mVideoSourcesArea.init();

        if (savedInstanceState == null) {
            mSettingsFragment = new SettingsFragment();
        }

        LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);

        mSettingsDialog = new SettingsDialog(this, Utils.PX(1500), Utils.PX(930));
        //mSettingsDialog.setTitle("Settings");
        //mSettingsDialog.setContent(mSettingsFragment.getListView());

        mVideoSourceManageDialog = new VideoSourcesManageDialog(this, Utils.PX(1200), Utils.PX(930));
    }

    public void checkPermissions() {
        String[] requiredPermissions = new String[] {
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE };

        List<String> ungrantedPermissions = new LinkedList<>();
        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ungrantedPermissions.add(permission);
            }
        }

        if (ungrantedPermissions.size() > 0) {
            ActivityCompat.requestPermissions(this,
                    ungrantedPermissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }

                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    private void keepScreenOn() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    public void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    public void showSettingsDialog() {
        mSettingsDialog.showAtLocation(findViewById(R.id.area_control), Gravity.CENTER, 0, 0);
        // inflate the layout of the popup window
        /*LayoutInflater inflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.base_dialog_wrapper, null);

        PopupWindow win = new PopupWindow(Utils.PX(1200), Utils.PX(900));
        win.setElevation(10);
        // Closes the popup window when touch outside.
        win.setOutsideTouchable(true);
        win.setFocusable(true);
        // Removes default background.
        win.setBackgroundDrawable(getDrawable(R.drawable.common_area_bg));
        win.setContentView(popupView);
        win.showAtLocation(findViewById(R.id.area_control), Gravity.CENTER, 0, 0);*/

        /*FrameLayout fl = findViewById(R.id.settings_container);
        fl.setVisibility(View.VISIBLE);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.settings_container, mSettingsFragment);
        ft.commit();*/
    }

    public void showVideoSourceManageDialog() {
        mVideoSourceManageDialog.showAtLocation(findViewById(R.id.main_layout), Gravity.CENTER, 0, 0);
    }

    public void showPickImageDialog() {
        Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhoto , 1);//one can be replaced with any action code
    }

    public void generateTestData() {
        // TEST ////////////////////////////////////////////////////////////////////////////////////
        for (int i = 1; i <= 5; i++) {
            VideoSourcesDataModel.VideoSourceConfig s = new VideoSourcesDataModel.VideoSourceConfig();
            s.alias = String.format("Test Source %d", i);
            s.type = i;
            s.localCameraConfig.cameraId = "1";
            s.localCameraConfig.captureWidth = 352;
            s.localCameraConfig.captureHeight = 288;

            s.remoteCameraConfig.host = "192.168.0.9";
            s.remoteCameraConfig.port = 5555;

            s.rtspConfig.url = "10.1.36.111:5000/ch2";
            s.rtspConfig.useTcp = true;

            s.rtmpConfig.url = "8.8.8.8:1935/live/a";

            s.fileConfig.path = "/sdcard/video/clip-1.mp4";
            s.fileConfig.loop = true;

            mVideoSourcesDataModel.add(s);
        }

        String json = mVideoSourcesDataModel.toJson();
        Log.i("MainActivity", json);

        mSceneOverlayDataModel[0] = new SceneOverlayDataModel();

        for (int i = 0; i < 5; i++) {
            SceneOverlayDataModel.Overlay videoOverlay = new SceneOverlayDataModel.VideoOverlay();
            videoOverlay.name = "MAIN CAMERA";
            mSceneOverlayDataModel[0].add(videoOverlay);

            SceneOverlayDataModel.Overlay imageOverlay = new SceneOverlayDataModel.ImageOverlay();
            imageOverlay.name = "LOGO";
            mSceneOverlayDataModel[0].add(imageOverlay);

            SceneOverlayDataModel.Overlay textOverlay = new SceneOverlayDataModel.TextOverlay();
            textOverlay.name = "Lower thirds";
            mSceneOverlayDataModel[0].add(textOverlay);
        }


        // TEST ENDS ///////////////////////////////////////////////////////////////////////////////
    }
}
