package com.shen.alarmiq.world;

import android.icu.text.TimeZoneNames;
import android.icu.util.TimeZone;
import android.icu.util.ULocale;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Provides city data for the world clock. Uses android.icu for localized,
 * comprehensive city lists, augmented with custom aliases for major cities
 * that aren't the primary timezone anchors (e.g., Delhi, Mumbai).
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

    private static final City[] CUSTOM_ALIASES = {
            new City("Beijing", "Asia/Shanghai"),
            new City("Bengaluru", "Asia/Kolkata"),
            new City("Chennai", "Asia/Kolkata"),
            new City("Delhi", "Asia/Kolkata"),
            new City("Mumbai", "Asia/Kolkata"),
            new City("San Francisco", "America/Los_Angeles"),
            new City("Seattle", "America/Los_Angeles"),
            new City("Washington D.C.", "America/New_York"),
    };

    /** Returns a small list of featured cities for the default view. */
    public static List<City> all() {
        return filter("");
    }

    public static List<City> filter(String query) {
        List<City> results = new ArrayList<>();
        String q = query == null ? "" : query.trim().toLowerCase(Locale.getDefault());
        Locale locale = Locale.getDefault();
        TimeZoneNames tzNames = TimeZoneNames.getInstance(ULocale.forLocale(locale));

        // 1. Add ICU Canonical Locations (~400+ cities)
        Set<String> ids = TimeZone.getAvailableIDs(TimeZone.SystemTimeZoneType.CANONICAL_LOCATION, null, null);
        for (String id : ids) {
            String name = tzNames.getExemplarLocationName(id);
            if (name == null || name.isEmpty()) {
                // Fallback for names not in ICU
                int slash = id.lastIndexOf('/');
                name = slash >= 0 ? id.substring(slash + 1).replace('_', ' ') : id;
            }

            if (q.isEmpty() || name.toLowerCase(locale).contains(q)) {
                results.add(new City(name, id));
            }
        }

        // 2. Add Custom Aliases (Delhi, Mumbai, etc.)
        for (City alias : CUSTOM_ALIASES) {
            if (q.isEmpty() || alias.displayName.toLowerCase(locale).contains(q)) {
                // Only add if not already present as a primary name (avoid duplicates if ICU adds them)
                boolean exists = false;
                for (City r : results) {
                    if (r.displayName.equalsIgnoreCase(alias.displayName)) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    results.add(alias);
                }
            }
        }

        sortAlpha(results);

        // Limit default results if no query
        if (q.isEmpty() && results.size() > 20) {
            // Pick a few diverse ones if we wanted a "featured" list, 
            // but for simplicity we'll just return the top sorted ones or all.
            // Let's just return all for a competitive feel.
        }

        return results;
    }

    public static String displayNameForZone(String zoneId) {
        TimeZoneNames tzNames = TimeZoneNames.getInstance(ULocale.getDefault());
        String name = tzNames.getExemplarLocationName(zoneId);
        if (name != null && !name.isEmpty()) return name;

        // Fallback: check aliases
        for (City alias : CUSTOM_ALIASES) {
            if (alias.zoneId.equals(zoneId)) return alias.displayName;
        }

        // Fallback: prettify "America/New_York" → "New York"
        int slash = zoneId.lastIndexOf('/');
        String tail = slash >= 0 ? zoneId.substring(slash + 1) : zoneId;
        return tail.replace('_', ' ');
    }

    public static java.util.TimeZone timeZoneFor(String zoneId) {
        return java.util.TimeZone.getTimeZone(zoneId);
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
