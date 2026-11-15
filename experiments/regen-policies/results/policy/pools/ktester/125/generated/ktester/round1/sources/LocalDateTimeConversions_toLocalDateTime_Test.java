package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.sql.Timestamp;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toLocalDateTime_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use a default Converter – this mirrors the behaviour of Converter.DEFAULT
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally
        converter = null;
    }

    /**
     * Helper that compares the result of the static method with the Converter's own conversion.
     */
    private void assertConversionEquals(Object source) {
        LocalDateTime expected = converter.convert(source, LocalDateTime.class);
        LocalDateTime actual = LocalDateTimeConversions.toLocalDateTime(source, converter);
        assertEquals(expected, actual, () -> "Conversion mismatch for source type " + (source == null ? "null" : source.getClass()));
    }

    /**
     * Parses an ISO‑8601 string into a {@link LocalDateTime}.
     */
    private static LocalDateTime parseExpected(String iso) {
        return LocalDateTime.parse(iso);
    }

    /**
     * Calls the conversion method and asserts the result equals the expected LocalDateTime.
     */
    private static void assertConverted(Object source, String expectedIso) {
        LocalDateTime expected = parseExpected(expectedIso);
        LocalDateTime actual = LocalDateTimeConversions.toLocalDateTime(source, converter);
        assertEquals(expected, actual, () -> "Conversion of " + (source == null ? "null" : source.getClass()) + " did not produce the expected LocalDateTime");
    }

    /**
     * Provides the successful conversion scenarios extracted from the JSON spec.
     */
    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Direct LocalDateTime – should be returned unchanged
        Arguments.of(LocalDateTime.of(2023, 5, 10, 15, 30, 45), "2023-05-10T15:30:45"), // ZonedDateTime – zone offset is discarded
        Arguments.of(ZonedDateTime.of(2023, 5, 10, 15, 30, 45, 0, ZoneOffset.ofHours(-5)), "2023-05-10T15:30:45"), // OffsetDateTime – offset ignored
        Arguments.of(OffsetDateTime.of(2023, 5, 10, 15, 30, 45, 0, ZoneOffset.ofHours(2)), "2023-05-10T15:30:45"), // java.util.Date – interpreted in the default (UTC) zone
        Arguments.of(new Date(1683738645000L), "2023-05-10T15:30:45"), // java.sql.Timestamp – nanoseconds are preserved
        Arguments.of(Timestamp.valueOf("2023-05-10 15:30:45.123"), "2023-05-10T15:30:45.123"), // LocalDate – time defaults to midnight
        Arguments.of(LocalDate.of(2023, 5, 10), "2023-05-10T00:00"), // LocalTime – date defaults to epoch (1970‑01‑01)
        Arguments.of(LocalTime.of(15, 30, 45), "1970-01-01T15:30:45"), // Long epoch‑millis – interpreted as an instant in the default zone
        Arguments.of(1683738645000L, "2023-05-10T15:30:45"), // ISO‑8601 String
        Arguments.of("2023-05-10T15:30:45", "2023-05-10T15:30:45"));
    }

    /**
     * Provides the exception scenarios extracted from the JSON spec.
     */
    private static Stream<Arguments> exceptionConversionProvider() {
        return Stream.of(// Unsupported Boolean source
        Arguments.of(Boolean.TRUE, Converter.DEFAULT, IllegalArgumentException.class), // Malformed ISO‑8601 string
        Arguments.of("2023/05/10 15:30:45", Converter.DEFAULT, DateTimeParseException.class), // Numeric type other than Long (BigDecimal)
        Arguments.of(new java.math.BigDecimal("12345.67"), Converter.DEFAULT, IllegalArgumentException.class), // Null Converter argument – should NPE before any conversion logic
        Arguments.of("2023-05-10T15:30:45", null, NullPointerException.class), // Empty string – parsing fails
        Arguments.of("", Converter.DEFAULT, DateTimeParseException.class));
    }

    @Test
    void testNullInput() {
        assertNull(LocalDateTimeConversions.toLocalDateTime(null, converter), "Null source should produce null LocalDateTime");
    }

    @Test
    void testLocalDateTimeInput() {
        // strip nanos for deterministic comparison
        LocalDateTime now = LocalDateTime.now().withNano(0);
        assertConversionEquals(now);
    }

    @Test
    void testDateInput() {
        Date date = Date.from(Instant.parse("2023-03-15T10:15:30Z"));
        assertConversionEquals(date);
    }

    @Test
    void testTimestampInput() {
        Timestamp ts = Timestamp.from(Instant.parse("2022-12-01T08:45:00Z"));
        assertConversionEquals(ts);
    }

    @Test
    void testInstantInput() {
        Instant instant = Instant.parse("2021-07-20T14:30:45Z");
        assertConversionEquals(instant);
    }

    @Test
    void testLocalDateInput() {
        LocalDate date = LocalDate.of(2020, Month.APRIL, 5);
        assertConversionEquals(date);
    }

    @Test
    void testLocalTimeInput() {
        LocalTime time = LocalTime.of(13, 20, 45);
        assertConversionEquals(time);
    }

    @Test
    void testCalendarInput() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTime(Date.from(Instant.parse("2019-11-11T11:11:11Z")));
        assertConversionEquals(cal);
    }

    @Test
    void testStringInput() {
        // ISO‑8601 format is understood by the Converter
        String isoString = "2024-01-02T16:30:00";
        assertConversionEquals(isoString);
    }

    @Test
    void testMapInput() {
        // The Converter can build a LocalDateTime from a map with "time" and optional "nanos"
        Map<String, Object> map = new HashMap<>();
        // 2023-01-01T00:00:00Z in epoch millis
        map.put("time", 1672531200000L);
        map.put("nanos", 0);
        assertConversionEquals(map);
    }

    @ParameterizedTest(name = "[{index}] convert {0} → {1}")
    @MethodSource("validConversionProvider")
    void testValidConversions(Object source, String expectedIso) {
        assertConverted(source, expectedIso);
    }

    @ParameterizedTest(name = "[{index}] convert {0} (converter={1}) → throws {2}")
    @MethodSource("exceptionConversionProvider")
    void testConversionExceptions(Object source, Converter suppliedConverter, Class<? extends Throwable> expectedException) {
        // When suppliedConverter is null we pass null directly; otherwise we use the default instance.
        Converter conv = suppliedConverter != null ? suppliedConverter : null;
        assertThrows(expectedException, () -> LocalDateTimeConversions.toLocalDateTime(source, conv), () -> "Expected " + expectedException.getSimpleName() + " for source: " + source);
    }

    @Test
    void testNullSourceThrowsNPE() {
        // According to the spec, a null source triggers a NullPointerException
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toLocalDateTime(null, converter));
    }
}
