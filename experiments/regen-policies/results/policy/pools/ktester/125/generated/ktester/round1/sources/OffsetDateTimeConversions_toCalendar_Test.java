package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Calendar;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigInteger;
import java.time.*;
import java.util.Date;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toCalendar_Test {

    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    @BeforeEach
    void setupBeforeEach() {
        // Default mock behaviour – can be overridden in individual tests
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getTimeZone()).thenReturn(TimeZone.getTimeZone("UTC"));
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Long value – already covered by a dedicated test, but kept here for completeness
        Arguments.of("Long milliseconds → Calendar (UTC)", 123_456_789L, "UTC", 123_456_789L), // ISO‑8601 string with a different zone (America/New_York)
        Arguments.of("ISO‑8601 String → Calendar (America/New_York)", "2023-01-01T00:00:00Z", "America/New_York", Instant.parse("2023-01-01T00:00:00Z").toEpochMilli()), // java.util.Date instance
        Arguments.of("Date instance → Calendar (Europe/Paris)", // 2020‑12‑31T23:00:00Z
        new Date(1_609_459_200_000L), "Europe/Paris", 1_609_459_200_000L), // OffsetDateTime with offset +02:00, converter zone UTC – epoch millis must be offset‑aware
        Arguments.of("OffsetDateTime → Calendar (UTC)", OffsetDateTime.parse("2022-12-31T23:00:00+02:00"), "UTC", OffsetDateTime.parse("2022-12-31T23:00:00+02:00").toInstant().toEpochMilli()));
    }

    @Test
    void toCalendar_withLongInput_returnsCalendarWithCorrectTimeAndZone() {
        // GIVEN
        // 2021‑06‑01T12:00:00Z
        long epochMillis = 1_622_548_800_000L;
        // WHEN
        Calendar result = OffsetDateTimeConversions.toCalendar(epochMillis, mockConverter);
        // THEN
        assertNotNull(result, "Resulting Calendar should not be null");
        assertEquals(TimeZone.getTimeZone("UTC"), result.getTimeZone(), "Calendar should use the time zone supplied by ConverterOptions");
        assertEquals(epochMillis, result.getTimeInMillis(), "Calendar time in milliseconds should match the converted long value");
    }

    @Test
    void toCalendar_withNullInput_returnsCalendarAtEpochZero() {
        // GIVEN
        // mockConverter already returns UTC time zone from its options
        // WHEN
        Calendar result = OffsetDateTimeConversions.toCalendar(null, mockConverter);
        // THEN
        assertNotNull(result, "Resulting Calendar should not be null even when input is null");
        assertEquals(TimeZone.getTimeZone("UTC"), result.getTimeZone(), "Calendar should still use the time zone supplied by ConverterOptions");
        assertEquals(0L, result.getTimeInMillis(), "Null input should be converted to 0 milliseconds (epoch start)");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionProvider")
    void toCalendar_validConversions(String description, Object source, String tzId, long expectedMillis) {
        // Arrange – configure the converter to return the requested time‑zone
        TimeZone tz = TimeZone.getTimeZone(tzId);
        when(mockOptions.getTimeZone()).thenReturn(tz);
        // Act
        Calendar cal = OffsetDateTimeConversions.toCalendar(source, mockConverter);
        // Assert
        assertNotNull(cal, "Calendar must not be null");
        assertEquals(tz, cal.getTimeZone(), "Calendar should use the converter's time zone");
        assertEquals(expectedMillis, cal.getTimeInMillis(), "Calendar timeInMillis does not match expected value");
    }

    @Test
    void toCalendar_nullConverter_throwsNullPointerException() {
        // GIVEN
        Long source = 123_456_789L;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toCalendar(source, null), "Calling toCalendar with a null Converter should throw NPE");
    }

    @Test
    void toCalendar_converterWithNullOptions_throwsNullPointerException() {
        // GIVEN
        when(mockConverter.getOptions()).thenReturn(null);
        Long source = 123_456_789L;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toCalendar(source, mockConverter), "Converter returning null Options should cause NPE");
    }

    @Test
    void toCalendar_optionsWithNullTimeZone_throwsNullPointerException() {
        // GIVEN
        when(mockOptions.getTimeZone()).thenReturn(null);
        Long source = 123_456_789L;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toCalendar(source, mockConverter), "Null TimeZone supplied to Calendar.getInstance should cause NPE");
    }

    @Test
    void toCalendar_unsupportedSourceType_throwsIllegalArgumentException() {
        // GIVEN
        Object unsupported = new Object();
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toCalendar(unsupported, mockConverter), "Unsupported source type should result in IllegalArgumentException");
    }

    @Test
    void toCalendar_unparsableString_throwsNumberFormatException() {
        // GIVEN
        String badString = "not-a-date";
        // WHEN / THEN
        assertThrows(NumberFormatException.class, () -> OffsetDateTimeConversions.toCalendar(badString, mockConverter), "Unparsable string should cause NumberFormatException");
    }

    @Test
    void toCalendar_bigIntegerOverflow_throwsIllegalArgumentException() {
        // GIVEN
        // Long.MAX_VALUE + 1
        BigInteger overflow = new BigInteger("9223372036854775808");
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toCalendar(overflow, mockConverter), "BigInteger larger than Long.MAX_VALUE should cause IllegalArgumentException");
    }

    @Test
    void toCalendar_floatingPointString_throwsNumberFormatException() {
        // GIVEN
        String floating = "123.45";
        // WHEN / THEN
        assertThrows(NumberFormatException.class, () -> OffsetDateTimeConversions.toCalendar(floating, mockConverter), "Floating‑point string cannot be parsed as long epoch millis");
    }

    @Test
    void toCalendar_instantBeyondLongRange_throwsArithmeticException() {
        // GIVEN
        // far beyond Long.MAX_VALUE milliseconds
        Instant tooBig = Instant.MAX;
        // WHEN / THEN
        assertThrows(ArithmeticException.class, () -> OffsetDateTimeConversions.toCalendar(tooBig, mockConverter), "Instant whose epoch milli exceeds Long range should cause ArithmeticException");
    }
}
