package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link DateConversions}.
 *
 * The tests cover:
 *   • Normal usage of every public static method.
 *   • Edge‑case values (epoch 0, far‑future/negative timestamps, null arguments).
 *   • Behaviour that depends on the {@link ConverterOptions#getZoneId()} setting.
 *   • Correct handling of {@link java.sql.Date} vs {@link java.util.Date}.
 *
 * All objects are created via real constructors – no mocking or subclassing of library
 * classes is performed, as required by the task description.
 */
public class DateConversionsTest {

    private Converter utcConverter;
    private Converter nyConverter;

    @Before
    public void setUp() {
        // ConverterOptions is part of the library; we assume it has a public no‑arg ctor
        // and a setter for the ZoneId (or a constructor that accepts a ZoneId).
        // The exact API is not shown, therefore we use reflection to set the zone if needed.
        ConverterOptions utcOptions = new ConverterOptions();
        setZoneId(utcOptions, ZoneId.of("UTC"));
        utcConverter = new Converter(utcOptions);

        ConverterOptions nyOptions = new ConverterOptions();
        setZoneId(nyOptions, ZoneId.of("America/New_York"));
        nyConverter = new Converter(nyOptions);
    }

    /**
     * Helper – uses reflection to set the zone on a ConverterOptions instance.
     * This works for any library version that stores the ZoneId in a field named
     * “zoneId” or provides a setter called “setZoneId”.
     */
    private void setZoneId(ConverterOptions options, ZoneId zoneId) {
        try {
            // Try a conventional setter first
            options.getClass().getMethod("setZoneId", ZoneId.class).invoke(options, zoneId);
        } catch (Exception ignored) {
            // Fallback to direct field access
            try {
                java.lang.reflect.Field f = options.getClass().getDeclaredField("zoneId");
                f.setAccessible(true);
                f.set(options, zoneId);
            } catch (Exception e) {
                throw new RuntimeException("Unable to set ZoneId on ConverterOptions", e);
            }
        }
    }

    /* -------------------------------------------------------------
     *  Basic (happy‑path) tests
     * ------------------------------------------------------------- */

    @Test
    public void testToZonedDateTime_Utc() {
        Date date = new Date(1_600_000_000_000L); // 2020‑09‑13T12:26:40Z
        ZonedDateTime zdt = DateConversions.toZonedDateTime(date, utcConverter);
        assertEquals(ZoneId.of("UTC"), zdt.getZone());
        assertEquals(Instant.ofEpochMilli(date.getTime()), zdt.toInstant());
    }

    @Test
    public void testToZonedDateTime_NewYork() {
        Date date = new Date(1_600_000_000_000L);
        ZonedDateTime zdt = DateConversions.toZonedDateTime(date, nyConverter);
        assertEquals(ZoneId.of("America/New_York"), zdt.getZone());
        assertEquals(Instant.ofEpochMilli(date.getTime()), zdt.toInstant());
    }

    @Test
    public void testToLong() {
        Date date = new Date(123_456_789L);
        assertEquals(123_456_789L, DateConversions.toLong(date, utcConverter));
    }

    @Test
    public void testToDouble() {
        Date date = new Date(1_500L); // 1.5 seconds
        assertEquals(1.5, DateConversions.toDouble(date, utcConverter), 0.000_001);
    }

    @Test
    public void testToSqlDate() {
        Date date = new Date(9_999L);
        java.sql.Date sqlDate = DateConversions.toSqlDate(date, utcConverter);
        assertEquals(date.getTime(), sqlDate.getTime());
    }

    @Test
    public void testToDate() {
        Date src = new Date(42L);
        Date dst = DateConversions.toDate(src, utcConverter);
        assertEquals(src, dst);
    }

    @Test
    public void testToTimestamp() {
        Date date = new Date(7_777L);
        Timestamp ts = DateConversions.toTimestamp(date, utcConverter);
        assertEquals(date.getTime(), ts.getTime());
    }

    @Test
    public void testToCalendar() {
        Date date = new Date(3_333L);
        Calendar cal = DateConversions.toCalendar(date, utcConverter);
        assertEquals(date.getTime(), cal.getTimeInMillis());
    }

    @Test
    public void testToBigDecimal() {
        Date date = new Date(1_234_567_890L);
        BigDecimal bd = DateConversions.toBigDecimal(date, utcConverter);
        // epochMillis / 1_000_000_000 (GRAND) with scale 9, rounding down
        BigDecimal expected = new BigDecimal(date.getTime())
                .divide(BigDecimalConversions.GRAND, 9, RoundingMode.DOWN);
        assertEquals(0, bd.compareTo(expected));
    }

    @Test
    public void testToInstant_UtilDate() {
        Date date = new Date(5_555L);
        Instant instant = DateConversions.toInstant(date, utcConverter);
        assertEquals(date.toInstant(), instant);
    }

    @Test
    public void testToInstant_SqlDate() {
        java.sql.Date sqlDate = new java.sql.Date(8_888L);
        Instant instant = DateConversions.toInstant(sqlDate, utcConverter);
        // java.sql.Date loses the time‑zone, but toInstant() on a util.Date wrapper should match
        assertEquals(new java.util.Date(sqlDate.getTime()).toInstant(), instant);
    }

    @Test
    public void testToOffsetDateTime() {
        Date date = new Date(9_000L);
        OffsetDateTime odt = DateConversions.toOffsetDateTime(date, utcConverter);
        assertEquals(ZoneOffset.UTC, odt.getOffset());
        assertEquals(date.toInstant(), odt.toInstant());
    }

    @Test
    public void testToLocalDateTime() {
        Date date = new Date(1_600_000_123_456L);
        LocalDateTime ldt = DateConversions.toLocalDateTime(date, utcConverter);
        assertEquals(LocalDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC")), ldt);
    }

    @Test
    public void testToLocalDate() {
        Date date = new Date(1_600_000_123_456L);
        LocalDate ld = DateConversions.toLocalDate(date, utcConverter);
        assertEquals(LocalDate.ofInstant(date.toInstant(), ZoneId.of("UTC")), ld);
    }

    @Test
    public void testToLocalTime() {
        Date date = new Date(1_600_000_123_456L);
        LocalTime lt = DateConversions.toLocalTime(date, utcConverter);
        assertEquals(LocalTime.ofInstant(date.toInstant(), ZoneId.of("UTC")), lt);
    }

    @Test
    public void testToBigInteger() {
        Date date = new Date(12_345L);
        BigInteger bi = DateConversions.toBigInteger(date, utcConverter);
        // InstantConversions.toBigInteger is expected to return the epoch‑millis as a BigInteger
        assertEquals(BigInteger.valueOf(date.getTime()), bi);
    }

    @Test
    public void testToAtomicLong() {
        Date date = new Date(77_777L);
        AtomicLong al = DateConversions.toAtomicLong(date, utcConverter);
        assertEquals(date.getTime(), al.get());
    }

    @Test
    public void testSqlDateToString() {
        java.sql.Date sqlDate = new java.sql.Date(1_600_000_123_456L);
        String s = DateConversions.sqlDateToString(sqlDate, utcConverter);
        // Expected format: yyyy-MM-dd'T'HH:mm:ss.SSS+00:00 (UTC)
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        String expected = ZonedDateTime.ofInstant(sqlDate.toInstant(), ZoneId.of("UTC"))
                .format(fmt);
        assertEquals(expected, s);
    }

    @Test
    public void testToString_Utc() {
        Date date = new Date(1_600_000_123_456L);
        String s = DateConversions.toString(date, utcConverter);
        DateTimeFormatter fmt = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
                .appendOffset("+HH:MM", "Z")
                .toFormatter();
        String expected = ZonedDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC"))
                .format(fmt);
        assertEquals(expected, s);
    }

    @Test
    public void testToMap() {
        Date date = new Date(1_600_000_123_456L);
        Map<String, Object> map = DateConversions.toMap(date, utcConverter);
        ZonedDateTime zdt = DateConversions.toZonedDateTime(date, utcConverter);
        assertEquals(zdt.toLocalDate().toString(), map.get(MapConversions.DATE));
        assertEquals(zdt.toLocalTime().toString(), map.get(MapConversions.TIME));
        assertEquals(utcConverter.getOptions().getZoneId().toString(), map.get(MapConversions.ZONE));
        assertEquals(date.getTime(), map.get(MapConversions.EPOCH_MILLIS));
    }

    /* -------------------------------------------------------------
     *  Edge‑case and exception tests
     * ------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToZonedDateTime_NullInput() {
        DateConversions.toZonedDateTime(null, utcConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_NullInput() {
        DateConversions.toLong(null, utcConverter);
    }

    @Test
    public void testToLong_EpochZero() {
        Date epoch = new Date(0L);
        assertEquals(0L, DateConversions.toLong(epoch, utcConverter));
    }

    @Test
    public void testToDouble_NegativeTimestamp() {
        Date date = new Date(-2_500L); // -2.5 seconds
        assertEquals(-2.5, DateConversions.toDouble(date, utcConverter), 0.000_001);
    }

    @Test
    public void testToBigDecimal_RoundingDown() {
        // epochMillis = 1_234_567_890 -> 1.234567890 after division by 1_000_000_000
        Date date = new Date(1_234_567_890L);
        BigDecimal bd = DateConversions.toBigDecimal(date, utcConverter);
        assertEquals("1.234567890", bd.toPlainString());
    }

    @Test
    public void testToString_ZoneOffsetHalfHour() {
        // Use a zone with a half‑hour offset e.g., Asia/Kathmandu (+05:45)
        ConverterOptions opt = new ConverterOptions();
        setZoneId(opt, ZoneId.of("Asia/Kathmandu"));
        Converter kathmanduConverter = new Converter(opt);

        Date date = new Date(1_600_000_123_456L);
        String s = DateConversions.toString(date, kathmanduConverter);
        DateTimeFormatter fmt = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
                .appendOffset("+HH:MM", "Z")
                .toFormatter();
        String expected = ZonedDateTime.ofInstant(date.toInstant(), ZoneId.of("Asia/Kathmandu"))
                .format(fmt);
        assertEquals(expected, s);
    }

    @Test(expected = NullPointerException.class)
    public void testSqlDateToString_NullInput() {
        DateConversions.sqlDateToString(null, utcConverter);
    }

    @Test
    public void testToMap_NullInput() {
        try {
            DateConversions.toMap(null, utcConverter);
            fail("Expected NullPointerException");
        } catch (NullPointerException ignored) {
        }
    }

    @Test
    public void testToInstant_NullInput() {
        try {
            DateConversions.toInstant(null, utcConverter);
            fail("Expected NullPointerException");
        } catch (NullPointerException ignored) {
        }
    }

    @Test
    public void testToLocalTime_NullInput() {
        try {
            DateConversions.toLocalTime(null, utcConverter);
            fail("Expected NullPointerException");
        } catch (NullPointerException ignored) {
        }
    }

    @Test
    public void testToBigInteger_NullInput() {
        try {
            DateConversions.toBigInteger(null, utcConverter);
            fail("Expected NullPointerException");
        } catch (NullPointerException ignored) {
        }
    }

    @Test
    public void testToAtomicLong_NullInput() {
        try {
            DateConversions.toAtomicLong(null, utcConverter);
            fail("Expected NullPointerException");
        } catch (NullPointerException ignored) {
        }
    }

    @Test
    public void testToCalendar_NullInput() {
        try {
            DateConversions.toCalendar(null, utcConverter);
            fail("Expected NullPointerException");
        } catch (NullPointerException ignored) {
        }
    }

    @Test
    public void testToOffsetDateTime_NullInput() {
        try {
            DateConversions.toOffsetDateTime(null, utcConverter);
            fail("Expected NullPointerException");
        } catch (NullPointerException ignored) {
        }
    }

    @Test
    public void testToLocalDateTime_NullInput() {
        try {
            DateConversions.toLocalDateTime(null, utcConverter);
            fail("Expected NullPointerException");
        } catch (NullPointerException ignored) {
        }
    }

    @Test
    public void testToLocalDate_NullInput() {
        try {
            DateConversions.toLocalDate(null, utcConverter);
            fail("Expected NullPointerException");
        } catch (NullPointerException ignored) {
        }
    }

    @Test
    public void testToString_NullInput() {
        try {
            DateConversions.toString(null, utcConverter);
            fail("Expected NullPointerException");
        } catch (NullPointerException ignored) {
        }
    }

    /* -------------------------------------------------------------
     *  Very large timestamps (close to Long.MAX_VALUE)
     * ------------------------------------------------------------- */

    @Test
    public void testVeryLargeTimestamp() {
        long nearMax = Long.MAX_VALUE - 1_000_000L; // still representable as Date
        Date date = new Date(nearMax);
        // Ensure no overflow in any conversion
        assertEquals(nearMax, DateConversions.toLong(date, utcConverter));
        assertEquals(nearMax / 1000.0, DateConversions.toDouble(date, utcConverter), 0.0);
        assertEquals(date.toInstant(), DateConversions.toInstant(date, utcConverter));
        assertEquals(nearMax, DateConversions.toAtomicLong(date, utcConverter).get());
        assertEquals(nearMax, DateConversions.toTimestamp(date, utcConverter).getTime());
        assertEquals(nearMax, DateConversions.toSqlDate(date, utcConverter).getTime());
        assertEquals(nearMax, DateConversions.toCalendar(date, utcConverter).getTimeInMillis());
    }
}
