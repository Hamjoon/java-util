package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convert2long_Test {

    static Stream<Arguments> nullInputProvider() {
        return Stream.of(Arguments.of("When the input is null the method must return CommonValues.LONG_ZERO (0L)", null, 0L));
    }

    static Stream<Arguments> stringToLongProvider() {
        return Stream.of(Arguments.of("A numeric string should be parsed and returned as the corresponding long value.", "12345", 12345L), Arguments.of("A negative numeric string should be parsed correctly.", "-987", -987L));
    }

    static Stream<Arguments> wrapperConversionProvider() {
        return Stream.of(Arguments.of("Integer → long", Integer.valueOf(42), 42L), Arguments.of("Double → long (truncation)", Double.valueOf(3.14159), 3L), Arguments.of("Long → long (unchanged)", Long.valueOf(1234567890123456789L), 1234567890123456789L), Arguments.of("Byte → long", Byte.valueOf((byte) 127), 127L), Arguments.of("Short → long", Short.valueOf((short) 32767), 32767L));
    }

    static Stream<Arguments> bigNumberProvider() {
        return Stream.of(// BigInteger represented as a String (fits into long)
        Arguments.of("BigInteger (as String) fitting in long", "123456789012345678901234567890", 123456789012345678901234567890L), // BigDecimal via Double (truncation)
        Arguments.of("BigDecimal (via Double) truncates", Double.valueOf(9999.99), 9999L));
    }

    static Stream<Arguments> booleanProvider() {
        return Stream.of(Arguments.of("Boolean true → 1", true, 1L), Arguments.of("Boolean false → 0", false, 0L));
    }

    static Stream<Arguments> characterProvider() {
        return Stream.of(Arguments.of("Digit character '7' → 7", '7', 7L), Arguments.of("Non‑digit character 'a' → exception", 'a', IllegalArgumentException.class));
    }

    static Stream<Arguments> atomicProvider() {
        return Stream.of(Arguments.of("AtomicLong unwrapped", new AtomicLong(1234567890L), 1234567890L), Arguments.of("AtomicInteger widened", new AtomicInteger(-42), -42L), Arguments.of("AtomicBoolean true → 1", new AtomicBoolean(true), 1L), Arguments.of("AtomicBoolean false → 0", new AtomicBoolean(false), 0L));
    }

    static Stream<Arguments> dateTimeProvider() {
        return Stream.of(Arguments.of("java.util.Date → epoch millis", new Date(1625097600000L), 1625097600000L), Arguments.of("Instant → epoch millis", Instant.ofEpochSecond(1625097600L, 0), 1625097600000L), Arguments.of("LocalDate → days since epoch (2021‑07‑01)", LocalDate.of(2021, 7, 1), 18720L), Arguments.of("LocalDateTime → epoch millis (system default zone)", LocalDateTime.of(2021, 7, 1, 12, 34, 56), LocalDateTime.of(2021, 7, 1, 12, 34, 56).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()), Arguments.of("ZonedDateTime → epoch millis (Europe/Paris)", ZonedDateTime.parse("2021-07-01T12:34:56+02:00[Europe/Paris]"), ZonedDateTime.parse("2021-07-01T12:34:56+02:00[Europe/Paris]").toInstant().toEpochMilli()), Arguments.of("java.sql.Timestamp → epoch millis", new Timestamp(1625097600000L), 1625097600000L));
    }

    static Stream<Arguments> bigDirectProvider() {
        return Stream.of(Arguments.of("BigInteger fitting in long", new BigInteger("12345678901234567890"), 12345678901234567890L), Arguments.of("BigDecimal truncates", new BigDecimal("98765.4321"), 98765L));
    }

    static Stream<Arguments> stringEdgeProvider() {
        return Stream.of(Arguments.of("Empty string → exception", "", IllegalArgumentException.class), Arguments.of("String with whitespace trimmed", "   42   ", 42L), Arguments.of("Decimal string → exception", "123.99", IllegalArgumentException.class), Arguments.of("Long.MAX_VALUE string", "9223372036854775807", 9223372036854775807L), Arguments.of("Overflow string → exception", "9223372036854775808", IllegalArgumentException.class), Arguments.of("Scientific notation → exception", "1e3", IllegalArgumentException.class), Arguments.of("Whitespace‑only string → exception", "   ", IllegalArgumentException.class), Arguments.of("Hex string → exception", "0x1A", IllegalArgumentException.class));
    }

    static Stream<Arguments> numericPrimitiveProvider() {
        return Stream.of(Arguments.of("Long.MAX_VALUE as Long", Long.MAX_VALUE, Long.MAX_VALUE), Arguments.of("Long.MIN_VALUE as Long", Long.MIN_VALUE, Long.MIN_VALUE), Arguments.of("Positive double truncates toward zero", 3.9999, 3L), Arguments.of("Negative double truncates toward zero", -3.9999, -3L), Arguments.of("Float literal (as Double) truncates", 127.0, 127L));
    }

    static Stream<Arguments> bufferProvider() {
        return Stream.of(Arguments.of("CharBuffer with numeric content", CharBuffer.wrap("4567"), 4567L), Arguments.of("ByteBuffer with ASCII digits", ByteBuffer.wrap(new byte[] { 49, 50, 51 }), 123L), Arguments.of("Non‑numeric CharBuffer → exception", CharBuffer.wrap("12a3"), IllegalArgumentException.class), Arguments.of("Non‑numeric ByteBuffer → exception", ByteBuffer.wrap(new byte[] { 49, 50, 51, 65 }), IllegalArgumentException.class), Arguments.of("Empty CharBuffer → exception", CharBuffer.wrap(""), IllegalArgumentException.class), Arguments.of("Empty ByteBuffer → exception", ByteBuffer.wrap(new byte[] {}), IllegalArgumentException.class));
    }

    @Test
    void convert2long_NullInput_ReturnsZero() {
        long result = Converter.convert2long(null);
        assertEquals(0L, result, "Null input should result in LONG_ZERO (0L)");
    }

    @Test
    void convert2long_LongInput_ReturnsSameValue() {
        Long source = 12345L;
        long result = Converter.convert2long(source);
        assertEquals(source.longValue(), result);
    }

    @Test
    void convert2long_StringInput_ReturnsParsedLong() {
        String source = "67890";
        long result = Converter.convert2long(source);
        assertEquals(67890L, result);
    }

    @Test
    void convert2long_IntegerInput_ReturnsLongValue() {
        Integer source = 42;
        long result = Converter.convert2long(source);
        assertEquals(42L, result);
    }

    @Test
    void convert2long_DateInput_ReturnsEpochMillis() {
        // arbitrary timestamp
        long epochMillis = 1_600_000_000_000L;
        Date source = new Date(epochMillis);
        long result = Converter.convert2long(source);
        assertEquals(epochMillis, result);
    }

    @Test
    void convert2long_CalendarInput_ReturnsEpochMillis() {
        long epochMillis = 1_610_000_000_000L;
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(epochMillis);
        long result = Converter.convert2long(cal);
        assertEquals(epochMillis, result);
    }

    // ---------- Null Input ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullInputProvider")
    void convert2long_NullInput(String description, Object input, long expected) {
        long result = Converter.convert2long(input);
        assertEquals(expected, result);
    }

    // ---------- String to long conversion ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("stringToLongProvider")
    void convert2long_StringInput(String description, String input, long expected) {
        long result = Converter.convert2long(input);
        assertEquals(expected, result);
    }

    // ---------- Primitive wrapper conversions ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("wrapperConversionProvider")
    void convert2long_WrapperInput(String description, Object input, long expected) {
        long result = Converter.convert2long(input);
        assertEquals(expected, result);
    }

    // ---------- BigInteger and BigDecimal conversions ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("bigNumberProvider")
    void convert2long_BigNumberInput(String description, Object input, long expected) {
        long result = Converter.convert2long(input);
        assertEquals(expected, result);
    }

    // ---------- Unsupported type conversion ----------
    @Test
    void convert2long_UnsupportedObject_ThrowsIllegalArgumentException() {
        Object unsupported = new Object();
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2long(unsupported));
    }

    // ---------- Boolean to long conversion ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("booleanProvider")
    void convert2long_BooleanInput(String description, Boolean input, long expected) {
        long result = Converter.convert2long(input);
        assertEquals(expected, result);
    }

    // ---------- Character conversion ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("characterProvider")
    void convert2long_CharacterInput(String description, Character input, Object expected) {
        if (expected instanceof Long) {
            long result = Converter.convert2long(input);
            assertEquals(expected, result);
        } else {
            assertThrows(IllegalArgumentException.class, () -> Converter.convert2long(input));
        }
    }

    // ---------- Atomic type conversions ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("atomicProvider")
    void convert2long_AtomicInput(String description, Object input, long expected) {
        long result = Converter.convert2long(input);
        assertEquals(expected, result);
    }

    // ---------- Date and time conversions ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("dateTimeProvider")
    void convert2long_DateTimeInput(String description, Object input, long expected) {
        long result = Converter.convert2long(input);
        assertEquals(expected, result);
    }

    // ---------- BigInteger and BigDecimal direct instances ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("bigDirectProvider")
    void convert2long_BigDirectInstances(String description, Object input, Object expected) {
        if (expected instanceof Long) {
            long result = Converter.convert2long(input);
            assertEquals(expected, result);
        } else {
            assertThrows(IllegalArgumentException.class, () -> Converter.convert2long(input));
        }
    }

    // ---------- String edge cases ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("stringEdgeProvider")
    void convert2long_StringEdgeCases(String description, String input, Object expected) {
        if (expected instanceof Long) {
            long result = Converter.convert2long(input);
            assertEquals(expected, result);
        } else {
            assertThrows(IllegalArgumentException.class, () -> Converter.convert2long(input));
        }
    }

    // ---------- Numeric primitive edge cases ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("numericPrimitiveProvider")
    void convert2long_NumericPrimitiveEdge(String description, Object input, long expected) {
        long result = Converter.convert2long(input);
        assertEquals(expected, result);
    }

    // ---------- Buffer based conversions ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("bufferProvider")
    void convert2long_BufferInput(String description, Object input, Object expected) {
        if (expected instanceof Long) {
            long result = Converter.convert2long(input);
            assertEquals(expected, result);
        } else {
            assertThrows(IllegalArgumentException.class, () -> Converter.convert2long(input));
        }
    }

    // ---------- Unsupported collection types ----------
    @Test
    void convert2long_ListInput_ThrowsIllegalArgumentException() {
        List<Integer> list = Arrays.asList(1, 2, 3);
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2long(list));
    }

    // ---------- BigInteger overflow ----------
    @Test
    void convert2long_BigIntegerOverflow_ThrowsIllegalArgumentException() {
        // Long.MAX_VALUE + 1
        BigInteger overflow = new BigInteger("9223372036854775808");
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2long(overflow));
    }

    // ---------- BigDecimal overflow ----------
    @Test
    void convert2long_BigDecimalOverflow_ThrowsIllegalArgumentException() {
        BigDecimal overflow = new BigDecimal("9223372036854775808");
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2long(overflow));
    }

    // ---------- Unsupported UUID conversion ----------
    @Test
    void convert2long_UUIDInput_ThrowsIllegalArgumentException() {
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2long(uuid));
    }

    @Test
    void convert2long_EnumInput_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2long(TestEnum.SOME_CONSTANT));
    }

    // ---------- StringBuilder unsupported conversion ----------
    @Test
    void convert2long_StringBuilderInput_ThrowsIllegalArgumentException() {
        StringBuilder sb = new StringBuilder("12345");
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2long(sb));
    }
}
