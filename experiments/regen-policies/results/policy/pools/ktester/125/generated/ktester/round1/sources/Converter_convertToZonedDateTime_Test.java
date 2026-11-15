package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.sql.Timestamp;
import java.util.stream.Stream;

// Mockito not actually used, but kept from template
@ExtendWith(MockitoExtension.class)
class Converter_convertToZonedDateTime_Test {

    /**
     * Valid source objects that should yield a concrete {@link ZonedDateTime}.
     */
    private static Stream<Arguments> validConversionProvider() {
        ZoneId system = ZoneId.systemDefault();
        // Instant
        Instant instant = Instant.parse("2023-05-15T10:30:00Z");
        ZonedDateTime fromInstant = ZonedDateTime.ofInstant(instant, system);
        // java.util.Date wrapping the same instant
        Date date = Date.from(instant);
        ZonedDateTime fromDate = fromInstant;
        // LocalDateTime (no zone information)
        LocalDateTime ldt = LocalDateTime.of(2023, 5, 15, 10, 30, 0);
        ZonedDateTime fromLdt = ZonedDateTime.of(ldt, system);
        // ISO‑8601 string with explicit zone
        String isoWithZone = "2023-05-15T10:30:00+02:00[Europe/Paris]";
        ZonedDateTime fromIsoWithZone = ZonedDateTime.parse(isoWithZone);
        // ISO‑8601 string without zone – converter should apply the system default zone
        String isoWithoutZone = "2023-05-15T10:30:00";
        ZonedDateTime fromIsoWithoutZone = ZonedDateTime.of(LocalDateTime.parse(isoWithoutZone), system);
        // LocalDate – start of day in the system default zone
        LocalDate localDate = LocalDate.of(2023, 5, 15);
        ZonedDateTime fromLocalDate = ZonedDateTime.of(localDate.atStartOfDay(), system);
        // java.sql.Timestamp
        Timestamp ts = Timestamp.valueOf("2023-05-15 10:30:00");
        ZonedDateTime fromTimestamp = ZonedDateTime.ofInstant(ts.toInstant(), system);
        // Calendar (UTC instance, but conversion must use system default zone)
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(instant.toEpochMilli());
        ZonedDateTime fromCalendar = ZonedDateTime.ofInstant(calendar.toInstant(), system);
        // OffsetDateTime – preserve the original offset as the zone
        OffsetDateTime odt = OffsetDateTime.of(2023, 5, 15, 10, 30, 0, 0, ZoneOffset.ofHours(2));
        ZonedDateTime fromOffsetDateTime = odt.toZonedDateTime();
        // Long representing epoch‑millis
        // 2023‑05‑15T10:30:00Z
        Long epochMillis = 1684138200000L;
        ZonedDateTime fromLong = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), system);
        return Stream.of(Arguments.of(instant, fromInstant), Arguments.of(date, fromDate), Arguments.of(ldt, fromLdt), Arguments.of(isoWithZone, fromIsoWithZone), Arguments.of(isoWithoutZone, fromIsoWithoutZone), Arguments.of(localDate, fromLocalDate), Arguments.of(ts, fromTimestamp), Arguments.of(calendar, fromCalendar), Arguments.of(odt, fromOffsetDateTime), Arguments.of(epochMillis, fromLong));
    }

    /**
     * Strings that are malformed ISO‑8601 representations – should raise {@link DateTimeParseException}.
     */
    private static Stream<String> malformedStringProvider() {
        return Stream.of(// wrong separators
        "2023/05/15 10:30:00", // empty string
        "", // whitespace only
        "   ");
    }

    /**
     * Source objects that have no conversion path to {@link ZonedDateTime}.
     */
    private static Stream<Object> unsupportedSourceProvider() {
        return Stream.of(Boolean.TRUE, ZoneId.of("UTC"), new java.math.BigInteger("12345678901234567890"), Arrays.asList("2023-05-15T10:30:00Z"), Collections.singletonMap("key", "value"));
    }

    @Test
    void convertNullReturnsNull() {
        ZonedDateTime result = Converter.convertToZonedDateTime(null);
        assertNull(result, "Converting null should return null");
    }

    @Test
    void convertZonedDateTimeReturnsSameInstance() {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime result = Converter.convertToZonedDateTime(now);
        assertSame(now, result, "Converting an existing ZonedDateTime should return the same instance");
    }

    @Test
    void convertInstantToZonedDateTime() {
        Instant instant = Instant.now();
        ZonedDateTime expected = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
        ZonedDateTime result = Converter.convertToZonedDateTime(instant);
        assertEquals(expected, result, "Instant should be converted to ZonedDateTime using system default zone");
    }

    @Test
    void convertDateToZonedDateTime() {
        Date date = new Date();
        Instant instant = date.toInstant();
        ZonedDateTime expected = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
        ZonedDateTime result = Converter.convertToZonedDateTime(date);
        assertEquals(expected, result, "Date should be converted to ZonedDateTime using system default zone");
    }

    @Test
    void convertLocalDateTimeToZonedDateTime() {
        LocalDateTime ldt = LocalDateTime.now();
        ZonedDateTime expected = ldt.atZone(ZoneId.systemDefault());
        ZonedDateTime result = Converter.convertToZonedDateTime(ldt);
        assertEquals(expected, result, "LocalDateTime should be converted to ZonedDateTime using system default zone");
    }

    @Test
    void convertCalendarToZonedDateTime() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(System.currentTimeMillis());
        Instant instant = calendar.toInstant();
        ZonedDateTime expected = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
        ZonedDateTime result = Converter.convertToZonedDateTime(calendar);
        assertEquals(expected, result, "Calendar should be converted to ZonedDateTime using system default zone");
    }

    @Test
    void convertMapWithTimeKeyToZonedDateTime() {
        Instant instant = Instant.now();
        Map<String, Object> map = new HashMap<>();
        // typical key used by Converter for date/time maps
        map.put("time", instant.toEpochMilli());
        ZonedDateTime expected = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
        ZonedDateTime result = Converter.convertToZonedDateTime(map);
        assertEquals(expected, result, "Map containing a \"time\" key should be converted to ZonedDateTime");
    }

    @Test
    void convertStringIso8601ToZonedDateTime() {
        // The Converter supports ISO‑8601 strings for date‑time types
        String iso = "2023-08-15T12:34:56+02:00[Europe/Paris]";
        ZonedDateTime expected = ZonedDateTime.parse(iso);
        ZonedDateTime result = Converter.convertToZonedDateTime(iso);
        assertEquals(expected, result, "ISO‑8601 string should be parsed into the correct ZonedDateTime");
    }

    @ParameterizedTest(name = "[{index}] convert {0} → {1}")
    @MethodSource("validConversionProvider")
    void convertValidSources(Object source, ZonedDateTime expected) {
        ZonedDateTime result = Converter.convertToZonedDateTime(source);
        assertEquals(expected, result, () -> "Conversion of " + source.getClass().getSimpleName() + " did not produce the expected ZonedDateTime");
    }

    @ParameterizedTest(name = "[{index}] malformed string \"{0}\"")
    @MethodSource("malformedStringProvider")
    void convertMalformedStringThrows(String malformed) {
        assertThrows(DateTimeParseException.class, () -> Converter.convertToZonedDateTime(malformed), "Expected DateTimeParseException for malformed string: \"" + malformed + "\"");
    }

    @ParameterizedTest(name = "[{index}] unsupported source {0}")
    @MethodSource("unsupportedSourceProvider")
    void convertUnsupportedSourceThrows(Object unsupported) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToZonedDateTime(unsupported), "Expected IllegalArgumentException for unsupported source type: " + unsupported.getClass().getSimpleName());
    }

    /**
     * Epoch‑millis that lie outside the range supported by {@link Instant#ofEpochMilli(long)}.
     */
    @Test
    void convertEpochMillisOutOfRangeThrows() {
        // far beyond the Instant range
        Long outOfRange = Long.MAX_VALUE;
        assertThrows(DateTimeException.class, () -> Converter.convertToZonedDateTime(outOfRange), "Expected DateTimeException for out‑of‑range epoch‑millis value");
    }
}
