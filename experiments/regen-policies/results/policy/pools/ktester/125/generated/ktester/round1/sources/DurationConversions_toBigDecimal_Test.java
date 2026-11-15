package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DurationConversions_toBigDecimal_Test {

    private static Converter converter;

    /* --------------------------------------------------------------------- */
    private static class DummyConverter extends Converter {

        DummyConverter() {
            super(new ConverterOptions());
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Default converter – the method under test does not actually use it,
        // but a non‑null instance is required for some test scenarios.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static resources if any (none in this case)
        converter = null;
    }

    static Stream<Arguments> positiveDurationsProvider() {
        return Stream.of(// 10 seconds, 0 nanos
        Arguments.of(Duration.ofSeconds(10, 0), null, new BigDecimal("10"), "Convert a simple positive duration of 10 seconds with zero nanoseconds to a BigDecimal representing 10."), // 5.5 seconds
        Arguments.of(Duration.ofSeconds(5, 500_000_000), null, new BigDecimal("5.5"), "Convert a positive duration that includes fractional seconds (5 seconds + 500,000,000 nanoseconds) to a BigDecimal of 5.5."), // 0.25 seconds
        Arguments.of(Duration.ofSeconds(0, 250_000_000), null, new BigDecimal("0.25"), "Convert a duration that is less than one second (250 ms) to a BigDecimal of 0.25."));
    }

    static Stream<Arguments> zeroAndNegativeProvider() {
        return Stream.of(// zero
        Arguments.of(Duration.ZERO, null, BigDecimal.ZERO, "Convert a zero duration to BigDecimal.ZERO."), // -3 seconds
        Arguments.of(Duration.ofSeconds(-3, 0), null, new BigDecimal("-3"), "Convert a negative whole‑second duration to a negative BigDecimal."), // -2.5 seconds (negative nanos)
        Arguments.of(Duration.ofSeconds(-2, -500_000_000), null, new BigDecimal("-2.5"), "Convert a negative duration where the nanosecond part is also negative (equivalent to –2.5 seconds)."), // -0.75 seconds (mixed sign)
        Arguments.of(Duration.ofSeconds(-1, 250_000_000), null, new BigDecimal("-0.75"), "Convert a mixed sign duration: -1 second + 250 ms nanoseconds yields –0.75 seconds."));
    }

    static Stream<Arguments> largeValuesProvider() {
        return Stream.of(// Long.MAX_VALUE seconds + 0.999999999
        Arguments.of(Duration.ofSeconds(Long.MAX_VALUE, 999_999_999), null, new BigDecimal(Long.MAX_VALUE).add(new BigDecimal("0.999999999")), "Convert the maximum long seconds plus the maximum nanosecond fraction; verifies that the method can handle values near the limits without overflow."), // Long.MIN_VALUE seconds
        Arguments.of(Duration.ofSeconds(Long.MIN_VALUE, 0), null, new BigDecimal(Long.MIN_VALUE), "Convert the minimum long seconds (negative extreme) to ensure correct sign handling."));
    }

    static Stream<Arguments> tinyNanosProvider() {
        return Stream.of(// 1 nanosecond
        Arguments.of(Duration.ofSeconds(0, 1), null, new BigDecimal("0.000000001"), "Convert a duration of exactly 1 nanosecond (0 s + 1 ns) to a BigDecimal with nine decimal places."), // 0.999999999 seconds
        Arguments.of(Duration.ofSeconds(0, 999_999_999), null, new BigDecimal("0.999999999"), "Convert a duration of the maximum positive nanosecond fraction (0 s + 999 999 999 ns) to a BigDecimal."), // -1 nanosecond
        Arguments.of(Duration.ofSeconds(0, -1), null, new BigDecimal("-0.000000001"), "Convert a duration of exactly –1 nanosecond (0 s – 1 ns) to a negative BigDecimal with nine decimal places."), // -0.999999999 seconds
        Arguments.of(Duration.ofSeconds(0, -999_999_999), null, new BigDecimal("-0.999999999"), "Convert a duration of the maximum negative nanosecond fraction (0 s – 999 999 999 ns) to a negative BigDecimal."));
    }

    static Stream<Arguments> normalizationProvider() {
        return Stream.of(// -1 sec + 1_000_000_000 nanos => 0 sec
        Arguments.of(Duration.ofSeconds(-1, 1_000_000_000), null, BigDecimal.ZERO, "Duration.ofSeconds normalizes –1 s + 1 000 000 000 ns to 0 s; conversion should yield BigDecimal.ZERO."), // 1 sec - 1_000_000_000 nanos => 0 sec
        Arguments.of(Duration.ofSeconds(1, -1_000_000_000), null, BigDecimal.ZERO, "Duration.ofSeconds normalizes 1 s – 1 000 000 000 ns to 0 s; conversion should yield BigDecimal.ZERO."), // -2 sec + 2_500_000_000 nanos => +0.5 sec
        Arguments.of(Duration.ofSeconds(-2, 2_500_000_000), null, new BigDecimal("0.5"), "Normalization where the positive nanosecond part outweighs the negative seconds, resulting in +0.5 seconds."));
    }

    static Stream<Arguments> alternativeFactoryProvider() {
        return Stream.of(// 2 minutes => 120 seconds
        Arguments.of(Duration.ofMinutes(2), null, new BigDecimal("120"), "Convert a Duration created via ofMinutes (2 min = 120 s) to verify correct handling of alternative factory methods."), // 1 hour => 3600 seconds
        Arguments.of(Duration.ofHours(1), null, new BigDecimal("3600"), "Convert a Duration created via ofHours (1 h = 3600 s) to verify correct handling of larger whole‑second values."), // nanos only => 0.123456789 seconds
        Arguments.of(Duration.ofNanos(123_456_789), null, new BigDecimal("0.123456789"), "Convert a Duration created via ofNanos to ensure nanosecond‑only durations are handled correctly."), // ISO‑8601 parse => 1.5 seconds
        Arguments.of(Duration.parse("PT1.5S"), null, new BigDecimal("1.5"), "Parse an ISO‑8601 duration string representing 1.5 seconds and convert it to a BigDecimal."));
    }

    @Test
    void toBigDecimal_convertsDurationWithSecondsAndNanos() {
        // given
        // 5.25 seconds
        Duration duration = Duration.ofSeconds(5, 250_000_000);
        // when
        BigDecimal result = DurationConversions.toBigDecimal(duration, converter);
        // then
        BigDecimal expected = BigDecimal.valueOf(5.25);
        assertEquals(expected, result, "Duration should be converted to the correct BigDecimal value");
    }

    @Test
    void toBigDecimal_convertsZeroDuration() {
        // given
        Duration zero = Duration.ZERO;
        // when
        BigDecimal result = DurationConversions.toBigDecimal(zero, converter);
        // then
        assertEquals(BigDecimal.ZERO, result, "Zero duration must be converted to BigDecimal zero");
    }

    @Test
    void toBigDecimal_convertsNegativeDuration() {
        // given
        // -3.5 seconds
        Duration negative = Duration.ofSeconds(-3, -500_000_000);
        // when
        BigDecimal result = DurationConversions.toBigDecimal(negative, converter);
        // then
        BigDecimal expected = BigDecimal.valueOf(-3.5);
        assertEquals(expected, result, "Negative duration should be converted correctly");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("positiveDurationsProvider")
    void toBigDecimal_convertsPositiveDurations(Duration from, Converter conv, BigDecimal expected, String description) {
        // when
        BigDecimal result = DurationConversions.toBigDecimal(from, conv);
        // then
        assertEquals(expected, result, description);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("zeroAndNegativeProvider")
    void toBigDecimal_convertsZeroAndNegative(Duration from, Converter conv, BigDecimal expected, String description) {
        BigDecimal result = DurationConversions.toBigDecimal(from, conv);
        assertEquals(expected, result, description);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("largeValuesProvider")
    void toBigDecimal_convertsLargeValues(Duration from, Converter conv, BigDecimal expected, String description) {
        BigDecimal result = DurationConversions.toBigDecimal(from, conv);
        assertEquals(expected, result, description);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("tinyNanosProvider")
    void toBigDecimal_convertsTinyNanos(Duration from, Converter conv, BigDecimal expected, String description) {
        BigDecimal result = DurationConversions.toBigDecimal(from, conv);
        assertEquals(expected, result, description);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("normalizationProvider")
    void toBigDecimal_normalizationCases(Duration from, Converter conv, BigDecimal expected, String description) {
        BigDecimal result = DurationConversions.toBigDecimal(from, conv);
        assertEquals(expected, result, description);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void toBigDecimal_ignoresNonNullConverter() {
        // given
        // 3.25 seconds
        Duration duration = Duration.ofSeconds(3, 250_000_000);
        Converter dummy = new DummyConverter();
        // when
        BigDecimal result = DurationConversions.toBigDecimal(duration, dummy);
        // then
        assertEquals(new BigDecimal("3.25"), result, "Pass a non‑null Converter implementation; the method should ignore it and still return the correct BigDecimal.");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("alternativeFactoryProvider")
    void toBigDecimal_alternativeFactories(Duration from, Converter conv, BigDecimal expected, String description) {
        BigDecimal result = DurationConversions.toBigDecimal(from, conv);
        assertEquals(expected, result, description);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void toBigDecimal_largeSecondsWithoutNanos() {
        Duration duration = Duration.ofSeconds(Long.MAX_VALUE, 0);
        BigDecimal result = DurationConversions.toBigDecimal(duration, null);
        assertEquals(new BigDecimal(Long.MAX_VALUE), result, "Convert the maximum long second value with zero nanoseconds to verify no overflow and correct sign.");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void toBigDecimal_nullFrom_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> DurationConversions.toBigDecimal(null, null), "Passing a null `from` argument should cause a NullPointerException when the cast to Duration is attempted.");
    }

    @Test
    void toBigDecimal_invalidType_throwsClassCastException() {
        assertThrows(ClassCastException.class, () -> DurationConversions.toBigDecimal("not a duration", null), "Passing an object that is not a Duration should result in a ClassCastException.");
    }
}
