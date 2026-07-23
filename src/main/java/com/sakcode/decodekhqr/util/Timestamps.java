package com.sakcode.decodekhqr.util;

import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class Timestamps {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy 'at' HH:mm:ss");

    private Timestamps() {
    }

    public static long tenMinutesFromNowMillis() {
        ZoneId localZone = ZoneId.systemDefault();
        return LocalDateTime.now(localZone).plusMinutes(10).atZone(localZone).toInstant().toEpochMilli();
    }

    public static String nowStamp() {
        long millis = Instant.now().toEpochMilli();
        return millis + " - " + toUtc7Display(millis);
    }

    public static String labelFor(String epochMillisText) {
        if (StringUtils.isBlank(epochMillisText)) {
            return "";
        }
        return epochMillisText + " - " + toUtc7Display(Long.parseLong(epochMillisText));
    }

    private static String toUtc7Display(long epochMillis) {
        LocalDateTime utcDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.of("UTC"));
        return utcDateTime.plusHours(7).format(DISPLAY_FORMAT);
    }
}
