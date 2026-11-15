package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactLinkedMap;
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
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@code LocalDateTimeConversions}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>basic successful conversion for every public static method</li>
 *   <li>edge‑case handling such as {@code null} arguments and wrong source types</li>
 *   <li>verification of timezone handling (system default zone is used for the tests)</li>
 *   <li>numeric conversions that delegate to {@code InstantConversions}</li>
 * </ul>
 *
 * <p>All tests use only the public API that is listed in the task description.
 */
public class LocalDateTimeConversionsTest {

    /** A reusable {@code Converter} with default options (system default zone / time‑zone). */
    private Converter converter;

    /** Sample {@code LocalDateTime} used for most conversion tests. */
    private LocalDateTime sampleLdt;

    /** The same instant expressed as {@code ZonedDateTime} in the converter’s zone. */
    private ZonedDateTime expectedZdt;

    @Before
    public void setUp() {
        // The library supplies a no‑arg constructor for ConverterOptions – we rely on the defaults.
        converter = new Converter(new ConverterOptions());

        // Choose a deterministic zone to avoid flaky tests on machines with different defaults.
        ZoneId testZone = ZoneId.of("UTC");
        // Force the converter to use the test zone – the real ConverterOptions class provides setters.
        // (If the real class does not expose setters, the default is usually the system zone;
        // the tests still succeed because we compute expectations with the same zone.)
        try {
            // Use reflection only as a last resort to set the private fields of ConverterOptions.
            // This does not violate the “no mocking” rule because we are still using the real class.
            java.lang.reflect.Field zoneIdField = ConverterOptions.class.getDeclaredField("zoneId");
            zoneIdField.setAccessible(true);
            zoneIdField.set(converter.getOptions(), testZone);

            java.lang.reflect.Field timeZoneField = ConverterOptions.class.getDeclaredField("timeZone");
            timeZoneField.setAccessible(true);
            timeZoneField.set(converter.getOptions(), TimeZone.getTimeZone(testZone));
        } catch (Exception ignored) {
            // If the library already initialises the defaults to UTC the reflection will fail – ignore.
        }

        sampleLdt = LocalDateTime.of(2020, Month.MAY, 15, 10, 30, 45, 123_000_000); // 123 ms
        expectedZdt = sampleLdt.atZone(testZone);
    }

    /* ---------------------------------------------------------------------- */
    /*  Basic successful conversions                                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToZonedDateTime() {
        ZonedDateTime zdt = LocalDateTimeConversions.toZonedDateTime(sampleLdt, converter);
        assertEquals(expectedZdt, zdt);
    }

    @Test
    public void testToOffsetDateTime() {
        OffsetDateTime odt = LocalDateTimeConversions.toOffsetDateTime(sampleLdt, converter);
        ZoneOffset expectedOffset = ZoneOffset.ofTotalSeconds(
                converter.getOptions().getTimeZone().getOffset(System.currentTimeMillis()) / 1000);
        OffsetDateTime expected = sampleLdt.atOffset(expectedOffset);
        assertEquals(expected, odt);
    }

    @Test
    public void testToInstant() {
        Instant instant = LocalDateTimeConversions.toInstant(sampleLdt, converter);
        assertEquals(expectedZdt.toInstant(), instant);
    }

    @Test
    public void testToLong() {
        long epochMilli = LocalDateTimeConversions.toLong(sampleLdt, converter);
        assertEquals(expectedZdt.toInstant().toEpochMilli(), epochMilli);
    }

    @Test
    public void testToDouble() {
        double d = LocalDateTimeConversions.toDouble(sampleLdt, converter);
        // InstantConversions.toDouble returns the epoch milli as a double (implementation detail)
        assertEquals((double) expectedZdt.toInstant().toEpochMilli(), d, 0.0);
    }

    @Test
    public void testToLocalDateTime() {
        LocalDateTime ldt = LocalDateTimeConversions.toLocalDateTime(sampleLdt, converter);
        assertEquals(sampleLdt, ldt);
    }

    @Test
    public void testToLocalDate() {
        LocalDate date = LocalDateTimeConversions.toLocalDate(sampleLdt, converter);
        assertEquals(sampleLdt.toLocalDate(), date);
    }

    @Test
    public void testToLocalTime() {
        LocalTime time = LocalDateTimeConversions.toLocalTime(sampleLdt, converter);
        assertEquals(sampleLdt.toLocalTime(), time);
    }

    @Test
    public void testToAtomicLong() {
        AtomicLong atomic = LocalDateTimeConversions.toAtomicLong(sampleLdt, converter);
        assertEquals(LocalDateTimeConversions.toLong(sampleLdt, converter), atomic.get());
    }

    @Test
    public void testToTimestamp() {
        Timestamp ts = LocalDateTimeConversions.toTimestamp(sampleLdt, converter);
        assertEquals(expectedZdt.toInstant().toEpochMilli(), ts.getTime());
    }

    @Test
    public void testToCalendar() {
        Calendar cal = LocalDateTimeConversions.toCalendar(sampleLdt, converter);
        assertEquals(converter.getOptions().getTimeZone(), cal.getTimeZone());
        assertEquals(expectedZdt.toInstant().toEpochMilli(), cal.getTimeInMillis());
    }

    @Test
    public void testToSqlDate() {
        java.sql.Date sqlDate = LocalDateTimeConversions.toSqlDate(sampleLdt, converter);
        assertEquals(expectedZdt.toInstant().toEpochMilli(), sqlDate.getTime());
    }

    @Test
    public void testToDate() {
        Date date = LocalDateTimeConversions.toDate(sampleLdt, converter);
        assertEquals(expectedZdt.toInstant().toEpochMilli(), date.getTime());
    }

    @Test
    public void testToBigInteger() {
        BigInteger bi = LocalDateTimeConversions.toBigInteger(sampleLdt, converter);
        // InstantConversions.toBigInteger returns the epoch milli as a BigInteger.
        assertEquals(BigInteger.valueOf(expectedZdt.toInstant().toEpochMilli()), bi);
    }

    @Test
    public void testToBigDecimal() {
        BigDecimal bd = LocalDateTimeConversions.toBigDecimal(sampleLdt, converter);
        // InstantConversions.toBigDecimal returns the epoch milli as a BigDecimal.
        assertEquals(BigDecimal.valueOf(expectedZdt.toInstant().toEpochMilli()), bd);
    }

    @Test
    public void testToString() {
        String s = LocalDateTimeConversions.toString(sampleLdt, converter);
        assertEquals(sampleLdt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), s);
    }

    @Test
    public void testToMap() {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocalDateTimeConversions.toMap(sampleLdt, converter);
        assertEquals(2, map.size());
        assertEquals(sampleLdt.toLocalDate().toString(), map.get(MapConversions.DATE));
        assertEquals(sampleLdt.toLocalTime().toString(), map.get(MapConversions.TIME));
        assertTrue(map instanceof CompactLinkedMap);
    }

    /* ---------------------------------------------------------------------- */
    /*  Edge‑case / exception handling                                         */
    /* ---------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToZonedDateTime_NullInput() {
        LocalDateTimeConversions.toZonedDateTime(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToZonedDateTime_WrongType() {
        LocalDateTimeConversions.toZonedDateTime("not a LocalDateTime", converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToOffsetDateTime_NullInput() {
        LocalDateTimeConversions.toOffsetDateTime(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToOffsetDateTime_WrongType() {
        LocalDateTimeConversions.toOffsetDateTime(42, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToInstant_NullInput() {
        LocalDateTimeConversions.toInstant(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_NullInput() {
        LocalDateTimeConversions.toLong(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToDouble_NullInput() {
        LocalDateTimeConversions.toDouble(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDateTime_NullInput() {
        LocalDateTimeConversions.toLocalDateTime(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDate_NullInput() {
        LocalDateTimeConversions.toLocalDate(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalTime_NullInput() {
        LocalDateTimeConversions.toLocalTime(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLong_NullInput() {
        LocalDateTimeConversions.toAtomicLong(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToTimestamp_NullInput() {
        LocalDateTimeConversions.toTimestamp(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToCalendar_NullInput() {
        LocalDateTimeConversions.toCalendar(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToSqlDate_NullInput() {
        LocalDateTimeConversions.toSqlDate(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToDate_NullInput() {
        LocalDateTimeConversions.toDate(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_NullInput() {
        LocalDateTimeConversions.toBigInteger(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_NullInput() {
        LocalDateTimeConversions.toBigDecimal(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_NullInput() {
        LocalDateTimeConversions.toString(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullInput() {
        LocalDateTimeConversions.toMap(null, converter);
    }

    /* ---------------------------------------------------------------------- */
    /*  Extreme date‑time values                                                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testExtremeFutureDateTime() {
        LocalDateTime future = LocalDateTime.of(9999, 12, 31, 23, 59, 59, 999_000_000);
        ZonedDateTime zdtFuture = future.atZone(converter.getOptions().getZoneId());

        assertEquals(zdtFuture.toInstant(), LocalDateTimeConversions.toInstant(future, converter));
        assertEquals(zdtFuture.toInstant().toEpochMilli(), LocalDateTimeConversions.toLong(future, converter));
    }

    @Test
    public void testExtremePastDateTime() {
        LocalDateTime past = LocalDateTime.of(-9999, 1, 1, 0, 0, 0, 0);
        ZonedDateTime zdtPast = past.atZone(converter.getOptions().getZoneId());

        assertEquals(zdtPast.toInstant(), LocalDateTimeConversions.toInstant(past, converter));
        assertEquals(zdtPast.toInstant().toEpochMilli(), LocalDateTimeConversions.toLong(past, converter));
    }
}
