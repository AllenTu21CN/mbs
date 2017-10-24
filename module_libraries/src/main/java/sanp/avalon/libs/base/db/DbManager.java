package sanp.avalon.libs.base.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sanp.avalon.libs.base.utils.LogManager;

/**
 * Created by zhangxd on 2017/10/12.
 */

public class DbManager {

    private static final String TAG = "DbManager";

    private DbHelper mDbHelper;

    private static DbManager instance;

    private static Context mContext;


    public static DbManager getInstance() {
        if (instance == null) {
            instance = new DbManager(mContext);
        }
        return instance;
    }

    public static void setContext(Context context) {
        mContext = context;
    }

    public DbManager(Context context) {
        mDbHelper = new DbHelper(context, DbHelper.DATABASE_NAME, null, DbHelper.VERSION);

    }

    public void insert(String value, String name, String date) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("name", name);
        contentValues.put("value", value);
        contentValues.put("date", date);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        try {
            db.insert(DbHelper.TABLE_NAME, null, contentValues);
            db.close();
        } catch (SQLiteException e) {
            LogManager.e(TAG, e);
            db.close();
        }

    }

    public void delete(int id) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        try {
            db.delete(DbHelper.TABLE_NAME, "id=?", new String[]{String.valueOf(id)});
            db.close();
        } catch (SQLiteException e) {
            LogManager.e(TAG, e);
            db.close();
        }
    }

    public List queryHistroyAll() {
        List<Map<String, String>> mList = new ArrayList<>();
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        Cursor cursor = db.query(DbHelper.TABLE_NAME, null, null, null, null, null, "id desc");
        while (cursor.moveToNext()) {
            Map<String, String> mHistroyMap = new HashMap<>();
            int id = cursor.getInt(0);
            String value = cursor.getString(1);
            String name = cursor.getString(2);
            String date = cursor.getString(3);
            mHistroyMap.put("id",String.valueOf(id));
            mHistroyMap.put("value", value);
            mHistroyMap.put("name", name);
            mHistroyMap.put("date", date);
            mList.add(mHistroyMap);
        }
        try {
            cursor.close();
            db.close();
        } catch (SQLiteException e) {
            cursor.close();
            db.close();
            LogManager.e(TAG, e);
        }
        return mList;
    }

}
