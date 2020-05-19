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
import com.shouzhong.bankcard.BankCardInfoBean;
import com.shouzhong.bankcard.BankCardUtils;
import com.shouzhong.drivinglicense.DrivingLicenseUtils;
import com.shouzhong.idcard.IdCardUtils;
import com.shouzhong.idcard2.IdCard2Utils;
import com.shouzhong.licenseplate.LicensePlateUtils;
import com.shouzhong.nsfw.NsfwUtils;
import com.shouzhong.text.TextRecognition;
import com.wintone.bankcard.BankCardAPI;

import org.tensorflow.lite.Interpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * 识别工具类
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
        decodeFormats.add(BarcodeFormat.EAN_8);
        decodeFormats.add(BarcodeFormat.EAN_13);
        decodeFormats.add(BarcodeFormat.UPC_A);
        decodeFormats.add(BarcodeFormat.UPC_E);
        decodeFormats.add(BarcodeFormat.ITF);
        decodeFormats.add(BarcodeFormat.RSS_14);
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
        } catch (Exception e) { }
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
        if (bmp.getWidth() % 2 == 1 || bmp.getHeight() % 2 == 1) {
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth() / 2 * 2, bmp.getHeight() / 2 * 2);
        }
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        BankCardAPI api = BankCardUtils.init();
        try {
            byte[] data = Utils.bitmapToNv21(bmp);
            String s = BankCardUtils.decode(api, data, width, height);
            if (TextUtils.isEmpty(s)) throw new Exception("failure");
            BankCardUtils.release(api);
            return s;
        } catch (Exception e) {}
        try {
            Matrix m = new Matrix();
            m.setRotate(90, width / 2, height / 2);
            bmp = Bitmap.createBitmap(bmp, 0, 0, width, height, m, true);
            byte[] data = Utils.bitmapToNv21(bmp);
            String s = BankCardUtils.decode(api, data, height, width);
            if (TextUtils.isEmpty(s)) throw new Exception("failure");
            BankCardUtils.release(api);
            return s;
        } catch (Exception e) {}
        BankCardUtils.release(api);
        throw new Exception("failure");
    }

    /**
     * 获取银行卡信息，请在子线程运行
     *
     * @param cardNumber 银行卡号
     * @return
     */
    public static BankCardInfoBean getBankCardInfo(String cardNumber) {
        return BankCardUtils.getBankCardInfo(cardNumber);
    }

    /**
     * 识别身份证，建议在子线程运行
     *
     * @param bmp
     * @return
     */
    public static com.shouzhong.scanner.Result decodeIdCard(Context context, Bitmap bmp) throws Exception {
        if (bmp == null) return null;
        boolean boo = IdCardUtils.initDict(context);
        if (!boo) throw new Exception("init failure");
        final byte[] obtain = new byte[4096];
        int len = IdCardUtils.decode(bmp, obtain);
        if (len <= 0) {
            Matrix m = new Matrix();
            m.setRotate(90, (float) bmp.getWidth() / 2, (float) bmp.getHeight() / 2);
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
            len = IdCardUtils.decode(bmp, obtain);
        }
        IdCardUtils.clearDict();
        if (len <= 0) return null;
        return Utils.decodeIdCard(obtain, len);
    }

    /**
     * 识别身份证，建议在子线程运行（第二种方式）
     *
     * @param bmp
     * @return
     */
    public static com.shouzhong.scanner.Result decodeIdCard2(Bitmap bmp) throws Exception {
        if (bmp == null) return null;
        if (bmp.getWidth() % 2 == 1 || bmp.getHeight() % 2 == 1) {
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth() / 2 * 2, bmp.getHeight() / 2 * 2);
        }
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        try {
            byte[] data = Utils.bitmapToNv21(bmp);
            String s = IdCard2Utils.decode(data, width, height);
            if (TextUtils.isEmpty(s)) throw new Exception("failure");
            IdCard2Utils.close();
            com.shouzhong.scanner.Result result = new com.shouzhong.scanner.Result();
            result.type = s.contains("cardNumber") ? com.shouzhong.scanner.Result.TYPE_ID_CARD_FRONT : com.shouzhong.scanner.Result.TYPE_ID_CARD_BACK;
            result.data = s;
            return result;
        } catch (Exception e) {}
        try {
            Matrix m = new Matrix();
            m.setRotate(90, width / 2, height / 2);
            bmp = Bitmap.createBitmap(bmp, 0, 0, width, height, m, true);
            byte[] data = Utils.bitmapToNv21(bmp);
            String s = IdCard2Utils.decode(data, height, width);
            if (TextUtils.isEmpty(s)) throw new Exception("failure");
            IdCard2Utils.close();
            com.shouzhong.scanner.Result result = new com.shouzhong.scanner.Result();
            result.type = s.contains("cardNumber") ? com.shouzhong.scanner.Result.TYPE_ID_CARD_FRONT : com.shouzhong.scanner.Result.TYPE_ID_CARD_BACK;
            result.data = s;
            return result;
        } catch (Exception e) {}
        IdCard2Utils.close();
        return null;
    }

    /**
     * 识别驾驶证，建议在子线程运行
     *
     * @param bmp
     * @return
     * @throws Exception
     */
    public static String decodeDrivingLicense(Bitmap bmp) throws Exception {
        if (bmp == null) return null;
        if (bmp.getWidth() % 2 == 1 || bmp.getHeight() % 2 == 1) {
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth() / 2 * 2, bmp.getHeight() / 2 * 2);
        }
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        try {
            byte[] data = Utils.bitmapToNv21(bmp);
            String s = DrivingLicenseUtils.decode(data, width, height);
            if (TextUtils.isEmpty(s)) throw new Exception("failure");
            DrivingLicenseUtils.close();
            return s;
        } catch (Exception e) {}
        try {
            Matrix m = new Matrix();
            m.setRotate(90, width / 2, height / 2);
            bmp = Bitmap.createBitmap(bmp, 0, 0, width, height, m, true);
            byte[] data = Utils.bitmapToNv21(bmp);
            String s = DrivingLicenseUtils.decode(data, height, width);
            if (TextUtils.isEmpty(s)) throw new Exception("failure");
            DrivingLicenseUtils.close();
            return s;
        } catch (Exception e) {}
        DrivingLicenseUtils.close();
        return null;
    }

    /**
     * 车牌识别，建议在子线程运行
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
     * 图片文字识别，请在子线程运行，慎重使用，可能会随时失效，图片限制500k以内
     * 如果图片太大可以使用luban等之类的图片压缩库
     *
     * @param imageFilePath
     * @return
     */
    public static String decodeText(String imageFilePath) {
        return TextRecognition.recognize(imageFilePath);
    }

    /**
     * 黄图识别，建议在子线程运行
     *
     * @param context
     * @param bmp
     * @return 大于0.3可以说图片涉黄，根据实际情况取值
     * @throws Exception
     */
    public static float decodeNsfw(Context context, Bitmap bmp) throws Exception {
        Interpreter tflite = NsfwUtils.getInterpreter(context);
        float f = NsfwUtils.decode(tflite, bmp);
        NsfwUtils.release(tflite);
        return f;
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
