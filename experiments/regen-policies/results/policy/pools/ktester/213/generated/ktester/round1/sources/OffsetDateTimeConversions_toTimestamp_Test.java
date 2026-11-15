package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toTimestamp_Test {

    @Mock
    private Converter // Converter is not used by the method, but required by the signature
    mockConverter;

    private static Stream<Arguments> validOffsetDateTimeProvider() {
        return Stream.of(// typical positive offset
        arguments(OffsetDateTime.parse("2023-01-15T10:15:30+02:00"), "2023-01-15 08:15:30.0", "Convert a typical OffsetDateTime with a positive offset (+02:00)"), // typical negative offset
        arguments(OffsetDateTime.parse("2023-06-01T23:45:00-05:00"), "2023-06-02 04:45:00.0", "Convert an OffsetDateTime with a negative offset (-05:00)"), // UTC (Z) offset
        arguments(OffsetDateTime.parse("2020-02-29T12:00:00Z"), "2020-02-29 12:00:00.0", "Convert an OffsetDateTime at UTC (offset Z)"), // Epoch start
        arguments(OffsetDateTime.parse("1970-01-01T00:00:00+00:00"), "1970-01-01 00:00:00.0", "Epoch start conversion"), // Maximum positive ZoneOffset (+18:00)
        arguments(OffsetDateTime.parse("2023-01-01T00:00:00+18:00"), "2022-12-31 06:00:00.0", "Maximum positive ZoneOffset (+18:00)"), // Maximum negative ZoneOffset (-18:00)
        arguments(OffsetDateTime.parse("2023-01-01T00:00:00-18:00"), "2023-01-01 18:00:00.0", "Maximum negative ZoneOffset (-18:00)"), // Nanosecond precision preservation
        arguments(OffsetDateTime.parse("2023-01-01T12:34:56.123456789+02:00"), "2023-01-01 10:34:56.123456789", "Nanosecond fraction should be preserved"), // Pre‑epoch (just before 1970‑01‑01 UTC)
        arguments(OffsetDateTime.parse("1969-12-31T23:59:59Z"), "1969-12-31 23:59:59.0", "Timestamp just before the Unix epoch"), // Pre‑epoch with negative offset
        arguments(OffsetDateTime.parse("1969-12-31T00:00:00-05:00"), "1969-12-31 05:00:00.0", "Negative offset still before epoch"), // Maximum Timestamp range supported by java.sql.Timestamp
        arguments(OffsetDateTime.of(292278994, 8, 17, 7, 12, 55, 0, ZoneOffset.UTC), "292278994-08-17 07:12:55.0", "Highest instant java.sql.Timestamp can represent"));
    }

    @Test
    void testToTimestampValidOffsetDateTime() {
        // GIVEN
        OffsetDateTime odt = OffsetDateTime.of(2023, 5, 17, 13, 45, 30, 123_000_000, ZoneOffset.UTC);
        Timestamp expected = Timestamp.from(odt.toInstant());
        // WHEN
        Timestamp actual = OffsetDateTimeConversions.toTimestamp(odt, mockConverter);
        // THEN
        assertEquals(expected, actual, "Timestamp produced from OffsetDateTime should match expected value");
    }

    @Test
    void testToTimestampNullInput() {
        // GIVEN
        Object nullInput = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toTimestamp(nullInput, mockConverter), "Passing null should result in NullPointerException due to cast");
    }

    @Test
    void testToTimestampInvalidType() {
        // GIVEN
        String notAnOffsetDateTime = "2023-05-17T13:45:30Z";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> OffsetDateTimeConversions.toTimestamp(notAnOffsetDateTime, mockConverter), "Passing a non‑OffsetDateTime object should cause ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validOffsetDateTimeProvider")
    void testToTimestampValidConversions(OffsetDateTime input, String expectedString, String description) {
        // WHEN
        Timestamp actual = OffsetDateTimeConversions.toTimestamp(input, mockConverter);
        // THEN
        assertEquals(expectedString, actual.toString(), () -> "Failed: " + description + " – expected " + expectedString + " but got " + actual);
    }

    @Test
    void testToTimestampInvalidString() {
        // GIVEN
        String notAnOffsetDateTime = "2023-01-01T00:00:00Z";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> OffsetDateTimeConversions.toTimestamp(notAnOffsetDateTime, mockConverter), "Providing a String should cause ClassCastException");
    }

    @Test
    void testToTimestampInvalidLocalDateTime() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.now();
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> OffsetDateTimeConversions.toTimestamp(ldt, mockConverter), "Providing a LocalDateTime should cause ClassCastException");
    }

    @Test
    void testToTimestampConverterIgnoredEvenWhenNonNull() {
        // GIVEN
        OffsetDateTime odt = OffsetDateTime.parse("2023-04-01T12:00:00+01:00");
        Converter hostileConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                throw new AssertionError("Converter should not be invoked");
            }
        };
        // WHEN
        Timestamp ts = OffsetDateTimeConversions.toTimestamp(odt, hostileConverter);
        // THEN
        assertEquals("2023-04-01 11:00:00.0", ts.toString(), "Conversion must succeed and ignore the supplied Converter");
    }

    @Test
    void testToTimestampOverflowBeyondTimestampCapacity() {
        // GIVEN: an instant beyond the max value Timestamp can handle
        OffsetDateTime beyond = OffsetDateTime.of(292278994, 8, 18, 0, 0, 0, 0, ZoneOffset.UTC);
        // WHEN / THEN
        assertThrows(DateTimeException.class, () -> OffsetDateTimeConversions.toTimestamp(beyond, mockConverter), "Instant beyond Timestamp range should throw DateTimeException");
    }

    @Test
    void testToTimestampWithNullConverter() {
        // GIVEN
        OffsetDateTime odt = OffsetDateTime.of(2023, 5, 17, 13, 45, 30, 123_000_000, ZoneOffset.UTC);
        Timestamp expected = Timestamp.from(odt.toInstant());
        // WHEN
        // explicit null converter
        Timestamp actual = OffsetDateTimeConversions.toTimestamp(odt, null);
        // THEN
        assertEquals(expected, actual, "Method must work correctly even when the Converter argument is null");
    }
}
