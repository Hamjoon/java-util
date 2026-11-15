package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalTimeConversions_toAtomicInteger_Test {

    /**
     * Shared {@link Converter} used by all test methods.
     */
    private static Converter sharedConverter;

    @Mock
    Converter // injected by MockitoExtension
    mockConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a real Converter with default options.
        sharedConverter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // No resources to release, but null‑out for good measure.
        sharedConverter = null;
    }

    static Stream<Arguments> numericConversionProvider() {
        return Stream.of(// Simple integer
        Arguments.of(42, 42), // Long larger than Integer.MAX_VALUE – low‑order 32 bits are kept
        Arguments.of(1234567890123L, (int) 1234567890123L), // String containing a decimal number
        Arguments.of("12345", 12345), // Double – truncates toward zero
        Arguments.of(99.99, 99), // Very large long – overflow to negative int
        Arguments.of(1234567890123456789L, (int) 1234567890123456789L));
    }

    static Stream<Arguments> additionalNumericConversionProvider() {
        return Stream.of(// AtomicInteger input – unwraps its int value
        Arguments.of(new AtomicInteger(7), 7), // AtomicLong input – truncates to int
        Arguments.of(new AtomicLong(1234567890123L), (int) 1234567890123L), // BigDecimal with fractional part – truncates toward zero
        Arguments.of(new BigDecimal("12345.678"), 12345), // Negative BigDecimal
        Arguments.of(new BigDecimal("-9876.543"), -9876), // BigInteger equal to 2^32 + 1 – low‑order 32 bits = 1
        Arguments.of(new BigInteger("4294967297"), 1), // Negative BigInteger – maps directly
        Arguments.of(new BigInteger("-1"), -1), // String with surrounding whitespace – should be trimmed
        Arguments.of("   42  ", 42));
    }

    static Stream<Arguments> invalidInputProvider() {
        return Stream.of(// Empty string
        Arguments.of("", NumberFormatException.class), // Non‑numeric string
        Arguments.of("not a number", NumberFormatException.class), // Null reference – the library defines this as NPE for numeric conversions
        Arguments.of(null, NullPointerException.class), // Boolean value – not supported
        Arguments.of(Boolean.TRUE, IllegalArgumentException.class), // Number larger than Long.MAX_VALUE (as a String)
        Arguments.of("9223372036854775808", NumberFormatException.class), // BigInteger exceeding Long.MAX_VALUE – overflow should raise ArithmeticException
        Arguments.of(new BigInteger("9223372036854775808"), ArithmeticException.class), // BigDecimal exceeding Long.MAX_VALUE – overflow should raise ArithmeticException
        Arguments.of(new BigDecimal("9223372036854775808"), ArithmeticException.class), // Plain Object – no numeric representation
        Arguments.of(new Object(), IllegalArgumentException.class), // Array – unsupported
        Arguments.of(new int[] { 1, 2, 3 }, IllegalArgumentException.class), // List – unsupported
        Arguments.of(java.util.List.of(1, 2), IllegalArgumentException.class), // LocalTime – unsupported
        Arguments.of(java.time.LocalTime.now(), IllegalArgumentException.class));
    }

    @Test
    void toAtomicInteger_fromInteger_returnsSameValue() {
        // given
        Integer source = 7;
        // when
        AtomicInteger result = LocalTimeConversions.toAtomicInteger(source, sharedConverter);
        // then
        assertNotNull(result, "Result should never be null");
        assertEquals(7, result.get(), "AtomicInteger value should match the integer source");
    }

    @Test
    void toAtomicInteger_fromStringNumeric_returnsParsedValue() {
        // given
        String source = "12345";
        // when
        AtomicInteger result = LocalTimeConversions.toAtomicInteger(source, sharedConverter);
        // then
        assertNotNull(result);
        assertEquals(12345, result.get(), "AtomicInteger should contain the numeric value parsed from the string");
    }

    @Test
    void toAtomicInteger_fromLongTruncatesToInt() {
        // given
        // exceeds int range; truncation expected
        Long source = 9_876_543_210L;
        // when
        AtomicInteger result = LocalTimeConversions.toAtomicInteger(source, sharedConverter);
        // then
        assertNotNull(result);
        // The conversion follows the implementation in toLong → cast to int,
        // which results in the low‑order 32 bits of the long value.
        int expected = (int) (long) source;
        assertEquals(expected, result.get(), "AtomicInteger should contain the low‑order 32 bits of the long source");
    }

    @Test
    void toAtomicInteger_fromNull_returnsZero() {
        // given
        Object source = null;
        // when
        AtomicInteger result = LocalTimeConversions.toAtomicInteger(source, sharedConverter);
        // then
        assertNotNull(result);
        assertEquals(0, result.get(), "Null source should be converted to an AtomicInteger with value 0");
    }

    @ParameterizedTest(name = "[{index}] from={0} → expected={1}")
    @MethodSource("numericConversionProvider")
    void toAtomicInteger_numericConversions(Object from, int expected) {
        // when
        AtomicInteger result = LocalTimeConversions.toAtomicInteger(from, sharedConverter);
        // then
        assertNotNull(result, "Result must never be null");
        assertEquals(expected, result.get(), () -> "AtomicInteger value should match expected conversion for input: " + from);
    }

    @ParameterizedTest(name = "[{index}] from={0} → expected={1}")
    @MethodSource("additionalNumericConversionProvider")
    void toAtomicInteger_additionalNumericConversions(Object from, int expected) {
        AtomicInteger result = LocalTimeConversions.toAtomicInteger(from, sharedConverter);
        assertNotNull(result);
        assertEquals(expected, result.get(), () -> "AtomicInteger value should match expected conversion for input: " + from);
    }

    @ParameterizedTest(name = "[{index}] from={0} → expects {1}")
    @MethodSource("invalidInputProvider")
    void toAtomicInteger_invalidInputs(Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> LocalTimeConversions.toAtomicInteger(from, sharedConverter), () -> "Expected " + expectedException.getSimpleName() + " for input: " + from);
    }

    @Test
    void toAtomicInteger_customConverter_isUsed() {
        // Arrange – the mock converter will return 555L for any input.
        when(mockConverter.toLong(any())).thenReturn(555L);
        // Act
        AtomicInteger result = LocalTimeConversions.toAtomicInteger("customObject", mockConverter);
        // Assert
        assertNotNull(result);
        assertEquals(555, result.get(), "AtomicInteger should contain the value supplied by the custom Converter (555)");
    }
}
