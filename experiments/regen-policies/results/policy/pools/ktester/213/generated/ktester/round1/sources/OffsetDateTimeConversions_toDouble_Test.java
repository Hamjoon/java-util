package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toDouble_Test {

    /**
     * Converter instance – not used by the method but required by the signature.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Converter can be created with default options; the method under test does not rely on it.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test.
        converter = null;
    }

    /**
     * Supplies the valid test data.
     *
     * <p>Each {@link Arguments} entry contains:
     * <ul>
     *   <li>the {@link OffsetDateTime} to convert,</li>
     *   <li>the expected double value (parsed from the JSON string),</li>
     *   <li>a short description for readability.</li>
     * </ul>
     */
    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// 2023‑01‑01T00:00:00Z → epoch second 1672531200
        Arguments.of(OffsetDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), Double.parseDouble("1672531200.0"), "Midnight UTC, no nanos"), // 2023‑01‑01T00:00:00.123456789Z
        Arguments.of(OffsetDateTime.of(2023, 1, 1, 0, 0, 0, 123_456_789, ZoneOffset.UTC), Double.parseDouble("1672531200.123456789"), "Midnight UTC with nanoseconds"), // 2023‑01‑01T12:34:56+02:00 → normalized to 2023‑01‑01T10:34:56Z
        Arguments.of(OffsetDateTime.of(2023, 1, 1, 12, 34, 56, 0, ZoneOffset.ofHours(2)), Double.parseDouble("1672579296.0"), "+02:00 offset"), // 2022‑12‑31T23:59:59.999999999Z (max nanos before next second)
        Arguments.of(OffsetDateTime.of(2022, 12, 31, 23, 59, 59, 999_999_999, ZoneOffset.UTC), Double.parseDouble("1672531199.999999999"), "Just before next second, max nanos"), // Epoch start
        Arguments.of(OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), Double.parseDouble("0.0"), "Epoch start"), // One second before epoch (negative epoch second)
        Arguments.of(OffsetDateTime.of(1969, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC), Double.parseDouble("-1.0"), "One second before epoch"), // Leap day with -05:30 offset → 2000‑02‑29T12:00:00‑05:30 = 2000‑02‑29T17:30:00Z
        Arguments.of(OffsetDateTime.of(2000, 2, 29, 12, 0, 0, 0, ZoneOffset.ofHoursMinutes(-5, -30)), Double.parseDouble("951825600.0"), "Leap day with negative offset"), // DST transition (Europe, spring forward) – explicit offset +01:00
        Arguments.of(OffsetDateTime.of(2023, 3, 26, 2, 30, 0, 0, ZoneOffset.ofHours(1)), Double.parseDouble("1679793000.0"), "DST transition (spring forward) with explicit offset"));
    }

    /**
     * Supplies the exception‑scenario test data.
     */
    private static Stream<Arguments> exceptionScenarioProvider() {
        return Stream.of(// Null source → NullPointerException (cast fails on null)
        Arguments.of(null, NullPointerException.class, "null source should cause NPE"), // Wrong type: String
        Arguments.of("2023-01-01T00:00:00Z", ClassCastException.class, "String source should cause ClassCastException"), // Wrong type: Integer
        Arguments.of(12345, ClassCastException.class, "Integer source should cause ClassCastException"));
    }

    /**
     * Verify that a regular {@code OffsetDateTime} is converted to the correct double value.
     * The expected value is calculated as epoch‑seconds plus nanoseconds expressed as a fraction of a second.
     */
    @Test
    void toDouble_withUtcOffset_returnsCorrectDouble() {
        // given
        OffsetDateTime odt = OffsetDateTime.of(// year, month, day
        2023, // year, month, day
        5, // year, month, day
        10, // hour, minute, second
        12, // hour, minute, second
        34, // hour, minute, second
        56, // nanosecond
        123_456_789, // offset
        ZoneOffset.UTC);
        // when
        double result = OffsetDateTimeConversions.toDouble(odt, converter);
        // then
        Instant instant = odt.toInstant();
        double expected = instant.getEpochSecond() + instant.getNano() / 1_000_000_000d;
        assertEquals(expected, result, 1e-12, "Conversion to double should match epoch seconds plus nanos fraction");
    }

    /**
     * Verify that an {@code OffsetDateTime} with a non‑zero offset is handled correctly.
     * The offset is normalised to an {@code Instant} (UTC) before the calculation.
     */
    @Test
    void toDouble_withPositiveOffset_returnsCorrectDouble() {
        // given: offset of +02:30
        OffsetDateTime odt = OffsetDateTime.of(2021, 12, 31, 23, 0, 0, 0, ZoneOffset.ofHoursMinutes(2, 30));
        // when
        double result = OffsetDateTimeConversions.toDouble(odt, converter);
        // then
        // should be 2021‑12‑31T20:30:00Z
        Instant instant = odt.toInstant();
        double expected = instant.getEpochSecond() + instant.getNano() / 1_000_000_000d;
        assertEquals(expected, result, 1e-12);
    }

    /**
     * Verify that a {@code null} source causes a {@link NullPointerException},
     * because the implementation casts the argument to {@code OffsetDateTime} without a null check.
     */
    @Test
    void toDouble_withNullSource_throwsNPE() {
        // given
        Object nullSource = null;
        // when / then
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toDouble(nullSource, converter));
    }

    /**
     * Parameterised test that validates conversion of various {@link OffsetDateTime}
     * instances to the expected {@code double} representation (epoch seconds + fractional nanos).
     *
     * The expected values are taken from the JSON specification and parsed as {@code double}.
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validConversionProvider")
    void toDouble_validConversions(OffsetDateTime odt, double expected, String description) {
        // when
        // converter not required
        double result = OffsetDateTimeConversions.toDouble(odt, null);
        // then
        // Use a tight tolerance; the conversion logic is deterministic.
        assertEquals(expected, result, 1e-12, () -> "Failed for case: " + description);
    }

    /**
     * Parameterised test that verifies the method throws the expected exception
     * for invalid input values.
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("exceptionScenarioProvider")
    void toDouble_exceptionScenarios(Object from, Class<? extends Throwable> expectedException, String description) {
        // when / then
        assertThrows(expectedException, () -> OffsetDateTimeConversions.toDouble(from, null), () -> "Expected " + expectedException.getSimpleName() + " for case: " + description);
    }
}
