package com.sanbu.tools;

import android.os.SystemProperties;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sanbu.base.BaseError;
import com.sanbu.base.Result;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * example
 * 1. 需求:
 * 1) 导出文件`/path1/file1`,导入时默认还原到`/path11/file11`
 * 2) 导出目录`/path2`,导入时默认还原到`/path22`
 * 3) 导出Android系统属性`com.example.prop1`,导入时默认按原属性还原
 * 4) 导出Linux环境变量`VAR1`,导入时默认按原变量名还原
 * 5) 导出当前应用中`getValue`返回值,导入时通过`setValue`方法还原
 * <p>
 * 2. 导出端代码:
 * ExtHelper helper = new ExtHelper() {
 *
 * @Override public Result extract4Others(IEItem item, String file) {
 * if (item.type != DataType.Others)
 * throw new RuntimeException("invalid type");
 * if (item.srcUri.startsWith("getValue")) {
 * // do something
 * return Result.SUCCESS;
 * } else {
 * return new Result(BaseError.ACTION_UNSUPPORTED, "unknown method: " + item.srcUri);
 * }
 * }
 * }
 * Exporter exporter = new Exporter();
 * exporter.setTargets(Arrays.asList(
 * new IEItem("文件名", DataType.File, "/path1/file1", "/path11/file11"),
 * new IEItem("目录名", DataType.File, "/path2", "/path22"),
 * new IEItem("属性名", DataType.AndroidProp, "com.example.prop1", "com.example.prop1"),
 * new IEItem("环境变量名", DataType.LinuxEnv, "VAR1", "VAR1"),
 * new IEItem("应用数据名", DataType.Others, "getValue#param1#param2", "setValue#param1#param2")
 * ));
 * exporter.exec("/sdcard/export1.tar.gz", PackageFormat.TarGz, "/sdcard/tmp/", helper);
 * <p>
 * 3. 导出文件`export1.tar.gz`的内容:
 * -- .avalon.ie.json # 索引文件
 * -- file
 * -- ./path11/file11
 * -- ./path22
 * -- prop
 * -- com.example.prop1
 * -- env
 * -- VAR1
 * -- others
 * -- other1
 * `.avalon.ie.json`内容:
 * ```
 * [
 * {
 * "name": "文件名";
 * "type": "File";
 * "srcUri": "./file/path11/file11";
 * "dstUri": "/path11/file11";
 * },
 * {
 * "name": "目录名";
 * "type": "File";
 * "srcUri": "./file/path22";
 * "dstUri": "/path22";
 * },
 * {
 * "name": "属性名";
 * "type": "AndroidProp";
 * "srcUri": "./prop/com.example.prop1";
 * "dstUri": "com.example.prop1";
 * },
 * {
 * "name": "环境变量名";
 * "type": "LinuxEnv";
 * "srcUri": "./env/VAR1";
 * "dstUri": "VAR1";
 * },
 * {
 * "name": "应用数据名";
 * "type": "Others";
 * "srcUri": "./others/other1";
 * "dstUri": "setValue#param1#param2";
 * }
 * ]
 * ```
 * <p>
 * 4. 导入端代码:
 * ExtHelper helper = new ExtHelper() {
 * @Override public Result extract4Others(IEItem item, String file) {
 * if (item.type != DataType.Others)
 * throw new RuntimeException("invalid type");
 * if (item.dstUri.startsWith("setValue")) {
 * // do something
 * return Result.SUCCESS;
 * } else {
 * return new Result(BaseError.ACTION_UNSUPPORTED, "unknown method: " + item.dstUri);
 * }
 * }
 * }
 * Importer importer = Importer.parse("/sdcard/export1.tar.gz", PackageFormat.TarGz, "/sdcard/tmp/");
 * importer.exec(helper);
 **/

// data importer and exporter
public interface DataImpExp {

    String TAG = DataImpExp.class.getSimpleName();

    String IE_INFO_FILE = ".avalon.ie.json";

    enum PackageFormat {
        TarGz,
    }

    enum DataType {
        File,
        AndroidProp,
        LinuxEnv,       // TODO
        WinsEnv,        // TODO
        WinsRegedit,    // TODO
        Others
    }

    class IEItem {
        public String name;
        public DataType type;
        public String srcUri;
        public String dstUri;
        public boolean enabled;

        public IEItem(String name, DataType type, String srcUri, String dstUri, boolean enabled) {
            this.name = name;
            this.type = type;
            this.srcUri = srcUri;
            this.dstUri = dstUri;
            this.enabled = enabled;
        }

        public IEItem(String name, DataType type, String srcUri, String dstUri) {
            this(name, type, srcUri, dstUri, true);
        }

        public IEItem(IEItem other) {
            this(other.name, other.type, other.srcUri, other.dstUri, other.enabled);
        }

        public boolean isValid() {
            return !StringUtil.isEmpty(name) && type != null &&
                    !StringUtil.isEmpty(srcUri) && !StringUtil.isEmpty(dstUri);
        }
    }

    interface ExtExporter {
        Result/*String:item-data*/ exec(IEItem item);
    }

    interface ExtImporter {
        Result exec(IEItem item, String value);
    }

    class Exporter {
        private static final String ACTION = Exporter.class.getSimpleName() + ".exec";
        private static final String FILE_DIR = "file";
        private static final String PROP_DIR = "prop";
        private static final String OTHER_DIR = "others";

        private List<IEItem> mTargets;

        public Exporter() {
        }

        public void setTargets(List<IEItem> targets) {
            mTargets = targets;
        }

        public List<IEItem> getTargets() {
            return mTargets;
        }

        public Result/*outputFile*/ exec(PackageFormat format, String tmpDir, String outDir, String packagePrefix) {
            return exec(format, tmpDir, outDir, packagePrefix, null);
        }

        public Result/*outputFile*/ exec(PackageFormat format, String tmpDir, String outDir, String packagePrefix, ExtExporter helper) {
            if (StringUtil.isEmpty(tmpDir) || StringUtil.isEmpty(outDir))
                return Utils.genError(ACTION, BaseError.INVALID_PARAM,
                        "exec failed, input invalid!", "无效的路径");

            String ts = TimeUtil.getCurrentTime("yyyyMMdd.HHmmss.SSS");
            File opsDir = new File(tmpDir, packagePrefix + "." + ts);

            Result result = exec(format, opsDir, new File(outDir), helper);
            if (result.isSuccessful())
                LogUtil.i(TAG, "success to export " + result.data);
            FileUtil.deleteDir(opsDir);
            return result;
        }

        private Result exec(PackageFormat format, File opsDir, File outDir, ExtExporter helper) {
            opsDir.mkdirs();
            outDir.mkdirs();

            // output targets to operating dir
            Result result = outputTargets(mTargets, opsDir, helper);
            if (!result.isSuccessful())
                return result;
            List<IEItem> outputs = (List<IEItem>) result.data;

            // gen ie-info file
            String content = new Gson().toJson(outputs);
            File dstPath = genDstPath(opsDir, IE_INFO_FILE);
            if (!FileUtil.writeToFile(dstPath.getAbsolutePath(), content))
                return Utils.genError(ACTION, BaseError.INTERNAL_ERROR,
                        "writeToFile failed", "内部错误,导出描述信息写入文件失败");

            // gen output package
            switch (format) {
                case TarGz: {
                    String name = opsDir.getName() + ".tar.gz";
                    File output = new File(outDir, name);

                    String cmd = String.format("cd %s; busybox tar -czf %s %s",
                            opsDir.getParent(), output, opsDir.getName());
                    LocalLinuxUtil.Result ret = Utils.execCmd(cmd, 0);
                    if (ret.code == 0)
                        return Result.buildSuccess(output.getAbsolutePath());
                    else
                        return Utils.genError(ACTION, BaseError.INTERNAL_ERROR,
                                "tar to gz failed: " + ret.AllToString(), "内部错误,导出打包失败");
                }
                default:
                    return Utils.genError(ACTION, BaseError.ACTION_UNSUPPORTED,
                            "not support the package format: " + format.name(),
                            "无效的导出格式: " + format.name());
            }
        }

        private static Result outputTargets(List<IEItem> targets, File opsDir, ExtExporter helper) {
            List<IEItem> items = new ArrayList<>(targets.size());

            int otherIdx = 0;
            for (IEItem item: targets) {
                if (!item.isValid())
                    return Utils.genError(ACTION, BaseError.INVALID_PARAM,
                            "target is invalid: " + item.name, "无效的目标: " + item.name);
                if (!item.enabled)
                    continue;

                switch (item.type) {
                    case File: {
                        // check src file
                        File src = new File(item.srcUri);
                        if (!src.exists())
                            return Utils.genError(ACTION, BaseError.TARGET_NOT_FOUND,
                                    "target is not found: " + item.srcUri, "目标文件不存在: " + item.srcUri);
                        else if (!src.isFile() && !src.isDirectory())
                            return Utils.genError(ACTION, BaseError.INVALID_PARAM,
                                    "target is not a file or directory: " + item.srcUri, "目标不是有效的文件或者目录: " + item.srcUri);

                        // gen dst path
                        File dstPath = genDstPath(new File(opsDir, FILE_DIR), src.getName());
                        String relative = new File(FILE_DIR, dstPath.getName()).getPath();

                        // copy src file to dst path
                        if (!Utils.copyFileByShell(item.srcUri, dstPath.getAbsolutePath()))
                            return Utils.genError(ACTION, BaseError.INTERNAL_ERROR,
                                    "copyFileByShell failed", "内部错误,文件复制失败: " + item.name);

                        //add json item
                        IEItem temp = new IEItem(item.name, item.type, relative, item.dstUri, true);
                        items.add(temp);
                        break;
                    }
                    case AndroidProp: {
                        // get prop value
                        String propName = item.srcUri;
                        String propValue = SystemProperties.get(propName, "");

                        // gen dst path
                        File dstPath = genDstPath(new File(opsDir, PROP_DIR), propName);
                        String relative = new File(PROP_DIR, dstPath.getName()).getPath();

                        // write the prop value to dst file
                        if (!FileUtil.writeToFile(dstPath.getAbsolutePath(), propValue))
                            return Utils.genError(ACTION, BaseError.INTERNAL_ERROR,
                                    "writeToFile failed", "内部错误,系统属性写入文件失败: " + item.name);

                        //add json item
                        IEItem temp = new IEItem(item.name, item.type, relative, item.dstUri, true);
                        items.add(temp);
                        break;
                    }
                    case LinuxEnv:
                    case WinsEnv:
                    case WinsRegedit:
                        return Utils.genError(ACTION, BaseError.ACTION_UNSUPPORTED,
                                "not support for " + item.type.name(), "暂不支持导出数据: " + item.type.name());
                    case Others: {
                        if (helper == null)
                            return Utils.genError(ACTION, BaseError.INTERNAL_ERROR,
                                    "has no valid helper for Others", "内部逻辑错误,无法导出" + item.name);

                        // get date by external helper
                        Result result = helper.exec(item);
                        if (!result.isSuccessful())
                            return Utils.genError(ACTION, BaseError.INTERNAL_ERROR,
                                    "extract4Others failed: " + result.getMessage(),
                                    "内部错误,导出失败:" + item.name);
                        String data = (String) result.data;

                        // gen dst path
                        ++otherIdx;
                        File dstPath = genDstPath(new File(opsDir, OTHER_DIR), "other." + otherIdx);
                        String relative = new File(OTHER_DIR, dstPath.getName()).getPath();

                        // write the prop value to dst file
                        if (!FileUtil.writeToFile(dstPath.getAbsolutePath(), data))
                            return Utils.genError(ACTION, BaseError.INTERNAL_ERROR,
                                    "writeToFile failed", "内部错误,外部数据写入文件失败: " + item.name);

                        //add json item
                        IEItem temp = new IEItem(item.name, item.type, relative, item.dstUri, true);
                        items.add(temp);
                        break;
                    }
                }
            }

            return Result.buildSuccess(items);
        }

        private static File genDstPath(File dstDir, String dstName) {
            dstDir.mkdirs();
            File dstPath = new File(dstDir, dstName);
            while (dstPath.exists()) {
                dstName += "(1)";
                dstPath = new File(dstDir, dstName);
            }
            return dstPath;
        }
    }

    class Importer {
        private static final String ACTION = Importer.class.getSimpleName() + ".exec";

        private static final String TAR_GZ_SUFFIX = ".tar.gz";

        private File mOpsDir;
        private List<IEItem> mResource;

        public static Result/*Importer*/ parse(String iePackagePath, PackageFormat format, String tmpDir) {
            if (StringUtil.isEmpty(iePackagePath) || StringUtil.isEmpty(tmpDir) ||
                    !new File(iePackagePath).exists() || new File(tmpDir).isFile())
                return Utils.genError(ACTION, BaseError.INVALID_PARAM,
                        "invalid package path or tmp dir", "无效的导入路径");
            new File(tmpDir).mkdirs();

            switch (format) {
                case TarGz: {
                    String packageName = new File(iePackagePath).getName();
                    if (!packageName.endsWith(TAR_GZ_SUFFIX))
                        return Utils.genError(ACTION, BaseError.INVALID_PARAM,
                                "invalid package: " + packageName, "无效的压缩包: " + packageName);

                    // unzip package
                    String cmd = String.format("busybox tar -xzf %s -C %s", iePackagePath, tmpDir);
                    LocalLinuxUtil.Result result = Utils.execCmd(cmd, 0);
                    if (result.code != 0)
                        return Utils.genError(ACTION, BaseError.INTERNAL_ERROR,
                                "unzip tar.gz failed: " + result.AllToString(), "内部错误,解压数据包失败");

                    // check operating dir and ie-info file
                    String name = packageName.substring(0, packageName.lastIndexOf(TAR_GZ_SUFFIX));
                    File opsDir = new File(tmpDir, name);
                    File info = new File(opsDir, IE_INFO_FILE);
                    if (!opsDir.exists() || !info.exists())
                        return Utils.genError(ACTION, BaseError.INVALID_PARAM,
                                "invalid package", "数据包内容无效");

                    // get resource info from ie-info file
                    List<IEItem> resource = parseResource(info.getAbsolutePath());
                    if (resource == null)
                        return Utils.genError(ACTION, BaseError.INTERNAL_ERROR,
                                "get resource info from ie-info file failed", "内部错误,读取数据包导入信息失败");

                    // fix resource path
                    for (IEItem item: resource)
                        item.srcUri = new File(opsDir, item.srcUri).getAbsolutePath();

                    Importer importer = new Importer(opsDir, resource);
                    return Result.buildSuccess(importer);
                }
                default:
                    return Utils.genError(ACTION, BaseError.ACTION_UNSUPPORTED,
                            "not support package format: " + format.name(), "不支持的导入包类型: " + format.name());
            }
        }

        private Importer(File opsDir, List<IEItem> resource) {
            mOpsDir = opsDir;
            mResource = resource;
        }

        public List<IEItem> getResource() {
            return mResource;
        }

        public Result updateResIndex(List<IEItem> resource) {
            // TODO: check resource
            mResource = resource;
            return Result.SUCCESS;
        }

        public Result exec() {
            return exec(null);
        }

        public Result exec(ExtImporter helper) {
            if (mResource == null)
                return Utils.genError(ACTION, BaseError.ACTION_ILLEGAL,
                        "invalid Importer", "逻辑错误,无效的导入");

            List<String> hints = new LinkedList<>();
            for (IEItem item: mResource) {
                if (!item.isValid()) {
                    onError(hints, "invalid item: " + item.name,
                            "无效的数据项: " + item.name);
                    continue;
                }

                File src = new File(item.srcUri);
                if (!src.exists()) {
                    onError(hints, "not find the prop file: " + item.srcUri,
                            "无法找到" + item.name + "的数据文件");
                    continue;
                }

                switch (item.type.toString()) {
                    case "File": {
                        if (!Utils.copyFileByShell(item.srcUri, item.dstUri))
                            onError(hints, "copy file failed: " + item.name,
                                    "导入文件" + item.name + "失败");
                        break;
                    }
                    case "AndroidProp": {
                        String propName = item.dstUri;

                        // read prop value from file
                        String value = FileUtil.readToText(item.srcUri);
                        if (value == null) {
                            onError(hints, "read prop file failed: " + item.srcUri,
                                    "读取系统属性[" + item.name + "]数据文件失败");
                            break;
                        }

                        // set value to prop
                        String current = SystemProperties.get(propName, "");
                        if (!current.equals(value)) {
                            try {
                                SystemProperties.set(propName, value);
                            } catch (Exception e) {
                                onError(hints, "set prop failed(" + propName + "):" + e.getMessage(),
                                        "设置系统属性[" + item.name + "]失败");
                                break;
                            }
                        }

                        break;
                    }
                    case "LinuxEnv":
                    case "WinsEnv":
                    case "WinsRegedit":
                        onError(hints, "not support for " + item.type.name(),
                                "暂不支持导入数据: " + item.type.name());
                        break;
                    case "Others": {
                        if (helper == null) {
                            onError(hints, "has no valid helper for Others",
                                    "内部逻辑错误,无法导入" + item.name);
                            break;
                        }

                        // read item value from file
                        String value = FileUtil.readToText(item.srcUri);
                        if (value == null) {
                            onError(hints, "read data for others failed: " + item.srcUri,
                                    "读取[" + item.name + "]数据文件失败");
                            break;
                        }

                        Result result = helper.exec(item, value);
                        if (!result.isSuccessful())
                            onError(hints, "extract4Others failed: " + result.getMessage(),
                                    "内部错误,导入失败:" + item.name);
                        break;
                    }
                    default:
                        break;
                }
            }

            if (hints.size() > 0)
                return new Result(BaseError.INTERNAL_ERROR, combineHints(hints));

            FileUtil.deleteDir(mOpsDir);
            return Result.SUCCESS;
        }

        private static List<IEItem> parseResource(String infoFile) {
            String content = FileUtil.readToText(infoFile);
            if (content == null) {
                LogUtil.w(TAG, "parseResource failed: " + infoFile);
                return null;
            }

            return new Gson().fromJson(content, new TypeToken<List<IEItem>>(){}.getType());
        }

        private static void onError(List<String> hints, String message, String hint) {
            LogUtil.w(TAG, ACTION, message);
            hints.add(hint);
        }

        private static String combineHints(List<String> hints) {
            String hint = "部分导入失败:\n";
            for (String h: hints)
                hint += h + ";\n";
            return hint;
        }
    }

    class Utils {
        static LocalLinuxUtil.Result execCmd(String cmd, int success) {
            LogUtil.d(TAG, String.format("run exec cmd:[%s]", cmd));
            LocalLinuxUtil.Result result = LocalLinuxUtil.doShellWithResult(cmd);
            if (result.code != success)
                LogUtil.w(TAG, String.format("run exec cmd[%s] failed: %s", cmd, result.AllToString()));
            return result;
        }

        static boolean copyFileByShell(String src, String dst) {
            File srcFile = new File(src);
            File dstFile = new File(dst);
            dstFile.getParentFile().mkdirs();

            String cmd = null;
            if (srcFile.isDirectory()) {
                if (!dstFile.exists())
                    cmd = String.format("cp -a %s %s", src, dst);
                else if (dstFile.isDirectory())
                    cmd = String.format("cp -a %s/* %s/", src, dst);
                else
                    LogUtil.w(TAG, "copyFileByShell, dst conflicts with src: " + dst);
            } else {
                cmd = String.format("cp -a %s %s", src, dst);
            }

            if (cmd == null)
                return false;
            else
                return Utils.execCmd(cmd, 0).code == 0;
        }

        private static Result genError(String action, int error, String message, String hint) {
            LogUtil.w(TAG, action + ":" + message);
            return new Result(error, hint);
        }

        private static Result genError(String action, int error, Throwable throwable, String message, String hint) {
            LogUtil.w(TAG, action + ":" + message, throwable);
            return new Result(error, hint);
        }
    }
}
