package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toSqlDate_Test {

    /**
     * Shared Converter used by all test cases.
     */
    private static Converter converter;

    /**
     * Shared {@link Converter} used by most test cases.
     */
    private static Converter defaultConverter;

    /**
     * Original JVM time‑zone, restored after the suite finishes.
     */
    private static String originalTimeZone;

    @BeforeAll
    static void setupBeforeAll() {
        // Use the default options – they contain the built‑in conversion rules.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
        // Force UTC to make map‑based conversions deterministic.
        originalTimeZone = System.getProperty("user.timezone");
        System.setProperty("user.timezone", "UTC");
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        // The library provides a singleton default converter.
        defaultConverter = DefaultConverter.INSTANCE;
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – helps the GC in long‑running test suites.
        converter = null;
        // Restore original time‑zone.
        if (originalTimeZone != null) {
            System.setProperty("user.timezone", originalTimeZone);
            TimeZone.setDefault(TimeZone.getTimeZone(originalTimeZone));
        }
        defaultConverter = null;
    }

    @Test
    void toSqlDate_givenUtilDate_returnsEqualDate() {
        // GIVEN
        // a fixed epoch‑millisecond value
        Date source = new Date(1_638_000_000_000L);
        // WHEN
        Date result = ZonedDateTimeConversions.toSqlDate(source, converter);
        // THEN
        assertNotNull(result, "Result should never be null");
        assertEquals(source.getTime(), result.getTime(), "The converted Date must have the same epoch milli as the source Date");
    }

    @Test
    void toSqlDate_givenZonedDateTime_returnsCorrespondingDate() {
        // GIVEN
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 1, 12, 30, 45, 123_000_000, ZoneId.of("UTC"));
        long expectedMillis = zdt.toInstant().toEpochMilli();
        // WHEN
        Date result = ZonedDateTimeConversions.toSqlDate(zdt, converter);
        // THEN
        assertNotNull(result);
        assertEquals(expectedMillis, result.getTime(), "The converted Date must represent the same instant as the ZonedDateTime");
    }

    @Test
    void toSqlDate_givenLongEpochMillis_returnsDateWithSameInstant() {
        // GIVEN
        // arbitrary epoch milli value
        Long epochMillis = 1_600_000_000_000L;
        // WHEN
        Date result = ZonedDateTimeConversions.toSqlDate(epochMillis, converter);
        // THEN
        assertNotNull(result);
        assertEquals(epochMillis.longValue(), result.getTime(), "The converted Date must have the same epoch milli as the Long source");
    }

    @Test
    void toSqlDate_givenNull_returnsEpochZeroDate() {
        // GIVEN
        Object source = null;
        // WHEN
        Date result = ZonedDateTimeConversions.toSqlDate(source, converter);
        // THEN
        assertNotNull(result);
        assertEquals(0L, result.getTime(), "When the source is null the conversion should yield a Date at epoch start (0 ms)");
    }

    @Test
    void toSqlDate_givenPrimitiveLong_returnsDateWithSameInstant() {
        // 2021‑06‑01T12:00:00Z
        long epoch = 1_622_548_800_000L;
        Date result = ZonedDateTimeConversions.toSqlDate(epoch, defaultConverter);
        assertNotNull(result);
        assertEquals(epoch, result.getTime());
    }

    @Test
    void toSqlDate_givenInteger_returnsDateWithSameInstant() {
        // 1973‑11‑29T21:33:09Z
        int epoch = 123_456_789;
        Date result = ZonedDateTimeConversions.toSqlDate(epoch, defaultConverter);
        assertNotNull(result);
        assertEquals(epoch, result.getTime());
    }

    @Test
    void toSqlDate_givenNumericString_returnsDateWithSameInstant() {
        // 2021‑01‑01T00:00:00Z
        String src = "1609459200000";
        Date result = ZonedDateTimeConversions.toSqlDate(src, defaultConverter);
        assertNotNull(result);
        assertEquals(1_609_459_200_000L, result.getTime());
    }

    @Test
    void toSqlDate_givenSqlTimestamp_preservesNanos() {
        Timestamp ts = Timestamp.valueOf("2022-12-31 23:59:59.123");
        Date result = ZonedDateTimeConversions.toSqlDate(ts, defaultConverter);
        assertNotNull(result);
        assertEquals(ts.getTime(), result.getTime());
    }

    @Test
    void toSqlDate_givenLocalDateTime_usesSystemDefaultZone() {
        LocalDateTime ldt = LocalDateTime.of(2020, 5, 15, 10, 30, 45);
        long expected = ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        Date result = ZonedDateTimeConversions.toSqlDate(ldt, defaultConverter);
        assertNotNull(result);
        assertEquals(expected, result.getTime());
    }

    @Test
    void toSqlDate_givenZonedDateTime_returnsExactInstant() {
        ZonedDateTime zdt = ZonedDateTime.parse("2023-03-10T08:15:30+02:00[Europe/Paris]");
        // 2023‑03‑10T06:15:30Z
        long expected = zdt.toInstant().toEpochMilli();
        Date result = ZonedDateTimeConversions.toSqlDate(zdt, defaultConverter);
        assertNotNull(result);
        assertEquals(expected, result.getTime());
    }

    @Test
    void toSqlDate_givenOffsetDateTime_returnsExactInstant() {
        OffsetDateTime odt = OffsetDateTime.parse("2022-08-15T13:45:30+05:30");
        // 2022‑08‑15T08:15:30Z
        long expected = odt.toInstant().toEpochMilli();
        Date result = ZonedDateTimeConversions.toSqlDate(odt, defaultConverter);
        assertNotNull(result);
        assertEquals(expected, result.getTime());
    }

    @Test
    void toSqlDate_givenInstant_returnsSameInstant() {
        // 2022‑12‑31T23:59:59Z
        Instant instant = Instant.ofEpochMilli(1_672_531_199_000L);
        Date result = ZonedDateTimeConversions.toSqlDate(instant, defaultConverter);
        assertNotNull(result);
        assertEquals(instant.toEpochMilli(), result.getTime());
    }

    @Test
    void toSqlDate_givenCalendar_returnsCalendarInstant() {
        ZonedDateTime zdt = ZonedDateTime.parse("2021-11-05T10:20:30+01:00[Europe/Paris]");
        Calendar cal = GregorianCalendar.from(zdt);
        // 2021‑11‑05T09:20:30Z
        long expected = cal.toInstant().toEpochMilli();
        Date result = ZonedDateTimeConversions.toSqlDate(cal, defaultConverter);
        assertNotNull(result);
        assertEquals(expected, result.getTime());
    }

    @Test
    void toSqlDate_givenBigDecimal_preservesFractionalMillis() {
        // 2021‑06‑01T12:00:00.123Z
        BigDecimal bd = new BigDecimal("1622548800123");
        Date result = ZonedDateTimeConversions.toSqlDate(bd, defaultConverter);
        assertNotNull(result);
        assertEquals(1_622_548_800_123L, result.getTime());
    }

    @Test
    void toSqlDate_givenBigInteger_negativeEpoch() {
        // one day before epoch
        BigInteger bi = new BigInteger("-86400000");
        Date result = ZonedDateTimeConversions.toSqlDate(bi, defaultConverter);
        assertNotNull(result);
        assertEquals(-86_400_000L, result.getTime());
    }

    @Test
    void toSqlDate_givenAtomicLong_returnsCorrespondingDate() {
        AtomicLong al = new AtomicLong(0L);
        Date result = ZonedDateTimeConversions.toSqlDate(al, defaultConverter);
        assertNotNull(result);
        assertEquals(0L, result.getTime());
    }

    @Test
    void toSqlDate_givenSqlDate_returnsMidnightUTC() {
        java.sql.Date sqlDate = java.sql.Date.valueOf("2020-02-29");
        // java.sql.Date stores the epoch millis at midnight UTC of the given day
        long expected = sqlDate.getTime();
        Date result = ZonedDateTimeConversions.toSqlDate(sqlDate, defaultConverter);
        assertNotNull(result);
        assertEquals(expected, result.getTime());
    }

    @Test
    void toSqlDate_givenMapWithDateTimeZone_returnsCombinedInstant() {
        Map<String, Object> map = new HashMap<>();
        map.put("date", "2021-07-20");
        map.put("time", "14:45:00");
        map.put("zone", "America/New_York");
        // 2021‑07‑20 14:45 in New York = 2021‑07‑20T18:45:00Z
        long expected = ZonedDateTime.of(LocalDate.of(2021, 7, 20), LocalTime.of(14, 45, 0), ZoneId.of("America/New_York")).toInstant().toEpochMilli();
        Date result = ZonedDateTimeConversions.toSqlDate(map, defaultConverter);
        assertNotNull(result);
        assertEquals(expected, result.getTime());
    }

    @Test
    void toSqlDate_givenMapMissingZone_usesSystemDefault() {
        Map<String, Object> map = new HashMap<>();
        map.put("date", "2022-03-10");
        map.put("time", "15:30:00");
        // system default is forced to UTC in @BeforeAll
        long expected = ZonedDateTime.of(LocalDate.of(2022, 3, 10), LocalTime.of(15, 30, 0), ZoneId.systemDefault()).toInstant().toEpochMilli();
        Date result = ZonedDateTimeConversions.toSqlDate(map, defaultConverter);
        assertNotNull(result);
        assertEquals(expected, result.getTime());
    }

    @Test
    void toSqlDate_givenMapMissingTime_defaultsToMidnight() {
        Map<String, Object> map = new HashMap<>();
        map.put("date", "2021-12-31");
        map.put("zone", "UTC");
        long expected = ZonedDateTime.of(LocalDate.of(2021, 12, 31), LocalTime.MIDNIGHT, ZoneId.of("UTC")).toInstant().toEpochMilli();
        Date result = ZonedDateTimeConversions.toSqlDate(map, defaultConverter);
        assertNotNull(result);
        assertEquals(expected, result.getTime());
    }

    @Test
    void toSqlDate_givenNonNumericString_throwsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> ZonedDateTimeConversions.toSqlDate("invalid-number", defaultConverter));
    }

    @Test
    void toSqlDate_givenStringWithDecimalPoint_throwsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> ZonedDateTimeConversions.toSqlDate("12345.67", defaultConverter));
    }

    @Test
    void toSqlDate_givenNullSource_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> ZonedDateTimeConversions.toSqlDate(null, defaultConverter));
    }

    @Test
    void toSqlDate_givenUnsupportedObject_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> ZonedDateTimeConversions.toSqlDate(new Object(), defaultConverter));
    }

    @Test
    void toSqlDate_givenNullConverter_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toSqlDate(1622548800000L, null));
    }

    @Test
    void toSqlDate_givenMapMissingDate_throwsIllegalArgumentException() {
        Map<String, Object> map = new HashMap<>();
        map.put("time", "12:00:00");
        map.put("zone", "UTC");
        assertThrows(IllegalArgumentException.class, () -> ZonedDateTimeConversions.toSqlDate(map, defaultConverter));
    }

    @Test
    void toSqlDate_givenMapInvalidDateFormat_throwsDateTimeParseException() {
        Map<String, Object> map = new HashMap<>();
        // not ISO‑8601
        map.put("date", "31-12-2021");
        map.put("time", "12:00:00");
        map.put("zone", "UTC");
        assertThrows(DateTimeParseException.class, () -> ZonedDateTimeConversions.toSqlDate(map, defaultConverter));
    }

    @Test
    void toSqlDate_givenMapInvalidTimeFormat_throwsDateTimeParseException() {
        Map<String, Object> map = new HashMap<>();
        map.put("date", "2022-05-01");
        // invalid hour/minute
        map.put("time", "25:61:00");
        map.put("zone", "UTC");
        assertThrows(DateTimeParseException.class, () -> ZonedDateTimeConversions.toSqlDate(map, defaultConverter));
    }

    @Test
    void toSqlDate_givenMapInvalidZone_throwsDateTimeException() {
        Map<String, Object> map = new HashMap<>();
        map.put("date", "2022-01-01");
        map.put("time", "10:00:00");
        map.put("zone", "Not/A_Zone");
        assertThrows(DateTimeException.class, () -> ZonedDateTimeConversions.toSqlDate(map, defaultConverter));
    }

    @Test
    void toSqlDate_givenBoolean_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> ZonedDateTimeConversions.toSqlDate(Boolean.TRUE, defaultConverter));
    }

    @Test
    void toSqlDate_givenBigDecimalOverflow_throwsArithmeticException() {
        // Long.MAX_VALUE + 1
        BigDecimal overflow = new BigDecimal("9223372036854775808");
        assertThrows(ArithmeticException.class, () -> ZonedDateTimeConversions.toSqlDate(overflow, defaultConverter));
    }
}
