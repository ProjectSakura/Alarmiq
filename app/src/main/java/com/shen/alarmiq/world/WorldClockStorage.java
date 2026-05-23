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

    public List<SavedCity> getAll() {
        String raw = prefs.getString(KEY_IDS, "");
        if (raw.isEmpty()) return new ArrayList<>();
        
        String[] parts = raw.split("\\|");
        List<SavedCity> results = new ArrayList<>();
        boolean needsRewrite = false;

        for (String p : parts) {
            SavedCity sc = SavedCity.deserialize(p);
            if (sc != null) {
                results.add(sc);
                // If it was an old format (didn't contain ;;), we should rewrite it eventually
                if (!p.contains(";;")) {
                    needsRewrite = true;
                }
            }
        }

        if (needsRewrite) {
            save(results);
        }

        return results;
    }

    public void add(String zoneId, String cityName) {
        List<SavedCity> list = getAll();
        SavedCity newItem = new SavedCity(zoneId, cityName);
        
        // Avoid exact duplicates (same city name and zone)
        if (!list.contains(newItem)) {
            list.add(newItem);
            save(list);
        }
    }

    public void remove(SavedCity city) {
        List<SavedCity> list = getAll();
        list.remove(city);
        save(list);
    }

    public void save(List<SavedCity> cities) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cities.size(); i++) {
            if (i > 0) sb.append('|');
            sb.append(cities.get(i).serialize());
        }
        prefs.edit().putString(KEY_IDS, sb.toString()).apply();
    }
}
