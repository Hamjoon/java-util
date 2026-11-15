package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Date;
import java.time.ZoneOffset;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.ZoneId;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toSqlDate_Test {

    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    @BeforeEach
    void setupBeforeEach() {
        // The method under test uses converter.getOptions().getZoneId()
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        // Use UTC to make the expected results deterministic regardless of the host timezone
        when(mockOptions.getZoneId()).thenReturn(ZoneOffset.UTC);
    }

    private static Stream<Arguments> zoneShiftProvider() {
        return Stream.of(Arguments.of("UTC → Los Angeles (UTC‑08) shift crossing midnight", "2023-07-15T00:30:00Z", ZoneId.of("America/Los_Angeles"), "2023-07-14"), Arguments.of("UTC → New York (UTC‑05) – same calendar day", "2023-12-31T23:00:00Z", ZoneId.of("America/New_York"), "2023-12-31"), Arguments.of("UTC → Los Angeles (UTC‑08) – stays same day", "2023-12-31T23:30:00Z", ZoneId.of("America/Los_Angeles"), "2023-12-31"), Arguments.of("UTC → Los Angeles (UTC‑08) – previous year", "2023-01-01T02:00:00Z", ZoneId.of("America/Los_Angeles"), "2022-12-31"));
    }

    private static Stream<Arguments> invalidSimpleDateProvider() {
        return Stream.of(Arguments.of("Invalid day (Feb 30)", "2023-02-30"), Arguments.of("Invalid month (13)", "2023-13-01"));
    }

    @Test
    void simpleDateIsConvertedCorrectly() {
        String input = "2023-08-15";
        Date expected = Date.valueOf("2023-08-15");
        Date actual = StringConversions.toSqlDate(input, mockConverter);
        assertEquals(expected, actual);
    }

    @Test
    void isoInstantWithZIsConvertedToCorrectDate() {
        // Instant at UTC midnight – the date part should be 2023‑08‑15
        String input = "2023-08-15T00:00:00Z";
        Date expected = Date.valueOf("2023-08-15");
        Date actual = StringConversions.toSqlDate(input, mockConverter);
        assertEquals(expected, actual);
    }

    @Test
    void isoZonedDateTimeWithoutZIsConvertedToCorrectDate() {
        // 2023‑08‑15T23:00:00+02:00  -> instant 2023‑08‑15T21:00:00Z
        // With UTC zone the local date is still 2023‑08‑15
        String input = "2023-08-15T23:00:00+02:00";
        Date expected = Date.valueOf("2023-08-15");
        Date actual = StringConversions.toSqlDate(input, mockConverter);
        assertEquals(expected, actual);
    }

    @Test
    void nonIsoFormatIsHandledByDateUtilities() {
        // DateUtilities can parse many common patterns; "2023/08/15" is one of them.
        String input = "2023/08/15";
        Date expected = Date.valueOf("2023-08-15");
        Date actual = StringConversions.toSqlDate(input, mockConverter);
        assertEquals(expected, actual);
    }

    @Test
    void unparsableStringReturnsNull() {
        String input = "not-a-date-at-all";
        Date actual = StringConversions.toSqlDate(input, mockConverter);
        assertNull(actual);
    }

    @Test
    void whitespaceIsTrimmedBeforeParsing() {
        String input = "   2023-07-15   ";
        Date expected = Date.valueOf("2023-07-15");
        Date actual = StringConversions.toSqlDate(input, mockConverter);
        assertEquals(expected, actual);
    }

    @Test
    void simpleDateWithTIsNotTakenByFastPath() {
        // Contains a 'T', so the fast‑path is bypassed.
        String input = "2023-07-15T00:00:00";
        Date expected = Date.valueOf("2023-07-15");
        Date actual = StringConversions.toSqlDate(input, mockConverter);
        assertEquals(expected, actual);
    }

    @Test
    void isoWithoutZIsConvertedToCorrectDate() {
        // 2023‑07‑15T08:20:30+02:00  -> instant 2023‑07‑15T06:20:30Z
        // With UTC zone the local date stays 2023‑07‑15.
        String input = "2023-07-15T08:20:30+02:00";
        Date expected = Date.valueOf("2023-07-15");
        Date actual = StringConversions.toSqlDate(input, mockConverter);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("zoneShiftProvider")
    void isoWithOffsetAndZoneShift(String description, String input, ZoneId targetZone, String expectedDate) {
        // Build a converter that returns the required zone.
        Converter converter = mock(Converter.class);
        ConverterOptions options = mock(ConverterOptions.class);
        when(converter.getOptions()).thenReturn(options);
        when(options.getZoneId()).thenReturn(targetZone);
        Date expected = Date.valueOf(expectedDate);
        Date actual = StringConversions.toSqlDate(input, converter);
        assertEquals(expected, actual, description);
    }

    @Test
    void nonStringInputThrowsClassCastException() {
        Integer notAString = 12345;
        assertThrows(ClassCastException.class, () -> StringConversions.toSqlDate(notAString, mockConverter));
    }

    @Test
    void nullFromInputThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> StringConversions.toSqlDate(null, mockConverter));
    }

    @Test
    void nullConverterThrowsNullPointerException() {
        String input = "2023-07-15";
        assertThrows(NullPointerException.class, () -> StringConversions.toSqlDate(input, null));
    }

    @Test
    void nullConverterOptionsThrowsNullPointerException() {
        // Converter returns null options.
        Converter converter = mock(Converter.class);
        when(converter.getOptions()).thenReturn(null);
        String input = "2023-07-15";
        assertThrows(NullPointerException.class, () -> StringConversions.toSqlDate(input, converter));
    }

    @Test
    void nullConverterZoneIdThrowsNullPointerException() {
        // Options exist but return null zone id.
        Converter converter = mock(Converter.class);
        ConverterOptions options = mock(ConverterOptions.class);
        when(converter.getOptions()).thenReturn(options);
        when(options.getZoneId()).thenReturn(null);
        String input = "2023-07-15";
        assertThrows(NullPointerException.class, () -> StringConversions.toSqlDate(input, converter));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidSimpleDateProvider")
    void invalidSimpleDateValuesThrowIllegalArgumentException(String description, String input) {
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toSqlDate(input, mockConverter), description);
    }
}
