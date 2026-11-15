package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toOffsetDateTime_Test {

    /**
     * Converter used by the tests – created once for the whole test run.
     */
    private static Converter converter;

    /**
     * Preserve the original default time‑zone so we can restore it after tests that change it.
     */
    private static TimeZone originalDefaultZone;

    /**
     * Runs once before any test method.
     */
    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance.  The default constructor
        // provides a clean set of options (no overrides).  Using the real implementation
        // keeps the test close to production behaviour.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    /**
     * Runs once after all tests have finished – currently nothing to clean up.
     */
    @AfterAll
    static void teardownAfterAll() {
        // restore the original default zone to avoid side‑effects on other test classes
        TimeZone.setDefault(originalDefaultZone);
        converter = null;
    }

    /**
     * Provides a stream of arguments for successful conversions.
     * Each argument consists of:
     *   - a description (for readability)
     *   - the input object to be converted
     *   - the expected ISO‑8601 string representation of the resulting OffsetDateTime
     *   - an optional Consumer to set up test environment (e.g., default time‑zone)
     */
    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// Null handling – already covered above, but kept for completeness
        Arguments.of("Null handling", null, null, (Runnable) () -> {
        }), // String ISO with offset
        Arguments.of("String ISO with offset", "2023-03-25T10:15:30+02:00", "2023-03-25T10:15:30+02:00", (Runnable) () -> {
        }), // String ISO without offset – default zone forced to UTC
        Arguments.of("String ISO without offset (default zone UTC)", "2023-03-25T10:15:30", "2023-03-25T10:15:30Z", (Runnable) () -> TimeZone.setDefault(TimeZone.getTimeZone("UTC"))), // Instant conversion
        Arguments.of("Instant conversion", Instant.ofEpochSecond(0), "1970-01-01T00:00:00Z", (Runnable) () -> {
        }), // Date conversion
        Arguments.of("Date conversion", new Date(0L), "1970-01-01T00:00:00Z", (Runnable) () -> {
        }), // Timestamp conversion
        Arguments.of("Timestamp conversion", Timestamp.from(Instant.ofEpochSecond(0)), "1970-01-01T00:00:00Z", (Runnable) () -> {
        }), // ZonedDateTime pass‑through
        Arguments.of("ZonedDateTime pass‑through", ZonedDateTime.of(2022, 1, 15, 12, 0, 0, 0, ZoneId.of("Europe/Paris")), "2022-01-15T12:00:00+01:00", (Runnable) () -> {
        }), // OffsetDateTime pass‑through
        Arguments.of("OffsetDateTime pass‑through", OffsetDateTime.of(2021, 12, 31, 23, 59, 59, 0, ZoneOffset.ofHours(-5)), "2021-12-31T23:59:59-05:00", (Runnable) () -> {
        }), // Calendar conversion (UTC)
        Arguments.of("Calendar conversion", GregorianCalendar.from(ZonedDateTime.of(2020, 5, 20, 8, 30, 0, 0, ZoneId.of("UTC"))), "2020-05-20T08:30:00Z", (Runnable) () -> {
        }), // Long epoch millis conversion
        Arguments.of("Long epoch millis conversion", 1609459200000L, "2021-01-01T00:00:00Z", (Runnable) () -> {
        }));
    }

    /**
     * Provides inputs that should lead to a null result (unsupported or unparsable types).
     */
    private static Stream<Arguments> nullResultProvider() {
        return Stream.of(Arguments.of("Invalid string handling", "not-a-date"), Arguments.of("Unsupported type handling", true), // Object with no conversion rule – should return null without throwing
        Arguments.of("Completely unsupported object", new Object()));
    }

    /**
     * Provides inputs that are expected to cause exceptions.
     */
    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// DST gap – 2022-03-27 02:30 does not exist in New York
        Arguments.of("DST gap ZonedDateTime", ZonedDateTime.of(2022, 3, 27, 2, 30, 0, 0, ZoneId.of("America/New_York")), DateTimeException.class), // Invalid offset for given local date‑time (the offset is out of range for the zone)
        Arguments.of("Invalid OffsetDateTime offset", OffsetDateTime.of(2021, 10, 31, 2, 30, 0, 0, ZoneOffset.ofHours(2)), DateTimeException.class), // LocalDateTime with year out of range for ZonedDateTime conversion
        Arguments.of("Overflow LocalDateTime year", LocalDateTime.of(999_999_999, 12, 31, 23, 59, 59), DateTimeException.class), // StringBuilder with impossible date (Feb 30)
        Arguments.of("Invalid date in StringBuilder", new StringBuilder("2023-02-30T10:00:00"), DateTimeParseException.class), // Date with epoch millis beyond representable range
        Arguments.of("Date beyond Instant range", new Date(Long.MAX_VALUE), DateTimeException.class), // ZonedDateTime with instant beyond max supported
        Arguments.of("ZonedDateTime with huge instant", ZonedDateTime.ofInstant(Instant.ofEpochSecond(Long.MAX_VALUE), ZoneId.of("UTC")), DateTimeException.class), // OffsetDateTime with huge instant
        Arguments.of("OffsetDateTime with huge instant", OffsetDateTime.ofInstant(Instant.ofEpochSecond(Long.MAX_VALUE), ZoneOffset.UTC), DateTimeException.class), // Calendar with invalid TimeZone ID
        Arguments.of("Calendar with invalid zone", Calendar.getInstance(TimeZone.getTimeZone("Invalid/Zone")), ZoneRulesException.class));
    }

    @Test
    void toOffsetDateTime_withValidIsoString_returnsExpectedOffsetDateTime() {
        // GIVEN
        String isoString = "2023-08-15T12:34:56+02:00";
        // WHEN
        OffsetDateTime result = StringConversions.toOffsetDateTime(isoString, converter);
        // THEN
        assertNotNull(result, "Result should not be null for a valid ISO string");
        assertEquals(OffsetDateTime.parse(isoString), result, "The parsed OffsetDateTime should match the ISO string representation");
    }

    @Test
    void toOffsetDateTime_withNullInput_returnsNull() {
        // GIVEN
        Object nullInput = null;
        // WHEN
        OffsetDateTime result = StringConversions.toOffsetDateTime(nullInput, converter);
        // THEN
        assertNull(result, "Result should be null when the input value is null");
    }

    @Test
    void toOffsetDateTime_withNonParsableString_returnsNull() {
        // GIVEN
        String badString = "not-a-date-time";
        // WHEN
        OffsetDateTime result = StringConversions.toOffsetDateTime(badString, converter);
        // THEN
        // The internal toZonedDateTime returns null for unparsable values,
        // consequently toOffsetDateTime also returns null.
        assertNull(result, "Result should be null for a non‑parsable date‑time string");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("successfulConversionProvider")
    void toOffsetDateTime_successfulConversions(String description, Object input, String expectedIso, Runnable setup) {
        // Arrange
        // e.g., set default zone for the test
        setup.run();
        // Act
        OffsetDateTime result = StringConversions.toOffsetDateTime(input, converter);
        // Assert
        if (expectedIso == null) {
            assertNull(result, description + " – expected null");
        } else {
            assertNotNull(result, description + " – result should not be null");
            assertEquals(OffsetDateTime.parse(expectedIso), result, description + " – parsed result does not match expected");
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nullResultProvider")
    void toOffsetDateTime_returnsNullForUnsupportedOrInvalid(String description, Object input) {
        assertNull(StringConversions.toOffsetDateTime(input, converter), description + " – expected null");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("exceptionProvider")
    void toOffsetDateTime_throwsExpectedException(String description, Object input, Class<? extends Throwable> expectedException) {
        // The method under test delegates to toZonedDateTime which may throw.
        // We assert that the same exception propagates out of toOffsetDateTime.
        assertThrows(expectedException, () -> StringConversions.toOffsetDateTime(input, converter), description + " – expected exception");
    }
}
