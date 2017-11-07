package sanp.avalon.libs.base.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import sanp.multimedia.R;

/**
 * 数据库管理类，读取 raw 文件下的数据库文件，使用时，定义好数据库文件，放入该目录即可
 */
public class DBManager {

    private final String FILE_PATH = "/data/mpx.sqlite";
    private String mFilePath;
    private SQLiteDatabase mDatabase;
    private Context mContext;

    public DBManager(Context context) {
        this.mContext = context;
        mFilePath = System.getenv("EXTERNAL_STORAGE") + FILE_PATH;  // 内部存储
    }

    public SQLiteDatabase openDatabase() {
        FileOutputStream fos = null;
        InputStream inputStream = null;
        File file = null;
        try {
            file = new File(mFilePath);
            if (!file.exists()) {
                if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
                inputStream = mContext.getResources().openRawResource(
                        R.raw.mpx);
                if (inputStream == null) {
                    LogManager.e("inputStream null");
                }
                fos = new FileOutputStream(mFilePath);
                if (fos == null) {
                    LogManager.e("fos null");
                }
                byte[] buffer = new byte[400000];
                int count = 0;
                while ((count = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, count);
                }
            }
            mDatabase = SQLiteDatabase.openOrCreateDatabase(mFilePath, null);
            return mDatabase;
        } catch (Exception e) {
            LogManager.e(e.getMessage());
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                   LogManager.e(e);
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                   LogManager.e(e);
                }
            }
        }
        return null;
    }
}