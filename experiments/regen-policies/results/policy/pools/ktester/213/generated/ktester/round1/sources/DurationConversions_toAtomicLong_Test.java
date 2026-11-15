package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DurationConversions_toAtomicLong_Test {

    /**
     * The Converter instance required by the method signature.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Use default options – the Converter is not actually used by the method,
        // but we provide a real instance to keep the contract intact.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and avoid accidental reuse between tests.
        converter = null;
    }

    private static Stream<Arguments> successfulConversionCases() {
        return Stream.of(Arguments.of(Duration.ofSeconds(5), 5_000L, "Convert a positive Duration of 5 seconds; expect 5,000 ms"), Arguments.of(Duration.ZERO, 0L, "Convert a zero Duration; expect 0 ms"), Arguments.of(Duration.ofSeconds(-3), -3_000L, "Convert a negative Duration of -3 seconds; expect -3,000 ms"), Arguments.of(Duration.ofDays(1), 86_400_000L, "Convert a Duration of 1 day; expect 86,400,000 ms"));
    }

    private static Stream<Arguments> exceptionCases() {
        return Stream.of(// Null input – the cast succeeds (null) but calling toMillis() triggers NPE.
        Arguments.of(null, NullPointerException.class, "Pass a null reference as the 'from' argument; should throw NPE"), // Wrong type – explicit cast to Duration fails.
        Arguments.of("not a duration", ClassCastException.class, "Provide a String instead of Duration; should throw ClassCastException"), // Overflow – Duration.toMillis() overflows Long.MAX_VALUE.
        Arguments.of(Duration.ofSeconds(Long.MAX_VALUE), ArithmeticException.class, "Duration whose millis exceed Long.MAX_VALUE; should throw ArithmeticException"));
    }

    @Test
    void toAtomicLong_convertsDurationToAtomicLong() {
        // GIVEN
        // 123.456 seconds
        Duration duration = Duration.ofSeconds(123, 456_000_000);
        long expectedMillis = duration.toMillis();
        // WHEN
        AtomicLong result = DurationConversions.toAtomicLong(duration, converter);
        // THEN
        assertNotNull(result, "Resulting AtomicLong should not be null");
        assertEquals(expectedMillis, result.get(), "AtomicLong should contain the duration in milliseconds");
    }

    @Test
    void toAtomicLong_nullDuration_throwsNullPointerException() {
        // GIVEN
        Object nullDuration = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> DurationConversions.toAtomicLong(nullDuration, converter), "Passing a null Duration should result in NPE because the method dereferences it");
    }

    @Test
    void toAtomicLong_invalidType_throwsClassCastException() {
        // GIVEN
        Object notADuration = "not a duration";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> DurationConversions.toAtomicLong(notADuration, converter), "Passing an object that is not a Duration should cause a ClassCastException");
    }

    /**
     * Parameterized test covering a variety of {@link Duration} inputs that should
     * be correctly converted to an {@link AtomicLong} containing the duration
     * expressed in milliseconds.
     *
     * The {@code converter} argument is deliberately passed as {@code null}
     * because the implementation does not depend on it.
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("successfulConversionCases")
    void toAtomicLong_successfulConversions(Duration from, long expectedMillis, String description) {
        // WHEN
        AtomicLong result = DurationConversions.toAtomicLong(from, null);
        // THEN
        assertNotNull(result, "Resulting AtomicLong should never be null");
        assertEquals(expectedMillis, result.get(), () -> "AtomicLong should contain the expected milliseconds for: " + description);
    }

    /**
     * Parameterized test that verifies the method throws the expected exception
     * for various invalid inputs.
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("exceptionCases")
    void toAtomicLong_exceptionScenarios(Object from, Class<? extends Throwable> expectedException, String description) {
        // WHEN / THEN
        assertThrows(expectedException, () -> DurationConversions.toAtomicLong(from, null), () -> "Expected " + expectedException.getSimpleName() + " for case: " + description);
    }
}
