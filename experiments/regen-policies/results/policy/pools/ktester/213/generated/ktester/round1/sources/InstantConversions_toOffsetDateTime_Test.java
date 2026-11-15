package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.Date;

@ExtendWith(MockitoExtension.class)
class InstantConversions_toOffsetDateTime_Test {

    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    @BeforeEach
    void setupBeforeEach() {
        // Wire the mock hierarchy: Converter -> ConverterOptions -> TimeZone
        when(mockConverter.getOptions()).thenReturn(mockOptions);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test interference.
        reset(mockConverter, mockOptions);
    }

    /**
     * Helper that configures the mock {@link Converter} to return the supplied {@link TimeZone}.
     */
    private void configureConverter(TimeZone tz) {
        when(mockOptions.getTimeZone()).thenReturn(tz);
    }

    @Test
    void toOffsetDateTime_PositiveOffset() {
        // GIVEN
        // 1970‑01‑01T00:00:00Z
        Instant instant = Instant.ofEpochSecond(0L);
        TimeZone tz = TimeZone.getTimeZone("GMT+02:00");
        when(mockOptions.getTimeZone()).thenReturn(tz);
        // WHEN
        OffsetDateTime result = InstantConversions.toOffsetDateTime(instant, mockConverter);
        // THEN
        ZoneOffset expectedOffset = ZoneOffset.ofHours(2);
        OffsetDateTime expected = instant.atOffset(expectedOffset);
        assertEquals(expected, result);
    }

    @Test
    void toOffsetDateTime_NegativeOffset() {
        // GIVEN
        // 1970‑01‑01T00:00:00Z
        Instant instant = Instant.ofEpochSecond(0L);
        TimeZone tz = TimeZone.getTimeZone("GMT-08:00");
        when(mockOptions.getTimeZone()).thenReturn(tz);
        // WHEN
        OffsetDateTime result = InstantConversions.toOffsetDateTime(instant, mockConverter);
        // THEN
        ZoneOffset expectedOffset = ZoneOffset.ofHours(-8);
        OffsetDateTime expected = instant.atOffset(expectedOffset);
        assertEquals(expected, result);
    }

    @Test
    void toOffsetDateTime_NullInstantThrowsNPE() {
        // GIVEN
        TimeZone tz = TimeZone.getTimeZone("GMT");
        when(mockOptions.getTimeZone()).thenReturn(tz);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> InstantConversions.toOffsetDateTime(null, mockConverter));
    }

    @Test
    void toOffsetDateTime_WrongTypeThrowsClassCastException() {
        // GIVEN
        TimeZone tz = TimeZone.getTimeZone("GMT");
        when(mockOptions.getTimeZone()).thenReturn(tz);
        Object notAnInstant = "2023-01-01";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> InstantConversions.toOffsetDateTime(notAnInstant, mockConverter));
    }

    @Test
    void toOffsetDateTime_UtcOffset() {
        // GIVEN: an instant and a converter that supplies UTC
        Instant instant = Instant.parse("2023-01-01T12:34:56Z");
        configureConverter(TimeZone.getTimeZone("UTC"));
        // WHEN
        OffsetDateTime result = InstantConversions.toOffsetDateTime(instant, mockConverter);
        // THEN: the result must have offset +00:00 and represent the same instant
        OffsetDateTime expected = OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
        assertEquals(expected, result);
    }

    @Test
    void toOffsetDateTime_PositiveOffset_GmtPlus02() {
        // GIVEN: an instant and a converter that supplies GMT+02:00
        Instant instant = Instant.parse("2023-06-15T08:00:00Z");
        configureConverter(TimeZone.getTimeZone("GMT+02:00"));
        // WHEN
        OffsetDateTime result = InstantConversions.toOffsetDateTime(instant, mockConverter);
        // THEN: offset +02:00 must be applied
        OffsetDateTime expected = OffsetDateTime.ofInstant(instant, ZoneOffset.ofHours(2));
        assertEquals(expected, result);
    }

    @Test
    void toOffsetDateTime_NegativeOffset_GmtMinus05() {
        // GIVEN: an instant and a converter that supplies GMT-05:00
        Instant instant = Instant.parse("2023-12-01T15:30:00Z");
        configureConverter(TimeZone.getTimeZone("GMT-05:00"));
        // WHEN
        OffsetDateTime result = InstantConversions.toOffsetDateTime(instant, mockConverter);
        // THEN: offset -05:00 must be applied
        OffsetDateTime expected = OffsetDateTime.ofInstant(instant, ZoneOffset.ofHours(-5));
        assertEquals(expected, result);
    }

    @Test
    void toOffsetDateTime_HalfHourOffset_GmtPlus0530() {
        // GIVEN: an instant and a converter that supplies GMT+05:30 (non‑whole‑hour offset)
        Instant instant = Instant.parse("2023-03-10T10:15:30Z");
        configureConverter(TimeZone.getTimeZone("GMT+05:30"));
        // WHEN
        OffsetDateTime result = InstantConversions.toOffsetDateTime(instant, mockConverter);
        // THEN: offset +05:30 must be applied
        ZoneOffset halfHour = ZoneOffset.ofHoursMinutes(5, 30);
        OffsetDateTime expected = OffsetDateTime.ofInstant(instant, halfHour);
        assertEquals(expected, result);
    }

    @Test
    void toOffsetDateTime_NullTimeZoneThrowsNPE() {
        // GIVEN: a valid instant but the converter returns null TimeZone
        Instant instant = Instant.parse("2022-01-01T00:00:00Z");
        // mockOptions.getTimeZone() -> null
        configureConverter(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> InstantConversions.toOffsetDateTime(instant, mockConverter));
    }

    @Test
    void toOffsetDateTime_InvalidSourceTypeThrowsClassCastException() {
        // GIVEN: a source object that is not an Instant
        // java.util.Date
        Date notAnInstant = new Date();
        configureConverter(TimeZone.getTimeZone("UTC"));
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> InstantConversions.toOffsetDateTime(notAnInstant, mockConverter));
    }

    @Test
    void toOffsetDateTime_NullSourceObjectThrowsNPE() {
        // GIVEN: null source object
        configureConverter(TimeZone.getTimeZone("UTC"));
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> InstantConversions.toOffsetDateTime(null, mockConverter));
    }

    @Test
    void toOffsetDateTime_NullConverterThrowsNPE() {
        // GIVEN: a valid instant but the converter reference itself is null
        Instant instant = Instant.parse("2023-01-01T00:00:00Z");
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> InstantConversions.toOffsetDateTime(instant, null));
    }
}
