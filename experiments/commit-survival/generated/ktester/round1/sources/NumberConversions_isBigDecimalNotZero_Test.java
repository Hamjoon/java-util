package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_isBigDecimalNotZero_Test {

    /**
     * The (mocked) Converter instance required by the method signature.
     */
    @Mock
    private Converter mockConverter;

    private static Stream<Arguments> provideBigDecimalValues() {
        return Stream.of(// Zero values
        Arguments.of(BigDecimal.ZERO, false), Arguments.of(new BigDecimal("0.0000"), false), Arguments.of(new BigDecimal("-0.0000"), false), Arguments.of(new BigDecimal("0.0").setScale(10), false), // Positive non‑zero values
        Arguments.of(new BigDecimal("123.456"), true), Arguments.of(new BigDecimal("1E-20"), true), Arguments.of(new BigDecimal("12345678901234567890"), true), Arguments.of(new BigDecimal("0.1").setScale(10), true), // Negative non‑zero values
        Arguments.of(new BigDecimal("-0.001"), true), Arguments.of(new BigDecimal("-1E-20"), true), Arguments.of(new BigDecimal("-12345678901234567890"), true));
    }

    private static Stream<Arguments> provideInvalidTypes() {
        return Stream.of(Arguments.of(Integer.valueOf(5)), Arguments.of("123.45"), Arguments.of(new java.util.Date()));
    }

    @Test
    void isBigDecimalNotZero_returnsFalseWhenZero() {
        // GIVEN
        BigDecimal zero = BigDecimal.ZERO;
        // WHEN
        boolean result = NumberConversions.isBigDecimalNotZero(zero, mockConverter);
        // THEN
        assertFalse(result, "Zero should be reported as not non‑zero");
    }

    @Test
    void isBigDecimalNotZero_returnsTrueWhenPositiveNonZero() {
        // GIVEN
        BigDecimal positive = new BigDecimal("123.45");
        // WHEN
        boolean result = NumberConversions.isBigDecimalNotZero(positive, mockConverter);
        // THEN
        assertTrue(result, "Positive non‑zero value should be reported as non‑zero");
    }

    @Test
    void isBigDecimalNotZero_returnsTrueWhenNegativeNonZero() {
        // GIVEN
        BigDecimal negative = new BigDecimal("-0.001");
        // WHEN
        boolean result = NumberConversions.isBigDecimalNotZero(negative, mockConverter);
        // THEN
        assertTrue(result, "Negative non‑zero value should be reported as non‑zero");
    }

    @Test
    void isBigDecimalNotZero_throwsClassCastExceptionForNonBigDecimal() {
        // GIVEN
        Object notABigDecimal = "not a number";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> NumberConversions.isBigDecimalNotZero(notABigDecimal, mockConverter), "A non‑BigDecimal argument should cause a ClassCastException");
    }

    /**
     * Verifies that zero values (including different scales and the
     * "negative zero" representation) return {@code false}, while any
     * non‑zero value returns {@code true}.
     */
    @ParameterizedTest(name = "{index}: from={0} → expected={1}")
    @MethodSource("provideBigDecimalValues")
    void isBigDecimalNotZero_returnsExpected(Object from, boolean expected) {
        // WHEN
        boolean result = NumberConversions.isBigDecimalNotZero(from, mockConverter);
        // THEN
        assertEquals(expected, result, () -> "Expected isBigDecimalNotZero(" + from + ") to be " + expected);
    }

    /**
     * Verifies that passing a {@code null} reference for {@code from}
     * results in a {@link NullPointerException} (the cast succeeds,
     * but the subsequent {@code compareTo} call dereferences {@code null}).
     */
    @Test
    void isBigDecimalNotZero_throwsNullPointerExceptionWhenFromIsNull() {
        assertThrows(NullPointerException.class, () -> NumberConversions.isBigDecimalNotZero(null, mockConverter), "A null 'from' argument should cause NullPointerException");
    }

    /**
     * Verifies that passing an object that is not a {@link BigDecimal}
     * triggers a {@link ClassCastException}.
     */
    @ParameterizedTest(name = "{index}: from={0} (type={0.getClass().getSimpleName()})")
    @MethodSource("provideInvalidTypes")
    void isBigDecimalNotZero_throwsClassCastExceptionForInvalidType(Object from) {
        assertThrows(ClassCastException.class, () -> NumberConversions.isBigDecimalNotZero(from, mockConverter), "Non‑BigDecimal argument should cause ClassCastException");
    }
}
