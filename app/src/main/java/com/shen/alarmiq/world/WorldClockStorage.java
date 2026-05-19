package com.shen.alarmiq.world;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class WorldClockStorage {

    private static final String PREFS = "alarmiq_world";
    private static final String KEY_IDS = "zone_ids";

    private final SharedPreferences prefs;

    public WorldClockStorage(Context ctx) {
        this.prefs = ctx.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public List<String> getAll() {
        String raw = prefs.getString(KEY_IDS, "");
        if (raw.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(raw.split("\\|")));
    }

    public void add(String zoneId) {
        Set<String> set = new LinkedHashSet<>(getAll());
        set.add(zoneId);
        save(new ArrayList<>(set));
    }

    public void remove(String zoneId) {
        List<String> list = getAll();
        list.remove(zoneId);
        save(list);
    }

    public void save(List<String> ids) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) sb.append('|');
            sb.append(ids.get(i));
        }
        prefs.edit().putString(KEY_IDS, sb.toString()).apply();
    }
}
