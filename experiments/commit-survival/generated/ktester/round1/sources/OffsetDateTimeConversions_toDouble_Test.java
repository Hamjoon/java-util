package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.DateTimeException;
import java.util.Date;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toDouble_Test {

    /**
     * The (mocked) {@link Converter} required by the method signature.
     */
    @Mock
    private Converter converter;

    private static Stream<org.junit.jupiter.params.provider.Arguments> validOffsetDateTimeProvider() {
        return Stream.of(// Group: Valid OffsetDateTime conversions
        org.junit.jupiter.params.provider.Arguments.of("1970-01-01T00:00:00Z", 0.0), org.junit.jupiter.params.provider.Arguments.of("1970-01-01T00:00:00.123456789Z", 0.123456789), org.junit.jupiter.params.provider.Arguments.of("2023-03-25T12:34:56.987654321+02:00", 1679740496.9876543), org.junit.jupiter.params.provider.Arguments.of("1969-12-31T23:59:59.999999999Z", -0.000000001), // Group: Additional valid OffsetDateTime conversions
        org.junit.jupiter.params.provider.Arguments.of("2022-01-01T12:00:00-05:30", 1641058200.0), org.junit.jupiter.params.provider.Arguments.of("3000-01-01T00:00:00Z", 32503420800.0), org.junit.jupiter.params.provider.Arguments.of("1970-01-01T00:00:00.999999999Z", 0.999999999), org.junit.jupiter.params.provider.Arguments.of("1969-12-31T23:59:58.250000000Z", -1.75), // Same as earlier but with a non‑null converter (the mock is non‑null)
        org.junit.jupiter.params.provider.Arguments.of("2022-01-01T12:00:00-05:30", 1641058200.0));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> invalidInputProvider() {
        return Stream.of(// Group: Invalid inputs
        org.junit.jupiter.params.provider.Arguments.of(null, NullPointerException.class), org.junit.jupiter.params.provider.Arguments.of("2020-01-01T00:00:00Z", ClassCastException.class), // Group: Additional invalid inputs
        org.junit.jupiter.params.provider.Arguments.of(new Date(), ClassCastException.class), org.junit.jupiter.params.provider.Arguments.of(null, NullPointerException.class));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> outOfRangeProvider() {
        return Stream.of(// Group: Invalid range inputs
        org.junit.jupiter.params.provider.Arguments.of("1000000001-01-01T00:00:00Z"), org.junit.jupiter.params.provider.Arguments.of("1000000000-12-31T23:59:59+18:00"));
    }

    /**
     * Verify that a typical {@link OffsetDateTime} is converted to the correct {@code double}
     * representation (seconds since the epoch plus the fractional nanosecond part).
     */
    @Test
    void toDouble_returnsCorrectDoubleForUtcOffset() {
        // GIVEN
        OffsetDateTime odt = OffsetDateTime.of(// 2020‑01‑02
        2020, // 2020‑01‑02
        1, // 2020‑01‑02
        2, // 12:34:56.789
        12, // 12:34:56.789
        34, // 12:34:56.789
        56, // 12:34:56.789
        789_000_000, // UTC offset
        ZoneOffset.UTC);
        Instant instant = odt.toInstant();
        double expected = instant.getEpochSecond() + instant.getNano() / 1_000_000_000d;
        // WHEN
        double actual = OffsetDateTimeConversions.toDouble(odt, converter);
        // THEN
        assertEquals(expected, actual, 1e-12, "The double value should represent the epoch second plus the fractional nanoseconds.");
    }

    /**
     * Verify that a non‑UTC offset is handled correctly – the conversion uses the
     * {@link OffsetDateTime}'s instant, which is always in UTC.
     */
    @Test
    void toDouble_handlesNonUtcOffset() {
        // GIVEN
        OffsetDateTime odt = OffsetDateTime.of(2021, 5, 15, 23, 59, 59, 123_456_789, // +05:30 offset
        ZoneOffset.ofHoursMinutes(5, 30));
        // instant is normalized to UTC
        Instant instant = odt.toInstant();
        double expected = instant.getEpochSecond() + instant.getNano() / 1_000_000_000d;
        // WHEN
        double actual = OffsetDateTimeConversions.toDouble(odt, converter);
        // THEN
        assertEquals(expected, actual, 1e-12);
    }

    /**
     * The method casts the input to {@link OffsetDateTime} without a null‑check,
     * therefore a {@code NullPointerException} is expected when {@code null} is supplied.
     */
    @Test
    void toDouble_nullInput_throwsNullPointerException() {
        // GIVEN
        Object nullInput = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toDouble(nullInput, converter));
    }

    /**
     * Valid {@link OffsetDateTime} conversions – the method should return a double that
     * equals the instant's epoch second plus the nanosecond fraction.
     */
    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @MethodSource("validOffsetDateTimeProvider")
    void toDouble_validOffsetDateTime(String isoString, double expected) {
        // GIVEN
        OffsetDateTime odt = OffsetDateTime.parse(isoString);
        // WHEN
        double actual = OffsetDateTimeConversions.toDouble(odt, converter);
        // THEN
        assertEquals(expected, actual, 1e-12, () -> "Incorrect double conversion for " + isoString);
    }

    /**
     * Invalid inputs that should cause an exception. The expected exception type
     * is asserted for each case.
     */
    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @MethodSource("invalidInputProvider")
    void toDouble_invalidInputs(Object from, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        assertThrows(expectedException, () -> OffsetDateTimeConversions.toDouble(from, converter));
    }

    /**
     * Inputs that are syntactically valid {@link OffsetDateTime}s but fall outside
     * the range supported by {@link java.time.Instant}. The method should propagate
     * a {@link DateTimeException}.
     */
    @ParameterizedTest(name = "[{index}] {0} → DateTimeException")
    @MethodSource("outOfRangeProvider")
    void toDouble_outOfRangeInputs(String isoString) {
        // GIVEN
        OffsetDateTime odt = OffsetDateTime.parse(isoString);
        // WHEN / THEN
        assertThrows(DateTimeException.class, () -> OffsetDateTimeConversions.toDouble(odt, converter));
    }

    /**
     * Verify that a {@code null} {@link Converter} does not affect the result.
     * (The method does not use the converter at all.)
     */
    @Test
    void toDouble_nullConverter_isIgnored() {
        // GIVEN
        OffsetDateTime odt = OffsetDateTime.of(2022, 1, 1, 12, 0, 0, 0, // -05:30
        ZoneOffset.ofHours(-5).minusMinutes(30));
        double expected = odt.toInstant().getEpochSecond() + odt.toInstant().getNano() / 1_000_000_000d;
        // WHEN
        double actual = OffsetDateTimeConversions.toDouble(odt, null);
        // THEN
        assertEquals(expected, actual, 1e-12);
    }
}
