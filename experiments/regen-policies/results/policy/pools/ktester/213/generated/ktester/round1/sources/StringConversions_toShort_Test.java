package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toShort_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach(@Mock ConverterOptions mockOptions) {
        // The Converter constructor expects a ConverterOptions instance.
        // We mock it and make sure it returns an empty map for overrides.
        MockitoExtension.initMocks(this);
        //noinspection unchecked
        MockitoExtension.getMock(ConverterOptions.class).when(mockOptions.getConverterOverrides()).thenReturn(Collections.emptyMap());
        this.converter = new Converter(mockOptions);
    }

    @AfterEach
    void teardownAfterEach() {
        // Release the reference – not strictly required but keeps the test clean.
        this.converter = null;
    }

    /**
     * Supplies all test cases from the JSON document.
     *
     * @return a stream of arguments: {@code (from, expectedResult)}
     */
    private static Stream<Arguments> provideToShortCases() {
        return Stream.of(// Empty Input
        Arguments.of("", (short) 0), // Whitespace Only
        Arguments.of("   ", (short) 0), // Valid Short Parsing
        Arguments.of("123", (short) 123), Arguments.of(String.valueOf(Short.MAX_VALUE), Short.MAX_VALUE), Arguments.of(String.valueOf(Short.MIN_VALUE), Short.MIN_VALUE), // Negative Zero
        Arguments.of("-0", (short) 0), // Whitespace Handling (fallback to toLong)
        Arguments.of("  456  ", (short) 456), // Leading Plus Sign
        Arguments.of("+123", (short) 123), // String With Leading and Trailing Tabs
        Arguments.of("\t\t78\t", (short) 78), // Out of Range Parsing (exception from toLong)
        Arguments.of("32768", IllegalArgumentException.class), Arguments.of("-32769", IllegalArgumentException.class), // Non‑numeric Parsing (exception)
        Arguments.of("abc", IllegalArgumentException.class), Arguments.of("12.34", IllegalArgumentException.class), // Hexadecimal Notation
        Arguments.of("0x7F", IllegalArgumentException.class), Arguments.of("0X7F", IllegalArgumentException.class), // Number With Underscores (Java 8+)
        Arguments.of("1_234", IllegalArgumentException.class), // Scientific Notation
        Arguments.of("1e2", IllegalArgumentException.class), // Unicode Full‑Width Digits
        Arguments.of("\uFF11\uFF12\uFF13", IllegalArgumentException.class), // Embedded Whitespace
        Arguments.of("12 34", IllegalArgumentException.class), // Sign Only
        Arguments.of("+", IllegalArgumentException.class), Arguments.of("-", IllegalArgumentException.class), // Trailing Non‑Numeric Characters
        Arguments.of("123abc", IllegalArgumentException.class), // Binary Notation Not Supported
        Arguments.of("0b1010", IllegalArgumentException.class), // Boundary Overflow with Large Number
        Arguments.of("999999999999999999999", IllegalArgumentException.class), // Negative Large Number Overflow
        Arguments.of("-999999999999999999", IllegalArgumentException.class), // Null Input
        Arguments.of(null, NullPointerException.class), // String Object Not Actually a String (different type)
        Arguments.of(new StringBuilder("123"), ClassCastException.class), Arguments.of(new StringBuilder("-45"), ClassCastException.class), // Invalid CharSequence Type (ClassCastException)
        Arguments.of(new StringBuffer("123"), ClassCastException.class), // Array Input (ClassCastException)
        Arguments.of(new String[] { "123" }, ClassCastException.class));
    }

    @Test
    void toShort_ValidShortString_ReturnsShort() {
        Short result = StringConversions.toShort("123", converter);
        assertEquals((short) 123, result);
    }

    @Test
    void toShort_EmptyString_ReturnsZero() {
        Short result = StringConversions.toShort("", converter);
        assertEquals((short) 0, result);
    }

    @Test
    void toShort_NonNumericString_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toShort("abc", converter));
    }

    @Test
    void toShort_NumberOutsideShortRange_ThrowsIllegalArgumentException() {
        // 40000 is outside the range of a short (-32768 to 32767)
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toShort("40000", converter));
    }

    @Test
    void toShort_NumberExactlyAtUpperBound_ReturnsShortMaxValue() {
        Short result = StringConversions.toShort(String.valueOf(Short.MAX_VALUE), converter);
        assertEquals(Short.MAX_VALUE, result);
    }

    @Test
    void toShort_NumberExactlyAtLowerBound_ReturnsShortMinValue() {
        Short result = StringConversions.toShort(String.valueOf(Short.MIN_VALUE), converter);
        assertEquals(Short.MIN_VALUE, result);
    }

    /**
     * Parameterised test that runs through every scenario described in the
     * supplied JSON.  The second argument is either the expected {@link Short}
     * value or the {@link Class} of the exception that should be thrown.
     */
    @ParameterizedTest(name = "{index}: from={0} => expected={1}")
    @MethodSource("provideToShortCases")
    void toShort_VariousInputs(Object from, Object expected) {
        // The method does not actually use the Converter argument for the
        // current test cases, so we pass {@code null} as required by the spec.
        if (expected instanceof Class) {
            // Expected an exception
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, () -> StringConversions.toShort(from, null));
        } else {
            // Expected a successful conversion
            Short result = StringConversions.toShort(from, null);
            assertEquals(expected, result);
        }
    }
}
