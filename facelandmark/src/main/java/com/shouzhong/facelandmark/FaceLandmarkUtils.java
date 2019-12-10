package com.shouzhong.facelandmark;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import zeusees.tracking.Face;
import zeusees.tracking.FaceTracking;

public class FaceLandmarkUtils {

    /**
     * 初始化
     *
     * @param context
     * @return
     */
    public static long init(Context context, byte[] data, int width, int height) {
        String path = context.getExternalFilesDir("facelandmark").getAbsolutePath();
        copyAssets(context, "facelandmark", path);
        long session = FaceTracking.createSession(path);
        FaceTracking.initTracking(data, height, width, session);
        return session;
    }

    /**
     * 识别
     *
     * @param session
     * @param data
     * @param width
     * @param height
     * @return
     */
    public static List<Face> recognize(long session, byte[] data, int width, int height) {
        FaceTracking.update(data, height, width, session);
        int len = FaceTracking.getTrackingNum(session);
        if (len <= 0) return null;
        List<Face> list = new ArrayList<>();
        for (int i = 0; i < len; i++) {
            int[] landmarks = FaceTracking.getTrackingLandmarkByIndex(i, session);
            int[] faceRect = FaceTracking.getTrackingLocationByIndex(i, session);
            int[] attributes = FaceTracking.getAttributeByIndex(i, session);
            float[] eulerAngles = FaceTracking.getEulerAngleByIndex(i, session);
            int id = FaceTracking.getTrackingIDByIndex(i, session);
            list.add(new Face(id, faceRect, landmarks, attributes, eulerAngles));
        }
        return list;
    }

    /**
     * 释放内存
     *
     * @param session
     */
    public static void release(long session) {
        FaceTracking.releaseSession(session);
    }

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
}
