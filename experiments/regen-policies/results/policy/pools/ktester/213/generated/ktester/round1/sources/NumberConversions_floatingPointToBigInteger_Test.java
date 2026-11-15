package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

class NumberConversions_floatingPointToBigInteger_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // ConverterOptions has a no‑arg constructor that creates default options.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test.
        converter = null;
    }

    /**
     * Parses the expected value from the JSON description.
     */
    private static Object parseExpected(String expected) {
        if (expected.endsWith("Exception")) {
            // Return the exception class name – the test method will turn it into a Class.
            return expected;
        }
        // numeric expected value – treat as a decimal string for BigInteger construction
        return new BigInteger(expected);
    }

    /**
     * Returns the {@link Class} object for a given exception name.
     */
    private static Class<? extends Throwable> exceptionClassFor(String name) {
        switch(name) {
            case "NumberFormatException":
                return NumberFormatException.class;
            case "NullPointerException":
                return NullPointerException.class;
            case "IllegalArgumentException":
                return IllegalArgumentException.class;
            case "RuntimeException":
                return RuntimeException.class;
            default:
                throw new IllegalArgumentException("Unsupported exception: " + name);
        }
    }

    static Stream<Arguments> positiveNumbersProvider() {
        return Stream.of(Arguments.of("Positive fractional double should be floored (Math.floor) to 3", 3.7, "3"), Arguments.of("Positive whole number double should remain unchanged (floor of 5.0 is 5)", 5.0, "5"), Arguments.of("Large positive fractional double is floored to the nearest lower integer", 123456789.999, "123456789"));
    }

    static Stream<Arguments> zeroValueProvider() {
        return Stream.of(Arguments.of("Zero is not greater than 0, so the ternary uses Math.ceil(0) which yields 0", 0.0, "0"));
    }

    static Stream<Arguments> negativeNumbersProvider() {
        return Stream.of(Arguments.of("Negative fractional double should be ceiled (Math.ceil) to -2", -2.3, "-2"), Arguments.of("Negative whole number double remains unchanged (ceil of -4.0 is -4)", -4.0, "-4"), Arguments.of("Negative value whose ceil is 0 should result in BigInteger 0", -0.0001, "0"));
    }

    static Stream<Arguments> specialFloatingProvider() {
        return Stream.of(Arguments.of("NaN formatted with %.0f yields \"NaN\", which cannot be parsed by BigInteger constructor", Double.NaN, "NumberFormatException"), Arguments.of("Positive infinity formatted with %.0f yields \"Infinity\", leading to NumberFormatException", Double.POSITIVE_INFINITY, "NumberFormatException"), Arguments.of("Negative infinity formatted with %.0f yields \"-Infinity\", causing NumberFormatException", Double.NEGATIVE_INFINITY, "NumberFormatException"));
    }

    static Stream<Arguments> differentNumericProvider() {
        return Stream.of(Arguments.of("BigDecimal is converted to double (7.9) then floored to 7", new BigDecimal("7.9"), "7"), Arguments.of("BigInteger is converted to double (-15.0) then ceiled (still -15)", new BigInteger("-15"), "-15"), Arguments.of("Float wrapper is unboxed to double 2.6, floored to 2", Float.valueOf(2.6f), "2"));
    }

    static Stream<Arguments> stringInputsProvider() {
        return Stream.of(Arguments.of("String representing a positive decimal should be parsed and floored", "123.45", "123"), Arguments.of("String representing a negative decimal should be parsed and ceiled", "-78.9", "-78"), Arguments.of("String \"0.0\" parses to double 0.0; ceil yields 0", "0.0", "0"), Arguments.of("Scientific notation string should be parsed to double 1000.0", "1e3", "1000"), Arguments.of("String \"NaN\" parses to Double.NaN; formatting yields \"NaN\" -> exception", "NaN", "NumberFormatException"));
    }

    static Stream<Arguments> integerWrapperProvider() {
        return Stream.of(Arguments.of("Integer wrapper is converted to double 10.0, floor keeps 10", Integer.valueOf(10), "10"), Arguments.of("Long wrapper is converted to double -20.0, ceil keeps -20", Long.valueOf(-20L), "-20"), Arguments.of("Short wrapper is converted to double 5.0, floor keeps 5", Short.valueOf((short) 5), "5"), Arguments.of("Byte wrapper is converted to double -3.0, ceil keeps -3", Byte.valueOf((byte) -3), "-3"));
    }

    static Stream<Arguments> atomicNumericProvider() {
        return Stream.of(Arguments.of("AtomicInteger value 7 is converted to double 7.0, floor yields 7", AtomicInteger.valueOf(7), "7"), Arguments.of("AtomicLong value -9 is converted to double -9.0, ceil yields -9", AtomicLong.valueOf(-9L), "-9"));
    }

    static Stream<Arguments> boundaryProvider() {
        return Stream.of(Arguments.of("The smallest positive double (≈4.9e‑324) floors to 0", Double.MIN_VALUE, "0"), Arguments.of("Negative zero is not greater than 0, so Math.ceil(-0.0) returns 0", -0.0, "0"), Arguments.of("Positive value less than 1 floors to 0", 0.5, "0"), Arguments.of("Negative value greater than -1 ceils to 0", -0.5, "0"), Arguments.of("Value just below 2 floors to 1 due to floating‑point precision", 1.9999999999999998, "1"), Arguments.of("Value just below -1 ceils to -1", -1.0000000000000002, "-1"));
    }

    static Stream<Arguments> nullAndInvalidProvider() {
        return Stream.of(Arguments.of("Passing null as the source object should result in a NullPointerException from toDouble", null, "NullPointerException"), Arguments.of("An object that cannot be converted to a numeric value causes NumberFormatException", new Object(), "NumberFormatException"), Arguments.of("Empty string cannot be parsed to a double, leading to NumberFormatException", "", "NumberFormatException"));
    }

    static Stream<Arguments> nonNumericStringProvider() {
        return Stream.of(Arguments.of("A string that cannot be parsed as a number should cause NumberFormatException", "abc", "NumberFormatException"), Arguments.of("A whitespace‑only string is not a valid numeric representation", "   ", "NumberFormatException"));
    }

    static Stream<Arguments> floatSpecialProvider() {
        return Stream.of(Arguments.of("Float.NaN is converted to double NaN; formatting yields \"NaN\" -> exception", Float.NaN, "NumberFormatException"), Arguments.of("Float.POSITIVE_INFINITY becomes double Infinity; \"Infinity\" cannot be parsed", Float.POSITIVE_INFINITY, "NumberFormatException"), Arguments.of("Float.NEGATIVE_INFINITY becomes double -Infinity; \"-Infinity\" cannot be parsed", Float.NEGATIVE_INFINITY, "NumberFormatException"));
    }

    static Stream<Arguments> overflowProvider() {
        return Stream.of(Arguments.of("BigDecimal larger than Double.MAX_VALUE converts to double Infinity", new BigDecimal("1e309"), "NumberFormatException"), Arguments.of("Very large BigInteger overflows to double Infinity", new BigInteger("1" + "0".repeat(400)), "NumberFormatException"));
    }

    static Stream<Arguments> infinityStringProvider() {
        return Stream.of(Arguments.of("String \"Infinity\" parses to Double.POSITIVE_INFINITY; formatting yields \"Infinity\"", "Infinity", "NumberFormatException"), Arguments.of("String \"-Infinity\" parses to Double.NEGATIVE_INFINITY; formatting yields \"-Infinity\"", "-Infinity", "NumberFormatException"));
    }

    @Test
    void positiveDoubleIsFloored() {
        Double source = 123.456;
        BigInteger result = NumberConversions.floatingPointToBigInteger(source, converter);
        assertEquals(BigInteger.valueOf(123), result);
    }

    @Test
    void negativeDoubleIsCeiled() {
        Double source = -123.456;
        BigInteger result = NumberConversions.floatingPointToBigInteger(source, converter);
        assertEquals(BigInteger.valueOf(-123), result);
    }

    @Test
    void integerIsConvertedCorrectly() {
        Integer source = 42;
        BigInteger result = NumberConversions.floatingPointToBigInteger(source, converter);
        assertEquals(BigInteger.valueOf(42), result);
    }

    @Test
    void stringNumberIsConvertedAndFloored() {
        String source = "99.9";
        BigInteger result = NumberConversions.floatingPointToBigInteger(source, converter);
        assertEquals(BigInteger.valueOf(99), result);
    }

    @Test
    void bigDecimalIsConvertedAndFloored() {
        BigDecimal source = new BigDecimal("12345.678");
        BigInteger result = NumberConversions.floatingPointToBigInteger(source, converter);
        assertEquals(BigInteger.valueOf(12345), result);
    }

    @Test
    void bigIntegerIsReturnedUnchanged() {
        BigInteger source = new BigInteger("9876543210");
        BigInteger result = NumberConversions.floatingPointToBigInteger(source, converter);
        // The conversion goes through toDouble, which may lose precision,
        // but the expected behaviour of the method is to floor the double value.
        // For this magnitude the double representation is exact.
        assertEquals(source, result);
    }

    @Test
    void nullInputYieldsZero() {
        Object source = null;
        BigInteger result = NumberConversions.floatingPointToBigInteger(source, converter);
        assertEquals(BigInteger.ZERO, result);
    }

    @Test
    void negativeZeroIsHandledAsZero() {
        Double source = -0.0;
        BigInteger result = NumberConversions.floatingPointToBigInteger(source, converter);
        assertEquals(BigInteger.ZERO, result);
    }

    /* -------------------------------------------------------------
     *  Positive numbers
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("positiveNumbersProvider")
    void positiveNumbers(String description, Object from, String expected) {
        Object exp = parseExpected(expected);
        if (exp instanceof String) {
            Class<? extends Throwable> ex = exceptionClassFor((String) exp);
            assertThrows(ex, () -> NumberConversions.floatingPointToBigInteger(from, converter));
        } else {
            BigInteger expectedBI = (BigInteger) exp;
            BigInteger result = NumberConversions.floatingPointToBigInteger(from, converter);
            assertEquals(expectedBI, result, description);
        }
    }

    /* -------------------------------------------------------------
     *  Zero value
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("zeroValueProvider")
    void zeroValue(String description, Object from, String expected) {
        Object exp = parseExpected(expected);
        if (exp instanceof String) {
            Class<? extends Throwable> ex = exceptionClassFor((String) exp);
            assertThrows(ex, () -> NumberConversions.floatingPointToBigInteger(from, converter));
        } else {
            BigInteger expectedBI = (BigInteger) exp;
            BigInteger result = NumberConversions.floatingPointToBigInteger(from, converter);
            assertEquals(expectedBI, result, description);
        }
    }

    /* -------------------------------------------------------------
     *  Negative numbers
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("negativeNumbersProvider")
    void negativeNumbers(String description, Object from, String expected) {
        Object exp = parseExpected(expected);
        if (exp instanceof String) {
            Class<? extends Throwable> ex = exceptionClassFor((String) exp);
            assertThrows(ex, () -> NumberConversions.floatingPointToBigInteger(from, converter));
        } else {
            BigInteger expectedBI = (BigInteger) exp;
            BigInteger result = NumberConversions.floatingPointToBigInteger(from, converter);
            assertEquals(expectedBI, result, description);
        }
    }

    /* -------------------------------------------------------------
     *  Non‑numeric or special floating‑point values
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("specialFloatingProvider")
    void specialFloatingValues(String description, Object from, String expected) {
        Object exp = parseExpected(expected);
        if (exp instanceof String) {
            Class<? extends Throwable> ex = exceptionClassFor((String) exp);
            assertThrows(ex, () -> NumberConversions.floatingPointToBigInteger(from, converter));
        } else {
            fail("Expected an exception for special floating‑point values");
        }
    }

    /* -------------------------------------------------------------
     *  Different numeric object types (via Converter delegation)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("differentNumericProvider")
    void differentNumericTypes(String description, Object from, String expected) {
        Object exp = parseExpected(expected);
        if (exp instanceof String) {
            Class<? extends Throwable> ex = exceptionClassFor((String) exp);
            assertThrows(ex, () -> NumberConversions.floatingPointToBigInteger(from, converter));
        } else {
            BigInteger expectedBI = (BigInteger) exp;
            BigInteger result = NumberConversions.floatingPointToBigInteger(from, converter);
            assertEquals(expectedBI, result, description);
        }
    }

    /* -------------------------------------------------------------
     *  String inputs
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("stringInputsProvider")
    void stringInputs(String description, Object from, String expected) {
        Object exp = parseExpected(expected);
        if (exp instanceof String) {
            Class<? extends Throwable> ex = exceptionClassFor((String) exp);
            assertThrows(ex, () -> NumberConversions.floatingPointToBigInteger(from, converter));
        } else {
            BigInteger expectedBI = (BigInteger) exp;
            BigInteger result = NumberConversions.floatingPointToBigInteger(from, converter);
            assertEquals(expectedBI, result, description);
        }
    }

    /* -------------------------------------------------------------
     *  Integer wrapper types
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("integerWrapperProvider")
    void integerWrapperTypes(String description, Object from, String expected) {
        Object exp = parseExpected(expected);
        if (exp instanceof String) {
            Class<? extends Throwable> ex = exceptionClassFor((String) exp);
            assertThrows(ex, () -> NumberConversions.floatingPointToBigInteger(from, converter));
        } else {
            BigInteger expectedBI = (BigInteger) exp;
            BigInteger result = NumberConversions.floatingPointToBigInteger(from, converter);
            assertEquals(expectedBI, result, description);
        }
    }

    /* -------------------------------------------------------------
     *  Atomic numeric types
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("atomicNumericProvider")
    void atomicNumericTypes(String description, Object from, String expected) {
        Object exp = parseExpected(expected);
        if (exp instanceof String) {
            Class<? extends Throwable> ex = exceptionClassFor((String) exp);
            assertThrows(ex, () -> NumberConversions.floatingPointToBigInteger(from, converter));
        } else {
            BigInteger expectedBI = (BigInteger) exp;
            BigInteger result = NumberConversions.floatingPointToBigInteger(from, converter);
            assertEquals(expectedBI, result, description);
        }
    }

    /* -------------------------------------------------------------
     *  Boundary floating‑point values
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("boundaryProvider")
    void boundaryFloatingValues(String description, Object from, String expected) {
        Object exp = parseExpected(expected);
        if (exp instanceof String) {
            Class<? extends Throwable> ex = exceptionClassFor((String) exp);
            assertThrows(ex, () -> NumberConversions.floatingPointToBigInteger(from, converter));
        } else {
            BigInteger expectedBI = (BigInteger) exp;
            BigInteger result = NumberConversions.floatingPointToBigInteger(from, converter);
            assertEquals(expectedBI, result, description);
        }
    }

    /* -------------------------------------------------------------
     *  Null and invalid inputs
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullAndInvalidProvider")
    void nullAndInvalidInputs(String description, Object from, String expected) {
        Object exp = parseExpected(expected);
        if (exp instanceof String) {
            Class<? extends Throwable> ex = exceptionClassFor((String) exp);
            assertThrows(ex, () -> NumberConversions.floatingPointToBigInteger(from, converter));
        } else {
            fail("Expected an exception for null/invalid inputs");
        }
    }

    /* -------------------------------------------------------------
     *  Non‑numeric string inputs
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nonNumericStringProvider")
    void nonNumericStringInputs(String description, Object from, String expected) {
        Object exp = parseExpected(expected);
        if (exp instanceof String) {
            Class<? extends Throwable> ex = exceptionClassFor((String) exp);
            assertThrows(ex, () -> NumberConversions.floatingPointToBigInteger(from, converter));
        } else {
            fail("Expected an exception for non‑numeric string inputs");
        }
    }

    /* -------------------------------------------------------------
     *  Float special values
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("floatSpecialProvider")
    void floatSpecialValues(String description, Object from, String expected) {
        Object exp = parseExpected(expected);
        if (exp instanceof String) {
            Class<? extends Throwable> ex = exceptionClassFor((String) exp);
            assertThrows(ex, () -> NumberConversions.floatingPointToBigInteger(from, converter));
        } else {
            fail("Expected an exception for Float special values");
        }
    }

    /* -------------------------------------------------------------
     *  Overflow to Infinity via large numeric types
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("overflowProvider")
    void overflowToInfinity(String description, Object from, String expected) {
        Object exp = parseExpected(expected);
        if (exp instanceof String) {
            Class<? extends Throwable> ex = exceptionClassFor((String) exp);
            assertThrows(ex, () -> NumberConversions.floatingPointToBigInteger(from, converter));
        } else {
            fail("Expected an exception for overflow cases");
        }
    }

    /* -------------------------------------------------------------
     *  String representations of Infinity
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("infinityStringProvider")
    void infinityStringRepresentations(String description, Object from, String expected) {
        Object exp = parseExpected(expected);
        if (exp instanceof String) {
            Class<? extends Throwable> ex = exceptionClassFor((String) exp);
            assertThrows(ex, () -> NumberConversions.floatingPointToBigInteger(from, converter));
        } else {
            fail("Expected an exception for Infinity string representations");
        }
    }

    /* -------------------------------------------------------------
     *  Converter throwing exception
     * ------------------------------------------------------------- */
    @Test
    void converterThrowsIllegalArgumentException() {
        // Mock the static NumberConversions.toDouble method to throw an IllegalArgumentException.
        try (var mocked = mockStatic(NumberConversions.class)) {
            mocked.when(() -> NumberConversions.toDouble(any(), any())).thenThrow(new IllegalArgumentException("Conversion failed"));
            Converter dummy = mock(Converter.class);
            assertThrows(IllegalArgumentException.class, () -> NumberConversions.floatingPointToBigInteger(123.45, dummy));
        }
    }

    @Test
    void converterThrowsRuntimeException() {
        try (var mocked = mockStatic(NumberConversions.class)) {
            mocked.when(() -> NumberConversions.toDouble(any(), any())).thenThrow(new RuntimeException("Unexpected error"));
            Converter dummy = mock(Converter.class);
            assertThrows(RuntimeException.class, () -> NumberConversions.floatingPointToBigInteger("42", dummy));
        }
    }
}
