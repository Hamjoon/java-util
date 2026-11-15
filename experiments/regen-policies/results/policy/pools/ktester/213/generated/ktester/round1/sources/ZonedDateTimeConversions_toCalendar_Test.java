package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Calendar;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toCalendar_Test {

    @Mock
    private ConverterOptions // will be injected by Mockito
    mockOptions;

    @Mock
    private Converter // will be injected by Mockito
    mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mock hierarchy:
        // converter.getOptions() -> mockOptions
        // mockOptions.getTimeZone() -> a deterministic TimeZone
        when(mockConverter.getOptions()).thenReturn(mockOptions);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test interference
        reset(mockOptions, mockConverter);
    }

    @Test
    void toCalendar_convertsZonedDateTimeUsingConverterTimeZone() {
        // GIVEN – a ZonedDateTime and a Converter whose options return a known TimeZone
        ZonedDateTime zdt = ZonedDateTime.of(// year, month, day
        2023, // year, month, day
        5, // year, month, day
        10, // hour, minute, second, nano
        15, // hour, minute, second, nano
        30, // hour, minute, second, nano
        45, // hour, minute, second, nano
        123_456_789, // zone of the source value
        ZoneId.of("UTC"));
        TimeZone expectedTimeZone = TimeZone.getTimeZone("America/New_York");
        when(mockOptions.getTimeZone()).thenReturn(expectedTimeZone);
        // WHEN – the static conversion method is invoked
        Calendar result = ZonedDateTimeConversions.toCalendar(zdt, mockConverter);
        // THEN – the Calendar reflects the same instant as the ZonedDateTime
        assertNotNull(result, "Resulting Calendar should not be null");
        assertEquals(zdt.toInstant().toEpochMilli(), result.getTimeInMillis(), "Calendar milliseconds must match ZonedDateTime instant");
        // AND – the Calendar uses the TimeZone supplied by the ConverterOptions
        assertEquals(expectedTimeZone, result.getTimeZone(), "Calendar TimeZone must be the one provided by ConverterOptions");
    }

    @Test
    void toCalendar_throwsClassCastExceptionWhenSourceIsNotZonedDateTime() {
        // GIVEN – an object that is not a ZonedDateTime
        Object notZdt = "not a ZonedDateTime";
        when(mockOptions.getTimeZone()).thenReturn(TimeZone.getDefault());
        // WHEN / THEN – a ClassCastException is expected
        assertThrows(ClassCastException.class, () -> ZonedDateTimeConversions.toCalendar(notZdt, mockConverter));
    }

    @Test
    void toCalendar_standardConversion_UtcTimeZone() {
        // GIVEN – a ZonedDateTime in America/New_York and a Converter that forces UTC
        ZonedDateTime zdt = ZonedDateTime.of(2023, 3, 15, 10, 30, 45, 123_000_000, ZoneId.of("America/New_York"));
        when(mockOptions.getTimeZone()).thenReturn(TimeZone.getTimeZone("UTC"));
        // WHEN
        Calendar cal = ZonedDateTimeConversions.toCalendar(zdt, mockConverter);
        // THEN – Calendar uses UTC and epoch millis are preserved
        assertNotNull(cal);
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
        assertEquals(1678888245123L, cal.getTimeInMillis(), "Epoch milliseconds must match expected value");
    }

    @Test
    void toCalendar_leapDayConversion_DifferentTargetZone() {
        // GIVEN – 2020‑02‑29 23:59:59 in Europe/Paris, target zone Asia/Tokyo
        ZonedDateTime zdt = ZonedDateTime.of(2020, 2, 29, 23, 59, 59, 0, ZoneId.of("Europe/Paris"));
        when(mockOptions.getTimeZone()).thenReturn(TimeZone.getTimeZone("Asia/Tokyo"));
        // WHEN
        Calendar cal = ZonedDateTimeConversions.toCalendar(zdt, mockConverter);
        // THEN – epoch millis unchanged, target zone applied
        assertNotNull(cal);
        assertEquals(TimeZone.getTimeZone("Asia/Tokyo"), cal.getTimeZone());
        assertEquals(1583020799000L, cal.getTimeInMillis(), "Epoch milliseconds must match expected leap‑day value");
    }

    @Test
    void toCalendar_epochInstant_DefaultTimeZone() {
        // GIVEN – Instant.EPOCH and default system time zone
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC"));
        when(mockOptions.getTimeZone()).thenReturn(TimeZone.getDefault());
        // WHEN
        Calendar cal = ZonedDateTimeConversions.toCalendar(zdt, mockConverter);
        // THEN
        assertNotNull(cal);
        assertEquals(TimeZone.getDefault(), cal.getTimeZone());
        assertEquals(0L, cal.getTimeInMillis(), "Epoch milliseconds for Instant.EPOCH must be zero");
    }

    @Test
    void toCalendar_nullSource_throwsNullPointerException() {
        // GIVEN
        when(mockOptions.getTimeZone()).thenReturn(TimeZone.getTimeZone("UTC"));
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toCalendar(null, mockConverter));
    }

    @Test
    void toCalendar_nullConverter_throwsNullPointerException() {
        // GIVEN – a valid ZonedDateTime but a null converter
        ZonedDateTime zdt = ZonedDateTime.now();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toCalendar(zdt, null));
    }

    @Test
    void toCalendar_nanosecondPrecisionTruncated() {
        // GIVEN – nanoseconds beyond millis are dropped, target zone Europe/Berlin
        ZonedDateTime zdt = ZonedDateTime.of(2021, 5, 20, 12, 34, 56, 987_654_321, ZoneId.of("UTC"));
        when(mockOptions.getTimeZone()).thenReturn(TimeZone.getTimeZone("Europe/Berlin"));
        // WHEN
        Calendar cal = ZonedDateTimeConversions.toCalendar(zdt, mockConverter);
        // THEN
        assertNotNull(cal);
        assertEquals(TimeZone.getTimeZone("Europe/Berlin"), cal.getTimeZone());
        assertEquals(1621514096987L, cal.getTimeInMillis(), "Millis should be truncated from nanoseconds");
    }

    @Test
    void toCalendar_wrongInputType_throwsClassCastException() {
        // GIVEN – a LocalDateTime instead of ZonedDateTime
        LocalDateTime ldt = LocalDateTime.of(2022, 1, 1, 0, 0);
        when(mockOptions.getTimeZone()).thenReturn(TimeZone.getTimeZone("UTC"));
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> ZonedDateTimeConversions.toCalendar(ldt, mockConverter));
    }

    @Test
    void toCalendar_optionsReturnsNullTimeZone_throwsNullPointerException() {
        // GIVEN – Options returns null TimeZone
        ZonedDateTime zdt = ZonedDateTime.of(2023, 7, 1, 10, 0, 0, 0, ZoneId.of("America/New_York"));
        // null zone
        when(mockOptions.getTimeZone()).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toCalendar(zdt, mockConverter));
    }

    @Test
    void toCalendar_extremeFutureDate() {
        // GIVEN – year 9999‑12‑31 23:59:59 UTC, target zone UTC
        ZonedDateTime zdt = ZonedDateTime.of(9999, 12, 31, 23, 59, 59, 0, ZoneId.of("UTC"));
        when(mockOptions.getTimeZone()).thenReturn(TimeZone.getTimeZone("UTC"));
        // WHEN
        Calendar cal = ZonedDateTimeConversions.toCalendar(zdt, mockConverter);
        // THEN
        assertNotNull(cal);
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
        assertEquals(253402300799000L, cal.getTimeInMillis(), "Epoch milliseconds for the far‑future date must match");
    }

    @Test
    void toCalendar_sameInstantDifferentSourceZone() {
        // GIVEN – source zone America/New_York, instant 1650000000123L, target UTC
        long instantMillis = 1_650_000_000_123L;
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(instantMillis), ZoneId.of("America/New_York"));
        when(mockOptions.getTimeZone()).thenReturn(TimeZone.getTimeZone("UTC"));
        // WHEN
        Calendar cal = ZonedDateTimeConversions.toCalendar(zdt, mockConverter);
        // THEN
        assertNotNull(cal);
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
        assertEquals(instantMillis, cal.getTimeInMillis(), "Epoch milliseconds must be preserved regardless of source zone");
    }

    @Test
    void toCalendar_customGmtOffsetTimeZone() {
        // GIVEN – source Asia/Tokyo, target GMT+05:30 (custom offset)
        ZonedDateTime zdt = ZonedDateTime.of(2024, 1, 15, 8, 45, 30, 0, ZoneId.of("Asia/Tokyo"));
        TimeZone customTz = TimeZone.getTimeZone("GMT+05:30");
        when(mockOptions.getTimeZone()).thenReturn(customTz);
        // WHEN
        Calendar cal = ZonedDateTimeConversions.toCalendar(zdt, mockConverter);
        // THEN
        assertNotNull(cal);
        assertEquals(customTz, cal.getTimeZone());
        assertEquals(1705271130000L, cal.getTimeInMillis(), "Epoch milliseconds must match the instant irrespective of custom offset zone");
    }

    @Test
    void toCalendar_converterReturnsNullOptions_throwsNullPointerException() {
        // GIVEN – a valid ZonedDateTime but converter.getOptions() returns null
        ZonedDateTime zdt = ZonedDateTime.now();
        // Options is null
        when(mockConverter.getOptions()).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toCalendar(zdt, mockConverter));
    }
}
