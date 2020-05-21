package com.sanbu.board;

import android.os.Build;

import com.sanbu.tools.LogUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by Tuyj on 2017/12/11.
 */

public class Allwinner {

    private static final String Brand = "Allwinner";
    private static final String Model = "dolphin";

    private static final String SUNXI_USB_OTG = "/sys/devices/platform/sunxi_otg/otg_role";
    private static final String SUNXI_USB_UDC = "/sys/bus/platform/devices/sunxi_usb_udc/otg_role";

    private static final String USB_MODE_HOST = "1";
    private static final String USB_MODE_OTG = "2";

    public static boolean isProduct() {
        String brand = Build.BRAND;
        return brand.startsWith(Brand);
    }

    public static boolean isDolphin() {
        String brand = Build.BRAND;
        String model = Build.MODEL;
        return (brand.startsWith(Brand) && model.startsWith(Model));
    }

    public static void switchUsb2OTG() {
        switchUsbMode(USB_MODE_OTG);
    }

    public static void switchUsb2Host() {
        switchUsbMode(USB_MODE_HOST);
    }

    private static void switchUsbMode(String mode) {
        String usbRoleFile;
        if(new File(SUNXI_USB_OTG).exists())
            usbRoleFile = SUNXI_USB_OTG;
        else
            usbRoleFile = SUNXI_USB_UDC;

        try {
            String currentMode = currentUsbMode(usbRoleFile).replace("\r", "").replace("\n", "").replace(" ", "");
            if(currentMode.equals(mode))
                return;

            LogUtil.w("USB_SWITCH: try to set usb as " + mode);
            FileWriter fileWriter = new FileWriter(usbRoleFile);
            fileWriter.write(mode);
            fileWriter.close();
        } catch (IOException e) {
            LogUtil.e("USB_SWITCH: write file error:");
            e.printStackTrace();
        }
    }

    public static String currentUsbMode(String usbRoleFile) throws IOException {
        Process localProcess = Runtime.getRuntime().exec("cat " + usbRoleFile);
        try {
            if (localProcess.waitFor() != 0) {
                LogUtil.e("USB_SWITCH: get current usb mode fail! exit value = " + localProcess.exitValue());
                return null;
            }
        } catch (InterruptedException e) {
            LogUtil.e("USB_SWITCH: get current usb mode interrupted:");
            e.printStackTrace();
            return null;
        }

        BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(localProcess.getInputStream()));
        StringBuilder localStringBuilder = new StringBuilder("");
        while (true) {
            String str = localBufferedReader.readLine();
            if (str == null)
                break;
            localStringBuilder.append(str);
            localStringBuilder.append('\n');
        }
        LogUtil.i("USB_SWITCH: get current usb mode: " + localStringBuilder.toString());
        return localStringBuilder.toString();
    }
}
