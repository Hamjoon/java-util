package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toAtomicLong_Test {

    /**
     * Converter used by all test cases.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Converter needs a ConverterOptions instance; the default constructor
        // creates an options object with the standard built‑in conversions.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// numeric objects
        arguments(123L, 123L), arguments(456, 456L), arguments("789", 789L), // double truncation
        arguments(10.5, 10L), arguments(new BigInteger("1000"), 1000L), // boolean conversions
        arguments(Boolean.TRUE, 1L), arguments(Boolean.FALSE, 0L), // atomic type inputs
        arguments(new AtomicLong(55L), 55L), arguments(new AtomicInteger(33), 33L), // primitive wrapper conversions
        arguments(Byte.valueOf((byte) 7), 7L), arguments(Short.valueOf((short) -12), -12L), arguments(Float.valueOf(12.9f), 12L), // BigDecimal edge cases (fractional part is truncated)
        arguments(new BigDecimal("12345.67"), 12345L), // character conversions
        arguments('5', 5L), // string edge cases
        arguments("-42", -42L), arguments("  99  ", 99L), // extreme long values
        arguments(Long.MAX_VALUE, Long.MAX_VALUE), arguments(Long.MIN_VALUE, Long.MIN_VALUE));
    }

    private static Stream<Arguments> exceptionConversionProvider() {
        // used for most cases
        Converter defaultConverter = new Converter(new ConverterOptions());
        return Stream.of(// malformed numeric strings
        arguments("12a34", defaultConverter, NumberFormatException.class), arguments("--5", defaultConverter, NumberFormatException.class), arguments("", defaultConverter, NumberFormatException.class), // unsupported CharSequence implementations
        arguments(new StringBuilder("456"), defaultConverter, NumberFormatException.class), arguments(new StringBuffer("789"), defaultConverter, NumberFormatException.class), // non‑numeric character
        arguments('a', defaultConverter, NumberFormatException.class), // BigInteger overflow
        arguments(new BigInteger("9223372036854775808"), defaultConverter, ArithmeticException.class), // BigDecimal overflow (positive & negative)
        arguments(new BigDecimal("9223372036854775808"), defaultConverter, ArithmeticException.class), arguments(new BigDecimal("-9223372036854775809"), defaultConverter, ArithmeticException.class), // special floating‑point values
        arguments(Double.NaN, defaultConverter, IllegalArgumentException.class), arguments(Double.POSITIVE_INFINITY, defaultConverter, IllegalArgumentException.class), arguments(Float.NEGATIVE_INFINITY, defaultConverter, IllegalArgumentException.class), // unsupported types
        arguments(new java.util.Date(), defaultConverter, IllegalArgumentException.class), arguments(new java.util.ArrayList<>(), defaultConverter, IllegalArgumentException.class), // null source – contract states a NullPointerException
        arguments(null, defaultConverter, NullPointerException.class), // null converter
        arguments(123, null, NullPointerException.class));
    }

    @Test
    void toAtomicLong_fromLong_returnsSameValue() {
        Long source = 123L;
        AtomicLong result = NumberConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(123L, result.get());
    }

    @Test
    void toAtomicLong_fromStringNumber_returnsParsedValue() {
        String source = "456";
        AtomicLong result = NumberConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(456L, result.get());
    }

    @Test
    void toAtomicLong_fromNull_returnsZero() {
        // According to the conversion contract, null converts to zero for primitive
        // numeric types.
        AtomicLong result = NumberConversions.toAtomicLong(null, converter);
        assertNotNull(result);
        assertEquals(0L, result.get());
    }

    @Test
    void toAtomicLong_fromInvalidString_throwsNumberFormatException() {
        String source = "not-a-number";
        assertThrows(NumberFormatException.class, () -> NumberConversions.toAtomicLong(source, converter));
    }

    @Test
    void toAtomicLong_fromBigInteger_returnsCorrectValue() {
        java.math.BigInteger source = new java.math.BigInteger("789");
        AtomicLong result = NumberConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(789L, result.get());
    }

    @Test
    void toAtomicLong_fromAtomicInteger_returnsConvertedValue() {
        java.util.concurrent.atomic.AtomicInteger source = new java.util.concurrent.atomic.AtomicInteger(321);
        AtomicLong result = NumberConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(321L, result.get());
    }

    /* -------------------------------------------------------------
     *  Parameterized tests for successful conversions returning AtomicLong
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: toAtomicLong({0}) → {1}")
    @MethodSource("validConversionProvider")
    void toAtomicLong_validConversions(Object source, long expected) {
        AtomicLong result = NumberConversions.toAtomicLong(source, converter);
        assertNotNull(result, "Result must not be null");
        assertEquals(expected, result.get(), () -> "Expected AtomicLong value " + expected + " for source: " + source);
    }

    /* -------------------------------------------------------------
     *  Parameterized tests for conversions that must throw an exception
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: toAtomicLong({0}) throws {1}")
    @MethodSource("exceptionConversionProvider")
    void toAtomicLong_expectedExceptions(Object source, Converter conv, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> NumberConversions.toAtomicLong(source, conv), () -> "Expected " + expectedException.getSimpleName() + " for source: " + source);
    }

    /* -------------------------------------------------------------
     *  Additional explicit tests for clarity (optional but kept for
     *  readability and to document historic expectations).
     * ------------------------------------------------------------- */
    @Test
    void toAtomicLong_fromNull_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> NumberConversions.toAtomicLong(null, converter));
    }

    @Test
    void toAtomicLong_fromNullConverter_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> NumberConversions.toAtomicLong(123, null));
    }
}
