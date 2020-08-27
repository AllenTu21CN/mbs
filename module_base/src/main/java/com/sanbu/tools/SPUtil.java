package com.sanbu.tools;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.gson.Gson;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Type;

public class SPUtil {

    private static final String TAG = SPUtil.class.getSimpleName();

    private static Context gContext;
    private static String gPath;

    public static final void init(Context context, String sharedPath) {
        synchronized (SPUtil.class) {
            if (gContext == null) {
                gContext = context;
                gPath = sharedPath;
                initSharedPrePath(context, sharedPath);
            }
        }
    }

    private static final void initSharedPrePath(Context context, String path) {
        try {
            File spPath = new File(path);
            if (!spPath.exists())
                spPath.mkdirs();

            Field field = ContextWrapper.class.getDeclaredField("mBase");
            field.setAccessible(true);
            Object obj = field.get(context);
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

    private String mNamespace;
    private SharedPreferences mSPImpl;

    public SPUtil(String namespace) {
        if (gContext == null)
            throw new RuntimeException("call SPUtil init first");
        mNamespace = namespace;
        mSPImpl = gContext.getSharedPreferences(mNamespace, Context.MODE_PRIVATE);
    }

    public void putString(String key, String value) {
        SharedPreferences.Editor editor = mSPImpl.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public void putInt(String key, int value) {
        SharedPreferences.Editor editor = mSPImpl.edit();
        editor.putInt(key, value);
        editor.commit();
    }

    public void putLong(String key, long value) {
        SharedPreferences.Editor editor = mSPImpl.edit();
        editor.putLong(key, value);
        editor.commit();
    }

    public void putFloat(String key, float value) {
        SharedPreferences.Editor editor = mSPImpl.edit();
        editor.putFloat(key, value);
        editor.commit();
    }

    public void putBoolean(String key, boolean value) {
        SharedPreferences.Editor editor = mSPImpl.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public void putObject(String key, Object object) {
        String json = object instanceof String ? (String) object : new Gson().toJson(object);
        putString(key, json);
    }

    public String getString(String key, String defaultValue) {
        return mSPImpl.getString(key, defaultValue);
    }

    public int getInt(String key, int defValue) {
        return mSPImpl.getInt(key, defValue);
    }

    public long getLong(String key, long defValue) {
        return mSPImpl.getLong(key, defValue);
    }

    public float getFloat(String key, float defValue) {
        return mSPImpl.getFloat(key, defValue);
    }

    public boolean getBoolean(String key, boolean defValue) {
        return mSPImpl.getBoolean(key, defValue);
    }

    public <T> T getObject(String key, Type type, T defValue) {
        String obStr = mSPImpl.getString(key, null);
        if (TextUtils.isEmpty(obStr))
            return defValue;
        return new Gson().fromJson(obStr, type);
    }

    public boolean contains(String key) {
        return mSPImpl.contains(key);
    }

    public void remove(String key) {
        SharedPreferences.Editor editor = mSPImpl.edit();
        editor.remove(key);
        editor.commit();
    }

    public void clear() {
        SharedPreferences.Editor editor = mSPImpl.edit();
        editor.clear();
        editor.commit();
    }

    public void delete() {
        File sp = new File(gPath, mNamespace + ".xml");
        if (sp.exists() && sp.isFile())
            sp.delete();
    }
}
