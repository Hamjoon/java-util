package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigInteger;
import java.time.Year;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class YearConversions_toBigInteger_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    /**
     * Real converter used for the majority of cases (default behaviour).
     */
    private static Converter defaultConverter;

    /**
     * Simple holder for a single test case.
     */
    private static final class TestCase {

        final Object from;

        // may be null
        final Converter converter;

        // BigInteger or exception class
        final Object expected;

        final String description;

        TestCase(Object from, Converter converter, Object expected, String description) {
            this.from = from;
            this.converter = converter;
            this.expected = expected;
            this.description = description;
        }

        @Override
        public String toString() {
            // JUnit displays the description in the test name.
            return description;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter requires a ConverterOptions instance; the default options are sufficient for the tests.
        converter = new Converter(new ConverterOptions());
        defaultConverter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // No external resources to clean up, but we null‑out the reference for completeness.
        converter = null;
        defaultConverter = null;
    }

    /**
     * Helper to create a mock Converter that returns a fixed int value.
     */
    private static Converter mockReturningInt(int value) {
        Converter mock = Mockito.mock(Converter.class);
        // The real Converter API is `convert(Object, Class<T>)`.  We stub any call
        // that asks for Integer (or int) to return the given value.
        Mockito.when(mock.convert(Mockito.any(), Mockito.eq(Integer.class))).thenReturn(value);
        Mockito.when(mock.convert(Mockito.any(), Mockito.eq(int.class))).thenReturn(value);
        return mock;
    }

    /**
     * Helper to create a mock Converter that parses a String to int.
     */
    private static Converter mockStringToNumber() {
        Converter mock = Mockito.mock(Converter.class);
        Mockito.when(mock.convert(Mockito.any(), Mockito.eq(Integer.class))).thenAnswer(invocation -> {
            Object arg = invocation.getArgument(0);
            if (arg instanceof String) {
                return Integer.parseInt(((String) arg).trim());
            }
            throw new IllegalArgumentException("Unsupported input for StringToNumberConverter");
        });
        Mockito.when(mock.convert(Mockito.any(), Mockito.eq(int.class))).thenAnswer(invocation -> {
            Object arg = invocation.getArgument(0);
            if (arg instanceof String) {
                return Integer.parseInt(((String) arg).trim());
            }
            throw new IllegalArgumentException("Unsupported input for StringToNumberConverter");
        });
        return mock;
    }

    /**
     * Helper to create a mock Converter that converts a BigDecimal to int (truncates).
     */
    private static Converter mockBigDecimalToInt() {
        Converter mock = Mockito.mock(Converter.class);
        Mockito.when(mock.convert(Mockito.any(), Mockito.eq(Integer.class))).thenAnswer(invocation -> {
            Object arg = invocation.getArgument(0);
            if (arg instanceof BigDecimal) {
                // truncates fractional part
                return ((BigDecimal) arg).intValue();
            }
            throw new IllegalArgumentException("Unsupported input for BigDecimalToIntConverter");
        });
        Mockito.when(mock.convert(Mockito.any(), Mockito.eq(int.class))).thenAnswer(invocation -> {
            Object arg = invocation.getArgument(0);
            if (arg instanceof BigDecimal) {
                return ((BigDecimal) arg).intValue();
            }
            throw new IllegalArgumentException("Unsupported input for BigDecimalToIntConverter");
        });
        return mock;
    }

    /**
     * Helper to create a mock Converter that always returns 999.
     */
    private static Converter mockConstantInt() {
        return mockReturningInt(999);
    }

    /**
     * All normal conversion cases (including edge cases).
     */
    static Stream<Arguments> conversionCases() {
        List<TestCase> cases = new ArrayList<>();
        // --- conversion tests -------------------------------------------------
        cases.add(new TestCase(null, defaultConverter, BigInteger.ZERO, "null source should be treated as 0"));
        cases.add(new TestCase(123, defaultConverter, BigInteger.valueOf(123), "Integer input should be wrapped"));
        cases.add(new TestCase(123L, defaultConverter, BigInteger.valueOf(123), "Long within int range should be down‑cast"));
        cases.add(new TestCase("456", mockStringToNumber(), BigInteger.valueOf(456), "String numeric parsed by StringToNumberConverter"));
        cases.add(new TestCase(Year.of(2023), defaultConverter, BigInteger.valueOf(2023), "Year instance converted to its int value"));
        cases.add(new TestCase(new Object(), defaultConverter, IllegalArgumentException.class, "Unsupported object type should cause IllegalArgumentException"));
        cases.add(new TestCase(new BigDecimal("789.01"), mockBigDecimalToInt(), BigInteger.valueOf(789), "BigDecimal truncated by custom converter"));
        // --- edge cases -------------------------------------------------------
        cases.add(new TestCase(Short.valueOf((short) 12), defaultConverter, BigInteger.valueOf(12), "Short value"));
        cases.add(new TestCase(Byte.valueOf((byte) -8), defaultConverter, BigInteger.valueOf(-8), "Byte (negative) value"));
        cases.add(new TestCase(Float.valueOf(123.45f), defaultConverter, BigInteger.valueOf(123), "Float truncated"));
        cases.add(new TestCase(Double.valueOf(-987.65), defaultConverter, BigInteger.valueOf(-987), "Double truncated"));
        cases.add(new TestCase(new AtomicInteger(555), defaultConverter, BigInteger.valueOf(555), "AtomicInteger"));
        cases.add(new TestCase(new AtomicLong(123456L), defaultConverter, BigInteger.valueOf(123456), "AtomicLong within int range"));
        cases.add(new TestCase(new AtomicBoolean(true), defaultConverter, BigInteger.valueOf(1), "AtomicBoolean true -> 1"));
        cases.add(new TestCase(new AtomicBoolean(false), defaultConverter, BigInteger.valueOf(0), "AtomicBoolean false -> 0"));
        cases.add(new TestCase(Map.of(MapConversions.YEAR, 1999), defaultConverter, BigInteger.valueOf(1999), "Map containing YEAR key"));
        cases.add(new TestCase(Integer.MAX_VALUE, defaultConverter, BigInteger.valueOf(Integer.MAX_VALUE), "Maximum int value"));
        cases.add(new TestCase(Integer.MIN_VALUE, defaultConverter, BigInteger.valueOf(Integer.MIN_VALUE), "Minimum int value"));
        cases.add(new TestCase(Year.of(-44), defaultConverter, BigInteger.valueOf(-44), "Negative Year"));
        cases.add(new TestCase("   42   ", mockStringToNumber(), BigInteger.valueOf(42), "String with surrounding whitespace"));
        cases.add(new TestCase("any", mockConstantInt(), BigInteger.valueOf(999), "ConstantIntConverter always returns 999"));
        return cases.stream().map(tc -> Arguments.of(tc));
    }

    /**
     * Cases that are expected to throw an exception.
     */
    static Stream<Arguments> exceptionCases() {
        List<TestCase> cases = new ArrayList<>();
        cases.add(new TestCase(Long.MAX_VALUE, defaultConverter, IllegalArgumentException.class, "Long exceeding int range should throw IllegalArgumentException"));
        cases.add(new TestCase("not-a-number", mockStringToNumber(), NumberFormatException.class, "Unparseable string should cause NumberFormatException"));
        cases.add(new TestCase(Map.of("foo", 123), defaultConverter, IllegalArgumentException.class, "Map without YEAR key should throw IllegalArgumentException"));
        cases.add(new TestCase(new BigInteger("12345678901234567890"), defaultConverter, IllegalArgumentException.class, "BigInteger outside int range should throw IllegalArgumentException"));
        cases.add(new TestCase(Double.NaN, defaultConverter, IllegalArgumentException.class, "NaN double should throw IllegalArgumentException"));
        cases.add(new TestCase(Boolean.TRUE, defaultConverter, IllegalArgumentException.class, "Plain Boolean not supported should throw IllegalArgumentException"));
        cases.add(new TestCase(null, null, NullPointerException.class, "Null Converter should cause NullPointerException"));
        return cases.stream().map(tc -> Arguments.of(tc));
    }

    @Test
    void toBigInteger_fromYear_returnsCorrectValue() {
        Year year = Year.of(2023);
        BigInteger result = YearConversions.toBigInteger(year, converter);
        assertEquals(BigInteger.valueOf(2023), result);
    }

    @Test
    void toBigInteger_fromInteger_returnsCorrectValue() {
        Integer source = 1999;
        BigInteger result = YearConversions.toBigInteger(source, converter);
        assertEquals(BigInteger.valueOf(1999), result);
    }

    @Test
    void toBigInteger_fromString_returnsCorrectValue() {
        String source = "2005";
        BigInteger result = YearConversions.toBigInteger(source, converter);
        assertEquals(BigInteger.valueOf(2005), result);
    }

    @Test
    void toBigInteger_fromNull_returnsZero() {
        Object source = null;
        BigInteger result = YearConversions.toBigInteger(source, converter);
        assertEquals(BigInteger.ZERO, result);
    }

    @Test
    void toBigInteger_fromMapWithYearKey_returnsCorrectValue() {
        // The Converter treats a map with a "year" entry as a Year source.
        Map<String, Object> map = Map.of("year", 2010);
        BigInteger result = YearConversions.toBigInteger(map, converter);
        assertEquals(BigInteger.valueOf(2010), result);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("conversionCases")
    void testToBigInteger_conversions(TestCase tc) {
        if (tc.expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) tc.expected)) {
            // Expected an exception – delegate to the exception test method.
            assertThrows((Class<? extends Throwable>) tc.expected, () -> YearConversions.toBigInteger(tc.from, tc.converter), tc.description);
        } else {
            // Normal successful conversion.
            BigInteger result = YearConversions.toBigInteger(tc.from, tc.converter);
            assertEquals(tc.expected, result, tc.description);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("exceptionCases")
    void testToBigInteger_exceptions(TestCase tc) {
        assertThrows((Class<? extends Throwable>) tc.expected, () -> YearConversions.toBigInteger(tc.from, tc.converter), tc.description);
    }
}
