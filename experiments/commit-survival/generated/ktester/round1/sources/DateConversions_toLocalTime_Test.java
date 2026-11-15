package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Date;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import org.mockito.Mockito;
import java.time.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DateConversions_toLocalTime_Test {

    private Converter converter;

    /**
     * A mock Converter that will return a pre‑configured ConverterOptions.
     */
    @Mock
    private Converter mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Build a Converter with a deterministic ZoneId (UTC) so the expected
        // LocalTime values are independent of the machine’s default time‑zone.
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(ZoneId.of("UTC"));
        converter = new Converter(options);
    }

    /**
     * Helper to build a mock Converter that returns the supplied ZoneId.
     */
    private static Converter mockConverterWithZone(String zoneId) {
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(ZoneId.of(zoneId));
        Converter converter = mock(Converter.class);
        when(converter.getOptions()).thenReturn(options);
        return converter;
    }

    /**
     * Supplies the arguments for {@link #toLocalTime_validConversions(Object, String, LocalTime)}.
     */
    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// 1. java.util.Date → UTC
        Arguments.of(// 2023‑05‑15T12:34:56Z
        new Date(1684155296000L), "UTC", LocalTime.of(12, 34, 56)), // 2. java.sql.Timestamp → America/New_York (UTC‑5)
        Arguments.of(java.sql.Timestamp.from(Instant.parse("2023-05-15T08:15:30.123Z")), "America/New_York", // nanos are ignored per test‑case description
        LocalTime.of(4, 15, 30)), // 3. java.time.Instant → UTC+02:00
        Arguments.of(Instant.parse("2023-01-01T23:45:00Z"), "UTC+02:00", LocalTime.of(1, 45, 0)), // 4. java.time.LocalDateTime → UTC
        Arguments.of(LocalDateTime.of(2022, 12, 31, 23, 59, 59), "UTC", LocalTime.of(23, 59, 59)), // 5. java.time.ZonedDateTime → UTC
        Arguments.of(ZonedDateTime.of(2022, 6, 1, 10, 20, 30, 0, // +03:00
        ZoneId.of("Europe/Moscow")), "UTC", LocalTime.of(7, 20, 30)), // 6. java.time.OffsetDateTime → UTC
        Arguments.of(OffsetDateTime.of(2022, 6, 1, 10, 20, 30, 0, // +05:30
        ZoneOffset.ofHoursMinutes(5, 30)), "UTC", LocalTime.of(4, 50, 30)), // 7. Long epoch millis → UTC
        Arguments.of(// epoch
        0L, "UTC", LocalTime.of(0, 0, 0)), // 8. String in ISO_LOCAL_DATE_TIME format → UTC
        Arguments.of("2023-04-05T14:30:15", "UTC", LocalTime.of(14, 30, 15)));
    }

    @Test
    void toLocalTime_fromJavaUtilDate_returnsCorrectLocalTime() {
        // GIVEN: a java.util.Date representing the epoch (1970‑01‑01T00:00:00Z)
        Date epoch = new Date(0L);
        // WHEN: converting the Date to a LocalTime using the UTC zone
        LocalTime result = DateConversions.toLocalTime(epoch, converter);
        // THEN: the resulting LocalTime should be midnight (00:00) in UTC
        assertEquals(LocalTime.MIDNIGHT, result, "LocalTime should be midnight for epoch in UTC");
    }

    @Test
    void toLocalTime_fromSqlTimestamp_returnsCorrectLocalTime() {
        // GIVEN: a java.sql.Timestamp for 2023‑03‑15T12:34:56.789Z
        java.sql.Timestamp ts = java.sql.Timestamp.from(java.time.Instant.parse("2023-03-15T12:34:56.789Z"));
        // WHEN: converting the Timestamp to a LocalTime using the UTC zone
        LocalTime result = DateConversions.toLocalTime(ts, converter);
        // THEN: the LocalTime should reflect the time component of the instant
        assertEquals(LocalTime.of(12, 34, 56, 789_000_000), result, "LocalTime should match the time part of the Timestamp in UTC");
    }

    /**
     * Parameterized test that verifies {@code toLocalTime} works for all supported
     * source types with the expected ZoneId.
     */
    @ParameterizedTest(name = "{index}: from={0} zone={1} => expected={2}")
    @MethodSource("validConversionProvider")
    void toLocalTime_validConversions(Object source, String zoneId, LocalTime expected) {
        // GIVEN: a mock Converter that reports the required ZoneId
        Converter converter = mockConverterWithZone(zoneId);
        // WHEN: the conversion is performed
        LocalTime result = DateConversions.toLocalTime(source, converter);
        // THEN: the result matches the expected LocalTime (nanoseconds are ignored
        //       unless the expected value explicitly contains them)
        assertEquals(expected, result, () -> String.format("Conversion of %s with zone %s produced %s, expected %s", source.getClass().getSimpleName(), zoneId, result, expected));
    }

    @Test
    void toLocalTime_nullSource_throwsNPE() {
        // GIVEN: a valid Converter (UTC)
        Converter converter = mockConverterWithZone("UTC");
        // WHEN / THEN: passing null as the source should raise NPE
        assertThrows(NullPointerException.class, () -> DateConversions.toLocalTime(null, converter), "Expected NullPointerException when source object is null");
    }

    @Test
    void toLocalTime_nullConverter_throwsNPE() {
        // GIVEN: a non‑null source object
        Date date = new Date(0L);
        // WHEN / THEN: passing null as the converter should raise NPE
        assertThrows(NullPointerException.class, () -> DateConversions.toLocalTime(date, null), "Expected NullPointerException when Converter is null");
    }

    @Test
    void toLocalTime_bothNull_throwsNPE() {
        // WHEN / THEN: both arguments null should raise NPE
        assertThrows(NullPointerException.class, () -> DateConversions.toLocalTime(null, null), "Expected NullPointerException when both arguments are null");
    }
}
