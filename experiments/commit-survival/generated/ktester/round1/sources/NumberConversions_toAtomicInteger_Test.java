package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toAtomicInteger_Test {

    /**
     * The {@link Converter} instance used for all tests.
     */
    private static Converter converter;

    /**
     * Default {@link Converter} used when the test does not require a custom one.
     */
    private static Converter defaultConverter;

    class Dummy {
    }

    /**
     * Simple POJO that mimics a user‑defined numeric type.
     */
    private static final class CustomNumber {

        private final int value;

        CustomNumber(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – the library provides a no‑arg constructor for
        // ConverterOptions that yields the standard conversion table.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly necessary but keeps the
        // test class tidy.
        converter = null;
    }

    /**
     * Supplies the data for {@link #shouldConvertToAtomicInteger(Object, Converter, Object, String)}.
     *
     * <p>Each {@link Arguments} entry contains:
     * <ul>
     *   <li>source object</li>
     *   <li>converter (null for default)</li>
     *   <li>expected int value (or exception class)</li>
     *   <li>human‑readable description</li>
     * </ul>
     * </p>
     */
    private static Stream<Arguments> positiveCases() {
        // Custom converters used in a few test rows
        Converter customNumberConverter = mock(Converter.class);
        when(customNumberConverter.convert(any(), eq(Integer.class))).thenReturn(77);
        Converter customReturning55 = mock(Converter.class);
        when(customReturning55.convert(isNull(), eq(Integer.class))).thenReturn(55);
        Converter customReturningNull = mock(Converter.class);
        when(customReturningNull.convert(isNull(), eq(Integer.class))).thenReturn(null);
        return Stream.of(// Direct numeric types
        Arguments.of(123, null, 123, "Direct Integer input should be converted to the same int value."), Arguments.of(Long.valueOf(9876543210L), null, (int) 9876543210L, "Long input is converted via toInt, which casts the long to int (possible overflow)."), Arguments.of(Double.valueOf(42.99), null, 42, "Floating‑point numbers are truncated to their integer part."), Arguments.of("-256", null, -256, "String containing a parsable integer should be parsed correctly."), // Booleans
        Arguments.of(Boolean.TRUE, null, 1, "Boolean true is interpreted as 1."), Arguments.of(Boolean.FALSE, null, 0, "Boolean false is interpreted as 0."), // Primitive wrappers
        Arguments.of(Byte.valueOf((byte) 5), null, 5, "Byte value should be widened to int without loss."), Arguments.of(Short.valueOf((short) -123), null, -123, "Short value (including negative) is converted correctly."), Arguments.of(Float.valueOf(7.9f), null, 7, "Floating‑point numbers are truncated toward zero."), // Big numbers
        Arguments.of(new BigDecimal("12345.67"), null, 12345, "BigDecimal with fractional part is truncated to its integer part."), Arguments.of(new BigInteger("2147483648"), null, (int) new BigInteger("2147483648").intValue(), "BigInteger larger than Integer.MAX_VALUE overflows the int cast (wrap‑around)."), // Atomic types
        Arguments.of(new AtomicInteger(42), null, 42, "AtomicInteger source should be unwrapped to its int value."), Arguments.of(new AtomicLong(99L), null, 99, "AtomicLong is converted via toInt (casting long to int)."), Arguments.of(new AtomicBoolean(true), null, 1, "AtomicBoolean true maps to 1, false would map to 0."), // CharSequence handling
        Arguments.of(new StringBuilder("456"), null, 456, "Any CharSequence containing a parsable integer should be accepted."), Arguments.of("   77  ", null, 77, "String with surrounding whitespace is trimmed before parsing."), Arguments.of("+88", null, 88, "String with explicit plus sign is parsed as a positive integer."), // Null source with a custom converter
        Arguments.of(null, customReturning55, 55, "When source is null but a Converter is supplied, the Converter is invoked and its result (55) is used."), // NaN / infinities
        Arguments.of(Double.NaN, null, 0, "NaN cast to int yields 0."), Arguments.of(Double.POSITIVE_INFINITY, null, Integer.MAX_VALUE, "Positive infinity cast to int yields Integer.MAX_VALUE."), Arguments.of(Double.NEGATIVE_INFINITY, null, Integer.MIN_VALUE, "Negative infinity cast to int yields Integer.MIN_VALUE."), // Custom object handled by a custom converter
        Arguments.of(new CustomNumber(77), customNumberConverter, 77, "When the source object is not a known numeric type, the supplied Converter is invoked; the custom converter returns 77."));
    }

    private static Stream<Arguments> exceptionCases() {
        // Converter that throws an IllegalStateException when used
        Converter throwingConverter = mock(Converter.class);
        when(throwingConverter.convert(any(), eq(Integer.class))).thenThrow(new IllegalStateException("converter failure"));
        // Converter that returns a non‑numeric String, which leads to NumberFormatException
        Converter badReturnConverter = mock(Converter.class);
        when(badReturnConverter.convert(isNull(), eq(Integer.class))).thenReturn("notANumber");
        return Stream.of(Arguments.of("123abc", null, NumberFormatException.class, "String containing numeric prefix followed by non‑numeric characters should cause parsing to fail."), Arguments.of("", null, NumberFormatException.class, "Empty string cannot be parsed to an int."), Arguments.of("   ", null, NumberFormatException.class, "String consisting only of whitespace should be trimmed to empty and then fail parsing."), Arguments.of("0xFF", null, NumberFormatException.class, "Hexadecimal notation is not supported by toInt and should raise NumberFormatException."), Arguments.of("2147483648", null, NumberFormatException.class, "String representing a value greater than Integer.MAX_VALUE should cause overflow detection and throw NumberFormatException."), Arguments.of(new int[] { 1, 2, 3 }, null, IllegalArgumentException.class, "Array types are not supported for numeric conversion and should trigger IllegalArgumentException."), Arguments.of(java.util.Arrays.asList(1, 2, 3), null, IllegalArgumentException.class, "Collection types are unsupported and should result in IllegalArgumentException."), Arguments.of("123", throwingConverter, IllegalStateException.class, "When a supplied Converter throws a RuntimeException, the exception should propagate unchanged."), Arguments.of(null, badReturnConverter, NumberFormatException.class, "Converter returns an incompatible type (String) that cannot be parsed to int, leading to NumberFormatException."));
    }

    @Test
    void shouldConvertInteger() {
        AtomicInteger result = NumberConversions.toAtomicInteger(42, converter);
        assertNotNull(result);
        assertEquals(42, result.get());
    }

    @Test
    void shouldConvertLong() {
        AtomicInteger result = NumberConversions.toAtomicInteger(123L, converter);
        assertNotNull(result);
        assertEquals(123, result.get());
    }

    @Test
    void shouldConvertStringNumber() {
        AtomicInteger result = NumberConversions.toAtomicInteger("77", converter);
        assertNotNull(result);
        assertEquals(77, result.get());
    }

    @Test
    void shouldConvertDouble() {
        AtomicInteger result = NumberConversions.toAtomicInteger(3.14, converter);
        assertNotNull(result);
        // Conversion follows the same rules as Integer conversion – truncates toward zero.
        assertEquals(3, result.get());
    }

    @Test
    void shouldConvertBigDecimal() {
        AtomicInteger result = NumberConversions.toAtomicInteger(new java.math.BigDecimal("99.99"), converter);
        assertNotNull(result);
        assertEquals(99, result.get());
    }

    @Test
    void shouldConvertAtomicLong() {
        AtomicInteger result = NumberConversions.toAtomicInteger(new java.util.concurrent.atomic.AtomicLong(55L), converter);
        assertNotNull(result);
        assertEquals(55, result.get());
    }

    @Test
    void shouldReturnZeroWhenNull() {
        AtomicInteger result = NumberConversions.toAtomicInteger(null, converter);
        assertNotNull(result);
        assertEquals(0, result.get());
    }

    @Test
    void shouldThrowNumberFormatExceptionForNonNumericString() {
        assertThrows(NumberFormatException.class, () -> NumberConversions.toAtomicInteger("not-a-number", converter));
    }

    @Test
    void shouldThrowClassCastExceptionForUnsupportedType() {
        // Passing an object that the Converter cannot turn into a number.
        class Dummy {
        }
        assertThrows(ClassCastException.class, () -> NumberConversions.toAtomicInteger(new Dummy(), converter));
    }

    /**
     * Parameterized test that covers all successful conversion scenarios.
     *
     * <p>The {@code expected} column holds either an {@link Integer} (the expected
     * int value) or the {@link Class} of an exception that should be thrown.</p>
     */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("positiveCases")
    void shouldConvertToAtomicInteger(Object from, Converter converter, Object expected, String description) {
        Converter usedConverter = (converter == null) ? defaultConverter : converter;
        // When the expected result is an exception class we assert that it is thrown.
        if (expected instanceof Class<?> && Throwable.class.isAssignableFrom((Class<?>) expected)) {
            @SuppressWarnings("unchecked")
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, () -> NumberConversions.toAtomicInteger(from, usedConverter), description);
            return;
        }
        // Normal successful conversion – we expect a non‑null AtomicInteger with the
        // exact int value supplied in the test data.
        AtomicInteger result = NumberConversions.toAtomicInteger(from, usedConverter);
        assertNotNull(result, description);
        assertEquals(((Integer) expected).intValue(), result.get(), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("exceptionCases")
    void shouldThrowExpectedException(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
        Converter usedConverter = (converter == null) ? defaultConverter : converter;
        assertThrows(expectedException, () -> NumberConversions.toAtomicInteger(from, usedConverter), description);
    }
}
