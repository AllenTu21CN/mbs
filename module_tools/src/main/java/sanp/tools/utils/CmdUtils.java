package sanp.tools.utils;

import org.xutils.common.util.IOUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * Created by Vald on 2017/4/19.
 */

public class CmdUtils {

    /**
     * linux命令输出
     *
     * @param cmd 命令字符
     * @return String
     */
    public static String resultExeCmd(String cmd) {
        String returnString = "";
        Process pro = null;
        Runtime runTime = Runtime.getRuntime();
        if (runTime == null) {
            System.err.println("Create runtime false!");
        }
        try {
            pro = runTime.exec(cmd);
            BufferedReader input = new BufferedReader(new InputStreamReader(pro.getInputStream()));
            PrintWriter output = new PrintWriter(new OutputStreamWriter(pro.getOutputStream()));
            String line;
            while ((line = input.readLine()) != null) {
                returnString = returnString + line + "\n";
            }
            input.close();
            output.close();
            pro.destroy();
        } catch (Exception e) {
//            LogManager.e(CmdUtils.class.getName(),e);
        }
        return returnString;
    }

    public static String resultExeCmd(String cmd, String encode) {
        String returnString = "";
        Process pro = null;
        Runtime runTime = Runtime.getRuntime();
        if (runTime == null) {
            System.err.println("Create runtime false!");
        }
        try {
            pro = runTime.exec(cmd);
            byte[] bs = IOUtil.readBytes(pro.getInputStream());
            returnString = new String(bs,encode);
            pro.destroy();
        } catch (Exception e) {
//            LogManager.e(CmdUtils.class.getName(),e);
        }
        return returnString;
    }
}
