package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toCalendar_Test {

    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    private AutoCloseable mocks;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise @Mock fields (handled by MockitoExtension) and set common stubbing.
        mocks = MockitoAnnotations.openMocks(this);
        // The converter should return the mocked options.
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        // Default to UTC for all tests – individual tests may override if needed.
        when(mockOptions.getTimeZone()).thenReturn(TimeZone.getTimeZone("UTC"));
        // Stub the generic conversion to long used by OffsetDateTimeConversions.toLong(...).
        // The real implementation ultimately calls converter.convert(value, Long.class);
        when(mockConverter.convert(any(), eq(Long.class))).thenAnswer(invocation -> {
            Object src = invocation.getArgument(0);
            if (src instanceof Number) {
                return ((Number) src).longValue();
            }
            if (src instanceof Date) {
                return ((Date) src).getTime();
            }
            // For unsupported types return null – the method under test will then throw,
            // but our test inputs are limited to supported ones.
            return null;
        });
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Close the Mockito openMocks resource.
        if (mocks != null) {
            mocks.close();
        }
    }

    /* -------------------------------------------------------------
     *  Valid conversion scenarios (parameterised)
     * ------------------------------------------------------------- */
    private static Stream<Arguments> validConversionsProvider() {
        return Stream.of(// primitive long
        Arguments.of(1622548800000L, "UTC", 1622548800000L, "primitive long"), // ISO‑8601 string
        Arguments.of("2021-06-01T00:00:00Z", "UTC", 1622505600000L, "ISO‑8601 string"), // java.util.Date
        Arguments.of(new Date(1622548800000L), "UTC", 1622548800000L, "java.util.Date"), // java.time.Instant
        Arguments.of(Instant.ofEpochMilli(1622548800000L), "UTC", 1622548800000L, "Instant"), // OffsetDateTime with +02:00 offset (converted to UTC)
        Arguments.of(OffsetDateTime.parse("2021-06-01T00:00:00+02:00"), "UTC", 1622505600000L, "OffsetDateTime +02:00"), // custom time‑zone supplied by ConverterOptions
        Arguments.of(1622548800000L, "America/New_York", 1622548800000L, "primitive long with custom zone"), // java.sql.Timestamp (subclass of Date)
        Arguments.of(new Timestamp(1622548800000L), "UTC", 1622548800000L, "Timestamp"), // LocalDateTime (interpreted in the converter's zone)
        Arguments.of(LocalDateTime.of(2021, 6, 1, 0, 0), "UTC", 1622505600000L, "LocalDateTime"), // LocalDate (start of day in the converter's zone)
        Arguments.of(LocalDate.of(2021, 6, 1), "UTC", 1622505600000L, "LocalDate"), // LocalTime (date defaults to epoch day)
        Arguments.of(LocalTime.of(12, 34, 56), "UTC", 45_296_000L, "LocalTime"), // BigDecimal representing epoch millis
        Arguments.of(BigDecimal.valueOf(1622548800000L), "UTC", 1622548800000L, "BigDecimal"), // BigInteger representing epoch millis
        Arguments.of(BigInteger.valueOf(1622548800000L), "UTC", 1622548800000L, "BigInteger"), // AtomicLong holding epoch millis
        Arguments.of(new AtomicLong(1622548800000L), "UTC", 1622548800000L, "AtomicLong"), // Custom formatted string (format handling is delegated to the converter)
        Arguments.of("2021-06-01 00:00:00", "UTC", 1622505600000L, "custom formatted string"));
    }

    /* -------------------------------------------------------------
     *  Exception scenarios – invalid source types
     * ------------------------------------------------------------- */
    private static Stream<Arguments> invalidSourceProvider() {
        return Stream.of(Arguments.of("not-a-date-or-number", DateTimeParseException.class, new DateTimeParseException("Unable to parse", "not-a-date-or-number", 0)), Arguments.of("2021-13-01T00:00:00Z", DateTimeParseException.class, new DateTimeParseException("Invalid month", "2021-13-01T00:00:00Z", 5)), Arguments.of(Boolean.TRUE, IllegalArgumentException.class, new IllegalArgumentException("Boolean not supported")), Arguments.of(new Object(), IllegalArgumentException.class, new IllegalArgumentException("Unsupported source type")), Arguments.of(BigDecimal.valueOf(1622548800.123), ArithmeticException.class, new ArithmeticException("Fractional epoch millis not allowed")));
    }

    @Test
    void toCalendar_FromLong_UsesProvidedTimeZoneAndMillis() {
        // GIVEN
        // 2021‑07‑01T12:00:00Z
        long epochMillis = 1_622_548_800_000L;
        // a Long
        Object from = epochMillis;
        // WHEN
        Calendar result = OffsetDateTimeConversions.toCalendar(from, mockConverter);
        // THEN
        assertNotNull(result, "Resulting Calendar should not be null");
        assertEquals(TimeZone.getTimeZone("UTC"), result.getTimeZone(), "Calendar should use the time zone supplied by ConverterOptions");
        assertEquals(epochMillis, result.getTimeInMillis(), "Calendar's millis should match the converted long value");
    }

    @Test
    void toCalendar_FromDate_UsesProvidedTimeZoneAndMillis() {
        // GIVEN
        // same instant as above
        Date date = new Date(1_622_548_800_000L);
        Object from = date;
        // WHEN
        Calendar result = OffsetDateTimeConversions.toCalendar(from, mockConverter);
        // THEN
        assertNotNull(result, "Resulting Calendar should not be null");
        assertEquals(TimeZone.getTimeZone("UTC"), result.getTimeZone(), "Calendar should use the time zone supplied by ConverterOptions");
        assertEquals(date.getTime(), result.getTimeInMillis(), "Calendar's millis should match the Date's epoch millis");
    }

    @Test
    void toCalendar_WithCustomTimeZone_RespectsConverterOption() {
        // GIVEN
        TimeZone pacific = TimeZone.getTimeZone("America/Los_Angeles");
        when(mockOptions.getTimeZone()).thenReturn(pacific);
        long epochMillis = 1_622_548_800_000L;
        Object from = epochMillis;
        // WHEN
        Calendar result = OffsetDateTimeConversions.toCalendar(from, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals(pacific, result.getTimeZone(), "Calendar should reflect the custom time zone from ConverterOptions");
        assertEquals(epochMillis, result.getTimeInMillis(), "Calendar's millis should still reflect the converted value");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("validConversionsProvider")
    void toCalendar_ValidConversions(Object from, String zoneId, long expectedMillis, String description) {
        // Arrange – configure the mocked time‑zone and the conversion to long.
        when(mockOptions.getTimeZone()).thenReturn(TimeZone.getTimeZone(zoneId));
        when(mockConverter.convert(eq(from), eq(Long.class))).thenReturn(expectedMillis);
        // Act
        Calendar result = OffsetDateTimeConversions.toCalendar(from, mockConverter);
        // Assert
        assertNotNull(result, "Resulting Calendar must not be null");
        assertEquals(TimeZone.getTimeZone(zoneId), result.getTimeZone(), "Calendar should use the time‑zone supplied by ConverterOptions");
        assertEquals(expectedMillis, result.getTimeInMillis(), "Calendar's millis must match the value produced by the conversion");
    }

    /* -------------------------------------------------------------
     *  Exception scenarios – null arguments
     * ------------------------------------------------------------- */
    @Test
    void toCalendar_NullConverter_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toCalendar(1622548800000L, null), "Passing a null Converter should result in NPE");
    }

    @Test
    void toCalendar_ConverterReturnsNullOptions_ThrowsNullPointerException() {
        when(mockConverter.getOptions()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toCalendar(1622548800000L, mockConverter), "Null ConverterOptions should cause NPE");
    }

    @Test
    void toCalendar_OptionsReturnNullTimeZone_ThrowsNullPointerException() {
        when(mockOptions.getTimeZone()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toCalendar(1622548800000L, mockConverter), "Null TimeZone in options should cause NPE");
    }

    @Test
    void toCalendar_NullSource_ThrowsNullPointerException() {
        // The conversion layer will attempt to convert null to Long and will throw NPE.
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toCalendar(null, mockConverter), "Null source object should result in NPE");
    }

    @ParameterizedTest(name = "[{index}] Invalid source: {0}")
    @MethodSource("invalidSourceProvider")
    void toCalendar_InvalidSourceTypes(Object from, Class<? extends Throwable> expectedType, RuntimeException toThrow) {
        // Stub the converter to throw the desired exception when converting this source.
        when(mockConverter.convert(eq(from), eq(Long.class))).thenThrow(toThrow);
        assertThrows(expectedType, () -> OffsetDateTimeConversions.toCalendar(from, mockConverter), "Conversion of an invalid source should propagate the expected exception");
    }

    /* -------------------------------------------------------------
     *  Exception scenarios – invalid converter configuration
     * ------------------------------------------------------------- */
    @Test
    void toCalendar_CustomFormatMissing_ThrowsIllegalArgumentException() {
        // Simulate a missing date format – the conversion layer throws IllegalArgumentException.
        String source = "2021-06-01 00:00:00";
        when(mockConverter.convert(eq(source), eq(Long.class))).thenThrow(new IllegalArgumentException("Date format is null"));
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toCalendar(source, mockConverter), "Missing custom date format should cause IllegalArgumentException");
    }

    @Test
    void toCalendar_UnsupportedLocale_ThrowsIllegalArgumentException() {
        // Simulate an unsupported locale configuration.
        String source = "2021-06-01T00:00:00Z";
        when(mockConverter.convert(eq(source), eq(Long.class))).thenThrow(new IllegalArgumentException("Unsupported locale"));
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toCalendar(source, mockConverter), "Unsupported locale should cause IllegalArgumentException");
    }
}
