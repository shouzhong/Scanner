package com.shouzhong.scanner.demo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.shouzhong.scanner.ScannerUtils;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private final String TAG = getClass().getSimpleName();

    TextView tv;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            tv.setText((String) msg.obj);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv = findViewById(R.id.tv);
    }

    public void onClickBtn(View v) {
        Intent intent = new Intent(this, ScannerActivity.class);
        startActivity(intent);
    }

    public void onClickBtn2(View v) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String path = getExternalFilesDir("image").getAbsolutePath() + "/a.jpg";
                String s = ScannerUtils.decodeText(path);
                Message msg = handler.obtainMessage();
                msg.what = 0;
                msg.obj = TextUtils.isEmpty(s) ? "识别失败" : s;
                handler.sendMessage(msg);
            }
        }).start();
    }

}
