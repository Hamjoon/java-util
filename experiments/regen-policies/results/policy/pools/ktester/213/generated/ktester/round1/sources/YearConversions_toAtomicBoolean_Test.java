package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.time.Year;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import java.util.Date;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class YearConversions_toAtomicBoolean_Test {

    /**
     * Shared Converter instance used by all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // ConverterOptions has a no‑arg constructor that creates default options.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        converter = null;
    }

    /**
     * Provides all normal (non‑exception) test cases defined in the JSON spec.
     *
     * Each argument consists of:
     * <ul>
     *   <li>source object (may be {@code null})</li>
     *   <li>boolean indicating whether a mock {@link Converter} is required</li>
     *   <li>Integer value that the mock should return (ignored when {@code useMock} is {@code false})</li>
     *   <li>Exception to be thrown by the mock (null when no exception should be thrown)</li>
     *   <li>Expected boolean result (null when an exception is expected)</li>
     *   <li>Human‑readable description (used as assertion message)</li>
     * </ul>
     */
    private static Stream<Arguments> normalCasesProvider() {
        return Stream.of(// Direct numeric input
        Arguments.of(5, false, null, null, true, "Direct numeric input 5 → true"), Arguments.of(0, false, null, null, false, "Direct numeric input 0 → false"), // String input that must be handled by a mock converter
        Arguments.of("yes", true, 1, null, true, "Mocked conversion of \"yes\" → 1 → true"), Arguments.of("no", true, 0, null, false, "Mocked conversion of \"no\" → 0 → false"), // Null input with default converter
        Arguments.of(null, false, null, null, false, "Null input with default converter → false"), // Various numeric wrapper types
        Arguments.of(Integer.valueOf(123), false, null, null, true, "Integer object 123 → true"), Arguments.of(Long.valueOf(0L), false, null, null, false, "Long object 0 → false"), Arguments.of(new BigInteger("-5"), false, null, null, true, "BigInteger -5 → true"), // Year instance
        Arguments.of(Year.of(2022), false, null, null, true, "Year 2022 → true"), // Booleans
        Arguments.of(true, false, null, null, true, "Boolean true → true"), Arguments.of(false, false, null, null, false, "Boolean false → false"), // String numbers parsed by the default converter
        Arguments.of("123", false, null, null, true, "String \"123\" → true"), Arguments.of("0", false, null, null, false, "String \"0\" → false"), // Mocked string conversions with custom mapping
        Arguments.of("custom", true, 0, null, false, "Mocked conversion of \"custom\" → 0 → false"), Arguments.of("custom", true, -10, null, true, "Mocked conversion of \"custom\" → -10 → true"), // Null source but mock returns a non‑zero int
        Arguments.of(null, true, 5, null, true, "Mocked conversion of null → 5 → true"));
    }

    /**
     * Provides all cases that are expected to throw an exception.
     *
     * Arguments:
     * <ul>
     *   <li>source object</li>
     *   <li>boolean indicating whether a mock {@link Converter} is required</li>
     *   <li>Exception that the mock should throw (null when no mock is needed)</li>
     *   <li>Expected exception class</li>
     *   <li>Description for the assertion message</li>
     * </ul>
     */
    private static Stream<Arguments> exceptionCasesProvider() {
        return Stream.of(// Non‑numeric string with default converter → NumberFormatException
        Arguments.of("abc", false, null, NumberFormatException.class, "Non‑numeric string \"abc\" should cause NumberFormatException"), // Mocked converter that deliberately throws a ConversionException
        Arguments.of("bad", true, new ConversionException("forced"), ConversionException.class, "Mocked converter throws ConversionException for \"bad\""), // Unsupported source type (Date) → ConversionException
        Arguments.of(new Date(), false, null, ConversionException.class, "Unsupported java.util.Date source should cause ConversionException"), // Whitespace‑only string → NumberFormatException
        Arguments.of("   ", false, null, NumberFormatException.class, "Whitespace‑only string should cause NumberFormatException"), // Numeric string that overflows int → NumberFormatException
        Arguments.of("9999999999", false, null, NumberFormatException.class, "Overflowing numeric string should cause NumberFormatException"), // Mocked converter that throws NullPointerException
        Arguments.of(new Object(), true, new NullPointerException("mock NPE"), NullPointerException.class, "Mocked converter throws NullPointerException for generic object"), // Generic object with default converter → ConversionException
        Arguments.of(new Object(), false, null, ConversionException.class, "Unsupported generic object should cause ConversionException"));
    }

    @Test
    void nullInputProducesFalse() {
        AtomicBoolean result = YearConversions.toAtomicBoolean(null, converter);
        assertNotNull(result);
        assertFalse(result.get(), "null should be converted to false");
    }

    @Test
    void integerZeroProducesFalse() {
        AtomicBoolean result = YearConversions.toAtomicBoolean(0, converter);
        assertFalse(result.get());
    }

    @Test
    void integerNonZeroProducesTrue() {
        AtomicBoolean result = YearConversions.toAtomicBoolean(42, converter);
        assertTrue(result.get());
    }

    @Test
    void stringZeroProducesFalse() {
        AtomicBoolean result = YearConversions.toAtomicBoolean("0", converter);
        assertFalse(result.get());
    }

    @Test
    void stringNonZeroProducesTrue() {
        AtomicBoolean result = YearConversions.toAtomicBoolean("123", converter);
        assertTrue(result.get());
    }

    @Test
    void booleanTrueProducesTrue() {
        AtomicBoolean result = YearConversions.toAtomicBoolean(true, converter);
        assertTrue(result.get());
    }

    @Test
    void booleanFalseProducesFalse() {
        AtomicBoolean result = YearConversions.toAtomicBoolean(false, converter);
        assertFalse(result.get());
    }

    @Test
    void yearObjectProducesTrue() {
        Year year = Year.of(2023);
        AtomicBoolean result = YearConversions.toAtomicBoolean(year, converter);
        // Year 2023 converts to int 2023, which is non‑zero → true
        assertTrue(result.get());
    }

    @Test
    void mapWithValueZeroProducesFalse() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "0");
        AtomicBoolean result = YearConversions.toAtomicBoolean(map, converter);
        assertFalse(result.get());
    }

    @Test
    void mapWithValueNonZeroProducesTrue() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "7");
        AtomicBoolean result = YearConversions.toAtomicBoolean(map, converter);
        assertTrue(result.get());
    }

    @ParameterizedTest(name = "{5}")
    @MethodSource("normalCasesProvider")
    void testNormalCases(Object from, boolean useMock, Integer mockReturn, RuntimeException mockException, Boolean expected, String description) {
        Converter convToUse;
        if (useMock) {
            // Create a mock Converter and stub its behaviour.
            convToUse = mock(Converter.class);
            if (mockException != null) {
                when(convToUse.convert(any(), eq(Integer.class))).thenThrow(mockException);
            } else {
                // Stub conversion for the specific 'from' value (including null).
                when(convToUse.convert(eq(from), eq(Integer.class))).thenReturn(mockReturn);
                // Fallback stub – any other value returns 0 (won’t be used in these tests).
                when(convToUse.convert(any(), eq(Integer.class))).thenReturn(0);
            }
        } else {
            // default shared converter
            convToUse = converter;
        }
        AtomicBoolean result = YearConversions.toAtomicBoolean(from, convToUse);
        assertNotNull(result, "Result must never be null");
        assertEquals(expected, result.get(), description);
    }

    @ParameterizedTest(name = "{4}")
    @MethodSource("exceptionCasesProvider")
    void testExceptionCases(Object from, boolean useMock, RuntimeException mockException, Class<? extends Throwable> expectedException, String description) {
        Converter convToUse;
        if (useMock) {
            convToUse = mock(Converter.class);
            when(convToUse.convert(any(), eq(Integer.class))).thenThrow(mockException);
        } else {
            convToUse = converter;
        }
        assertThrows(expectedException, () -> YearConversions.toAtomicBoolean(from, convToUse), description);
    }
}
