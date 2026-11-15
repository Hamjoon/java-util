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
import java.time.temporal.ChronoField;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toDouble_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The method under test does not use the converter, but we provide a real instance
        // for the cases where a non‑null converter is supplied.
        converter = new Converter(new ConverterOptions());
    }

    /**
     * Supplies all valid conversion scenarios described in the JSON test data.
     */
    private static Stream<Arguments> validConversionProvider() {
        // Helper to parse expected double from string literal (preserves sign and fraction).
        java.util.function.Function<String, Double> d = Double::valueOf;
        // Standard converter (null) – the method ignores it.
        Converter nullConv = null;
        // A dummy non‑null converter for the specific test case.
        Converter dummyConv = new Converter(new ConverterOptions()) {

            @Override
            public <T> T convert(Object src, Class<T> target) {
                // Implementation is irrelevant for the method under test.
                return null;
            }
        };
        return Stream.of(// ---- Group: Valid conversions ----
        Arguments.of("Standard OffsetDateTime with positive offset and nanosecond fraction", OffsetDateTime.of(2023, 3, 25, 12, 34, 56, 789_123_456, ZoneOffset.ofHours(2)), nullConv, d.apply("1679740496.7891235")), Arguments.of("Epoch start (1970‑01‑01T00:00:00Z)", OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC), nullConv, d.apply("0.0")), Arguments.of("Instant just before the epoch (one nanosecond before 1970‑01‑01T00:00:00Z)", OffsetDateTime.ofInstant(Instant.ofEpochSecond(-1, 999_999_999), ZoneOffset.UTC), nullConv, d.apply("-0.000000001")), Arguments.of("Far‑future date (year 3000)", OffsetDateTime.ofInstant(Instant.parse("3000-01-01T00:00:00Z"), ZoneOffset.UTC), nullConv, d.apply("32503680000.0")), Arguments.of("Very early date (minimum supported year)", OffsetDateTime.ofInstant(Instant.parse("-999999999-01-01T00:00:00Z"), ZoneOffset.UTC), nullConv, d.apply("-31556952000000000000.0")), // ---- Group: Additional valid conversions ----
        Arguments.of("Negative hour offset (-05:00) with zero nanoseconds", OffsetDateTime.of(2023, 3, 25, 12, 34, 56, 0, ZoneOffset.ofHours(-5)), nullConv, d.apply("1679758496.0")), Arguments.of("Y2K midnight UTC", OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), nullConv, d.apply("946684800.0")), Arguments.of("Maximum positive offset (+18:00)", OffsetDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(18)), nullConv, d.apply("1672466400.0")), Arguments.of("Maximum negative offset (‑18:00) on the day before the epoch", OffsetDateTime.of(1969, 12, 31, 0, 0, 0, 0, ZoneOffset.ofHours(-18)), nullConv, d.apply("-21600.0")), Arguments.of("Offset with hours, minutes and seconds (+05:30:15) and nanosecond fraction", OffsetDateTime.of(2023, 3, 25, 12, 34, 56, 789_123_456, ZoneOffset.ofHoursMinutesSeconds(5, 30, 15)), nullConv, d.apply("1679720681.7891235")), // ---- Group: Converter parameter non‑null (ignored) ----
        Arguments.of("Non‑null converter supplied – result must be identical to null‑converter case", OffsetDateTime.of(2023, 3, 25, 12, 34, 56, 0, ZoneOffset.ofHours(-5)), dummyConv, d.apply("1679758496.0")), // ---- Group: Different offsets, same instant ----
        Arguments.of("Offset +02:00 with zero nanoseconds (instant 2023‑03‑25T10:34:56Z)", OffsetDateTime.of(2023, 3, 25, 12, 34, 56, 0, ZoneOffset.ofHours(2)), nullConv, d.apply("1679740496.0")), Arguments.of("Offset +00:00 representing the same instant as previous case", OffsetDateTime.of(2023, 3, 25, 10, 34, 56, 0, ZoneOffset.UTC), nullConv, d.apply("1679740496.0")));
    }

    private static Stream<Arguments> dateTimeExceptionProvider() {
        return Stream.of(Arguments.of("Year exceeds the maximum supported by java.time.Instant", OffsetDateTime.of(1_000_000_001, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)), Arguments.of("Year below the minimum supported by java.time.Instant", OffsetDateTime.of(-1_000_000_001, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)), Arguments.of("Maximum year with large negative offset pushes instant beyond Instant.MAX", OffsetDateTime.of(1_000_000_000, 12, 31, 23, 59, 59, 999_999_999, ZoneOffset.ofHours(-18))), Arguments.of("Minimum year with large positive offset pushes instant before Instant.MIN", OffsetDateTime.of(-1_000_000_000, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(18))));
    }

    @Test
    void toDouble_withUtcOffset_returnsCorrectDouble() {
        // GIVEN
        OffsetDateTime odt = OffsetDateTime.of(// 2020‑01‑02
        2020, // 2020‑01‑02
        1, // 2020‑01‑02
        2, // 03:04:05
        3, // 03:04:05
        4, // 03:04:05
        5, // nanoseconds
        123_456_789, // UTC offset
        ZoneOffset.UTC);
        Instant instant = odt.toInstant();
        double expected = instant.getEpochSecond() + instant.getNano() / 1_000_000_000.0;
        // WHEN
        double result = OffsetDateTimeConversions.toDouble(odt, converter);
        // THEN
        assertEquals(expected, result, 1e-12, "Conversion from OffsetDateTime to double should match epoch seconds plus nanos fraction");
    }

    @Test
    void toDouble_withPositiveOffset_returnsCorrectDouble() {
        // GIVEN
        // Offset of +02:30 (2 hours 30 minutes)
        OffsetDateTime odt = OffsetDateTime.of(2021, 12, 31, 23, 59, 59, 987_654_321, ZoneOffset.ofHoursMinutes(2, 30));
        // instant is always in UTC
        Instant instant = odt.toInstant();
        double expected = instant.getEpochSecond() + instant.getNano() / 1_000_000_000.0;
        // WHEN
        double result = OffsetDateTimeConversions.toDouble(odt, converter);
        // THEN
        assertEquals(expected, result, 1e-12, "Conversion should correctly handle non‑UTC offsets");
    }

    @Test
    void toDouble_withNegativeOffset_returnsCorrectDouble() {
        // GIVEN
        // Offset of -05:45 (minus 5 hours 45 minutes)
        OffsetDateTime odt = OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHoursMinutes(-5, -45));
        // should be 5h45m after epoch (i.e., +20700 seconds)
        Instant instant = odt.toInstant();
        double expected = instant.getEpochSecond() + instant.getNano() / 1_000_000_000.0;
        // WHEN
        double result = OffsetDateTimeConversions.toDouble(odt, converter);
        // THEN
        assertEquals(expected, result, 1e-12, "Conversion should correctly handle negative offsets");
    }

    @Test
    void toDouble_nullInput_throwsNullPointerException() {
        // GIVEN
        OffsetDateTime nullOdt = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toDouble(nullOdt, converter), "Passing a null OffsetDateTime should result in NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionProvider")
    void toDouble_validConversions(String description, OffsetDateTime from, Converter conv, double expected) {
        // WHEN
        double result = OffsetDateTimeConversions.toDouble(from, conv);
        // THEN
        // Use a small tolerance for fractional seconds; for whole‑second values a tolerance of 0 works.
        double tolerance = (Math.abs(expected - Math.rint(expected)) < 1e-12) ? 0.0 : 1e-12;
        assertEquals(expected, result, tolerance, () -> "Failed conversion: " + description);
    }

    @Test
    void toDouble_nullFrom_throwsNullPointerException() {
        // GIVEN
        OffsetDateTime nullOdt = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toDouble(nullOdt, converter), "Passing a null OffsetDateTime should result in NullPointerException");
    }

    @Test
    void toDouble_wrongType_throwsClassCastException() {
        // GIVEN
        Object notAnOffsetDateTime = "2023-03-25T12:34:56+02:00";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> OffsetDateTimeConversions.toDouble(notAnOffsetDateTime, converter), "Providing a non‑OffsetDateTime argument should throw ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("dateTimeExceptionProvider")
    void toDouble_dateTimeExceptionScenarios(String description, OffsetDateTime from) {
        // WHEN / THEN
        assertThrows(DateTimeException.class, () -> OffsetDateTimeConversions.toDouble(from, converter), () -> "Expected DateTimeException for case: " + description);
    }
}
