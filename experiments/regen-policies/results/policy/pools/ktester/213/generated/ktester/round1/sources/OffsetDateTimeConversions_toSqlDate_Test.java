package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Date;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import java.time.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toSqlDate_Test {

    @Mock
    private Converter // mock of the Converter passed to the method
    mockConverter;

    @Mock
    private ConverterOptions // mock of the ConverterOptions returned by Converter#getOptions()
    mockOptions;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the mock chain: converter -> options -> zoneId
        when(mockConverter.getOptions()).thenReturn(mockOptions);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the interactions to keep each test independent
        Mockito.reset(mockConverter, mockOptions);
    }

    private static Stream<Arguments> normalConversionProvider() {
        return Stream.of(// source already in target zone (Europe/Paris is UTC+2)
        Arguments.of(OffsetDateTime.parse("2023-05-15T10:20:30+02:00"), ZoneId.of("Europe/Paris"), LocalDate.of(2023, 5, 15)), // source in UTC, target zone America/New_York (UTC‑4 in May) – still same calendar day
        Arguments.of(OffsetDateTime.of(2023, 5, 10, 15, 30, 0, 0, ZoneOffset.UTC), ZoneId.of("America/New_York"), LocalDate.of(2023, 5, 10)));
    }

    /**
     * Verify that a normal conversion (no date‑boundary shift) returns the correct java.sql.Date.
     */
    @Test
    void toSqlDate_sameDayAfterZoneConversion() {
        // GIVEN
        OffsetDateTime source = OffsetDateTime.of(2023, 5, 10, 15, 30, 0, 0, ZoneId.of("UTC"));
        // Convert to a zone that does not cross midnight (America/New_York is UTC‑4 in May)
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("America/New_York"));
        // WHEN
        Date result = OffsetDateTimeConversions.toSqlDate(source, mockConverter);
        // THEN
        // 15:30 UTC => 11:30 EDT, same calendar day
        LocalDate expectedLocalDate = LocalDate.of(2023, 5, 10);
        assertEquals(Date.valueOf(expectedLocalDate), result);
    }

    /**
     * Verify that a conversion that crosses a date boundary returns the previous day.
     */
    @Test
    void toSqlDate_crossesDateBoundary() {
        // GIVEN
        OffsetDateTime source = OffsetDateTime.of(2023, 5, 10, 1, 0, 0, 0, ZoneId.of("UTC"));
        // Convert to Los Angeles (UTC‑7 in May) – this moves the instant back to the previous day
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("America/Los_Angeles"));
        // WHEN
        Date result = OffsetDateTimeConversions.toSqlDate(source, mockConverter);
        // THEN
        // 01:00 UTC => 18:00 PDT on the previous calendar day
        LocalDate expectedLocalDate = LocalDate.of(2023, 5, 9);
        assertEquals(Date.valueOf(expectedLocalDate), result);
    }

    /**
     * Verify that a {@code null} source results in a {@link NullPointerException}.
     * The method casts the argument to {@link OffsetDateTime} and then dereferences it,
     * so a null argument should cause an NPE.
     */
    @Test
    void toSqlDate_nullSource_throwsNPE() {
        // GIVEN
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toSqlDate(null, mockConverter));
    }

    /**
     * Verify that a normal conversion (no date‑boundary shift) returns the correct {@link java.sql.Date}.
     *
     * <p>Two sub‑cases are exercised via {@link MethodSource}:
     * <ul>
     *   <li>source offset already matches the converter's zone (same calendar day)</li>
     *   <li>source offset differs but after zone conversion the day stays the same</li>
     * </ul>
     */
    @ParameterizedTest(name = "{index} => source={0}, zone={1}, expected={2}")
    @MethodSource("normalConversionProvider")
    void toSqlDate_normalConversion(OffsetDateTime source, ZoneId targetZone, LocalDate expected) {
        // GIVEN
        when(mockOptions.getZoneId()).thenReturn(targetZone);
        // WHEN
        Date result = OffsetDateTimeConversions.toSqlDate(source, mockConverter);
        // THEN
        assertEquals(Date.valueOf(expected), result, () -> "Expected SQL Date for source " + source + " with target zone " + targetZone);
    }

    @Test
    void toSqlDate_nullConverter_throwsNPE() {
        // GIVEN
        OffsetDateTime source = OffsetDateTime.parse("2023-05-15T10:20:30+02:00");
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toSqlDate(source, null));
    }

    @Test
    void toSqlDate_converterWithNullOptions_throwsNPE() {
        // GIVEN
        OffsetDateTime source = OffsetDateTime.parse("2023-05-15T10:20:30+02:00");
        // mockConverter already returns mockOptions; make that null
        when(mockConverter.getOptions()).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toSqlDate(source, mockConverter));
    }

    @Test
    void toSqlDate_converterWithNullZoneId_throwsNPE() {
        // GIVEN
        OffsetDateTime source = OffsetDateTime.parse("2023-05-15T10:20:30+02:00");
        // mockOptions returns null ZoneId
        when(mockOptions.getZoneId()).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toSqlDate(source, mockConverter));
    }

    @Test
    void toSqlDate_invalidFromType_throwsClassCastException() {
        // GIVEN
        String invalidSource = "2023-05-15T10:20:30+02:00";
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("Europe/Paris"));
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> OffsetDateTimeConversions.toSqlDate(invalidSource, mockConverter));
    }

    @Test
    void toSqlDate_yearOutOfRange_throwsIllegalArgumentException() {
        // GIVEN
        // OffsetDateTime with year 10000 (beyond java.sql.Date range)
        OffsetDateTime source = OffsetDateTime.parse("10000-01-01T00:00:00Z");
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toSqlDate(source, mockConverter));
    }
}
