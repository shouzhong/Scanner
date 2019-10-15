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
import com.shouzhong.idcard.IdCardUtils;
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
    private IViewFinder viewFinderView;
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
    private NV21 nv21;
    private int[] previewSize;
    private boolean isSaveBmp;
    private boolean isRotateDegree90Recognition = false;
    private boolean enableZXing = false;
    private boolean enableZBar = false;
    private boolean enableBankCard = false;
    private boolean enableIdCard = false;
    private boolean enableLicensePlate = false;
    private boolean isIdCardInit = false;
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
            Camera.Parameters parameters = camera.getParameters();
            int previewWidth = parameters.getPreviewSize().width;
            int previewHeight = parameters.getPreviewSize().height;
            int  rotationCount = getRotationCount();
            boolean isRotated = rotationCount == 1 || rotationCount == 3;
            //根据ViewFinderView和preview的尺寸之比，缩放扫码区域
            Rect rect = getScaledRect(previewWidth, previewHeight);
            byte[] tempData = null;
            byte[] tempData2 = null;
            int width = 0;
            int height = 0;
            int width2 = 0;
            int height2 = 0;
            if (scanner != null || enableZXing || enableBankCard || enableIdCard) {
                byte[] temp = Utils.clipNV21(data, previewWidth, previewHeight, rect.left, rect.top, rect.width(), rect.height());
                if (isRotated) {
                    tempData = Utils.rotateNV21Degree90(temp, rect.width(), rect.height());
                    width = rect.height();
                    height = rect.width();
                    if (isRotateDegree90Recognition) {
                        tempData2 = temp;
                        width2 = rect.width();
                        height2 = rect.height();
                    }
                } else {
                    tempData = temp;
                    width = rect.width();
                    height = rect.height();
                    if (isRotateDegree90Recognition) {
                        tempData2 = Utils.rotateNV21Degree90(temp, rect.width(), rect.height());
                        width2 = rect.height();
                        height2 = rect.width();
                    }
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
            if (enableZBar && result == null) {
                try {
                    Image barcode = new Image(previewWidth, previewHeight, "Y800");
                    barcode.setData(data);
                    barcode.setCrop(rect.left, rect.top, rect.width(), rect.height());
                    int code = getImageScanner().scanImage(barcode);
                    if (code == 0) throw new Exception("failure");
                    // 识别成功
                    SymbolSet syms = getImageScanner().getResults();
                    for (Symbol sym : syms) {
                        final String s = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT ? new String(sym.getDataBytes(), StandardCharsets.UTF_8) : sym.getData();
                        if (!TextUtils.isEmpty(s)) {
                            result = new Result();
                            result.type = Result.TYPE_CODE;
                            result.text = s;
                            break;
                        }
                    }
                } catch (Exception e) {}
            }
            if (enableZXing && result == null) {
                try {
                    PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(tempData, width, height, 0, 0, width, height, false);
                    String s = getMultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(source)), hints0).getText();
                    result = new Result();
                    result.type = Result.TYPE_CODE;
                    result.text = s;
                } catch (Exception e) {}
                if (result == null && isRotateDegree90Recognition) {
                    try {
                        PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(tempData2, width2, height2, 0, 0, width2, height2, false);
                        String s = getMultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(source)), hints0).getText();
                        result = new Result();
                        result.type = Result.TYPE_CODE;
                        result.text = s;
                    } catch (Exception e) {}
                }
            }
            if (enableBankCard && result == null) {
                try {
                    String s = BankCardUtils.decode(getBankCardAPI(), tempData, width, height);
                    if (!TextUtils.isEmpty(s)) {
                        result = new Result();
                        result.type = Result.TYPE_BANK_CARD;
                        result.text = s;
                    }
                } catch (Exception e) {}
                if (result == null && isRotateDegree90Recognition) {
                    try {
                        String s = BankCardUtils.decode(getBankCardAPI(), tempData2, width2, height2);
                        if (!TextUtils.isEmpty(s)) {
                            result = new Result();
                            result.type = Result.TYPE_BANK_CARD;
                            result.text = s;
                        }
                    } catch (Exception e) {}
                }
            }
            if (enableIdCard && result == null) {
                try {
                    if (!isIdCardInit) isIdCardInit = IdCardUtils.initDict(getContext());
                    if (!isIdCardInit) throw new Exception("failure");
                    byte[] obtain = new byte[4396];
                    int len = IdCardUtils.decode(tempData, width, height, obtain);
                    if (len > 0) result = Utils.decodeIdCard(obtain, len);
                } catch (Exception e) {}
                if (result == null && isRotateDegree90Recognition) {
                    try {
                        if (!isIdCardInit) isIdCardInit = IdCardUtils.initDict(getContext());
                        if (!isIdCardInit) throw new Exception("failure");
                        byte[] obtain = new byte[4396];
                        int len = IdCardUtils.decode(tempData2, width2, height2, obtain);
                        if (len > 0) result = Utils.decodeIdCard(obtain, len);
                    } catch (Exception e) {}
                }
            }
            if (enableLicensePlate && result == null) {
                try {
                    String s = LicensePlateUtils.recognize(tempData, width, height, getLicensePlatId());
                    if (!TextUtils.isEmpty(s)) {
                        result = new Result();
                        result.type = Result.TYPE_LICENSE_PLATE;
                        result.text = s;
                    }
                } catch (Exception e) {}
                if (result == null && isRotateDegree90Recognition) {
                    try {
                        String s = LicensePlateUtils.recognize(tempData2, width2, height2, getLicensePlatId());
                        if (!TextUtils.isEmpty(s)) {
                            result = new Result();
                            result.type = Result.TYPE_LICENSE_PLATE;
                            result.text = s;
                        }
                    } catch (Exception e) {}
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
                Rect framingRect = viewFinderView.getFramingRect();//获得扫码框区域
                if (framingRect == null) return;
                int viewFinderViewWidth = ((View) viewFinderView).getWidth();
                int viewFinderViewHeight = ((View) viewFinderView).getHeight();
                //1.根据ViewFinderView和2000*2000的尺寸之比，缩放对焦区域
                Rect scaledRect = new Rect(framingRect);
                scaledRect.left = scaledRect.left * width / viewFinderViewWidth;
                scaledRect.right = scaledRect.right * width / viewFinderViewWidth;
                scaledRect.top = scaledRect.top * height / viewFinderViewHeight;
                scaledRect.bottom = scaledRect.bottom * height / viewFinderViewHeight;
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
     * @param viewFinderView
     */
    public void setViewFinder(IViewFinder viewFinderView) {
        if (viewFinderView == null || !(viewFinderView instanceof View)) throw new IllegalArgumentException("viewFinderView必须是View对象");
        this.viewFinderView = viewFinderView;
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
    public void setShouldAdjustFocusArea(boolean shouldAdjustFocusArea) {
        this.shouldAdjustFocusArea = shouldAdjustFocusArea;
    }

    /**
     * 是否保存图片
     *
     * @param b
     */
    public void setSaveBmp(boolean b) {
        isSaveBmp = b;
    }

    /**
     * 是否在原来识别的图像基础上旋转90度继续识别
     *
     * @param b
     */
    public void setRotateDegree90Recognition(boolean b) {
        this.isRotateDegree90Recognition = b;
    }

    /**
     * 是否使用ZXing识别
     *
     * @param enableZXing
     */
    public void setEnableZXing(boolean enableZXing) {
        this.enableZXing = enableZXing;
    }

    /**
     * 是否使用ZBar识别
     *
     * @param enableZBar
     */
    public void setEnableZBar(boolean enableZBar) {
        this.enableZBar = enableZBar;
    }

    /**
     * 是否使用银行卡识别
     *
     * @param enableBankCard
     */
    public void setEnableBankCard(boolean enableBankCard) {
        this.enableBankCard = enableBankCard;
    }

    /**
     * 是否使用身份证识别
     *
     * @param enableIdCard
     */
    public void setEnableIdCard(boolean enableIdCard) {
        this.enableIdCard = enableIdCard;
    }

    /**
     * 是否使用车牌识别
     *
     * @param enableLicensePlate
     */
    public void setEnableLicensePlate(boolean enableLicensePlate) {
        this.enableLicensePlate = enableLicensePlate;
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
            decodeFormats.add(BarcodeFormat.QR_CODE);
            decodeFormats.add(BarcodeFormat.CODABAR);
            decodeFormats.add(BarcodeFormat.CODE_39);
            decodeFormats.add(BarcodeFormat.CODE_93);
            decodeFormats.add(BarcodeFormat.CODE_128);
            hints0.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
            hints0.put(DecodeHintType.TRY_HARDER, BarcodeFormat.QR_CODE);
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
            imageScanner.setConfig(Symbol.QRCODE, Config.ENABLE, 1);
            imageScanner.setConfig(Symbol.CODABAR, Config.ENABLE, 1);
            imageScanner.setConfig(Symbol.CODE39, Config.ENABLE, 1);
            imageScanner.setConfig(Symbol.CODE93, Config.ENABLE, 1);
            imageScanner.setConfig(Symbol.CODE128, Config.ENABLE, 1);
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
    private synchronized long getLicensePlatId() {
        if (licensePlateId == 0) licensePlateId = LicensePlateUtils.initRecognizer(getContext());
        return licensePlateId;
    }

    private synchronized NV21 getNV21() {
        if (nv21 == null) nv21 = new NV21(getContext());
        return nv21;
    }

    void setupCameraPreview(final CameraWrapper cameraWrapper) {
        this.cameraWrapper = cameraWrapper;
        if (this.cameraWrapper == null) return;
        removeAllViews();
        cameraPreview = new CameraPreview(getContext(), previewSize[0], previewSize[1], cameraWrapper, this, this);
        addView(cameraPreview);
        addView(((View) viewFinderView));
    }

    /**
     * 打开系统相机，并进行基本的初始化
     */
    private void startCamera() {
        if (cameraHandlerThread == null) {
            cameraHandlerThread = new CameraHandlerThread(this);
        }
        cameraHandlerThread.startCamera(CameraUtils.getDefaultCameraId());
    }

    /**
     * 释放相机资源等各种资源
     */
    private void stopCamera() {
        if (cameraHandlerThread != null) {
            cameraHandlerThread.quit();
            cameraHandlerThread = null;
        }
        if (cameraWrapper != null) {
            cameraPreview.stopCameraPreview();//停止相机预览并置空各种回调
            cameraPreview = null;
            cameraWrapper.camera.release();//释放资源
            cameraWrapper = null;
        }
        previewSize = null;
        scaledRect = null;
        focusAreas = null;
        multiFormatReader = null;
        imageScanner = null;
        if (bankCardAPI != null) {
            BankCardUtils.release(bankCardAPI);
            bankCardAPI = null;
        }
        if (isIdCardInit) {
            IdCardUtils.clearDict();
            isIdCardInit = false;
        }
        if (licensePlateId != 0) {
            LicensePlateUtils.releaseRecognizer(licensePlateId);
            licensePlateId = 0;
        }
        nv21 = null;
        removeAllViews();
    }

    /**
     * 根据ViewFinderView和preview的尺寸之比，缩放扫码区域
     */
    private Rect getScaledRect(int previewWidth, int previewHeight) {
        if (scaledRect == null) {
            Rect framingRect = viewFinderView.getFramingRect();//获得扫码框区域
            int viewFinderViewWidth = ((View) viewFinderView).getWidth();
            int viewFinderViewHeight = ((View) viewFinderView).getHeight();
            scaledRect = new Rect(framingRect);
            Point p = new Point();
            ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getSize(p);
            int o = p.x == p.y ? 0 : p.x < p.y ? Configuration.ORIENTATION_PORTRAIT : Configuration.ORIENTATION_LANDSCAPE;
            float ratio = o == Configuration.ORIENTATION_PORTRAIT ? previewHeight * 1f / previewWidth : previewWidth * 1f / previewHeight;
            float r = viewFinderViewWidth * 1f / viewFinderViewHeight;
            if (ratio < r){
                int width = o == Configuration.ORIENTATION_PORTRAIT ? previewHeight : previewWidth;
                scaledRect.left = scaledRect.left * width / viewFinderViewWidth;
                scaledRect.right = scaledRect.right * width / viewFinderViewWidth;
                scaledRect.top = scaledRect.top * width / viewFinderViewWidth;
                scaledRect.bottom = scaledRect.bottom * width / viewFinderViewWidth;
            } else {
                int height = o == Configuration.ORIENTATION_PORTRAIT ? previewWidth : previewHeight;
                scaledRect.left = scaledRect.left * height / viewFinderViewHeight;
                scaledRect.right = scaledRect.right * height / viewFinderViewHeight;
                scaledRect.top = scaledRect.top * height / viewFinderViewHeight;
                scaledRect.bottom = scaledRect.bottom * height / viewFinderViewHeight;
            }
//            scaledRect.left = (int) (0.8f * scaledRect.left);
//            scaledRect.right = (int) (1.25f * scaledRect.right);
//            scaledRect.top = (int) (0.8f * scaledRect.top);
//            scaledRect.bottom = (int) (1.25f * scaledRect.bottom);
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
     * @param camera
     */
    void setOptimalPreviewSize(Camera camera) {
        if (previewSize != null) return;
        try {
            if (camera == null) throw new NullPointerException("camera is null");
            //相机图像默认都是横屏(即宽>高)
            List<Camera.Size> sizes = camera.getParameters().getSupportedPreviewSizes();
            if (sizes == null) throw new NullPointerException("size is null");
            int w, h;
            int width = getMeasuredWidth();
            int height = getMeasuredHeight();
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                w = width;
                h = height;
            } else {
                w = height;
                h = width;
            }
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
            DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
            int width = dm.widthPixels > dm.heightPixels ? dm.widthPixels : dm.heightPixels;	// 屏幕宽度
            int height = dm.widthPixels > dm.heightPixels ? dm.heightPixels : dm.widthPixels;	// 屏幕高度
            previewSize = new int[] {width , height};
        }
    }
}