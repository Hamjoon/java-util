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
 * Comprehensive JUnit‑4 test suite for {@link LocalDateTimeConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>basic happy‑path verification for every conversion method</li>
 *   <li>edge‑case handling – {@code null} arguments and wrong source types</li>
 *   <li>zone‑offset calculations (including a DST‑relevant zone)</li>
 *   <li>numeric conversions that delegate to {@link InstantConversions}</li>
 *   <li>string and map representations</li>
 * </ul>
 *
 * The test class lives in the same package as {@code LocalDateTimeConversions}
 * so it can invoke the package‑private static methods directly.
 */
public class LocalDateTimeConversionsTest {

    /** A converter that always uses UTC – convenient for deterministic tests. */
    private Converter utcConverter;

    /** A converter that uses the America/New_York zone (DST changes) – used for offset tests. */
    private Converter nyConverter;

    @Before
    public void setUp() {
        // ------------------------------------------------------------
        // Build a Converter that works with UTC.
        // ------------------------------------------------------------
        ConverterOptions utcOptions = new ConverterOptions();
        utcOptions.setZoneId(ZoneId.of("UTC"));
        utcOptions.setTimeZone(TimeZone.getTimeZone("UTC"));
        utcConverter = new Converter(utcOptions);

        // ------------------------------------------------------------
        // Build a Converter that works with America/New_York.
        // ------------------------------------------------------------
        ConverterOptions nyOptions = new ConverterOptions();
        nyOptions.setZoneId(ZoneId.of("America/New_York"));
        nyOptions.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        nyConverter = new Converter(nyOptions);
    }

    // -----------------------------------------------------------------
    // 1.  Basic “happy‑path” tests – one for each conversion method.
    // -----------------------------------------------------------------

    @Test
    public void testToZonedDateTime() {
        LocalDateTime ldt = LocalDateTime.of(2022, 5, 15, 10, 30, 45);
        ZonedDateTime zdt = LocalDateTimeConversions.toZonedDateTime(ldt, utcConverter);
        assertEquals(ldt, zdt.toLocalDateTime());
        assertEquals(ZoneId.of("UTC"), zdt.getZone());
    }

    @Test
    public void testToOffsetDateTime_Utc() {
        LocalDateTime ldt = LocalDateTime.of(2022, 5, 15, 12, 0);
        OffsetDateTime odt = LocalDateTimeConversions.toOffsetDateTime(ldt, utcConverter);
        assertEquals(ldt, odt.toLocalDateTime());
        assertEquals(ZoneOffset.UTC, odt.getOffset());
    }

    @Test
    public void testToOffsetDateTime_NewYork_DuringStandardTime() {
        // 2022‑01‑15 is standard‑time (offset –05:00)
        LocalDateTime ldt = LocalDateTime.of(2022, 1, 15, 8, 0);
        OffsetDateTime odt = LocalDateTimeConversions.toOffsetDateTime(ldt, nyConverter);
        assertEquals(ldt, odt.toLocalDateTime());
        assertEquals(ZoneOffset.ofHours(-5), odt.getOffset());
    }

    @Test
    public void testToOffsetDateTime_NewYork_DuringDaylightSaving() {
        // 2022‑07‑15 is daylight‑saving time (offset –04:00)
        LocalDateTime ldt = LocalDateTime.of(2022, 7, 15, 8, 0);
        OffsetDateTime odt = LocalDateTimeConversions.toOffsetDateTime(ldt, nyConverter);
        assertEquals(ldt, odt.toLocalDateTime());
        assertEquals(ZoneOffset.ofHours(-4), odt.getOffset());
    }

    @Test
    public void testToInstant() {
        LocalDateTime ldt = LocalDateTime.of(1970, 1, 1, 0, 0, 1);
        Instant instant = LocalDateTimeConversions.toInstant(ldt, utcConverter);
        assertEquals(Instant.ofEpochSecond(1), instant);
    }

    @Test
    public void testToLong() {
        LocalDateTime ldt = LocalDateTime.of(2022, 3, 1, 0, 0);
        long epochMilli = LocalDateTimeConversions.toLong(ldt, utcConverter);
        assertEquals(ldt.atZone(ZoneId.of("UTC")).toInstant().toEpochMilli(), epochMilli);
    }

    @Test
    public void testToDouble() {
        LocalDateTime ldt = LocalDateTime.of(2022, 1, 1, 0, 0);
        double d = LocalDateTimeConversions.toDouble(ldt, utcConverter);
        // InstantConversions.toDouble returns the epoch‑millis as double.
        double expected = ldt.atZone(ZoneId.of("UTC")).toInstant().toEpochMilli();
        assertEquals(expected, d, 0.0);
    }

    @Test
    public void testToLocalDateTime() {
        LocalDateTime ldt = LocalDateTime.of(1999, 12, 31, 23, 59, 59);
        LocalDateTime result = LocalDateTimeConversions.toLocalDateTime(ldt, utcConverter);
        assertEquals(ldt, result);
    }

    @Test
    public void testToLocalDate() {
        LocalDateTime ldt = LocalDateTime.of(2020, 2, 29, 12, 34);
        LocalDate date = LocalDateTimeConversions.toLocalDate(ldt, utcConverter);
        assertEquals(LocalDate.of(2020, 2, 29), date);
    }

    @Test
    public void testToLocalTime() {
        LocalDateTime ldt = LocalDateTime.of(2020, 2, 29, 12, 34, 56);
        LocalTime time = LocalDateTimeConversions.toLocalTime(ldt, utcConverter);
        assertEquals(LocalTime.of(12, 34, 56), time);
    }

    @Test
    public void testToAtomicLong() {
        LocalDateTime ldt = LocalDateTime.of(2000, 1, 1, 0, 0);
        AtomicLong atomic = LocalDateTimeConversions.toAtomicLong(ldt, utcConverter);
        long expected = ldt.atZone(ZoneId.of("UTC")).toInstant().toEpochMilli();
        assertEquals(expected, atomic.get());
    }

    @Test
    public void testToTimestamp() {
        LocalDateTime ldt = LocalDateTime.of(2021, 6, 30, 15, 45, 30, 123_000_000);
        Timestamp ts = LocalDateTimeConversions.toTimestamp(ldt, utcConverter);
        Instant expected = ldt.atZone(ZoneId.of("UTC")).toInstant();
        assertEquals(Timestamp.from(expected), ts);
    }

    @Test
    public void testToCalendar() {
        LocalDateTime ldt = LocalDateTime.of(2021, 12, 31, 23, 59, 59);
        Calendar cal = LocalDateTimeConversions.toCalendar(ldt, utcConverter);
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
        long expected = ldt.atZone(ZoneId.of("UTC")).toInstant().toEpochMilli();
        assertEquals(expected, cal.getTimeInMillis());
    }

    @Test
    public void testToSqlDate() {
        LocalDateTime ldt = LocalDateTime.of(1970, 1, 1, 0, 0, 5);
        java.sql.Date sqlDate = LocalDateTimeConversions.toSqlDate(ldt, utcConverter);
        assertEquals(new java.sql.Date(5_000L), sqlDate);
    }

    @Test
    public void testToDate() {
        LocalDateTime ldt = LocalDateTime.of(1970, 1, 1, 0, 0, 10);
        Date date = LocalDateTimeConversions.toDate(ldt, utcConverter);
        assertEquals(new Date(10_000L), date);
    }

    @Test
    public void testToBigInteger() {
        LocalDateTime ldt = LocalDateTime.of(1970, 1, 1, 0, 0, 1);
        BigInteger bi = LocalDateTimeConversions.toBigInteger(ldt, utcConverter);
        BigInteger expected = BigInteger.valueOf(1_000L);
        assertEquals(expected, bi);
    }

    @Test
    public void testToBigDecimal() {
        LocalDateTime ldt = LocalDateTime.of(1970, 1, 1, 0, 0, 2);
        BigDecimal bd = LocalDateTimeConversions.toBigDecimal(ldt, utcConverter);
        BigDecimal expected = BigDecimal.valueOf(2_000L);
        assertEquals(expected, bd);
    }

    @Test
    public void testToString() {
        LocalDateTime ldt = LocalDateTime.of(2022, 8, 15, 12, 30, 45);
        String s = LocalDateTimeConversions.toString(ldt, utcConverter);
        assertEquals(ldt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), s);
    }

    @Test
    public void testToMap() {
        LocalDateTime ldt = LocalDateTime.of(2022, 8, 15, 12, 30, 45);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocalDateTimeConversions.toMap(ldt, utcConverter);
        assertEquals(2, map.size());
        assertEquals(ldt.toLocalDate().toString(), map.get(MapConversions.DATE));
        assertEquals(ldt.toLocalTime().toString(), map.get(MapConversions.TIME));
    }

    // -----------------------------------------------------------------
    // 2.  Edge‑case / error handling tests.
    // -----------------------------------------------------------------

    /** Helper that asserts a {@code NullPointerException} for a given conversion lambda. */
    private interface Conversion {
        void invoke(Object from, Converter c);
    }

    private void assertNPE(Conversion conv) {
        try {
            conv.invoke(null, utcConverter);
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
            // success
        }
    }

    private void assertCCE(Conversion conv) {
        try {
            conv.invoke("not a LocalDateTime", utcConverter);
            fail("Expected ClassCastException");
        } catch (ClassCastException expected) {
            // success
        }
    }

    @Test
    public void testNullInputThrowsNPE() {
        assertNPE(LocalDateTimeConversions::toZonedDateTime);
        assertNPE(LocalDateTimeConversions::toOffsetDateTime);
        assertNPE(LocalDateTimeConversions::toInstant);
        assertNPE(LocalDateTimeConversions::toLong);
        assertNPE(LocalDateTimeConversions::toDouble);
        assertNPE(LocalDateTimeConversions::toLocalDateTime);
        assertNPE(LocalDateTimeConversions::toLocalDate);
        assertNPE(LocalDateTimeConversions::toLocalTime);
        assertNPE(LocalDateTimeConversions::toAtomicLong);
        assertNPE(LocalDateTimeConversions::toTimestamp);
        assertNPE(LocalDateTimeConversions::toCalendar);
        assertNPE(LocalDateTimeConversions::toSqlDate);
        assertNPE(LocalDateTimeConversions::toDate);
        assertNPE(LocalDateTimeConversions::toBigInteger);
        assertNPE(LocalDateTimeConversions::toBigDecimal);
        assertNPE(LocalDateTimeConversions::toString);
        assertNPE(LocalDateTimeConversions::toMap);
    }

    @Test
    public void testInvalidSourceTypeThrowsClassCastException() {
        assertCCE(LocalDateTimeConversions::toZonedDateTime);
        assertCCE(LocalDateTimeConversions::toOffsetDateTime);
        assertCCE(LocalDateTimeConversions::toInstant);
        assertCCE(LocalDateTimeConversions::toLong);
        assertCCE(LocalDateTimeConversions::toDouble);
        assertCCE(LocalDateTimeConversions::toLocalDateTime);
        assertCCE(LocalDateTimeConversions::toLocalDate);
        assertCCE(LocalDateTimeConversions::toLocalTime);
        assertCCE(LocalDateTimeConversions::toAtomicLong);
        assertCCE(LocalDateTimeConversions::toTimestamp);
        assertCCE(LocalDateTimeConversions::toCalendar);
        assertCCE(LocalDateTimeConversions::toSqlDate);
        assertCCE(LocalDateTimeConversions::toDate);
        assertCCE(LocalDateTimeConversions::toBigInteger);
        assertCCE(LocalDateTimeConversions::toBigDecimal);
        assertCCE(LocalDateTimeConversions::toString);
        assertCCE(LocalDateTimeConversions::toMap);
    }

    @Test
    public void testToOffsetDateTime_WithZoneHavingHalfHourOffset() {
        // Zone offset +05:30 (e.g., Asia/Kolkata)
        ConverterOptions opt = new ConverterOptions();
        opt.setZoneId(ZoneId.of("Asia/Kolkata"));
        opt.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
        Converter kolkataConverter = new Converter(opt);

        LocalDateTime ldt = LocalDateTime.of(2022, 3, 10, 12, 0);
        OffsetDateTime odt = LocalDateTimeConversions.toOffsetDateTime(ldt, kolkataConverter);
        assertEquals(ZoneOffset.ofHoursMinutes(5, 30), odt.getOffset());
    }

    @Test
    public void testToTimestamp_NanosecondPrecisionPreserved() {
        LocalDateTime ldt = LocalDateTime.of(2022, 1, 1, 0, 0, 0, 987_654_321);
        Timestamp ts = LocalDateTimeConversions.toTimestamp(ldt, utcConverter);
        // Timestamp stores nanos; they should match the original value.
        assertEquals(987_654_321, ts.getNanos());
    }

    @Test
    public void testToCalendar_DaylightSavingTransition() {
        // 2022‑03‑13 02:30 does not exist in America/New_York (gap)
        // Java resolves the gap by moving forward to 03:30 with offset -04:00.
        LocalDateTime ldt = LocalDateTime.of(2022, 3, 13, 2, 30);
        Calendar cal = LocalDateTimeConversions.toCalendar(ldt, nyConverter);
        ZonedDateTime zdt = ldt.atZone(ZoneId.of("America/New_York"));
        assertEquals(zdt.toInstant().toEpochMilli(), cal.getTimeInMillis());
    }

    // -----------------------------------------------------------------
    // 3.  Verification that map keys are the expected constants.
    // -----------------------------------------------------------------

    @Test
    public void testMapContainsExpectedKeys() {
        LocalDateTime ldt = LocalDateTime.of(2023, 1, 1, 0, 0);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocalDateTimeConversions.toMap(ldt, utcConverter);
        assertTrue(map.containsKey(MapConversions.DATE));
        assertTrue(map.containsKey(MapConversions.TIME));
        assertEquals(ldt.toLocalDate().toString(), map.get(MapConversions.DATE));
        assertEquals(ldt.toLocalTime().toString(), map.get(MapConversions.TIME));
    }
}
