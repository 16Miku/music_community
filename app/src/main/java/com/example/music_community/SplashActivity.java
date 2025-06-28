package com.example.music_community;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

// 实现 PrivacyDialogFragment.PrivacyDialogListener 接口，以便接收 Fragment 的回调
public class SplashActivity extends AppCompatActivity implements PrivacyDialogFragment.PrivacyDialogListener {

    private static final String PREFS_NAME = "MyPrefsFile"; // SharedPreferences 文件名
    private static final String KEY_PRIVACY_AGREED = "privacy_agreed"; // 隐私协议同意状态的键
    private static final long SPLASH_DISPLAY_LENGTH = 3000; // 闪屏页显示时长，3秒

    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        handler = new Handler(Looper.getMainLooper());


        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkPrivacyAgreement();
            }
        }, SPLASH_DISPLAY_LENGTH);
    }

    /**
     * 检查隐私协议同意状态，并决定是显示弹窗还是跳转主页
     */
    private void checkPrivacyAgreement() {


        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        boolean privacyAgreed = settings.getBoolean(KEY_PRIVACY_AGREED, false);

        if (privacyAgreed) {


            navigateToMain();
        } else {


            showPrivacyDialog();
        }
    }

    /**
     * 显示隐私协议弹窗 Fragment
     */
    private void showPrivacyDialog() {

        FragmentManager fragmentManager = getSupportFragmentManager();

        PrivacyDialogFragment privacyDialogFragment = new PrivacyDialogFragment();

        privacyDialogFragment.setCancelable(false);

        privacyDialogFragment.show(fragmentManager, "privacy_dialog");
    }

    /**
     * 跳转到主页 MainActivity
     */
    private void navigateToMain() {

        Intent intent = new Intent(SplashActivity.this, MainActivity.class);

        startActivity(intent);


        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        finish();
    }


    @Override
    public void onPrivacyAgreed() {


        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        SharedPreferences.Editor editor = settings.edit();


        editor.putBoolean(KEY_PRIVACY_AGREED, true);

        editor.apply();

        navigateToMain();
    }

    @Override
    public void onPrivacyDisagreed() {


        Toast.makeText(SplashActivity.this, "您已选择不同意隐私协议，应用将退出。", Toast.LENGTH_SHORT).show();

        finish();

        System.exit(0);
    }
}
