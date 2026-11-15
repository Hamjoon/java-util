package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toFloat_Test {

    @Mock
    private Converter // not used by the method but required by the signature
    converter;

    @Mock
    private Converter // not used by the method but required by the signature
    mockConverter;

    /**
     * Provides a stream of arguments for {@link #test_toFloat_various(Object, Object, Object)}.
     *
     * Each {@code Arguments} entry contains:
     * <ul>
     *   <li>the {@code from} value passed to {@code toFloat}</li>
     *   <li>the {@code converter} argument (null, a mock, or a simple stub)</li>
     *   <li>the expected outcome – either a {@link Float} instance or an exception class</li>
     * </ul>
     */
    static Stream<Arguments> provideToFloatCases() {
        // a stub implementation of Converter – the method does not use it
        Converter stubConverter = new Converter() {
        };
        return Stream.of(// empty / whitespace strings
        Arguments.of("", null, 0.0f), Arguments.of("   ", null, 0.0f), // normal numbers
        Arguments.of("123.45", null, 123.45f), Arguments.of("-0.001", null, -0.001f), // special literals
        Arguments.of("NaN", null, Float.NaN), Arguments.of("Infinity", null, Float.POSITIVE_INFINITY), Arguments.of("-Infinity", null, Float.NEGATIVE_INFINITY), // malformed / non‑numeric inputs
        Arguments.of("abc", null, IllegalArgumentException.class), Arguments.of("123,45", null, IllegalArgumentException.class), // null reference – the cast yields null, which is treated as empty
        Arguments.of(null, null, 0.0f), // whitespace trimming performed by Float.valueOf
        Arguments.of(" 123.45 ", null, 123.45f), // explicit plus sign
        Arguments.of("+123.45", null, 123.45f), // scientific notation
        Arguments.of("1e3", null, 1000.0f), Arguments.of("1e-3", null, 0.001f), // hexadecimal floating‑point literal
        Arguments.of("0x1.0p2", null, 4.0f), // overflow / underflow
        Arguments.of("1e40", null, Float.POSITIVE_INFINITY), Arguments.of("1e-50", null, 0.0f), // plus‑sign infinity
        Arguments.of("+Infinity", null, Float.POSITIVE_INFINITY), // negative zero
        Arguments.of("-0", null, -0.0f), // leading zeros
        Arguments.of("00012.34", null, 12.34f), // internal whitespace – not parseable
        Arguments.of("12 34", null, IllegalArgumentException.class), // exact Float.MAX_VALUE
        Arguments.of("3.4028235e38", null, 3.4028235e38f), // exact Float.MIN_VALUE (smallest positive)
        Arguments.of("1.4e-45", null, 1.4e-45f), // non‑null converter argument (ignored)
        Arguments.of("123.45", stubConverter, 123.45f), // non‑String objects – should cause ClassCastException on the cast
        Arguments.of(Integer.valueOf(123), null, ClassCastException.class), Arguments.of(Boolean.TRUE, null, ClassCastException.class), Arguments.of(new Object(), null, ClassCastException.class), Arguments.of(new StringBuilder("123.45"), null, ClassCastException.class), // numeric strings with trailing/leading characters
        Arguments.of("123abc", null, IllegalArgumentException.class), Arguments.of("abc123", null, IllegalArgumentException.class), // malformed literals
        Arguments.of("1.2.3", null, IllegalArgumentException.class), Arguments.of("0xG", null, IllegalArgumentException.class), // invalid suffixes
        Arguments.of("NaNxyz", null, IllegalArgumentException.class), Arguments.of("InfinityXYZ", null, IllegalArgumentException.class));
    }

    @Test
    void toFloat_parsesValidFloatString() {
        // given
        Object from = "123.45";
        // when
        Float result = StringConversions.toFloat(from, mockConverter);
        // then
        assertEquals(123.45f, result);
    }

    @Test
    void toFloat_returnsZeroForEmptyString() {
        // given
        Object from = "";
        // when
        Float result = StringConversions.toFloat(from, mockConverter);
        // then
        assertEquals(0.0f, result);
    }

    @Test
    void toFloat_returnsZeroForNull() {
        // given
        Object from = null;
        // when
        Float result = StringConversions.toFloat(from, mockConverter);
        // then
        assertEquals(0.0f, result);
    }

    @Test
    void toFloat_throwsIllegalArgumentExceptionForNonNumericString() {
        // given
        Object from = "not-a-number";
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toFloat(from, mockConverter));
        assertTrue(ex.getMessage().contains("Value 'not-a-number'"));
    }

    /**
     * Executes the matrix of test cases defined in {@link #provideToFloatCases()}.
     *
     * @param from       the value supplied to {@code StringConversions.toFloat}
     * @param converter  the {@link Converter} argument (may be {@code null})
     * @param expected   either a {@link Float} with the expected result or an exception {@link Class}
     */
    @ParameterizedTest(name = "[{index}] from=''{0}'' → expected={2}")
    @MethodSource("provideToFloatCases")
    void test_toFloat_various(Object from, Object converter, Object expected) {
        // -------------------------------------------------------------
        // 1. Expected exception?
        // -------------------------------------------------------------
        if (expected instanceof Class<?>) {
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            Throwable thrown = assertThrows(exClass, () -> StringConversions.toFloat(from, (Converter) converter));
            // For IllegalArgumentException we also verify that the message mentions the value
            if (exClass == IllegalArgumentException.class && from != null) {
                assertTrue(thrown.getMessage().contains(String.valueOf(from)), "Exception message should contain the offending value");
            }
            return;
        }
        // -------------------------------------------------------------
        // 2. Expected normal Float result
        // -------------------------------------------------------------
        Float result = StringConversions.toFloat(from, (Converter) converter);
        Float expectedFloat = (Float) expected;
        // NaN needs special handling – Float.equals returns false for NaN
        if (expectedFloat.isNaN()) {
            assertTrue(result.isNaN(), "Result should be NaN");
            return;
        }
        // Positive/negative infinity – compare using isInfinite and sign
        if (expectedFloat.isInfinite()) {
            assertTrue(result.isInfinite(), "Result should be infinite");
            assertEquals(expectedFloat > 0, result > 0, "Result infinity sign should match expected");
            return;
        }
        // Distinguish -0.0 from +0.0 using raw bits
        if (Float.floatToIntBits(expectedFloat) == Float.floatToIntBits(-0.0f)) {
            assertEquals(Float.floatToIntBits(-0.0f), Float.floatToIntBits(result), "Result should be -0.0");
            return;
        }
        // Regular equality check
        assertEquals(expectedFloat, result, () -> "Expected " + expectedFloat + " but got " + result);
    }
}
