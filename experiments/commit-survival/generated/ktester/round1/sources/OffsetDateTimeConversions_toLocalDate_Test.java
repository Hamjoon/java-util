package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.time.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import java.time.format.DateTimeParseException;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toLocalDate_Test {

    /**
     * Converter used for all test cases.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // ConverterOptions has a public no‑arg constructor in the library.
        // If a different way to obtain a default options instance is required,
        // adjust this line accordingly.
        converter = new Converter(new ConverterOptions());
    }

    /**
     * Supplies a range of source objects together with the LocalDate they should resolve to.
     */
    private static Stream<Arguments> provideObjectsForToLocalDate() {
        // Helper for the Calendar case
        Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCal.setTimeInMillis(Instant.parse("2020-11-05T14:30:00Z").toEpochMilli());
        // Expected date for the OffsetTime case – the conversion falls back to the
        // current date in the system default zone (implementation‑defined).
        LocalDate today = LocalDate.now();
        return Stream.of(// OffsetDateTime – the method ultimately delegates to toZonedDateTime()
        Arguments.of(OffsetDateTime.of(2023, 5, 10, 15, 30, 0, 0, ZoneOffset.UTC), LocalDate.of(2023, 5, 10)), // ZonedDateTime – another supported temporal type
        Arguments.of(ZonedDateTime.of(2023, 6, 15, 0, 0, 0, 0, ZoneOffset.ofHours(2)), LocalDate.of(2023, 6, 15)), // java.sql.Timestamp – convertible via the built‑in conversion rules
        Arguments.of(Timestamp.valueOf("2022-01-01 10:20:30.0"), LocalDate.of(2022, 1, 1)), // java.util.Date – should be interpreted as an instant in the default zone
        Arguments.of(Date.from(Instant.parse("2021-12-31T23:59:59Z")), LocalDate.of(2021, 12, 31)), // Calendar (UTC) – the time‑zone of the calendar is taken into account
        Arguments.of(utcCal, LocalDate.of(2020, 11, 5)), // Long representing epoch‑millis
        Arguments.of(Long.valueOf(Instant.parse("2019-01-31T00:00:00Z").toEpochMilli()), LocalDate.of(2019, 1, 31)), // ISO‑8601 string with offset
        Arguments.of("2024-02-29T12:00:00+01:00", LocalDate.of(2024, 2, 29)), // Date‑only string (no time/offset)
        Arguments.of("2023-01-01", LocalDate.of(2023, 1, 1)), // LocalDateTime – treated as if it were in the system default zone
        Arguments.of(LocalDateTime.of(2022, 5, 20, 15, 30, 45), LocalDate.of(2022, 5, 20)), // OffsetTime – lacks a date, conversion falls back to today’s date
        Arguments.of(OffsetTime.of(13, 45, 0, 0, ZoneOffset.ofHours(2)), today));
    }

    /**
     * Supplies inputs that should cause conversion failures.
     */
    private static Stream<Arguments> provideInvalidInputs() {
        // A collection that cannot be interpreted as a date/time value.
        List<Integer> list = Arrays.asList(1, 2, 3);
        return Stream.of(// Unsupported type (Integer) – library throws ConversionException
        Arguments.of(Integer.valueOf(12345), ConversionException.class), // Malformed date‑time string – parsing fails
        Arguments.of("not-a-date-time", DateTimeParseException.class), // Epoch milliseconds far beyond the supported range – overflow
        Arguments.of(Long.valueOf(Long.MAX_VALUE), DateTimeException.class), // Invalid zone identifier in the string
        Arguments.of("2023-01-01T00:00:00[Foo/Bar]", DateTimeException.class), // Unsupported collection type
        Arguments.of(list, ConversionException.class));
    }

    /**
     * Verify that {@code OffsetDateTimeConversions.toLocalDate(Object, Converter)}
     * correctly converts a variety of source types to {@link LocalDate}.
     */
    @ParameterizedTest
    @MethodSource("provideObjectsForToLocalDate")
    void testToLocalDate(Object source, LocalDate expected) {
        LocalDate actual = OffsetDateTimeConversions.toLocalDate(source, converter);
        assertEquals(expected, actual, () -> "Conversion of " + source.getClass().getSimpleName() + " did not produce the expected LocalDate");
    }

    /**
     * {@code null} source should trigger {@link IllegalArgumentException}.
     */
    @Test
    void testNullSourceThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toLocalDate(null, converter));
    }

    /**
     * Verify that unsupported or malformed inputs raise the expected exceptions.
     */
    @ParameterizedTest(name = "[{index}] source={0} → throws {1}")
    @MethodSource("provideInvalidInputs")
    void testInvalidConversions(Object source, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> OffsetDateTimeConversions.toLocalDate(source, converter), () -> "Expected " + expectedException.getSimpleName() + " for source: " + source);
    }
}
