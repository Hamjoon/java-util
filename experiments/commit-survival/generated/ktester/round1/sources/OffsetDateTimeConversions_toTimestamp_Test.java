package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toTimestamp_Test {

    @Mock
    private Converter // not used by the method but required by the signature
    converter;

    private static Stream<Arguments> validOffsetDateTimeProvider() {
        return Stream.of(// Standard UTC conversion with nanoseconds
        Arguments.of(OffsetDateTime.of(2023, 3, 15, 10, 30, 45, 123_456_789, ZoneOffset.UTC), "Standard conversion of a UTC OffsetDateTime with nanos"), // +02:00 offset – should map to the same instant as UTC 10:30:45
        Arguments.of(OffsetDateTime.of(2023, 3, 15, 12, 30, 45, 0, ZoneOffset.ofHours(2)), "+02:00 offset conversion"), // Far‑future supported year
        Arguments.of(OffsetDateTime.of(9999, 12, 31, 23, 59, 59, 999_999_999, ZoneOffset.UTC), "Maximum supported year (9999) conversion"), // Negative offset –05:00
        Arguments.of(OffsetDateTime.of(2023, 3, 15, 7, 30, 45, 0, ZoneOffset.ofHours(-5)), "-05:00 offset conversion"), // Non‑hour offset +05:30
        Arguments.of(OffsetDateTime.of(2023, 3, 15, 18, 0, 0, 0, ZoneOffset.ofHoursMinutes(5, 30)), "+05:30 offset conversion"), // Maximum ZoneOffset (+18:00)
        Arguments.of(OffsetDateTime.of(2023, 1, 1, 18, 0, 0, 0, ZoneOffset.MAX), "Maximum positive offset (+18:00)"), // Minimum ZoneOffset (‑18:00)
        Arguments.of(OffsetDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.MIN), "Minimum negative offset (‑18:00)"), // Epoch start
        Arguments.of(OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), "Epoch start (1970‑01‑01T00:00:00Z)"), // Just before epoch with max nanos
        Arguments.of(OffsetDateTime.of(1969, 12, 31, 23, 59, 59, 999_999_999, ZoneOffset.UTC), "One nanosecond before epoch"), // Very early historical date (year -9999)
        Arguments.of(OffsetDateTime.of(-9999, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), "Very early historical date (year -9999)"));
    }

    private static Stream<Arguments> outOfRangeOffsetDateTimeProvider() {
        return Stream.of(// Far future beyond Timestamp.MAX_VALUE (year 292,278,994‑08‑17)
        Arguments.of(OffsetDateTime.of(292_278_994, 8, 17, 7, 12, 55, 808_000_000, ZoneOffset.UTC), "Instant exceeds java.sql.Timestamp maximum"), // Far past before Timestamp.MIN_VALUE (year -292,278,994‑01‑01)
        Arguments.of(OffsetDateTime.of(-292_278_994, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), "Instant precedes java.sql.Timestamp minimum"));
    }

    @Test
    @DisplayName("toTimestamp converts a valid OffsetDateTime to the correct Timestamp")
    void testToTimestampValid() {
        // given
        OffsetDateTime odt = OffsetDateTime.parse("2023-08-15T12:34:56+02:00");
        Timestamp expected = Timestamp.from(odt.toInstant());
        // when
        Timestamp actual = OffsetDateTimeConversions.toTimestamp(odt, converter);
        // then
        assertEquals(expected, actual, "The Timestamp produced should match the expected value");
    }

    @Test
    @DisplayName("toTimestamp throws ClassCastException when the source object is not an OffsetDateTime")
    void testToTimestampInvalidType() {
        // given
        Object notAnOffsetDateTime = "2023-08-15T12:34:56+02:00";
        // when / then
        assertThrows(ClassCastException.class, () -> OffsetDateTimeConversions.toTimestamp(notAnOffsetDateTime, converter), "A ClassCastException should be thrown for an incompatible source type");
    }

    @Test
    @DisplayName("toTimestamp throws NullPointerException when the source object is null")
    void testToTimestampNullSource() {
        // given
        Object nullSource = null;
        // when / then
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toTimestamp(nullSource, converter), "A NullPointerException should be thrown when the source object is null");
    }

    @Test
    @DisplayName("toTimestamp throws ClassCastException when the source is a String")
    void testToTimestampStringSource() {
        Object notAnOffsetDateTime = "2023-03-15T10:30:45Z";
        assertThrows(ClassCastException.class, () -> OffsetDateTimeConversions.toTimestamp(notAnOffsetDateTime, converter), "A ClassCastException should be thrown for a String source");
    }

    @Test
    @DisplayName("toTimestamp throws ClassCastException when the source is a LocalDateTime")
    void testToTimestampLocalDateTimeSource() {
        Object notAnOffsetDateTime = LocalDateTime.of(2023, 3, 15, 10, 30, 45);
        assertThrows(ClassCastException.class, () -> OffsetDateTimeConversions.toTimestamp(notAnOffsetDateTime, converter), "A ClassCastException should be thrown for a LocalDateTime source");
    }

    /* -------------------------------------------------------------
     *  Converter argument independence
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("toTimestamp ignores a non‑null Converter argument")
    void testConverterArgumentIgnored() {
        OffsetDateTime odt = OffsetDateTime.of(2023, 3, 15, 10, 30, 45, 123_456_789, ZoneOffset.UTC);
        // Use a Mockito mock to represent a non‑null Converter
        Converter nonNullConverter = mock(Converter.class);
        Timestamp expected = Timestamp.from(odt.toInstant());
        Timestamp actual = OffsetDateTimeConversions.toTimestamp(odt, nonNullConverter);
        assertEquals(expected, actual, "Result must be identical regardless of the Converter instance supplied");
    }

    /* -------------------------------------------------------------
     *  Out‑of‑range Instant handling
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {1}")
    @MethodSource("outOfRangeOffsetDateTimeProvider")
    @DisplayName("toTimestamp propagates DateTimeException for Instants outside Timestamp range")
    void testToTimestampOutOfRange(OffsetDateTime odt, String description) {
        assertThrows(DateTimeException.class, () -> OffsetDateTimeConversions.toTimestamp(odt, converter), () -> "Expected DateTimeException for out‑of‑range case: " + description);
    }
}
