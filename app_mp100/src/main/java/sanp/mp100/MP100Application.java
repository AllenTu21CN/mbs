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

import sanp.avalon.libs.base.utils.FileSaveUtils;
import sanp.avalon.libs.base.utils.LogManager;
import sanp.mp100.integration.BusinessPlatform;


/**
 * Created by Tuyj on 2017/10/27.
 */

public class MP100Application extends Application {

    public static final String TMP_FILE_PREFIX = "MP100/tmp";
    public static final String EXTERNAL_STORAGE_DIRECTORY = Environment.getExternalStorageDirectory() + "/";
    public static final String TMP_FILE_PATH = EXTERNAL_STORAGE_DIRECTORY  + TMP_FILE_PREFIX;

    private static final Map<String, Integer> TMP_SETTINGS_FILES = new HashMap<String, Integer>() {{
        put("connection.json", R.raw.connection);
        put("org.json", R.raw.org);
        put("sources.json", R.raw.sources);
        put("scenes.json", R.raw.scenes);
        put("output_formats.json", R.raw.output_formats);
    }};

    private static final Map<String, Integer> VIDEO_TEST_FILES = new HashMap<String, Integer>() {{
        put("test_1080p_24fps_2mbps_32secs.mp4", R.raw.test_1080p_24fps_2mbps_32secs);
        put("test_720p_24fps_1mbps_17secs.mp4", R.raw.test_720p_24fps_1mbps_17secs);
    }};

    @Override
    public void onCreate() {
        super.onCreate();

        saveTmpSettingsFilesToStorage();
        saveMp4ToStorage();

        BusinessPlatform.getInstance().init(this);
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

    private void saveTmpSettingsFilesToStorage() {
        for(String filename: TMP_SETTINGS_FILES.keySet()) {
            try {
                FileSaveUtils.saveToSDCard(this, TMP_FILE_PATH, filename, TMP_SETTINGS_FILES.get(filename));
            } catch (Throwable e) {
                LogManager.e("saveTmpSettingsFilesToStorage error " + e);
            }
        }
    }

    private void saveMp4ToStorage() {
        for(String filename: VIDEO_TEST_FILES.keySet()) {
            try {
                FileSaveUtils.saveToSDCard(this, TMP_FILE_PATH, filename, VIDEO_TEST_FILES.get(filename));
            } catch (Throwable e) {
                LogManager.e("saveMp4ToStorage error " + e);
            }
        }
    }
}
