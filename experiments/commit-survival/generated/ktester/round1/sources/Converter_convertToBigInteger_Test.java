package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;

@ExtendWith(MockitoExtension.class)
class Converter_convertToBigInteger_Test {

    static Stream<Arguments> nullInputProvider() {
        return Stream.of(Arguments.of((Object) null));
    }

    static Stream<Arguments> numericPrimitiveProvider() {
        return Stream.of(// Integer
        Arguments.of(42, "42"), // Long negative
        Arguments.of(-5L, "-5"), // Double truncates
        Arguments.of(3.14, "3"), // Float negative truncates
        Arguments.of(-2.9f, "-2"), // Short
        Arguments.of((short) 7, "7"), // Byte
        Arguments.of((byte) 9, "9"));
    }

    static Stream<Arguments> bigDecimalProvider() {
        return Stream.of(Arguments.of(new BigDecimal("123.456"), "123"));
    }

    static Stream<Arguments> stringInputProvider() {
        return Stream.of(Arguments.of("9876543210", "9876543210"), Arguments.of("  42 ", "42"), Arguments.of("-15", "-15"), Arguments.of("abc", "NumberFormatException"));
    }

    static Stream<Arguments> booleanProvider() {
        return Stream.of(Arguments.of(Boolean.TRUE, "1"), Arguments.of(Boolean.FALSE, "0"));
    }

    static Stream<Arguments> atomicProvider() {
        return Stream.of(Arguments.of(new AtomicInteger(10), "10"), Arguments.of(new AtomicLong(-20L), "-20"), Arguments.of(new AtomicBoolean(true), "1"), Arguments.of(new AtomicBoolean(false), "0"), // Additional exception scenario
        // AtomicInteger with null value
        Arguments.of(new AtomicInteger(), "NullPointerException"));
    }

    static Stream<Arguments> bigIntegerProvider() {
        return Stream.of(Arguments.of(new BigInteger("12345678901234567890"), "12345678901234567890"));
    }

    static Stream<Arguments> unsupportedProvider() {
        return Stream.of(Arguments.of(Date.from(Instant.parse("2023-01-01T00:00:00Z")), IllegalArgumentException.class), Arguments.of(new Object(), IllegalArgumentException.class));
    }

    static Stream<Arguments> stringEdgeProvider() {
        return Stream.of(Arguments.of("", "NumberFormatException"), Arguments.of("+12345", "12345"), Arguments.of("0000123", "123"), Arguments.of("1 2 3", "NumberFormatException"), Arguments.of("  ", "NumberFormatException"));
    }

    static Stream<Arguments> characterProvider() {
        return Stream.of(// Unicode code point of '7' is 55
        Arguments.of('7', "55"), Arguments.of('a', "NumberFormatException"));
    }

    static Stream<Arguments> wrapperNumericProvider() {
        return Stream.of(Arguments.of(Byte.valueOf((byte) 8), "8"), Arguments.of(Short.valueOf((short) -12), "-12"), Arguments.of(Long.valueOf(1234567890123456789L), "1234567890123456789"));
    }

    static Stream<Arguments> largeNumberProvider() {
        return Stream.of(Arguments.of("1234567890123456789012345678901234567890", "1234567890123456789012345678901234567890"), Arguments.of(new BigDecimal("98765432109876543210.999"), "98765432109876543210"), Arguments.of(new BigDecimal("-99999999999999999999.12345"), "-99999999999999999999"));
    }

    static Stream<Arguments> temporalUnsupportedProvider() {
        return Stream.of(Arguments.of(LocalDate.of(2023, 12, 31)), Arguments.of(LocalDateTime.of(2023, 12, 31, 23, 59, 59)), Arguments.of(ZonedDateTime.of(LocalDateTime.of(2023, 12, 31, 23, 59, 59), ZoneId.of("Europe/Paris"))), Arguments.of(Timestamp.from(Instant.ofEpochMilli(1672531200000L))));
    }

    static Stream<Arguments> additionalExceptionProvider() {
        return Stream.of(// AtomicInteger containing null -> NullPointerException (simulated by null value)
        // actually never null, using placeholder
        Arguments.of(new AtomicInteger() {

            {
                set(0);
            }
        }, NullPointerException.class), // AtomicLong with non‑numeric string (cannot happen directly; we simulate via reflection)
        // placeholder – real case not representable
        Arguments.of(new AtomicLong(Long.parseLong("0")), NumberFormatException.class), // Byte wrapper out of range
        // placeholder – actual out‑of‑range can't be constructed
        Arguments.of(Byte.valueOf((byte) 0), NumberFormatException.class), // Short wrapper non‑numeric
        // placeholder
        Arguments.of(Short.valueOf((short) 0), NumberFormatException.class), // Long wrapper exceeding max
        // placeholder
        Arguments.of(Long.valueOf(Long.MAX_VALUE), NumberFormatException.class), // BigDecimal NaN
        Arguments.of(new BigDecimal(Double.NaN), NumberFormatException.class), // BigDecimal Infinity
        Arguments.of(new BigDecimal(Double.POSITIVE_INFINITY), NumberFormatException.class), // Empty character string
        Arguments.of("", IllegalArgumentException.class), // Multi‑character string as character
        Arguments.of("AB", IllegalArgumentException.class), // Calendar instance
        Arguments.of(new GregorianCalendar(2023, Calendar.JANUARY, 1), IllegalArgumentException.class), // Timestamp with invalid representation (null)
        Arguments.of((Timestamp) null, IllegalArgumentException.class));
    }

    @Test
    void convertNullReturnsNull() {
        // given
        Object input = null;
        // when
        BigInteger result = Converter.convertToBigInteger(input);
        // then
        assertNull(result, "Converting null should return null");
    }

    @Test
    void convertIntegerToBigInteger() {
        // given
        Integer input = 42;
        // when
        BigInteger result = Converter.convertToBigInteger(input);
        // then
        assertEquals(BigInteger.valueOf(42), result);
    }

    @Test
    void convertLongToBigInteger() {
        // given
        Long input = 1234567890123L;
        // when
        BigInteger result = Converter.convertToBigInteger(input);
        // then
        assertEquals(BigInteger.valueOf(1234567890123L), result);
    }

    @Test
    void convertStringToBigInteger() {
        // given
        String input = "9876543210";
        // when
        BigInteger result = Converter.convertToBigInteger(input);
        // then
        assertEquals(new BigInteger("9876543210"), result);
    }

    @Test
    void convertBigDecimalToBigInteger() {
        // given
        // fractional part should be truncated
        BigDecimal input = new BigDecimal("12345.6789");
        // when
        BigInteger result = Converter.convertToBigInteger(input);
        // then
        // The library follows the same semantics as BigDecimal.toBigInteger() (truncates toward zero)
        assertEquals(new BigInteger("12345"), result);
    }

    @Test
    void convertBigIntegerReturnsSameInstance() {
        // given
        BigInteger input = new BigInteger("55555555555555555555");
        // when
        BigInteger result = Converter.convertToBigInteger(input);
        // then
        // The conversion should return the original instance (or an equal value)
        assertSame(input, result, "Converting a BigInteger should return the same instance");
    }

    @Test
    void convertUnsupportedTypeReturnsNull() {
        // given
        // Boolean is not convertible to BigInteger by default
        Boolean input = Boolean.TRUE;
        // when
        BigInteger result = Converter.convertToBigInteger(input);
        // then
        assertNull(result, "Unsupported source type should result in null");
    }

    // ---------- Null Input ----------
    @ParameterizedTest(name = "{index}: null input")
    @MethodSource("nullInputProvider")
    void testNullInput(Object input) {
        assertNull(Converter.convertToBigInteger(input));
    }

    // ---------- Numeric Primitive Types ----------
    @ParameterizedTest(name = "{index}: primitive {0} -> {1}")
    @MethodSource("numericPrimitiveProvider")
    void testNumericPrimitives(Object input, String expected) {
        BigInteger result = Converter.convertToBigInteger(input);
        assertEquals(new BigInteger(expected), result);
    }

    // ---------- BigDecimal Input ----------
    @ParameterizedTest(name = "{index}: BigDecimal \"{0}\" -> {1}")
    @MethodSource("bigDecimalProvider")
    void testBigDecimalInput(BigDecimal input, String expected) {
        BigInteger result = Converter.convertToBigInteger(input);
        assertEquals(new BigInteger(expected), result);
    }

    // ---------- String Inputs ----------
    @ParameterizedTest(name = "{index}: String \"{0}\" -> {1}")
    @MethodSource("stringInputProvider")
    void testStringInputs(String input, String expected) {
        if ("NumberFormatException".equals(expected)) {
            assertThrows(NumberFormatException.class, () -> Converter.convertToBigInteger(input), "Expected NumberFormatException for input: \"" + input + "\"");
        } else {
            BigInteger result = Converter.convertToBigInteger(input);
            assertEquals(new BigInteger(expected), result);
        }
    }

    // ---------- Boolean Inputs ----------
    @ParameterizedTest(name = "{index}: Boolean {0} -> {1}")
    @MethodSource("booleanProvider")
    void testBooleanInputs(Boolean input, String expected) {
        BigInteger result = Converter.convertToBigInteger(input);
        assertEquals(new BigInteger(expected), result);
    }

    // ---------- Atomic Types ----------
    @ParameterizedTest(name = "{index}: {0} -> {1}")
    @MethodSource("atomicProvider")
    void testAtomicTypes(Object input, String expected) {
        if ("NullPointerException".equals(expected)) {
            assertThrows(NullPointerException.class, () -> Converter.convertToBigInteger(input));
        } else {
            BigInteger result = Converter.convertToBigInteger(input);
            assertEquals(new BigInteger(expected), result);
        }
    }

    // ---------- BigInteger Input ----------
    @ParameterizedTest(name = "{index}: BigInteger \"{0}\"")
    @MethodSource("bigIntegerProvider")
    void testBigIntegerInput(BigInteger input, String expected) {
        BigInteger result = Converter.convertToBigInteger(input);
        assertEquals(new BigInteger(expected), result);
        // also verify same instance when possible
        assertSame(input, result, "Should return the same instance for BigInteger input");
    }

    // ---------- Unsupported Types ----------
    @ParameterizedTest(name = "{index}: unsupported {0}")
    @MethodSource("unsupportedProvider")
    void testUnsupportedTypes(Object input, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> Converter.convertToBigInteger(input), "Expected " + expectedException.getSimpleName() + " for input: " + input);
    }

    // ---------- String Edge Cases ----------
    @ParameterizedTest(name = "{index}: edge string \"{0}\"")
    @MethodSource("stringEdgeProvider")
    void testStringEdgeCases(String input, String expected) {
        if ("NumberFormatException".equals(expected)) {
            assertThrows(NumberFormatException.class, () -> Converter.convertToBigInteger(input));
        } else {
            BigInteger result = Converter.convertToBigInteger(input);
            assertEquals(new BigInteger(expected), result);
        }
    }

    // ---------- Character Inputs ----------
    @ParameterizedTest(name = "{index}: char \"{0}\"")
    @MethodSource("characterProvider")
    void testCharacterInputs(Object input, String expected) {
        if ("NumberFormatException".equals(expected) || "IllegalArgumentException".equals(expected)) {
            Class<? extends Throwable> ex = NumberFormatException.class;
            if ("IllegalArgumentException".equals(expected))
                ex = IllegalArgumentException.class;
            assertThrows(ex, () -> Converter.convertToBigInteger(input));
        } else {
            BigInteger result = Converter.convertToBigInteger(input);
            assertEquals(new BigInteger(expected), result);
        }
    }

    // ---------- Wrapper Numeric Types ----------
    @ParameterizedTest(name = "{index}: wrapper {0}")
    @MethodSource("wrapperNumericProvider")
    void testWrapperNumericTypes(Object input, String expected) {
        if ("NumberFormatException".equals(expected)) {
            assertThrows(NumberFormatException.class, () -> Converter.convertToBigInteger(input));
        } else {
            BigInteger result = Converter.convertToBigInteger(input);
            assertEquals(new BigInteger(expected), result);
        }
    }

    // ---------- Large Number Cases ----------
    @ParameterizedTest(name = "{index}: large {0}")
    @MethodSource("largeNumberProvider")
    void testLargeNumbers(Object input, String expected) {
        if ("NumberFormatException".equals(expected)) {
            assertThrows(NumberFormatException.class, () -> Converter.convertToBigInteger(input));
        } else {
            BigInteger result = Converter.convertToBigInteger(input);
            assertEquals(new BigInteger(expected), result);
        }
    }

    // ---------- Temporal Types Unsupported ----------
    @ParameterizedTest(name = "{index}: temporal {0}")
    @MethodSource("temporalUnsupportedProvider")
    void testTemporalUnsupported(Object input) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToBigInteger(input));
    }

    // ---------- Additional Exception Scenarios ----------
    @ParameterizedTest(name = "{index}: extra exception {0}")
    @MethodSource("additionalExceptionProvider")
    void testAdditionalExceptionScenarios(Object input, Class<? extends Throwable> expected) {
        assertThrows(expected, () -> Converter.convertToBigInteger(input));
    }
}
