package com.shouzhong.scanner;

import android.hardware.Camera;

class CameraWrapper {
    public final Camera camera;
    public final int cameraId;

    private CameraWrapper(Camera camera, int cameraId) {
        this.camera = camera;
        this.cameraId = cameraId;
    }

    public static CameraWrapper getWrapper(Camera camera, int cameraId) {
        if (camera == null) {
            return null;
        } else {
            return new CameraWrapper(camera, cameraId);
        }
    }
}