package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.sql.Timestamp;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Stream;

class NumberConversions_toLocalDateTime_Test {

    private Converter converter;

    private static ZoneId originalDefaultZone;

    @BeforeAll
    static void setupBeforeAll() {
        // Force the JVM default zone to UTC so that tests that rely on the
        // system default zone behave deterministically.
        originalDefaultZone = ZoneId.systemDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @BeforeEach
    void setupBeforeEach() {
        // The Converter has a public constructor that requires a ConverterOptions instance.
        // Using the default options is sufficient for these tests.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore the original default zone to avoid side‑effects for other tests.
        TimeZone.setDefault(TimeZone.getTimeZone(originalDefaultZone));
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// LocalDateTime (already a LocalDateTime)
        Arguments.of(LocalDateTime.of(2023, 5, 10, 15, 30, 45), "2023-05-10T15:30:45"), // ZonedDateTime (UTC)
        Arguments.of(ZonedDateTime.of(2023, 5, 10, 15, 30, 45, 0, ZoneId.of("UTC")), "2023-05-10T15:30:45"), // OffsetDateTime (+02:00)
        Arguments.of(OffsetDateTime.of(2023, 5, 10, 15, 30, 45, 0, ZoneOffset.ofHours(2)), "2023-05-10T15:30:45"), // Timestamp with nanoseconds
        Arguments.of(Timestamp.valueOf("2023-05-10 15:30:45.123456789"), "2023-05-10T15:30:45.123456789"), // Instant with nanoseconds (epoch seconds + nanos)
        Arguments.of(Instant.ofEpochSecond(1683731445L, 123456789L), "2023-05-10T15:30:45.123456789"), // java.util.Date (UTC instant)
        Arguments.of(Date.from(Instant.parse("2023-05-10T15:30:45Z")), "2023-05-10T15:30:45"), // Calendar (UTC)
        Arguments.of(createUtcCalendar(2023, Calendar.MAY, 10, 15, 30, 45), "2023-05-10T15:30:45"), // LocalDate (midnight)
        Arguments.of(LocalDate.of(2023, 5, 10), "2023-05-10T00:00"), // LocalTime (combined with epoch date)
        Arguments.of(LocalTime.of(15, 30, 45), "1970-01-01T15:30:45"));
    }

    private static Stream<Arguments> edgeCaseProvider() {
        return Stream.of(// null input
        Arguments.of(null, null), // java.sql.Date (date only)
        Arguments.of(java.sql.Date.valueOf("2023-05-10"), "2023-05-10T00:00"), // Year
        Arguments.of(Year.of(2023), "2023-01-01T00:00"), // OffsetTime (ignore offset, combine with epoch)
        Arguments.of(OffsetTime.of(15, 30, 45, 123456789, ZoneOffset.ofHours(-5)), "1970-01-01T15:30:45.123456789"), // ZonedDateTime (non‑UTC zone)
        Arguments.of(ZonedDateTime.of(2023, 5, 10, 15, 30, 45, 0, ZoneId.of("America/New_York")), "2023-05-10T15:30:45"), // Calendar (America/Los_Angeles)
        Arguments.of(createCalendarInZone("America/Los_Angeles", 2023, Calendar.MAY, 10, 15, 30, 45), "2023-05-10T15:30:45"), // Instant.EPOCH
        Arguments.of(Instant.EPOCH, "1970-01-01T00:00"), // LocalTime.MIDNIGHT
        Arguments.of(LocalTime.MIDNIGHT, "1970-01-01T00:00"), // Leap‑day LocalDate
        Arguments.of(LocalDate.of(2020, 2, 29), "2020-02-29T00:00"), // Timestamp with zero nanos
        Arguments.of(Timestamp.valueOf("2023-05-10 15:30:45.000000000"), "2023-05-10T15:30:45"));
    }

    private static Stream<Arguments> unsupportedSourceProvider() {
        return Stream.of(Arguments.of(new Object(), IllegalArgumentException.class), Arguments.of("just a plain string", IllegalArgumentException.class), Arguments.of(Boolean.TRUE, IllegalArgumentException.class), Arguments.of(Integer.valueOf(42), IllegalArgumentException.class), Arguments.of(YearMonth.of(2023, 5), IllegalArgumentException.class));
    }

    private static Stream<Arguments> malformedStringProvider() {
        return Stream.of(Arguments.of("2023-02-30T10:15:30", DateTimeParseException.class), Arguments.of("2023-13-01T00:00:00", DateTimeParseException.class), Arguments.of("not-a-date-time", DateTimeParseException.class));
    }

    private static Stream<Arguments> outOfRangeProvider() {
        // Calendar set to year 10000 (beyond LocalDateTime range)
        Calendar farFuture = Calendar.getInstance();
        farFuture.set(10000, Calendar.JANUARY, 1, 0, 0, 0);
        farFuture.set(Calendar.MILLISECOND, 0);
        // LocalDate with an absurd year
        LocalDate absurdYear = LocalDate.of(999_999_999, 1, 1);
        return Stream.of(Arguments.of(farFuture, DateTimeException.class), Arguments.of(absurdYear, DateTimeException.class));
    }

    /**
     * Creates a UTC {@link Calendar} set to the supplied date‑time fields.
     */
    private static Calendar createUtcCalendar(int year, int month, int day, int hour, int minute, int second) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setLenient(false);
        cal.set(year, month, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    /**
     * Creates a {@link Calendar} in the given zone with the supplied fields.
     */
    private static Calendar createCalendarInZone(String zoneId, int year, int month, int day, int hour, int minute, int second) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(zoneId));
        cal.setLenient(false);
        cal.set(year, month, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    @Test
    void toLocalDateTime_givenNull_returnsNull() {
        assertNull(NumberConversions.toLocalDateTime(null, converter));
    }

    @Test
    void toLocalDateTime_givenLocalDateTime_returnsSameInstance() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime result = NumberConversions.toLocalDateTime(now, converter);
        assertEquals(now, result);
    }

    @Test
    void toLocalDateTime_givenDate_convertsCorrectly() {
        // current instant
        Date date = new Date();
        LocalDateTime expected = Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime result = NumberConversions.toLocalDateTime(date, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDateTime_givenTimestamp_convertsCorrectly() {
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        LocalDateTime expected = ts.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime result = NumberConversions.toLocalDateTime(ts, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDateTime_givenLocalDate_convertsToStartOfDay() {
        LocalDate localDate = LocalDate.of(2023, Month.MARCH, 15);
        LocalDateTime expected = localDate.atStartOfDay();
        LocalDateTime result = NumberConversions.toLocalDateTime(localDate, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDateTime_givenLocalTime_convertsToTodayWithTime() {
        LocalTime localTime = LocalTime.of(13, 45, 30);
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        LocalDateTime expected = LocalDateTime.of(today, localTime);
        LocalDateTime result = NumberConversions.toLocalDateTime(localTime, converter);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] from={0} → expected={1}")
    @MethodSource("validConversionProvider")
    void toLocalDateTime_validConversions(Object from, String expectedIso) {
        LocalDateTime expected = LocalDateTime.parse(expectedIso);
        LocalDateTime result = NumberConversions.toLocalDateTime(from, null);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] edge case from={0}")
    @MethodSource("edgeCaseProvider")
    void toLocalDateTime_edgeCases(Object from, String expectedIso) {
        if (expectedIso == null) {
            assertNull(NumberConversions.toLocalDateTime(from, null));
        } else {
            LocalDateTime expected = LocalDateTime.parse(expectedIso);
            LocalDateTime result = NumberConversions.toLocalDateTime(from, null);
            assertEquals(expected, result);
        }
    }

    @ParameterizedTest(name = "[{index}] unsupported from={0}")
    @MethodSource("unsupportedSourceProvider")
    void toLocalDateTime_unsupportedSources(Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> NumberConversions.toLocalDateTime(from, null));
    }

    @ParameterizedTest(name = "[{index}] malformed string=\"{0}\"")
    @MethodSource("malformedStringProvider")
    void toLocalDateTime_malformedStrings(String input, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> NumberConversions.toLocalDateTime(input, null));
    }

    @ParameterizedTest(name = "[{index}] out‑of‑range from={0}")
    @MethodSource("outOfRangeProvider")
    void toLocalDateTime_outOfRangeValues(Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> NumberConversions.toLocalDateTime(from, null));
    }
}
