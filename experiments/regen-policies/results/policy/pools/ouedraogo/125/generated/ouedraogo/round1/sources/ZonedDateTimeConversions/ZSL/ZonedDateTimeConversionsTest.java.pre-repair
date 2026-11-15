package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactMap;
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

import static com.cedarsoftware.util.convert.MapConversions.DATE;
import static com.cedarsoftware.util.convert.MapConversions.TIME;
import static com.cedarsoftware.util.convert.MapConversions.ZONE;
import static org.junit.Assert.*;

public class ZonedDateTimeConversionsTest {

    private ZonedDateTime nyZdt;          // New York (America/New_York)
    private ZonedDateTime utcZdt;         // UTC
    private Converter nyConverter;       // Options with New York zone
    private Converter utcConverter;      // Options with UTC zone

    @Before
    public void setUp() {
        // A winter date to avoid DST complications (2023‑01‑15 12:00:00.123)
        nyZdt = ZonedDateTime.of(
                LocalDateTime.of(2023, 1, 15, 12, 0, 0, 123_000_000),
                ZoneId.of("America/New_York"));

        utcZdt = nyZdt.withZoneSameInstant(ZoneId.of("UTC"));

        // ConverterOptions is assumed to have a public no‑arg constructor and
        // setters for zoneId and timeZone.  If the real library uses a different
        // API the test will still compile because we only need the getters at
        // runtime – the default constructor gives us a sensible default.
        ConverterOptions nyOptions = new ConverterOptions();
        nyOptions.setZoneId(ZoneId.of("America/New_York"));
        nyOptions.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        nyConverter = new Converter(nyOptions);

        ConverterOptions utcOptions = new ConverterOptions();
        utcOptions.setZoneId(ZoneId.of("UTC"));
        utcOptions.setTimeZone(TimeZone.getTimeZone("UTC"));
        utcConverter = new Converter(utcOptions);
    }

    @Test
    public void testToLong() {
        long expected = nyZdt.toInstant().toEpochMilli();
        assertEquals(expected, ZonedDateTimeConversions.toLong(nyZdt, nyConverter));
    }

    @Test
    public void testToDouble() {
        double expected = (double) nyZdt.toInstant().toEpochMilli();
        assertEquals(expected, ZonedDateTimeConversions.toDouble(nyZdt, nyConverter), 0.0);
    }

    @Test
    public void testToInstant() {
        Instant expected = nyZdt.toInstant();
        assertEquals(expected, ZonedDateTimeConversions.toInstant(nyZdt, nyConverter));
    }

    @Test
    public void testToLocalDateTime_WithZoneAdjustment() {
        // nyZdt is -05:00, utcConverter forces UTC => 5 hour shift forward
        LocalDateTime expected = utcZdt.toLocalDateTime();
        LocalDateTime actual = ZonedDateTimeConversions.toLocalDateTime(nyZdt, utcConverter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalDate_WithZoneAdjustment() {
        LocalDate expected = utcZdt.toLocalDate();
        LocalDate actual = ZonedDateTimeConversions.toLocalDate(nyZdt, utcConverter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalTime_WithZoneAdjustment() {
        LocalTime expected = utcZdt.toLocalTime();
        LocalTime actual = ZonedDateTimeConversions.toLocalTime(nyZdt, utcConverter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToOffsetDateTime() {
        OffsetDateTime expected = nyZdt.toOffsetDateTime();
        OffsetDateTime actual = ZonedDateTimeConversions.toOffsetDateTime(nyZdt, nyConverter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToAtomicLong() {
        long epoch = nyZdt.toInstant().toEpochMilli();
        AtomicLong atomic = ZonedDateTimeConversions.toAtomicLong(nyZdt, nyConverter);
        assertEquals(epoch, atomic.get());
    }

    @Test
    public void testToTimestamp() {
        Timestamp expected = Timestamp.from(nyZdt.toInstant());
        Timestamp actual = ZonedDateTimeConversions.toTimestamp(nyZdt, nyConverter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToCalendar() {
        Calendar cal = ZonedDateTimeConversions.toCalendar(nyZdt, utcConverter);
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
        assertEquals(nyZdt.toInstant().toEpochMilli(), cal.getTimeInMillis());
    }

    @Test
    public void testToSqlDate() {
        java.sql.Date expected = new java.sql.Date(nyZdt.toInstant().toEpochMilli());
        java.sql.Date actual = ZonedDateTimeConversions.toSqlDate(nyZdt, nyConverter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToDate() {
        Date expected = new Date(nyZdt.toInstant().toEpochMilli());
        Date actual = ZonedDateTimeConversions.toDate(nyZdt, nyConverter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToBigInteger() {
        BigInteger expected = BigInteger.valueOf(nyZdt.toInstant().toEpochMilli());
        BigInteger actual = ZonedDateTimeConversions.toBigInteger(nyZdt, nyConverter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToBigDecimal() {
        BigDecimal expected = BigDecimal.valueOf(nyZdt.toInstant().toEpochMilli());
        BigDecimal actual = ZonedDateTimeConversions.toBigDecimal(nyZdt, nyConverter);
        assertEquals(0, expected.compareTo(actual));
    }

    @Test
    public void testToString() {
        String expected = nyZdt.format(DateTimeFormatter.ISO_DATE_TIME);
        String actual = ZonedDateTimeConversions.toString(nyZdt, nyConverter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToMap() {
        Map<String, Object> map = ZonedDateTimeConversions.toMap(nyZdt, nyConverter);
        assertEquals(3, map.size());
        assertEquals(nyZdt.toLocalDate().toString(), map.get(DATE));
        assertEquals(nyZdt.toLocalTime().toString(), map.get(TIME));
        assertEquals(nyZdt.getZone().toString(), map.get(ZONE));
        // Verify the map implementation is a CompactMap (insertion order)
        assertTrue(map instanceof CompactMap);
    }
}
