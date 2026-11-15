package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigInteger;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_toLocalDate_Test {

    private Converter converter;

    // UTC+9
    private static final TimeZone TOKYO = TimeZone.getTimeZone("Asia/Tokyo");

    // UTC+1/+2
    private static final TimeZone PARIS = TimeZone.getTimeZone("Europe/Paris");

    // UTC‑5/-4
    private static final TimeZone NEW_YORK = TimeZone.getTimeZone("America/New_York");

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    @BeforeEach
    void setupBeforeEach() {
        // The Converter is the only dependency needed for the static method.
        // In most test cases the converter is not required (null is passed), but we keep a default instance.
        converter = new Converter(new DefaultConverterOptions());
    }

    /**
     * Helper to create a Calendar with the given fields (month is 1‑based for callers).
     */
    private Calendar createCalendar(int year, int month, int day, int hour, int minute, int second, int millis, TimeZone tz) {
        Calendar cal = Calendar.getInstance(tz);
        cal.clear();
        cal.set(year, month - 1, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, millis);
        return cal;
    }

    static Stream<Arguments> provideFromLocalDate() {
        return Stream.of(Arguments.of(LocalDate.of(2023, 5, 17), null, LocalDate.of(2023, 5, 17)));
    }

    static Stream<Arguments> provideFromLocalDateTime() {
        return Stream.of(Arguments.of(LocalDateTime.of(2022, 12, 31, 23, 59, 59), null, LocalDate.of(2022, 12, 31)));
    }

    static Stream<Arguments> provideFromZonedDateTime() {
        return Stream.of(Arguments.of(ZonedDateTime.of(2021, 2, 28, 10, 15, 30, 0, ZoneId.of("Europe/Paris")), null, LocalDate.of(2021, 2, 28)));
    }

    static Stream<Arguments> provideFromInstant() {
        return Stream.of(Arguments.of(Instant.ofEpochSecond(0L), null, LocalDate.of(1970, 1, 1)));
    }

    static Stream<Arguments> provideFromDate() {
        return Stream.of(Arguments.of(new Date(1234567890000L), null, LocalDate.of(2009, 2, 13)));
    }

    static Stream<Arguments> provideFromSqlDate() {
        return Stream.of(Arguments.of(java.sql.Date.valueOf("2000-01-01"), null, LocalDate.of(2000, 1, 1)));
    }

    static Stream<Arguments> provideFromCalendar() {
        Calendar cal = Calendar.getInstance(UTC);
        cal.clear();
        cal.set(1995, Calendar.JULY, 20, 5, 30, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return Stream.of(Arguments.of(cal, null, LocalDate.of(1995, 7, 20)));
    }

    static Stream<Arguments> provideFromLong() {
        return Stream.of(Arguments.of(0L, null, LocalDate.of(1970, 1, 1)));
    }

    static Stream<Arguments> provideFromStringISODate() {
        return Stream.of(Arguments.of("2023-08-15", null, LocalDate.of(2023, 8, 15)));
    }

    static Stream<Arguments> provideFromStringISODateTimeWithOffset() {
        return Stream.of(Arguments.of("2023-08-15T13:45:30+02:00", null, LocalDate.of(2023, 8, 15)));
    }

    static Stream<Arguments> provideFromYearMonth() {
        return Stream.of(Arguments.of(YearMonth.of(2024, 2), null, LocalDate.of(2024, 2, 1)));
    }

    static Stream<Arguments> provideFromYear() {
        return Stream.of(Arguments.of(Year.of(1999), null, LocalDate.of(1999, 1, 1)));
    }

    static Stream<Arguments> provideFromInvalidString() {
        return Stream.of(Arguments.of("not-a-date", null));
    }

    static Stream<Arguments> provideFromUnsupportedType() {
        return Stream.of(Arguments.of(new BigInteger("12345678901234567890"), null));
    }

    static Stream<Arguments> provideFromLocalTime() {
        return Stream.of(Arguments.of(LocalTime.of(12, 34, 56), null));
    }

    static Stream<Arguments> provideFromMonthDay() {
        return Stream.of(Arguments.of(MonthDay.of(2, 29), null));
    }

    static Stream<Arguments> provideFromUnsupportedMap() {
        Map<String, Object> map = Collections.singletonMap("year", 2022);
        return Stream.of(Arguments.of(map, null));
    }

    static Stream<Arguments> provideFromStringWithInvalidOffset() {
        return Stream.of(Arguments.of("2023-08-15T13:45:30+25:00", null));
    }

    static Stream<Arguments> provideFromStringEmpty() {
        return Stream.of(Arguments.of("", null));
    }

    @Test
    void testCalendarToLocalDate() {
        // Calendar in Tokyo (UTC+9) – date should be the same calendar date regardless of zone
        Calendar calTokyo = createCalendar(2023, 6, 15, 23, 59, 59, 999, TOKYO);
        LocalDate expectedTokyo = LocalDate.of(2023, 6, 15);
        assertEquals(expectedTokyo, CalendarConversions.toLocalDate(calTokyo, converter));
        // Calendar in New York (UTC‑5) – crossing midnight UTC should still give the local date
        Calendar calNY = createCalendar(2023, 1, 1, 0, 30, 0, 0, NEW_YORK);
        LocalDate expectedNY = LocalDate.of(2023, 1, 1);
        assertEquals(expectedNY, CalendarConversions.toLocalDate(calNY, converter));
        // Calendar in Paris (UTC+1) – a date near the end of the year
        Calendar calParis = createCalendar(1969, 12, 31, 23, 0, 0, 0, PARIS);
        LocalDate expectedParis = LocalDate.of(1969, 12, 31);
        assertEquals(expectedParis, CalendarConversions.toLocalDate(calParis, converter));
    }

    @Test
    void testStringToLocalDate() {
        assertEquals(LocalDate.of(2020, 2, 29), CalendarConversions.toLocalDate("2020-02-29", converter));
        assertEquals(LocalDate.of(1999, 12, 31), CalendarConversions.toLocalDate("1999-12-31", converter));
    }

    @Test
    void testLongEpochMilliToLocalDate() {
        // 2021‑04‑05T12:34:56.789Z → 2021‑04‑05 in UTC
        long epochMilli = Instant.parse("2021-04-05T12:34:56.789Z").toEpochMilli();
        assertEquals(LocalDate.of(2021, 4, 5), CalendarConversions.toLocalDate(epochMilli, converter));
        // Epoch start (1970‑01‑01)
        assertEquals(LocalDate.of(1970, 1, 1), CalendarConversions.toLocalDate(0L, converter));
    }

    @Test
    void testTimestampToLocalDate() {
        Timestamp ts = Timestamp.valueOf("2005-08-15 10:20:30.123");
        assertEquals(LocalDate.of(2005, 8, 15), CalendarConversions.toLocalDate(ts, converter));
    }

    @Test
    void testLocalDateTimeToLocalDate() {
        LocalDateTime ldt = LocalDateTime.of(2018, 11, 30, 23, 59, 59);
        assertEquals(LocalDate.of(2018, 11, 30), CalendarConversions.toLocalDate(ldt, converter));
    }

    @Test
    void testZonedDateTimeToLocalDate() {
        ZonedDateTime zdt = ZonedDateTime.of(2015, 5, 20, 5, 0, 0, 0, ZoneId.of("Europe/Paris"));
        assertEquals(LocalDate.of(2015, 5, 20), CalendarConversions.toLocalDate(zdt, converter));
    }

    @Test
    void testNullInputReturnsNull() {
        assertNull(CalendarConversions.toLocalDate(null, converter));
    }

    // ---------- toLocalDate_fromLocalDate ----------
    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("provideFromLocalDate")
    void toLocalDate_fromLocalDate(Object from, Converter conv, LocalDate expected) {
        assertEquals(expected, CalendarConversions.toLocalDate(from, conv));
    }

    // ---------- toLocalDate_fromLocalDateTime ----------
    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("provideFromLocalDateTime")
    void toLocalDate_fromLocalDateTime(Object from, Converter conv, LocalDate expected) {
        assertEquals(expected, CalendarConversions.toLocalDate(from, conv));
    }

    // ---------- toLocalDate_fromZonedDateTime ----------
    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("provideFromZonedDateTime")
    void toLocalDate_fromZonedDateTime(Object from, Converter conv, LocalDate expected) {
        assertEquals(expected, CalendarConversions.toLocalDate(from, conv));
    }

    // ---------- toLocalDate_fromInstant ----------
    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("provideFromInstant")
    void toLocalDate_fromInstant(Object from, Converter conv, LocalDate expected) {
        assertEquals(expected, CalendarConversions.toLocalDate(from, conv));
    }

    // ---------- toLocalDate_fromDate ----------
    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("provideFromDate")
    void toLocalDate_fromDate(Object from, Converter conv, LocalDate expected) {
        assertEquals(expected, CalendarConversions.toLocalDate(from, conv));
    }

    // ---------- toLocalDate_fromSqlDate ----------
    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("provideFromSqlDate")
    void toLocalDate_fromSqlDate(Object from, Converter conv, LocalDate expected) {
        assertEquals(expected, CalendarConversions.toLocalDate(from, conv));
    }

    // ---------- toLocalDate_fromCalendar ----------
    @ParameterizedTest(name = "[{index}] from=Calendar({0})")
    @MethodSource("provideFromCalendar")
    void toLocalDate_fromCalendar(Object from, Converter conv, LocalDate expected) {
        assertEquals(expected, CalendarConversions.toLocalDate(from, conv));
    }

    // ---------- toLocalDate_fromLongEpochMilli ----------
    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("provideFromLong")
    void toLocalDate_fromLong(Object from, Converter conv, LocalDate expected) {
        assertEquals(expected, CalendarConversions.toLocalDate(from, conv));
    }

    // ---------- toLocalDate_fromStringISODate ----------
    @ParameterizedTest(name = "[{index}] from=\"{0}\"")
    @MethodSource("provideFromStringISODate")
    void toLocalDate_fromStringISODate(Object from, Converter conv, LocalDate expected) {
        assertEquals(expected, CalendarConversions.toLocalDate(from, conv));
    }

    // ---------- toLocalDate_fromStringISODateTimeWithOffset ----------
    @ParameterizedTest(name = "[{index}] from=\"{0}\"")
    @MethodSource("provideFromStringISODateTimeWithOffset")
    void toLocalDate_fromStringISODateTimeWithOffset(Object from, Converter conv, LocalDate expected) {
        assertEquals(expected, CalendarConversions.toLocalDate(from, conv));
    }

    // ---------- toLocalDate_fromYearMonth ----------
    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("provideFromYearMonth")
    void toLocalDate_fromYearMonth(Object from, Converter conv, LocalDate expected) {
        assertEquals(expected, CalendarConversions.toLocalDate(from, conv));
    }

    // ---------- toLocalDate_fromYear ----------
    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("provideFromYear")
    void toLocalDate_fromYear(Object from, Converter conv, LocalDate expected) {
        assertEquals(expected, CalendarConversions.toLocalDate(from, conv));
    }

    // ---------- toLocalDate_fromInvalidString ----------
    @ParameterizedTest(name = "[{index}] from=\"{0}\" expects DateTimeParseException")
    @MethodSource("provideFromInvalidString")
    void toLocalDate_fromInvalidString(Object from, Converter conv) {
        assertThrows(DateTimeParseException.class, () -> CalendarConversions.toLocalDate(from, conv));
    }

    // ---------- toLocalDate_fromNull ----------
    @Test
    void toLocalDate_fromNull() {
        assertThrows(NullPointerException.class, () -> CalendarConversions.toLocalDate(null, null));
    }

    // ---------- toLocalDate_fromUnsupportedType ----------
    @ParameterizedTest(name = "[{index}] from={0} expects IllegalArgumentException")
    @MethodSource("provideFromUnsupportedType")
    void toLocalDate_fromUnsupportedType(Object from, Converter conv) {
        assertThrows(IllegalArgumentException.class, () -> CalendarConversions.toLocalDate(from, conv));
    }

    // ---------- toLocalDate_fromLocalTime ----------
    @ParameterizedTest(name = "[{index}] from={0} expects IllegalArgumentException")
    @MethodSource("provideFromLocalTime")
    void toLocalDate_fromLocalTime(Object from, Converter conv) {
        assertThrows(IllegalArgumentException.class, () -> CalendarConversions.toLocalDate(from, conv));
    }

    // ---------- toLocalDate_fromMonthDay ----------
    @ParameterizedTest(name = "[{index}] from={0} expects IllegalArgumentException")
    @MethodSource("provideFromMonthDay")
    void toLocalDate_fromMonthDay(Object from, Converter conv) {
        assertThrows(IllegalArgumentException.class, () -> CalendarConversions.toLocalDate(from, conv));
    }

    // ---------- toLocalDate_fromUnsupportedMap ----------
    @ParameterizedTest(name = "[{index}] from={0} expects IllegalArgumentException")
    @MethodSource("provideFromUnsupportedMap")
    void toLocalDate_fromUnsupportedMap(Object from, Converter conv) {
        assertThrows(IllegalArgumentException.class, () -> CalendarConversions.toLocalDate(from, conv));
    }

    // ---------- toLocalDate_fromStringWithInvalidOffset ----------
    @ParameterizedTest(name = "[{index}] from=\"{0}\" expects DateTimeParseException")
    @MethodSource("provideFromStringWithInvalidOffset")
    void toLocalDate_fromStringWithInvalidOffset(Object from, Converter conv) {
        assertThrows(DateTimeParseException.class, () -> CalendarConversions.toLocalDate(from, conv));
    }

    // ---------- toLocalDate_fromStringEmpty ----------
    @ParameterizedTest(name = "[{index}] from=\"{0}\" expects DateTimeParseException")
    @MethodSource("provideFromStringEmpty")
    void toLocalDate_fromStringEmpty(Object from, Converter conv) {
        assertThrows(DateTimeParseException.class, () -> CalendarConversions.toLocalDate(from, conv));
    }
}
