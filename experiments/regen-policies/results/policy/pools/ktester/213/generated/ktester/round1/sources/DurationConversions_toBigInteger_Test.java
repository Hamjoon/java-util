package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigInteger;
import java.time.Duration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DurationConversions_toBigInteger_Test {

    /**
     * A real {@link Converter} instance used for the conversion calls.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh Converter for each test to avoid state leakage.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up for now.
        converter = null;
    }

    private static Stream<Arguments> positiveDurationsProvider() {
        return Stream.of(Arguments.of("Convert a positive Duration with both seconds and nanoseconds. 10 seconds → 10 000 000 000 ns, plus 500 ns = 10 000 000 500 ns.", Duration.ofSeconds(10, 500), "10000000500"), Arguments.of("Convert a Duration that contains only whole seconds (3 seconds).", Duration.ofSeconds(3), "3000000000"), Arguments.of("Convert a Duration expressed solely in nanoseconds (123 456 789 ns).", Duration.ofNanos(123_456_789), "123456789"));
    }

    private static Stream<Arguments> negativeDurationsProvider() {
        return Stream.of(Arguments.of("Convert a negative Duration with both seconds and nanoseconds. -10 seconds → -10 000 000 000 ns, plus -500 ns = -10 000 000 500 ns.", Duration.ofSeconds(-10, -500), "-10000000500"), Arguments.of("Convert a Duration that contains only whole negative seconds (-3 seconds).", Duration.ofSeconds(-3), "-3000000000"), Arguments.of("Convert a Duration expressed solely in negative nanoseconds.", Duration.ofNanos(-123_456_789), "-123456789"));
    }

    private static Stream<Arguments> zeroDurationProvider() {
        return Stream.of(Arguments.of("Convert a zero Duration; result must be zero nanoseconds.", Duration.ZERO, "0"));
    }

    private static Stream<Arguments> largeDurationsProvider() {
        return Stream.of(Arguments.of("Convert the maximum positive Duration (Long.MAX_VALUE seconds plus the maximum nanosecond adjustment).", Duration.ofSeconds(Long.MAX_VALUE, 999_999_999), "9223372036854775807999999999"), Arguments.of("Convert the minimum (most negative) Duration (Long.MIN_VALUE seconds with the most negative nanosecond adjustment).", Duration.ofSeconds(Long.MIN_VALUE, -999_999_999), "-9223372036854775808999999999"));
    }

    /**
     * Verify that a positive {@link Duration} is correctly converted to a {@link BigInteger}
     * representing the total number of nanoseconds.
     */
    @Test
    void toBigInteger_positiveDuration() {
        // given
        // 12 seconds + 345,678,901 nanos
        Duration duration = Duration.ofSeconds(12, 345_678_901);
        BigInteger expected = BigInteger.valueOf(12L).multiply(BigInteger.valueOf(1_000_000_000L)).add(BigInteger.valueOf(345_678_901L));
        // when
        BigInteger actual = DurationConversions.toBigInteger(duration, converter);
        // then
        assertEquals(expected, actual, "The conversion to BigInteger did not match the expected nanosecond total.");
    }

    /**
     * Verify that a negative {@link Duration} is correctly converted.
     */
    @Test
    void toBigInteger_negativeDuration() {
        // given
        // -5 seconds - 123,456,789 nanos
        Duration duration = Duration.ofSeconds(-5, -123_456_789);
        // Duration normalises the nanosecond part, so the total nanoseconds are:
        // (-5 * 1_000_000_000) + (-123_456_789) = -5_123_456_789
        BigInteger expected = BigInteger.valueOf(-5L).multiply(BigInteger.valueOf(1_000_000_000L)).add(BigInteger.valueOf(-123_456_789L));
        // when
        BigInteger actual = DurationConversions.toBigInteger(duration, converter);
        // then
        assertEquals(expected, actual, "Negative durations should be converted to a negative BigInteger representing nanoseconds.");
    }

    /**
     * Verify that a zero {@link Duration} yields a zero {@link BigInteger}.
     */
    @Test
    void toBigInteger_zeroDuration() {
        // given
        Duration duration = Duration.ZERO;
        BigInteger expected = BigInteger.ZERO;
        // when
        BigInteger actual = DurationConversions.toBigInteger(duration, converter);
        // then
        assertEquals(expected, actual, "Zero duration should convert to BigInteger zero.");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("positiveDurationsProvider")
    void toBigInteger_positiveDurations(String description, Duration from, String expectedStr) {
        // given
        BigInteger expected = new BigInteger(expectedStr);
        // when
        BigInteger actual = DurationConversions.toBigInteger(from, null);
        // then
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("negativeDurationsProvider")
    void toBigInteger_negativeDurations(String description, Duration from, String expectedStr) {
        // given
        BigInteger expected = new BigInteger(expectedStr);
        // when
        BigInteger actual = DurationConversions.toBigInteger(from, null);
        // then
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("largeDurationsProvider")
    void toBigInteger_largeDurations(String description, Duration from, String expectedStr) {
        BigInteger expected = new BigInteger(expectedStr);
        BigInteger actual = DurationConversions.toBigInteger(from, null);
        assertEquals(expected, actual, description);
    }

    @Test
    @DisplayName("Null 'from' argument should throw NullPointerException")
    void toBigInteger_nullInput() {
        // when / then
        assertThrows(NullPointerException.class, () -> DurationConversions.toBigInteger(null, null), "Passing a null 'from' argument should result in a NullPointerException because the method casts it to Duration.");
    }

    @Test
    @DisplayName("Invalid type for 'from' argument should throw ClassCastException")
    void toBigInteger_invalidType() {
        // given
        Object notADuration = "PT10S";
        // when / then
        assertThrows(ClassCastException.class, () -> DurationConversions.toBigInteger(notADuration, null), "Passing an object that is not a Duration (e.g., a String) should cause a ClassCastException.");
    }
}
