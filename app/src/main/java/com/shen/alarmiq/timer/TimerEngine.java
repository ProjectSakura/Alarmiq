package com.shen.alarmiq.timer;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * Singleton holding the one active countdown timer.
 *
 * <p>State is mirrored to SharedPreferences so the timer survives process death.
 * AlarmManager handles the actual expiry broadcast — the fragment only manages
 * the live countdown render.</p>
 */
public class TimerEngine {

    public enum State { IDLE, RUNNING, PAUSED }

    private static final String PREFS = "alarmiq_timer";
    private static final String KEY_STATE = "state";
    private static final String KEY_TOTAL = "total_ms";
    private static final String KEY_END = "end_elapsed_at_ms";
    private static final String KEY_REMAINING = "remaining_ms";
    private static final String KEY_REPEAT = "repeat_count";

    private static final int REQUEST_CODE = 0x7137E001;

    private static TimerEngine instance;

    public static synchronized TimerEngine get(Context ctx) {
        if (instance == null) instance = new TimerEngine(ctx.getApplicationContext());
        return instance;
    }

    private final Context appCtx;
    private final SharedPreferences prefs;

    private State state;
    private long totalMs;        // initial requested duration
    private long endAtMs;        // wall-clock time the timer expires (RUNNING only)
    private long remainingMs;    // remaining when paused
    private int repeatCount;     // 0 = infinite, 1 = once, 2 = twice

    private TimerEngine(Context ctx) {
        this.appCtx = ctx;
        this.prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        load();
    }

    private void load() {
        state = State.valueOf(prefs.getString(KEY_STATE, State.IDLE.name()));
        totalMs = prefs.getLong(KEY_TOTAL, 0L);
        endAtMs = prefs.getLong(KEY_END, 0L);
        remainingMs = prefs.getLong(KEY_REMAINING, 0L);
        repeatCount = prefs.getInt(KEY_REPEAT, 0);
        // If a running timer's end is already in the past, treat as idle.
        if (state == State.RUNNING && System.currentTimeMillis() >= endAtMs) {
            reset();
        }
    }

    private void save() {
        prefs.edit()
                .putString(KEY_STATE, state.name())
                .putLong(KEY_TOTAL, totalMs)
                .putLong(KEY_END, endAtMs)
                .putLong(KEY_REMAINING, remainingMs)
                .putInt(KEY_REPEAT, repeatCount)
                .apply();
    }

    public State getState() { return state; }
    public long getTotalMs() { return totalMs; }
    public int getRepeatCount() { return repeatCount; }

    public void setRepeatCount(int count) {
        this.repeatCount = count;
        save();
    }

    public long getRemainingMs() {
        if (state == State.RUNNING) {
            return Math.max(0L, endAtMs - System.currentTimeMillis());
        }
        if (state == State.PAUSED) return remainingMs;
        return totalMs;
    }

    public void start(long durationMs) {
        if (durationMs <= 0) return;
        totalMs = durationMs;
        endAtMs = System.currentTimeMillis() + durationMs;
        remainingMs = 0L;
        state = State.RUNNING;
        save();
        scheduleAlarm(endAtMs);
    }

    public void pause() {
        if (state != State.RUNNING) return;
        remainingMs = Math.max(0L, endAtMs - System.currentTimeMillis());
        endAtMs = 0L;
        state = State.PAUSED;
        save();
        cancelAlarm();
    }

    public void resume() {
        if (state != State.PAUSED) return;
        endAtMs = System.currentTimeMillis() + remainingMs;
        remainingMs = 0L;
        state = State.RUNNING;
        save();
        scheduleAlarm(endAtMs);
    }

    public void reset() {
        state = State.IDLE;
        totalMs = 0L;
        endAtMs = 0L;
        remainingMs = 0L;
        save();
        cancelAlarm();
    }

    /** Called by TimerReceiver after firing. */
    public void onExpired() {
        state = State.IDLE;
        endAtMs = 0L;
        remainingMs = 0L;
        // Preserve totalMs so the user can quickly re-fire the same duration.
        save();
    }

    private void scheduleAlarm(long triggerAtMillis) {
        AlarmManager am = (AlarmManager) appCtx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = pendingIntent();
        AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(triggerAtMillis, pi);
        am.setAlarmClock(info, pi);
    }

    private void cancelAlarm() {
        AlarmManager am = (AlarmManager) appCtx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        am.cancel(pendingIntent());
    }

    private PendingIntent pendingIntent() {
        Intent intent = new Intent(appCtx, TimerReceiver.class);
        intent.setAction(TimerReceiver.ACTION_FIRE);
        return PendingIntent.getBroadcast(appCtx, REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
