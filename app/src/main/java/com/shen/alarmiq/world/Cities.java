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
 * Curated list of cities for the picker. Each entry maps a friendly display
 * name to its IANA time zone id. Using a curated list (rather than
 * TimeZone.getAvailableIDs which returns ~600 entries including aliases and
 * Etc/GMT zones) keeps the picker manageable.
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
            new City("Auckland", "Pacific/Auckland"),
            new City("Bangkok", "Asia/Bangkok"),
            new City("Beijing", "Asia/Shanghai"),
            new City("Berlin", "Europe/Berlin"),
            new City("Brisbane", "Australia/Brisbane"),
            new City("Buenos Aires", "America/Argentina/Buenos_Aires"),
            new City("Cairo", "Africa/Cairo"),
            new City("Cape Town", "Africa/Johannesburg"),
            new City("Chicago", "America/Chicago"),
            new City("Denver", "America/Denver"),
            new City("Dubai", "Asia/Dubai"),
            new City("Dublin", "Europe/Dublin"),
            new City("Hong Kong", "Asia/Hong_Kong"),
            new City("Honolulu", "Pacific/Honolulu"),
            new City("Istanbul", "Europe/Istanbul"),
            new City("Jakarta", "Asia/Jakarta"),
            new City("Johannesburg", "Africa/Johannesburg"),
            new City("Karachi", "Asia/Karachi"),
            new City("Kolkata", "Asia/Kolkata"),
            new City("Lagos", "Africa/Lagos"),
            new City("London", "Europe/London"),
            new City("Los Angeles", "America/Los_Angeles"),
            new City("Madrid", "Europe/Madrid"),
            new City("Manila", "Asia/Manila"),
            new City("Melbourne", "Australia/Melbourne"),
            new City("Mexico City", "America/Mexico_City"),
            new City("Moscow", "Europe/Moscow"),
            new City("Mumbai", "Asia/Kolkata"),
            new City("Nairobi", "Africa/Nairobi"),
            new City("New Delhi", "Asia/Kolkata"),
            new City("New York", "America/New_York"),
            new City("Paris", "Europe/Paris"),
            new City("Reykjavik", "Atlantic/Reykjavik"),
            new City("Rio de Janeiro", "America/Sao_Paulo"),
            new City("Riyadh", "Asia/Riyadh"),
            new City("Rome", "Europe/Rome"),
            new City("San Francisco", "America/Los_Angeles"),
            new City("Santiago", "America/Santiago"),
            new City("São Paulo", "America/Sao_Paulo"),
            new City("Seoul", "Asia/Seoul"),
            new City("Shanghai", "Asia/Shanghai"),
            new City("Singapore", "Asia/Singapore"),
            new City("Stockholm", "Europe/Stockholm"),
            new City("Sydney", "Australia/Sydney"),
            new City("Taipei", "Asia/Taipei"),
            new City("Tehran", "Asia/Tehran"),
            new City("Tokyo", "Asia/Tokyo"),
            new City("Toronto", "America/Toronto"),
            new City("Vancouver", "America/Vancouver"),
            new City("Vienna", "Europe/Vienna"),
            new City("Warsaw", "Europe/Warsaw"),
            new City("Wellington", "Pacific/Auckland"),
            new City("Zurich", "Europe/Zurich"),
    };

    public static List<City> all() {
        return Arrays.asList(CITIES);
    }

    public static List<City> filter(String query) {
        if (query == null || query.trim().isEmpty()) {
            List<City> out = new ArrayList<>(Arrays.asList(CITIES));
            sortAlpha(out);
            return out;
        }
        String q = query.trim().toLowerCase(Locale.getDefault());
        List<City> out = new ArrayList<>();
        for (City c : CITIES) {
            if (c.displayName.toLowerCase(Locale.getDefault()).contains(q)) {
                out.add(c);
            }
        }
        sortAlpha(out);
        return out;
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
