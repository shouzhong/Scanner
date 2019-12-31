package com.shouzhong.scanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;

class Utils {

    /**
     * nv21转bitmap
     *
     * @param nv21
     * @param width
     * @param height
     * @return
     */
    static final Bitmap nv21ToBitmap(byte[] nv21, int width, int height) {
        Bitmap bitmap = null;
        try {
            YuvImage image = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            image.compressToJpeg(new Rect(0, 0, width, height), 80, stream);
            bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    /**
     * bitmap转nv21
     *
     * @param bitmap
     * @return
     */
    public static byte[] bitmapToNv21(Bitmap bitmap) {
        int width = bitmap.getWidth() % 2 == 0 ? bitmap.getWidth() : bitmap.getWidth() - 1;
        int height = bitmap.getHeight() % 2 == 0 ? bitmap.getHeight() : bitmap.getHeight() - 1;
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);
        int[] argb = new int[width * height];
        bitmap.getPixels(argb, 0, width, 0, 0, width, height);
        byte[] yuv = new byte[width * height * 3 / 2];
        encodeYUV420SP(yuv, argb, width, height);
        return yuv;
    }

    static void encodeYUV420SP(byte[] yuv420sp, int[] argb, int width, int height) {
        final int frameSize = width * height;
        int yIndex = 0;
        int uvIndex = frameSize;
        int a, R, G, B, Y, U, V;
        int index = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                a = (argb[index] & 0xff000000) >> 24; // a is not used obviously
                R = (argb[index] & 0xff0000) >> 16;
                G = (argb[index] & 0xff00) >> 8;
                B = (argb[index] & 0xff) >> 0;
                // well known RGB to YUV algorithm
                Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
                U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
                V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;

                // NV21 has a plane of Y and interleaved planes of VU each sampled by a factor of 2
                //    meaning for every 4 Y pixels there are 1 V and 1 U.  Note the sampling is every other
                //    pixel AND every other scanline.
                yuv420sp[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420sp[uvIndex++] = (byte) ((V < 0) ? 0 : ((V > 255) ? 255 : V));
                    yuv420sp[uvIndex++] = (byte) ((U < 0) ? 0 : ((U > 255) ? 255 : U));
                }
                index++;
            }
        }
    }

    /**
     * 保存图片
     *
     * @param context
     * @param bitmap
     */
    static final String saveBitmap(final Context context, Bitmap bitmap) {
        try {
            final String local = context.getExternalCacheDir().getAbsolutePath() + "/img" + System.currentTimeMillis() + ".jpg";
            final File file = new File(local);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
            }
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
            if (null != bitmap) {
                bitmap.recycle();
            }
            return local;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * NV21裁剪  算法效率 3ms
     *
     * @param src    源数据
     * @param width  源宽
     * @param height 源高
     * @param left   顶点坐标
     * @param top    顶点坐标
     * @param clip_w 裁剪后的宽
     * @param clip_h 裁剪后的高
     * @return 裁剪后的数据
     */
    static final byte[] clipNV21(byte[] src, int width, int height, int left, int top, int clip_w, int clip_h) {
        if (left > width || top > height) {
            return null;
        }
        if (width == clip_w && height == clip_h) return src;
        //取偶
        int x = left / 2 * 2, y = top / 2 * 2;
        int w = clip_w / 2 * 2, h = clip_h / 2 * 2;
        int y_unit = w * h;
        int uv = y_unit / 2;
        byte[] nData = new byte[y_unit + uv];
        int uv_index_dst = w * h - y / 2 * w;
        int uv_index_src = width * height + x;
        int srcPos0 = y * width;
        int destPos0 = 0;
        int uvSrcPos0 = uv_index_src;
        int uvDestPos0 = uv_index_dst;
        for (int i = y; i < y + h; i++) {
            System.arraycopy(src, srcPos0 + x, nData, destPos0, w);//y内存块复制
            srcPos0 += width;
            destPos0 += w;
            if ((i & 1) == 0) {
                System.arraycopy(src, uvSrcPos0, nData, uvDestPos0, w);//uv内存块复制
                uvSrcPos0 += width;
                uvDestPos0 += w;
            }
        }
        return nData;
    }

    /**
     * 剪切NV21数据并且镜像 算法效率1080x1920 14ms 1280x720 6ms
     *
     * @param src
     * @param width
     * @param height
     * @param left
     * @param top
     * @param clip_w
     * @param clip_h
     * @return
     */
    static final byte[] clipMirrorNV21(byte[] src, int width, int height, int left, int top, int clip_w, int clip_h) {
        if (left > width || top > height) {
            return null;
        }
        //取偶
        int x = left, y = top;
        int w = clip_w, h = clip_h;
        int y_unit = w * h;
        int src_unit = width * height;
        int uv = y_unit / 2;
        byte[] nData = new byte[y_unit + uv];
        int nPos = (y - 1) * width;
        int mPos;

        for (int i = y, len_i = y + h; i < len_i; i++) {
            nPos += width;
            mPos = src_unit + (i >> 1) * width;
            for (int j = x, len_j = x + w; j < len_j; j++) {
                nData[(i - y + 1) * w - j + x - 1] = src[nPos + j];
                if ((i & 1) == 0) {
                    int m = y_unit + (((i - y) >> 1) + 1) * w - j + x - 1;
                    if ((m & 1) == 0) {
                        m++;
                        nData[m] = src[mPos + j];
                        continue;
                    }
                    m--;
                    nData[m] = src[mPos + j];
                }
            }
        }
        return nData;
    }

    /**
     * 旋转NV21数据90度
     *
     * @param data
     * @param imageWidth
     * @param imageHeight
     * @return
     */
    static final byte[] rotateNV21Degree90(byte[] data, int imageWidth, int imageHeight) {
        byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
        int i = 0;
        for (int x = 0; x < imageWidth; x++) {
            for (int y = imageHeight - 1; y >= 0; y--) {
                yuv[i] = data[y * imageWidth + x];
                i++;
            }
        }
        i = imageWidth * imageHeight * 3 / 2 - 1;
        for (int x = imageWidth - 1; x > 0; x = x - 2) {
            for (int y = 0; y < imageHeight / 2; y++) {
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + x];
                i--;
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth)
                        + (x - 1)];
                i--;
            }
        }
        return yuv;
    }

    /**
     * 旋转NV21数据180度
     *
     * @param data
     * @param imageWidth
     * @param imageHeight
     * @return
     */
    static final byte[] rotateNV21Degree180(byte[] data, int imageWidth, int imageHeight) {
        byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
        int i = 0;
        int count = 0;
        for (i = imageWidth * imageHeight - 1; i >= 0; i--) {
            yuv[count] = data[i];
            count++;
        }
        i = imageWidth * imageHeight * 3 / 2 - 1;
        for (i = imageWidth * imageHeight * 3 / 2 - 1; i >= imageWidth
                * imageHeight; i -= 2) {
            yuv[count++] = data[i - 1];
            yuv[count++] = data[i];
        }
        return yuv;
    }

    /**
     * 旋转NV21数据270度
     *
     * @param data
     * @param imageWidth
     * @param imageHeight
     * @return
     */
    static final byte[] rotateNV21Degree270(byte[] data, int imageWidth, int imageHeight) {
        byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
        int nWidth = 0, nHeight = 0;
        int wh = 0;
        int uvHeight = 0;
        if (imageWidth != nWidth || imageHeight != nHeight) {
            nWidth = imageWidth;
            nHeight = imageHeight;
            wh = imageWidth * imageHeight;
            uvHeight = imageHeight >> 1;// uvHeight = height / 2
        }

        int k = 0;
        for (int i = 0; i < imageWidth; i++) {
            int nPos = 0;
            for (int j = 0; j < imageHeight; j++) {
                yuv[k] = data[nPos + i];
                k++;
                nPos += imageWidth;
            }
        }
        for (int i = 0; i < imageWidth; i += 2) {
            int nPos = wh;
            for (int j = 0; j < uvHeight; j++) {
                yuv[k] = data[nPos + i];
                yuv[k + 1] = data[nPos + i + 1];
                k += 2;
                nPos += imageWidth;
            }
        }
        return rotateNV21Degree180(rotateNV21Degree90(data, imageWidth, imageHeight), imageWidth, imageHeight);
    }

    // 以下为身份证识别
    /**
     * 解析
     *
     * @param bResultBuf
     * @param resLen
     * @return
     */
    static final Result decodeIdCard(byte[] bResultBuf, int resLen) throws Exception {
        byte code;
        int i, j, rdCount;
        String content = null;
        Result idCard = new Result();
        rdCount = 0;
        idCard.type = bResultBuf[rdCount++];
        JSONObject out = new JSONObject();
        while (rdCount < resLen) {
            code = bResultBuf[rdCount++];
            i = 0;
            j = rdCount;
            while (rdCount < resLen) {
                i++;
                rdCount++;
                if (bResultBuf[rdCount] == 0x20) break;
            }
            try {
                content = new String(bResultBuf, j, i, "GBK");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if (idCard.type == Result.TYPE_ID_CARD_FRONT) {
                if (code == 0x21) {
                    out.put("cardNumber", content);
                    String year = content.substring(6, 10);
                    String month = content.substring(10, 12);
                    String day = content.substring(12, 14);
                    out.put("birth", year + "-" + month + "-" + day);
                } else if (code == 0x22) {
                    out.put("name", content);
                } else if (code == 0x23) {
                    out.put("sex", content);
                } else if (code == 0x24) {
                    out.put("nation", content);
                } else if (code == 0x25) {
                    out.put("address", content);
                }
            } else if (idCard.type == Result.TYPE_ID_CARD_BACK) {
                if (code == 0x26) {
                    out.put("organization", content);
                } else if (code == 0x27) {
                    out.put("validPeriod", content);
                }
            }
            rdCount++;
        }
        if (idCard.type != Result.TYPE_ID_CARD_FRONT && idCard.type != Result.TYPE_ID_CARD_BACK) return null;
        idCard.data = out.toString();
        return idCard;
    }
}
