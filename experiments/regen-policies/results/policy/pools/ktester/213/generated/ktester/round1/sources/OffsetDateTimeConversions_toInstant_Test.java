package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toInstant_Test {

    /**
     * Mocked {@link Converter} – not used by the method under test.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Common test data: an {@link OffsetDateTime} representing a known instant.
     */
    private static final OffsetDateTime SAMPLE_OFFSET_DATE_TIME = OffsetDateTime.of(2023, 5, 17, 12, 34, 56, 123_456_789, ZoneOffset.UTC);

    /**
     * Expected {@link Instant} for {@link #SAMPLE_OFFSET_DATE_TIME}.
     */
    private static final Instant EXPECTED_INSTANT = SAMPLE_OFFSET_DATE_TIME.toInstant();

    /**
     * A dummy non‑null {@link Converter} instance for the specific test case that supplies one.
     */
    private static final Converter DUMMY_CONVERTER = new Converter() {
    };

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Simple UTC conversion
        Arguments.of("Convert a UTC OffsetDateTime to Instant; the instant should match the same date‑time value.", OffsetDateTime.of(2023, 5, 10, 15, 30, 45, 0, ZoneOffset.UTC), Instant.parse("2023-05-10T15:30:45Z")), // Positive offset (+05:30) – should be shifted back to UTC
        Arguments.of("Convert an OffsetDateTime with +05:30 offset; the resulting instant must be adjusted back to UTC.", OffsetDateTime.of(2023, 5, 10, 15, 30, 45, 0, ZoneOffset.ofHoursMinutes(5, 30)), Instant.parse("2023-05-10T10:00:45Z")), // Maximum positive offset (+18:00)
        Arguments.of("OffsetDateTime with the maximum positive offset (+18:00). The instant must be shifted 18 hours back to UTC.", OffsetDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(18)), Instant.parse("2022-12-31T06:00:00Z")), // Maximum negative offset (‑18:00)
        Arguments.of("OffsetDateTime with the maximum negative offset (‑18:00). The instant must be shifted 18 hours forward to UTC.", OffsetDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHoursMinutes(-18, 0)), Instant.parse("2023-01-01T18:00:00Z")), // Nanosecond precision
        Arguments.of("OffsetDateTime includes a non‑zero nanosecond field; the resulting Instant must preserve the nanosecond precision.", OffsetDateTime.of(2023, 5, 10, 15, 30, 45, 123_456_789, ZoneOffset.UTC), Instant.parse("2023-05-10T15:30:45.123456789Z")), // Pre‑epoch date (just before 1970‑01‑01)
        Arguments.of("OffsetDateTime representing a moment before the Unix epoch; Instant should correctly handle negative epoch seconds.", OffsetDateTime.of(1969, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC), Instant.parse("1969-12-31T23:59:59Z")), // Leap‑day with negative offset
        Arguments.of("Leap‑day date with a negative offset; verifies correct conversion across leap years and offset adjustment.", OffsetDateTime.of(1960, 2, 29, 12, 0, 0, 0, ZoneOffset.ofHours(-5)), Instant.parse("1960-02-29T17:00:00Z")), // Non‑null Converter argument (method ignores it)
        Arguments.of("Converter argument is supplied (even though the method ignores it); conversion should still succeed.", OffsetDateTime.of(2023, 5, 10, 15, 30, 45, 0, ZoneOffset.UTC), Instant.parse("2023-05-10T15:30:45Z")));
    }

    private static Stream<Arguments> invalidSourceProvider() {
        return Stream.of(Arguments.of("Providing a String instead of an OffsetDateTime should cause a ClassCastException.", "2023-05-10T15:30:45Z"), Arguments.of("Providing a LocalDateTime (which is not an OffsetDateTime) should also result in a ClassCastException.", LocalDateTime.of(2023, 5, 10, 15, 30, 45)));
    }

    @Test
    void toInstant_givenValidOffsetDateTime_returnsCorrespondingInstant() {
        // GIVEN
        Object source = SAMPLE_OFFSET_DATE_TIME;
        // WHEN
        Instant result = OffsetDateTimeConversions.toInstant(source, mockConverter);
        // THEN
        assertEquals(EXPECTED_INSTANT, result, "The Instant returned by toInstant should match the one produced by OffsetDateTime#toInstant()");
    }

    @Test
    void toInstant_givenNullSource_throwsNullPointerException() {
        // GIVEN
        Object source = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toInstant(source, mockConverter), "Calling toInstant with a null source should result in a NullPointerException");
    }

    @Test
    void toInstant_givenIncorrectType_throwsClassCastException() {
        // GIVEN
        Object source = "I am not an OffsetDateTime";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> OffsetDateTimeConversions.toInstant(source, mockConverter), "Calling toInstant with a non‑OffsetDateTime source should result in a ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionProvider")
    void toInstant_validConversions(String description, OffsetDateTime source, Instant expected) {
        // GIVEN
        Object from = source;
        // WHEN
        Instant result = OffsetDateTimeConversions.toInstant(from, mockConverter);
        // THEN
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidSourceProvider")
    void toInstant_invalidSourceTypes(String description, Object invalidSource) {
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> OffsetDateTimeConversions.toInstant(invalidSource, mockConverter), description);
    }

    @Test
    void toInstant_withRealConverterInstance_behavesSameAsMock() {
        // GIVEN
        OffsetDateTime odt = OffsetDateTime.of(2023, 5, 10, 15, 30, 45, 0, ZoneOffset.UTC);
        Instant expected = odt.toInstant();
        // WHEN
        Instant result = OffsetDateTimeConversions.toInstant(odt, DUMMY_CONVERTER);
        // THEN
        assertEquals(expected, result, "Even when a concrete Converter implementation is supplied, the conversion must produce the same Instant.");
    }
}
