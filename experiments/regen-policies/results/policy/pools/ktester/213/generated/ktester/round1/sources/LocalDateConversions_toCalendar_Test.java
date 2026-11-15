package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import org.mockito.Mock;
import org.mockito.Mockito;
import java.time.format.DateTimeParseException;
import java.util.TimeZone;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toCalendar_Test {

    /**
     * Converter used in every test – created in {@link #setupBeforeEach()}.
     */
    private Converter converter;

    /**
     * Fixed time‑zone used for deterministic results.
     */
    private static final TimeZone TEST_TZ = TimeZone.getTimeZone("UTC");

    @BeforeEach
    void setupBeforeEach() {
        // Build a real Converter with a known time‑zone option.
        ConverterOptions options = new ConverterOptions();
        options.setTimeZone(TEST_TZ);
        converter = new Converter(options);
    }

    /**
     * Verify that a {@link java.util.Date} is converted to a {@link Calendar}
     * with the same epoch‑millis value.
     */
    @Test
    void toCalendar_fromDate_returnsMatchingCalendar() {
        // GIVEN
        // 2021‑09‑01T00:00:00Z
        long epochMillis = 1_630_454_400_000L;
        Date source = new Date(epochMillis);
        // WHEN
        Calendar result = LocalDateConversions.toCalendar(source, converter);
        // THEN
        assertNotNull(result, "Resulting Calendar should not be null");
        assertEquals(TEST_TZ, result.getTimeZone(), "Calendar must use the Converter's time‑zone");
        assertEquals(epochMillis, result.getTimeInMillis(), "Calendar epoch millis must match source Date");
    }

    /**
     * Verify that a {@link LocalDateTime} is interpreted using the
     * Converter's time‑zone (UTC in this test) and converted correctly.
     */
    @Test
    void toCalendar_fromLocalDateTime_usesConverterTimeZone() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2021, 9, 1, 12, 30, 15);
        // Expected instant = 2021‑09‑01T12:30:15Z because the Converter's zone is UTC.
        long expectedMillis = ldt.toInstant(ZoneOffset.UTC).toEpochMilli();
        // WHEN
        Calendar result = LocalDateConversions.toCalendar(ldt, converter);
        // THEN
        assertNotNull(result);
        assertEquals(TEST_TZ, result.getTimeZone());
        assertEquals(expectedMillis, result.getTimeInMillis(), "Calendar should represent the same instant as the LocalDateTime interpreted in UTC");
    }

    /**
     * Verify that a {@link ZonedDateTime} with a non‑UTC offset is correctly
     * converted to the same instant, while the Calendar retains the
     * Converter's configured time‑zone.
     */
    @Test
    void toCalendar_fromZonedDateTime_preservesInstant() {
        // GIVEN
        ZonedDateTime zdt = ZonedDateTime.of(2021, 9, 1, 8, 0, 0, 0, // 2021‑09‑01T08:00‑05:00 == 2021‑09‑01T13:00Z
        ZoneOffset.ofHours(-5));
        long expectedMillis = zdt.toInstant().toEpochMilli();
        // WHEN
        Calendar result = LocalDateConversions.toCalendar(zdt, converter);
        // THEN
        assertNotNull(result);
        assertEquals(TEST_TZ, result.getTimeZone(), "Calendar must keep the Converter's time‑zone, not the source zone");
        assertEquals(expectedMillis, result.getTimeInMillis(), "Calendar epoch millis must match the source ZonedDateTime instant");
    }

    /**
     * Verify that an {@link Instant} is converted correctly.
     */
    @Test
    void toCalendar_fromInstant_returnsMatchingCalendar() {
        // GIVEN
        Instant instant = Instant.parse("2022-01-15T10:20:30.123Z");
        long expectedMillis = instant.toEpochMilli();
        // WHEN
        Calendar result = LocalDateConversions.toCalendar(instant, converter);
        // THEN
        assertNotNull(result);
        assertEquals(TEST_TZ, result.getTimeZone());
        assertEquals(expectedMillis, result.getTimeInMillis(), "Calendar should have the same epoch millis as the source Instant");
    }

    /**
     * Passing {@code null} as the source object should cause a {@link NullPointerException}
     * because {@code toZonedDateTime} validates its input.
     */
    @Test
    void testToCalendarNullFrom() {
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toCalendar(null, converter), "Null source should trigger NullPointerException");
    }

    /**
     * Passing {@code null} as the {@link Converter} must result in a
     * {@link NullPointerException} when the method tries to call
     * {@code converter.getOptions()}.
     */
    @Test
    void testToCalendarNullConverter() {
        Date source = new Date();
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toCalendar(source, null), "Null converter should trigger NullPointerException");
    }

    /**
     * If {@code converter.getOptions()} returns {@code null}, the subsequent
     * call to {@code getTimeZone()} will throw a {@link NullPointerException}.
     */
    @Test
    void testToCalendarNullOptions() {
        // Mock a Converter that returns null options.
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.getOptions()).thenReturn(null);
        Date source = new Date();
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toCalendar(source, mockConverter), "Null options should trigger NullPointerException");
    }

    /**
     * When the {@link ConverterOptions} returns a {@code null} {@link TimeZone},
     * {@code Calendar.getInstance(null)} throws a {@link NullPointerException}.
     */
    @Test
    void testToCalendarNullTimeZone() {
        // Mock ConverterOptions to return null timezone.
        ConverterOptions mockOptions = mock(ConverterOptions.class);
        when(mockOptions.getTimeZone()).thenReturn(null);
        // Mock Converter to return the mocked options.
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        Date source = new Date();
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toCalendar(source, mockConverter), "Null TimeZone should trigger NullPointerException");
    }

    /**
     * Passing an object of an unsupported type (e.g., {@link Boolean}) must
     * result in an {@link IllegalArgumentException} from {@code toZonedDateTime}.
     */
    @Test
    void testToCalendarUnsupportedType() {
        Boolean unsupported = Boolean.TRUE;
        assertThrows(IllegalArgumentException.class, () -> LocalDateConversions.toCalendar(unsupported, converter), "Unsupported source type should trigger IllegalArgumentException");
    }

    /**
     * A malformed date‑time string cannot be parsed into a {@link ZonedDateTime},
     * so the underlying parser throws a {@link DateTimeParseException},
     * which should propagate out of {@code toCalendar}.
     */
    @Test
    void testToCalendarInvalidStringFormat() {
        // impossible date and time
        String malformed = "2023-02-30T25:61:00";
        assertThrows(DateTimeParseException.class, () -> LocalDateConversions.toCalendar(malformed, converter), "Malformed date‑time string should trigger DateTimeParseException");
    }

    /**
     * Convert a {@link java.util.Date} representing 2023‑03‑15T12:30:45.123Z
     * using the default UTC converter and verify the exact millisecond value.
     */
    @Test
    void testToCalendarFromDate() {
        // epoch millis for 2023‑03‑15T12:30:45.123Z
        long expectedMillis = 1678884645123L;
        Date source = new Date(expectedMillis);
        Calendar result = LocalDateConversions.toCalendar(source, converter);
        assertNotNull(result);
        assertEquals(TEST_TZ, result.getTimeZone(), "Calendar must use UTC");
        assertEquals(expectedMillis, result.getTimeInMillis(), "Calendar epoch millis must match the source Date");
    }
}
