package com.shouzhong.licenseplate2;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import pr.platerecognization.PlateRecognition;

public class LicensePlate2Utils {

    public static final String ASSETS_DIR = "lpr2";
    public static final String CASCADE_XML = "cascade.xml";
    public static final String CASCADE_DOUBLE_XML = "cascade_double.xml";
    public static final String LPR_HKMC_MLZ = "LPR_hkmc.mlz";

    private static void copyAssets(Context context, String assetDir, String dir) {
        String[] files;
        try {
            // 获得Assets一共有几多文件
            files = context.getAssets().list(assetDir);
        } catch (IOException e1) {
            return;
        }
        File mWorkingPath = new File(dir);
        // 如果文件路径不存在
        if (!mWorkingPath.exists()) {
            // 创建文件夹
            if (!mWorkingPath.mkdirs()) {
                // 文件夹创建不成功时调用
            }
        }
        for (String file : files) {
            try {
                // 根据路径判断是文件夹还是文件
                if (!file.contains(".")) {
                    if (0 == assetDir.length()) {
                        copyAssets(context, file, dir + file + "/");
                    } else {
                        copyAssets(context, assetDir + "/" + file, dir + "/" + file + "/");
                    }
                    continue;
                }
                File outFile = new File(mWorkingPath, file);
                InputStream in;
                if (0 != assetDir.length()) {
                    in = context.getAssets().open(assetDir + "/" + file);
                } else {
                    in = context.getAssets().open(file);
                }
                int len1 = in.available();
                int len2 = outFile.exists() ? (int) outFile.length() : 0;
                if (outFile.exists() && len1 == len2) {
                    in.close();
                    continue;
                }
                if (outFile.exists()) outFile.delete();
                OutputStream out = new FileOutputStream(outFile);
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static long initRecognizer(Context context) {
        String path = context.getExternalFilesDir(ASSETS_DIR).getAbsolutePath();
        String cascade = path + File.separator + CASCADE_XML;
        String cascadeDouble = path + File.separator + CASCADE_DOUBLE_XML;
        String lprHkmc = path + File.separator + LPR_HKMC_MLZ;
        copyAssets(context, ASSETS_DIR, path);
        return PlateRecognition.InitPlateRecognizer(cascade, cascadeDouble, lprHkmc);
    }

    public static void releaseRecognizer(final long l) {
        PlateRecognition.ReleasePlateRecognizer(l);
    }

    public static String recognize(byte[] data, int width, int height, long id) {
        int w = width / 8 > height ? height * 8 : width;
        int h = width / 8 > height ? height : width / 8;
        String s = PlateRecognition.RunRecognitionAsRaw(data, height, width, id, 0.8f, h, w, 1);
        return s;
    }
}
