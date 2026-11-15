package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toCalendar_Test {

    /**
     * Mocked options that the {@link Converter} will use.
     */
    @Mock
    ConverterOptions mockOptions;

    /**
     * Real {@link Converter} instance that delegates to the mocked options.
     */
    Converter converter;

    /**
     * A fixed time‑zone used for deterministic results.
     */
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mocked options to always return UTC.
        when(mockOptions.getTimeZone()).thenReturn(UTC);
        // The options may also be asked for converter overrides; return an empty map.
        when(mockOptions.getConverterOverrides()).thenReturn(java.util.Collections.emptyMap());
        // Build a real Converter that holds the mocked options.
        converter = new Converter(mockOptions);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test interference.
        clearInvocations(mockOptions);
    }

    /**
     * Verify that a {@link java.util.Date} is correctly transformed into a {@link Calendar}
     * whose instant (millis) matches the original date and whose time‑zone is the one
     * supplied by {@link ConverterOptions}.
     */
    @Test
    void testToCalendarFromDate() {
        // GIVEN – a known instant (2023‑01‑01T12:34:56Z) expressed as java.util.Date
        Instant instant = Instant.parse("2023-01-01T12:34:56Z");
        Date date = Date.from(instant);
        // WHEN – converting the Date to a Calendar via the focal method
        Calendar calendar = LocalDateConversions.toCalendar(date, converter);
        // THEN – the Calendar should use the UTC zone and contain the same epoch millis
        assertNotNull(calendar, "Calendar must not be null");
        assertEquals(UTC, calendar.getTimeZone(), "Calendar must use the UTC time‑zone");
        assertEquals(instant.toEpochMilli(), calendar.getTimeInMillis(), "Calendar epoch millis must match the source Date");
    }

    /**
     * Verify that an {@link java.time.Instant} is correctly transformed into a {@link Calendar}.
     */
    @Test
    void testToCalendarFromInstant() {
        // GIVEN – a known instant (2022‑12‑31T23:59:59Z)
        Instant instant = Instant.parse("2022-12-31T23:59:59Z");
        // WHEN – converting the Instant to a Calendar via the focal method
        Calendar calendar = LocalDateConversions.toCalendar(instant, converter);
        // THEN – the Calendar should reflect the same instant in UTC
        assertNotNull(calendar, "Calendar must not be null");
        assertEquals(UTC, calendar.getTimeZone(), "Calendar must use the UTC time‑zone");
        assertEquals(instant.toEpochMilli(), calendar.getTimeInMillis(), "Calendar epoch millis must match the source Instant");
    }

    /**
     * Verify that a {@link java.time.LocalDateTime} is converted using the time‑zone
     * supplied by the {@link ConverterOptions}. The LocalDateTime has no zone information,
     * so the conversion should assume the options’ zone (UTC in this test).
     */
    @Test
    void testToCalendarFromLocalDateTime() {
        // GIVEN – a LocalDateTime representing 2021‑06‑15T08:15:30 (no zone)
        java.time.LocalDateTime ldt = java.time.LocalDateTime.of(2021, 6, 15, 8, 15, 30);
        // The expected instant is the same moment interpreted in UTC.
        Instant expected = ldt.atZone(ZoneId.of("UTC")).toInstant();
        // WHEN – converting the LocalDateTime to a Calendar
        Calendar calendar = LocalDateConversions.toCalendar(ldt, converter);
        // THEN – the Calendar must be in UTC and hold the expected epoch millis
        assertNotNull(calendar, "Calendar must not be null");
        assertEquals(UTC, calendar.getTimeZone(), "Calendar must use the UTC time‑zone");
        assertEquals(expected.toEpochMilli(), calendar.getTimeInMillis(), "Calendar epoch millis must match the LocalDateTime interpreted in UTC");
    }

    /**
     * Passing a {@code null} {@link Converter} must result in a {@link NullPointerException}
     * because the implementation immediately dereferences the argument to obtain options.
     */
    @Test
    void testToCalendar_nullConverter() {
        // GIVEN
        Date date = new Date(1_600_000_000_000L);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toCalendar(date, null), "Calling toCalendar with a null Converter should throw NPE");
    }

    /**
     * If the {@link Converter} returns {@code null} for {@link Converter#getOptions()},
     * a {@link NullPointerException} is expected when the method tries to call
     * {@code getOptions().getTimeZone()}.
     */
    @Test
    void testToCalendar_converterWithNullOptions() {
        // GIVEN – a mock Converter whose getOptions() returns null
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.getOptions()).thenReturn(null);
        Date date = new Date(1_600_000_000_000L);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toCalendar(date, mockConverter), "Converter with null options should cause NPE");
    }

    /**
     * When the {@link ConverterOptions#getTimeZone()} returns {@code null},
     * {@link Calendar#getInstance(TimeZone)} throws a {@link NullPointerException}.
     */
    @Test
    void testToCalendar_converterWithNullTimeZone() {
        // GIVEN – configure the shared mockOptions to return null for the time‑zone
        when(mockOptions.getTimeZone()).thenReturn(null);
        Date date = new Date(1_600_000_000_000L);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toCalendar(date, converter), "ConverterOptions returning null TimeZone should cause NPE");
    }

    /**
     * The focal method should reject a {@code null} source object.
     * The underlying {@code toZonedDateTime} implementation is expected to throw
     * {@link IllegalArgumentException} for a null input.
     */
    @Test
    void testToCalendar_nullFrom() {
        // GIVEN – a null source
        Object from = null;
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> LocalDateConversions.toCalendar(from, converter), "Null source object should cause IllegalArgumentException");
    }

    /**
     * Passing an unsupported source type (plain {@link Object}) must result in
     * {@link IllegalArgumentException} because no conversion path to {@link ZonedDateTime}
     * exists for that type.
     */
    @Test
    void testToCalendar_unsupportedFromType() {
        // GIVEN – an object of a type that cannot be converted
        Object unsupported = new Object();
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> LocalDateConversions.toCalendar(unsupported, converter), "Unsupported source type should cause IllegalArgumentException");
    }

    /**
     * A non‑parseable {@link String} should trigger an {@link IllegalArgumentException}
     * from the underlying conversion logic.
     */
    @Test
    void testToCalendar_invalidString() {
        // GIVEN – a string that does not represent a date/time
        String badString = "not-a-date";
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> LocalDateConversions.toCalendar(badString, converter), "Invalid date string should cause IllegalArgumentException");
    }
}
