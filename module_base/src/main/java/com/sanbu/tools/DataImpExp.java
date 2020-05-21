package com.sanbu.tools;

import com.sanbu.base.Result;

import java.util.List;

/** example
 * 1. 需求:
 * 1) 导出文件`/path1/file1`,导入时默认还原到`/path11/file11`
 * 2) 导出目录`/path2`,导入时默认还原到`/path22`
 * 3) 导出Android系统属性`com.example.prop1`,导入时默认按原属性还原
 * 4) 导出Linux环境变量`VAR1`,导入时默认按原变量名还原
 * 5) 导出当前应用中`getValue`返回值,导入时通过`setValue`方法还原
 *
 * 2. 导出端代码:
 * ExtHelper helper = new ExtHelper() {
 *     @Override
 *     public Result extract4Others(IEItem item, String file) {
 *         if (item.type != DataType.Others)
 *             throw new RuntimeException("invalid type");
 *         if (item.srcUri.startsWith("getValue")) {
 *             // do something
 *             return Result.SUCCESS;
 *         } else {
 *             return new Result(BaseError.ACTION_UNSUPPORTED, "unknown method: " + item.srcUri);
 *         }
 *     }
 * }
 * Exporter exporter = new Exporter();
 * exporter.setTargets(Arrays.asList(
 *      new IEItem("文件名", DataType.File, "/path1/file1", "/path11/file11"),
 *      new IEItem("目录名", DataType.File, "/path2", "/path22"),
 *      new IEItem("属性名", DataType.AndroidProp, "com.example.prop1", "com.example.prop1"),
 *      new IEItem("环境变量名", DataType.LinuxEnv, "VAR1", "VAR1"),
 *      new IEItem("应用数据名", DataType.Others, "getValue#param1#param2", "setValue#param1#param2")
 * ));
 * exporter.exec("/sdcard/export1.tar.gz", PackageFormat.TarGz, "/sdcard/tmp/", helper);
 *
 * 3. 导出文件`export1.tar.gz`的内容:
 * -- .avalon.ie.json # 索引文件
 * -- file
 *    -- ./path11/file11
 *    -- ./path22
 * -- prop
 *    -- com.example.prop1
 * -- env
 *    -- VAR1
 * -- others
 *    -- other1
 * `.avalon.ie.json`内容:
 * ```
 * [
 *      {
 *         "name": "文件名";
 *         "type": "File";
 *         "srcUri": "./file/path11/file11";
 *         "dstUri": "/path11/file11";
 *      },
 *      {
 *         "name": "目录名";
 *         "type": "File";
 *         "srcUri": "./file/path22";
 *         "dstUri": "/path22";
 *      },
 *      {
 *         "name": "属性名";
 *         "type": "AndroidProp";
 *         "srcUri": "./prop/com.example.prop1";
 *         "dstUri": "com.example.prop1";
 *      },
 *      {
 *         "name": "环境变量名";
 *         "type": "LinuxEnv";
 *         "srcUri": "./env/VAR1";
 *         "dstUri": "VAR1";
 *      },
 *      {
 *         "name": "应用数据名";
 *         "type": "Others";
 *         "srcUri": "./others/other1";
 *         "dstUri": "setValue#param1#param2";
 *      }
 * ]
 * ```
 *
 * 4. 导入端代码:
 * ExtHelper helper = new ExtHelper() {
 *     @Override
 *     public Result extract4Others(IEItem item, String file) {
 *         if (item.type != DataType.Others)
 *             throw new RuntimeException("invalid type");
 *         if (item.dstUri.startsWith("setValue")) {
 *             // do something
 *             return Result.SUCCESS;
 *         } else {
 *             return new Result(BaseError.ACTION_UNSUPPORTED, "unknown method: " + item.dstUri);
 *         }
 *     }
 * }
 * Importer importer = Importer.parse("/sdcard/export1.tar.gz", PackageFormat.TarGz, "/sdcard/tmp/");
 * importer.exec(helper);
**/

// data importer and exporter
public interface DataImpExp {
    enum PackageFormat {
        TarGz,
    }

    enum DataType {
        File,
        AndroidProp,    // TODO
        LinuxEnv,       // TODO
        WinsEnv,        // TODO
        WinsRegedit,    // TODO
        Others          // TODO
    }

    class IEItem {
        public String name;
        public DataType type;
        public String srcUri;
        public String dstUri;
        public boolean enabled;

        public IEItem(String name, DataType type, String srcUri, String dstUri) {
            this.name = name;
            this.type = type;
            this.srcUri = srcUri;
            this.dstUri = dstUri;
            this.enabled = true;
        }
    }

    interface ExtHelper {
        Result extract4Others(IEItem item, String file);
    }

    class Exporter {
        public Exporter() {

        }

        public void setTargets(List<IEItem> targets) {

        }

        public List<IEItem> getTargets() {
            return null;
        }

        public Result exec(String iePackagePath, PackageFormat format) {
            return exec(iePackagePath, format, null, null);
        }

        public Result exec(String iePackagePath, PackageFormat format, String tmpDir, ExtHelper helper) {
            return Result.SUCCESS;
        }
    }

    class Importer {
        public static Importer parse(String iePackagePath, PackageFormat format, String tmpDir) {
            return null;
        }

        private Importer() {

        }

        public List<IEItem> getResources() {
            return null;
        }

        public Result updateResIndex(List<IEItem> resource) {
            return Result.SUCCESS;
        }

        public Result exec() {
            return exec(null);
        }

        public Result exec(ExtHelper helper) {
            return Result.SUCCESS;
        }
    }
}
