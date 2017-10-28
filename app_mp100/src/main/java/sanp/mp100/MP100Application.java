package sanp.mp100;

import android.app.Application;

import java.util.HashMap;
import java.util.Map;

import sanp.avalon.libs.base.utils.FileSaveUtils;
import sanp.avalon.libs.base.utils.LogManager;
import sanp.mp100.integration.BusinessPlatform;


/**
 * Created by Tuyj on 2017/10/27.
 */

public class MP100Application extends Application {

    public static final String TMP_FILE_PATH = System.getenv("EXTERNAL_STORAGE") + "/MP100/tmp";

    private static final Map<String, Integer> TMP_SETTINGS_FILES = new HashMap<String, Integer>() {{
        put("connection.txt", R.raw.connection);
        put("org.txt", R.raw.org);
    }};

    @Override
    public void onCreate() {
        super.onCreate();

        saveTmpSettingsFilesToStorage();

        BusinessPlatform.getInstance().init(this);
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
}
