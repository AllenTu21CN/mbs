package sanp.avalon.libs.base.utils;

import android.content.Context;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;

/**
 * Created by huang on 2017/6/2.
 */

public class CommonUtil {

    public static String Tag = CommonUtil.class.getName();

    /**
     * @param context
     * @param path      jar编译后的dex 路径：dx.bat --dex --output=test.dex test.jar
     * @param mainClass 主类
     * @param args      主类执行参数
     */
    public static void loadDex(Context context, String path, String mainClass, String... args) {
        try {
            File dexOutputDir = context.getDir("dex0", Context.MODE_APPEND);
            DexClassLoader loader = new DexClassLoader(path, dexOutputDir.getAbsolutePath(), null, context.getClassLoader());
            Class clazz = loader.loadClass(mainClass);
            if (args == null) {
                args = new String[]{};
            }
            Method method = clazz.getMethod("main", args.getClass());
            method.setAccessible(true);
            method.invoke(null, new Object[]{args});

        } catch (Exception e) {
            LogManager.e(Tag, e);
        }
    }

    /**
     * 反射通过属性名称获取对象的私有属性
     *
     * @param obj
     * @param name
     * @param classOfT
     * @param <T>
     * @return
     */
    public static <T> T getPrivateByName(Object obj, String name, Class<T> classOfT) {
        try {
            Class<?> objClazz = obj.getClass();
            Field field = objClazz.getDeclaredField(name);
            Field.setAccessible(new Field[]{field}, true);
            if (field != null && classOfT != null) return classOfT.cast(field.get(obj));
        } catch (Exception e) {
            LogManager.e(Tag, e);
        }
        return null;

    }


    /**
     * 反射调用构造器
     *
     * @param params
     * @param classOfT
     * @param <T>
     * @return
     */
    public static <T> T constructor(Object[] params, Class[] parameterTypes, Class<T> classOfT) {
        try {
            if (parameterTypes == null) parameterTypes = getparameterTypes(params);
            Constructor<T> c = classOfT.getDeclaredConstructor(parameterTypes);
            if (c != null) return c.newInstance(params);
        } catch (Exception e) {
            LogManager.e(Tag, e);
        }
        return null;
    }

    /**
     * 反射调用方法
     *
     * @param obj
     * @param name
     * @param params
     * @param <T>
     * @return
     */
    public static <T> T method(Object obj, String name, Object[] params) {
        return method(obj, name, params, null, null);
    }

    /**
     * 反射调用方法
     *
     * @param obj
     * @param name
     * @param params
     * @param classOfT
     * @param <T>
     * @return
     */
    public static <T> T method(Object obj, String name, Object[] params, Class<?>[] parameterTypes, Class<T> classOfT) {
        try {
            Class<?> objClazz = obj.getClass();
            if (parameterTypes == null) parameterTypes = getparameterTypes(params);
            Method method = objClazz.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            Object result = method.invoke(obj, params);
            if (result != null && classOfT != null) return classOfT.cast(result);
        } catch (Exception e) {
            LogManager.e(Tag, e);
        }
        return null;
    }

    private static Class<?>[] getparameterTypes(Object[] params) {
        Class<?>[] parameterTypes = null;
        if (params != null && params.length > 0) {
            parameterTypes = new Class[params.length];
            for (int i = 0; i < params.length; i++) {
                parameterTypes[i] = params[i].getClass();
            }
        }
        return parameterTypes;
    }

}
