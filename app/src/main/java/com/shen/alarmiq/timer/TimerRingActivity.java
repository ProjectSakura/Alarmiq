package com.shen.alarmiq.timer;

import android.app.KeyguardManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.shen.alarmiq.R;

public class TimerRingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        setShowWhenLocked(true);
        setTurnScreenOn(true);

        KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        if (km != null) km.requestDismissKeyguard(this, null);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_timer_ring);

        View root = findViewById(R.id.timerRingRoot);
        int basePx = (int) (24 * getResources().getDisplayMetrics().density);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left + basePx, bars.top + basePx,
                    bars.right + basePx, bars.bottom + basePx);
            return insets;
        });

        MaterialButton dismiss = findViewById(R.id.btnDismiss);
        dismiss.setOnClickListener(v -> dismissTimer());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                dismissTimer();
            }
        });
    }

    private void dismissTimer() {
        stopService(new Intent(this, TimerSoundService.class));
        finishAndRemoveTask();
    }
}
