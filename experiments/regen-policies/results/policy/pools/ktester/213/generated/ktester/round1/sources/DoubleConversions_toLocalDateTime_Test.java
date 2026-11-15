package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import java.util.stream.*;

class DoubleConversions_toLocalDateTime_Test {

    /**
     * Shared Converter instance used by all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter requires a ConverterOptions instance – the default constructor provides sensible defaults.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // No external resources to clean up; just null‑ify the reference.
        converter = null;
    }

    static Stream<Arguments> successfulConversionProvider() {
        ZoneId systemZone = ZoneId.systemDefault();
        return Stream.of(// Direct LocalDateTime – should be returned unchanged
        Arguments.of(LocalDateTime.of(2023, 5, 10, 15, 30, 45, 123_000_000), "Direct conversion when the source object is already a LocalDateTime – the method should return the same value.", LocalDateTime.of(2023, 5, 10, 15, 30, 45, 123_000_000)), // ZonedDateTime – zone stripped
        Arguments.of(ZonedDateTime.of(2023, 5, 10, 15, 30, 45, 0, ZoneId.of("UTC")), "ZonedDateTime is converted to a LocalDateTime by discarding the zone information.", LocalDateTime.of(2023, 5, 10, 15, 30, 45)), // OffsetDateTime – offset preserved, then stripped
        Arguments.of(OffsetDateTime.of(2023, 5, 10, 15, 30, 45, 0, ZoneOffset.ofHours(-5)), "OffsetDateTime is first turned into a ZonedDateTime (preserving the offset) and then to a LocalDateTime – the local fields stay unchanged.", LocalDateTime.of(2023, 5, 10, 15, 30, 45)), // Instant – interpreted in system default zone
        Arguments.of(Instant.parse("2023-05-10T15:30:45Z"), "Instant is interpreted in the JVM default time‑zone and then converted to a LocalDateTime.", Instant.parse("2023-05-10T15:30:45Z").atZone(systemZone).toLocalDateTime()), // java.util.Date – epoch millis conversion
        Arguments.of(// 2023‑03‑20T12:00:00Z
        new Date(1_680_000_000_000L), "java.util.Date is converted via its epoch‑millis representation.", Instant.ofEpochMilli(1_680_000_000_000L).atZone(systemZone).toLocalDateTime()), // Calendar with explicit zone
        Arguments.of(Calendar.getInstance(TimeZone.getTimeZone("America/New_York")), "Calendar (with explicit zone) is transformed to a ZonedDateTime and then to a LocalDateTime.", ZonedDateTime.ofInstant(((Calendar) Calendar.getInstance(TimeZone.getTimeZone("America/New_York"))).toInstant(), ((Calendar) Calendar.getInstance(TimeZone.getTimeZone("America/New_York"))).getTimeZone().toZoneId()).toLocalDateTime()), // Long epoch‑millis
        Arguments.of(Long.valueOf(1_680_000_000_000L), "A raw epoch‑millis Long is treated like a Date/Instant.", Instant.ofEpochMilli(1_680_000_000_000L).atZone(systemZone).toLocalDateTime()), // LocalDate – start of day
        Arguments.of(LocalDate.of(2023, 5, 10), "LocalDate is converted to the start of day (midnight) LocalDateTime.", LocalDateTime.of(2023, 5, 10, 0, 0)), // java.sql.Timestamp – nanosecond precision retained
        Arguments.of(Timestamp.valueOf("2023-05-10 15:30:45.123"), "java.sql.Timestamp carries nanosecond precision and should be converted accurately.", Timestamp.valueOf("2023-05-10 15:30:45.123").toLocalDateTime()));
    }

    static Stream<Arguments> unsupportedTypeProvider() {
        return Stream.of(Arguments.of(Boolean.TRUE, "Unsupported source type (Boolean) must cause a ConversionException."), Arguments.of(LocalTime.of(15, 30, 45), "LocalTime alone does not contain a date component; conversion should fail."), Arguments.of("2023-05-10T15:30:45", "String values are not part of the supported conversion set for toLocalDateTime."));
    }

    static Stream<Arguments> exceptionScenarioProvider() {
        return Stream.of(Arguments.of(OffsetTime.of(15, 30, 45, 0, ZoneOffset.ofHours(-5)), "OffsetTime lacks a date component; conversion to LocalDateTime should fail.", ConversionException.class), Arguments.of(Duration.ofHours(5), "Duration cannot be interpreted as a date‑time; method must throw ConversionException.", ConversionException.class), Arguments.of(new GregorianCalendar(), "Converter argument is null; internal call to toZonedDateTime will throw NPE.", NullPointerException.class), Arguments.of(new Object(), "Arbitrary object type is unsupported for conversion to LocalDateTime.", ConversionException.class), Arguments.of("2023-13-40T25:61:61", "Malformed date‑time string cannot be parsed; conversion should raise ConversionException.", ConversionException.class));
    }

    /**
     * Verify that a {@link LocalDateTime} is returned unchanged (apart from possible zone normalisation).
     */
    @Test
    void toLocalDateTime_givenLocalDateTime_returnsSameValue() {
        // truncate nanos for deterministic comparison
        LocalDateTime now = LocalDateTime.now().withNano(0);
        LocalDateTime result = DoubleConversions.toLocalDateTime(now, converter);
        assertEquals(now, result, "Conversion of a LocalDateTime should return the same value");
    }

    /**
     * Verify conversion from {@link ZonedDateTime} – the zone information should be stripped.
     */
    @Test
    void toLocalDateTime_givenZonedDateTime_stripsZone() {
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 17, 14, 30, 45, 0, ZoneId.of("Europe/Paris"));
        LocalDateTime expected = zdt.toLocalDateTime();
        LocalDateTime result = DoubleConversions.toLocalDateTime(zdt, converter);
        assertEquals(expected, result, "ZonedDateTime should be converted to its LocalDateTime component");
    }

    /**
     * Verify conversion from {@link java.util.Date}. The conversion uses the system default time‑zone.
     */
    @Test
    void toLocalDateTime_givenDate_convertsCorrectly() {
        // Create a deterministic Date (epoch millis)
        // 2022-01-01T12:00:00Z
        Date date = new Date(1_640_995_200_000L);
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime expected = ZonedDateTime.ofInstant(date.toInstant(), zone).toLocalDateTime();
        LocalDateTime result = DoubleConversions.toLocalDateTime(date, converter);
        assertEquals(expected, result, "Date should be converted to a LocalDateTime using the system default zone");
    }

    /**
     * Verify conversion from {@link java.sql.Timestamp}.
     */
    @Test
    void toLocalDateTime_givenTimestamp_convertsCorrectly() {
        // Timestamp with nanosecond precision
        Timestamp ts = Timestamp.valueOf("2023-08-15 09:45:30.123456789");
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime expected = ZonedDateTime.ofInstant(ts.toInstant(), zone).toLocalDateTime();
        LocalDateTime result = DoubleConversions.toLocalDateTime(ts, converter);
        assertEquals(expected, result, "Timestamp should be converted to a LocalDateTime using the system default zone");
    }

    /**
     * Verify that a {@code null} source returns {@code null}.
     */
    @Test
    void toLocalDateTime_givenNull_returnsNull() {
        assertNull(DoubleConversions.toLocalDateTime(null, converter), "When the source object is null the method should return null (no conversion performed).");
    }

    /**
     * Parameterised test covering all successful conversion scenarios.
     *
     * Each {@link Arguments} entry contains:
     * <ul>
     *   <li>the source object (the {@code from} parameter)</li>
     *   <li>a description string (used for the test display name)</li>
     *   <li>the expected {@link LocalDateTime} result</li>
     * </ul>
     */
    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("successfulConversionProvider")
    void toLocalDateTime_successfulConversions(Object from, String description, LocalDateTime expected) {
        LocalDateTime result = DoubleConversions.toLocalDateTime(from, converter);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("unsupportedTypeProvider")
    void toLocalDateTime_unsupportedTypes_throwConversionException(Object from, String description) {
        assertThrows(ConversionException.class, () -> DoubleConversions.toLocalDateTime(from, converter), description);
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("exceptionScenarioProvider")
    void toLocalDateTime_exceptionScenarios(Object from, String description, Class<? extends Throwable> expectedException) {
        // For the case where the converter itself is null we expect a NPE from the internal call.
        if (expectedException == NullPointerException.class) {
            assertThrows(NullPointerException.class, () -> DoubleConversions.toLocalDateTime(from, null), description);
        } else {
            assertThrows(expectedException, () -> DoubleConversions.toLocalDateTime(from, converter), description);
        }
    }
}
