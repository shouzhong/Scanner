package com.shouzhong.bankcard;

import android.text.TextUtils;

public class BankCardInfoBean {

    // 银行卡号
    public String cardNumber;
    // 银行卡（英文）类型
    public String cardType;
    // 银行（英文）名称
    public String bank;

    @Override
    public String toString() {
        return "BankCardInfoBean{" +
                "cardNumber='" + cardNumber + '\'' +
                ", cardType='" + cardType + ',' + getCNCardType(cardType) + '\'' +
                ", bank='" + bank + ',' + getCNBankName(bank) + '\'' +
                ", bankId='" + getBankId(bank) + '\'' +
                '}';
    }

    /**
     * 获取银行（中文）名称
     *
     * @param bank
     * @return
     */
    public static String getCNBankName(String bank) {
        if (TextUtils.isEmpty(bank)) return null;
        switch (bank) {
            case "NBBANK": return "宁波银行";
            case "BJBANK": return "北京银行";
            case "CEB": return "光大银行";
            case "GDB": return "广发银行";
            case "HXBANK": return "华夏银行";
            case "CITIC": return "中信银行";
            case "SPABANK": return "平安银行";
            case "CIB": return "兴业银行";
            case "CMBC": return "民生银行";
            case "SPDB": return "浦发银行";
            case "COMM": return "交通银行";
            case "PSBC": return "邮储银行";
            case "CMB": return "招商银行";
            case "CCB": return "建设银行";
            case "BOC": return "中国银行";
            case "ABC": return "农业银行";
            case "ICBC": return "工商银行";
            default: return "未知银行";
        }
    }

    /**
     * 获取银行编号
     *
     * @param bank
     * @return
     */
    public static String getBankId(String bank) {
        if (TextUtils.isEmpty(bank)) return null;
        switch (bank) {
            case "NBBANK": return "1056";
            case "BJBANK": return "1032";
            case "CEB": return "1022";
            case "GDB": return "1027";
            case "HXBANK": return "1025";
            case "CITIC": return "1021";
            case "SPABANK": return "1010";
            case "CIB": return "1009";
            case "CMBC": return "1006";
            case "SPDB": return "1004";
            case "COMM": return "1020";
            case "PSBC": return "1066";
            case "CMB": return "1001";
            case "CCB": return "1003";
            case "BOC": return "1026";
            case "ABC": return "1005";
            case "ICBC": return "1002";
            default: return "未知";
        }
    }

    /**
     * 获取银行卡（中文）类型
     *
     * @param cardType
     * @return
     */
    public static String getCNCardType(String cardType) {
        if (TextUtils.isEmpty(cardType)) return null;
        switch (cardType) {
            case "DC": return "储蓄卡";
            case "CC": return "信用卡";
            case "SCC": return "准贷记卡";
            case "PC": return "预支付卡";
            default: return "未知卡";
        }
    }
}
