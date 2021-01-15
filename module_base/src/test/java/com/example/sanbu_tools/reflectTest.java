package com.example.sanbu_tools;

import com.google.gson.Gson;
import com.sanbu.base.Result;
import com.sanbu.tools.LogUtil;

import org.junit.Test;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class reflectTest {
    @Test
    public void test1() throws Exception {
        LogUtil.initEnv(false);

        Object obj1 = Result.SUCCESS;
        LogUtil.i("obj1: " + obj1);

        LogUtil.i("clazz1: " + obj1.getClass());

        String name1 = obj1.getClass().getName();
        LogUtil.i("name1: " + name1);

        String str1 = new Gson().toJson(obj1);
        LogUtil.i("str1: " + str1);

        Class clazz2;
        try {
            clazz2 = Class.forName(name1);
        } catch (ClassNotFoundException e) {
            clazz2 = null;
        }
        LogUtil.i("clazz2: " + clazz2);

        Object obj2 = new Gson().fromJson(str1, clazz2);
        LogUtil.i("obj2: " + obj2);

        String str2 = new Gson().toJson(obj2);
        LogUtil.i("str2: " + str2);
    }
}