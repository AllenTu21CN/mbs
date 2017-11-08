package sanp.tools.utils;

import android.database.sqlite.SQLiteDatabase;
import java.io.File;

public class DBManager {

    private static Object gLock = new Object();
    private static SQLiteDatabase mSQLiteDatabase = null;

    public static SQLiteDatabase initAppDatabase(String dbFilePath) {
        synchronized (gLock) {
            if(mSQLiteDatabase == null) {
                File file = new File(dbFilePath);
                if (!file.exists()) {
                    LogManager.e("can't find the db file: " + dbFilePath);
                    return null;
                }
                mSQLiteDatabase = SQLiteDatabase.openOrCreateDatabase(dbFilePath, null);
            }
            return mSQLiteDatabase;
        }
    }

    public static SQLiteDatabase getAppDatabase() {
        return mSQLiteDatabase;
    }
}