package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Calendar;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class StringConversions_toCalendar_Test {

    /**
     * When {@code StringConversions.toZonedDateTime(..)} returns {@code null},
     * {@code toCalendar(..)} must also return {@code null}.
     */
    @Test
    void toCalendar_returnsNullWhenZonedDateTimeIsNull() {
        // Arrange
        Converter mockConverter = mock(Converter.class);
        // Mock the static call toStringConversions.toZonedDateTime to return null
        try (MockedStatic<StringConversions> mocked = Mockito.mockStatic(StringConversions.class)) {
            mocked.when(() -> StringConversions.toZonedDateTime(any(), eq(mockConverter))).thenReturn(null);
            // Act
            Calendar result = StringConversions.toCalendar("2023-01-01T00:00Z", mockConverter);
            // Assert
            assertNull(result, "Expected null Calendar when ZonedDateTime conversion yields null");
        }
    }

    /**
     * Verify that a proper {@link Calendar} is produced when a valid {@link ZonedDateTime}
     * is returned from {@code toZonedDateTime(..)} and the converter's zone id is applied.
     */
    @Test
    void toCalendar_convertsZonedDateTimeWithUserZone() {
        // Arrange
        // Input string – the actual value is irrelevant because we mock the conversion.
        String input = "2023-01-01T12:00:00Z";
        // Create a mock Converter and its options
        Converter mockConverter = mock(Converter.class);
        ConverterOptions mockOptions = mock(ConverterOptions.class);
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        // User‑requested zone: America/New_York (UTC‑5 in winter)
        ZoneId userZone = ZoneId.of("America/New_York");
        when(mockOptions.getZoneId()).thenReturn(userZone);
        // The ZonedDateTime that the static method will return (UTC)
        ZonedDateTime utcZdt = ZonedDateTime.of(2023, 1, 1, 12, 0, 0, 0, ZoneId.of("UTC"));
        try (MockedStatic<StringConversions> mocked = Mockito.mockStatic(StringConversions.class)) {
            mocked.when(() -> StringConversions.toZonedDateTime(eq(input), eq(mockConverter))).thenReturn(utcZdt);
            // Act
            Calendar calendar = StringConversions.toCalendar(input, mockConverter);
            // Assert
            assertNotNull(calendar, "Calendar should not be null when conversion succeeds");
            // The calendar must be in the user‑specified zone
            TimeZone expectedTz = TimeZone.getTimeZone(userZone);
            assertEquals(expectedTz.getID(), calendar.getTimeZone().getID(), "Calendar time zone should match the converter's zone id");
            // The instant (epoch milli) must be unchanged – only the zone representation differs
            long expectedEpochMilli = utcZdt.toInstant().toEpochMilli();
            assertEquals(expectedEpochMilli, calendar.getTimeInMillis(), "Calendar epoch milliseconds should match the original ZonedDateTime instant");
        }
    }

    /**
     * Same Zone Conversion – the input ZonedDateTime already uses the converter's zone.
     * The resulting Calendar should keep the same instant and zone id.
     */
    @Test
    void toCalendar_sameZoneConversion() {
        // Arrange
        String isoString = "2023-03-25T10:15:30+02:00[Europe/Paris]";
        // zone = Europe/Paris
        ZonedDateTime parsed = ZonedDateTime.parse(isoString);
        Converter mockConverter = mock(Converter.class);
        ConverterOptions mockOptions = mock(ConverterOptions.class);
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        ZoneId userZone = ZoneId.of("Europe/Paris");
        when(mockOptions.getZoneId()).thenReturn(userZone);
        try (MockedStatic<StringConversions> mocked = mockStatic(StringConversions.class)) {
            mocked.when(() -> StringConversions.toZonedDateTime(eq(isoString), eq(mockConverter))).thenReturn(parsed);
            // Act
            Calendar calendar = StringConversions.toCalendar(isoString, mockConverter);
            // Assert
            assertNotNull(calendar, "Calendar must not be null");
            assertEquals(TimeZone.getTimeZone(userZone).getID(), calendar.getTimeZone().getID(), "Calendar time zone should be Europe/Paris");
            long expectedMillis = Instant.parse("2023-03-25T08:15:30Z").toEpochMilli();
            assertEquals(expectedMillis, calendar.getTimeInMillis(), "Epoch milliseconds must match the instant represented by the input string");
        }
    }

    /**
     * Different Zone Conversion – the input zone differs from the converter's zone.
     * The Calendar must retain the same instant but be created with the converter's zone.
     */
    @Test
    void toCalendar_differentZoneConversion() {
        // Arrange
        String isoString = "2023-03-25T10:15:30+02:00[Europe/Paris]";
        // 2023-03-25T10:15:30+02:00[Europe/Paris]
        ZonedDateTime parsed = ZonedDateTime.parse(isoString);
        Converter mockConverter = mock(Converter.class);
        ConverterOptions mockOptions = mock(ConverterOptions.class);
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        ZoneId userZone = ZoneId.of("America/New_York");
        when(mockOptions.getZoneId()).thenReturn(userZone);
        try (MockedStatic<StringConversions> mocked = mockStatic(StringConversions.class)) {
            mocked.when(() -> StringConversions.toZonedDateTime(eq(isoString), eq(mockConverter))).thenReturn(parsed);
            // Act
            Calendar calendar = StringConversions.toCalendar(isoString, mockConverter);
            // Assert
            assertNotNull(calendar, "Calendar must not be null");
            assertEquals(TimeZone.getTimeZone(userZone).getID(), calendar.getTimeZone().getID(), "Calendar time zone should be America/New_York");
            long expectedMillis = Instant.parse("2023-03-25T08:15:30Z").toEpochMilli();
            assertEquals(expectedMillis, calendar.getTimeInMillis(), "Epoch milliseconds must stay unchanged");
            // Verify the local date‑time seen in the new zone (should be 04:15:30)
            assertEquals(2023, calendar.get(Calendar.YEAR));
            assertEquals(Calendar.MARCH, calendar.get(Calendar.MONTH));
            assertEquals(25, calendar.get(Calendar.DAY_OF_MONTH));
            assertEquals(4, calendar.get(Calendar.HOUR_OF_DAY));
            assertEquals(15, calendar.get(Calendar.MINUTE));
            assertEquals(30, calendar.get(Calendar.SECOND));
        }
    }

    /**
     * Null Input String – the method casts {@code from} to {@code String} without a null‑check.
     * Passing {@code null} should raise a {@link NullPointerException}.
     */
    @Test
    void toCalendar_nullInputString_throwsNPE() {
        // Arrange
        Converter mockConverter = mock(Converter.class);
        // No need to mock static method because the NPE occurs before the call.
        // Act & Assert
        assertThrows(NullPointerException.class, () -> StringConversions.toCalendar(null, mockConverter), "Expected NullPointerException when 'from' is null");
    }

    /**
     * Invalid Converter Options – when {@code converter.getOptions()} returns {@code null},
     * a {@link NullPointerException} is expected while adjusting the ZonedDateTime.
     */
    @Test
    void toCalendar_converterWithNullOptions_throwsNPE() {
        // Arrange
        String input = "2023-03-25T10:15:30+02:00[Europe/Paris]";
        ZonedDateTime parsed = ZonedDateTime.parse(input);
        Converter mockConverter = mock(Converter.class);
        // simulate bad options
        when(mockConverter.getOptions()).thenReturn(null);
        try (MockedStatic<StringConversions> mocked = mockStatic(StringConversions.class)) {
            mocked.when(() -> StringConversions.toZonedDateTime(eq(input), eq(mockConverter))).thenReturn(parsed);
            // Act & Assert
            assertThrows(NullPointerException.class, () -> StringConversions.toCalendar(input, mockConverter), "Expected NullPointerException when converter options are null");
        }
    }
}
