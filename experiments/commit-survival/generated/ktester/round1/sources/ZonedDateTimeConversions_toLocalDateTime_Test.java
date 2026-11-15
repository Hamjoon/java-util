package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toLocalDateTime_Test {

    @Mock
    private Converter converter;

    @Mock
    private ConverterOptions options;

    @BeforeEach
    void setupBeforeEach() {
        // By default the mock converter returns the mock options
        when(converter.getOptions()).thenReturn(options);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks after each test to avoid interaction leakage
        reset(converter, options);
    }

    @Test
    void toLocalDateTime_WithSameZone_ReturnsUnchangedLocalDateTime() {
        // GIVEN
        ZoneId zone = ZoneId.of("UTC");
        ZonedDateTime source = ZonedDateTime.of(LocalDateTime.of(2023, Month.MAY, 10, 15, 30, 45), zone);
        // Converter is configured to use the same zone as the source
        when(options.getZoneId()).thenReturn(zone);
        // WHEN
        LocalDateTime result = ZonedDateTimeConversions.toLocalDateTime(source, converter);
        // THEN
        assertEquals(source.toLocalDateTime(), result, "When the target zone equals the source zone the LocalDateTime should be unchanged");
    }

    @Test
    void toLocalDateTime_WithDifferentZone_ConvertsInstantCorrectly() {
        // GIVEN
        ZoneId sourceZone = ZoneId.of("UTC");
        ZoneId targetZone = ZoneId.of("America/New_York");
        ZonedDateTime source = ZonedDateTime.of(LocalDateTime.of(2023, Month.DECEMBER, 25, 12, 0, 0), sourceZone);
        // Expected result after adjusting the instant to the target zone
        LocalDateTime expected = source.withZoneSameInstant(targetZone).toLocalDateTime();
        when(options.getZoneId()).thenReturn(targetZone);
        // WHEN
        LocalDateTime result = ZonedDateTimeConversions.toLocalDateTime(source, converter);
        // THEN
        assertEquals(expected, result, "The LocalDateTime should reflect the same instant in the target zone");
    }

    @Test
    void toLocalDateTime_NullSource_ThrowsNullPointerException() {
        // GIVEN
        when(options.getZoneId()).thenReturn(ZoneId.systemDefault());
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toLocalDateTime(null, converter), "Passing a null source should result in a NullPointerException");
    }

    @Test
    void toLocalDateTime_NullConverter_ThrowsNullPointerException() {
        // GIVEN
        ZonedDateTime source = ZonedDateTime.now();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toLocalDateTime(source, null), "Passing a null converter should result in a NullPointerException");
    }

    @Test
    void toLocalDateTime_InvalidFromType_ThrowsClassCastException() {
        // GIVEN
        when(options.getZoneId()).thenReturn(ZoneId.of("UTC"));
        String invalidFrom = "2023-05-15T10:30:00Z";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> ZonedDateTimeConversions.toLocalDateTime(invalidFrom, converter), "Providing a non‑ZonedDateTime source should cause a ClassCastException");
    }

    @Test
    void toLocalDateTime_ConverterWithNullZoneId_ThrowsNullPointerException() {
        // GIVEN
        ZonedDateTime source = ZonedDateTime.of(LocalDateTime.of(2023, 5, 15, 10, 30, 0), ZoneId.of("UTC"));
        // null ZoneId
        when(options.getZoneId()).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toLocalDateTime(source, converter), "A null ZoneId in the converter options should propagate a NullPointerException");
    }

    @Test
    void toLocalDateTime_DifferentZoneWinterOffset_ConvertsCorrectly() {
        // GIVEN
        ZoneId sourceZone = ZoneId.of("UTC");
        // winter = UTC‑5
        ZoneId targetZone = ZoneId.of("America/New_York");
        ZonedDateTime source = ZonedDateTime.of(LocalDateTime.of(2023, 1, 15, 14, 0, 0), sourceZone);
        LocalDateTime expected = LocalDateTime.of(2023, 1, 15, 9, 0);
        when(options.getZoneId()).thenReturn(targetZone);
        // WHEN
        LocalDateTime result = ZonedDateTimeConversions.toLocalDateTime(source, converter);
        // THEN
        assertEquals(expected, result, "Winter offset conversion (UTC‑5) should yield 09:00 local time");
    }

    @Test
    void toLocalDateTime_NonHourOffsetConversion_Handles45MinuteOffset() {
        // GIVEN
        ZoneId sourceZone = ZoneId.of("UTC");
        // UTC+5:45
        ZoneId targetZone = ZoneId.of("Asia/Kathmandu");
        ZonedDateTime source = ZonedDateTime.of(LocalDateTime.of(2023, 5, 15, 12, 0, 0), sourceZone);
        LocalDateTime expected = LocalDateTime.of(2023, 5, 15, 17, 45);
        when(options.getZoneId()).thenReturn(targetZone);
        // WHEN
        LocalDateTime result = ZonedDateTimeConversions.toLocalDateTime(source, converter);
        // THEN
        assertEquals(expected, result, "Conversion to a zone with a 45‑minute offset must preserve the extra minutes");
    }

    @Test
    void toLocalDateTime_NanosecondPrecisionPreservation() {
        // GIVEN
        ZoneId zone = ZoneId.of("UTC");
        ZonedDateTime source = ZonedDateTime.of(LocalDateTime.of(2023, 5, 15, 10, 30, 45, 123_456_789), zone);
        when(options.getZoneId()).thenReturn(zone);
        // WHEN
        LocalDateTime result = ZonedDateTimeConversions.toLocalDateTime(source, converter);
        // THEN
        LocalDateTime expected = LocalDateTime.of(2023, 5, 15, 10, 30, 45, 123_456_789);
        assertEquals(expected, result, "Nanosecond component must be retained when source and target zones are identical");
    }

    @Test
    void toLocalDateTime_EpochConversionToPositiveOffset() {
        // GIVEN
        // UTC+9
        ZoneId targetZone = ZoneId.of("Asia/Tokyo");
        ZonedDateTime source = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC"));
        LocalDateTime expected = LocalDateTime.of(1970, 1, 1, 9, 0);
        when(options.getZoneId()).thenReturn(targetZone);
        // WHEN
        LocalDateTime result = ZonedDateTimeConversions.toLocalDateTime(source, converter);
        // THEN
        assertEquals(expected, result, "Epoch instant shifted to UTC+9 should become 1970‑01‑01T09:00");
    }

    @Test
    void toLocalDateTime_LargeYearConversion_MaxSupportedYear() {
        // GIVEN
        ZoneId zone = ZoneId.of("UTC");
        ZonedDateTime source = ZonedDateTime.of(LocalDateTime.of(9999, 12, 31, 23, 59, 59, 999_999_999), zone);
        when(options.getZoneId()).thenReturn(zone);
        // WHEN
        LocalDateTime result = ZonedDateTimeConversions.toLocalDateTime(source, converter);
        // THEN
        LocalDateTime expected = LocalDateTime.of(9999, 12, 31, 23, 59, 59, 999_999_999);
        assertEquals(expected, result, "Conversion should succeed for the maximum year supported by LocalDateTime");
    }

    @Test
    void toLocalDateTime_OriginalZonedDateTimeImmutability() {
        // GIVEN
        ZoneId sourceZone = ZoneId.of("UTC");
        // UTC‑7 (summer)
        ZoneId targetZone = ZoneId.of("America/Los_Angeles");
        ZonedDateTime source = ZonedDateTime.of(LocalDateTime.of(2023, 5, 15, 14, 0, 0), sourceZone);
        // keep a reference for later comparison
        ZonedDateTime sourceCopy = source;
        when(options.getZoneId()).thenReturn(targetZone);
        // WHEN
        LocalDateTime result = ZonedDateTimeConversions.toLocalDateTime(source, converter);
        // THEN
        assertEquals(LocalDateTime.of(2023, 5, 15, 7, 0), result, "Converted LocalDateTime should be 07:00 (UTC‑7)");
        assertEquals(sourceCopy, source, "The original ZonedDateTime must remain unchanged after conversion");
    }

    @Test
    void toLocalDateTime_DifferentZoneIdSameOffset_TreatedAsEqual() {
        // GIVEN
        ZoneId utc = ZoneId.of("UTC");
        // same offset as UTC
        ZoneId gmt = ZoneId.of("GMT");
        ZonedDateTime source = ZonedDateTime.of(LocalDateTime.of(2023, 5, 15, 10, 30, 0), utc);
        when(options.getZoneId()).thenReturn(gmt);
        // WHEN
        LocalDateTime result = ZonedDateTimeConversions.toLocalDateTime(source, converter);
        // THEN
        assertEquals(source.toLocalDateTime(), result, "Zones with identical offsets (UTC vs GMT) should yield the original LocalDateTime");
    }

    @Test
    void toLocalDateTime_ConverterWithNullOptions_ThrowsNullPointerException() {
        // GIVEN
        ZonedDateTime source = ZonedDateTime.of(LocalDateTime.of(2023, 5, 15, 10, 30, 0), ZoneId.of("UTC"));
        // Configure the mock converter to return null for getOptions()
        when(converter.getOptions()).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toLocalDateTime(source, converter), "A null options object from the converter should cause a NullPointerException");
    }
}
