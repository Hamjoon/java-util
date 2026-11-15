package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class StringConversions_toAtomicLong_Test {

    private Converter converter;

    /**
     * Simple Converter that validates the supplied numeric string is within a
     * defined inclusive range. It delegates the actual parsing to {@link Long#parseLong(String)}.
     */
    private static class RangeConverter extends Converter {

        private final long low;

        private final long high;

        RangeConverter(long low, long high) {
            super(new ConverterOptions());
            this.low = low;
            this.high = high;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T convert(Object source, Class<T> target) {
            if (source == null) {
                throw new IllegalArgumentException("Source cannot be null");
            }
            long val = Long.parseLong(source.toString().trim());
            if (val < low || val > high) {
                throw new IllegalArgumentException("Value out of range");
            }
            return (T) Long.valueOf(val);
        }
    }

    /**
     * Dummy converter that does nothing special – used only to verify that a
     * non‑null converter does not affect the null‑source behaviour.
     */
    private static class DummyConverter extends Converter {

        DummyConverter() {
            super(new ConverterOptions());
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T convert(Object source, Class<T> target) {
            // For the purpose of the test we simply delegate to the default conversion.
            return super.convert(source, target);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // The Converter can be instantiated with default options.
        // ConverterOptions has a public no‑arg constructor.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test
        converter = null;
    }

    private static Stream<Arguments> directNumericProvider() {
        return Stream.of(// Integer
        Arguments.of(12345, 12345L), // Double (truncated)
        Arguments.of(12345.67, 12345L), // Long
        Arguments.of(9876543210L, 9876543210L), // Boolean true
        Arguments.of(true, 1L), // Boolean false
        Arguments.of(false, 0L));
    }

    private static Stream<Arguments> stringConversionProvider() {
        return Stream.of(Arguments.of("  42  ", 42L), Arguments.of("1234567890123", 1234567890123L), Arguments.of("12.34", 12L), Arguments.of("abc123", NumberFormatException.class));
    }

    private static Stream<Arguments> unsupportedSourceProvider() {
        return Stream.of(// java.util.Date
        Arguments.of(new Date()), // generic Object
        Arguments.of(new Object()), // java.net.URL
        Arguments.of(Mockito.mock(URL.class)), // java.lang.Character
        Arguments.of('A'), // StringBuilder
        Arguments.of(new StringBuilder("123")), // ByteBuffer
        Arguments.of(ByteBuffer.allocate(3)), // char[]
        Arguments.of(new char[] { '1', '2' }), // CharBuffer
        Arguments.of(CharBuffer.wrap("123")));
    }

    private static Stream<Arguments> numericWrapperProvider() {
        return Stream.of(Arguments.of((byte) 127, 127L), Arguments.of((short) 32767, 32767L), Arguments.of(3.14159f, 3L), Arguments.of(2.718281828459045, 2L));
    }

    private static Stream<Arguments> stringEdgeCaseProvider() {
        return Stream.of(Arguments.of("", NumberFormatException.class), Arguments.of("   ", NumberFormatException.class), Arguments.of("+12345", 12345L), Arguments.of("-9876", -9876L), Arguments.of("00000123", 123L), Arguments.of("1e3", NumberFormatException.class), Arguments.of("9223372036854775807", Long.MAX_VALUE), Arguments.of("-9223372036854775808", Long.MIN_VALUE));
    }

    private static Stream<Arguments> nanInfinityProvider() {
        return Stream.of(Arguments.of("NaN"), Arguments.of("Infinity"), Arguments.of("NaN_Double"), Arguments.of("-Infinity_Double"));
    }

    @Test
    void toAtomicLong_fromStringNumber_returnsCorrectAtomicLong() {
        // given
        String source = "12345";
        // when
        AtomicLong result = StringConversions.toAtomicLong(source, converter);
        // then
        assertNotNull(result, "Resulting AtomicLong should not be null");
        assertEquals(12345L, result.get(), "AtomicLong should contain the parsed long value");
    }

    @Test
    void toAtomicLong_fromLongObject_returnsSameValue() {
        // given
        Long source = 9876L;
        // when
        AtomicLong result = StringConversions.toAtomicLong(source, converter);
        // then
        assertNotNull(result);
        assertEquals(9876L, result.get());
    }

    @Test
    void toAtomicLong_fromIntegerNumber_returnsCorrectAtomicLong() {
        // given
        Integer source = 42;
        // when
        AtomicLong result = StringConversions.toAtomicLong(source, converter);
        // then
        assertNotNull(result);
        assertEquals(42L, result.get());
    }

    @Test
    void toAtomicLong_fromNull_returnsAtomicLongWithZero() {
        // given
        Object source = null;
        // when
        AtomicLong result = StringConversions.toAtomicLong(source, converter);
        // then
        assertNotNull(result);
        assertEquals(0L, result.get(), "When the source is null the conversion should yield 0 (the default for primitive long)");
    }

    // ---------- Null handling (both parameters null) ----------
    @Test
    void toAtomicLong_nullSourceAndNullConverter_returnsZero() {
        // Both arguments are null – the method should behave as if the source is null.
        AtomicLong result = StringConversions.toAtomicLong(null, null);
        assertNotNull(result);
        assertEquals(0L, result.get());
    }

    // ---------- Direct numeric types ----------
    @ParameterizedTest(name = "{index}: from={0} → {1}")
    @MethodSource("directNumericProvider")
    void toAtomicLong_directNumericTypes(Object from, long expected) {
        AtomicLong result = StringConversions.toAtomicLong(from, converter);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    // ---------- String conversions ----------
    @ParameterizedTest(name = "{index}: \"{0}\" → {1}")
    @MethodSource("stringConversionProvider")
    void toAtomicLong_stringConversions(String from, Object expected) {
        if (expected instanceof Class && ((Class<?>) expected).equals(NumberFormatException.class)) {
            assertThrows(NumberFormatException.class, () -> StringConversions.toAtomicLong(from, converter), "Expected NumberFormatException for input: \"" + from + "\"");
        } else {
            AtomicLong result = StringConversions.toAtomicLong(from, converter);
            assertNotNull(result);
            assertEquals(((Long) expected).longValue(), result.get(), "Incorrect conversion for input string: \"" + from + "\"");
        }
    }

    // ---------- Range validation via custom Converter ----------
    @Test
    void toAtomicLong_rangeConverter_insideRange() {
        Converter rangeConverter = new RangeConverter(50, 100);
        AtomicLong result = StringConversions.toAtomicLong("75", rangeConverter);
        assertNotNull(result);
        assertEquals(75L, result.get());
    }

    @Test
    void toAtomicLong_rangeConverter_outsideRange_throws() {
        Converter rangeConverter = new RangeConverter(50, 100);
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toAtomicLong("150", rangeConverter), "Value outside the supplied range should trigger IllegalArgumentException");
    }

    // ---------- Unsupported source types ----------
    @ParameterizedTest(name = "{index}: unsupported type {0}")
    @MethodSource("unsupportedSourceProvider")
    void toAtomicLong_unsupportedSourceTypes(Object from) {
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toAtomicLong(from, converter), "Unsupported source type should cause IllegalArgumentException");
    }

    // ---------- Null handling extended (null source, non‑null converter) ----------
    @Test
    void toAtomicLong_nullSourceWithNonNullConverter_returnsZero() {
        Converter dummy = new DummyConverter();
        AtomicLong result = StringConversions.toAtomicLong(null, dummy);
        assertNotNull(result);
        assertEquals(0L, result.get());
    }

    // ---------- Numeric wrapper types ----------
    @ParameterizedTest(name = "{index}: wrapper {0} → {1}")
    @MethodSource("numericWrapperProvider")
    void toAtomicLong_numericWrapperTypes(Object from, long expected) {
        AtomicLong result = StringConversions.toAtomicLong(from, converter);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    // ---------- Big number types ----------
    @Test
    void toAtomicLong_bigIntegerTooLarge_throws() {
        BigInteger big = new BigInteger("12345678901234567890");
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toAtomicLong(big, converter));
    }

    @Test
    void toAtomicLong_bigIntegerMinValue_accepted() {
        // Long.MIN_VALUE
        BigInteger min = new BigInteger("-9223372036854775808");
        AtomicLong result = StringConversions.toAtomicLong(min, converter);
        assertNotNull(result);
        assertEquals(Long.MIN_VALUE, result.get());
    }

    @Test
    void toAtomicLong_bigDecimalTruncated() {
        BigDecimal bd = new BigDecimal("99.99");
        AtomicLong result = StringConversions.toAtomicLong(bd, converter);
        assertNotNull(result);
        assertEquals(99L, result.get());
    }

    @Test
    void toAtomicLong_bigDecimalOverflow_throws() {
        BigDecimal bd = new BigDecimal("12345678901234567890.123");
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toAtomicLong(bd, converter));
    }

    // ---------- String edge cases ----------
    @ParameterizedTest(name = "{index}: \"{0}\" → {1}")
    @MethodSource("stringEdgeCaseProvider")
    void toAtomicLong_stringEdgeCases(String from, Object expected) {
        if (expected instanceof Class && ((Class<?>) expected).equals(NumberFormatException.class)) {
            assertThrows(NumberFormatException.class, () -> StringConversions.toAtomicLong(from, converter));
        } else {
            AtomicLong result = StringConversions.toAtomicLong(from, converter);
            assertNotNull(result);
            assertEquals(((Long) expected).longValue(), result.get(), "Failed conversion for edge case string: \"" + from + "\"");
        }
    }

    // ---------- Overflow / Underflow handling ----------
    @Test
    void toAtomicLong_overflowString_throws() {
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toAtomicLong("9223372036854775808", converter));
    }

    @Test
    void toAtomicLong_underflowString_throws() {
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toAtomicLong("-9223372036854775809", converter));
    }

    // ---------- Atomic source types ----------
    @Test
    void toAtomicLong_fromAtomicLong_unwrapped() {
        AtomicLong src = new AtomicLong(55555L);
        AtomicLong result = StringConversions.toAtomicLong(src, converter);
        assertNotNull(result);
        assertEquals(55555L, result.get());
    }

    @Test
    void toAtomicLong_fromAtomicInteger_unwrapped() {
        AtomicInteger src = new AtomicInteger(777);
        AtomicLong result = StringConversions.toAtomicLong(src, converter);
        assertNotNull(result);
        assertEquals(777L, result.get());
    }

    @Test
    void toAtomicLong_fromAtomicBooleanTrue() {
        AtomicBoolean src = new AtomicBoolean(true);
        AtomicLong result = StringConversions.toAtomicLong(src, converter);
        assertNotNull(result);
        assertEquals(1L, result.get());
    }

    @Test
    void toAtomicLong_fromAtomicBooleanFalse() {
        AtomicBoolean src = new AtomicBoolean(false);
        AtomicLong result = StringConversions.toAtomicLong(src, converter);
        assertNotNull(result);
        assertEquals(0L, result.get());
    }

    // ---------- NaN and Infinity handling ----------
    @ParameterizedTest(name = "{index}: special value \"{0}\" → IllegalArgumentException")
    @MethodSource("nanInfinityProvider")
    void toAtomicLong_nanInfinityHandling(String value) {
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toAtomicLong(value, converter));
    }

    // ---------- Custom converter side‑effects ----------
    @Test
    void toAtomicLong_converterReturnsNull_throwsIllegalArgument() {
        Converter nullReturning = Mockito.mock(Converter.class);
        // Simulate a converter that returns null for any conversion request
        Mockito.when(nullReturning.convert(any(), eq(Long.class))).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toAtomicLong("42", nullReturning), "Converter returning null should be treated as illegal conversion");
    }

    @Test
    void toAtomicLong_converterReturnsInvalidType_throwsIllegalArgument() {
        Converter invalidReturn = Mockito.mock(Converter.class);
        // Return a String instead of a Long
        Mockito.when(invalidReturn.convert(any(), eq(Long.class))).thenReturn((Long) (Object) "not-a-long");
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toAtomicLong("42", invalidReturn));
    }

    @Test
    void toAtomicLong_converterThrowsRuntimeException_propagates() {
        Converter exceptionThrower = Mockito.mock(Converter.class);
        Mockito.when(exceptionThrower.convert(any(), eq(Long.class))).thenThrow(new RuntimeException("boom"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> StringConversions.toAtomicLong("10", exceptionThrower));
        assertEquals("boom", ex.getMessage());
    }

    // ---------- Unsupported source type – custom Number subclass ----------
    @Test
    void toAtomicLong_customNumberSubclass_throwsIllegalArgument() {
        // Simulate a custom Number subclass (we just use an anonymous subclass)
        Number customNumber = new Number() {

            @Override
            public int intValue() {
                return 0;
            }

            @Override
            public long longValue() {
                return 0L;
            }

            @Override
            public float floatValue() {
                return 0f;
            }

            @Override
            public double doubleValue() {
                return 0d;
            }

            @Override
            public String toString() {
                return "1e20";
            }
        };
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toAtomicLong(customNumber, converter));
    }
}
