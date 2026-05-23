package com.shen.alarmiq.alarm;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

import com.shen.alarmiq.AlarmiqApp;
import com.shen.alarmiq.R;
import com.shen.alarmiq.math.Difficulty;

public class AlarmRingingService extends Service {

    private static final int NOTIFICATION_ID = 0xA1A2;
    /** Distinct request code so the guard PendingIntent doesn't collide with the regular alarm one. */
    private static final int GUARD_REQUEST_CODE = 0x7E5A0001;
    /** How often we enforce volume + alive checks (ms). */
    private static final long ENFORCE_INTERVAL_MS = 1500L;
    /** Delay after which a force-stop guard re-fires the alarm (ms). */
    private static final long GUARD_DELAY_MS = 90_000L;

    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private PowerManager.WakeLock wakeLock;
    private long currentAlarmId = -1L;

    private final Handler enforceHandler = new Handler(Looper.getMainLooper());
    private final Runnable enforce = new Runnable() {
        @Override
        public void run() {
            restoreAlarmVolume();
            ensureMediaPlayerRunning();
            
            // If the activity isn't active, periodically re-trigger it.
            if (!AlarmRingActivity.isActive) {
                Alarm alarm = new AlarmStorage(AlarmRingingService.this).getById(currentAlarmId);
                
                // For "None" mode, we don't aggressively force the activity back to the front 
                // every 1.5s if it's already hidden, as the user might be using the phone.
                if (alarm != null && alarm.difficulty != Difficulty.NONE) {
                    String label = (alarm.label != null && !alarm.label.trim().isEmpty())
                            ? alarm.label
                            : getString(R.string.alarm_notification_title);
                    
                    int fgType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                            ? ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                            : 0;
                    
                    // Force a total refresh of the notification to re-trigger fullScreenIntent
                    stopForeground(false);
                    ServiceCompat.startForeground(AlarmRingingService.this, 
                            NOTIFICATION_ID, 
                            buildNotification(currentAlarmId, label, false), 
                            fgType);
                    
                    // Use AlarmManager to force-launch the activity. 
                    forceLaunchActivity(currentAlarmId);
                }
            }
            
            enforceHandler.postDelayed(this, ENFORCE_INTERVAL_MS);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        long alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L);
        if (alarmId < 0) {
            stopSelf();
            return START_NOT_STICKY;
        }

        // Re-entrant start (e.g., anti-cheat guard fired while we're still running):
        // just refresh the guard, don't restart media or notification.
        if (currentAlarmId == alarmId && mediaPlayer != null) {
            scheduleAntiCheatGuard();
            return START_STICKY;
        }
        currentAlarmId = alarmId;

        Alarm alarm = new AlarmStorage(this).getById(alarmId);
        String label = (alarm != null && alarm.label != null && !alarm.label.trim().isEmpty())
                ? alarm.label
                : getString(R.string.alarm_notification_title);

        int fgType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                : 0;
        ServiceCompat.startForeground(this, NOTIFICATION_ID,
                buildNotification(alarmId, label, alarm != null && alarm.difficulty == Difficulty.NONE), fgType);
        acquireWakeLock();
        startRingtone(alarm);
        startVibration();
        launchRingActivity(alarmId);

        rescheduleIfRepeating(alarm);
        enforceHandler.removeCallbacks(enforce);
        enforceHandler.post(enforce);
        if (alarm != null && alarm.difficulty != Difficulty.NONE) {
            scheduleAntiCheatGuard();
        }
        return START_STICKY;
    }

    /**
     * Schedule a "guard" alarm 90 seconds in the future. If the user
     * force-stops Alarmiq before dismissing, this alarm will re-launch the
     * service. Cancelled on legitimate dismissal in {@link #onDestroy()}.
     */
    private void scheduleAntiCheatGuard() {
        if (currentAlarmId < 0) return;
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (am == null) return;
        long when = System.currentTimeMillis() + GUARD_DELAY_MS;
        PendingIntent pi = guardPendingIntent();
        try {
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(when, pi), pi);
        } catch (SecurityException ignored) {
            // setAlarmClock requires the USE_EXACT_ALARM permission already declared.
        }
    }

    private void cancelAntiCheatGuard() {
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = guardPendingIntent();
        am.cancel(pi);
        pi.cancel();
    }

    private PendingIntent guardPendingIntent() {
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.setAction(AlarmReceiver.ACTION_FIRE);
        intent.putExtra(AlarmScheduler.EXTRA_ALARM_ID, currentAlarmId);
        return PendingIntent.getBroadcast(this,
                GUARD_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void restoreAlarmVolume() {
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (am == null) return;
        int max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        int cur = am.getStreamVolume(AudioManager.STREAM_ALARM);
        // Restore if reduced below 70% of max — small adjustments by the user are OK.
        if (cur < max * 0.7f) {
            try {
                am.setStreamVolume(AudioManager.STREAM_ALARM, max, 0);
            } catch (SecurityException ignored) {
                // Ringer mode silent etc. — nothing we can do without a NotificationPolicy grant.
            }
        }
    }

    private void ensureMediaPlayerRunning() {
        if (mediaPlayer == null) return;
        try {
            if (!mediaPlayer.isPlaying()) mediaPlayer.start();
        } catch (IllegalStateException ignored) {}
    }

    private void rescheduleIfRepeating(Alarm alarm) {
        if (alarm == null) return;
        if (alarm.isRepeating() && alarm.enabled) {
            new AlarmScheduler(this).schedule(alarm);
        } else {
            alarm.enabled = false;
            new AlarmStorage(this).upsert(alarm);
        }
    }

    private android.app.Notification buildNotification(long alarmId, String label, boolean isNoChallenge) {
        Intent open = new Intent(this, AlarmRingActivity.class);
        open.putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Use a unique request code based on current time to force the system 
        // to see this as a fresh interrupt event every time.
        int requestCode = (int) (System.currentTimeMillis() & 0x7fffffff);
        PendingIntent contentPi = PendingIntent.getActivity(this,
                requestCode,
                open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, AlarmiqApp.ALARM_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(label)
                .setContentText(getString(R.string.alarm_notification_text))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setOngoing(true)
                .setAutoCancel(false)
                .setFullScreenIntent(contentPi, true)
                .setContentIntent(contentPi)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        if (isNoChallenge) {
            // Traditional buttons only for "None" mode
            Intent dismissIntent = new Intent(this, AlarmReceiver.class);
            dismissIntent.setAction(AlarmReceiver.ACTION_DISMISS);
            dismissIntent.putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId);
            PendingIntent dismissPi = PendingIntent.getBroadcast(this, 1, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            Intent snoozeIntent = new Intent(this, AlarmReceiver.class);
            snoozeIntent.setAction(AlarmReceiver.ACTION_SNOOZE);
            snoozeIntent.putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId);
            PendingIntent snoozePi = PendingIntent.getBroadcast(this, 2, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            builder.addAction(0, getString(R.string.snooze), snoozePi);
            builder.addAction(0, getString(R.string.dismiss), dismissPi);
        }

        return builder.build();
    }

    private void launchRingActivity(long alarmId) {
        Intent open = new Intent(this, AlarmRingActivity.class);
        open.putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        startActivity(open);
    }

    private void forceLaunchActivity(long alarmId) {
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (am == null) return;

        Intent open = new Intent(this, AlarmRingActivity.class);
        open.putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK 
                | Intent.FLAG_ACTIVITY_CLEAR_TOP 
                | Intent.FLAG_ACTIVITY_NO_USER_ACTION);

        int requestCode = (int) (System.currentTimeMillis() & 0x7fffffff);
        PendingIntent pi = PendingIntent.getActivity(this,
                requestCode,
                open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Schedule an "alarm" to fire in 100ms that launches the activity.
        // setAlarmClock is the most aggressive way to bypass background restrictions.
        long triggerAt = System.currentTimeMillis() + 100;
        try {
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(triggerAt, pi), pi);
        } catch (SecurityException ignored) {}
    }

    private void startRingtone(Alarm alarm) {
        try {
            Uri uri = null;
            if (alarm != null && alarm.soundUri != null) {
                try {
                    uri = Uri.parse(alarm.soundUri);
                } catch (Exception ignored) {}
            }
            if (uri == null) {
                uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM);
            }
            if (uri == null) {
                uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }
            if (uri == null) return;
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            mediaPlayer.setLooping(true);
            mediaPlayer.setDataSource(this, uri);
            mediaPlayer.prepare();
            mediaPlayer.start();

            AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
            if (am != null) {
                int max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM);
                am.setStreamVolume(AudioManager.STREAM_ALARM,
                        Math.max(1, (int) (max * 0.85f)), 0);
            }
        } catch (Exception ignored) {}
    }

    private void startVibration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = vm != null ? vm.getDefaultVibrator() : null;
        } else {
            vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        }
        if (vibrator == null || !vibrator.hasVibrator()) return;
        long[] pattern = {0, 600, 400, 600, 400};
        VibrationEffect effect = VibrationEffect.createWaveform(pattern, 0);
        vibrator.vibrate(effect);
    }

    private void acquireWakeLock() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "Alarmiq:RingWakeLock");
        wakeLock.setReferenceCounted(false);
        wakeLock.acquire(10 * 60 * 1000L);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        enforceHandler.removeCallbacks(enforce);
        cancelAntiCheatGuard();
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
    }
}
