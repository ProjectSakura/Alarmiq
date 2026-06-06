package com.shen.alarmiq.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class AlarmScheduler {

    public static final String EXTRA_ALARM_ID = "extra_alarm_id";

    private final Context context;
    private final AlarmManager alarmManager;

    public AlarmScheduler(Context context) {
        this.context = context.getApplicationContext();
        this.alarmManager = (AlarmManager) this.context.getSystemService(Context.ALARM_SERVICE);
    }

    public boolean canScheduleExact() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return alarmManager.canScheduleExactAlarms();
        }
        return true;
    }

    public long schedule(Alarm alarm) {
        return schedule(alarm, alarm.nextTriggerMillis());
    }

    public long schedule(Alarm alarm, long trigger) {
        PendingIntent pi = pendingIntentFor(alarm.id);
        AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(trigger, pi);
        alarmManager.setAlarmClock(info, pi);

        // Schedule upcoming notification 30 minutes before if difficulty is NONE
        if (alarm.difficulty == com.shen.alarmiq.math.Difficulty.NONE) {
            long upcomingTrigger = trigger - (30 * 60 * 1000L);
            long now = System.currentTimeMillis();
            
            // If the alarm is in the future, schedule the notification.
            // If it's within the 30-min window, fire it almost immediately (1s delay).
            if (trigger > now) {
                long finalTrigger = Math.max(upcomingTrigger, now + 1000L);
                PendingIntent upi = upcomingPendingIntentFor(alarm.id);
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, finalTrigger, upi);
            } else {
                cancelUpcoming(alarm.id);
            }
        } else {
            // Cancel any existing upcoming notification if difficulty changed
            cancelUpcoming(alarm.id);
        }

        return trigger;
    }

    public long snooze(Alarm alarm) {
        return snooze(alarm, System.currentTimeMillis());
    }

    public long snooze(Alarm alarm, long baseTime) {
        long trigger = baseTime + (5 * 60 * 1000L);
        PendingIntent pi = pendingIntentFor(alarm.id);
        AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(trigger, pi);
        alarmManager.setAlarmClock(info, pi);
        
        // After snooze, we don't usually show an upcoming notification for the same instance
        cancelUpcoming(alarm.id);
        
        return trigger;
    }

    public void cancel(long alarmId) {
        PendingIntent pi = pendingIntentFor(alarmId);
        alarmManager.cancel(pi);
        pi.cancel();
        cancelUpcoming(alarmId);
    }

    public void cancelUpcoming(long alarmId) {
        PendingIntent upi = upcomingPendingIntentFor(alarmId);
        alarmManager.cancel(upi);
        upi.cancel();
    }

    private PendingIntent pendingIntentFor(long alarmId) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(AlarmReceiver.ACTION_FIRE);
        intent.putExtra(EXTRA_ALARM_ID, alarmId);
        int requestCode = (int) (alarmId & 0x3fffffff);
        return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private PendingIntent upcomingPendingIntentFor(long alarmId) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(AlarmReceiver.ACTION_UPCOMING);
        intent.putExtra(EXTRA_ALARM_ID, alarmId);
        int requestCode = (int) (alarmId & 0x3fffffff) | 0x40000000;
        return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
