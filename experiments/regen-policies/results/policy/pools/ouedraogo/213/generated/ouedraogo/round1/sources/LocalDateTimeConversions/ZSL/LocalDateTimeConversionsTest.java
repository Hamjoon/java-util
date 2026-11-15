package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class LocalDateTimeConversionsTest {

    private static final LocalDateTime SAMPLE_LDT = LocalDateTime.of(
            2022, Month.MARCH, 14,
            15, 9, 26, 123_000_000);   // 2022-03-14T15:09:26.123

    private Converter utcConverter;
    private Converter nyConverter;

    @Before
    public void setUp() {
        // ----- UTC converter -------------------------------------------------
        ConverterOptions utcOptions = new ConverterOptions();
        utcOptions.setZoneId(ZoneId.of("UTC"));
        utcOptions.setTimeZone(TimeZone.getTimeZone("UTC"));
        utcConverter = new Converter(utcOptions);

        // ----- New‑York (America/New_York) converter ------------------------
        ConverterOptions nyOptions = new ConverterOptions();
        nyOptions.setZoneId(ZoneId.of("America/New_York"));
        nyOptions.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        nyConverter = new Converter(nyOptions);
    }

    /* --------------------------------------------------------------
     *  Helper methods to compute the “ground‑truth” values that the
     *  conversion methods should produce.
     * -------------------------------------------------------------- */
    private ZonedDateTime zoned(ZoneId zone) {
        return SAMPLE_LDT.atZone(zone);
    }

    private OffsetDateTime offset(ZoneId zone) {
        ZoneOffset offset = zone.getRules().getOffset(Instant.now());
        return SAMPLE_LDT.atOffset(offset);
    }

    private Instant instant(ZoneId zone) {
        return zoned(zone).toInstant();
    }

    private long epochMilli(ZoneId zone) {
        return instant(zone).toEpochMilli();
    }

    private double epochMilliAsDouble(ZoneId zone) {
        return (double) epochMilli(zone);
    }

    /* --------------------------------------------------------------
     *  Tests for each static conversion method.
     * -------------------------------------------------------------- */

    @Test
    public void testToZonedDateTime_Utc() {
        ZonedDateTime expected = zoned(ZoneId.of("UTC"));
        ZonedDateTime actual = LocalDateTimeConversions.toZonedDateTime(SAMPLE_LDT, utcConverter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToZonedDateTime_NewYork() {
        ZonedDateTime expected = zoned(ZoneId.of("America/New_York"));
        ZonedDateTime actual = LocalDateTimeConversions.toZonedDateTime(SAMPLE_LDT, nyConverter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToOffsetDateTime_Utc() {
        OffsetDateTime expected = offset(ZoneId.of("UTC"));
        OffsetDateTime actual = LocalDateTimeConversions.toOffsetDateTime(SAMPLE_LDT, utcConverter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToOffsetDateTime_NewYork() {
        OffsetDateTime expected = offset(ZoneId.of("America/New_York"));
        OffsetDateTime actual = LocalDateTimeConversions.toOffsetDateTime(SAMPLE_LDT, nyConverter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToInstant_Utc() {
        Instant expected = instant(ZoneId.of("UTC"));
        Instant actual = LocalDateTimeConversions.toInstant(SAMPLE_LDT, utcConverter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToInstant_NewYork() {
        Instant expected = instant(ZoneId.of("America/New_York"));
        Instant actual = LocalDateTimeConversions.toInstant(SAMPLE_LDT, nyConverter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLong_Utc() {
        long expected = epochMilli(ZoneId.of("UTC"));
        long actual = LocalDateTimeConversions.toLong(SAMPLE_LDT, utcConverter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLong_NewYork() {
        long expected = epochMilli(ZoneId.of("America/New_York"));
        long actual = LocalDateTimeConversions.toLong(SAMPLE_LDT, nyConverter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToDouble_Utc() {
        double expected = epochMilliAsDouble(ZoneId.of("UTC"));
        double actual = LocalDateTimeConversions.toDouble(SAMPLE_LDT, utcConverter);
        assertEquals(expected, actual, 0.0);
    }

    @Test
    public void testToDouble_NewYork() {
        double expected = epochMilliAsDouble(ZoneId.of("America/New_York"));
        double actual = LocalDateTimeConversions.toDouble(SAMPLE_LDT, nyConverter);
        assertEquals(expected, actual, 0.0);
    }

    @Test
    public void testToLocalDateTime_Utc() {
        LocalDateTime expected = SAMPLE_LDT;
        LocalDateTime actual = LocalDateTimeConversions.toLocalDateTime(SAMPLE_LDT, utcConverter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalDate_Utc() {
        LocalDate expected = SAMPLE_LDT.toLocalDate();
        LocalDate actual = LocalDateTimeConversions.toLocalDate(SAMPLE_LDT, utcConverter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalTime_Utc() {
        LocalTime expected = SAMPLE_LDT.toLocalTime();
        LocalTime actual = LocalDateTimeConversions.toLocalTime(SAMPLE_LDT, utcConverter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToAtomicLong_Utc() {
        long expected = epochMilli(ZoneId.of("UTC"));
        AtomicLong actual = LocalDateTimeConversions.toAtomicLong(SAMPLE_LDT, utcConverter);
        assertEquals(expected, actual.get());
    }

    @Test
    public void testToTimestamp_Utc() {
        Timestamp expected = Timestamp.from(zoned(ZoneId.of("UTC")).toInstant());
        Timestamp actual = LocalDateTimeConversions.toTimestamp(SAMPLE_LDT, utcConverter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToCalendar_Utc() {
        Calendar expected = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        expected.setTimeInMillis(epochMilli(ZoneId.of("UTC")));

        Calendar actual = LocalDateTimeConversions.toCalendar(SAMPLE_LDT, utcConverter);
        assertEquals(expected.getTimeInMillis(), actual.getTimeInMillis());
        assertEquals(expected.getTimeZone(), actual.getTimeZone());
    }

    @Test
    public void testToSqlDate_Utc() {
        java.sql.Date expected = java.sql.Date.valueOf(SAMPLE_LDT.toLocalDate());
        java.sql.Date actual = LocalDateTimeConversions.toSqlDate(SAMPLE_LDT, utcConverter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToDate_Utc() {
        Date expected = new Date(epochMilli(ZoneId.of("UTC")));
        Date actual = LocalDateTimeConversions.toDate(SAMPLE_LDT, utcConverter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToBigInteger_Utc() {
        BigInteger expected = BigInteger.valueOf(epochMilli(ZoneId.of("UTC")));
        BigInteger actual = LocalDateTimeConversions.toBigInteger(SAMPLE_LDT, utcConverter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToBigDecimal_Utc() {
        BigDecimal expected = BigDecimal.valueOf(epochMilli(ZoneId.of("UTC")));
        BigDecimal actual = LocalDateTimeConversions.toBigDecimal(SAMPLE_LDT, utcConverter);
        assertEquals(0, expected.compareTo(actual));
    }

    @Test
    public void testToYear_Utc() {
        Year expected = Year.of(SAMPLE_LDT.getYear());
        Year actual = LocalDateTimeConversions.toYear(SAMPLE_LDT, utcConverter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToYearMonth_Utc() {
        YearMonth expected = YearMonth.of(SAMPLE_LDT.getYear(), SAMPLE_LDT.getMonth());
        YearMonth actual = LocalDateTimeConversions.toYearMonth(SAMPLE_LDT, utcConverter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToMonthDay_Utc() {
        MonthDay expected = MonthDay.of(SAMPLE_LDT.getMonth(), SAMPLE_LDT.getDayOfMonth());
        MonthDay actual = LocalDateTimeConversions.toMonthDay(SAMPLE_LDT, utcConverter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToString_Utc() {
        String expected = SAMPLE_LDT.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String actual = LocalDateTimeConversions.toString(SAMPLE_LDT, utcConverter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToMap_Utc() {
        Map<?, ?> map = LocalDateTimeConversions.toMap(SAMPLE_LDT, utcConverter);
        assertNotNull(map);
        assertTrue(map.containsKey(MapConversions.LOCAL_DATE_TIME));
        assertEquals(SAMPLE_LDT.toString(), map.get(MapConversions.LOCAL_DATE_TIME));
    }
}
