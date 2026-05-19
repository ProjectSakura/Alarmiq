package com.shen.alarmiq.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class AlarmReceiver extends BroadcastReceiver {

    public static final String ACTION_FIRE = "com.shen.alarmiq.ACTION_FIRE";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        long alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L);
        if (alarmId < 0) return;

        Intent svc = new Intent(context, AlarmRingingService.class);
        svc.putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(svc);
        } else {
            context.startService(svc);
        }
    }
}
