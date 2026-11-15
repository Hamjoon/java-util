package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toZonedDateTime_Test {

    /**
     * Mocked {@link Converter} used in all tests.
     */
    @Mock
    Converter mockConverter;

    /**
     * Mocked options returned by {@link Converter#getOptions()}.
     */
    @Mock
    ConverterOptions mockOptions;

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mock hierarchy so that converter.getOptions().getZoneId() returns UTC.
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks after each test to avoid cross‑test interference.
        reset(mockConverter, mockOptions);
    }

    @Test
    void toZonedDateTime_ParsesIsoStringWithoutZone_UsesConverterZone() {
        // GIVEN
        String isoWithoutZone = "2023-01-01T12:00:00";
        // WHEN
        ZonedDateTime result = StringConversions.toZonedDateTime(isoWithoutZone, mockConverter);
        // THEN
        ZonedDateTime expected = ZonedDateTime.of(2023, 1, 1, 12, 0, 0, 0, ZoneId.of("UTC"));
        assertEquals(expected, result, "The parsed ZonedDateTime should use the Converter's ZoneId when the input lacks a zone.");
    }

    @Test
    void toZonedDateTime_ParsesIsoStringWithOffset_IgnoresConverterZone() {
        // GIVEN
        String isoWithOffset = "2023-01-01T12:00:00+02:00";
        // WHEN
        ZonedDateTime result = StringConversions.toZonedDateTime(isoWithOffset, mockConverter);
        // THEN
        ZonedDateTime expected = ZonedDateTime.parse(isoWithOffset);
        assertEquals(expected, result, "The parsed ZonedDateTime should respect the offset present in the input string.");
    }

    @Test
    void toZonedDateTime_NullInput_ThrowsNullPointerException() {
        // GIVEN
        Object nullInput = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> StringConversions.toZonedDateTime(nullInput, mockConverter), "Passing null should result in a NullPointerException because the method casts to String.");
    }

    @Test
    void toZonedDateTime_NonStringInput_ThrowsClassCastException() {
        // GIVEN
        // Integer, not a String
        Object nonString = 12345;
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> StringConversions.toZonedDateTime(nonString, mockConverter), "Passing a non‑String object should cause a ClassCastException due to the cast inside the method.");
    }

    @Test
    void toZonedDateTime_ValidIsoStringWithExplicitUtcZone() {
        // GIVEN
        String isoUtc = "2023-08-15T12:34:56Z";
        // Converter zone is UTC (default from @BeforeEach)
        // WHEN
        ZonedDateTime result = StringConversions.toZonedDateTime(isoUtc, mockConverter);
        // THEN
        ZonedDateTime expected = ZonedDateTime.of(2023, 8, 15, 12, 34, 56, 0, ZoneOffset.UTC);
        assertEquals(expected, result, "Explicit UTC offset in the string must be honoured regardless of the converter's zone.");
    }

    @Test
    void toZonedDateTime_ValidIsoStringWithOffset_IgnoresConverterZone() {
        // GIVEN
        String isoWithOffset = "2023-08-15T12:34:56+02:00";
        // different zone
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("America/New_York"));
        // WHEN
        ZonedDateTime result = StringConversions.toZonedDateTime(isoWithOffset, mockConverter);
        // THEN
        ZonedDateTime expected = ZonedDateTime.of(2023, 8, 15, 12, 34, 56, 0, ZoneOffset.ofHours(2));
        assertEquals(expected, result, "The offset present in the input string must override the converter's zone.");
    }

    @Test
    void toZonedDateTime_ValidIsoStringWithoutZone_UsesConverterZone() {
        // GIVEN
        String isoWithoutZone = "2023-08-15T12:34:56";
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("Europe/Paris"));
        // WHEN
        ZonedDateTime result = StringConversions.toZonedDateTime(isoWithoutZone, mockConverter);
        // THEN
        ZonedDateTime expected = ZonedDateTime.of(2023, 8, 15, 12, 34, 56, 0, ZoneId.of("Europe/Paris"));
        assertEquals(expected, result, "When the input lacks a zone, the converter's ZoneId must be applied.");
    }

    @Test
    void toZonedDateTime_ValidIsoStringWithZoneIdAndOffset_IgnoresConverterZone() {
        // GIVEN
        // String contains both offset and explicit zone ID.
        String isoWithZoneAndOffset = "2023-08-15T12:34:56+02:00[Europe/Paris]";
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("America/New_York"));
        // WHEN
        ZonedDateTime result = StringConversions.toZonedDateTime(isoWithZoneAndOffset, mockConverter);
        // THEN
        ZonedDateTime expected = ZonedDateTime.of(2023, 8, 15, 12, 34, 56, 0, ZoneId.of("Europe/Paris"));
        assertEquals(expected, result, "Explicit zone ID in the string must be honoured; the converter's zone is ignored.");
    }

    @Test
    void toZonedDateTime_ValidDateOnlyString_UsesConverterZone() {
        // GIVEN
        String dateOnly = "2023-08-15";
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("Europe/Paris"));
        // WHEN
        ZonedDateTime result = StringConversions.toZonedDateTime(dateOnly, mockConverter);
        // THEN
        ZonedDateTime expected = ZonedDateTime.of(2023, 8, 15, 0, 0, 0, 0, ZoneId.of("Europe/Paris"));
        assertEquals(expected, result, "Date‑only strings should default time to 00:00 and use the converter's zone.");
    }

    @Test
    void toZonedDateTime_ValidIsoStringWithFractionalSeconds_IgnoresConverterZone() {
        // GIVEN
        String isoFractional = "2023-08-15T12:34:56.789Z";
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("America/Los_Angeles"));
        // WHEN
        ZonedDateTime result = StringConversions.toZonedDateTime(isoFractional, mockConverter);
        // THEN
        ZonedDateTime expected = ZonedDateTime.of(2023, 8, 15, 12, 34, 56, 789_000_000, ZoneOffset.UTC);
        assertEquals(expected, result, "Nanosecond precision and explicit UTC offset must be honoured.");
    }

    @Test
    void toZonedDateTime_NumericEpochMillisString_UsesConverterZone() {
        // GIVEN
        // corresponds to 2021-08-15T12:00:00Z
        String epochMillis = "1629038400000";
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
        // WHEN
        ZonedDateTime result = StringConversions.toZonedDateTime(epochMillis, mockConverter);
        // THEN
        ZonedDateTime expected = ZonedDateTime.ofInstant(Instant.ofEpochMilli(1629038400000L), ZoneId.of("UTC"));
        assertEquals(expected, result, "Numeric epoch‑millis strings should be interpreted as an instant and then shifted to the converter's zone.");
    }

    @Test
    void toZonedDateTime_ValidIsoStringWithZeroOffset_IgnoresConverterZone() {
        // GIVEN
        String isoZeroOffset = "2023-08-15T12:34:56+00:00";
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("Asia/Tokyo"));
        // WHEN
        ZonedDateTime result = StringConversions.toZonedDateTime(isoZeroOffset, mockConverter);
        // THEN
        ZonedDateTime expected = ZonedDateTime.of(2023, 8, 15, 12, 34, 56, 0, ZoneOffset.UTC);
        assertEquals(expected, result, "Explicit +00:00 offset must be respected, ignoring the converter's zone.");
    }

    @Test
    void toZonedDateTime_StringWithWhitespace_ParsesAfterTrim() {
        // GIVEN
        String withSpace = " 2023-08-15T12:34:56Z ";
        // Converter zone is UTC (default)
        // WHEN
        ZonedDateTime result = StringConversions.toZonedDateTime(withSpace, mockConverter);
        // THEN
        ZonedDateTime expected = ZonedDateTime.of(2023, 8, 15, 12, 34, 56, 0, ZoneOffset.UTC);
        assertEquals(expected, result, "Leading/trailing whitespace should be ignored by the parser.");
    }

    @Test
    void toZonedDateTime_ConverterWithNullZone_UsesSystemDefault() {
        // GIVEN
        String isoWithoutZone = "2023-08-15T12:34:56";
        // simulate null zone
        when(mockOptions.getZoneId()).thenReturn(null);
        // WHEN
        ZonedDateTime result = StringConversions.toZonedDateTime(isoWithoutZone, mockConverter);
        // THEN
        ZoneId systemDefault = ZoneId.systemDefault();
        ZonedDateTime expected = ZonedDateTime.of(2023, 8, 15, 12, 34, 56, 0, systemDefault);
        assertEquals(expected, result, "When the converter's ZoneId is null, the system default zone must be used.");
    }

    @Test
    void toZonedDateTime_InvalidFormat_ThrowsDateTimeParseException() {
        // GIVEN
        String invalid = "not-a-date";
        // WHEN / THEN
        assertThrows(DateTimeParseException.class, () -> StringConversions.toZonedDateTime(invalid, mockConverter), "A non‑date string must cause DateTimeParseException.");
    }

    @Test
    void toZonedDateTime_NullStringInput_ThrowsNullPointerException() {
        // GIVEN
        Object nullInput = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> StringConversions.toZonedDateTime(nullInput, mockConverter), "Passing null should result in NullPointerException because of the cast to String.");
    }

    @Test
    void toZonedDateTime_EmptyStringInput_ThrowsDateTimeParseException() {
        // GIVEN
        String empty = "";
        // WHEN / THEN
        assertThrows(DateTimeParseException.class, () -> StringConversions.toZonedDateTime(empty, mockConverter), "An empty string cannot be parsed and must throw DateTimeParseException.");
    }

    @Test
    void toZonedDateTime_StringContainingOnlyZoneId_ThrowsDateTimeParseException() {
        // GIVEN
        String onlyZone = "Europe/Paris";
        // WHEN / THEN
        assertThrows(DateTimeParseException.class, () -> StringConversions.toZonedDateTime(onlyZone, mockConverter), "A bare zone ID is not a valid date‑time representation and must cause parsing failure.");
    }

    @Test
    void toZonedDateTime_LeapSecondRepresentation_ThrowsDateTimeParseException() {
        // GIVEN
        String leapSecond = "2023-06-30T23:59:60Z";
        // WHEN / THEN
        assertThrows(DateTimeParseException.class, () -> StringConversions.toZonedDateTime(leapSecond, mockConverter), "Leap‑second representation is invalid and must trigger DateTimeParseException.");
    }

    @Test
    void toZonedDateTime_NullConverter_ThrowsNullPointerException() {
        // GIVEN
        String isoUtc = "2023-08-15T12:34:56Z";
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> StringConversions.toZonedDateTime(isoUtc, null), "Passing a null converter must raise NullPointerException before parsing.");
    }
}
