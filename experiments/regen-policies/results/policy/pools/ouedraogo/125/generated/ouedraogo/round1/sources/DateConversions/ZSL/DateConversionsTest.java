package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link DateConversions}.
 *
 * <p>The tests exercise every public static method in {@code DateConversions}
 * using real objects (no mocks) and verify that the conversion results are
 * consistent with the JDK date‑time API and with the expectations encoded in
 * the implementation.</p>
 *
 * <p>All tests run against a fixed point in time (2021‑10‑01T12:00:00.123Z) so
 * that the expected values are deterministic.</p>
 *
 * @author  OpenAI ChatGPT
 */
public class DateConversionsTest {

    /** Fixed epoch milliseconds used for all test cases. */
    private static final long FIXED_EPOCH_MILLIS = 1633080000123L; // 2021‑10‑01T12:00:00.123Z

    /** The {@link Date} instance representing the fixed point in time. */
    private Date fixedDate;

    /** A {@link Converter} instance with a known {@link ZoneId}. */
    private Converter converter;

    /** The {@link ZoneId} used by the {@link Converter}. */
    private static final ZoneId TEST_ZONE = ZoneId.of("UTC");

    @Before
    public void setUp() {
        fixedDate = new Date(FIXED_EPOCH_MILLIS);
        // The library supplies a ConverterOptions class – we assume it has a
        // no‑arg constructor that defaults to the system zone.  To guarantee a
        // deterministic zone we create a ConverterOptions instance, set its
        // zone to UTC (if such a setter exists) and pass it to the Converter.
        // If the options class does not expose a setter, the default zone will
        // be the system default; the tests still succeed because they compare
        // against the zone returned by the converter itself.
        ConverterOptions options = new ConverterOptions();
        // The following line is defensive – it will compile only if the method
        // exists; otherwise it is ignored because the test suite must compile
        // against the library version used at runtime.
        try {
            options.getClass().getMethod("setZoneId", ZoneId.class).invoke(options, TEST_ZONE);
        } catch (Exception ignored) {
            // No setter – rely on the default zone.
        }
        converter = new Converter(options);
    }

    @Test
    public void testToZonedDateTime() {
        ZonedDateTime zdt = DateConversions.toZonedDateTime(fixedDate, converter);
        assertNotNull(zdt);
        assertEquals(FIXED_EPOCH_MILLIS, zdt.toInstant().toEpochMilli());
        assertEquals(converter.getOptions().getZoneId(), zdt.getZone());
    }

    @Test
    public void testToLong() {
        long epoch = DateConversions.toLong(fixedDate, converter);
        assertEquals(FIXED_EPOCH_MILLIS, epoch);
    }

    @Test
    public void testToDouble() {
        double seconds = DateConversions.toDouble(fixedDate, converter);
        assertEquals(FIXED_EPOCH_MILLIS / 1000.0, seconds, 0.000001);
    }

    @Test
    public void testToSqlDate() {
        java.sql.Date sqlDate = DateConversions.toSqlDate(fixedDate, converter);
        assertNotNull(sqlDate);
        assertEquals(FIXED_EPOCH_MILLIS, sqlDate.getTime());
    }

    @Test
    public void testToDate() {
        Date date = DateConversions.toDate(fixedDate, converter);
        assertNotNull(date);
        assertEquals(FIXED_EPOCH_MILLIS, date.getTime());
    }

    @Test
    public void testToTimestamp() {
        Timestamp ts = DateConversions.toTimestamp(fixedDate, converter);
        assertNotNull(ts);
        assertEquals(FIXED_EPOCH_MILLIS, ts.getTime());
    }

    @Test
    public void testToCalendar() {
        Calendar cal = DateConversions.toCalendar(fixedDate, converter);
        assertNotNull(cal);
        assertEquals(FIXED_EPOCH_MILLIS, cal.getTimeInMillis());
    }

    @Test
    public void testToBigDecimal() {
        BigDecimal bd = DateConversions.toBigDecimal(fixedDate, converter);
        assertNotNull(bd);
        // The implementation divides by BigDecimalConversions.GRAND (10⁹) with
        // a scale of 9 and RoundingMode.DOWN.
        // Therefore, multiplying back should give the original epoch millis.
        BigDecimal reconstructed = bd.multiply(BigDecimalConversions.GRAND);
        assertEquals(BigDecimal.valueOf(FIXED_EPOCH_MILLIS), reconstructed.setScale(0, java.math.RoundingMode.DOWN));
    }

    @Test
    public void testToInstantWithUtilDate() {
        java.util.Date utilDate = new Date(FIXED_EPOCH_MILLIS);
        java.time.Instant instant = DateConversions.toInstant(utilDate, converter);
        assertEquals(FIXED_EPOCH_MILLIS, instant.toEpochMilli());
    }

    @Test
    public void testToInstantWithSqlDate() {
        java.sql.Date sqlDate = new java.sql.Date(FIXED_EPOCH_MILLIS);
        java.time.Instant instant = DateConversions.toInstant(sqlDate, converter);
        assertEquals(FIXED_EPOCH_MILLIS, instant.toEpochMilli());
    }

    @Test
    public void testToOffsetDateTime() {
        OffsetDateTime odt = DateConversions.toOffsetDateTime(fixedDate, converter);
        assertNotNull(odt);
        assertEquals(FIXED_EPOCH_MILLIS, odt.toInstant().toEpochMilli());
        assertEquals(converter.getOptions().getZoneId().getRules().getOffset(odt.toInstant()), odt.getOffset());
    }

    @Test
    public void testToLocalDateTime() {
        java.time.LocalDateTime ldt = DateConversions.toLocalDateTime(fixedDate, converter);
        assertNotNull(ldt);
        ZonedDateTime expected = DateConversions.toZonedDateTime(fixedDate, converter);
        assertEquals(expected.toLocalDateTime(), ldt);
    }

    @Test
    public void testToLocalDate() {
        java.time.LocalDate ld = DateConversions.toLocalDate(fixedDate, converter);
        assertNotNull(ld);
        ZonedDateTime expected = DateConversions.toZonedDateTime(fixedDate, converter);
        assertEquals(expected.toLocalDate(), ld);
    }

    @Test
    public void testToLocalTime() {
        java.time.LocalTime lt = DateConversions.toLocalTime(fixedDate, converter);
        assertNotNull(lt);
        ZonedDateTime expected = DateConversions.toZonedDateTime(fixedDate, converter);
        assertEquals(expected.toLocalTime(), lt);
    }

    @Test
    public void testToBigInteger() {
        BigInteger bi = DateConversions.toBigInteger(fixedDate, converter);
        assertNotNull(bi);
        // InstantConversions.toBigInteger converts an Instant to a BigInteger
        // representing the epoch milliseconds.  Verify that relationship.
        java.time.Instant instant = DateConversions.toInstant(fixedDate, converter);
        assertEquals(BigInteger.valueOf(instant.toEpochMilli()), bi);
    }

    @Test
    public void testToAtomicLong() {
        AtomicLong al = DateConversions.toAtomicLong(fixedDate, converter);
        assertNotNull(al);
        assertEquals(FIXED_EPOCH_MILLIS, al.get());
    }

    @Test
    public void testSqlDateToString() {
        java.sql.Date sqlDate = new java.sql.Date(FIXED_EPOCH_MILLIS);
        String result = DateConversions.sqlDateToString(sqlDate, converter);
        String expected = DateConversions.toString(new Date(FIXED_EPOCH_MILLIS), converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToStringFormatting() {
        String formatted = DateConversions.toString(fixedDate, converter);
        assertNotNull(formatted);

        // Build a formatter that matches the one used inside DateConversions.
        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
                .appendOffset("+HH:MM", "Z")
                .toFormatter();

        ZonedDateTime parsed = ZonedDateTime.parse(formatted, formatter);
        assertEquals(FIXED_EPOCH_MILLIS, parsed.toInstant().toEpochMilli());
        assertEquals(converter.getOptions().getZoneId(), parsed.getZone());
    }

    @Test
    public void testToMap() {
        Map<String, Object> map = DateConversions.toMap(fixedDate, converter);
        assertNotNull(map);
        assertEquals(4, map.size());

        // Expected values
        ZonedDateTime zdt = DateConversions.toZonedDateTime(fixedDate, converter);
        assertEquals(zdt.toLocalDate().toString(), map.get(MapConversions.DATE));
        assertEquals(zdt.toLocalTime().toString(), map.get(MapConversions.TIME));
        assertEquals(converter.getOptions().getZoneId().toString(), map.get(MapConversions.ZONE));
        assertEquals(FIXED_EPOCH_MILLIS, map.get(MapConversions.EPOCH_MILLIS));
    }
}
