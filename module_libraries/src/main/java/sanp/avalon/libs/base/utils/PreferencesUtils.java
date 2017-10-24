package sanp.avalon.libs.base.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

/**
 * SharedPreferences使用帮助类
 * 
 * @author dong_xinguang
 * 
 */
public class PreferencesUtils {

	/**
	 * 实现单例
	 */
	private static PreferencesUtils mSPHelper;

	/**
	 * 上下文
	 */
	private static Context mContext;

	/**
	 * 私有构造
	 */
	private PreferencesUtils() {
	};

	/**
	 * 提供获得单例接口
	 * 
	 * @param context
	 *            上下文
	 * @return 实例对象
	 */
	public static PreferencesUtils getInstance(Context context) {
		mContext = context;
		if (null == mSPHelper) {
			mSPHelper = new PreferencesUtils();
		}
		mSPHelper.initConfig();
		return mSPHelper;
	}

	/**
	 * 配置文件名称
	 */
	private static final String SHARE_PREFERENCE = "sdsc_shared_preference";

	/**
	 * 配置文件
	 */
	private SharedPreferences mSharedPreferences;
	private Editor editor;

	/**
	 * 初始化sharedPreferences、Editor
	 */
	public void initConfig() {
		if (mSharedPreferences == null)
			mSharedPreferences = mContext.getSharedPreferences(
					SHARE_PREFERENCE, Context.MODE_PRIVATE);
		if (editor == null)
			editor = mSharedPreferences.edit();
	}

	/**
	 * 释放sharedPreferences、Editor
	 */
	public void free() {
		if (mSharedPreferences != null) {
			mSharedPreferences = null;
		}
		if (editor != null) {
			editor = null;
		}
	}

	/**
	 * 存string类型的数据
	 * 
	 * @param key
	 * @param data
	 */
	public void putSharedDatas(String key, String data) {
		editor.putString(key, data);
		editor.commit();
	}

	/**
	 * 存boolean类型的数据
	 * 
	 * @param key
	 * @param data
	 */
	public void putSharedDatas(String key, Boolean data) {
		editor.putBoolean(key, data);
		editor.commit();
	}

	/**
	 * 存float类型的数据
	 * 
	 * @param key
	 * @param data
	 */
	public void putSharedDatas(String key, float data) {
		// Editor editor = mSharedPreferences.edit();
		editor.putFloat(key, data);
		editor.commit();
	}

	/**
	 * 存int类型的数据
	 * 
	 * @param key
	 * @param data
	 */
	public void putSharedDatas(String key, int data) {
		editor.putInt(key, data);
		editor.commit();
	}

	/**
	 * 存string类型的数据
	 * 
	 * @param key
	 * @param data
	 */
	public void putSharedDatas(String key, long data) {
		editor.putLong(key, data);
		editor.commit();
	}

	/**
	 * 取key对应的值，string类型
	 * 
	 * @param key
	 * @param defaultValue
	 *            默认值
	 * @return
	 */
	public String getStringSharedDatas(String key, String defaultValue) {
		return mSharedPreferences.getString(key, defaultValue);
	}

	/**
	 * 取key对应的值，boolean类型
	 * 
	 * @param key
	 * @param defaultValue
	 *            默认值
	 * @return
	 */
	public boolean getBooleanSharedDatas(String key, boolean defaultValue) {
		return mSharedPreferences.getBoolean(key, defaultValue);
	}

	/**
	 * 取key对应的值，int类型
	 * 
	 * @param key
	 * @param defaultValue
	 *            默认值
	 * @return
	 */
	public int getIntSharedDatas(String key, int defaultValue) {
		return mSharedPreferences.getInt(key, defaultValue);
	}

	/**
	 * 取key对应的值，float类型
	 * 
	 * @param key
	 * @param defaultValue
	 *            默认值
	 * @return
	 */
	public float getFloatSharedDatas(String key, float defaultValue) {
		return mSharedPreferences.getFloat(key, defaultValue);
	}

	/**
	 * 取key对应的值，long类型
	 * 
	 * @param key
	 * @param defaultValue
	 *            默认值
	 * @return
	 */
	public long getLongSharedDatas(String key, long defaultValue) {
		return mSharedPreferences.getLong(key, defaultValue);
	}

	/**
	 * 是否包含key
	 */
	public boolean hasKey(String key) {

		return mSharedPreferences.contains(key);
	}

	/**
	 * 删除key
	 */
	public void removeKey(String key) {
		editor.remove(key);
		editor.commit();
	}

}
