package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.format.DateTimeParseException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toDate_Test {

    private static TimeZone originalDefaultZone;

    private Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Force a deterministic time‑zone for the conversion logic
        originalDefaultZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @BeforeEach
    void setupBeforeEach() {
        // Converter with default options – no custom overrides needed for these tests
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore the original default zone
        TimeZone.setDefault(originalDefaultZone);
    }

    /* --------------------------------------------------------------
     *  Parameterized tests for the “Valid conversions – numeric and date types”
     * -------------------------------------------------------------- */
    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Long (epoch millis)
        Arguments.of(1622548800000L, 1622548800000L), // Integer (treated as long)
        Arguments.of(123456789, 123456789L), // java.util.Date
        Arguments.of(new java.util.Date(1609459200000L), 1609459200000L), // java.sql.Timestamp (millis part only)
        Arguments.of(Timestamp.valueOf("2021-07-15 08:30:45"), 1626337845000L), // java.time.Instant
        Arguments.of(Instant.ofEpochMilli(1610000000000L), 1610000000000L), // java.time.LocalDate (start of day UTC)
        Arguments.of(LocalDate.of(2021, 6, 1), LocalDate.of(2021, 6, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()), // java.time.LocalDateTime (interpreted in UTC)
        Arguments.of(LocalDateTime.of(2021, 6, 1, 12, 34, 56), LocalDateTime.of(2021, 6, 1, 12, 34, 56).atZone(ZoneOffset.UTC).toInstant().toEpochMilli()), // ZonedDateTime (converted to its instant)
        Arguments.of(ZonedDateTime.of(2021, 6, 1, 12, 0, 0, 0, // 12:00 CEST = 10:00 UTC
        ZoneId.of("Europe/Paris")), ZonedDateTime.of(2021, 6, 1, 12, 0, 0, 0, ZoneId.of("Europe/Paris")).toInstant().toEpochMilli()), // OffsetDateTime
        Arguments.of(OffsetDateTime.parse("2021-06-01T12:00:00+02:00"), OffsetDateTime.parse("2021-06-01T12:00:00+02:00").toInstant().toEpochMilli()), // YearMonth (first day of month at start of day UTC)
        Arguments.of(YearMonth.of(2021, 6), YearMonth.of(2021, 6).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()), // Year (Jan 1st at start of day UTC)
        Arguments.of(Year.of(2021), Year.of(2021).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()), // ISO‑8601 string with Z
        Arguments.of("2021-06-01T12:34:56Z", 1622550896000L), // ISO‑8601 string with offset +05:30 (converted to UTC)
        Arguments.of("2021-06-01T12:34:56+05:30", 1622544296000L), // Numeric string representing seconds since epoch (converted to ms)
        Arguments.of("1622548800", 1622548800000L));
    }

    @Test
    void toDate_nullInput_returnsEpoch() {
        Date result = LocalDateConversions.toDate(null, converter);
        assertNotNull(result);
        assertEquals(0L, result.getTime(), "null should be converted to epoch (0 ms)");
    }

    @Test
    void toDate_DateInput_returnsSameInstant() {
        // some arbitrary timestamp
        Date source = new Date(1_600_000_000_000L);
        Date result = LocalDateConversions.toDate(source, converter);
        assertNotNull(result);
        assertEquals(source.getTime(), result.getTime(), "Date input should be converted to a Date with the same epoch millis");
    }

    @Test
    void toDate_LongInput_returnsCorrespondingDate() {
        Long millis = 1_600_000_000_123L;
        Date result = LocalDateConversions.toDate(millis, converter);
        assertNotNull(result);
        assertEquals(millis.longValue(), result.getTime(), "Long input should be interpreted as epoch milliseconds");
    }

    @Test
    void toDate_CalendarInput_returnsCorrespondingDate() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(1_600_000_000_456L);
        Date result = LocalDateConversions.toDate(cal, converter);
        assertNotNull(result);
        assertEquals(cal.getTimeInMillis(), result.getTime(), "Calendar input should be converted to a Date with the same epoch millis");
    }

    @Test
    void toDate_InstantInput_returnsCorrespondingDate() {
        Instant instant = Instant.ofEpochMilli(1_600_000_000_789L);
        Date result = LocalDateConversions.toDate(instant, converter);
        assertNotNull(result);
        assertEquals(instant.toEpochMilli(), result.getTime(), "Instant input should be converted to a Date with the same epoch millis");
    }

    @Test
    void toDate_LocalDateInput_returnsStartOfDayUTC() {
        LocalDate localDate = LocalDate.of(2020, Month.JANUARY, 15);
        Date expected = Date.from(localDate.atStartOfDay(ZoneOffset.UTC).toInstant());
        Date result = LocalDateConversions.toDate(localDate, converter);
        assertNotNull(result);
        assertEquals(expected.getTime(), result.getTime(), "LocalDate should be converted to the start‑of‑day UTC instant");
    }

    @Test
    void toDate_LocalDateTimeInput_returnsCorrespondingInstantUTC() {
        LocalDateTime ldt = LocalDateTime.of(2020, Month.FEBRUARY, 20, 13, 45, 30, 123_000_000);
        Date expected = Date.from(ldt.atZone(ZoneOffset.UTC).toInstant());
        Date result = LocalDateConversions.toDate(ldt, converter);
        assertNotNull(result);
        assertEquals(expected.getTime(), result.getTime(), "LocalDateTime should be converted using UTC zone");
    }

    @Test
    void toDate_ZonedDateTimeInput_returnsCorrespondingInstant() {
        ZonedDateTime zdt = ZonedDateTime.of(2021, 3, 10, 8, 15, 0, 0, ZoneId.of("America/New_York"));
        Date expected = Date.from(zdt.toInstant());
        Date result = LocalDateConversions.toDate(zdt, converter);
        assertNotNull(result);
        assertEquals(expected.getTime(), result.getTime(), "ZonedDateTime should be converted to its instant");
    }

    @Test
    void toDate_OffsetDateTimeInput_returnsCorrespondingInstant() {
        OffsetDateTime odt = OffsetDateTime.of(2022, 4, 5, 12, 30, 45, 0, ZoneOffset.ofHours(-5));
        Date expected = Date.from(odt.toInstant());
        Date result = LocalDateConversions.toDate(odt, converter);
        assertNotNull(result);
        assertEquals(expected.getTime(), result.getTime(), "OffsetDateTime should be converted to its instant");
    }

    @Test
    void toDate_TimestampInput_returnsCorrespondingDate() {
        Timestamp ts = new Timestamp(1_600_000_001_234L);
        // extra nanos are ignored for Date conversion
        ts.setNanos(567_000_000);
        Date expected = new Date(ts.getTime());
        Date result = LocalDateConversions.toDate(ts, converter);
        assertNotNull(result);
        assertEquals(expected.getTime(), result.getTime(), "Timestamp should be converted to a Date using its millisecond value");
    }

    @ParameterizedTest(name = "[{index}] toDate({0}) → epoch {1}")
    @MethodSource("validConversionProvider")
    void toDate_validConversions(Object from, long expectedEpochMillis) {
        Date result = LocalDateConversions.toDate(from, converter);
        assertNotNull(result);
        assertEquals(expectedEpochMillis, result.getTime(), () -> "Conversion of " + from + " should yield epoch " + expectedEpochMillis);
    }

    /* --------------------------------------------------------------
     *  Edge cases – null and empty inputs
     * -------------------------------------------------------------- */
    @Test
    void toDate_nullInput_throwsException() {
        // According to the JSON spec this should throw an exception (the original test
        // in the framework expects epoch 0, but the spec also contains a contradictory
        // expectation – we test the exception variant here).
        assertThrows(RuntimeException.class, () -> LocalDateConversions.toDate(null, converter), "Null source should result in an exception");
    }

    @Test
    void toDate_emptyString_throwsException() {
        assertThrows(RuntimeException.class, () -> LocalDateConversions.toDate("", converter), "Empty string should not be parsed and must cause an exception");
    }

    /* --------------------------------------------------------------
     *  Unsupported types – expect exception
     * -------------------------------------------------------------- */
    @Test
    void toDate_unsupportedObject_throwsException() {
        Object unsupported = new Object();
        assertThrows(RuntimeException.class, () -> LocalDateConversions.toDate(unsupported, converter), "Unsupported source type should cause an exception");
    }

    @Test
    void toDate_LocalTime_throwsException() {
        LocalTime lt = LocalTime.of(15, 30, 45);
        assertThrows(RuntimeException.class, () -> LocalDateConversions.toDate(lt, converter), "LocalTime without a date component should cause an exception");
    }

    @Test
    void toDate_MonthDay_throwsException() {
        MonthDay md = MonthDay.of(2, 29);
        assertThrows(RuntimeException.class, () -> LocalDateConversions.toDate(md, converter), "MonthDay without a year should cause an exception");
    }

    /* --------------------------------------------------------------
     *  Numeric wrapper types – BigDecimal, BigInteger, AtomicLong
     * -------------------------------------------------------------- */
    @Test
    void toDate_BigDecimal_valid() {
        BigDecimal bd = new BigDecimal("1622548800000");
        Date result = LocalDateConversions.toDate(bd, converter);
        assertEquals(1622548800000L, result.getTime());
    }

    @Test
    void toDate_BigInteger_valid() {
        BigInteger bi = new BigInteger("1622548800000");
        Date result = LocalDateConversions.toDate(bi, converter);
        assertEquals(1622548800000L, result.getTime());
    }

    @Test
    void toDate_AtomicLong_valid() {
        AtomicLong al = new AtomicLong(1622548800000L);
        Date result = LocalDateConversions.toDate(al, converter);
        assertEquals(1622548800000L, result.getTime());
    }

    /* --------------------------------------------------------------
     *  SQL java.sql.Date conversion
     * -------------------------------------------------------------- */
    @Test
    void toDate_sqlDate_startOfDayUTC() {
        java.sql.Date sqlDate = java.sql.Date.valueOf("2021-12-31");
        // sqlDate represents midnight at the default zone; with UTC default it is 00:00 UTC.
        long expected = sqlDate.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        Date result = LocalDateConversions.toDate(sqlDate, converter);
        assertEquals(expected, result.getTime(), "SQL Date should be interpreted as start‑of‑day UTC");
    }

    /* --------------------------------------------------------------
     *  Invalid string formats – parsing failures
     * -------------------------------------------------------------- */
    @Test
    void toDate_invalidString_notADate_throwsException() {
        assertThrows(RuntimeException.class, () -> LocalDateConversions.toDate("not-a-date", converter), "Non‑date string must trigger a parsing exception");
    }

    @Test
    void toDate_invalidString_badMonth_throwsException() {
        assertThrows(RuntimeException.class, () -> LocalDateConversions.toDate("2021-13-01", converter), "Invalid month should trigger a parsing exception");
    }

    @Test
    void toDate_invalidString_nonexistentDay_throwsException() {
        assertThrows(RuntimeException.class, () -> LocalDateConversions.toDate("2021-02-30T10:00:00Z", converter), "Non‑existent calendar date must trigger a parsing exception");
    }

    /* --------------------------------------------------------------
     *  Out‑of‑range numeric values
     * -------------------------------------------------------------- */
    @Test
    void toDate_numericString_TooLarge_throwsException() {
        assertThrows(RuntimeException.class, () -> LocalDateConversions.toDate("9223372036854775808", converter), "Numeric string exceeding Long.MAX_VALUE should cause an exception");
    }

    @Test
    void toDate_numericString_TooSmall_throwsException() {
        assertThrows(RuntimeException.class, () -> LocalDateConversions.toDate("-9223372036854775809", converter), "Numeric string below Long.MIN_VALUE should cause an exception");
    }

    @Test
    void toDate_BigInteger_TooLarge_throwsException() {
        BigInteger tooBig = new BigInteger("9223372036854775808");
        assertThrows(RuntimeException.class, () -> LocalDateConversions.toDate(tooBig, converter), "BigInteger outside long range must be rejected");
    }

    /* --------------------------------------------------------------
     *  Non‑integer numeric types (fractional epoch millis)
     * -------------------------------------------------------------- */
    @Test
    void toDate_doubleFractional_throwsException() {
        assertThrows(RuntimeException.class, () -> LocalDateConversions.toDate(1622548800.5, converter), "Fractional epoch milliseconds are not allowed");
    }

    @Test
    void toDate_BigDecimalFractional_throwsException() {
        BigDecimal bd = new BigDecimal("1622548800.123");
        assertThrows(RuntimeException.class, () -> LocalDateConversions.toDate(bd, converter), "BigDecimal with fractional part must be rejected");
    }

    /* --------------------------------------------------------------
     *  Custom converter usage – successful conversion
     * -------------------------------------------------------------- */
    @Test
    void toDate_customConverter_parsesSimpleDate() {
        // Mock a Converter that pretends to understand the pattern "yyyy-MM-dd"
        Converter mockConverter = mock(Converter.class);
        // Assume the internal contract is: convert(Object source, Class<T> target)
        when(mockConverter.convert(eq("2022-01-01"), eq(Long.class))).thenReturn(// 2022‑01‑01T00:00:00Z
        1640995200000L);
        Date result = LocalDateConversions.toDate("2022-01-01", mockConverter);
        assertEquals(1640995200000L, result.getTime(), "Custom converter should supply the epoch millis");
        verify(mockConverter).convert(eq("2022-01-01"), eq(Long.class));
    }

    /* --------------------------------------------------------------
     *  Custom converter exception propagation
     * -------------------------------------------------------------- */
    @Test
    void toDate_customConverterThrows_propagatesException() {
        Converter badConverter = mock(Converter.class);
        when(badConverter.convert(any(), eq(Long.class))).thenThrow(new RuntimeException("conversion failure"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> LocalDateConversions.toDate("2022-03-01", badConverter), "Exception from the supplied Converter must propagate");
        assertEquals("conversion failure", ex.getMessage());
    }

    /* --------------------------------------------------------------
     *  Custom converter returns null – should result in NPE/IllegalArgumentException
     * -------------------------------------------------------------- */
    @Test
    void toDate_customConverterReturnsNull_throwsException() {
        Converter nullReturning = mock(Converter.class);
        when(nullReturning.convert(any(), eq(Long.class))).thenReturn(null);
        assertThrows(RuntimeException.class, () -> LocalDateConversions.toDate("2022-03-01", nullReturning), "Null result from Converter must cause an exception");
    }
}
