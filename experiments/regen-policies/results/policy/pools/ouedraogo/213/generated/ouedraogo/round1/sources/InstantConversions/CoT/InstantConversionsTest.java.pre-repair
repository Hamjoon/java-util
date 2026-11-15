package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class InstantConversionsTest {

    private Instant instant;
    private Converter dummyConverter; // will be null for most tests

    @Before
    public void setUp() {
        // A fixed instant for deterministic tests
        instant = Instant.parse("2023-03-25T12:34:56.123456789Z");
        dummyConverter = null; // many methods only need the object, not the converter
    }

    /* ----------------------------------------------------------------------
     * List of public (package‑private static) methods in InstantConversions
     * ----------------------------------------------------------------------
     * Map toMap(Object from, Converter converter)
     * ZonedDateTime toZonedDateTime(Object from, Converter converter)
     * OffsetDateTime toOffsetDateTime(Object from, Converter converter)
     * long toLong(Object from, Converter converter)
     * double toDouble(Object from, Converter converter)
     * AtomicLong toAtomicLong(Object from, Converter converter)
     * Timestamp toTimestamp(Object from, Converter converter)
     * java.sql.Date toSqlDate(Object from, Converter converter)
     * Date toDate(Object from, Converter converter)
     * Calendar toCalendar(Object from, Converter converter)
     * BigInteger toBigInteger(Object from, Converter converter)
     * BigDecimal toBigDecimal(Object from, Converter converter)
     * LocalDateTime toLocalDateTime(Object from, Converter converter)
     * LocalDate toLocalDate(Object from, Converter converter)
     * LocalTime toLocalTime(Object from, Converter converter)
     * ---------------------------------------------------------------------- */

    /* --------------------------- Basic functionality tests --------------------------- */

    @Test
    public void testToMap() {
        Map result = InstantConversions.toMap(instant, dummyConverter);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(instant.toString(), result.get(MapConversions.INSTANT));
    }

    @Test
    public void testToLong() {
        long epochMilli = InstantConversions.toLong(instant, dummyConverter);
        assertEquals(instant.toEpochMilli(), epochMilli);
    }

    @Test
    public void testToDouble() {
        double seconds = InstantConversions.toDouble(instant, dummyConverter);
        double expected = instant.getEpochSecond() + instant.getNano() / 1_000_000_000d;
        assertEquals(expected, seconds, 1e-12);
    }

    @Test
    public void testToAtomicLong() {
        AtomicLong atomic = InstantConversions.toAtomicLong(instant, dummyConverter);
        assertEquals(instant.toEpochMilli(), atomic.get());
    }

    @Test
    public void testToTimestamp() {
        Timestamp ts = InstantConversions.toTimestamp(instant, dummyConverter);
        assertEquals(Timestamp.from(instant), ts);
    }

    @Test
    public void testToDate() {
        Date date = InstantConversions.toDate(instant, dummyConverter);
        assertEquals(new Date(instant.toEpochMilli()), date);
    }

    @Test
    public void testToBigInteger() {
        BigInteger bigInt = InstantConversions.toBigInteger(instant, dummyConverter);
        BigInteger expected = BigInteger.valueOf(instant.getEpochSecond())
                .multiply(BigInteger.valueOf(1_000_000_000L))
                .add(BigInteger.valueOf(instant.getNano()));
        assertEquals(expected, bigInt);
    }

    @Test
    public void testToBigDecimal() {
        BigDecimal bd = InstantConversions.toBigDecimal(instant, dummyConverter);
        BigDecimal expected = BigDecimal.valueOf(instant.getEpochSecond())
                .add(BigDecimal.valueOf(instant.getNano(), 9)); // nano as fraction of a second
        assertEquals(0, expected.compareTo(bd));
    }

    /* --------------------------- Edge case & exception tests --------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToZonedDateTime_NullConverter() {
        // Converter is required for zone information; passing null must NPE
        InstantConversions.toZonedDateTime(instant, null);
    }

    @Test(expected = NullPointerException.class)
    public void testToOffsetDateTime_NullConverter() {
        InstantConversions.toOffsetDateTime(instant, null);
    }

    @Test(expected = NullPointerException.class)
    public void testToSqlDate_NullConverter() {
        InstantConversions.toSqlDate(instant, null);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDateTime_NullConverter() {
        InstantConversions.toLocalDateTime(instant, null);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDate_NullConverter() {
        InstantConversions.toLocalDate(instant, null);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalTime_NullConverter() {
        InstantConversions.toLocalTime(instant, null);
    }

    @Test
    public void testToDouble_EpochSecondZero_NanoPrecision() {
        Instant zeroSec = Instant.ofEpochSecond(0, 500_000_000); // 0.5 seconds
        double result = InstantConversions.toDouble(zeroSec, dummyConverter);
        assertEquals(0.5, result, 1e-12);
    }

    @Test
    public void testToLong_MinValue() {
        Instant min = Instant.ofEpochMilli(Long.MIN_VALUE);
        long result = InstantConversions.toLong(min, dummyConverter);
        assertEquals(Long.MIN_VALUE, result);
    }

    @Test
    public void testToLong_MaxValue() {
        Instant max = Instant.ofEpochMilli(Long.MAX_VALUE);
        long result = InstantConversions.toLong(max, dummyConverter);
        assertEquals(Long.MAX_VALUE, result);
    }

    @Test
    public void testToOffsetDateTime_ZoneOffsetCalculation() {
        // Build a minimal Converter that returns a known TimeZone (UTC) to avoid NPE.
        ConverterOptions options = new ConverterOptions();
        options.setTimeZone(TimeZone.getTimeZone("UTC"));
        Converter converter = new Converter(options);

        OffsetDateTime odt = InstantConversions.toOffsetDateTime(instant, converter);
        ZoneOffset expectedOffset = ZoneOffset.UTC;
        assertEquals(expectedOffset, odt.getOffset());
        assertEquals(instant, odt.toInstant());
    }

    @Test
    public void testToZonedDateTime_ZoneIdCalculation() {
        // Build a minimal Converter that returns a known ZoneId (UTC) to avoid NPE.
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(java.time.ZoneId.of("UTC"));
        Converter converter = new Converter(options);

        ZonedDateTime zdt = InstantConversions.toZonedDateTime(instant, converter);
        assertEquals(java.time.ZoneId.of("UTC"), zdt.getZone());
        assertEquals(instant, zdt.toInstant());
    }
}
