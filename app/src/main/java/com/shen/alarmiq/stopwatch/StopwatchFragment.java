package com.shen.alarmiq.stopwatch;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.shen.alarmiq.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StopwatchFragment extends Fragment {

    private TextView txtMain;
    private TextView txtFraction;
    private MaterialButton btnStartPause;
    private MaterialButton btnLap;
    private MaterialButton btnReset;
    private RecyclerView recyclerLaps;

    private boolean running = false;
    /** Cumulative elapsed at last pause. */
    private long accumulatedMs = 0L;
    /** elapsedRealtime when the current run started; 0 when paused. */
    private long runStartElapsed = 0L;
    private final List<Long> laps = new ArrayList<>();

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            updateDisplay();
            handler.postDelayed(this, 60);
        }
    };

    private LapAdapter lapAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stopwatch, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        txtMain = view.findViewById(R.id.txtMain);
        txtFraction = view.findViewById(R.id.txtFraction);
        btnStartPause = view.findViewById(R.id.btnStartPause);
        btnLap = view.findViewById(R.id.btnLap);
        btnReset = view.findViewById(R.id.btnReset);
        recyclerLaps = view.findViewById(R.id.recyclerLaps);

        lapAdapter = new LapAdapter();
        recyclerLaps.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerLaps.setAdapter(lapAdapter);

        ViewCompat.setOnApplyWindowInsetsListener(recyclerLaps, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(),
                    v.getPaddingRight(), bars.bottom + (int) (24 * getResources().getDisplayMetrics().density));
            return insets;
        });

        btnStartPause.setOnClickListener(v -> toggle());
        btnLap.setOnClickListener(v -> recordLap());
        btnReset.setOnClickListener(v -> reset());

        updateDisplay();
        syncButtons();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (running) handler.post(tick);
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    private long currentElapsedMs() {
        if (running) {
            return accumulatedMs + (SystemClock.elapsedRealtime() - runStartElapsed);
        }
        return accumulatedMs;
    }

    private void toggle() {
        if (running) {
            accumulatedMs += SystemClock.elapsedRealtime() - runStartElapsed;
            runStartElapsed = 0L;
            running = false;
            handler.removeCallbacks(tick);
        } else {
            runStartElapsed = SystemClock.elapsedRealtime();
            running = true;
            handler.post(tick);
        }
        syncButtons();
    }

    private void reset() {
        running = false;
        runStartElapsed = 0L;
        accumulatedMs = 0L;
        handler.removeCallbacks(tick);
        laps.clear();
        lapAdapter.notifyDataSetChanged();
        updateDisplay();
        syncButtons();
    }

    private void recordLap() {
        laps.add(0, currentElapsedMs());
        lapAdapter.notifyItemInserted(0);
        recyclerLaps.scrollToPosition(0);
    }

    private void syncButtons() {
        btnStartPause.setText(running ? R.string.pause : R.string.start);
        btnLap.setEnabled(running);
        btnReset.setEnabled(!running && (accumulatedMs > 0 || !laps.isEmpty()));
    }

    private void updateDisplay() {
        long ms = currentElapsedMs();
        long totalSeconds = ms / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        long hundredths = (ms % 1000) / 10;
        if (hours > 0) {
            txtMain.setText(String.format(Locale.getDefault(), "%d:%02d:%02d",
                    hours, minutes, seconds));
        } else {
            txtMain.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
        }
        txtFraction.setText(String.format(Locale.getDefault(), ".%02d", hundredths));
    }

    private class LapAdapter extends RecyclerView.Adapter<LapAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_lap, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            long ms = laps.get(position);
            int displayNumber = laps.size() - position;
            holder.txtNum.setText(getString(R.string.lap_label, displayNumber));
            holder.txtTime.setText(formatLap(ms));
        }

        @Override
        public int getItemCount() {
            return laps.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final TextView txtNum;
            final TextView txtTime;
            VH(View v) {
                super(v);
                txtNum = v.findViewById(R.id.txtLapNum);
                txtTime = v.findViewById(R.id.txtLapTime);
            }
        }
    }

    private static String formatLap(long ms) {
        long totalSeconds = ms / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        long hundredths = (ms % 1000) / 10;
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d.%02d",
                    hours, minutes, seconds, hundredths);
        }
        return String.format(Locale.getDefault(), "%02d:%02d.%02d",
                minutes, seconds, hundredths);
    }
}
