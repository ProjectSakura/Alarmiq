package com.shen.alarmiq.alarm;

import android.app.KeyguardManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.format.DateFormat;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.shen.alarmiq.R;
import com.shen.alarmiq.challenge.AnswerMatcher;
import com.shen.alarmiq.challenge.Challenge;
import com.shen.alarmiq.challenge.ChallengeGenerator;
import com.shen.alarmiq.math.Difficulty;

import java.util.List;
import java.util.Locale;

public class AlarmRingActivity extends AppCompatActivity {

    public static boolean isActive = false;
    private static final double PARAGRAPH_SIMILARITY_THRESHOLD = 0.90;

    private long alarmId;
    private Alarm alarm;
    private Difficulty difficulty;
    private List<Challenge> challenges;
    private int challengeIndex = 0;

    private TextView txtRingTime;
    private TextView txtRingLabel;
    private TextView txtProgress;
    private TextView txtTypeLabel;
    private TextView txtPromptShort;
    private TextView txtPromptLong;
    private TextView txtFeedback;
    private TextInputLayout inputLayout;
    private TextInputEditText inputAnswer;
    private MaterialButton btnSubmit;
    private LinearProgressIndicator progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        com.shen.alarmiq.AlarmiqApp.applySavedTheme(this);
        super.onCreate(savedInstanceState);

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

        // Lockdown: Prevent dismissal via task switching if possible
        try {
            startLockTask();
        } catch (Exception ignored) {}

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_alarm_ring);

        View ringRoot = findViewById(R.id.ringRoot);
        int basePx = (int) (24 * getResources().getDisplayMetrics().density);
        ViewCompat.setOnApplyWindowInsetsListener(ringRoot, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left + basePx, bars.top + basePx,
                    bars.right + basePx, bars.bottom + basePx);
            return insets;
        });

        alarmId = getIntent().getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L);
        alarm = new AlarmStorage(this).getById(alarmId);
        difficulty = alarm != null ? alarm.difficulty : Difficulty.NORMAL;
        challenges = new ChallengeGenerator().generate(difficulty);

        txtRingTime = findViewById(R.id.txtRingTime);
        txtRingLabel = findViewById(R.id.txtRingLabel);
        txtProgress = findViewById(R.id.txtProgress);
        txtTypeLabel = findViewById(R.id.txtTypeLabel);
        txtPromptShort = findViewById(R.id.txtPromptShort);
        txtPromptLong = findViewById(R.id.txtPromptLong);
        txtFeedback = findViewById(R.id.txtFeedback);
        inputLayout = findViewById(R.id.inputLayout);
        inputAnswer = findViewById(R.id.inputAnswer);
        btnSubmit = findViewById(R.id.btnSubmit);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setMax(challenges.size());

        txtRingTime.setText(formatNow());
        if (alarm != null && alarm.label != null && !alarm.label.trim().isEmpty()) {
            txtRingLabel.setText(alarm.label);
        } else {
            txtRingLabel.setText(R.string.app_name);
        }

        btnSubmit.setOnClickListener(v -> trySubmit());
        inputAnswer.setOnEditorActionListener((tv, action, event) -> {
            if (action == EditorInfo.IME_ACTION_DONE || action == EditorInfo.IME_ACTION_GO) {
                trySubmit();
                return true;
            }
            return false;
        });

        renderChallenge();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // No exit until every challenge is done.
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        isActive = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActive = true;
        hideSystemUI();
        
        // Lockdown: Attempt to lock the task again if it was somehow unlocked
        try {
            startLockTask();
        } catch (Exception ignored) {}

        if (isInMultiWindowMode()) {
            relaunchMe();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isActive = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        isActive = false;
        if (!isFinishing()) {
            relaunchMe();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus && !isFinishing()) {
            // Focus lost (likely notification shade or system dialog).
            // Relaunch to pull back focus.
            new Handler(Looper.getMainLooper()).postDelayed(this::relaunchMe, 500);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isActive = false;
        try {
            stopLockTask();
        } catch (Exception ignored) {}
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        relaunchMe();
    }

    private void relaunchMe() {
        if (!isFinishing()) {
            Intent intent = new Intent(this, AlarmRingActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK 
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP 
                    | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        }
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

    private String formatNow() {
        boolean is24 = DateFormat.is24HourFormat(this);
        java.util.Calendar c = java.util.Calendar.getInstance();
        if (is24) {
            return String.format(Locale.getDefault(), "%02d:%02d",
                    c.get(java.util.Calendar.HOUR_OF_DAY),
                    c.get(java.util.Calendar.MINUTE));
        }
        int h = c.get(java.util.Calendar.HOUR);
        if (h == 0) h = 12;
        return String.format(Locale.getDefault(), "%d:%02d %s",
                h, c.get(java.util.Calendar.MINUTE),
                c.get(java.util.Calendar.AM_PM) == 0 ? "AM" : "PM");
    }

    private void renderChallenge() {
        Challenge ch = challenges.get(challengeIndex);
        txtProgress.setText(getString(R.string.problem_progress,
                challengeIndex + 1, challenges.size()));
        progressBar.setProgressCompat(challengeIndex, true);
        txtTypeLabel.setText(ch.labelRes);
        txtFeedback.setVisibility(View.INVISIBLE);

        // Configure per type
        switch (ch.type) {
            case MATH:
                txtPromptShort.setVisibility(View.VISIBLE);
                txtPromptShort.setText(ch.prompt);
                txtPromptShort.setTextAppearance(
                        com.google.android.material.R.style.TextAppearance_Material3_DisplaySmall);
                txtPromptLong.setVisibility(View.GONE);
                inputLayout.setVisibility(View.VISIBLE);
                inputLayout.setHint("=");
                inputAnswer.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
                inputAnswer.setMaxLines(1);
                inputAnswer.setText("");
                btnSubmit.setEnabled(true);
                btnSubmit.setText(R.string.submit);
                break;
            case REVERSE_TYPE:
                txtPromptShort.setVisibility(View.GONE);
                txtPromptLong.setVisibility(View.VISIBLE);
                txtPromptLong.setText(ch.prompt);
                inputLayout.setVisibility(View.VISIBLE);
                inputLayout.setHint(getString(R.string.challenge_reverse_hint));
                inputAnswer.setInputType(InputType.TYPE_CLASS_TEXT);
                inputAnswer.setMaxLines(2);
                inputAnswer.setText("");
                btnSubmit.setEnabled(true);
                btnSubmit.setText(R.string.submit);
                break;
            case PARAGRAPH:
                txtPromptShort.setVisibility(View.GONE);
                txtPromptLong.setVisibility(View.VISIBLE);
                txtPromptLong.setText(ch.prompt);
                inputLayout.setVisibility(View.VISIBLE);
                inputLayout.setHint(getString(R.string.challenge_paragraph_hint));
                inputAnswer.setInputType(InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
                inputAnswer.setMaxLines(6);
                inputAnswer.setText("");
                btnSubmit.setEnabled(true);
                btnSubmit.setText(R.string.submit);
                break;
        }

        inputAnswer.requestFocus();
    }

    private void trySubmit() {
        Challenge ch = challenges.get(challengeIndex);
        boolean ok;
        switch (ch.type) {
            case MATH: {
                String raw = String.valueOf(inputAnswer.getText()).trim();
                if (raw.isEmpty()) { wrong(R.string.incorrect_try_again); return; }
                try {
                    int given = Integer.parseInt(raw);
                    ok = String.valueOf(given).equals(ch.expected);
                } catch (NumberFormatException e) { ok = false; }
                if (!ok) { wrong(R.string.incorrect_try_again); return; }
                break;
            }
            case REVERSE_TYPE: {
                String raw = String.valueOf(inputAnswer.getText());
                ok = AnswerMatcher.exactNormalized(ch.expected, raw);
                if (!ok) { wrong(R.string.incorrect_try_again); return; }
                break;
            }
            case PARAGRAPH: {
                String raw = String.valueOf(inputAnswer.getText());
                ok = AnswerMatcher.similar(ch.expected, raw, PARAGRAPH_SIMILARITY_THRESHOLD);
                if (!ok) { wrong(R.string.challenge_match_too_low); return; }
                break;
            }
            default:
                ok = false;
        }
        advance();
    }

    private void wrong(int messageRes) {
        txtFeedback.setText(messageRes);
        txtFeedback.setVisibility(View.VISIBLE);
        inputAnswer.setText("");
        inputAnswer.animate()
                .translationX(20f).setDuration(60)
                .withEndAction(() -> inputAnswer.animate()
                        .translationX(-20f).setDuration(60)
                        .withEndAction(() -> inputAnswer.animate()
                                .translationX(0f).setDuration(60).start()).start()).start();
    }

    private void advance() {
        challengeIndex++;
        if (challengeIndex >= challenges.size()) {
            dismissAlarm();
        } else {
            renderChallenge();
        }
    }

    private void dismissAlarm() {
        try {
            stopLockTask();
        } catch (Exception ignored) {}
        stopService(new Intent(this, AlarmRingingService.class));
        finishAndRemoveTask();
    }
}
