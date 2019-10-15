package com.shouzhong.bankcard;

import com.wintone.bankcard.BankCardAPI;

public class BankCardUtils {

    /**
     * 初始化
     *
     * @return
     */
    public static BankCardAPI init() {
        BankCardAPI bankCardAPI = new BankCardAPI();
        bankCardAPI.WTInitCardKernal("", 0);
        return bankCardAPI;
    }

    /**
     * 识别
     *
     * @param api
     * @param image
     * @param width
     * @param height
     * @return
     * @throws Exception
     */
    public static String decode(BankCardAPI api, byte[] image, int width, int height) throws Exception {
        int[] borders = new int[4];
        char[] resultData = new char[30];
        int[] picture = new int[32000];
        api.WTSetROI(new int[]{0, 0, width, height}, width, height);
        int code = api.RecognizeNV21(image, width, height, borders, resultData, 30, new int[1], picture);
        if (code != 0 || borders[0] != 1 || borders[2] != 1 || borders[3] != 1) throw new Exception("failure");
        final StringBuffer sb = new StringBuffer();
        for (char c : resultData) {
            if (c >= '0' && c <= '9') sb.append(c);
        }
        return sb.toString();
    }

    /**
     * 释放资源
     *
     * @param api
     */
    public static void release(BankCardAPI api) {
        api.WTUnInitCardKernal();
    }
}
