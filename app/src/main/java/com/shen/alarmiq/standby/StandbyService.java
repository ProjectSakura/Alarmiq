package com.shen.alarmiq.standby;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.shen.alarmiq.R;

public class StandbyService extends Service {

    private static final String CHANNEL_ID = "standby_monitor_channel";
    private static final int NOTIFICATION_ID = 1001;
    
    private StandbyReceiver standbyReceiver;
    private android.view.OrientationEventListener orientationEventListener;
    private boolean isPluggedIn = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
        
        standbyReceiver = new StandbyReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                super.onReceive(context, intent);
                updateChargingState();
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        filter.addAction(Intent.ACTION_DOCK_EVENT);
        registerReceiver(standbyReceiver, filter);

        setupOrientationListener();
        updateChargingState();
    }

    private void updateChargingState() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);
        if (batteryStatus != null) {
            int plugged = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, -1);
            isPluggedIn = plugged == android.os.BatteryManager.BATTERY_PLUGGED_AC ||
                          plugged == android.os.BatteryManager.BATTERY_PLUGGED_USB ||
                          plugged == android.os.BatteryManager.BATTERY_PLUGGED_WIRELESS;
            
            if (isPluggedIn) {
                orientationEventListener.enable();
            } else {
                orientationEventListener.disable();
            }
        }
    }

    private void setupOrientationListener() {
        orientationEventListener = new android.view.OrientationEventListener(this, 
                android.hardware.SensorManager.SENSOR_DELAY_NORMAL) {
            private long lastTriggerTime = 0;
            private static final long DEBOUNCE_MS = 2000; // Prevent rapid re-triggers

            @Override
            public void onOrientationChanged(int orientation) {
                if (orientation == ORIENTATION_UNKNOWN) return;

                // Check if physical orientation is horizontal (landscape)
                // 90 is landscape right, 270 is landscape left
                // We use a wider margin (30-40 degrees) to be more forgiving of slight tilts
                boolean isPhysicalLandscape = (orientation >= 45 && orientation <= 135) || 
                                             (orientation >= 225 && orientation <= 315);

                if (isPhysicalLandscape && isPluggedIn) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastTriggerTime > DEBOUNCE_MS) {
                        Log.d("StandbyService", "Physical landscape detected (angle: " + orientation + "), triggering Standby");
                        lastTriggerTime = currentTime;
                        
                        Intent triggerIntent = new Intent("com.shen.alarmiq.intent.action.TRIGGER_STANDBY");
                        triggerIntent.setPackage(getPackageName());
                        sendBroadcast(triggerIntent);
                    }
                }
            }
        };
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.standby_mode),
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription(getString(R.string.standby_enabled_desc));
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.standby_mode))
                .setContentText(getString(R.string.standby_enabled_desc))
                .setSmallIcon(R.drawable.ic_alarm)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (standbyReceiver != null) {
            unregisterReceiver(standbyReceiver);
        }
        if (orientationEventListener != null) {
            orientationEventListener.disable();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
