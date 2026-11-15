package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.sql.Date;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toLocalDate_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – no custom overrides needed for these tests
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        converter = null;
    }

    private static Stream<Arguments> provideInstantCases() {
        return Stream.of(Arguments.of(Instant.parse("2023-07-04T12:00:00Z"), LocalDate.of(2023, 7, 4)));
    }

    private static Stream<Arguments> provideSqlDateCases() {
        return Stream.of(Arguments.of(Date.valueOf("1999-12-31"), LocalDate.of(1999, 12, 31)));
    }

    private static Stream<Arguments> provideLongEpochMilliCases() {
        return Stream.of(// 1622505600000L == 2021‑06‑01T00:00:00Z
        Arguments.of(1622505600000L, LocalDate.of(2021, 6, 1)));
    }

    private static Stream<Arguments> provideIsoLocalDateStringCases() {
        return Stream.of(Arguments.of("2025-11-14", LocalDate.of(2025, 11, 14)));
    }

    private static Stream<Arguments> provideIsoDateTimeStringCases() {
        return Stream.of(Arguments.of("2020-08-20T15:45:30", LocalDate.of(2020, 8, 20)));
    }

    @Test
    void testToLocalDateFromLocalDate() {
        LocalDate source = LocalDate.of(2023, 5, 10);
        LocalDate result = LocalDateTimeConversions.toLocalDate(source, converter);
        assertEquals(source, result);
    }

    @Test
    void testToLocalDateFromLocalDateTime() {
        LocalDateTime source = LocalDateTime.of(2023, 5, 10, 15, 30, 45);
        LocalDate expected = source.toLocalDate();
        LocalDate result = LocalDateTimeConversions.toLocalDate(source, converter);
        assertEquals(expected, result);
    }

    @Test
    void testToLocalDateFromZonedDateTime() {
        ZonedDateTime source = ZonedDateTime.of(2023, 5, 10, 15, 30, 0, 0, ZoneOffset.UTC);
        LocalDate expected = source.toLocalDate();
        LocalDate result = LocalDateTimeConversions.toLocalDate(source, converter);
        assertEquals(expected, result);
    }

    @Test
    void testToLocalDateFromDate() {
        // 2023‑05‑10T00:00:00Z
        Instant instant = Instant.parse("2023-05-10T00:00:00Z");
        Date source = Date.from(instant);
        LocalDate expected = instant.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate result = LocalDateTimeConversions.toLocalDate(source, converter);
        assertEquals(expected, result);
    }

    @Test
    void testToLocalDateFromString() {
        // ISO‑8601 LocalDate format
        String source = "2023-05-10";
        LocalDate expected = LocalDate.parse(source);
        LocalDate result = LocalDateTimeConversions.toLocalDate(source, converter);
        assertEquals(expected, result);
    }

    @Test
    void testToLocalDateFromMapWithTimeKey() {
        // Map representation expected by Converter for Date/Time types
        Map<String, Object> source = new LinkedHashMap<>();
        // Epoch milliseconds for 2023‑05‑10T00:00:00Z
        long epochMillis = 1683686400000L;
        source.put("time", epochMillis);
        LocalDate expected = Instant.ofEpochMilli(epochMillis).atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate result = LocalDateTimeConversions.toLocalDate(source, converter);
        assertEquals(expected, result);
    }

    @Test
    void testToLocalDateFromNull() {
        assertNull(LocalDateTimeConversions.toLocalDate(null, converter));
    }

    /**
     * Conversion from {@link Instant} – the implementation uses UTC when
     * delegating to {@code toZonedDateTime}, therefore we assert against the
     * UTC date component.
     */
    @ParameterizedTest
    @MethodSource("provideInstantCases")
    void testToLocalDateFromInstant(Instant instant, LocalDate expected) {
        LocalDate result = LocalDateTimeConversions.toLocalDate(instant, converter);
        assertEquals(expected, result);
    }

    /**
     * Conversion from {@link java.sql.Date}.  This type already represents a
     * date‑only value, so the result should match the value returned by
     * {@code toLocalDate()}.
     */
    @ParameterizedTest
    @MethodSource("provideSqlDateCases")
    void testToLocalDateFromSqlDate(Date sqlDate, LocalDate expected) {
        LocalDate result = LocalDateTimeConversions.toLocalDate(sqlDate, converter);
        assertEquals(expected, result);
    }

    /**
     * Conversion from a {@code long} representing epoch milliseconds.
     * The conversion is performed in UTC.
     */
    @ParameterizedTest
    @MethodSource("provideLongEpochMilliCases")
    void testToLocalDateFromLongEpochMilli(Long epochMilli, LocalDate expected) {
        LocalDate result = LocalDateTimeConversions.toLocalDate(epochMilli, converter);
        assertEquals(expected, result);
    }

    /**
     * ISO‑8601 date string (no time component) – should be parsed directly.
     */
    @ParameterizedTest
    @MethodSource("provideIsoLocalDateStringCases")
    void testToLocalDateFromStringIsoLocalDate(String source, LocalDate expected) {
        LocalDate result = LocalDateTimeConversions.toLocalDate(source, converter);
        assertEquals(expected, result);
    }

    /**
     * ISO‑8601 date‑time string – the date component should be extracted.
     */
    @ParameterizedTest
    @MethodSource("provideIsoDateTimeStringCases")
    void testToLocalDateFromStringIsoDateTime(String source, LocalDate expected) {
        LocalDate result = LocalDateTimeConversions.toLocalDate(source, converter);
        assertEquals(expected, result);
    }

    /**
     * Null source value – should return {@code null} without throwing.
     */
    @Test
    void testToLocalDate_NullInput() {
        assertNull(LocalDateTimeConversions.toLocalDate(null, converter));
    }

    /**
     * Invalid date string – expect {@link DateTimeParseException}.
     */
    @Test
    void testToLocalDate_InvalidString() {
        String source = "not-a-date";
        assertThrows(DateTimeParseException.class, () -> LocalDateTimeConversions.toLocalDate(source, converter));
    }

    /**
     * Null {@link Converter} argument – the method should defensively throw
     * {@link NullPointerException}.
     */
    @Test
    void testToLocalDate_NullConverter() {
        String source = "2022-01-01";
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toLocalDate(source, null));
    }

    /**
     * Unsupported source type – expect {@link IllegalArgumentException}.
     */
    @Test
    void testToLocalDate_UnsupportedType() {
        Boolean source = Boolean.TRUE;
        assertThrows(IllegalArgumentException.class, () -> LocalDateTimeConversions.toLocalDate(source, converter));
    }

    @Test
    void testToLocalDate_fromLocalDate_JSONGroup() {
        LocalDate source = LocalDate.of(2022, 12, 31);
        LocalDate result = LocalDateTimeConversions.toLocalDate(source, new Converter());
        assertEquals(LocalDate.of(2022, 12, 31), result);
    }
}
