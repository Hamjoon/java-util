package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import static org.junit.Assert.*;

public class BigIntegerConversionsTest {

    private Converter converter;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        // Assume ConverterOptions has a public no‑arg constructor.
        converter = new Converter(new ConverterOptions());
    }

    /* --------------------------------------------------------------------- */
    /*  toBigDecimal                                                          */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToBigDecimal_Basic() {
        BigInteger bi = new BigInteger("12345678901234567890");
        BigDecimal result = BigIntegerConversions.toBigDecimal(bi, converter);
        assertEquals(new BigDecimal(bi), result);
    }

    /* --------------------------------------------------------------------- */
    /*  toUUID                                                                 */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToUUID_ExactConversion() {
        // UUID: 123e4567-e89b-12d3-a456-426655440000
        // Corresponding 128‑bit integer in hex: 123e4567e89b12d3a456426655440000
        BigInteger bi = new BigInteger("123e4567e89b12d3a456426655440000", 16);
        UUID uuid = BigIntegerConversions.toUUID(bi, converter);
        assertEquals(UUID.fromString("123e4567-e89b-12d3-a456-426655440000"), uuid);
    }

    @Test
    public void testToUUID_PaddingWithZeros() {
        // Small number that needs leading zeros to become 32 hex chars
        BigInteger bi = BigInteger.valueOf(1); // hex "1"
        UUID uuid = BigIntegerConversions.toUUID(bi, converter);
        // Expected UUID is 00000000-0000-0000-0000-000000000001
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), uuid);
    }

    @Test
    public void testToUUID_NegativeNumberThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Cannot convert a negative number");
        BigInteger bi = new BigInteger("-1");
        BigIntegerConversions.toUUID(bi, converter);
    }

    /* --------------------------------------------------------------------- */
    /*  toInstant                                                              */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToInstant_Basic() {
        // 5 seconds + 200 milliseconds = 5_000_000_200 nanoseconds
        BigInteger nanos = BigInteger.valueOf(5_000_000_200L);
        Instant instant = BigIntegerConversions.toInstant(nanos, converter);
        assertEquals(Instant.ofEpochSecond(5, 200_000_000), instant);
    }

    @Test
    public void testToInstant_LargeValue() {
        // 2^60 nanoseconds (well beyond int range)
        BigInteger nanos = BigInteger.valueOf(2).pow(60);
        Instant instant = BigIntegerConversions.toInstant(nanos, converter);
        long expectedSeconds = nanos.divide(BigIntegerConversions.BILLION).longValue();
        int expectedNanos = nanos.remainder(BigIntegerConversions.BILLION).intValue();
        assertEquals(Instant.ofEpochSecond(expectedSeconds, expectedNanos), instant);
    }

    /* --------------------------------------------------------------------- */
    /*  toDuration                                                             */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToDuration_Basic() {
        // 3 seconds + 500 ms = 3_500_000_000 nanoseconds
        BigInteger nanos = BigInteger.valueOf(3_500_000_000L);
        Duration duration = BigIntegerConversions.toDuration(nanos, converter);
        assertEquals(Duration.ofSeconds(3, 500_000_000), duration);
    }

    /* --------------------------------------------------------------------- */
    /*  toDate, toSqlDate, toTimestamp                                          */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToDateConversions() {
        // 2022‑01‑01T12:34:56.789Z  => epoch seconds 1641035696, nanos 789_000_000
        Instant instant = Instant.parse("2022-01-01T12:34:56.789Z");
        BigInteger nanos = BigInteger.valueOf(instant.getEpochSecond()).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(instant.getNano()));

        Date date = BigIntegerConversions.toDate(nanos, converter);
        assertEquals(Date.from(instant), date);

        java.sql.Date sqlDate = BigIntegerConversions.toSqlDate(nanos, converter);
        // sqlDate truncates to milliseconds
        assertEquals(new java.sql.Date(instant.toEpochMilli()), sqlDate);

        Timestamp timestamp = BigIntegerConversions.toTimestamp(nanos, converter);
        assertEquals(Timestamp.from(instant), timestamp);
    }

    /* --------------------------------------------------------------------- */
    /*  toCalendar                                                             */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToCalendar_Basic() {
        // 2020‑06‑15T08:30:00Z  => epoch millis 1592219400000
        Instant instant = Instant.parse("2020-06-15T08:30:00Z");
        BigInteger nanos = BigInteger.valueOf(instant.getEpochSecond()).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(instant.getNano()));

        Calendar cal = BigIntegerConversions.toCalendar(nanos, converter);
        assertEquals(instant.toEpochMilli(), cal.getTimeInMillis());
    }

    /* --------------------------------------------------------------------- */
    /*  toZonedDateTime, toLocalDate, toLocalDateTime, toOffsetDateTime       */
    /* --------------------------------------------------------------------- */
    @Test
    public void testZonedAndLocalDateTimeConversions() {
        // 2021‑12‑31T23:59:59.123456789Z
        Instant instant = Instant.parse("2021-12-31T23:59:59.123456789Z");
        BigInteger nanos = BigInteger.valueOf(instant.getEpochSecond()).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(instant.getNano()));

        ZonedDateTime zdt = BigIntegerConversions.toZonedDateTime(nanos, converter);
        assertEquals(instant.atZone(converter.getOptions().getZoneId()), zdt);

        LocalDate ld = BigIntegerConversions.toLocalDate(nanos, converter);
        assertEquals(zdt.toLocalDate(), ld);

        LocalDateTime ldt = BigIntegerConversions.toLocalDateTime(nanos, converter);
        assertEquals(zdt.toLocalDateTime(), ldt);

        OffsetDateTime odt = BigIntegerConversions.toOffsetDateTime(nanos, converter);
        assertEquals(zdt.toOffsetDateTime(), odt);
    }

    /* --------------------------------------------------------------------- */
    /*  toLocalTime                                                            */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToLocalTime_Valid() {
        // 01:02:03.004005006 = 1h*3_600_000_000_000 + 2m*60_000_000_000 + 3s*1_000_000_000
        // + 4_005_006 nanoseconds
        long nanosOfDay = 1L * 3_600_000_000_000L
                + 2L * 60_000_000_000L
                + 3L * 1_000_000_000L
                + 4_005_006L;
        BigInteger bi = BigInteger.valueOf(nanosOfDay);
        LocalTime lt = BigIntegerConversions.toLocalTime(bi, converter);
        assertEquals(LocalTime.of(1, 2, 3, 4_005_006), lt);
    }

    @Test
    public void testToLocalTime_NegativeThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("must be >= 0");
        BigInteger bi = new BigInteger("-1");
        BigIntegerConversions.toLocalTime(bi, converter);
    }

    @Test
    public void testToLocalTime_TooLargeThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("must be >= 0 && <= 86399999999999");
        // One nanosecond beyond the last valid nanosecond of a day
        BigInteger bi = BigInteger.valueOf(86_400_000_000_000L);
        BigIntegerConversions.toLocalTime(bi, converter);
    }
}
