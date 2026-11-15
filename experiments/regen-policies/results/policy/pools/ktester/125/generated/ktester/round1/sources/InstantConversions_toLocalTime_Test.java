package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.Date;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class InstantConversions_toLocalTime_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The Converter needs a non‑null options instance; the default options are fine.
        converter = new Converter(new ConverterOptions());
    }

    private static Stream<Arguments> validConversionsProvider() {
        return Stream.of(// Instant
        Arguments.of(Instant.parse("2023-03-25T10:15:30.123Z"), LocalTime.of(10, 15, 30, 123_000_000), "Instant → LocalTime (UTC default zone)"), // java.util.Date
        Arguments.of(Date.from(Instant.parse("2023-03-25T10:15:30.123Z")), LocalTime.of(10, 15, 30, 123_000_000), "Date → LocalTime (UTC default zone)"), // java.sql.Timestamp
        Arguments.of(Timestamp.from(Instant.parse("2023-03-25T10:15:30.123Z")), LocalTime.of(10, 15, 30, 123_000_000), "Timestamp → LocalTime (preserves nanoseconds)"), // LocalDateTime (treated as default zone)
        Arguments.of(LocalDateTime.of(2023, 3, 25, 10, 15, 30, 123_000_000), LocalTime.of(10, 15, 30, 123_000_000), "LocalDateTime → LocalTime (default zone)"), // ZonedDateTime with a non‑default zone (America/New_York)
        Arguments.of(ZonedDateTime.of(2023, 3, 25, 10, 15, 30, 0, ZoneId.of("America/New_York")), LocalTime.of(10, 15, 30), "ZonedDateTime (America/New_York) → LocalTime (zone retained)"), // OffsetDateTime with -05:00 offset
        Arguments.of(OffsetDateTime.of(2023, 3, 25, 10, 15, 30, 0, ZoneOffset.of("-05:00")), LocalTime.of(10, 15, 30), "OffsetDateTime (‑05:00) → LocalTime"), // Calendar with custom zone (America/Los_Angeles) and millis
        Arguments.of((() -> {
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"));
            cal.set(2023, Calendar.MARCH, 25, 10, 15, 30);
            cal.set(Calendar.MILLISECOND, 123);
            return cal;
        }).call(), LocalTime.of(10, 15, 30, 123_000_000), "Calendar (America/Los_Angeles) → LocalTime (preserves fields)"), // Long epoch millis
        Arguments.of(// 2023‑03‑25T10:15:30.123Z
        1679748930123L, LocalTime.of(10, 15, 30, 123_000_000), "Long epoch millis → LocalTime (UTC default zone)"), // LocalDate (midnight)
        Arguments.of(LocalDate.of(2023, 3, 25), LocalTime.MIDNIGHT, "LocalDate → LocalTime (start‑of‑day)"));
    }

    private static Stream<Arguments> invalidConversionsProvider() {
        return Stream.of(// Unsupported type Boolean
        Arguments.of(Boolean.TRUE, DefaultConverter.INSTANCE, IllegalArgumentException.class, "Unsupported source type Boolean should raise IllegalArgumentException"), // Null input object
        Arguments.of(null, DefaultConverter.INSTANCE, NullPointerException.class, "Null source should raise NullPointerException"), // Null Converter
        Arguments.of(Instant.parse("2023-03-25T10:15:30.123Z"), null, NullPointerException.class, "Null Converter should raise NullPointerException"), // Unparseable String
        Arguments.of("not-a-date-time", DefaultConverter.INSTANCE, DateTimeParseException.class, "Invalid string should cause DateTimeParseException"), // Unsupported Number type (Double)
        Arguments.of(12345.678, DefaultConverter.INSTANCE, IllegalArgumentException.class, "Double (non‑Long) should raise IllegalArgumentException"), // java.sql.Date (date‑only)
        Arguments.of(java.sql.Date.valueOf("2023-03-25"), DefaultConverter.INSTANCE, IllegalArgumentException.class, "java.sql.Date (date‑only) should raise IllegalArgumentException"));
    }

    @Test
    void toLocalTime_fromInstant_returnsCorrectLocalTime() {
        // GIVEN
        // 1970‑01‑01T00:00:00Z
        Instant instant = Instant.ofEpochSecond(0L);
        LocalTime expected = LocalTime.MIDNIGHT;
        // WHEN
        LocalTime actual = InstantConversions.toLocalTime(instant, converter);
        // THEN
        assertEquals(expected, actual, "LocalTime derived from Instant should be midnight (UTC).");
    }

    @Test
    void toLocalTime_fromDate_returnsCorrectLocalTime() {
        // GIVEN
        // 2023‑03‑15T12:34:56Z
        Instant instant = Instant.parse("2023-03-15T12:34:56Z");
        Date date = Date.from(instant);
        LocalTime expected = LocalTime.of(12, 34, 56);
        // WHEN
        LocalTime actual = InstantConversions.toLocalTime(date, converter);
        // THEN
        assertEquals(expected, actual, "LocalTime derived from java.util.Date should match the instant's time component.");
    }

    @Test
    void toLocalTime_fromLongEpochMilli_returnsCorrectLocalTime() {
        // GIVEN
        // 2022‑01‑01T00:00:00Z
        long epochMilli = 1_640_995_200_000L;
        LocalTime expected = LocalTime.MIDNIGHT;
        // WHEN
        LocalTime actual = InstantConversions.toLocalTime(epochMilli, converter);
        // THEN
        assertEquals(expected, actual, "LocalTime derived from epoch milliseconds should be midnight (UTC).");
    }

    @Test
    void toLocalTime_nullInput_throwsNullPointerException() {
        // GIVEN
        Object nullValue = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> InstantConversions.toLocalTime(nullValue, converter), "Passing a null source should result in NPE (the underlying toZonedDateTime cannot handle null).");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validConversionsProvider")
    void toLocalTime_validConversions(Object from, LocalTime expected, String description) {
        // Preserve original default zone and enforce UTC for the duration of the test
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            LocalTime actual = InstantConversions.toLocalTime(from, DefaultConverter.INSTANCE);
            assertEquals(expected, actual, description);
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("invalidConversionsProvider")
    void toLocalTime_invalidConversions(Object from, Converter conv, Class<? extends Throwable> expectedException, String description) {
        // For the cases that rely on the default zone we still enforce UTC to keep the
        // environment deterministic, but the exception is thrown before any zone logic.
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            assertThrows(expectedException, () -> InstantConversions.toLocalTime(from, conv), description);
        } finally {
            TimeZone.setDefault(original);
        }
    }
}
