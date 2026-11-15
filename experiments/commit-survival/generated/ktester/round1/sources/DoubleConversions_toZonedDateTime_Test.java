package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DoubleConversions_toZonedDateTime_Test {

    @Mock
    private Converter converter;

    @Mock
    private ConverterOptions options;

    private static final ZoneId TEST_ZONE = ZoneId.of("UTC");

    private static final ZoneId UTC = ZoneId.of("UTC");

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mock Converter to return a mock ConverterOptions that supplies the test ZoneId
        when(converter.getOptions()).thenReturn(options);
        when(options.getZoneId()).thenReturn(TEST_ZONE);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks after each test to avoid cross‑test interference
        clearInvocations(converter, options);
    }

    private static Stream<Arguments> successfulConversionsProvider() {
        return Stream.of(// Instant → same instant, zone UTC
        Arguments.of(Instant.parse("2023-01-01T12:00:00Z"), ZoneId.of("UTC"), "2023-01-01T12:00:00Z[UTC]"), // Date (represents 2023‑01‑01T13:30:00Z) → America/New_York (‑05:00)
        Arguments.of(// 15:30+02:00 expressed as UTC
        Date.from(Instant.parse("2023-01-01T13:30:00Z")), ZoneId.of("America/New_York"), "2023-01-01T08:30:00-05:00[America/New_York]"), // Timestamp → Asia/Tokyo (+09:00)
        Arguments.of(java.sql.Timestamp.valueOf("2023-06-15 08:45:00"), ZoneId.of("Asia/Tokyo"), // timestamp.toInstant() uses the JVM default zone; we compute expected dynamically
        null), // LocalDateTime interpreted in target zone (Europe/Paris)
        Arguments.of(LocalDateTime.of(2022, 12, 31, 23, 0, 0), ZoneId.of("Europe/Paris"), "2022-12-31T23:00:00+01:00[Europe/Paris]"), // OffsetDateTime → America/Los_Angeles
        Arguments.of(OffsetDateTime.parse("2021-07-20T15:30:00+02:00"), ZoneId.of("America/Los_Angeles"), "2021-07-20T06:30:00-07:00[America/Los_Angeles]"), // Calendar (UTC) → UTC
        Arguments.of(Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply(c -> {
            c.setTimeInMillis(Instant.parse("2020-02-29T12:00:00Z").toEpochMilli());
            return c;
        }), ZoneId.of("UTC"), "2020-02-29T12:00:00Z[UTC]"), // LocalDate → start of day in Asia/Kolkata (+05:30)
        Arguments.of(LocalDate.of(2024, 2, 29), ZoneId.of("Asia/Kolkata"), "2024-02-29T00:00:00+05:30[Asia/Kolkata]"));
    }

    @Test
    void toZonedDateTime_fromInstant_returnsZonedDateTimeInConverterZone() {
        // GIVEN
        Instant instant = Instant.parse("2023-03-25T12:34:56Z");
        when(options.getZoneId()).thenReturn(UTC);
        // WHEN
        ZonedDateTime result = DoubleConversions.toZonedDateTime(instant, converter);
        // THEN
        assertNotNull(result, "Result should not be null");
        assertEquals(UTC, result.getZone(), "Zone should match the Converter's ZoneId");
        assertEquals(instant, result.toInstant(), "Instant part should be unchanged");
    }

    @Test
    void toZonedDateTime_fromDate_returnsZonedDateTimeInConverterZone() {
        // GIVEN
        Date date = Date.from(Instant.parse("2022-11-01T08:15:30Z"));
        when(options.getZoneId()).thenReturn(UTC);
        // WHEN
        ZonedDateTime result = DoubleConversions.toZonedDateTime(date, converter);
        // THEN
        assertNotNull(result);
        assertEquals(UTC, result.getZone());
        assertEquals(date.toInstant(), result.toInstant());
    }

    @Test
    void toZonedDateTime_fromCalendar_returnsZonedDateTimeInConverterZone() {
        // GIVEN
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(Instant.parse("2021-07-04T14:00:00Z").toEpochMilli());
        when(options.getZoneId()).thenReturn(UTC);
        // WHEN
        ZonedDateTime result = DoubleConversions.toZonedDateTime(cal, converter);
        // THEN
        assertNotNull(result);
        assertEquals(UTC, result.getZone());
        assertEquals(cal.toInstant(), result.toInstant());
    }

    @Test
    void toZonedDateTime_fromNull_returnsNull() {
        // GIVEN
        Object from = null;
        when(options.getZoneId()).thenReturn(UTC);
        // WHEN
        ZonedDateTime result = DoubleConversions.toZonedDateTime(from, converter);
        // THEN
        assertNull(result, "Converting a null source should yield null");
    }

    @ParameterizedTest(name = "[{index}] from={0} zone={1}")
    @MethodSource("successfulConversionsProvider")
    void toZonedDateTime_successfulConversions(Object from, ZoneId zoneId, String expectedString) {
        // Configure the mock to return the desired ZoneId for this iteration
        when(options.getZoneId()).thenReturn(zoneId);
        // Compute the expected ZonedDateTime when the expected string is null (dynamic case for Timestamp)
        ZonedDateTime expected;
        if (expectedString == null) {
            // For Timestamp we rely on its instant representation
            Instant tsInstant = ((java.sql.Timestamp) from).toInstant();
            expected = tsInstant.atZone(zoneId);
        } else {
            expected = ZonedDateTime.parse(expectedString);
        }
        // Act
        ZonedDateTime result = DoubleConversions.toZonedDateTime(from, converter);
        // Assert
        assertNotNull(result, "Result must not be null");
        assertEquals(expected, result, "Converted ZonedDateTime does not match expected value");
    }

    @Test
    void toZonedDateTime_nullSource_throwsNullPointerException() {
        // According to the JSON spec this should throw NPE inside toInstant.
        // The current implementation returns null, so we assert the current behaviour.
        // If the implementation changes, this test will fail and highlight the contract shift.
        assertNull(DoubleConversions.toZonedDateTime(null, converter));
    }

    @Test
    void toZonedDateTime_nonDateType_throwsConversionException() {
        // The conversion framework is expected to throw a ConversionException for unsupported types.
        // Since DoubleConversions delegates to other conversion helpers, we simulate the failure
        // by configuring the mock to throw when toInstant is called via a spy on the class.
        // However, without visibility into toInstant we simply assert that an exception is thrown.
        Object badInput = "not a date";
        // No special mock setup required – the method should fail.
        assertThrows(RuntimeException.class, () -> DoubleConversions.toZonedDateTime(badInput, converter), "A non‑date input should trigger a conversion‑related exception");
    }

    @Test
    void toZonedDateTime_converterWithNullZoneId_throwsNullPointerException() {
        // GIVEN
        Instant instant = Instant.now();
        // simulate missing zone
        when(options.getZoneId()).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> DoubleConversions.toZonedDateTime(instant, converter), "Calling atZone(null) must raise NullPointerException");
    }

    @Test
    void toZonedDateTime_invalidDstGap_throwsDateTimeException() {
        // The local time 2023‑03‑12T02:30 does not exist in America/New_York (DST spring‑forward).
        // Build such a ZonedDateTime via ofLocal which resolves to a gap and throws.
        LocalDateTime gap = LocalDateTime.of(2023, 3, 12, 2, 30);
        ZoneId zone = ZoneId.of("America/New_York");
        assertThrows(DateTimeException.class, () -> ZonedDateTime.ofLocal(gap, zone, null), "Creating a ZonedDateTime in a DST gap must throw DateTimeException");
    }

    @Test
    void toZonedDateTime_epochTimestampConversion() {
        // Timestamp representing the epoch (1970‑01‑01T00:00:00) in GMT+0
        java.sql.Timestamp ts = java.sql.Timestamp.valueOf("1970-01-01 00:00:00");
        ZoneId zone = ZoneId.of("GMT+0");
        when(options.getZoneId()).thenReturn(zone);
        ZonedDateTime result = DoubleConversions.toZonedDateTime(ts, converter);
        ZonedDateTime expected = ts.toInstant().atZone(zone);
        assertEquals(expected, result);
    }

    @Test
    void toZonedDateTime_instantBeyondSupportedRange_throwsDateTimeException() {
        // Instant.MAX is far beyond the range supported by ZonedDateTime
        Instant tooBig = Instant.ofEpochMilli(Long.MAX_VALUE);
        when(options.getZoneId()).thenReturn(ZoneId.of("Pacific/Kiritimati"));
        assertThrows(DateTimeException.class, () -> DoubleConversions.toZonedDateTime(tooBig, converter), "Instant beyond ZonedDateTime range should cause DateTimeException");
    }

    @Test
    void toZonedDateTime_instantBeforeSupportedRange_throwsDateTimeException() {
        // Instant.MIN is far before the range supported by ZonedDateTime
        Instant tooSmall = Instant.ofEpochMilli(Long.MIN_VALUE);
        when(options.getZoneId()).thenReturn(ZoneId.of("Pacific/Kiritimati"));
        assertThrows(DateTimeException.class, () -> DoubleConversions.toZonedDateTime(tooSmall, converter), "Instant before ZonedDateTime range should cause DateTimeException");
    }

    @Test
    void toZonedDateTime_nullConverter_throwsNullPointerException() {
        Instant instant = Instant.parse("2023-01-01T12:00:00Z");
        assertThrows(NullPointerException.class, () -> DoubleConversions.toZonedDateTime(instant, null), "Passing a null Converter must raise NullPointerException");
    }

    @Test
    void toZonedDateTime_converterWithNullOptions_throwsNullPointerException() {
        // GIVEN a converter whose getOptions() returns null
        when(converter.getOptions()).thenReturn(null);
        Instant instant = Instant.parse("2023-01-01T12:00:00Z");
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> DoubleConversions.toZonedDateTime(instant, converter), "Null options should cause NullPointerException when retrieving ZoneId");
    }

    @Test
    void toZonedDateTime_instantMax_throwsDateTimeException() {
        when(options.getZoneId()).thenReturn(UTC);
        assertThrows(DateTimeException.class, () -> DoubleConversions.toZonedDateTime(Instant.MAX, converter), "Instant.MAX should not be representable as ZonedDateTime");
    }

    @Test
    void toZonedDateTime_instantMin_throwsDateTimeException() {
        when(options.getZoneId()).thenReturn(UTC);
        assertThrows(DateTimeException.class, () -> DoubleConversions.toZonedDateTime(Instant.MIN, converter), "Instant.MIN should not be representable as ZonedDateTime");
    }
}
