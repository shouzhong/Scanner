package com.shouzhong.scanner;

import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

class CameraHandlerThread extends HandlerThread {

    private ScannerView mScannerView;

    public CameraHandlerThread(ScannerView scannerView) {
        super("CameraHandlerThread");
        mScannerView = scannerView;
        start();
    }

    /**
     * 打开系统相机，并进行基本的初始化
     */
    public void startCamera(final int cameraId) {
        Handler localHandler = new Handler(getLooper());
        localHandler.post(new Runnable() {
            @Override
            public void run() {
                final Camera camera = CameraUtils.getCamera(cameraId);//打开camera
                if (camera != null) mScannerView.setOptimalPreviewSize(camera);
                Handler mainHandler = new Handler(Looper.getMainLooper());//切换到主线程
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mScannerView.setupCameraPreview(CameraWrapper.getWrapper(camera, cameraId));
                    }
                });
            }
        });
    }
}
