package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.time.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

class Converter_convert2int_Test {

    static Stream<Arguments> numericAndWrapperProvider() {
        return Stream.of(// Integer wrapper
        Arguments.of("Integer 42 → 42", Integer.valueOf(42), 42), // Negative Integer
        Arguments.of("Integer -7 → -7", Integer.valueOf(-7), -7), // Long within int range
        Arguments.of("Long 12345L → 12345", 12345L, 12345), // Long overflow – cast follows Java rules (lower 32 bits)
        Arguments.of("Long 0xFFFFFFFFL → -1 (int cast)", 0xFFFFFFFFL, (int) 0xFFFFFFFFL), // Double truncation
        Arguments.of("Double 12.9 → 12", 12.9, 12), Arguments.of("Double -3.14 → -3", -3.14, -3), // Float truncation
        Arguments.of("Float 2.99f → 2", 2.99f, 2), // Numeric strings
        Arguments.of("String \"123\" → 123", "123", 123), Arguments.of("String \"-456\" → -456", "-456", -456), Arguments.of("String with whitespace \"  78  \" → 78", "  78  ", 78), // Hexadecimal string (prefixed with 0x)
        Arguments.of("Hex string \"0x1A\" → 26", "0x1A", 26), // Octal‑like string (leading zero) – treated as decimal
        Arguments.of("Octal‑like string \"075\" → 75", "075", 75), // Boolean values
        Arguments.of("Boolean true → 1", Boolean.TRUE, 1), Arguments.of("Boolean false → 0", Boolean.FALSE, 0), // Non‑numeric string – conversion fails, default 0
        Arguments.of("Non‑numeric string \"not a number\" → 0", "not a number", 0));
    }

    static Stream<Arguments> charSequenceProvider() {
        return Stream.of(Arguments.of("Single numeric character \"9\" → 9", "9", 9), Arguments.of("Single non‑numeric character \"A\" → 0", "A", 0), Arguments.of("Multi‑character numeric string \"12\" → 12", "12", 12), Arguments.of("String with whitespace \"  34  \" → 34", "  34  ", 34));
    }

    static Stream<Arguments> dateTimeProvider() {
        return Stream.of(// ISO‑8601 LocalDateTime string → epoch seconds
        Arguments.of("ISO‑8601 \"2023-01-01T12:34:56\" → 1672574096", "2023-01-01T12:34:56", 1672574096), // Milliseconds since epoch → seconds (int)
        Arguments.of("Long milliseconds 1672574096000L → 1672574096", 1672574096000L, 1672574096));
    }

    static Stream<Arguments> unsupportedCollectionProvider() {
        return Stream.of(Arguments.of("Array [1,2,3] → 0", new int[] { 1, 2, 3 }, 0), Arguments.of("List [1,2,3] → 0", Arrays.asList(1, 2, 3), 0), Arguments.of("Map {key=value} → 0", Collections.singletonMap("key", "value"), 0));
    }

    static Stream<Arguments> atomicAndBigNumberProvider() {
        return Stream.of(Arguments.of("AtomicInteger(77) → 77", new AtomicInteger(77), 77), Arguments.of("BigDecimal \"123.45\" → 123", new BigDecimal("123.45"), 123), // BigInteger larger than int range – lower 32 bits as per Java casting
        Arguments.of("BigInteger \"9876543210\" → 1589131938", new BigInteger("9876543210"), (int) new BigInteger("9876543210").longValue()));
    }

    static Stream<Arguments> edgeOverflowProvider() {
        return Stream.of(Arguments.of("Maximum int 2147483647 → 2147483647", 2147483647, 2147483647), Arguments.of("Value just above max int 2147483648 → -2147483648 (wrap)", 2147483648L, -2147483648), Arguments.of("Minimum int -2147483648 → -2147483648", -2147483648, -2147483648), Arguments.of("Value just below min int -2147483649 → 2147483647 (wrap)", -2147483649L, 2147483647));
    }

    @Test
    @DisplayName("convert2int returns 0 when source is null")
    void testNullReturnsZero() {
        assertEquals(0, Converter.convert2int(null), "Converter.convert2int(null) should return 0");
    }

    @Test
    @DisplayName("convert2int returns the same int value when source is an Integer")
    void testIntegerInput() {
        Integer source = 42;
        assertEquals(42, Converter.convert2int(source));
    }

    @Test
    @DisplayName("convert2int converts a numeric String to int")
    void testStringNumeric() {
        assertEquals(123, Converter.convert2int("123"));
        assertEquals(-7, Converter.convert2int("-7"));
    }

    @Test
    @DisplayName("convert2int converts a Double to int by truncation")
    void testDoubleInput() {
        assertEquals(12, Converter.convert2int(12.9));
        assertEquals(-3, Converter.convert2int(-3.14));
    }

    @Test
    @DisplayName("convert2int converts a Boolean to int (true → 1, false → 0)")
    void testBooleanInput() {
        assertEquals(1, Converter.convert2int(Boolean.TRUE));
        assertEquals(0, Converter.convert2int(Boolean.FALSE));
    }

    @Test
    @DisplayName("convert2int converts a Long to int (with possible overflow handling by the Converter)")
    void testLongInput() {
        assertEquals(12345, Converter.convert2int(12345L));
        // If the long value exceeds int range, the Converter follows Java casting rules
        assertEquals((int) 0xFFFFFFFFL, Converter.convert2int(0xFFFFFFFFL));
    }

    @Test
    @DisplayName("convert2int converts a BigDecimal to int")
    void testBigDecimalInput() {
        assertEquals(99, Converter.convert2int(new java.math.BigDecimal("99.99")));
    }

    @Test
    @DisplayName("convert2int converts a CharSequence (String) containing a number with whitespace")
    void testStringWithWhitespace() {
        assertEquals(5, Converter.convert2int(" 5 "));
    }

    @Test
    @DisplayName("convert2int returns 0 for an unsupported type (e.g., a plain Object)")
    void testUnsupportedType() {
        // The Converter returns the default primitive value (0) when it cannot perform a conversion.
        assertEquals(0, Converter.convert2int(new Object()));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("numericAndWrapperProvider")
    @DisplayName("Numeric and wrapper conversions")
    void testNumericAndWrapperConversions(String description, Object input, int expected) {
        assertEquals(expected, Converter.convert2int(input), description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("charSequenceProvider")
    @DisplayName("CharSequence conversions")
    void testCharSequenceConversions(String description, Object input, int expected) {
        assertEquals(expected, Converter.convert2int(input), description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("dateTimeProvider")
    @DisplayName("Date/Time conversions")
    void testDateTimeConversions(String description, Object input, int expected) {
        assertEquals(expected, Converter.convert2int(input), description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("unsupportedCollectionProvider")
    @DisplayName("Unsupported collection/map conversions")
    void testUnsupportedCollections(String description, Object input, int expected) {
        assertEquals(expected, Converter.convert2int(input), description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("atomicAndBigNumberProvider")
    @DisplayName("Atomic and big‑number conversions")
    void testAtomicAndBigNumberConversions(String description, Object input, int expected) {
        assertEquals(expected, Converter.convert2int(input), description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("edgeOverflowProvider")
    @DisplayName("Edge case overflow handling")
    void testEdgeOverflow(String description, Object input, int expected) {
        assertEquals(expected, Converter.convert2int(input), description);
    }

    @Test
    @DisplayName("Unsupported plain Object throws IllegalArgumentException")
    void testUnsupportedObjectException() {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2int(new Object()), "Converting a plain Object should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("CharBuffer with non‑numeric content throws NumberFormatException")
    void testCharBufferNumberFormatException() {
        CharBuffer cb = CharBuffer.wrap("xyz");
        assertThrows(NumberFormatException.class, () -> Converter.convert2int(cb), "Non‑numeric CharBuffer should cause NumberFormatException");
    }

    @Test
    @DisplayName("Empty ByteBuffer throws BufferUnderflowException")
    void testByteBufferUnderflowException() {
        ByteBuffer bb = ByteBuffer.allocate(0);
        assertThrows(BufferUnderflowException.class, () -> Converter.convert2int(bb), "Empty ByteBuffer should cause BufferUnderflowException");
    }

    @Test
    @DisplayName("UUID string not numeric throws NumberFormatException")
    void testUuidStringNumberFormatException() {
        String uuidStr = UUID.randomUUID().toString();
        assertThrows(NumberFormatException.class, () -> Converter.convert2int(uuidStr), "UUID string should not be parsable as a number");
    }

    @Test
    @DisplayName("Malformed ISO‑8601 date string throws DateTimeParseException")
    void testMalformedIsoDateException() {
        String badIso = "2023-13-40T99:99:99";
        assertThrows(DateTimeParseException.class, () -> Converter.convert2int(badIso), "Malformed ISO‑8601 string should propagate DateTimeParseException");
    }

    @Test
    @DisplayName("Wrapping null CharSequence in CharBuffer throws NullPointerException")
    void testNullCharBufferThrowsNPE() {
        assertThrows(NullPointerException.class, () -> {
            // CharBuffer.wrap(null) throws NPE before we even call convert2int
            CharBuffer cb = CharBuffer.wrap((CharSequence) null);
            Converter.convert2int(cb);
        }, "CharBuffer.wrap(null) should throw NullPointerException");
    }
}
