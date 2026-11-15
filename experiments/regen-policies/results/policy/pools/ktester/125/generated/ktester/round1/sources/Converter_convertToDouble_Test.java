package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;

class Converter_convertToDouble_Test {

    /**
     * A {@link Number} implementation whose {@code doubleValue()} method throws a
     * {@link RuntimeException}.  Used to verify that the exception propagates out of
     * {@link Converter#convertToDouble(Object)}.
     */
    static class BrokenNumber extends Number {

        @Override
        public int intValue() {
            return 0;
        }

        @Override
        public long longValue() {
            return 0L;
        }

        @Override
        public float floatValue() {
            return 0f;
        }

        @Override
        public double doubleValue() {
            throw new RuntimeException("BrokenNumber cannot provide a double value");
        }
    }

    /* --------------------------------------------------- */
    private static Stream<Arguments> numericProvider() {
        return Stream.of(// Integer
        arguments(42, 42.0), // Long (large value to test magnitude preservation)
        arguments(1234567890123L, 1234567890123.0), // Float (provided as a Float instance)
        arguments(3.14f, 3.14), // Double (already a double)
        arguments(2.718281828459045, 2.718281828459045), // BigDecimal
        arguments(new BigDecimal("123.456"), 123.456), // BigInteger
        arguments(new BigInteger("9876543210"), 9876543210.0), // Byte (auto‑boxed)
        arguments((byte) 127, 127.0), // Short (auto‑boxed)
        arguments((short) 32767, 32767.0));
    }

    /* --------------------------------------------------- */
    private static Stream<Arguments> stringParsingProvider() {
        return Stream.of(arguments("123.45", 123.45), arguments("-0.001", -0.001), arguments("1e3", 1000.0), arguments("   42   ", 42.0), arguments("NaN", Double.NaN), arguments("Infinity", Double.POSITIVE_INFINITY), arguments("-Infinity", Double.NEGATIVE_INFINITY));
    }

    /* --------------------------------------------------- */
    private static Stream<Arguments> stringExceptionProvider() {
        return Stream.of(arguments("", NumberFormatException.class), arguments("123abc", NumberFormatException.class), arguments("--5", NumberFormatException.class));
    }

    /* --------------------------------------------------- */
    private static Stream<Arguments> booleanProvider() {
        return Stream.of(arguments(true, 1.0), arguments(false, 0.0));
    }

    /* --------------------------------------------------- */
    private static Stream<Arguments> charProvider() {
        return Stream.of(arguments('5', 5.0), arguments("7", 7.0), arguments("12.34", 12.34));
    }

    /* --------------------------------------------------- */
    private static Stream<Arguments> dateTimeProvider() {
        return Stream.of(arguments(new Date(0L), 0.0), arguments(new Timestamp(1622548800000L), 1622548800000.0), arguments(Instant.ofEpochMilli(123456789L), 123456789.0), // Unsupported date/time types – expect null
        arguments(LocalDate.of(2020, 1, 1), null), arguments(LocalDateTime.of(2020, 1, 1, 12, 30, 15), null), arguments(ZonedDateTime.parse("2020-01-01T12:00:00Z"), null));
    }

    /* --------------------------------------------------- */
    private static Stream<Arguments> unsupportedProvider() {
        return Stream.of(arguments(new Object(), null), arguments(new int[] { 1, 2, 3 }, null), arguments(UUID.randomUUID(), null));
    }

    /* --------------------------------------------------- */
    private static Stream<Arguments> specialNumericProvider() {
        return Stream.of(arguments(Double.NaN, Double.NaN), arguments(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY), arguments(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY));
    }

    /* --------------------------------------------------- */
    private static Stream<Arguments> atomicExceptionProvider() {
        return Stream.of(arguments(new AtomicBoolean(true), UnsupportedOperationException.class), arguments(new AtomicInteger(5), UnsupportedOperationException.class), arguments(new AtomicLong(123L), UnsupportedOperationException.class));
    }

    @Test
    void convertNullReturnsNull() {
        Double result = Converter.convertToDouble(null);
        assertNull(result, "Converting null should return null for object target type");
    }

    @Test
    void convertFromNumberTypes() {
        assertEquals(42.0, Converter.convertToDouble(42), "Integer should convert to Double");
        assertEquals(42.0, Converter.convertToDouble(42L), "Long should convert to Double");
        assertEquals(42.5, Converter.convertToDouble(42.5f), "Float should convert to Double");
        assertEquals(42.5, Converter.convertToDouble(42.5d), "Double should stay unchanged");
        assertEquals(42.0, Converter.convertToDouble(new BigDecimal("42")), "BigDecimal should convert to Double");
        assertEquals(42.0, Converter.convertToDouble(new BigInteger("42")), "BigInteger should convert to Double");
    }

    @Test
    void convertFromStringNumeric() {
        assertEquals(123.0, Converter.convertToDouble("123"), "Numeric String should convert to Double");
        assertEquals(123.45, Converter.convertToDouble("123.45"), "Decimal String should convert to Double");
        assertEquals(-10.0, Converter.convertToDouble("-10"), "Negative numeric String should convert to Double");
    }

    @Test
    void convertFromStringNonNumericThrowsException() {
        // The Converter throws an IllegalArgumentException for non‑numeric strings when targeting Double
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToDouble("not-a-number"), "Non‑numeric String should cause an IllegalArgumentException");
    }

    @Test
    void convertFromMapWithValueKey() {
        Map<String, Object> map = new HashMap<>();
        // the convention used by Converter for map‑based conversion
        map.put("_v", "99.99");
        Double result = Converter.convertToDouble(map);
        assertEquals(99.99, result, "Map containing \"_v\" key should be converted to Double");
    }

    @Test
    void convertFromBoolean() {
        // According to Converter's documentation, true -> 1.0, false -> 0.0 when converting to numeric types
        assertEquals(1.0, Converter.convertToDouble(true), "Boolean true should convert to 1.0");
        assertEquals(0.0, Converter.convertToDouble(false), "Boolean false should convert to 0.0");
    }

    @Test
    void convertFromAtomicTypes() {
        assertEquals(7.0, Converter.convertToDouble(java.util.concurrent.atomic.AtomicInteger.valueOf(7)), "AtomicInteger should convert to Double");
        assertEquals(8.0, Converter.convertToDouble(java.util.concurrent.atomic.AtomicLong.valueOf(8L)), "AtomicLong should convert to Double");
        // AtomicBoolean conversion follows the same rule as Boolean
        assertEquals(1.0, Converter.convertToDouble(java.util.concurrent.atomic.AtomicBoolean.TRUE), "AtomicBoolean true should convert to 1.0");
        assertEquals(0.0, Converter.convertToDouble(java.util.concurrent.atomic.AtomicBoolean.FALSE), "AtomicBoolean false should convert to 0.0");
    }

    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @MethodSource("numericProvider")
    void convertFromNumericTypes(Object input, double expected) {
        Double result = Converter.convertToDouble(input);
        assertEquals(expected, result, () -> "Numeric conversion failed for input: " + input);
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" → {1}")
    @MethodSource("stringParsingProvider")
    void convertFromStringParsing(String input, double expected) {
        Double result = Converter.convertToDouble(input);
        if (Double.isNaN(expected)) {
            assertTrue(Double.isNaN(result), "Expected NaN for input \"" + input + "\"");
        } else if (Double.isInfinite(expected)) {
            assertEquals(expected, result, "Expected infinite value for input \"" + input + "\"");
        } else {
            assertEquals(expected, result, "String parsing failed for input \"" + input + "\"");
        }
    }

    @Test
    void convertFromStringNonNumericReturnsNull() {
        // According to the JSON spec a non‑numeric string should result in null,
        // not an exception (the earlier test class expected an exception – we follow the spec).
        Double result = Converter.convertToDouble("not a number");
        assertNull(result, "Non‑numeric string should return null");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" throws {1}")
    @MethodSource("stringExceptionProvider")
    void convertFromStringParsingExceptions(String input, Class<? extends Throwable> exception) {
        assertThrows(exception, () -> Converter.convertToDouble(input), () -> "Expected " + exception.getSimpleName() + " for input \"" + input + "\"");
    }

    @ParameterizedTest(name = "[{index}] Char/CharSequence {0} → {1}")
    @MethodSource("charProvider")
    void convertFromCharAndCharSequence(Object input, double expected) {
        Double result = Converter.convertToDouble(input);
        assertEquals(expected, result, "Char/CharSequence conversion failed for input: " + input);
    }

    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @MethodSource("dateTimeProvider")
    void convertFromDateTime(Object input, Double expected) {
        Double result = Converter.convertToDouble(input);
        if (expected == null) {
            assertNull(result, "Expected null for unsupported date/time type: " + input);
        } else {
            assertEquals(expected, result, "Date/Time conversion failed for input: " + input);
        }
    }

    @ParameterizedTest(name = "[{index}] Unsupported {0} → null")
    @MethodSource("unsupportedProvider")
    void convertFromUnsupportedTypes(Object input, Double ignored) {
        Double result = Converter.convertToDouble(input);
        assertNull(result, "Unsupported type should result in null: " + input.getClass());
    }

    @ParameterizedTest(name = "[{index}] Special {0} → {1}")
    @MethodSource("specialNumericProvider")
    void convertFromSpecialNumericValues(Double input, Double expected) {
        Double result = Converter.convertToDouble(input);
        if (Double.isNaN(expected)) {
            assertTrue(Double.isNaN(result), "Expected NaN for input");
        } else {
            assertEquals(expected, result, "Special numeric value conversion failed");
        }
    }

    /* --------------------------------------------------- */
    @Test
    void convertFromEmptyByteBufferThrowsBufferUnderflowException() {
        ByteBuffer empty = ByteBuffer.allocate(0);
        assertThrows(BufferUnderflowException.class, () -> Converter.convertToDouble(empty), "Empty ByteBuffer should trigger BufferUnderflowException");
    }

    @Test
    void convertFromOversizedByteBufferThrowsIllegalArgumentException() {
        // 9 bytes – more than the 8 required for a double
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] { 0x40, 0x09, 0x21, (byte) 0xfb, 0x54, 0x44, 0x2d, 0x18, 0x00 });
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToDouble(buffer), "ByteBuffer with more than 8 bytes should cause IllegalArgumentException");
    }

    /* --------------------------------------------------- */
    @Test
    void convertFromEmptyCharBufferThrowsBufferUnderflowException() {
        CharBuffer empty = CharBuffer.allocate(0);
        assertThrows(BufferUnderflowException.class, () -> Converter.convertToDouble(empty), "Empty CharBuffer should trigger BufferUnderflowException");
    }

    @Test
    void convertFromNonNumericCharBufferThrowsNumberFormatException() {
        CharBuffer buffer = CharBuffer.wrap("abc");
        assertThrows(NumberFormatException.class, () -> Converter.convertToDouble(buffer), "Non‑numeric CharBuffer should cause NumberFormatException");
    }

    @ParameterizedTest(name = "[{index}] {0} throws {1}")
    @MethodSource("atomicExceptionProvider")
    void convertFromAtomicTypesUnsupported(Object atomic, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> Converter.convertToDouble(atomic), () -> "Expected " + expectedException.getSimpleName() + " for atomic type " + atomic.getClass().getSimpleName());
    }

    @Test
    void convertFromBrokenNumberPropagatesRuntimeException() {
        BrokenNumber broken = new BrokenNumber();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> Converter.convertToDouble(broken), "BrokenNumber should propagate its RuntimeException");
        assertEquals("BrokenNumber cannot provide a double value", ex.getMessage());
    }
}
