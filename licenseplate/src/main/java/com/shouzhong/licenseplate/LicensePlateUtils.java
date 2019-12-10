package com.shouzhong.licenseplate;

import android.content.Context;
import android.graphics.Bitmap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

public class LicensePlateUtils {

    public static final String ASSETS_DIR = "lpr";
    public static final String CASCADE_FILENAME = "cascade.xml";
    public static final String HORIZONAL_FINEMAPPING_PROTOTXT = "HorizonalFinemapping.prototxt";
    public static final String HORIZONAL_FINEMAPPING_CAFFEMODEL = "HorizonalFinemapping.caffemodel";
    public static final String SEGMENTATION_PROTOTXT = "Segmentation.prototxt";
    public static final String SEGMENTATION_CAFFEMODEL = "Segmentation.caffemodel";
    public static final String RECOGNIZATION_PROTOTXT = "CharacterRecognization.prototxt";
    public static final String RECOGNIZATION_CAFFEMODEL = "CharacterRecognization.caffemodel";
    public static final String FREE_INCEPTION_PROTOTXT = "SegmenationFree-Inception.prototxt";
    public static final String FREE_INCEPTION_CAFFEMODEL = "SegmenationFree-Inception.caffemodel";

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

    /**
     * 初始化识别资源
     *
     * @param context
     * @return
     */
    public static long initRecognizer(Context context) {
        String path = context.getExternalFilesDir(ASSETS_DIR).getAbsolutePath();
        String cascade_filename = path + File.separator + CASCADE_FILENAME;
        String finemapping_prototxt = path + File.separator + HORIZONAL_FINEMAPPING_PROTOTXT;
        String finemapping_caffemodel = path + File.separator + HORIZONAL_FINEMAPPING_CAFFEMODEL;
        String segmentation_prototxt = path + File.separator + SEGMENTATION_PROTOTXT;
        String segmentation_caffemodel = path + File.separator + SEGMENTATION_CAFFEMODEL;
        String character_prototxt = path + File.separator + RECOGNIZATION_PROTOTXT;
        String character_caffemodel = path + File.separator + RECOGNIZATION_CAFFEMODEL;
        String segmentation_free_prototxt = path + File.separator + FREE_INCEPTION_PROTOTXT;
        String segmentation_free_caffemodel = path + File.separator + FREE_INCEPTION_CAFFEMODEL;
        copyAssets(context, ASSETS_DIR, path);
        //调用JNI 加载资源函数
        return PlateRecognition.initPlateRecognizer(
                cascade_filename,
                finemapping_prototxt, finemapping_caffemodel,
                segmentation_prototxt, segmentation_caffemodel,
                character_prototxt, character_caffemodel,
                segmentation_free_prototxt, segmentation_free_caffemodel);
    }

    /**
     * 释放识别资源
     *
     * @param l
     */
    public static void releaseRecognizer(long l) {
        PlateRecognition.releasePlateRecognizer(l);
    }


    /**
     * 识别
     *
     * @param data
     * @param width
     * @param height
     * @param id
     * @return
     */
    public static String recognize(byte[] data, int width, int height, long id) {
        return PlateRecognition.recognize(data, width, height, id);
    }

    /**
     * 识别
     *
     * @param bmp
     * @param id
     * @return
     */
    public static String recognizeBmp(Bitmap bmp, long id) {
        return PlateRecognition.recognizeBmp(bmp, id);
    }
}