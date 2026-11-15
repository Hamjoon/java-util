package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DurationConversions_toLong_Test {

    @Mock
    private Converter // unused by the method under test
    mockConverter;

    /**
     * A dummy {@link Converter} that throws if its {@code convert} method is called.
     *  Used to prove that {@code DurationConversions.toLong} never touches the converter.
     */
    private static final Converter THROWING_CONVERTER = new Converter() {

        @Override
        public Object convert(Object src, Class<?> target) {
            throw new RuntimeException("Converter should not be called");
        }
    };

    private static Stream<Arguments> provideSuccessfulConversions() {
        return Stream.of(// basic cases
        Arguments.of("Positive Duration of 5 seconds → 5000 ms", Duration.ofSeconds(5), null, 5_000L), Arguments.of("Zero Duration → 0 ms", Duration.ZERO, null, 0L), Arguments.of("Negative Duration of -2 seconds → -2000 ms", Duration.ofSeconds(-2), null, -2_000L), Arguments.of("Exact 123‑ms Duration with a non‑null Converter (ignored)", Duration.ofMillis(123), new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                // never called
                return null;
            }
        }, 123L), // boundary / truncation cases
        Arguments.of("Maximum positive Duration that fits in a long (Long.MAX_VALUE ms)", Duration.ofMillis(Long.MAX_VALUE), null, Long.MAX_VALUE), Arguments.of("Duration of Long.MAX_VALUE nanoseconds → floor(Long.MAX_VALUE/1_000_000) ms", Duration.ofNanos(Long.MAX_VALUE), null, Long.MAX_VALUE / 1_000_000L), Arguments.of("999 ns (< 1 ms) truncates to 0 ms", Duration.ofNanos(999), null, 0L), Arguments.of("1_500_000 ns = 1.5 ms truncates toward zero → 1 ms", Duration.ofNanos(1_500_000), null, 1L), Arguments.of("One day → 86 400 000 ms", Duration.ofDays(1), null, 86_400_000L), Arguments.of("Negative one hour → -3 600 000 ms", Duration.ofHours(-1), null, -3_600_000L), Arguments.of("Largest whole‑second Duration fitting in long → 9 223 372 036 854 775 000 ms", Duration.ofSeconds(Long.MAX_VALUE / 1_000), null, 9_223_372_036_854_775_000L), Arguments.of("Exact one‑millisecond Duration with a throwing Converter (should be ignored)", Duration.ofNanos(1_000_000), THROWING_CONVERTER, 1L), // using the @Mock instance to prove it stays untouched
        Arguments.of("Positive Duration with mocked Converter (should be ignored)", // 5 250 ms
        Duration.ofSeconds(5).plusMillis(250), mockConverter, 5_250L));
    }

    private static Stream<Arguments> provideExceptionalConversions() {
        // Helper: a Duration subclass whose toMillis() deliberately throws.
        Duration throwingDuration = new Duration() {

            @Override
            public long toMillis() {
                throw new IllegalStateException("custom");
            }

            // The remaining abstract methods are given trivial implementations.
            @Override
            public long getSeconds() {
                return 0;
            }

            @Override
            public int getNano() {
                return 0;
            }

            @Override
            public Duration plus(Duration duration) {
                return this;
            }

            @Override
            public Duration minus(Duration duration) {
                return this;
            }

            @Override
            public Duration multipliedBy(long multiplier) {
                return this;
            }

            @Override
            public Duration dividedBy(long divisor) {
                return this;
            }

            @Override
            public Duration negated() {
                return this;
            }

            @Override
            public Duration abs() {
                return this;
            }

            @Override
            public boolean isZero() {
                return false;
            }

            @Override
            public boolean isNegative() {
                return false;
            }

            @Override
            public boolean isPositive() {
                return false;
            }
        };
        return Stream.of(// overflow cases – Duration.toMillis() throws ArithmeticException
        Arguments.of("Duration.ofSeconds(Long.MAX_VALUE/1000 + 2) overflows to millis", Duration.ofSeconds(Long.MAX_VALUE / 1_000 + 2), null, ArithmeticException.class), Arguments.of("Duration.ofSeconds(Long.MAX_VALUE/1000 + 1) overflows to millis (additional group)", Duration.ofSeconds(Long.MAX_VALUE / 1_000 + 1), null, ArithmeticException.class), // null source object
        Arguments.of("Null 'from' argument → NullPointerException", null, null, NullPointerException.class), // non‑Duration source
        Arguments.of("String source → ClassCastException", "not a duration", null, ClassCastException.class), // Duration subclass that throws inside toMillis()
        Arguments.of("Custom Duration whose toMillis() throws IllegalStateException", throwingDuration, null, IllegalStateException.class));
    }

    @Test
    void toLong_returnsMilliseconds_forPositiveDuration() {
        // given
        // 5,250 ms
        Duration duration = Duration.ofSeconds(5).plusMillis(250);
        // when
        long result = DurationConversions.toLong(duration, mockConverter);
        // then
        assertEquals(5_250L, result, "The conversion should return the total milliseconds");
    }

    @Test
    void toLong_returnsZero_forZeroDuration() {
        // given
        Duration zero = Duration.ZERO;
        // when
        long result = DurationConversions.toLong(zero, mockConverter);
        // then
        assertEquals(0L, result);
    }

    @Test
    void toLong_returnsNegativeMilliseconds_forNegativeDuration() {
        // given
        // -3,400 ms
        Duration negative = Duration.ofSeconds(-3).minusMillis(400);
        // when
        long result = DurationConversions.toLong(negative, mockConverter);
        // then
        assertEquals(-3_400L, result);
    }

    @Test
    void toLong_throwsClassCastException_whenObjectIsNotDuration() {
        // given
        Object notADuration = "not a duration";
        // when / then
        assertThrows(ClassCastException.class, () -> DurationConversions.toLong(notADuration, mockConverter));
    }

    @Test
    void toLong_throwsNullPointerException_whenFromIsNull() {
        // given
        Object nullObj = null;
        // when / then
        assertThrows(NullPointerException.class, () -> DurationConversions.toLong(nullObj, mockConverter));
    }

    /* -------------------------------------------------------------
     *  Successful conversions – the method returns the expected long.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideSuccessfulConversions")
    void toLong_returnsExpectedMilliseconds(String description, Duration from, Converter converter, long expected) {
        // when
        long result = DurationConversions.toLong(from, converter);
        // then
        assertEquals(expected, result, description);
        // verify that the converter (if it is the mock) was never used
        if (converter == mockConverter) {
            Mockito.verifyNoInteractions(mockConverter);
        }
    }

    /* -------------------------------------------------------------
     *  Exceptional conversions – the method should propagate the exception.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideExceptionalConversions")
    void toLong_propagatesExpectedException(String description, Object from, Converter converter, Class<? extends Throwable> expectedException) {
        // when / then
        assertThrows(expectedException, () -> DurationConversions.toLong(from, converter), description);
    }
}
