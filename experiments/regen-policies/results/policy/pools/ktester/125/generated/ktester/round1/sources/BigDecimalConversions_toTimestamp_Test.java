package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.Timestamp;
import java.time.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_toTimestamp_Test {

    /**
     * Shared {@link Converter} instance used for all tests.
     */
    private static Converter converter;

    /**
     * Original default zone – restored after all tests.
     */
    private static ZoneId originalDefaultZone;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter has a public constructor that requires a ConverterOptions instance.
        // Using the default options is sufficient for the built‑in conversions we need.
        converter = new Converter(new ConverterOptions());
        // Force the default zone to UTC so that conversions that rely on the system default
        // zone (e.g., LocalDateTime) behave deterministically.
        originalDefaultZone = ZoneId.systemDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore the original default zone.
        TimeZone.setDefault(TimeZone.getTimeZone(originalDefaultZone));
        converter = null;
    }

    private static Stream<Arguments> validConversionProvider() {
        // 2023‑01‑01T12:34:56Z
        Instant instant = Instant.ofEpochMilli(1672574096000L);
        Timestamp tsFromInstant = Timestamp.from(instant);
        // 1. java.util.Date
        Date date = new Date(1672574096000L);
        // 2. java.time.Instant (same as above)
        // 3. LocalDateTime (system default zone forced to UTC)
        LocalDateTime ldt = LocalDateTime.of(2023, 1, 1, 12, 34, 56);
        Timestamp tsFromLdt = Timestamp.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
        // 4. ZonedDateTime UTC
        ZonedDateTime zdtUtc = ZonedDateTime.of(2023, 1, 1, 12, 34, 56, 0, ZoneOffset.UTC);
        Timestamp tsFromZdtUtc = Timestamp.from(zdtUtc.toInstant());
        // 5. Timestamp with nanos
        Timestamp sourceTsWithNanos = Timestamp.valueOf("2023-01-01 12:34:56.123456789");
        // 6. Long epoch millis
        Long longMillis = 1672574096000L;
        // 7. BigDecimal seconds.nanos (seconds part = 1672574096, nanos = 123456789)
        BigDecimal bd = new BigDecimal("1672574096.123456789");
        Timestamp tsFromBd = Timestamp.from(Instant.ofEpochSecond(1672574096L, 123456789L));
        return Stream.of(Arguments.of(date, tsFromInstant, "Date → Timestamp (preserve instant)"), Arguments.of(instant, tsFromInstant, "Instant → Timestamp (identity)"), Arguments.of(ldt, tsFromLdt, "LocalDateTime → Timestamp (system default zone = UTC)"), Arguments.of(zdtUtc, tsFromZdtUtc, "ZonedDateTime UTC → Timestamp"), Arguments.of(sourceTsWithNanos, sourceTsWithNanos, "Timestamp (already) → Timestamp (same nanos)"), Arguments.of(longMillis, tsFromInstant, "Long epoch millis → Timestamp"), Arguments.of(bd, tsFromBd, "BigDecimal seconds.nanos → Timestamp"));
    }

    private static Stream<Arguments> additionalValidConversionProvider() {
        // java.sql.Date (stores only date, time = 00:00:00)
        java.sql.Date sqlDate = new java.sql.Date(1672574096000L);
        Timestamp tsFromSqlDate = Timestamp.from(sqlDate.toInstant());
        // LocalDate → start of day UTC
        LocalDate localDate = LocalDate.of(2023, 1, 1);
        Timestamp tsFromLocalDate = Timestamp.from(localDate.atStartOfDay(ZoneOffset.UTC).toInstant());
        // LocalTime → epoch date (1970‑01‑01) + time
        LocalTime localTime = LocalTime.of(12, 34, 56, 123456789);
        Instant instantFromLocalTime = LocalDate.ofEpochDay(0).atTime(localTime).toInstant(ZoneOffset.UTC);
        Timestamp tsFromLocalTime = Timestamp.from(instantFromLocalTime);
        // OffsetDateTime +02:00 → UTC instant
        OffsetDateTime odt = OffsetDateTime.of(2023, 1, 1, 12, 34, 56, 0, ZoneOffset.ofHours(2));
        Timestamp tsFromOdt = Timestamp.from(odt.toInstant());
        // ZonedDateTime America/New_York (UTC‑5) → UTC instant
        ZonedDateTime zdtNy = ZonedDateTime.of(2023, 1, 1, 12, 34, 56, 0, ZoneId.of("America/New_York"));
        Timestamp tsFromZdtNy = Timestamp.from(zdtNy.toInstant());
        // Calendar set to known epoch millis
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(1672574096000L);
        Timestamp tsFromCal = Timestamp.from(cal.toInstant());
        // BigInteger epoch millis
        BigInteger bi = new BigInteger("1672574096000");
        Timestamp tsFromBigInt = Timestamp.from(Instant.ofEpochMilli(bi.longValue()));
        // Negative epoch millis (one second before epoch)
        Long negativeMillis = -1000L;
        Timestamp tsFromNegative = Timestamp.from(Instant.ofEpochMilli(negativeMillis));
        return Stream.of(Arguments.of(sqlDate, tsFromSqlDate, "java.sql.Date → Timestamp (midnight UTC)"), Arguments.of(localDate, tsFromLocalDate, "LocalDate → Timestamp (start of day UTC)"), Arguments.of(localTime, tsFromLocalTime, "LocalTime → Timestamp (epoch date + time)"), Arguments.of(odt, tsFromOdt, "OffsetDateTime +02:00 → Timestamp (UTC)"), Arguments.of(zdtNy, tsFromZdtNy, "ZonedDateTime America/New_York → Timestamp (UTC)"), Arguments.of(cal, tsFromCal, "Calendar (UTC) → Timestamp"), Arguments.of(bi, tsFromBigInt, "BigInteger epoch millis → Timestamp"), Arguments.of(negativeMillis, tsFromNegative, "Negative epoch millis → Timestamp before 1970"));
    }

    private static Stream<Arguments> invalidInputProvider() {
        return Stream.of(Arguments.of(null, NullPointerException.class, "null source → NPE"), Arguments.of(Boolean.TRUE, IllegalArgumentException.class, "Boolean source → IAE"), Arguments.of("2023-01-01T12:34:56Z", IllegalArgumentException.class, "Raw ISO‑8601 String → IAE"));
    }

    private static Stream<Arguments> numericEdgeCaseProvider() {
        return Stream.of(Arguments.of(Long.valueOf(Long.MAX_VALUE), DateTimeException.class, "Long.MAX_VALUE epoch millis → DateTimeException"), Arguments.of(Double.NaN, IllegalArgumentException.class, "Double.NaN → IllegalArgumentException"), Arguments.of(Double.POSITIVE_INFINITY, IllegalArgumentException.class, "Double.POSITIVE_INFINITY → IllegalArgumentException"));
    }

    @Test
    void toTimestamp_fromInstant_returnsSameInstant() {
        // GIVEN
        // Timestamp has millisecond precision
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        // WHEN
        Timestamp ts = BigDecimalConversions.toTimestamp(now, converter);
        // THEN
        assertNotNull(ts, "Timestamp should not be null");
        assertEquals(now.toEpochMilli(), ts.getTime(), "Timestamp milliseconds must match the source Instant");
    }

    @Test
    void toTimestamp_fromDate_returnsSameEpochMilli() {
        // GIVEN
        // current time
        Date date = new Date();
        long expectedMillis = date.getTime();
        // WHEN
        Timestamp ts = BigDecimalConversions.toTimestamp(date, converter);
        // THEN
        assertNotNull(ts);
        assertEquals(expectedMillis, ts.getTime(), "Timestamp milliseconds must match the source Date");
    }

    @Test
    void toTimestamp_fromLocalDateTime_assumesSystemDefaultZone() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2023, Month.MARCH, 15, 12, 30, 45, 123_000_000);
        ZoneId zone = ZoneId.systemDefault();
        Instant expected = ldt.atZone(zone).toInstant();
        // WHEN
        Timestamp ts = BigDecimalConversions.toTimestamp(ldt, converter);
        // THEN
        assertNotNull(ts);
        assertEquals(expected.toEpochMilli(), ts.getTime(), "Timestamp should represent the same instant as the LocalDateTime in the system default zone");
    }

    @Test
    void toTimestamp_fromOffsetDateTime_usesProvidedOffset() {
        // GIVEN
        OffsetDateTime odt = OffsetDateTime.of(2023, 5, 10, 8, 15, 30, 500_000_000, ZoneOffset.ofHours(-4));
        Instant expected = odt.toInstant();
        // WHEN
        Timestamp ts = BigDecimalConversions.toTimestamp(odt, converter);
        // THEN
        assertNotNull(ts);
        assertEquals(expected.toEpochMilli(), ts.getTime(), "Timestamp should represent the same instant as the OffsetDateTime");
    }

    @Test
    void toTimestamp_fromString_parsesAsNumberOfSeconds() {
        // GIVEN
        // The library treats a numeric string as a number of seconds (or milliseconds) depending on its size.
        // Using a clear value that fits in seconds.
        // 2021‑07‑01T00:00:00Z
        String seconds = "1625097600";
        Instant expected = Instant.ofEpochSecond(Long.parseLong(seconds));
        // WHEN
        Timestamp ts = BigDecimalConversions.toTimestamp(seconds, converter);
        // THEN
        assertNotNull(ts);
        assertEquals(expected.toEpochMilli(), ts.getTime(), "String containing epoch seconds should be converted to the corresponding Timestamp");
    }

    @Test
    void toTimestamp_nullInput_throwsNullPointerException() {
        // GIVEN
        Object nullObj = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> BigDecimalConversions.toTimestamp(nullObj, converter), "Converting a null source should result in NPE because Timestamp.from(null) is illegal");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validConversionProvider")
    void toTimestamp_validConversions(Object from, Timestamp expected, String description) {
        Timestamp actual = BigDecimalConversions.toTimestamp(from, converter);
        assertNotNull(actual, "Resulting Timestamp must not be null");
        assertEquals(expected.getTime(), actual.getTime(), "Milliseconds part differs for case: " + description);
        // Timestamp stores nanoseconds separately; verify full precision when applicable.
        assertEquals(expected.getNanos(), actual.getNanos(), "Nanoseconds part differs for case: " + description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("additionalValidConversionProvider")
    void toTimestamp_additionalValidConversions(Object from, Timestamp expected, String description) {
        Timestamp actual = BigDecimalConversions.toTimestamp(from, converter);
        assertNotNull(actual);
        assertEquals(expected.getTime(), actual.getTime(), "Milliseconds differ for case: " + description);
        assertEquals(expected.getNanos(), actual.getNanos(), "Nanoseconds differ for case: " + description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("invalidInputProvider")
    void toTimestamp_invalidInputs(Object from, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> BigDecimalConversions.toTimestamp(from, null), "Expected exception for case: " + description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("numericEdgeCaseProvider")
    void toTimestamp_numericEdgeCases(Object from, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> BigDecimalConversions.toTimestamp(from, null), "Expected exception for case: " + description);
    }

    @Test
    void toTimestamp_localDateTimeDuringDstGap_throwsDateTimeException() {
        // Europe/Paris has a DST gap on 2023‑03‑26 02:00 → 03:00.
        // Force the default zone to Europe/Paris for this specific test.
        ZoneId previous = ZoneId.systemDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Paris"));
            LocalDateTime gapLdt = LocalDateTime.of(2023, 3, 26, 2, 30);
            assertThrows(DateTimeException.class, () -> BigDecimalConversions.toTimestamp(gapLdt, null), "LocalDateTime in DST gap should raise DateTimeException");
        } finally {
            // Restore original default zone.
            TimeZone.setDefault(TimeZone.getTimeZone(previous));
        }
    }

    @Test
    void toTimestamp_unsupportedObject_throwsIllegalArgumentException() {
        Object unsupported = new Object();
        assertThrows(IllegalArgumentException.class, () -> BigDecimalConversions.toTimestamp(unsupported, null), "Arbitrary Object should trigger IllegalArgumentException");
    }
}
