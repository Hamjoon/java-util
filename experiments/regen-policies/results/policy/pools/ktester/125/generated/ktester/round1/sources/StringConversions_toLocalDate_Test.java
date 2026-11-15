package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StringConversions_toLocalDate_Test {

    /**
     * Shared {@link Converter} used by the tests.
     */
    private static Converter sharedConverter;

    /**
     * Fresh {@link Converter} instance for each test (isolates state).
     */
    private Converter converter;

    @Mock
    Converter mockConverterThatThrows;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter requires a ConverterOptions instance; the default constructor
        // provides a fully functional converter with the built‑in conversions.
        sharedConverter = new Converter(new ConverterOptions());
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a fresh instance to avoid cross‑test contamination (e.g. custom conversions).
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        converter = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        sharedConverter = null;
    }

    private static Stream<Arguments> zonedDateTimeProvider() {
        return Stream.of(Arguments.of(ZonedDateTime.of(2023, 5, 15, 10, 30, 0, 0, ZoneId.of("UTC")), LocalDate.of(2023, 5, 15)), Arguments.of(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("America/New_York")), LocalDate.of(1970, 1, 1)), Arguments.of(ZonedDateTime.of(2000, 2, 29, 0, 0, 0, 0, ZoneId.of("UTC")), LocalDate.of(2000, 2, 29)), Arguments.of(ZonedDateTime.of(9999, 12, 31, 23, 59, 59, 0, ZoneId.of("UTC")), LocalDate.of(9999, 12, 31)));
    }

    private static Stream<Arguments> localDateTimeAndInstantProvider() {
        return Stream.of(Arguments.of(LocalDateTime.of(2020, 2, 29, 23, 59), LocalDate.of(2020, 2, 29)), Arguments.of(Instant.parse("2021-12-31T23:00:00Z"), LocalDate.of(2021, 12, 31)));
    }

    private static Stream<Arguments> stringParsingProvider() {
        return Stream.of(Arguments.of("2024-01-01", LocalDate.of(2024, 1, 1)), Arguments.of("2023-03-10T15:30:00+02:00[Europe/Paris]", LocalDate.of(2023, 3, 10)));
    }

    @Test
    void toLocalDate_nullInput_returnsNull() {
        assertNull(StringConversions.toLocalDate(null, converter));
    }

    @Test
    void toLocalDate_stringIsoDate_returnsExpectedLocalDate() {
        // ISO‑8601 date string – the internal conversion path goes through
        // toZonedDateTime which parses the string as a LocalDate at start of day UTC.
        String isoDate = "2023-05-01";
        LocalDate expected = LocalDate.of(2023, 5, 1);
        LocalDate actual = StringConversions.toLocalDate(isoDate, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalDate_javaUtilDate_returnsExpectedLocalDate() {
        // java.util.Date represents an instant; the conversion should keep the date part
        // in the default system zone. To make the test deterministic we use UTC.
        ZoneId utc = ZoneOffset.UTC;
        Instant instant = LocalDate.of(2022, 12, 31).atStartOfDay(utc).toInstant();
        Date date = Date.from(instant);
        // Ensure the converter uses UTC for the test – we create a Converter with default
        // options (which does not force a specific zone) and then verify the result.
        LocalDate actual = StringConversions.toLocalDate(date, converter);
        assertEquals(LocalDate.of(2022, 12, 31), actual);
    }

    @Test
    void toLocalDate_calendar_returnsExpectedLocalDate() {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.set(2021, Calendar.JUNE, 15, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        LocalDate expected = LocalDate.of(2021, 6, 15);
        LocalDate actual = StringConversions.toLocalDate(cal, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalDate_zonedDateTime_returnsSameLocalDate() {
        ZonedDateTime zdt = ZonedDateTime.of(2020, 2, 29, 13, 45, 0, 0, ZoneId.of("America/New_York"));
        LocalDate expected = LocalDate.of(2020, 2, 29);
        LocalDate actual = StringConversions.toLocalDate(zdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalDate_unsupportedType_returnsNull() {
        // Boolean is not convertible to a date/time type by the built‑in conversions.
        Boolean unsupported = Boolean.TRUE;
        assertNull(StringConversions.toLocalDate(unsupported, converter));
    }

    @ParameterizedTest
    @MethodSource("zonedDateTimeProvider")
    void toLocalDate_zonedDateTime_returnsExpected(ZonedDateTime input, LocalDate expected) {
        assertEquals(expected, StringConversions.toLocalDate(input, converter));
    }

    @ParameterizedTest
    @MethodSource("localDateTimeAndInstantProvider")
    void toLocalDate_temporalInputs_returnsExpected(Object input, LocalDate expected) {
        assertEquals(expected, StringConversions.toLocalDate(input, converter));
    }

    @ParameterizedTest
    @MethodSource("stringParsingProvider")
    void toLocalDate_stringParsing_returnsExpected(String input, LocalDate expected) {
        assertEquals(expected, StringConversions.toLocalDate(input, converter));
    }

    @Test
    void toLocalDate_malformedString_throwsDateTimeParseException() {
        assertThrows(DateTimeParseException.class, () -> StringConversions.toLocalDate("not-a-date", converter));
    }

    @Test
    void toLocalDate_emptyString_throwsDateTimeParseException() {
        assertThrows(DateTimeParseException.class, () -> StringConversions.toLocalDate("", converter));
    }

    @Test
    void toLocalDate_invalidMonthString_throwsDateTimeParseException() {
        assertThrows(DateTimeParseException.class, () -> StringConversions.toLocalDate("2023-13-01", converter));
    }

    @Test
    void toLocalDate_invalidZoneIdString_throwsDateTimeException() {
        assertThrows(DateTimeException.class, () -> StringConversions.toLocalDate("2023-01-01T00:00:00+02:00[Invalid/Zone]", converter));
    }

    @Test
    void toLocalDate_unsupportedInteger_returnsNull() {
        assertNull(StringConversions.toLocalDate(12345, converter));
    }

    @Test
    void toLocalDate_unsupportedObject_throwsClassCastException() {
        // The underlying toZonedDateTime implementation attempts a cast that fails.
        assertThrows(ClassCastException.class, () -> StringConversions.toLocalDate(new Object(), converter));
    }

    @Test
    void toLocalDate_converterThrowsRuntimeException_propagates() {
        // Configure the mock to throw a RuntimeException for any conversion attempt.
        when(mockConverterThatThrows.convert(any(), any())).thenThrow(new RuntimeException("boom"));
        // The exact internal call chain is not important – we only need to ensure the exception bubbles up.
        RuntimeException ex = assertThrows(RuntimeException.class, () -> StringConversions.toLocalDate("2024-01-01", mockConverterThatThrows));
        assertEquals("boom", ex.getMessage());
    }
}
