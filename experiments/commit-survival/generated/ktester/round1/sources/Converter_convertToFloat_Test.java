package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigInteger;
import java.time.*;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class Converter_convertToFloat_Test {

    static Stream<Arguments> nullInputProvider() {
        return Stream.of(arguments((Object) null));
    }

    static Stream<Arguments> numericPrimitiveProvider() {
        return Stream.of(// Integer
        arguments(42, 42.0f), // Long (precision loss expected)
        arguments(1234567890123L, 1.2345679E12f), // Double
        arguments(3.14d, 3.14f), // Float (unchanged)
        arguments(5.5f, 5.5f), // Float.MAX_VALUE
        arguments(Float.MAX_VALUE, Float.MAX_VALUE), // Float.MIN_VALUE (positive non‑zero)
        arguments(Float.MIN_VALUE, Float.MIN_VALUE));
    }

    static Stream<Arguments> bigNumberProvider() {
        return Stream.of(arguments(new BigDecimal("2.71828"), 2.71828f), arguments(new BigInteger("100"), 100.0f));
    }

    static Stream<Arguments> stringRepresentationProvider() {
        return Stream.of(arguments("123.45", 123.45f), arguments("   7.89  ", 7.89f), arguments("NaN", Float.NaN), arguments("Infinity", Float.POSITIVE_INFINITY), arguments("-Infinity", Float.NEGATIVE_INFINITY));
    }

    static Stream<Arguments> booleanProvider() {
        return Stream.of(arguments(Boolean.TRUE, 1.0f), arguments(Boolean.FALSE, 0.0f));
    }

    static Stream<Arguments> atomicProvider() {
        return Stream.of(arguments(new AtomicInteger(10), 10.0f), arguments(new AtomicLong(123L), 123.0f));
    }

    static Stream<Arguments> dateTimeProvider() {
        // java.util.Date → milliseconds since epoch
        Date epochDate = new Date(0L);
        // LocalDate → days since epoch (1970‑01‑01)
        LocalDate localDate = LocalDate.of(2023, 1, 1);
        long daysSinceEpoch = ChronoUnit.DAYS.between(LocalDate.ofEpochDay(0), localDate);
        // LocalDateTime → milliseconds since epoch in system default zone
        LocalDateTime ldt = LocalDateTime.of(2023, 1, 1, 12, 34, 56);
        ZonedDateTime zdt = ldt.atZone(ZoneId.systemDefault());
        long epochMilli = zdt.toInstant().toEpochMilli();
        return Stream.of(arguments(epochDate, 0.0f), arguments(localDate, (float) daysSinceEpoch), arguments(ldt, (float) epochMilli));
    }

    static Stream<Arguments> unsupportedProvider() {
        return Stream.of(arguments(new Object()), arguments(new int[] { 1, 2, 3 }));
    }

    static Stream<Arguments> invalidStringProvider() {
        return Stream.of(arguments("abc"), arguments("12abc"), arguments(""), arguments("   "));
    }

    @Test
    @DisplayName("convertToFloat returns the same Float instance")
    void testFloatInput() {
        // given
        Float source = 12.34f;
        // when
        Float result = Converter.convertToFloat(source);
        // then
        assertNotNull(result);
        assertEquals(source, result);
    }

    @Test
    @DisplayName("convertToFloat converts Double to Float")
    void testDoubleInput() {
        // given
        Double source = 56.78;
        // when
        Float result = Converter.convertToFloat(source);
        // then
        assertNotNull(result);
        assertEquals(source.floatValue(), result);
    }

    @Test
    @DisplayName("convertToFloat converts Integer to Float")
    void testIntegerInput() {
        // given
        Integer source = 42;
        // when
        Float result = Converter.convertToFloat(source);
        // then
        assertNotNull(result);
        assertEquals(source.floatValue(), result);
    }

    @Test
    @DisplayName("convertToFloat converts numeric String to Float")
    void testStringInput() {
        // given
        String source = "3.1415";
        // when
        Float result = Converter.convertToFloat(source);
        // then
        assertNotNull(result);
        assertEquals(Float.parseFloat(source), result);
    }

    @Test
    @DisplayName("convertToFloat converts BigDecimal to Float")
    void testBigDecimalInput() {
        // given
        BigDecimal source = new BigDecimal("6.28");
        // when
        Float result = Converter.convertToFloat(source);
        // then
        assertNotNull(result);
        assertEquals(source.floatValue(), result);
    }

    @Test
    @DisplayName("convertToFloat returns null when input is null")
    void testNullInput() {
        // given
        Object source = null;
        // when
        Float result = Converter.convertToFloat(source);
        // then
        assertNull(result);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] Null input should return null")
    @MethodSource("nullInputProvider")
    void testNullInputParameterized(Object input) {
        assertNull(Converter.convertToFloat(input));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @MethodSource("numericPrimitiveProvider")
    void testNumericPrimitives(Object source, Float expected) {
        Float result = Converter.convertToFloat(source);
        assertNotNull(result);
        // Use Float.compare to correctly handle NaN / infinities if they ever appear
        assertEquals(0, Float.compare(expected, result), () -> "Expected " + expected + " but got " + result + " for source " + source);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @MethodSource("bigNumberProvider")
    void testBigNumbers(Object source, Float expected) {
        Float result = Converter.convertToFloat(source);
        assertNotNull(result);
        assertEquals(0, Float.compare(expected, result), () -> "Expected " + expected + " but got " + result);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] \"{0}\" → {1}")
    @MethodSource("stringRepresentationProvider")
    void testStringRepresentations(String source, Float expected) {
        Float result = Converter.convertToFloat(source);
        assertNotNull(result);
        if (Float.isNaN(expected)) {
            assertTrue(Float.isNaN(result), "Expected NaN");
        } else if (Float.isInfinite(expected)) {
            assertEquals(expected, result);
        } else {
            assertEquals(0, Float.compare(expected, result));
        }
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @MethodSource("booleanProvider")
    void testBooleanConversions(Boolean source, Float expected) {
        Float result = Converter.convertToFloat(source);
        assertNotNull(result);
        assertEquals(0, Float.compare(expected, result));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @MethodSource("atomicProvider")
    void testAtomicConversions(Object source, Float expected) {
        Float result = Converter.convertToFloat(source);
        assertNotNull(result);
        assertEquals(0, Float.compare(expected, result));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @MethodSource("dateTimeProvider")
    void testDateTimeConversions(Object source, Float expected) {
        Float result = Converter.convertToFloat(source);
        assertNotNull(result);
        // For very large epoch‑millisecond values the float may lose precision;
        // we therefore compare using a delta that accounts for float rounding.
        float delta = Math.max(1.0f, Math.abs(expected) * 1e-5f);
        assertEquals(expected, result, delta, () -> "Expected " + expected + " but got " + result + " for source " + source);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] Unsupported type {0} should throw IAE")
    @MethodSource("unsupportedProvider")
    void testUnsupportedTypes(Object source) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToFloat(source), "Expected IllegalArgumentException for source: " + source);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] Invalid string \"{0}\" should throw NFE")
    @MethodSource("invalidStringProvider")
    void testInvalidStringFormats(String source) {
        assertThrows(NumberFormatException.class, () -> Converter.convertToFloat(source), "Expected NumberFormatException for source: \"" + source + "\"");
    }
}
