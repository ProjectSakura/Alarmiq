package com.shen.alarmiq.alarm;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AlarmStorage {

    private static final String PREFS = "alarmiq_prefs";
    private static final String KEY_ALARMS = "alarms_json";

    private final SharedPreferences prefs;

    public AlarmStorage(Context ctx) {
        this.prefs = ctx.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public List<Alarm> getAll() {
        String raw = prefs.getString(KEY_ALARMS, "[]");
        List<Alarm> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                out.add(Alarm.fromJson(arr.getJSONObject(i)));
            }
        } catch (JSONException ignored) {}
        Collections.sort(out, new Comparator<Alarm>() {
            @Override
            public int compare(Alarm a, Alarm b) {
                int t1 = a.hour * 60 + a.minute;
                int t2 = b.hour * 60 + b.minute;
                return Integer.compare(t1, t2);
            }
        });
        return out;
    }

    public Alarm getById(long id) {
        for (Alarm a : getAll()) {
            if (a.id == id) return a;
        }
        return null;
    }

    public void saveAll(List<Alarm> alarms) {
        JSONArray arr = new JSONArray();
        try {
            for (Alarm a : alarms) {
                arr.put(a.toJson());
            }
        } catch (JSONException ignored) {}
        prefs.edit().putString(KEY_ALARMS, arr.toString()).apply();
    }

    public void upsert(Alarm alarm) {
        List<Alarm> alarms = getAll();
        boolean replaced = false;
        for (int i = 0; i < alarms.size(); i++) {
            if (alarms.get(i).id == alarm.id) {
                alarms.set(i, alarm);
                replaced = true;
                break;
            }
        }
        if (!replaced) alarms.add(alarm);
        saveAll(alarms);
    }

    public void delete(long id) {
        List<Alarm> alarms = getAll();
        for (int i = 0; i < alarms.size(); i++) {
            if (alarms.get(i).id == id) {
                alarms.remove(i);
                break;
            }
        }
        saveAll(alarms);
    }
}
