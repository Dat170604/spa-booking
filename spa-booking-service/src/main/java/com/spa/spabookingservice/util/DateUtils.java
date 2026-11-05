package com.spa.spabookingservice.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class DateUtils {
    public static final String DATE_FORMAT = "dd/MM/yyyy";

    public static String formatDate(LocalDate date) {
        if (date == null) return null;
        return date.format(DateTimeFormatter.ofPattern(DATE_FORMAT));
    }

    public static LocalTime calculateEndTime(LocalTime startTime, int durationMinutes) {
        return startTime.plusMinutes(durationMinutes);
    }
}