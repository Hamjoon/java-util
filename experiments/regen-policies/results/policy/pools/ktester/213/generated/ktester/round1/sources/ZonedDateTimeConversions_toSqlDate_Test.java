package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Date;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toSqlDate_Test {

    @Mock
    private Converter // the Converter used by the method
    mockConverter;

    @Mock
    private ConverterOptions // ConverterOptions returned from the Converter
    mockOptions;

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mock hierarchy so that mockConverter.getOptions() returns mockOptions
        when(mockConverter.getOptions()).thenReturn(mockOptions);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks after each test to avoid cross‑test interference
        reset(mockConverter, mockOptions);
    }

    /**
     * Verify that a ZonedDateTime is converted to a java.sql.Date using the
     * Converter's target ZoneId without changing the calendar day.
     */
    @Test
    void toSqlDate_sameDayAfterZoneConversion() {
        // GIVEN – a ZonedDateTime in New York (UTC‑4) and a Converter that targets UTC
        ZonedDateTime source = ZonedDateTime.of(2023, 5, 15, 10, 30, 0, 0, // 2023‑05‑15T10:30‑04:00
        ZoneId.of("America/New_York"));
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
        // WHEN
        Date result = ZonedDateTimeConversions.toSqlDate(source, mockConverter);
        // THEN – the instant in UTC is 2023‑05‑15T14:30Z, the local date is still 2023‑05‑15
        Date expected = Date.valueOf(LocalDate.of(2023, 5, 15));
        assertEquals(expected, result, "The SQL Date should represent the same calendar day after zone conversion.");
    }

    /**
     * Verify that a ZonedDateTime whose instant crosses midnight when converted
     * to the target zone results in the correct next‑day java.sql.Date.
     */
    @Test
    void toSqlDate_crossesMidnightAfterZoneConversion() {
        // GIVEN – a ZonedDateTime just before midnight in Los Angeles (UTC‑7)
        ZonedDateTime source = ZonedDateTime.of(2023, 5, 15, 23, 30, 0, 0, // 2023‑05‑15T23:30‑07:00
        ZoneId.of("America/Los_Angeles"));
        // Converter targets UTC; the instant becomes 2023‑05‑16T06:30Z (next day)
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
        // WHEN
        Date result = ZonedDateTimeConversions.toSqlDate(source, mockConverter);
        // THEN – the resulting SQL Date should be 2023‑05‑16
        Date expected = Date.valueOf(LocalDate.of(2023, 5, 16));
        assertEquals(expected, result, "The SQL Date should reflect the next calendar day after zone conversion.");
    }

    /**
     * Verify that the method throws a ClassCastException when the supplied
     * source object is not a ZonedDateTime.
     */
    @Test
    void toSqlDate_invalidSourceType() {
        // GIVEN – an object that is not a ZonedDateTime
        Object notZoned = "2023-05-15";
        // The converter's zone is irrelevant for this test, but we still stub it
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
        // WHEN / THEN – a ClassCastException is expected
        assertThrows(ClassCastException.class, () -> ZonedDateTimeConversions.toSqlDate(notZoned, mockConverter), "A non‑ZonedDateTime source should cause a ClassCastException.");
    }

    /**
     * Verify that a ZonedDateTime that moves *back* a day after zone conversion
     * yields the previous calendar day as a java.sql.Date.
     */
    @Test
    void toSqlDate_previousDayAfterZoneConversion() {
        // GIVEN – 01:30 on 2023‑03‑15 in Europe/Paris (+02)
        ZonedDateTime source = ZonedDateTime.parse("2023-03-15T01:30+02:00[Europe/Paris]");
        // Converter targets UTC; the instant becomes 2023‑03‑14T23:30Z (previous day)
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
        // WHEN
        Date result = ZonedDateTimeConversions.toSqlDate(source, mockConverter);
        // THEN – the SQL Date should represent 2023‑03‑14
        Date expected = Date.valueOf(LocalDate.of(2023, 3, 14));
        assertEquals(expected, result, "The SQL Date should represent the previous calendar day after zone conversion.");
    }

    /**
     * Verify that passing {@code null} as the source argument results in a
     * {@link NullPointerException} (the cast to {@link ZonedDateTime} yields null,
     * and the subsequent call to {@code withZoneSameInstant} dereferences it).
     */
    @Test
    void toSqlDate_nullSource() {
        // GIVEN
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toSqlDate(null, mockConverter), "A null source should cause a NullPointerException.");
    }

    /**
     * Verify that passing {@code null} as the {@link Converter} argument results in a
     * {@link NullPointerException} because the method attempts to call {@code getOptions()}.
     */
    @Test
    void toSqlDate_nullConverter() {
        // GIVEN – a valid ZonedDateTime source
        ZonedDateTime source = ZonedDateTime.now();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toSqlDate(source, null), "A null converter should cause a NullPointerException.");
    }

    /**
     * Verify that a {@link Converter} returning {@code null} for {@link ConverterOptions}
     * leads to a {@link NullPointerException} when the method tries to obtain the target
     * {@link ZoneId}.
     */
    @Test
    void toSqlDate_converterWithNullOptions() {
        // GIVEN – a valid source
        ZonedDateTime source = ZonedDateTime.now();
        // Stub the converter to return null options
        when(mockConverter.getOptions()).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toSqlDate(source, mockConverter), "A converter with null options should cause a NullPointerException.");
    }

    /**
     * Verify that a {@link ConverterOptions} returning {@code null} for {@link ZoneId}
     * causes a {@link NullPointerException} because {@code withZoneSameInstant(null)} is illegal.
     */
    @Test
    void toSqlDate_converterWithNullZoneId() {
        // GIVEN – a valid source
        ZonedDateTime source = ZonedDateTime.now();
        // Stub the options to return a null ZoneId
        when(mockOptions.getZoneId()).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toSqlDate(source, mockConverter), "A converter with null ZoneId should cause a NullPointerException.");
    }
}
