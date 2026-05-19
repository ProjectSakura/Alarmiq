package com.shen.alarmiq;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;

public class AlarmiqApp extends Application {

    public static final String ALARM_CHANNEL_ID = "alarmiq_active_alarm";
    public static final String TIMER_CHANNEL_ID = "alarmiq_active_timer";
    public static final String PREFS_SETTINGS = "alarmiq_settings";
    public static final String KEY_THEME = "theme_mode";

    @Override
    public void onCreate() {
        super.onCreate();
        applySavedTheme();
        DynamicColors.applyToActivitiesIfAvailable(this,
                new DynamicColorsOptions.Builder().build());
        createAlarmChannel();
        createTimerChannel();
    }

    private void applySavedTheme() {
        SharedPreferences prefs = getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE);
        int mode = prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    private void createAlarmChannel() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm == null) return;
        if (nm.getNotificationChannel(ALARM_CHANNEL_ID) != null) return;
        NotificationChannel channel = new NotificationChannel(
                ALARM_CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(getString(R.string.channel_desc));
        channel.setSound(null, null);
        channel.enableVibration(false);
        channel.setBypassDnd(true);
        channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(channel);
    }

    private void createTimerChannel() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm == null) return;
        if (nm.getNotificationChannel(TIMER_CHANNEL_ID) != null) return;
        NotificationChannel channel = new NotificationChannel(
                TIMER_CHANNEL_ID,
                getString(R.string.timer_channel_name),
                NotificationManager.IMPORTANCE_HIGH);
        channel.setSound(null, null);
        channel.enableVibration(false);
        channel.setBypassDnd(true);
        channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(channel);
    }
}
