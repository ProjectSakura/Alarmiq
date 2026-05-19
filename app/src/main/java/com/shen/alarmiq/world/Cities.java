package com.shen.alarmiq.world;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Provides city data for the world clock. Uses a small list of "featured" cities
 * as default suggestions, but supports dynamic searching across the entire
 * system TimeZone database.
 */
public final class Cities {

    public static class City {
        public final String displayName;
        public final String zoneId;
        public City(String displayName, String zoneId) {
            this.displayName = displayName;
            this.zoneId = zoneId;
        }
    }

    private static final City[] CITIES = {
            new City("Berlin", "Europe/Berlin"),
            new City("Cairo", "Africa/Cairo"),
            new City("Chicago", "America/Chicago"),
            new City("Dubai", "Asia/Dubai"),
            new City("Hong Kong", "Asia/Hong_Kong"),
            new City("London", "Europe/London"),
            new City("Los Angeles", "America/Los_Angeles"),
            new City("Moscow", "Europe/Moscow"),
            new City("New York", "America/New_York"),
            new City("Paris", "Europe/Paris"),
            new City("Seoul", "Asia/Seoul"),
            new City("Sydney", "Australia/Sydney"),
            new City("Tokyo", "Asia/Tokyo"),
    };

    public static List<City> all() {
        return Arrays.asList(CITIES);
    }

    public static List<City> filter(String query) {
        List<City> results = new ArrayList<>();
        String q = query == null ? "" : query.trim().toLowerCase(Locale.getDefault());

        // 1. Check curated list first (priority)
        for (City c : CITIES) {
            if (q.isEmpty() || c.displayName.toLowerCase(Locale.getDefault()).contains(q)) {
                results.add(c);
            }
        }

        // 2. Add from system TimeZone database if not already present
        if (!q.isEmpty()) {
            String[] ids = TimeZone.getAvailableIDs();
            for (String id : ids) {
                // Only consider "Continent/City" or "Continent/Region/City" patterns
                if (!id.contains("/")) continue;
                
                String name = id.substring(id.lastIndexOf('/') + 1).replace('_', ' ');
                if (name.toLowerCase(Locale.getDefault()).contains(q)) {
                    // Avoid duplicates if already in curated list
                    boolean exists = false;
                    for (City r : results) {
                        if (r.zoneId.equals(id)) { exists = true; break; }
                    }
                    if (!exists) {
                        results.add(new City(name, id));
                    }
                }
            }
        }

        sortAlpha(results);
        return results;
    }

    public static String displayNameForZone(String zoneId) {
        for (City c : CITIES) {
            if (c.zoneId.equals(zoneId)) return c.displayName;
        }
        // Fallback: prettify "America/New_York" → "New York"
        int slash = zoneId.lastIndexOf('/');
        String tail = slash >= 0 ? zoneId.substring(slash + 1) : zoneId;
        return tail.replace('_', ' ');
    }

    public static TimeZone timeZoneFor(String zoneId) {
        return TimeZone.getTimeZone(zoneId);
    }

    private static void sortAlpha(List<City> list) {
        final Collator collator = Collator.getInstance(Locale.getDefault());
        Collections.sort(list, new Comparator<City>() {
            @Override
            public int compare(City a, City b) {
                return collator.compare(a.displayName, b.displayName);
            }
        });
    }

    private Cities() {}
}
