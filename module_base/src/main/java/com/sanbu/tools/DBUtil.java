package com.sanbu.tools;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DBUtil {

    private static final String TAG = DBUtil.class.getSimpleName();

    public interface TableHelper {
        void setDbUtil(DBUtil dbUtil);

        void onCreate(SQLiteDatabase db);

        void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion);
    }

    private static Context gContext;

    public static void init(Context context) {
        gContext = context;
    }

    private DbHelper mDbHelper;
    private List<TableHelper> mTableHelpers = new ArrayList<>();

    public DBUtil(String dbName, int dbVersion, List<Class> tableClasses) {
        if (gContext == null)
            throw new RuntimeException("call DBUtil.init first");

        try {
            for (Class clazz : tableClasses) {
                if (!TableHelper.class.isAssignableFrom(clazz))
                    throw new RuntimeException("clazzOfTable is not valid: " + clazz);

                TableHelper table = (TableHelper) clazz.newInstance();
                table.setDbUtil(this);
                mTableHelpers.add(table);
            }
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("create from clazzOfTable failed", e);
        }

        mDbHelper = new DbHelper(gContext, dbName, dbVersion);
    }

    public void release() {
        if (mDbHelper != null) {
            mDbHelper.close();
            mDbHelper = null;
        }

        synchronized (mTableHelpers) {
            mTableHelpers.clear();
        }
    }

    public <T> T getTable(Class<T> clazzOfTable) {
        if (!TableHelper.class.isAssignableFrom(clazzOfTable)) {
            LogUtil.w(TAG, "clazzOfTable(" + clazzOfTable + ") is not valid");
            return null;
        }

        synchronized (mTableHelpers) {
            for (TableHelper table : mTableHelpers) {
                if (table.getClass() == clazzOfTable) {
                    return (T) table;
                }
            }
            return null;
        }
    }

    public SQLiteDatabase getRawDB() {
        return mDbHelper.getWritableDatabase();
    }

    public Throwable insert(String tableName, ContentValues values) {
        SQLiteDatabase db = null;
        try {
            db = mDbHelper.getWritableDatabase();
            db.insertOrThrow(tableName, null, values);
            return null;
        } catch (Exception e) {
            return e;
        } finally {
            if (db != null)
                db.close();
        }
    }

    public Throwable delete(String tableName, String whereClause, String[] whereArgs) {
        SQLiteDatabase db = null;
        try {
            db = mDbHelper.getWritableDatabase();
            db.delete(tableName, whereClause, whereArgs);
            return null;
        } catch (Exception e) {
            return e;
        } finally {
            if (db != null)
                db.close();
        }
    }

    public Throwable clearTable(String tableName) {
        SQLiteDatabase db = null;
        try {
            db = mDbHelper.getWritableDatabase();
            db.delete(tableName, null, null);
            return null;
        } catch (Exception e) {
            return e;
        } finally {
            if (db != null)
                db.close();
        }
    }

    public Throwable update(String tableName, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = null;
        try {
            db = mDbHelper.getWritableDatabase();
            db.update(tableName, values, selection, selectionArgs);
            return null;
        } catch (Exception e) {
            return e;
        } finally {
            if (db != null)
                db.close();
        }
    }

    @Deprecated
    public ContentValues queryTableItem(String tableName, String selection, String[] selectionArgs, String orderBy) {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor cursor = db.query(tableName, null, selection, selectionArgs, null, null, orderBy);
        ContentValues values = new ContentValues();
        try {
            while (cursor.moveToNext()) {
                for (int i = 0; i < cursor.getColumnCount(); ++i) {
                    values.put(cursor.getColumnName(i), cursor.getString(i));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        cursor.close();
        db.close();
        return values;
    }

    public List<ContentValues> enumTable(String tableName, String orderBy) {
        return enumTable(tableName, orderBy, -1);
    }

    public List<ContentValues> enumTable(String tableName, String orderBy, int maxCount) {
        return queryTable(tableName, null, null, orderBy, maxCount);
    }

    public List<ContentValues> queryTable(String tableName, String selection, String[] selectionArgs, String orderBy) {
        return queryTable(tableName, selection, selectionArgs, orderBy, -1);
    }

    public List<ContentValues> queryTable(String tableName, String selection, String[] selectionArgs, String orderBy, int maxCount) {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        List<ContentValues> values = queryTable(db, tableName, selection, selectionArgs, orderBy, maxCount);
        db.close();
        return values;
    }

    public int countTableItem(String tableName, String field) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = mDbHelper.getReadableDatabase();
            cursor = db.rawQuery(String.format("select count(%s) from %s", field, tableName),null);
            if (cursor.moveToFirst())
                return cursor.getInt(0);
            else
                return -1;
        } catch (Exception e) {
            LogUtil.w(TAG, "countTableItem error", e);
            return -1;
        } finally {
            if (cursor != null)
                cursor.close();
            if (db != null)
                db.close();
        }
    }

    public int countTableItem(String tableName, String field, String... values) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = mDbHelper.getReadableDatabase();
            cursor = db.rawQuery(String.format("select count(%s) from %s where %s in (%s)",
                    field, tableName, field, StringUtil.join(",", values)),null);
            if (cursor.moveToFirst())
                return cursor.getInt(0);
            else
                return -1;
        } catch (Exception e) {
            LogUtil.w(TAG, "countTableItem error", e);
            return -1;
        } finally {
            if (cursor != null)
                cursor.close();
            if (db != null)
                db.close();
        }
    }

    public ContentValues getFirstMatch(String sql, String[] selectionArgs) {
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = mDbHelper.getReadableDatabase();
            cursor = db.rawQuery(sql, selectionArgs);

            if (cursor.moveToFirst()) {
                ContentValues values = new ContentValues();
                for (int i = 0; i < cursor.getColumnCount(); ++i) {
                    values.put(cursor.getColumnName(i), cursor.getString(i));
                }
                return values;
            } else {
                return null;
            }
        } catch (Exception e) {
            LogUtil.w(TAG, "getFirstMatch failed", e);
            return null;
        } finally {
            if (cursor != null)
                cursor.close();
            if (db != null)
                db.close();
        }
    }

    public static List<ContentValues> queryTable(SQLiteDatabase rDB, String tableName, String selection, String[] selectionArgs, String orderBy, int maxCount) {
        List<ContentValues> rows = new ArrayList<>();
        if (maxCount == 0)
            return rows;

        Cursor cursor = null;
        try {
            cursor = rDB.query(tableName, null, selection, selectionArgs, null, null, orderBy);
            while (cursor.moveToNext()) {
                ContentValues values = new ContentValues();
                for (int i = 0; i < cursor.getColumnCount(); ++i) {
                    values.put(cursor.getColumnName(i), cursor.getString(i));
                }
                rows.add(values);

                if (maxCount > 0)
                    --maxCount;
                if (maxCount == 0)
                    break;
            }
        } catch (Exception e) {
            LogUtil.w(TAG, "queryTable failed", e);
        } finally {
            if (cursor != null)
                cursor.close();
        }

        return rows;
    }

    private class DbHelper extends SQLiteOpenHelper {

        public DbHelper(Context context, String name, int version) {
            super(context, name, null, version);
            getReadableDatabase();
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            synchronized (mTableHelpers) {
                for (TableHelper helper : mTableHelpers)
                    helper.onCreate(db);
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            synchronized (mTableHelpers) {
                for (TableHelper helper : mTableHelpers)
                    helper.onUpgrade(db, oldVersion, newVersion);
            }
        }
    }
}
