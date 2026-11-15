package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toString_Test {

    private static Stream<Arguments> validOffsetDateTimeProvider() {
        return Stream.of(// Typical positive offset
        Arguments.of(OffsetDateTime.parse("2023-03-25T14:30:00+02:00"), "Converts a typical OffsetDateTime with a positive hour offset to its ISO‑8601 string representation."), // Negative hour‑minute offset
        Arguments.of(OffsetDateTime.parse("2021-12-31T23:59:59-05:30"), "Converts an OffsetDateTime with a negative hour‑minute offset, ensuring the sign and minutes are preserved."), // UTC (uses the 'Z' suffix)
        Arguments.of(OffsetDateTime.parse("2020-01-01T00:00:00Z"), "Converts an OffsetDateTime at UTC (zone offset Z) and verifies the 'Z' suffix is used."), // Maximum positive offset (+18:00)
        Arguments.of(OffsetDateTime.parse("2000-01-01T00:00:00+18:00"), "Maximum positive offset (+18:00) is formatted correctly according to ISO‑8601."), // Maximum negative offset (‑18:00)
        Arguments.of(OffsetDateTime.parse("1999-12-31T23:59:59-18:00"), "Maximum negative offset (‑18:00) is formatted correctly."), // Nanosecond precision preserved
        Arguments.of(OffsetDateTime.parse("2022-06-15T12:34:56.123456789+01:00"), "OffsetDateTime with nanosecond precision is preserved in the string output."), // Single‑digit fractional second retained
        Arguments.of(OffsetDateTime.parse("2022-06-15T12:34:56.1+01:00"), "A single‑digit fractional second is retained without trailing zeros."), // Explicit +00:00 offset (should be formatted as Z by ISO_OFFSET_DATE_TIME)
        Arguments.of(OffsetDateTime.parse("2021-07-01T00:00:00+00:00"), "Explicit +00:00 offset is formatted with the Z suffix (ISO standard)."), // Zero offset with fractional seconds
        Arguments.of(OffsetDateTime.parse("2021-07-01T12:34:56.789+00:00"), "Zero offset combined with fractional seconds should retain both the fraction and the Z suffix."));
    }

    private static Stream<Arguments> nullFromProvider() {
        return Stream.of(Arguments.of(null, null, "Passing a null `from` argument should cause a NullPointerException when the method attempts to call `format` on the null OffsetDateTime."), Arguments.of(null, new Converter(new ConverterOptions()), "Passing a null `from` argument while providing a non‑null converter should still result in a NullPointerException."));
    }

    private static Stream<Arguments> invalidTypeProvider() {
        return Stream.of(Arguments.of(java.time.LocalDateTime.parse("2023-03-25T14:30:00"), null, "Providing an object that is not an OffsetDateTime (e.g., LocalDateTime) must result in a ClassCastException due to the forced cast."), Arguments.of(java.time.LocalDateTime.parse("2023-03-25T14:30:00"), new Converter(new ConverterOptions()), "Providing an object that is not an OffsetDateTime with a non‑null converter must still cause a ClassCastException."));
    }

    @Test
    void toString_ShouldFormatOffsetDateTimeUsingIsoOffsetDateTime() {
        // GIVEN
        OffsetDateTime input = OffsetDateTime.of(2023, 5, 17, 14, 30, 45, 123_000_000, ZoneOffset.ofHoursMinutes(2, 30));
        // The Converter argument is not used by the method, but we must pass a non‑null value.
        Converter dummyConverter = new Converter(new ConverterOptions());
        // WHEN
        String result = OffsetDateTimeConversions.toString(input, dummyConverter);
        // THEN
        String expected = input.format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        assertEquals(expected, result, "The toString method should format the OffsetDateTime using ISO_OFFSET_DATE_TIME");
    }

    @Test
    void toString_ShouldHandleNullConverter() {
        // GIVEN
        OffsetDateTime input = OffsetDateTime.now();
        // WHEN
        String result = OffsetDateTimeConversions.toString(input, null);
        // THEN
        String expected = input.format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        assertEquals(expected, result, "The method should ignore a null Converter parameter and still format correctly");
    }

    @Test
    void toString_ShouldThrowClassCastExceptionForInvalidInput() {
        // GIVEN
        Object notAnOffsetDateTime = "2023-01-01T00:00:00Z";
        Converter dummyConverter = new Converter(new ConverterOptions());
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> OffsetDateTimeConversions.toString(notAnOffsetDateTime, dummyConverter), "Passing a non‑OffsetDateTime object should result in a ClassCastException");
    }

    /* -------------------------------------------------------------
     *  Valid OffsetDateTime conversions (including extreme offsets,
     *  fractional seconds and zero‑offset variations)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => {2}")
    @MethodSource("validOffsetDateTimeProvider")
    void toString_ValidOffsetDateTime_ShouldReturnIsoString(OffsetDateTime input, String description) {
        // GIVEN – a non‑null dummy converter (the method ignores it)
        Converter dummyConverter = new Converter(new ConverterOptions());
        // WHEN
        String result = OffsetDateTimeConversions.toString(input, dummyConverter);
        // THEN – the result must match ISO_OFFSET_DATE_TIME formatting
        String expected = input.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        assertEquals(expected, result, description);
    }

    /* -------------------------------------------------------------
     *  Null `from` argument – expect NullPointerException
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => {2}")
    @MethodSource("nullFromProvider")
    void toString_NullFrom_ShouldThrowNullPointerException(Object from, Converter converter, String description) {
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toString(from, converter), description);
    }

    /* -------------------------------------------------------------
     *  Invalid `from` type – expect ClassCastException
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => {2}")
    @MethodSource("invalidTypeProvider")
    void toString_InvalidFromType_ShouldThrowClassCastException(Object from, Converter converter, String description) {
        assertThrows(ClassCastException.class, () -> OffsetDateTimeConversions.toString(from, converter), description);
    }

    /* -------------------------------------------------------------
     *  Non‑null converter is ignored – behaviour already covered
     *  by the valid conversion test using a dummy converter.
     * ------------------------------------------------------------- */
    @Test
    void toString_NonNullConverter_IgnoredByImplementation() {
        // GIVEN
        OffsetDateTime input = OffsetDateTime.parse("2023-03-25T14:30:00+02:00");
        Converter dummy = new Converter(new ConverterOptions());
        // WHEN
        String result = OffsetDateTimeConversions.toString(input, dummy);
        // THEN
        String expected = input.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        assertEquals(expected, result, "The method does not use the `converter` argument; a non‑null converter should have no effect on the output.");
    }
}
