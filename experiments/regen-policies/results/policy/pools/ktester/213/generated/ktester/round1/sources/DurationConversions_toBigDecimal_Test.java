package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DurationConversions_toBigDecimal_Test {

    // shared for all tests
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter has a public constructor that requires ConverterOptions.
        // An empty options instance is sufficient for the built‑in conversions.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up; let GC handle the static reference.
        converter = null;
    }

    /**
     * Helper that builds the expected {@link BigDecimal} from a {@link Duration}
     * using the same arithmetic that {@code BigDecimalConversions.secondsAndNanosToDouble}
     * would employ (seconds + nanos / 1_000_000_000).
     */
    private static BigDecimal expectedBigDecimal(Duration d) {
        long seconds = d.getSeconds();
        int nanos = d.getNano();
        // nanos are scaled to 9 decimal places (i.e., 1_000_000_000 nanos = 1 second)
        BigDecimal secPart = BigDecimal.valueOf(seconds);
        // move decimal point left 9 places
        BigDecimal nanoPart = BigDecimal.valueOf(nanos, 9);
        return secPart.add(nanoPart);
    }

    /**
     * Parameter provider for all *valid* conversion scenarios.
     *
     * <p>Each {@link Arguments} contains:
     * <ul>
     *   <li>a {@link Duration} instance to convert,</li>
     *   <li>a {@link Converter} (may be {@code null}, the shared instance, or a Mockito mock),</li>
     *   <li>the expected {@link BigDecimal} result (computed via {@link #expectedBigDecimal(Duration)}).</li>
     * </ul>
     */
    private static Stream<Arguments> validConversionProvider() {
        // A Mockito mock – the method must ignore it.
        Converter mockConverter = mock(Converter.class);
        // A fresh real Converter instance to prove that the argument is truly ignored.
        Converter freshConverter = new Converter(new ConverterOptions());
        return Stream.of(// ---- Valid conversions (original JSON group) ----
        Arguments.of(Duration.ofSeconds(0, 0), null, expectedBigDecimal(Duration.ofSeconds(0, 0))), Arguments.of(Duration.ofSeconds(10, 0), null, expectedBigDecimal(Duration.ofSeconds(10, 0))), Arguments.of(Duration.ofSeconds(10, 500_000_000), null, expectedBigDecimal(Duration.ofSeconds(10, 500_000_000))), Arguments.of(Duration.ofSeconds(-10, 0), null, expectedBigDecimal(Duration.ofSeconds(-10, 0))), Arguments.of(Duration.ofSeconds(-10, 500_000_000), null, expectedBigDecimal(Duration.ofSeconds(-10, 500_000_000))), Arguments.of(Duration.ofSeconds(Long.MAX_VALUE, 0), null, expectedBigDecimal(Duration.ofSeconds(Long.MAX_VALUE, 0))), // ---- Additional valid conversions (original JSON group) ----
        Arguments.of(Duration.ofSeconds(0, 1), null, expectedBigDecimal(Duration.ofSeconds(0, 1))), Arguments.of(Duration.ofSeconds(0, 999_999_999), null, expectedBigDecimal(Duration.ofSeconds(0, 999_999_999))), Arguments.of(Duration.ofSeconds(1, 999_999_999), null, expectedBigDecimal(Duration.ofSeconds(1, 999_999_999))), Arguments.of(Duration.ofSeconds(-1, -500_000_000), null, expectedBigDecimal(Duration.ofSeconds(-1, -500_000_000))), Arguments.of(Duration.ofSeconds(Long.MIN_VALUE, 0), null, expectedBigDecimal(Duration.ofSeconds(Long.MIN_VALUE, 0))), Arguments.of(Duration.ofSeconds(Long.MAX_VALUE, 999_999_999), null, expectedBigDecimal(Duration.ofSeconds(Long.MAX_VALUE, 999_999_999))), // Converter instance is ignored – we use a Mockito mock here.
        Arguments.of(Duration.ofSeconds(12345, 123_456_789), mockConverter, expectedBigDecimal(Duration.ofSeconds(12345, 123_456_789))), // Explicit fresh real Converter instance (again ignored)
        Arguments.of(Duration.ofSeconds(12345, 123_456_789), freshConverter, expectedBigDecimal(Duration.ofSeconds(12345, 123_456_789))));
    }

    /**
     * Provider for the exception‑related test cases.
     *
     * <p>Each argument supplies the {@code from} object, a {@code Converter}
     * (always {@code null} for these cases), and the expected exception type.
     */
    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(Arguments.of(null, null, NullPointerException.class), Arguments.of("not a duration", null, ClassCastException.class), Arguments.of(Instant.now(), null, ClassCastException.class), Arguments.of(new Object(), null, ClassCastException.class));
    }

    @Test
    void toBigDecimal_positiveDuration() {
        // GIVEN
        // 5.123456789 seconds
        Duration duration = Duration.ofSeconds(5, 123_456_789);
        // WHEN
        BigDecimal result = DurationConversions.toBigDecimal(duration, converter);
        // THEN
        BigDecimal expected = expectedBigDecimal(duration);
        assertEquals(0, expected.compareTo(result), () -> "Expected " + expected + " but got " + result);
    }

    @Test
    void toBigDecimal_zeroDuration() {
        // GIVEN
        Duration duration = Duration.ZERO;
        // WHEN
        BigDecimal result = DurationConversions.toBigDecimal(duration, converter);
        // THEN
        BigDecimal expected = expectedBigDecimal(duration);
        assertEquals(0, expected.compareTo(result), () -> "Expected zero BigDecimal but got " + result);
    }

    @Test
    void toBigDecimal_negativeDuration() {
        // GIVEN
        // -3.250000000 seconds
        Duration duration = Duration.ofSeconds(-3, -250_000_000);
        // WHEN
        BigDecimal result = DurationConversions.toBigDecimal(duration, converter);
        // THEN
        BigDecimal expected = expectedBigDecimal(duration);
        assertEquals(0, expected.compareTo(result), () -> "Expected " + expected + " but got " + result);
    }

    @Test
    void toBigDecimal_nullInput_throwsNPE() {
        // GIVEN
        Duration duration = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> DurationConversions.toBigDecimal(duration, converter), "Passing null should result in NullPointerException because the method casts to Duration");
    }

    @ParameterizedTest(name = "[{index}] duration={0}, converter={1}")
    @MethodSource("validConversionProvider")
    void toBigDecimal_validConversions(Duration duration, Converter conv, BigDecimal expected) {
        // WHEN
        BigDecimal result = DurationConversions.toBigDecimal(duration, conv);
        // THEN
        assertEquals(0, expected.compareTo(result), () -> "Expected " + expected + " but got " + result);
    }

    @ParameterizedTest(name = "[{index}] from={0} → {2}")
    @MethodSource("exceptionProvider")
    void toBigDecimal_exceptionScenarios(Object from, Converter conv, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> DurationConversions.toBigDecimal(from, conv), () -> "Expected " + expectedException.getSimpleName() + " when converting " + from);
    }
}
