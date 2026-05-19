package com.shen.alarmiq.timer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class TimerReceiver extends BroadcastReceiver {

    public static final String ACTION_FIRE = "com.shen.alarmiq.timer.ACTION_FIRE";

    @Override
    public void onReceive(Context context, Intent intent) {
        TimerEngine.get(context).onExpired();
        Intent svc = new Intent(context, TimerSoundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(svc);
        } else {
            context.startService(svc);
        }
    }
}
