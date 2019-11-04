package com.shouzhong.bankcard;

import android.text.TextUtils;

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
        if (code != 0) throw new Exception("failure");
//        if (borders[0] != 1 || borders[1] != 1 || borders[2] != 1 || borders[3] != 1) throw new Exception("failure");
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

    /**
     * 获取银行卡信息
     *
     * @param cardNumber
     * @return
     */
    public static BankCardInfoBean getBankCardInfo(String cardNumber) {
        try {
            String s = BankCardInfo.bankCardInfo(cardNumber);
            if (TextUtils.isEmpty(s)) return null;
            String[] ss = s.split(",");
            if (ss == null || ss.length != 2) return null;
            BankCardInfoBean b = new BankCardInfoBean();
            b.cardNumber = cardNumber;
            b.cardType = ss[0];
            b.bank = ss[1];
            return b;
        } catch (Exception e) {}
        return null;
    }
}
