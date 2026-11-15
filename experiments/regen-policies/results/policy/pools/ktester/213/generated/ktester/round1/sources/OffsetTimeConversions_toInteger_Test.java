package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OffsetTimeConversions_toInteger_Test {

    private Converter converter;

    /**
     * Placeholder for the library's {@code ConversionException}.
     * In the actual project this class already exists; it is duplicated here only
     * to keep the test compilation‑self‑contained.
     */
    class ConversionException extends RuntimeException {

        ConversionException(String message) {
            super(message);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Converter needs a ConverterOptions instance – the default options are sufficient for our tests
        converter = new Converter(new ConverterOptions());
    }

    private static Stream<Arguments> conversionTestCases() {
        return Stream.of(// Long within int range
        arguments(123L, 123), // Integer.MAX_VALUE unchanged
        arguments(Integer.MAX_VALUE, Integer.MAX_VALUE), // Long just over Integer.MAX_VALUE – Java cast wraps to Integer.MIN_VALUE
        arguments((long) Integer.MAX_VALUE + 1L, (int) ((long) Integer.MAX_VALUE + 1L)), // Numeric string
        arguments("456", 456), // BigInteger within range
        arguments(new BigInteger("789"), 789), // BigDecimal truncated (floor) before casting
        arguments(new BigDecimal("123.99"), 123), // AtomicInteger unwrapped
        arguments(new AtomicInteger(321), 321), // AtomicLong converted to long then cast
        arguments(new AtomicLong(654L), 654));
    }

    private static Stream<Arguments> edgeCaseTestCases() {
        return Stream.of(// Negative numeric string
        arguments("-42", -42), // String with surrounding whitespace
        arguments("   77   ", 77), // String with explicit plus sign
        arguments("+15", 15), // Zero string
        arguments("0", 0), // Double truncated toward zero
        arguments(Double.valueOf(987.65), 987), // Float truncated toward zero
        arguments(Float.valueOf(-12.9f), -12), // BigInteger just above Integer.MAX_VALUE – overflow to MIN_VALUE
        arguments(new BigInteger("2147483648"), (int) new BigInteger("2147483648").longValue()), // BigDecimal overflow (truncated then overflow)
        arguments(new BigDecimal("2147483648.99"), (int) new BigDecimal("2147483648.99").longValue()), // Long.MAX_VALUE overflow (low 32 bits are all 1 -> -1)
        arguments(Long.MAX_VALUE, (int) Long.MAX_VALUE));
    }

    private static Stream<Arguments> exceptionTestCases() {
        return Stream.of(// Non‑numeric string -> NumberFormatException
        arguments("not a number", NumberFormatException.class), // Decimal string that cannot be parsed as int -> NumberFormatException
        arguments("123.45", NumberFormatException.class), // String larger than Long.MAX_VALUE -> NumberFormatException
        arguments("9223372036854775808", NumberFormatException.class), // Boolean not convertible -> IllegalArgumentException
        arguments(true, IllegalArgumentException.class), // Arbitrary object -> IllegalArgumentException
        arguments(new Object(), IllegalArgumentException.class), // OffsetTime not directly convertible -> IllegalArgumentException
        arguments(OffsetTime.now(), IllegalArgumentException.class), // BigInteger exceeding Long.MAX_VALUE -> ArithmeticException
        arguments(new BigInteger("9223372036854775808"), ArithmeticException.class), // BigDecimal exceeding Long.MAX_VALUE -> ArithmeticException
        arguments(new BigDecimal("9223372036854775808.99"), ArithmeticException.class));
    }

    @Test
    void toInteger_convertsStringNumber() {
        // given
        String source = "12345";
        // when
        int result = OffsetTimeConversions.toInteger(source, converter);
        // then
        assertEquals(12345, result);
    }

    @Test
    void toInteger_convertsLongNumber() {
        // given
        Long source = 9876L;
        // when
        int result = OffsetTimeConversions.toInteger(source, converter);
        // then
        assertEquals(9876, result);
    }

    @Test
    void toInteger_nullSourceReturnsZero() {
        // given
        Object source = null;
        // when
        int result = OffsetTimeConversions.toInteger(source, converter);
        // then
        assertEquals(0, result);
    }

    @Test
    void toInteger_convertsOffsetTimeToEpochSecondWithinIntRange() {
        // given: an OffsetTime whose epoch second fits into an int (e.g., 00:00:01Z -> 1 second)
        OffsetTime offsetTime = OffsetTime.of(0, 0, 1, 0, java.time.ZoneOffset.UTC);
        // The Converter knows how to turn OffsetTime into a long (epoch seconds). We rely on that.
        // when
        int result = OffsetTimeConversions.toInteger(offsetTime, converter);
        // then
        assertEquals(1, result);
    }

    /**
     * Conversion cases that should succeed and produce an int value.
     */
    @ParameterizedTest(name = "[{index}] toInteger({0}) = {1}")
    @MethodSource("conversionTestCases")
    void toInteger_conversionTests(Object source, int expected) {
        int result = OffsetTimeConversions.toInteger(source, null);
        assertEquals(expected, result);
    }

    /**
     * Edge‑case conversion tests (whitespace, signs, truncation, overflow).
     */
    @ParameterizedTest(name = "[{index}] toInteger({0}) = {1}")
    @MethodSource("edgeCaseTestCases")
    void toInteger_edgeCaseTests(Object source, int expected) {
        int result = OffsetTimeConversions.toInteger(source, null);
        assertEquals(expected, result);
    }

    /**
     * Tests that verify the correct exception type is thrown.
     */
    @ParameterizedTest(name = "[{index}] toInteger({0}) throws {1}")
    @MethodSource("exceptionTestCases")
    void toInteger_exceptionScenarios(Object source, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> OffsetTimeConversions.toInteger(source, null));
    }

    /**
     * Test that a custom Converter throwing ConversionException is propagated.
     */
    @Test
    void toInteger_propagatesConversionExceptionFromConverter() {
        // Arrange: a mock Converter that always throws ConversionException
        Converter mockConverter = mock(Converter.class);
        doThrow(new ConversionException("forced failure")).when(mockConverter).convert(any(), any());
        // value does not matter – the mock will throw first
        Object dummySource = new Object();
        // Act & Assert
        assertThrows(ConversionException.class, () -> OffsetTimeConversions.toInteger(dummySource, mockConverter));
    }
}
