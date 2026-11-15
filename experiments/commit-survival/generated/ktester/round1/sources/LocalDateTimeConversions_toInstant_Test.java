package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.stream.Stream;
import java.util.concurrent.atomic.AtomicLong;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toInstant_Test {

    private Converter converter;

    /* -------------------------------------------------
     * Helper class for custom conversion test
     * ------------------------------------------------- */
    private static final class CustomTemporal {
        // No fields – just a marker type for the custom converter test
    }

    @BeforeEach
    void setupBeforeEach() {
        // Converter requires a ConverterOptions instance; using the default options is sufficient
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
        // We replace the default converter only for that specific case by checking the type.
        Converter original = this.converter;
        this.converter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                if (src instanceof CustomTemporal && target == ZonedDateTime.class) {
                    return ZonedDateTime.of(2023, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);
                }
                // fallback to original behaviour
                return original.convert(src, target);
            }
        };
    }

    private static Stream<Arguments> validConversionProvider() {
        Instant baseInstant = Instant.parse("2023-01-01T12:00:00Z");
        ZonedDateTime utcZdt = ZonedDateTime.of(2023, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime offsetPlus2 = OffsetDateTime.of(2023, 1, 1, 12, 0, 0, 0, ZoneOffset.ofHours(2));
        // 2021‑09‑01T00:00:00Z
        long epochMillis = 1630454400000L;
        return Stream.of(// Instant directly
        Arguments.of("Pass an Instant directly; toInstant should return the same Instant.", baseInstant, baseInstant), // java.util.Date
        Arguments.of("Pass a java.util.Date; conversion should yield the equivalent Instant.", Date.from(baseInstant), baseInstant), // java.sql.Timestamp (treated as UTC for simplicity)
        Arguments.of("Pass a java.sql.Timestamp; conversion should produce the correct Instant.", Timestamp.valueOf("2023-01-01 12:00:00"), baseInstant), // LocalDateTime (treated as UTC)
        Arguments.of("Pass a LocalDateTime; conversion should treat it as UTC and return the matching Instant.", LocalDateTime.of(2023, 1, 1, 12, 0, 0), baseInstant), // ZonedDateTime already in UTC
        Arguments.of("Pass a ZonedDateTime already in UTC; toInstant should return its instant.", utcZdt, baseInstant), // OffsetDateTime with +02:00 (converted to UTC)
        Arguments.of("Pass an OffsetDateTime with +02:00 offset; conversion should adjust to UTC.", offsetPlus2, Instant.parse("2023-01-01T10:00:00Z")), // Long epoch milliseconds
        Arguments.of("Pass a Long representing epoch milliseconds; conversion should produce the corresponding Instant.", epochMillis, Instant.ofEpochMilli(epochMillis)), // ISO‑8601 string with Z
        Arguments.of("Pass an ISO‑8601 string; conversion should parse it into the correct Instant.", "2023-01-01T12:00:00Z", baseInstant), // GregorianCalendar in UTC
        Arguments.of("Pass a Calendar (GregorianCalendar) representing an instant in UTC; conversion should yield the same Instant.", GregorianCalendar.from(utcZdt), baseInstant), // java.sql.Date (date‑only) – interpreted as start‑of‑day UTC
        Arguments.of("Pass a java.sql.Date (date‑only); conversion should treat it as start‑of‑day UTC and return the corresponding Instant.", java.sql.Date.valueOf("2023-01-01"), Instant.parse("2023-01-01T00:00:00Z")), // LocalDate – midnight UTC
        Arguments.of("Pass a LocalDate; conversion should interpret it as midnight UTC of that date.", LocalDate.of(2023, 1, 1), Instant.parse("2023-01-01T00:00:00Z")), // Negative epoch‑millisecond value
        Arguments.of("Pass a negative epoch‑millisecond value (one day before 1970‑01‑01); conversion should produce the correct pre‑epoch Instant.", -86400000L, Instant.ofEpochMilli(-86400000L)), // AtomicLong containing epoch milliseconds
        Arguments.of("Pass an AtomicLong containing epoch milliseconds; conversion should yield the matching Instant.", AtomicLong.valueOf(epochMillis), Instant.ofEpochMilli(epochMillis)), // ISO‑8601 string with +02:00 offset
        Arguments.of("Pass an ISO‑8601 string with a +02:00 offset; conversion should adjust to UTC.", "2023-01-01T12:00:00+02:00", Instant.parse("2023-01-01T10:00:00Z")), // ISO‑8601 string without explicit zone (treated as UTC)
        Arguments.of("Pass an ISO‑8601 string without explicit zone; implementation treats it as UTC.", "2023-01-01T12:00:00", baseInstant), // ZonedDateTime in America/New_York (UTC‑5)
        Arguments.of("Pass a ZonedDateTime in a non‑UTC zone (America/New_York, UTC‑5); conversion should correctly compute the UTC Instant.", ZonedDateTime.of(2023, 1, 1, 12, 0, 0, 0, ZoneId.of("America/New_York")), Instant.parse("2023-01-01T17:00:00Z")), // Custom object with a Converter that returns a ZonedDateTime
        Arguments.of("Pass a custom object together with a Converter that knows how to turn it into a ZonedDateTime; toInstant should use the converter’s result.", new CustomTemporal(), baseInstant));
    }

    @Test
    void toInstant_fromDate_returnsCorrectInstant() {
        // GIVEN
        // 2020‑09‑13T12:26:40Z
        long epochMilli = 1_600_000_000_000L;
        Date date = new Date(epochMilli);
        Instant expected = Instant.ofEpochMilli(epochMilli);
        // WHEN
        Instant actual = LocalDateTimeConversions.toInstant(date, converter);
        // THEN
        assertEquals(expected, actual, "Instant derived from java.util.Date should match epoch milli");
    }

    @Test
    void toInstant_fromLongEpochMilli_returnsCorrectInstant() {
        // GIVEN
        // 2020‑09‑13T12:26:40Z
        Long epochMilli = 1_600_000_000_000L;
        Instant expected = Instant.ofEpochMilli(epochMilli);
        // WHEN
        Instant actual = LocalDateTimeConversions.toInstant(epochMilli, converter);
        // THEN
        assertEquals(expected, actual, "Instant derived from Long epoch milli should match");
    }

    @Test
    void toInstant_fromInstant_returnsSameInstant() {
        // GIVEN
        Instant now = Instant.now();
        // WHEN
        Instant actual = LocalDateTimeConversions.toInstant(now, converter);
        // THEN
        assertEquals(now, actual, "Passing an Instant should return the same Instant");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validConversionProvider")
    void toInstant_validConversions(String description, Object from, Instant expected) {
        // WHEN
        Instant actual = LocalDateTimeConversions.toInstant(from, converter);
        // THEN
        assertEquals(expected, actual, description);
    }

    @Test
    void toInstant_malformedString_throwsDateTimeParseException() {
        String malformed = "not-a-date";
        assertThrows(DateTimeParseException.class, () -> LocalDateTimeConversions.toInstant(malformed, converter), "Malformed date string should cause DateTimeParseException");
    }

    @Test
    void toInstant_unsupportedObject_throwsIllegalArgumentException() {
        Object unsupported = new Object();
        assertThrows(IllegalArgumentException.class, () -> LocalDateTimeConversions.toInstant(unsupported, converter), "Unsupported object type should cause IllegalArgumentException");
    }

    @Test
    void toInstant_nullSource_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toInstant(null, converter), "Null source should cause NullPointerException");
    }

    @Test
    void toInstant_doubleValue_throwsIllegalArgumentException() {
        Double doubleVal = 123.45;
        assertThrows(IllegalArgumentException.class, () -> LocalDateTimeConversions.toInstant(doubleVal, converter), "Double numeric type should be rejected with IllegalArgumentException");
    }

    @Test
    void toInstant_emptyString_throwsDateTimeParseException() {
        assertThrows(DateTimeParseException.class, () -> LocalDateTimeConversions.toInstant("", converter), "Empty string should cause DateTimeParseException");
    }

    @Test
    void toInstant_converterReturnsNull_throwsNullPointerException() {
        // Converter that returns null for any conversion request
        Converter nullReturningConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                return null;
            }
        };
        LocalDateTime ldt = LocalDateTime.of(2023, 1, 1, 12, 0, 0);
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toInstant(ldt, nullReturningConverter), "Converter returning null should cause NullPointerException when toInstant() is invoked on the result");
    }

    @Test
    void toInstant_converterThrowsException_propagatesException() {
        // Converter that deliberately throws an IllegalStateException
        Converter throwingConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                throw new IllegalStateException("conversion failed");
            }
        };
        LocalDateTime ldt = LocalDateTime.of(2023, 1, 1, 12, 0, 0);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> LocalDateTimeConversions.toInstant(ldt, throwingConverter), "Exception from converter should propagate unchanged");
        assertEquals("conversion failed", ex.getMessage());
    }
}
