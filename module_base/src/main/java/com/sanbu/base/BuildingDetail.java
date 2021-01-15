package com.sanbu.base;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sanbu.BaseConst;
import com.sanbu.tools.LogUtil;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class BuildingDetail {
    // 版本全称, E.G.: [2.0.0-a2-2020123116]
    public String versionName;

    // 版本SHA值, E.G.: [2c0866ef0]
    public String versionSHA;

    // 打包类型, E.G.: [release]
    public String buildType;

    // 打包时间, E.G.: [20210104 14:18:20]
    public String buildTime;

    // 版本来源, E.G.: [Tuyj-T470p]
    public String buildHost;

    // 渠道标记, E.G.: [3]
    public String flavor;

    // 动态库信息
    public List<String> jniLibs;

    public BuildingDetail(String versionName, String versionSHA, String buildType,
                          String buildTime, String buildHost, String flavor, List<String> jniLibs) {
        this.versionName = versionName;
        this.versionSHA = versionSHA;
        this.buildType = buildType;
        this.buildTime = buildTime;
        this.buildHost = buildHost;
        this.flavor = flavor;
        this.jniLibs = jniLibs;
    }

    public static BuildingDetail buildEmpty() {
        return new BuildingDetail("", "", "",
                "", "", "", Collections.EMPTY_LIST);
    }

    public static BuildingDetail build(Class clazzOfBuildConfig) {
        String versionName = "";
        String versionSHA = "";
        String buildType = "";
        String buildTime = "";
        String buildHost = "";
        String flavor = "";
        List<String> jniLibs = new LinkedList<>();

        try {
            Field[] fields = clazzOfBuildConfig.getFields();
            for (Field field : fields) {
                String key = field.getName();
                Object value = field.get(clazzOfBuildConfig);

                if (key.equals("VERSION_CODE")) {
                    versionName += "-" + value.toString();
                } else if (key.equals("VERSION_NAME")) {
                    versionName = value.toString() + versionName;
                } else if (key.equals("VERSION_GIT_ID")) {
                    versionSHA = value.toString();
                } else if (key.equals("BUILD_TYPE")) {
                    buildType = value.toString();
                } else if (key.equals("BUILD_DATE")) {
                    buildTime = value.toString();
                } else if (key.equals("BUILD_HOST")) {
                    buildHost = value.toString();
                } else if (key.equals("FLAVOR")) {
                    flavor = value.toString();
                    if (flavor.isEmpty())
                        flavor = "3";
                    else
                        flavor = flavor.substring(0, 2);
                } else if (key.equals("jniLibs")) {
                    List<String> lines = new Gson().fromJson(value.toString(),
                            new TypeToken<List<String>>() {}.getType());

                    for (String line : lines)
                        jniLibs.add(line);
                }
            }
        } catch (Exception e) {
            LogUtil.w(BaseConst.TAG, "build BuildingDetail failed", e);
        }

        return new BuildingDetail(versionName, versionSHA, buildType, buildTime, buildHost, flavor, jniLibs);
    }
}
