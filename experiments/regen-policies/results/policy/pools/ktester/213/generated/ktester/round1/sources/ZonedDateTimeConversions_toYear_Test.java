package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toYear_Test {

    @Mock
    private Converter // the Converter passed to the method
    mockConverter;

    @Mock
    private ConverterOptions // the options held inside the Converter
    mockOptions;

    // for closing the Mockito session
    private AutoCloseable mocks;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the Mockito annotations for this test instance
        mocks = MockitoAnnotations.openMocks(this);
        // By default, any call to mockConverter.getOptions() returns the mocked options object
        when(mockConverter.getOptions()).thenReturn(mockOptions);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Release Mockito resources
        mocks.close();
    }

    /**
     * Provides a stream of valid conversion arguments:
     * <ul>
     *   <li>source ZonedDateTime</li>
     *   <li>target ZoneId (the zone supplied via ConverterOptions)</li>
     *   <li>expected Year after conversion</li>
     * </ul>
     */
    private static Stream<Arguments> provideValidConversions() {
        return Stream.of(// ----- group: Valid conversions -----
        Arguments.of(ZonedDateTime.of(2022, 5, 15, 10, 30, 0, 0, ZoneId.of("UTC")), ZoneId.of("UTC"), Year.of(2022)), Arguments.of(ZonedDateTime.of(2022, 12, 31, 23, 30, 0, 0, ZoneId.of("America/New_York")), ZoneId.of("UTC"), Year.of(2023)), Arguments.of(ZonedDateTime.of(2023, 1, 1, 1, 0, 0, 0, ZoneId.of("Europe/Paris")), ZoneId.of("UTC"), Year.of(2022)), Arguments.of(ZonedDateTime.of(2021, 6, 30, 15, 45, 0, 0, ZoneId.of("Asia/Tokyo")), ZoneId.of("America/Los_Angeles"), Year.of(2021)), // ----- group: Additional edge cases -----
        Arguments.of(ZonedDateTime.of(2020, 2, 29, 23, 0, 0, 0, ZoneId.of("UTC")), ZoneId.of("UTC+02:00"), Year.of(2020)), Arguments.of(ZonedDateTime.of(2020, 12, 31, 23, 30, 0, 0, ZoneId.of("UTC-05:00")), ZoneId.of("UTC+14:00"), Year.of(2021)), Arguments.of(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")), ZoneId.of("UTC"), Year.of(1970)), Arguments.of(ZonedDateTime.of(9999, 12, 31, 23, 59, 59, 999_999_999, ZoneId.of("UTC")), ZoneId.of("UTC"), Year.of(9999)), Arguments.of(ZonedDateTime.of(1, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")), ZoneId.of("UTC"), Year.of(1)), Arguments.of(ZonedDateTime.of(2022, 3, 27, 2, 30, 0, 0, ZoneId.of("Europe/Paris")), ZoneId.of("UTC"), Year.of(2022)), Arguments.of(ZonedDateTime.of(2022, 10, 30, 2, 30, 0, 0, ZoneId.of("Europe/Paris")), ZoneId.of("UTC"), Year.of(2022)));
    }

    /**
     * Verify that the conversion respects the target {@link ZoneId} and returns the correct {@link Year}
     * when the instant stays within the same calendar year after zone conversion.
     */
    @Test
    void toYear_SameYearAfterZoneConversion() {
        // GIVEN
        // UTC‑5
        ZoneId targetZone = ZoneId.of("America/New_York");
        when(mockOptions.getZoneId()).thenReturn(targetZone);
        // 2022‑12‑31T23:00Z → 2022‑12‑31T18:00‑05:00 (still 2022)
        ZonedDateTime source = ZonedDateTime.of(2022, 12, 31, 23, 0, 0, 0, ZoneId.of("UTC"));
        // WHEN
        Year result = ZonedDateTimeConversions.toYear(source, mockConverter);
        // THEN
        assertNotNull(result, "Resulting Year should not be null");
        assertEquals(2022, result.getValue(), "Year should remain 2022 after conversion to America/New_York");
    }

    /**
     * Verify that the conversion correctly rolls over to the next calendar year when the
     * target zone moves the instant past midnight.
     */
    @Test
    void toYear_NextYearAfterZoneConversion() {
        // GIVEN
        // UTC+9
        ZoneId targetZone = ZoneId.of("Asia/Tokyo");
        when(mockOptions.getZoneId()).thenReturn(targetZone);
        // 2022‑12‑31T23:30Z → 2023‑01‑01T08:30+09:00 (next year)
        ZonedDateTime source = ZonedDateTime.of(2022, 12, 31, 23, 30, 0, 0, ZoneId.of("UTC"));
        // WHEN
        Year result = ZonedDateTimeConversions.toYear(source, mockConverter);
        // THEN
        assertNotNull(result, "Resulting Year should not be null");
        assertEquals(2023, result.getValue(), "Year should be 2023 after conversion to Asia/Tokyo");
    }

    /**
     * Verify that a {@code null} source object results in a {@link NullPointerException},
     * mirroring the behaviour of the implementation which casts the argument to {@link ZonedDateTime}
     * without a null‑check.
     */
    @Test
    void toYear_NullSourceThrowsException() {
        // GIVEN
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toYear(null, mockConverter), "Passing a null source should throw NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] {2} (zone: {1})")
    @MethodSource("provideValidConversions")
    void toYear_ValidConversions(ZonedDateTime source, ZoneId targetZone, Year expected) {
        // Arrange
        when(mockOptions.getZoneId()).thenReturn(targetZone);
        // Act
        Year result = ZonedDateTimeConversions.toYear(source, mockConverter);
        // Assert
        assertEquals(expected, result, () -> String.format("Expected Year %s but got %s for source %s and target zone %s", expected, result, source, targetZone));
    }

    @Test
    void toYear_NonZonedDateTimeThrowsClassCastException() {
        // GIVEN
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
        String invalidSource = "2022-05-15T10:30:00Z";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> ZonedDateTimeConversions.toYear(invalidSource, mockConverter), "A non‑ZonedDateTime source should trigger ClassCastException");
    }

    @Test
    void toYear_ConverterWithNullOptionsThrowsNPE() {
        // GIVEN
        when(mockConverter.getOptions()).thenReturn(null);
        ZonedDateTime source = ZonedDateTime.now();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toYear(source, mockConverter), "A Converter returning null options should cause NullPointerException");
    }

    @Test
    void toYear_ConverterWithNullZoneIdThrowsNPE() {
        // GIVEN
        when(mockOptions.getZoneId()).thenReturn(null);
        ZonedDateTime source = ZonedDateTime.now();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toYear(source, mockConverter), "A null ZoneId in ConverterOptions should cause NullPointerException");
    }

    @Test
    void toYear_NullConverterThrowsNPE() {
        // GIVEN
        ZonedDateTime source = ZonedDateTime.now();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toYear(source, null), "Passing a null Converter should cause NullPointerException");
    }
}
