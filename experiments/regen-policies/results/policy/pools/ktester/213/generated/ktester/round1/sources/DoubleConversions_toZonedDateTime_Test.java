package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoubleConversions_toZonedDateTime_Test {

    private Converter converter;

    private ConverterOptions options;

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("America/New_York");

    @Mock
    private Converter // Mockito mock for Converter
    mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a Converter with a known ZoneId so we can verify the zone
        // used by the conversion.
        options = new ConverterOptions();
        options.setZoneId(ZoneId.of("America/New_York"));
        converter = new Converter(options);
    }

    @AfterEach
    void teardownAfterEach() {
        // Release references – helps the GC and keeps the test isolated.
        converter = null;
        options = null;
    }

    private static Stream<Arguments> successfulConversions() {
        return Stream.of(// 1. java.util.Date epoch, UTC
        Arguments.of("Convert java.util.Date epoch (0 ms) to ZonedDateTime in UTC", new Date(0L), ZoneId.of("UTC"), ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneId.of("UTC"))), // 2. Instant with New York zone
        Arguments.of("Convert Instant to ZonedDateTime using America/New_York", Instant.parse("2023-03-15T12:34:56Z"), ZoneId.of("America/New_York"), ZonedDateTime.ofInstant(Instant.parse("2023-03-15T12:34:56Z"), ZoneId.of("America/New_York"))), // 3. java.sql.Timestamp to Paris zone
        Arguments.of("Convert java.sql.Timestamp to ZonedDateTime in Europe/Paris", Timestamp.valueOf("2022-12-31 23:59:59.123"), ZoneId.of("Europe/Paris"), ZonedDateTime.ofInstant(Timestamp.valueOf("2022-12-31 23:59:59.123").toInstant(), ZoneId.of("Europe/Paris"))), // 4. LocalDateTime to Tokyo zone
        Arguments.of("Convert LocalDateTime to ZonedDateTime using Asia/Tokyo", LocalDateTime.of(2021, 5, 10, 15, 30, 45), ZoneId.of("Asia/Tokyo"), ZonedDateTime.ofInstant(LocalDateTime.of(2021, 5, 10, 15, 30, 45).atZone(ZoneId.of("Asia/Tokyo")).toInstant(), ZoneId.of("Asia/Tokyo"))), // 5. Calendar (UTC) to UTC zone
        Arguments.of("Convert Calendar (UTC) to ZonedDateTime in UTC", calendarWithMillis(1_600_000_000_000L, "UTC"), ZoneId.of("UTC"), ZonedDateTime.ofInstant(Instant.ofEpochMilli(1_600_000_000_000L), ZoneId.of("UTC"))));
    }

    private static Calendar calendarWithMillis(long epochMilli, String tzId) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(tzId));
        cal.setTimeInMillis(epochMilli);
        return cal;
    }

    private static Stream<Arguments> additionalSuccessfulConversions() {
        return Stream.of(// 1. Existing ZonedDateTime (London) to Los Angeles
        Arguments.of("Convert ZonedDateTime London to America/Los_Angeles", ZonedDateTime.of(2022, 1, 1, 12, 0, 0, 0, ZoneId.of("Europe/London")), ZoneId.of("America/Los_Angeles"), ZonedDateTime.ofInstant(ZonedDateTime.of(2022, 1, 1, 12, 0, 0, 0, ZoneId.of("Europe/London")).toInstant(), ZoneId.of("America/Los_Angeles"))), // 2. LocalDate to Asia/Kolkata
        Arguments.of("Convert LocalDate to ZonedDateTime in Asia/Kolkata (start‑of‑day)", LocalDate.of(2022, 12, 25), ZoneId.of("Asia/Kolkata"), ZonedDateTime.ofInstant(LocalDate.of(2022, 12, 25).atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant(), ZoneId.of("Asia/Kolkata"))), // 3. LocalTime to UTC (epoch date)
        Arguments.of("Convert LocalTime to ZonedDateTime in UTC (epoch date 1970‑01‑01)", LocalTime.of(15, 45, 30), ZoneId.of("UTC"), ZonedDateTime.ofInstant(LocalDate.of(1970, 1, 1).atTime(LocalTime.of(15, 45, 30)).atZone(ZoneId.of("UTC")).toInstant(), ZoneId.of("UTC"))), // 4. OffsetDateTime to Europe/Paris
        Arguments.of("Convert OffsetDateTime to ZonedDateTime in Europe/Paris", OffsetDateTime.parse("2023-05-01T10:15:30+02:00"), ZoneId.of("Europe/Paris"), ZonedDateTime.ofInstant(OffsetDateTime.parse("2023-05-01T10:15:30+02:00").toInstant(), ZoneId.of("Europe/Paris"))), // 5. OffsetTime to UTC (epoch date)
        Arguments.of("Convert OffsetTime to ZonedDateTime in UTC (epoch date 1970‑01‑01)", OffsetTime.parse("23:59:59+01:00"), ZoneId.of("UTC"), ZonedDateTime.ofInstant(LocalDate.of(1970, 1, 1).atTime(OffsetTime.parse("23:59:59+01:00").toLocalTime()).atOffset(OffsetTime.parse("23:59:59+01:00").getOffset()).toInstant(), ZoneId.of("UTC"))), // 6. Long epoch millis to UTC
        Arguments.of("Convert Long epoch‑millis to ZonedDateTime in UTC", Long.valueOf(1_600_000_000_000L), ZoneId.of("UTC"), ZonedDateTime.ofInstant(Instant.ofEpochMilli(1_600_000_000_000L), ZoneId.of("UTC"))), // 7. java.sql.Date to Europe/Berlin
        Arguments.of("Convert java.sql.Date to ZonedDateTime in Europe/Berlin (start‑of‑day)", java.sql.Date.valueOf("2022-10-05"), ZoneId.of("Europe/Berlin"), ZonedDateTime.ofInstant(java.sql.Date.valueOf("2022-10-05").toInstant(), ZoneId.of("Europe/Berlin"))));
    }

    @Test
    void testToZonedDateTimeFromInstant() {
        // GIVEN
        Instant now = Instant.now();
        // WHEN
        ZonedDateTime result = DoubleConversions.toZonedDateTime(now, converter);
        // THEN
        // The instant part must be identical to the source instant.
        assertEquals(now, result.toInstant(), "The ZonedDateTime should represent the same instant as the source Instant.");
        // The zone must be the one supplied via ConverterOptions.
        assertEquals(options.getZoneId(), result.getZone(), "The ZonedDateTime should use the ZoneId configured in ConverterOptions.");
    }

    @Test
    void testToZonedDateTimeFromDate() {
        // GIVEN
        java.util.Date date = new java.util.Date();
        // WHEN
        ZonedDateTime result = DoubleConversions.toZonedDateTime(date, converter);
        // THEN
        // The instant derived from the Date must match.
        assertEquals(date.toInstant(), result.toInstant(), "The ZonedDateTime should represent the same instant as the source Date.");
        // The zone must be the one configured in the Converter.
        assertEquals(options.getZoneId(), result.getZone(), "The ZonedDateTime should use the ZoneId configured in ConverterOptions.");
    }

    @Test
    void testToZonedDateTimeFromEpochMillis() {
        // GIVEN
        long epochMillis = System.currentTimeMillis();
        // WHEN
        ZonedDateTime result = DoubleConversions.toZonedDateTime(epochMillis, converter);
        // THEN
        assertEquals(Instant.ofEpochMilli(epochMillis), result.toInstant(), "The ZonedDateTime should represent the same instant as the epoch milliseconds.");
        assertEquals(options.getZoneId(), result.getZone(), "The ZonedDateTime should use the ZoneId configured in ConverterOptions.");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("successfulConversions")
    void testSuccessfulConversions(String description, Object from, ZoneId zoneId, ZonedDateTime expected) {
        // arrange – set the zone required for this case
        options.setZoneId(zoneId);
        when(mockConverter.getOptions()).thenReturn(options);
        // act
        ZonedDateTime actual = DoubleConversions.toZonedDateTime(from, mockConverter);
        // assert – compare instant and zone separately for clearer failure messages
        assertEquals(expected.toInstant(), actual.toInstant(), () -> "Instant mismatch for case: " + description);
        assertEquals(expected.getZone(), actual.getZone(), () -> "Zone mismatch for case: " + description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("additionalSuccessfulConversions")
    void testAdditionalSuccessfulConversions(String description, Object from, ZoneId zoneId, ZonedDateTime expected) {
        options.setZoneId(zoneId);
        when(mockConverter.getOptions()).thenReturn(options);
        ZonedDateTime actual = DoubleConversions.toZonedDateTime(from, mockConverter);
        assertEquals(expected.toInstant(), actual.toInstant(), () -> "Instant mismatch for case: " + description);
        assertEquals(expected.getZone(), actual.getZone(), () -> "Zone mismatch for case: " + description);
    }

    @Test
    void testNullSourceThrowsNPE() {
        // source object is null – expect NPE regardless of converter
        assertThrows(NullPointerException.class, () -> DoubleConversions.toZonedDateTime(null, mockConverter), "Null source should cause NullPointerException");
    }

    @Test
    void testUnsupportedStringThrowsIAE() {
        // plain string, not a supported type
        String unsupported = "2023-03-15T12:34:56Z";
        assertThrows(IllegalArgumentException.class, () -> DoubleConversions.toZonedDateTime(unsupported, mockConverter), "Unsupported String should cause IllegalArgumentException");
    }

    @Test
    void testUnsupportedObjectThrowsIAE() {
        Object unsupported = new Object();
        assertThrows(IllegalArgumentException.class, () -> DoubleConversions.toZonedDateTime(unsupported, mockConverter), "Unsupported Object should cause IllegalArgumentException");
    }

    @Test
    void testConverterWithNullZoneIdFallsBackToSystemDefault() {
        // Arrange – converter's options return null for zoneId
        ConverterOptions nullZoneOptions = new ConverterOptions();
        nullZoneOptions.setZoneId(null);
        when(mockConverter.getOptions()).thenReturn(nullZoneOptions);
        Instant instant = Instant.parse("2023-03-15T12:34:56Z");
        ZonedDateTime result = DoubleConversions.toZonedDateTime(instant, mockConverter);
        assertEquals(instant, result.toInstant(), "Instant must stay unchanged");
        assertEquals(ZoneId.systemDefault(), result.getZone(), "When zoneId is null the system default zone should be used");
    }

    @Test
    void testDateBeyondInstantRangeThrowsDateTimeException() {
        // Date with Long.MAX_VALUE milliseconds is out of Instant range
        Date outOfRange = new Date(Long.MAX_VALUE);
        assertThrows(DateTimeException.class, () -> DoubleConversions.toZonedDateTime(outOfRange, mockConverter), "Out‑of‑range Date should cause DateTimeException");
    }

    @Test
    void testLocalDateTimeOutOfRangeThrowsDateTimeException() {
        // year far beyond supported range for Instant
        LocalDateTime ldt = LocalDateTime.of(999_999_999, 12, 31, 23, 59, 59);
        assertThrows(DateTimeException.class, () -> DoubleConversions.toZonedDateTime(ldt, mockConverter), "Out‑of‑range LocalDateTime should cause DateTimeException");
    }

    @Test
    void testOffsetDateTimeOutOfRangeThrowsDateTimeException() {
        // OffsetDateTime with absurd year
        OffsetDateTime odt = OffsetDateTime.parse("999999999-12-31T23:59:59+00:00");
        assertThrows(DateTimeException.class, () -> DoubleConversions.toZonedDateTime(odt, mockConverter), "Out‑of‑range OffsetDateTime should cause DateTimeException");
    }

    @Test
    void testVeryNegativeEpochMillisThrowsDateTimeException() {
        Long veryNegative = Long.valueOf(Long.MIN_VALUE);
        assertThrows(DateTimeException.class, () -> DoubleConversions.toZonedDateTime(veryNegative, mockConverter), "Very negative epoch millis should cause DateTimeException");
    }

    @Test
    void testNullConverterThrowsNPE() {
        // Both source and converter are null – NPE expected before type checks
        assertThrows(NullPointerException.class, () -> DoubleConversions.toZonedDateTime(new Object(), null), "Null converter should cause NullPointerException");
    }

    @Test
    void testConverterOptionsNullThrowsNPE() {
        // Converter returns null options – accessing getZoneId() leads to NPE
        when(mockConverter.getOptions()).thenReturn(null);
        Date date = new Date(0L);
        assertThrows(NullPointerException.class, () -> DoubleConversions.toZonedDateTime(date, mockConverter), "Null options inside Converter should cause NullPointerException");
    }
}
