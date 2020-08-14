package com.shouzhong.scanner.demo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.shouzhong.scanner.Callback;
import com.shouzhong.scanner.IViewFinder;
import com.shouzhong.scanner.Result;
import com.shouzhong.scanner.ScannerView;

public class ScannerActivity extends AppCompatActivity {

    private ScannerView scannerView;
    private TextView tvResult;
    private LinearLayout llQrcode;
    private LinearLayout llBarcode;

    private Vibrator vibrator;
    private int flag;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);
        scannerView = findViewById(R.id.sv);
        tvResult = findViewById(R.id.tv_result);
        llQrcode = findViewById(R.id.ll_qrcode);
        llBarcode = findViewById(R.id.ll_barcode);
        scannerView.setShouldAdjustFocusArea(true);
        scannerView.setViewFinder(new ViewFinder(this));
        scannerView.setSaveBmp(false);
        scannerView.setRotateDegree90Recognition(true);
        scannerView.setCallback(new Callback() {
            @Override
            public void result(Result result) {
                tvResult.setText("识别结果：\n" + result.toString());
                startVibrator();
                scannerView.restartPreviewAfterDelay(2000);
            }
        });
        ((SwitchCompat) findViewById(R.id.sc_direction)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                scannerView.onPause();
                scannerView.setCameraDirection(isChecked ? Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK);
                scannerView.onResume();
            }
        });
        ((SwitchCompat) findViewById(R.id.sc_zxing)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                scannerView.setEnableZXing(isChecked);
                if (isChecked) flag |= 0b1; else flag &= 0b10;
                llQrcode.setVisibility(flag == 0 ? View.GONE : View.VISIBLE);
                llBarcode.setVisibility(flag == 0 ? View.GONE : View.VISIBLE);
            }
        });
        ((SwitchCompat) findViewById(R.id.sc_zbar)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                scannerView.setEnableZBar(isChecked);
                if (isChecked) flag |= 0b10; else flag &= 0b1;
                llQrcode.setVisibility(flag == 0 ? View.GONE : View.VISIBLE);
                llBarcode.setVisibility(flag == 0 ? View.GONE : View.VISIBLE);
            }
        });
        ((SwitchCompat) findViewById(R.id.sc_qrcode)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                scannerView.setEnableQrcode(isChecked);
            }
        });
        ((SwitchCompat) findViewById(R.id.sc_barcode)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                scannerView.setEnableBarcode(isChecked);
            }
        });
        ((SwitchCompat) findViewById(R.id.sc_bank)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                scannerView.setEnableBankCard(isChecked);
            }
        });
        ((SwitchCompat) findViewById(R.id.sc_id_card)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                scannerView.setEnableIdCard(isChecked);
            }
        });
        ((SwitchCompat) findViewById(R.id.sc_license_plate)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                scannerView.setEnableLicensePlate(isChecked);
            }
        });
        ((SwitchCompat) findViewById(R.id.sc_id_card2)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                scannerView.setEnableIdCard2(isChecked);
            }
        });
        ((SwitchCompat) findViewById(R.id.sc_driving_license)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                scannerView.setEnableDrivingLicense(isChecked);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        scannerView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        scannerView.onPause();
    }

    @Override
    protected void onDestroy() {
        if (vibrator != null) {
            vibrator.cancel();
            vibrator = null;
        }
        super.onDestroy();
    }

    private void startVibrator() {
        if (vibrator == null)
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(300);
    }

    class ViewFinder extends View implements IViewFinder {
        private Rect framingRect;//扫码框所占区域
        private float widthRatio = 0.9f;//扫码框宽度占view总宽度的比例
        private float heightRatio = 0.8f;
        private float heightWidthRatio = 0.5626f;//扫码框的高宽比
        private int leftOffset = -1;//扫码框相对于左边的偏移量，若为负值，则扫码框会水平居中
        private int topOffset = -1;//扫码框相对于顶部的偏移量，若为负值，则扫码框会竖直居中

        private int laserColor = 0xff008577;// 扫描线颜色
        private int maskColor = 0x60000000;// 阴影颜色
        private int borderColor = 0xff008577;// 边框颜色
        private int borderStrokeWidth = 12;// 边框宽度
        private int borderLineLength = 72;// 边框长度

        private Paint laserPaint;// 扫描线
        private Paint maskPaint;// 阴影遮盖画笔
        private Paint borderPaint;// 边框画笔

        private int position;

        public ViewFinder(Context context) {
            super(context);
            setWillNotDraw(false);//需要进行绘制
            laserPaint = new Paint();
            laserPaint.setColor(laserColor);
            laserPaint.setStyle(Paint.Style.FILL);
            maskPaint = new Paint();
            maskPaint.setColor(maskColor);
            borderPaint = new Paint();
            borderPaint.setColor(borderColor);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(borderStrokeWidth);
            borderPaint.setAntiAlias(true);
        }

        @Override
        protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld) {
            updateFramingRect();
        }

        @Override
        public void onDraw(Canvas canvas) {
            if (getFramingRect() == null) {
                return;
            }
            drawViewFinderMask(canvas);
            drawViewFinderBorder(canvas);
            drawLaser(canvas);
        }

        private void drawLaser(Canvas canvas) {
            Rect framingRect = getFramingRect();
            int top = framingRect.top + 10 + position;
            canvas.drawRect(framingRect.left + 10, top, framingRect.right - 10, top + 5, laserPaint);
            position = framingRect.bottom - framingRect.top - 25 < position ? 0 : position + 2;
            //区域刷新
            postInvalidateDelayed(20, framingRect.left + 10, framingRect.top + 10, framingRect.right - 10, framingRect.bottom - 10);
        }

        /**
         * 绘制扫码框四周的阴影遮罩
         */
        private void drawViewFinderMask(Canvas canvas) {
            int width = canvas.getWidth();
            int height = canvas.getHeight();
            Rect framingRect = getFramingRect();
            canvas.drawRect(0, 0, width, framingRect.top, maskPaint);//扫码框顶部阴影
            canvas.drawRect(0, framingRect.top, framingRect.left, framingRect.bottom, maskPaint);//扫码框左边阴影
            canvas.drawRect(framingRect.right, framingRect.top, width, framingRect.bottom, maskPaint);//扫码框右边阴影
            canvas.drawRect(0, framingRect.bottom, width, height, maskPaint);//扫码框底部阴影
        }

        /**
         * 绘制扫码框的边框
         */
        private void drawViewFinderBorder(Canvas canvas) {
            Rect framingRect = getFramingRect();

            // Top-left corner
            Path path = new Path();
            path.moveTo(framingRect.left, framingRect.top + borderLineLength);
            path.lineTo(framingRect.left, framingRect.top);
            path.lineTo(framingRect.left + borderLineLength, framingRect.top);
            canvas.drawPath(path, borderPaint);

            // Top-right corner
            path.moveTo(framingRect.right, framingRect.top + borderLineLength);
            path.lineTo(framingRect.right, framingRect.top);
            path.lineTo(framingRect.right - borderLineLength, framingRect.top);
            canvas.drawPath(path, borderPaint);

            // Bottom-right corner
            path.moveTo(framingRect.right, framingRect.bottom - borderLineLength);
            path.lineTo(framingRect.right, framingRect.bottom);
            path.lineTo(framingRect.right - borderLineLength, framingRect.bottom);
            canvas.drawPath(path, borderPaint);

            // Bottom-left corner
            path.moveTo(framingRect.left, framingRect.bottom - borderLineLength);
            path.lineTo(framingRect.left, framingRect.bottom);
            path.lineTo(framingRect.left + borderLineLength, framingRect.bottom);
            canvas.drawPath(path, borderPaint);
        }

        /**
         * 设置framingRect的值（扫码框所占的区域）
         */
        private synchronized void updateFramingRect() {
            Point viewSize = new Point(getWidth(), getHeight());
            int width = getWidth() * 801 / 1080, height = getWidth() * 811 / 1080;
            width = (int) (getWidth() * widthRatio);
//            height = (int) (getHeight() * heightRatio);
            height = (int) (heightWidthRatio * width);

            int left, top;
            if (leftOffset < 0) {
                left = (viewSize.x - width) / 2;//水平居中
            } else {
                left = leftOffset;
            }
            if (topOffset < 0) {
                top = (viewSize.y - height) / 2;//竖直居中
            } else {
                top = topOffset;
            }
            framingRect = new Rect(left, top, left + width, top + height);
        }

        @Override
        public Rect getFramingRect() {
            return framingRect;
        }
    }

    class ViewFinder2 implements IViewFinder {
        @Override
        public Rect getFramingRect() {
            return new Rect(240, 240, 840, 840);
        }
    }
}
