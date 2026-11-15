package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class NumberConversions_isBigDecimalNotZero_Test {

    /**
     * The Converter is a required argument for the method under test, but the implementation does not use it.
     *  It is therefore mocked – the mock is injected by MockitoExtension.
     */
    @Mock
    private Converter converter;

    /**
     * The {@code Converter} argument is not used by the implementation, but we still
     * provide a mock to verify that a non‑null value does not affect the outcome.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Supplies all test cases for {@link #testIsBigDecimalNotZero(Object, Converter, Boolean, Class, String)}.
     *
     * @return a stream of {@link org.junit.jupiter.params.provider.Arguments}
     */
    Stream<org.junit.jupiter.params.provider.Arguments> isBigDecimalNotZeroProvider() {
        // A separate mock instance for the cases that require a non‑null Converter.
        Converter nonNullConverter = Mockito.mock(Converter.class);
        return Stream.of(// Zero values – should return false
        arguments(new BigDecimal("0"), null, false, null, "Zero BigDecimal should return false"), arguments(new BigDecimal("0.00"), null, false, null, "Zero represented with scale should return false"), arguments(BigDecimal.ZERO, null, false, null, "Zero constant (BigDecimal.ZERO) should return false"), arguments(new BigDecimal("-0"), null, false, null, "Negative zero representation should be treated as zero and return false"), arguments(new BigDecimal("0.0000"), null, false, null, "Zero with multiple trailing zeros after decimal point should return false"), arguments(new BigDecimal("0E-10"), null, false, null, "Zero expressed in scientific notation should return false"), // Non‑zero values – should return true
        arguments(new BigDecimal("123.45"), null, true, null, "Positive non‑zero BigDecimal should return true"), arguments(new BigDecimal("-987.65"), null, true, null, "Negative non‑zero BigDecimal should return true"), arguments(new BigDecimal("1E-10"), null, true, null, "Very small non‑zero BigDecimal (scientific notation) should return true"), arguments(new BigDecimal("1E+1000"), null, true, null, "Very large positive BigDecimal should return true"), arguments(new BigDecimal("-1E+1000"), null, true, null, "Very large negative BigDecimal should return true"), arguments(new BigDecimal("0.0000000000000000000000001"), null, true, null, "Extremely small non‑zero BigDecimal should return true"), arguments(new BigDecimal("12345678901234567890"), null, true, null, "Large integer value stored in BigDecimal should return true"), arguments(new BigDecimal("5"), nonNullConverter, true, null, "Positive BigDecimal with a non‑null Converter should return true"), arguments(new BigDecimal("0"), nonNullConverter, false, null, "Zero BigDecimal with a non‑null Converter should still return false (converter is ignored)"), // Exceptional cases
        arguments(null, null, null, NullPointerException.class, "Passing null for 'from' should throw NPE because of cast to BigDecimal"), arguments(Integer.valueOf(5), null, null, ClassCastException.class, "Passing a non‑BigDecimal object (Integer) should cause a ClassCastException"), arguments(new Object(), null, null, ClassCastException.class, "Passing an arbitrary Object that is not a BigDecimal should cause a ClassCastException"));
    }

    @Test
    void returnsTrueWhenBigDecimalIsNotZero() {
        // given
        BigDecimal value = new BigDecimal("123.45");
        // when / then
        assertTrue(NumberConversions.isBigDecimalNotZero(value, converter), "Method should return true for a non‑zero BigDecimal");
    }

    @Test
    void returnsFalseWhenBigDecimalIsZero() {
        // given
        BigDecimal zero = BigDecimal.ZERO;
        // when / then
        assertFalse(NumberConversions.isBigDecimalNotZero(zero, converter), "Method should return false for BigDecimal.ZERO");
    }

    @Test
    void throwsClassCastExceptionWhenArgumentIsNotBigDecimal() {
        // given
        Object notABigDecimal = "not a number";
        // when / then
        assertThrows(ClassCastException.class, () -> NumberConversions.isBigDecimalNotZero(notABigDecimal, converter), "Method should throw ClassCastException when the source object is not a BigDecimal");
    }

    @Test
    void throwsNullPointerExceptionWhenArgumentIsNull() {
        // given
        Object nullValue = null;
        // when / then
        assertThrows(NullPointerException.class, () -> NumberConversions.isBigDecimalNotZero(nullValue, converter), "Method should throw NullPointerException when the source object is null");
    }

    /**
     * Parameterized test that executes every case described in the JSON document.
     *
     * @param from               the value passed as the first argument to the method
     * @param converter          the {@code Converter} argument (may be {@code null})
     * @param expectedResult     expected boolean result (ignored when an exception is expected)
     * @param expectedException  expected exception class (or {@code null} if no exception)
     * @param description        textual description of the test case (used as the test name)
     */
    @ParameterizedTest(name = "{index}: {4}")
    @MethodSource("isBigDecimalNotZeroProvider")
    void testIsBigDecimalNotZero(Object from, Converter converter, Boolean expectedResult, Class<? extends Throwable> expectedException, String description) {
        if (expectedException != null) {
            // When an exception is expected we verify that the method throws it.
            assertThrows(expectedException, () -> NumberConversions.isBigDecimalNotZero(from, converter), description);
        } else {
            // Normal boolean outcome.
            boolean actual = NumberConversions.isBigDecimalNotZero(from, converter);
            assertEquals(expectedResult, actual, description);
        }
    }
}
