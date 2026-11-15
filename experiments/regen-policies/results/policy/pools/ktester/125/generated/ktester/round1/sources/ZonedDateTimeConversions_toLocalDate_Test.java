package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static com.cedarsoftware.util.convert.MapConversions.DATE;
import static com.cedarsoftware.util.convert.MapConversions.TIME;
import static com.cedarsoftware.util.convert.MapConversions.ZONE;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toLocalDate_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter needs a ConverterOptions instance; using default options is sufficient for these tests
        converter = new Converter(new ConverterOptions());
    }

    /**
     * Provider for all successful conversion scenarios.
     */
    private static Stream<Arguments> validConversionProvider() {
        // Helper to create a Calendar instance in America/New_York with the requested fields.
        Calendar nyCalendar = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        nyCalendar.set(2018, Calendar.JUNE, 30, 5, 0, 0);
        nyCalendar.set(Calendar.MILLISECOND, 0);
        // Map with DATE, TIME and ZONE (Paris) – should resolve to 2023‑03‑10.
        CompactMap<String, Object> mapWithAll = new CompactMap<>();
        mapWithAll.put(DATE, "2023-03-10");
        mapWithAll.put(TIME, "15:20:00");
        mapWithAll.put(ZONE, "Europe/Paris");
        // Map with only DATE.
        CompactMap<String, Object> mapDateOnly = new CompactMap<>();
        mapDateOnly.put(DATE, "2025-07-20");
        // Map with DATE and TIME only (no zone) – used in a different test group.
        CompactMap<String, Object> mapDateTimeNoZone = new CompactMap<>();
        mapDateTimeNoZone.put(DATE, "2022-05-10");
        mapDateTimeNoZone.put(TIME, "12:34:56");
        return Stream.of(// --- Valid conversions - each branch of toDifferentZone ---
        arguments(ZonedDateTime.of(2023, 8, 15, 10, 30, 0, 0, ZoneId.of("UTC")), "2023-08-15", "ZonedDateTime input – the method should return the LocalDate part of the supplied ZonedDateTime."), arguments(OffsetDateTime.of(2022, 12, 31, 23, 59, 59, 0, ZoneOffset.ofHours(-5)), "2022-12-31", "OffsetDateTime input – the date component should be extracted after converting to a ZonedDateTime."), arguments(LocalDateTime.of(2021, 5, 20, 14, 45, 30), "2021-05-20", "LocalDateTime input – should be interpreted in the system default zone and the date part returned."), arguments(Date.from(Instant.parse("2020-01-01T12:00:00Z")), "2020-01-01", "java.util.Date (epoch milliseconds) – conversion via Instant should yield the correct LocalDate."), arguments(Timestamp.valueOf("2019-07-04 08:15:00"), "2019-07-04", "java.sql.Timestamp – behaves like a Date; the date part must be extracted correctly."), arguments(nyCalendar, "2018-06-30", "Calendar instance – should be converted to an Instant respecting its time‑zone and then to LocalDate."), arguments(Long.valueOf(1609459200000L), "2021-01-01", "Long epoch‑millis – should be interpreted as UTC milliseconds and converted to the proper LocalDate."), arguments("2022-11-05", "2022-11-05", "ISO‑8601 date string – the parser should recognise the format and return the corresponding LocalDate."), arguments(mapWithAll, "2023-03-10", "Map containing DATE, TIME and ZONE keys – the method should build a ZonedDateTime from the parts and then extract the date."), // --- Direct LocalDate input ---
        arguments(LocalDate.of(2024, 2, 29), "2024-02-29", "LocalDate instance should be returned unchanged as a LocalDate."), // --- Instant input ---
        arguments(Instant.parse("2023-01-01T00:00:00Z"), "2023-01-01", "Instant representing midnight UTC should be converted to the same calendar date."), // --- java.sql.Date input ---
        arguments(java.sql.Date.valueOf("2022-04-15"), "2022-04-15", "java.sql.Date behaves like java.util.Date; the date part must be extracted correctly."), // --- Map with DATE only ---
        arguments(mapDateOnly, "2025-07-20", "Map containing only a DATE key should be interpreted as that date at start‑of‑day in the system default zone."), // --- String with date‑time and offset ---
        arguments("2022-03-01T10:15:30+02:00", "2022-03-01", "ISO‑8601 date‑time string that includes an offset should be parsed and the local date extracted."), // --- Number types representing epoch‑millis ---
        arguments(Integer.valueOf(1672531200000), "2023-01-01", "Integer epoch‑millis should be interpreted as UTC milliseconds and converted to the correct LocalDate."), arguments(Double.valueOf(1672531200000.0), "2023-01-01", "Double epoch‑millis should be handled the same way as integral types."), arguments(BigDecimal.valueOf(1672531200000L), "2023-01-01", "BigDecimal representing epoch‑millis must be converted to a LocalDate."), arguments(BigInteger.valueOf(1672531200000L), "2023-01-01", "BigInteger epoch‑millis should be interpreted as UTC milliseconds."), arguments(new AtomicLong(1672531200000L), "2023-01-01", "AtomicLong containing epoch‑millis must be converted to the proper LocalDate."));
    }

    /**
     * Provider for scenarios that should result in an exception.
     */
    private static Stream<Arguments> exceptionConversionProvider() {
        // Map with an impossible DATE (Feb 30)
        CompactMap<String, Object> mapInvalidDate = new CompactMap<>();
        mapInvalidDate.put(DATE, "2022-02-30");
        // Map with an invalid TIME string
        CompactMap<String, Object> mapInvalidTime = new CompactMap<>();
        mapInvalidTime.put(DATE, "2022-05-10");
        mapInvalidTime.put(TIME, "25:61:00");
        // Map with an invalid ZONE identifier
        CompactMap<String, Object> mapInvalidZone = new CompactMap<>();
        mapInvalidZone.put(DATE, "2022-07-01");
        mapInvalidZone.put(ZONE, "Invalid/Zone");
        // Map missing DATE key
        CompactMap<String, Object> mapMissingDate = new CompactMap<>();
        mapMissingDate.put(TIME, "12:00:00");
        mapMissingDate.put(ZONE, "UTC");
        // Map with DATE key but null value
        CompactMap<String, Object> mapNullDate = new CompactMap<>();
        mapNullDate.put(DATE, null);
        return Stream.of(arguments("2022-13-01", DateTimeParseException.class, "Malformed date string should cause a DateTimeParseException."), arguments(mapInvalidDate, DateTimeParseException.class, "Map contains a DATE key with an impossible date (Feb 30)."), arguments(mapInvalidTime, DateTimeParseException.class, "Map contains a TIME key with an invalid time string."), arguments(mapInvalidZone, DateTimeException.class, "Map contains a ZONE key with a non‑existent zone ID."), arguments(mapMissingDate, IllegalArgumentException.class, "Map provides TIME and ZONE but no DATE – should throw IllegalArgumentException."), arguments(Boolean.TRUE, IllegalArgumentException.class, "Unsupported source type (Boolean) should cause IllegalArgumentException."), arguments('A', IllegalArgumentException.class, "Unsupported source type (Character) should cause IllegalArgumentException."), arguments("2022-03-01T10:15:30+25:00", DateTimeParseException.class, "String contains an offset outside the valid range – should fail parsing."), arguments("15:45:30", DateTimeParseException.class, "String that represents only a time cannot be parsed to a date."), arguments(mapNullDate, NullPointerException.class, "Map contains a DATE key with a null value – should result in NullPointerException."));
    }

    /**
     * Verify that a ZonedDateTime instance is converted to the correct LocalDate,
     * respecting the zone conversion performed by {@code toDifferentZone}.
     */
    @Test
    void toLocalDate_fromZonedDateTime() {
        // Given: a ZonedDateTime in a non‑UTC zone
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 15, 23, 30, 0, 0, // UTC‑4 at this date
        ZoneId.of("America/New_York"));
        // When
        LocalDate result = ZonedDateTimeConversions.toLocalDate(zdt, converter);
        // Then: the date should be the same calendar day in the target zone (UTC by default in the conversion)
        // The internal conversion uses the system default zone; we assert the logical expectation:
        // 2023‑05‑15 23:30 EDT is 2023‑05‑16 03:30 UTC, so the LocalDate should be 2023‑05‑16.
        assertEquals(LocalDate.of(2023, 5, 16), result);
    }

    /**
     * Verify conversion from a {@code Map} that contains a {@code DATE} entry.
     * The map mimics the structure used by {@code MapConversions} for date/time values.
     */
    @Test
    void toLocalDate_fromMapWithDateKey() {
        // Given
        Map<String, Object> map = new HashMap<>();
        // The map stores the epoch milli value under the DATE key
        // corresponds to 2023‑03‑20T12:00:00Z
        map.put(MapConversions.DATE, 1_680_000_000_000L);
        // When
        LocalDate result = ZonedDateTimeConversions.toLocalDate(map, converter);
        // Then
        assertEquals(LocalDate.of(2023, 3, 20), result);
    }

    /**
     * Verify that a {@code java.util.Date} instance is correctly converted.
     */
    @Test
    void toLocalDate_fromDate() {
        // Given: a Date representing 2022‑12‑31 10:15 UTC
        Date date = Date.from(Instant.parse("2022-12-31T10:15:00Z"));
        // When
        LocalDate result = ZonedDateTimeConversions.toLocalDate(date, converter);
        // Then
        assertEquals(LocalDate.of(2022, 12, 31), result);
    }

    /**
     * Verify that a {@code java.sql.Timestamp} is handled correctly.
     */
    @Test
    void toLocalDate_fromTimestamp() {
        // Given: a Timestamp for 2021‑07‑04 00:00:00 UTC
        Timestamp ts = Timestamp.from(Instant.parse("2021-07-04T00:00:00Z"));
        // When
        LocalDate result = ZonedDateTimeConversions.toLocalDate(ts, converter);
        // Then
        assertEquals(LocalDate.of(2021, 7, 4), result);
    }

    /**
     * Verify that passing {@code null} returns {@code null}.
     */
    @Test
    void toLocalDate_nullInput() {
        // When
        LocalDate result = ZonedDateTimeConversions.toLocalDate(null, converter);
        // Then
        assertNull(result);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("validConversionProvider")
    void toLocalDate_validConversions(Object from, String expectedDate, String description) {
        LocalDate expected = LocalDate.parse(expectedDate);
        LocalDate result = ZonedDateTimeConversions.toLocalDate(from, null);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("exceptionConversionProvider")
    void toLocalDate_invalidConversions(Object from, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> ZonedDateTimeConversions.toLocalDate(from, null), description);
    }

    /**
     * Additional explicit test for a null source object when the converter is also null.
     */
    @Test
    void toLocalDate_nullSourceAndConverter() {
        // The method is expected to return null (as verified by the original test suite).
        assertNull(ZonedDateTimeConversions.toLocalDate(null, null));
    }
}
