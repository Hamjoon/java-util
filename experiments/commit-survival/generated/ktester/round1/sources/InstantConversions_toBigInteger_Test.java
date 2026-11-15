package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import java.time.Instant;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class InstantConversions_toBigInteger_Test {

    /**
     * A Converter instance – the method under test does not use it, but we provide a non‑null value.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a real Converter (default options are sufficient for these tests)
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static resources if needed.
        converter = null;
    }

    /**
     * Supplies the data for {@link #toBigInteger_NormalConversions}.
     */
    private static Stream<Arguments> normalConversionsProvider() {
        return Stream.of(// Zero instant
        arguments(Instant.ofEpochSecond(0, 0), BigInteger.ZERO, "Zero epoch instant should produce a BigInteger value of 0."), // One second, zero nanos
        arguments(Instant.ofEpochSecond(1, 0), BigInteger.valueOf(1_000_000_000L), "One‑second instant with zero nanoseconds should be 1 000 000 000 nanoseconds."), // Zero seconds, positive nanos
        arguments(Instant.ofEpochSecond(0, 123_456_789), BigInteger.valueOf(123_456_789L), "Zero‑second instant with 123 456 789 nanoseconds should return that exact count."), // Negative one second, zero nanos
        arguments(Instant.ofEpochSecond(-1, 0), BigInteger.valueOf(-1_000_000_000L), "Negative one second with zero nanoseconds should yield –1 000 000 000."), // Negative one second plus 500 000 000 nanos (i.e. –0.5 s)
        arguments(Instant.ofEpochSecond(-1, 500_000_000), BigInteger.valueOf(-500_000_000L), "Negative one second plus 500 000 000 nanos results in –500 000 000 nanoseconds."), // Very large positive instant (no overflow because we use BigInteger)
        arguments(Instant.ofEpochSecond(Long.MAX_VALUE, 999_999_999), BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.valueOf(1_000_000_000L)).add(BigInteger.valueOf(999_999_999L)), "Very large positive instant tests that the method correctly uses BigInteger arithmetic."), // Most negative instant
        arguments(Instant.ofEpochSecond(Long.MIN_VALUE, 0), BigInteger.valueOf(Long.MIN_VALUE).multiply(BigInteger.valueOf(1_000_000_000L)), "Extreme negative instant should be converted without overflow."));
    }

    /**
     * Instant at the epoch (0 seconds, 0 nanos) should produce a zero BigInteger.
     */
    @Test
    void toBigInteger_EpochZero() {
        Instant instant = Instant.ofEpochSecond(0, 0);
        BigInteger result = InstantConversions.toBigInteger(instant, converter);
        assertEquals(BigInteger.ZERO, result);
    }

    /**
     * Positive epoch seconds with nanoseconds.
     */
    @Test
    void toBigInteger_PositiveInstant() {
        long seconds = 1_234_567L;
        int nanos = 123_456_789;
        Instant instant = Instant.ofEpochSecond(seconds, nanos);
        BigInteger expected = BigInteger.valueOf(seconds).multiply(BigInteger.valueOf(1_000_000_000L)).add(BigInteger.valueOf(nanos));
        BigInteger result = InstantConversions.toBigInteger(instant, converter);
        assertEquals(expected, result);
    }

    /**
     * Negative epoch seconds with nanoseconds (before the epoch).
     */
    @Test
    void toBigInteger_NegativeInstant() {
        long seconds = -42L;
        // 0.5 seconds positive nanos
        int nanos = 500_000_000;
        // Instant normalises the sign: -42 seconds + 0.5s = -41.5 seconds => -41 seconds and -500_000_000 nanos internally.
        // However, Instant.ofEpochSecond handles the sign correctly, so we use it directly.
        Instant instant = Instant.ofEpochSecond(seconds, nanos);
        // Compute expected using the same algorithm as the method under test.
        long actualSeconds = instant.getEpochSecond();
        int actualNanos = instant.getNano();
        BigInteger expected = BigInteger.valueOf(actualSeconds).multiply(BigInteger.valueOf(1_000_000_000L)).add(BigInteger.valueOf(actualNanos));
        BigInteger result = InstantConversions.toBigInteger(instant, converter);
        assertEquals(expected, result);
    }

    /**
     * Passing a non‑Instant object should result in a ClassCastException.
     */
    @Test
    void toBigInteger_InvalidInput_ThrowsClassCastException() {
        Object notAnInstant = "2023-01-01T00:00:00Z";
        assertThrows(ClassCastException.class, () -> InstantConversions.toBigInteger(notAnInstant, converter));
    }

    /**
     * Various {@link Instant} values converted to nanoseconds.
     *
     * The expected values are calculated using the same algorithm as the
     * implementation: {@code seconds * 1_000_000_000 + nanos}.
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("normalConversionsProvider")
    void toBigInteger_NormalConversions(Instant from, BigInteger expected, String description) {
        // The method does not depend on the converter, so we can safely pass {@code null}
        BigInteger result = InstantConversions.toBigInteger(from, null);
        assertEquals(expected, result, description);
    }

    /**
     * Passing {@code null} as the source object should raise {@link NullPointerException}.
     */
    @Test
    void toBigInteger_NullFrom_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> InstantConversions.toBigInteger(null, null));
    }

    /**
     * Passing an object that is not an {@link Instant} should raise {@link ClassCastException}.
     */
    @Test
    void toBigInteger_InvalidType_ThrowsClassCastException() {
        Object notAnInstant = "2023-01-01T00:00:00Z";
        assertThrows(ClassCastException.class, () -> InstantConversions.toBigInteger(notAnInstant, null));
    }

    /**
     * Verify that a non‑null {@link Converter} does not influence the result.
     */
    @Test
    void toBigInteger_NonNullConverter_Ignored() {
        Instant instant = Instant.ofEpochSecond(2, 3);
        BigInteger expected = BigInteger.valueOf(2_000_000_003L);
        // Use the static converter created in @BeforeAll – any other non‑null instance works as well.
        BigInteger result = InstantConversions.toBigInteger(instant, converter);
        assertEquals(expected, result, "Converter argument is ignored; result must match pure conversion.");
    }
}
