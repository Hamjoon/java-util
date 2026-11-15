package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.format.DateTimeParseException;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DateConversions_toOffsetDateTime_Test {

    /**
     * The Converter used for all tests.
     */
    private static Converter converter;

    /**
     * A fixed zone that will be used by the ConverterOptions for deterministic results.
     */
    private static final ZoneId TEST_ZONE = ZoneId.of("America/New_York");

    @BeforeAll
    static void setupBeforeAll() {
        // Build a real Converter with a known zone – used by the happy‑path tests above.
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(TEST_ZONE);
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static converter reference.
        converter = null;
    }

    /**
     * Verify that an {@link Instant} is converted to an {@link OffsetDateTime}
     * with the zone supplied by the {@link ConverterOptions}.
     */
    @Test
    void toOffsetDateTime_fromInstant() {
        // GIVEN
        Instant instant = Instant.parse("2023-03-25T15:30:45.123Z");
        // WHEN
        OffsetDateTime result = DateConversions.toOffsetDateTime(instant, converter);
        // THEN
        OffsetDateTime expected = instant.atZone(TEST_ZONE).toOffsetDateTime();
        assertEquals(expected, result);
    }

    /**
     * Verify conversion from a {@link ZonedDateTime}. The original zone of the
     * source should be ignored – the result must be expressed in the Converter's zone.
     */
    @Test
    void toOffsetDateTime_fromZonedDateTime() {
        // GIVEN
        ZonedDateTime source = ZonedDateTime.of(2022, 12, 31, 23, 59, 59, 0, // different zone
        ZoneId.of("Europe/Paris"));
        // WHEN
        OffsetDateTime result = DateConversions.toOffsetDateTime(source, converter);
        // THEN
        // The instant part stays the same, only the zone/offset changes.
        OffsetDateTime expected = source.toInstant().atZone(TEST_ZONE).toOffsetDateTime();
        assertEquals(expected, result);
    }

    /**
     * Verify conversion from a {@link LocalDateTime}. The method should treat the
     * LocalDateTime as being in the Converter's zone.
     */
    @Test
    void toOffsetDateTime_fromLocalDateTime() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2021, Month.JUNE, 15, 10, 20, 30, 500_000_000);
        // WHEN
        OffsetDateTime result = DateConversions.toOffsetDateTime(ldt, converter);
        // THEN
        OffsetDateTime expected = ldt.atZone(TEST_ZONE).toOffsetDateTime();
        assertEquals(expected, result);
    }

    /**
     * Verify conversion from a {@link String} that represents an ISO‑8601 instant.
     * The Converter should parse the string to an {@link Instant} first.
     */
    @Test
    void toOffsetDateTime_fromStringIsoInstant() {
        // GIVEN
        String iso = "2020-01-02T03:04:05.006Z";
        // WHEN
        OffsetDateTime result = DateConversions.toOffsetDateTime(iso, converter);
        // THEN
        Instant instant = Instant.parse(iso);
        OffsetDateTime expected = instant.atZone(TEST_ZONE).toOffsetDateTime();
        assertEquals(expected, result);
    }

    /**
     * Verify conversion from a {@link Date}. The Date is interpreted as an
     * instant in the default UTC timeline and then shifted to the Converter's zone.
     */
    @Test
    void toOffsetDateTime_fromDate() {
        // GIVEN
        Date date = Date.from(Instant.parse("2019-07-04T12:00:00Z"));
        // WHEN
        OffsetDateTime result = DateConversions.toOffsetDateTime(date, converter);
        // THEN
        OffsetDateTime expected = date.toInstant().atZone(TEST_ZONE).toOffsetDateTime();
        assertEquals(expected, result);
    }

    /**
     * Verify that a {@code null} source returns {@code null}. The underlying
     * {@code toInstant} conversion returns {@code null} for a null input, which
     * propagates through to the final result.
     */
    @Test
    void toOffsetDateTime_fromNull() {
        // GIVEN
        Object source = null;
        // WHEN
        OffsetDateTime result = DateConversions.toOffsetDateTime(source, converter);
        // THEN
        assertNull(result);
    }

    /**
     * Case: source object is {@code null} but the Converter is valid.
     * The underlying {@code toInstant} returns {@code null}, which leads to a
     * {@code NullPointerException} when {@code atZone(...)} is invoked.
     */
    @Test
    void toOffsetDateTime_nullSource_throwsNPE() {
        assertThrows(NullPointerException.class, () -> DateConversions.toOffsetDateTime(null, converter));
    }

    /**
     * Case: the {@link Converter} argument itself is {@code null}.
     * The method tries to call {@code converter.getOptions()} and must throw NPE.
     */
    @Test
    void toOffsetDateTime_nullConverter_throwsNPE() {
        Instant instant = Instant.now();
        assertThrows(NullPointerException.class, () -> DateConversions.toOffsetDateTime(instant, null));
    }

    /**
     * Case: Converter returns {@code null} for {@code getOptions()}.
     * We mock such a Converter and verify that a NPE propagates.
     */
    @Test
    void toOffsetDateTime_converterWithNullOptions_throwsNPE(@Mock Converter mockConverter) {
        // Mock behaviour: getOptions() returns null.
        when(mockConverter.getOptions()).thenReturn(null);
        Instant instant = Instant.now();
        assertThrows(NullPointerException.class, () -> DateConversions.toOffsetDateTime(instant, mockConverter));
    }

    /**
     * Case: ConverterOptions returns {@code null} for {@code getZoneId()}.
     * The call to {@code atZone(null)} must raise a NPE.
     */
    @Test
    void toOffsetDateTime_converterWithNullZoneId_throwsNPE(@Mock Converter mockConverter, @Mock ConverterOptions mockOptions) {
        // Options returns null zone id.
        when(mockOptions.getZoneId()).thenReturn(null);
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        Instant instant = Instant.now();
        assertThrows(NullPointerException.class, () -> DateConversions.toOffsetDateTime(instant, mockConverter));
    }

    /**
     * Case: an unsupported source type (plain Object) is supplied.
     * The internal {@code toInstant} should reject it and throw IllegalArgumentException.
     */
    @Test
    void toOffsetDateTime_unsupportedSource_throwsIAE() {
        Object unsupported = new Object();
        assertThrows(IllegalArgumentException.class, () -> DateConversions.toOffsetDateTime(unsupported, converter));
    }

    /**
     * Case: a malformed String that cannot be parsed as an instant.
     * The conversion to {@link Instant} must fail with DateTimeParseException.
     */
    @Test
    void toOffsetDateTime_malformedString_throwsDateTimeParseException() {
        String bad = "not-a-date-time";
        assertThrows(DateTimeParseException.class, () -> DateConversions.toOffsetDateTime(bad, converter));
    }

    /**
     * Case: a numeric epoch‑millis value that exceeds the range of {@link Instant}.
     * Creating an Instant from such a value throws a DateTimeException (or ArithmeticException).
     * We use Long.MAX_VALUE which is far beyond the supported epoch‑millis range.
     */
    @Test
    void toOffsetDateTime_outOfRangeLong_throwsDateTimeException() {
        Long outOfRange = Long.MAX_VALUE;
        assertThrows(DateTimeException.class, () -> DateConversions.toOffsetDateTime(outOfRange, converter));
    }
}
