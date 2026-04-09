package io.arconia.cli.utils;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Utilities for working with date and time.
 */
public final class DateTimeUtils {

    private DateTimeUtils() {}

    /**
     * ISO 8601 UTC formatter producing timestamps in the form {@code 2026-04-01T12:00:00Z}.
     */
    public static final DateTimeFormatter ISO_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    /**
     * The current UTC timestamp formatted as ISO 8601.
     */
    public static String nowIso() {
        return ISO_FORMATTER.format(Instant.now());
    }

}
