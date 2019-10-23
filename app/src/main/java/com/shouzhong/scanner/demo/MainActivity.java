package com.shouzhong.scanner.demo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.shouzhong.scanner.ScannerUtils;

public class MainActivity extends AppCompatActivity {

    private final String TAG = getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onClickBtn(View v) {
        Intent intent = new Intent(this, ScannerActivity.class);
        startActivity(intent);
    }

    public void onClickBtn2(View v) {
//        String path = getExternalCacheDir() + "/img1571715865515.jpg";
//        try {
//            Bitmap bmp = BitmapFactory.decodeFile(path);
////            Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.cc);
//            Log.e(TAG, ScannerUtils.decodeBank(bmp));
//        } catch (Exception e) {
//            Log.e(TAG, e.getMessage());
//        }
        try {
            Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.dd);
            Log.e(TAG, ScannerUtils.decodeIdCard(this, bmp).toString());
        } catch (Exception e) {}
    }


}
