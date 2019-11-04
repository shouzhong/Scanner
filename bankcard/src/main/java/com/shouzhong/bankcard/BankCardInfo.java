package com.shouzhong.bankcard;

public class BankCardInfo {

    static {
        System.loadLibrary("BankCardInfo");
    }

    public native static String bankCardInfo(String cardNumber);

}
