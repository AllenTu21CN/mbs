package sanp.mp100;

import android.app.Application;
import android.os.Environment;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import sanp.InitMultimedia;
import sanp.tools.utils.FileSaveUtils;
import sanp.tools.utils.LogManager;

/**
 * Created by Tuyj on 2017/10/27.
 */

public class MP100Application extends Application {

    public static final String EXTERNAL_STORAGE_DIRECTORY = Environment.getExternalStorageDirectory() + "/";    // "/sdcard/"
    public static String APP_NAME;
    public static String HOME_EXTERNAL_PATH;
    public static String TMP_FILE_PREFIX;
    public static String TMP_FILE_PATH;

    private static final Map<String, Integer> RES_BG_FILES = new HashMap<String, Integer>() {{
        put("video_bg.jpg", R.raw.video_bg);
        put("overlapping_1.jpg", R.raw.overlapping_1);
        put("overlapping_2.jpg", R.raw.overlapping_2);
        put("overlapping_3.jpg", R.raw.overlapping_3);
        put("overlapping_4.jpg", R.raw.overlapping_4);
        put("symmetrical_2.jpg", R.raw.symmetrical_2);
        put("asymmetric_3.jpg", R.raw.asymmetric_3);
    }};

    private static final Map<String, Integer> TMP_SETTINGS_FILES = new HashMap<String, Integer>() {{
        put("connection.json", R.raw.connection);
        put("org.json", R.raw.org);
        put("sources.json", R.raw.sources);
        put("scenes.json", R.raw.scenes);
        put("output_formats.json", R.raw.output_formats);
        put("rtmp_config.json", R.raw.rtmp_config);
    }};

    @Override
    public void onCreate() {
        super.onCreate();
        initPath();
        saveResBgPicturesToStorage();
        saveTmpSettingsFilesToStorage();
        InitMultimedia.init(this, HOME_EXTERNAL_PATH);
    }

    private void initPath() {
        APP_NAME = this.getResources().getString(R.string.app_name);    // "MP100"
        HOME_EXTERNAL_PATH = EXTERNAL_STORAGE_DIRECTORY  + APP_NAME;        // "/sdcard/MP100"
        TMP_FILE_PREFIX = APP_NAME + "/tmp";                                // "MP100/tmp"
        TMP_FILE_PATH = EXTERNAL_STORAGE_DIRECTORY  + TMP_FILE_PREFIX;      // "/sdcard/MP100/tmp"
    }

    private void saveResBgPicturesToStorage() {
        LogManager.i("save bg pictures to storage: " + HOME_EXTERNAL_PATH);
        for(String filename: RES_BG_FILES.keySet()) {
            try {
                FileSaveUtils.saveToSDCard(this, HOME_EXTERNAL_PATH, filename, RES_BG_FILES.get(filename));
            } catch (Throwable e) {
                LogManager.e("saveResBgPicturesToStorage error " + e);
            }
        }
    }

    private void saveTmpSettingsFilesToStorage() {
        LogManager.i("save temp files to storage: " + TMP_FILE_PATH);
        for(String filename: TMP_SETTINGS_FILES.keySet()) {
            try {
                FileSaveUtils.saveToSDCard(this, TMP_FILE_PATH, filename, TMP_SETTINGS_FILES.get(filename));
            } catch (Throwable e) {
                LogManager.e("saveTmpSettingsFilesToStorage error " + e);
            }
        }
    }

    public static <T> T loadSettingsFromTmpFile(String filename, Class<T> classOf) {
        String tmpFile = TMP_FILE_PATH + "/" + filename;
        LogManager.w(String.format("TODO: will read tmp file[%s] which is copy from Monica\\app_mp100\\src\\main\\res\\raw", tmpFile));
        try {
            JsonReader reader = new JsonReader(new FileReader(tmpFile));
            return new Gson().fromJson(reader, classOf);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("not found " + tmpFile);
        }
    }

    public static <T> T loadSettingsFromTmpFile(String filename, Type type) {
        String tmpFile = TMP_FILE_PATH + "/" + filename;
        LogManager.w(String.format("TODO: will read tmp file[%s] which is copy from Monica\\app_mp100\\src\\main\\res\\raw", tmpFile));
        try {
            JsonReader reader = new JsonReader(new FileReader(tmpFile));
            return new Gson().fromJson(reader, type);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("not found " + tmpFile);
        }
    }
}
