package com.shouzhong.idcard;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import exocr.exocrengine.EXOCREngine;

public class IdCardUtils {

    /**
     * 识别
     *
     * @param image
     * @param width
     * @param height
     * @param result
     * @return
     * @throws Exception
     */
    public static int decode(byte[] image, int width, int height, byte[] result) throws Exception {
        int len = EXOCREngine.nativeRecoIDCardRawdat(image, width, height, width, 1, result, result.length);
        return len;
    }

    /**
     * 识别
     *
     * @param bmp
     * @param result
     * @return
     */
    public static int decode(Bitmap bmp, byte[] result) {
        int len = EXOCREngine.nativeRecoIDCardBitmap(bmp, result, result.length);
        return len;
    }

    /**
     * 字典初始化
     *
     * @param context
     * @return
     */
    public static final boolean initDict(final Context context) {
        File dir = context.getExternalFilesDir("idcard");
        if (dir.exists() && dir.isFile()) dir.delete();
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, "zocr0.lib");
        // step1: 检测字典是否存在
        boolean okFile = checkFile(context, file);
        if (!okFile) {
            clearDict();
            return false;
        }
        // step2: 检测字典是否正确
        boolean okDict = checkDict(dir.getAbsolutePath());
        if (!okDict) {
            clearDict();
            return false;
        }
        // step3: 检测字典签名
        return checkSign(context);
    }

    /**
     * 清除字典
     */
    public static final void clearDict() {
        int code = EXOCREngine.nativeDone();
        Log.e("kalu", "clearDict ==> code = " + code);
    }

    private static final boolean checkSign(final Context context) {
        int code = EXOCREngine.nativeCheckSignature(context);
        Log.e("kalu", "checkSign ==> code = " + code);
        return code == 1;
    }

    private static final boolean checkDict(final String path) {
        try {
            byte[] bytes = path.getBytes("UTF-8");
            int code = EXOCREngine.nativeInit(bytes);
            Log.e("kalu", "checkDict(UTF-8) ==> code = " + code);
            if (code < 0) {
                bytes = path.getBytes("GBK");
                code = EXOCREngine.nativeInit(bytes);
                Log.e("kalu", "checkDict(GBK) ==> code = " + code);
            }
            if (code < 0) {
                bytes = path.getBytes("GB2312");
                code = EXOCREngine.nativeInit(bytes);
                Log.e("kalu", "checkDict(GB2312) ==> code = " + code);
            }
            if (code < 0) {
                bytes = path.getBytes("UTF-16");
                code = EXOCREngine.nativeInit(bytes);
                Log.e("kalu", "checkDict(UTF-16) ==> code = " + code);
            }
            return code >= 0;
        } catch (Exception e) { }
        return false;
    }

    private static final boolean checkFile(final Context context, final File file) {
        try {
            //在assets资产目录下获取授权文件
            InputStream myInput = context.getAssets().open("zocr0.lib");
            int len1 = myInput.available();
            int len2 = file.exists() ? (int) file.length() : 0;
            if (file.exists() && len1 == len2) {
                myInput.close();
                return true;
            }
            if (file.exists()) file.delete();
            //将授权文件写到 data/data/包名 目录下
            OutputStream myOutput = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = myInput.read(buffer)) != -1) {
                myOutput.write(buffer, 0, length);
            }
            myOutput.close();
            myInput.close();
            return true;
        } catch (Exception e) {
            Log.e("===============", e.getMessage());
            return false;
        }
    }
}
