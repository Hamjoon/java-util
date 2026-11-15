package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_isBigDecimalNotZero_Test {

    /**
     * Shared {@link Converter} instance used by all test cases.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter constructor requires a ConverterOptions instance.
        // Using the default options is sufficient for these tests.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        converter = null;
    }

    /**
     * Provides a stream of arguments representing the test cases described in the
     * JSON payload.  Each argument consists of:
     * <ul>
     *   <li>the {@code from} object to pass to the method</li>
     *   <li>the expected result (Boolean) – null when an exception is expected</li>
     *   <li>the expected exception type – null when a normal result is expected</li>
     * </ul>
     */
    private static Stream<Arguments> bigDecimalNotZeroProvider() {
        return Stream.of(// Zero BigDecimal
        Arguments.of(new BigDecimal("0"), false, null), // Positive non‑zero
        Arguments.of(new BigDecimal("123.45"), true, null), // Negative non‑zero
        Arguments.of(new BigDecimal("-0.001"), true, null), // null input – should throw NullPointerException when cast
        Arguments.of(null, null, NullPointerException.class), // Wrong type – ClassCastException
        Arguments.of(Integer.valueOf(5), null, ClassCastException.class), // Zero with trailing scale
        Arguments.of(new BigDecimal("0.0000"), false, null), // Negative zero (scale > 0) – still equals ZERO
        Arguments.of(new BigDecimal("-0"), false, null), // Very small positive (scientific notation)
        Arguments.of(new BigDecimal("1E-10"), true, null), // Very small negative (scientific notation)
        Arguments.of(new BigDecimal("-1E-10"), true, null), // Large magnitude number
        Arguments.of(new BigDecimal("12345678901234567890"), true, null), // Zero with explicit scale via setScale
        Arguments.of(new BigDecimal("0.0").setScale(5), false, null), // Zero after stripping trailing zeros
        Arguments.of(new BigDecimal("0.00").stripTrailingZeros(), false, null), // Result of arithmetic that yields zero (add)
        Arguments.of(new BigDecimal("0").add(new BigDecimal("0")), false, null), // Multiplying zero by a non‑zero number
        Arguments.of(new BigDecimal("0").multiply(new BigDecimal("5")), false, null), // Division that results in zero
        Arguments.of(new BigDecimal("0").divide(new BigDecimal("1")), false, null));
    }

    @Test
    void returnsFalseWhenValueIsZero() {
        BigDecimal zero = BigDecimal.ZERO;
        assertFalse(NumberConversions.isBigDecimalNotZero(zero, converter), "Zero should be reported as not non‑zero");
    }

    @Test
    void returnsTrueWhenValueIsPositiveNonZero() {
        BigDecimal positive = new BigDecimal("123.45");
        assertTrue(NumberConversions.isBigDecimalNotZero(positive, converter), "Positive non‑zero value should be reported as non‑zero");
    }

    @Test
    void returnsTrueWhenValueIsNegativeNonZero() {
        BigDecimal negative = new BigDecimal("-0.001");
        assertTrue(NumberConversions.isBigDecimalNotZero(negative, converter), "Negative non‑zero value should be reported as non‑zero");
    }

    @Test
    void throwsClassCastExceptionWhenObjectIsNotBigDecimal() {
        Object notABigDecimal = "123";
        assertThrows(ClassCastException.class, () -> NumberConversions.isBigDecimalNotZero(notABigDecimal, converter), "Passing a non‑BigDecimal should result in ClassCastException");
    }

    @ParameterizedTest(name = "{index}: from={0}, expected={1}, exception={2}")
    @MethodSource("bigDecimalNotZeroProvider")
    void testIsBigDecimalNotZero(Object from, Boolean expectedResult, Class<? extends Throwable> expectedException) {
        if (expectedException != null) {
            // When an exception is expected, ensure it is thrown.
            assertThrows(expectedException, () -> NumberConversions.isBigDecimalNotZero(from, null), () -> "Expected exception " + expectedException.getSimpleName() + " for input: " + from);
        } else {
            // Normal boolean result expected.
            boolean actual = NumberConversions.isBigDecimalNotZero(from, null);
            assertEquals(expectedResult, actual, () -> "Unexpected result for input: " + from);
        }
    }
}
