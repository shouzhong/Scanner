package com.shouzhong.nsfw;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class NsfwUtils {

    /**
     * 获取识别器
     *
     * @param context
     * @return
     */
    public static Interpreter getInterpreter(Context context) {
        try {
            Interpreter.Options tfliteOptions = new Interpreter.Options();
            return new Interpreter(loadModelFile(context), tfliteOptions);
        } catch (Exception e) {}
        return null;
    }

    /**
     * 识别
     *
     * @param tflite
     * @param bitmap
     * @return
     */
    public static float decode(Interpreter tflite, Bitmap bitmap) {
        if (tflite == null || bitmap == null) return 0.0f;
        try {
            Bitmap temp = Bitmap.createScaledBitmap(squareBitmap(bitmap), 224, 224, true);
            ByteBuffer buf = convertBitmapToByteBuffer(temp);
            float[][] out = new float[1][2];
            tflite.run(buf, out);
            temp.recycle();
            buf.clear();
            return out[0][1];
        } catch (Exception e) {}
        return 0.0f;
    }

    /**
     * 释放
     *
     * @param tflite
     */
    public static void release(Interpreter tflite) {
        try {
            if (tflite != null) tflite.close();
        } catch (Exception e) {}
    }

    private static MappedByteBuffer loadModelFile(Context context) throws Exception {
        File file = new File(context.getExternalFilesDir("nsfw").getAbsolutePath() + "/nsfw.tflite");
        InputStream input = context.getAssets().open("nsfw.tflite");
        int len1 = input.available();
        int len2 = file.exists() ? (int) file.length() : 0;
        if (!file.exists() || !file.isFile() || len1 != len2) {
            if (file.exists()) file.delete();
            OutputStream output = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = input.read(buffer)) != -1) {
                output.write(buffer, 0, length);
            }
            output.close();
            input.close();
        }
        return new RandomAccessFile(file, "r").getChannel().map(FileChannel.MapMode.READ_ONLY, 0, len1);
    }

    private static ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int x = Math.max((w - 224) / 2, 0);
        int y = Math.max((h - 224) / 2, 0);
        int[] pixels = new int[224 * 224];
        ByteBuffer buf = ByteBuffer.allocateDirect(224 * 224 * 3 * 4);
        bitmap.getPixels(pixels, 0, 224, x, y, 224, 224);
        for (int color : pixels) {
            buf.putFloat(Color.blue(color) - 104);
            buf.putFloat(Color.green(color) - 117);
            buf.putFloat(Color.red(color) - 123);
        }
        return buf;
    }

    private static Bitmap squareBitmap(Bitmap bmp) {
        if (bmp.getWidth() == bmp.getHeight()) return bmp;
        int length = Math.max(bmp.getWidth(), bmp.getHeight());
        int padding = Math.abs(bmp.getWidth() - bmp.getHeight());
        // 背图
        Bitmap outBmp = Bitmap.createBitmap(length, length, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(outBmp);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        canvas.drawRGB(0, 0, 0);
        boolean b = bmp.getWidth() > bmp.getHeight();
        canvas.drawBitmap(bmp, b ? 0 : padding / 2, b ? padding / 2 : 0, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_ATOP));
        // 画正方形的
        canvas.drawRect(0, 0, length, length, paint);
        return outBmp;
    }
}
