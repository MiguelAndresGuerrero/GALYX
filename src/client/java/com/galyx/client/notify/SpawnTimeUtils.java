package com.galyx.client.notify;

import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class SpawnTimeUtils {

    private static final DateTimeFormatter[] TIME_FORMATS = {
            DateTimeFormatter.ofPattern("h:mm a", Locale.US),
            DateTimeFormatter.ofPattern("h a", Locale.US),
            DateTimeFormatter.ofPattern("H:mm")
    };

    private SpawnTimeUtils() {
    }

    public static LocalTime parseTime(String text) {
        String trimmed = text.trim().toUpperCase(Locale.US);
        for (DateTimeFormatter formatter : TIME_FORMATS) {
            try {
                return LocalTime.parse(trimmed, formatter);
            } catch (Exception ignored) {
                // probamos el siguiente formato
            }
        }
        return null;
    }

    public static ZonedDateTime nextOccurrence(LocalTime spawnTime, ZonedDateTime now) {
        ZonedDateTime occurrence = now.with(spawnTime);
        if (occurrence.isBefore(now)) {
            occurrence = occurrence.plusDays(1);
        }
        return occurrence;
    }

}