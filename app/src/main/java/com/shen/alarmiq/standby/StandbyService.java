package com.shen.alarmiq.standby;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.shen.alarmiq.R;

public class StandbyService extends Service {

    private static final String CHANNEL_ID = "standby_monitor_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final String TAG = "StandbyService";
    
    private BroadcastReceiver chargingReceiver;
    private android.view.OrientationEventListener orientationEventListener;
    private boolean isPluggedIn = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
        
        chargingReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "Charging state changed: " + intent.getAction());
                updateChargingState();
            }
        };
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(chargingReceiver, filter);

        setupOrientationListener();
        updateChargingState();
    }

    private void updateChargingState() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);
        if (batteryStatus != null) {
            int plugged = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            boolean nowPlugged = plugged == BatteryManager.BATTERY_PLUGGED_AC ||
                                plugged == BatteryManager.BATTERY_PLUGGED_USB ||
                                plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS;
            
            Log.d(TAG, "Plugged state: " + nowPlugged + " (value: " + plugged + ")");
            
            if (nowPlugged != isPluggedIn) {
                isPluggedIn = nowPlugged;
                if (isPluggedIn) {
                    orientationEventListener.enable();
                } else {
                    orientationEventListener.disable();
                }
            }
        }
    }

    private void setupOrientationListener() {
        orientationEventListener = new android.view.OrientationEventListener(this, 
                android.hardware.SensorManager.SENSOR_DELAY_NORMAL) {
            private long lastTriggerTime = 0;
            private static final long DEBOUNCE_MS = 3000;

            @Override
            public void onOrientationChanged(int orientation) {
                if (orientation == ORIENTATION_UNKNOWN) return;

                boolean isPhysicalLandscape = (orientation >= 60 && orientation <= 120) || 
                                             (orientation >= 240 && orientation <= 300);

                if (isPhysicalLandscape && isPluggedIn) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastTriggerTime > DEBOUNCE_MS) {
                        Log.d(TAG, "Standby trigger: Angle " + orientation);
                        lastTriggerTime = currentTime;
                        
                        Intent intent = new Intent(StandbyService.this, StandbyActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK 
                                | Intent.FLAG_ACTIVITY_SINGLE_TOP 
                                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        try {
                            startActivity(intent);
                        } catch (Exception e) {
                            Intent triggerIntent = new Intent("com.shen.alarmiq.intent.action.TRIGGER_STANDBY");
                            triggerIntent.setPackage(getPackageName());
                            sendBroadcast(triggerIntent);
                        }
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
            channel.setSound(null, null);
            channel.enableVibration(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.standby_mode))
                .setContentText(getString(R.string.standby_enabled_desc))
                .setSmallIcon(R.drawable.ic_alarm)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // Xiaomi/OnePlus specific: Swiping away from recents kills the service.
        // We schedule a restart via AlarmManager as a "life-raft".
        Log.d(TAG, "onTaskRemoved: Scheduling service restart for OEM persistence");
        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
        restartServiceIntent.setPackage(getPackageName());

        PendingIntent restartServicePendingIntent = PendingIntent.getService(
                getApplicationContext(), 1, restartServiceIntent, 
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        
        AlarmManager alarmService = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        if (alarmService != null) {
            // Restart in 1 second
            alarmService.set(AlarmManager.ELAPSED_REALTIME, 
                    SystemClock.elapsedRealtime() + 1000, 
                    restartServicePendingIntent);
        }
        
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (chargingReceiver != null) {
            unregisterReceiver(chargingReceiver);
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
