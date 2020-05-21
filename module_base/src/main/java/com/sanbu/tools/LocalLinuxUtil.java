package com.sanbu.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Tuyj on 2017/12/7.
 */

public class LocalLinuxUtil {

    public static class Result {
        public int code;
        public List<String> stdOut = new LinkedList<>();
        public List<String> stdErr = new LinkedList<>();

        public Result(int code) {
            this.code = code;
        }

        public String AllToString() {
            String out = "code: " + code;

            if (stdOut.size() > 0) {
                out += ", out: [";
                for (String o : stdOut)
                    out += o + "; ";
                out += "]";
            }

            if (stdErr.size() > 0) {
                out += ", err: [";
                for (String e: stdErr)
                    out += e + "; ";
                out += "]";
            }
            return out;
        }

        public String OutToString() {
            String out = "";
            for (String o : stdOut)
                out += o + "\n";
            return out;
        }

        public String ErrorToString() {
            String out = "";
            for (String o : stdErr)
                out += o + "\n";
            return out;
        }
    }

    public static int doShell(String cmd) {
        try {
            String cmds[] = {"sh", "-c", cmd};
            Process process = Runtime.getRuntime().exec(cmds);
            return process.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static int doCommand(String cmd) {
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            return process.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static int doCommandByRoot(String cmd) {
        return doCommand("su -c \"" + cmd + "\""); // /system/bin/su or /system/xbin/su
    }

    public static Result doShellWithResult(String cmd) {
        Result result = new Result(-1);

        try {
            String line;

            Process process = Runtime.getRuntime().exec(new String[] {"sh", "-c", cmd});
            result.code = process.waitFor();

            BufferedReader inputOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((line = inputOut.readLine()) != null)
                result.stdOut.add(line);
            inputOut.close();

            if (result.code != 0) {
                BufferedReader inputErr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                while ((line = inputErr.readLine()) != null)
                    result.stdErr.add(line);
                inputErr.close();
            }

        } catch (Exception e) {
            result.stdErr.add(e.getLocalizedMessage());
        }

        return result;
    }

    public static Result doCommandWithResult(String cmd) {
        Result result = new Result(-1);

        try {
            String line;

            Process process = Runtime.getRuntime().exec(cmd);
            result.code = process.waitFor();

            BufferedReader inputOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((line = inputOut.readLine()) != null)
                result.stdOut.add(line);
            inputOut.close();

            if (result.code != 0) {
                BufferedReader inputErr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                while ((line = inputErr.readLine()) != null)
                    result.stdErr.add(line);
                inputErr.close();
            }

        } catch (Exception e) {
            result.stdErr.add(e.getLocalizedMessage());
        }

        return result;
    }

    public static Result doCommandWithResultByRoot(String cmd) {
        return doCommandWithResult("su -c \"" + cmd + "\""); // /system/bin/su or /system/xbin/su
    }
}
