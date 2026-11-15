package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toLocalTime_Test {

    /**
     * The Converter used for all test cases.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Converter requires a ConverterOptions instance – the default constructor yields a
        // fully functional options object with the built‑in conversions registered.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up after each test.
        converter = null;
    }

    static Stream<Arguments> validConversionProvider() {
        return Stream.of(// ZonedDateTime – same local time component
        Arguments.of("ZonedDateTime input – should return its local time", ZonedDateTime.of(2023, 5, 10, 14, 30, 45, 0, ZoneId.of("UTC")), LocalTime.of(14, 30, 45)), // OffsetDateTime – local time preserved
        Arguments.of("OffsetDateTime input – should preserve its local time", OffsetDateTime.of(2023, 5, 10, 9, 15, 0, 0, ZoneOffset.ofHours(-5)), LocalTime.of(9, 15, 0)), // LocalDateTime – system default zone assumed (UTC in CI)
        Arguments.of("LocalDateTime input – should return its time component", LocalDateTime.of(2023, 5, 10, 23, 59, 59), LocalTime.of(23, 59, 59)), // java.util.Date – interpreted as an instant in UTC
        Arguments.of("Date input – should yield the instant's time component (UTC)", new Date(Instant.parse("2023-01-01T12:00:00Z").toEpochMilli()), LocalTime.of(12, 0, 0)), // java.sql.Timestamp – behaves like Date
        Arguments.of("Timestamp input – should yield the same time as the underlying instant", Timestamp.valueOf("2023-01-01 12:00:00"), LocalTime.of(12, 0, 0)), // Long epoch‑millis – interpreted as UTC instant
        Arguments.of("Long epoch‑millis input – should produce the corresponding UTC time", Instant.parse("2023-01-01T12:00:00Z").toEpochMilli(), LocalTime.of(12, 0, 0)), // ISO‑8601 String with offset
        Arguments.of("ISO‑8601 string with offset – should parse and return its local time", "2023-07-15T08:45:30+02:00", LocalTime.of(8, 45, 30)));
    }

    static Stream<Arguments> invalidInputProvider() {
        return Stream.of(// String without offset – cannot be parsed to ZonedDateTime
        Arguments.of("String without offset – should throw DateTimeParseException", "08:45:30", DateTimeParseException.class), // Null source object
        Arguments.of("Null source – should throw NullPointerException", null, NullPointerException.class), // Unsupported type Boolean
        Arguments.of("Boolean input – should throw IllegalArgumentException", Boolean.TRUE, IllegalArgumentException.class));
    }

    static Stream<Arguments> malformedStringProvider() {
        return Stream.of(Arguments.of("Impossible date/time values – should throw DateTimeParseException", "2023-02-30T25:61:61Z", DateTimeParseException.class), Arguments.of("Completely non‑date string – should throw DateTimeParseException", "not-a-date-time", DateTimeParseException.class));
    }

    static Stream<Arguments> unsupportedTemporalProvider() {
        return Stream.of(Arguments.of("OffsetTime input – lacks date component, should throw DateTimeException", OffsetTime.of(12, 30, 0, 0, ZoneOffset.UTC), DateTimeException.class), Arguments.of("LocalTime input – no date/zone, should throw DateTimeException", LocalTime.of(12, 30), DateTimeException.class), Arguments.of("LocalDate input – no time, should throw DateTimeException", LocalDate.of(2023, 5, 10), DateTimeException.class), Arguments.of("java.sql.Date input – date‑only, should throw DateTimeException", java.sql.Date.valueOf("2023-05-10"), DateTimeException.class));
    }

    static Stream<Arguments> unsupportedObjectProvider() {
        return Stream.of(Arguments.of("Arbitrary Object – not a recognised source type", new Object(), IllegalArgumentException.class), Arguments.of("Collection (ArrayList) – unsupported source type", new ArrayList<>(), IllegalArgumentException.class));
    }

    /**
     * Verify that an {@link OffsetDateTime} is converted to the correct {@link LocalTime}.
     */
    @Test
    void toLocalTime_fromOffsetDateTime() {
        // Arrange
        OffsetDateTime odt = OffsetDateTime.of(// date
        2023, // date
        5, // date
        10, // time
        15, // time
        30, // time
        45, // time
        0, // +02:00
        ZoneOffset.ofHours(2));
        // Act
        LocalTime result = OffsetDateTimeConversions.toLocalTime(odt, converter);
        // Assert
        assertEquals(LocalTime.of(15, 30, 45), result, "The LocalTime extracted from the OffsetDateTime should match the original time component.");
    }

    /**
     * Verify that an ISO‑8601 formatted {@link String} representing an offset date‑time is
     * correctly converted to a {@link LocalTime}.
     */
    @Test
    void toLocalTime_fromString() {
        // Arrange
        String isoString = "2023-05-10T08:15:20+01:00";
        // Act
        LocalTime result = OffsetDateTimeConversions.toLocalTime(isoString, converter);
        // Assert
        assertEquals(LocalTime.of(8, 15, 20), result, "The LocalTime extracted from the ISO‑8601 string should reflect the time component.");
    }

    /**
     * Verify that a {@link ZonedDateTime} is converted to the correct {@link LocalTime}.
     */
    @Test
    void toLocalTime_fromZonedDateTime() {
        // Arrange
        ZonedDateTime zdt = ZonedDateTime.of(2021, 12, 31, 23, 59, 59, 0, ZoneId.of("America/New_York"));
        // Act
        LocalTime result = OffsetDateTimeConversions.toLocalTime(zdt, converter);
        // Assert
        assertEquals(LocalTime.of(23, 59, 59), result, "The LocalTime extracted from the ZonedDateTime should match its time component.");
    }

    /**
     * Verify that a {@link LocalDateTime} (which has no zone/offset) is treated as a
     * {@link ZonedDateTime} in the system default zone and the resulting {@link LocalTime}
     * matches the original time component.
     */
    @Test
    void toLocalTime_fromLocalDateTime() {
        // Arrange
        LocalDateTime ldt = LocalDateTime.of(2022, 3, 14, 12, 0, 0);
        // Act
        LocalTime result = OffsetDateTimeConversions.toLocalTime(ldt, converter);
        // Assert
        assertEquals(LocalTime.of(12, 0, 0), result, "The LocalTime extracted from a LocalDateTime should be its time component.");
    }

    /* --------------------------------------------------------------
     *  Valid conversion test cases (parameterized)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionProvider")
    void toLocalTime_validConversions(String description, Object from, LocalTime expected) {
        // Act
        LocalTime result = OffsetDateTimeConversions.toLocalTime(from, null);
        // Assert
        assertEquals(expected, result, description);
    }

    /* --------------------------------------------------------------
     *  Invalid input test cases – expecting exceptions
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidInputProvider")
    void toLocalTime_invalidInputs(String description, Object from, Class<? extends Throwable> expectedException) {
        // Act & Assert
        assertThrows(expectedException, () -> OffsetDateTimeConversions.toLocalTime(from, null), description);
    }

    /* --------------------------------------------------------------
     *  Malformed string inputs – expecting DateTimeParseException
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("malformedStringProvider")
    void toLocalTime_malformedStrings(String description, String malformed, Class<? extends Throwable> expected) {
        assertThrows(expected, () -> OffsetDateTimeConversions.toLocalTime(malformed, null), description);
    }

    /* --------------------------------------------------------------
     *  Unsupported temporal types – expecting DateTimeException
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("unsupportedTemporalProvider")
    void toLocalTime_unsupportedTemporalTypes(String description, Object from, Class<? extends Throwable> expected) {
        assertThrows(expected, () -> OffsetDateTimeConversions.toLocalTime(from, null), description);
    }

    /* --------------------------------------------------------------
     *  Completely unsupported object types – expecting IllegalArgumentException
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("unsupportedObjectProvider")
    void toLocalTime_unsupportedObjects(String description, Object from, Class<? extends Throwable> expected) {
        assertThrows(expected, () -> OffsetDateTimeConversions.toLocalTime(from, null), description);
    }
}
