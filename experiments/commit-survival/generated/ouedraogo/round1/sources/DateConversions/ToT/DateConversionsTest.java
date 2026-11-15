package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

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
 * Comprehensive JUnit‑4 test suite for {@link DateConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Normal conversions for each public static method.</li>
 *   <li>Edge cases – {@code null} arguments and wrong argument types.</li>
 *   <li>Behaviour that depends on the {@link ConverterOptions} time‑zone.</li>
 *   <li>Correctness of the map produced by {@code toMap()}.</li>
 * </ul>
 *
 * The real {@code Converter} and {@code ConverterOptions} classes from the
 * Cedar library are used – no mocking, no subclassing, only the public API.
 */
public class DateConversionsTest {

    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final ZoneId PST = ZoneId.of("America/Los_Angeles");

    private Converter utcConverter;
    private Converter pstConverter;

    /** Helper to create a {@link Converter} with a specific zone. */
    private static Converter createConverter(ZoneId zone) {
        // The real library provides a public no‑arg constructor for ConverterOptions.
        // The zone can be set via the options API – the exact method name is not
        // part of the public API shown, but in the actual library it is
        // {@code setZoneId(ZoneId)}.
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(zone);          // <-- assumed public setter
        return new Converter(options);
    }

    @Before
    public void setUp() {
        utcConverter = createConverter(UTC);
        pstConverter = createConverter(PST);
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /*-------------------------------------------------------------
     *  toZonedDateTime
     *------------------------------------------------------------*/
    @Test
    public void testToZonedDateTime_Utc() {
        Date date = new Date(1_600_000_000_000L); // 2020‑09‑13T12:26:40Z
        ZonedDateTime zdt = DateConversions.toZonedDateTime(date, utcConverter);
        assertEquals(UTC, zdt.getZone());
        assertEquals(Instant.ofEpochMilli(date.getTime()), zdt.toInstant());
    }

    @Test
    public void testToZonedDateTime_Pst() {
        Date date = new Date(1_600_000_000_000L);
        ZonedDateTime zdt = DateConversions.toZonedDateTime(date, pstConverter);
        assertEquals(PST, zdt.getZone());
        assertEquals(Instant.ofEpochMilli(date.getTime()), zdt.toInstant());
    }

    @Test
    public void testToZonedDateTime_Null() {
        thrown.expect(NullPointerException.class);
        DateConversions.toZonedDateTime(null, utcConverter);
    }

    @Test
    public void testToZonedDateTime_WrongType() {
        thrown.expect(ClassCastException.class);
        DateConversions.toZonedDateTime("not a date", utcConverter);
    }

    /*-------------------------------------------------------------
     *  toLong
     *------------------------------------------------------------*/
    @Test
    public void testToLong() {
        Date date = new Date(123456789L);
        long epoch = DateConversions.toLong(date, utcConverter);
        assertEquals(123456789L, epoch);
    }

    @Test
    public void testToLong_Null() {
        thrown.expect(NullPointerException.class);
        DateConversions.toLong(null, utcConverter);
    }

    /*-------------------------------------------------------------
     *  toDouble
     *------------------------------------------------------------*/
    @Test
    public void testToDouble() {
        Date date = new Date(1_500L); // 1.5 seconds
        double seconds = DateConversions.toDouble(date, utcConverter);
        assertEquals(1.5, seconds, 0.000_001);
    }

    @Test
    public void testToDouble_Null() {
        thrown.expect(NullPointerException.class);
        DateConversions.toDouble(null, utcConverter);
    }

    /*-------------------------------------------------------------
     *  toSqlDate
     *------------------------------------------------------------*/
    @Test
    public void testToSqlDate() {
        Date date = new Date(987_654_321L);
        java.sql.Date sqlDate = DateConversions.toSqlDate(date, utcConverter);
        assertEquals(date.getTime(), sqlDate.getTime());
    }

    /*-------------------------------------------------------------
     *  toDate
     *------------------------------------------------------------*/
    @Test
    public void testToDate() {
        Date date = new Date(42L);
        Date result = DateConversions.toDate(date, utcConverter);
        assertEquals(date.getTime(), result.getTime());
        // ensure a new instance is created
        assertNotSame(date, result);
    }

    /*-------------------------------------------------------------
     *  toTimestamp
     *------------------------------------------------------------*/
    @Test
    public void testToTimestamp() {
        Date date = new Date(1_234_567L);
        Timestamp ts = DateConversions.toTimestamp(date, utcConverter);
        assertEquals(date.getTime(), ts.getTime());
    }

    /*-------------------------------------------------------------
     *  toCalendar
     *------------------------------------------------------------*/
    @Test
    public void testToCalendar() {
        Date date = new Date(2_000_000L);
        Calendar cal = DateConversions.toCalendar(date, utcConverter);
        assertEquals(date.getTime(), cal.getTimeInMillis());
        assertEquals(UTC, cal.getTimeZone().toZoneId());
    }

    /*-------------------------------------------------------------
     *  toBigDecimal
     *------------------------------------------------------------*/
    @Test
    public void testToBigDecimal() {
        // epoch millis = 1_234_567_890
        Date date = new Date(1_234_567_890L);
        BigDecimal result = DateConversions.toBigDecimal(date, utcConverter);
        // The library divides by 1_000 (GRAND) with scale 9, rounding down.
        BigDecimal expected = new BigDecimal(date.getTime())
                .divide(BigDecimalConversions.GRAND, 9, RoundingMode.DOWN);
        assertEquals(0, expected.compareTo(result));
    }

    @Test
    public void testToBigDecimal_Null() {
        thrown.expect(NullPointerException.class);
        DateConversions.toBigDecimal(null, utcConverter);
    }

    /*-------------------------------------------------------------
     *  toInstant
     *------------------------------------------------------------*/
    @Test
    public void testToInstant_UtilDate() {
        Date date = new Date(5_000L);
        Instant instant = DateConversions.toInstant(date, utcConverter);
        assertEquals(date.toInstant(), instant);
    }

    @Test
    public void testToInstant_SqlDate() {
        java.sql.Date sqlDate = new java.sql.Date(7_000L);
        Instant instant = DateConversions.toInstant(sqlDate, utcConverter);
        // The conversion creates a java.util.Date wrapper first.
        assertEquals(Instant.ofEpochMilli(sqlDate.getTime()), instant);
    }

    @Test
    public void testToInstant_Null() {
        thrown.expect(NullPointerException.class);
        DateConversions.toInstant(null, utcConverter);
    }

    /*-------------------------------------------------------------
     *  toOffsetDateTime
     *------------------------------------------------------------*/
    @Test
    public void testToOffsetDateTime() {
        Date date = new Date(1_600_000_000_000L);
        OffsetDateTime odt = DateConversions.toOffsetDateTime(date, utcConverter);
        assertEquals(Instant.ofEpochMilli(date.getTime()), odt.toInstant());
        assertEquals(UTC, odt.getOffset().getRules().getOffset(odt));
    }

    /*-------------------------------------------------------------
     *  toLocalDateTime
     *------------------------------------------------------------*/
    @Test
    public void testToLocalDateTime() {
        Date date = new Date(1_600_000_000_000L);
        LocalDateTime ldt = DateConversions.toLocalDateTime(date, utcConverter);
        assertEquals(LocalDateTime.ofInstant(date.toInstant(), UTC), ldt);
    }

    /*-------------------------------------------------------------
     *  toLocalDate
     *------------------------------------------------------------*/
    @Test
    public void testToLocalDate() {
        Date date = new Date(1_600_000_000_000L);
        LocalDate ld = DateConversions.toLocalDate(date, utcConverter);
        assertEquals(LocalDate.ofInstant(date.toInstant(), UTC), ld);
    }

    /*-------------------------------------------------------------
     *  toLocalTime
     *------------------------------------------------------------*/
    @Test
    public void testToLocalTime() {
        // 13:45:30.123 UTC
        Date date = new Date(Instant.parse("2020-09-13T13:45:30.123Z").toEpochMilli());
        LocalTime lt = DateConversions.toLocalTime(date, utcConverter);
        assertEquals(LocalTime.of(13, 45, 30, 123_000_000), lt);
    }

    /*-------------------------------------------------------------
     *  toBigInteger
     *------------------------------------------------------------*/
    @Test
    public void testToBigInteger() {
        Date date = new Date(1_600_000_123_456L);
        Instant instant = date.toInstant();
        BigInteger expected = instant.getEpochSecond() == 0
                ? BigInteger.valueOf(instant.getNano())
                : BigInteger.valueOf(instant.getEpochSecond())
                  .multiply(BigInteger.valueOf(1_000_000_000L))
                  .add(BigInteger.valueOf(instant.getNano()));
        BigInteger result = DateConversions.toBigInteger(date, utcConverter);
        assertEquals(expected, result);
    }

    /*-------------------------------------------------------------
     *  toAtomicLong
     *------------------------------------------------------------*/
    @Test
    public void testToAtomicLong() {
        Date date = new Date(9_876_543L);
        AtomicLong atomic = DateConversions.toAtomicLong(date, utcConverter);
        assertEquals(date.getTime(), atomic.get());
    }

    /*-------------------------------------------------------------
     *  sqlDateToString
     *------------------------------------------------------------*/
    @Test
    public void testSqlDateToString() {
        java.sql.Date sqlDate = new java.sql.Date(1_600_000_000_000L);
        String result = DateConversions.sqlDateToString(sqlDate, utcConverter);
        // Expected format: yyyy‑MM‑dd'T'HH:mm:ss.SSS+HH:MM
        ZonedDateTime zdt = Instant.ofEpochMilli(sqlDate.getTime()).atZone(UTC);
        DateTimeFormatter fmt = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
                .appendOffset("+HH:MM", "Z")
                .toFormatter();
        assertEquals(zdt.format(fmt), result);
    }

    /*-------------------------------------------------------------
     *  toString
     *------------------------------------------------------------*/
    @Test
    public void testToString_Utc() {
        Date date = new Date(1_600_000_000_000L);
        String text = DateConversions.toString(date, utcConverter);
        ZonedDateTime zdt = Instant.ofEpochMilli(date.getTime()).atZone(UTC);
        DateTimeFormatter fmt = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
                .appendOffset("+HH:MM", "Z")
                .toFormatter();
        assertEquals(zdt.format(fmt), text);
    }

    @Test
    public void testToString_Pst() {
        Date date = new Date(1_600_000_000_000L);
        String text = DateConversions.toString(date, pstConverter);
        ZonedDateTime zdt = Instant.ofEpochMilli(date.getTime()).atZone(PST);
        DateTimeFormatter fmt = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
                .appendOffset("+HH:MM", "Z")
                .toFormatter();
        assertEquals(zdt.format(fmt), text);
    }

    @Test
    public void testToString_Null() {
        thrown.expect(NullPointerException.class);
        DateConversions.toString(null, utcConverter);
    }

    /*-------------------------------------------------------------
     *  toMap
     *------------------------------------------------------------*/
    @Test
    public void testToMap() {
        Date date = new Date(1_600_000_123_456L);
        Map<String, Object> map = DateConversions.toMap(date, utcConverter);

        ZonedDateTime zdt = DateConversions.toZonedDateTime(date, utcConverter);

        assertEquals(zdt.toLocalDate().toString(), map.get(MapConversions.DATE));
        assertEquals(zdt.toLocalTime().toString(), map.get(MapConversions.TIME));
        assertEquals(UTC.toString(), map.get(MapConversions.ZONE));
        assertEquals(date.getTime(), map.get(MapConversions.EPOCH_MILLIS));
    }

    @Test
    public void testToMap_Null() {
        thrown.expect(NullPointerException.class);
        DateConversions.toMap(null, utcConverter);
    }

    /*-------------------------------------------------------------
     *  General error handling – verify that a ClassCastException is
     *  thrown when the supplied object is not a java.util.Date.
     *------------------------------------------------------------*/
    @Test
    public void testAllMethods_ClassCastOnWrongType() {
        Object notADate = "2020-01-01";
        // List of methods that perform an unchecked cast to Date.
        // We verify that each throws ClassCastException when given a wrong type.
        try {
            DateConversions.toZonedDateTime(notADate, utcConverter);
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) {}

        try {
            DateConversions.toLong(notADate, utcConverter);
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) {}

        try {
            DateConversions.toDouble(notADate, utcConverter);
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) {}

        try {
            DateConversions.toSqlDate(notADate, utcConverter);
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) {}

        try {
            DateConversions.toDate(notADate, utcConverter);
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) {}

        try {
            DateConversions.toTimestamp(notADate, utcConverter);
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) {}

        try {
            DateConversions.toCalendar(notADate, utcConverter);
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) {}

        try {
            DateConversions.toBigDecimal(notADate, utcConverter);
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) {}

        try {
            DateConversions.toInstant(notADate, utcConverter);
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) {}

        try {
            DateConversions.toOffsetDateTime(notADate, utcConverter);
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) {}

        try {
            DateConversions.toLocalDateTime(notADate, utcConverter);
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) {}

        try {
            DateConversions.toLocalDate(notADate, utcConverter);
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) {}

        try {
            DateConversions.toLocalTime(notADate, utcConverter);
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) {}

        try {
            DateConversions.toBigInteger(notADate, utcConverter);
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) {}

        try {
            DateConversions.toAtomicLong(notADate, utcConverter);
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) {}

        try {
            DateConversions.sqlDateToString(notADate, utcConverter);
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) {}

        try {
            DateConversions.toString(notADate, utcConverter);
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) {}

        try {
            DateConversions.toMap(notADate, utcConverter);
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) {}
    }
}
