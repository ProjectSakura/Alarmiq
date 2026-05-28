package com.shen.alarmiq;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import com.shen.alarmiq.standby.StandbyReceiver;

public class AlarmiqApp extends Application {

    public static final String ALARM_CHANNEL_ID = "alarmiq_active_alarm";
    public static final String TIMER_CHANNEL_ID = "alarmiq_active_timer";
    public static final String PREFS_SETTINGS = "alarmiq_settings";
    public static final String KEY_THEME = "theme_mode";
    public static final String KEY_LAST_SYSTEM_THEME = "last_system_theme";

    @Override
    public void onCreate() {
        super.onCreate();
        applySavedTheme(this);
        DynamicColors.applyToActivitiesIfAvailable(this,
                new DynamicColorsOptions.Builder().build());
        createAlarmChannel();
        createTimerChannel();
        startStandbyServiceIfEnabled();
    }

    public void startStandbyServiceIfEnabled() {
        SharedPreferences prefs = getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE);
        boolean enabled = prefs.getBoolean(StandbyReceiver.KEY_STANDBY_ENABLED, true);
        if (enabled) {
            Intent serviceIntent = new Intent(this, com.shen.alarmiq.standby.StandbyService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        }
    }

    @Override
    public void onConfigurationChanged(@androidx.annotation.NonNull android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int currentSystemTheme = newConfig.uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        SharedPreferences prefs = getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE);
        int lastSystemTheme = prefs.getInt(KEY_LAST_SYSTEM_THEME, -1);
        
        if (lastSystemTheme != -1 && currentSystemTheme != lastSystemTheme) {
            // System theme changed while app is running, reset to follow system
            prefs.edit()
                    .putInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    .putInt(KEY_LAST_SYSTEM_THEME, currentSystemTheme)
                    .apply();
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    public static void applySavedTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE);
        
        // Use Resources.getSystem() to get the actual system theme, 
        // which is unaffected by AppCompatDelegate overrides in the current process.
        int currentSystemTheme = Resources.getSystem().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        int lastSystemTheme = prefs.getInt(KEY_LAST_SYSTEM_THEME, -1);
        
        int mode = prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        
        if (lastSystemTheme != -1 && currentSystemTheme != lastSystemTheme) {
            // System theme changed since last run, reset to follow system
            mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            prefs.edit().putInt(KEY_THEME, mode).apply();
        }
        
        prefs.edit().putInt(KEY_LAST_SYSTEM_THEME, currentSystemTheme).apply();
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
