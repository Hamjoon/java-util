package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static com.cedarsoftware.util.convert.MapConversions.ZONED_DATE_TIME;
import static org.junit.Assert.*;

public class ZonedDateTimeConversionsTest {

    private ZonedDateTime utcZdt;
    private Converter converter;

    @Before
    public void setUp() {
        // A fixed instant that is easy to reason about (2023‑03‑15T12:30:45.123Z)
        utcZdt = ZonedDateTime.of(
                2023, 3, 15,
                12, 30, 45, 123_000_000,
                ZoneId.of("UTC")
        );

        // Build ConverterOptions with a known ZoneId and TimeZone (UTC)
        ConverterOptions options = new ConverterOptions();
        // The real ConverterOptions class provides setters – they are used here.
        // If the library uses a different API, adjust accordingly.
        options.setZoneId(ZoneId.of("UTC"));
        options.setTimeZone(TimeZone.getTimeZone("UTC"));

        converter = new Converter(options);
    }

    @Test
    public void testToLong() {
        long expected = utcZdt.toInstant().toEpochMilli();
        long actual = ZonedDateTimeConversions.toLong(utcZdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToDouble() {
        // InstantConversions.toDouble ultimately returns the epoch milli as a double.
        double expected = (double) utcZdt.toInstant().toEpochMilli();
        double actual = ZonedDateTimeConversions.toDouble(utcZdt, converter);
        assertEquals(expected, actual, 0.0);
    }

    @Test
    public void testToInstant() {
        Instant expected = utcZdt.toInstant();
        Instant actual = ZonedDateTimeConversions.toInstant(utcZdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalDateTime() {
        ZoneId targetZone = converter.getOptions().getZoneId();
        LocalDateTime expected = utcZdt.withZoneSameInstant(targetZone).toLocalDateTime();
        LocalDateTime actual = ZonedDateTimeConversions.toLocalDateTime(utcZdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalDate() {
        ZoneId targetZone = converter.getOptions().getZoneId();
        LocalDate expected = utcZdt.withZoneSameInstant(targetZone).toLocalDate();
        LocalDate actual = ZonedDateTimeConversions.toLocalDate(utcZdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalTime() {
        ZoneId targetZone = converter.getOptions().getZoneId();
        LocalTime expected = utcZdt.withZoneSameInstant(targetZone).toLocalTime();
        LocalTime actual = ZonedDateTimeConversions.toLocalTime(utcZdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToOffsetDateTime() {
        OffsetDateTime expected = utcZdt.toOffsetDateTime();
        OffsetDateTime actual = ZonedDateTimeConversions.toOffsetDateTime(utcZdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToAtomicLong() {
        long expected = utcZdt.toInstant().toEpochMilli();
        AtomicLong atomic = ZonedDateTimeConversions.toAtomicLong(utcZdt, converter);
        assertEquals(expected, atomic.get());
    }

    @Test
    public void testToTimestamp() {
        Timestamp expected = Timestamp.from(utcZdt.toInstant());
        Timestamp actual = ZonedDateTimeConversions.toTimestamp(utcZdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToCalendar() {
        Calendar cal = ZonedDateTimeConversions.toCalendar(utcZdt, converter);
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
        assertEquals(utcZdt.toInstant().toEpochMilli(), cal.getTimeInMillis());
    }

    @Test
    public void testToSqlDate() {
        ZoneId targetZone = converter.getOptions().getZoneId();
        java.sql.Date expected = java.sql.Date.valueOf(
                utcZdt.withZoneSameInstant(targetZone).toLocalDate()
        );
        java.sql.Date actual = ZonedDateTimeConversions.toSqlDate(utcZdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToDate() {
        long epochMilli = utcZdt.toInstant().toEpochMilli();
        Date expected = new Date(epochMilli);
        Date actual = ZonedDateTimeConversions.toDate(utcZdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToBigInteger() {
        BigInteger expected = InstantConversions.toBigInteger(utcZdt.toInstant(), converter);
        BigInteger actual = ZonedDateTimeConversions.toBigInteger(utcZdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToBigDecimal() {
        BigDecimal expected = InstantConversions.toBigDecimal(utcZdt.toInstant(), converter);
        BigDecimal actual = ZonedDateTimeConversions.toBigDecimal(utcZdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToYear() {
        ZoneId targetZone = converter.getOptions().getZoneId();
        Year expected = Year.from(
                utcZdt.withZoneSameInstant(targetZone).toLocalDate()
        );
        Year actual = ZonedDateTimeConversions.toYear(utcZdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToYearMonth() {
        ZoneId targetZone = converter.getOptions().getZoneId();
        YearMonth expected = YearMonth.from(
                utcZdt.withZoneSameInstant(targetZone).toLocalDate()
        );
        YearMonth actual = ZonedDateTimeConversions.toYearMonth(utcZdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToMonthDay() {
        ZoneId targetZone = converter.getOptions().getZoneId();
        MonthDay expected = MonthDay.from(
                utcZdt.withZoneSameInstant(targetZone).toLocalDate()
        );
        MonthDay actual = ZonedDateTimeConversions.toMonthDay(utcZdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToString() {
        String expected = utcZdt.format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
        String actual = ZonedDateTimeConversions.toString(utcZdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToMap() {
        String zdtString = ZonedDateTimeConversions.toString(utcZdt, converter);
        Map<String, Object> map = ZonedDateTimeConversions.toMap(utcZdt, converter);
        assertNotNull(map);
        assertEquals(1, map.size());
        assertTrue(map.containsKey(ZONED_DATE_TIME));
        assertEquals(zdtString, map.get(ZONED_DATE_TIME));
    }
}
