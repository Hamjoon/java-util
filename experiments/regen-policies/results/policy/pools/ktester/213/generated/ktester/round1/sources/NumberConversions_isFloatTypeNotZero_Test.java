package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

class NumberConversions_isFloatTypeNotZero_Test {

    private static Converter converter;

    private static Converter defaultConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // The real Converter with default options – used for the majority of cases.
        defaultConverter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static resources if needed (none for this test).
        converter = null;
    }

    private static Stream<Arguments> zeroValuesProvider() {
        return Stream.of(Arguments.of(0.0, false, "Double zero should be converted to 0.0 and the method returns false."), Arguments.of(0, false, "Integer zero is converted to double 0.0, resulting in false."), Arguments.of("0.0", false, "String \"0.0\" is parsed as double 0.0, so the method returns false."), Arguments.of(new BigDecimal("0"), false, "BigDecimal zero converts to double 0.0, yielding false."), Arguments.of(Float.valueOf(0f), false, "Float zero (auto‑boxed to Float) converts to double 0.0, result is false."), Arguments.of(null, false, "Null input is treated as zero by toDouble (returns 0.0), so false is returned."));
    }

    private static Stream<Arguments> nonZeroNumericPrimitivesProvider() {
        return Stream.of(Arguments.of(3.14, true, "Positive double value should be converted to itself and return true."), Arguments.of(-2.718, true, "Negative double value should be converted to itself and return true."), Arguments.of(1e10, true, "Large double expressed in scientific notation should be non‑zero."), Arguments.of(5, true, "Positive integer is converted to double 5.0, which is non‑zero."), Arguments.of(-7, true, "Negative integer is converted to double -7.0, which is non‑zero."), Arguments.of(0.0, false, "Explicit double zero (duplicate of existing test) – kept for completeness of boundary set."));
    }

    private static Stream<Arguments> nonZeroBoxedNumericProvider() {
        return Stream.of(Arguments.of(Float.valueOf(2.5f), true, "Boxed Float 2.5 converts to double 2.5, method returns true."), Arguments.of(Float.valueOf(-3.3f), true, "Boxed Float -3.3 converts to double -3.3, method returns true."), Arguments.of(Long.valueOf(1234567890123L), true, "Boxed Long with large positive value converts to non‑zero double."), Arguments.of(new AtomicInteger(42), true, "AtomicInteger holding 42 is converted to double 42.0, returns true."), Arguments.of(new AtomicLong(-99L), true, "AtomicLong holding -99 converts to double -99.0, returns true."));
    }

    private static Stream<Arguments> stringRepresentationsProvider() {
        return Stream.of(Arguments.of("123.456", true, "String containing a positive decimal number is parsed to non‑zero double."), Arguments.of("-0.001", true, "String with a small negative number parses to non‑zero double."), Arguments.of("1e-9", true, "String in scientific notation representing a tiny non‑zero value."), Arguments.of("NaN", true, "String \"NaN\" is parsed to Double.NaN; NaN != 0 yields true."), Arguments.of("Infinity", true, "String \"Infinity\" parses to Double.POSITIVE_INFINITY, which is non‑zero."));
    }

    private static Stream<Arguments> bigDecimalAndBigIntegerProvider() {
        return Stream.of(Arguments.of(new BigDecimal("0.0001"), true, "Non‑zero BigDecimal converts to a small non‑zero double."), Arguments.of(new BigDecimal("-987654321.12345"), true, "Large negative BigDecimal is non‑zero after conversion."), Arguments.of(new BigInteger("12345678901234567890"), true, "BigInteger with a large magnitude converts to a non‑zero double."), Arguments.of(new BigInteger("-1"), true, "Negative BigInteger converts to non‑zero double."));
    }

    private static Stream<Arguments> specialFloatingPointProvider() {
        return Stream.of(Arguments.of(-0.0, false, "Negative zero is considered equal to 0.0; method should return false."), Arguments.of(Double.valueOf(Double.NaN), true, "Boxed Double.NaN results in true because NaN != 0."), Arguments.of(Double.valueOf(Double.POSITIVE_INFINITY), true, "Boxed Double.POSITIVE_INFINITY is non‑zero."), Arguments.of(Double.valueOf(Double.NEGATIVE_INFINITY), true, "Boxed Double.NEGATIVE_INFINITY is non‑zero."));
    }

    private static Stream<Arguments> unsupportedTypesProvider() {
        return Stream.of(Arguments.of(Boolean.TRUE, false, "Boolean true cannot be converted to a number; toDouble returns 0.0, so method returns false."), Arguments.of(Date.from(Instant.parse("2023-01-01T00:00:00Z")), false, "Date object is not a numeric type; conversion yields 0.0, resulting in false."), Arguments.of(new Object(), false, "Arbitrary Object cannot be interpreted as a number; toDouble returns 0.0."));
    }

    @Test
    void returnsTrueWhenNonZeroInteger() {
        assertTrue(NumberConversions.isFloatTypeNotZero(5, defaultConverter), "Integer 5 should be considered non‑zero.");
    }

    @Test
    void returnsFalseWhenZeroInteger() {
        assertFalse(NumberConversions.isFloatTypeNotZero(0, defaultConverter), "Integer 0 should be considered zero.");
    }

    @Test
    void returnsTrueWhenNonZeroString() {
        assertTrue(NumberConversions.isFloatTypeNotZero("3.14", defaultConverter), "String \"3.14\" should be converted to a non‑zero double.");
    }

    @Test
    void returnsFalseWhenZeroString() {
        assertFalse(NumberConversions.isFloatTypeNotZero("0", defaultConverter), "String \"0\" should be converted to zero.");
    }

    @Test
    void returnsFalseWhenNull() {
        assertFalse(NumberConversions.isFloatTypeNotZero(null, defaultConverter), "Null should be treated as zero by the Converter.");
    }

    @Test
    void returnsTrueWhenNonZeroDouble() {
        assertTrue(NumberConversions.isFloatTypeNotZero(0.0001d, defaultConverter), "A small non‑zero double should be considered non‑zero.");
    }

    @Test
    void returnsFalseWhenZeroDouble() {
        assertFalse(NumberConversions.isFloatTypeNotZero(0.0d, defaultConverter), "Zero double should be considered zero.");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("zeroValuesProvider")
    void testZeroValues(Object from, boolean expected, String description) {
        assertEquals(expected, NumberConversions.isFloatTypeNotZero(from, defaultConverter), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("nonZeroNumericPrimitivesProvider")
    void testNonZeroNumericPrimitives(Object from, boolean expected, String description) {
        assertEquals(expected, NumberConversions.isFloatTypeNotZero(from, defaultConverter), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("nonZeroBoxedNumericProvider")
    void testNonZeroBoxedNumeric(Object from, boolean expected, String description) {
        assertEquals(expected, NumberConversions.isFloatTypeNotZero(from, defaultConverter), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("stringRepresentationsProvider")
    void testStringRepresentations(String from, boolean expected, String description) {
        assertEquals(expected, NumberConversions.isFloatTypeNotZero(from, defaultConverter), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("bigDecimalAndBigIntegerProvider")
    void testBigDecimalAndBigInteger(Object from, boolean expected, String description) {
        assertEquals(expected, NumberConversions.isFloatTypeNotZero(from, defaultConverter), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("specialFloatingPointProvider")
    void testSpecialFloatingPoint(Object from, boolean expected, String description) {
        assertEquals(expected, NumberConversions.isFloatTypeNotZero(from, defaultConverter), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("unsupportedTypesProvider")
    void testUnsupportedTypes(Object from, boolean expected, String description) {
        assertEquals(expected, NumberConversions.isFloatTypeNotZero(from, defaultConverter), description);
    }

    @Test
    void nullConverterWithNonZeroNumber() {
        // When converter is null the implementation should still fall back to a default conversion.
        assertTrue(NumberConversions.isFloatTypeNotZero(9.99, null), "When converter is null, the method should fall back to default conversion and return true for non‑zero double.");
    }

    @Test
    void nullConverterWithZeroNumber() {
        assertFalse(NumberConversions.isFloatTypeNotZero(0, null), "Null converter with zero input still results in false.");
    }

    @Test
    void parsingErrorAlphabeticString() {
        assertThrows(NumberFormatException.class, () -> NumberConversions.isFloatTypeNotZero("abc", defaultConverter), "String that cannot be parsed as a number should cause NumberFormatException.");
    }

    @Test
    void parsingErrorAlphanumericString() {
        assertThrows(NumberFormatException.class, () -> NumberConversions.isFloatTypeNotZero("123abc", defaultConverter), "Alphanumeric string should trigger NumberFormatException.");
    }

    @Test
    void converterThrowsRuntimeException() {
        // Mockito mock that throws RuntimeException for *any* method invocation.
        Converter throwingConverter = Mockito.mock(Converter.class, invocation -> {
            throw new RuntimeException("forced failure");
        });
        RuntimeException ex = assertThrows(RuntimeException.class, () -> NumberConversions.isFloatTypeNotZero(5, throwingConverter), "Converter that throws RuntimeException during conversion should propagate the exception.");
        assertEquals("forced failure", ex.getMessage());
    }

    @Test
    void characterInputThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.isFloatTypeNotZero('a', defaultConverter), "Passing a Character should cause IllegalArgumentException as it cannot be converted to double.");
    }
}
