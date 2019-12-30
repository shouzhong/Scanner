package com.shouzhong.idcard2;

import android.text.TextUtils;
import android.util.Log;

import com.ym.idcard.reg.NativeOcr;

import org.json.JSONObject;

public class IdCard2Utils {

    public static String decode(byte[] data, int width, int height) {
        try {
            String s = NativeOcr.getInstance().getResult();
            if (!TextUtils.isEmpty(s)) return json(s);
            NativeOcr.getInstance().decode(data, width, height);
            return json(NativeOcr.getInstance().getResult());
        } catch (Exception e) {
            Log.e("DrivingLicenceUtils", "Exception:" + e.getMessage());
        }
        return null;
    }

    public static void close() {
        NativeOcr.close();
    }


    private static String json(String s) {
        if (TextUtils.isEmpty(s)) return s;
        try {
            JSONObject o = new JSONObject(s);
            JSONObject out = new JSONObject();
            String cardNum = o.getJSONObject("Num").optString("value");
            if (!TextUtils.isEmpty(cardNum)) {
                out.put("cardNumber", cardNum);
                out.put("name", o.getJSONObject("Name").optString("value"));
                out.put("sex", o.getJSONObject("Sex").optString("value"));
                out.put("nation", o.getJSONObject("Folk").optString("value"));
                String birth = o.getJSONObject("Birt").optString("value");
                if (!TextUtils.isEmpty(birth)) {
                    birth = birth.replaceAll("[年|月]", "-").replaceAll("[日| ]", "");
                    out.put("birth", birth);
                }
                out.put("address", o.getJSONObject("Addr").optString("value"));
            } else {
                out.put("organization", o.getJSONObject("Issue").optString("value"));
                String valid = o.getJSONObject("Valid").optString("value");
                if (!TextUtils.isEmpty(valid)) {
                    valid = valid.replaceAll("[-| ]", "").replace("至", "-");
                    out.put("validPeriod", valid);
                }
            }
            return out.toString();
        } catch (Exception e) {}
        return null;
    }
}
