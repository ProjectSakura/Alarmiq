package com.shen.alarmiq.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class AlarmReceiver extends BroadcastReceiver {

    public static final String ACTION_FIRE = "com.shen.alarmiq.ACTION_FIRE";
    public static final String ACTION_DISMISS = "com.shen.alarmiq.ACTION_DISMISS";
    public static final String ACTION_SNOOZE = "com.shen.alarmiq.ACTION_SNOOZE";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        long alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L);
        if (alarmId < 0) return;

        if (ACTION_SNOOZE.equals(action)) {
            snooze(context, alarmId);
            return;
        }

        if (ACTION_DISMISS.equals(action)) {
            context.stopService(new Intent(context, AlarmRingingService.class));
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

    private void snooze(Context context, long alarmId) {
        context.stopService(new Intent(context, AlarmRingingService.class));
        Alarm alarm = new AlarmStorage(context).getById(alarmId);
        if (alarm != null) {
            new AlarmScheduler(context).snooze(alarm);
        }
    }
}
