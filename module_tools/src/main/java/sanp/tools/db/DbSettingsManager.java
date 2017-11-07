package sanp.tools.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import sanp.tools.utils.DBManager;
import sanp.tools.utils.LogManager;
import sanp.tools.utils.ToolsManager;

/**
 * Created by Tom on 2017/3/10.
 */

public class DbSettingsManager {
    private String TAG = "DbSettingsManager";

    private DBManager dbm;
    private SQLiteDatabase db;
    private Context mContext;

    public DbSettingsManager(Context mContext) {
        this.mContext = mContext;
    }

    public boolean updateInfo(String key, String value) {
        dbm = new DBManager(mContext);
        db = dbm.openDatabase();
        if (db == null) {
            LogManager.e(TAG, "数据库调用失败");
            return false;
        }
        String sql = "update settings set value = '" + value + "' where name = '" + key + "';";
        try {
            db.execSQL(sql);
            db.close();
            return true;
        } catch (SQLiteException e) {
            LogManager.e(TAG, "插入异常" + e.getMessage());
            db.close();
        }
        return false;
    }

    public String selectorInfo(String key, String defultValue) {
        dbm = new DBManager(mContext);
        db = dbm.openDatabase();
        String sql = "select value from settings where name = '" + key + "'";
        Cursor cursor = db.rawQuery(sql, null);
        if (cursor == null) {
            LogManager.e(TAG, "cursor is null");
            insertInfo(key, defultValue);
            return null;
        }
        String value = "";
        while (cursor.moveToNext()) {
            if (cursor.isLast()) {
                try {
                    value = cursor.getString(cursor
                            .getColumnIndex("value"));
                } catch (IllegalStateException exaption) {
                    LogManager.e(TAG, "表中没有此字段");
                    insertInfo(key, defultValue);
                }
            }
        }
        if (ToolsManager.isEmpty(value)) {
            value = defultValue;
            insertInfo(key, defultValue);
        }
        db.close();
        return value;
    }

    public void insertInfo(String key, String defultValue) {
        dbm = new DBManager(mContext);
        db = dbm.openDatabase();
        String sql = "insert into settings (name,value) values ('" + key + "','" + defultValue + "')";
        try {
            db.execSQL(sql);
            db.close();
        } catch (SQLiteException e) {
            LogManager.e(TAG, "插入异常" + e.getMessage());
        }
    }

    public void cleanInfo() {
        dbm = new DBManager(mContext);
        db = dbm.openDatabase();
        String sql = "delete from settings";
        try {
            db.execSQL(sql);
            db.close();
        } catch (SQLiteException e) {
            LogManager.e(TAG, "异常" + e.getMessage());
        }
    }
}
