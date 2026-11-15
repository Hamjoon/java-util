package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link BigIntegerConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>numeric conversions (BigDecimal, Instant, Duration)</li>
 *   <li>date‑time conversions (Date, java.sql.Date, Timestamp, Calendar, ZonedDateTime,
 *       LocalDate, LocalDateTime, LocalTime, OffsetDateTime)</li>
 *   <li>UUID conversion, including padding and negative‑value handling</li>
 *   <li>edge cases such as zero, maximum LocalTime, and values that require padding</li>
 * </ul>
 *
 * No mocking is used – all objects are created via public constructors or factory
 * methods that appear in the supplied source or the declared public API of
 * {@link Converter}.
 */
public class BigIntegerConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // The Converter API only exposes a constructor that takes a ConverterOptions instance.
        // We rely on the default behaviour of ConverterOptions (system default time‑zone / zone‑id).
        converter = new Converter(new ConverterOptions());
    }

    // -------------------------------------------------------------------------
    // toBigDecimal
    // -------------------------------------------------------------------------
    @Test
    public void toBigDecimal_convertsBigIntegerCorrectly() {
        BigInteger bi = new BigInteger("12345678901234567890");
        BigDecimal expected = new BigDecimal(bi);
        assertEquals(expected, BigIntegerConversions.toBigDecimal(bi, converter));
    }

    // -------------------------------------------------------------------------
    // toUUID
    // -------------------------------------------------------------------------
    @Test
    public void toUUID_convertsPositiveBigIntegerToUuid() {
        // UUID: 123e4567-e89b-12d3-a456-426655440000
        String uuidHex = "123e4567e89b12d3a456426655440000";
        BigInteger bigInt = new BigInteger(uuidHex, 16);
        UUID expected = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
        assertEquals(expected, BigIntegerConversions.toUUID(bigInt, converter));
    }

    @Test
    public void toUUID_padsWithLeadingZerosWhenNecessary() {
        // A UUID consisting of only 1 hex digit: "1"
        BigInteger bigInt = BigInteger.ONE; // 0x1
        UUID uuid = BigIntegerConversions.toUUID(bigInt, converter);
        // The expected UUID string will be 00000000-0000-0000-0000-000000000001
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), uuid);
    }

    @Test(expected = IllegalArgumentException.class)
    public void toUUID_throwsOnNegativeBigInteger() {
        BigInteger negative = new BigInteger("-1");
        BigIntegerConversions.toUUID(negative, converter);
    }

    // -------------------------------------------------------------------------
    // toInstant / toDuration
    // -------------------------------------------------------------------------
    @Test
    public void toInstant_convertsNanosecondsToInstantCorrectly() {
        // 1_500_000_000 nanoseconds = 1 second + 500_000_000 ns
        BigInteger nanos = BigInteger.valueOf(1_500_000_000L);
        Instant expected = Instant.ofEpochSecond(1L, 500_000_000);
        assertEquals(expected, BigIntegerConversions.toInstant(nanos, converter));
    }

    @Test
    public void toDuration_convertsNanosecondsToDurationCorrectly() {
        // 3_200_000_000_000 nanoseconds = 3_200 seconds + 0 ns
        BigInteger nanos = BigInteger.valueOf(3_200_000_000_000L);
        Duration expected = Duration.ofSeconds(3_200L);
        assertEquals(expected, BigIntegerConversions.toDuration(nanos, converter));
    }

    // -------------------------------------------------------------------------
    // toDate, toSqlDate, toTimestamp
    // -------------------------------------------------------------------------
    @Test
    public void dateConversionsAllRepresentSameInstant() {
        // 2_000_000_000_000 nanoseconds = 2 seconds
        BigInteger nanos = BigInteger.valueOf(2_000_000_000_000L);
        Instant instant = Instant.ofEpochSecond(2L);
        Date date = BigIntegerConversions.toDate(nanos, converter);
        java.sql.Date sqlDate = BigIntegerConversions.toSqlDate(nanos, converter);
        Timestamp timestamp = BigIntegerConversions.toTimestamp(nanos, converter);

        assertEquals(Date.from(instant), date);
        assertEquals(java.sql.Date.from(instant), sqlDate);
        assertEquals(Timestamp.from(instant), timestamp);
    }

    // -------------------------------------------------------------------------
    // toCalendar
    // -------------------------------------------------------------------------
    @Test
    public void toCalendar_returnsCalendarWithCorrectEpochMillis() {
        // 1_500_000_000_000 nanoseconds = 1_500_000 milliseconds
        BigInteger nanos = BigInteger.valueOf(1_500_000_000_000L);
        Calendar cal = BigIntegerConversions.toCalendar(nanos, converter);
        assertEquals(1_500_000L, cal.getTimeInMillis());
        // Verify the time zone comes from ConverterOptions (system default)
        assertEquals(ConverterOptions.getDefaultTimeZone().getID(),
                     cal.getTimeZone().getID());
    }

    // -------------------------------------------------------------------------
    // toZonedDateTime and derived conversions
    // -------------------------------------------------------------------------
    @Test
    public void toZonedDateTime_usesConverterOptionsZoneId() {
        // 0 nanoseconds -> epoch start
        BigInteger nanos = BigInteger.ZERO;
        ZonedDateTime zdt = BigIntegerConversions.toZonedDateTime(nanos, converter);
        assertEquals(Instant.EPOCH, zdt.toInstant());
        assertEquals(ConverterOptions.getDefaultZoneId(), zdt.getZone());
    }

    @Test
    public void toLocalDate_and_toLocalDateTime_and_toOffsetDateTime_consistency() {
        // 2021‑03‑14T15:09:26.123456789Z  => nanoseconds since epoch
        Instant instant = Instant.parse("2021-03-14T15:09:26.123456789Z");
        BigInteger nanos = BigInteger.valueOf(instant.getEpochSecond())
                .multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(instant.getNano()));

        LocalDate expectedDate = LocalDate.of(2021, 3, 14);
        LocalDateTime expectedDateTime = LocalDateTime.of(2021, 3, 14, 15, 9, 26, 123_456_789);
        OffsetDateTime expectedOffset = OffsetDateTime.ofInstant(instant, ConverterOptions.getDefaultZoneId());

        assertEquals(expectedDate, BigIntegerConversions.toLocalDate(nanos, converter));
        assertEquals(expectedDateTime, BigIntegerConversions.toLocalDateTime(nanos, converter));
        assertEquals(expectedOffset, BigIntegerConversions.toOffsetDateTime(nanos, converter));
    }

    // -------------------------------------------------------------------------
    // toLocalTime
    // -------------------------------------------------------------------------
    @Test
    public void toLocalTime_convertsNanosecondsWithinDay() {
        // 13:45:30.123456789 -> nanoseconds since start of day
        LocalTime lt = LocalTime.of(13, 45, 30, 123_456_789);
        BigInteger nanos = BigInteger.valueOf(lt.toNanoOfDay());
        assertEquals(lt, BigIntegerConversions.toLocalTime(nanos, converter));
    }

    @Test
    public void toLocalTime_acceptsMaximumNanosecondOfDay() {
        // 23:59:59.999999999
        LocalTime max = LocalTime.of(23, 59, 59, 999_999_999);
        BigInteger nanos = BigInteger.valueOf(max.toNanoOfDay());
        assertEquals(max, BigIntegerConversions.toLocalTime(nanos, converter));
    }

    @Test
    public void toLocalTime_rejectsNegativeValue() {
        BigInteger negative = BigInteger.valueOf(-1);
        try {
            BigIntegerConversions.toLocalTime(negative, converter);
            fail("Expected IllegalArgumentException for negative nanoseconds");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("must be >= 0"));
        }
    }

    @Test
    public void toLocalTime_rejectsValueLargerThanOneDay() {
        // One day + 1 nanosecond
        BigInteger tooLarge = BigInteger.valueOf(24L * 60 * 60 * 1_000_000_000L + 1);
        try {
            BigIntegerConversions.toLocalTime(tooLarge, converter);
            fail("Expected IllegalArgumentException for nanoseconds > 24h");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("must be >= 0 && <= 86399999999999"));
        }
    }

    // -------------------------------------------------------------------------
    // Additional edge cases
    // -------------------------------------------------------------------------
    @Test
    public void toInstant_handlesZeroNanoseconds() {
        assertEquals(Instant.EPOCH, BigIntegerConversions.toInstant(BigInteger.ZERO, converter));
    }

    @Test
    public void toDuration_handlesZeroNanoseconds() {
        assertEquals(Duration.ZERO, BigIntegerConversions.toDuration(BigInteger.ZERO, converter));
    }

    @Test
    public void toBigDecimal_handlesZero() {
        assertEquals(BigDecimal.ZERO, BigIntegerConversions.toBigDecimal(BigInteger.ZERO, converter));
    }
}
