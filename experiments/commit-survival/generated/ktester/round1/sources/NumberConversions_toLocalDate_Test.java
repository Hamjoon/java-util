package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.sql.Timestamp;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toLocalDate_Test {

    /**
     * The Converter instance used for all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance – use the default constructor.
        // The class is part of the same library, so we can instantiate it directly.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static Converter reference.
        converter = null;
    }

    /**
     * ---------- Conversion from java.util.Date ----------
     */
    static Stream<Arguments> dateConversionProvider() {
        return Stream.of(Arguments.of(Date.from(Instant.parse("2023-04-15T10:20:30Z")), LocalDate.of(2023, 4, 15), "Date is converted to ZonedDateTime using system default zone, then to LocalDate."), Arguments.of(// leap day
        Date.from(Instant.parse("2020-02-29T23:59:59Z")), LocalDate.of(2020, 2, 29), "Ensures leap‑day handling is correct when converting from Date."));
    }

    /**
     * ---------- Conversion from java.time.LocalDateTime ----------
     */
    static Stream<Arguments> localDateTimeProvider() {
        return Stream.of(Arguments.of(LocalDateTime.of(2021, 12, 31, 23, 59, 59), LocalDate.of(2021, 12, 31), "LocalDateTime at the end of the year should be converted to the same date part."), Arguments.of(LocalDateTime.of(2022, 2, 28, 0, 0, 0), LocalDate.of(2022, 2, 28), "Standard non‑leap‑year date conversion."));
    }

    /**
     * ---------- Unsupported type conversion ----------
     */
    static Stream<Arguments> unsupportedTypeProvider() {
        return Stream.of(Arguments.of("some random string", "String"), Arguments.of(new Object(), "Object"));
    }

    @Test
    void givenLocalDate_returnsSameDate() {
        // GIVEN
        LocalDate source = LocalDate.of(2021, Month.MARCH, 15);
        // WHEN
        LocalDate result = NumberConversions.toLocalDate(source, converter);
        // THEN
        assertEquals(source, result);
    }

    @Test
    void givenLocalDateTime_convertsToDatePart() {
        // GIVEN
        LocalDateTime source = LocalDateTime.of(2022, Month.JULY, 4, 13, 45, 30);
        LocalDate expected = source.toLocalDate();
        // WHEN
        LocalDate result = NumberConversions.toLocalDate(source, converter);
        // THEN
        assertEquals(expected, result);
    }

    @Test
    void givenZonedDateTime_convertsToLocalDate() {
        // GIVEN
        ZonedDateTime source = ZonedDateTime.of(2020, 12, 31, 23, 59, 59, 0, ZoneId.of("America/New_York"));
        LocalDate expected = source.toLocalDate();
        // WHEN
        LocalDate result = NumberConversions.toLocalDate(source, converter);
        // THEN
        assertEquals(expected, result);
    }

    @Test
    void givenOffsetDateTime_convertsToLocalDate() {
        // GIVEN
        OffsetDateTime source = OffsetDateTime.of(2019, 2, 28, 10, 20, 30, 0, ZoneOffset.ofHours(-5));
        LocalDate expected = source.toLocalDate();
        // WHEN
        LocalDate result = NumberConversions.toLocalDate(source, converter);
        // THEN
        assertEquals(expected, result);
    }

    @Test
    void givenInstant_convertsToLocalDateUsingSystemDefaultZone() {
        // GIVEN
        Instant instant = Instant.parse("2000-01-01T12:34:56Z");
        // The conversion ultimately goes through toZonedDateTime which uses the
        // system default time‑zone.  We compute the expected value accordingly.
        ZoneId systemZone = ZoneId.systemDefault();
        LocalDate expected = instant.atZone(systemZone).toLocalDate();
        // WHEN
        LocalDate result = NumberConversions.toLocalDate(instant, converter);
        // THEN
        assertEquals(expected, result);
    }

    @Test
    void givenDate_convertsToLocalDateUsingSystemDefaultZone() {
        // GIVEN
        Instant instant = Instant.parse("1995-05-23T08:15:00Z");
        Date source = Date.from(instant);
        ZoneId systemZone = ZoneId.systemDefault();
        LocalDate expected = instant.atZone(systemZone).toLocalDate();
        // WHEN
        LocalDate result = NumberConversions.toLocalDate(source, converter);
        // THEN
        assertEquals(expected, result);
    }

    @Test
    void givenCalendar_convertsToLocalDate() {
        // GIVEN
        ZonedDateTime zdt = ZonedDateTime.of(2018, 11, 30, 5, 0, 0, 0, ZoneId.of("Europe/Paris"));
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(zdt.toInstant().toEpochMilli());
        LocalDate expected = zdt.toLocalDate();
        // WHEN
        LocalDate result = NumberConversions.toLocalDate(cal, converter);
        // THEN
        assertEquals(expected, result);
    }

    @Test
    void givenYear_convertsToFirstDayOfYear() {
        // GIVEN
        Year source = Year.of(2025);
        LocalDate expected = LocalDate.of(2025, 1, 1);
        // WHEN
        LocalDate result = NumberConversions.toLocalDate(source, converter);
        // THEN
        assertEquals(expected, result);
    }

    @Test
    void givenLongEpochMilli_convertsToLocalDateUsingSystemDefaultZone() {
        // GIVEN
        // 2022-01-01T00:00:00Z
        long epochMilli = 1_640_995_200_000L;
        ZoneId systemZone = ZoneId.systemDefault();
        LocalDate expected = Instant.ofEpochMilli(epochMilli).atZone(systemZone).toLocalDate();
        // WHEN
        LocalDate result = NumberConversions.toLocalDate(epochMilli, converter);
        // THEN
        assertEquals(expected, result);
    }

    @Test
    void givenNullReturnsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        LocalDate result = NumberConversions.toLocalDate(source, converter);
        // THEN
        assertNull(result);
    }

    @Test
    void givenUnsupportedTypeThrowsException() {
        // GIVEN
        // No conversion rule for plain Object -> ZonedDateTime
        Object source = new Object();
        // WHEN / THEN
        assertThrows(RuntimeException.class, () -> NumberConversions.toLocalDate(source, converter));
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("dateConversionProvider")
    void givenDate_convertsCorrectly(Date source, LocalDate expected, String description) {
        LocalDate result = NumberConversions.toLocalDate(source, converter);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("localDateTimeProvider")
    void givenLocalDateTime_convertsCorrectly(LocalDateTime source, LocalDate expected, String description) {
        LocalDate result = NumberConversions.toLocalDate(source, converter);
        assertEquals(expected, result, description);
    }

    /**
     * ---------- Null input handling ----------
     */
    @Test
    void givenNullSource_throwsNullPointerException() {
        // The implementation delegates to toZonedDateTime which throws NPE for null input.
        assertThrows(NullPointerException.class, () -> NumberConversions.toLocalDate(null, converter));
    }

    @Test
    void givenNullConverter_throwsNullPointerException() {
        Date source = new Date();
        assertThrows(NullPointerException.class, () -> NumberConversions.toLocalDate(source, null));
    }

    @ParameterizedTest(name = "Unsupported type {1} should cause ClassCastException")
    @MethodSource("unsupportedTypeProvider")
    void givenUnsupportedType_throwsClassCastException(Object source, String typeName) {
        assertThrows(ClassCastException.class, () -> NumberConversions.toLocalDate(source, converter), "Converting a " + typeName + " must fail with ClassCastException");
    }

    /**
     * ---------- Invalid temporal values (construction failures) ----------
     */
    @Test
    void givenInvalidLocalDateTime_throwsDateTimeExceptionAtCreation() {
        // The exception is thrown when trying to create the invalid LocalDateTime.
        assertThrows(DateTimeException.class, () -> LocalDateTime.of(2021, 2, 29, 12, 0));
    }

    @Test
    void givenInvalidZonedDateTime_throwsDateTimeExceptionAtCreation() {
        assertThrows(DateTimeException.class, () -> ZonedDateTime.of(2021, 4, 31, 0, 0, 0, 0, ZoneId.systemDefault()));
    }

    /**
     * ---------- Timestamp overflow ----------
     */
    @Test
    void givenTimestampBeyondRange_throwsDateTimeException() {
        // This timestamp is far beyond the range supported by ZonedDateTime.
        Timestamp ts = Timestamp.valueOf(LocalDateTime.of(292278994, 8, 17, 7, 12, 55));
        assertThrows(DateTimeException.class, () -> NumberConversions.toLocalDate(ts, converter));
    }

    /**
     * ---------- Instant out of range ----------
     */
    @Test
    void givenInstantOutOfRange_throwsDateTimeException() {
        // Instant with epoch seconds beyond Long.MAX_VALUE throws an exception when converted.
        Instant instant = Instant.ofEpochSecond(Long.MAX_VALUE, 999_999_999);
        assertThrows(DateTimeException.class, () -> NumberConversions.toLocalDate(instant, converter));
    }
}
