# Scanner
## 说明
识别库，识别器可选择，这里有你常用的二维码/条码识别，还有你可能用到的身份证、银行卡识别、车牌，如果没有你想要的，可以自定义识别器。该库只识别扫描框内的图像，识别速率上大大提高，而且这个库比起其它的库就是解决了摄像头预览变形，预览页面高度自定义，你可以像常规一样整个页面都是预览，或者你可以选择在任何位置定义任何尺寸的预览，扫描框也高度自定义，你可以像常规一样居中，或者你也可以在预览的任何位置定义任何尺寸的扫描框（实际识别的扫描框和画上去的扫描框不一定是一样的，由你自己决定）。
## 效果图

<table>
    <tr>
        <td><img src="https://github.com/shouzhong/Scanner/blob/master/img/1.jpg"/></td>
        <td><img src="https://github.com/shouzhong/Scanner/blob/master/img/2.jpg"/></td>
    </tr>
    <tr>
        <td><img src="https://github.com/shouzhong/Scanner/blob/master/img/3.jpg"/></td>
        <td><img src="https://github.com/shouzhong/Scanner/blob/master/img/4.jpg"/></td>
    </tr>
</table>

## [下载 apk-demo](https://raw.githubusercontent.com/shouzhong/Scanner/master/app/release/app-release.apk)

## 使用
### 依赖
```
implementation 'com.shouzhong:Scanner:1.0.2'
```
以下选择自己需要的
```
// zxing
implementation 'com.google.zxing:core:3.3.3'
// zbar
implementation 'com.shouzhong:ScannerZBarLib:1.0.0'
// 银行卡识别
implementation 'com.shouzhong:ScannerBankCardLib:1.0.0'
// 身份证识别
implementation 'com.shouzhong:ScannerIdCardLib:1.0.1'
// 车牌识别
implementation 'com.shouzhong:ScannerLicensePlateLib:1.0.0'
```
### 代码
基本使用
```
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    <com.shouzhong.scanner.ScannerView
        android:id="@+id/sv"
        android:layout_width="match_parent"
        android:layout_height="1080px"
        android:background="#000000"/>
</RelativeLayout>
```
```
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_scanner);
    scannerView = findViewById(R.id.sv);
    scannerView.setViewFinder(new ViewFinder(this));
    scannerView.setSaveBmp(true);
    scannerView.setEnableZXing(true);
    scannerView.setEnableZBar(true);
    scannerView.setEnableBankCard(true);
    scannerView.setEnableIdCard(true);
    scannerView.setEnableLicensePlate(true);
    scannerView.setCallback(new Callback() {
        @Override
        public void result(Result result) {
            tvResult.setText("识别结果：\n" + result.toString());
            scannerView.restartPreviewAfterDelay(2000);
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
```
开启或者关闭某个识别器
```
// 启用zxing识别器
scannerView.setEnableZXing(true);
// 启用zbar识别器
scannerView.setEnableZBar(true);
// 启用银行卡识别器
scannerView.setEnableBankCard(true);
// 启用身份证识别器（这里只支持2代身份证）
scannerView.setEnableIdCard(true);
// 启用车牌识别
scannerView.setEnableLicensePlate(true);
```
如果你想自定义识别器
```
scannerView.setScanner(new IScanner() {
    /**
     * 这里实现自己的识别器，并把识别结果返回
     *
     * @param data 矩形框内nv21图像数据
     * @param width 图像宽度
     * @param height 图像高度
     * @return
     * @throws Exception
     */
    @Override
    public Result scan(byte[] data, int width, int height) throws Exception {
        // 如果你想转为Bitmap，请使用NV21.nv21ToBitmap(byte[] nv21, int width, int height)
        return null;
    }
});
```
这里没给默认的预览页面，需要自己自定义，下面给个例子
```
class ViewFinder extends View implements IViewFinder {
    private Rect framingRect;//扫码框所占区域
    private float widthRatio = 0.8f;//扫码框宽度占view总宽度的比例
    private float heightWidthRatio = 1f;//扫码框的高宽比
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
        int width, height;
        width = (int) (getWidth() * widthRatio);
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
```

## 回调说明

Result

属性 | 说明
------------ | -------------
TYPE_CODE | 类型：二维码/条码
TYPE_ID_CARD_FRONT | 类型：身份证人头面
TYPE_ID_CARD_BACK | 类型：身份证国徽面
TYPE_BANK_CARD | 类型：银行卡
TYPE_LICENSE_PLATE | 类型：车牌
type | 结果类型
path | 保存的图片路径
text | type为TYPE_CODE或TYPE_BANK_CARD或TYPE_LICENSE_PLATE返回结果
cardNum | type为TYPE_ID_CARD_FRONT，身份证号
name | type为TYPE_ID_CARD_FRONT，名字
sex | type为TYPE_ID_CARD_FRONT，性别
address | type为TYPE_ID_CARD_FRONT，地址
nation | type为TYPE_ID_CARD_FRONT，民族
birth | type为TYPE_ID_CARD_FRONT，出生年月日：yyyy-MM-dd
office | type为TYPE_ID_CARD_BACK，签发机关
validDate | type为TYPE_ID_CARD_BACK，有限期限：yyyyMMdd-yyyyMMdd

## 方法说明

ScannerView

方法名 | 说明
------------ | -------------
setViewFinder | 扫描区域
setCallback | 扫码成功后的回调
setEnableZXing | 是否启用zxing识别器，默认false
setEnableZBar | 是否启用zbar识别器，默认false
setEnableBankCard | 是否启用银行卡识别器，默认false
setEnableIdCard | 是否启用身份证识别器，默认false
setEnableLicensePlate | 是否启用车牌识别器，默认false
setScanner | 自定义识别器
onResume | 开启扫描
onPause | 停止扫描
restartPreviewAfterDelay | 设置多少毫秒后重启扫描
setFlash | 开启/关闭闪光灯
toggleFlash | 切换闪光灯的点亮状态
isFlashOn | 闪光灯是否被点亮
setShouldAdjustFocusArea | 设置是否要根据扫码框的位置去调整对焦区域的位置，部分手机不支持，默认false
setSaveBmp | 设置是否保存条码图片，默认false

ScannerUtils

方法名 | 说明
------------ | -------------
decodeCode | 识别二维码/条码，建议在子线程运行
decodeBank | 识别银行卡，建议在子线程运行
decodeIdCard | 识别身份证，建议在子线程运行
decodeLicensePlate | 识别车牌，建议在子线程运行
createBarcode | 条码生成，建议在子线程运行
createQRCode | 二维码生成，建议在子线程运行
addLogo | 往图片中间加logo

NV21

方法名 | 说明
------------ | -------------
nv21ToBitmap | nv21转bitmap
bitmapToNv21 | bitmap转nv21

## 注意事项
1. 该项目使用opencv-3.4.6，[点击下载](https://nchc.dl.sourceforge.net/project/opencvlibrary/3.4.6/opencv-3.4.6-android-sdk.zip)<br>
2. NDK版本r14b，r20编译不过去，[点击下载](https://links.jianshu.com/go?to=https%3A%2F%2Fdeveloper.android.google.cn%2Fndk%2Fdownloads%2Folder_releases.html)
3. 集成的时候请把licennseplate的CMakeLists.txt的第12行替换成自己的opencv-android-sdk的JNI路径
4. 如果是linux用户，请把licennseplate的build.gradle添加以下
```
android {
...
    defaultConfig {
        ...
        externalNativeBuild {
            cmake {
                cppFlags "-std=c++11"
                // linux请添加以下
                arguments "-DANDROID_TOOLCHAIN=gcc", "-DANDROID_ARM_NEON=TRUE", "-DANDROID_STL_FORCE_FEATURES=OFF"
            }
        }
    }
}
```

## 混淆
```
-dontwarn com.shouzhong.**
-keep class com.shouzhong.** {*;}
-dontwarn com.google.zxing.**
-keep class com.google.zxing.**
-dontwarn net.sourceforge.zbar.**
-keep class net.sourceforge.zbar.** {*;}
-keep class com.wintone.bankcard.** {*;}
-dontwarn com.wintone.bankcard.**
-keep class exocr.exocrengine.** {*;}
-dontwarn exocr.exocrengine.**
```