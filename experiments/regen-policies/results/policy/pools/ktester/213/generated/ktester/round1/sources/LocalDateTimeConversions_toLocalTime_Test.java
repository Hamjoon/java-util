package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.format.DateTimeParseException;
import java.sql.Timestamp;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toLocalTime_Test {

    private static TimeZone originalDefaultZone;

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Force a deterministic zone for the whole test suite
        originalDefaultZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        // Converter needs a ConverterOptions instance – use the default constructor
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore the original default time‑zone
        TimeZone.setDefault(originalDefaultZone);
    }

    /**
     * Creates a {@link Calendar} with the supplied fields and zone offset.
     */
    private static Calendar createCalendar(int year, int month, int day, int hour, int minute, int second, ZoneOffset offset) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(offset));
        cal.clear();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, second);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    static Stream<Arguments> validConversionProvider() {
        return Stream.of(// ZonedDateTime
        Arguments.of(ZonedDateTime.of(2023, 5, 10, 14, 30, 0, 0, ZoneOffset.UTC), LocalTime.of(14, 30), "Direct ZonedDateTime input"), // LocalDateTime
        Arguments.of(LocalDateTime.of(2023, 5, 10, 9, 15, 45), LocalTime.of(9, 15, 45), "LocalDateTime input – default zone conversion"), // OffsetDateTime
        Arguments.of(OffsetDateTime.of(2023, 5, 10, 22, 5, 0, 0, ZoneOffset.ofHours(2)), LocalTime.of(22, 5), "OffsetDateTime retains its offset"), // LocalTime
        Arguments.of(LocalTime.of(7, 45, 30), LocalTime.of(7, 45, 30), "LocalTime should be returned unchanged"), // Instant
        Arguments.of(Instant.parse("2023-05-10T12:00:00Z"), LocalTime.of(12, 0), "Instant interpreted in UTC (default zone)"), // java.util.Date
        Arguments.of(Date.from(Instant.parse("2023-05-10T08:20:15Z")), LocalTime.of(8, 20, 15), "Date → Instant → LocalTime"), // java.sql.Timestamp
        Arguments.of(Timestamp.valueOf("2023-05-10 16:40:00"), LocalTime.of(16, 40), "Timestamp conversion"), // Long epoch‑millis
        Arguments.of(Long.valueOf(Instant.parse("2023-05-10T23:55:00Z").toEpochMilli()), LocalTime.of(23, 55), "Long epoch‑millis conversion"), // String time only
        Arguments.of("14:30:15", LocalTime.of(14, 30, 15), "String representation of a time"));
    }

    @Test
    void toLocalTime_fromLocalDateTime() {
        LocalDateTime ldt = LocalDateTime.of(2023, Month.MAY, 10, 14, 30, 15, 123_000_000);
        LocalTime expected = ldt.toLocalTime();
        LocalTime actual = LocalDateTimeConversions.toLocalTime(ldt, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalTime_fromZonedDateTime() {
        ZonedDateTime zdt = ZonedDateTime.of(LocalDateTime.of(2023, Month.JUNE, 1, 9, 45, 30, 500_000_000), ZoneId.of("UTC"));
        LocalTime expected = zdt.toLocalTime();
        LocalTime actual = LocalDateTimeConversions.toLocalTime(zdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalTime_fromInstant() {
        Instant instant = Instant.parse("2023-07-15T12:34:56.789Z");
        // Converter will interpret the instant in the default zone (UTC because of setup)
        LocalTime expected = LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalTime();
        LocalTime actual = LocalDateTimeConversions.toLocalTime(instant, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalTime_fromDate() {
        Instant instant = Instant.parse("2022-12-31T23:59:59.001Z");
        Date date = Date.from(instant);
        LocalTime expected = LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalTime();
        LocalTime actual = LocalDateTimeConversions.toLocalTime(date, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalTime_fromCalendar() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(Instant.parse("2021-01-01T08:15:30.250Z").toEpochMilli());
        LocalTime expected = LocalDateTime.ofInstant(cal.toInstant(), ZoneId.systemDefault()).toLocalTime();
        LocalTime actual = LocalDateTimeConversions.toLocalTime(cal, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalTime_fromString() {
        String timeString = "16:20:45.123";
        LocalTime expected = LocalTime.parse(timeString, DateTimeFormatter.ISO_LOCAL_TIME);
        LocalTime actual = LocalDateTimeConversions.toLocalTime(timeString, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalTime_fromLongEpochMillis() {
        long epochMillis = Instant.parse("2020-05-05T05:05:05.555Z").toEpochMilli();
        LocalTime expected = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault()).toLocalTime();
        LocalTime actual = LocalDateTimeConversions.toLocalTime(epochMillis, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalTime_fromMapWithTimeKey() {
        long epochMillis = Instant.parse("2019-03-03T03:03:03.003Z").toEpochMilli();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("time", epochMillis);
        LocalTime expected = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault()).toLocalTime();
        LocalTime actual = LocalDateTimeConversions.toLocalTime(map, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalTime_nullInputReturnsNull() {
        assertNull(LocalDateTimeConversions.toLocalTime(null, converter));
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validConversionProvider")
    void toLocalTime_validConversions(Object from, LocalTime expected, String description) {
        LocalTime actual = LocalDateTimeConversions.toLocalTime(from, converter);
        assertEquals(expected, actual, description);
    }

    @Test
    void toLocalTime_midnightConstant() {
        LocalTime actual = LocalDateTimeConversions.toLocalTime(LocalTime.MIDNIGHT, converter);
        assertEquals(LocalTime.MIDNIGHT, actual);
    }

    @Test
    void toLocalTime_maxNanosecondPrecision() {
        LocalTime source = LocalTime.of(23, 59, 59, 999_999_999);
        LocalTime actual = LocalDateTimeConversions.toLocalTime(source, converter);
        assertEquals(source, actual);
    }

    @Test
    void toLocalTime_stringWithNanosecondFraction() {
        String src = "12:34:56.123456789";
        LocalTime expected = LocalTime.of(12, 34, 56, 123_456_789);
        LocalTime actual = LocalDateTimeConversions.toLocalTime(src, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalTime_localDateDefaultsToMidnight() {
        LocalDate date = LocalDate.of(2023, 5, 10);
        LocalTime actual = LocalDateTimeConversions.toLocalTime(date, converter);
        assertEquals(LocalTime.MIDNIGHT, actual);
    }

    @Test
    void toLocalTime_sqlDateDefaultsToMidnight() {
        java.sql.Date sqlDate = java.sql.Date.valueOf("2023-05-10");
        LocalTime actual = LocalDateTimeConversions.toLocalTime(sqlDate, converter);
        assertEquals(LocalTime.MIDNIGHT, actual);
    }

    @Test
    void toLocalTime_calendarWithZoneOffset() {
        Calendar cal = createCalendar(2023, Calendar.MAY, 10, 15, 45, 30, ZoneOffset.ofHours(-3));
        LocalTime actual = LocalDateTimeConversions.toLocalTime(cal, converter);
        assertEquals(LocalTime.of(15, 45, 30), actual);
    }

    @Test
    void toLocalTime_isoDateTimeWithoutOffset() {
        String src = "2023-05-10T14:30:00";
        LocalTime actual = LocalDateTimeConversions.toLocalTime(src, converter);
        assertEquals(LocalTime.of(14, 30), actual);
    }

    @Test
    void toLocalTime_isoDateTimeWithOffset() {
        String src = "2023-05-10T14:30:00+02:00";
        LocalTime actual = LocalDateTimeConversions.toLocalTime(src, converter);
        // The offset is kept; the local‑time part is 14:30
        assertEquals(LocalTime.of(14, 30), actual);
    }

    @Test
    void toLocalTime_negativeEpochMillis() {
        // 1 ms before 1970‑01‑01 UTC
        long epochMillis = -1L;
        LocalTime expected = LocalTime.of(23, 59, 59, 999_000_000);
        LocalTime actual = LocalDateTimeConversions.toLocalTime(Long.valueOf(epochMillis), converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalTime_nullInputThrowsNPE() {
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toLocalTime(null, converter));
    }

    @Test
    void toLocalTime_unsupportedTypeThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> LocalDateTimeConversions.toLocalTime(Boolean.TRUE, converter));
    }

    @Test
    void toLocalTime_malformedStringThrowsParseException() {
        assertThrows(DateTimeParseException.class, () -> LocalDateTimeConversions.toLocalTime("not-a-time", converter));
    }
}
