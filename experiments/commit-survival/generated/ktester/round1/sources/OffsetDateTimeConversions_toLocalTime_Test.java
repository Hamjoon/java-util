package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.sql.Time;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toLocalTime_Test {

    /**
     * The mock Converter that will be passed to the method under test.
     */
    private Converter mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh mock for each test to avoid cross‑test interference.
        mockConverter = mock(Converter.class);
    }

    /**
     * Supplies the successful conversion arguments.
     */
    private static Stream<Arguments> conversionScenarios() {
        return Stream.of(// 1. OffsetDateTime input
        Arguments.of("Standard OffsetDateTime input should be converted to its local time component.", OffsetDateTime.parse("2023-08-15T10:15:30+02:00"), LocalTime.of(10, 15, 30)), // 2. ZonedDateTime input
        Arguments.of("ZonedDateTime is accepted by toZonedDateTime, resulting in the same local time.", ZonedDateTime.parse("2023-08-15T10:15:30+02:00[Europe/Paris]"), LocalTime.of(10, 15, 30)), // 3. LocalDateTime input (system default zone is used internally)
        Arguments.of("LocalDateTime should be treated as a ZonedDateTime at the system default zone, preserving its time.", LocalDateTime.of(2023, 8, 15, 22, 45, 5), LocalTime.of(22, 45, 5)), // 4. Direct LocalTime input
        Arguments.of("Direct LocalTime input should be returned unchanged.", LocalTime.of(13, 30, 0), LocalTime.of(13, 30, 0)), // 5. String representation of a time
        Arguments.of("String representing a time should be parsed to LocalTime.", "15:45:20", LocalTime.of(15, 45, 20)), // 6. java.sql.Time input
        Arguments.of("java.sql.Time should be converted to LocalTime.", Time.valueOf("08:20:00"), LocalTime.of(8, 20, 0)), // 7. java.util.Date (instant) input – interpreted as UTC by the conversion logic
        Arguments.of("Date (instant) should be interpreted as UTC and converted to its local time component.", Date.from(Instant.parse("2023-08-15T12:00:00Z")), LocalTime.of(12, 0, 0)));
    }

    /**
     * Supplies the exception‑expecting arguments.
     */
    private static Stream<Arguments> exceptionScenarios() {
        return Stream.of(// 1. Non‑parsable string
        Arguments.of("A non‑parsable string should cause a DateTimeParseException when toZonedDateTime attempts to parse it.", "not-a-time", DateTimeParseException.class), // 2. Date‑only string
        Arguments.of("A string containing only a date (no time or zone) cannot be parsed to a ZonedDateTime and should raise DateTimeParseException.", "2023-08-15", DateTimeParseException.class), // 3. Invalid hour value
        Arguments.of("A string with an invalid hour value (25) should trigger a DateTimeParseException during parsing.", "25:00:00", DateTimeParseException.class), // 4. LocalDate without a time component
        Arguments.of("LocalDate lacks a time component; converting it to a ZonedDateTime is invalid and should throw a DateTimeException.", LocalDate.of(2023, 8, 15), DateTimeException.class), // 5. Unsupported Boolean type
        Arguments.of("A Boolean value is not supported for conversion and should result in a ClassCastException.", true, ClassCastException.class), // 6. Null source (propagates NullPointerException)
        Arguments.of("Passing null should result in a NullPointerException from toZonedDateTime.", null, NullPointerException.class), // 7. Unsupported Integer type
        Arguments.of("Unsupported type (e.g., Integer) should cause a ClassCastException during conversion.", 12345, ClassCastException.class));
    }

    /**
     * Verifies that {@code OffsetDateTimeConversions.toLocalTime(Object, Converter)}
     * delegates to {@code toZonedDateTime} and returns the {@link java.time.LocalTime}
     * component of the resulting {@link java.time.ZonedDateTime}.
     */
    @Test
    void toLocalTime_returnsLocalTimeFromZonedDateTime() {
        // GIVEN
        // the actual value is irrelevant – it is mocked.
        Object source = "2023-08-15T12:34:56+02:00";
        ZonedDateTime expectedZdt = ZonedDateTime.of(2023, 8, 15, 12, 34, 56, 0, ZoneOffset.ofHours(2));
        LocalTime expectedLocalTime = expectedZdt.toLocalTime();
        // Mock the static call to toZonedDateTime so that it returns the ZonedDateTime we control.
        try (MockedStatic<OffsetDateTimeConversions> mockedStatic = mockStatic(OffsetDateTimeConversions.class)) {
            mockedStatic.when(() -> OffsetDateTimeConversions.toZonedDateTime(eq(source), eq(mockConverter))).thenReturn(expectedZdt);
            // WHEN
            LocalTime actual = OffsetDateTimeConversions.toLocalTime(source, mockConverter);
            // THEN
            assertEquals(expectedLocalTime, actual, "The returned LocalTime should be the one extracted from the mocked ZonedDateTime.");
            // Verify that the static method was invoked exactly once with the same arguments.
            mockedStatic.verify(() -> OffsetDateTimeConversions.toZonedDateTime(eq(source), eq(mockConverter)), times(1));
        }
    }

    /**
     * Ensures that a {@code null} source value is handled in the same way as the
     * underlying {@code toZonedDateTime} implementation – i.e., the static method
     * is still called and its result (which may be {@code null}) propagates.
     */
    @Test
    void toLocalTime_propagatesNullFromToZonedDateTime() {
        // GIVEN
        Object source = null;
        try (MockedStatic<OffsetDateTimeConversions> mockedStatic = mockStatic(OffsetDateTimeConversions.class)) {
            mockedStatic.when(() -> OffsetDateTimeConversions.toZonedDateTime(eq(source), eq(mockConverter))).thenReturn(null);
            // WHEN / THEN
            assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toLocalTime(source, mockConverter), "When toZonedDateTime returns null, calling toLocalTime should throw NPE.");
            mockedStatic.verify(() -> OffsetDateTimeConversions.toZonedDateTime(eq(source), eq(mockConverter)), times(1));
        }
    }

    /**
     * Executes a series of successful conversion scenarios defined in the JSON test matrix.
     * Each argument supplies the input {@code from} object and the expected {@link LocalTime}.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("conversionScenarios")
    void toLocalTime_successfulConversions(String description, Object from, LocalTime expected) {
        // No special mocking – we rely on the real implementation of toZonedDateTime.
        LocalTime actual = OffsetDateTimeConversions.toLocalTime(from, null);
        assertEquals(expected, actual, description);
    }

    /**
     * Executes a series of inputs that are expected to cause an exception.
     * The expected exception class is asserted via {@link Assertions#assertThrows}.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionScenarios")
    void toLocalTime_expectedExceptions(String description, Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> OffsetDateTimeConversions.toLocalTime(from, null), description);
    }
}
