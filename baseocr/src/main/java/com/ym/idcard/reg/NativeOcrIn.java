package com.ym.idcard.reg;

public class NativeOcrIn {
    static {
        System.loadLibrary("ocr");
    }

    public static native int start(String pkg, String sha1, String key);

    public static native boolean res();
}
