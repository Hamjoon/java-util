package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toInstant_Test {

    /**
     * Converter used for all tests – created once for the whole test run.
     */
    private static Converter converter;

    /**
     * Preserve the original default time‑zone so we can restore it after the tests.
     */
    private static TimeZone originalDefaultTimeZone;

    @BeforeAll
    static void setupBeforeAll() {
        // Keep the original default zone and force UTC for deterministic results.
        originalDefaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        // Build a plain Converter (no custom options needed for these tests).
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore the original default time‑zone to avoid side‑effects on other tests.
        TimeZone.setDefault(originalDefaultTimeZone);
    }

    private static Stream<Arguments> provideSuccessfulConversions() {
        return Stream.of(// Instant → Instant (no conversion)
        Arguments.of("Instant should be returned unchanged", Instant.parse("2023-05-01T12:34:56.789Z"), Instant.parse("2023-05-01T12:34:56.789Z")), // java.util.Date → Instant
        Arguments.of("Date → Instant using epoch milliseconds", new Date(1_680_000_000_000L), Instant.ofEpochMilli(1_680_000_000_000L)), // java.sql.Timestamp → Instant
        Arguments.of("Timestamp → Instant using its time value", Timestamp.valueOf("2023-08-15 10:20:30.123"), Timestamp.valueOf("2023-08-15 10:20:30.123").toInstant()), // LocalDateTime → Instant (system default zone, forced to UTC)
        Arguments.of("LocalDateTime → Instant using system default zone (UTC)", LocalDateTime.of(2022, 12, 31, 23, 59, 59, 0), LocalDateTime.of(2022, 12, 31, 23, 59, 59, 0).atZone(ZoneId.systemDefault()).toInstant()), // ZonedDateTime → Instant
        Arguments.of("ZonedDateTime → Instant preserves the underlying instant", ZonedDateTime.of(2021, 7, 4, 14, 30, 0, 0, ZoneId.of("Europe/Paris")), ZonedDateTime.of(2021, 7, 4, 14, 30, 0, 0, ZoneId.of("Europe/Paris")).toInstant()), // OffsetDateTime → Instant
        Arguments.of("OffsetDateTime → Instant respects the offset", OffsetDateTime.of(2020, 2, 29, 6, 15, 0, 0, ZoneOffset.ofHours(-5)), OffsetDateTime.of(2020, 2, 29, 6, 15, 0, 0, ZoneOffset.ofHours(-5)).toInstant()), // Calendar (UTC) → Instant
        Arguments.of("Calendar (UTC) → Instant based on its epoch millis", utcCalendar(1_600_000_000_000L), Instant.ofEpochMilli(1_600_000_000_000L)), // Long epoch millis → Instant
        Arguments.of("Long epoch millis → Instant", Long.valueOf(1_500_000_000_000L), Instant.ofEpochMilli(1_500_000_000_000L)), // ISO‑8601 string with Z → Instant
        Arguments.of("ISO‑8601 string with Z suffix parses to Instant", "2024-01-15T08:45:30.123Z", Instant.parse("2024-01-15T08:45:30.123Z")));
    }

    private static Stream<Arguments> provideAdditionalSuccessfulConversions() {
        return Stream.of(// LocalDate → start of day in system default zone
        Arguments.of("LocalDate interpreted as start of day in system default zone", LocalDate.of(2023, 3, 15), LocalDate.of(2023, 3, 15).atStartOfDay(ZoneId.systemDefault()).toInstant()), // java.sql.Date → start of day in system default zone
        Arguments.of("java.sql.Date treated as start of day in system default zone", java.sql.Date.valueOf("2022-10-05"), LocalDate.of(2022, 10, 5).atStartOfDay(ZoneId.systemDefault()).toInstant()), // Calendar (America/New_York) → same epoch millis as UTC calendar
        Arguments.of("Calendar with non‑UTC zone yields same epoch‑millis Instant", newYorkCalendar(1_600_000_000_000L), Instant.ofEpochMilli(1_600_000_000_000L)), // ISO‑8601 string with explicit offset
        Arguments.of("ISO‑8601 string with +02:00 offset is parsed correctly", "2024-01-15T08:45:30+02:00", // 2‑hour offset removed
        Instant.parse("2024-01-15T06:45:30Z")), // ISO‑8601 string without zone → interpreted as LocalDateTime in system default zone
        Arguments.of("ISO‑8601 string without zone is interpreted as LocalDateTime in system default zone", "2024-01-15T08:45:30", LocalDateTime.parse("2024-01-15T08:45:30").atZone(ZoneId.systemDefault()).toInstant()), // Double epoch millis → Instant
        Arguments.of("Double representing epoch millis converts to the same Instant as Long", Double.valueOf(1_500_000_000_000.0), Instant.ofEpochMilli(1_500_000_000_000L)));
    }

    private static Stream<Arguments> provideConversionExceptions() {
        return Stream.of(// Null source object
        Arguments.of("null source object should throw NullPointerException", null, new Converter(new ConverterOptions()), NullPointerException.class), // Null converter
        Arguments.of("null Converter should throw NullPointerException", Instant.now(), null, NullPointerException.class), // java.sql.Time (time‑only) – unsupported
        Arguments.of("java.sql.Time cannot be converted to Instant", java.sql.Time.valueOf("12:34:56"), new Converter(new ConverterOptions()), ConversionException.class), // LocalTime – unsupported
        Arguments.of("LocalTime cannot be converted to Instant", LocalTime.of(12, 34, 56), new Converter(new ConverterOptions()), ConversionException.class), // AtomicLong – unsupported
        Arguments.of("AtomicLong is not a supported source type", new AtomicLong(1_600_000_000_000L), new Converter(new ConverterOptions()), ConversionException.class), // BigDecimal – unsupported
        Arguments.of("BigDecimal is not interpreted as epoch millis", new BigDecimal("1_600_000_000_000"), new Converter(new ConverterOptions()), ConversionException.class), // Invalid ISO‑8601 date (Feb 30)
        Arguments.of("Impossible date string should cause ConversionException", "2024-02-30T10:00:00Z", new Converter(new ConverterOptions()), ConversionException.class), // Invalid month (13)
        Arguments.of("Month 13 is invalid – ConversionException expected", "2024-13-01", new Converter(new ConverterOptions()), ConversionException.class), // Double.NaN – not a valid epoch millis
        Arguments.of("Double.NaN cannot be mapped to an Instant", Double.NaN, new Converter(new ConverterOptions()), ConversionException.class), // Double.POSITIVE_INFINITY – not a valid epoch millis
        Arguments.of("Infinite double value cannot be mapped to an Instant", Double.POSITIVE_INFINITY, new Converter(new ConverterOptions()), ConversionException.class), // Empty string – parsing fails
        Arguments.of("Empty string should cause ConversionException", "", new Converter(new ConverterOptions()), ConversionException.class), // Unsupported Character type
        Arguments.of("Character is unsupported – ConversionException expected", Character.valueOf('A'), new Converter(new ConverterOptions()), ConversionException.class));
    }

    /**
     * Creates a UTC {@link Calendar} set to the given epoch milliseconds.
     */
    private static Calendar utcCalendar(long epochMilli) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(epochMilli);
        return cal;
    }

    /**
     * Creates a {@link Calendar} in America/New_York zone set to the given epoch milliseconds.
     */
    private static Calendar newYorkCalendar(long epochMilli) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        cal.setTimeInMillis(epochMilli);
        return cal;
    }

    @Test
    void toInstant_fromDate_returnsSameInstant() {
        // 2021‑10‑01T00:00:00Z
        Date date = new Date(1_633_072_800_000L);
        Instant expected = date.toInstant();
        Instant actual = LocalDateConversions.toInstant(date, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toInstant_fromTimestamp_returnsSameInstant() {
        Timestamp ts = new Timestamp(1_633_072_800_000L);
        Instant expected = ts.toInstant();
        Instant actual = LocalDateConversions.toInstant(ts, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toInstant_fromLocalDateTime_usesSystemDefaultZone() {
        // With the default zone forced to UTC the conversion is deterministic.
        LocalDateTime ldt = LocalDateTime.of(2021, 10, 1, 12, 30, 45);
        Instant expected = ldt.atZone(ZoneOffset.UTC).toInstant();
        Instant actual = LocalDateConversions.toInstant(ldt, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toInstant_fromZonedDateTime_preservesInstant() {
        ZonedDateTime zdt = ZonedDateTime.of(2021, 10, 1, 12, 30, 45, 0, ZoneId.of("America/New_York"));
        Instant expected = zdt.toInstant();
        Instant actual = LocalDateConversions.toInstant(zdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toInstant_fromOffsetDateTime_preservesInstant() {
        OffsetDateTime odt = OffsetDateTime.of(2021, 10, 1, 12, 30, 45, 0, ZoneOffset.ofHours(-5));
        Instant expected = odt.toInstant();
        Instant actual = LocalDateConversions.toInstant(odt, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toInstant_fromLongEpochMilli_returnsCorrespondingInstant() {
        // 2021‑10‑01T00:00:00Z
        Long epochMilli = 1_633_072_800_000L;
        Instant expected = Instant.ofEpochMilli(epochMilli);
        Instant actual = LocalDateConversions.toInstant(epochMilli, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toInstant_fromCalendar_returnsSameInstant() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(1_633_072_800_000L);
        Instant expected = cal.toInstant();
        Instant actual = LocalDateConversions.toInstant(cal, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toInstant_fromNull_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toInstant(null, converter));
    }

    @Test
    void toInstant_fromUnsupportedType_throwsIllegalArgumentException() {
        // No conversion rule for plain Object → Instant
        Object unsupported = new Object();
        assertThrows(IllegalArgumentException.class, () -> LocalDateConversions.toInstant(unsupported, converter));
    }

    /**
     * Parameterized test covering the main positive conversion scenarios described
     * in the JSON test data (group “toInstant conversion tests”).
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideSuccessfulConversions")
    void toInstant_successful(String description, Object from, Instant expected) {
        Instant actual = LocalDateConversions.toInstant(from, converter);
        assertEquals(expected, actual, description);
    }

    /**
     * Parameterized test covering the “additional toInstant conversion tests” group.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideAdditionalSuccessfulConversions")
    void toInstant_additionalSuccess(String description, Object from, Instant expected) {
        Instant actual = LocalDateConversions.toInstant(from, converter);
        assertEquals(expected, actual, description);
    }

    /**
     * Parameterized test covering the “toInstant exception tests” group.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideConversionExceptions")
    void toInstant_exceptions(String description, Object from, Converter conv, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> LocalDateConversions.toInstant(from, conv), description);
    }
}
