package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.time.*;
import java.util.Date;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StringConversions_toTimestamp_Test {

    /**
     * The Converter used by the tests – created once for the whole suite.
     */
    private static Converter converter;

    /**
     * The original default time‑zone, saved so we can restore it after the tests.
     */
    private static TimeZone originalDefaultZone;

    @BeforeAll
    static void setupBeforeAll() {
        // Preserve the original default zone and force UTC for deterministic results
        originalDefaultZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        // Initialise a real Converter with default options.
        // ConverterOptions has a public no‑arg constructor in the library.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore the original default zone so other test classes are not affected
        TimeZone.setDefault(originalDefaultZone);
    }

    @Test
    void toTimestamp_NullInput_ReturnsNull() {
        Timestamp ts = StringConversions.toTimestamp(null, converter);
        assertNull(ts, "Converting a null source should yield a null Timestamp");
    }

    @Test
    void toTimestamp_DateInput_ReturnsEquivalentTimestamp() {
        // 2021‑10‑01T00:00:00Z
        long epochMilli = 1_640_995_200_000L;
        Date date = new Date(epochMilli);
        Timestamp ts = StringConversions.toTimestamp(date, converter);
        assertNotNull(ts, "Timestamp should not be null for a valid Date");
        assertEquals(epochMilli, ts.getTime(), "Timestamp epoch millis must match the Date's epoch millis");
    }

    @Test
    void toTimestamp_InstantInput_ReturnsEquivalentTimestamp() {
        // 2021‑10‑01T00:00:00Z
        Instant instant = Instant.ofEpochMilli(1_640_995_200_000L);
        Timestamp ts = StringConversions.toTimestamp(instant, converter);
        assertNotNull(ts);
        assertEquals(instant.toEpochMilli(), ts.getTime());
    }

    @Test
    void toTimestamp_LocalDateTimeInput_ReturnsTimestampUsingDefaultZone() {
        // With the default zone forced to UTC, the conversion should be straightforward.
        LocalDateTime ldt = LocalDateTime.of(2021, 10, 1, 0, 0, 0);
        long expectedMillis = ldt.toInstant(ZoneOffset.UTC).toEpochMilli();
        Timestamp ts = StringConversions.toTimestamp(ldt, converter);
        assertNotNull(ts);
        assertEquals(expectedMillis, ts.getTime(), "Timestamp should represent the same instant as the LocalDateTime in UTC");
    }

    @Test
    void toTimestamp_ZonedDateTimeInput_ReturnsEquivalentTimestamp() {
        ZonedDateTime zdt = ZonedDateTime.of(2021, 10, 1, 12, 34, 56, 0, // UTC‑4 at this date
        ZoneId.of("America/New_York"));
        long expectedMillis = zdt.toInstant().toEpochMilli();
        Timestamp ts = StringConversions.toTimestamp(zdt, converter);
        assertNotNull(ts);
        assertEquals(expectedMillis, ts.getTime(), "Timestamp must match the epoch millis of the supplied ZonedDateTime");
    }

    @Test
    void toTimestamp_StringIso8601_ReturnsParsedTimestamp() {
        // ISO‑8601 format that the library knows how to parse
        String iso = "2021-10-01T15:30:45Z";
        Instant instant = Instant.parse(iso);
        long expectedMillis = instant.toEpochMilli();
        Timestamp ts = StringConversions.toTimestamp(iso, converter);
        assertNotNull(ts);
        assertEquals(expectedMillis, ts.getTime(), "String in ISO‑8601 format should be parsed to the correct Timestamp");
    }

    @Test
    void toTimestamp_UnparsableString_ReturnsNull() {
        // A string that cannot be interpreted as a date/time should result in null
        String bad = "not-a-date";
        Timestamp ts = StringConversions.toTimestamp(bad, converter);
        assertNull(ts, "Unparsable string should yield a null Timestamp");
    }

    /**
     * Group: "toTimestamp - handling of null Instant"
     * <p>
     * When the source object is null the internal call to {@code toInstant} returns null,
     * therefore {@code toTimestamp} must return null.
     */
    @Test
    void toTimestamp_NullSource_WithNullConverter_ReturnsNull() {
        // Both parameters are null – the method should gracefully return null
        Timestamp ts = StringConversions.toTimestamp(null, null);
        assertNull(ts, "Both arguments null should result in a null Timestamp");
    }

    /**
     * Group: "toTimestamp - handling of null Instant"
     * <p>
     * A mock {@link Converter} that forces {@code toInstant} to return {@code null}
     * (simulating an unsupported conversion) – the method should propagate the null result.
     */
    @Test
    void toTimestamp_MockedConverterReturnsNull_ResultsInNullTimestamp() {
        // Arrange: a mock Converter that always returns null when asked for an Instant.
        Converter mockConverter = mock(Converter.class);
        // The real StringConversions.toInstant eventually calls Converter.convert(..., Instant.class)
        // We stub that generic call to return null for any input.
        when(mockConverter.convert(any(), eq(Instant.class))).thenReturn(null);
        // Act
        Timestamp ts = StringConversions.toTimestamp("anyString", mockConverter);
        // Assert
        assertNull(ts, "When the underlying conversion yields null, the Timestamp must be null");
    }

    /**
     * Group: "toTimestamp - null converter"
     * <p>
     * Passing a non‑null source object while the {@code Converter} argument is null
     * causes a {@link NullPointerException} inside {@code toInstant}, which must propagate.
     */
    @Test
    void toTimestamp_NullConverter_ThrowsNullPointerException() {
        String iso = "2023-01-01T00:00:00Z";
        assertThrows(NullPointerException.class, () -> StringConversions.toTimestamp(iso, null), "A null Converter should cause a NullPointerException");
    }

    /**
     * Group: "toTimestamp - invalid string format"
     * <p>
     * A String that cannot be parsed as an ISO‑8601 instant triggers a
     * {@link DateTimeParseException} during the internal {@code toInstant} conversion.
     */
    @Test
    void toTimestamp_InvalidString_ThrowsDateTimeParseException() {
        String bad = "not-a-timestamp";
        assertThrows(DateTimeParseException.class, () -> StringConversions.toTimestamp(bad, converter), "An unparsable string should cause DateTimeParseException");
    }

    /**
     * Group: "toTimestamp - unsupported type"
     * <p>
     * Providing an object of a type that {@code toInstant} does not know how to convert
     * results in an {@link IllegalArgumentException}.
     */
    @Test
    void toTimestamp_UnsupportedObject_ThrowsIllegalArgumentException() {
        Object unsupported = new Object();
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toTimestamp(unsupported, converter), "Supplying an unsupported type should raise IllegalArgumentException");
    }

    /**
     * Group: "toTimestamp - instant overflow"
     * <p>
     * When the source is {@link Instant#MAX} the internal call to
     * {@code instant.toEpochMilli()} overflows and throws a {@link DateTimeException},
     * which must be propagated.
     */
    @Test
    void toTimestamp_InstantMax_ThrowsDateTimeException() {
        Instant max = Instant.MAX;
        assertThrows(DateTimeException.class, () -> StringConversions.toTimestamp(max, converter), "Converting Instant.MAX should propagate DateTimeException due to overflow");
    }
}
