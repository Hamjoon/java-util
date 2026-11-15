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
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link LocalDateConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>normal conversion paths for every static method</li>
 *   <li>edge cases (epoch, far future dates, leap day)</li>
 *   <li>error scenarios (null source, wrong source type)</li>
 * </ul>
 *
 * The {@link Converter} is created with default {@link ConverterOptions}.  The default
 * options use the JVM default {@link ZoneId} and {@link TimeZone}, which is sufficient
 * for deterministic assertions because the test uses the same defaults when calculating
 * expected values.
 */
public class LocalDateConversionsTest {

    private Converter converter;
    private ZoneId defaultZoneId;
    private TimeZone defaultTimeZone;

    @Before
    public void setUp() {
        // The library provides a public ConverterOptions class with a no‑arg constructor.
        // The default options use the JVM default zone and time‑zone.
        ConverterOptions options = new ConverterOptions(); // assumed public
        converter = new Converter(options);
        defaultZoneId = converter.getOptions().getZoneId();
        defaultTimeZone = converter.getOptions().getTimeZone();
    }

    /** Helper to create a {@link LocalDate} from a year‑month‑day string. */
    private LocalDate ld(String isoDate) {
        return LocalDate.parse(isoDate, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    /* ---------------------------------------------------------------------- */
    /*  toInstant                                                             */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToInstant_NormalDate() {
        LocalDate source = ld("2023-03-15");
        Instant expected = ZonedDateTime.of(source, LocalTime.MIDNIGHT, defaultZoneId).toInstant();
        assertEquals(expected, LocalDateConversions.toInstant(source, converter));
    }

    @Test
    public void testToInstant_Epoch() {
        LocalDate epoch = LocalDate.ofEpochDay(0); // 1970‑01‑01
        Instant expected = ZonedDateTime.of(epoch, LocalTime.MIDNIGHT, defaultZoneId).toInstant();
        assertEquals(expected, LocalDateConversions.toInstant(epoch, converter));
    }

    @Test(expected = NullPointerException.class)
    public void testToInstant_NullSource() {
        LocalDateConversions.toInstant(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToInstant_WrongSourceType() {
        LocalDateConversions.toInstant("2023-01-01", converter);
    }

    /* ---------------------------------------------------------------------- */
    /*  toLong                                                                */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToLong_NormalDate() {
        LocalDate source = ld("2021-12-31");
        long expected = LocalDateConversions.toInstant(source, converter).toEpochMilli();
        assertEquals(expected, LocalDateConversions.toLong(source, converter));
    }

    @Test
    public void testToLong_LeapDay() {
        LocalDate leap = ld("2020-02-29");
        long expected = LocalDateConversions.toInstant(leap, converter).toEpochMilli();
        assertEquals(expected, LocalDateConversions.toLong(leap, converter));
    }

    /* ---------------------------------------------------------------------- */
    /*  toLocalDateTime                                                       */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToLocalDateTime() {
        LocalDate source = ld("1999-07-04");
        LocalDateTime expected = ZonedDateTime.of(source, LocalTime.MIDNIGHT, defaultZoneId)
                                               .toLocalDateTime();
        assertEquals(expected, LocalDateConversions.toLocalDateTime(source, converter));
    }

    /* ---------------------------------------------------------------------- */
    /*  toZonedDateTime                                                        */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToZonedDateTime() {
        LocalDate source = ld("2000-01-01");
        ZonedDateTime expected = ZonedDateTime.of(source, LocalTime.MIDNIGHT, defaultZoneId);
        assertEquals(expected, LocalDateConversions.toZonedDateTime(source, converter));
    }

    /* ---------------------------------------------------------------------- */
    /*  toOffsetDateTime                                                      */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToOffsetDateTime() {
        LocalDate source = ld("2022-11-05");
        // Compute the offset that the library will compute:
        int offsetMillis = defaultTimeZone.getOffset(System.currentTimeMillis());
        ZoneOffset offset = ZoneOffset.ofTotalSeconds(offsetMillis / 1000);
        OffsetDateTime expected = OffsetDateTime.of(source, LocalTime.MIDNIGHT, offset);
        assertEquals(expected, LocalDateConversions.toOffsetDateTime(source, converter));
    }

    @Test
    public void testToOffsetDateTime_DifferentTimeZone() {
        // Use a converter with a known time‑zone (UTC‑5) to verify offset handling.
        ConverterOptions opts = new ConverterOptions();
        opts.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        opts.setZoneId(ZoneId.of("America/New_York"));
        Converter nyConverter = new Converter(opts);

        LocalDate source = ld("2022-03-13"); // DST change day
        int offsetMillis = opts.getTimeZone().getOffset(System.currentTimeMillis());
        ZoneOffset offset = ZoneOffset.ofTotalSeconds(offsetMillis / 1000);
        OffsetDateTime expected = OffsetDateTime.of(source, LocalTime.MIDNIGHT, offset);
        assertEquals(expected, LocalDateConversions.toOffsetDateTime(source, nyConverter));
    }

    /* ---------------------------------------------------------------------- */
    /*  toDouble                                                               */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToDouble() {
        LocalDate source = ld("1970-01-02"); // exactly 1 day after epoch
        double expected = 24 * 60 * 60; // seconds in a day
        assertEquals(expected, LocalDateConversions.toDouble(source, converter), 0.0);
    }

    /* ---------------------------------------------------------------------- */
    /*  toAtomicLong                                                           */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToAtomicLong() {
        LocalDate source = ld("2005-05-05");
        long expected = LocalDateConversions.toLong(source, converter);
        AtomicLong atomic = LocalDateConversions.toAtomicLong(source, converter);
        assertEquals(expected, atomic.get());
    }

    /* ---------------------------------------------------------------------- */
    /*  toTimestamp                                                            */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToTimestamp() {
        LocalDate source = ld("1970-01-01");
        Timestamp ts = LocalDateConversions.toTimestamp(source, converter);
        assertEquals(0L, ts.getTime());
    }

    @Test
    public void testToTimestamp_FarFuture() {
        LocalDate source = ld("3000-01-01");
        long days = source.toEpochDay();
        long expectedMillis = days * 86_400_000L;
        assertEquals(expectedMillis, LocalDateConversions.toTimestamp(source, converter).getTime());
    }

    /* ---------------------------------------------------------------------- */
    /*  toCalendar                                                             */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToCalendar() {
        LocalDate source = ld("2021-01-01");
        Calendar cal = LocalDateConversions.toCalendar(source, converter);
        long expected = LocalDateConversions.toInstant(source, converter).toEpochMilli();
        assertEquals(expected, cal.getTimeInMillis());
        assertEquals(defaultTimeZone, cal.getTimeZone());
    }

    /* ---------------------------------------------------------------------- */
    /*  toSqlDate                                                              */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToSqlDate() {
        LocalDate source = ld("2000-02-29");
        java.sql.Date sqlDate = LocalDateConversions.toSqlDate(source, converter);
        long expected = LocalDateConversions.toLong(source, converter);
        assertEquals(expected, sqlDate.getTime());
    }

    /* ---------------------------------------------------------------------- */
    /*  toDate                                                                 */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToDate() {
        LocalDate source = ld("1995-12-31");
        Date date = LocalDateConversions.toDate(source, converter);
        long expected = LocalDateConversions.toLong(source, converter);
        assertEquals(expected, date.getTime());
    }

    /* ---------------------------------------------------------------------- */
    /*  toBigInteger & toBigDecimal                                            */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToBigInteger() {
        LocalDate source = ld("1970-01-01");
        BigInteger bi = LocalDateConversions.toBigInteger(source, converter);
        // InstantConversions.toBigInteger simply returns the epoch milli as BigInteger.
        assertEquals(BigInteger.ZERO, bi);
    }

    @Test
    public void testToBigDecimal() {
        LocalDate source = ld("1970-01-02");
        BigDecimal bd = LocalDateConversions.toBigDecimal(source, converter);
        // Expected: 86_400_000 (milliseconds) as BigDecimal
        assertEquals(new BigDecimal(86_400_000L), bd);
    }

    /* ---------------------------------------------------------------------- */
    /*  toString                                                               */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToString() {
        LocalDate source = ld("2024-10-13");
        String expected = "2024-10-13";
        assertEquals(expected, LocalDateConversions.toString(source, converter));
    }

    /* ---------------------------------------------------------------------- */
    /*  toMap                                                                  */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToMap() {
        LocalDate source = ld("2022-06-30");
        Map<String, Object> map = LocalDateConversions.toMap(source, converter);
        assertNotNull(map);
        assertTrue(map.containsKey(MapConversions.DATE));
        assertEquals(source.toString(), map.get(MapConversions.DATE));
    }

    /* ---------------------------------------------------------------------- */
    /*  Error handling – null converter                                         */
    /* ---------------------------------------------------------------------- */
    @Test(expected = NullPointerException.class)
    public void testMethods_NullConverter() {
        LocalDate source = ld("2020-01-01");
        // Any method that uses the converter will NPE when the converter argument is null.
        LocalDateConversions.toInstant(source, null);
    }

    /* ---------------------------------------------------------------------- */
    /*  Error handling – null source                                            */
    /* ---------------------------------------------------------------------- */
    @Test(expected = NullPointerException.class)
    public void testToString_NullSource() {
        LocalDateConversions.toString(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullSource() {
        LocalDateConversions.toMap(null, converter);
    }
}
