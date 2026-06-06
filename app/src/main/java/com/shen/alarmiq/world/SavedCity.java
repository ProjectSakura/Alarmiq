package com.shen.alarmiq.world;

import java.util.Objects;

/**
 * Represents a city saved in the world clock.
 * Stores both the TimeZone ID and the user-facing city name.
 */
public class SavedCity {
    public final String zoneId;
    public final String cityName;

    public SavedCity(String zoneId, String cityName) {
        // Sanitize input to prevent delimiter injection
        this.zoneId = zoneId == null ? "" : zoneId.replace("|", "").replace(";", "");
        this.cityName = cityName == null ? "" : cityName.replace("|", "").replace(";", "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SavedCity that = (SavedCity) o;
        return Objects.equals(zoneId, that.zoneId) &&
                Objects.equals(cityName, that.cityName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(zoneId, cityName);
    }

    /** Serializes to "zoneId;;cityName" */
    public String serialize() {
        return zoneId + ";;" + cityName;
    }

    /** Deserializes from "zoneId;;cityName" or raw "zoneId" */
    public static SavedCity deserialize(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        String[] parts = raw.split(";;");
        if (parts.length >= 2) {
            return new SavedCity(parts[0], parts[1]);
        }
        // Migration fallback: if only zoneId is present
        return new SavedCity(raw, Cities.displayNameForZone(raw));
    }
}
