package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.Duration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DurationConversions_toBigDecimal_Test {

    /**
     * Mocked Converter – not used by the method under test but required for the signature.
     */
    @Mock
    private Converter converter;

    /**
     * Mocked Converter – not used by the method under test but required for the signature.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Simple dummy implementation to prove the method does not depend on the Converter instance.
     */
    private static final class DummyConverter implements Converter {
        // No behaviour needed.
    }

    /* --------------------------------------------------------------
     *  Data providers
     * -------------------------------------------------------------- */
    private static Stream<Arguments> validDurationProvider() {
        return Stream.of(// Group: Valid Duration Conversions
        Arguments.of(Duration.ofSeconds(123, 456_000_000), null, new BigDecimal("123.456"), "Positive duration with both seconds and nanoseconds"), Arguments.of(Duration.ofSeconds(0, 0), null, BigDecimal.ZERO, "Zero duration"), Arguments.of(Duration.ofSeconds(-5, -300_000_000), null, new BigDecimal("-5.3"), "Negative duration with nanoseconds"), Arguments.of(Duration.ofSeconds(Long.MAX_VALUE, 0), null, new BigDecimal(Long.toString(Long.MAX_VALUE)), "Maximum long seconds value"), Arguments.of(Duration.ofSeconds(0, 999_999_999), null, new BigDecimal("0.999999999"), "Duration with only nanoseconds at the upper bound"), // Group: Boundary and Sign Normalization
        Arguments.of(Duration.ofSeconds(-10), null, new BigDecimal("-10"), "Negative duration with only seconds"), Arguments.of(Duration.ofSeconds(10, 0), null, new BigDecimal("10"), "Positive duration with seconds and zero nanoseconds"), Arguments.of(Duration.ofSeconds(5, -300_000_000), null, new BigDecimal("4.7"), "Duration where nanoseconds are negative (normalization)"), Arguments.of(Duration.ofSeconds(0, -1), null, new BigDecimal("-0.000000001"), "Only a single negative nanosecond"), Arguments.of(Duration.ofSeconds(Long.MIN_VALUE, 0), null, new BigDecimal(Long.toString(Long.MIN_VALUE)), "Extreme negative seconds value"), Arguments.of(Duration.ofSeconds(0, -999_999_999), null, new BigDecimal("-0.999999999"), "Maximum negative nanoseconds without seconds"), // Group: Converter Argument Ignored
        Arguments.of(Duration.ofSeconds(42, 123_456_789), new DummyConverter(), new BigDecimal("42.123456789"), "Non‑null Converter instance does not affect conversion"));
    }

    private static Stream<Arguments> invalidInputProvider() {
        return Stream.of(// Null `from` argument – should raise NullPointerException due to cast.
        Arguments.of(null, null, NullPointerException.class, "Null `from` argument"), // Non‑Duration object – should raise ClassCastException.
        Arguments.of("not a duration", null, ClassCastException.class, "`from` is not a Duration instance"));
    }

    @Test
    void toBigDecimal_positiveDuration() {
        // given
        // 123.456 seconds
        Duration duration = Duration.ofSeconds(123, 456_000_000);
        // when
        BigDecimal result = DurationConversions.toBigDecimal(duration, converter);
        // then
        BigDecimal expected = new BigDecimal("123.456");
        assertEquals(0, expected.compareTo(result), () -> "Expected " + expected + " but got " + result);
    }

    @Test
    void toBigDecimal_negativeDuration() {
        // given
        // -10.250 seconds
        Duration duration = Duration.ofSeconds(-10, -250_000_000);
        // when
        BigDecimal result = DurationConversions.toBigDecimal(duration, converter);
        // then
        BigDecimal expected = new BigDecimal("-10.25");
        assertEquals(0, expected.compareTo(result), () -> "Expected " + expected + " but got " + result);
    }

    @Test
    void toBigDecimal_subSecondOnly() {
        // given
        // 0.75 seconds
        Duration duration = Duration.ofNanos(750_000_000);
        // when
        BigDecimal result = DurationConversions.toBigDecimal(duration, converter);
        // then
        BigDecimal expected = new BigDecimal("0.75");
        assertEquals(0, expected.compareTo(result), () -> "Expected " + expected + " but got " + result);
    }

    @Test
    void toBigDecimal_zeroDuration() {
        // given
        Duration duration = Duration.ZERO;
        // when
        BigDecimal result = DurationConversions.toBigDecimal(duration, converter);
        // then
        assertEquals(BigDecimal.ZERO, result, "Zero duration should convert to BigDecimal.ZERO");
    }

    /* --------------------------------------------------------------
     *  Parameterized tests for successful conversions
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("validDurationProvider")
    void toBigDecimal_validConversions(Duration from, Converter converter, BigDecimal expected, String description) {
        // when
        BigDecimal result = DurationConversions.toBigDecimal(from, converter);
        // then
        assertEquals(0, expected.compareTo(result), () -> description + " – expected " + expected + " but got " + result);
    }

    /* --------------------------------------------------------------
     *  Parameterized tests for expected exceptions
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("invalidInputProvider")
    void toBigDecimal_invalidInputs(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
        // then
        assertThrows(expectedException, () -> DurationConversions.toBigDecimal(from, converter), () -> description + " – expected exception " + expectedException.getSimpleName());
    }
}
