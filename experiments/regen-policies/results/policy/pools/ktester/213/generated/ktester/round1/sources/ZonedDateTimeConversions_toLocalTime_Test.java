package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.sql.Timestamp;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Stream;
import static com.cedarsoftware.util.convert.MapConversions.ZONED_DATE_TIME;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toLocalTime_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The Converter needs a non‑null options instance; the default options are sufficient.
        converter = new Converter(new ConverterOptions());
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// ZonedDateTime input
        Arguments.of(ZonedDateTime.of(2023, 7, 15, 13, 45, 30, 0, ZoneId.of("UTC")), LocalTime.of(13, 45, 30), "Direct ZonedDateTime input – the method should return the same local time component."), // LocalDateTime input (date part ignored)
        Arguments.of(LocalDateTime.of(2023, 5, 10, 8, 15, 0), LocalTime.of(8, 15, 0), "LocalDateTime input – the date part is ignored and only the time part is returned."), // LocalTime input (unchanged)
        Arguments.of(LocalTime.of(22, 30, 0), LocalTime.of(22, 30, 0), "LocalTime input – should be returned unchanged."), // java.util.Date input
        Arguments.of(Date.from(Instant.parse("2022-01-01T12:00:00Z")), LocalTime.of(12, 0, 0), "java.util.Date representing an instant – conversion should yield the same local time."), // java.sql.Timestamp input
        Arguments.of(Timestamp.valueOf("2022-03-05 09:45:00"), LocalTime.of(9, 45, 0), "java.sql.Timestamp input – behaves like a Date and should yield the same local time."), // Long epoch‑millis input
        Arguments.of(Instant.parse("2023-04-01T16:20:15Z").toEpochMilli(), LocalTime.of(16, 20, 15), "Long epoch‑millis input – interpreted as an instant and converted to the corresponding local time."), // ISO‑8601 string input
        Arguments.of("2023-03-15T14:20:00Z", LocalTime.of(14, 20, 0), "ISO‑8601 string representing an instant – parser should create a ZonedDateTime and return its time."));
    }

    private static Stream<Arguments> illegalArgumentProvider() {
        return Stream.of(// Boolean input
        Arguments.of(Boolean.TRUE, "An unsupported source type (Boolean) must cause an IllegalArgumentException."), // Double input (unsupported numeric type)
        Arguments.of(12345.67, "A Double value is not a supported representation for a temporal instant; conversion should fail."), // Map without required ZONED_DATE_TIME key
        Arguments.of(Collections.singletonMap("foo", "2023-01-01T12:00:00Z"), "Map without required ZONED_DATE_TIME key should throw IllegalArgumentException."), // Unsupported object type (Year)
        Arguments.of(Year.of(2023), "Year objects are not handled by the conversion logic and should trigger IllegalArgumentException."));
    }

    private static Stream<Arguments> invalidStringProvider() {
        return Stream.of(Arguments.of("2023-13-40T99:99:99Z", "String that does not conform to ISO‑8601 format should cause a DateTimeParseException."), Arguments.of("", "Empty string cannot be parsed into a ZonedDateTime and must raise a DateTimeParseException."));
    }

    @Test
    void toLocalTime_fromZonedDateTime_returnsCorrectLocalTime() {
        // GIVEN
        ZonedDateTime zdt = ZonedDateTime.of(LocalDateTime.of(2023, Month.MARCH, 25, 10, 15, 30), ZoneId.of("UTC"));
        LocalTime expected = LocalTime.of(10, 15, 30);
        // WHEN
        LocalTime actual = ZonedDateTimeConversions.toLocalTime(zdt, converter);
        // THEN
        assertEquals(expected, actual, "The extracted LocalTime should match the time component of the source ZonedDateTime.");
    }

    @Test
    void toLocalTime_fromDate_returnsCorrectLocalTime() {
        // GIVEN
        ZonedDateTime zdt = ZonedDateTime.of(LocalDateTime.of(2023, Month.MARCH, 25, 22, 45, 5), ZoneId.of("UTC"));
        Date date = Date.from(zdt.toInstant());
        LocalTime expected = LocalTime.of(22, 45, 5);
        // WHEN
        LocalTime actual = ZonedDateTimeConversions.toLocalTime(date, converter);
        // THEN
        assertEquals(expected, actual, "Conversion from java.util.Date should yield the same LocalTime as the original instant.");
    }

    @Test
    void toLocalTime_nullInput_returnsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        LocalTime actual = ZonedDateTimeConversions.toLocalTime(source, converter);
        // THEN
        assertNull(actual, "A null source should result in a null LocalTime.");
    }

    /* --------------------------------------------------------------
     *  Valid conversion test cases – parameterized
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("validConversionProvider")
    void toLocalTime_validConversions(Object source, LocalTime expected, String description) {
        // WHEN
        LocalTime actual = ZonedDateTimeConversions.toLocalTime(source, converter);
        // THEN
        assertEquals(expected, actual, description);
    }

    /* --------------------------------------------------------------
     *  Null and error handling – NullPointerException
     * -------------------------------------------------------------- */
    @Test
    void toLocalTime_nullSourceAndNullConverter_throwsNPE() {
        // GIVEN
        Object source = null;
        Converter nullConverter = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toLocalTime(source, nullConverter), "Passing a null source object should result in a NullPointerException.");
    }

    /* --------------------------------------------------------------
     *  Unsupported source types – IllegalArgumentException
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("illegalArgumentProvider")
    void toLocalTime_unsupportedSource_throwsIllegalArgumentException(Object source, String description) {
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> ZonedDateTimeConversions.toLocalTime(source, converter), description);
    }

    /* --------------------------------------------------------------
     *  Invalid ISO‑8601 string parsing – DateTimeParseException
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("invalidStringProvider")
    void toLocalTime_invalidString_throwsDateTimeParseException(String source, String description) {
        // WHEN / THEN
        assertThrows(DateTimeParseException.class, () -> ZonedDateTimeConversions.toLocalTime(source, converter), description);
    }

    /* --------------------------------------------------------------
     *  Map with invalid ZONED_DATE_TIME value – DateTimeParseException
     * -------------------------------------------------------------- */
    @Test
    void toLocalTime_mapWithInvalidZonedDateTimeValue_throwsDateTimeParseException() {
        // GIVEN
        Map<String, Object> map = Collections.singletonMap(ZONED_DATE_TIME, "not-a-date");
        // WHEN / THEN
        assertThrows(DateTimeParseException.class, () -> ZonedDateTimeConversions.toLocalTime(map, converter), "Map contains the required key but the associated value is not parsable; should raise DateTimeParseException.");
    }

    /* --------------------------------------------------------------
     *  Additional sanity check – null source with non‑null converter
     * -------------------------------------------------------------- */
    @Test
    void toLocalTime_nullSource_nonNullConverter_returnsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        LocalTime result = ZonedDateTimeConversions.toLocalTime(source, converter);
        // THEN
        assertNull(result, "A null source with a valid converter should return null (no time to extract).");
    }
}
