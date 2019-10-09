package com.wintone.bankcard;

public class BankCardAPI {

    public native String GetBankInfo(String var1);

    public native String GetKernalVersion();

    public native int RecognizeNV21(byte[] var1, int var2, int var3, int[] var4, char[] var5, int var6, int[] var7, int[] var8);

    public native int RecognizeStreamNV21Ex(byte[] var1, int var2, int var3, int[] var4, char[] var5, int var6, int[] var7);

    public native void WTDetectFrameLines(byte[] var1, int var2, int var3, int var4, int[] var5);

    public native void WTGetCharPos(int var1, int[] var2);

    public native int WTInitCardKernal(String var1, int var2);

    public native int WTRecognizeImage(String var1, char[] var2, int var3, int[] var4);

    public native int WTRecognizeMemory(int[] var1, int var2, int var3, int var4, char[] var5, int var6, int[] var7);

    public native int WTRecognizeStreamNV21(byte[] var1, int var2, int var3, char[] var4, int var5, int[] var6);

    public native void WTSetROI(int[] var1, int var2, int var3);

    public native void WTUnInitCardKernal();

    static {
        System.loadLibrary("AndroidBankCard");
    }

}
