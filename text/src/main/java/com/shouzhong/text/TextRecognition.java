package com.shouzhong.text;

public class TextRecognition {

    static {
        System.loadLibrary("TextRecognition");
    }

    public native static String recognize(String path);
}
