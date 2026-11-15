package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.*;
import java.util.stream.Stream;

class Converter_convertToByte_Test {

    /* -------------------- Valid conversions -------------------- */
    static Stream<Arguments> validConversionsProvider() {
        return Stream.of(// Direct Byte instance
        Arguments.of((byte) 5, Byte.valueOf((byte) 5), "Direct Byte instance should be returned unchanged."), // Integer within range
        Arguments.of(Integer.valueOf(123), Byte.valueOf((byte) 123), "Integer is within byte range; conversion should cast to Byte preserving the value."), // Long within range
        Arguments.of(Long.valueOf(200L), Byte.valueOf((byte) 200), "Long is converted to Byte by truncating to the low‑order 8 bits."), // Short
        Arguments.of(Short.valueOf((short) -12), Byte.valueOf((byte) -12), "Short value is cast to Byte."), // Double (truncates)
        Arguments.of(Double.valueOf(45.9), Byte.valueOf((byte) 45), "Floating‑point numbers are truncated (floor) to an integer before conversion to Byte."), // Float (truncates)
        Arguments.of(Float.valueOf(-3.7f), Byte.valueOf((byte) -3), "Float value is truncated and then cast to Byte."), // BigDecimal
        Arguments.of(new BigDecimal("127"), Byte.valueOf((byte) 127), "BigDecimal representing a value inside byte range converts to Byte."), // BigInteger
        Arguments.of(new BigInteger("-128"), Byte.valueOf((byte) -128), "BigInteger within byte range converts to Byte."), // Numeric String
        Arguments.of("64", Byte.valueOf((byte) 64), "String containing a decimal number is parsed and converted to Byte."), // Boolean true
        Arguments.of(Boolean.TRUE, Byte.valueOf((byte) 1), "Boolean true is mapped to numeric 1 and then to Byte."), // Boolean false
        Arguments.of(Boolean.FALSE, Byte.valueOf((byte) 0), "Boolean false is mapped to numeric 0 and then to Byte."), // Character 'A'
        Arguments.of(Character.valueOf('A'), Byte.valueOf((byte) 65), "Character is converted using its Unicode code point (65 for 'A')."), // Null
        Arguments.of(null, null, "Null input should result in a null Byte return value."));
    }

    /* -------------------- Boundary and overflow -------------------- */
    static Stream<Arguments> overflowProvider() {
        return Stream.of(Arguments.of(Integer.valueOf(300), Byte.valueOf((byte) 44), "Integer value larger than 127 overflows; conversion keeps low‑order 8 bits (300 → 44)."), Arguments.of(Integer.valueOf(-130), Byte.valueOf((byte) 126), "Negative integer below -128 wraps around (‑130 → 126)."), Arguments.of(Long.valueOf(1024L), Byte.valueOf((byte) 0), "Long value that is a multiple of 256 results in zero after truncation."));
    }

    /* -------------------- Floating‑point special values -------------------- */
    static Stream<Arguments> floatingSpecialProvider() {
        return Stream.of(Arguments.of(Double.NaN, IllegalArgumentException.class, "NaN cannot be mapped to a numeric byte; conversion should throw an exception."), Arguments.of(Float.POSITIVE_INFINITY, IllegalArgumentException.class, "Infinity is not a finite number; conversion should fail with an exception."), Arguments.of(Float.NaN, IllegalArgumentException.class, "Float.NaN is not a finite number; conversion must fail with IllegalArgumentException."), Arguments.of(Double.POSITIVE_INFINITY, IllegalArgumentException.class, "Positive infinity for double cannot be mapped to a byte; should throw IllegalArgumentException."), Arguments.of(Double.NEGATIVE_INFINITY, IllegalArgumentException.class, "Negative infinity for double is invalid for byte conversion; expect IllegalArgumentException."));
    }

    /* -------------------- String parsing variations -------------------- */
    static Stream<Arguments> stringParsingProvider() {
        return Stream.of(Arguments.of("  42  ", Byte.valueOf((byte) 42), "String with leading/trailing whitespace is trimmed and parsed."), Arguments.of("+15", Byte.valueOf((byte) 15), "String with an explicit plus sign is parsed correctly."), Arguments.of("-9", Byte.valueOf((byte) -9), "Negative numeric string is parsed to a negative byte."), Arguments.of("0x1F", NumberFormatException.class, "Hexadecimal notation is not supported; parsing should throw NumberFormatException."), Arguments.of("", NumberFormatException.class, "Empty string cannot be parsed to a number."), Arguments.of("abc", NumberFormatException.class, "Non‑numeric string should cause a parsing exception."), Arguments.of("12.3", NumberFormatException.class, "String containing a decimal point is not an integer representation; parsing must fail."), Arguments.of("1e2", NumberFormatException.class, "Scientific notation is not supported for Byte parsing; expect NumberFormatException."), Arguments.of("   ", NumberFormatException.class, "String consisting only of whitespace cannot be parsed to a number."), Arguments.of("+", NumberFormatException.class, "A sign without digits is not a valid numeric string; conversion should throw NumberFormatException."));
    }

    /* -------------------- BigDecimal / BigInteger edge cases -------------------- */
    static Stream<Arguments> bigNumberProvider() {
        return Stream.of(Arguments.of(new BigDecimal("123.9"), Byte.valueOf((byte) 123), "BigDecimal with fractional part is truncated before conversion."), Arguments.of(new BigDecimal("200"), Byte.valueOf((byte) 200), "BigDecimal outside byte range is truncated to low‑order 8 bits."), Arguments.of(new BigInteger("1025"), Byte.valueOf((byte) 1), "BigInteger larger than 255 wraps around (1025 → 1)."));
    }

    /* -------------------- Character code‑point conversions -------------------- */
    static Stream<Arguments> characterProvider() {
        return Stream.of(Arguments.of(Character.valueOf((char) 300), Byte.valueOf((byte) 44), "Character with code point 300 overflows a byte; low‑order bits are kept."), Arguments.of(Character.valueOf('\u0100'), Byte.valueOf((byte) 0), "Unicode character 0x0100 equals 256, which maps to zero after truncation."));
    }

    /* -------------------- Unsupported or non‑convertible types -------------------- */
    static Stream<Arguments> unsupportedProvider() {
        return Stream.of(Arguments.of(new Object(), IllegalArgumentException.class, "Arbitrary Object has no conversion path to Byte; should raise an exception."), Arguments.of(TestEnum.ONE, IllegalArgumentException.class, "Enum values are not directly convertible to Byte."), Arguments.of(Arrays.asList(1, 2, 3), IllegalArgumentException.class, "Collections are not supported for Byte conversion."), Arguments.of(new Date(), IllegalArgumentException.class, "java.util.Date has no direct conversion to Byte; method must raise IllegalArgumentException."), Arguments.of(java.time.LocalDate.now(), IllegalArgumentException.class, "LocalDate is not convertible to Byte; expect IllegalArgumentException."), Arguments.of(UUID.randomUUID(), IllegalArgumentException.class, "UUID cannot be mapped to a Byte; conversion should fail."), Arguments.of(java.util.concurrent.atomic.AtomicInteger.valueOf(5), IllegalArgumentException.class, "AtomicInteger is not a supported source type for Byte conversion; expect IllegalArgumentException."));
    }

    /* -------------------- NIO buffer conversions -------------------- */
    static Stream<Arguments> nioBufferProvider() {
        return Stream.of(Arguments.of(ByteBuffer.wrap(new byte[] { (byte) 127 }), Byte.valueOf((byte) 127), "ByteBuffer containing a single byte should be converted to the corresponding Byte."), Arguments.of(CharBuffer.wrap("9"), Byte.valueOf((byte) 9), "CharBuffer with a numeric character is parsed and converted to Byte."));
    }

    /* -------------------- NIO buffer edge cases (invalid content) -------------------- */
    static Stream<Arguments> nioBufferInvalidProvider() {
        return Stream.of(Arguments.of(ByteBuffer.allocate(0), IllegalArgumentException.class, "An empty ByteBuffer provides no data to convert; the method should reject it."), Arguments.of(ByteBuffer.wrap(new byte[] { (byte) 1, (byte) 2 }), IllegalArgumentException.class, "ByteBuffer containing more than one byte is ambiguous for single‑byte conversion; expect exception."), Arguments.of(CharBuffer.allocate(0), IllegalArgumentException.class, "An empty CharBuffer cannot be parsed to a numeric value; conversion should fail."), Arguments.of(CharBuffer.wrap("12"), IllegalArgumentException.class, "CharBuffer with more than one character is ambiguous for Byte conversion; expect IllegalArgumentException."), Arguments.of(CharBuffer.wrap("A"), NumberFormatException.class, "Non‑numeric character in CharBuffer cannot be parsed to a number; should raise NumberFormatException."));
    }

    @Test
    void convertNullReturnsNull() {
        Byte result = Converter.convertToByte(null);
        assertNull(result, "Converting a null source should return null for Byte");
    }

    @Test
    void convertByteReturnsSameValue() {
        Byte source = (byte) 42;
        Byte result = Converter.convertToByte(source);
        assertEquals(source, result, "Converting a Byte should return the same value");
    }

    @Test
    void convertIntegerReturnsByte() {
        Integer source = 123;
        Byte result = Converter.convertToByte(source);
        assertEquals(Byte.valueOf(source.byteValue()), result, "Integer should be converted to its byte representation");
    }

    @Test
    void convertLongReturnsByte() {
        Long source = 200L;
        Byte result = Converter.convertToByte(source);
        assertEquals(Byte.valueOf(source.byteValue()), result, "Long should be converted to its byte representation");
    }

    @Test
    void convertStringNumberReturnsByte() {
        String source = "55";
        Byte result = Converter.convertToByte(source);
        assertEquals(Byte.valueOf((byte) 55), result, "Numeric String should be parsed and converted to Byte");
    }

    @Test
    void convertStringInvalidThrowsException() {
        String source = "not-a-number";
        assertThrows(NumberFormatException.class, () -> Converter.convertToByte(source), "Converting a non‑numeric String should throw NumberFormatException");
    }

    @Test
    void convertBooleanTrueReturnsOne() {
        Boolean source = Boolean.TRUE;
        Byte result = Converter.convertToByte(source);
        assertEquals(Byte.valueOf((byte) 1), result, "Boolean true should be converted to byte value 1");
    }

    @Test
    void convertBooleanFalseReturnsZero() {
        Boolean source = Boolean.FALSE;
        Byte result = Converter.convertToByte(source);
        assertEquals(Byte.valueOf((byte) 0), result, "Boolean false should be converted to byte value 0");
    }

    @Test
    void convertBigDecimalReturnsByte() {
        Byte result = Converter.convertToByte(new java.math.BigDecimal("127"));
        assertEquals(Byte.valueOf((byte) 127), result, "BigDecimal should be converted to its byte representation");
    }

    @Test
    void convertBigIntegerReturnsByte() {
        Byte result = Converter.convertToByte(new java.math.BigInteger("64"));
        assertEquals(Byte.valueOf((byte) 64), result, "BigInteger should be converted to its byte representation");
    }

    @Test
    void convertCharReturnsByte() {
        // ASCII 65
        Character source = 'A';
        Byte result = Converter.convertToByte(source);
        assertEquals(Byte.valueOf((byte) 65), result, "Character should be converted to its ASCII byte value");
    }

    @Test
    void convertByteArrayFirstElementReturned() {
        byte[] source = { 10, 20, 30 };
        // The Converter treats a byte[] as a collection and extracts the "value" key;
        // when converting a byte[] to Byte it returns the first element.
        Byte result = Converter.convertToByte(source);
        assertEquals(Byte.valueOf((byte) 10), result, "Byte array should be converted to the first element as Byte");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validConversionsProvider")
    void testValidConversions(Object source, Byte expected, String description) {
        Byte result = Converter.convertToByte(source);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("overflowProvider")
    void testOverflowConversions(Object source, Byte expected, String description) {
        Byte result = Converter.convertToByte(source);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("floatingSpecialProvider")
    void testFloatingPointSpecialValues(Object source, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> Converter.convertToByte(source), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("stringParsingProvider")
    void testStringParsing(Object source, Object expectedOrException, String description) {
        if (expectedOrException instanceof Byte) {
            Byte expected = (Byte) expectedOrException;
            Byte result = Converter.convertToByte(source);
            assertEquals(expected, result, description);
        } else {
            Class<? extends Throwable> ex = (Class<? extends Throwable>) expectedOrException;
            assertThrows(ex, () -> Converter.convertToByte(source), description);
        }
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("bigNumberProvider")
    void testBigNumberConversions(Object source, Byte expected, String description) {
        Byte result = Converter.convertToByte(source);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("characterProvider")
    void testCharacterConversions(Character source, Byte expected, String description) {
        Byte result = Converter.convertToByte(source);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("unsupportedProvider")
    void testUnsupportedTypes(Object source, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> Converter.convertToByte(source), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("nioBufferProvider")
    void testNioBufferConversions(Object source, Byte expected, String description) {
        Byte result = Converter.convertToByte(source);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("nioBufferInvalidProvider")
    void testNioBufferInvalidContent(Object source, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> Converter.convertToByte(source), description);
    }
}
