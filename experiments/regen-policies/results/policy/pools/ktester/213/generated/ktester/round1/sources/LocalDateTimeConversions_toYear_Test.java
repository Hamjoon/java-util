package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toYear_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Use default options – the default ZoneId is UTC unless overridden.
        ConverterOptions options = new ConverterOptions();
        // Ensure a known zone to make the test deterministic.
        options.setZoneId(ZoneId.of("UTC"));
        converter = new Converter(options);
    }

    static Stream<Arguments> normalConversionProvider() {
        return Stream.of(// Standard path – a valid LocalDateTime and a converter with a proper ZoneId
        Arguments.of(LocalDateTime.of(2023, 5, 15, 10, 30), ZoneId.of("UTC"), Year.of(2023)), // Edge of year – last second of the year, still same year in the given zone
        Arguments.of(LocalDateTime.of(1999, 12, 31, 23, 59, 59), ZoneId.of("America/New_York"), Year.of(1999)));
    }

    static Stream<Arguments> boundaryYearProvider() {
        return Stream.of(// Leap day – should stay in the same year
        Arguments.of(LocalDateTime.of(2020, 2, 29, 0, 0), ZoneId.of("UTC"), Year.of(2020)), // Maximum positive offset – date does not roll over to previous/next year
        Arguments.of(LocalDateTime.of(2021, 1, 1, 0, 0), ZoneId.of("UTC+14"), Year.of(2021)), // Maximum negative offset – still stays in the same calendar year
        Arguments.of(LocalDateTime.of(2021, 12, 31, 23, 59, 59, 999_999_999), ZoneId.of("UTC-12"), Year.of(2021)));
    }

    @Test
    void toYear_convertsLocalDateTimeToCorrectYear() {
        // given
        LocalDateTime ldt = LocalDateTime.of(2023, 5, 17, 14, 30, 45);
        // when
        Year result = LocalDateTimeConversions.toYear(ldt, converter);
        // then
        assertNotNull(result, "Resulting Year should not be null");
        assertEquals(2023, result.getValue(), "Year value should match the LocalDateTime year");
    }

    @Test
    void toYear_usesConverterZoneId() {
        // given: a LocalDateTime that would fall on a different calendar day in another zone
        // 23:00 on Dec 31
        LocalDateTime ldt = LocalDateTime.of(2023, 12, 31, 23, 0);
        // Change the converter's zone to one hour ahead (UTC+01:00)
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(ZoneId.of("UTC+01:00"));
        Converter convWithOffset = new Converter(options);
        // when
        Year result = LocalDateTimeConversions.toYear(ldt, convWithOffset);
        // then
        // In UTC+01 the same instant is 2024-01-01, so the year should be 2024
        assertEquals(2024, result.getValue(), "Year should reflect the converter's zone offset (UTC+01 shifts date to next year)");
    }

    @Test
    void toYear_nullInputReturnsNull() {
        // given
        Object nullObj = null;
        // when
        Year result = LocalDateTimeConversions.toYear(nullObj, converter);
        // then
        assertNull(result, "Converting null should return null");
    }

    @Test
    void toYear_nullInputThrowsNullPointerException() {
        // The implementation dereferences the input before any null‑check,
        // therefore a NullPointerException is expected.
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toYear(null, converter), "Converting a null `from` should throw NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] from={0} zone={1} => expected={2}")
    @MethodSource("normalConversionProvider")
    void toYear_normalConversion(LocalDateTime from, ZoneId zone, Year expected) {
        // given
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(zone);
        Converter conv = new Converter(options);
        // when
        Year result = LocalDateTimeConversions.toYear(from, conv);
        // then
        assertEquals(expected, result, () -> String.format("Year derived from %s with zone %s should be %s", from, zone, expected));
    }

    @Test
    void toYear_invalidFromType_throwsClassCastException() {
        // given a String instead of a LocalDateTime
        Object badFrom = "2023-05-15T10:30";
        // when / then
        assertThrows(ClassCastException.class, () -> LocalDateTimeConversions.toYear(badFrom, converter), "Passing a non‑LocalDateTime should cause ClassCastException");
    }

    @Test
    void toYear_nullConverter_throwsNullPointerException() {
        LocalDateTime ldt = LocalDateTime.of(2023, 5, 15, 10, 30);
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toYear(ldt, null), "A null converter should cause NullPointerException when accessed");
    }

    @Test
    void toYear_converterWithNullOptions_throwsNullPointerException() {
        // Mock a Converter that returns null for getOptions()
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.getOptions()).thenReturn(null);
        LocalDateTime ldt = LocalDateTime.of(2023, 5, 15, 10, 30);
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toYear(ldt, mockConverter), "When converter.getOptions() returns null, a NullPointerException is expected");
    }

    @Test
    void toYear_optionsWithNullZoneId_throwsDateTimeException() {
        // Mock options to return a null ZoneId
        ConverterOptions mockOptions = mock(ConverterOptions.class);
        when(mockOptions.getZoneId()).thenReturn(null);
        // Mock converter to return the mocked options
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        LocalDateTime ldt = LocalDateTime.of(2023, 5, 15, 10, 30);
        // atZone(null) throws DateTimeException
        assertThrows(DateTimeException.class, () -> LocalDateTimeConversions.toYear(ldt, mockConverter), "A null ZoneId should cause DateTimeException from LocalDateTime.atZone(null)");
    }

    @ParameterizedTest(name = "[{index}] from={0} zone={1} => expected={2}")
    @MethodSource("boundaryYearProvider")
    void toYear_boundaryYearTransitions(LocalDateTime from, ZoneId zone, Year expected) {
        // given
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(zone);
        Converter conv = new Converter(options);
        // when
        Year result = LocalDateTimeConversions.toYear(from, conv);
        // then
        assertEquals(expected, result, () -> String.format("Boundary test: %s with zone %s should yield year %s", from, zone, expected));
    }
}
