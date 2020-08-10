package com.shouzhong.drivinglicense;

import android.text.TextUtils;
import android.util.Log;

import com.ym.idcard.reg.NativeOcrJz;

import org.json.JSONObject;

public class DrivingLicenseUtils {

    public static String decode(byte[] data, int width, int height) {
        try {
            String s = NativeOcrJz.getInstance().getResult();
            if (!TextUtils.isEmpty(s)) return json(s);
            NativeOcrJz.getInstance().decode(data, width, height);
            return json(NativeOcrJz.getInstance().getResult());
        } catch (Exception e) {
            Log.e("DrivingLicenceUtils", "Exception:" + e.getMessage());
        }
        return null;
    }

    public static void close() {
        NativeOcrJz.close();
    }

    private static String json(String s) {
        if (TextUtils.isEmpty(s)) return s;
        try {
            JSONObject o = new JSONObject(s);
            JSONObject out = new JSONObject();
            out.put("cardNumber", o.optString("Num"));
            out.put("name", o.optString("Name"));
            out.put("sex", o.optString("Sex"));
            out.put("nationality", o.optString("Nation"));
            out.put("address", o.optString("Addr"));
            String birth = o.optString("Birt");
            if (!TextUtils.isEmpty(birth)) {
                birth = birth.replaceAll("[年|月]", "-").replaceAll("[日| ]", "");
                out.put("birth", birth);
            }
            out.put("firstIssue", o.optString("Issue"));
            out.put("_class", o.optString("DrivingType"));
            String valid = o.optString("RegisterDate");
            if (!TextUtils.isEmpty(valid)) {
                valid = valid.replaceAll("[-| ]", "").replace("至", "-");
                out.put("validPeriod", valid);
            }
            return out.toString();
        } catch (Exception e) {}
        return null;
    }
}
