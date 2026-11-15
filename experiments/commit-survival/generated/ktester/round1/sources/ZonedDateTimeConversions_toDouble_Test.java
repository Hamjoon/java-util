package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toDouble_Test {

    /**
     * A mock {@link Converter}. The implementation of {@code toDouble} does not use it,
     *  but the signature requires a non‑null instance.
     */
    @Mock
    private Converter mockConverter;

    private static Converter converterReturning(double value) {
        return new Converter() {

            @Override
            public double convert(Instant i) {
                return value;
            }
        };
    }

    private static Converter converterFromInstant(java.util.function.Function<Instant, Double> fn) {
        return new Converter() {

            @Override
            public double convert(Instant i) {
                return fn.apply(i);
            }
        };
    }

    private static Converter converterThrowing(RuntimeException ex) {
        return new Converter() {

            @Override
            public double convert(Instant i) {
                throw ex;
            }
        };
    }

    private static Stream<Arguments> validConversions() {
        return Stream.of(Arguments.of("Standard conversion: UTC ZonedDateTime", ZonedDateTime.of(2020, 5, 15, 10, 30, 0, 0, ZoneId.of("UTC")), converterFromInstant(i -> i.getEpochSecond() + i.getNano() / 1_000_000_000.0), 1589521800.0), Arguments.of("Edge case: epoch instant", ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("Z")), converterFromInstant(Instant::getEpochSecond), 0.0), Arguments.of("Zone offset handling (+02:00)", ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneId.of("+02:00")), converterFromInstant(Instant::getEpochSecond), -7200.0));
    }

    private static Stream<Arguments> nullHandling() {
        return Stream.of(Arguments.of("Source object is null", null, converterFromInstant(Instant::getEpochSecond), NullPointerException.class), Arguments.of("Converter is null", ZonedDateTime.of(2022, 3, 10, 12, 0, 0, 0, ZoneOffset.UTC), null, NullPointerException.class));
    }

    private static Stream<Arguments> converterEdgeValues() {
        return Stream.of(Arguments.of("Converter returns NaN", ZonedDateTime.of(2021, 7, 1, 0, 0, 0, 0, ZoneOffset.UTC), converterReturning(Double.NaN), Double.NaN), Arguments.of("Converter returns +Infinity", ZonedDateTime.of(2021, 7, 1, 0, 0, 0, 0, ZoneOffset.UTC), converterReturning(Double.POSITIVE_INFINITY), Double.POSITIVE_INFINITY), Arguments.of("Converter returns -Infinity", ZonedDateTime.of(2021, 7, 1, 0, 0, 0, 0, ZoneOffset.UTC), converterReturning(Double.NEGATIVE_INFINITY), Double.NEGATIVE_INFINITY));
    }

    private static Stream<Arguments> nanosecondPrecision() {
        return Stream.of(Arguments.of("1 ns before epoch", ZonedDateTime.of(1969, 12, 31, 23, 59, 59, 999_999_999, ZoneOffset.UTC), converterFromInstant(i -> i.getEpochSecond() + i.getNano() / 1_000_000_000.0), -0.000000001), Arguments.of("Half‑second after epoch", ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 500_000_000, ZoneOffset.UTC), converterFromInstant(i -> i.getEpochSecond() + i.getNano() / 1_000_000_000.0), 0.5));
    }

    private static Stream<Arguments> converterExceptionPropagation() {
        return Stream.of(Arguments.of("Converter throws IllegalStateException", ZonedDateTime.of(2022, 5, 20, 15, 45, 30, 0, ZoneOffset.UTC), converterThrowing(new IllegalStateException("converter failure")), IllegalStateException.class), Arguments.of("Converter throws NullPointerException", ZonedDateTime.of(1999, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC), converterThrowing(new NullPointerException("converter NPE")), NullPointerException.class));
    }

    @Test
    void toDouble_returnsCorrectEpochDouble() {
        // GIVEN a known ZonedDateTime (UTC) and a mock Converter
        ZonedDateTime zdt = ZonedDateTime.of(// year, month, day
        2023, // year, month, day
        5, // year, month, day
        10, // hour, minute, second
        12, // hour, minute, second
        34, // hour, minute, second
        56, // nanosecond
        123_456_789, // zone
        ZoneOffset.UTC);
        // Expected double representation: epoch seconds + nanosecond fraction
        double expected = zdt.toInstant().getEpochSecond() + zdt.toInstant().getNano() / 1_000_000_000d;
        // WHEN the static conversion method is invoked
        double actual = ZonedDateTimeConversions.toDouble(zdt, mockConverter);
        // THEN the result matches the expected epoch‑based double value
        assertEquals(expected, actual, 1e-12, "ZonedDateTimeConversions.toDouble should return the epoch seconds as a double");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversions")
    void validConversions(String description, ZonedDateTime from, Converter converter, double expected) {
        double actual = ZonedDateTimeConversions.toDouble(from, converter);
        assertEquals(expected, actual, 1e-12, () -> "Unexpected conversion result for case: " + description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullHandling")
    void nullHandling(String description, Object from, Converter converter, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> ZonedDateTimeConversions.toDouble(from, converter), () -> "Expected " + expectedException.getSimpleName() + " for case: " + description);
    }

    @Test
    void invalidSourceType_throwsClassCastException() {
        LocalDateTime ldt = LocalDateTime.of(2022, 3, 10, 12, 0, 0);
        Converter converter = converterFromInstant(Instant::getEpochSecond);
        assertThrows(ClassCastException.class, () -> ZonedDateTimeConversions.toDouble(ldt, converter), "Passing a non‑ZonedDateTime should result in ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("converterEdgeValues")
    void converterEdgeValues(String description, ZonedDateTime from, Converter converter, double expected) {
        double actual = ZonedDateTimeConversions.toDouble(from, converter);
        if (Double.isNaN(expected)) {
            assertTrue(Double.isNaN(actual), "Result should be NaN for case: " + description);
        } else {
            assertEquals(expected, actual, 0.0, () -> "Unexpected edge value for case: " + description);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nanosecondPrecision")
    void nanosecondPrecision(String description, ZonedDateTime from, Converter converter, double expected) {
        double actual = ZonedDateTimeConversions.toDouble(from, converter);
        assertEquals(expected, actual, 1e-12, () -> "Precision mismatch for case: " + description);
    }

    @Test
    void extremeFutureDate() {
        ZonedDateTime zdt = ZonedDateTime.of(3000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        Converter converter = converterFromInstant(Instant::getEpochSecond);
        double actual = ZonedDateTimeConversions.toDouble(zdt, converter);
        assertEquals(32_503_680_000.0, actual, 1e-6, "Future date conversion should produce the correct large epoch second value");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("converterExceptionPropagation")
    void converterExceptionPropagation(String description, ZonedDateTime from, Converter converter, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> ZonedDateTimeConversions.toDouble(from, converter), () -> "Expected exception to propagate for case: " + description);
    }
}
