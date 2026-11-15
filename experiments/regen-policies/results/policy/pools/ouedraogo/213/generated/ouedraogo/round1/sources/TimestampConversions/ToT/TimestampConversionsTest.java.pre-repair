package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link TimestampConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical values (current instant, epoch, far future)</li>
 *   <li>Edge cases (nanosecond precision, millisecond‑only precision, negative epoch)</li>
 *   <li>All conversion methods defined in {@code TimestampConversions}</li>
 *   <li>Verification that the {@code Converter} options (zone / time‑zone) are honoured</li>
 * </ul>
 *
 * No mocking is performed – real {@link Timestamp}, {@link Converter},
 * {@link ConverterOptions} and Java‑time objects are used.
 */
public class TimestampConversionsTest {

    private Converter utcConverter;          // Converter whose options use UTC
    private ZoneId utcZone;                  // Cached ZoneId for readability
    private TimeZone utcTimeZone;            // Cached TimeZone for Calendar tests

    @Before
    public void setUp() {
        // The library supplies a default {@code ConverterOptions} constructor that
        // initialises the zone to UTC.  If the constructor behaves differently the
        // test suite will still pass because we explicitly set the zone / time‑zone.
        ConverterOptions options = new ConverterOptions()
                .withZoneId(ZoneOffset.UTC)          // force UTC for all conversions
                .withTimeZone(TimeZone.getTimeZone("UTC"));
        utcConverter = new Converter(options);
        utcZone = ZoneOffset.UTC;
        utcTimeZone = TimeZone.getTimeZone("UTC");
    }

    /**
     * Helper that creates a {@link Timestamp} from an {@link Instant}.
     * The {@code Timestamp} constructor stores the millisecond part in the
     * internal long value and the nanosecond part in a separate field.
     */
    private Timestamp timestampFrom(Instant instant) {
        Timestamp ts = new Timestamp(instant.toEpochMilli());
        ts.setNanos(instant.getNano());
        return ts;
    }

    /* ---------------------------------------------------------------------- */
    /*  toDouble                                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToDouble_MillisecondPrecision() {
        // 2022‑01‑01T12:34:56.789Z  (exact millisecond, no extra nanos)
        Instant instant = Instant.parse("2022-01-01T12:34:56.789Z");
        Timestamp ts = timestampFrom(instant);

        double expected = instant.getEpochSecond() + instant.getNano() / 1_000_000_000.0;
        double actual = TimestampConversions.toDouble(ts, utcConverter);

        assertEquals("Double conversion for millisecond precision is incorrect",
                expected, actual, 1e-12);
    }

    @Test
    public void testToDouble_NanosecondPrecision() {
        // 2022‑01‑01T12:34:56.789123456Z  (nanosecond part beyond milliseconds)
        Instant instant = Instant.parse("2022-01-01T12:34:56.789123456Z");
        Timestamp ts = timestampFrom(instant);

        double expected = instant.getEpochSecond() + instant.getNano() / 1_000_000_000.0;
        double actual = TimestampConversions.toDouble(ts, utcConverter);

        assertEquals("Double conversion for nanosecond precision is incorrect",
                expected, actual, 1e-12);
    }

    @Test
    public void testToDouble_NegativeEpoch() {
        // 1965‑12‑31T23:59:59.500Z (negative epoch milliseconds)
        Instant instant = Instant.parse("1965-12-31T23:59:59.500Z");
        Timestamp ts = timestampFrom(instant);

        double expected = instant.getEpochSecond() + instant.getNano() / 1_000_000_000.0;
        double actual = TimestampConversions.toDouble(ts, utcConverter);

        assertEquals("Double conversion for negative epoch is incorrect",
                expected, actual, 1e-12);
    }

    /* ---------------------------------------------------------------------- */
    /*  toBigDecimal                                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToBigDecimal_SimpleInstant() {
        Instant instant = Instant.parse("2020-02-29T01:02:03.004005006Z");
        Timestamp ts = timestampFrom(instant);

        BigDecimal actual = TimestampConversions.toBigDecimal(ts, utcConverter);

        // Expected: seconds + nanos/1_000_000_000 as a BigDecimal
        BigDecimal expected = BigDecimal.valueOf(instant.getEpochSecond())
                .add(BigDecimal.valueOf(instant.getNano()).divide(BigDecimal.valueOf(1_000_000_000)));

        assertEquals("BigDecimal conversion does not match expected value",
                0, expected.compareTo(actual));
    }

    @Test
    public void testToBigDecimal_EpochZero() {
        Instant instant = Instant.EPOCH;
        Timestamp ts = timestampFrom(instant);

        BigDecimal actual = TimestampConversions.toBigDecimal(ts, utcConverter);
        assertEquals("BigDecimal conversion of epoch should be zero", BigDecimal.ZERO, actual);
    }

    /* ---------------------------------------------------------------------- */
    /*  toBigInteger                                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToBigInteger_SimpleInstant() {
        Instant instant = Instant.parse("2021-07-01T12:00:00.123456789Z");
        Timestamp ts = timestampFrom(instant);

        BigInteger actual = TimestampConversions.toBigInteger(ts, utcConverter);

        // Expected: total nanoseconds since epoch as a BigInteger
        BigInteger expected = BigInteger.valueOf(instant.getEpochSecond())
                .multiply(BigInteger.valueOf(1_000_000_000L))
                .add(BigInteger.valueOf(instant.getNano()));

        assertEquals("BigInteger conversion does not match expected nanoseconds",
                expected, actual);
    }

    @Test
    public void testToBigInteger_EpochZero() {
        Timestamp ts = timestampFrom(Instant.EPOCH);
        BigInteger actual = TimestampConversions.toBigInteger(ts, utcConverter);
        assertEquals("BigInteger conversion of epoch should be zero", BigInteger.ZERO, actual);
    }

    /* ---------------------------------------------------------------------- */
    /*  toLocalDateTime                                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToLocalDateTime_UtcZone() {
        Instant instant = Instant.parse("2023-03-15T08:30:45.123456789Z");
        Timestamp ts = timestampFrom(instant);

        LocalDateTime actual = TimestampConversions.toLocalDateTime(ts, utcConverter);
        LocalDateTime expected = instant.atZone(utcZone).toLocalDateTime();

        assertEquals("LocalDateTime conversion failed", expected, actual);
    }

    /* ---------------------------------------------------------------------- */
    /*  toDuration                                                              */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToDuration_BetweenEpochAndInstant() {
        Instant instant = Instant.parse("1975-05-20T10:20:30.400500600Z");
        Timestamp ts = timestampFrom(instant);

        Duration actual = TimestampConversions.toDuration(ts, utcConverter);
        Duration expected = Duration.between(Instant.EPOCH, instant);

        assertEquals("Duration conversion is incorrect", expected, actual);
    }

    /* ---------------------------------------------------------------------- */
    /*  toOffsetDateTime                                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToOffsetDateTime_UtcOffset() {
        Instant instant = Instant.parse("2000-01-01T00:00:00.999999999Z");
        Timestamp ts = timestampFrom(instant);

        OffsetDateTime actual = TimestampConversions.toOffsetDateTime(ts, utcConverter);
        OffsetDateTime expected = instant.atOffset(ZoneOffset.UTC);

        assertEquals("OffsetDateTime conversion is incorrect", expected, actual);
    }

    /* ---------------------------------------------------------------------- */
    /*  toCalendar                                                              */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToCalendar_UtcTimeZone() {
        Instant instant = Instant.parse("1999-12-31T23:59:59.500Z");
        Timestamp ts = timestampFrom(instant);

        Calendar cal = TimestampConversions.toCalendar(ts, utcConverter);

        assertEquals("Calendar time zone must be UTC", utcTimeZone, cal.getTimeZone());
        assertEquals("Calendar milliseconds value is incorrect",
                ts.getTime(), cal.getTimeInMillis());
    }

    /* ---------------------------------------------------------------------- */
    /*  toDate                                                                  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToDate_Equality() {
        Instant instant = Instant.parse("2010-10-10T10:10:10.010Z");
        Timestamp ts = timestampFrom(instant);

        Date actual = TimestampConversions.toDate(ts, utcConverter);
        Date expected = Date.from(instant);

        assertEquals("Date conversion does not match expected value", expected, actual);
    }

    /* ---------------------------------------------------------------------- */
    /*  toSqlDate                                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToSqlDate_CorrectLocalDate() {
        Instant instant = Instant.parse("2022-08-15T00:00:00.000Z");
        Timestamp ts = timestampFrom(instant);

        java.sql.Date actual = TimestampConversions.toSqlDate(ts, utcConverter);
        java.sql.Date expected = java.sql.Date.valueOf(LocalDate.of(2022, 8, 15));

        assertEquals("SQL Date conversion is incorrect", expected, actual);
    }

    /* ---------------------------------------------------------------------- */
    /*  toLong                                                                  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToLong_MillisExact() {
        Timestamp ts = Timestamp.valueOf("2021-12-31 23:59:59.123456789");
        long actual = TimestampConversions.toLong(ts, utcConverter);
        assertEquals("toLong should return the same millisecond value as getTime()",
                ts.getTime(), actual);
    }

    /* ---------------------------------------------------------------------- */
    /*  toYear                                                                  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToYear_Utc() {
        Instant instant = Instant.parse("1995-06-15T12:00:00Z");
        Timestamp ts = timestampFrom(instant);

        Year actual = TimestampConversions.toYear(ts, utcConverter);
        Year expected = Year.of(1995);

        assertEquals("Year conversion is incorrect", expected, actual);
    }

    /* ---------------------------------------------------------------------- */
    /*  toYearMonth                                                             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToYearMonth_Utc() {
        Instant instant = Instant.parse("2005-11-30T08:30:00Z");
        Timestamp ts = timestampFrom(instant);

        YearMonth actual = TimestampConversions.toYearMonth(ts, utcConverter);
        YearMonth expected = YearMonth.of(2005, 11);

        assertEquals("YearMonth conversion is incorrect", expected, actual);
    }

    /* ---------------------------------------------------------------------- */
    /*  toMonthDay                                                              */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToMonthDay_Utc() {
        Instant instant = Instant.parse("2019-02-29T00:00:00Z"); // 2019 is not leap, will be adjusted by parser
        // Use a valid date instead:
        instant = Instant.parse("2020-02-29T00:00:00Z");
        Timestamp ts = timestampFrom(instant);

        MonthDay actual = TimestampConversions.toMonthDay(ts, utcConverter);
        MonthDay expected = MonthDay.of(2, 29);

        assertEquals("MonthDay conversion is incorrect", expected, actual);
    }

    /* ---------------------------------------------------------------------- */
    /*  toString (millisecond vs nanosecond formatting)                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToString_MillisecondPattern() {
        // Nanos exactly divisible by 1_000_000 → millisecond pattern
        Instant instant = Instant.parse("2022-01-01T10:20:30.123Z");
        Timestamp ts = timestampFrom(instant);

        String actual = TimestampConversions.toString(ts, utcConverter);
        String expected = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .withZone(ZoneOffset.UTC)
                .format(instant);

        assertEquals("String formatting (millisecond) is incorrect", expected, actual);
    }

    @Test
    public void testToString_NanosecondPattern() {
        // Nanos not divisible by 1_000_000 → nanosecond pattern
        Instant instant = Instant.parse("2022-01-01T10:20:30.123456789Z");
        Timestamp ts = timestampFrom(instant);

        String actual = TimestampConversions.toString(ts, utcConverter);
        String expected = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'")
                .withZone(ZoneOffset.UTC)
                .format(instant);

        assertEquals("String formatting (nanosecond) is incorrect", expected, actual);
    }

    /* ---------------------------------------------------------------------- */
    /*  toMap                                                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToMap_ContainsTimestampKey() {
        Instant instant = Instant.parse("2023-04-01T12:34:56.789Z");
        Timestamp ts = timestampFrom(instant);

        Map<String, Object> map = TimestampConversions.toMap(ts, utcConverter);

        // The library defines the constant key in MapConversions.TIMESTAMP
        String expectedKey = MapConversions.TIMESTAMP;
        assertTrue("Map must contain the timestamp key", map.containsKey(expectedKey));

        String expectedValue = TimestampConversions.toString(ts, utcConverter);
        assertEquals("Map value for timestamp key must match formatted string",
                expectedValue, map.get(expectedKey));
    }

    /* ---------------------------------------------------------------------- */
    /*  Helper: ensure ConverterOptions can be customised (if needed)          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testConverterOptions_OverrideZone() {
        // Verify that a non‑UTC zone is respected by the conversion methods.
        ZoneId paris = ZoneId.of("Europe/Paris");
        ConverterOptions options = new ConverterOptions()
                .withZoneId(paris)
                .withTimeZone(TimeZone.getTimeZone(paris));
        Converter parisConverter = new Converter(options);

        Instant instant = Instant.parse("2022-12-31T23:00:00Z"); // 00:00 in Paris (+1)
        Timestamp ts = timestampFrom(instant);

        LocalDateTime ldt = TimestampConversions.toLocalDateTime(ts, parisConverter);
        assertEquals("LocalDateTime must reflect the overridden zone",
                LocalDateTime.of(2023, 1, 1, 0, 0), ldt);
    }
}
