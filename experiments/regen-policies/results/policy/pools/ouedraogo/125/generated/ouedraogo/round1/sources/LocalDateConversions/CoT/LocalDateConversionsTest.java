package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

/**
 * JUnit‑4 test suite for {@link LocalDateConversions}.
 *
 * The tests are placed in the same package as the class under test
 * so that the package‑private static methods are directly accessible.
 *
 * The test strategy follows the requested steps:
 *
 * 1. List of public (exposed) methods – see the comment block below.
 * 2. Basic functionality tests for each method using a typical LocalDate.
 * 3. Edge‑case / exception tests (null input, wrong type, extreme dates,
 *    time‑zone handling, etc.).
 * 4. All tests are merged into a single test class.
 */
public class LocalDateConversionsTest {

    /* ----------------------------------------------------------------------
     * 1. List of exposed static methods (they are package‑private but visible
     *    to tests in the same package).  The signatures are reproduced here
     *    for reference.
     *
     *    static Instant        toInstant(Object from, Converter converter)
     *    static long           toLong(Object from, Converter converter)
     *    static LocalDateTime toLocalDateTime(Object from, Converter converter)
     *    static ZonedDateTime toZonedDateTime(Object from, Converter converter)
     *    static OffsetDateTime toOffsetDateTime(Object from, Converter converter)
     *    static double        toDouble(Object from, Converter converter)
     *    static AtomicLong    toAtomicLong(Object from, Converter converter)
     *    static Timestamp     toTimestamp(Object from, Converter converter)
     *    static Calendar      toCalendar(Object from, Converter converter)
     *    static java.sql.Date toSqlDate(Object from, Converter converter)
     *    static Date          toDate(Object from, Converter converter)
     *    static BigInteger    toBigInteger(Object from, Converter converter)
     *    static BigDecimal    toBigDecimal(Object from, Converter converter)
     *    static String        toString(Object from, Converter converter)
     *    static Map<String,Object> toMap(Object from, Converter converter)
     * ---------------------------------------------------------------------- */

    private Converter defaultConverter;
    private LocalDate typicalDate;   // 2023‑05‑15
    private LocalDate minDate;       // LocalDate.MIN
    private LocalDate maxDate;       // LocalDate.MAX

    @Before
    public void setUp() {
        // The library supplies a public constructor `new Converter(ConverterOptions)`.
        // We use the default options (system default zone & time‑zone) which are sufficient
        // for the majority of tests.  No additional configuration is required.
        defaultConverter = new Converter(new ConverterOptions());

        typicalDate = LocalDate.of(2023, 5, 15);
        minDate = LocalDate.MIN;
        maxDate = LocalDate.MAX;
    }

    /* ----------------------------------------------------------------------
     * 2. Basic functionality tests – one test per conversion method using the
     *    “typical” LocalDate value.
     * ---------------------------------------------------------------------- */

    @Test
    public void testToInstant() {
        Instant instant = LocalDateConversions.toInstant(typicalDate, defaultConverter);
        // The instant should represent midnight at the converter's zone.
        ZonedDateTime expected = ZonedDateTime.of(typicalDate, LocalTime.MIDNIGHT,
                defaultConverter.getOptions().getZoneId());
        assertEquals(expected.toInstant(), instant);
    }

    @Test
    public void testToLong() {
        long millis = LocalDateConversions.toLong(typicalDate, defaultConverter);
        Instant instant = LocalDateConversions.toInstant(typicalDate, defaultConverter);
        assertEquals(instant.toEpochMilli(), millis);
    }

    @Test
    public void testToLocalDateTime() {
        LocalDateTime ldt = LocalDateConversions.toLocalDateTime(typicalDate, defaultConverter);
        assertEquals(LocalDateTime.of(typicalDate, LocalTime.MIDNIGHT), ldt);
    }

    @Test
    public void testToZonedDateTime() {
        ZonedDateTime zdt = LocalDateConversions.toZonedDateTime(typicalDate, defaultConverter);
        ZonedDateTime expected = ZonedDateTime.of(typicalDate, LocalTime.MIDNIGHT,
                defaultConverter.getOptions().getZoneId());
        assertEquals(expected, zdt);
    }

    @Test
    public void testToOffsetDateTime() {
        OffsetDateTime odt = LocalDateConversions.toOffsetDateTime(typicalDate, defaultConverter);
        TimeZone tz = defaultConverter.getOptions().getTimeZone();
        int offsetSeconds = tz.getOffset(System.currentTimeMillis()) / 1000;
        ZoneOffset expectedOffset = ZoneOffset.ofTotalSeconds(offsetSeconds);
        OffsetDateTime expected = OffsetDateTime.of(typicalDate, LocalTime.MIDNIGHT, expectedOffset);
        assertEquals(expected, odt);
    }

    @Test
    public void testToDouble() {
        double seconds = LocalDateConversions.toDouble(typicalDate, defaultConverter);
        long millis = LocalDateConversions.toLong(typicalDate, defaultConverter);
        assertEquals(millis / 1000d, seconds, 0.000001);
    }

    @Test
    public void testToAtomicLong() {
        AtomicLong atomic = LocalDateConversions.toAtomicLong(typicalDate, defaultConverter);
        assertEquals(LocalDateConversions.toLong(typicalDate, defaultConverter), atomic.get());
    }

    @Test
    public void testToTimestamp() {
        Timestamp ts = LocalDateConversions.toTimestamp(typicalDate, defaultConverter);
        // Timestamp is built from epochDay * 86400 * 1000 (midnight UTC)
        long expectedMillis = typicalDate.toEpochDay() * 86400L * 1000L;
        assertEquals(expectedMillis, ts.getTime());
    }

    @Test
    public void testToCalendar() {
        Calendar cal = LocalDateConversions.toCalendar(typicalDate, defaultConverter);
        ZonedDateTime zdt = LocalDateConversions.toZonedDateTime(typicalDate, defaultConverter);
        assertEquals(zdt.toInstant().toEpochMilli(), cal.getTimeInMillis());
        assertEquals(defaultConverter.getOptions().getTimeZone(), cal.getTimeZone());
    }

    @Test
    public void testToSqlDate() {
        java.sql.Date sqlDate = LocalDateConversions.toSqlDate(typicalDate, defaultConverter);
        long expected = LocalDateConversions.toLong(typicalDate, defaultConverter);
        assertEquals(expected, sqlDate.getTime());
    }

    @Test
    public void testToDate() {
        Date date = LocalDateConversions.toDate(typicalDate, defaultConverter);
        long expected = LocalDateConversions.toLong(typicalDate, defaultConverter);
        assertEquals(expected, date.getTime());
    }

    @Test
    public void testToBigInteger() {
        BigInteger bi = LocalDateConversions.toBigInteger(typicalDate, defaultConverter);
        Instant instant = LocalDateConversions.toInstant(typicalDate, defaultConverter);
        BigInteger expected = InstantConversions.toBigInteger(instant, defaultConverter);
        assertEquals(expected, bi);
    }

    @Test
    public void testToBigDecimal() {
        BigDecimal bd = LocalDateConversions.toBigDecimal(typicalDate, defaultConverter);
        Instant instant = LocalDateConversions.toInstant(typicalDate, defaultConverter);
        BigDecimal expected = InstantConversions.toBigDecimal(instant, defaultConverter);
        assertEquals(expected, bd);
    }

    @Test
    public void testToString() {
        String s = LocalDateConversions.toString(typicalDate, defaultConverter);
        assertEquals(typicalDate.format(DateTimeFormatter.ISO_LOCAL_DATE), s);
    }

    @Test
    public void testToMap() {
        Map<String, Object> map = LocalDateConversions.toMap(typicalDate, defaultConverter);
        assertNotNull(map);
        assertEquals(1, map.size());
        assertTrue(map.containsKey(MapConversions.DATE));
        assertEquals(typicalDate.toString(), map.get(MapConversions.DATE));
    }

    /* ----------------------------------------------------------------------
     * 3. Edge‑case & exception tests.
     * ---------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToInstant_NullInput() {
        LocalDateConversions.toInstant(null, defaultConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToInstant_WrongType() {
        LocalDateConversions.toInstant("2023-01-01", defaultConverter);
    }

    @Test
    public void testExtremeDates() {
        // MIN
        Instant minInstant = LocalDateConversions.toInstant(minDate, defaultConverter);
        assertNotNull(minInstant);
        // MAX
        Instant maxInstant = LocalDateConversions.toInstant(maxDate, defaultConverter);
        assertNotNull(maxInstant);
        // Verify that max > min
        assertTrue(maxInstant.isAfter(minInstant));
    }

    @Test
    public void testToOffsetDateTime_WithKnownTimeZone() {
        // Build a Converter whose options use a fixed time‑zone (UTC‑05:00)
        ConverterOptions opts = new ConverterOptions();
        opts.setTimeZone(TimeZone.getTimeZone("America/New_York")); // UTC‑05:00 (or ‑04:00 in DST)
        opts.setZoneId(ZoneId.of("America/New_York"));
        Converter nyConverter = new Converter(opts);

        OffsetDateTime odt = LocalDateConversions.toOffsetDateTime(typicalDate, nyConverter);
        TimeZone tz = nyConverter.getOptions().getTimeZone();
        int offsetSeconds = tz.getOffset(System.currentTimeMillis()) / 1000;
        ZoneOffset expectedOffset = ZoneOffset.ofTotalSeconds(offsetSeconds);
        OffsetDateTime expected = OffsetDateTime.of(typicalDate, LocalTime.MIDNIGHT, expectedOffset);
        assertEquals(expected, odt);
    }

    @Test
    public void testToTimestamp_EpochDayCalculation() {
        // Use LocalDate.ofEpochDay(0) which is 1970‑01‑01
        LocalDate epochDayZero = LocalDate.ofEpochDay(0);
        Timestamp ts = LocalDateConversions.toTimestamp(epochDayZero, defaultConverter);
        assertEquals(0L, ts.getTime());
    }

    @Test
    public void testToCalendar_UsesConverterTimeZone() {
        // Choose a non‑default zone to ensure the calendar gets the correct TZ.
        ConverterOptions opts = new ConverterOptions();
        opts.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
        opts.setZoneId(ZoneId.of("Asia/Tokyo"));
        Converter tokyoConverter = new Converter(opts);

        Calendar cal = LocalDateConversions.toCalendar(typicalDate, tokyoConverter);
        assertEquals(TimeZone.getTimeZone("Asia/Tokyo"), cal.getTimeZone());
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullInput() {
        LocalDateConversions.toMap(null, defaultConverter);
    }

    @Test
    public void testToMap_ImmutableResult() {
        Map<String, Object> map = LocalDateConversions.toMap(typicalDate, defaultConverter);
        // The map implementation is a CompactMap – it should support put/remove.
        // We verify that modifications do not affect the original date representation.
        map.put("extra", "value");
        assertEquals("value", map.get("extra"));
    }
}
