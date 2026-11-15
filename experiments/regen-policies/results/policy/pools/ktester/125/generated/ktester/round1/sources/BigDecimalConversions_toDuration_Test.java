package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_toDuration_Test {

    @Mock
    private Converter // Not used by the method but required by signature
    mockConverter;

    /**
     * Provides a stream of arguments representing the JSON test cases.
     *
     * Each {@link Arguments} entry contains:
     * <ul>
     *   <li>the {@code from} object (a {@link BigDecimal} or {@code null})</li>
     *   <li>the {@code Converter} to pass (either {@code null}, the {@code mockConverter},
     *       or a custom implementation for the one special case)</li>
     *   <li>the expected result – either an ISO‑8601 duration string, the literal
     *       {@code "PT"} (meaning “any Duration, just don’t throw”), or a fully‑qualified
     *       exception class name</li>
     *   <li>a human‑readable description (used only for the test name)</li>
     * </ul>
     */
    private static Stream<Arguments> toDurationCases() {
        // Helper to create a Converter that simply returns the input unchanged
        Converter identityConverter = new Converter() {

            @Override
            public Object convert(Object o, Class<?> c) {
                return o;
            }
        };
        return Stream.of(// ----- group: toDuration tests -----
        Arguments.of(new BigDecimal("0"), null, "PT0S", "Zero value should produce a zero Duration."), Arguments.of(new BigDecimal("123"), null, "PT123S", "Whole number of seconds without fractional part."), Arguments.of(new BigDecimal("123.456789012"), null, "PT123.456789012S", "Positive value with exactly 9 fractional digits – nanos are derived from those digits."), Arguments.of(new BigDecimal("1.123456789123"), null, "PT1.123456789S", "Positive value with more than 9 fractional digits – extra digits are truncated after moving the decimal point 9 places."), Arguments.of(new BigDecimal("-10"), null, "PT-10S", "Negative whole‑second value."), Arguments.of(new BigDecimal("-10.5"), null, "PT-10.5S", "Negative value with a fractional part – verifies handling of negative remainder and nanos."), Arguments.of(null, null, "java.lang.NullPointerException", "Null `from` argument should cause a NullPointerException when cast to BigDecimal."), Arguments.of("not a BigDecimal", null, "java.lang.ClassCastException", "Passing an object that is not a BigDecimal should result in a ClassCastException."), Arguments.of(BigDecimal.valueOf(Long.MAX_VALUE).add(BigDecimal.ONE), null, "PT", "Value larger than Long.MAX_VALUE – `longValue()` overflows, but the method still returns a Duration; the exact string depends on the overflow semantics."), // ----- group: toDuration additional tests -----
        Arguments.of(new BigDecimal("0.123456"), null, "PT0.123456S", "Positive fractional seconds with fewer than 9 digits; nanos should be scaled correctly."), Arguments.of(new BigDecimal("-0.123456"), null, "PT-0.123456S", "Negative fractional seconds with fewer than 9 digits; verifies handling of negative nanos."), Arguments.of(new BigDecimal("0.999999999"), null, "PT0.999999999S", "Fractional part exactly 9 digits (maximum nanosecond precision)."), Arguments.of(new BigDecimal("0.123456789123"), null, "PT0.123456789S", "More than 9 fractional digits – extra digits are truncated after moving the decimal point 9 places."), Arguments.of(new BigDecimal("1E3"), null, "PT1000S", "Exponent notation should be interpreted correctly (1E3 = 1000 seconds)."), Arguments.of(new BigDecimal("-0.0"), null, "PT0S", "Negative zero should be treated as zero Duration."), Arguments.of(new BigDecimal("0.000000001"), null, "PT0.000000001S", "Exactly one nanosecond; tests minimal non‑zero nanos handling."), Arguments.of(new BigDecimal("-0.000000001"), null, "PT-0.000000001S", "Exactly negative one nanosecond; verifies sign handling for sub‑second nanos."), Arguments.of(BigDecimal.valueOf(Long.MAX_VALUE).add(BigDecimal.ONE), null, "PT-9223372036854775808S", "Value just above Long.MAX_VALUE causes overflow of longValue(); Duration should be created with the overflowed seconds."), Arguments.of(new BigDecimal("123.456"), identityConverter, "PT123.456S", "Non‑null Converter argument is ignored; result must be identical to when converter is null."));
    }

    @Test
    void toDuration_wholeSeconds() {
        // 5 seconds, no fractional part
        BigDecimal input = BigDecimal.valueOf(5);
        Duration result = BigDecimalConversions.toDuration(input, mockConverter);
        assertEquals(Duration.ofSeconds(5, 0), result);
    }

    @Test
    void toDuration_fractionalSecondsExactNineDigits() {
        // 3 seconds + 123,456,789 nanos
        BigDecimal input = new BigDecimal("3.123456789");
        Duration result = BigDecimalConversions.toDuration(input, mockConverter);
        assertEquals(Duration.ofSeconds(3, 123_456_789), result);
    }

    @Test
    void toDuration_fractionalSecondsMoreThanNineDigits_Truncates() {
        // Extra digits after the 9‑th decimal place must be ignored (truncated)
        BigDecimal input = new BigDecimal("2.123456789123");
        Duration result = BigDecimalConversions.toDuration(input, mockConverter);
        // Expected nanos = 123456789 (the 9‑digit part), the rest is dropped
        assertEquals(Duration.ofSeconds(2, 123_456_789), result);
    }

    @Test
    void toDuration_negativeFractionalSeconds() {
        // -1.5 seconds
        BigDecimal input = new BigDecimal("-1.5");
        Duration result = BigDecimalConversions.toDuration(input, mockConverter);
        // The implementation does: seconds = -1, nanos = -0.5 * 1_000_000_000 = -500_000_000
        // Duration normalises this to -2 seconds + 500_000_000 nanos
        Duration expected = Duration.ofSeconds(-2, 500_000_000);
        assertEquals(expected, result);
    }

    @Test
    void toDuration_invalidInput_throwsClassCastException() {
        Object notABigDecimal = "not a number";
        assertThrows(ClassCastException.class, () -> BigDecimalConversions.toDuration(notABigDecimal, mockConverter));
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("toDurationCases")
    void toDuration_parameterised(Object from, Converter converter, String expected, String description) {
        // Use the mockConverter when the test case explicitly passes null for the converter.
        Converter effectiveConverter = (converter == null) ? mockConverter : converter;
        // Determine whether the expected value represents an exception.
        if (expected.startsWith("java.lang.")) {
            // Expected an exception – assert that it is thrown.
            Class<? extends Throwable> exClass;
            try {
                exClass = (Class<? extends Throwable>) Class.forName(expected);
            } catch (ClassNotFoundException e) {
                fail("Exception class not found: " + expected, e);
                return;
            }
            assertThrows(exClass, () -> BigDecimalConversions.toDuration(from, effectiveConverter), "Expected exception " + expected);
            return;
        }
        // Expected a Duration (or just any Duration for the special "PT" case).
        Duration result;
        try {
            result = BigDecimalConversions.toDuration(from, effectiveConverter);
        } catch (Throwable t) {
            fail("Unexpected exception thrown: " + t, t);
            return;
        }
        if ("PT".equals(expected)) {
            // The test only cares that a Duration object is produced without throwing.
            assertNotNull(result, "Resulting Duration should not be null");
            return;
        }
        // Parse the expected ISO‑8601 duration string.
        Duration expectedDuration = Duration.parse(expected);
        assertEquals(expectedDuration, result, () -> "Duration mismatch for input " + from + ". Expected: " + expectedDuration + ", got: " + result);
    }
}
