package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.format.DateTimeParseException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

class CalendarConversions_toLocalDate_Test {

    /**
     * Shared Converter instance used for all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; using default options.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference.
        converter = null;
    }

    /**
     * Provides a stream of arguments for the supported conversion cases.
     *
     * Each argument consists of:
     *   - the source object {@code from}
     *   - the expected {@link LocalDate} result
     *   - a description used in the test name.
     */
    private static Stream<Arguments> toLocalDateProvider() {
        ZoneId systemZone = ZoneId.systemDefault();
        return Stream.of(// Direct LocalDate – should be returned unchanged.
        Arguments.of(LocalDate.of(2023, 5, 17), LocalDate.of(2023, 5, 17), "Direct LocalDate input should be returned unchanged."), // LocalDateTime – only the date component is kept.
        Arguments.of(LocalDateTime.of(2023, 5, 17, 14, 30, 45), LocalDate.of(2023, 5, 17), "LocalDateTime is converted to its date component."), // ZonedDateTime – date in the same zone.
        Arguments.of(ZonedDateTime.of(2023, 5, 17, 14, 30, 45, 0, ZoneId.of("UTC")), LocalDate.of(2023, 5, 17), "ZonedDateTime is converted to its local date in the same zone."), // OffsetDateTime – offset may shift the date.
        Arguments.of(OffsetDateTime.of(2023, 5, 17, 23, 59, 59, 0, ZoneOffset.ofHours(-5)), // 2023-05-18 in UTC because -5h shifts it forward one day.
        OffsetDateTime.of(2023, 5, 17, 23, 59, 59, 0, ZoneOffset.ofHours(-5)).toZonedDateTime().toLocalDate(), "OffsetDateTime is first converted to a ZonedDateTime (preserving the offset) then the date part is extracted; the offset shifts the date to the next day."), // java.util.Date – conversion uses the system default zone.
        Arguments.of(Date.from(Instant.ofEpochMilli(0L)), Instant.ofEpochMilli(0L).atZone(systemZone).toLocalDate(), "java.util.Date representing the epoch (0 ms) should map to 1970‑01‑01 in the system default zone."), // java.sql.Timestamp – treated like a Date.
        Arguments.of(Timestamp.valueOf("2023-02-28 23:59:59.123"), Timestamp.valueOf("2023-02-28 23:59:59.123").toInstant().atZone(systemZone).toLocalDate(), "Timestamp is treated like a Date; only the date component is kept."), // Calendar (UTC) – leap day handling.
        Arguments.of(createUtcCalendar(2024, Calendar.FEBRUARY, 29, 12, 0, 0), LocalDate.of(2024, 2, 29), "Leap‑day in a Calendar (UTC) should be correctly converted to LocalDate."), // Long epoch milliseconds.
        Arguments.of(Long.valueOf(1672531199000L), Instant.ofEpochMilli(1672531199000L).atZone(systemZone).toLocalDate(), "Epoch milliseconds for 2022‑12‑31 23:59:59 UTC should yield the corresponding LocalDate."), // ISO‑8601 date string.
        Arguments.of("2025-12-31", LocalDate.of(2025, 12, 31), "ISO‑8601 date string is parsed to a LocalDate."), // BigInteger interpreted as epoch milliseconds.
        Arguments.of(BigInteger.valueOf(0L), Instant.ofEpochMilli(0L).atZone(systemZone).toLocalDate(), "BigInteger interpreted as epoch milliseconds (0) should map to the epoch date."), // AtomicLong holding epoch milliseconds.
        Arguments.of(new AtomicLong(1704067200000L), Instant.ofEpochMilli(1704067200000L).atZone(systemZone).toLocalDate(), "AtomicLong holding epoch milliseconds for 2024‑01‑01 should be converted correctly."));
    }

    /**
     * Creates a {@link Calendar} instance in UTC with the supplied fields.
     *
     * @param year   the year (e.g., 2024)
     * @param month  the {@link Calendar} month constant (0‑based)
     * @param day    the day of month
     * @param hour   hour of day (0‑23)
     * @param minute minute
     * @param second second
     * @return a configured {@link Calendar}
     */
    private static Calendar createUtcCalendar(int year, int month, int day, int hour, int minute, int second) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(year, month, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    @Test
    void toLocalDate_fromLocalDate_returnsSame() {
        LocalDate expected = LocalDate.of(2023, Month.MARCH, 15);
        LocalDate actual = CalendarConversions.toLocalDate(expected, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalDate_fromLocalDateTime_stripsTime() {
        LocalDateTime ldt = LocalDateTime.of(2023, Month.APRIL, 10, 14, 30, 45);
        LocalDate expected = ldt.toLocalDate();
        LocalDate actual = CalendarConversions.toLocalDate(ldt, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalDate_fromZonedDateTime_respectsZone() {
        ZonedDateTime zdt = ZonedDateTime.of(2022, 12, 31, 23, 0, 0, 0, ZoneId.of("America/New_York"));
        LocalDate expected = zdt.toLocalDate();
        LocalDate actual = CalendarConversions.toLocalDate(zdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalDate_fromOffsetDateTime_respectsOffset() {
        OffsetDateTime odt = OffsetDateTime.of(2021, 6, 5, 8, 15, 0, 0, ZoneOffset.ofHours(-5));
        LocalDate expected = odt.toLocalDate();
        LocalDate actual = CalendarConversions.toLocalDate(odt, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalDate_fromDate_convertsUsingDefaultZone() {
        // java.util.Date stores an instant; conversion uses the system default zone.
        Date date = Date.from(Instant.parse("2020-01-02T10:20:30Z"));
        ZoneId systemZone = ZoneId.systemDefault();
        LocalDate expected = date.toInstant().atZone(systemZone).toLocalDate();
        LocalDate actual = CalendarConversions.toLocalDate(date, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalDate_fromSqlTimestamp_convertsCorrectly() {
        Timestamp ts = Timestamp.valueOf("2019-07-04 12:34:56.789");
        ZoneId systemZone = ZoneId.systemDefault();
        LocalDate expected = ts.toInstant().atZone(systemZone).toLocalDate();
        LocalDate actual = CalendarConversions.toLocalDate(ts, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalDate_fromCalendar_convertsCorrectly() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2018, Calendar.NOVEMBER, 30, 5, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        ZoneId zone = cal.getTimeZone().toZoneId();
        LocalDate expected = cal.toInstant().atZone(zone).toLocalDate();
        LocalDate actual = CalendarConversions.toLocalDate(cal, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalDate_fromString_parsesISODate() {
        String isoDate = "2024-02-29";
        LocalDate expected = LocalDate.parse(isoDate);
        LocalDate actual = CalendarConversions.toLocalDate(isoDate, converter);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("toLocalDateProvider")
    void toLocalDate_supportedTypes(Object from, LocalDate expected, String description) {
        LocalDate actual = CalendarConversions.toLocalDate(from, converter);
        assertEquals(expected, actual, description);
    }

    @Test
    void toLocalDate_nullConverter_throwsNullPointerException() {
        LocalDate source = LocalDate.of(2023, 1, 1);
        assertThrows(NullPointerException.class, () -> CalendarConversions.toLocalDate(source, null), "Passing a null Converter should result in a NullPointerException before any conversion is attempted.");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @ValueSource(strings = { "2023-02-30", "not-a-date" })
    void toLocalDate_malformedString_throwsDateTimeParseException(String malformed) {
        assertThrows(DateTimeParseException.class, () -> CalendarConversions.toLocalDate(malformed, converter), "A malformed date string should cause DateTimeParseException.");
    }

    @Test
    void toLocalDate_nullSource_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> CalendarConversions.toLocalDate(null, converter), "Passing a null source object must result in a NullPointerException.");
    }

    @Test
    void toLocalDate_unsupportedBoolean_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> CalendarConversions.toLocalDate(Boolean.TRUE, converter), "Unsupported type (Boolean) should cause an IllegalArgumentException.");
    }

    @Test
    void toLocalDate_unsupportedLocalTime_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> CalendarConversions.toLocalDate(LocalTime.of(14, 30, 0), converter), "LocalTime contains only a time component; converting it to a LocalDate is unsupported.");
    }

    @Test
    void toLocalDate_unsupportedMap_throwsIllegalArgumentException() {
        Map<String, String> map = Collections.singletonMap("key", "value");
        assertThrows(IllegalArgumentException.class, () -> CalendarConversions.toLocalDate(map, converter), "A Map instance is not a convertible temporal type and should trigger IllegalArgumentException.");
    }

    @Test
    void toLocalDate_unsupportedBigDecimal_throwsIllegalArgumentException() {
        BigDecimal bd = BigDecimal.valueOf(12345.67);
        assertThrows(IllegalArgumentException.class, () -> CalendarConversions.toLocalDate(bd, converter), "BigDecimal is not a supported source for date conversion.");
    }

    @Test
    void toLocalDate_unsupportedAtomicInteger_throwsIllegalArgumentException() {
        AtomicInteger ai = new AtomicInteger(1000);
        assertThrows(IllegalArgumentException.class, () -> CalendarConversions.toLocalDate(ai, converter), "AtomicInteger is not a recognized temporal source; conversion should fail.");
    }
}
