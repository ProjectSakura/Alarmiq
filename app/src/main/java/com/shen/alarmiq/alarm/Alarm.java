package com.shen.alarmiq.alarm;

import com.shen.alarmiq.math.Difficulty;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Locale;

public class Alarm {
    public long id;
    public int hour;
    public int minute;
    public String label;
    public Difficulty difficulty;
    public boolean enabled;
    /** Bitmask: bit 0 = Sunday … bit 6 = Saturday. 0 means one-shot. */
    public int repeatMask;
    /** content:// URI for the chosen ringtone, or null to use the system default alarm tone. */
    public String soundUri;
    public String soundTitle;

    public Alarm() {
        this.id = System.currentTimeMillis();
        this.hour = 7;
        this.minute = 0;
        this.label = "";
        this.difficulty = Difficulty.NORMAL;
        this.enabled = true;
        this.repeatMask = 0;
        this.soundUri = null;
        this.soundTitle = null;
    }

    public String formatTime(boolean is24h) {
        if (is24h) {
            return String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
        }
        int h = hour % 12;
        if (h == 0) h = 12;
        return String.format(Locale.getDefault(), "%d:%02d %s", h, minute, hour < 12 ? "AM" : "PM");
    }

    public boolean isRepeating() {
        return repeatMask != 0;
    }

    /** Returns the next trigger time in millis (epoch), based on now. */
    public long nextTriggerMillis() {
        return nextTriggerMillis(java.util.Calendar.getInstance());
    }

    /** Returns the next trigger time in millis (epoch), based on the provided reference time. */
    public long nextTriggerMillis(java.util.Calendar now) {
        java.util.Calendar candidate = (java.util.Calendar) now.clone();
        candidate.set(java.util.Calendar.HOUR_OF_DAY, hour);
        candidate.set(java.util.Calendar.MINUTE, minute);
        candidate.set(java.util.Calendar.SECOND, 0);
        candidate.set(java.util.Calendar.MILLISECOND, 0);

        if (!isRepeating()) {
            if (!candidate.after(now)) {
                candidate.add(java.util.Calendar.DAY_OF_YEAR, 1);
            }
            return candidate.getTimeInMillis();
        }

        for (int i = 0; i < 8; i++) {
            int dayOfWeek = candidate.get(java.util.Calendar.DAY_OF_WEEK); // 1=Sun
            int bit = 1 << (dayOfWeek - 1);
            if ((repeatMask & bit) != 0 && candidate.after(now)) {
                return candidate.getTimeInMillis();
            }
            candidate.add(java.util.Calendar.DAY_OF_YEAR, 1);
        }
        return candidate.getTimeInMillis();
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("hour", hour);
        o.put("minute", minute);
        o.put("label", label == null ? "" : label);
        o.put("difficulty", difficulty.name());
        o.put("enabled", enabled);
        o.put("repeatMask", repeatMask);
        if (soundUri != null) o.put("soundUri", soundUri);
        if (soundTitle != null) o.put("soundTitle", soundTitle);
        return o;
    }

    public static Alarm fromJson(JSONObject o) throws JSONException {
        Alarm a = new Alarm();
        a.id = o.getLong("id");
        a.hour = o.getInt("hour");
        a.minute = o.getInt("minute");
        a.label = o.optString("label", "");
        a.difficulty = Difficulty.fromName(o.optString("difficulty", "NORMAL"));
        a.enabled = o.optBoolean("enabled", true);
        a.repeatMask = o.optInt("repeatMask", 0);
        a.soundUri = o.has("soundUri") ? o.optString("soundUri", null) : null;
        a.soundTitle = o.has("soundTitle") ? o.optString("soundTitle", null) : null;
        return a;
    }
}
