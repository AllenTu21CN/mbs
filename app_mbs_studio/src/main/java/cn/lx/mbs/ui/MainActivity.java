package cn.lx.mbs.ui;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;


import com.google.gson.Gson;
import com.sanbu.media.TSLayout;
import com.sanbu.tools.EventPub;
import com.sanbu.tools.LogUtil;
import com.sanbu.tools.PermissionUtil;
import com.sanbu.tools.ToastUtil;

import java.util.Arrays;
import java.util.List;

import cn.lx.mbs.R;
import cn.lx.mbs.LXConst;
import cn.lx.mbs.support.MBS;
import cn.lx.mbs.support.structures.ChannelId;
import cn.lx.mbs.support.structures.CommonOverlay;
import cn.lx.mbs.support.structures.Layout;
import cn.lx.mbs.support.structures.OverlayDst;
import cn.lx.mbs.support.structures.OverlaySrc;
import cn.lx.mbs.support.structures.SurfaceId;
import cn.lx.mbs.ui.model.SceneOverlayDataModel;
import cn.lx.mbs.ui.model.VideoSourcesDataModel;
import cn.lx.mbs.ui.view.AudioMixerArea;
import cn.lx.mbs.ui.view.ControlArea;
import cn.lx.mbs.ui.view.HintDialog;
import cn.lx.mbs.ui.view.OverlayArea;
import cn.lx.mbs.ui.view.PreviewArea;
import cn.lx.mbs.ui.view.ProgramArea;
import cn.lx.mbs.ui.view.SettingsDialog;
import cn.lx.mbs.ui.view.SettingsFragment;
import cn.lx.mbs.ui.view.Utils;
import cn.lx.mbs.ui.view.VideoSourcesArea;
import cn.lx.mbs.ui.view.VideoSourcesManageDialog;
import cn.sanbu.avalon.endpoint3.utils.SysResChecking;
import cn.sanbu.avalon.media.CameraHelper;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    public static final String APP_REQUEST_PERMISSION[] = {
            // network
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.INTERNET,

            // storage
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,

            // multimedia
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            // Manifest.permission.MODIFY_AUDIO_SETTINGS,

            // others
            // Manifest.permission.WAKE_LOCK,

            // Manifest.permission.BROADCAST_STICKY,
            // Manifest.permission.DISABLE_KEYGUARD,
            // Manifest.permission.WRITE_SETTINGS,
            // Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
    };

    public static final int REQUEST_IMAGE_OVERLAY_GET_CONTENT = 1;

    private Handler mHandler;

    private View mTopGroup;
    private PreviewArea mPreviewArea;
    private ProgramArea mProgramArea;
    private ControlArea mControlArea;
    private OverlayArea mOverlayArea;
    private AudioMixerArea mAudioMixerArea;
    private VideoSourcesArea mVideoSourcesArea;

    private VideoSourcesDataModel mVideoSourcesDataModel = new VideoSourcesDataModel();
    private SceneOverlayDataModel[] mSceneOverlayDataModel = new SceneOverlayDataModel[10];
    private List<Layout> mSceneLayouts;

    @Override
    @SuppressLint("SourceLockedOrientationActivity")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtil.w(UIConst.TAG, TAG,"onCreate");
        mHandler = new Handler();

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        hideSystemUI();
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // grant PERMISSION first
        if (!PermissionUtil.isGranted(this, APP_REQUEST_PERMISSION)) {
            PermissionUtil.checkPermissions(this, APP_REQUEST_PERMISSION, granted -> {
                if (granted) {
                    LogUtil.w(UIConst.TAG, TAG, "request permissions succ, reboot application now");
                    mHandler.post(() -> ToastUtil.show("授权成功,请重启", true));
                } else {
                    LogUtil.e(UIConst.TAG, TAG, "request permissions failed: " + new Gson().toJson(APP_REQUEST_PERMISSION));
                    mHandler.post(() -> ToastUtil.show("未授权,无法启动", true));
                }

                mHandler.postDelayed(() -> MyApplication.exit(), 1000);
            });

            // give up init for application
            return;
        }

        generateTestData();
        initViews();
        initEvents();

        checkSysRes(3);
    }

    @Override
    protected void onDestroy() {
        LogUtil.w(UIConst.TAG, TAG, "onDestroy");
        releaseEvents();
        super.onDestroy();
        if (MBS.getInstance().isReady())
            MyApplication.exit();
    }

    @Override
    protected void onStop() {
        LogUtil.w(UIConst.TAG, TAG, "onStop");
        super.onStop();
        //if (MBS.getInstance().isReady()) {
        //    MyApplication.exit();
        //}
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                return true;
            default:
                break;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                new HintDialog(this, Utils.PX(800), Utils.PX(500),
                        "Warning", "Are you sure to exit?\nThat will stop all task automatically",
                        true, () -> {
                    // onConfirmed
                    MyApplication.exit();
                    return true;
                }).show(mTopGroup);
                return true;
            default:
                break;
        }

        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case REQUEST_IMAGE_OVERLAY_GET_CONTENT :
                Uri uri = data.getData();
                if (mOverlayArea != null && mOverlayArea.getAddImageDialog() != null) {
                    mOverlayArea.getAddImageDialog().setExternalImageUri(uri);
                }
                break;

            default :
                // Ignore
                break;
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
        if (!MBS.getInstance().isReady()) {
            ToastUtil.show("尚未启动完成,无法使用", true);
            return;
        }

        SettingsDialog settingsDialog = new SettingsDialog(this, Utils.PX(1500), Utils.PX(930));
        //settingsDialog.setTitle("Settings");
        //settingsDialog.setContent(mSettingsFragment.getListView());

        settingsDialog.showAtLocation(findViewById(R.id.area_control), Gravity.CENTER, 0, 0);
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

    @Deprecated
    public void showSettingsDialog2() {
        SettingsFragment mSettingsFragment = new SettingsFragment();
    }

    public void showVideoSourceManageDialog(VideoSourcesArea parent, int entryId) {
        if (!MBS.getInstance().isReady()) {
            ToastUtil.show("尚未启动完成,无法使用", true);
            return;
        }

        VideoSourcesManageDialog videoSourceManageDialog = new VideoSourcesManageDialog(
                this, Utils.PX(1200), Utils.PX(930), parent, entryId);
        videoSourceManageDialog.showAtLocation(mTopGroup, Gravity.CENTER, 0, 0);
    }

    public void showImageOverlayPickDialog() {
        if (!MBS.getInstance().isReady()) {
            ToastUtil.show("尚未启动完成,无法使用", true);
            return;
        }

        Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhoto , REQUEST_IMAGE_OVERLAY_GET_CONTENT);
    }

    public VideoSourcesDataModel getVideoSourcesDataModel() {
        return mVideoSourcesDataModel;
    }

    public SceneOverlayDataModel getSceneOverlayDataModel(int index) { return mSceneOverlayDataModel[index]; }

    public Layout getSceneLayout(int index) {
        return mSceneLayouts.get(index % 6);
    }

    private void initViews() {
        Utils.init(this);

        mTopGroup = findViewById(R.id.main_layout);

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

        initSurfaceCallbacks();
    }

    private void initEvents() {
    }

    private void releaseEvents() {
        EventPub.getDefaultPub().syncPending();
    }

    private void initSurfaceCallbacks() {
        // preview surfaceView callback
        SurfaceHolderCallback callback = new SurfaceHolderCallback(SurfaceId.PVW);
        SurfaceView view = (SurfaceView) findViewById(R.id.preview_surface_view);
        SurfaceHolder holder = view.getHolder();
        holder.setFormat(PixelFormat.TRANSPARENT);
        holder.addCallback(callback);

        // preview surfaceView callback
        callback = new SurfaceHolderCallback(SurfaceId.PGM);
        view = (SurfaceView) findViewById(R.id.program_surface_view);
        holder = view.getHolder();
        holder.setFormat(PixelFormat.TRANSPARENT);
        holder.addCallback(callback);

        View videoSourceArea = findViewById(R.id.area_video_source);

        // preview surfaceView callback
        callback = new SurfaceHolderCallback(SurfaceId.IN1);
        view = (SurfaceView) videoSourceArea.findViewById(R.id.video_source_1).findViewById(R.id.source_surface_view);
        holder = view.getHolder();
        holder.setFormat(PixelFormat.TRANSPARENT);
        holder.addCallback(callback);

        // preview surfaceView callback
        callback = new SurfaceHolderCallback(SurfaceId.IN2);
        view = (SurfaceView) videoSourceArea.findViewById(R.id.video_source_2).findViewById(R.id.source_surface_view);
        holder = view.getHolder();
        holder.setFormat(PixelFormat.TRANSPARENT);
        holder.addCallback(callback);

        // preview surfaceView callback
        callback = new SurfaceHolderCallback(SurfaceId.IN3);
        view = (SurfaceView) videoSourceArea.findViewById(R.id.video_source_3).findViewById(R.id.source_surface_view);
        holder = view.getHolder();
        holder.setFormat(PixelFormat.TRANSPARENT);
        holder.addCallback(callback);

        // preview surfaceView callback
        callback = new SurfaceHolderCallback(SurfaceId.IN4);
        view = (SurfaceView) videoSourceArea.findViewById(R.id.video_source_4).findViewById(R.id.source_surface_view);
        holder = view.getHolder();
        holder.setFormat(PixelFormat.TRANSPARENT);
        holder.addCallback(callback);
    }

    private void checkSysRes(int retryCount) {
        String[] cameraIds = CameraHelper.getInstance().getCameraIdList();

        SysResChecking.check(MyApplication.getContext(), mHandler, result -> {
            if (!result.isSuccessful()) {
                if (retryCount == 0) {
                    ToastUtil.show("应用启动失败, " + result.getMessage(), true);
                    mHandler.postDelayed(() -> MyApplication.exit(), 1000);
                } else {
                    ToastUtil.show(result.getMessage() + ", 请稍等", true);
                    LogUtil.w(UIConst.TAG, TAG, "system resource is in using, try it later");
                    mHandler.postDelayed(() -> checkSysRes(retryCount - 1), 1000);
                }
                return;
            }

            LogUtil.i(UIConst.TAG, TAG, "mbs system resource is ready");
            MBS.getInstance().startEP(MyApplication.getContext());

            mHandler.post(() -> doTest());

        }, LXConst.REQUIRED_PORTS, Arrays.asList(cameraIds));
    }

    private void doTest() {
        MBS.getInstance().removeSource(0, null);
        MBS.getInstance().removeSource(1, null);
        MBS.getInstance().removeSource(2, null);
        MBS.getInstance().removeSource(3, null);
    }

    private void generateTestData() {
        // TEST ////////////////////////////////////////////////////////////////////////////////////
        for (int i = 1; i <= 5; i++) {
            VideoSourcesDataModel.VideoSourceConfig s = new VideoSourcesDataModel.VideoSourceConfig();
            s.alias = String.format("Test Source %d", i);
            s.type = i;
            s.localCameraConfig.cameraId = "1";
            s.localCameraConfig.captureWidth = 1280;
            s.localCameraConfig.captureHeight = 720;

            s.remoteCameraConfig.host = "192.168.0.9";
            s.remoteCameraConfig.port = 5555;

            s.rtspConfig.url = "rtsp://10.1.11.161:554/ch3";
            s.rtspConfig.useTcp = true;

            s.rtmpConfig.url = "8.8.8.8:1935/live/a";

            s.fileConfig.path = LXConst.TEST_MP4;
            s.fileConfig.loop = true;

            mVideoSourcesDataModel.add(s);
        }

        String json = mVideoSourcesDataModel.toJson();
        LogUtil.i(UIConst.TAG, TAG, json);

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

        Layout layout1 = new Layout().addOverlays(TSLayout.AB, Arrays.asList(
                OverlaySrc.buildImage(LXConst.BG_IMAGE_LOADING),
                OverlaySrc.buildImage(LXConst.BG_IMAGE_LOADING)
        ));

        Layout layout2 = new Layout().addOverlays(TSLayout.AB, Arrays.asList(
                OverlaySrc.buildStream(ChannelId.IN1),
                OverlaySrc.buildImage(LXConst.BG_IMAGE_LOADING)
        ));

        Layout layout3 = new Layout().addOverlays(TSLayout.AB, Arrays.asList(
                OverlaySrc.buildStream(ChannelId.IN1),
                OverlaySrc.buildStream(ChannelId.IN2)
        ));

        Layout layout4 = new Layout().addOverlays(TSLayout.AB_ZYTX, Arrays.asList(
                OverlaySrc.buildStream(ChannelId.IN1),
                OverlaySrc.buildEmpty()
        ));

        Layout layout5 = new Layout().addOverlays(TSLayout.AB_ZYTX, Arrays.asList(
                OverlaySrc.buildStream(ChannelId.IN1),
                OverlaySrc.buildStream(ChannelId.IN2)
        ));

        Layout layout6 = new Layout().addOverlays(TSLayout.AB_ZYTX, Arrays.asList(
                OverlaySrc.buildStream(ChannelId.IN1),
                OverlaySrc.buildStream(ChannelId.IN2)
        )).addCommonOverlays(Arrays.asList(
                new CommonOverlay(OverlaySrc.buildImage(LXConst.BG_IMAGE_LOADING),
                        new OverlayDst(TSLayout.AB_IN_RT.regions.get(1))),
                new CommonOverlay(OverlaySrc.buildStream(ChannelId.IN2),
                        new OverlayDst(TSLayout.AB_IN_RB.regions.get(1)))));

        mSceneLayouts = Arrays.asList(layout1, layout2, layout3, layout4, layout5, layout6);

        // TEST ENDS ///////////////////////////////////////////////////////////////////////////////
    }

    class SurfaceHolderCallback implements SurfaceHolder.Callback {

        private SurfaceId mId;

        private SurfaceHolderCallback(SurfaceId id) {
            mId = id;
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            LogUtil.i(UIConst.TAG, TAG, mId.name() + " surfaceCreated");
            MBS.getInstance().onSurfaceCreated(mId, holder.getSurface());
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            LogUtil.i(UIConst.TAG, TAG, mId.name() + " surfaceChanged");
            MBS.getInstance().onSurfaceChanged(mId, format, width, height);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            LogUtil.i(UIConst.TAG, TAG, mId.name() + " surfaceDestroyed");
            MBS.getInstance().onSurfaceDestroyed(mId);
        }
    }
}
