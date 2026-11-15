package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_toTimestamp_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    @Mock
    private Converter // used for cases where a custom conversion is needed
    mockConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter requires a ConverterOptions instance; the default options are sufficient for our tests.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly necessary but keeps the pattern tidy.
        converter = null;
    }

    /* --------------------------------------------------------------
     *  Parameterized test for a few valid numeric conversions
     * -------------------------------------------------------------- */
    private static Stream<Arguments> numericSourceProvider() {
        return Stream.of(Arguments.of(1672578896123L, Timestamp.from(Instant.ofEpochMilli(1672578896123L))), Arguments.of(BigDecimal.valueOf(1672578896.123456789), Timestamp.from(Instant.ofEpochSecond(1672578896L, 123_456_789L))), Arguments.of(BigInteger.valueOf(1672578896123L), Timestamp.from(Instant.ofEpochMilli(1672578896123L))));
    }

    @Test
    void toTimestamp_fromDate_returnsEquivalentTimestamp() {
        // GIVEN
        // 2021‑10‑01T00:00:00Z
        long epochMillis = 1_633_072_800_000L;
        Date source = new Date(epochMillis);
        // WHEN
        Timestamp result = BigDecimalConversions.toTimestamp(source, converter);
        // THEN
        assertNotNull(result, "Resulting Timestamp should not be null");
        assertEquals(epochMillis, result.getTime(), "Timestamp should represent the same epoch milliseconds as the source Date");
    }

    @Test
    void toTimestamp_fromInstant_returnsEquivalentTimestamp() {
        // GIVEN
        // 2021‑10‑01T00:00:00Z
        Instant source = Instant.ofEpochMilli(1_633_072_800_000L);
        // WHEN
        Timestamp result = BigDecimalConversions.toTimestamp(source, converter);
        // THEN
        assertNotNull(result, "Resulting Timestamp should not be null");
        assertEquals(source.toEpochMilli(), result.getTime(), "Timestamp should represent the same epoch milliseconds as the source Instant");
    }

    @Test
    void toTimestamp_fromBigDecimalSeconds_returnsEquivalentTimestamp() {
        // GIVEN
        // BigDecimal representing epoch seconds (no fractional part)
        // 2021‑10‑01T00:00:00Z in seconds
        BigDecimal source = BigDecimal.valueOf(1_633_072_800L);
        Timestamp expected = Timestamp.from(Instant.ofEpochSecond(source.longValue()));
        // WHEN
        Timestamp result = BigDecimalConversions.toTimestamp(source, converter);
        // THEN
        assertNotNull(result, "Resulting Timestamp should not be null");
        assertEquals(expected, result, "Timestamp produced from BigDecimal seconds should match the expected value");
    }

    @Test
    void toTimestamp_fromNull_returnsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        Timestamp result = BigDecimalConversions.toTimestamp(source, converter);
        // THEN
        assertNull(result, "Converting a null source should yield a null Timestamp");
    }

    /* --------------------------------------------------------------
     *  Valid conversions – direct Instant
     * -------------------------------------------------------------- */
    @Test
    void toTimestamp_fromInstant_returnsExactTimestamp() {
        // GIVEN
        Instant source = Instant.parse("2023-01-01T12:34:56.123456789Z");
        // WHEN
        Timestamp result = BigDecimalConversions.toTimestamp(source, converter);
        // THEN
        assertNotNull(result);
        assertEquals(source.toEpochMilli(), result.getTime(), "Epoch milliseconds must match");
        assertEquals(source.getNano(), result.getNanos(), "Nanosecond part must be preserved");
    }

    /* --------------------------------------------------------------
     *  Valid conversions – java.sql.Timestamp source
     * -------------------------------------------------------------- */
    @Test
    void toTimestamp_fromSqlTimestamp_returnsEqualTimestamp() {
        // GIVEN
        Timestamp source = new Timestamp(1672578896123L);
        // add nanos beyond the millisecond part
        source.setNanos(123456789);
        // WHEN
        Timestamp result = BigDecimalConversions.toTimestamp(source, converter);
        // THEN
        assertNotNull(result);
        assertEquals(source, result, "Result should be equal to the original Timestamp");
    }

    /* --------------------------------------------------------------
     *  Valid conversions – Calendar source
     * -------------------------------------------------------------- */
    @Test
    void toTimestamp_fromCalendar_returnsEquivalentTimestamp() {
        // GIVEN
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        // 2023‑01‑01T12:34:56.123Z
        calendar.setTimeInMillis(1_672_578_896_123L);
        // WHEN
        Timestamp result = BigDecimalConversions.toTimestamp(calendar, converter);
        // THEN
        assertNotNull(result);
        assertEquals(calendar.getTimeInMillis(), result.getTime(), "Timestamp should reflect the Calendar's millisecond value");
    }

    /* --------------------------------------------------------------
     *  Valid conversions – Long epoch‑millis source
     * -------------------------------------------------------------- */
    @Test
    void toTimestamp_fromLong_returnsEquivalentTimestamp() {
        // GIVEN
        // 2023‑01‑01T12:34:56.123Z
        Long epochMillis = 1_672_578_896_123L;
        // WHEN
        Timestamp result = BigDecimalConversions.toTimestamp(epochMillis, converter);
        // THEN
        assertNotNull(result);
        assertEquals(epochMillis.longValue(), result.getTime(), "Timestamp should represent the same epoch milliseconds as the source Long");
    }

    /* --------------------------------------------------------------
     *  Valid conversions – BigDecimal seconds source (fractional)
     * -------------------------------------------------------------- */
    @Test
    void toTimestamp_fromBigDecimalSecondsWithFraction_returnsExactTimestamp() {
        // GIVEN
        // 1672578896.123456789 seconds = 2023‑01‑01T12:34:56.123456789Z
        BigDecimal source = new BigDecimal("1672578896.123456789");
        Instant expected = Instant.ofEpochSecond(1672578896L, 123_456_789L);
        Timestamp expectedTs = Timestamp.from(expected);
        // WHEN
        Timestamp result = BigDecimalConversions.toTimestamp(source, converter);
        // THEN
        assertNotNull(result);
        assertEquals(expectedTs, result, "Timestamp produced from BigDecimal seconds must match the expected instant");
    }

    /* --------------------------------------------------------------
     *  Valid conversions – LocalDateTime with supplied Converter
     * -------------------------------------------------------------- */
    @Test
    void toTimestamp_fromLocalDateTime_usingCustomConverter() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2023, 1, 1, 12, 34, 56, 123_456_789);
        Instant expectedInstant = ldt.atZone(ZoneOffset.UTC).toInstant();
        // Mock the converter to return the expected Instant when asked to convert the LocalDateTime.
        when(mockConverter.convert(eq(ldt), eq(Instant.class))).thenReturn(expectedInstant);
        // WHEN
        Timestamp result = BigDecimalConversions.toTimestamp(ldt, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals(Timestamp.from(expectedInstant), result);
        verify(mockConverter, times(1)).convert(eq(ldt), eq(Instant.class));
    }

    /* --------------------------------------------------------------
     *  Null handling – both arguments null (should still return null)
     * -------------------------------------------------------------- */
    @Test
    void toTimestamp_bothNull_returnsNull() {
        assertNull(BigDecimalConversions.toTimestamp(null, null));
    }

    /* --------------------------------------------------------------
     *  Invalid type – unsupported source without converter
     * -------------------------------------------------------------- */
    @Test
    void toTimestamp_unsupportedString_withoutConverter_throwsException() {
        // GIVEN
        String source = "not a date";
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> BigDecimalConversions.toTimestamp(source, converter), "Unsupported source type should cause an IllegalArgumentException");
    }

    /* --------------------------------------------------------------
     *  Invalid type – unsupported object with failing converter
     * -------------------------------------------------------------- */
    @Test
    void toTimestamp_unsupportedObject_converterThrows_propagatesException() {
        // GIVEN
        Object source = new Object();
        when(mockConverter.convert(eq(source), eq(Instant.class))).thenThrow(new UnsupportedOperationException("cannot convert"));
        // WHEN / THEN
        assertThrows(UnsupportedOperationException.class, () -> BigDecimalConversions.toTimestamp(source, mockConverter));
    }

    /* --------------------------------------------------------------
     *  Missing converter for LocalDateTime
     * -------------------------------------------------------------- */
    @Test
    void toTimestamp_localDateTimeWithoutConverter_throwsException() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2023, 1, 1, 12, 0, 0);
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> BigDecimalConversions.toTimestamp(ldt, converter), "LocalDateTime without a Converter should raise an exception");
    }

    /* --------------------------------------------------------------
     *  Converter returns null – leads to NPE inside Timestamp.from
     * -------------------------------------------------------------- */
    @Test
    void toTimestamp_converterReturnsNull_throwsNullPointerException() {
        // GIVEN
        String source = "anyObject";
        when(mockConverter.convert(eq(source), eq(Instant.class))).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> BigDecimalConversions.toTimestamp(source, mockConverter));
    }

    /* --------------------------------------------------------------
     *  Instant out of Timestamp range (Instant.MAX)
     * -------------------------------------------------------------- */
    @Test
    void toTimestamp_instantMax_throwsIllegalArgumentException() {
        // GIVEN
        Instant source = Instant.MAX;
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> BigDecimalConversions.toTimestamp(source, converter), "Instant.MAX is outside the range representable by java.sql.Timestamp");
    }

    /* --------------------------------------------------------------
     *  Malformed ISO‑8601 string
     * -------------------------------------------------------------- */
    @Test
    void toTimestamp_malformedIsoString_throwsDateTimeParseException() {
        // GIVEN
        // February 30 does not exist
        String source = "2023-02-30T12:00:00Z";
        // WHEN / THEN
        assertThrows(DateTimeParseException.class, () -> BigDecimalConversions.toTimestamp(source, converter));
    }

    /* --------------------------------------------------------------
     *  BigDecimal overflow beyond Timestamp capacity
     * -------------------------------------------------------------- */
    @Test
    void toTimestamp_bigDecimalOverflow_throwsException() {
        // GIVEN
        BigDecimal huge = new BigDecimal(Long.MAX_VALUE).multiply(BigDecimal.valueOf(1_000_000_000L));
        // WHEN / THEN
        assertThrows(ArithmeticException.class, () -> BigDecimalConversions.toTimestamp(huge, converter), "Overflowing BigDecimal should raise an exception");
    }

    /* --------------------------------------------------------------
     *  Unsupported numeric type (Float) without converter
     * -------------------------------------------------------------- */
    @Test
    void toTimestamp_floatWithoutConverter_throwsException() {
        // GIVEN
        Float source = Float.valueOf(12345.67f);
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> BigDecimalConversions.toTimestamp(source, converter), "Float is not a supported source type without a custom Converter");
    }

    @ParameterizedTest
    @MethodSource("numericSourceProvider")
    void toTimestamp_numericSources_returnCorrectTimestamp(Object source, Timestamp expected) {
        Timestamp result = BigDecimalConversions.toTimestamp(source, converter);
        assertNotNull(result);
        assertEquals(expected, result);
    }

    /* --------------------------------------------------------------
     *  UUID conversion – not supported, should throw
     * -------------------------------------------------------------- */
    @Test
    void toTimestamp_uuidWithoutConverter_throwsException() {
        // GIVEN
        UUID source = UUID.randomUUID();
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> BigDecimalConversions.toTimestamp(source, converter));
    }
}
