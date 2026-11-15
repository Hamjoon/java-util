package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DoubleConversions_toLocalDateTime_Test {

    private Converter converter;

    /**
     * Simple holder for parameterized test arguments.
     */
    private static class ArgumentsHolder {

        final Object input;

        // string representation of the expected LocalDateTime
        final String expected;

        final String description;

        ArgumentsHolder(Object input, String expected, String description) {
            this.input = input;
            this.expected = expected;
            this.description = description;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // ConverterOptions has a public no‑arg constructor in the library.
        // Using the default options gives us all built‑in conversions.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    /* -----------------------------------------------------------
     *  Valid conversion test cases (parameterized)
     * ----------------------------------------------------------- */
    private static Stream<ArgumentsHolder> validConversionProvider() {
        return Stream.of(// ZonedDateTime (UTC) → LocalDateTime (zone info dropped)
        new ArgumentsHolder(ZonedDateTime.of(2023, 5, 10, 14, 30, 0, 0, ZoneId.of("UTC")), "2023-05-10T14:30", "Convert a ZonedDateTime (UTC) to LocalDateTime – the zone information is dropped, leaving the same date‑time fields."), // Instant → LocalDateTime using system default zone
        new ArgumentsHolder(Instant.parse("2023-05-10T14:30:00Z"), Instant.parse("2023-05-10T14:30:00Z").atZone(ZoneId.systemDefault()).toLocalDateTime().toString(), "Convert an Instant to LocalDateTime using the system default time‑zone."), // java.util.Date → LocalDateTime using system default zone
        new ArgumentsHolder(Date.from(Instant.parse("2023-05-10T14:30:00Z")), Instant.parse("2023-05-10T14:30:00Z").atZone(ZoneId.systemDefault()).toLocalDateTime().toString(), "Convert a java.util.Date (which internally holds an instant) to LocalDateTime."), // java.sql.Timestamp → LocalDateTime via its Instant
        new ArgumentsHolder(Timestamp.valueOf("2023-05-10 14:30:00"), Timestamp.valueOf("2023-05-10 14:30:00").toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().toString(), "Convert a java.sql.Timestamp to LocalDateTime; Timestamp is first turned into an Instant."), // Calendar (UTC) → LocalDateTime using system default zone
        new ArgumentsHolder(createUtcCalendar(2023, Calendar.MAY, 10, 14, 30, 0), Instant.ofEpochMilli(createUtcCalendar(2023, Calendar.MAY, 10, 14, 30, 0).getTimeInMillis()).atZone(ZoneId.systemDefault()).toLocalDateTime().toString(), "Convert a java.util.Calendar (UTC) to LocalDateTime; the calendar’s instant is used."), // LocalDateTime → LocalDateTime (treated as system default zone)
        new ArgumentsHolder(LocalDateTime.of(2023, 5, 10, 14, 30), "2023-05-10T14:30", "Convert a LocalDateTime directly – toZonedDateTime should treat it as being in the system default zone and the resulting LocalDateTime remains unchanged."));
    }

    /* -----------------------------------------------------------
     *  Helper methods & holder class
     * ----------------------------------------------------------- */
    private static Calendar createUtcCalendar(int year, int month, int day, int hour, int minute, int second) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setLenient(false);
        cal.set(year, month, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    @Test
    void toLocalDateTime_fromDate_returnsEquivalentLocalDateTime() {
        // GIVEN: a java.util.Date representing a known instant
        Instant instant = Instant.parse("2023-03-15T10:20:30.123Z");
        Date date = Date.from(instant);
        // WHEN: converting the Date to LocalDateTime via DoubleConversions
        LocalDateTime result = DoubleConversions.toLocalDateTime(date, converter);
        // THEN: the result should match the LocalDateTime part of the same instant in the system default zone
        LocalDateTime expected = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        assertEquals(expected, result);
    }

    @Test
    void toLocalDateTime_fromZonedDateTime_returnsSameLocalDateTime() {
        // GIVEN: a ZonedDateTime in a specific zone
        ZonedDateTime zoned = ZonedDateTime.of(2022, 12, 31, 23, 59, 59, 0, ZoneId.of("Europe/Paris"));
        // WHEN: converting the ZonedDateTime to LocalDateTime
        LocalDateTime result = DoubleConversions.toLocalDateTime(zoned, converter);
        // THEN: the LocalDateTime part should be identical to the one from the ZonedDateTime
        assertEquals(zoned.toLocalDateTime(), result);
    }

    @Test
    void toLocalDateTime_nullInput_returnsNull() {
        // GIVEN: a null source object
        Object source = null;
        // WHEN: converting null
        LocalDateTime result = DoubleConversions.toLocalDateTime(source, converter);
        // THEN: the conversion should yield null (the underlying toZonedDateTime returns null)
        assertNull(result);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("validConversionProvider")
    void toLocalDateTime_validConversions(ArgumentsHolder holder) {
        // WHEN
        LocalDateTime result = DoubleConversions.toLocalDateTime(holder.input, converter);
        // THEN
        assertEquals(holder.expected, result.toString(), "Unexpected LocalDateTime conversion result.");
    }

    /* -----------------------------------------------------------
     *  Exception scenario test cases
     * ----------------------------------------------------------- */
    @Test
    void toLocalDateTime_nullSource_throwsNullPointerException() {
        // GIVEN
        Object source = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> DoubleConversions.toLocalDateTime(source, converter), "Null source should cause NullPointerException.");
    }

    @Test
    void toLocalDateTime_unsupportedString_throwsClassCastException() {
        // GIVEN
        Object source = "not a date";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> DoubleConversions.toLocalDateTime(source, converter), "Unsupported type String should cause ClassCastException.");
    }

    @Test
    void toLocalDateTime_unknownObject_throwsIllegalArgumentException() {
        // GIVEN
        Object source = new Object();
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> DoubleConversions.toLocalDateTime(source, converter), "Unknown object type should cause IllegalArgumentException.");
    }

    @Test
    void toLocalDateTime_localDate_throwsDateTimeException() {
        // GIVEN
        Object source = LocalDate.of(2023, 5, 10);
        // WHEN / THEN
        assertThrows(DateTimeException.class, () -> DoubleConversions.toLocalDateTime(source, converter), "Converting a LocalDate should raise DateTimeException.");
    }

    @Test
    void toLocalDateTime_localTime_throwsDateTimeException() {
        // GIVEN
        Object source = LocalTime.of(14, 30);
        // WHEN / THEN
        assertThrows(DateTimeException.class, () -> DoubleConversions.toLocalDateTime(source, converter), "Converting a LocalTime should raise DateTimeException.");
    }

    @Test
    void toLocalDateTime_duration_throwsUnsupportedOperationException() {
        // GIVEN
        Object source = Duration.ofHours(5);
        // WHEN / THEN
        assertThrows(UnsupportedOperationException.class, () -> DoubleConversions.toLocalDateTime(source, converter), "Converting a Duration should throw UnsupportedOperationException.");
    }

    @Test
    void toLocalDateTime_nullConverter_throwsNullPointerException() {
        // GIVEN
        Object source = OffsetDateTime.now();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> DoubleConversions.toLocalDateTime(source, null), "Null converter should cause NullPointerException.");
    }
}
