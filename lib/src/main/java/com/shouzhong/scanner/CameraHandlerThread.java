package com.shouzhong.scanner;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

/**
 * 相机线程
 *
 */
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
                mScannerView.setCameraWrapper(CameraWrapper.getWrapper(CameraUtils.getCamera(cameraId), cameraId));
                Handler mainHandler = new Handler(Looper.getMainLooper());//切换到主线程
                mainHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mScannerView.setOptimalPreviewSize();
                            mScannerView.setupCameraPreview();
                        } catch (Exception e) {}
                    }
                }, 50);
            }
        });
    }
}
