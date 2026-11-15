package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toAtomicLong_Test {

    private static Converter converter;

    /* --------------------------------------------------------------------- */
    private static final long EPOCH_MILLIS_2020_01_02T03_04_05Z = Instant.parse("2020-01-02T03:04:05Z").toEpochMilli();

    /* --------------------------------------------------------------------- */
    @Mock
    Converter // Mockito will inject a mock instance
    mockConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // The real Converter works with the default options – sufficient for the
        // majority of conversion scenarios.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static converter reference
        converter = null;
    }

    /* --------------------------------------------------------------------- */
    private static Stream<Arguments> numericProvider() {
        return Stream.of(Arguments.of(42, 42L, "Integer → AtomicLong"), Arguments.of(1234567890123L, 1234567890123L, "Long → AtomicLong"), Arguments.of(3.14, 3L, "Double → AtomicLong (truncation)"));
    }

    /* --------------------------------------------------------------------- */
    private static Stream<Arguments> stringProvider() {
        return Stream.of(Arguments.of("123456", 123456L, "Valid integer string"), Arguments.of("  98765  ", 98765L, "String with surrounding whitespace"));
    }

    @Test
    void testToAtomicLongFromLong() {
        Long source = 123L;
        AtomicLong result = LocalDateTimeConversions.toAtomicLong(source, converter);
        assertNotNull(result, "Resulting AtomicLong should not be null");
        assertEquals(123L, result.get(), "AtomicLong value should match the source long");
    }

    @Test
    void testToAtomicLongFromString() {
        String source = "456";
        AtomicLong result = LocalDateTimeConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(456L, result.get(), "String should be converted to the corresponding long value");
    }

    @Test
    void testToAtomicLongFromDate() {
        // Date#getTime() returns the epoch milliseconds as a long
        // a fixed instant
        Date source = new Date(1_600_000_000_000L);
        AtomicLong result = LocalDateTimeConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(1_600_000_000_000L, result.get(), "Date should be converted to its epoch millisecond representation");
    }

    @Test
    void testToAtomicLongFromNull() {
        AtomicLong result = LocalDateTimeConversions.toAtomicLong(null, converter);
        assertNotNull(result, "Result should be a non‑null AtomicLong even when source is null");
        assertEquals(0L, result.get(), "Null source should be converted to 0 according to Converter contract");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("numericProvider")
    void testNumericConversions(Object source, long expected, String displayName) {
        AtomicLong result = LocalDateTimeConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("stringProvider")
    void testStringConversionsValid(String source, long expected, String displayName) {
        AtomicLong result = LocalDateTimeConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    @Test
    void testStringConversionsNonIntegralNumber() {
        assertThrows(NumberFormatException.class, () -> LocalDateTimeConversions.toAtomicLong("12.34", converter), "Parsing a non‑integral numeric string should fail");
    }

    @Test
    void testStringConversionsNonNumeric() {
        assertThrows(NumberFormatException.class, () -> LocalDateTimeConversions.toAtomicLong("notANumber", converter), "Parsing a non‑numeric string should fail");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testNullSource() {
        AtomicLong result = LocalDateTimeConversions.toAtomicLong(null, converter);
        assertNotNull(result);
        assertEquals(0L, result.get(), "Null source should be treated as zero");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testBooleanTrue() {
        AtomicLong result = LocalDateTimeConversions.toAtomicLong(Boolean.TRUE, converter);
        assertEquals(1L, result.get());
    }

    @Test
    void testBooleanFalse() {
        AtomicLong result = LocalDateTimeConversions.toAtomicLong(Boolean.FALSE, converter);
        assertEquals(0L, result.get());
    }

    @Test
    void testTemporalStringIso() {
        AtomicLong result = LocalDateTimeConversions.toAtomicLong("2020-01-02T03:04:05", converter);
        assertEquals(EPOCH_MILLIS_2020_01_02T03_04_05Z, result.get(), "ISO‑8601 LocalDateTime string should be interpreted as UTC");
    }

    @Test
    void testTemporalLocalDateTimeInstance() {
        LocalDateTime ldt = LocalDateTime.of(2020, 1, 2, 3, 4, 5);
        AtomicLong result = LocalDateTimeConversions.toAtomicLong(ldt, converter);
        assertEquals(EPOCH_MILLIS_2020_01_02T03_04_05Z, result.get(), "LocalDateTime instance should be converted using UTC offset");
    }

    @Test
    void testTemporalDateInstance() {
        // 2021‑01‑01T00:00:00Z
        Date date = new Date(1_609_459_200_000L);
        AtomicLong result = LocalDateTimeConversions.toAtomicLong(date, converter);
        assertEquals(1_609_459_200_000L, result.get());
    }

    @Test
    void testTemporalCalendarInstance() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(1_609_459_200_000L);
        AtomicLong result = LocalDateTimeConversions.toAtomicLong(cal, converter);
        assertEquals(1_609_459_200_000L, result.get());
    }

    @Test
    void testTemporalInvalidFormat() {
        assertThrows(DateTimeParseException.class, () -> LocalDateTimeConversions.toAtomicLong("2020/01/02 03:04:05", converter));
        assertThrows(DateTimeParseException.class, () -> LocalDateTimeConversions.toAtomicLong("not-a-date-time", converter));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testBigDecimalConversion() {
        BigDecimal bd = new BigDecimal("12345.67");
        AtomicLong result = LocalDateTimeConversions.toAtomicLong(bd, converter);
        assertEquals(12345L, result.get(), "BigDecimal should be truncated toward zero");
    }

    @Test
    void testBigIntegerConversion() {
        BigInteger bi = new BigInteger("9876543210123456789");
        AtomicLong result = LocalDateTimeConversions.toAtomicLong(bi, converter);
        assertEquals(9876543210123456789L, result.get(), "BigInteger value that fits into a long should be preserved");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testStringOverflow() {
        assertThrows(NumberFormatException.class, () -> LocalDateTimeConversions.toAtomicLong("9223372036854775808", converter), "Value larger than Long.MAX_VALUE should overflow");
    }

    @Test
    void testNumberUnderflow() {
        // Use BigInteger because the literal is out of range for a Java long
        BigInteger under = new BigInteger("-9223372036854775809");
        assertThrows(NumberFormatException.class, () -> LocalDateTimeConversions.toAtomicLong(under, converter), "Value smaller than Long.MIN_VALUE should cause NumberFormatException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testUnsupportedObject() {
        Object unsupported = new Object();
        assertThrows(IllegalArgumentException.class, () -> LocalDateTimeConversions.toAtomicLong(unsupported, converter), "Unsupported source type should raise IllegalArgumentException");
    }

    @Test
    void testCustomConverterReturnsValue() {
        // Simulate a converter that knows how to turn the string "customValue" into Long 555
        when(mockConverter.convert(eq("customValue"), eq(Long.class))).thenReturn(555L);
        AtomicLong result = LocalDateTimeConversions.toAtomicLong("customValue", mockConverter);
        assertEquals(555L, result.get());
        verify(mockConverter).convert(eq("customValue"), eq(Long.class));
    }

    @Test
    void testCustomConverterThrowsRuntimeException() {
        when(mockConverter.convert(any(), eq(Long.class))).thenThrow(new RuntimeException("converter failure"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> LocalDateTimeConversions.toAtomicLong("anyValue", mockConverter));
        assertEquals("converter failure", ex.getMessage());
    }

    @Test
    void testCustomConverterReturnsNull() {
        when(mockConverter.convert(any(), eq(Long.class))).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> LocalDateTimeConversions.toAtomicLong("someString", mockConverter), "If the converter returns null, toLong cannot produce a primitive and should fail");
    }
}
