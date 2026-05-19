package com.shen.alarmiq;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.shen.alarmiq.alarm.Alarm;
import com.shen.alarmiq.alarm.AlarmScheduler;
import com.shen.alarmiq.alarm.AlarmStorage;

import java.util.List;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        AlarmStorage storage = new AlarmStorage(context);
        AlarmScheduler scheduler = new AlarmScheduler(context);
        List<Alarm> alarms = storage.getAll();
        for (Alarm a : alarms) {
            if (a.enabled) {
                scheduler.schedule(a);
            }
        }
    }
}
