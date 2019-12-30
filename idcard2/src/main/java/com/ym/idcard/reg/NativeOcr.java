package com.ym.idcard.reg;

import com.shouzhong.baseocr.OcrUtils;

public class NativeOcr {
    static {
        System.loadLibrary("IDCardengine");
    }

    public native int freeImage(long paramLong, long[] paramArrayOfLong);

    public native int closeOCR(long[] paramArrayOfLong);

    public native long GetCardNum(byte[] paramArrayOfByte, int paramInt);

    public native long GetTrnImageThread();

    public native int GetCardNumRectThread(int[] paramArrayOfInt);

    public native int GetCharInfoThread(int[] paramArrayOfInt, int paramInt);

    public native int GetCardBinInfo(long paramLong, byte[] paramArrayOfByte, int paramInt);

    public native int LicenseStr(byte[] paramArrayOfByte);

    public native int RecYuvImg(byte[] paramArrayOfByte1, int paramInt1, int paramInt2, int[] paramArrayOfInt, byte[] paramArrayOfByte2);

    public native int GetResult(byte[] paramArrayOfByte, int paramInt);

    public native int startBCR(long[] paramArrayOfLong, byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, int paramInt, byte[] paramArrayOfByte3);

    public native int startOCR(long[] paramArrayOfLong, byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, int paramInt1, int paramInt2);

    public native int startBCRBeiJing(long[] paramArrayOfLong, byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, int paramInt, byte[] paramArrayOfByte3);

    public native int closeBCR(long[] paramArrayOfLong);

    public native int doImageBCR(long paramLong1, long paramLong2, long[] paramArrayOfLong);

    public native int doLineOCR(long paramLong1, long paramLong2, long[] paramArrayOfLong, byte[] paramArrayOfByte, int paramInt);

    public native int imageChecking(long paramLong1, long paramLong2, int paramInt);

    public native int checkingCopyID(long paramLong);

    public native void freeBField(long paramLong1, long paramLong2, int paramInt);

    public native void setProgressFunc(long paramLong, boolean paramBoolean);

    public native long loadImageMem(long paramLong1, long paramLong2, int paramInt1, int paramInt2, int paramInt3);

    public native int getFieldId(long paramLong);

    public native int getFieldText(long paramLong, byte[] paramArrayOfByte, int paramInt);

    public native long getNextField(long paramLong);

    public native int getFieldRect(long paramLong, int[] paramArrayOfInt);

    public native int codeConvert(long paramLong, byte[] paramArrayOfByte, int paramInt);

    public native int setoption(long paramLong, byte[] paramArrayOfByte1, byte[] paramArrayOfByte2);

    public native int getLastErr(long paramLong, byte[] paramArrayOfByte, int paramInt);

    public native long DupImage(long paramLong, int[] paramArrayOfInt);

    public native long getheadImgRect(long paramLong, int[] paramArrayOfInt);

    public native long SaveImage(long paramLong, byte[] paramArrayOfByte);

    public native int GetCardType(long paramLong1, long paramLong2);

    public native long GetHeadInfo(int[] paramArrayOfInt);

    public native int getheadImg(long paramLong1, long paramLong2, byte[] paramArrayOfByte);

    public native int ClearAll(int paramInt);

    public native int startBCR(long[] paramArrayOfLong, byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, int paramInt);

    public native long getYData(byte[] paramArrayOfByte, int paramInt1, int paramInt2);

    public native byte CheckCardEdgeLine(long paramLong1, long paramLong2, int[] paramArrayOfInt, int paramInt1, int paramInt2, int paramInt3, int paramInt4);

    public native byte[] BImageToImagebyte(long paramLong);

    public native int BImageToImagebyte(long paramLong, byte[] paramArrayOfByte);

    public native long YuvToRgb(byte[] paramArrayOfByte, int paramInt1, int paramInt2, int paramInt3);

    public native long FreeRgb(long paramLong);

    public native long SetSwitch(long paramLong, int paramInt1, int paramInt2);

    private static int GetState(int paramInt) {
        if (NativeOcrIn.res()) {
            if (paramInt == 1) {
                try {
                    byte[] bytes = new byte[1024];
                    getInstance().GetResult(bytes, 1024);
                    int end = 0;
                    for (int i = 0; i < 1024; i++) {
                        if (bytes[i] == 0) {
                            end = i;
                            break;
                        }
                    }
                    getInstance().result = new String(bytes, 0, end, "GBK");
                } catch (Exception e) {}
            }
        }
        return 1234;
    }

    private static NativeOcr instance;

    private String result;
    private byte[] license;
    private int[] rect;

    private NativeOcr() {
        OcrUtils.init();
        license = new byte[256];
        rect = new int[4];
    }

    public static NativeOcr getInstance() {
        if (instance == null) {
            synchronized (NativeOcr.class) {
                if (instance == null) instance = new NativeOcr();
            }
        }
        return instance;
    }

    public void decode(byte[] data, int width, int height) {
        rect[2] = width;
        rect[3] = height;
        RecYuvImg(data, width, height, rect, license);
    }

    public String getResult() {
        String s = result;
        result = null;
        return s;
    }

    public static void close() {
        if (instance == null) return;
        instance.ClearAll(1);
        instance = null;
    }
}
