package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JUnit‑4 test suite for {@link OffsetDateTimeConversions}.
 *
 * The tests cover:
 *   • Normal conversions for each static method.
 *   • Edge‑case handling (different offsets, epoch boundary, null arguments).
 *   • Behaviour of {@code toMap} and {@code toString}.
 *
 * Only public APIs that appear in the source code or in the supplied
 * {@code Converter} signature are used – no mocking frameworks, no subclassing
 * of library types and no external library calls.
 */
public class OffsetDateTimeConversionsTest {

    private Converter converter;
    private ZoneId testZoneId;
    private TimeZone testTimeZone;

    @Before
    public void setUp() {
        /*
         * The library provides a mutable {@code ConverterOptions} class that
         * supplies a {@code ZoneId} and a {@code TimeZone}.  The exact API of
         * {@code ConverterOptions} is not shown, but the source uses the two
         * getter methods below.  The test therefore creates a fresh options
         * instance, sets the zone to a known value and passes it to the
         * {@code Converter} constructor.
         */
        ConverterOptions options = new ConverterOptions();
        testZoneId = ZoneId.of("America/New_York");               // UTC‑05:00 (standard)
        testTimeZone = TimeZone.getTimeZone(testZoneId);
        options.setZoneId(testZoneId);
        options.setTimeZone(testTimeZone);
        converter = new Converter(options);
    }

    /**
     * Helper that builds an {@link OffsetDateTime} with the supplied instant
     * and a fixed offset of +02:00.  The offset is deliberately different from
     * the {@code testZoneId} to verify that the conversion methods correctly
     * normalise via {@code Instant}.
     */
    private static OffsetDateTime odtOf(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.ofHours(2));
    }

    @Test
    public void testToInstant() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        OffsetDateTime odt = odtOf(now);
        Instant result = OffsetDateTimeConversions.toInstant(odt, converter);
        assertEquals("Instant conversion must match the underlying instant", now, result);
    }

    @Test
    public void testToLong() {
        Instant instant = Instant.parse("2023-01-15T12:34:56.789Z");
        OffsetDateTime odt = odtOf(instant);
        long expected = instant.toEpochMilli();
        assertEquals(expected, OffsetDateTimeConversions.toLong(odt, converter));
    }

    @Test
    public void testToAtomicLong() {
        Instant instant = Instant.parse("1970-01-01T00:00:01Z"); // 1 000 ms
        OffsetDateTime odt = odtOf(instant);
        AtomicLong atomic = OffsetDateTimeConversions.toAtomicLong(odt, converter);
        assertEquals(1000L, atomic.get());
    }

    @Test
    public void testToDouble() {
        // 2022‑12‑31T23:59:59.123456789Z  →  epoch seconds + nanos/1e9
        Instant instant = Instant.ofEpochSecond(1672531199L, 123_456_789);
        OffsetDateTime odt = odtOf(instant);
        double expected = instant.getEpochSecond() + instant.getNano() / 1_000_000_000.0;
        double actual = OffsetDateTimeConversions.toDouble(odt, converter);
        assertEquals(expected, actual, 1e-12);
    }

    @Test
    public void testToBigInteger() {
        Instant instant = Instant.ofEpochSecond(1_600_000_000L); // 2020‑09‑13T12:26:40Z
        OffsetDateTime odt = odtOf(instant);
        // InstantConversions.toBigInteger converts the instant to the number of
        // milliseconds since the epoch (as a positive or negative BigInteger).
        BigInteger expected = BigInteger.valueOf(instant.toEpochMilli());
        BigInteger actual = OffsetDateTimeConversions.toBigInteger(odt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToBigDecimal() {
        Instant instant = Instant.ofEpochSecond(1_600_000_000L, 500_000_000); // .5 s
        OffsetDateTime odt = odtOf(instant);
        // Expected decimal = seconds + nanos/1_000_000_000, represented as
        // a BigDecimal with the same scale as the nanos part.
        BigDecimal expected = new BigDecimal(instant.getEpochSecond())
                .add(new BigDecimal(instant.getNano()).divide(BigDecimal.valueOf(1_000_000_000)));
        BigDecimal actual = OffsetDateTimeConversions.toBigDecimal(odt, converter);
        assertEquals(0, expected.compareTo(actual));
    }

    @Test
    public void testToLocalDateTime() {
        OffsetDateTime odt = OffsetDateTime.of(2021, 5, 21, 15, 30, 45, 0, ZoneOffset.ofHours(-4));
        // Conversion uses the converter's zone (America/New_York = UTC‑05)
        // The instant represented by the odt is 2021‑05‑21T19:30:45Z.
        // In New York (UTC‑05) that instant is 2021‑05‑21T14:30:45.
        LocalDateTime expected = LocalDateTime.of(2021, 5, 21, 14, 30, 45);
        assertEquals(expected, OffsetDateTimeConversions.toLocalDateTime(odt, converter));
    }

    @Test
    public void testToLocalDate() {
        OffsetDateTime odt = OffsetDateTime.of(2020, 2, 29, 23, 0, 0, 0, ZoneOffset.UTC);
        // In New York this instant is 2020‑02‑29T18:00:00‑05:00, same calendar day.
        LocalDate expected = LocalDate.of(2020, 2, 29);
        assertEquals(expected, OffsetDateTimeConversions.toLocalDate(odt, converter));
    }

    @Test
    public void testToLocalTime() {
        OffsetDateTime odt = OffsetDateTime.of(2020, 12, 31, 23, 0, 0, 0, ZoneOffset.UTC);
        // Convert to New York (UTC‑05) → 18:00:00
        LocalTime expected = LocalTime.of(18, 0, 0);
        assertEquals(expected, OffsetDateTimeConversions.toLocalTime(odt, converter));
    }

    @Test
    public void testToTimestamp() {
        Instant instant = Instant.parse("2000-01-01T00:00:00Z");
        OffsetDateTime odt = odtOf(instant);
        Timestamp ts = OffsetDateTimeConversions.toTimestamp(odt, converter);
        assertEquals(Timestamp.from(instant), ts);
    }

    @Test
    public void testToCalendar() {
        Instant instant = Instant.parse("1999-12-31T23:59:59.999Z");
        OffsetDateTime odt = odtOf(instant);
        Calendar cal = OffsetDateTimeConversions.toCalendar(odt, converter);
        assertEquals(testTimeZone, cal.getTimeZone());
        assertEquals(instant.toEpochMilli(), cal.getTimeInMillis());
    }

    @Test
    public void testToSqlDate() {
        OffsetDateTime odt = OffsetDateTime.of(2022, 3, 15, 10, 20, 30, 0, ZoneOffset.ofHours(3));
        java.sql.Date sqlDate = OffsetDateTimeConversions.toSqlDate(odt, converter);
        // In New York the same instant is 2022‑03‑15.
        assertEquals(java.sql.Date.valueOf("2022-03-15"), sqlDate);
    }

    @Test
    public void testToZonedDateTime() {
        Instant instant = Instant.parse("2021-07-01T12:00:00Z");
        OffsetDateTime odt = odtOf(instant);
        ZonedDateTime zdt = OffsetDateTimeConversions.toZonedDateTime(odt, converter);
        assertEquals(testZoneId, zdt.getZone());
        assertEquals(instant, zdt.toInstant());
    }

    @Test
    public void testToDate() {
        Instant instant = Instant.parse("2010-06-30T15:45:30.123Z");
        OffsetDateTime odt = odtOf(instant);
        Date date = OffsetDateTimeConversions.toDate(odt, converter);
        assertEquals(instant.toEpochMilli(), date.getTime());
    }

    @Test
    public void testToOffsetTime() {
        OffsetDateTime odt = OffsetDateTime.of(2021, 8, 10, 9, 15, 30, 0, ZoneOffset.ofHours(-7));
        OffsetTime expected = OffsetTime.of(9, 15, 30, 0, ZoneOffset.ofHours(-7));
        assertEquals(expected, OffsetDateTimeConversions.toOffsetTime(odt, converter));
    }

    @Test
    public void testToYear() {
        OffsetDateTime odt = OffsetDateTime.of(1995, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC);
        Year year = OffsetDateTimeConversions.toYear(odt, converter);
        assertEquals(Year.of(1995), year);
    }

    @Test
    public void testToYearMonth() {
        OffsetDateTime odt = OffsetDateTime.of(2005, 5, 15, 0, 0, 0, 0, ZoneOffset.UTC);
        YearMonth ym = OffsetDateTimeConversions.toYearMonth(odt, converter);
        assertEquals(YearMonth.of(2005, 5), ym);
    }

    @Test
    public void testToMonthDay() {
        OffsetDateTime odt = OffsetDateTime.of(2020, 2, 29, 12, 0, 0, 0, ZoneOffset.UTC);
        MonthDay md = OffsetDateTimeConversions.toMonthDay(odt, converter);
        assertEquals(MonthDay.of(2, 29), md);
    }

    @Test
    public void testToString() {
        OffsetDateTime odt = OffsetDateTime.of(2021, 11, 5, 13, 45, 30, 0, ZoneOffset.ofHoursMinutes(5, 30));
        String expected = odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        assertEquals(expected, OffsetDateTimeConversions.toString(odt, converter));
    }

    @Test
    public void testToMap() {
        OffsetDateTime odt = OffsetDateTime.of(2022, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        Map<String, Object> map = OffsetDateTimeConversions.toMap(odt, converter);
        assertTrue(map.containsKey(MapConversions.OFFSET_DATE_TIME));
        assertEquals(OffsetDateTimeConversions.toString(odt, converter), map.get(MapConversions.OFFSET_DATE_TIME));
    }

    @Test(expected = NullPointerException.class)
    public void testNullOffsetDateTimeThrowsNPE() {
        // All conversion methods dereference the object as an OffsetDateTime,
        // therefore a null argument must result in NPE.
        OffsetDateTimeConversions.toInstant(null, converter);
    }

    @Test
    public void testEdgeCase_EpochStart() {
        // The instant exactly at the epoch (1970‑01‑01T00:00:00Z)
        Instant epoch = Instant.EPOCH;
        OffsetDateTime odt = odtOf(epoch);
        assertEquals(0L, OffsetDateTimeConversions.toLong(odt, converter));
        assertEquals(0.0, OffsetDateTimeConversions.toDouble(odt, converter), 0.0);
        assertEquals(BigInteger.ZERO, OffsetDateTimeConversions.toBigInteger(odt, converter));
        assertEquals(BigDecimal.ZERO, OffsetDateTimeConversions.toBigDecimal(odt, converter));
    }

    @Test
    public void testEdgeCase_NegativeEpoch() {
        // One second before the epoch.
        Instant before = Instant.ofEpochSecond(-1, 0);
        OffsetDateTime odt = odtOf(before);
        assertEquals(-1000L, OffsetDateTimeConversions.toLong(odt, converter));
        assertEquals(-1.0, OffsetDateTimeConversions.toDouble(odt, converter), 1e-12);
        assertEquals(BigInteger.valueOf(-1000L), OffsetDateTimeConversions.toBigInteger(odt, converter));
        assertEquals(new BigDecimal("-1.0"), OffsetDateTimeConversions.toBigDecimal(odt, converter));
    }
}
