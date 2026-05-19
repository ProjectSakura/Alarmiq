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
        long trigger = alarm.nextTriggerMillis();
        PendingIntent pi = pendingIntentFor(alarm.id);
        AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(trigger, pi);
        alarmManager.setAlarmClock(info, pi);
        return trigger;
    }

    public void cancel(long alarmId) {
        PendingIntent pi = pendingIntentFor(alarmId);
        alarmManager.cancel(pi);
        pi.cancel();
    }

    private PendingIntent pendingIntentFor(long alarmId) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(AlarmReceiver.ACTION_FIRE);
        intent.putExtra(EXTRA_ALARM_ID, alarmId);
        int requestCode = (int) (alarmId & 0x7fffffff);
        return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
