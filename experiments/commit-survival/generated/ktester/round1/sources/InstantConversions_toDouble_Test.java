package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import org.mockito.Mockito;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class InstantConversions_toDouble_Test {

    /**
     * Converter instance used for all tests.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Converter requires a ConverterOptions instance; the default constructor provides sensible defaults.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and ensure no test leaks state.
        converter = null;
    }

    /**
     * Supplies all test cases defined in the JSON specification.
     *
     * @return a stream of {@link Arguments} for the parameterised test
     */
    static Stream<Arguments> provideToDoubleCases() {
        // A mock Converter – the implementation is irrelevant because the method
        // does not use it; we only need a non‑null instance for one test case.
        Converter mockConverter = Mockito.mock(Converter.class);
        return Stream.of(// 1. epoch instant (0,0)
        arguments(Instant.ofEpochSecond(0, 0), null, 0.0, "Convert the epoch instant (0 seconds, 0 nanos) to double; expects 0.0"), // 2. whole‑second instant
        arguments(Instant.ofEpochSecond(1, 0), null, 1.0, "Convert an instant with whole second part only; expects double equal to seconds"), // 3. half‑second fraction
        arguments(Instant.ofEpochSecond(0, 500_000_000), null, 0.5, "Convert an instant with 0 seconds and 500 million nanos (0.5 seconds); expects 0.5"), // 4. negative instant with positive nanos
        arguments(Instant.ofEpochSecond(-1, 250_000_000), null, -0.75, "Convert a negative instant where nanos are positive; -1 + 0.25 = -0.75"), // 5. maximum representable instant
        arguments(Instant.ofEpochSecond(Long.MAX_VALUE, 999_999_999), null, Long.MAX_VALUE + 0.999999999, "Convert the maximum representable instant; verifies handling of large epoch seconds and max nanos"), // 6. null source object → NPE
        arguments(null, null, NullPointerException.class, "Passing null as the source object should throw NPE because of cast to Instant"), // 7. non‑Instant source → ClassCastException
        arguments("2023-01-01T00:00:00Z", null, ClassCastException.class, "Passing an object that is not an Instant should result in ClassCastException"), // 8. minimum representable instant (Long.MIN_VALUE, 0)
        arguments(Instant.ofEpochSecond(Long.MIN_VALUE, 0), null, (double) Long.MIN_VALUE, "Convert the minimum representable instant (Long.MIN_VALUE seconds, 0 nanos) to double; expects the double representation of Long.MIN_VALUE."), // 9. minimum instant with maximum nanos
        arguments(Instant.ofEpochSecond(Long.MIN_VALUE, 999_999_999), null, Long.MIN_VALUE + 0.999999999, "Convert the minimum instant with maximum nanos; result should be Long.MIN_VALUE as double plus 0.999999999 seconds."), // 10. single nanosecond after epoch
        arguments(Instant.ofEpochSecond(0, 1), null, 1.0e-9, "Convert an instant representing a single nanosecond after the epoch; expects 1e‑9."), // 11. maximum sub‑second fraction
        arguments(Instant.ofEpochSecond(0, 999_999_999), null, 0.999999999, "Convert an instant with the maximum sub‑second fraction (just under one second); expects 0.999999999."), // 12. negative whole‑second instant
        arguments(Instant.ofEpochSecond(-1, 0), null, -1.0, "Convert a negative whole‑second instant with zero nanos; expects -1.0."), // 13. one whole second plus half a second
        arguments(Instant.ofEpochSecond(1, 500_000_000), null, 1.5, "Convert an instant with one whole second and half a second in nanos; expects 1.5."), // 14. maximum long seconds, zero nanos
        arguments(Instant.ofEpochSecond(Long.MAX_VALUE, 0), null, (double) Long.MAX_VALUE, "Convert the maximum representable instant (Long.MAX_VALUE seconds, 0 nanos); expects the double representation of Long.MAX_VALUE."), // 15. just before maximum, with max nanos
        arguments(Instant.ofEpochSecond(Long.MAX_VALUE - 1, 999_999_999), null, (Long.MAX_VALUE - 1) + 0.999999999, "Convert the instant just one second before the maximum, with maximum nanos; expects Long.MAX_VALUE‑1 as double plus 0.999999999."), // 16. parsed instant using a non‑null (mock) converter
        arguments(Instant.ofEpochSecond(1672578896, 789_000_000), mockConverter, 1672578896.789, "Convert a parsed instant (2023‑01‑01T12:34:56.789Z) using a non‑null converter; verifies that the converter argument is ignored and the correct double is returned."), // 17. instant created from epoch milliseconds (1234 ms)
        arguments(Instant.ofEpochSecond(1234, 0), null, 1234.0, "Convert an instant created from epoch milliseconds (1234 ms → 1 s 234 ms) expressed as seconds and zero nanos; expects 1234.0."));
    }

    @Test
    void toDouble_zeroInstant_returnsZero() {
        // GIVEN
        Instant instant = Instant.ofEpochSecond(0, 0);
        // WHEN
        double result = InstantConversions.toDouble(instant, converter);
        // THEN
        assertEquals(0.0, result, 0.0, "Zero epoch instant should convert to 0.0");
    }

    @Test
    void toDouble_positiveInstant_returnsCorrectDouble() {
        // GIVEN
        long seconds = 12_345L;
        // 0.678 seconds
        int nanos = 678_000_000;
        Instant instant = Instant.ofEpochSecond(seconds, nanos);
        // 12345.678
        double expected = seconds + nanos / 1_000_000_000.0;
        // WHEN
        double result = InstantConversions.toDouble(instant, converter);
        // THEN
        assertEquals(expected, result, 1e-12, "Positive instant conversion mismatch");
    }

    @Test
    void toDouble_negativeInstant_returnsCorrectDouble() {
        // GIVEN
        // Instant.ofEpochSecond(-10, 500_000_000) represents -9.5 seconds
        long seconds = -10L;
        int nanos = 500_000_000;
        Instant instant = Instant.ofEpochSecond(seconds, nanos);
        // because -10 + 0.5 = -9.5
        double expected = -9.5;
        // WHEN
        double result = InstantConversions.toDouble(instant, converter);
        // THEN
        assertEquals(expected, result, 1e-12, "Negative instant conversion mismatch");
    }

    @Test
    void toDouble_maxLongInstant_returnsExpectedValue() {
        // GIVEN
        long seconds = Long.MAX_VALUE;
        int nanos = 999_999_999;
        Instant instant = Instant.ofEpochSecond(seconds, nanos);
        double expected = seconds + nanos / 1_000_000_000.0;
        // WHEN
        double result = InstantConversions.toDouble(instant, converter);
        // THEN
        assertEquals(expected, result, 1e-6, "Maximum long instant conversion should be accurate within tolerance");
    }

    /**
     * Parameterised test that executes every scenario described in the JSON
     * test case list.  The {@code from} argument is typed as {@code Object}
     * because one case deliberately supplies a {@code String} to provoke a
     * {@link ClassCastException}.
     *
     * @param from        the source object passed to {@code toDouble}
     * @param conv        the {@link Converter} argument (may be {@code null})
     * @param expected    either the expected {@code double} result or the
     *                    {@link Class} of an expected exception
     * @param description textual description used as the display name
     */
    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("provideToDoubleCases")
    void toDouble_various(Object from, Converter conv, Object expected, String description) {
        if (expected instanceof Class) {
            // Expected an exception – verify that it is thrown.
            assertThrows((Class<? extends Throwable>) expected, () -> InstantConversions.toDouble(from, conv), description);
        } else {
            // Expected a numeric result – compare with an appropriate tolerance.
            double expectedVal = ((Number) expected).doubleValue();
            double result = InstantConversions.toDouble(from, conv);
            double tolerance = Math.abs(expectedVal) > 1e15 ? 1e-3 : 1e-12;
            assertEquals(expectedVal, result, tolerance, description);
        }
    }
}
