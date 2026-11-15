package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import java.sql.Timestamp;
import java.sql.Date;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DateConversions_toLocalTime_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Use default options – they provide a ZoneId (system default) needed for conversion
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    /**
     * Provides a stream of arguments for successful conversions.
     * Each argument is an array: {Object from, ZoneId zone, LocalTime expected, String description}
     */
    static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// java.util.Date with UTC zone
        Arguments.of(Date.from(Instant.parse("2023-03-15T10:20:30.123Z")), ZoneId.of("UTC"), LocalTime.of(10, 20, 30, 123_000_000), "Convert java.util.Date (UTC) to LocalTime"), // java.time.Instant with America/New_York (UTC‑5)
        Arguments.of(Instant.parse("2022-12-31T23:59:59.999Z"), ZoneId.of("America/New_York"), LocalTime.of(18, 59, 59, 999_000_000), "Instant to LocalTime with New York offset"), // java.time.LocalDateTime with Europe/Paris (UTC+1)
        Arguments.of(LocalDateTime.of(2021, 1, 1, 0, 0, 0), ZoneId.of("Europe/Paris"), LocalTime.of(0, 0, 0), "LocalDateTime midnight with Paris zone"), // java.time.LocalTime (zone should be ignored)
        Arguments.of(LocalTime.of(15, 45, 30), ZoneId.of("UTC"), LocalTime.of(15, 45, 30), "LocalTime source should be unchanged"), // java.sql.Timestamp with nanosecond precision
        Arguments.of(Timestamp.valueOf("2020-06-01 12:34:56.789"), ZoneId.of("UTC"), LocalTime.of(12, 34, 56, 789_000_000), "Timestamp conversion preserving nanos"), // ISO‑8601 String with UTC
        Arguments.of("2022-08-15T07:30:45.250Z", ZoneId.of("UTC"), LocalTime.of(7, 30, 45, 250_000_000), "String ISO instant parsed to LocalTime"), // java.time.ZonedDateTime with UTC converter (shift from +02:00)
        Arguments.of(ZonedDateTime.of(2023, 5, 10, 14, 15, 16, 123_000_000, ZoneId.of("Europe/Berlin")), ZoneId.of("UTC"), LocalTime.of(12, 15, 16, 123_000_000), "ZonedDateTime +02:00 to UTC LocalTime"), // java.util.Calendar set to 2023-03-15T22:15:45 in +02:00, converter UTC
        Arguments.of(calendarAt(2023, 3, 15, 22, 15, 45, ZoneOffset.ofHours(2)), ZoneId.of("UTC"), LocalTime.of(20, 15, 45), "Calendar with +02:00 zone converted to UTC LocalTime"), // java.sql.Date (midnight) with America/Los_Angeles (UTC‑7)
        Arguments.of(Date.valueOf(LocalDate.of(2021, 7, 4)), ZoneId.of("America/Los_Angeles"), LocalTime.of(17, 0, 0), "java.sql.Date interpreted as midnight UTC then shifted to -07:00"), // OffsetDateTime with UTC converter
        Arguments.of(OffsetDateTime.of(2022, 3, 10, 10, 0, 0, 0, ZoneOffset.ofHoursMinutes(5, 30)), ZoneId.of("UTC"), LocalTime.of(4, 30, 0), "OffsetDateTime +05:30 to UTC LocalTime"), // LocalTime with nanoseconds, zone Asia/Tokyo (should be ignored)
        Arguments.of(LocalTime.of(23, 59, 59, 999_999_999), ZoneId.of("Asia/Tokyo"), LocalTime.of(23, 59, 59, 999_999_999), "LocalTime nanoseconds preserved regardless of zone"), // String with Kiritimati (+14) offset
        Arguments.of("2022-12-31T23:59:59.999999999Z", ZoneId.of("Pacific/Kiritimati"), LocalTime.of(13, 59, 59, 999_999_999), "ISO string shifted by +14 hours"));
    }

    /**
     * Creates a {@link Calendar} instance set to the specified date‑time in the given offset.
     * The calendar's time zone is set to the offset, and the instant is derived accordingly.
     */
    private static Calendar calendarAt(int year, int month, int day, int hour, int minute, int second, ZoneOffset offset) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(offset));
        cal.setLenient(false);
        cal.set(year, month - 1, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    @Test
    void toLocalTime_fromLocalDateTime() {
        LocalDateTime ldt = LocalDateTime.of(2023, Month.MAY, 10, 14, 30, 45, 123_000_000);
        LocalTime expected = ldt.toLocalTime();
        LocalTime actual = DateConversions.toLocalTime(ldt, converter);
        assertEquals(expected, actual, "LocalTime should be extracted unchanged from LocalDateTime");
    }

    @Test
    void toLocalTime_fromDate() {
        // Instant corresponds to 2023‑05‑10T14:30:45.123Z
        Instant instant = Instant.parse("2023-05-10T14:30:45.123Z");
        Date date = Date.from(instant);
        // Expected LocalTime in the converter's zone
        ZoneId zone = converter.getOptions().getZoneId();
        LocalTime expected = LocalDateTime.ofInstant(instant, zone).toLocalTime();
        LocalTime actual = DateConversions.toLocalTime(date, converter);
        assertEquals(expected, actual, "LocalTime derived from java.util.Date must match expected value");
    }

    @Test
    void toLocalTime_fromStringIsoInstant() {
        String iso = "2023-05-10T14:30:45.123Z";
        ZoneId zone = converter.getOptions().getZoneId();
        Instant instant = Instant.parse(iso);
        LocalTime expected = LocalDateTime.ofInstant(instant, zone).toLocalTime();
        LocalTime actual = DateConversions.toLocalTime(iso, converter);
        assertEquals(expected, actual, "String in ISO‑8601 instant format should be converted correctly");
    }

    @Test
    void toLocalTime_fromMapWithEpochMillis() {
        // Map representation expected by Converter for Date/Time types
        // corresponds to 2023‑05‑10T14:30:45.123Z
        long epochMillis = 1_683_731_445_123L;
        Map<String, Object> map = new HashMap<>();
        map.put("time", epochMillis);
        ZoneId zone = converter.getOptions().getZoneId();
        Instant instant = Instant.ofEpochMilli(epochMillis);
        LocalTime expected = LocalDateTime.ofInstant(instant, zone).toLocalTime();
        LocalTime actual = DateConversions.toLocalTime(map, converter);
        assertEquals(expected, actual, "Map containing epoch millis should be converted to correct LocalTime");
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("successfulConversionProvider")
    void toLocalTime_successfulConversions(Object from, ZoneId zone, LocalTime expected, String description) {
        // Build a Converter with the required zone
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(zone);
        Converter customConverter = new Converter(options);
        LocalTime actual = DateConversions.toLocalTime(from, customConverter);
        assertEquals(expected, actual, description);
    }

    @Test
    void toLocalTime_nullFrom_throwsNPE() {
        assertThrows(NullPointerException.class, () -> DateConversions.toLocalTime(null, converter), "Passing null as source should propagate NullPointerException");
    }

    @Test
    void toLocalTime_nullConverter_throwsNPE() {
        Instant instant = Instant.parse("2023-01-01T12:00:00Z");
        assertThrows(NullPointerException.class, () -> DateConversions.toLocalTime(instant, null), "Null converter should cause NullPointerException when options are accessed");
    }

    @Test
    void toLocalTime_unsupportedTypeBoolean_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> DateConversions.toLocalTime(Boolean.TRUE, converter), "Unsupported source type should result in IllegalArgumentException");
    }

    @Test
    void toLocalTime_malformedString_throwsDateTimeParseException() {
        assertThrows(DateTimeParseException.class, () -> DateConversions.toLocalTime("not-a-date", converter), "Malformed ISO string should cause DateTimeParseException");
    }

    @Test
    void toLocalTime_longMaxValue_throwsDateTimeException() {
        assertThrows(DateTimeException.class, () -> DateConversions.toLocalTime(Long.MAX_VALUE, converter), "Epoch millis beyond Instant range should throw DateTimeException");
    }

    @Test
    void toLocalTime_yearMonth_throwsDateTimeException() {
        assertThrows(DateTimeException.class, () -> DateConversions.toLocalTime(YearMonth.of(2022, 5), converter), "YearMonth cannot be converted to an Instant");
    }

    @Test
    void toLocalTime_year_throwsDateTimeException() {
        assertThrows(DateTimeException.class, () -> DateConversions.toLocalTime(Year.of(2025), converter), "Year cannot be converted to an Instant");
    }

    @Test
    void toLocalTime_localDateTimeOutOfRange_throwsDateTimeException() {
        // LocalDateTime beyond the range supported by Instant (year 10000)
        LocalDateTime ldt = LocalDateTime.of(10000, 1, 1, 0, 0, 0);
        assertThrows(DateTimeException.class, () -> DateConversions.toLocalTime(ldt, converter), "Out‑of‑range LocalDateTime should cause DateTimeException");
    }

    @Test
    void toLocalTime_converterWithNullZoneId_throwsNPE() {
        // Mock a Converter whose options return null for ZoneId
        ConverterOptions mockOptions = mock(ConverterOptions.class);
        when(mockOptions.getZoneId()).thenReturn(null);
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        Date date = Date.from(Instant.parse("2023-03-15T10:20:30.123Z"));
        assertThrows(NullPointerException.class, () -> DateConversions.toLocalTime(date, mockConverter), "Null ZoneId in options should cause NullPointerException");
    }
}
