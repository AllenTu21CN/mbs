package com.sanbu.tools;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public class BuildingPrinter {

    public static void print(Class clazzOfBuildConfig) {
        print(null, null, clazzOfBuildConfig);
    }

    public static void print(String tag, Class clazzOfBuildConfig) {
        print(tag, null, clazzOfBuildConfig);
    }

    public static void print(String tag, String subTags, Class clazzOfBuildConfig) {
        print(tag, subTags, ">>>>>>>>>>>>>>>>>>>>>Building Information>>>>>>>>>>>>>>>>>>>>>");

        try {
            Field[] fields = clazzOfBuildConfig.getFields();
            for (Field field : fields) {
                String key = field.getName();
                Object value = field.get(clazzOfBuildConfig);

                if (key.equals("jniLibs")) {
                    print(tag, subTags, key + ": ");
                    List<String> lines = new Gson().fromJson(value.toString(), new TypeToken<List<String>>() {}.getType());
                    for (String line : lines)
                        print(tag, subTags, "  " + line);
                } else {
                    print(tag, subTags, key + ": " + value.toString());
                }
            }
        } catch (Exception e) {
            print(tag, subTags, "Invalid buildConfig: " + e.getMessage());
        }

        print(tag, subTags, "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
    }

    private static void print(String tag, String subTags, String text) {
        if (tag == null)
            LogUtil.w(text);
        else if (subTags == null)
            LogUtil.w(tag, text);
        else
            LogUtil.w(tag, subTags, text);
    }
}
