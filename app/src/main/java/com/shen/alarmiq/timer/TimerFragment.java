package com.shen.alarmiq.timer;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.shen.alarmiq.R;

import java.util.Locale;

public class TimerFragment extends Fragment {

    private TimerEngine engine;

    private LinearLayout inputGroup;
    private LinearLayout countdownGroup;
    private TextInputEditText inputHours;
    private TextInputEditText inputMinutes;
    private TextInputEditText inputSeconds;
    private TextView txtCountdown;
    private TextView txtTotal;
    private MaterialButton btnStart;
    private MaterialButton btnReset;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            refresh();
            if (engine.getState() == TimerEngine.State.RUNNING) {
                handler.postDelayed(this, 200);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_timer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        engine = TimerEngine.get(requireContext());

        inputGroup = view.findViewById(R.id.inputGroup);
        countdownGroup = view.findViewById(R.id.countdownGroup);
        inputHours = view.findViewById(R.id.inputHours);
        inputMinutes = view.findViewById(R.id.inputMinutes);
        inputSeconds = view.findViewById(R.id.inputSeconds);
        txtCountdown = view.findViewById(R.id.txtCountdown);
        txtTotal = view.findViewById(R.id.txtTotal);
        btnStart = view.findViewById(R.id.btnTimerStart);
        btnReset = view.findViewById(R.id.btnTimerReset);

        btnStart.setOnClickListener(v -> onPrimaryAction());
        btnReset.setOnClickListener(v -> {
            engine.reset();
            refresh();
        });

        View scrollView = view.findViewById(R.id.timerScroll);
        int basePx = (int) (24 * getResources().getDisplayMetrics().density);
        ViewCompat.setOnApplyWindowInsetsListener(scrollView, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(),
                    v.getPaddingRight(), bars.bottom + basePx);
            return insets;
        });

        refresh();
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
        if (engine.getState() == TimerEngine.State.RUNNING) {
            handler.post(tick);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    private void onPrimaryAction() {
        TimerEngine.State state = engine.getState();
        if (state == TimerEngine.State.RUNNING) {
            engine.pause();
            handler.removeCallbacks(tick);
        } else if (state == TimerEngine.State.PAUSED) {
            engine.resume();
            handler.post(tick);
        } else {
            long durationMs = readDurationMs();
            if (durationMs <= 0) return;
            engine.start(durationMs);
            handler.post(tick);
        }
        refresh();
    }

    private long readDurationMs() {
        int h = parseField(inputHours, 99);
        int m = parseField(inputMinutes, 59);
        int s = parseField(inputSeconds, 59);
        long total = (long) h * 3600_000L + (long) m * 60_000L + (long) s * 1000L;
        return total;
    }

    private int parseField(TextInputEditText field, int max) {
        String s = String.valueOf(field.getText()).trim();
        if (s.isEmpty()) return 0;
        try {
            int v = Integer.parseInt(s);
            if (v < 0) return 0;
            if (v > max) return max;
            return v;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void refresh() {
        TimerEngine.State state = engine.getState();
        long remaining = engine.getRemainingMs();
        long total = engine.getTotalMs();

        boolean idle = state == TimerEngine.State.IDLE;
        inputGroup.setVisibility(idle ? View.VISIBLE : View.GONE);
        countdownGroup.setVisibility(idle ? View.GONE : View.VISIBLE);

        if (!idle) {
            txtCountdown.setText(format(remaining));
            txtTotal.setText(getString(R.string.timer_of_total, format(total)));
        }

        switch (state) {
            case RUNNING:
                btnStart.setText(R.string.pause);
                btnReset.setEnabled(true);
                break;
            case PAUSED:
                btnStart.setText(R.string.resume);
                btnReset.setEnabled(true);
                break;
            case IDLE:
            default:
                btnStart.setText(R.string.start);
                btnReset.setEnabled(total > 0);
                break;
        }
    }

    private static String format(long ms) {
        long totalSeconds = (ms + 999) / 1000; // round up so 00:00 only at true zero
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }
}
