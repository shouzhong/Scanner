package com.shouzhong.scanner;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.shouzhong.bankcard.BankCardUtils;
import com.shouzhong.drivinglicense.DrivingLicenseUtils;
import com.shouzhong.idcard.IdCardUtils;
import com.shouzhong.idcard2.IdCard2Utils;
import com.shouzhong.licenseplate.LicensePlateUtils;
import com.wintone.bankcard.BankCardAPI;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 扫描页面
 *
 *
 */
public class ScannerView extends FrameLayout implements Camera.PreviewCallback, CameraPreview.FocusAreaSetter {

    public static final String TAG = "ScannerView";

    private CameraWrapper cameraWrapper;
    private IViewFinder viewFinder;
    private CameraPreview cameraPreview;
    private Rect scaledRect;
    private ArrayList<Camera.Area> focusAreas;
    private CameraHandlerThread cameraHandlerThread;
    private boolean shouldAdjustFocusArea;//是否需要自动调整对焦区域
    private MultiFormatReader multiFormatReader;
    private ImageScanner imageScanner;
    private Map<DecodeHintType, Object> hints0;
    private BankCardAPI bankCardAPI;
    private Callback callback;
    private IScanner scanner;
    private int cameraDirection = Camera.CameraInfo.CAMERA_FACING_BACK;
    private int[] previewSize;
    private boolean isSaveBmp;
    private boolean isRotateDegree90Recognition = false;
    private boolean enableZXing = false;
    private boolean enableZBar = false;
    private boolean enableQrcode = true;
    private boolean enableBarcode = true;
    private boolean enableBankCard = false;
    private boolean enableIdCard = false;
    private boolean enableLicensePlate = false;
    private boolean enableIdCard2 = false;
    private boolean enableDrivingLicense = false;
    private boolean isIdCardInit = false;
    private boolean isIdCard2Init = false;
    private boolean isDrivingLicenseInit = false;
    private long licensePlateId;

    public ScannerView(Context context) {
        this(context, null);
    }

    public ScannerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScannerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Called as preview frames are displayed.<br/>
     * This callback is invoked on the event thread open(int) was called from.<br/>
     * (此方法与Camera.open运行于同一线程，在本项目中，就是CameraHandlerThread线程)
     */
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (callback == null) return;
        try {
            int previewWidth = previewSize[0];
            int previewHeight = previewSize[1];
            int  rotationCount = getRotationCount();
            boolean isRotated = rotationCount == 1 || rotationCount == 3;
            //根据预览页面尺寸和preview的尺寸之比，缩放扫码区域
            Rect rect = getScaledRect(previewWidth, previewHeight);
            byte[] tempData = null;
            byte[] tempData2 = null;
            int width = 0;
            int height = 0;
            int width2 = 0;
            int height2 = 0;
            int w = rect.width() / 2 * 2;
            int h = rect.height() / 2 * 2;
            byte[] temp = Utils.clipNV21(data, previewWidth, previewHeight, rect.left, rect.top, w, h);
            if (isRotated) {
                tempData = Utils.rotateNV21Degree90(temp, w, h);
                width = h;
                height = w;
                if (isRotateDegree90Recognition) {
                    tempData2 = temp;
                    width2 = w;
                    height2 = h;
                }
            } else {
                tempData = temp;
                width = w;
                height = h;
                if (isRotateDegree90Recognition) {
                    tempData2 = Utils.rotateNV21Degree90(temp, w, h);
                    width2 = h;
                    height2 = w;
                }
            }
            Result result = null;
            if (scanner != null && result == null) {
                try {
                    result = scanner.scan(tempData, width, height);
                } catch (Exception e) {}
                if (result == null && isRotateDegree90Recognition) {
                    try {
                        result = scanner.scan(tempData2, width2, height2);
                    } catch (Exception e) {}
                }
            }
            if (enableZBar && result == null && (enableBarcode || enableQrcode)) {
                try {
                    Image barcode = new Image(width, height, "Y800");
                    barcode.setData(tempData);
//                    barcode.setCrop(rect.left, rect.top, rect.width(), rect.height());
                    int code = getImageScanner().scanImage(barcode);
                    if (code == 0) throw new Exception("failure");
                    // 识别成功
                    SymbolSet syms = getImageScanner().getResults();
                    for (Symbol sym : syms) {
                        final String s = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT ? new String(sym.getDataBytes(), StandardCharsets.UTF_8) : sym.getData();
                        if (!TextUtils.isEmpty(s)) {
                            result = new Result();
                            result.type = Result.TYPE_CODE;
                            result.data = s;
                            break;
                        }
                    }
                } catch (Exception e) {}
            }
            if (enableZXing && result == null && (enableBarcode || enableQrcode)) {
                try {
                    PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(tempData, width, height, 0, 0, width, height, false);
                    String s = getMultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(source)), hints0).getText();
                    result = new Result();
                    result.type = Result.TYPE_CODE;
                    result.data = s;
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
                if (result == null && isRotateDegree90Recognition) {
                    try {
                        PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(tempData2, width2, height2, 0, 0, width2, height2, false);
                        String s = getMultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(source)), hints0).getText();
                        result = new Result();
                        result.type = Result.TYPE_CODE;
                        result.data = s;
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage());
                    }
                }
            }
            if (enableBankCard && result == null) {
                try {
                    String s = BankCardUtils.decode(getBankCardAPI(), tempData, width, height);
                    if (!TextUtils.isEmpty(s)) {
                        result = new Result();
                        result.type = Result.TYPE_BANK_CARD;
                        result.data = s;
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
                if (result == null && isRotateDegree90Recognition) {
                    try {
                        String s = BankCardUtils.decode(getBankCardAPI(), tempData2, width2, height2);
                        if (!TextUtils.isEmpty(s)) {
                            result = new Result();
                            result.type = Result.TYPE_BANK_CARD;
                            result.data = s;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage());
                    }
                }
            }
            if (enableIdCard && result == null) {
                try {
                    if (!isIdCardInit) isIdCardInit = IdCardUtils.initDict(getContext());
                    if (!isIdCardInit) throw new Exception("failure");
                    byte[] obtain = new byte[4396];
                    int len = IdCardUtils.decode(tempData, width, height, obtain);
                    if (len > 0) result = Utils.decodeIdCard(obtain, len);
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
                if (result == null && isRotateDegree90Recognition) {
                    try {
                        if (!isIdCardInit) isIdCardInit = IdCardUtils.initDict(getContext());
                        if (!isIdCardInit) throw new Exception("failure");
                        byte[] obtain = new byte[4396];
                        int len = IdCardUtils.decode(tempData2, width2, height2, obtain);
                        if (len > 0) result = Utils.decodeIdCard(obtain, len);
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage());
                    }
                }
            }
            if (enableLicensePlate && result == null) {
                try {
                    String s = LicensePlateUtils.recognize(tempData, width, height, getLicensePlateId());
                    if (!TextUtils.isEmpty(s)) {
                        result = new Result();
                        result.type = Result.TYPE_LICENSE_PLATE;
                        result.data = s;
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
                if (result == null && isRotateDegree90Recognition) {
                    try {
                        String s = LicensePlateUtils.recognize(tempData2, width2, height2, getLicensePlateId());
                        if (!TextUtils.isEmpty(s)) {
                            result = new Result();
                            result.type = Result.TYPE_LICENSE_PLATE;
                            result.data = s;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage());
                    }
                }
            }
            if (enableIdCard2 && result == null) {
                try {
                    if (!isIdCard2Init) isIdCard2Init = true;
                    String s = IdCard2Utils.decode(tempData, width, height);
                    if (!TextUtils.isEmpty(s)) {
                        result = new Result();
                        result.type = s.contains("cardNumber") ? Result.TYPE_ID_CARD_FRONT : Result.TYPE_ID_CARD_BACK;
                        result.data = s;
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
                if (result == null && isRotateDegree90Recognition) {
                    try {
                        String s = IdCard2Utils.decode(tempData2, width2, height2);
                        if (!TextUtils.isEmpty(s)) {
                            result = new Result();
                            result.type = s.contains("cardNumber") ? Result.TYPE_ID_CARD_FRONT : Result.TYPE_ID_CARD_BACK;
                            result.data = s;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage());
                    }
                }
            }
            if (enableDrivingLicense && result == null) {
                try {
                    if (!isDrivingLicenseInit) isDrivingLicenseInit = true;
                    String s = DrivingLicenseUtils.decode(tempData, width, height);
                    if (!TextUtils.isEmpty(s)) {
                        result = new Result();
                        result.type = Result.TYPE_DRIVING_LICENSE;
                        result.data = s;
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
                if (result == null && isRotateDegree90Recognition) {
                    try {
                        String s = DrivingLicenseUtils.decode(tempData2, width2, height2);
                        if (!TextUtils.isEmpty(s)) {
                            result = new Result();
                            result.type = Result.TYPE_DRIVING_LICENSE;
                            result.data = s;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage());
                    }
                }
            }
            if (result == null) {
                getOneMoreFrame();
                return;
            }
            if (isSaveBmp) {
                Bitmap bmp = Utils.nv21ToBitmap(data, previewWidth, previewHeight);
                bmp= Bitmap.createBitmap(bmp, rect.left, rect.top, rect.width(), rect.height());
                if (rotationCount != 0) {
                    // 旋转图片
                    Matrix m = new Matrix();
                    m.setRotate(rotationCount * 90, (float) bmp.getWidth() / 2, (float) bmp.getHeight() / 2);
                    bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
                }
                result.path = Utils.saveBitmap(getContext(), bmp);
                if (TextUtils.isEmpty(result.path)) {
                    getOneMoreFrame();
                    return;
                }
            }
            final Result result1 = result;
            post(new Runnable() {//切换到主线程
                @Override
                public void run() {
                    if (callback != null) callback.result(result1);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            getOneMoreFrame();
        }
    }

    @Override
    public void setAutoFocusArea() {
        //设置对焦区域
        if (!shouldAdjustFocusArea || cameraWrapper == null) return;
        try {
            Camera.Parameters parameters = cameraWrapper.camera.getParameters();
            if (parameters == null || parameters.getMaxNumFocusAreas() <= 0) {
                Log.e(TAG, "不支持设置对焦区域");
                return;
            }
            if (focusAreas == null) {
                int width = 2000, height = 2000;
                Rect framingRect = viewFinder.getFramingRect();//获得扫码框区域
                if (framingRect == null) return;
                int w = getWidth();
                int h = getHeight();
                //1.根据预览页面尺寸和2000*2000的尺寸之比，缩放对焦区域
                Rect scaledRect = new Rect(framingRect);
                scaledRect.left = scaledRect.left * width / w;
                scaledRect.right = scaledRect.right * width / w;
                scaledRect.top = scaledRect.top * height / h;
                scaledRect.bottom = scaledRect.bottom * height / h;
                //2.旋转对焦区域
                Rect rotatedRect = new Rect(scaledRect);
                int rotationCount = getRotationCount();
                if (rotationCount == 1) {//若相机图像需要顺时针旋转90度，则将扫码框逆时针旋转90度
                    rotatedRect.left = scaledRect.top;
                    rotatedRect.top = 2000 - scaledRect.right;
                    rotatedRect.right = scaledRect.bottom;
                    rotatedRect.bottom = 2000 - scaledRect.left;
                } else if (rotationCount == 2) {//若相机图像需要顺时针旋转180度,则将扫码框逆时针旋转180度
                    rotatedRect.left = 2000 - scaledRect.right;
                    rotatedRect.top = 2000 - scaledRect.bottom;
                    rotatedRect.right = 2000 - scaledRect.left;
                    rotatedRect.bottom = 2000 - scaledRect.top;
                } else if (rotationCount == 3) {//若相机图像需要顺时针旋转270度，则将扫码框逆时针旋转270度
                    rotatedRect.left = 2000 - scaledRect.bottom;
                    rotatedRect.top = scaledRect.left;
                    rotatedRect.right = 2000 - scaledRect.top;
                    rotatedRect.bottom = scaledRect.right;
                }
                //3.坐标系平移
                Rect rect = new Rect(rotatedRect.left - 1000, rotatedRect.top - 1000, rotatedRect.right - 1000, rotatedRect.bottom - 1000);
                Camera.Area area = new Camera.Area(rect, 1000);
                focusAreas = new ArrayList<>();
                focusAreas.add(area);
            }
            parameters.setFocusAreas(focusAreas);
            cameraWrapper.camera.setParameters(parameters);
        } catch (Exception e) {}
    }

    // ******************************************************************************
    //
    // ******************************************************************************

    /**
     * 回调
     *
     * @param callback
     */
    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    /**
     * 扫描区域
     *
     * @param viewFinder
     */
    public void setViewFinder(IViewFinder viewFinder) {
        this.viewFinder = viewFinder;
    }

    /**
     * 摄像头方向
     *
     * @param direction Camera.CameraInfo.CAMERA_FACING_BACK or Camera.CameraInfo.CAMERA_FACING_FRONT
     */
    public void setCameraDirection(int direction) {
        this.cameraDirection = direction;
    }

    /**
     * 开启扫描
     *
     */
    public void onResume() {
        startCamera();
    }

    /**
     * 停止扫描
     *
     */
    public void onPause() {
        stopCamera();
    }


    /**
     * 重启
     *
     * @param delayMillis
     */
    public void restartPreviewAfterDelay(long delayMillis) {
        postDelayed(new Runnable() {
            @Override
            public void run() {
                getOneMoreFrame();
            }
        }, delayMillis);
    }

    /**
     * 开启/关闭闪光灯
     */
    public void setFlash(boolean flag) {
        if (cameraWrapper == null || !CameraUtils.isFlashSupported(cameraWrapper.camera)) return;
        Camera.Parameters parameters = cameraWrapper.camera.getParameters();
        if (TextUtils.equals(parameters.getFlashMode(), Camera.Parameters.FLASH_MODE_TORCH) && flag)
            return;
        if (TextUtils.equals(parameters.getFlashMode(), Camera.Parameters.FLASH_MODE_OFF) && !flag)
            return;
        parameters.setFlashMode(flag ? Camera.Parameters.FLASH_MODE_TORCH : Camera.Parameters.FLASH_MODE_OFF);
        cameraWrapper.camera.setParameters(parameters);
    }

    /**
     * 切换闪光灯的点亮状态
     */
    public void toggleFlash() {
        if (cameraWrapper == null || !CameraUtils.isFlashSupported(cameraWrapper.camera)) return;
        Camera.Parameters parameters = cameraWrapper.camera.getParameters();
        if (TextUtils.equals(parameters.getFlashMode(), Camera.Parameters.FLASH_MODE_TORCH)) {
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        } else {
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        }
        cameraWrapper.camera.setParameters(parameters);
    }

    /**
     * 闪光灯是否被点亮
     */
    public boolean isFlashOn() {
        if (cameraWrapper == null || !CameraUtils.isFlashSupported(cameraWrapper.camera)) return false;
        Camera.Parameters parameters = cameraWrapper.camera.getParameters();
        return TextUtils.equals(parameters.getFlashMode(), Camera.Parameters.FLASH_MODE_TORCH);
    }

    /**
     * 设置是否要根据扫码框的位置去调整对焦区域的位置<br/>
     * 默认值为false，即不调整，会使用系统默认的配置，那么对焦区域会位于预览画面的中央<br/>
     * <br/>
     * (经测试，此功能对少数机型无效，待优化)
     */
    public void setShouldAdjustFocusArea(boolean boo) {
        this.shouldAdjustFocusArea = boo;
    }

    /**
     * 是否保存图片
     *
     * @param boo
     */
    public void setSaveBmp(boolean boo) {
        this.isSaveBmp = boo;
    }

    /**
     * 是否在原来识别的图像基础上旋转90度继续识别
     *
     * @param boo
     */
    public void setRotateDegree90Recognition(boolean boo) {
        this.isRotateDegree90Recognition = boo;
    }

    /**
     * 是否使用ZXing识别
     *
     * @param boo
     */
    public void setEnableZXing(boolean boo) {
        this.enableZXing = boo;
    }

    /**
     * 是否使用ZBar识别
     *
     * @param boo
     */
    public void setEnableZBar(boolean boo) {
        this.enableZBar = boo;
    }

    /**
     * 是否启用二维码识别
     *
     * @param boo
     */
    public void setEnableQrcode(boolean boo) {
        if (boo == enableQrcode) return;
        this.enableQrcode = boo;
        hints0 = null;
    }

    /**
     * 是否启用条码识别
     *
     * @param boo
     */
    public void setEnableBarcode(boolean boo) {
        if (boo == enableBarcode) return;
        this.enableBarcode = boo;
        imageScanner = null;
    }

    /**
     * 是否使用银行卡识别
     *
     * @param boo
     */
    public void setEnableBankCard(boolean boo) {
        this.enableBankCard = boo;
    }

    /**
     * 是否使用身份证识别
     *
     * @param boo
     */
    public void setEnableIdCard(boolean boo) {
        this.enableIdCard = boo;
    }

    /**
     * 是否使用车牌识别
     *
     * @param boo
     */
    public void setEnableLicensePlate(boolean boo) {
        this.enableLicensePlate = boo;
    }

    /**
     * 是否使用身份证识别（第二种方式）
     *
     * @param boo
     */
    public void setEnableIdCard2(boolean boo) {
        this.enableIdCard2 = boo;
    }

    /**
     * 是否使用驾驶证识别
     *
     * @param boo
     */
    public void setEnableDrivingLicense(boolean boo) {
        this.enableDrivingLicense = boo;
    }

    /**
     * 设置识别器
     *
     * @param scanner
     */
    public void setScanner(IScanner scanner) {
        this.scanner = scanner;
    }

    // ******************************************************************************
    //
    // ******************************************************************************

    /**
     * 再获取一帧图像数据进行识别（会再次触发onPreviewFrame方法）
     */
    private void getOneMoreFrame() {
        if (cameraWrapper != null) {
            try {
                cameraWrapper.camera.setOneShotPreviewCallback(this);
            } catch (Exception e) {}
        }
    }

    /**
     * 创建zxing识别器
     */
    private synchronized MultiFormatReader getMultiFormatReader() {
        if (multiFormatReader == null) {
            multiFormatReader = new MultiFormatReader();
        }
        if (hints0 == null) {
            hints0 = new HashMap<>();
            List<BarcodeFormat> decodeFormats = new ArrayList<>();
            if (enableQrcode) {
                decodeFormats.add(BarcodeFormat.QR_CODE);
            }
            if (enableBarcode) {
                decodeFormats.add(BarcodeFormat.CODABAR);
                decodeFormats.add(BarcodeFormat.CODE_39);
                decodeFormats.add(BarcodeFormat.CODE_93);
                decodeFormats.add(BarcodeFormat.CODE_128);
                decodeFormats.add(BarcodeFormat.EAN_8);
                decodeFormats.add(BarcodeFormat.EAN_13);
                decodeFormats.add(BarcodeFormat.UPC_A);
                decodeFormats.add(BarcodeFormat.UPC_E);
                decodeFormats.add(BarcodeFormat.ITF);
                decodeFormats.add(BarcodeFormat.RSS_14);
            }
            hints0.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
            if (enableQrcode) {
                hints0.put(DecodeHintType.TRY_HARDER, BarcodeFormat.QR_CODE);
            } else if (enableBarcode) {
                hints0.put(DecodeHintType.TRY_HARDER, BarcodeFormat.CODE_128);
            }
            hints0.put(DecodeHintType.CHARACTER_SET, "utf-8");
        }
        return multiFormatReader;
    }

    /**
     * 创建zbar识别器
     *
     * @return
     */
    private synchronized ImageScanner getImageScanner() {
        if (imageScanner == null) {
            imageScanner = new ImageScanner();
            imageScanner.setConfig(0, Config.ENABLE, 0);
            if (enableQrcode) {
                imageScanner.setConfig(Symbol.QRCODE, Config.ENABLE, 1);
            }
            if (enableBarcode) {
                imageScanner.setConfig(Symbol.CODABAR, Config.ENABLE, 1);
                imageScanner.setConfig(Symbol.CODE39, Config.ENABLE, 1);
                imageScanner.setConfig(Symbol.CODE93, Config.ENABLE, 1);
                imageScanner.setConfig(Symbol.CODE128, Config.ENABLE, 1);
                imageScanner.setConfig(Symbol.EAN8, Config.ENABLE, 1);
                imageScanner.setConfig(Symbol.EAN13, Config.ENABLE, 1);
                imageScanner.setConfig(Symbol.UPCA, Config.ENABLE, 1);
                imageScanner.setConfig(Symbol.UPCE, Config.ENABLE, 1);
            }
        }
        return imageScanner;
    }

    /**
     * 创建银行卡识别器
     *
     * @return
     */
    private synchronized BankCardAPI getBankCardAPI() {
        if (bankCardAPI == null) {
            bankCardAPI = BankCardUtils.init();
        }
        return bankCardAPI;
    }

    /**
     * 初始化车牌识别
     *
     */
    private synchronized long getLicensePlateId() {
        if (licensePlateId == 0) licensePlateId = LicensePlateUtils.initRecognizer(getContext());
        return licensePlateId;
    }

    void setCameraWrapper(CameraWrapper cameraWrapper) {
        this.cameraWrapper = cameraWrapper;
    }

    void setupCameraPreview() {
        if (this.cameraWrapper == null) return;
        removeAllViews();
        cameraPreview = new CameraPreview(getContext(), previewSize[0], previewSize[1], cameraWrapper, this, this);
        addView(cameraPreview);
        if (viewFinder instanceof View) addView(((View) viewFinder));
    }

    /**
     * 打开系统相机，并进行基本的初始化
     */
    private void startCamera() {
        if (cameraHandlerThread == null) {
            cameraHandlerThread = new CameraHandlerThread(this);
        }
        cameraHandlerThread.startCamera(CameraUtils.getDefaultCameraId(cameraDirection));
    }

    /**
     * 释放相机资源等各种资源
     */
    private void stopCamera() {
        if (cameraHandlerThread != null) {
            try {
                cameraHandlerThread.quit();
                cameraHandlerThread = null;
            } catch (Exception e) {}
        }
        if (cameraWrapper != null) {
            try {
                cameraPreview.stopCameraPreview();//停止相机预览并置空各种回调
                cameraPreview = null;
                cameraWrapper.camera.release();//释放资源
                cameraWrapper = null;
            } catch (Exception e) {}
        }
        previewSize = null;
        scaledRect = null;
        focusAreas = null;
        multiFormatReader = null;
        imageScanner = null;
        if (bankCardAPI != null) {
            try {
                BankCardUtils.release(bankCardAPI);
            } catch (Exception e) {}
            bankCardAPI = null;
        }
        if (isIdCardInit) {
            try {
                IdCardUtils.clearDict();
            } catch (Exception e) {}
            isIdCardInit = false;
        }
        if (licensePlateId != 0) {
            try {
                LicensePlateUtils.releaseRecognizer(licensePlateId);
            } catch (Exception e) {}
            licensePlateId = 0;
        }
        if (isIdCard2Init) {
            try {
                IdCard2Utils.close();
            } catch (Exception e) {}
            isIdCard2Init = false;
        }
        if (isDrivingLicenseInit) {
            try {
                DrivingLicenseUtils.close();
            } catch (Exception e) {}
            isDrivingLicenseInit = false;
        }
        removeAllViews();
    }

    /**
     * 根据预览页面尺寸和preview的尺寸之比，缩放扫码区域
     */
    private Rect getScaledRect(int previewWidth, int previewHeight) {
        if (scaledRect == null) {
            Rect framingRect = viewFinder.getFramingRect();//获得扫码框区域
            int w = getWidth();
            int h = getHeight();
            scaledRect = new Rect(framingRect);
            Point p = new Point();
            ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getSize(p);
            int o = p.x == p.y ? 0 : p.x < p.y ? Configuration.ORIENTATION_PORTRAIT : Configuration.ORIENTATION_LANDSCAPE;
            float ratio = o == Configuration.ORIENTATION_PORTRAIT ? previewHeight * 1f / previewWidth : previewWidth * 1f / previewHeight;
            float r = w * 1f / h;
            if (ratio < r){
                int width = o == Configuration.ORIENTATION_PORTRAIT ? previewHeight : previewWidth;
                scaledRect.left = scaledRect.left * width / w;
                scaledRect.right = scaledRect.right * width / w;
                scaledRect.top = scaledRect.top * width / w;
                scaledRect.bottom = scaledRect.bottom * width / w;
            } else {
                int height = o == Configuration.ORIENTATION_PORTRAIT ? previewWidth : previewHeight;
                scaledRect.left = scaledRect.left * height / h;
                scaledRect.right = scaledRect.right * height / h;
                scaledRect.top = scaledRect.top * height / h;
                scaledRect.bottom = scaledRect.bottom * height / h;
            }
            int rotationCount = getRotationCount();
            int left = scaledRect.left;
            int top = scaledRect.top;
            int right = scaledRect.right;
            int bottom = scaledRect.bottom;
            if (rotationCount == 1) {
                scaledRect.left = top;
                scaledRect.top = previewHeight - right;
                scaledRect.right = bottom;
                scaledRect.bottom = previewHeight - left;
            } else if (rotationCount == 2) {
                scaledRect.left = previewWidth - right;
                scaledRect.top = previewHeight - bottom;
                scaledRect.right = previewWidth - left;
                scaledRect.bottom = previewHeight - top;
            } else if (rotationCount == 3) {
                scaledRect.left = previewWidth - bottom;
                scaledRect.top = left;
                scaledRect.right = previewWidth - top;
                scaledRect.bottom = right;
            }
            if (scaledRect.left < 0)  scaledRect.left = 0;
            if (scaledRect.top < 0) scaledRect.top = 0;
            if (scaledRect.right > previewWidth) scaledRect.right = previewWidth;
            if (scaledRect.bottom > previewHeight) scaledRect.bottom = previewHeight;
        }
        return scaledRect;
    }

    /**
     * 获取（旋转角度/90）
     */
    private int getRotationCount() {
        int displayOrientation = cameraPreview.getDisplayOrientation();
        return displayOrientation / 90;
    }

    /**
     * 找到一个合适的previewSize（根据控件的尺寸）
     *
     */
    void setOptimalPreviewSize() {
        if (previewSize != null || cameraWrapper == null) return;
        int w, h;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            w = getMeasuredWidth();
            h = getMeasuredHeight();
        } else {
            w = getMeasuredHeight();
            h = getMeasuredWidth();
        }
        try {
            if (cameraWrapper.camera == null) throw new NullPointerException("camera is null");
            //相机图像默认都是横屏(即宽>高)
            List<Camera.Size> sizes = cameraWrapper.camera.getParameters().getSupportedPreviewSizes();
            if (sizes == null) throw new NullPointerException("size is null");
            double targetRatio = w * 1.0 / h;
            Camera.Size optimalSize = null;
            double minDiff = Double.MAX_VALUE;
            double aspectTolerance = Double.MAX_VALUE;
            int targetHeight = h;
            // 获取最佳尺寸
            for (Camera.Size size : sizes) {
                double ratio = size.width * 1.0 / size.height;
                if (Math.abs(ratio - targetRatio) > aspectTolerance) continue;
                if (Math.abs(size.height - targetHeight) <= minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                    aspectTolerance = Math.abs(ratio - targetRatio);
                }
            }
            previewSize = new int[] {optimalSize.width, optimalSize.height};
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
            int width = dm.widthPixels > dm.heightPixels ? dm.widthPixels : dm.heightPixels;	// 屏幕宽度
            int height = dm.widthPixels > dm.heightPixels ? dm.heightPixels : dm.widthPixels;	// 屏幕高度
            if (w * 1.0f / h <= 1.0f)
            previewSize = w * 1.0f / h > 1.0f ? new int[] {width , height} : new int[] {height, height};
        }
    }
}