package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.convert.ZonedDateTimeConversions;
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

    private ZonedDateTime utcZdt;          // ZonedDateTime in UTC (the converter's default zone)
    private ZonedDateTime nyZdt;           // ZonedDateTime in a different zone (America/New_York)
    private Converter converter;          // Converter with default options (UTC)

    /**
     * Initialise a deterministic environment:
     * <ul>
     *   <li>Force the JVM default time‑zone to UTC – this is the zone used by the default {@link ConverterOptions}
     *       when we create a {@link Converter} with a no‑arg {@code new ConverterOptions()}.</li>
     *   <li>Create a {@link Converter} that will therefore return UTC for {@code getOptions().getZoneId()}
     *       and {@code getOptions().getTimeZone()}.</li>
     *   <li>Prepare two {@link ZonedDateTime} instances – one already in UTC and another in
     *       {@code America/New_York} – to test the zone‑adjustment logic.</li>
     * </ul>
     */
    @Before
    public void setUp() {
        // Force JVM default zone to UTC – this influences ZoneId.systemDefault() used by ConverterOptions
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        System.setProperty("user.timezone", "UTC");

        // ZonedDateTime in UTC (the “reference” value)
        utcZdt = ZonedDateTime.of(
                LocalDate.of(2023, Month.MARCH, 25),
                LocalTime.of(14, 30, 15, 123_000_000), // 123 ms
                ZoneId.of("UTC"));

        // Same instant but expressed in New York time (UTC‑5/‑4 depending on DST)
        nyZdt = utcZdt.withZoneSameInstant(ZoneId.of("America/New_York"));

        // Build a Converter that uses the default options (which now resolve to UTC)
        converter = new Converter(new ConverterOptions());
    }

    @Test
    public void testToLong() {
        long expected = utcZdt.toInstant().toEpochMilli();
        assertEquals(expected, ZonedDateTimeConversions.toLong(utcZdt, converter));
    }

    @Test
    public void testToInstant() {
        Instant expected = utcZdt.toInstant();
        assertEquals(expected, ZonedDateTimeConversions.toInstant(utcZdt, converter));
    }

    @Test
    public void testToDouble() {
        // InstantConversions.toDouble ultimately returns a double representation of the epoch‑millis.
        // We compute the same value here for verification.
        double expected = (double) utcZdt.toInstant().toEpochMilli();
        double actual = ZonedDateTimeConversions.toDouble(utcZdt, converter);
        assertEquals(expected, actual, 0.0);
    }

    @Test
    public void testToLocalDateTime_SameZone() {
        // Converter’s zone is UTC, the source ZonedDateTime is also UTC – no adjustment expected.
        LocalDateTime expected = utcZdt.toLocalDateTime();
        assertEquals(expected, ZonedDateTimeConversions.toLocalDateTime(utcZdt, converter));
    }

    @Test
    public void testToLocalDateTime_DifferentZone() {
        // nyZdt represents the same instant as utcZdt but in New York. After conversion to UTC
        // the local date‑time must match utcZdt’s local date‑time.
        LocalDateTime expected = utcZdt.toLocalDateTime();
        assertEquals(expected, ZonedDateTimeConversions.toLocalDateTime(nyZdt, converter));
    }

    @Test
    public void testToLocalDate() {
        // toLocalDate uses the zone‑adjustment helper – the result must be the UTC date.
        LocalDate expected = utcZdt.toLocalDate();
        assertEquals(expected, ZonedDateTimeConversions.toLocalDate(utcZdt, converter));
    }

    @Test
    public void testToLocalTime() {
        LocalTime expected = utcZdt.toLocalTime();
        assertEquals(expected, ZonedDateTimeConversions.toLocalTime(utcZdt, converter));
    }

    @Test
    public void testToOffsetDateTime() {
        OffsetDateTime expected = utcZdt.toOffsetDateTime();
        assertEquals(expected, ZonedDateTimeConversions.toOffsetDateTime(utcZdt, converter));
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
        assertEquals(expected, ZonedDateTimeConversions.toTimestamp(utcZdt, converter));
    }

    @Test
    public void testToCalendar() {
        Calendar cal = ZonedDateTimeConversions.toCalendar(utcZdt, converter);
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
        assertEquals(utcZdt.toInstant().toEpochMilli(), cal.getTimeInMillis());
    }

    @Test
    public void testToSqlDate() {
        java.sql.Date expected = new java.sql.Date(utcZdt.toInstant().toEpochMilli());
        assertEquals(expected, ZonedDateTimeConversions.toSqlDate(utcZdt, converter));
    }

    @Test
    public void testToDate() {
        Date expected = new Date(utcZdt.toInstant().toEpochMilli());
        assertEquals(expected, ZonedDateTimeConversions.toDate(utcZdt, converter));
    }

    @Test
    public void testToBigInteger() {
        // InstantConversions.toBigInteger simply returns the epoch‑millis as a BigInteger.
        BigInteger expected = BigInteger.valueOf(utcZdt.toInstant().toEpochMilli());
        assertEquals(expected, ZonedDateTimeConversions.toBigInteger(utcZdt, converter));
    }

    @Test
    public void testToBigDecimal() {
        // InstantConversions.toBigDecimal returns the epoch‑millis as a BigDecimal.
        BigDecimal expected = BigDecimal.valueOf(utcZdt.toInstant().toEpochMilli());
        assertEquals(0, expected.compareTo(ZonedDateTimeConversions.toBigDecimal(utcZdt, converter)));
    }

    @Test
    public void testToString() {
        String expected = utcZdt.format(DateTimeFormatter.ISO_DATE_TIME);
        assertEquals(expected, ZonedDateTimeConversions.toString(utcZdt, converter));
    }

    @Test
    public void testToMap() {
        Map<String, Object> map = ZonedDateTimeConversions.toMap(utcZdt, converter);
        assertEquals(3, map.size());
        assertEquals(utcZdt.toLocalDate().toString(), map.get(DATE));
        assertEquals(utcZdt.toLocalTime().toString(), map.get(TIME));
        assertEquals(utcZdt.getZone().toString(), map.get(ZONE));
    }

    @Test
    public void testZoneAdjustmentLogic() {
        // Verify that a ZonedDateTime that is NOT in the converter's zone is correctly
        // converted to the converter's zone for the date‑only and time‑only helpers.
        // nyZdt is 5 hours behind UTC on 2023‑03‑25 (DST not yet in effect).
        LocalDate expectedDate = utcZdt.toLocalDate(); // 2023‑03‑25
        LocalTime expectedTime = utcZdt.toLocalTime(); // 14:30:15.123

        assertEquals(expectedDate, ZonedDateTimeConversions.toLocalDate(nyZdt, converter));
        assertEquals(expectedTime, ZonedDateTimeConversions.toLocalTime(nyZdt, converter));
    }
}
