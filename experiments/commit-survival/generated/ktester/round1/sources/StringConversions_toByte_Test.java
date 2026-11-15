package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

class StringConversions_toByte_Test {

    private Converter converter;

    /**
     * Simple holder for a test case.
     */
    private static final class TestCase {

        final Object from;

        // null, a real Converter, or a placeholder string
        final Object converter;

        // Byte value or Class<? extends Throwable>
        final Object expected;

        final String description;

        TestCase(Object from, Object converter, Object expected, String description) {
            this.from = from;
            this.converter = converter;
            this.expected = expected;
            this.description = description;
        }

        @Override
        public String toString() {
            return description + " (from=" + from + ", converter=" + converter + ")";
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a mock ConverterOptions – the Converter is not used by toByte,
        // but the constructor requires an instance.
        ConverterOptions mockOptions = org.mockito.Mockito.mock(ConverterOptions.class);
        converter = new Converter(mockOptions);
        // The Converter constructor requires a ConverterOptions instance.
        ConverterOptions mockOptions = mock(ConverterOptions.class);
        converter = new Converter(mockOptions);
    }

    /**
     * Supplies all test cases defined in the JSON document.
     */
    private static Stream<TestCase> testCases() {
        return Stream.of(// Empty string handling
        new TestCase("", null, (byte) 0, "Empty string should return 0"), new TestCase("   ", null, (byte) 0, "Whitespace‑only string should be treated as empty"), // Valid byte parsing (direct Byte.valueOf)
        new TestCase("127", null, (byte) 127, "Maximum positive byte"), new TestCase("-128", null, (byte) -128, "Minimum negative byte"), new TestCase("0", null, (byte) 0, "Zero literal"), // NumberFormatException but convertible via toLong (within range)
        new TestCase("42.0", null, (byte) 42, "Decimal string parsed via toLong"), new TestCase("+15", null, (byte) 15, "Leading plus sign parsed via toLong"), // NumberFormatException and out‑of‑range value (IllegalArgumentException)
        new TestCase("200", null, IllegalArgumentException.class, "Value exceeds Byte.MAX_VALUE"), new TestCase("-200", null, IllegalArgumentException.class, "Value below Byte.MIN_VALUE"), // Non‑numeric input leading to IllegalArgumentException
        new TestCase("abc", null, IllegalArgumentException.class, "Completely non‑numeric string"), // Null input handling
        new TestCase(null, null, NullPointerException.class, "Null 'from' causes NPE before any logic"), // Whitespace handling around numeric values
        new TestCase(" 12 ", null, IllegalArgumentException.class, "Leading/trailing spaces cause failure"), new TestCase("\t-7\n", null, IllegalArgumentException.class, "Tab/newline around number cause failure"), // Non‑String input type
        new TestCase(42, null, ClassCastException.class, "Integer input causes ClassCastException"), new TestCase(true, null, ClassCastException.class, "Boolean input causes ClassCastException"), // Hexadecimal notation
        new TestCase("0x7F", null, IllegalArgumentException.class, "Hex literal not accepted"), new TestCase("0XFF", null, IllegalArgumentException.class, "Upper‑case hex prefix not accepted"), // Scientific notation
        new TestCase("1e2", null, IllegalArgumentException.class, "Scientific notation not accepted"), new TestCase("-5e0", null, IllegalArgumentException.class, "Negative scientific notation not accepted"), // Zero variations
        new TestCase("-0", null, (byte) 0, "\"-0\" parsed as zero"), new TestCase("+0", null, (byte) 0, "\"+0\" parsed as zero"), new TestCase("0.0", null, (byte) 0, "Decimal zero parsed via toLong"), // Leading zeros
        new TestCase("0012", null, (byte) 12, "Leading zeros accepted"), new TestCase("000", null, (byte) 0, "All‑zero string with leading zeros"), // Large numeric string beyond Long range
        new TestCase("123456789012345678901234567890", null, IllegalArgumentException.class, "Number far beyond Long range"), new TestCase("-999999999999999999999999999999", null, IllegalArgumentException.class, "Very large negative number"), // Converter argument non‑null (ignored)
        new TestCase("42", "MockConverter", (byte) 42, "Non‑null converter does not affect parsing"), new TestCase("200", "MockConverter", IllegalArgumentException.class, "Out‑of‑range still throws even with non‑null converter"), // Trailing non‑numeric characters
        new TestCase("123abc", null, IllegalArgumentException.class, "Trailing alphabetic characters cause failure"), new TestCase("45!", null, IllegalArgumentException.class, "Trailing punctuation causes failure"), // Embedded whitespace or separators
        new TestCase("1 2", null, IllegalArgumentException.class, "Internal space between digits"), new TestCase("\t7\n", null, IllegalArgumentException.class, "Tab/newline inside numeric literal"), new TestCase("1_2", null, IllegalArgumentException.class, "Underscore separator not accepted"), // Unsupported numeric prefixes
        new TestCase("0b1010", null, IllegalArgumentException.class, "Binary prefix not recognized"), new TestCase("− 5", null, IllegalArgumentException.class, "Unicode minus sign not accepted"));
    }

    @Test
    void emptyStringReturnsZero() {
        Byte result = StringConversions.toByte("", converter);
        assertEquals((byte) 0, result);
    }

    @Test
    void numericStringWithinByteRange() {
        Byte result = StringConversions.toByte("123", converter);
        assertEquals((byte) 123, result);
    }

    @Test
    void negativeNumericStringWithinByteRange() {
        Byte result = StringConversions.toByte("-5", converter);
        assertEquals((byte) -5, result);
    }

    @Test
    void zeroStringReturnsZero() {
        Byte result = StringConversions.toByte("0", converter);
        assertEquals((byte) 0, result);
    }

    @Test
    void nonNumericStringThrowsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toByte("abc", converter));
        assertTrue(ex.getMessage().contains("not parseable as a byte value"));
    }

    @Test
    void numericStringOutsideByteRangeThrowsIllegalArgumentException() {
        // 128 is just above Byte.MAX_VALUE (127)
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toByte("128", converter));
        assertTrue(ex.getMessage().contains("outside"));
    }

    @Test
    void decimalStringThrowsIllegalArgumentException() {
        // Decimal numbers cannot be parsed directly as a byte
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toByte("12.5", converter));
        assertTrue(ex.getMessage().contains("not parseable as a byte value"));
    }

    /**
     * Executes each {@link TestCase}.  If the expected value is a {@link Byte},
     * the method asserts that the returned value matches.  If the expected value
     * is an exception class, the test asserts that the exception is thrown and
     * that its message contains a hint indicating the cause.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("testCases")
    void toByte_behaviour(TestCase tc) {
        // Resolve the converter argument: if the test case supplies a placeholder
        // string we still pass a real (non‑null) Converter; otherwise we use null.
        Converter conv = (tc.converter instanceof String) ? converter : (Converter) tc.converter;
        if (tc.expected instanceof Byte) {
            Byte result = StringConversions.toByte(tc.from, conv);
            assertEquals(tc.expected, result, "Returned byte does not match expected");
        } else if (tc.expected instanceof Class) {
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) tc.expected;
            Throwable thrown = assertThrows(exClass, () -> StringConversions.toByte(tc.from, conv), "Expected exception " + exClass.getSimpleName());
            // For IllegalArgumentException we also verify that the message mentions the byte range
            if (exClass == IllegalArgumentException.class) {
                assertTrue(thrown.getMessage().contains("byte"), "Exception message should mention 'byte' range");
            }
        } else {
            fail("Test case has unsupported expected type: " + tc.expected);
        }
    }
}
