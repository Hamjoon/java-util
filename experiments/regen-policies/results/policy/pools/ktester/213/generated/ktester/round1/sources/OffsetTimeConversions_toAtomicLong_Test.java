package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class OffsetTimeConversions_toAtomicLong_Test {

    /**
     * Shared Converter instance used by all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – they are sufficient for the built‑in conversions we need.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static Converter reference.
        converter = null;
    }

    /**
     * Supplies all test cases defined in the JSON document.
     *
     * @return a stream of arguments for the parameterised test.
     */
    private static Stream<Arguments> provideToAtomicLongCases() {
        return Stream.of(// ------------------- Valid numeric conversions -------------------
        Arguments.of(Long.valueOf(123L), converter, 123L, "Direct Long instance should be converted to AtomicLong with the same value."), Arguments.of(Integer.valueOf(456), converter, 456L, "Integer instance should be converted via Number.longValue() to AtomicLong."), Arguments.of(new BigDecimal("789.99"), converter, 789L, "BigDecimal should be truncated to its long value when creating AtomicLong."), Arguments.of(new BigInteger("321"), converter, 321L, "BigInteger should be converted to long and wrapped in AtomicLong."), // ------------------- String based conversions -------------------
        Arguments.of("9876543210", converter, 9_876_543_210L, "A numeric string should be parsed to a long and returned inside AtomicLong."), Arguments.of("   42   ", converter, 42L, "String with surrounding whitespace should be trimmed and parsed correctly."), Arguments.of("-15", converter, -15L, "Negative numeric string should be parsed to a negative long value."), Arguments.of("notANumber", converter, NumberFormatException.class, "Non‑numeric string should cause NumberFormatException during conversion."), // ------------------- Temporal conversions -------------------
        Arguments.of(OffsetTime.parse("10:15:30+02:00"), converter, 36_930_000_000_000L, "OffsetTime should be converted to the number of nanoseconds since midnight."), Arguments.of(Instant.ofEpochSecond(1_622_548_800L), converter, 1_622_548_800_000L, "Instant should be converted to epoch milliseconds and wrapped in AtomicLong."), // ------------------- Boolean and null handling -------------------
        Arguments.of(Boolean.TRUE, converter, 1L, "Boolean true should be interpreted as 1."), Arguments.of(Boolean.FALSE, converter, 0L, "Boolean false should be interpreted as 0."), Arguments.of(null, converter, 0L, "Null input should be treated as zero rather than throwing an exception."), // ------------------- Unsupported type handling -------------------
        Arguments.of(new Object(), converter, IllegalArgumentException.class, "Unsupported object type should result in IllegalArgumentException."), // ------------------- Custom Converter usage -------------------
        Arguments.of("custom:12345", customConverter(), 12_345L, "Custom converter extracts numeric part from a non‑standard string."), // ------------------- Additional numeric type conversions -------------------
        Arguments.of(Double.valueOf(123.456), converter, 123L, "Double value should be truncated to its long part when converting to AtomicLong."), Arguments.of(Float.valueOf(-78.9f), converter, -78L, "Float value should be truncated to its long part (negative) when converting."), Arguments.of(Short.valueOf((short) 32_767), converter, 32_767L, "Short instance should be promoted to long correctly."), Arguments.of(Byte.valueOf((byte) -128), converter, -128L, "Byte instance should be promoted to long correctly."), Arguments.of(new AtomicLong(555L), converter, 555L, "An AtomicLong input should be unwrapped and re‑wrapped without changing its value."), Arguments.of(new AtomicInteger(666), converter, 666L, "An AtomicInteger should be converted via its intValue() to an AtomicLong."), // ------------------- String edge cases -------------------
        Arguments.of("+999", converter, 999L, "String with an explicit plus sign should be parsed as a positive long."), Arguments.of("00123", converter, 123L, "String with leading zeros should be parsed correctly, discarding the zeros."), Arguments.of("123.45", converter, NumberFormatException.class, "Decimal point in a numeric string is not allowed for long parsing."), Arguments.of("1e6", converter, NumberFormatException.class, "Scientific notation is not accepted by the long parser."), Arguments.of("", converter, NumberFormatException.class, "Empty string cannot be parsed to a number."), Arguments.of("   ", converter, NumberFormatException.class, "String containing only whitespace should be treated as non‑numeric and cause NumberFormatException."), Arguments.of("true", converter, NumberFormatException.class, "String literal \"true\" is not a numeric representation; conversion should fail."), // ------------------- CharSequence implementation -------------------
        Arguments.of(new StringBuilder("777"), converter, 777L, "A CharSequence implementation containing a numeric value should be parsed like a regular String."), // ------------------- Overflow handling -------------------
        Arguments.of(new BigInteger("9223372036854775808"), converter, IllegalArgumentException.class, "BigInteger larger than Long.MAX_VALUE should trigger IllegalArgumentException."), Arguments.of(new BigDecimal("9223372036854775808.9"), converter, IllegalArgumentException.class, "BigDecimal exceeding the long range should cause IllegalArgumentException."), // ------------------- Custom converter returning non‑numeric value -------------------
        Arguments.of("custom:abc", nonNumericReturningConverter(), NumberFormatException.class, "Custom converter returns a non‑numeric string, leading to NumberFormatException."), // ------------------- Converter exception handling -------------------
        Arguments.of("123", throwingConverter(), IllegalStateException.class, "Custom converter throws IllegalStateException; it should propagate."), Arguments.of("456", nullReturningConverter(), NullPointerException.class, "Custom converter returns null; should cause NullPointerException when unboxing to long."), // ------------------- Unsupported temporal type handling -------------------
        Arguments.of(LocalDate.parse("2020-01-01"), converter, IllegalArgumentException.class, "LocalDate is not a supported temporal type for conversion to AtomicLong."), Arguments.of(OffsetDateTime.parse("2020-01-01T10:15:30+02:00"), converter, IllegalArgumentException.class, "OffsetDateTime is not handled by the conversion logic and should result in IllegalArgumentException."), // ------------------- Array and collection input handling -------------------
        Arguments.of(new int[] { 1, 2, 3 }, converter, IllegalArgumentException.class, "Primitive array input is unsupported and should cause IllegalArgumentException."), Arguments.of(java.util.List.of(1, 2, 3), converter, IllegalArgumentException.class, "Collection input (List) is not a supported source type and should trigger IllegalArgumentException."));
    }

    /**
     * Converter that extracts the numeric part after "custom:" prefix.
     */
    private static Converter customConverter() {
        Converter mock = Mockito.mock(Converter.class);
        Mockito.when(mock.convert(Mockito.eq("custom:12345"), Mockito.eq(Long.class))).thenReturn(12_345L);
        // For any other input we delegate to the default converter to avoid accidental NPEs.
        Mockito.when(mock.convert(Mockito.any(), Mockito.eq(Long.class))).thenAnswer(invocation -> converter.convert(invocation.getArgument(0), Long.class));
        return mock;
    }

    /**
     * Converter that returns a non‑numeric string when asked for a Long.
     */
    private static Converter nonNumericReturningConverter() {
        Converter mock = Mockito.mock(Converter.class);
        Mockito.when(mock.convert(Mockito.eq("custom:abc"), Mockito.eq(Long.class))).thenReturn(// returning a String instead of a Number
        "abc");
        return mock;
    }

    /**
     * Converter that always throws IllegalStateException.
     */
    private static Converter throwingConverter() {
        Converter mock = Mockito.mock(Converter.class);
        Mockito.when(mock.convert(Mockito.any(), Mockito.eq(Long.class))).thenThrow(new IllegalStateException("forced failure"));
        return mock;
    }

    /**
     * Converter that returns null for any conversion request.
     */
    private static Converter nullReturningConverter() {
        Converter mock = Mockito.mock(Converter.class);
        Mockito.when(mock.convert(Mockito.any(), Mockito.eq(Long.class))).thenReturn(null);
        return mock;
    }

    @Test
    void toAtomicLong_fromOffsetTime_returnsAtomicLongWithConvertedValue() {
        // given
        OffsetTime offsetTime = OffsetTime.of(12, 30, 45, 123_456_789, ZoneOffset.UTC);
        // when
        AtomicLong result = OffsetTimeConversions.toAtomicLong(offsetTime, converter);
        // then
        assertNotNull(result, "Resulting AtomicLong should not be null");
        // The expected value is whatever the Converter would produce when converting to Long.
        Long expected = converter.convert(offsetTime, Long.class);
        assertEquals(expected.longValue(), result.get(), "AtomicLong should contain the long value produced by the Converter");
    }

    @Test
    void toAtomicLong_fromStringNumber_returnsAtomicLongWithParsedValue() {
        // given
        String numberAsString = "12345";
        // when
        AtomicLong result = OffsetTimeConversions.toAtomicLong(numberAsString, converter);
        // then
        assertNotNull(result);
        Long expected = converter.convert(numberAsString, Long.class);
        assertEquals(expected.longValue(), result.get(), "AtomicLong should contain the parsed long value from the string");
    }

    @Test
    void toAtomicLong_fromNull_returnsAtomicLongWithZero() {
        // given
        Object nullObject = null;
        // when
        AtomicLong result = OffsetTimeConversions.toAtomicLong(nullObject, converter);
        // then
        assertNotNull(result);
        // Converter returns null for object types, but the toLong method returns a primitive long,
        // which the documentation says is 0 for null sources.
        assertEquals(0L, result.get(), "AtomicLong should contain 0 when the source object is null");
    }

    @Test
    void toAtomicLong_fromMapWithValueKey_returnsAtomicLongWithMappedValue() {
        // given: a map that follows the Converter's map‑to‑primitive convention
        // (a key named "_v" or "value" is typically used; we use "value" here)
        var map = java.util.Map.of("value", "98765");
        // when
        AtomicLong result = OffsetTimeConversions.toAtomicLong(map, converter);
        // then
        assertNotNull(result);
        Long expected = converter.convert(map, Long.class);
        assertEquals(expected.longValue(), result.get(), "AtomicLong should contain the long value extracted from the map");
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("provideToAtomicLongCases")
    void toAtomicLong_variousInputs(Object from, Converter conv, Object expected, String description) {
        if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
            // Expected an exception.
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, () -> OffsetTimeConversions.toAtomicLong(from, conv), description);
        } else {
            // Expected a successful conversion.
            AtomicLong result = OffsetTimeConversions.toAtomicLong(from, conv);
            assertNotNull(result, description);
            long expectedLong = ((Number) expected).longValue();
            assertEquals(expectedLong, result.get(), description);
        }
    }
}
