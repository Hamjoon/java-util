package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toLocalDateTime_Test {

    /**
     * The Converter used for all tests.
     */
    private Converter converter;

    /**
     * Preserve the original JVM default time‑zone so we can restore it after the suite runs.
     */
    private static TimeZone originalDefaultZone;

    @BeforeAll
    static void setupBeforeAll() {
        // Force a known time‑zone (UTC) for deterministic results.
        originalDefaultZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @BeforeEach
    void setupBeforeEach() {
        // The Converter has a public constructor that requires ConverterOptions.
        // An empty options instance gives us the default conversion behaviour.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // No resources to clean up for the current tests.
        converter = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore the original default zone to avoid side‑effects on other tests.
        TimeZone.setDefault(originalDefaultZone);
    }

    private static Stream<Arguments> zonedDateTimeProvider() {
        return Stream.of(Arguments.of("ZonedDateTime input (UTC)", ZonedDateTime.of(2023, 5, 10, 15, 30, 45, 0, ZoneOffset.UTC), "2023-05-10T15:30:45"), Arguments.of("ZonedDateTime input (America/New_York)", ZonedDateTime.of(2023, 5, 10, 15, 30, 45, 0, ZoneId.of("America/New_York")), "2023-05-10T15:30:45"));
    }

    private static Stream<Arguments> instantProvider() {
        return Stream.of(Arguments.of("Instant at epoch", Instant.ofEpochSecond(0), "1970-01-01T00:00:00"));
    }

    private static Stream<Arguments> dateProvider() {
        return Stream.of(Arguments.of("java.util.Date epoch millis 0", new Date(0L), "1970-01-01T00:00:00"));
    }

    private static Stream<Arguments> timestampProvider() {
        return Stream.of(Arguments.of("java.sql.Timestamp epoch", Timestamp.from(Instant.ofEpochSecond(0)), "1970-01-01T00:00:00"));
    }

    private static Stream<Arguments> longEpochProvider() {
        return Stream.of(Arguments.of("Long epoch‑millis 0", 0L, "1970-01-01T00:00:00"), Arguments.of("Negative epoch‑millis (-86400000)", -86400000L, "1969-12-31T00:00:00"));
    }

    private static Stream<Arguments> isoStringProvider() {
        return Stream.of(Arguments.of("ISO‑8601 without offset", "2023-08-15T12:34:56", "2023-08-15T12:34:56"), Arguments.of("ISO‑8601 with offset +02:00 (offset discarded)", "2023-08-15T12:34:56+02:00", "2023-08-15T12:34:56"), Arguments.of("ISO‑8601 with Z designator", "2023-08-15T12:34:56Z", "2023-08-15T12:34:56"), Arguments.of("Date‑only string", "2023-08-15", "2023-08-15T00:00:00"), Arguments.of("String with fractional seconds", "2023-08-15T12:34:56.123", "2023-08-15T12:34:56.123"));
    }

    private static Stream<Arguments> localDateTimeProvider() {
        return Stream.of(Arguments.of("LocalDateTime input", LocalDateTime.of(2022, 12, 31, 23, 59, 59), "2022-12-31T23:59:59"));
    }

    private static Stream<Arguments> localDateProvider() {
        return Stream.of(Arguments.of("LocalDate input", LocalDate.of(2022, 1, 1), "2022-01-01T00:00:00"));
    }

    private static Stream<Arguments> offsetDateTimeProvider() {
        return Stream.of(Arguments.of("OffsetDateTime with +02:00 offset", OffsetDateTime.of(2021, 7, 20, 10, 15, 30, 0, ZoneOffset.ofHours(2)), "2021-07-20T10:15:30"));
    }

    private static Stream<Arguments> calendarProvider() {
        Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCal.setTimeInMillis(0L);
        Calendar laCal = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"));
        laCal.setTimeInMillis(0L);
        return Stream.of(Arguments.of("Calendar UTC epoch", utcCal, "1970-01-01T00:00:00"), Arguments.of("Calendar America/Los_Angeles epoch", laCal, "1969-12-31T16:00:00"));
    }

    private static Stream<Arguments> atomicLongProvider() {
        return Stream.of(Arguments.of("AtomicLong epoch‑millis 0", new AtomicLong(0L), "1970-01-01T00:00:00"));
    }

    private static Stream<Arguments> unparseableStringProvider() {
        return Stream.of(Arguments.of("Unparseable string", "not-a-date"), Arguments.of("Invalid ISO‑8601 variant", "2023/08/15 12:34:56"), Arguments.of("Empty string", ""), Arguments.of("Time‑only string", "12:34:56"));
    }

    private static Stream<Arguments> unsupportedTypeProvider() {
        return Stream.of(Arguments.of("Unsupported generic Object", new Object()), Arguments.of("LocalTime (no date)", LocalTime.of(12, 34, 56)), Arguments.of("java.sql.Time (time only)", new java.sql.Time(0L)), Arguments.of("OffsetTime (no date)", OffsetTime.of(12, 34, 56, 0, ZoneOffset.ofHours(2))), Arguments.of("BigDecimal", new BigDecimal("12345")), Arguments.of("BigInteger", new BigInteger("12345")), Arguments.of("AtomicInteger", new AtomicInteger(0)), Arguments.of("YearMonth", YearMonth.of(2023, 8)), Arguments.of("UUID", UUID.randomUUID()));
    }

    @Test
    void shouldReturnNullWhenSourceIsNull() {
        // given
        Object source = null;
        // when
        LocalDateTime result = LocalDateConversions.toLocalDateTime(source, converter);
        // then
        assertNull(result, "Converting a null source should yield null");
    }

    @Test
    void shouldReturnSameLocalDateTimeWhenSourceIsAlreadyLocalDateTime() {
        // given
        LocalDateTime source = LocalDateTime.of(2022, Month.JANUARY, 2, 3, 4, 5, 0);
        // when
        LocalDateTime result = LocalDateConversions.toLocalDateTime(source, converter);
        // then
        assertEquals(source, result, "A LocalDateTime source should be returned unchanged");
    }

    @Test
    void shouldConvertUtilDateToLocalDateTime() {
        // given – a java.util.Date representing an instant in UTC
        Instant instant = Instant.parse("2022-01-02T03:04:05Z");
        Date source = Date.from(instant);
        // when
        LocalDateTime result = LocalDateConversions.toLocalDateTime(source, converter);
        // then
        LocalDateTime expected = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        assertEquals(expected, result, "Date should be converted to the correct LocalDateTime");
    }

    @Test
    void shouldConvertSqlTimestampToLocalDateTime() {
        // given – a java.sql.Timestamp representing the same instant as above
        Timestamp source = Timestamp.from(Instant.parse("2022-01-02T03:04:05Z"));
        // when
        LocalDateTime result = LocalDateConversions.toLocalDateTime(source, converter);
        // then
        LocalDateTime expected = LocalDateTime.of(2022, 1, 2, 3, 4, 5);
        assertEquals(expected, result, "Timestamp should be converted to the correct LocalDateTime");
    }

    @Test
    void shouldParseIsoStringToLocalDateTime() {
        // given – an ISO‑8601 date‑time string (the default format understood by Converter)
        String source = "2022-01-02T03:04:05";
        // when
        LocalDateTime result = LocalDateConversions.toLocalDateTime(source, converter);
        // then
        LocalDateTime expected = LocalDateTime.of(2022, 1, 2, 3, 4, 5);
        assertEquals(expected, result, "ISO string should be parsed into the correct LocalDateTime");
    }

    @Test
    void shouldConvertLocalDateToStartOfDayLocalDateTime() {
        // given – a LocalDate (no time component)
        LocalDate source = LocalDate.of(2022, 1, 2);
        // when
        LocalDateTime result = LocalDateConversions.toLocalDateTime(source, converter);
        // then – the conversion should treat the date as start of day (midnight) in the default zone (UTC)
        LocalDateTime expected = LocalDateTime.of(2022, 1, 2, 0, 0);
        assertEquals(expected, result, "LocalDate should be converted to start‑of‑day LocalDateTime");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("zonedDateTimeProvider")
    void shouldConvertZonedDateTime(String description, Object from, String expectedIso) {
        LocalDateTime result = LocalDateConversions.toLocalDateTime(from, null);
        LocalDateTime expected = LocalDateTime.parse(expectedIso);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("instantProvider")
    void shouldConvertInstant(String description, Object from, String expectedIso) {
        LocalDateTime result = LocalDateConversions.toLocalDateTime(from, null);
        LocalDateTime expected = LocalDateTime.parse(expectedIso);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("dateProvider")
    void shouldConvertUtilDate(String description, Object from, String expectedIso) {
        LocalDateTime result = LocalDateConversions.toLocalDateTime(from, null);
        LocalDateTime expected = LocalDateTime.parse(expectedIso);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("timestampProvider")
    void shouldConvertSqlTimestamp(String description, Object from, String expectedIso) {
        LocalDateTime result = LocalDateConversions.toLocalDateTime(from, null);
        LocalDateTime expected = LocalDateTime.parse(expectedIso);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("longEpochProvider")
    void shouldConvertLongEpochMillis(String description, Object from, String expectedIso) {
        LocalDateTime result = LocalDateConversions.toLocalDateTime(from, null);
        LocalDateTime expected = LocalDateTime.parse(expectedIso);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("isoStringProvider")
    void shouldParseIsoString(String description, Object from, String expectedIso) {
        LocalDateTime result = LocalDateConversions.toLocalDateTime(from, null);
        LocalDateTime expected = LocalDateTime.parse(expectedIso);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("localDateTimeProvider")
    void shouldPassThroughLocalDateTime(String description, Object from, String expectedIso) {
        LocalDateTime result = LocalDateConversions.toLocalDateTime(from, null);
        LocalDateTime expected = LocalDateTime.parse(expectedIso);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("localDateProvider")
    void shouldConvertLocalDate(String description, Object from, String expectedIso) {
        LocalDateTime result = LocalDateConversions.toLocalDateTime(from, null);
        LocalDateTime expected = LocalDateTime.parse(expectedIso);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("offsetDateTimeProvider")
    void shouldConvertOffsetDateTime(String description, Object from, String expectedIso) {
        LocalDateTime result = LocalDateConversions.toLocalDateTime(from, null);
        LocalDateTime expected = LocalDateTime.parse(expectedIso);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("calendarProvider")
    void shouldConvertCalendar(String description, Calendar calendar, String expectedIso) {
        LocalDateTime result = LocalDateConversions.toLocalDateTime(calendar, null);
        LocalDateTime expected = LocalDateTime.parse(expectedIso);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("atomicLongProvider")
    void shouldConvertAtomicLong(String description, AtomicLong atomicLong, String expectedIso) {
        LocalDateTime result = LocalDateConversions.toLocalDateTime(atomicLong, null);
        LocalDateTime expected = LocalDateTime.parse(expectedIso);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("unparseableStringProvider")
    void shouldThrowDateTimeParseExceptionForBadString(String description, String source) {
        assertThrows(DateTimeParseException.class, () -> LocalDateConversions.toLocalDateTime(source, null), description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("unsupportedTypeProvider")
    void shouldThrowConversionExceptionForUnsupportedType(String description, Object source) {
        assertThrows(ConversionException.class, () -> LocalDateConversions.toLocalDateTime(source, null), description);
    }

    @Test
    void shouldThrowNullPointerExceptionWhenBothArgumentsAreNull() {
        // According to the JSON spec a null source (and null converter) should raise NPE.
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toLocalDateTime(null, null), "Both arguments null should trigger NullPointerException");
    }
}
