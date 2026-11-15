package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toLocalDate_Test {

    /**
     * The Converter used for all tests.
     */
    private static Converter converter;

    @Mock
    Converter // Mockito will inject a mock
    throwingConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance – use the default constructor.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static Converter reference.
        converter = null;
    }

    /**
     * Provides test data for {@link #toLocalDate_fromDate(java.util.Date)}.
     * The JSON describes two cases:
     * <ul>
     *   <li>UTC date – the time part must be ignored.</li>
     *   <li>Date with a negative offset – after normalisation to UTC the
     *       calendar day may change.</li>
     * </ul>
     */
    private static Stream<Arguments> dateProvider() {
        // case 1: 2023‑03‑15T10:20:30Z  → 2023‑03‑15
        Date utcDate = Date.from(Instant.parse("2023-03-15T10:20:30Z"));
        // case 2: 2023‑12‑31T23:59:59‑05:00 → 2024‑01‑01 (UTC)
        // Build the instant with the offset, then create a Date (which stores UTC)
        // equivalent UTC instant
        Instant offsetInstant = Instant.parse("2024-01-01T04:59:59Z");
        Date offsetDate = Date.from(offsetInstant);
        return Stream.of(Arguments.of(utcDate, LocalDate.of(2023, 3, 15)), Arguments.of(offsetDate, LocalDate.of(2024, 1, 1)));
    }

    private static Stream<Arguments> unsupportedTypesProvider() {
        return Stream.of(// String
        Arguments.of("2023-03-15"), // Integer
        Arguments.of(Integer.valueOf(12345)), // Year
        Arguments.of(Year.of(2023)), // Duration
        Arguments.of(Duration.ofHours(5)));
    }

    @Test
    void toLocalDate_fromLocalDate() {
        LocalDate expected = LocalDate.of(2023, Month.MARCH, 15);
        LocalDate actual = NumberConversions.toLocalDate(expected, converter);
        assertEquals(expected, actual, "LocalDate should be returned unchanged");
    }

    @Test
    void toLocalDate_fromLocalDateTime() {
        LocalDateTime ldt = LocalDateTime.of(2023, Month.APRIL, 10, 14, 30, 45);
        LocalDate expected = ldt.toLocalDate();
        LocalDate actual = NumberConversions.toLocalDate(ldt, converter);
        assertEquals(expected, actual, "LocalDate should be extracted from LocalDateTime");
    }

    @Test
    void toLocalDate_fromZonedDateTime() {
        ZonedDateTime zdt = ZonedDateTime.of(2022, 12, 5, 9, 0, 0, 0, ZoneId.of("UTC"));
        LocalDate expected = zdt.toLocalDate();
        LocalDate actual = NumberConversions.toLocalDate(zdt, converter);
        assertEquals(expected, actual, "LocalDate should be extracted from ZonedDateTime");
    }

    @Test
    void toLocalDate_fromInstant() {
        Instant instant = Instant.parse("2021-07-20T10:15:30.00Z");
        // toZonedDateTime uses the system default zone
        ZoneId zone = ZoneId.systemDefault();
        LocalDate expected = ZonedDateTime.ofInstant(instant, zone).toLocalDate();
        LocalDate actual = NumberConversions.toLocalDate(instant, converter);
        assertEquals(expected, actual, "LocalDate should be derived from Instant using system default zone");
    }

    @Test
    void toLocalDate_fromDate() {
        // java.util.Date stores an instant in UTC; conversion will use system default zone.
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2020, Calendar.JUNE, 18, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();
        ZoneId zone = ZoneId.systemDefault();
        LocalDate expected = ZonedDateTime.ofInstant(date.toInstant(), zone).toLocalDate();
        LocalDate actual = NumberConversions.toLocalDate(date, converter);
        assertEquals(expected, actual, "LocalDate should be derived from java.util.Date");
    }

    @Test
    void toLocalDate_fromCalendar() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        calendar.set(2019, Calendar.NOVEMBER, 3, 12, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        ZoneId zone = calendar.getTimeZone().toZoneId();
        LocalDate expected = ZonedDateTime.ofInstant(calendar.toInstant(), zone).toLocalDate();
        LocalDate actual = NumberConversions.toLocalDate(calendar, converter);
        assertEquals(expected, actual, "LocalDate should be derived from Calendar respecting its time zone");
    }

    @Test
    void toLocalDate_fromTimestamp() {
        Timestamp ts = Timestamp.valueOf("2018-02-28 23:59:59.123456789");
        ZoneId zone = ZoneId.systemDefault();
        LocalDate expected = ZonedDateTime.ofInstant(ts.toInstant(), zone).toLocalDate();
        LocalDate actual = NumberConversions.toLocalDate(ts, converter);
        assertEquals(expected, actual, "LocalDate should be derived from Timestamp");
    }

    @Test
    void toLocalDate_nullFrom() {
        // The method internally calls toZonedDateTime which will NPE on null.
        assertThrows(NullPointerException.class, () -> NumberConversions.toLocalDate(null, converter), "Passing null as the source object should throw NullPointerException");
    }

    @ParameterizedTest(name = "toLocalDate unsupported type [{0}]")
    @MethodSource("unsupportedTypesProvider")
    void toLocalDate_unsupportedTypes(Object from) {
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toLocalDate(from, converter), "Unsupported source type should cause IllegalArgumentException");
    }

    @Test
    void toLocalDate_converterThrows() {
        // Arrange: make the mock throw a RuntimeException when any conversion is attempted.
        // The internal implementation eventually calls converter.convert(...). We don't know the exact
        // method signature, so we use a generic stub that throws for any call.
        doThrow(new RuntimeException("Converter failure")).when(throwingConverter).convert(any(), any());
        Date date = Date.from(Instant.parse("2023-03-15T10:20:30Z"));
        // Act & Assert: the RuntimeException should propagate unchanged.
        RuntimeException ex = assertThrows(RuntimeException.class, () -> NumberConversions.toLocalDate(date, throwingConverter), "Exception from Converter should be propagated");
        assertEquals("Converter failure", ex.getMessage());
    }
}
