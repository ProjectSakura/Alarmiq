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

        // Check for overlay permission on Android 10+ for background start
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(context)) {
                Log.d(TAG, "Missing overlay permission - cannot launch Standby from background");
                // Optional: Show a notification here if we haven't told the user lately
                return;
            }
        }

        if (Intent.ACTION_POWER_CONNECTED.equals(action) || 
            Intent.ACTION_DOCK_EVENT.equals(action) ||
            "com.shen.alarmiq.intent.action.TRIGGER_STANDBY".equals(action)) {
            
            Log.d(TAG, "Attempting to launch StandbyActivity");
            checkAndLaunchStandby(context);
        }
    }

    private void checkAndLaunchStandby(Context context) {
        // Only launch if in landscape
        int orientation = context.getResources().getConfiguration().orientation;
        if (orientation != android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            Log.d(TAG, "Not launching Standby: Device is in portrait");
            return;
        }

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
