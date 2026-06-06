package com.shen.alarmiq.standby;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.shen.alarmiq.AlarmiqApp;

public class StandbyReceiver extends BroadcastReceiver {

    private static final String TAG = "StandbyReceiver";
    public static final String KEY_STANDBY_ENABLED = "standby_enabled";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "onReceive: " + action);
        
        SharedPreferences prefs = context.getSharedPreferences(AlarmiqApp.PREFS_SETTINGS, Context.MODE_PRIVATE);
        boolean enabled = prefs.getBoolean(KEY_STANDBY_ENABLED, true);

        if (!enabled) {
            Log.d(TAG, "Standby is disabled in settings");
            return;
        }

        // OnePlus/Xiaomi specific: Always attempt to ensure service is running on power events
        if (Intent.ACTION_POWER_CONNECTED.equals(action) || 
            Intent.ACTION_BOOT_COMPLETED.equals(action) || 
            "android.intent.action.LOCKED_BOOT_COMPLETED".equals(action)) {
            
            Intent serviceIntent = new Intent(context, StandbyService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        }

        if ("com.shen.alarmiq.intent.action.TRIGGER_STANDBY".equals(action)) {
            // Check for overlay permission on Android 10+ for background start
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(context)) {
                    Log.d(TAG, "Missing overlay permission - cannot launch Standby from background");
                    return;
                }
            }
            
            Log.d(TAG, "Attempting to launch StandbyActivity via TRIGGER_STANDBY");
            checkAndLaunchStandby(context);
        }
    }

    private void checkAndLaunchStandby(Context context) {
        Intent standbyIntent = new Intent(context, StandbyActivity.class);
        standbyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK 
                | Intent.FLAG_ACTIVITY_SINGLE_TOP 
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try {
            context.startActivity(standbyIntent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start StandbyActivity", e);
        }
    }
}
