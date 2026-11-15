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

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link OffsetDateTimeConversions}.
 *
 * The tests cover:
 *   • normal (happy‑path) conversions,
 *   • edge values (epoch, far past/future, different offsets),
 *   • error handling – null arguments and wrong source types,
 *   • verification of auxiliary objects (Calendar time‑zone, Map contents, etc.).
 *
 * The {@link Converter} used in the tests is created with the default
 * {@link ConverterOptions}.  Those options expose the system default
 * {@link java.util.TimeZone} and {@link java.time.ZoneId}, which are sufficient
 * for the conversion logic in {@code OffsetDateTimeConversions}.
 */
public class OffsetDateTimeConversionsTest {

    private Converter converter;               // shared for all tests
    private ZoneId utcZone;                    // reference zone used in assertions
    private OffsetDateTime utcOdt;              // OffsetDateTime at UTC (epoch)
    private OffsetDateTime plusTwoOdt;         // OffsetDateTime with +02:00 offset
    private OffsetDateTime minusFiveOdt;       // OffsetDateTime with -05:00 offset
    private OffsetDateTime farFutureOdt;       // far future instant (year 3000)

    @Before
    public void setUp() {
        // The Converter class requires a ConverterOptions instance.
        // The library supplies a public no‑arg constructor for the options.
        converter = new Converter(new ConverterOptions());

        utcZone = ZoneId.of("UTC");
        utcOdt = OffsetDateTime.ofInstant(Instant.EPOCH, utcZone);                     // 1970‑01‑01T00:00Z
        plusTwoOdt = OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.ofHours(2));   // 1970‑01‑01T02:00+02:00
        minusFiveOdt = OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.ofHours(-5)); // 1969‑12‑31T19:00-05:00
        farFutureOdt = OffsetDateTime.ofInstant(Instant.parse("3000-01-01T00:00:00Z"), utcZone);
    }

    /* -----------------------------------------------------------
     *  Happy‑path conversion tests
     * ----------------------------------------------------------- */

    @Test
    public void testToInstant() {
        assertEquals(Instant.EPOCH, OffsetDateTimeConversions.toInstant(utcOdt, converter));
        assertEquals(Instant.EPOCH, OffsetDateTimeConversions.toInstant(plusTwoOdt, converter));
        assertEquals(Instant.EPOCH, OffsetDateTimeConversions.toInstant(minusFiveOdt, converter));
        assertEquals(Instant.parse("3000-01-01T00:00:00Z"),
                OffsetDateTimeConversions.toInstant(farFutureOdt, converter));
    }

    @Test
    public void testToLong() {
        assertEquals(0L, OffsetDateTimeConversions.toLong(utcOdt, converter));
        assertEquals(0L, OffsetDateTimeConversions.toLong(plusTwoOdt, converter));
        assertEquals(0L, OffsetDateTimeConversions.toLong(minusFiveOdt, converter));
        assertEquals(Instant.parse("3000-01-01T00:00:00Z").toEpochMilli(),
                OffsetDateTimeConversions.toLong(farFutureOdt, converter));
    }

    @Test
    public void testToLocalDateTime() {
        LocalDateTime expected = LocalDateTime.ofInstant(Instant.EPOCH, utcZone);
        assertEquals(expected, OffsetDateTimeConversions.toLocalDateTime(utcOdt, converter));
        assertEquals(expected, OffsetDateTimeConversions.toLocalDateTime(plusTwoOdt, converter));
        assertEquals(expected, OffsetDateTimeConversions.toLocalDateTime(minusFiveOdt, converter));
    }

    @Test
    public void testToLocalDate() {
        LocalDate expected = LocalDate.of(1970, 1, 1);
        assertEquals(expected, OffsetDateTimeConversions.toLocalDate(utcOdt, converter));
        assertEquals(expected, OffsetDateTimeConversions.toLocalDate(plusTwoOdt, converter));
        assertEquals(expected, OffsetDateTimeConversions.toLocalDate(minusFiveOdt, converter));
    }

    @Test
    public void testToLocalTime() {
        LocalTime expected = LocalTime.MIDNIGHT;
        assertEquals(expected, OffsetDateTimeConversions.toLocalTime(utcOdt, converter));
        assertEquals(expected, OffsetDateTimeConversions.toLocalTime(plusTwoOdt, converter));
        assertEquals(expected, OffsetDateTimeConversions.toLocalTime(minusFiveOdt, converter));
    }

    @Test
    public void testToAtomicLong() {
        assertEquals(new AtomicLong(0L), OffsetDateTimeConversions.toAtomicLong(utcOdt, converter));
        assertEquals(new AtomicLong(0L), OffsetDateTimeConversions.toAtomicLong(minusFiveOdt, converter));
    }

    @Test
    public void testToTimestamp() {
        Timestamp ts = Timestamp.from(Instant.EPOCH);
        assertEquals(ts, OffsetDateTimeConversions.toTimestamp(utcOdt, converter));
        assertEquals(ts, OffsetDateTimeConversions.toTimestamp(plusTwoOdt, converter));
    }

    @Test
    public void testToCalendar() {
        Calendar cal = Calendar.getInstance(converter.getOptions().getTimeZone());
        cal.setTimeInMillis(0L);
        Calendar result = OffsetDateTimeConversions.toCalendar(utcOdt, converter);
        assertEquals(cal.getTimeInMillis(), result.getTimeInMillis());
        assertEquals(cal.getTimeZone(), result.getTimeZone());
    }

    @Test
    public void testToSqlDate() {
        java.sql.Date sqlDate = new java.sql.Date(0L);
        assertEquals(sqlDate, OffsetDateTimeConversions.toSqlDate(utcOdt, converter));
    }

    @Test
    public void testToZonedDateTime() {
        ZonedDateTime zdt = Instant.EPOCH.atZone(converter.getOptions().getZoneId());
        assertEquals(zdt, OffsetDateTimeConversions.toZonedDateTime(utcOdt, converter));
        // Verify that the offset part of the original ODT is ignored – conversion uses the
        // ConverterOptions zone, not the ODT's own offset.
        assertEquals(converter.getOptions().getZoneId(), OffsetDateTimeConversions.toZonedDateTime(plusTwoOdt, converter).getZone());
    }

    @Test
    public void testToDate() {
        Date date = new Date(0L);
        assertEquals(date, OffsetDateTimeConversions.toDate(utcOdt, converter));
    }

    @Test
    public void testToBigInteger() {
        // InstantConversions delegates to the same logic used for Instant → BigInteger.
        // For the epoch, the value is zero.
        assertEquals(BigInteger.ZERO, OffsetDateTimeConversions.toBigInteger(utcOdt, converter));
        // For farFuture, compute expected value.
        BigInteger expected = BigInteger.valueOf(Instant.parse("3000-01-01T00:00:00Z").toEpochMilli());
        assertEquals(expected, OffsetDateTimeConversions.toBigInteger(farFutureOdt, converter));
    }

    @Test
    public void testToBigDecimal() {
        // Epoch instant → 0.0
        assertEquals(BigDecimal.ZERO, OffsetDateTimeConversions.toBigDecimal(utcOdt, converter));

        // Far‑future instant → seconds + nanos as decimal.
        Instant future = Instant.parse("3000-01-01T00:00:00Z");
        BigDecimal expected = new BigDecimal(future.getEpochSecond())
                .add(new BigDecimal(future.getNano()).movePointLeft(9));
        assertEquals(0, expected.compareTo(OffsetDateTimeConversions.toBigDecimal(farFutureOdt, converter)));
    }

    @Test
    public void testToOffsetTime() {
        OffsetTime expected = utcOdt.toOffsetTime();
        assertEquals(expected, OffsetDateTimeConversions.toOffsetTime(utcOdt, converter));
        assertEquals(plusTwoOdt.toOffsetTime(), OffsetDateTimeConversions.toOffsetTime(plusTwoOdt, converter));
    }

    @Test
    public void testToString() {
        String iso = utcOdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        assertEquals(iso, OffsetDateTimeConversions.toString(utcOdt, converter));
        assertEquals(plusTwoOdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                OffsetDateTimeConversions.toString(plusTwoOdt, converter));
    }

    @Test
    public void testToMap() {
        Map<String, Object> map = OffsetDateTimeConversions.toMap(utcOdt, converter);
        ZonedDateTime zdt = OffsetDateTimeConversions.toZonedDateTime(utcOdt, converter);

        assertEquals(zdt.toLocalDate().toString(), map.get(MapConversions.DATE));
        assertEquals(zdt.toLocalTime().toString(), map.get(MapConversions.TIME));
        assertEquals(zdt.getOffset().toString(), map.get(MapConversions.OFFSET));

        // Verify map implementation is a CompactMap (insertion order preserved)
        assertTrue(map instanceof CompactMap);
        assertEquals(3, map.size());
    }

    @Test
    public void testToDouble() {
        double expected = 0.0d; // epoch instant
        assertEquals(expected, OffsetDateTimeConversions.toDouble(utcOdt, converter), 0.0);

        // Future instant: compute expected double via Instant → seconds + nanos
        Instant future = Instant.parse("3000-01-01T00:00:00Z");
        double futureExpected = BigDecimalConversions.secondsAndNanosToDouble(future.getEpochSecond(),
                future.getNano()).doubleValue();
        assertEquals(futureExpected, OffsetDateTimeConversions.toDouble(farFutureOdt, converter), 0.0);
    }

    /* -----------------------------------------------------------
     *  Edge‑case & exception tests
     * ----------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToInstant_NullInput() {
        OffsetDateTimeConversions.toInstant(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_NullInput() {
        OffsetDateTimeConversions.toLong(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToInstant_WrongType() {
        OffsetDateTimeConversions.toInstant("not an OffsetDateTime", converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToString_WrongType() {
        OffsetDateTimeConversions.toString(Boolean.TRUE, converter);
    }

    @Test
    public void testToLong_NegativeEpoch() {
        // OffsetDateTime before 1970‑01‑01 produces negative epoch millis.
        OffsetDateTime beforeEpoch = OffsetDateTime.ofInstant(Instant.parse("-0005-01-01T00:00:00Z"), utcZone);
        long expected = beforeEpoch.toInstant().toEpochMilli();
        assertEquals(expected, OffsetDateTimeConversions.toLong(beforeEpoch, converter));
    }

    @Test
    public void testToCalendar_DifferentTimeZone() {
        // Change the ConverterOptions time‑zone to a non‑UTC zone and verify that the
        // Calendar reflects this zone while still holding the same instant.
        ConverterOptions opts = new ConverterOptions();
        opts.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        Converter nyConverter = new Converter(opts);

        Calendar cal = OffsetDateTimeConversions.toCalendar(utcOdt, nyConverter);
        assertEquals(TimeZone.getTimeZone("America/New_York"), cal.getTimeZone());
        assertEquals(0L, cal.getTimeInMillis());
    }

    @Test
    public void testToMap_OffsetPreservation() {
        // Ensure that the map contains the *original* offset, not the zone‑id derived from
        // ConverterOptions.  The offset is taken from the ZonedDateTime produced by
        // toZonedDateTime, which uses the ConverterOptions zone, so we verify that the
        // offset matches that zone.
        ConverterOptions opts = new ConverterOptions();
        opts.setZoneId(ZoneId.of("Europe/Paris"));
        Converter parisConverter = new Converter(opts);

        Map<String, Object> map = OffsetDateTimeConversions.toMap(plusTwoOdt, parisConverter);
        ZonedDateTime zdt = OffsetDateTimeConversions.toZonedDateTime(plusTwoOdt, parisConverter);
        assertEquals(zdt.getOffset().toString(), map.get(MapConversions.OFFSET));
    }

    @Test
    public void testToDouble_NanosecondPrecision() {
        // Create an instant with nanoseconds that are not a whole millisecond.
        OffsetDateTime odt = OffsetDateTime.ofInstant(Instant.ofEpochSecond(1, 123_456_789), utcZone);
        double expected = BigDecimalConversions.secondsAndNanosToDouble(1, 123_456_789).doubleValue();
        assertEquals(expected, OffsetDateTimeConversions.toDouble(odt, converter), 0.0);
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullInput() {
        OffsetDateTimeConversions.toMap(null, converter);
    }
}
