package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.math.BigInteger;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class OffsetTimeConversions_toBigInteger_Test {

    /**
     * Shared Converter instance used by all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter needs a ConverterOptions instance – the default options are sufficient for this test.
        converter = new Converter(new ConverterOptions());
    }

    /**
     * Supplies all valid conversion cases.  {@code null} may be used for the
     * {@link Converter} argument because the implementation never touches it.
     */
    static Stream<Arguments> validConversionProvider() {
        // a normal (non‑mock) converter – used only to prove that the method works
        // even when a real Converter instance is supplied.
        Converter realConverter = new Converter(new ConverterOptions());
        // a mock that throws on any method call – the conversion must succeed because
        // the method never invokes the converter.
        Converter throwingConverter = Mockito.mock(Converter.class, invocation -> {
            throw new RuntimeException("Converter should not be called");
        });
        return Stream.of(// 1. midnight UTC → 0
        arguments(OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC), null, "0"), // 2. 12:34:56.123456789+02:00 → 38096 123 456 789
        arguments(OffsetTime.of(12, 34, 56, 123_456_789, ZoneOffset.ofHours(2)), null, "38096123456789"), // 3. 23:59:59.999999999‑05:00 → 104 399 999 999 999
        arguments(OffsetTime.of(23, 59, 59, 999_999_999, ZoneOffset.ofHours(-5)), null, "104399999999999"), // 4. 06:30+01:30 with a dummy converter (the JSON expected value is 23400000000)
        arguments(OffsetTime.of(6, 30, 0, 0, ZoneOffset.ofHoursMinutes(1, 30)), realConverter, "23400000000"), // 5. Negative epoch seconds: 00:00+01:00 → -3 600 000 000 000
        arguments(OffsetTime.of(0, 0, 0, 0, ZoneOffset.ofHours(1)), null, "-3600000000000"), // 6. Maximum positive offset (+18:00)
        arguments(OffsetTime.of(23, 59, 59, 0, ZoneOffset.MAX), null, "21599000000000"), // 7. Maximum negative offset (‑18:00)
        arguments(OffsetTime.of(0, 0, 0, 0, ZoneOffset.MIN), null, "64800000000000"), // 8. Small non‑zero nanoseconds: 12:00:00.000000001 UTC
        arguments(OffsetTime.of(12, 0, 0, 1, ZoneOffset.UTC), null, "43200000000001"), // 9. Converter that throws – conversion must still succeed
        arguments(OffsetTime.of(1, 2, 3, 4, ZoneOffset.UTC), throwingConverter, "3723000000004"), // 10. Offset with hours and minutes (+05:30)
        arguments(OffsetTime.of(10, 15, 30, 500_000_000, ZoneOffset.ofHoursMinutes(5, 30)), null, "17130500000000"));
    }

    /**
     * Supplies the invalid test cases.
     */
    static Stream<Arguments> invalidConversionProvider() {
        return Stream.of(// null source → NullPointerException
        arguments(null, null, NullPointerException.class), // non‑OffsetTime source (String) → ClassCastException
        arguments("12:00", null, ClassCastException.class));
    }

    /**
     * Verifies that {@code OffsetTimeConversions.toBigInteger} correctly converts an {@link OffsetTime}
     * into a {@link BigInteger} representing the number of nanoseconds since the Unix epoch.
     */
    @Test
    void testToBigInteger_PositiveOffset() {
        // GIVEN: an OffsetTime with a positive UTC offset
        OffsetTime offsetTime = OffsetTime.of(12, 34, 56, 789_123_456, ZoneOffset.ofHours(2));
        // WHEN: converting to BigInteger via the static method
        BigInteger actual = OffsetTimeConversions.toBigInteger(offsetTime, converter);
        // THEN: the result must match the calculation performed with the public API
        // epoch day = 1970‑01‑01
        Instant instant = offsetTime.atDate(LocalDate.ofEpochDay(0)).toInstant();
        BigInteger expected = BigInteger.valueOf(instant.getEpochSecond()).multiply(BigInteger.valueOf(1_000_000_000L)).add(BigInteger.valueOf(instant.getNano()));
        assertEquals(expected, actual);
    }

    /**
     * Verifies conversion when the {@link OffsetTime} has a negative UTC offset.
     */
    @Test
    void testToBigInteger_NegativeOffset() {
        // GIVEN: an OffsetTime with a negative UTC offset
        OffsetTime offsetTime = OffsetTime.of(23, 45, 1, 123_456_789, ZoneOffset.ofHours(-5));
        // WHEN
        BigInteger actual = OffsetTimeConversions.toBigInteger(offsetTime, converter);
        // THEN
        Instant instant = offsetTime.atDate(LocalDate.ofEpochDay(0)).toInstant();
        BigInteger expected = BigInteger.valueOf(instant.getEpochSecond()).multiply(BigInteger.valueOf(1_000_000_000L)).add(BigInteger.valueOf(instant.getNano()));
        assertEquals(expected, actual);
    }

    /**
     * Verifies that a {@code null} source results in a {@link NullPointerException}
     * because the method casts the argument to {@link OffsetTime} without a null‑check.
     */
    @Test
    void testToBigInteger_NullSource() {
        // GIVEN a null source
        Object nullSource = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> OffsetTimeConversions.toBigInteger(nullSource, converter));
    }

    /**
     * Parameterised test covering all *valid* conversion scenarios from the JSON
     * specification.  The expected value is both computed via the public API
     * (to guarantee correctness) and cross‑checked against the literal string
     * supplied in the original test data.
     */
    @ParameterizedTest(name = "[{index}] {0} with converter={1}")
    @MethodSource("validConversionProvider")
    void testToBigInteger_ValidConversions(OffsetTime offsetTime, Converter suppliedConverter, String expectedString) {
        // WHEN
        BigInteger actual = OffsetTimeConversions.toBigInteger(offsetTime, suppliedConverter);
        // THEN – compute the expected value using the same algorithm the method uses
        Instant instant = offsetTime.atDate(LocalDate.ofEpochDay(0)).toInstant();
        BigInteger expected = BigInteger.valueOf(instant.getEpochSecond()).multiply(BigIntegerConversions.BILLION).add(BigInteger.valueOf(instant.getNano()));
        assertEquals(expected, actual, "Computed value does not match algorithmic expectation");
        // Also verify that the literal string from the JSON matches (if provided)
        if (expectedString != null) {
            assertEquals(new BigInteger(expectedString), actual, "Literal expected string does not match the actual result");
        }
    }

    /**
     * Parameterised test covering all *invalid* input scenarios from the JSON
     * specification.
     */
    @ParameterizedTest(name = "[{index}] from={0} converter={1} → {2}")
    @MethodSource("invalidConversionProvider")
    void testToBigInteger_InvalidInputs(Object from, Converter converter, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> OffsetTimeConversions.toBigInteger(from, converter), "Expected exception was not thrown");
    }
}
