package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigIntegerConversions_toOffsetDateTime_Test {

    /**
     * Shared Converter instance used by all tests.
     */
    private static Converter converter;

    /**
     * Original default zone – saved before each test and restored afterwards.
     */
    private static ZoneId originalDefaultZone;

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – the Converter comes with all standard conversions.
        converter = new Converter(new ConverterOptions());
    }

    @BeforeEach
    void setupBeforeEach() {
        // Force UTC to make all zone‑dependent expectations deterministic.
        originalDefaultZone = ZoneId.systemDefault();
        ZoneId utc = ZoneOffset.UTC;
        System.setProperty("user.timezone", utc.getId());
        TimeZone.setDefault(TimeZone.getTimeZone(utc));
    }

    @AfterEach
    void teardownAfterEach() {
        // Restore the original default zone to avoid side‑effects on other tests.
        System.setProperty("user.timezone", originalDefaultZone.getId());
        TimeZone.setDefault(TimeZone.getTimeZone(originalDefaultZone));
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally.
        converter = null;
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> validConversionProvider() {
        ZoneId utc = ZoneOffset.UTC;
        // will be UTC because of @BeforeEach
        ZoneId system = ZoneId.systemDefault();
        // ZonedDateTime (already has zone/offset)
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 10, 12, 30, 0, 0, ZoneOffset.ofHours(-5));
        // Instant (interpreted as UTC)
        Instant instant = Instant.parse("2023-05-10T17:30:00Z");
        // java.util.Date (interpreted in default zone, which is UTC)
        Date date = Date.from(Instant.parse("2023-05-10T17:30:00Z"));
        // java.sql.Timestamp (same as Date)
        Timestamp ts = Timestamp.from(Instant.parse("2023-05-10T17:30:00Z"));
        // Long epoch‑millis
        // 2023‑05‑10T17:30:00Z
        Long epochLong = 1683736200000L;
        // ISO‑8601 string with offset
        String isoOffset = "2023-05-10T12:30:00-05:00";
        // ISO‑8601 string without offset (treated as LocalDateTime in default zone)
        String isoNoOffset = "2023-05-10T12:30:00";
        // Date‑only string
        String dateOnly = "2023-05-10";
        // LocalDateTime (treated as default zone)
        LocalDateTime ldt = LocalDateTime.of(2023, 5, 10, 12, 30, 0);
        // LocalDate (start of day in default zone)
        LocalDate ld = LocalDate.of(2023, 5, 10);
        // OffsetDateTime (should be returned unchanged)
        OffsetDateTime odt = OffsetDateTime.of(2023, 5, 10, 12, 30, 0, 0, ZoneOffset.ofHours(-5));
        // OffsetTime (epoch date 1970‑01‑01 with same offset)
        OffsetTime ot = OffsetTime.of(12, 30, 0, 0, ZoneOffset.ofHours(-5));
        // Calendar (UTC)
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(1683736200000L);
        // java.sql.Date (date‑only, start of day)
        java.sql.Date sqlDate = java.sql.Date.valueOf("2023-05-10");
        // BigInteger epoch‑millis
        BigInteger bigInt = new BigInteger("1683736200000");
        // BigDecimal epoch‑millis
        BigDecimal bigDec = new BigDecimal("1683736200000");
        return Stream.of(// ZonedDateTime
        org.junit.jupiter.params.provider.Arguments.of(zdt, zdt.toOffsetDateTime()), // Instant
        org.junit.jupiter.params.provider.Arguments.of(instant, instant.atOffset(ZoneOffset.UTC)), // java.util.Date
        org.junit.jupiter.params.provider.Arguments.of(date, date.toInstant().atOffset(ZoneOffset.UTC)), // java.sql.Timestamp
        org.junit.jupiter.params.provider.Arguments.of(ts, ts.toInstant().atOffset(ZoneOffset.UTC)), // Long epoch‑millis
        org.junit.jupiter.params.provider.Arguments.of(epochLong, OffsetDateTime.ofInstant(Instant.ofEpochMilli(epochLong), system)), // ISO‑8601 with offset
        org.junit.jupiter.params.provider.Arguments.of(isoOffset, OffsetDateTime.parse(isoOffset)), // ISO‑8601 without offset
        org.junit.jupiter.params.provider.Arguments.of(isoNoOffset, LocalDateTime.parse(isoNoOffset).atZone(system).toOffsetDateTime()), // Date‑only string
        org.junit.jupiter.params.provider.Arguments.of(dateOnly, LocalDate.parse(dateOnly).atStartOfDay(system).toOffsetDateTime()), // LocalDateTime
        org.junit.jupiter.params.provider.Arguments.of(ldt, ldt.atZone(system).toOffsetDateTime()), // LocalDate
        org.junit.jupiter.params.provider.Arguments.of(ld, ld.atStartOfDay(system).toOffsetDateTime()), // OffsetDateTime (identity)
        org.junit.jupiter.params.provider.Arguments.of(odt, odt), // OffsetTime
        org.junit.jupiter.params.provider.Arguments.of(ot, ot.atDate(LocalDate.ofEpochDay(0)).toOffsetDateTime()), // Calendar
        org.junit.jupiter.params.provider.Arguments.of(cal, OffsetDateTime.ofInstant(Instant.ofEpochMilli(cal.getTimeInMillis()), utc)), // java.sql.Date
        org.junit.jupiter.params.provider.Arguments.of(sqlDate, sqlDate.toLocalDate().atStartOfDay(system).toOffsetDateTime()), // BigInteger
        org.junit.jupiter.params.provider.Arguments.of(bigInt, OffsetDateTime.ofInstant(Instant.ofEpochMilli(bigInt.longValue()), system)), // BigDecimal
        org.junit.jupiter.params.provider.Arguments.of(bigDec, OffsetDateTime.ofInstant(Instant.ofEpochMilli(bigDec.longValue()), system)));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> invalidConversionProvider() {
        // Unsupported source types and malformed data
        return Stream.of(// null source – per specification this should throw NPE
        org.junit.jupiter.params.provider.Arguments.of(null, NullPointerException.class), // Boolean (unsupported)
        org.junit.jupiter.params.provider.Arguments.of(Boolean.TRUE, ConversionException.class), // UUID (unsupported)
        org.junit.jupiter.params.provider.Arguments.of(UUID.randomUUID(), ConversionException.class), // LocalTime (unsupported)
        org.junit.jupiter.params.provider.Arguments.of(LocalTime.of(12, 30, 0), ConversionException.class), // java.sql.Time (unsupported)
        org.junit.jupiter.params.provider.Arguments.of(java.sql.Time.valueOf("12:30:00"), ConversionException.class), // Malformed ISO‑8601 string
        org.junit.jupiter.params.provider.Arguments.of("2023-13-40T25:61:00", ConversionException.class), // Empty string
        org.junit.jupiter.params.provider.Arguments.of("", ConversionException.class), // Numeric string overflow
        org.junit.jupiter.params.provider.Arguments.of("9223372036854775808", ConversionException.class), // Double epoch‑millis (unsupported numeric type)
        org.junit.jupiter.params.provider.Arguments.of(1683736200000.0d, ConversionException.class), // BigDecimal with fractional milliseconds
        org.junit.jupiter.params.provider.Arguments.of(new BigDecimal("1683736200.123"), ConversionException.class), // Extremely large BigInteger (overflow)
        org.junit.jupiter.params.provider.Arguments.of(new BigInteger("9223372036854775808"), ConversionException.class), // Negative epoch‑millis Long (valid conversion, but we include as success elsewhere)
        // Null converter – should NPE before any conversion logic runs
        org.junit.jupiter.params.provider.Arguments.of(Instant.parse("2023-05-10T12:00:00Z"), NullPointerException.class));
    }

    /**
     * Verify that a {@link ZonedDateTime} is correctly converted to an {@link OffsetDateTime}.
     * The expected result is the same instant with the original offset retained.
     */
    @Test
    void testToOffsetDateTime_fromZonedDateTime() {
        // GIVEN
        ZonedDateTime source = ZonedDateTime.of(2023, 5, 15, // 123 ms
        10, // 123 ms
        30, // 123 ms
        45, // 123 ms
        123_000_000, // +02:00
        ZoneOffset.ofHoursMinutes(2, 0));
        // WHEN
        OffsetDateTime result = BigIntegerConversions.toOffsetDateTime(source, converter);
        // THEN
        assertNotNull(result, "Result must not be null");
        assertEquals(source.toOffsetDateTime(), result, "OffsetDateTime derived from ZonedDateTime should match expected value");
    }

    /**
     * Verify conversion from an {@link Instant}.  The {@link Converter} should interpret the
     * instant in the system default time‑zone, then the method should return the corresponding
     * {@link OffsetDateTime}.  To make the test deterministic we explicitly set the default
     * zone to UTC for the duration of the test.
     */
    @Test
    void testToOffsetDateTime_fromInstant() {
        // GIVEN
        ZoneId originalZone = ZoneId.systemDefault();
        try {
            // Force UTC to have a predictable offset (zero)
            ZoneId utc = ZoneOffset.UTC;
            System.setProperty("user.timezone", utc.getId());
            TimeZone.setDefault(TimeZone.getTimeZone(utc));
            Instant instant = Instant.parse("2023-01-01T12:00:00Z");
            // WHEN
            OffsetDateTime result = BigIntegerConversions.toOffsetDateTime(instant, converter);
            // THEN
            OffsetDateTime expected = instant.atOffset(ZoneOffset.UTC);
            assertNotNull(result, "Result must not be null");
            assertEquals(expected, result, "OffsetDateTime derived from Instant should be UTC offset");
        } finally {
            // Restore original default zone to avoid side effects on other tests
            System.setProperty("user.timezone", originalZone.getId());
            TimeZone.setDefault(TimeZone.getTimeZone(originalZone));
        }
    }

    /**
     * Verify that a {@link Date} (java.util) is converted correctly.
     * The expected {@link OffsetDateTime} is derived from the date's instant in the
     * system default zone (again forced to UTC for determinism).
     */
    @Test
    void testToOffsetDateTime_fromDate() {
        // GIVEN
        ZoneId originalZone = ZoneId.systemDefault();
        try {
            ZoneId utc = ZoneOffset.UTC;
            System.setProperty("user.timezone", utc.getId());
            TimeZone.setDefault(TimeZone.getTimeZone(utc));
            Date date = Date.from(Instant.parse("2022-12-31T23:59:59Z"));
            // WHEN
            OffsetDateTime result = BigIntegerConversions.toOffsetDateTime(date, converter);
            // THEN
            OffsetDateTime expected = date.toInstant().atOffset(ZoneOffset.UTC);
            assertNotNull(result);
            assertEquals(expected, result);
        } finally {
            System.setProperty("user.timezone", originalZone.getId());
            TimeZone.setDefault(TimeZone.getTimeZone(originalZone));
        }
    }

    /**
     * Ensure that passing {@code null} returns {@code null} rather than throwing.
     * The conversion chain in {@link BigIntegerConversions} treats a null source as a null result.
     */
    @Test
    void testToOffsetDateTime_nullInput() {
        // GIVEN
        Object source = null;
        // WHEN
        OffsetDateTime result = BigIntegerConversions.toOffsetDateTime(source, converter);
        // THEN
        assertNull(result, "Converting a null source should yield null");
    }

    @ParameterizedTest(name = "{index} => source={0}")
    @MethodSource("validConversionProvider")
    void testToOffsetDateTime_valid(Object source, OffsetDateTime expected) {
        // WHEN
        OffsetDateTime result = BigIntegerConversions.toOffsetDateTime(source, converter);
        // THEN
        assertNotNull(result, "Result must not be null for source: " + source);
        assertEquals(expected, result, () -> "Converted OffsetDateTime does not match expected for source: " + source);
    }

    @ParameterizedTest(name = "{index} => source={0}, expectedException={1}")
    @MethodSource("invalidConversionProvider")
    void testToOffsetDateTime_invalid(Object source, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        assertThrows(expectedException, () -> BigIntegerConversions.toOffsetDateTime(source, converter), () -> "Expected " + expectedException.getSimpleName() + " for source: " + source);
    }

    @Test
    void testToOffsetDateTime_nullConverter() {
        // GIVEN
        Instant instant = Instant.parse("2023-05-10T12:00:00Z");
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> BigIntegerConversions.toOffsetDateTime(instant, null), "Passing a null Converter should throw NullPointerException");
    }
}
