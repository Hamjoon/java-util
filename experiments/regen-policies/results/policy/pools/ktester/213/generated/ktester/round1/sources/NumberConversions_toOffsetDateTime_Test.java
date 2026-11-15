package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toOffsetDateTime_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Converter requires an options instance – the default constructor is sufficient for the built‑in conversions
        converter = new Converter(new ConverterOptions());
        // Most tests assume the system default zone is UTC for deterministic results.
        // Store the original zone and force UTC; it will be restored after each test.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        System.setProperty("user.timezone", "UTC");
    }

    @AfterEach
    void teardownAfterEach() {
        // Restore the original default zone (system default may have been changed by a test)
        // In a real‑world suite we would capture the original zone in @BeforeEach,
        // but for simplicity we just reset to UTC which is safe for the remaining tests.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        System.setProperty("user.timezone", "UTC");
    }

    private static Stream<Arguments> provideValidConversions() {
        return Stream.of(// ZonedDateTime – summer offset +02:00
        Arguments.of(ZonedDateTime.of(2023, 5, 10, 14, 30, 0, 0, ZoneId.of("Europe/Paris")), "2023-05-10T14:30+02:00"), // Already an OffsetDateTime
        Arguments.of(OffsetDateTime.of(2022, 12, 1, 9, 15, 0, 0, ZoneOffset.of("-05:00")), "2022-12-01T09:15-05:00"), // LocalDateTime – system default zone forced to UTC in @BeforeEach
        Arguments.of(LocalDateTime.of(2021, 1, 1, 0, 0), "2021-01-01T00:00Z"), // java.util.Date epoch 0
        Arguments.of(new Date(0L), "1970-01-01T00:00Z"), // java.sql.Timestamp
        Arguments.of(Timestamp.from(Instant.parse("2020-06-15T12:00:00Z")), "2020-06-15T12:00Z"), // Long epoch‑millis
        Arguments.of(1609459200000L, "2021-01-01T00:00Z"), // ISO‑8601 string with offset
        Arguments.of("2022-03-04T12:30:45+01:00", "2022-03-04T12:30:45+01:00"));
    }

    private static Calendar createCalendar(int year, int month, int day, int hour, int minute, int second, String zoneId) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(zoneId));
        cal.set(year, month - 1, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    private static Stream<Arguments> provideLocalDateAndYear() {
        return Stream.of(Arguments.of(LocalDate.of(2022, 12, 31), "2022-12-31T00:00Z"), Arguments.of(Year.of(2020), "2020-01-01T00:00Z"));
    }

    private static Stream<Arguments> provideStringParsing() {
        return Stream.of(// No offset – treat as UTC (system default zone is forced to UTC)
        Arguments.of("2023-07-01T15:45:30", "2023-07-01T15:45:30Z"), // Date‑only string – start of day UTC
        Arguments.of("2023-07-01", "2023-07-01T00:00Z"));
    }

    private static Stream<Arguments> provideInstantInputs() {
        return Stream.of(Arguments.of(Instant.ofEpochSecond(0L), "1970-01-01T00:00Z"), Arguments.of(Instant.ofEpochSecond(-86400L), "1969-12-31T00:00Z"));
    }

    @Test
    void toOffsetDateTime_fromZonedDateTime() {
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 10, 12, 30, 45, 0, ZoneId.of("Europe/Paris"));
        OffsetDateTime expected = zdt.toOffsetDateTime();
        OffsetDateTime actual = NumberConversions.toOffsetDateTime(zdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toOffsetDateTime_fromInstant() {
        // 2022-01-01T12:30:45Z
        Instant instant = Instant.ofEpochSecond(1_640_995_845L);
        OffsetDateTime expected = OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
        OffsetDateTime actual = NumberConversions.toOffsetDateTime(instant, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toOffsetDateTime_fromLocalDateTime() {
        // Converter will treat a LocalDateTime as being in the system default zone.
        // To make the test deterministic we set the default zone explicitly.
        ZoneId originalZone = ZoneId.systemDefault();
        try {
            ZoneId testZone = ZoneId.of("America/New_York");
            System.setProperty("user.timezone", testZone.getId());
            // Force the JVM to pick up the new default zone
            TimeZone.setDefault(TimeZone.getTimeZone(testZone));
            LocalDateTime ldt = LocalDateTime.of(2021, 12, 15, 8, 15, 30);
            ZonedDateTime zdt = ldt.atZone(testZone);
            OffsetDateTime expected = zdt.toOffsetDateTime();
            OffsetDateTime actual = NumberConversions.toOffsetDateTime(ldt, converter);
            assertEquals(expected, actual);
        } finally {
            // Restore original default zone
            System.setProperty("user.timezone", originalZone.getId());
            TimeZone.setDefault(TimeZone.getTimeZone(originalZone));
        }
    }

    @Test
    void toOffsetDateTime_fromDate() {
        // java.util.Date represents an instant in UTC
        Date date = Date.from(Instant.parse("2020-06-01T10:20:30Z"));
        OffsetDateTime expected = OffsetDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
        OffsetDateTime actual = NumberConversions.toOffsetDateTime(date, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toOffsetDateTime_fromCalendar() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"));
        cal.set(2019, Calendar.MARCH, 5, 14, 45, 0);
        cal.set(Calendar.MILLISECOND, 0);
        OffsetDateTime expected = OffsetDateTime.ofInstant(cal.toInstant(), cal.getTimeZone().toZoneId());
        OffsetDateTime actual = NumberConversions.toOffsetDateTime(cal, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toOffsetDateTime_fromStringIsoOffset() {
        String iso = "2022-11-03T09:15:00+01:00";
        OffsetDateTime expected = OffsetDateTime.parse(iso);
        OffsetDateTime actual = NumberConversions.toOffsetDateTime(iso, converter);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("provideValidConversions")
    void toOffsetDateTime_validConversions(Object from, String expectedIso) {
        // Converter argument is null per the JSON spec – the method does not need it for these types.
        OffsetDateTime expected = OffsetDateTime.parse(expectedIso);
        OffsetDateTime actual = NumberConversions.toOffsetDateTime(from, null);
        assertEquals(expected, actual);
    }

    @Test
    void toOffsetDateTime_nullSourceThrowsNPE() {
        assertThrows(NullPointerException.class, () -> NumberConversions.toOffsetDateTime(null, null));
    }

    @Test
    void toOffsetDateTime_longMinValueThrowsDateTimeException() {
        assertThrows(DateTimeException.class, () -> NumberConversions.toOffsetDateTime(Long.MIN_VALUE, null));
    }

    @ParameterizedTest
    @ValueSource(strings = { "true", "false" })
    void toOffsetDateTime_booleanUnsupported(boolean value) {
        Boolean bool = value;
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toOffsetDateTime(bool, null));
    }

    @Test
    void toOffsetDateTime_arbitraryObjectUnsupported() {
        Object obj = new Object();
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toOffsetDateTime(obj, null));
    }

    @Test
    void toOffsetDateTime_calendarConversion() {
        Calendar cal = createCalendar(2023, 3, 15, 12, 0, 0, "America/New_York");
        // The calendar represents 2023‑03‑15T12:00 in New York (UTC‑05:00)
        OffsetDateTime expected = OffsetDateTime.ofInstant(cal.toInstant(), cal.getTimeZone().toZoneId());
        OffsetDateTime actual = NumberConversions.toOffsetDateTime(cal, null);
        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @MethodSource("provideLocalDateAndYear")
    void toOffsetDateTime_localDateAndYear(Object from, String expectedIso) {
        OffsetDateTime expected = OffsetDateTime.parse(expectedIso);
        OffsetDateTime actual = NumberConversions.toOffsetDateTime(from, null);
        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @MethodSource("provideStringParsing")
    void toOffsetDateTime_stringParsing(String input, String expectedIso) {
        OffsetDateTime expected = OffsetDateTime.parse(expectedIso);
        OffsetDateTime actual = NumberConversions.toOffsetDateTime(input, null);
        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @MethodSource("provideInstantInputs")
    void toOffsetDateTime_instantInputs(Instant instant, String expectedIso) {
        OffsetDateTime expected = OffsetDateTime.parse(expectedIso);
        OffsetDateTime actual = NumberConversions.toOffsetDateTime(instant, null);
        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @ValueSource(strings = { "2023-02-30T10:00:00", "2023-07-01T25:00:00Z", "not-a-date" })
    void toOffsetDateTime_invalidStringThrows(String bad) {
        assertThrows(DateTimeParseException.class, () -> NumberConversions.toOffsetDateTime(bad, null));
    }

    @Test
    void toOffsetDateTime_localTimeUnsupported() {
        LocalTime lt = LocalTime.of(12, 30);
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toOffsetDateTime(lt, null));
    }

    @Test
    void toOffsetDateTime_offsetTimeUnsupported() {
        OffsetTime ot = OffsetTime.of(12, 30, 0, 0, ZoneOffset.ofHours(2));
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toOffsetDateTime(ot, null));
    }

    @Test
    void toOffsetDateTime_yearMonthUnsupported() {
        // YearMonth is not imported directly; construct via reflection to avoid compile error
        Object yearMonth = YearMonth.of(2023, 5);
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toOffsetDateTime(yearMonth, null));
    }

    @Test
    void toOffsetDateTime_bigDecimalOverflowThrows() {
        BigDecimal tooBig = new BigDecimal(Long.MAX_VALUE).add(BigDecimal.ONE);
        assertThrows(DateTimeException.class, () -> NumberConversions.toOffsetDateTime(tooBig, null));
    }

    @Test
    void toOffsetDateTime_fractionalNumberThrows() {
        // Using a Double with a fractional part – the conversion expects whole epoch‑millis.
        Double fractional = 1622548800.5;
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toOffsetDateTime(fractional, null));
    }

    @Test
    void toOffsetDateTime_atomicLongUnsupported() {
        AtomicLong atomic = new AtomicLong(Long.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toOffsetDateTime(atomic, null));
    }

    @Test
    void toOffsetDateTime_durationUnsupported() {
        Duration duration = Duration.ofHours(5);
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toOffsetDateTime(duration, null));
    }
}
