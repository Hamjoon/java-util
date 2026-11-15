package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.format.DateTimeParseException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class StringConversions_toOffsetDateTime_Test {

    /**
     * Shared options and converter used by all test cases.
     */
    private static ConverterOptions options;

    private static Converter converter;

    /**
     * Original default time‑zone – restored after all tests.
     */
    private static TimeZone originalTimeZone;

    @BeforeAll
    static void setupBeforeAll() {
        // Default options – no custom overrides needed for these tests.
        options = new ConverterOptions();
        converter = new Converter(options);
        // Use UTC as the system default zone to make the results deterministic.
        originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        // Initialise a default Converter (no special options required for these tests).
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Help GC – not strictly necessary but keeps the pattern tidy.
        converter = null;
        options = null;
        // Restore the original default time‑zone.
        TimeZone.setDefault(originalTimeZone);
        converter = null;
    }

    @Test
    void shouldConvertIsoOffsetDateTimeString() {
        // GIVEN
        String iso = "2023-08-15T12:34:56+02:00";
        // WHEN
        OffsetDateTime result = StringConversions.toOffsetDateTime(iso, converter);
        // THEN
        assertNotNull(result, "Result should not be null for a valid ISO‑8601 offset date‑time string");
        assertEquals(OffsetDateTime.parse(iso), result, "The converted OffsetDateTime must match the parsed ISO string");
    }

    @Test
    void shouldReturnNullWhenSourceIsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        OffsetDateTime result = StringConversions.toOffsetDateTime(source, converter);
        // THEN
        assertNull(result, "Result should be null when the source object is null");
    }

    @Test
    void shouldReturnNullForUnparseableString() {
        // GIVEN
        String bad = "not-a-date";
        // WHEN
        OffsetDateTime result = StringConversions.toOffsetDateTime(bad, converter);
        // THEN
        assertNull(result, "Result should be null when the string cannot be parsed as an OffsetDateTime");
    }

    @Test
    void shouldConvertZonedDateTimeStringViaZonedParsing() {
        // GIVEN
        // A string that ZonedDateTime can parse but OffsetDateTime.parse cannot.
        // Example: includes a zone ID instead of an explicit offset.
        String zoned = "2023-08-15T12:34:56[Europe/Paris]";
        // WHEN
        OffsetDateTime result = StringConversions.toOffsetDateTime(zoned, converter);
        // THEN
        assertNotNull(result, "Result should not be null when the string is a valid ZonedDateTime");
        // The expected OffsetDateTime is the same instant with the zone's offset at that moment.
        OffsetDateTime expected = OffsetDateTime.parse("2023-08-15T12:34:56+02:00");
        assertEquals(expected, result, "The conversion should respect the zone offset derived from the zone ID");
    }

    @Test
    void shouldPropagateConversionExceptionAsNull() {
        // GIVEN
        // A string that causes a DateTimeParseException inside toZonedDateTime.
        String malformed = "2023-13-99T25:61:61Z";
        // WHEN
        OffsetDateTime result = StringConversions.toOffsetDateTime(malformed, converter);
        // THEN
        assertNull(result, "When parsing fails, the method should return null rather than throwing");
    }

    @Test
    void shouldThrowNullPointerExceptionWhenConverterIsNull() {
        // GIVEN
        String iso = "2023-08-15T12:34:56+02:00";
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> StringConversions.toOffsetDateTime(iso, null), "A null Converter should cause a NullPointerException");
    }

    @Test
    void unsupportedObjectShouldYieldNullResult() {
        // GIVEN
        Object unsupported = new Object();
        // WHEN
        OffsetDateTime result = StringConversions.toOffsetDateTime(unsupported, converter);
        // THEN
        assertNull(result, "Unsupported source types should result in a null conversion");
    }

    @Test
    void shouldConvertIsoUtcString() {
        String iso = "2023-08-15T12:34:56Z";
        OffsetDateTime result = StringConversions.toOffsetDateTime(iso, converter);
        assertNotNull(result);
        assertEquals(OffsetDateTime.parse(iso), result);
    }

    @Test
    void shouldConvertUtilDateEpoch() {
        // 1970‑01‑01T00:00:00Z
        Date date = new Date(0L);
        OffsetDateTime result = StringConversions.toOffsetDateTime(date, converter);
        assertNotNull(result);
        assertEquals(OffsetDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneOffset.UTC), result);
    }

    @Test
    void shouldConvertInstantEpoch() {
        Instant instant = Instant.ofEpochSecond(0L);
        OffsetDateTime result = StringConversions.toOffsetDateTime(instant, converter);
        assertNotNull(result);
        assertEquals(OffsetDateTime.ofInstant(instant, ZoneOffset.UTC), result);
    }

    @Test
    void shouldConvertLocalDateTimeAssumingUtc() {
        LocalDateTime ldt = LocalDateTime.of(2023, 8, 15, 12, 34, 56);
        OffsetDateTime result = StringConversions.toOffsetDateTime(ldt, converter);
        assertNotNull(result);
        // With the default zone set to UTC, the offset must be zero.
        assertEquals(OffsetDateTime.ofInstant(ldt.toInstant(ZoneOffset.UTC), ZoneOffset.UTC), result);
    }

    @Test
    void shouldConvertZonedDateTimeWithExplicitOffset() {
        ZonedDateTime zdt = ZonedDateTime.of(2023, 8, 15, 12, 34, 56, 0, ZoneOffset.ofHours(3));
        OffsetDateTime result = StringConversions.toOffsetDateTime(zdt, converter);
        assertNotNull(result);
        assertEquals(zdt.toOffsetDateTime(), result);
    }

    @Test
    void shouldConvertOffsetDateTimeDirectly() {
        OffsetDateTime odt = OffsetDateTime.of(2023, 8, 15, 12, 34, 56, 0, ZoneOffset.ofHoursMinutes(5, 30));
        OffsetDateTime result = StringConversions.toOffsetDateTime(odt, converter);
        assertNotNull(result);
        assertEquals(odt, result);
    }

    @Test
    void shouldConvertSqlTimestampEpoch() {
        Timestamp ts = new Timestamp(0L);
        OffsetDateTime result = StringConversions.toOffsetDateTime(ts, converter);
        assertNotNull(result);
        assertEquals(OffsetDateTime.ofInstant(ts.toInstant(), ZoneOffset.UTC), result);
    }

    @Test
    void shouldConvertCalendarWithExplicitTimeZone() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        // epoch instant
        cal.setTimeInMillis(0L);
        OffsetDateTime result = StringConversions.toOffsetDateTime(cal, converter);
        assertNotNull(result);
        ZoneId zone = ZoneId.of("America/New_York");
        OffsetDateTime expected = OffsetDateTime.ofInstant(Instant.ofEpochMilli(0), zone);
        assertEquals(expected, result);
    }

    @Test
    void shouldThrowDateTimeParseExceptionForUnparseableString() {
        String bad = "not-a-date";
        assertThrows(DateTimeParseException.class, () -> StringConversions.toOffsetDateTime(bad, converter), "Unparseable strings must result in DateTimeParseException");
    }

    @Test
    void shouldThrowDateTimeParseExceptionForMalformedOffsetString() {
        String malformed = "2023-08-15T12:34:56+25:00";
        assertThrows(DateTimeParseException.class, () -> StringConversions.toOffsetDateTime(malformed, converter));
    }

    @Test
    void shouldThrowDateTimeParseExceptionForBracketedZoneIdString() {
        String bracketed = "2023-08-15T12:34:56[Europe/Paris]";
        assertThrows(DateTimeParseException.class, () -> StringConversions.toOffsetDateTime(bracketed, converter));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionForBooleanSource() {
        Boolean bool = Boolean.TRUE;
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toOffsetDateTime(bool, converter));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionForZoneIdSource() {
        ZoneId zoneId = ZoneId.of("Europe/Paris");
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toOffsetDateTime(zoneId, converter));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionForZoneOffsetSource() {
        ZoneOffset offset = ZoneOffset.ofHours(3);
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toOffsetDateTime(offset, converter));
    }

    @Test
    void shouldThrowDateTimeExceptionForLocalDateOnly() {
        LocalDate date = LocalDate.of(2023, 8, 15);
        assertThrows(DateTimeException.class, () -> StringConversions.toOffsetDateTime(date, converter));
    }

    @Test
    void shouldThrowDateTimeExceptionForLocalTimeOnly() {
        LocalTime time = LocalTime.of(12, 34, 56);
        assertThrows(DateTimeException.class, () -> StringConversions.toOffsetDateTime(time, converter));
    }

    @Test
    void shouldThrowDateTimeExceptionForOffsetTimeOnly() {
        OffsetTime ot = OffsetTime.of(12, 34, 56, 0, ZoneOffset.ofHours(2));
        assertThrows(DateTimeException.class, () -> StringConversions.toOffsetDateTime(ot, converter));
    }

    @Test
    void shouldThrowDateTimeExceptionForSqlDateOnly() {
        java.sql.Date sqlDate = java.sql.Date.valueOf("2023-08-15");
        assertThrows(DateTimeException.class, () -> StringConversions.toOffsetDateTime(sqlDate, converter));
    }
}
