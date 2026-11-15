package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toMap_Test {

    private static Converter converter;

    /**
     * Simple holder for a single test case used by the parameterized tests.
     */
    private static class ArgumentsHolder {

        final Object input;

        final Map<String, Object> expected;

        ArgumentsHolder(Object input, Map<String, Object> expected) {
            this.input = input;
            this.expected = expected;
        }

        @Override
        public String toString() {
            return String.valueOf(input);
        }
    }

    /**
     * Holder for exception‑expectation test cases.
     */
    private static class ExceptionCase {

        final Object from;

        final Class<? extends Throwable> exceptionClass;

        final String description;

        ExceptionCase(Object from, Class<? extends Throwable> exceptionClass, String description) {
            this.from = from;
            this.exceptionClass = exceptionClass;
            this.description = description;
        }

        // Used by MethodSource to unpack arguments
        static Stream<Arguments> toArguments(ExceptionCase ec) {
            return Stream.of(Arguments.of(ec.from, ec.exceptionClass, ec.description));
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; using the default constructor.
        converter = new Converter(new ConverterOptions());
    }

    /**
     * Test data for the "toMap - valid conversions" group.
     */
    static Stream<ArgumentsHolder> validConversionsProvider() {
        return Stream.of(// OffsetDateTime
        new ArgumentsHolder(OffsetDateTime.of(2023, 5, 10, 15, 30, 45, 0, ZoneOffset.ofHours(2)), Map.of(MapConversions.DATE, "2023-05-10", MapConversions.TIME, "15:30:45", MapConversions.OFFSET, "+02:00")), // ZonedDateTime (Europe/Paris)
        new ArgumentsHolder(ZonedDateTime.of(2022, 12, 31, 23, 59, 59, 0, ZoneId.of("Europe/Paris")), Map.of(MapConversions.DATE, "2022-12-31", MapConversions.TIME, "23:59:59", MapConversions.OFFSET, "+01:00")), // LocalDateTime (interpreted as UTC)
        new ArgumentsHolder(LocalDateTime.of(2021, 1, 15, 8, 5, 30), Map.of(MapConversions.DATE, "2021-01-15", MapConversions.TIME, "08:05:30", MapConversions.OFFSET, "Z")), // java.util.Date (epoch millis, UTC)
        new ArgumentsHolder(new Date(Instant.parse("2020-06-01T12:00:00Z").toEpochMilli()), Map.of(MapConversions.DATE, "2020-06-01", MapConversions.TIME, "12:00:00", MapConversions.OFFSET, "Z")));
    }

    /**
     * Test data for the "toMap - boundary and special cases" group.
     */
    static Stream<ArgumentsHolder> boundarySpecialProvider() {
        return Stream.of(// Leap day UTC
        new ArgumentsHolder(OffsetDateTime.of(2024, 2, 29, 0, 0, 0, 0, ZoneOffset.UTC), Map.of(MapConversions.DATE, "2024-02-29", MapConversions.TIME, "00:00:00", MapConversions.OFFSET, "Z")), // Non‑standard minute offset
        new ArgumentsHolder(OffsetDateTime.of(2023, 10, 29, 2, 30, 0, 0, ZoneOffset.ofHoursMinutes(1, 30)), Map.of(MapConversions.DATE, "2023-10-29", MapConversions.TIME, "02:30:00", MapConversions.OFFSET, "+01:30")), // DST spring forward New York (2021‑03‑14 02:30 does not exist, Java resolves to 03:30‑04:00)
        new ArgumentsHolder(ZonedDateTime.of(2021, 3, 14, 2, 30, 0, 0, ZoneId.of("America/New_York")), Map.of(MapConversions.DATE, "2021-03-14", // resolved time after gap
        MapConversions.TIME, // resolved time after gap
        "03:30:00", MapConversions.OFFSET, "-04:00")), // DST fall back ambiguous time – earlier offset (-04:00) is chosen
        new ArgumentsHolder(ZonedDateTime.of(2021, 11, 7, 1, 30, 0, 0, ZoneId.of("America/New_York")), Map.of(MapConversions.DATE, "2021-11-07", MapConversions.TIME, "01:30:00", MapConversions.OFFSET, "-04:00")), // Epoch instant
        new ArgumentsHolder(Instant.parse("1970-01-01T00:00:00Z"), Map.of(MapConversions.DATE, "1970-01-01", MapConversions.TIME, "00:00:00", MapConversions.OFFSET, "Z")), // SQL Timestamp at epoch
        new ArgumentsHolder(new Timestamp(0L), Map.of(MapConversions.DATE, "1970-01-01", MapConversions.TIME, "00:00:00", MapConversions.OFFSET, "Z")));
    }

    /**
     * Test data for cases that should throw an exception.
     */
    static Stream<ExceptionCase> exceptionProvider() {
        return Stream.of(// Null source
        new ExceptionCase(null, NullPointerException.class, "null source should cause NPE"), // Null converter
        new ExceptionCase(OffsetDateTime.now(), NullPointerException.class, "null converter should cause NPE"), // Unsupported BigDecimal
        new ExceptionCase(BigDecimal.valueOf(1622548800), IllegalArgumentException.class, "BigDecimal not supported"), // Unsupported AtomicLong
        new ExceptionCase(new AtomicLong(0L), IllegalArgumentException.class, "AtomicLong not supported"), // Unsupported String
        new ExceptionCase("some string", IllegalArgumentException.class, "String not supported"), // Unsupported generic Object
        new ExceptionCase(new Object(), IllegalArgumentException.class, "Object not supported"), // Instant overflow (beyond ZonedDateTime range)
        new ExceptionCase(Instant.ofEpochSecond(Long.MAX_VALUE), DateTimeException.class, "Instant overflow should cause DateTimeException"));
    }

    /**
     * Required for JUnit5 MethodSource to emit the ExceptionCase stream correctly.
     */
    private static Stream<Arguments> exceptionProvider() {
        return exceptionProvider().flatMap(ExceptionCase::toArguments);
    }

    @Test
    void testToMapWithOffsetDateTime() {
        // GIVEN – an OffsetDateTime instance with a non‑UTC offset
        OffsetDateTime offsetDateTime = OffsetDateTime.of(2023, 5, 17, 12, 34, 56, 0, ZoneOffset.ofHoursMinutes(2, 30));
        // WHEN – converting to a map via the focal method
        Map<String, Object> result = OffsetDateTimeConversions.toMap(offsetDateTime, converter);
        // THEN – the map contains the expected date, time and offset strings
        assertEquals("2023-05-17", result.get(MapConversions.DATE));
        assertEquals("12:34:56", result.get(MapConversions.TIME));
        assertEquals("+02:30", result.get(MapConversions.OFFSET));
    }

    @Test
    void testToMapWithZonedDateTime() {
        // GIVEN – a ZonedDateTime that the Converter can turn into an OffsetDateTime internally
        // (Converter knows how to handle ZonedDateTime → OffsetDateTime conversion)
        java.time.ZonedDateTime zonedDateTime = java.time.ZonedDateTime.of(2021, 12, 31, 23, 59, 59, 0, java.time.ZoneId.of("America/New_York"));
        // WHEN – converting to a map via the focal method
        Map<String, Object> result = OffsetDateTimeConversions.toMap(zonedDateTime, converter);
        // THEN – the map reflects the equivalent offset date‑time values
        // New York on 2021‑12‑31 23:59:59 is UTC‑05:00 (standard time)
        assertEquals("2021-12-31", result.get(MapConversions.DATE));
        assertEquals("23:59:59", result.get(MapConversions.TIME));
        assertEquals("-05:00", result.get(MapConversions.OFFSET));
    }

    @ParameterizedTest(name = "[{index}] valid conversion: {0}")
    @MethodSource("validConversionsProvider")
    void testToMapValidConversions(ArgumentsHolder holder) {
        // WHEN
        Map<String, Object> result = OffsetDateTimeConversions.toMap(holder.input, converter);
        // THEN – map contains exactly the expected entries
        assertEquals(holder.expected.get(MapConversions.DATE), result.get(MapConversions.DATE));
        assertEquals(holder.expected.get(MapConversions.TIME), result.get(MapConversions.TIME));
        assertEquals(holder.expected.get(MapConversions.OFFSET), result.get(MapConversions.OFFSET));
    }

    @ParameterizedTest(name = "[{index}] boundary/special case: {0}")
    @MethodSource("boundarySpecialProvider")
    void testToMapBoundaryAndSpecialCases(ArgumentsHolder holder) {
        Map<String, Object> result = OffsetDateTimeConversions.toMap(holder.input, converter);
        assertEquals(holder.expected.get(MapConversions.DATE), result.get(MapConversions.DATE));
        assertEquals(holder.expected.get(MapConversions.TIME), result.get(MapConversions.TIME));
        assertEquals(holder.expected.get(MapConversions.OFFSET), result.get(MapConversions.OFFSET));
    }

    @ParameterizedTest(name = "[{index}] exception case: {2}")
    @MethodSource("exceptionProvider")
    void testToMapExceptionCases(Object from, Class<? extends Throwable> expectedException, String description) {
        // The converter argument may be null for the dedicated test case.
        Converter usedConverter = (description.contains("null converter")) ? null : converter;
        assertThrows(expectedException, () -> OffsetDateTimeConversions.toMap(from, usedConverter), description);
    }
}
