package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class BigIntegerConversions_toTimestamp_Test {

    /**
     * Shared Converter instance used by all tests.
     */
    private static Converter converter;

    /**
     * Shared default Converter instance used by most tests.
     */
    private static Converter defaultConverter;

    /**
     * Original default time‑zone – restored after all tests.
     */
    private static ZoneId originalZone;

    @BeforeAll
    static void setupBeforeAll() {
        // Preserve the original default zone and enforce UTC for deterministic results.
        originalZone = ZoneId.systemDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        // Initialise a default Converter (empty options are sufficient for built‑in conversions).
        defaultConverter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore the original default zone.
        TimeZone.setDefault(TimeZone.getTimeZone(originalZone));
        defaultConverter = null;
    }

    @Test
    void toTimestamp_fromInstant_returnsMatchingTimestamp() {
        // GIVEN
        Instant now = Instant.now();
        // WHEN
        Timestamp ts = BigIntegerConversions.toTimestamp(now, converter);
        // THEN
        assertNotNull(ts, "Resulting Timestamp should not be null");
        assertEquals(Timestamp.from(now), ts, "Timestamp should represent the same instant as the source Instant");
    }

    @Test
    void toTimestamp_fromDate_returnsMatchingTimestamp() {
        // GIVEN
        // current time in milliseconds
        Date now = new Date();
        // WHEN
        Timestamp ts = BigIntegerConversions.toTimestamp(now, converter);
        // THEN
        assertNotNull(ts, "Resulting Timestamp should not be null");
        Timestamp expected = new Timestamp(now.getTime());
        assertEquals(expected, ts, "Timestamp should represent the same instant as the source Date");
    }

    @Test
    void toTimestamp_fromBigIntegerMilliseconds_returnsMatchingTimestamp() {
        // GIVEN
        Instant now = Instant.now();
        // Use milliseconds since epoch – this is how the library treats a BigInteger
        // when converting to an Instant (via toInstant → Instant.ofEpochMilli).
        BigInteger millis = BigInteger.valueOf(now.toEpochMilli());
        // WHEN
        Timestamp ts = BigIntegerConversions.toTimestamp(millis, converter);
        // THEN
        assertNotNull(ts, "Resulting Timestamp should not be null");
        Timestamp expected = Timestamp.from(Instant.ofEpochMilli(millis.longValue()));
        assertEquals(expected, ts, "Timestamp should match the instant derived from the BigInteger milliseconds value");
    }

    @Test
    void conversion_fromJavaUtilDate_specificEpoch() {
        // Input: new java.util.Date(1622505600000L) → 2021‑06‑01T00:00:00Z
        Date source = new Date(1622505600000L);
        Timestamp ts = BigIntegerConversions.toTimestamp(source, defaultConverter);
        assertNotNull(ts);
        Timestamp expected = Timestamp.from(Instant.ofEpochMilli(1622505600000L));
        assertEquals(expected, ts);
    }

    @Test
    void conversion_fromInstant_specificValue() {
        // Input: Instant.parse("2022-01-15T12:34:56Z")
        Instant source = Instant.parse("2022-01-15T12:34:56Z");
        Timestamp ts = BigIntegerConversions.toTimestamp(source, defaultConverter);
        assertNotNull(ts);
        assertEquals(Timestamp.from(source), ts);
    }

    @Test
    void conversion_fromLocalDateTime_systemDefaultZone() {
        // Input: LocalDateTime.of(2020, 5, 20, 10, 15, 30)
        LocalDateTime ldt = LocalDateTime.of(2020, 5, 20, 10, 15, 30);
        Timestamp ts = BigIntegerConversions.toTimestamp(ldt, defaultConverter);
        assertNotNull(ts);
        // The conversion treats the LocalDateTime as being in the system default zone (UTC here).
        ZonedDateTime zdt = ldt.atZone(ZoneId.systemDefault());
        Instant expectedInstant = zdt.toInstant();
        assertEquals(Timestamp.from(expectedInstant), ts);
    }

    @Test
    void conversion_fromLocalDate_midnight() {
        // Input: LocalDate.of(2019, 12, 31) → midnight of that date in default zone.
        LocalDate ld = LocalDate.of(2019, 12, 31);
        Timestamp ts = BigIntegerConversions.toTimestamp(ld, defaultConverter);
        assertNotNull(ts);
        ZonedDateTime zdt = ld.atStartOfDay(ZoneId.systemDefault());
        Instant expected = zdt.toInstant();
        assertEquals(Timestamp.from(expected), ts);
    }

    @Test
    void conversion_fromOffsetDateTime_normalizedToUTC() {
        // Input: OffsetDateTime.parse("2023-03-10T08:00:00+02:00")
        OffsetDateTime odt = OffsetDateTime.parse("2023-03-10T08:00:00+02:00");
        Timestamp ts = BigIntegerConversions.toTimestamp(odt, defaultConverter);
        assertNotNull(ts);
        // already normalized to UTC
        Instant expected = odt.toInstant();
        assertEquals(Timestamp.from(expected), ts);
    }

    @Test
    void conversion_fromZonedDateTime_toUTC() {
        // Input: ZonedDateTime.parse("2021-11-05T14:30:00-05:00[America/New_York]")
        ZonedDateTime zdt = ZonedDateTime.parse("2021-11-05T14:30:00-05:00[America/New_York]");
        Timestamp ts = BigIntegerConversions.toTimestamp(zdt, defaultConverter);
        assertNotNull(ts);
        // UTC instant
        Instant expected = zdt.toInstant();
        assertEquals(Timestamp.from(expected), ts);
    }

    @Test
    void conversion_fromLongEpochMillis() {
        // Input: 1625097600000L → 2021‑07‑01T00:00:00Z
        long epochMillis = 1625097600000L;
        Timestamp ts = BigIntegerConversions.toTimestamp(epochMillis, defaultConverter);
        assertNotNull(ts);
        Timestamp expected = Timestamp.from(Instant.ofEpochMilli(epochMillis));
        assertEquals(expected, ts);
    }

    @Test
    void conversion_fromBigIntegerEpochSeconds() {
        // Input: BigInteger.valueOf(1609459200L) → 2021‑01‑01T00:00:00Z
        BigInteger seconds = BigInteger.valueOf(1609459200L);
        Timestamp ts = BigIntegerConversions.toTimestamp(seconds, defaultConverter);
        assertNotNull(ts);
        Timestamp expected = Timestamp.from(Instant.ofEpochSecond(seconds.longValue()));
        assertEquals(expected, ts);
    }

    @Test
    void nullInput_handling() {
        assertThrows(NullPointerException.class, () -> BigIntegerConversions.toTimestamp(null, defaultConverter));
    }

    @Test
    void unsupportedType_withCustomConverter_returnsInstantFromConverter() {
        // Custom converter that returns Instant.now() for any request.
        Converter custom = new Converter(new ConverterOptions()) {

            @Override
            public Object convert(Object src, Class<?> target) {
                if (target == Instant.class) {
                    return Instant.parse("2022-12-31T23:59:59Z");
                }
                throw new IllegalArgumentException("Unsupported conversion");
            }
        };
        String unsupported = "unsupported-type";
        Timestamp ts = BigIntegerConversions.toTimestamp(unsupported, custom);
        assertNotNull(ts);
        assertEquals(Timestamp.from(Instant.parse("2022-12-31T23:59:59Z")), ts);
    }

    @Test
    void conversion_fromSqlTimestamp_identity() {
        Timestamp source = Timestamp.valueOf("2022-03-01 12:00:00");
        Timestamp ts = BigIntegerConversions.toTimestamp(source, defaultConverter);
        assertNotNull(ts);
        // The method should return a Timestamp representing the same instant (epoch millis).
        assertEquals(source.getTime(), ts.getTime());
    }

    @Test
    void conversion_fromSqlDate_toMidnightUTC() {
        // java.sql.Date represents midnight UTC of the given epoch millis.
        // 2021‑01‑01T00:00:00Z
        java.sql.Date sqlDate = new java.sql.Date(1609459200000L);
        Timestamp ts = BigIntegerConversions.toTimestamp(sqlDate, defaultConverter);
        assertNotNull(ts);
        Timestamp expected = Timestamp.from(Instant.ofEpochMilli(1609459200000L));
        assertEquals(expected, ts);
    }

    @Test
    void conversion_fromCalendar_preservesInstant() {
        // Calendar set to 2020‑06‑15T08:30:45.123Z
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.set(2020, Calendar.JUNE, 15, 8, 30, 45);
        cal.set(Calendar.MILLISECOND, 123);
        Timestamp ts = BigIntegerConversions.toTimestamp(cal, defaultConverter);
        assertNotNull(ts);
        Timestamp expected = new Timestamp(cal.getTimeInMillis());
        assertEquals(expected, ts);
    }

    @Test
    void conversion_fromNegativeLongEpochMillis() {
        // -86400000L → 1969‑12‑31T00:00:00Z
        long negative = -86_400_000L;
        Timestamp ts = BigIntegerConversions.toTimestamp(negative, defaultConverter);
        assertNotNull(ts);
        Timestamp expected = Timestamp.from(Instant.ofEpochMilli(negative));
        assertEquals(expected, ts);
    }

    @Test
    void conversion_fromVeryLargeBigInteger_upperInstantRange() {
        // 253402300799L seconds → 9999‑12‑31T23:59:59Z
        BigInteger seconds = BigInteger.valueOf(253402300799L);
        Timestamp ts = BigIntegerConversions.toTimestamp(seconds, defaultConverter);
        assertNotNull(ts);
        Timestamp expected = Timestamp.from(Instant.ofEpochSecond(seconds.longValue()));
        assertEquals(expected, ts);
    }

    @Test
    void conversion_fromOffsetTime_combinedWithCurrentDate() {
        // OffsetTime 13:45:30+02:00 → today at 11:45:30Z
        OffsetTime ot = OffsetTime.parse("13:45:30+02:00");
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        OffsetDateTime odt = ot.atDate(today);
        Timestamp ts = BigIntegerConversions.toTimestamp(ot, defaultConverter);
        assertNotNull(ts);
        Timestamp expected = Timestamp.from(odt.toInstant());
        assertEquals(expected, ts);
    }

    @Test
    void customConverterReturnsNull_forInstant_throwsNPE() {
        Converter custom = new Converter(new ConverterOptions()) {

            @Override
            public Object convert(Object src, Class<?> target) {
                // deliberately return null for any conversion
                return null;
            }
        };
        assertThrows(NullPointerException.class, () -> BigIntegerConversions.toTimestamp("any-string", custom));
    }

    @Test
    void customConverterThrowsException_propagates() {
        Converter custom = new Converter(new ConverterOptions()) {

            @Override
            public Object convert(Object src, Class<?> target) {
                throw new IllegalArgumentException("cannot convert");
            }
        };
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> BigIntegerConversions.toTimestamp("trigger-exception", custom));
        assertEquals("cannot convert", ex.getMessage());
    }

    @Test
    void unsupportedSource_withoutConverter_throwsIllegalArgumentException() {
        List<Integer> unsupported = new ArrayList<>();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> BigIntegerConversions.toTimestamp(unsupported, defaultConverter));
        // Message content is not asserted – only that the correct exception type is thrown.
    }

    @Test
    void conversion_fromIntegerEpochSeconds() {
        // 123456789 seconds → 1973‑11‑29T21:33:09Z
        Integer seconds = 123_456_789;
        Timestamp ts = BigIntegerConversions.toTimestamp(seconds, defaultConverter);
        assertNotNull(ts);
        Timestamp expected = Timestamp.from(Instant.ofEpochSecond(seconds.longValue()));
        assertEquals(expected, ts);
    }

    @Test
    void conversion_fromOutOfRangeBigInteger_throwsDateTimeException() {
        // far beyond Instant.MAX
        BigInteger outOfRange = BigInteger.valueOf(Long.MAX_VALUE);
        assertThrows(DateTimeException.class, () -> BigIntegerConversions.toTimestamp(outOfRange, defaultConverter));
    }
}
