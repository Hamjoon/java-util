package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.Calendar;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.format.DateTimeParseException;

@ExtendWith(MockitoExtension.class)
class StringConversions_toCalendar_Test {

    @Mock
    private Converter converter;

    @Mock
    private ConverterOptions options;

    /**
     * Common stubbing for every test: the converter must return its options,
     * and the options must return a known ZoneId.
     */
    @BeforeEach
    void setupBeforeEach() {
        when(converter.getOptions()).thenReturn(options);
        // Use a non‑UTC zone to verify the conversion to the user zone works.
        when(options.getZoneId()).thenReturn(ZoneId.of("America/New_York"));
    }

    @Test
    void toCalendar_whenZonedDateTimeIsNull_returnsNull() {
        try (MockedStatic<StringConversions> mocked = mockStatic(StringConversions.class)) {
            // Arrange: toZonedDateTime returns null for the given input.
            mocked.when(() -> StringConversions.toZonedDateTime("ignored", converter)).thenReturn(null);
            // Act
            Calendar result = StringConversions.toCalendar("ignored", converter);
            // Assert
            assertNull(result, "Expected null Calendar when ZonedDateTime conversion yields null");
        }
    }

    @Test
    void toCalendar_whenZonedDateTimeIsValid_returnsCalendarInUserZone() {
        // Input string – the actual value is irrelevant because we mock the static call.
        String input = "2022-05-10T15:30:00Z";
        // The ZonedDateTime that the mocked toZonedDateTime will return.
        ZonedDateTime sourceZdt = ZonedDateTime.of(2022, 5, 10, 15, 30, 0, 0, ZoneOffset.UTC);
        // Expected ZonedDateTime after applying the user's zone (America/New_York).
        ZoneId userZone = ZoneId.of("America/New_York");
        ZonedDateTime expectedZdtUser = sourceZdt.withZoneSameInstant(userZone);
        try (MockedStatic<StringConversions> mocked = mockStatic(StringConversions.class)) {
            // Stub the static helper.
            mocked.when(() -> StringConversions.toZonedDateTime(input, converter)).thenReturn(sourceZdt);
            // Act
            Calendar calendar = StringConversions.toCalendar(input, converter);
            // Verify the result is not null.
            assertNotNull(calendar, "Calendar should not be null for a valid conversion");
            // The Calendar should be created with the user's time‑zone.
            TimeZone expectedTz = TimeZone.getTimeZone(userZone);
            assertEquals(expectedTz.getID(), calendar.getTimeZone().getID(), "Calendar time‑zone must match the Converter's zone");
            // The instant (epoch milli) must match the expected ZonedDateTime.
            long expectedMillis = expectedZdtUser.toInstant().toEpochMilli();
            assertEquals(expectedMillis, calendar.getTimeInMillis(), "Calendar instant must represent the same moment as the ZonedDateTime in the user zone");
        }
    }

    @Test
    void toCalendar_validIsoInstantWithUtcZone_returnsCorrectCalendar() {
        // --------------------------------------------------------------
        // Scenario: a proper ISO‑8601 instant string ending with 'Z'
        // is parsed and the converter’s target zone is UTC.
        // --------------------------------------------------------------
        String isoString = "2023-03-25T10:15:30Z";
        // Expected epoch milliseconds for the exact instant.
        long expectedMillis = 1679739330000L;
        ZoneId utc = ZoneId.of("UTC");
        // Stub the options to return UTC.
        when(options.getZoneId()).thenReturn(utc);
        // Mock the static conversion to return a ZonedDateTime in UTC.
        ZonedDateTime sourceZdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(expectedMillis), ZoneOffset.UTC);
        try (MockedStatic<StringConversions> mocked = mockStatic(StringConversions.class)) {
            mocked.when(() -> StringConversions.toZonedDateTime(isoString, converter)).thenReturn(sourceZdt);
            Calendar result = StringConversions.toCalendar(isoString, converter);
            assertNotNull(result, "Resulting Calendar should not be null");
            assertEquals(TimeZone.getTimeZone(utc).getID(), result.getTimeZone().getID(), "Calendar must use UTC time‑zone");
            assertEquals(expectedMillis, result.getTimeInMillis(), "Calendar epoch milliseconds must match the expected instant");
        }
    }

    @Test
    void toCalendar_nonStringFrom_throwsClassCastException() {
        // --------------------------------------------------------------
        // Scenario: the 'from' argument is not a String (e.g., Integer).
        // The method casts without checking, so a ClassCastException is expected.
        // --------------------------------------------------------------
        Integer nonString = 12345;
        // No need to mock static methods – the cast fails before they are invoked.
        assertThrows(ClassCastException.class, () -> StringConversions.toCalendar(nonString, converter), "Passing a non‑String 'from' should raise ClassCastException");
    }

    @Test
    void toCalendar_nullConverter_throwsNullPointerException() {
        // --------------------------------------------------------------
        // Scenario: the Converter argument itself is null.
        // The method dereferences it immediately, causing NPE.
        // --------------------------------------------------------------
        String input = "2023-01-01T00:00:00Z";
        assertThrows(NullPointerException.class, () -> StringConversions.toCalendar(input, null), "Null Converter should cause NullPointerException");
    }

    @Test
    void toCalendar_converterWithNullOptions_throwsNullPointerException() {
        // --------------------------------------------------------------
        // Scenario: converter.getOptions() returns null.
        // Subsequent call to getZoneId() triggers NPE.
        // --------------------------------------------------------------
        when(converter.getOptions()).thenReturn(null);
        String input = "2023-01-01T00:00:00Z";
        assertThrows(NullPointerException.class, () -> StringConversions.toCalendar(input, converter), "Null options from Converter should cause NullPointerException");
    }

    @Test
    void toCalendar_converterWithNullZoneId_throwsNullPointerException() {
        // --------------------------------------------------------------
        // Scenario: options.getZoneId() returns null.
        // ZonedDateTime.withZoneSameInstant(null) throws NPE.
        // --------------------------------------------------------------
        when(options.getZoneId()).thenReturn(null);
        String input = "2023-01-01T00:00:00Z";
        // Mock the static helper to return a valid ZonedDateTime so the
        // failure occurs at withZoneSameInstant(null).
        ZonedDateTime anyZdt = ZonedDateTime.now(ZoneOffset.UTC);
        try (MockedStatic<StringConversions> mocked = mockStatic(StringConversions.class)) {
            mocked.when(() -> StringConversions.toZonedDateTime(input, converter)).thenReturn(anyZdt);
            assertThrows(NullPointerException.class, () -> StringConversions.toCalendar(input, converter), "Null ZoneId should cause NullPointerException");
        }
    }

    @Test
    void toCalendar_toZonedDateTimeThrowsDateTimeParseException_propagates() {
        // --------------------------------------------------------------
        // Scenario: the delegated toZonedDateTime throws DateTimeParseException
        // (e.g., due to an invalid ISO string). The exception should propagate.
        // --------------------------------------------------------------
        String badInput = "not-a-date";
        try (MockedStatic<StringConversions> mocked = mockStatic(StringConversions.class)) {
            mocked.when(() -> StringConversions.toZonedDateTime(badInput, converter)).thenThrow(new DateTimeParseException("Invalid format", badInput, 0));
            assertThrows(DateTimeParseException.class, () -> StringConversions.toCalendar(badInput, converter), "DateTimeParseException should propagate from toZonedDateTime");
        }
    }
}
