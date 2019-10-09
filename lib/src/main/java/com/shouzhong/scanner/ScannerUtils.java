package com.shouzhong.scanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.text.TextUtils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.shouzhong.licenseplate.LicensePlateUtils;
import com.wintone.bankcard.BankCardAPI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import exocr.exocrengine.EXOCREngine;

/**
 * Created by Administrator on 2018/07/31.
 *
 *
 */

public class ScannerUtils {

    /**
     * 识别二维码/条码，建议在子线程运行
     *
     * @param bmp
     * @return
     */
    public static String decodeCode(Bitmap bmp) throws Exception {
        if (bmp == null) throw new Exception("图片不存在");
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        Map<DecodeHintType, Object> hints = new HashMap<>();
        List<BarcodeFormat> decodeFormats = new ArrayList<>();
        decodeFormats.add(BarcodeFormat.QR_CODE);
        decodeFormats.add(BarcodeFormat.CODABAR);
        decodeFormats.add(BarcodeFormat.CODE_39);
        decodeFormats.add(BarcodeFormat.CODE_93);
        decodeFormats.add(BarcodeFormat.CODE_128);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
        hints.put(DecodeHintType.TRY_HARDER, BarcodeFormat.QR_CODE);
        hints.put(DecodeHintType.CHARACTER_SET, "utf-8");
        MultiFormatReader reader = new MultiFormatReader();//初始化解析对象
        try {
            int[] pixels = new int[width * height];
            bmp.getPixels(pixels, 0, width, 0, 0, width, height);
            //新建一个RGBLuminanceSource对象
            RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
            //将图片转换成二进制图片
            BinaryBitmap binaryBitmap = new BinaryBitmap(new GlobalHistogramBinarizer(source));
            Result result = reader.decode(binaryBitmap, hints);//开始解析
            return result.getText();
        } catch (Exception e) {}
        Matrix m = new Matrix();
        m.setRotate(90, (float) bmp.getWidth() / 2, (float) bmp.getHeight() / 2);
        bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
        width = bmp.getWidth();
        height = bmp.getHeight();
        int[] pixels = new int[width * height];
        bmp.getPixels(pixels, 0, width, 0, 0, width, height);
        //新建一个RGBLuminanceSource对象
        RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
        //将图片转换成二进制图片
        BinaryBitmap binaryBitmap = new BinaryBitmap(new GlobalHistogramBinarizer(source));
        Result result = reader.decode(binaryBitmap, hints);//开始解析
        return result.getText();
    }

    /**
     * 识别银行卡，建议在子线程运行，识别率很低
     *
     * @param bmp
     * @return
     */
    public static String decodeBank(Bitmap bmp) throws Exception {
        if (bmp == null) return null;
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        BankCardAPI api = new BankCardAPI();
        api.WTInitCardKernal("", 0);
        api.WTSetROI(new int[]{0, 0, width, height}, width, height);
        byte[] data = Utils.bitmapToNv21(bmp, width, height);
        int[] borders = new int[4];
        char[] resultData = new char[30];
        int[] picture = new int[32000];
        int result = api.RecognizeNV21(data, width, height, borders, resultData, resultData.length, new int[1], picture);
        if (result != 0) {
            Matrix m = new Matrix();
            m.setRotate(90, width / 2, height / 2);
            bmp = Bitmap.createBitmap(bmp, 0, 0, width, height, m, true);
            data = Utils.bitmapToNv21(bmp, height, width);
            api.WTSetROI(new int[]{0, 0, height, width}, height, width);
            result = api.RecognizeNV21(data, height, width, borders, resultData, resultData.length, new int[1], picture);
            if (result != 0) {
                api.WTUnInitCardKernal();
                return null;
            }
        }
        api.WTUnInitCardKernal();
        final StringBuffer sb = new StringBuffer();
        for (char c : resultData) {
            if (c >= '0' && c <= '9') sb.append(c);
        }
        return sb.toString();
    }

    /**
     * 识别身份证，建议在子线程运行
     *
     * @param bmp
     * @return
     */
    public static com.shouzhong.scanner.Result decodeIdCard(Context context, Bitmap bmp) throws Exception {
        if (bmp == null) return null;
        boolean boo = Utils.initDict(context);
        if (!boo) Utils.initDict(context);
        final byte[] obtain = new byte[4096];
        int len = EXOCREngine.nativeRecoIDCardBitmap(bmp, obtain, obtain.length);
        if (len <= 0) {
            Matrix m = new Matrix();
            m.setRotate(90, (float) bmp.getWidth() / 2, (float) bmp.getHeight() / 2);
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
            len = EXOCREngine.nativeRecoIDCardBitmap(bmp, obtain, obtain.length);
        }
        Utils.clearDict();
        if (len <= 0) return null;
        return Utils.decodeIdCard(obtain, len);
    }

    /**
     * 车牌识别
     *
     * @param context
     * @param bmp
     * @return
     * @throws Exception
     */
    public static String decodeLicensePlate(Context context, Bitmap bmp) throws Exception {
        if (bmp == null) return null;
        long id = LicensePlateUtils.initRecognizer(context);
        String s = LicensePlateUtils.recognizeBmp(bmp, id);
        LicensePlateUtils.releaseRecognizer(id);
        if (TextUtils.isEmpty(s)) return null;
        return s;
    }

    /**
     * 条码生成，建议在子线程运行
     *
     * @param contents
     * @param desiredWidth
     * @param desiredHeight
     * @return
     */
    public static Bitmap createBarcode(String contents, int desiredWidth, int desiredHeight) throws Exception {
        final int WHITE = 0xFFFFFFFF;
        final int BLACK = 0xFF000000;
        Hashtable<EncodeHintType, Object> hst = new Hashtable<>();
        hst.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        //容错级别
        hst.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        //设置空白边距的宽度
        hst.put(EncodeHintType.MARGIN, 0);
        BitMatrix result = new MultiFormatWriter().encode(contents, BarcodeFormat.CODE_128, desiredWidth, desiredHeight, hst);
        int width = result.getWidth();
        int height = result.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    /**
     * 二维码生成，建议在子线程运行
     *
     * @param str
     * @param size
     * @return
     */
    public static Bitmap createQRCode(String str, int size, Bitmap logo) throws Exception {
        final int WHITE = 0xFFFFFFFF;
        final int BLACK = 0xFF000000;
        MultiFormatWriter writer = new MultiFormatWriter();
        Hashtable<EncodeHintType, Object> hst = new Hashtable<>();
        hst.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        //容错级别
        hst.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        //设置空白边距的宽度
        hst.put(EncodeHintType.MARGIN, 0);
        BitMatrix matrix = writer.encode(str, BarcodeFormat.QR_CODE, size, size, hst);//生成二维码矩阵信息
        int width = matrix.getWidth();//矩阵高度
        int height = matrix.getHeight();//矩阵宽度
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = matrix.get(x, y) ? BLACK : WHITE;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return addLogo(bitmap, logo, 0.25f);
    }

    /**
     * 往图形中间添加logo，建议在子线程运行
     *
     * @param src
     * @param logo
     * @param scale 缩放比例，0~1
     * @return
     */
    public static Bitmap addLogo(Bitmap src, Bitmap logo, float scale) throws Exception {
        if (src == null) return null;
        if (logo == null) return src;
        //获取图片的宽高
        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();
        int logoWidth = logo.getWidth();
        int logoHeight = logo.getHeight();
        if (srcWidth == 0 || srcHeight == 0) return null;
        if (logoWidth == 0 || logoHeight == 0 || scale == 0.0f) return src;
        scale = scale < 0 || scale > 1 ? 0.25f : scale;
        //logo大小为二维码整体大小的1/5
        float scaleFactor = srcWidth * scale / logoWidth;
        Bitmap bitmap = Bitmap.createBitmap(srcWidth, srcHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(src, 0, 0, null);
        canvas.scale(scaleFactor, scaleFactor, srcWidth / 2, srcHeight / 2);
        canvas.drawBitmap(logo, (srcWidth - logoWidth) / 2, (srcHeight - logoHeight) / 2, null);
        canvas.save();
        canvas.restore();
        return bitmap;
    }
}
