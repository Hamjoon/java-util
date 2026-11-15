package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.time.DayOfWeek;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

class Converter_convert2short_Test {

    private static Stream<Arguments> validNumericProvider() {
        return Stream.of(Arguments.of(Integer.valueOf(123), (short) 123), Arguments.of(Short.valueOf((short) -200), (short) -200), Arguments.of(Long.valueOf(12345L), (short) 12345), Arguments.of(Double.valueOf(12.34), (short) 12), Arguments.of(Float.valueOf(-12.3f), (short) -12), Arguments.of(new BigDecimal("12345.67"), (short) 12345), // overflow wrap
        Arguments.of(new BigInteger("123456789"), (short) -13035), Arguments.of("45", (short) 45), Arguments.of("-123", (short) -123), Arguments.of("7", (short) 7), Arguments.of("-15", (short) -15), // whitespace trimmed by Converter
        Arguments.of(" 42 ", (short) 42), Arguments.of(Boolean.TRUE, (short) 1), Arguments.of(Boolean.FALSE, (short) 0), // char literal
        Arguments.of('A', (short) 'A'), Arguments.of(Character.valueOf('z'), (short) 'z'), Arguments.of(Byte.valueOf((byte) 5), (short) 5), Arguments.of(AtomicInteger.valueOf(7), (short) 7), // overflow wrap
        Arguments.of(AtomicLong.valueOf(40000L), (short) -25536));
    }

    private static Stream<Arguments> overflowNumericProvider() {
        return Stream.of(// just above Short.MAX_VALUE
        Arguments.of(Integer.valueOf(32768), (short) -32768), // 0xFFFF
        Arguments.of(Integer.valueOf(65535), (short) -1), // just below Short.MIN_VALUE
        Arguments.of("-32769", (short) 32767), // -2*Short.MAX_VALUE-2
        Arguments.of("-65536", (short) 0), // negative double overflow
        Arguments.of(-40000.0, (short) 25536));
    }

    private static Stream<Arguments> stringParsingProvider() {
        return Stream.of(Arguments.of("+123", (short) 123), Arguments.of("0010", (short) 10));
    }

    private static Stream<Arguments> invalidStringProvider() {
        return Stream.of(Arguments.of("", IllegalArgumentException.class), Arguments.of("   ", IllegalArgumentException.class), Arguments.of("0x10", IllegalArgumentException.class), Arguments.of("abc", IllegalArgumentException.class), Arguments.of("12.34", IllegalArgumentException.class), Arguments.of("1e3", IllegalArgumentException.class), // leading whitespace not trimmed per spec
        Arguments.of(" 123", IllegalArgumentException.class));
    }

    private static Stream<Arguments> additionalTypesProvider() {
        return Stream.of(Arguments.of(Byte.valueOf((byte) 5), (short) 5), Arguments.of(Short.valueOf((short) -200), (short) -200), Arguments.of(Long.valueOf(12345L), (short) 12345), Arguments.of(new BigInteger("123456789"), (short) -13035), Arguments.of(new BigDecimal("12345.67"), (short) 12345), Arguments.of(Character.valueOf('z'), (short) 'z'), // single‑char buffer
        Arguments.of(CharBuffer.wrap("d"), (short) 'd'), Arguments.of(AtomicInteger.valueOf(7), (short) 7), Arguments.of(AtomicLong.valueOf(40000L), (short) -25536), Arguments.of(Float.valueOf(123.99f), (short) 123));
    }

    private static Stream<Arguments> unsupportedTypesProvider() {
        return Stream.of(Arguments.of(new Object()), Arguments.of(new Date()), Arguments.of(Instant.now()), Arguments.of(UUID.randomUUID()), Arguments.of(Arrays.asList(1, 2, 3)), Arguments.of(DayOfWeek.MONDAY), // more than one character
        Arguments.of(CharBuffer.wrap("ab")), Arguments.of(ByteBuffer.wrap(new byte[] { 1, 2, 3 })), Arguments.of(Double.NaN), Arguments.of(Double.POSITIVE_INFINITY), Arguments.of(Float.NaN), Arguments.of(new int[] { 1, 2, 3 }), Arguments.of(new String[] { "1", "2" }), Arguments.of(new StringBuilder("123")));
    }

    @Test
    void convertNullReturnsZero() {
        // GIVEN a null source
        Object source = null;
        // WHEN converting to short
        short result = Converter.convert2short(source);
        // THEN the result is the primitive default 0
        assertEquals(0, result, "null should be converted to 0");
    }

    @Test
    void convertNumberTypes() {
        // Integer
        assertEquals((short) 123, Converter.convert2short(Integer.valueOf(123)));
        // Long (value within short range)
        assertEquals((short) -200, Converter.convert2short(Long.valueOf(-200L)));
        // Double (fractional part is truncated)
        assertEquals((short) 45, Converter.convert2short(Double.valueOf(45.99)));
        // Float
        assertEquals((short) -12, Converter.convert2short(Float.valueOf(-12.3f)));
        // BigDecimal
        assertEquals((short) 32767, Converter.convert2short(new java.math.BigDecimal("32767")));
        // BigInteger
        assertEquals((short) -32768, Converter.convert2short(new java.math.BigInteger("-32768")));
    }

    @Test
    void convertStringRepresentations() {
        // Numeric string
        assertEquals((short) 7, Converter.convert2short("7"));
        // Negative numeric string
        assertEquals((short) -15, Converter.convert2short("-15"));
        // String with whitespace (should be trimmed by the converter)
        assertEquals((short) 42, Converter.convert2short(" 42 "));
    }

    @Test
    void convertBooleanValues() {
        // true should become 1
        assertEquals((short) 1, Converter.convert2short(Boolean.TRUE));
        // false should become 0
        assertEquals((short) 0, Converter.convert2short(Boolean.FALSE));
    }

    @Test
    void convertCharValues() {
        // Character 'A' -> 65
        assertEquals((short) 'A', Converter.convert2short('A'));
        // Character '\u0000' -> 0
        assertEquals((short) 0, Converter.convert2short('\u0000'));
    }

    @Test
    void convertUnsupportedTypeFallsBackToZeroOrThrows() {
        // The Converter throws an IllegalArgumentException when it cannot find a conversion.
        // We verify that behaviour for an obviously unsupported type (e.g., a plain Object).
        Object unsupported = new Object();
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2short(unsupported));
    }

    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @MethodSource("validNumericProvider")
    void convertValidNumeric(Object source, short expected) {
        short actual = Converter.convert2short(source);
        assertEquals(expected, actual, () -> "Conversion of " + source + " should yield " + expected);
    }

    @ParameterizedTest(name = "[{index}] overflow {0} → {1}")
    @MethodSource("overflowNumericProvider")
    void convertOverflowNumeric(Object source, short expected) {
        short actual = Converter.convert2short(source);
        assertEquals(expected, actual, () -> "Overflow conversion of " + source + " should wrap to " + expected);
    }

    @ParameterizedTest(name = "[{index}] string \"{0}\" → {1}")
    @MethodSource("stringParsingProvider")
    void convertStringParsing(Object source, short expected) {
        short actual = Converter.convert2short(source);
        assertEquals(expected, actual, () -> "String parsing of \"" + source + "\" should yield " + expected);
    }

    @ParameterizedTest(name = "[{index}] invalid string \"{0}\" throws {1}")
    @MethodSource("invalidStringProvider")
    void convertInvalidStringThrows(Object source, Class<? extends Throwable> exception) {
        assertThrows(exception, () -> Converter.convert2short(source), () -> "Parsing \"" + source + "\" should throw " + exception.getSimpleName());
    }

    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @MethodSource("additionalTypesProvider")
    void convertAdditionalTypes(Object source, short expected) {
        short actual = Converter.convert2short(source);
        assertEquals(expected, actual, () -> "Conversion of " + source.getClass().getSimpleName() + " should yield " + expected);
    }

    @ParameterizedTest(name = "[{index}] unsupported {0} throws IAE")
    @MethodSource("unsupportedTypesProvider")
    void convertUnsupportedThrows(Object source) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2short(source), () -> "Conversion of " + source.getClass().getSimpleName() + " should throw IllegalArgumentException");
    }
}
