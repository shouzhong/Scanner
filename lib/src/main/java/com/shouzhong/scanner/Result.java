package com.shouzhong.scanner;

public class Result {

    // 二维码/条码
    public static final int TYPE_CODE = 0;
    // 身份证人头面
    public static final int TYPE_ID_CARD_FRONT = 1;
    // 身份证国徽面
    public static final int TYPE_ID_CARD_BACK = 2;
    // 银行卡
    public static final int TYPE_BANK_CARD = 3;
    // 车牌
    public static final int TYPE_LICENSE_PLATE = 4;

    public int type;

    public String path;// 图片路径

    // 以下为type为TYPE_CODE或TYPE_BANK_CARD返回结果
    public String text;//

    // 以下为type为TYPE_ID_CARD_FRONT或TYPE_ID_CARD_BACK返回结果
    // TYPE_ID_CARD_FRONT
    public String cardNum;// 身份证号
    public String name;// 名字
    public String sex;// 性别
    public String address;// 地址
    public String nation;// 民族
    public String birth;// 出生年月日：yyyy-MM-dd
    // TYPE_ID_CARD_BACK
    public String office;// 签发机关
    public String validDate;// 有限期限：yyyyMMdd-yyyyMMdd

    @Override
    public String toString() {
        if (type == TYPE_CODE) {
            return "类型：二维码/条码\n" + "结果：" + text;
        }
        if (type == TYPE_ID_CARD_FRONT) {
            return "类型：身份证人头面\n身份证号：" + cardNum
                    + "\n姓名：" + name
                    + "\n性别：" + sex
                    + "\n民族：" + nation
                    + "\n出生：" + birth
                    + "\n地址：" + address;

        }
        if (type == TYPE_ID_CARD_BACK) {
            return "类型：身份证国徽面\n签发机关：" + office
                    + "\n有效期限：" + validDate;
        }
        if (type == TYPE_BANK_CARD) {
            return "类型：银行卡\n结果：" + text;
        }
        if (type == TYPE_LICENSE_PLATE) {
            return "类型：车牌\n结果：" + text;
        }
        return super.toString();
    }
}
