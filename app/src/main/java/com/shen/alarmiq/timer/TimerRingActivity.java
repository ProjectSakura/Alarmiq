package com.shen.alarmiq.timer;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
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

    private boolean isFinished = false;

    private final BroadcastReceiver stopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TimerSoundService.ACTION_STOPPED.equals(intent.getAction())) {
                doDismiss();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        com.shen.alarmiq.AlarmiqApp.applySavedTheme(this);
        super.onCreate(savedInstanceState);

        // Optimized screen setup for instant appearance
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            if (km != null) km.requestDismissKeyguard(this, null);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }

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

        IntentFilter filter = new IntentFilter(TimerSoundService.ACTION_STOPPED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stopReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(stopReceiver, filter);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isFinished) {
            finishAndRemoveTask();
            return;
        }
        hideSystemUI();
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (isFinished) return;
        // Faster re-entry if the user swipes away during ringing
        Intent intent = new Intent(this, TimerRingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    private void dismissTimer() {
        stopService(new Intent(this, TimerSoundService.class));
        doDismiss();
    }

    private void doDismiss() {
        if (!isFinished) {
            isFinished = true;
            finishAndRemoveTask();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(stopReceiver);
        } catch (Exception ignored) {}
    }
}
