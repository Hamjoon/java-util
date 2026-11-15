package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toInstant_Test {

    /**
     * Converter instance – not used by the method but required by the signature.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter does not participate in the toInstant logic, but we create a real instance
        // to keep the test realistic and to guard against future changes.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static resources if any (none for now).
        converter = null;
    }

    /**
     * Valid conversions – each entry contains a ZonedDateTime and the expected Instant.
     */
    private static Stream<Arguments> validZonedDateTimeProvider() {
        return Stream.of(// UTC – straightforward mapping
        Arguments.of(ZonedDateTime.of(2023, 3, 25, 10, 15, 30, 0, ZoneId.of("UTC")), Instant.parse("2023-03-25T10:15:30Z"), "Convert a UTC ZonedDateTime to Instant; the result should match the same UTC timestamp."), // Europe/Paris is UTC+1 at the given date (no DST yet)
        Arguments.of(ZonedDateTime.of(2023, 3, 25, 12, 15, 30, 0, ZoneId.of("Europe/Paris")), Instant.parse("2023-03-25T11:15:30Z"), "Convert a ZonedDateTime with a non‑UTC offset (Paris, UTC+1) to Instant; the Instant should be adjusted to UTC."), // Epoch start – edge case
        Arguments.of(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")), Instant.EPOCH, "Edge case: the epoch start in UTC should produce Instant.EPOCH."));
    }

    /**
     * Invalid inputs that should result in an exception (null, wrong type, etc.).
     */
    private static Stream<Arguments> invalidInputProvider() {
        return Stream.of(// Null source
        Arguments.of(null, NullPointerException.class, "Passing a null reference as the source object should result in a NullPointerException."), // Wrong type: LocalDateTime
        Arguments.of(LocalDateTime.of(2023, 3, 25, 10, 15, 30), ClassCastException.class, "Providing a LocalDateTime (not a ZonedDateTime) must cause a ClassCastException."), // Wrong type: plain String
        Arguments.of("2023-03-25T10:15:30Z", ClassCastException.class, "Supplying a String instead of a ZonedDateTime should also trigger a ClassCastException."));
    }

    @Test
    void toInstant_givenUtcZonedDateTime_returnsCorrespondingInstant() {
        // GIVEN
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 10, 12, 30, 45, 0, ZoneId.of("UTC"));
        Instant expected = Instant.ofEpochSecond(zdt.toEpochSecond(), zdt.getNano());
        // WHEN
        Instant actual = ZonedDateTimeConversions.toInstant(zdt, converter);
        // THEN
        assertEquals(expected, actual, "Instant should match the UTC ZonedDateTime");
    }

    @Test
    void toInstant_givenNonUtcZonedDateTime_returnsInstantInUTC() {
        // GIVEN
        // UTC‑4
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 10, 8, 30, 45, 0, ZoneId.of("America/New_York"));
        // The same moment expressed as an Instant (UTC)
        Instant expected = zdt.toInstant();
        // WHEN
        Instant actual = ZonedDateTimeConversions.toInstant(zdt, converter);
        // THEN
        assertEquals(expected, actual, "Instant should represent the same moment regardless of zone");
    }

    @Test
    void toInstant_givenNull_throwsNullPointerException() {
        // GIVEN
        Object nullObj = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toInstant(nullObj, converter), "Calling toInstant with a null source should throw NPE");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validZonedDateTimeProvider")
    void toInstant_validZonedDateTime_returnsExpectedInstant(ZonedDateTime from, Instant expected, String description) {
        // WHEN
        // converter can be null per JSON
        Instant actual = ZonedDateTimeConversions.toInstant(from, null);
        // THEN
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("invalidInputProvider")
    void toInstant_invalidInputs_throwExpectedException(Object from, Class<? extends Throwable> expectedException, String description) {
        // WHEN / THEN
        assertThrows(expectedException, () -> ZonedDateTimeConversions.toInstant(from, null), description);
    }

    /**
     * Out‑of‑range ZonedDateTime – the underlying {@code toInstant()} should throw DateTimeException.
     */
    @Test
    void toInstant_outOfRangeZonedDateTime_throwsDateTimeException() {
        // GIVEN: a year far beyond the range supported by java.time.Instant
        ZonedDateTime outOfRange = ZonedDateTime.of(// year
        1_000_000_001, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        // WHEN / THEN
        assertThrows(DateTimeException.class, () -> ZonedDateTimeConversions.toInstant(outOfRange, null), "A ZonedDateTime whose instant lies beyond the supported range should cause DateTimeException.");
    }
}
