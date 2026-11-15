package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.time.format.DateTimeFormatter;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toOffsetDateTime_Test {

    /**
     * The Converter instance used for all tests.
     */
    private static Converter converter;

    /* --------------------------------------------------------------------- */
    private static class Arguments {

        static org.junit.jupiter.params.provider.Arguments of(Object... args) {
            return org.junit.jupiter.params.provider.Arguments.of(args);
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // ConverterOptions has a no‑arg constructor that yields the default configuration.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly necessary but keeps the pattern tidy.
        converter = null;
    }

    /**
     * Provides inputs that are expected to result in {@link DateTimeParseException}.
     */
    private static Stream<Arguments> invalidStringProvider() {
        return Stream.of(Arguments.of("not-a-date"), // invalid offset
        Arguments.of("2023-08-01T12:34:56+25:00"));
    }

    /**
     * Provides inputs that should cause {@link IllegalArgumentException}.
     */
    private static Stream<Arguments> illegalArgumentProvider() {
        return Stream.of(Arguments.of(new Object()), Arguments.of(Boolean.TRUE), Arguments.of(Duration.ofHours(5)), Arguments.of(Period.ofDays(10)));
    }

    /**
     * Provides inputs that should cause {@link DateTimeException}.
     */
    private static Stream<Arguments> dateTimeExceptionProvider() {
        return Stream.of(Arguments.of(LocalDate.of(2023, 8, 1)), Arguments.of(LocalTime.of(12, 34, 56)), Arguments.of(MonthDay.of(8, 1)), Arguments.of(Year.of(2023)), Arguments.of(YearMonth.of(2023, 8)), Arguments.of(OffsetTime.of(12, 34, 56, 0, ZoneOffset.ofHours(2))));
    }

    @Test
    void toOffsetDateTime_nullInput_returnsNull() {
        OffsetDateTime result = StringConversions.toOffsetDateTime(null, converter);
        assertNull(result, "Converting a null source should return null");
    }

    @Test
    void toOffsetDateTime_fromZonedDateTime_returnsEquivalentOffsetDateTime() {
        ZonedDateTime zdt = ZonedDateTime.of(2023, 8, 15, 10, 30, 45, 123_000_000, // +02:00 during summer
        ZoneId.of("Europe/Paris"));
        OffsetDateTime expected = zdt.toOffsetDateTime();
        OffsetDateTime actual = StringConversions.toOffsetDateTime(zdt, converter);
        assertEquals(expected, actual, "OffsetDateTime derived from ZonedDateTime should match");
    }

    @Test
    void toOffsetDateTime_fromIsoString_returnsParsedOffsetDateTime() {
        // ISO‑8601 string that includes an offset
        String iso = "2023-08-01T12:34:56+02:00";
        OffsetDateTime expected = OffsetDateTime.parse(iso, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        OffsetDateTime actual = StringConversions.toOffsetDateTime(iso, converter);
        assertEquals(expected, actual, "ISO‑8601 string should be parsed to the correct OffsetDateTime");
    }

    @Test
    void toOffsetDateTime_fromStringWithoutOffset_fallsBackToZonedDateTimeAndReturnsNull() {
        // The underlying toZonedDateTime cannot parse a plain LocalDateTime without zone info,
        // therefore it returns null and the method under test should also return null.
        String withoutOffset = "2023-08-01T12:34:56";
        OffsetDateTime actual = StringConversions.toOffsetDateTime(withoutOffset, converter);
        assertNull(actual, "String without zone/offset information should result in null");
    }

    @Test
    void toOffsetDateTime_fromDate_returnsOffsetDateTimeUsingSystemDefaultZone() {
        // java.util.Date does not carry zone information; conversion uses the system default zone.
        Date date = Date.from(Instant.parse("2023-08-01T12:34:56Z"));
        ZoneId systemZone = ZoneId.systemDefault();
        ZonedDateTime expectedZdt = date.toInstant().atZone(systemZone);
        OffsetDateTime expected = expectedZdt.toOffsetDateTime();
        OffsetDateTime actual = StringConversions.toOffsetDateTime(date, converter);
        assertEquals(expected, actual, "Date should be converted using the system default zone");
    }

    @Test
    void toOffsetDateTime_fromOffsetDateTime_returnsSameInstanceValue() {
        OffsetDateTime source = OffsetDateTime.of(2023, 8, 1, 12, 34, 56, 0, ZoneOffset.ofHours(-5));
        OffsetDateTime result = StringConversions.toOffsetDateTime(source, converter);
        assertEquals(source, result, "When the source is already an OffsetDateTime it should be returned unchanged");
    }

    @Test
    void toOffsetDateTime_fromEpochMilliLong_returnsOffsetDateTimeInSystemZone() {
        // corresponds to 2023-07-31T23:34:56Z
        long epochMilli = 1_690_893_296_000L;
        Instant instant = Instant.ofEpochMilli(epochMilli);
        ZoneId systemZone = ZoneId.systemDefault();
        OffsetDateTime expected = OffsetDateTime.ofInstant(instant, systemZone);
        OffsetDateTime actual = StringConversions.toOffsetDateTime(epochMilli, converter);
        assertEquals(expected, actual, "Long epoch‑millis should be interpreted as an instant in the system default zone");
    }

    @ParameterizedTest(name = "Invalid string ''{0}'' should throw DateTimeParseException")
    @MethodSource("invalidStringProvider")
    void toOffsetDateTime_invalidString_throwsDateTimeParseException(String input) {
        assertThrows(DateTimeParseException.class, () -> StringConversions.toOffsetDateTime(input, converter), "Parsing an invalid date‑time string must throw DateTimeParseException");
    }

    @ParameterizedTest(name = "Input of type {0} should throw IllegalArgumentException")
    @MethodSource("illegalArgumentProvider")
    void toOffsetDateTime_illegalArgument_throwsIllegalArgumentException(Object input) {
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toOffsetDateTime(input, converter), "Unsupported conversion should raise IllegalArgumentException");
    }

    @ParameterizedTest(name = "Input {0} should throw DateTimeException")
    @MethodSource("dateTimeExceptionProvider")
    void toOffsetDateTime_dateTimeException_throwsDateTimeException(Object input) {
        assertThrows(DateTimeException.class, () -> StringConversions.toOffsetDateTime(input, converter), "Conversion of incomplete temporal types must throw DateTimeException");
    }
}
