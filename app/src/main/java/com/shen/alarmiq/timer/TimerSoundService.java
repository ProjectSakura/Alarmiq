package com.shen.alarmiq.timer;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

import com.shen.alarmiq.AlarmiqApp;
import com.shen.alarmiq.R;

public class TimerSoundService extends Service {

    public static final String ACTION_DISMISS = "com.shen.alarmiq.timer.ACTION_DISMISS";
    public static final String ACTION_STOPPED = "com.shen.alarmiq.timer.ACTION_STOPPED";

    private static final int NOTIFICATION_ID = 0xA1A3;

    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private PowerManager.WakeLock wakeLock;
    private int playCount = 0;
    private boolean isStopped = false;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_DISMISS.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        
        isStopped = false;

        // 1. Immediately launch Activity (highest priority)
        launchRingActivity();

        // 2. Start Foreground Service requirements
        int fgType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                : 0;
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), fgType);
        
        // 3. Acquire WakeLock
        acquireWakeLock();
        
        // 4. Start Sound and Vibration
        startRingtone();
        startVibration();
        
        return START_STICKY;
    }

    private android.app.Notification buildNotification() {
        Intent open = new Intent(this, TimerRingActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentPi = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, AlarmiqApp.TIMER_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(getString(R.string.timer_done))
                .setContentText(getString(R.string.dismiss_timer))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setOngoing(true)
                .setAutoCancel(false)
                .setFullScreenIntent(contentPi, true)
                .setContentIntent(contentPi)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();
    }

    private void launchRingActivity() {
        Intent open = new Intent(this, TimerRingActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        startActivity(open);
    }

    private void startRingtone() {
        // Run on background thread to avoid blocking main thread
        new Thread(() -> {
            try {
                TimerEngine engine = TimerEngine.get(this);
                int repeatCount = engine.getRepeatCount();
                
                // Use a standard MediaPlayer flow for faster initialization
                MediaPlayer mp = MediaPlayer.create(this, R.raw.timer_end, 
                    new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(), 
                    0);
                
                if (mp == null) {
                    if (repeatCount > 0) stopSelf();
                    return;
                }

                synchronized (this) {
                    if (isStopped) {
                        mp.release();
                        return;
                    }
                    mediaPlayer = mp;
                }

                if (repeatCount == 0) {
                    mediaPlayer.setLooping(true);
                } else {
                    mediaPlayer.setLooping(false);
                    mediaPlayer.setOnCompletionListener(doneMp -> {
                        synchronized (this) {
                            if (isStopped) return;
                            playCount++;
                            if (playCount < repeatCount) {
                                doneMp.start();
                            } else {
                                stopSelf();
                            }
                        }
                    });
                }
                
                mediaPlayer.start();
            } catch (Exception e) {
                if (TimerEngine.get(this).getRepeatCount() > 0) {
                    stopSelf();
                }
            }
        }).start();
    }

    private void startVibration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = vm != null ? vm.getDefaultVibrator() : null;
        } else {
            vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        }
        if (vibrator == null || !vibrator.hasVibrator()) return;
        
        TimerEngine engine = TimerEngine.get(this);
        int repeatIndex = (engine.getRepeatCount() == 0) ? 0 : -1;
        
        long[] pattern = {0, 400, 300, 400, 300};
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, repeatIndex));
    }

    private void acquireWakeLock() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "Alarmiq:TimerWakeLock");
        wakeLock.setReferenceCounted(false);
        wakeLock.acquire(10 * 60 * 1000L);
    }

    @Override
    public void onDestroy() {
        synchronized (this) {
            isStopped = true;
        }
        super.onDestroy();
        if (mediaPlayer != null) {
            try { mediaPlayer.stop(); } catch (Exception ignored) {}
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (vibrator != null) {
            vibrator.cancel();
            vibrator = null;
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.cancel(NOTIFICATION_ID);
        
        // Notify activity to finish.
        Intent stoppedIntent = new Intent(ACTION_STOPPED);
        stoppedIntent.setPackage(getPackageName());
        sendBroadcast(stoppedIntent);
    }
}
