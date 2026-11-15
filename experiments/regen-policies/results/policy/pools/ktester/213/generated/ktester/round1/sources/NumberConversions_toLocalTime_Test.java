package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toLocalTime_Test {

    /**
     * A mock Converter – the implementation of toLocalTime does not use it.
     */
    private static Converter mockConverter;

    @BeforeAll
    static void setupBeforeAll() {
        mockConverter = mock(Converter.class);
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Minimum value – midnight
        Arguments.of(0L, "00:00"), // Small positive value – 1 ms → 00:00:00.001 (1 µs is not representable,
        // the method works with whole milliseconds)
        Arguments.of(1L, "00:00:00.001"), // Maximum allowed value – 86 399 999 ms → 23:59:59.999
        Arguments.of(86_399_999L, "23:59:59.999"));
    }

    private static Stream<Arguments> validVariousNumberTypesProvider() {
        return Stream.of(// Integer (12 345 ms)
        Arguments.of(Integer.valueOf(12_345), "00:00:12.345"), // Long (5 000 ms)
        Arguments.of(Long.valueOf(5_000L), "00:00:05"), // Byte (127 ms)
        Arguments.of(Byte.valueOf((byte) 127), "00:00:00.127"), // Short (60 000 ms = 1 min)
        Arguments.of(Short.valueOf((short) 60_000), "00:01"), // BigInteger (12 345 678 ms)
        Arguments.of(new BigInteger("12345678"), "03:25:45.678"), // BigDecimal (9 876.543 ms → truncates to 9 876 ms)
        Arguments.of(new BigDecimal("9876.543"), "00:00:09.876"), // AtomicInteger (2 500 ms)
        Arguments.of(new AtomicInteger(2_500), "00:00:02.5"), // AtomicLong (max value)
        Arguments.of(new AtomicLong(86_399_999L), "23:59:59.999"));
    }

    private static Stream<Arguments> fractionalNumberProvider() {
        return Stream.of(// Double 1.9 ms → truncates to 1 ms → 00:00:00.001
        Arguments.of(Double.valueOf(1.9), "00:00:00.001"), // Float 12 345.9 ms → truncates to 12 345 ms → 00:00:12.345
        Arguments.of(Float.valueOf(12_345.9f), "00:00:12.345"));
    }

    private static Stream<Arguments> infiniteValuesProvider() {
        return Stream.of(Arguments.of(Double.POSITIVE_INFINITY), Arguments.of(Double.NEGATIVE_INFINITY), Arguments.of(Float.POSITIVE_INFINITY), Arguments.of(Float.NEGATIVE_INFINITY));
    }

    @Test
    void shouldConvertZeroMillisToMidnight() {
        LocalTime result = NumberConversions.toLocalTime(0L, mockConverter);
        assertEquals(LocalTime.MIDNIGHT, result);
    }

    @Test
    void shouldConvertTypicalMillisToLocalTime() {
        // 12:34:56.789  -> 12h 34m 56s 789ms = 45_296_789 milliseconds
        long millis = // hours
        12L * 60 * 60 * 1000 + // minutes
        34L * 60 * 1000 + // seconds
        56L * 1000 + // milliseconds
        789L;
        LocalTime expected = LocalTime.of(12, 34, 56, 789_000_000);
        LocalTime actual = NumberConversions.toLocalTime(millis, mockConverter);
        assertEquals(expected, actual);
    }

    @Test
    void shouldConvertMaximumAllowedMillis() {
        // 23:59:59.999  -> 86_399_999 milliseconds
        long maxMillis = 86_399_999L;
        LocalTime expected = LocalTime.of(23, 59, 59, 999_000_000);
        LocalTime actual = NumberConversions.toLocalTime(maxMillis, mockConverter);
        assertEquals(expected, actual);
    }

    @Test
    void shouldThrowWhenNegativeMillis() {
        long negative = -1L;
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> NumberConversions.toLocalTime(negative, mockConverter));
        assertTrue(ex.getMessage().contains("must be >= 0"));
    }

    @Test
    void shouldThrowWhenMillisExceedDayLength() {
        // 24:00:00.000 – one millisecond beyond the day
        long tooLarge = 86_400_000L;
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> NumberConversions.toLocalTime(tooLarge, mockConverter));
        assertTrue(ex.getMessage().contains("must be >= 0"));
    }

    @Test
    void shouldPropagateClassCastExceptionForNonNumber() {
        Object notANumber = "123";
        assertThrows(ClassCastException.class, () -> NumberConversions.toLocalTime(notANumber, mockConverter));
    }

    @ParameterizedTest(name = "[{index}] from={0} → expected={1}")
    @MethodSource("validConversionProvider")
    void shouldConvertValidMillis(Object from, String expectedString) {
        LocalTime expected = LocalTime.parse(expectedString);
        LocalTime actual = NumberConversions.toLocalTime(from, mockConverter);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @MethodSource("validVariousNumberTypesProvider")
    void shouldConvertVariousNumberTypes(Object from, String expectedString) {
        LocalTime expected = LocalTime.parse(expectedString);
        LocalTime actual = NumberConversions.toLocalTime(from, mockConverter);
        assertEquals(expected, actual);
    }

    @Test
    void shouldThrowWhenBigIntegerEqualsDayLength() {
        BigInteger dayLength = BigInteger.valueOf(86_400_000L);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> NumberConversions.toLocalTime(dayLength, mockConverter));
        assertTrue(ex.getMessage().contains("must be >= 0"));
    }

    @Test
    void shouldThrowWhenBigDecimalNegative() {
        BigDecimal negative = new BigDecimal("-1");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> NumberConversions.toLocalTime(negative, mockConverter));
        assertTrue(ex.getMessage().contains("must be >= 0"));
    }

    @Test
    void shouldThrowNullPointerExceptionWhenFromIsNull() {
        assertThrows(NullPointerException.class, () -> NumberConversions.toLocalTime(null, mockConverter));
    }

    @ParameterizedTest(name = "[{index}] from={0} → expected={1}")
    @MethodSource("fractionalNumberProvider")
    void shouldTruncateFractionalNumbers(Object from, String expectedString) {
        LocalTime expected = LocalTime.parse(expectedString);
        LocalTime actual = NumberConversions.toLocalTime(from, mockConverter);
        assertEquals(expected, actual);
    }

    @Test
    void shouldIgnoreNonNullConverter() {
        // Create a distinct mock to prove that the method does not depend on it.
        Converter dummyConverter = mock(Converter.class);
        // 5 seconds = 5 000 ms
        LocalTime expected = LocalTime.of(0, 0, 5);
        LocalTime actual = NumberConversions.toLocalTime(5_000L, dummyConverter);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "[{index}] infinite from={0}")
    @MethodSource("infiniteValuesProvider")
    void shouldThrowWhenInfiniteValue(Object infinite) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> NumberConversions.toLocalTime(infinite, mockConverter));
        assertTrue(ex.getMessage().contains("must be >= 0"));
    }
}
