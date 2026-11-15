package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convert2short_Test {

    /**
     * Holds the original singleton instance so we can restore it after the tests.
     */
    private static Converter originalInstance;

    /**
     * Mocked Converter that will replace the singleton during each test.
     */
    private Converter mockInstance;

    @BeforeAll
    static void setupBeforeAll() {
        // Preserve the original instance for later restoration.
        originalInstance = Converter.instance;
    }

    @BeforeEach
    void setupBeforeEach() {
        // By default create a Mockito mock; individual tests can replace it with a real instance.
        mockInstance = mock(Converter.class);
        Converter.instance = mockInstance;
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup needed beyond resetting the mock reference.
        mockInstance = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore the original singleton so other test suites are not affected.
        Converter.instance = originalInstance;
    }

    private static Stream<Arguments> nullAndNonNullProvider() {
        return Stream.of(// Short instance – should be returned unchanged
        Arguments.of(Short.valueOf((short) 123), (short) 123), // Integer that fits in a short
        Arguments.of(Integer.valueOf(456), (short) 456), // Numeric String (trailing/leading spaces are trimmed by the library)
        Arguments.of("789", (short) 789));
    }

    private static Stream<Arguments> numericOverflowProvider() {
        return Stream.of(// (short)40000 == -25536
        Arguments.of(Integer.valueOf(40000), (short) -25536), // (short)100000L == -31072
        Arguments.of(Long.valueOf(100000L), (short) -31072));
    }

    private static Stream<Arguments> booleanProvider() {
        return Stream.of(Arguments.of(Boolean.TRUE, (short) 1), Arguments.of(Boolean.FALSE, (short) 0));
    }

    private static Stream<Arguments> characterProvider() {
        return Stream.of(// 'A' -> 65
        Arguments.of('A', (short) 65), // '\uFFFF' == 65535 -> overflow to -1
        Arguments.of('\uFFFF', (short) -1));
    }

    private static Stream<Arguments> bigDecimalProvider() {
        return Stream.of(// Fractional part is truncated
        Arguments.of(new BigDecimal("123.45"), (short) 123), // Overflow case
        Arguments.of(new BigDecimal("40000"), (short) -25536));
    }

    private static Stream<Arguments> bigIntegerProvider() {
        return Stream.of(Arguments.of(new BigInteger("200"), (short) 200), Arguments.of(new BigInteger("70000"), (short) -25536));
    }

    private static Stream<Arguments> atomicProvider() {
        return Stream.of(Arguments.of(new AtomicInteger(250), (short) 250), // (short)50000 == -15536
        Arguments.of(new AtomicLong(50000L), (short) -15536));
    }

    private static Stream<Arguments> stringVariationsProvider() {
        return Stream.of(Arguments.of("  42  ", (short) 42), Arguments.of("+7", (short) 7), Arguments.of("-8", (short) -8));
    }

    private static Stream<Arguments> unsupportedTypesProvider() {
        return Stream.of(Arguments.of(new java.util.Date()), Arguments.of(java.time.LocalDate.now()), Arguments.of(new java.sql.Timestamp(System.currentTimeMillis())), Arguments.of(new StringBuilder("123")), Arguments.of(new int[] { 1, 2, 3 }));
    }

    @Test
    void convert2short_NullInput_ReturnsLogicalZero() {
        // Given: a null argument
        Object input = null;
        // When: the static conversion method is invoked
        short result = Converter.convert2short(input);
        // Then: the primitive logical zero is returned
        assertEquals((short) 0, result, "convert2short(null) should return the primitive logical zero (0)");
    }

    @Test
    void convert2short_NonNullInput_DelegatesToInstanceConvert() {
        // Given: a non‑null source object
        Object source = "123";
        // And: the mock is programmed to return a specific short value
        when(mockInstance.convert(source, short.class)).thenReturn((short) 7);
        // When: the static conversion method is called
        short result = Converter.convert2short(source);
        // Then: the result matches the stubbed value
        assertEquals((short) 7, result, "convert2short should return the value supplied by the instance.convert call");
        // And: the instance's convert method was invoked exactly once with the correct arguments
        verify(mockInstance, times(1)).convert(source, short.class);
    }

    @Test
    void convert2short_DifferentTypes_StillDelegatesCorrectly() {
        // Given: an Integer source object
        Integer source = 42;
        // And: the mock returns the short representation of that integer
        when(mockInstance.convert(source, short.class)).thenReturn(source.shortValue());
        // When: conversion is performed
        short result = Converter.convert2short(source);
        // Then: the short value matches the Integer's short value
        assertEquals(source.shortValue(), result);
        // Verify interaction
        verify(mockInstance).convert(source, short.class);
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] convert2short({0}) => {1}")
    @MethodSource("nullAndNonNullProvider")
    void convert2short_NullAndNonNull(Object input, short expected) {
        // Use a real Converter instance so the actual conversion logic runs.
        Converter.instance = new Converter(new DefaultConverterOptions());
        short result = Converter.convert2short(input);
        assertEquals(expected, result, () -> "Expected convert2short(" + input + ") to be " + expected);
    }

    @Test
    void convert2short_NonNumericString_ThrowsNumberFormatException() {
        Converter.instance = new Converter(new DefaultConverterOptions());
        assertThrows(NumberFormatException.class, () -> Converter.convert2short("notANumber"), "Non‑numeric String should cause NumberFormatException");
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] overflow convert2short({0}) => {1}")
    @MethodSource("numericOverflowProvider")
    void convert2short_NumericOverflow(Object input, short expected) {
        Converter.instance = new Converter(new DefaultConverterOptions());
        short result = Converter.convert2short(input);
        assertEquals(expected, result, () -> "Overflow conversion of " + input + " should yield " + expected);
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] Boolean {0} => {1}")
    @MethodSource("booleanProvider")
    void convert2short_Boolean(Boolean input, short expected) {
        Converter.instance = new Converter(new DefaultConverterOptions());
        short result = Converter.convert2short(input);
        assertEquals(expected, result, () -> "Boolean " + input + " should convert to short " + expected);
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] char {0} => {1}")
    @MethodSource("characterProvider")
    void convert2short_Character(Character input, short expected) {
        Converter.instance = new Converter(new DefaultConverterOptions());
        short result = Converter.convert2short(input);
        assertEquals(expected, result, () -> "Character '" + input + "' should convert to short " + expected);
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] BigDecimal {0} => {1}")
    @MethodSource("bigDecimalProvider")
    void convert2short_BigDecimal(BigDecimal input, short expected) {
        Converter.instance = new Converter(new DefaultConverterOptions());
        short result = Converter.convert2short(input);
        assertEquals(expected, result, () -> "BigDecimal " + input + " should convert to short " + expected);
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] BigInteger {0} => {1}")
    @MethodSource("bigIntegerProvider")
    void convert2short_BigInteger(BigInteger input, short expected) {
        Converter.instance = new Converter(new DefaultConverterOptions());
        short result = Converter.convert2short(input);
        assertEquals(expected, result, () -> "BigInteger " + input + " should convert to short " + expected);
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] Atomic {0} => {1}")
    @MethodSource("atomicProvider")
    void convert2short_Atomic(Object input, short expected) {
        Converter.instance = new Converter(new DefaultConverterOptions());
        short result = Converter.convert2short(input);
        assertEquals(expected, result, () -> "Atomic value " + input + " should convert to short " + expected);
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] String \"{0}\" => {1}")
    @MethodSource("stringVariationsProvider")
    void convert2short_StringVariations(String input, short expected) {
        Converter.instance = new Converter(new DefaultConverterOptions());
        short result = Converter.convert2short(input);
        assertEquals(expected, result, () -> "String \"" + input + "\" should convert to short " + expected);
    }

    @Test
    void convert2short_EmptyString_ThrowsNumberFormatException() {
        Converter.instance = new Converter(new DefaultConverterOptions());
        assertThrows(NumberFormatException.class, () -> Converter.convert2short(""), "Empty string should cause NumberFormatException");
    }

    // ----------------------------------------------------------------------
    @Test
    void convert2short_UnsupportedObject_ThrowsIllegalArgumentException() {
        Converter.instance = new Converter(new DefaultConverterOptions());
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2short(new Object()), "Unsupported source type should throw IllegalArgumentException");
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] Unsupported type {0}")
    @MethodSource("unsupportedTypesProvider")
    void convert2short_UnsupportedTypes(Object input) {
        Converter.instance = new Converter(new DefaultConverterOptions());
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2short(input), "Conversion of " + input.getClass().getSimpleName() + " should throw IllegalArgumentException");
    }
}
