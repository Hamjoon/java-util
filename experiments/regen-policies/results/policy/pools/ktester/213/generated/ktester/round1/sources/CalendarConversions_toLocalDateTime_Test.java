package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class CalendarConversions_toLocalDateTime_Test {

    private Converter converter;

    // UTC+9
    private static final TimeZone TOKYO = TimeZone.getTimeZone("Asia/Tokyo");

    // UTC+1/+2
    private static final TimeZone PARIS = TimeZone.getTimeZone("Europe/Paris");

    // UTC‑5/‑4
    private static final TimeZone NEW_YORK = TimeZone.getTimeZone("America/New_York");

    private static TimeZone ORIGINAL_DEFAULT_TZ;

    @BeforeAll
    static void setupBeforeAll() {
        // Force a deterministic default time‑zone for tests that rely on the system default.
        TimeZone original = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        // Store the original zone so it can be restored after all tests.
        ORIGINAL_DEFAULT_TZ = original;
    }

    @BeforeEach
    void setupBeforeEach() {
        // The Converter is the public entry point used by the library; it internally delegates to CalendarConversions.
        converter = new Converter(new DefaultConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore the original default zone.
        TimeZone.setDefault(ORIGINAL_DEFAULT_TZ);
    }

    /**
     * Helper that creates a {@link Calendar} with the supplied fields.
     * <p>
     * Note: {@code month} is 1‑based for the caller (January = 1) but {@link Calendar} expects a 0‑based month.
     */
    private static Calendar createCalendar(int year, int month, int day, int hour, int minute, int second, int millis, TimeZone tz) {
        Calendar cal = Calendar.getInstance(tz);
        cal.clear();
        cal.set(year, month - 1, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, millis);
        return cal;
    }

    /**
     * Provides all valid source objects together with the expected {@link LocalDateTime}.
     * The expected values are computed using the system default zone (forced to UTC in @BeforeAll)
     * where the conversion semantics depend on that zone.
     */
    private static Stream<Arguments> validConversionProvider() {
        ZoneId defaultZone = ZoneId.systemDefault();
        // 1. Direct LocalDateTime
        LocalDateTime ldt = LocalDateTime.of(2023, 5, 15, 10, 30, 45);
        // 2. ZonedDateTime (UTC)
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 15, 10, 30, 45, 0, ZoneId.of("UTC"));
        // 3. OffsetDateTime (+02:00)
        OffsetDateTime odt = OffsetDateTime.of(2023, 5, 15, 10, 30, 45, 0, ZoneOffset.ofHours(2));
        // 4. java.util.Date (instant in UTC)
        Date date = Date.from(Instant.parse("2023-05-15T10:30:45Z"));
        // 5. java.sql.Timestamp
        Timestamp ts = Timestamp.valueOf("2023-05-15 10:30:45");
        // 6. Instant
        Instant instant = Instant.parse("2023-05-15T10:30:45Z");
        // 7. LocalDate → start of day
        LocalDate localDate = LocalDate.of(2023, 5, 15);
        // 8. LocalTime → epoch date (1970‑01‑01)
        LocalTime localTime = LocalTime.of(10, 30, 45);
        // 9. YearMonth → first day of month at midnight
        YearMonth yearMonth = YearMonth.of(2023, 5);
        // 10. Year → first day of year at midnight
        Year year = Year.of(2023);
        // 11. MonthDay → combined with epoch year (1970)
        MonthDay monthDay = MonthDay.of(5, 15);
        // 12. ISO‑8601 string
        String isoString = "2023-05-15T10:30:45";
        // 13. Custom formatted string (DateUtilities should understand)
        String customString = "2023/05/15 10:30:45";
        // 14. Long epoch milliseconds
        long epochMillis = 1_234_567_890_123L;
        // 15. GregorianCalendar instance
        Calendar gregCal = new GregorianCalendar(2023, Calendar.MAY, 15, 10, 30, 45);
        gregCal.set(Calendar.MILLISECOND, 0);
        return Stream.of(Arguments.of(ldt, ldt), Arguments.of(zdt, ldt), Arguments.of(odt, ldt), Arguments.of(date, LocalDateTime.ofInstant(date.toInstant(), defaultZone)), Arguments.of(ts, LocalDateTime.ofInstant(ts.toInstant(), defaultZone)), Arguments.of(instant, LocalDateTime.ofInstant(instant, defaultZone)), Arguments.of(localDate, LocalDateTime.of(localDate, LocalTime.MIDNIGHT)), Arguments.of(localTime, LocalDateTime.of(LocalDate.of(1970, 1, 1), localTime)), Arguments.of(yearMonth, LocalDateTime.of(yearMonth.atDay(1), LocalTime.MIDNIGHT)), Arguments.of(year, LocalDateTime.of(year.atDay(1), LocalTime.MIDNIGHT)), Arguments.of(monthDay, LocalDateTime.of(LocalDate.of(1970, monthDay.getMonthValue(), monthDay.getDayOfMonth()), LocalTime.MIDNIGHT)), Arguments.of(isoString, LocalDateTime.of(2023, 5, 15, 10, 30, 45)), Arguments.of(customString, LocalDateTime.of(2023, 5, 15, 10, 30, 45)), Arguments.of(epochMillis, LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), defaultZone)), Arguments.of(gregCal, LocalDateTime.of(2023, 5, 15, 10, 30, 45)));
    }

    private static Stream<String> invalidStringProvider() {
        return Stream.of(// completely malformed
        "2023-13-40T25:61:61", // not a date at all
        "not-a-date", // impossible calendar date
        "2023/02/30 10:00:00");
    }

    private static Stream<Object> unsupportedProvider() {
        return Stream.of(true, new ArrayList<>(), UUID.randomUUID());
    }

    @Test
    void testCalendarToLocalDateTime_Tokyo() {
        Calendar cal = createCalendar(2023, 6, 15, 15, 30, 0, 500, TOKYO);
        LocalDateTime expected = LocalDateTime.of(2023, 6, 15, 15, 30, 0, 500_000_000);
        assertEquals(expected, converter.convert(cal, LocalDateTime.class));
    }

    @Test
    void testCalendarToLocalDateTime_Paris() {
        Calendar cal = createCalendar(2022, 12, 31, 23, 59, 59, 999, PARIS);
        LocalDateTime expected = LocalDateTime.of(2022, 12, 31, 23, 59, 59, 999_000_000);
        assertEquals(expected, converter.convert(cal, LocalDateTime.class));
    }

    @Test
    void testCalendarToLocalDateTime_NewYork() {
        Calendar cal = createCalendar(1970, 1, 1, 0, 0, 0, 0, NEW_YORK);
        LocalDateTime expected = LocalDateTime.of(1970, 1, 1, 0, 0, 0, 0);
        assertEquals(expected, converter.convert(cal, LocalDateTime.class));
    }

    @Test
    void testStringIsoToLocalDateTime() {
        String iso = "2021-04-05T12:34:56.123";
        LocalDateTime expected = LocalDateTime.of(2021, 4, 5, 12, 34, 56, 123_000_000);
        assertEquals(expected, converter.convert(iso, LocalDateTime.class));
    }

    @Test
    void testNullConversionReturnsNull() {
        assertNull(converter.convert(null, LocalDateTime.class));
    }

    @ParameterizedTest(name = "valid conversion #{index}")
    @MethodSource("validConversionProvider")
    void testValidConversions(Object from, LocalDateTime expected) {
        // The Converter API is the public entry point; it should delegate to CalendarConversions.
        LocalDateTime actual = converter.convert(from, LocalDateTime.class);
        assertEquals(expected, actual, () -> "Conversion of " + from.getClass().getSimpleName() + " did not produce the expected LocalDateTime");
    }

    @Test
    void testNullFromThrowsNullPointerException() {
        // Directly invoke the package‑private method to verify the contract.
        assertThrows(NullPointerException.class, () -> CalendarConversions.toLocalDateTime(null, null));
    }

    @ParameterizedTest(name = "invalid string ''{0}''")
    @MethodSource("invalidStringProvider")
    void testInvalidStringThrowsDateTimeParseException(String badInput) {
        assertThrows(DateTimeParseException.class, () -> converter.convert(badInput, LocalDateTime.class));
    }

    @ParameterizedTest(name = "unsupported type ''{0}''")
    @MethodSource("unsupportedProvider")
    void testUnsupportedSourceThrowsIllegalArgumentException(Object unsupported) {
        assertThrows(IllegalArgumentException.class, () -> converter.convert(unsupported, LocalDateTime.class));
    }
}
