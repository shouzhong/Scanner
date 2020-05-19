package com.shouzhong.scanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;

/**
 * nv21工具类
 *
 */
public class NV21 {

    private RenderScript rs;
    private ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
    private Type.Builder yuvType, rgbaType;
    private Allocation in, out;

    public NV21(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && context != null) {
            rs = RenderScript.create(context);
            yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
        }
    }

    /**
     * nv21转bitmap
     *
     * @param nv21
     * @param width
     * @param height
     * @return
     */
    public Bitmap nv21ToBitmap(byte[] nv21, int width, int height){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && rs != null && yuvToRgbIntrinsic != null) {
            if (yuvType == null){
                yuvType = new Type.Builder(rs, Element.U8(rs)).setX(nv21.length);
                in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);
                rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height);
                out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);
            }
            in.copyFrom(nv21);
            yuvToRgbIntrinsic.setInput(in);
            yuvToRgbIntrinsic.forEach(out);
            Bitmap bmpOut = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            out.copyTo(bmpOut);
            return bmpOut;
        } else return Utils.nv21ToBitmap(nv21, width, height);
    }

    /**
     * bitmap转nv21
     *
     * @param bitmap
     * @return
     */
    public byte[] bitmapToNv21(Bitmap bitmap) {
        return Utils.bitmapToNv21(bitmap);
    }
}
