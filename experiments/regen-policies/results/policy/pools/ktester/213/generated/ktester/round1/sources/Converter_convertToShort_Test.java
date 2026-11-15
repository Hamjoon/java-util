package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;

class Converter_convertToShort_Test {

    static Stream<Arguments> numericWrapperProvider() {
        return Stream.of(// Byte
        Arguments.of(Byte.valueOf((byte) 5), Short.valueOf((short) 5)), // Integer within range
        Arguments.of(Integer.valueOf(12345), Short.valueOf((short) 12345)), // Integer equal to Short.MAX_VALUE
        Arguments.of(Integer.valueOf(32767), Short.valueOf((short) 32767)), // Integer equal to Short.MIN_VALUE
        Arguments.of(Integer.valueOf(-32768), Short.valueOf((short) -32768)), // Integer overflow
        Arguments.of(Integer.valueOf(40000), IllegalArgumentException.class), // Long far outside range
        Arguments.of(Long.valueOf(1234567890123L), IllegalArgumentException.class), // Float truncation
        Arguments.of(Float.valueOf(12.34f), Short.valueOf((short) 12)), // Double truncation
        Arguments.of(Double.valueOf(56.78), Short.valueOf((short) 56)));
    }

    static Stream<Arguments> bigNumberProvider() {
        return Stream.of(Arguments.of(new BigDecimal("123.99"), Short.valueOf((short) 123)), Arguments.of(new BigInteger("32767"), Short.valueOf((short) 32767)), Arguments.of(new BigInteger("-32768"), Short.valueOf((short) -32768)), Arguments.of(new BigInteger("40000"), IllegalArgumentException.class));
    }

    static Stream<Arguments> stringNumericProvider() {
        return Stream.of(Arguments.of("123", Short.valueOf((short) 123)), Arguments.of("-32768", Short.valueOf((short) -32768)), Arguments.of("32767", Short.valueOf((short) 32767)), Arguments.of("40000", IllegalArgumentException.class), Arguments.of("12.34", Short.valueOf((short) 12)), Arguments.of("notANumber", NumberFormatException.class));
    }

    static Stream<Arguments> booleanProvider() {
        return Stream.of(Arguments.of(Boolean.TRUE, Short.valueOf((short) 1)), Arguments.of(Boolean.FALSE, Short.valueOf((short) 0)));
    }

    static Stream<Arguments> characterProvider() {
        return Stream.of(Arguments.of('A', Short.valueOf((short) 65)), // '\uFFFF' = 65535 -> overflows Short, expect IllegalArgumentException
        Arguments.of('\uFFFF', IllegalArgumentException.class));
    }

    static Stream<Arguments> atomicProvider() {
        return Stream.of(Arguments.of(new AtomicInteger(12345), Short.valueOf((short) 12345)), Arguments.of(new AtomicLong(32767L), Short.valueOf((short) 32767)), Arguments.of(new AtomicBoolean(true), Short.valueOf((short) 1)), Arguments.of(new AtomicLong(40000L), IllegalArgumentException.class));
    }

    static Stream<Arguments> unsupportedTypeProvider() {
        return Stream.of(Arguments.of(new Date(), IllegalArgumentException.class), Arguments.of(UUID.randomUUID(), IllegalArgumentException.class));
    }

    static Stream<Arguments> temporalProvider() {
        return Stream.of(Arguments.of(LocalDate.now()), Arguments.of(LocalDateTime.now()), Arguments.of(ZonedDateTime.now()), Arguments.of(Instant.now()), Arguments.of(Calendar.getInstance()));
    }

    static Stream<Arguments> bufferProvider() {
        ByteBuffer bb = ByteBuffer.allocate(2).putShort((short) 123);
        bb.flip();
        CharBuffer cb = CharBuffer.wrap(new char[] { '1', '2', '3' });
        return Stream.of(Arguments.of(bb), Arguments.of(cb));
    }

    static Stream<Arguments> enumProvider() {
        return Stream.of(Arguments.of(DayOfWeek.MONDAY), Arguments.of(Month.JANUARY));
    }

    static Stream<Arguments> stringEdgeCaseProvider() {
        return Stream.of(// Whitespace is not trimmed according to the JSON spec
        Arguments.of(" 123 ", NumberFormatException.class), // Explicit plus sign within range
        Arguments.of("+32767", Short.valueOf((short) 32767)), // Below Short.MIN_VALUE
        Arguments.of("-32769", IllegalArgumentException.class));
    }

    /* --------------------------------------------------------------------- *
     *  Helper method to avoid duplication in parameterised tests
     * --------------------------------------------------------------------- */
    private void executeConversionAndAssert(Object input, Object expected) {
        if (expected instanceof Class) {
            // Expected an exception
            assertThrows((Class<? extends Throwable>) expected, () -> Converter.convertToShort(input), () -> "Expected exception " + ((Class<?>) expected).getSimpleName() + " for input: " + input);
        } else {
            // Expected a Short value
            Short result = Converter.convertToShort(input);
            assertEquals((Short) expected, result, () -> "Expected Short value " + expected + " but got " + result + " for input: " + input);
        }
    }

    @Test
    void convertNullReturnsNull() {
        Short result = Converter.convertToShort(null);
        assertNull(result, "Converting null should return null for object target types");
    }

    @Test
    void convertShortReturnsSameValue() {
        Short source = 12;
        Short result = Converter.convertToShort(source);
        assertEquals(source, result);
    }

    @Test
    void convertIntegerToShort() {
        Integer source = 123;
        Short result = Converter.convertToShort(source);
        assertEquals(Short.valueOf(source.shortValue()), result);
    }

    @Test
    void convertLongToShort() {
        Long source = 321L;
        Short result = Converter.convertToShort(source);
        assertEquals(Short.valueOf(source.shortValue()), result);
    }

    @Test
    void convertStringNumberToShort() {
        String source = "45";
        Short result = Converter.convertToShort(source);
        assertEquals(Short.valueOf((short) 45), result);
    }

    @Test
    void convertStringWithWhitespaceToShort() {
        String source = "   7   ";
        Short result = Converter.convertToShort(source);
        assertEquals(Short.valueOf((short) 7), result);
    }

    @Test
    void convertBooleanTrueToShort() {
        Boolean source = Boolean.TRUE;
        Short result = Converter.convertToShort(source);
        assertEquals(Short.valueOf((short) 1), result);
    }

    @Test
    void convertBooleanFalseToShort() {
        Boolean source = Boolean.FALSE;
        Short result = Converter.convertToShort(source);
        assertEquals(Short.valueOf((short) 0), result);
    }

    @Test
    void convertBigDecimalToShort() {
        Short result = Converter.convertToShort(new java.math.BigDecimal("123"));
        assertEquals(Short.valueOf((short) 123), result);
    }

    @Test
    void convertBigIntegerToShort() {
        Short result = Converter.convertToShort(new java.math.BigInteger("256"));
        assertEquals(Short.valueOf((short) 256), result);
    }

    @Test
    void convertByteToShort() {
        Byte source = 9;
        Short result = Converter.convertToShort(source);
        assertEquals(Short.valueOf((short) 9), result);
    }

    @Test
    void convertCharDigitToShort() {
        Character source = '5';
        Short result = Converter.convertToShort(source);
        // Char '5' is not a numeric value for the converter; it should be treated as its Unicode code point.
        assertEquals(Short.valueOf((short) source.charValue()), result);
    }

    @Test
    void convertOverflowingIntegerClampsToShortRange() {
        // exceeds Short.MAX_VALUE (32767)
        Integer source = 40000;
        Short result = Converter.convertToShort(source);
        // The converter follows Java's narrowing conversion rules (truncates high bits)
        assertEquals(Short.valueOf(source.shortValue()), result);
    }

    @Test
    void convertNegativeOverflowInteger() {
        // below Short.MIN_VALUE (-32768)
        Integer source = -40000;
        Short result = Converter.convertToShort(source);
        assertEquals(Short.valueOf(source.shortValue()), result);
    }

    @Test
    void convertUnsupportedTypeReturnsNull() {
        // no conversion rule to Short
        Object source = new Object();
        Short result = Converter.convertToShort(source);
        assertNull(result, "Unsupported source types should result in null for object target types");
    }

    // ---------- Numeric wrapper types ----------
    @ParameterizedTest(name = "[{index}] {0} -> {1}")
    @MethodSource("numericWrapperProvider")
    @DisplayName("Numeric wrapper type conversions")
    void testNumericWrapperConversion(Object input, Object expected) {
        executeConversionAndAssert(input, expected);
    }

    // ---------- Big number conversion ----------
    @ParameterizedTest(name = "[{index}] {0} -> {1}")
    @MethodSource("bigNumberProvider")
    @DisplayName("BigDecimal / BigInteger conversions")
    void testBigNumberConversion(Object input, Object expected) {
        executeConversionAndAssert(input, expected);
    }

    // ---------- String numeric conversion ----------
    @ParameterizedTest(name = "[{index}] \"{0}\" -> {1}")
    @MethodSource("stringNumericProvider")
    @DisplayName("String to Short conversions")
    void testStringNumericConversion(String input, Object expected) {
        executeConversionAndAssert(input, expected);
    }

    // ---------- Boolean conversion ----------
    @ParameterizedTest(name = "[{index}] {0} -> {1}")
    @MethodSource("booleanProvider")
    @DisplayName("Boolean to Short conversions")
    void testBooleanConversion(Boolean input, Object expected) {
        executeConversionAndAssert(input, expected);
    }

    // ---------- Character conversion ----------
    @ParameterizedTest(name = "[{index}] '{0}' -> {1}")
    @MethodSource("characterProvider")
    @DisplayName("Character to Short conversions")
    void testCharacterConversion(Character input, Object expected) {
        executeConversionAndAssert(input, expected);
    }

    // ---------- Atomic type conversion ----------
    @ParameterizedTest(name = "[{index}] {0} -> {1}")
    @MethodSource("atomicProvider")
    @DisplayName("Atomic types to Short conversions")
    void testAtomicConversion(Object input, Object expected) {
        executeConversionAndAssert(input, expected);
    }

    // ---------- Unsupported type handling ----------
    @ParameterizedTest(name = "[{index}] {0} -> {1}")
    @MethodSource("unsupportedTypeProvider")
    @DisplayName("Unsupported source types")
    void testUnsupportedTypes(Object input, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> Converter.convertToShort(input));
    }

    // ---------- Unsupported temporal type conversion ----------
    @ParameterizedTest(name = "[{index}] {0} -> IllegalArgumentException")
    @MethodSource("temporalProvider")
    @DisplayName("Temporal types not convertible to Short")
    void testTemporalConversions(Object input) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToShort(input));
    }

    // ---------- Unsupported buffer conversion ----------
    @ParameterizedTest(name = "[{index}] {0} -> IllegalArgumentException")
    @MethodSource("bufferProvider")
    @DisplayName("Buffer types not convertible to Short")
    void testBufferConversions(Object input) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToShort(input));
    }

    // ---------- Enum conversion ----------
    @ParameterizedTest(name = "[{index}] {0} -> NumberFormatException")
    @MethodSource("enumProvider")
    @DisplayName("Enum constants conversion")
    void testEnumConversions(Object input) {
        assertThrows(NumberFormatException.class, () -> Converter.convertToShort(input));
    }

    // ---------- String format edge cases ----------
    @ParameterizedTest(name = "[{index}] \"{0}\" -> {1}")
    @MethodSource("stringEdgeCaseProvider")
    @DisplayName("String format edge cases")
    void testStringEdgeCases(String input, Object expected) {
        executeConversionAndAssert(input, expected);
    }
}
