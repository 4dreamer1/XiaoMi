package net.micode.notes.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import net.micode.notes.R;

public class WelcomeActivity extends AppCompatActivity {
    private TextView tvCountdown;
    private CountDownTimer countDownTimer;
    // 将 timeLeftInMillis 声明为 final，因为它不再被修改
    private static final long TIME_LEFT_IN_MILLIS = 3000; // 设置计时时长为3秒，单位为毫秒

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // 初始化控件
        tvCountdown = findViewById(R.id.tv_countdown);
        Button btnSkip = findViewById(R.id.btn_skip);  // 将 btnSkip 移至局部变量

        // 启动倒计时
        startCountdown();

        // 设置跳过按钮点击事件
        btnSkip.setOnClickListener(v -> goToMainActivity());

        // 使用 View.postDelayed() 替代 Handler
        tvCountdown.postDelayed(() -> {
            // 倒计时结束，隐藏倒计时文本，显示跳过按钮
            tvCountdown.setVisibility(View.GONE);  // 隐藏倒计时
            btnSkip.setVisibility(View.VISIBLE);  // 显示跳过按钮
        }, TIME_LEFT_IN_MILLIS);  // 3秒后显示
    }

    // 启动倒计时的方法
    private void startCountdown() {
        countDownTimer = new CountDownTimer(TIME_LEFT_IN_MILLIS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsRemaining = (int) (millisUntilFinished / 1000);
                // 使用资源字符串，避免文本拼接
                tvCountdown.setText(getString(R.string.countdown_text, secondsRemaining));  // 更新倒计时显示
            }

            @Override
            public void onFinish() {
                // 倒计时结束时不做任何操作
            }
        }.start();
    }

    // 跳转到主界面
    private void goToMainActivity() {
        Intent intent = new Intent(WelcomeActivity.this, NotesListActivity.class);
        startActivity(intent);
        finish();  // 结束当前欢迎页面，防止返回到欢迎页面
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();  // 在销毁时取消倒计时
        }
    }
}
