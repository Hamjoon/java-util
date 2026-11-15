package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toMonthDay_Test {

    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    private AutoCloseable mocks;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations for this test instance
        mocks = MockitoAnnotations.openMocks(this);
        // Default behaviour: the converter returns the mocked options
        when(mockConverter.getOptions()).thenReturn(mockOptions);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Release Mockito resources.
        mocks.close();
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Standard conversion: both source and target are UTC.
        Arguments.of("Standard conversion where source and target are UTC", ZonedDateTime.parse("2023-03-15T10:30:00Z"), ZoneId.of("UTC"), MonthDay.of(3, 15)), // America/New_York -> UTC crossing the year boundary.
        Arguments.of("America/New_York to UTC crossing midnight (2023‑12‑31 23:00‑05:00 => 2024‑01‑01 04:00Z)", ZonedDateTime.of(2023, 12, 31, 23, 0, 0, 0, ZoneId.of("America/New_York")), ZoneId.of("UTC"), MonthDay.of(1, 1)), // Same zone, no shift.
        Arguments.of("America/New_York to America/New_York (no zone shift)", ZonedDateTime.of(2023, 12, 31, 23, 0, 0, 0, ZoneId.of("America/New_York")), ZoneId.of("America/New_York"), MonthDay.of(12, 31)));
    }

    private static Stream<Arguments> errorHandlingProvider() {
        return Stream.of(// Null source object.
        Arguments.of("Null source should cause NullPointerException", null, ZoneId.of("UTC"), NullPointerException.class), // Wrong source type (LocalDate) leads to ClassCastException.
        Arguments.of("Wrong source type (LocalDate) should cause ClassCastException", LocalDate.of(2023, 5, 10), ZoneId.of("UTC"), ClassCastException.class), // Converter's options return null ZoneId.
        Arguments.of("Null ZoneId from options should cause NullPointerException", ZonedDateTime.parse("2023-06-01T12:00:00Z"), // targetZone null → mockOptions.getZoneId() returns null
        null, NullPointerException.class));
    }

    @Test
    void toMonthDay_convertsWithDifferentZone() {
        // GIVEN
        ZonedDateTime source = ZonedDateTime.of(2022, 3, 15, 10, 30, 0, 0, // 2022‑03‑15T10:30‑04:00[America/New_York]
        ZoneId.of("America/New_York"));
        // Converter is configured to use UTC as the target zone
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
        // WHEN
        MonthDay result = ZonedDateTimeConversions.toMonthDay(source, mockConverter);
        // THEN
        // The instant 2022‑03‑15T14:30Z corresponds to the same calendar day (2022‑03‑15) in UTC
        MonthDay expected = MonthDay.of(Month.MARCH, 15);
        assertEquals(expected, result);
    }

    @Test
    void toMonthDay_convertsWhenZoneIsSame() {
        // GIVEN
        ZonedDateTime source = ZonedDateTime.of(2021, 12, 31, 23, 45, 0, 0, // already UTC
        ZoneId.of("UTC"));
        // Converter also uses UTC – no zone shift should happen
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
        // WHEN
        MonthDay result = ZonedDateTimeConversions.toMonthDay(source, mockConverter);
        // THEN
        MonthDay expected = MonthDay.of(Month.DECEMBER, 31);
        assertEquals(expected, result);
    }

    @Test
    void toMonthDay_convertsAcrossDateBoundary() {
        // GIVEN
        // 2022‑01‑01 01:00 in Tokyo (+09:00) is 2021‑12‑31 16:00 UTC – date changes to previous day
        ZonedDateTime source = ZonedDateTime.of(2022, 1, 1, 1, 0, 0, 0, ZoneId.of("Asia/Tokyo"));
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
        // WHEN
        MonthDay result = ZonedDateTimeConversions.toMonthDay(source, mockConverter);
        // THEN
        // date after conversion is 2021‑12‑31
        MonthDay expected = MonthDay.of(Month.DECEMBER, 31);
        assertEquals(expected, result);
    }

    /**
     * Parameterized test covering the three "Valid conversions" cases from the JSON spec.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionProvider")
    void toMonthDay_validConversions(String description, ZonedDateTime source, ZoneId targetZone, MonthDay expected) {
        // GIVEN
        when(mockOptions.getZoneId()).thenReturn(targetZone);
        // WHEN
        MonthDay result = ZonedDateTimeConversions.toMonthDay(source, mockConverter);
        // THEN
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("errorHandlingProvider")
    void toMonthDay_errorHandling(String description, Object from, ZoneId targetZone, Class<? extends Throwable> expectedException) {
        // GIVEN
        if (targetZone != null) {
            when(mockOptions.getZoneId()).thenReturn(targetZone);
        } else {
            // Simulate options returning null (the method will NPE on withZoneSameInstant(null))
            when(mockOptions.getZoneId()).thenReturn(null);
        }
        // WHEN / THEN
        assertThrows(expectedException, () -> ZonedDateTimeConversions.toMonthDay(from, mockConverter), description);
    }

    @Test
    void toMonthDay_converterIsNull_throwsNullPointerException() {
        // GIVEN
        ZonedDateTime source = ZonedDateTime.parse("2023-01-01T00:00:00Z");
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toMonthDay(source, null), "Passing a null Converter should throw NullPointerException");
    }

    @Test
    void toMonthDay_converterOptionsIsNull_throwsNullPointerException() {
        // GIVEN
        ZonedDateTime source = ZonedDateTime.parse("2023-01-01T00:00:00Z");
        // Mock converter that returns null options.
        Converter converterWithNullOptions = mock(Converter.class);
        when(converterWithNullOptions.getOptions()).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toMonthDay(source, converterWithNullOptions), "Converter returning null options should throw NullPointerException");
    }
}
