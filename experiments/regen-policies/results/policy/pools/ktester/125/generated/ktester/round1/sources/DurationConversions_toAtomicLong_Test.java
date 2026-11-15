package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DurationConversions_toAtomicLong_Test {

    /**
     * Converter instance used for the tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; using the default options.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static converter reference.
        converter = null;
    }

    /**
     * Supplies valid {@link Duration} arguments together with the expected
     * millisecond value.
     */
    private static Stream<Arguments> validDurationProvider() {
        return Stream.of(Arguments.of("Positive duration of 1 second", Duration.ofSeconds(1), 1_000L), Arguments.of("Zero duration", Duration.ZERO, 0L), Arguments.of("Negative duration of -1234 ms", Duration.ofMillis(-1_234), -1_234L), Arguments.of("Maximum positive long value as milliseconds", Duration.ofMillis(Long.MAX_VALUE), Long.MAX_VALUE), Arguments.of("Maximum negative long value as milliseconds", Duration.ofMillis(Long.MIN_VALUE), Long.MIN_VALUE), Arguments.of("Sub‑millisecond duration 999 ns (truncates to 0)", Duration.ofNanos(999), 0L), Arguments.of("Exactly one millisecond expressed as nanos", Duration.ofNanos(1_000_000), 1L), Arguments.of("1.5 ms expressed as nanos (truncates to 1)", Duration.ofNanos(1_500_000), 1L), Arguments.of("1 second + 999,999 ns (truncates to 1000 ms)", Duration.ofSeconds(1).plusNanos(999_999), 1_000L));
    }

    @Test
    void toAtomicLong_convertsDurationToAtomicLong() {
        // given
        // 5 seconds 200 ms = 5200 ms
        Duration duration = Duration.ofSeconds(5).plusMillis(200);
        // when
        AtomicLong result = DurationConversions.toAtomicLong(duration, converter);
        // then
        assertNotNull(result, "Resulting AtomicLong should not be null");
        assertEquals(5_200L, result.get(), "AtomicLong should contain the duration in milliseconds");
    }

    @Test
    void toAtomicLong_zeroDuration_returnsZeroAtomicLong() {
        // given
        Duration zero = Duration.ZERO;
        // when
        AtomicLong result = DurationConversions.toAtomicLong(zero, converter);
        // then
        assertNotNull(result);
        assertEquals(0L, result.get());
    }

    /**
     * Verifies that a variety of {@link Duration} instances are correctly
     * converted to an {@link AtomicLong} containing the millisecond value.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validDurationProvider")
    void toAtomicLong_convertsDurationCorrectly(String description, Duration input, long expectedMillis) {
        // when
        AtomicLong result = DurationConversions.toAtomicLong(input, converter);
        // then
        assertNotNull(result, "Resulting AtomicLong should not be null");
        assertEquals(expectedMillis, result.get(), () -> "AtomicLong should contain the expected milliseconds for: " + description);
    }

    @Test
    void toAtomicLong_nullFrom_throwsNullPointerException() {
        // given
        Object from = null;
        // when / then
        assertThrows(NullPointerException.class, () -> DurationConversions.toAtomicLong(from, converter), "Passing null as 'from' should throw NullPointerException");
    }

    @Test
    void toAtomicLong_nonDurationFrom_throwsClassCastException() {
        // given
        Object from = "not a duration";
        // when / then
        assertThrows(ClassCastException.class, () -> DurationConversions.toAtomicLong(from, converter), "Passing a non‑Duration object should throw ClassCastException");
    }

    @Test
    void toAtomicLong_overflowDuration_throwsArithmeticException() {
        // given: a duration whose millisecond value exceeds Long.MAX_VALUE.
        // The expression mirrors the one from the original test data.
        long days = Long.MAX_VALUE / (24L * 60L * 60L * 1000L) + 1;
        Duration overflow = Duration.ofDays(days);
        // when / then
        assertThrows(ArithmeticException.class, () -> DurationConversions.toAtomicLong(overflow, converter), "Duration that overflows when converting to millis should throw ArithmeticException");
    }

    @Test
    void toAtomicLong_converterIsIgnored_evenIfItThrows() {
        // given a converter that throws if its convert method is ever called
        Converter hostileConverter = new Converter(new ConverterOptions()) {

            @Override
            public Object convert(Object src, Class<?> target) {
                throw new AssertionError("Converter should not be used by toAtomicLong");
            }
        };
        // 2000 ms expected
        Duration duration = Duration.ofSeconds(2);
        // when
        AtomicLong result = DurationConversions.toAtomicLong(duration, hostileConverter);
        // then
        assertNotNull(result);
        assertEquals(2_000L, result.get(), "Method must ignore the supplied Converter and correctly convert the Duration");
    }
}
