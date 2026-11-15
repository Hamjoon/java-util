package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import static com.cedarsoftware.util.convert.MapConversions.ZONED_DATE_TIME;
import static org.junit.Assert.*;

public class ZonedDateTimeConversionsTest {

    private static final ZoneId SOURCE_ZONE = ZoneOffset.UTC;                     // source ZonedDateTime zone
    private static final ZoneId TARGET_ZONE = ZoneId.of("America/New_York");       // converter option zone
    private static final ZonedDateTime SOURCE_ZDT = ZonedDateTime.of(
            2020, 2, 29, 12, 34, 56, 123_000_000, SOURCE_ZONE); // 2020‑02‑29 12:34:56.123 UTC

    private Converter converter;   // will be created with TARGET_ZONE

    /**
     * Helper that creates a {@link Converter} whose {@link ConverterOptions}
     * are configured to use the supplied {@code zoneId} for both the {@code ZoneId}
     * and the {@code TimeZone}.  The implementation uses reflection because the
     * {@code ConverterOptions} class is not part of the public test contract.
     */
    private static Converter createConverterWithZone(ZoneId zoneId) throws Exception {
        // instantiate a fresh ConverterOptions (assumes a public no‑arg constructor)
        ConverterOptions options = new ConverterOptions();

        // set the private fields that the conversion methods rely on
        Field zoneField = options.getClass().getDeclaredField("zoneId");
        zoneField.setAccessible(true);
        zoneField.set(options, zoneId);

        Field tzField = options.getClass().getDeclaredField("timeZone");
        tzField.setAccessible(true);
        tzField.set(options, TimeZone.getTimeZone(zoneId));

        // build the Converter with those options
        return new Converter(options);
    }

    @Before
    public void setUp() throws Exception {
        converter = createConverterWithZone(TARGET_ZONE);
    }

    /* --------------------------------------------------------------------- *
     *  Positive / typical use‑case tests
     * --------------------------------------------------------------------- */

    @Test
    public void testToLong() {
        long expected = SOURCE_ZDT.toInstant().toEpochMilli();
        long actual = ZonedDateTimeConversions.toLong(SOURCE_ZDT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToDouble() {
        double expected = (double) SOURCE_ZDT.toInstant().toEpochMilli();
        double actual = ZonedDateTimeConversions.toDouble(SOURCE_ZDT, converter);
        assertEquals(expected, actual, 0.0);
    }

    @Test
    public void testToInstant() {
        Instant expected = SOURCE_ZDT.toInstant();
        Instant actual = ZonedDateTimeConversions.toInstant(SOURCE_ZDT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalDateTime() {
        // source is UTC, target zone is America/New_York (UTC‑05:00 in Feb 2020)
        ZonedDateTime adjusted = SOURCE_ZDT.withZoneSameInstant(TARGET_ZONE);
        LocalDateTime expected = adjusted.toLocalDateTime();

        LocalDateTime actual = ZonedDateTimeConversions.toLocalDateTime(SOURCE_ZDT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalDate() {
        ZonedDateTime adjusted = SOURCE_ZDT.withZoneSameInstant(TARGET_ZONE);
        LocalDate expected = adjusted.toLocalDate();

        LocalDate actual = ZonedDateTimeConversions.toLocalDate(SOURCE_ZDT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalTime() {
        ZonedDateTime adjusted = SOURCE_ZDT.withZoneSameInstant(TARGET_ZONE);
        LocalTime expected = adjusted.toLocalTime();

        LocalTime actual = ZonedDateTimeConversions.toLocalTime(SOURCE_ZDT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToOffsetDateTime() {
        OffsetDateTime expected = SOURCE_ZDT.toOffsetDateTime();
        OffsetDateTime actual = ZonedDateTimeConversions.toOffsetDateTime(SOURCE_ZDT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToAtomicLong() {
        long epoch = SOURCE_ZDT.toInstant().toEpochMilli();
        AtomicLong expected = new AtomicLong(epoch);
        AtomicLong actual = ZonedDateTimeConversions.toAtomicLong(SOURCE_ZDT, converter);
        assertEquals(expected.get(), actual.get());
    }

    @Test
    public void testToTimestamp() {
        Instant instant = SOURCE_ZDT.toInstant();
        Timestamp expected = Timestamp.from(instant);
        Timestamp actual = ZonedDateTimeConversions.toTimestamp(SOURCE_ZDT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToCalendar() {
        Calendar cal = ZonedDateTimeConversions.toCalendar(SOURCE_ZDT, converter);
        long expectedMillis = SOURCE_ZDT.toInstant().toEpochMilli();

        assertEquals(expectedMillis, cal.getTimeInMillis());
        assertEquals(TimeZone.getTimeZone(TARGET_ZONE), cal.getTimeZone());
    }

    @Test
    public void testToSqlDate() {
        ZonedDateTime adjusted = SOURCE_ZDT.withZoneSameInstant(TARGET_ZONE);
        java.sql.Date expected = java.sql.Date.valueOf(adjusted.toLocalDate());

        java.sql.Date actual = ZonedDateTimeConversions.toSqlDate(SOURCE_ZDT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToDate() {
        long epoch = SOURCE_ZDT.toInstant().toEpochMilli();
        Date expected = new Date(epoch);
        Date actual = ZonedDateTimeConversions.toDate(SOURCE_ZDT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToBigInteger() {
        Instant instant = SOURCE_ZDT.toInstant();
        BigInteger expected = BigInteger.valueOf(instant.toEpochMilli());
        BigInteger actual = ZonedDateTimeConversions.toBigInteger(SOURCE_ZDT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToBigDecimal() {
        Instant instant = SOURCE_ZDT.toInstant();
        BigDecimal expected = BigDecimal.valueOf(instant.toEpochMilli());
        BigDecimal actual = ZonedDateTimeConversions.toBigDecimal(SOURCE_ZDT, converter);
        assertEquals(0, expected.compareTo(actual));
    }

    @Test
    public void testToYear() {
        ZonedDateTime adjusted = SOURCE_ZDT.withZoneSameInstant(TARGET_ZONE);
        Year expected = Year.from(adjusted.toLocalDate());
        Year actual = ZonedDateTimeConversions.toYear(SOURCE_ZDT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToYearMonth() {
        ZonedDateTime adjusted = SOURCE_ZDT.withZoneSameInstant(TARGET_ZONE);
        YearMonth expected = YearMonth.from(adjusted.toLocalDate());
        YearMonth actual = ZonedDateTimeConversions.toYearMonth(SOURCE_ZDT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToMonthDay() {
        ZonedDateTime adjusted = SOURCE_ZDT.withZoneSameInstant(TARGET_ZONE);
        MonthDay expected = MonthDay.from(adjusted.toLocalDate());
        MonthDay actual = ZonedDateTimeConversions.toMonthDay(SOURCE_ZDT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToString() {
        String expected = SOURCE_ZDT.format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
        String actual = ZonedDateTimeConversions.toString(SOURCE_ZDT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToMap() {
        String zdtString = ZonedDateTimeConversions.toString(SOURCE_ZDT, converter);
        Map<String, Object> map = ZonedDateTimeConversions.toMap(SOURCE_ZDT, converter);
        assertNotNull(map);
        assertEquals(1, map.size());
        assertTrue(map.containsKey(ZONED_DATE_TIME));
        assertEquals(zdtString, map.get(ZONED_DATE_TIME));
    }

    /* --------------------------------------------------------------------- *
     *  Edge‑case / error scenario tests
     * --------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToLong_NullInput() {
        ZonedDateTimeConversions.toLong(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToDouble_NullInput() {
        ZonedDateTimeConversions.toDouble(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToInstant_NullInput() {
        ZonedDateTimeConversions.toInstant(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDateTime_NullInput() {
        ZonedDateTimeConversions.toLocalDateTime(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDate_NullInput() {
        ZonedDateTimeConversions.toLocalDate(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalTime_NullInput() {
        ZonedDateTimeConversions.toLocalTime(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToOffsetDateTime_NullInput() {
        ZonedDateTimeConversions.toOffsetDateTime(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLong_NullInput() {
        ZonedDateTimeConversions.toAtomicLong(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToTimestamp_NullInput() {
        ZonedDateTimeConversions.toTimestamp(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToCalendar_NullInput() {
        ZonedDateTimeConversions.toCalendar(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToSqlDate_NullInput() {
        ZonedDateTimeConversions.toSqlDate(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToDate_NullInput() {
        ZonedDateTimeConversions.toDate(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_NullInput() {
        ZonedDateTimeConversions.toBigInteger(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_NullInput() {
        ZonedDateTimeConversions.toBigDecimal(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToYear_NullInput() {
        ZonedDateTimeConversions.toYear(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToYearMonth_NullInput() {
        ZonedDateTimeConversions.toYearMonth(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToMonthDay_NullInput() {
        ZonedDateTimeConversions.toMonthDay(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_NullInput() {
        ZonedDateTimeConversions.toString(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullInput() {
        ZonedDateTimeConversions.toMap(null, converter);
    }

    /**
     * Verify that a conversion that changes the zone really does shift the
     * date‑time components.  The source is UTC midnight on 2021‑01‑01.
     * Converting to America/Los_Angeles (UTC‑08:00) must yield the previous day.
     */
    @Test
    public void testZoneShiftEffect() throws Exception {
        ZoneId utc = ZoneOffset.UTC;
        ZoneId la = ZoneId.of("America/Los_Angeles");
        ZonedDateTime utcMidnight = ZonedDateTime.of(
                2021, 1, 1, 0, 0, 0, 0, utc);

        Converter laConverter = createConverterWithZone(la);

        LocalDate expectedDate = utcMidnight.withZoneSameInstant(la).toLocalDate(); // 2020‑12‑31
        LocalDate actualDate = ZonedDateTimeConversions.toLocalDate(utcMidnight, laConverter);
        assertEquals(expectedDate, actualDate);
    }
}
