package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_toOffsetDateTime_Test {

    /**
     * Shared Converter instance used by all tests.
     */
    private static Converter converter;

    /**
     * Original default zone – restored after all tests.
     */
    private static ZoneId originalDefaultZone;

    /**
     * Placeholder for the library's {@code ConversionException}.
     * In the real project this would be imported from the library.
     */
    class ConversionException extends RuntimeException {

        public ConversionException(String message, Throwable cause) {
            super(message, cause);
        }

        public ConversionException(String message) {
            super(message);
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a default Converter (the library requires ConverterOptions).
        converter = new Converter(new ConverterOptions());
        // Force a known default zone for tests that rely on system default.
        originalDefaultZone = ZoneId.systemDefault();
        ZoneId testZone = ZoneId.of("UTC+02:00");
        System.setProperty("user.timezone", testZone.getId());
        // Re‑apply the change (some JVMs cache the zone at startup).
        TimeZone.setDefault(TimeZone.getTimeZone(testZone));
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore original default zone.
        System.setProperty("user.timezone", originalDefaultZone.getId());
        TimeZone.setDefault(TimeZone.getTimeZone(originalDefaultZone));
        converter = null;
    }

    private static Stream<Arguments> provideTemporalSources() {
        // Instant (always UTC)
        Instant instant = Instant.parse("2023-03-15T10:30:45.123Z");
        // java.util.Date – interpreted in the system default zone (UTC+02)
        Date date = Date.from(instant);
        // java.sql.Timestamp – same as Instant
        Timestamp timestamp = Timestamp.from(instant);
        // Calendar with America/New_York zone (offset -04 at the given instant)
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        calendar.setTimeInMillis(Instant.parse("2023-03-15T12:30:45.123Z").toEpochMilli());
        // LocalDateTime – interpreted in system default zone (UTC+02)
        LocalDateTime ldt = LocalDateTime.of(2023, 3, 15, 12, 30, 45, 123_000_000);
        // ZonedDateTime with Europe/Paris (+01 or +02 depending on DST; March 15 2023 is +01)
        ZonedDateTime zdt = ZonedDateTime.parse("2023-03-15T12:30:45.123+01:00[Europe/Paris]");
        return Stream.of(// OffsetDateTime (identity)
        Arguments.of(OffsetDateTime.parse("2023-03-15T12:30:45.123+02:00"), "2023-03-15T12:30:45.123+02:00"), // ZonedDateTime
        Arguments.of(zdt, "2023-03-15T12:30:45.123+01:00"), // Instant
        Arguments.of(instant, "2023-03-15T10:30:45.123Z"), // Date (system default zone is UTC+02, so offset +02)
        Arguments.of(date, "2023-03-15T12:30:45.123+02:00"), // Timestamp
        Arguments.of(timestamp, "2023-03-15T10:30:45.123Z"), // Calendar (America/New_York offset -04 at this instant)
        Arguments.of(calendar, "2023-03-15T08:30:45.123-04:00"), // LocalDateTime (interpreted in system default zone UTC+02)
        Arguments.of(ldt, "2023-03-15T12:30:45.123+02:00"));
    }

    private static Stream<Arguments> provideBigDecimalEpochSeconds() {
        return Stream.of(Arguments.of(new BigDecimal("0"), "1970-01-01T00:00:00Z"), Arguments.of(new BigDecimal("-31536000"), "1969-01-01T00:00:00Z"), // 9999‑12‑31T23:59:59.999999999Z = 253402300799.999999999 seconds since epoch
        Arguments.of(new BigDecimal("253402300799.999999999"), "9999-12-31T23:59:59.999999999Z"));
    }

    @Test
    void testToOffsetDateTime_FromZonedDateTime() {
        // GIVEN: a ZonedDateTime in a non‑UTC zone.
        ZonedDateTime zonedDateTime = ZonedDateTime.of(// year, month, day
        2023, // year, month, day
        5, // year, month, day
        10, // hour, minute, second, nanosecond
        15, // hour, minute, second, nanosecond
        30, // hour, minute, second, nanosecond
        45, // hour, minute, second, nanosecond
        123_000_000, ZoneId.of("America/New_York"));
        // WHEN: converting to OffsetDateTime via the focal method.
        OffsetDateTime actual = BigDecimalConversions.toOffsetDateTime(zonedDateTime, converter);
        // THEN: the result should be the same instant with the same offset as the original ZonedDateTime.
        OffsetDateTime expected = zonedDateTime.toOffsetDateTime();
        assertEquals(expected, actual, "Converted OffsetDateTime does not match expected value");
    }

    @Test
    void testToOffsetDateTime_FromOffsetDateTime() {
        // GIVEN: an OffsetDateTime instance (the conversion library should treat it as identity).
        OffsetDateTime offsetDateTime = OffsetDateTime.of(2022, 12, 31, 23, 59, 59, 999_999_999, ZoneId.of("UTC").getRules().getOffset(java.time.Instant.now()));
        // WHEN: converting to OffsetDateTime via the focal method.
        OffsetDateTime actual = BigDecimalConversions.toOffsetDateTime(offsetDateTime, converter);
        // THEN: the result should be identical to the input.
        assertEquals(offsetDateTime, actual, "Conversion of an OffsetDateTime should return the same value");
    }

    @Test
    void testToOffsetDateTime_NullInput() {
        // GIVEN: a null source object.
        Object source = null;
        // WHEN / THEN: the method should return null (the internal conversion to ZonedDateTime also returns null).
        OffsetDateTime result = BigDecimalConversions.toOffsetDateTime(source, converter);
        assertNull(result, "Conversion of null should yield null");
    }

    @Test
    void testToOffsetDateTime_NullConverter() {
        OffsetDateTime src = OffsetDateTime.parse("2023-01-01T10:15:30+01:00");
        assertThrows(IllegalArgumentException.class, () -> BigDecimalConversions.toOffsetDateTime(src, null), "A null Converter should cause IllegalArgumentException");
    }

    @ParameterizedTest(name = "{index}: from {0}")
    @MethodSource("provideTemporalSources")
    void testToOffsetDateTime_SupportedSources(Object from, String expectedIso) {
        OffsetDateTime actual = BigDecimalConversions.toOffsetDateTime(from, converter);
        OffsetDateTime expected = OffsetDateTime.parse(expectedIso);
        assertEquals(expected, actual, () -> "Conversion result mismatch for source type " + from.getClass().getSimpleName());
    }

    @ParameterizedTest(name = "{index}: BigDecimal {0}")
    @MethodSource("provideBigDecimalEpochSeconds")
    void testToOffsetDateTime_BigDecimalEpochSeconds(BigDecimal from, String expectedIso) {
        OffsetDateTime actual = BigDecimalConversions.toOffsetDateTime(from, converter);
        OffsetDateTime expected = OffsetDateTime.parse(expectedIso);
        assertEquals(expected, actual, () -> "BigDecimal epoch seconds conversion failed for " + from);
    }

    @Test
    void testToOffsetDateTime_BigDecimalOutOfRange() {
        // one second beyond max Instant
        BigDecimal tooBig = new BigDecimal("253402300800");
        assertThrows(DateTimeException.class, () -> BigDecimalConversions.toOffsetDateTime(tooBig, converter), "Out‑of‑range epoch seconds should throw DateTimeException");
    }

    @Test
    void testToOffsetDateTime_BigDecimalTooPrecise() {
        // 10 fractional digits
        BigDecimal tooPrecise = new BigDecimal("0.1234567891");
        assertThrows(DateTimeException.class, () -> BigDecimalConversions.toOffsetDateTime(tooPrecise, converter), "Too‑precise BigDecimal should throw DateTimeException");
    }

    @Test
    void testToOffsetDateTime_UnsupportedString() {
        String badString = "2023-13-40T25:61:61Z";
        assertThrows(ConversionException.class, () -> BigDecimalConversions.toOffsetDateTime(badString, converter), "Malformed ISO‑8601 string should raise ConversionException");
    }

    @Test
    void testToOffsetDateTime_UnsupportedNumber() {
        Integer unsupported = Integer.valueOf(12345);
        assertThrows(ConversionException.class, () -> BigDecimalConversions.toOffsetDateTime(unsupported, converter), "Unsupported numeric type should raise ConversionException");
    }

    @Test
    void testToOffsetDateTime_UnsupportedObject() {
        String notADate = "not-a-date";
        assertThrows(ConversionException.class, () -> BigDecimalConversions.toOffsetDateTime(notADate, converter), "Non‑date string should raise ConversionException");
    }
}
