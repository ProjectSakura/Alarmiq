package com.shen.alarmiq;

import com.shen.alarmiq.alarm.Alarm;

import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.*;

public class AlarmTest {

    @Test
    public void testNextTriggerMillis_SameDay() {
        Alarm alarm = new Alarm();
        alarm.hour = 10;
        alarm.minute = 0;
        alarm.repeatMask = 0; // One-shot

        Calendar now = Calendar.getInstance();
        now.set(Calendar.HOUR_OF_DAY, 9);
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);

        long trigger = alarm.nextTriggerMillis(now);
        Calendar result = Calendar.getInstance();
        result.setTimeInMillis(trigger);

        assertEquals(10, result.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, result.get(Calendar.MINUTE));
        assertEquals(now.get(Calendar.DAY_OF_YEAR), result.get(Calendar.DAY_OF_YEAR));
    }

    @Test
    public void testNextTriggerMillis_NextDay() {
        Alarm alarm = new Alarm();
        alarm.hour = 8;
        alarm.minute = 0;
        alarm.repeatMask = 0; // One-shot

        Calendar now = Calendar.getInstance();
        now.set(Calendar.HOUR_OF_DAY, 9);
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);

        long trigger = alarm.nextTriggerMillis(now);
        Calendar result = Calendar.getInstance();
        result.setTimeInMillis(trigger);

        assertEquals(8, result.get(Calendar.HOUR_OF_DAY));
        assertEquals(now.get(Calendar.DAY_OF_YEAR) + 1, result.get(Calendar.DAY_OF_YEAR));
    }

    @Test
    public void testNextTriggerMillis_Repeating() {
        Alarm alarm = new Alarm();
        alarm.hour = 8;
        alarm.minute = 0;
        // Monday (bit 1) and Wednesday (bit 3)
        alarm.repeatMask = (1 << 1) | (1 << 3); 

        // Current time: Sunday (bit 0) 9:00 AM
        Calendar now = Calendar.getInstance();
        now.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        now.set(Calendar.HOUR_OF_DAY, 9);
        now.set(Calendar.MINUTE, 0);

        long trigger = alarm.nextTriggerMillis(now);
        Calendar result = Calendar.getInstance();
        result.setTimeInMillis(trigger);

        assertEquals(Calendar.MONDAY, result.get(Calendar.DAY_OF_WEEK));
        assertEquals(8, result.get(Calendar.HOUR_OF_DAY));
    }
}
