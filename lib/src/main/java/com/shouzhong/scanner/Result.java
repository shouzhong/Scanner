package com.shouzhong.scanner;

/**
 * 识别结果
 *
 */
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
    // 驾驶证
    public static final int TYPE_DRIVING_LICENSE = 5;

    // 类型
    public int type;
    // 图片路径
    public String path;
    // 数据
    public String data;
    // 以下是对data的说明
    // 当type为TYPE_CODE，TYPE_BANK_CARD，TYPE_LICENSE_PLATE时，data为字符串
    // 当type为TYPE_ID_CARD_FRONT时，data为json字符串，格式如下
    // {
    //      "cardNumber": "21412412421",// 身份证号
    //      "name": "张三",// 姓名
    //      "sex": "男",// 性别
    //      "nation": "汉",// 民族
    //      "birth": "1999-01-01",// 出生
    //      "address": "地址"// 地址
    // }
    // 当type为TYPE_ID_CARD_BACK时，data为json字符串，格式如下
    // {
    //      "organization": "签发机关",// 签发机关
    //      "validPeriod": "20180101-20380101"// 有效期限
    // }
    // 当type为TYPE_DRIVING_LICENSE时，data为json字符串，格式如下
    // {
    //      "cardNumber": "43623446432",// 证号
    //      "name": "张三",// 姓名
    //      "sex": "男",// 性别
    //      "nationality": "中国",// 国籍
    //      "address": "地址",// 地址
    //      "birth": "1999-01-01",// 出生日期
    //      "firstIssue": "2018-01-01",// 初次领证日期
    //      "_class": "C1",// 准驾车型
    //      "validPeriod": "20180101-20240101"// 有效期限
    // }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("类型：");
        if (type == TYPE_CODE) sb.append("二维码/条码");
        else if (type == TYPE_ID_CARD_FRONT) sb.append("身份证人头面");
        else if (type == TYPE_ID_CARD_BACK) sb.append("身份证国徽面");
        else if (type == TYPE_BANK_CARD) sb.append("银行卡");
        else if (type == TYPE_LICENSE_PLATE) sb.append("车牌");
        else if (type == TYPE_DRIVING_LICENSE) sb.append("驾驶证");
        else sb.append("未知类型（").append(type).append("）");
        sb.append("\n").append(data);
        return sb.toString();
    }
}
