package com.shouzhong.scanner;

import android.hardware.Camera;
import android.text.TextUtils;

import java.util.List;

/**
 * 相机工具类
 *
 */
class CameraUtils {

    private static final String TAG = "CameraUtils";

    /**
     * 返回第一个后置相机的id。若未找到后置相机，则返回-1
     **/
    static int getDefaultCameraId(int direction) {
        int numberOfCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == direction) return i;
//            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
//                return i;
//            }
        }
        return -1;
    }

    /**
     * 获取相机
     *
     * @param cameraId
     * @return
     */
    static Camera getCamera(int cameraId) {
        try {
            return cameraId == -1 ? Camera.open() : Camera.open(cameraId);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 是否支持闪光灯
     *
     * @param camera
     * @return
     */
    static boolean isFlashSupported(Camera camera) {
        if (camera == null) return false;
        try {
            Camera.Parameters parameters = camera.getParameters();
            if (parameters.getFlashMode() == null) return false;
            List<String> list = parameters.getSupportedFlashModes();
            if (list == null || list.size() == 0) return false;
            if (list.size() == 1 && TextUtils.equals(list.get(0), Camera.Parameters.FLASH_MODE_OFF)) return false;
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}