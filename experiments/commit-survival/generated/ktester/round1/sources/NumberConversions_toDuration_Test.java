package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toDuration_Test {

    @Mock
    private Converter // mocked Converter used in the tests
    mockConverter;

    /**
     * Simple converter that parses a {@link String} into a {@link Long}.
     */
    private static class StringToNumberConverter implements Converter {

        @SuppressWarnings("unchecked")
        @Override
        public <T> T convert(Object from, Class<T> toClass) {
            if (from == null)
                return null;
            if (toClass == Long.class) {
                return (T) Long.valueOf((String) from);
            }
            throw new IllegalArgumentException("Unsupported target type: " + toClass);
        }
    }

    /**
     * Converter that always throws a {@link RuntimeException}.
     */
    private static class ThrowingConverter implements Converter {

        @Override
        public <T> T convert(Object from, Class<T> toClass) {
            throw new RuntimeException("forced exception");
        }
    }

    /**
     * Converter that returns {@code null} regardless of input.
     */
    private static class NullReturningConverter implements Converter {

        @Override
        public <T> T convert(Object from, Class<T> toClass) {
            return null;
        }
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the mock to avoid interaction bleed‑over between tests
        reset(mockConverter);
    }

    /**
     * Parses the ISO‑8601 duration string used in the JSON spec.
     */
    private static Duration parseIsoDuration(String iso) {
        // The JSON uses the same format as Duration.parse, e.g. "PT0.123S"
        return Duration.parse(iso);
    }

    private static Stream<Arguments> numericConversionProvider() {
        return Stream.of(Arguments.of(123L, "PT0.123S"), Arguments.of(Integer.valueOf(456), "PT0.456S"), Arguments.of(Short.valueOf((short) 78), "PT0.078S"), Arguments.of(Byte.valueOf((byte) 9), "PT0.009S"), Arguments.of(Double.valueOf(2500.75), "PT2.50075S"), Arguments.of(Float.valueOf(1234.5f), "PT1.234S"));
    }

    /**
     * Verify that {@code NumberConversions.toDuration} delegates to {@code NumberConversions.toLong},
     * which in turn uses the supplied {@link Converter}.  The mock is configured to return a
     * specific {@code Long} value when the converter is asked to convert the input.
     */
    @Test
    void toDuration_usesConverterToLong_andCreatesCorrectDuration() {
        // Arrange
        String source = "12345";
        // The internal implementation of NumberConversions.toLong eventually calls
        // converter.convert(value, Long.class).  We stub that call.
        when(mockConverter.convert(eq(source), eq(Long.class))).thenReturn(12345L);
        // Act
        Duration result = NumberConversions.toDuration(source, mockConverter);
        // Assert
        assertEquals(Duration.ofMillis(12345L), result, "Duration should be created from the long value supplied by the Converter");
        // Verify that the converter was indeed asked to perform the conversion
        verify(mockConverter, times(1)).convert(eq(source), eq(Long.class));
    }

    /**
     * When the source object is already a {@link Number}, the conversion path may bypass the
     * {@link Converter}.  This test supplies a {@code Long} directly and expects the resulting
     * {@link Duration} to match the supplied millisecond value.
     */
    @Test
    void toDuration_withLongInput_returnsDurationOfSameMillis() {
        // Arrange
        // 7.5 seconds expressed in milliseconds
        Long source = 7_500L;
        // Act
        Duration result = NumberConversions.toDuration(source, mockConverter);
        // Assert
        assertEquals(Duration.ofMillis(7_500L), result, "Duration should directly reflect the long value when the source is a Long");
        // No interaction with the converter is expected for a pure Long input
        verifyNoInteractions(mockConverter);
    }

    /**
     * Verify that a {@link Number} of a different type (e.g., {@link Integer}) is correctly
     * transformed into a {@link Duration}.  The mock converter should not be invoked because
     * the static {@code toLong} method can extract the primitive value directly.
     */
    @Test
    void toDuration_withIntegerInput_returnsDurationOfSameMillis() {
        // Arrange
        // milliseconds
        Integer source = 42;
        // Act
        Duration result = NumberConversions.toDuration(source, mockConverter);
        // Assert
        assertEquals(Duration.ofMillis(42L), result, "Duration should correctly handle Integer input");
        verifyNoInteractions(mockConverter);
    }

    /**
     * Ensure that a {@code null} source yields a {@code Duration} of zero milliseconds.
     * The implementation of {@code toLong} treats {@code null} as zero for primitive
     * conversions, so we expect the same behaviour here.
     */
    @Test
    void toDuration_withNullInput_returnsZeroDuration() {
        // Act
        Duration result = NumberConversions.toDuration(null, mockConverter);
        // Assert
        assertEquals(Duration.ofMillis(0L), result, "Null input should be interpreted as zero milliseconds");
        // No conversion should be attempted
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toDuration_nullSource_andNullConverter_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> NumberConversions.toDuration(null, null), "Passing a null source and null converter should NPE (or IAE) per spec");
    }

    @Test
    void toDuration_unsupportedObject_andNullConverter_throwsIllegalArgumentException() {
        Object unsupported = new Object();
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toDuration(unsupported, null), "Unsupported source type should cause IllegalArgumentException");
    }

    @Test
    void toDuration_nonNumericString_withStringToNumberConverter_throwsNumberFormatException() {
        String source = "not-a-number";
        Converter conv = new StringToNumberConverter();
        assertThrows(NumberFormatException.class, () -> NumberConversions.toDuration(source, conv), "Non‑numeric string should cause NumberFormatException");
    }

    @Test
    void toDuration_durationObject_asSource_throwsIllegalArgumentException() {
        Duration src = Duration.ofSeconds(5);
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toDuration(src, null), "Passing a Duration as source is unsupported");
    }

    @ParameterizedTest(name = "toDuration({0}) → {1}")
    @MethodSource("numericConversionProvider")
    void toDuration_validNumericConversions(Object source, String expectedIso) {
        Duration expected = parseIsoDuration(expectedIso);
        Duration actual = NumberConversions.toDuration(source, mockConverter);
        assertEquals(expected, actual, () -> "Expected " + expected + " for source " + source);
        // No converter interaction for plain numbers
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toDuration_zeroLong_returnsZeroDuration() {
        assertEquals(parseIsoDuration("PT0S"), NumberConversions.toDuration(0L, mockConverter));
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toDuration_negativeLong_returnsNegativeDuration() {
        assertEquals(parseIsoDuration("PT-0.5S"), NumberConversions.toDuration(-500L, mockConverter));
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toDuration_longMaxValue_returnsLargestDuration() {
        Duration expected = Duration.ofMillis(Long.MAX_VALUE);
        assertEquals(expected, NumberConversions.toDuration(Long.MAX_VALUE, mockConverter));
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toDuration_longMinValue_returnsMostNegativeDuration() {
        Duration expected = Duration.ofMillis(Long.MIN_VALUE);
        assertEquals(expected, NumberConversions.toDuration(Long.MIN_VALUE, mockConverter));
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toDuration_instantConvertedToDuration() {
        Instant src = Instant.ofEpochMilli(1500L);
        assertEquals(parseIsoDuration("PT1.5S"), NumberConversions.toDuration(src, mockConverter));
    }

    @Test
    void toDuration_dateConvertedToDuration() {
        Date src = new Date(2500L);
        assertEquals(parseIsoDuration("PT2.5S"), NumberConversions.toDuration(src, mockConverter));
    }

    @Test
    void toDuration_timestampConvertedToDuration() {
        Timestamp src = Timestamp.valueOf("1970-01-01 00:00:03.123");
        assertEquals(parseIsoDuration("PT3.123S"), NumberConversions.toDuration(src, mockConverter));
    }

    @Test
    void toDuration_localDateTimeConvertedToDuration() {
        LocalDateTime src = LocalDateTime.of(1970, 1, 1, 0, 0, 4, 567_000_000);
        assertEquals(parseIsoDuration("PT4.567S"), NumberConversions.toDuration(src, mockConverter));
    }

    @Test
    void toDuration_zonedDateTimeConvertedToDuration() {
        ZonedDateTime src = ZonedDateTime.parse("1970-01-01T00:00:05.890Z");
        assertEquals(parseIsoDuration("PT5.89S"), NumberConversions.toDuration(src, mockConverter));
    }

    @Test
    void toDuration_offsetDateTimeConvertedToDuration() {
        OffsetDateTime src = OffsetDateTime.parse("1970-01-01T00:00:06.001+00:00");
        assertEquals(parseIsoDuration("PT6.001S"), NumberConversions.toDuration(src, mockConverter));
    }

    @Test
    void toDuration_yearUnsupported_throwsIllegalArgumentException() {
        Year src = Year.of(1970);
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toDuration(src, mockConverter));
    }

    @Test
    void toDuration_atomicIntegerConverted() {
        AtomicInteger src = new AtomicInteger(777);
        assertEquals(parseIsoDuration("PT0.777S"), NumberConversions.toDuration(src, mockConverter));
    }

    @Test
    void toDuration_atomicLongConverted() {
        AtomicLong src = new AtomicLong(1999L);
        assertEquals(parseIsoDuration("PT1.999S"), NumberConversions.toDuration(src, mockConverter));
    }

    @Test
    void toDuration_atomicBooleanUnsupported_throwsIllegalArgumentException() {
        AtomicBoolean src = new AtomicBoolean(true);
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toDuration(src, mockConverter));
    }

    @Test
    void toDuration_bigIntegerConverted() {
        BigInteger src = new BigInteger("1234567890123");
        assertEquals(parseIsoDuration("PT1234567890.123S"), NumberConversions.toDuration(src, mockConverter));
    }

    @Test
    void toDuration_bigDecimalConverted() {
        BigDecimal src = new BigDecimal("9876.543");
        assertEquals(parseIsoDuration("PT9.876S"), NumberConversions.toDuration(src, mockConverter));
    }

    @Test
    void toDuration_bigIntegerOverflow_throwsIllegalArgumentException() {
        // Long.MAX_VALUE + 1
        BigInteger src = new BigInteger("9223372036854775808");
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toDuration(src, mockConverter));
    }

    @Test
    void toDuration_stringWithCustomConverter() {
        Converter conv = new StringToNumberConverter();
        assertEquals(parseIsoDuration("PT12.345S"), NumberConversions.toDuration("12345", conv));
    }

    @Test
    void toDuration_negativeStringWithCustomConverter() {
        Converter conv = new StringToNumberConverter();
        assertEquals(parseIsoDuration("PT-3S"), NumberConversions.toDuration("-3000", conv));
    }

    @Test
    void toDuration_localDateUnsupported() {
        LocalDate src = LocalDate.of(2020, 5, 15);
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toDuration(src, mockConverter));
    }

    @Test
    void toDuration_calendarUnsupported() {
        Calendar src = new Calendar.Builder().setInstant(1000L).build();
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toDuration(src, mockConverter));
    }

    @Test
    void toDuration_sqlDateUnsupported() {
        java.sql.Date src = java.sql.Date.valueOf("2022-01-01");
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toDuration(src, mockConverter));
    }

    @Test
    void toDuration_converterThrowsRuntimeException_propagates() {
        Converter conv = new ThrowingConverter();
        assertThrows(RuntimeException.class, () -> NumberConversions.toDuration("12345", conv));
    }

    @Test
    void toDuration_converterReturnsNull_causesNullPointerException() {
        Converter conv = new NullReturningConverter();
        assertThrows(NullPointerException.class, () -> NumberConversions.toDuration(123L, conv));
    }

    @Test
    void toDuration_doubleOverflow_throwsIllegalArgumentException() {
        Double src = Double.MAX_VALUE;
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toDuration(src, mockConverter));
    }

    @Test
    void toDuration_bigDecimalOverflow_throwsIllegalArgumentException() {
        // > Long.MAX_VALUE
        BigDecimal src = new BigDecimal("9223372036854775808");
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toDuration(src, mockConverter));
    }

    @Test
    void toDuration_floatNaN_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toDuration(Float.NaN, mockConverter));
    }

    @Test
    void toDuration_floatPositiveInfinity_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toDuration(Float.POSITIVE_INFINITY, mockConverter));
    }

    @Test
    void toDuration_doubleNegativeInfinity_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toDuration(Double.NEGATIVE_INFINITY, mockConverter));
    }

    @Test
    void toDuration_numberSubclassThrowingException_propagatesRuntimeException() {
        Number badNumber = new Number() {

            @Override
            public int intValue() {
                return 0;
            }

            @Override
            public long longValue() {
                throw new RuntimeException("bad");
            }

            @Override
            public float floatValue() {
                return 0f;
            }

            @Override
            public double doubleValue() {
                return 0d;
            }
        };
        assertThrows(RuntimeException.class, () -> NumberConversions.toDuration(badNumber, mockConverter));
    }
}
