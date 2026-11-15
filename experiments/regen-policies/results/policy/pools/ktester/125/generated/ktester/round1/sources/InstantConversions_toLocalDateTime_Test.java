package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class InstantConversions_toLocalDateTime_Test {

    /**
     * Converter used for all tests – it contains the built‑in conversion rules.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; the default options are sufficient for our tests.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally.
        converter = null;
    }

    /**
     * Helper that returns the expected {@link LocalDateTime} for a given {@link Instant}
     * using the same zone conversion logic that {@code InstantConversions.toZonedDateTime}
     * applies (the system default zone).
     */
    private static LocalDateTime expectedFromInstant(Instant instant) {
        return instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * Supplies a stream of valid input objects together with the expected {@link LocalDateTime}.
     */
    private static Stream<Arguments> validConversionProvider() {
        Instant instant = Instant.parse("2023-01-02T03:04:05Z");
        LocalDateTime expected = expectedFromInstant(instant);
        // Date (java.util.Date)
        Date date = Date.from(instant);
        // Timestamp (java.sql.Timestamp)
        Timestamp timestamp = Timestamp.from(instant);
        // Long epoch‑millis
        Long epochMilli = instant.toEpochMilli();
        // Calendar (default time‑zone)
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        calendar.setTimeInMillis(instant.toEpochMilli());
        return Stream.of(Arguments.of(instant, expected), Arguments.of(date, expected), Arguments.of(timestamp, expected), Arguments.of(epochMilli, expected), Arguments.of(calendar, expected));
    }

    @Test
    void toLocalDateTime_fromInstant() {
        Instant instant = Instant.parse("2023-01-02T03:04:05Z");
        LocalDateTime expected = expectedFromInstant(instant);
        LocalDateTime actual = InstantConversions.toLocalDateTime(instant, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalDateTime_fromDate() {
        Instant instant = Instant.parse("2023-06-15T12:30:45Z");
        Date date = Date.from(instant);
        LocalDateTime expected = expectedFromInstant(instant);
        LocalDateTime actual = InstantConversions.toLocalDateTime(date, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalDateTime_fromTimestamp() {
        Instant instant = Instant.parse("2022-12-31T23:59:59Z");
        Timestamp timestamp = Timestamp.from(instant);
        LocalDateTime expected = expectedFromInstant(instant);
        LocalDateTime actual = InstantConversions.toLocalDateTime(timestamp, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalDateTime_fromLongEpochMilli() {
        Instant instant = Instant.parse("2021-03-10T08:15:30Z");
        Long epochMilli = instant.toEpochMilli();
        LocalDateTime expected = expectedFromInstant(instant);
        LocalDateTime actual = InstantConversions.toLocalDateTime(epochMilli, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalDateTime_fromNull_returnsNull() {
        assertNull(InstantConversions.toLocalDateTime(null, converter), "When the source object is null the conversion should return null.");
    }

    /* -------------------------------------------------------------
     *  Parameterized conversion tests
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from {0}")
    @MethodSource("validConversionProvider")
    void toLocalDateTime_validConversions(Object from, LocalDateTime expected) {
        // Act
        LocalDateTime actual = InstantConversions.toLocalDateTime(from, converter);
        // Assert
        assertEquals(expected, actual, () -> "Conversion from " + (from == null ? "null" : from.getClass().getSimpleName()) + " did not produce the expected LocalDateTime");
    }

    /* -------------------------------------------------------------
     *  Null‑converter test
     * ------------------------------------------------------------- */
    @Test
    void toLocalDateTime_nullConverter_throwsNullPointerException() {
        Instant instant = Instant.parse("2023-01-01T12:00:00Z");
        assertThrows(NullPointerException.class, () -> InstantConversions.toLocalDateTime(instant, null), "Providing a null Converter must result in a NullPointerException.");
    }
}
