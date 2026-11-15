package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.Date;
import java.sql.Timestamp;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toZonedDateTime_Test {

    /**
     * Converter used for all tests – created once with a known zone.
     */
    private static Converter converter;

    /**
     * Fixed zone used for deterministic assertions.
     */
    private static final ZoneId TEST_ZONE = ZoneId.of("UTC");

    /**
     * Default converter used by many tests – fixed to UTC.
     */
    private static Converter defaultConverter;

    /**
     * Fixed UTC zone used for deterministic assertions.
     */
    private static final ZoneId UTC = ZoneId.of("UTC");

    @BeforeAll
    static void setupBeforeAll() {
        // Build ConverterOptions with a deterministic zone.
        ConverterOptions options = new ConverterOptions();
        // ensure the converter uses UTC
        options.setZoneId(TEST_ZONE);
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static reference – not strictly necessary but keeps the pattern tidy.
        converter = null;
    }

    /**
     * Helper that converts an {@link Object} to {@link ZonedDateTime} using the class under test.
     */
    private static ZonedDateTime convert(Object source) {
        return NumberConversions.toZonedDateTime(source, defaultConverter);
    }

    /**
     * Helper to create a {@link Converter} that uses the supplied {@link ZoneId}.
     */
    private static Converter converterWithZone(ZoneId zone) {
        ConverterOptions opts = new ConverterOptions();
        opts.setZoneId(zone);
        return new Converter(opts);
    }

    private static Stream<ValidCase> validCasesProvider() {
        return Stream.of(// Date (epoch millis) → UTC
        new ValidCase(new Date(1672574400000L), UTC, "2023-01-01T12:00Z[UTC]"), // Instant → America/New_York (UTC‑5)
        new ValidCase(Instant.parse("2023-01-01T12:00:00Z"), ZoneId.of("America/New_York"), "2023-01-01T07:00-05:00[America/New_York]"), // Timestamp → UTC
        new ValidCase(Timestamp.valueOf("2023-01-01 12:00:00"), UTC, "2023-01-01T12:00Z[UTC]"), // Long epoch millis → UTC
        new ValidCase(1672574400000L, UTC, "2023-01-01T12:00Z[UTC]"), // ISO‑8601 string with +02:00 offset → UTC
        new ValidCase("2023-03-15T10:30:45+02:00", UTC, "2023-03-15T08:30:45Z[UTC]"), // ZonedDateTime source, converter zone Asia/Tokyo
        new ValidCase(ZonedDateTime.of(2023, 5, 20, 15, 45, 0, 0, ZoneId.of("Europe/Paris")), ZoneId.of("Asia/Tokyo"), "2023-05-20T22:45+09:00[Asia/Tokyo]"), // LocalDateTime with Europe/London zone
        new ValidCase(LocalDateTime.of(2023, 12, 31, 23, 0, 0), ZoneId.of("Europe/London"), "2023-12-31T23:00Z[Europe/London]"), // OffsetDateTime → America/Los_Angeles
        new ValidCase(OffsetDateTime.parse("2023-06-01T12:00:00+03:00"), ZoneId.of("America/Los_Angeles"), "2023-06-01T02:00-07:00[America/Los_Angeles]"), // Year → start of year UTC
        new ValidCase(Year.of(2025), UTC, "2025-01-01T00:00Z[UTC]"), // BigDecimal epoch seconds → UTC
        new ValidCase(BigDecimal.valueOf(1672574400L), UTC, "2023-01-01T12:00Z[UTC]"), // BigInteger epoch millis → UTC
        new ValidCase(BigInteger.valueOf(1672574400000L), UTC, "2023-01-01T12:00Z[UTC]"), // Duration of 5 hours → epoch+5h UTC
        new ValidCase(Duration.ofHours(5), UTC, "1970-01-01T05:00Z[UTC]"), // Calendar (epoch 0) → UTC
        new ValidCase(createZeroCalendar(TimeZone.getTimeZone("Asia/Kolkata")), UTC, "1970-01-01T00:00Z[UTC]"), // LocalDate start of day in America/New_York
        new ValidCase(LocalDate.of(2022, 2, 28), ZoneId.of("America/New_York"), "2022-02-28T00:00-05:00[America/New_York]"), // Leap‑day ISO string → UTC
        new ValidCase("2024-02-29T12:00:00Z", UTC, "2024-02-29T12:00Z[UTC]"), // String containing full zone information → same zone preserved
        new ValidCase("2023-10-29T01:30:00+02:00[Europe/Paris]", ZoneId.of("Europe/Paris"), "2023-10-29T01:30+02:00[Europe/Paris]"));
    }

    /**
     * Helper that creates a Calendar set to epoch (0) in the supplied {@link TimeZone}.
     */
    private static Calendar createZeroCalendar(TimeZone tz) {
        Calendar cal = Calendar.getInstance(tz);
        cal.setTimeInMillis(0L);
        return cal;
    }

    private static Stream<InvalidCase> invalidCasesProvider() {
        return Stream.of(// Null source → NullPointerException (the implementation dereferences source)
        new InvalidCase(null, UTC, NullPointerException.class), // Unparsable string → DateTimeParseException
        new InvalidCase("not-a-date", UTC, DateTimeParseException.class), // Unsupported type → IllegalArgumentException
        new InvalidCase(new Object(), UTC, IllegalArgumentException.class));
    }

    @Test
    void shouldConvert_javaUtilDate_toZonedDateTime() {
        // GIVEN
        Instant instant = Instant.parse("2023-03-15T12:34:56Z");
        Date date = Date.from(instant);
        // WHEN
        ZonedDateTime result = convert(date);
        // THEN
        ZonedDateTime expected = ZonedDateTime.ofInstant(instant, TEST_ZONE);
        assertEquals(expected, result);
    }

    @Test
    void shouldConvert_Instant_toZonedDateTime() {
        // GIVEN
        Instant instant = Instant.parse("2021-01-01T00:00:00Z");
        // WHEN
        ZonedDateTime result = convert(instant);
        // THEN
        ZonedDateTime expected = ZonedDateTime.ofInstant(instant, TEST_ZONE);
        assertEquals(expected, result);
    }

    @Test
    void shouldConvert_Timestamp_toZonedDateTime() {
        // GIVEN
        Instant instant = Instant.parse("2022-07-04T08:15:30Z");
        Timestamp timestamp = Timestamp.from(instant);
        // WHEN
        ZonedDateTime result = convert(timestamp);
        // THEN
        ZonedDateTime expected = ZonedDateTime.ofInstant(instant, TEST_ZONE);
        assertEquals(expected, result);
    }

    @Test
    void shouldConvert_LocalDateTime_toZonedDateTime_usingConverterZone() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2020, Month.DECEMBER, 31, 23, 59, 59);
        // The source LocalDateTime has no zone; the converter's zone (UTC) will be applied.
        Instant expectedInstant = ldt.atZone(TEST_ZONE).toInstant();
        // WHEN
        ZonedDateTime result = convert(ldt);
        // THEN
        ZonedDateTime expected = ZonedDateTime.ofInstant(expectedInstant, TEST_ZONE);
        assertEquals(expected, result);
    }

    @Test
    void shouldConvert_ZonedDateTime_toZonedDateTime_respectsConverterZone() {
        // GIVEN
        ZoneId sourceZone = ZoneId.of("America/New_York");
        ZonedDateTime source = ZonedDateTime.of(2019, 5, 20, 14, 30, 0, 0, sourceZone);
        // toInstant extracts the instant, then the converter's zone (UTC) is applied.
        Instant instant = source.toInstant();
        ZonedDateTime expected = ZonedDateTime.ofInstant(instant, TEST_ZONE);
        // WHEN
        ZonedDateTime result = convert(source);
        // THEN
        assertEquals(expected, result);
    }

    @Test
    void shouldReturnNull_whenSourceIsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        ZonedDateTime result = convert(source);
        // THEN
        assertNull(result, "Converting a null source should yield null");
    }

    @ParameterizedTest(name = "[{index}] convert {0} with zone {1}")
    @MethodSource("validCasesProvider")
    void shouldConvertVariousSources_toZonedDateTime(ValidCase tc) {
        // Arrange
        Converter conv = converterWithZone(tc.zone());
        // Act
        ZonedDateTime result = NumberConversions.toZonedDateTime(tc.source(), conv);
        // Assert
        ZonedDateTime expected = ZonedDateTime.parse(tc.expectedIso());
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] invalid conversion of {0}")
    @MethodSource("invalidCasesProvider")
    void shouldThrowWhenInvalidInputProvided(InvalidCase ic) {
        Converter conv = converterWithZone(ic.zone());
        assertThrows(ic.expectedException(), () -> NumberConversions.toZonedDateTime(ic.source(), conv));
    }

    @Test
    void shouldThrowWhenConverterIsNull() {
        Date date = new Date(1672574400000L);
        assertThrows(NullPointerException.class, () -> NumberConversions.toZonedDateTime(date, null));
    }
}
