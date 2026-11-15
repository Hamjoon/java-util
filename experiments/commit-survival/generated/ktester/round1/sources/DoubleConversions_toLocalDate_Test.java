package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.sql.Timestamp;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.Mockito;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class DoubleConversions_toLocalDate_Test {

    /**
     * Shared converter used by all test cases.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; the default constructor provides
        // the standard set of built‑in conversions.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly necessary but keeps the pattern tidy.
        converter = null;
    }

    private static Stream<Arguments> standardTemporalProvider() {
        return Stream.of(Arguments.of("LocalDate is returned unchanged", LocalDate.of(2023, 5, 10), LocalDate.of(2023, 5, 10)), Arguments.of("LocalDateTime is truncated to its date part", LocalDateTime.of(2023, 5, 10, 15, 30, 45), LocalDate.of(2023, 5, 10)), Arguments.of("ZonedDateTime yields its own LocalDate", ZonedDateTime.of(2023, 5, 10, 23, 0, 0, 0, ZoneId.of("Europe/Paris")), LocalDate.of(2023, 5, 10)), Arguments.of("Instant is converted using the system default zone", Instant.parse("2023-05-10T12:00:00Z"), Instant.parse("2023-05-10T12:00:00Z").atZone(ZoneId.systemDefault()).toLocalDate()), Arguments.of("java.util.Date follows the system default zone", Date.from(Instant.parse("2023-05-10T12:00:00Z")), Instant.parse("2023-05-10T12:00:00Z").atZone(ZoneId.systemDefault()).toLocalDate()), Arguments.of("java.sql.Timestamp (sub‑class of Date) works the same way", Timestamp.valueOf(LocalDateTime.of(2023, 5, 10, 8, 15)), LocalDate.of(2023, 5, 10)), Arguments.of("Calendar uses its own time‑zone (UTC in this case)", createUtcCalendar(2023, Calendar.MAY, 10, 0, 0, 0), ZonedDateTime.ofInstant(Instant.parse("2023-05-10T00:00:00Z"), ZoneId.of("UTC")).toLocalDate()), Arguments.of("Long epoch‑millis is interpreted as an instant", Long.valueOf(Instant.parse("2023-05-10T12:00:00Z").toEpochMilli()), Instant.parse("2023-05-10T12:00:00Z").atZone(ZoneId.systemDefault()).toLocalDate()), Arguments.of("ISO‑8601 string is parsed to an instant/ZonedDateTime", "2023-05-10T12:00:00Z", Instant.parse("2023-05-10T12:00:00Z").atZone(ZoneId.systemDefault()).toLocalDate()));
    }

    private static Stream<Arguments> additionalTemporalProvider() {
        return Stream.of(Arguments.of("OffsetDateTime in UTC yields the same date", OffsetDateTime.of(2023, 5, 10, 15, 30, 0, 0, ZoneOffset.UTC), LocalDate.of(2023, 5, 10)), Arguments.of("OffsetDateTime with -02:00 offset moves the date forward", OffsetDateTime.of(2023, 5, 10, 23, 30, 0, 0, ZoneOffset.ofHours(-2)), // 2023‑05‑11 in the default (UTC) zone
        OffsetDateTime.of(2023, 5, 10, 23, 30, 0, 0, ZoneOffset.ofHours(-2)).atZoneSameInstant(ZoneOffset.UTC).toLocalDate()), Arguments.of("java.sql.Date is interpreted via its epoch millis", new java.sql.Date(Instant.parse("2023-05-10T12:00:00Z").toEpochMilli()), LocalDate.of(2023, 5, 10)), Arguments.of("LocalTime is treated as time on the epoch day", LocalTime.of(15, 30, 45), LocalDate.of(1970, 1, 1)), Arguments.of("Plain ISO‑8601 date string parses directly to LocalDate", "2023-05-10", LocalDate.of(2023, 5, 10)), Arguments.of("ISO‑8601 date‑time with +02:00 offset shifts the date when viewed in UTC", "2023-05-10T00:00:00+02:00", // 2023‑05‑09T22:00Z → 2023‑05‑09 in UTC
        Instant.parse("2023-05-09T22:00:00Z").atZone(ZoneId.systemDefault()).toLocalDate()));
    }

    private static Stream<Arguments> epochBoundaryProvider() {
        return Stream.of(Arguments.of("Zero epoch millis → 1970‑01‑01", Long.valueOf(0L), LocalDate.of(1970, 1, 1)), Arguments.of("Negative epoch millis (one day before epoch) → 1969‑12‑31", Long.valueOf(-86_400_000L), LocalDate.of(1969, 12, 31)), Arguments.of("Instant just before midnight UTC on epoch day", Instant.parse("1970-01-01T23:59:59.999Z"), LocalDate.of(1970, 1, 1)));
    }

    private static Stream<Arguments> unsupportedValueProvider() {
        return Stream.of(Arguments.of("Boolean is not convertible", Boolean.TRUE, ConversionException.class), Arguments.of("Malformed date‑time string", "not-a-date-string", ConversionException.class), Arguments.of("Double is unsupported numeric type", Double.valueOf(123.45), ConversionException.class), Arguments.of("Empty string cannot be parsed", "", ConversionException.class), Arguments.of("YearMonth lacks day component", YearMonth.of(2023, 5), ConversionException.class), Arguments.of("Integer is unsupported", Integer.valueOf(12345), ConversionException.class), Arguments.of("ZoneId has no date‑time information", ZoneId.of("UTC"), ConversionException.class), Arguments.of("UUID is unrelated type", UUID.randomUUID(), ConversionException.class), Arguments.of("OffsetTime lacks date component", OffsetTime.of(15, 30, 0, 0, ZoneOffset.UTC), ConversionException.class), Arguments.of("java.sql.Time represents only a time", java.sql.Time.valueOf("12:34:56"), ConversionException.class));
    }

    /**
     * Creates a {@link Calendar} in UTC set to the supplied date‑time components.
     */
    private static Calendar createUtcCalendar(int year, int month, int day, int hour, int minute, int second) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(year, month, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    @Test
    void toLocalDate_fromLocalDate_returnsSameInstance() {
        LocalDate source = LocalDate.of(2022, 3, 15);
        LocalDate result = DoubleConversions.toLocalDate(source, converter);
        assertEquals(source, result);
    }

    @Test
    void toLocalDate_fromLocalDateTime_truncatesToDate() {
        LocalDateTime source = LocalDateTime.of(2022, 3, 15, 10, 30, 45);
        LocalDate expected = source.toLocalDate();
        LocalDate result = DoubleConversions.toLocalDate(source, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDate_fromZonedDateTime_respectsZone() {
        ZonedDateTime source = ZonedDateTime.of(2022, 3, 15, 23, 0, 0, 0, ZoneId.of("America/New_York"));
        LocalDate expected = source.toLocalDate();
        LocalDate result = DoubleConversions.toLocalDate(source, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDate_fromOffsetDateTime_respectsOffset() {
        OffsetDateTime source = OffsetDateTime.of(2022, 3, 15, 5, 0, 0, 0, ZoneOffset.ofHours(-4));
        LocalDate expected = source.toLocalDate();
        LocalDate result = DoubleConversions.toLocalDate(source, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDate_fromInstant_convertsUsingSystemDefaultZone() {
        Instant instant = Instant.parse("2022-03-15T12:34:56Z");
        // The conversion performed by DoubleConversions uses ZonedDateTime.ofInstant
        // with the system default zone.
        ZonedDateTime zdt = instant.atZone(ZoneId.systemDefault());
        LocalDate expected = zdt.toLocalDate();
        LocalDate result = DoubleConversions.toLocalDate(instant, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDate_fromDate_convertsUsingSystemDefaultZone() {
        // java.util.Date stores an instant in UTC.
        Date source = Date.from(Instant.parse("2022-03-15T08:00:00Z"));
        ZonedDateTime zdt = source.toInstant().atZone(ZoneId.systemDefault());
        LocalDate expected = zdt.toLocalDate();
        LocalDate result = DoubleConversions.toLocalDate(source, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDate_fromSqlTimestamp_convertsUsingSystemDefaultZone() {
        Timestamp source = Timestamp.from(Instant.parse("2022-03-15T23:59:59Z"));
        ZonedDateTime zdt = source.toInstant().atZone(ZoneId.systemDefault());
        LocalDate expected = zdt.toLocalDate();
        LocalDate result = DoubleConversions.toLocalDate(source, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDate_fromCalendar_convertsUsingCalendarTimeZone() {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("Europe/Paris"));
        cal.set(2022, Calendar.MARCH, 15, 12, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        ZonedDateTime zdt = cal.toInstant().atZone(ZoneId.of("Europe/Paris"));
        LocalDate expected = zdt.toLocalDate();
        LocalDate result = DoubleConversions.toLocalDate(cal, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDate_fromDuration_convertsToEpochDay() {
        // Duration of 2022‑03‑15 days since epoch (1970‑01‑01)
        long daysSinceEpoch = LocalDate.of(2022, 3, 15).toEpochDay();
        Duration source = Duration.ofDays(daysSinceEpoch);
        // DoubleConversions first turns the Duration into an Instant at epoch + duration.
        Instant instant = Instant.EPOCH.plus(source);
        LocalDate expected = instant.atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate result = DoubleConversions.toLocalDate(source, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDate_nullInputReturnsNull() {
        assertNull(DoubleConversions.toLocalDate(null, converter));
    }

    @Test
    void toLocalDate_unsupportedTypeThrowsException() {
        // No conversion rule for plain Object → LocalDate
        Object unsupported = new Object();
        Assertions.assertThrows(IllegalArgumentException.class, () -> DoubleConversions.toLocalDate(unsupported, converter));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("standardTemporalProvider")
    void toLocalDate_standardTemporal(String description, Object source, LocalDate expected) {
        LocalDate result = DoubleConversions.toLocalDate(source, converter);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("additionalTemporalProvider")
    void toLocalDate_additionalTemporal(String description, Object source, LocalDate expected) {
        LocalDate result = DoubleConversions.toLocalDate(source, converter);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("epochBoundaryProvider")
    void toLocalDate_epochBoundary(String description, Object source, LocalDate expected) {
        LocalDate result = DoubleConversions.toLocalDate(source, converter);
        assertEquals(expected, result, description);
    }

    @Test
    void toLocalDate_nullSourceThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> DoubleConversions.toLocalDate(null, converter));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("unsupportedValueProvider")
    void toLocalDate_unsupportedValues(String description, Object source, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> DoubleConversions.toLocalDate(source, converter), description);
    }

    @Test
    void toLocalDate_calendarWithNonUtcZone() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        cal.set(2023, Calendar.MAY, 10, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        LocalDate expected = cal.toInstant().atZone(ZoneId.of("America/New_York")).toLocalDate();
        LocalDate result = DoubleConversions.toLocalDate(cal, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDate_converterThrowsConversionException_isPropagated() {
        // Create a mock Converter that throws ConversionException for any conversion attempt.
        Converter throwingConverter = Mockito.mock(Converter.class);
        doThrow(new ConversionException("forced failure")).when(throwingConverter).convert(any(), any());
        // The source is a valid ISO‑8601 string; the failure should come from the mock.
        String source = "2023-05-10T12:00:00Z";
        assertThrows(ConversionException.class, () -> DoubleConversions.toLocalDate(source, throwingConverter));
    }
}
