package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toOffsetDateTime_Test {

    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    /**
     * Helper that prepares a mock {@link Converter} returning the supplied {@link TimeZone}.
     */
    private void mockConverterWith(TimeZone tz) {
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getTimeZone()).thenReturn(tz);
    }

    @Test
    void toOffsetDateTime_utcZone_returnsCorrectOffsetDateTime() {
        // given
        LocalDate date = LocalDate.of(2023, 5, 15);
        TimeZone utc = TimeZone.getTimeZone("UTC");
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getTimeZone()).thenReturn(utc);
        // when
        OffsetDateTime result = LocalDateConversions.toOffsetDateTime(date, mockConverter);
        // then
        assertEquals(date, result.toLocalDate(), "Date component should match the input LocalDate");
        assertEquals(LocalTime.MIDNIGHT, result.toLocalTime(), "Time component should be midnight");
        assertEquals(ZoneOffset.UTC, result.getOffset(), "Offset should be UTC (0)");
    }

    @Test
    void toOffsetDateTime_newYorkZone_returnsCorrectOffsetDateTime() {
        // given
        LocalDate date = LocalDate.of(2023, 12, 1);
        TimeZone newYork = TimeZone.getTimeZone("America/New_York");
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getTimeZone()).thenReturn(newYork);
        // when
        OffsetDateTime result = LocalDateConversions.toOffsetDateTime(date, mockConverter);
        // then
        ZoneOffset expectedOffset = ZoneOffset.ofTotalSeconds(newYork.getOffset(System.currentTimeMillis()) / 1000);
        assertEquals(date, result.toLocalDate(), "Date component should match the input LocalDate");
        assertEquals(LocalTime.MIDNIGHT, result.toLocalTime(), "Time component should be midnight");
        assertEquals(expectedOffset, result.getOffset(), "Offset should correspond to New York time zone at current instant");
    }

    @Test
    void toOffsetDateTime_nonLocalDate_throwsClassCastException() {
        // given
        String notALocalDate = "2023-01-01";
        TimeZone utc = TimeZone.getTimeZone("UTC");
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getTimeZone()).thenReturn(utc);
        // when / then
        assertThrows(ClassCastException.class, () -> LocalDateConversions.toOffsetDateTime(notALocalDate, mockConverter), "Passing a non‑LocalDate should result in a ClassCastException");
    }

    @Test
    void toOffsetDateTime_europeBerlinStandardOffset() {
        // given
        // winter – standard offset +01:00
        LocalDate date = LocalDate.of(2023, 1, 10);
        TimeZone berlin = TimeZone.getTimeZone("Europe/Berlin");
        mockConverterWith(berlin);
        // when
        OffsetDateTime result = LocalDateConversions.toOffsetDateTime(date, mockConverter);
        // then
        ZoneOffset expected = ZoneOffset.ofTotalSeconds(berlin.getOffset(System.currentTimeMillis()) / 1000);
        assertEquals(date, result.toLocalDate());
        assertEquals(LocalTime.MIDNIGHT, result.toLocalTime());
        assertEquals(expected, result.getOffset(), "Offset should be the current Berlin offset (normally +01:00 in winter)");
    }

    @Test
    void toOffsetDateTime_europeBerlinDaylightSavingOffset() {
        // given
        // summer – DST offset +02:00
        LocalDate date = LocalDate.of(2023, 7, 15);
        TimeZone berlin = TimeZone.getTimeZone("Europe/Berlin");
        mockConverterWith(berlin);
        // when
        OffsetDateTime result = LocalDateConversions.toOffsetDateTime(date, mockConverter);
        // then
        ZoneOffset expected = ZoneOffset.ofTotalSeconds(berlin.getOffset(System.currentTimeMillis()) / 1000);
        assertEquals(date, result.toLocalDate());
        assertEquals(LocalTime.MIDNIGHT, result.toLocalTime());
        assertEquals(expected, result.getOffset(), "Offset should be the current Berlin offset (normally +02:00 in summer)");
    }

    @Test
    void toOffsetDateTime_americaNewYorkStandardOffset() {
        // given
        LocalDate date = LocalDate.of(2023, 3, 15);
        TimeZone newYork = TimeZone.getTimeZone("America/New_York");
        mockConverterWith(newYork);
        // when
        OffsetDateTime result = LocalDateConversions.toOffsetDateTime(date, mockConverter);
        // then
        ZoneOffset expected = ZoneOffset.ofTotalSeconds(newYork.getOffset(System.currentTimeMillis()) / 1000);
        assertEquals(date, result.toLocalDate());
        assertEquals(LocalTime.MIDNIGHT, result.toLocalTime());
        assertEquals(expected, result.getOffset(), "Offset should be the current New York offset (depends on DST at the moment of execution)");
    }

    @Test
    void toOffsetDateTime_asiaKolkataHalfHourOffset() {
        // given
        LocalDate date = LocalDate.of(2023, 3, 15);
        TimeZone kolkata = TimeZone.getTimeZone("Asia/Kolkata");
        mockConverterWith(kolkata);
        // when
        OffsetDateTime result = LocalDateConversions.toOffsetDateTime(date, mockConverter);
        // then
        ZoneOffset expected = ZoneOffset.ofTotalSeconds(kolkata.getOffset(System.currentTimeMillis()) / 1000);
        assertEquals(date, result.toLocalDate());
        assertEquals(LocalTime.MIDNIGHT, result.toLocalTime());
        assertEquals(expected, result.getOffset(), "Offset should be +05:30 for Asia/Kolkata");
    }

    @Test
    void toOffsetDateTime_nullFrom_throwsNullPointerException() {
        // given
        mockConverterWith(TimeZone.getTimeZone("UTC"));
        // when / then
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toOffsetDateTime(null, mockConverter), "Null `from` argument should cause a NullPointerException when OffsetDateTime.of is invoked");
    }

    @Test
    void toOffsetDateTime_nullConverter_throwsNullPointerException() {
        // given
        LocalDate date = LocalDate.of(2023, 3, 15);
        // when / then
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toOffsetDateTime(date, null), "Null `converter` should cause a NullPointerException when getOptions() is called");
    }

    @Test
    void toOffsetDateTime_converterWithNullOptions_throwsNullPointerException() {
        // given
        LocalDate date = LocalDate.of(2023, 3, 15);
        when(mockConverter.getOptions()).thenReturn(null);
        // when / then
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toOffsetDateTime(date, mockConverter), "Null options should cause a NullPointerException when getTimeZone() is called");
    }

    @Test
    void toOffsetDateTime_converterWithNullTimeZone_throwsNullPointerException() {
        // given
        LocalDate date = LocalDate.of(2023, 3, 15);
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getTimeZone()).thenReturn(null);
        // when / then
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toOffsetDateTime(date, mockConverter), "Null TimeZone should cause a NullPointerException when getOffset is invoked");
    }

    @Test
    void toOffsetDateTime_timeZoneOffsetBeyondAllowedRange_throwsDateTimeException() {
        // given a TimeZone that reports +20:00 (72000 seconds) which is outside ZoneOffset limits (±18:00)
        TimeZone tzPlus20 = new TimeZone() {

            @Override
            public String getID() {
                return "GMT+20";
            }

            @Override
            public int getRawOffset() {
                return 20 * 3600 * 1000;
            }

            @Override
            public int getOffset(long date) {
                return 20 * 3600 * 1000;
            }

            @Override
            public void setRawOffset(int offsetMillis) {
            }

            @Override
            public boolean useDaylightTime() {
                return false;
            }

            @Override
            public boolean inDaylightTime(java.util.Date date) {
                return false;
            }
        };
        LocalDate date = LocalDate.of(2023, 1, 1);
        mockConverterWith(tzPlus20);
        // when / then
        assertThrows(DateTimeException.class, () -> LocalDateConversions.toOffsetDateTime(date, mockConverter), "Offset of +20:00 exceeds the allowed range and should trigger DateTimeException");
    }

    @Test
    void toOffsetDateTime_timeZoneOffsetBeyondAllowedRangeNegative_throwsDateTimeException() {
        // given a TimeZone that reports -20:00 (‑72000 seconds)
        TimeZone tzMinus20 = new TimeZone() {

            @Override
            public String getID() {
                return "GMT-20";
            }

            @Override
            public int getRawOffset() {
                return -20 * 3600 * 1000;
            }

            @Override
            public int getOffset(long date) {
                return -20 * 3600 * 1000;
            }

            @Override
            public void setRawOffset(int offsetMillis) {
            }

            @Override
            public boolean useDaylightTime() {
                return false;
            }

            @Override
            public boolean inDaylightTime(java.util.Date date) {
                return false;
            }
        };
        LocalDate date = LocalDate.of(2023, 1, 1);
        mockConverterWith(tzMinus20);
        // when / then
        assertThrows(DateTimeException.class, () -> LocalDateConversions.toOffsetDateTime(date, mockConverter), "Offset of -20:00 exceeds the allowed range and should trigger DateTimeException");
    }
}
