package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.sql.Timestamp;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_toLocalDateTime_Test {

    /**
     * Shared {@link Converter} instance used by all test cases.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter can be instantiated with default options – it is not used by the
        // conversion logic for the types we test, but the method signature requires it.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly necessary but keeps the pattern tidy.
        converter = null;
    }

    private static Stream<Arguments> validConversionsProvider() {
        ZoneId system = ZoneId.systemDefault();
        // java.util.Date representing the epoch
        Date epochDate = new Date(0L);
        LocalDateTime epochLdt = LocalDateTime.ofInstant(Instant.EPOCH, system);
        // LocalDateTime instance
        LocalDateTime ldt = LocalDateTime.of(2023, 5, 17, 14, 30, 45, 123_000_000);
        // ZonedDateTime with Europe/Paris zone
        ZonedDateTime parisZdt = ZonedDateTime.of(2022, 12, 31, 23, 59, 59, 0, ZoneId.of("Europe/Paris"));
        LocalDateTime parisLdt = parisZdt.toLocalDateTime();
        // OffsetDateTime with -05:00 offset
        OffsetDateTime offsetMinus5 = OffsetDateTime.of(2021, 7, 4, 9, 15, 0, 0, ZoneOffset.ofHours(-5));
        LocalDateTime offsetMinus5Ldt = offsetMinus5.toLocalDateTime();
        // java.sql.Timestamp
        Timestamp ts = Timestamp.valueOf("2020-01-01 12:00:00");
        LocalDateTime tsLdt = ts.toLocalDateTime();
        // Instant parsed from string
        Instant instant = Instant.parse("2024-03-15T08:20:30Z");
        LocalDateTime instantLdt = instant.atZone(system).toLocalDateTime();
        // Calendar UTC
        Calendar calUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calUtc.setTimeInMillis(1_600_000_000_000L);
        LocalDateTime calUtcLdt = LocalDateTime.ofInstant(Instant.ofEpochMilli(1_600_000_000_000L), system);
        // ISO‑8601 string without offset
        String isoString = "2022-08-10T16:45:00";
        LocalDateTime isoLdt = LocalDateTime.parse(isoString);
        // Calendar America/New_York
        Calendar calNy = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        calNy.setTimeInMillis(1_600_000_000_000L);
        LocalDateTime calNyLdt = LocalDateTime.ofInstant(Instant.ofEpochMilli(1_600_000_000_000L), ZoneId.of("America/New_York"));
        // java.sql.Date (date‑only)
        java.sql.Date sqlDate = java.sql.Date.valueOf("2021-12-25");
        LocalDateTime sqlDateLdt = LocalDateTime.ofInstant(Instant.ofEpochMilli(sqlDate.getTime()), system);
        // LocalDate
        LocalDate localDate = LocalDate.of(2022, 9, 30);
        LocalDateTime localDateLdt = LocalDateTime.of(localDate, LocalTime.MIDNIGHT);
        // LocalTime
        LocalTime localTime = LocalTime.of(13, 45, 20, 500_000_000);
        LocalDateTime localTimeLdt = LocalDateTime.of(LocalDate.ofEpochDay(0), localTime);
        // OffsetDateTime with +14 offset
        OffsetDateTime offsetPlus14 = OffsetDateTime.of(2023, 3, 10, 22, 15, 0, 0, ZoneOffset.ofHours(14));
        LocalDateTime offsetPlus14Ldt = offsetPlus14.toLocalDateTime();
        // ZonedDateTime UTC
        ZonedDateTime utcZdt = ZonedDateTime.ofInstant(Instant.parse("2024-01-01T00:00:00Z"), ZoneId.of("UTC"));
        LocalDateTime utcZdtLdt = utcZdt.toLocalDateTime();
        // ISO‑8601 string with offset (offset ignored)
        String isoWithOffset = "2022-08-10T16:45:00+02:00";
        LocalDateTime isoWithOffsetLdt = LocalDateTime.of(2022, 8, 10, 16, 45, 0);
        return Stream.of(Arguments.of(epochDate, epochLdt, "Convert java.util.Date representing the epoch (0 ms) to LocalDateTime using the system default time‑zone."), Arguments.of(ldt, ldt, "Pass a LocalDateTime instance directly; the method should return the same value."), Arguments.of(parisZdt, parisLdt, "Convert a ZonedDateTime with a non‑UTC zone; the returned LocalDateTime should be the zone‑local part."), Arguments.of(offsetMinus5, offsetMinus5Ldt, "Convert an OffsetDateTime; the offset is ignored and only the local date‑time part is kept."), Arguments.of(ts, tsLdt, "Convert a java.sql.Timestamp to LocalDateTime."), Arguments.of(instant, instantLdt, "Convert an Instant to LocalDateTime using the system default zone."), Arguments.of(calUtc, calUtcLdt, "Convert a java.util.Calendar (UTC) to LocalDateTime."), Arguments.of(isoString, isoLdt, "Parse an ISO‑8601 date‑time string into LocalDateTime."), Arguments.of(calNy, calNyLdt, "Convert a java.util.Calendar that uses a non‑UTC time‑zone (America/New_York)."), Arguments.of(sqlDate, sqlDateLdt, "Convert a java.sql.Date (date‑only) to LocalDateTime (midnight in system default zone)."), Arguments.of(localDate, localDateLdt, "Convert a LocalDate to LocalDateTime at midnight."), Arguments.of(localTime, localTimeLdt, "Convert a LocalTime to LocalDateTime using the epoch date (1970‑01‑01)."), Arguments.of(offsetPlus14, offsetPlus14Ldt, "Convert an OffsetDateTime with the maximum positive offset (+14)."), Arguments.of(utcZdt, utcZdtLdt, "Convert a ZonedDateTime that is already in UTC."), Arguments.of(isoWithOffset, isoWithOffsetLdt, "Parse an ISO‑8601 string that includes an offset; offset is discarded."));
    }

    private static Stream<Arguments> errorCasesProvider() {
        return Stream.of(Arguments.of(null, NullPointerException.class, "Passing a null source object should result in a NullPointerException."), Arguments.of("not-a-date-time", DateTimeParseException.class, "An invalid ISO‑8601 string should cause a DateTimeParseException."), Arguments.of(new Object(), IllegalArgumentException.class, "A source object of an unsupported type should trigger an IllegalArgumentException."));
    }

    private static Stream<Arguments> boundaryExceptionProvider() {
        return Stream.of(Arguments.of("", DateTimeParseException.class, "An empty string cannot be parsed as a date‑time and should cause a DateTimeParseException."), Arguments.of("2022-02-30T10:00:00", DateTimeParseException.class, "A string with an impossible calendar date (February 30) should trigger a DateTimeParseException."), Arguments.of(new Date(Long.MAX_VALUE), DateTimeException.class, "Date representing the maximum long value is outside the range supported by LocalDateTime."), Arguments.of(new Date(Long.MIN_VALUE), DateTimeException.class, "Date representing the minimum long value is outside the range supported by LocalDateTime."), Arguments.of(Instant.MAX, DateTimeException.class, "Instant.MAX is beyond the maximum instant that can be represented in a ZonedDateTime."), Arguments.of(Instant.MIN, DateTimeException.class, "Instant.MIN is before the minimum instant that can be represented in a ZonedDateTime."), Arguments.of(new BigDecimal("123.45"), IllegalArgumentException.class, "BigDecimal is not a supported source type for date‑time conversion."));
    }

    @Test
    void toLocalDateTime_fromTimestamp() {
        // GIVEN
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Timestamp ts = Timestamp.from(now);
        LocalDateTime expected = ts.toLocalDateTime();
        // WHEN
        LocalDateTime actual = BigDecimalConversions.toLocalDateTime(ts, converter);
        // THEN
        assertEquals(expected, actual, "Conversion from Timestamp should yield the same LocalDateTime");
    }

    @Test
    void toLocalDateTime_fromDate() {
        // GIVEN
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Date date = Date.from(now);
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime expected = LocalDateTime.ofInstant(now, zone);
        // WHEN
        LocalDateTime actual = BigDecimalConversions.toLocalDateTime(date, converter);
        // THEN
        assertEquals(expected, actual, "Conversion from java.util.Date should respect the system default zone");
    }

    @Test
    void toLocalDateTime_fromLocalDateTime() {
        // GIVEN
        // truncate nanos for deterministic comparison
        LocalDateTime source = LocalDateTime.now().withNano(0);
        // WHEN
        LocalDateTime actual = BigDecimalConversions.toLocalDateTime(source, converter);
        // THEN
        assertEquals(source, actual, "Passing a LocalDateTime should return the same instance/value");
    }

    @Test
    void toLocalDateTime_fromZonedDateTime() {
        // GIVEN
        ZonedDateTime zoned = ZonedDateTime.now().withNano(0);
        LocalDateTime expected = zoned.toLocalDateTime();
        // WHEN
        LocalDateTime actual = BigDecimalConversions.toLocalDateTime(zoned, converter);
        // THEN
        assertEquals(expected, actual, "Conversion from ZonedDateTime should drop the zone information");
    }

    @Test
    void toLocalDateTime_fromOffsetDateTime() {
        // GIVEN
        OffsetDateTime offset = OffsetDateTime.now().withNano(0);
        LocalDateTime expected = offset.toLocalDateTime();
        // WHEN
        LocalDateTime actual = BigDecimalConversions.toLocalDateTime(offset, converter);
        // THEN
        assertEquals(expected, actual, "Conversion from OffsetDateTime should produce the same LocalDateTime");
    }

    @Test
    void toLocalDateTime_fromStringRepresentingDateTime() {
        // GIVEN
        LocalDateTime now = LocalDateTime.now().withNano(0);
        // ISO‑8601 format
        String source = now.toString();
        // WHEN
        LocalDateTime actual = BigDecimalConversions.toLocalDateTime(source, converter);
        // THEN
        assertEquals(now, actual, "String in ISO‑8601 format should be parsed to the correct LocalDateTime");
    }

    @Test
    void toLocalDateTime_nullInputReturnsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        LocalDateTime actual = BigDecimalConversions.toLocalDateTime(source, converter);
        // THEN
        assertNull(actual, "Null input should result in a null LocalDateTime");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validConversionsProvider")
    void toLocalDateTime_validConversions(Object from, LocalDateTime expected, String description) {
        // WHEN
        LocalDateTime actual = BigDecimalConversions.toLocalDateTime(from, null);
        // THEN
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("errorCasesProvider")
    void toLocalDateTime_errorCases(Object from, Class<? extends Throwable> expectedException, String description) {
        // WHEN / THEN
        assertThrows(expectedException, () -> BigDecimalConversions.toLocalDateTime(from, null), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("boundaryExceptionProvider")
    void toLocalDateTime_boundaryExceptions(Object from, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> BigDecimalConversions.toLocalDateTime(from, null), description);
    }
}
