package com.shen.alarmiq.alarm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.format.DateFormat;

import androidx.core.app.NotificationCompat;

import com.shen.alarmiq.AlarmiqApp;
import com.shen.alarmiq.R;

import java.util.Calendar;

public class AlarmReceiver extends BroadcastReceiver {

    public static final String ACTION_FIRE = "com.shen.alarmiq.ACTION_FIRE";
    public static final String ACTION_DISMISS = "com.shen.alarmiq.ACTION_DISMISS";
    public static final String ACTION_SNOOZE = "com.shen.alarmiq.ACTION_SNOOZE";
    public static final String ACTION_UPCOMING = "com.shen.alarmiq.ACTION_UPCOMING";
    public static final String ACTION_DISMISS_UPCOMING = "com.shen.alarmiq.ACTION_DISMISS_UPCOMING";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        long alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L);
        if (alarmId < 0) return;

        if (ACTION_SNOOZE.equals(action)) {
            boolean isUpcoming = intent.getBooleanExtra("is_upcoming", false);
            snooze(context, alarmId, isUpcoming);
            return;
        }

        if (ACTION_DISMISS.equals(action)) {
            context.stopService(new Intent(context, AlarmRingingService.class));
            return;
        }

        if (ACTION_UPCOMING.equals(action)) {
            showUpcomingNotification(context, alarmId);
            return;
        }

        if (ACTION_DISMISS_UPCOMING.equals(action)) {
            dismissUpcoming(context, alarmId);
            return;
        }

        Intent svc = new Intent(context, AlarmRingingService.class);
        svc.putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(svc);
        } else {
            context.startService(svc);
        }
    }

    private void snooze(Context context, long alarmId, boolean isUpcoming) {
        context.stopService(new Intent(context, AlarmRingingService.class));
        Alarm alarm = new AlarmStorage(context).getById(alarmId);
        if (alarm != null) {
            alarm.skippedInstanceTime = 0; // Clear skip if snoozed
            AlarmScheduler scheduler = new AlarmScheduler(context);
            if (isUpcoming) {
                scheduler.snooze(alarm, alarm.nextTriggerMillis());
            } else {
                scheduler.snooze(alarm);
            }
        }
        // Dismiss upcoming notification if it's showing
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm != null) nm.cancel((int) (alarmId & 0x7fffffff));
    }

    private void showUpcomingNotification(Context context, long alarmId) {
        Alarm alarm = new AlarmStorage(context).getById(alarmId);
        if (alarm == null || !alarm.enabled) return;

        String timeStr = alarm.formatTime(DateFormat.is24HourFormat(context));
        String title = (alarm.label != null && !alarm.label.trim().isEmpty()) ? alarm.label : context.getString(R.string.upcoming_alarm_title);
        String text = context.getString(R.string.upcoming_alarm_text, timeStr);

        Intent dismissIntent = new Intent(context, AlarmReceiver.class);
        dismissIntent.setAction(ACTION_DISMISS_UPCOMING);
        dismissIntent.putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId);
        PendingIntent dismissPi = PendingIntent.getBroadcast(
                context, (int) (alarmId & 0x3fffffff) | 0x20000000, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent snoozeIntent = new Intent(context, AlarmReceiver.class);
        snoozeIntent.setAction(ACTION_SNOOZE);
        snoozeIntent.putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId);
        snoozeIntent.putExtra("is_upcoming", true);
        PendingIntent snoozePi = PendingIntent.getBroadcast(
                context, (int) (alarmId & 0x3fffffff) | 0x10000000, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, AlarmiqApp.ALARM_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .addAction(0, context.getString(R.string.snooze), snoozePi)
                .addAction(0, context.getString(R.string.upcoming_alarm_dismiss), dismissPi);

        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm != null) nm.notify((int) (alarmId & 0x7fffffff), builder.build());
    }

    private void dismissUpcoming(Context context, long alarmId) {
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm != null) nm.cancel((int) (alarmId & 0x7fffffff));

        AlarmStorage storage = new AlarmStorage(context);
        Alarm alarm = storage.getById(alarmId);
        if (alarm == null) return;

        alarm.skippedInstanceTime = 0; // Clear any old skip
        AlarmScheduler scheduler = new AlarmScheduler(context);
        if (alarm.isRepeating()) {
            long currentTrigger = alarm.nextTriggerMillis();
            rescheduleAfter(context, alarm, currentTrigger);
        } else {
            alarm.enabled = false;
            storage.upsert(alarm);
            scheduler.cancel(alarmId);
        }
    }

    private void rescheduleAfter(Context context, Alarm alarm, long afterMillis) {
        Calendar future = Calendar.getInstance();
        future.setTimeInMillis(afterMillis + 60000); // Start looking from 1 min after current trigger
        
        long nextTrigger = alarm.nextTriggerMillis(future);
        new AlarmScheduler(context).schedule(alarm, nextTrigger);
    }
}
