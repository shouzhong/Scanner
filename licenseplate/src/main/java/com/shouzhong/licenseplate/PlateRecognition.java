package com.shouzhong.licenseplate;

import android.graphics.Bitmap;

public class PlateRecognition {

    static {
        System.loadLibrary("LicensePlate");
    }

    static native long initPlateRecognizer(String casacde_detection,
                                           String finemapping_prototxt, String finemapping_caffemodel,
                                           String segmentation_prototxt, String segmentation_caffemodel,
                                           String charRecognization_proto, String charRecognization_caffemodel,
                                           String segmentation_free_prototxt, String segmentation_free_caffemodel);

    static native void releasePlateRecognizer(long id);

    public static native String recognize(byte[] data, int width, int height, long id);

    public static native String recognizeBmp(Bitmap bmp, long id);

}
