package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.sql.Timestamp;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DoubleConversions_toZonedDateTime_Test {

    /**
     * The Converter used for all test cases.
     */
    private Converter converter;

    /**
     * The ZoneId that the converter will apply when creating a ZonedDateTime.
     */
    private static final ZoneId TEST_ZONE = ZoneId.of("America/New_York");

    @BeforeEach
    void setupBeforeEach() {
        // Create a Converter with a custom zone so the expected value is deterministic.
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(TEST_ZONE);
        converter = new Converter(options);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the pattern consistent.
        converter = null;
    }

    private static Stream<Arguments> successfulConversionsProvider() {
        return Stream.of(// Instant → ZonedDateTime (UTC)
        Arguments.of("Convert an Instant to ZonedDateTime using UTC zone", Instant.parse("2023-01-15T10:15:30.00Z"), ZoneId.of("UTC"), ZonedDateTime.ofInstant(Instant.parse("2023-01-15T10:15:30.00Z"), ZoneId.of("UTC"))), // LocalDateTime → ZonedDateTime (America/New_York)
        Arguments.of("Convert a LocalDateTime to ZonedDateTime with New York zone", LocalDateTime.of(2023, 5, 20, 14, 30, 0), ZoneId.of("America/New_York"), ZonedDateTime.of(LocalDateTime.of(2023, 5, 20, 14, 30, 0), ZoneId.of("America/New_York"))), // java.util.Date (epoch) → ZonedDateTime (Asia/Tokyo)
        Arguments.of("Convert a java.util.Date (epoch) to ZonedDateTime with Tokyo zone", new Date(0L), ZoneId.of("Asia/Tokyo"), ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneId.of("Asia/Tokyo"))), // java.sql.Timestamp → ZonedDateTime (Europe/London)
        Arguments.of("Convert a java.sql.Timestamp to ZonedDateTime using London zone", Timestamp.valueOf("2022-12-31 23:59:59"), ZoneId.of("Europe/London"), ZonedDateTime.ofInstant(Timestamp.valueOf("2022-12-31 23:59:59").toInstant(), ZoneId.of("Europe/London"))), // ZonedDateTime (Paris) → ZonedDateTime (Los Angeles)
        Arguments.of("Convert a ZonedDateTime (Paris zone) to another zone (Los Angeles)", ZonedDateTime.of(LocalDateTime.of(2022, 2, 28, 23, 0, 0), ZoneId.of("Europe/Paris")), ZoneId.of("America/Los_Angeles"), ZonedDateTime.ofInstant(ZonedDateTime.of(LocalDateTime.of(2022, 2, 28, 23, 0, 0), ZoneId.of("Europe/Paris")).toInstant(), ZoneId.of("America/Los_Angeles"))), // OffsetDateTime (-05:00) → ZonedDateTime (UTC)
        Arguments.of("Convert an OffsetDateTime with a -05:00 offset to UTC ZonedDateTime", OffsetDateTime.of(2021, 12, 31, 23, 59, 59, 0, ZoneOffset.ofHours(-5)), ZoneId.of("UTC"), ZonedDateTime.ofInstant(OffsetDateTime.of(2021, 12, 31, 23, 59, 59, 0, ZoneOffset.ofHours(-5)).toInstant(), ZoneId.of("UTC"))), // Calendar (UTC) → ZonedDateTime (Europe/Berlin)
        Arguments.of("Convert a java.util.Calendar (set to 2020-06-15T12:30:00Z) to a ZonedDateTime in the Berlin zone", createCalendarUTC(2020, Calendar.JUNE, 15, 12, 30, 0), ZoneId.of("Europe/Berlin"), ZonedDateTime.ofInstant(createCalendarUTC(2020, Calendar.JUNE, 15, 12, 30, 0).toInstant(), ZoneId.of("Europe/Berlin"))), // java.sql.Date → ZonedDateTime (Asia/Kolkata)
        Arguments.of("Convert a java.sql.Date (date‑only) to ZonedDateTime – start of day shifted to Asia/Kolkata", java.sql.Date.valueOf("2000-01-01"), ZoneId.of("Asia/Kolkata"), ZonedDateTime.ofInstant(java.sql.Date.valueOf("2000-01-01").toInstant(), ZoneId.of("Asia/Kolkata"))));
    }

    /**
     * Helper to create a Calendar set to UTC with the supplied fields.
     */
    private static Calendar createCalendarUTC(int year, int month, int day, int hour, int minute, int second) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(year, month, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    private static Stream<Arguments> errorHandlingProvider() {
        return Stream.of(// Null source – according to the JSON this should throw NPE.
        Arguments.of("Passing a null source object should result in a NullPointerException.", null, ZoneId.of("UTC"), NullPointerException.class), // Unsupported plain Object
        Arguments.of("An unsupported source type (plain Object) should cause an IllegalArgumentException.", new Object(), ZoneId.of("UTC"), IllegalArgumentException.class), // Duration (unsupported)
        Arguments.of("Duration cannot be converted to an Instant; the method should throw IllegalArgumentException.", Duration.ofHours(5), ZoneId.of("UTC"), IllegalArgumentException.class), // Converter with null ZoneId – illegal configuration
        Arguments.of("Creating a Converter with a null ZoneId is illegal; the conversion should fail with IllegalArgumentException.", Instant.parse("1960-01-01T00:00:00Z"), null, IllegalArgumentException.class), // Null converter argument
        Arguments.of("Passing a null Converter should cause a NullPointerException when the method attempts to call converter.getOptions().", Instant.now(), ZoneId.of("UTC"), NullPointerException.class));
    }

    @Test
    void shouldConvertInstantToZonedDateTime() {
        Instant instant = Instant.parse("2023-03-15T12:34:56Z");
        ZonedDateTime expected = instant.atZone(TEST_ZONE);
        ZonedDateTime actual = DoubleConversions.toZonedDateTime(instant, converter);
        assertEquals(expected, actual, "Instant should be converted to ZonedDateTime using the converter's zone");
    }

    @Test
    void shouldConvertDateToZonedDateTime() {
        // java.util.Date stores milliseconds since epoch.
        Date date = Date.from(Instant.parse("2022-01-01T08:00:00Z"));
        ZonedDateTime expected = date.toInstant().atZone(TEST_ZONE);
        ZonedDateTime actual = DoubleConversions.toZonedDateTime(date, converter);
        assertEquals(expected, actual, "Date should be converted to ZonedDateTime using the converter's zone");
    }

    @Test
    void shouldConvertLongMillisToZonedDateTime() {
        // 2022-01-01T00:00:00Z
        long millis = 1_640_995_200_000L;
        ZonedDateTime expected = Instant.ofEpochMilli(millis).atZone(TEST_ZONE);
        ZonedDateTime actual = DoubleConversions.toZonedDateTime(millis, converter);
        assertEquals(expected, actual, "Long milliseconds should be converted to ZonedDateTime");
    }

    @Test
    void shouldConvertCalendarToZonedDateTime() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        // 2022-01-01T00:00:00Z
        calendar.setTimeInMillis(1_640_995_200_000L);
        ZonedDateTime expected = calendar.toInstant().atZone(TEST_ZONE);
        ZonedDateTime actual = DoubleConversions.toZonedDateTime(calendar, converter);
        assertEquals(expected, actual, "Calendar should be converted to ZonedDateTime");
    }

    @Test
    void shouldConvertZonedDateTimeIdentity() {
        ZonedDateTime source = ZonedDateTime.of(2021, 12, 31, 23, 59, 59, 0, ZoneId.of("Europe/Paris"));
        // The method first converts to Instant, then re‑applies the converter's zone.
        ZonedDateTime expected = source.toInstant().atZone(TEST_ZONE);
        ZonedDateTime actual = DoubleConversions.toZonedDateTime(source, converter);
        assertEquals(expected, actual, "Existing ZonedDateTime should be re‑zoned to the converter's zone");
    }

    @Test
    void shouldReturnNullWhenSourceIsNull() {
        ZonedDateTime actual = DoubleConversions.toZonedDateTime(null, converter);
        assertNull(actual, "Converting a null source should return null");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("successfulConversionsProvider")
    void shouldConvertVariousSources(String description, Object from, ZoneId zone, ZonedDateTime expected) {
        // Build a Converter that uses the zone required for this case.
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(zone);
        Converter localConverter = new Converter(options);
        ZonedDateTime actual = DoubleConversions.toZonedDateTime(from, localConverter);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("errorHandlingProvider")
    void shouldThrowForInvalidInputs(String description, Object from, ZoneId zone, Class<? extends Throwable> expectedException) {
        // Build a Converter – if the test case wants a null zone we pass null.
        Converter localConverter;
        if (zone != null) {
            ConverterOptions options = new ConverterOptions();
            options.setZoneId(zone);
            localConverter = new Converter(options);
        } else {
            // Simulate a Converter constructed with a null ZoneId (illegal per spec)
            // The actual constructor may throw, but we create it with null options to provoke the conversion error.
            ConverterOptions options = new ConverterOptions();
            options.setZoneId(null);
            localConverter = new Converter(options);
        }
        // If the test itself expects a NullPointerException because the converter is null,
        // we pass null directly.
        if (description.contains("null Converter")) {
            localConverter = null;
        }
        assertThrows(expectedException, () -> DoubleConversions.toZonedDateTime(from, localConverter), description);
    }
}
