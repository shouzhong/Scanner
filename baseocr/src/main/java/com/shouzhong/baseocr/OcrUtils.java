package com.shouzhong.baseocr;

import com.ym.idcard.reg.NativeOcrIn;

public class OcrUtils {

    private static final String PACKAGE_NAME = "com.tomcat.ocr.idcard";
    private static final String APP_SHA1 = "19:F4:E8:B7:A9:EE:A3:67:90:07:B0:C6:56:8D:AD:60:44:AB:6D:C9";
    private static final String OCR_API_KEY = "26f1f6a0d4d7cb0dd0e9b28f4cedef83";

    public static void init() {
        NativeOcrIn.start(PACKAGE_NAME, APP_SHA1, OCR_API_KEY);
    }
}
