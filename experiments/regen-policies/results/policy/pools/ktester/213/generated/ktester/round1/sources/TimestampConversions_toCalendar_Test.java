package com.cedarsoftware.util.convert;

import java.sql.Timestamp;
import java.util.Calendar;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Date;
import java.util.TimeZone;
import org.mockito.Mock;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimestampConversions_toCalendar_Test {

    private Converter converter;

    /**
     * Create a {@link Converter} that will be used for all tests.
     * The default {@link DefaultConverterOptions} are sufficient – they expose the
     * system default {@link java.util.TimeZone} which the {@code TimestampConversions.toCalendar}
     * method relies on.
     */
    @BeforeEach
    void setupBeforeEach() {
        converter = new Converter(new DefaultConverterOptions());
    }

    /**
     * Verify that {@code TimestampConversions.toCalendar(Object, Converter)} returns a
     * {@link Calendar} whose epoch‑millisecond value matches the supplied {@link Timestamp}
     * and whose time‑zone is the one configured in the {@link Converter}.
     */
    @Test
    void testToCalendarMatchesTimestampAndTimeZone() {
        // given – a concrete Timestamp instance
        Timestamp ts = Timestamp.valueOf("2023-06-15 12:34:56.123456789");
        // when – converting the Timestamp to a Calendar via the static method
        Calendar cal = TimestampConversions.toCalendar(ts, converter);
        // then – the Calendar reflects the same instant as the Timestamp
        assertEquals(ts.getTime(), cal.getTimeInMillis(), "Calendar milliseconds should equal Timestamp milliseconds");
        // and – the Calendar uses the Converter's configured TimeZone
        assertEquals(converter.getOptions().getTimeZone(), cal.getTimeZone(), "Calendar TimeZone should be the one from Converter options");
    }

    /**
     * Additional sanity check: converting a Timestamp that represents the epoch (0 ms)
     * should yield a Calendar whose fields correspond to the epoch in the configured zone.
     */
    @Test
    void testToCalendarWithEpochTimestamp() {
        // given – epoch Timestamp
        Timestamp epochTs = new Timestamp(0L);
        // when
        Calendar cal = TimestampConversions.toCalendar(epochTs, converter);
        // then
        assertEquals(0L, cal.getTimeInMillis(), "Calendar should represent the epoch (0 ms) exactly");
        assertEquals(converter.getOptions().getTimeZone(), cal.getTimeZone(), "Calendar TimeZone should still match the Converter's TimeZone");
    }

    /**
     * Valid conversion – UTC timezone.
     */
    @Test
    void testValidConversionUtcTimezone() {
        // given
        Timestamp ts = Timestamp.valueOf("2023-01-15 12:34:56.789");
        Converter mockConverter = mock(Converter.class);
        var mockOptions = mock(DefaultConverterOptions.class);
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getTimeZone()).thenReturn(TimeZone.getTimeZone("UTC"));
        // when
        Calendar cal = TimestampConversions.toCalendar(ts, mockConverter);
        // then
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone(), "Calendar should use UTC time zone");
        assertEquals(ts.getTime(), cal.getTimeInMillis(), "Calendar milliseconds should equal Timestamp milliseconds");
    }

    /**
     * Valid conversion – non‑UTC timezone (America/New_York).
     */
    @Test
    void testValidConversionNonUtcTimezone() {
        // given
        Timestamp ts = Timestamp.valueOf("2023-06-01 08:00:00.000");
        Converter mockConverter = mock(Converter.class);
        var mockOptions = mock(DefaultConverterOptions.class);
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getTimeZone()).thenReturn(TimeZone.getTimeZone("America/New_York"));
        // when
        Calendar cal = TimestampConversions.toCalendar(ts, mockConverter);
        // then
        assertEquals(TimeZone.getTimeZone("America/New_York"), cal.getTimeZone(), "Calendar should use America/New_York time zone");
        assertEquals(ts.getTime(), cal.getTimeInMillis(), "Calendar milliseconds should equal Timestamp milliseconds");
    }

    /**
     * Null handling – null source object should raise {@link NullPointerException}.
     */
    @Test
    void testNullSourceObjectThrowsNPE() {
        // given
        Converter mockConverter = mock(Converter.class);
        var mockOptions = mock(DefaultConverterOptions.class);
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getTimeZone()).thenReturn(TimeZone.getTimeZone("UTC"));
        // when / then
        assertThrows(NullPointerException.class, () -> TimestampConversions.toCalendar(null, mockConverter), "Passing null as the source object should throw NullPointerException");
    }

    /**
     * Null handling – null converter should raise {@link NullPointerException}.
     */
    @Test
    void testNullConverterThrowsNPE() {
        // given
        Timestamp ts = Timestamp.valueOf("2022-12-31 23:59:59.999");
        // when / then
        assertThrows(NullPointerException.class, () -> TimestampConversions.toCalendar(ts, null), "Passing null as the converter should throw NullPointerException");
    }

    /**
     * Invalid source type – a non‑Timestamp object should cause {@link ClassCastException}.
     */
    @Test
    void testInvalidSourceTypeThrowsClassCastException() {
        // given
        Date notATimestamp = new Date();
        // reuse the real converter from @BeforeEach
        // when / then
        assertThrows(ClassCastException.class, () -> TimestampConversions.toCalendar(notATimestamp, converter), "Providing a non‑Timestamp object should throw ClassCastException");
    }

    /**
     * Converter with {@code null} options should raise {@link NullPointerException}
     * when the method attempts to retrieve the time‑zone.
     */
    @Test
    void testConverterWithNullOptionsThrowsNPE() {
        // given
        Timestamp ts = Timestamp.valueOf("2021-05-10 10:10:10.010");
        Converter mockConverter = mock(Converter.class);
        // options are null
        when(mockConverter.getOptions()).thenReturn(null);
        // when / then
        assertThrows(NullPointerException.class, () -> TimestampConversions.toCalendar(ts, mockConverter), "Null options in Converter should cause NullPointerException");
    }

    /**
     * Converter options return {@code null} time‑zone – {@link Calendar#getInstance(null)}
     * throws {@link NullPointerException}.
     */
    @Test
    void testConverterWithNullTimeZoneThrowsNPE() {
        // given
        Timestamp ts = Timestamp.valueOf("2020-01-01 00:00:00.000");
        Converter mockConverter = mock(Converter.class);
        var mockOptions = mock(DefaultConverterOptions.class);
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        // time zone is null
        when(mockOptions.getTimeZone()).thenReturn(null);
        // when / then
        assertThrows(NullPointerException.class, () -> TimestampConversions.toCalendar(ts, mockConverter), "Null TimeZone in options should cause NullPointerException");
    }

    /**
     * Nanosecond truncation – the {@link Calendar} should contain only the millisecond part
     * of the {@link Timestamp}; any nanoseconds beyond the millisecond are discarded.
     */
    @Test
    void testNanosecondTruncation() {
        // given – Timestamp with nanosecond precision
        Timestamp ts = Timestamp.valueOf("2023-01-15 12:34:56.123456789");
        // Expected epoch millis (truncated to milliseconds)
        // Timestamp#getTime already truncates to ms
        long expectedMillis = ts.getTime();
        // the millisecond component
        int expectedMilliOfSecond = 123;
        // when
        Calendar cal = TimestampConversions.toCalendar(ts, converter);
        // then
        assertEquals(TimeZone.getDefault(), cal.getTimeZone(), "Calendar should use the Converter's (default) time zone");
        assertEquals(expectedMillis, cal.getTimeInMillis(), "Calendar milliseconds should match truncated Timestamp milliseconds");
        assertEquals(expectedMilliOfSecond, cal.get(Calendar.MILLISECOND), "Calendar.MILLISECOND field should reflect the truncated millisecond part");
    }
}
