package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toLocalDate_Test {

    /**
     * Converter used for all test cases.
     */
    private static Converter converter;

    /**
     * Simple wrapper used only for the custom‑converter test.
     */
    static final class MyDateWrapper {

        final int year, month, day;

        MyDateWrapper(int year, int month, int day) {
            this.year = year;
            this.month = month;
            this.day = day;
        }
    }

    /**
     * Minimal stub converter that knows how to turn {@link MyDateWrapper} into a {@link ZonedDateTime}.
     */
    static class MyDateWrapperConverter extends Converter {

        MyDateWrapperConverter() {
            super(new ConverterOptions());
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T convert(Object from, Class<T> toClass) {
            if (from instanceof MyDateWrapper && toClass == ZonedDateTime.class) {
                MyDateWrapper w = (MyDateWrapper) from;
                LocalDate ld = LocalDate.of(w.year, w.month, w.day);
                // Use system default zone for the conversion
                return (T) ld.atStartOfDay(ZoneId.systemDefault());
            }
            // Fallback to the default implementation for all other cases
            return super.convert(from, toClass);
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter needs a ConverterOptions instance – the default options are sufficient for these tests.
        converter = new Converter(new ConverterOptions());
    }

    /**
     * Creates a {@link Calendar} with the supplied fields and time‑zone.
     */
    private static Calendar createCalendar(int year, int month, int day, int hour, int minute, TimeZone tz) {
        Calendar cal = Calendar.getInstance(tz);
        cal.setLenient(false);
        cal.set(year, month, day, hour, minute, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    private static Stream<Arguments> successfulConversionsProvider() {
        return Stream.of(// OffsetDateTime
        Arguments.of(OffsetDateTime.parse("2023-05-17T15:30:45+02:00"), LocalDate.of(2023, 5, 17), "OffsetDateTime instance should be converted to a ZonedDateTime preserving the same instant"), // ZonedDateTime
        Arguments.of(ZonedDateTime.parse("2021-12-31T23:59:59+01:00[Europe/Paris]"), LocalDate.of(2021, 12, 31), "ZonedDateTime instance should be converted unchanged"), // LocalDateTime (leap‑year)
        Arguments.of(LocalDateTime.of(2020, 2, 29, 12, 0), LocalDate.of(2020, 2, 29), "LocalDateTime (leap‑year day) interpreted in the system default zone"), // LocalDate
        Arguments.of(LocalDate.of(2019, 7, 4), LocalDate.of(2019, 7, 4), "LocalDate should be returned unchanged"), // java.util.Date (UTC instant)
        Arguments.of(Date.from(Instant.parse("2022-01-15T08:00:00Z")), Instant.parse("2022-01-15T08:00:00Z").atZone(ZoneId.systemDefault()).toLocalDate(), "java.util.Date representing an instant in UTC"), // java.sql.Timestamp
        Arguments.of(Timestamp.valueOf("2024-03-01 00:00:00"), LocalDate.of(2024, 3, 1), "java.sql.Timestamp handled like Date"), // ISO‑8601 string with offset
        Arguments.of("2025-10-20T14:45:00+05:30", LocalDate.of(2025, 10, 20), "ISO‑8601 string with offset"), // Long epoch‑millis
        Arguments.of(1638316800000L, LocalDate.of(2021, 12, 1), "Long representing epoch milliseconds"));
    }

    private static Stream<Arguments> additionalSupportedProvider() {
        return Stream.of(// Instant (leap‑year)
        Arguments.of(Instant.parse("2000-02-29T12:34:56Z"), LocalDate.of(2000, 2, 29), "Instant representing a leap‑year day"), // java.sql.Date (date‑only)
        Arguments.of(java.sql.Date.valueOf("1999-12-31"), LocalDate.of(1999, 12, 31), "java.sql.Date should yield same LocalDate"), // Calendar with UTC+02:00 zone
        Arguments.of(createCalendar(2022, Calendar.MARCH, 15, 23, 30, TimeZone.getTimeZone("UTC+02:00")), LocalDate.of(2022, 3, 15), "Calendar set to 2022‑03‑15 23:30 in UTC+02:00"), // Negative epoch‑millis
        Arguments.of(-86400000L, LocalDate.of(1969, 12, 31), "Negative epoch‑millisecond value"), // AtomicLong just before 2023‑01‑01
        Arguments.of(new AtomicLong(1672531199000L), LocalDate.of(2022, 12, 31), "AtomicLong holding epoch‑millis before 2023‑01‑01"), // Map with year/month/day keys
        Arguments.of(Map.of("year", 2021, "month", 2, "day", 28), LocalDate.of(2021, 2, 28), "Map containing year, month and day keys"), // ISO‑8601 date‑only string
        Arguments.of("2023-04-05", LocalDate.of(2023, 4, 5), "ISO‑8601 date‑only string"));
    }

    @Test
    void toLocalDate_fromLocalDate_returnsSameDate() {
        // GIVEN
        LocalDate expected = LocalDate.of(2022, Month.MAY, 10);
        // WHEN
        LocalDate actual = OffsetDateTimeConversions.toLocalDate(expected, converter);
        // THEN
        assertEquals(expected, actual, "Converting a LocalDate should return the same LocalDate");
    }

    @Test
    void toLocalDate_fromLocalDateTime_returnsDatePart() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2022, Month.MAY, 10, 15, 30, 45);
        LocalDate expected = ldt.toLocalDate();
        // WHEN
        LocalDate actual = OffsetDateTimeConversions.toLocalDate(ldt, converter);
        // THEN
        assertEquals(expected, actual, "Converting a LocalDateTime should yield its date component");
    }

    @Test
    void toLocalDate_fromUtilDate_returnsCorrespondingLocalDate() {
        // GIVEN
        // Create a java.util.Date that represents 2022‑05‑10T12:34:56Z
        Instant instant = Instant.parse("2022-05-10T12:34:56Z");
        Date date = Date.from(instant);
        // Expected LocalDate is the date in the system default zone
        LocalDate expected = instant.atZone(ZoneId.systemDefault()).toLocalDate();
        // WHEN
        LocalDate actual = OffsetDateTimeConversions.toLocalDate(date, converter);
        // THEN
        assertEquals(expected, actual, "Converting a java.util.Date should produce the correct LocalDate");
    }

    @Test
    void toLocalDate_fromSqlTimestamp_returnsCorrespondingLocalDate() {
        // GIVEN
        Timestamp ts = Timestamp.valueOf("2022-05-10 23:59:59");
        // Expected LocalDate is the date part of the timestamp in the system default zone
        LocalDate expected = ts.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        // WHEN
        LocalDate actual = OffsetDateTimeConversions.toLocalDate(ts, converter);
        // THEN
        assertEquals(expected, actual, "Converting a java.sql.Timestamp should yield its date component");
    }

    @Test
    void toLocalDate_fromString_returnsParsedLocalDate() {
        // GIVEN
        String isoDate = "2022-05-10";
        LocalDate expected = LocalDate.parse(isoDate, DateTimeFormatter.ISO_LOCAL_DATE);
        // WHEN
        LocalDate actual = OffsetDateTimeConversions.toLocalDate(isoDate, converter);
        // THEN
        assertEquals(expected, actual, "Converting an ISO‑8601 date string should produce the correct LocalDate");
    }

    @Test
    void toLocalDate_fromOffsetDateTime_returnsSameDate() {
        // GIVEN
        OffsetDateTime odt = OffsetDateTime.of(2022, 5, 10, 8, 15, 0, 0, ZoneOffset.UTC);
        LocalDate expected = odt.toLocalDate();
        // WHEN
        LocalDate actual = OffsetDateTimeConversions.toLocalDate(odt, converter);
        // THEN
        assertEquals(expected, actual, "Converting an OffsetDateTime should return its date component");
    }

    @Test
    void toLocalDate_fromZonedDateTime_returnsSameDate() {
        // GIVEN
        ZonedDateTime zdt = ZonedDateTime.of(2022, 5, 10, 22, 0, 0, 0, ZoneId.of("America/New_York"));
        LocalDate expected = zdt.toLocalDate();
        // WHEN
        LocalDate actual = OffsetDateTimeConversions.toLocalDate(zdt, converter);
        // THEN
        assertEquals(expected, actual, "Converting a ZonedDateTime should return its date component");
    }

    @Test
    void toLocalDate_fromMapWithTimeKey_returnsCorrespondingLocalDate() {
        // GIVEN
        // The Converter expects a map with a "time" key (epoch milli) for date/time conversions.
        Instant instant = Instant.parse("2022-05-10T00:00:00Z");
        Map<String, Object> map = new HashMap<>();
        map.put("time", instant.toEpochMilli());
        LocalDate expected = instant.atZone(ZoneId.systemDefault()).toLocalDate();
        // WHEN
        LocalDate actual = OffsetDateTimeConversions.toLocalDate(map, converter);
        // THEN
        assertEquals(expected, actual, "Converting a map with a 'time' key should produce the correct LocalDate");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("successfulConversionsProvider")
    void toLocalDate_successfulConversions(Object from, LocalDate expected, String description) {
        // WHEN
        LocalDate actual = OffsetDateTimeConversions.toLocalDate(from, converter);
        // THEN
        assertEquals(expected, actual, description);
    }

    @Test
    void toLocalDate_nullInput_returnsNull() {
        // WHEN
        LocalDate actual = OffsetDateTimeConversions.toLocalDate(null, converter);
        // THEN
        assertNull(actual, "Null source should result in null");
    }

    @Test
    void toLocalDate_booleanInput_throwsConversionException() {
        // GIVEN
        Boolean source = Boolean.TRUE;
        // WHEN / THEN
        assertThrows(ConversionException.class, () -> OffsetDateTimeConversions.toLocalDate(source, converter), "Boolean cannot be converted to a date");
    }

    @Test
    void toLocalDate_malformedString_throwsConversionException() {
        // GIVEN
        String bad = "not-a-date";
        // WHEN / THEN
        assertThrows(ConversionException.class, () -> OffsetDateTimeConversions.toLocalDate(bad, converter), "Malformed date string must raise ConversionException");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("additionalSupportedProvider")
    void toLocalDate_additionalSupportedTypes(Object from, LocalDate expected, String description) {
        // WHEN
        LocalDate actual = OffsetDateTimeConversions.toLocalDate(from, converter);
        // THEN
        assertEquals(expected, actual, description);
    }

    @Test
    void toLocalDate_offsetTimeFails() {
        OffsetTime ot = OffsetTime.parse("23:45:00+02:00");
        assertThrows(ConversionException.class, () -> OffsetDateTimeConversions.toLocalDate(ot, converter), "OffsetTime lacks a date component and must fail");
    }

    @Test
    void toLocalDate_randomObjectFails() {
        Object obj = new Object();
        assertThrows(ConversionException.class, () -> OffsetDateTimeConversions.toLocalDate(obj, converter), "Completely unrelated object should trigger ConversionException");
    }

    @Test
    void toLocalDate_customConverterHandlesMyDateWrapper() {
        // GIVEN
        MyDateWrapper wrapper = new MyDateWrapper(2025, 6, 1);
        Converter custom = new MyDateWrapperConverter();
        LocalDate expected = LocalDate.of(2025, 6, 1);
        // WHEN
        LocalDate actual = OffsetDateTimeConversions.toLocalDate(wrapper, custom);
        // THEN
        assertEquals(expected, actual, "Custom converter should translate MyDateWrapper to the correct LocalDate");
    }

    @Test
    void toLocalDate_extremelyEarlyInstant() {
        Instant early = Instant.parse("-999999999-01-01T00:00:00Z");
        LocalDate expected = LocalDate.of(-999_999_999, 1, 1);
        LocalDate actual = OffsetDateTimeConversions.toLocalDate(early, converter);
        assertEquals(expected, actual, "Extremely early Instant should be converted without overflow");
    }

    @Test
    void toLocalDate_extremelyLateInstant() {
        Instant late = Instant.parse("+999999999-12-31T23:59:59Z");
        LocalDate expected = LocalDate.of(999_999_999, 12, 31);
        LocalDate actual = OffsetDateTimeConversions.toLocalDate(late, converter);
        assertEquals(expected, actual, "Extremely late Instant should be converted correctly");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = { "", "23:45:00", "2023-13-01T00:00:00Z" })
    void toLocalDate_invalidStringFormats(String badString) {
        assertThrows(ConversionException.class, () -> OffsetDateTimeConversions.toLocalDate(badString, converter), "Invalid string '" + badString + "' must raise ConversionException");
    }
}
