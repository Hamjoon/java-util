package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

/**
 * Unit tests for {@link OffsetDateTimeConversions}.
 *
 * <p>
 * The {@code Converter} argument is not required for the majority of the conversion
 * methods – they operate directly on the supplied {@link OffsetDateTime} instance.
 * For those methods that do not use the {@code Converter} (or its {@code ConverterOptions})
 * we can safely pass {@code null} as the second argument.
 * </p>
 *
 * <p>
 * The tests verify that each conversion method returns a value that is equivalent
 * to the value produced by the underlying JDK API or the corresponding helper class
 * from the Cedar library (e.g. {@link InstantConversions}, {@link BigDecimalConversions}).
 * </p>
 */
public class OffsetDateTimeConversionsTest {

    /** Helper creates an {@link OffsetDateTime} at the given epoch milli and with the supplied offset. */
    private static OffsetDateTime odtOfEpochMilli(long epochMilli, ZoneOffset offset) {
        return Instant.ofEpochMilli(epochMilli).atOffset(offset);
    }

    @Test
    public void testToInstant() {
        OffsetDateTime odt = odtOfEpochMilli(1_600_000_000_000L, ZoneOffset.UTC);
        Instant expected = odt.toInstant();
        assertEquals(expected, OffsetDateTimeConversions.toInstant(odt, null));
    }

    @Test
    public void testToLong_EpochMilli() {
        OffsetDateTime odt = odtOfEpochMilli(1_234_567_890L, ZoneOffset.ofHoursMinutes(2, 30));
        long expected = odt.toInstant().toEpochMilli();
        assertEquals(expected, OffsetDateTimeConversions.toLong(odt, null));
    }

    @Test
    public void testToLong_NegativeEpoch() {
        OffsetDateTime odt = odtOfEpochMilli(-123_456_789L, ZoneOffset.ofHours(-5));
        long expected = odt.toInstant().toEpochMilli();
        assertEquals(expected, OffsetDateTimeConversions.toLong(odt, null));
    }

    @Test
    public void testToAtomicLong() {
        OffsetDateTime odt = odtOfEpochMilli(42L, ZoneOffset.UTC);
        AtomicLong expected = new AtomicLong(odt.toInstant().toEpochMilli());
        assertEquals(expected, OffsetDateTimeConversions.toAtomicLong(odt, null));
    }

    @Test
    public void testToTimestamp() {
        OffsetDateTime odt = odtOfEpochMilli(9_876_543_210L, ZoneOffset.UTC);
        Timestamp expected = Timestamp.from(odt.toInstant());
        assertEquals(expected, OffsetDateTimeConversions.toTimestamp(odt, null));
    }

    @Test
    public void testToSqlDate() {
        OffsetDateTime odt = odtOfEpochMilli(0L, ZoneOffset.UTC);
        java.sql.Date expected = new java.sql.Date(0L);
        assertEquals(expected, OffsetDateTimeConversions.toSqlDate(odt, null));
    }

    @Test
    public void testToDate() {
        OffsetDateTime odt = odtOfEpochMilli(1_111_222_333L, ZoneOffset.UTC);
        Date expected = new Date(odt.toInstant().toEpochMilli());
        assertEquals(expected, OffsetDateTimeConversions.toDate(odt, null));
    }

    @Test
    public void testToBigInteger() {
        OffsetDateTime odt = odtOfEpochMilli(5_000L, ZoneOffset.UTC);
        Instant instant = odt.toInstant();
        // Expected value is produced by the same helper used in the implementation.
        BigInteger expected = InstantConversions.toBigInteger(instant, null);
        assertEquals(expected, OffsetDateTimeConversions.toBigInteger(odt, null));
    }

    @Test
    public void testToBigDecimal() {
        OffsetDateTime odt = odtOfEpochMilli(7_777L, ZoneOffset.UTC);
        Instant instant = odt.toInstant();
        BigDecimal expected = InstantConversions.toBigDecimal(instant, null);
        assertEquals(expected, OffsetDateTimeConversions.toBigDecimal(odt, null));
    }

    @Test
    public void testToOffsetTime() {
        OffsetDateTime odt = odtOfEpochMilli(1_000L, ZoneOffset.ofHours(3));
        OffsetTime expected = odt.toOffsetTime();
        assertEquals(expected, OffsetDateTimeConversions.toOffsetTime(odt, null));
    }

    @Test
    public void testToString_ISOFormat() {
        OffsetDateTime odt = OffsetDateTime.of(2023, 3, 15, 12, 34, 56, 123_456_789, ZoneOffset.ofHoursMinutes(5, 30));
        String expected = odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        assertEquals(expected, OffsetDateTimeConversions.toString(odt, null));
    }

    @Test
    public void testToDouble() {
        OffsetDateTime odt = odtOfEpochMilli(1_600_000_000_000L, ZoneOffset.UTC);
        Instant instant = odt.toInstant();
        double expected = BigDecimalConversions.secondsAndNanosToDouble(instant.getEpochSecond(), instant.getNano()).doubleValue();
        assertEquals(expected, OffsetDateTimeConversions.toDouble(odt, null), 0.0);
    }

    @Test
    public void testToDouble_WithNegativeNanos() {
        // Create an instant with a negative nanosecond adjustment (e.g., 0.5 seconds before the epoch)
        Instant instant = Instant.ofEpochSecond(-1, 500_000_000L); // -0.5 seconds
        OffsetDateTime odt = instant.atOffset(ZoneOffset.UTC);
        double expected = BigDecimalConversions.secondsAndNanosToDouble(instant.getEpochSecond(), instant.getNano()).doubleValue();
        assertEquals(expected, OffsetDateTimeConversions.toDouble(odt, null), 0.0);
    }
}
