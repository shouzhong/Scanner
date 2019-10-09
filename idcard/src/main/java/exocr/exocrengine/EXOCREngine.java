package exocr.exocrengine;

import android.content.Context;
import android.graphics.Bitmap;

/**
 *
 */
public final class EXOCREngine {
    static {
        System.loadLibrary("exocrenginec");
    }

    //natives/////////////////////////////////////////////////////
    public static native int nativeGetVersion(byte[] exversion);

    public static native int nativeInit(byte[] dbpath);

    public static native int nativeDone();

    public static native int nativeCheckSignature(Context context);

    //////////////////////////////////////////////////////////////
    //IDCardRecognition
    public static native int nativeRecoIDCardBitmap(Bitmap bitmap, byte[] bresult, int maxsize);

    public static native Bitmap nativeRecoIDCardStillImage(Bitmap bitmap, int tryhard, int bwantimg, byte[] bresult, int maxsize, int[] rets);

    public static native Bitmap nativeRecoIDCardStillImageV2(Bitmap bitmap, int tryhard, int bwantimg, byte[] bresult, int maxsize, int[] rects, int[] rets);

    public static native int nativeRecoIDCardRawdat(byte[] imgdata, int width, int height, int pitch, int imgfmt, byte[] bresult, int maxsize);

    public static native Bitmap nativeGetIDCardStdImg(byte[] NV21, int width, int height, byte[] bresult, int maxsize, int[] rects);

    /////////////////////////////////////////////////////////////
    //VECardRecognition
    public static native int nativeRecoVECardBitmap(Bitmap bitmap, byte[] bresult, int maxsize);

    public static native Bitmap nativeRecoVECardStillImage(Bitmap bitmap, int tryhard, int bwantimg, byte[] bresult, int maxsize, int[] rets);

    public static native Bitmap nativeRecoVECardStillImageV2(Bitmap bitmap, int tryhard, int bwantimg, byte[] bresult, int maxsize, int[] rects, int[] rets);

    public static native int nativeRecoVECardRawdat(byte[] imgdata, int width, int height, int pitch, int imgfmt, byte[] bresult, int maxsize);

    public static native Bitmap nativeGetVECardStdImg(byte[] NV21, int width, int height, byte[] bresult, int maxsize, int[] rects);

    //new version api for VE 20160307
    public static native Bitmap nativeRecoVE2CardNV21(byte[] imgnv21, int width, int height, int bwantimg, byte[] bresult, int maxsize, int[] rects, int[] rets);

    public static native Bitmap nativeRecoVE2CardStillImage(Bitmap bitmap, int tryhard, int bwantimg, byte[] bresult, int maxsize, int[] rects, int[] rets);

    //Scan Line Recogition
    public static native int nativeRecoScanLineRawdata(byte[] imgdata, int width, int height, int imgfmt, int lft, int rgt, int top, int btm, int nRecoType, byte[] bresult, int maxsize);

    //DriveLisenseRecognition
    public static native Bitmap nativeRecoDRCardNV21(byte[] imgnv21, int width, int height, int bwantimg, byte[] bresult, int maxsize, int[] rects, int[] rets);

    public static native Bitmap nativeRecoDRCardStillImage(Bitmap bitmap, int tryhard, int bwantimg, byte[] bresult, int maxsize, int[] rects, int[] rets);
}