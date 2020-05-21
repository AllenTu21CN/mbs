package com.sanbu.discarded.tools;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.sanbu.tools.LogUtil;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class SPUtil {

    private static final String TAG = SPUtil.class.getSimpleName();

    private static SPUtil gInstance;

    public static SPUtil getInstance() {
        if (gInstance == null) {
            synchronized (SPUtil.class) {
                if (gInstance == null)
                    gInstance = new SPUtil();
            }
        }
        return gInstance;
    }

    private Map<String, SharedPreferences> mSharedPrefMap = new HashMap<>();

    private Context mContext;
    private String mPath;

    private SPUtil() {

    }

    public void init(Context context, String sharedPath) {
        this.mContext = context;
        this.mPath = sharedPath;
        initSharedPrePath(sharedPath);
    }

    public void putString(String name, String key, String value) {
        SharedPreferences sp = getSharedPreferences(name);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public void putInt(String name, String key, int value) {
        SharedPreferences sp = getSharedPreferences(name);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(key, value);
        editor.commit();
    }

    public void putLong(String name, String key, long value) {
        SharedPreferences sp = getSharedPreferences(name);
        SharedPreferences.Editor editor = sp.edit();
        editor.putLong(key, value);
        editor.commit();
    }

    public void putFloat(String name, String key, float value) {
        SharedPreferences sp = getSharedPreferences(name);
        SharedPreferences.Editor editor = sp.edit();
        editor.putFloat(key, value);
        editor.commit();
    }

    public void putBoolean(String name, String key, boolean value) {
        SharedPreferences sp = getSharedPreferences(name);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public void putObject(String name, String key, Object object) {
        String gson = object instanceof String ? (String) object : new Gson().toJson(object);
        putString(name, key, gson);
    }

    public String getString(String name, String key, String defaultValue) {
        SharedPreferences sp = getSharedPreferences(name);
        return sp.getString(key, defaultValue);
    }

    public int getInt(String name, String key, int defValue) {
        SharedPreferences sp = getSharedPreferences(name);
        return sp.getInt(key, defValue);
    }

    public long getLong(String name, String key, long defValue) {
        SharedPreferences sp = getSharedPreferences(name);
        return sp.getLong(key, defValue);
    }

    public float getFloat(String name, String key, float defValue) {
        SharedPreferences sp = getSharedPreferences(name);
        return sp.getFloat(key, defValue);
    }

    public boolean getBoolean(String name, String key, boolean defValue) {
        SharedPreferences sp = getSharedPreferences(name);
        return sp.getBoolean(key, defValue);
    }

    public <T> T getObject(String name, String key, Type type, T defValue) {
        SharedPreferences sp = getSharedPreferences(name);
        String obStr = sp.getString(key, null);
        if (TextUtils.isEmpty(obStr))
            return defValue;
        return new Gson().fromJson(obStr, type);
    }

    public boolean contains(String name, String key) {
        return getSharedPreferences(name).contains(key);
    }

    public void remove(String name, String key) {
        SharedPreferences sp = getSharedPreferences(name);
        SharedPreferences.Editor editor = sp.edit();
        editor.remove(key);
        editor.commit();
    }

    public void clear(String name) {
        SharedPreferences sp = getSharedPreferences(name);
        SharedPreferences.Editor editor = sp.edit();
        editor.clear();
        editor.commit();
    }

    public void delete(String name) {
        File sp = new File(mPath, name + ".xml");
        if (sp.exists() && sp.isFile())
            sp.delete();
    }

    private void initSharedPrePath(String path) {
        try {
            File spPath = new File(path);
            if (!spPath.exists())
                spPath.mkdir();

            Field field = ContextWrapper.class.getDeclaredField("mBase");
            field.setAccessible(true);
            Object obj = field.get(mContext);
            field = obj.getClass().getDeclaredField("mPreferencesDir");
            field.setAccessible(true);
            field.set(obj, spPath);

        } catch (NoSuchFieldException e) {
            // TODO Auto-generated catch block
            LogUtil.e(TAG, e);
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            LogUtil.e(TAG, e);
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            LogUtil.e(TAG, e);
        }
    }

    private SharedPreferences getSharedPreferences(String name) {
        synchronized (mSharedPrefMap) {
            SharedPreferences sp = mSharedPrefMap.get(name);
            if (sp == null) {
                sp = mContext.getSharedPreferences(name, Context.MODE_PRIVATE);
                mSharedPrefMap.put(name, sp);
            }
            return sp;
        }
    }
}
