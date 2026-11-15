package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.TimeZone;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TimestampConversions_toCalendar_Test {

    /**
     * Mocked {@link Converter} used in the tests.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Mocked {@link ConverterOptions} returned by the converter.
     */
    @Mock
    private ConverterOptions mockOptions;

    /**
     * A fixed timestamp used for all test cases.
     */
    private Timestamp testTimestamp;

    /**
     * The expected time‑zone for the calendar (UTC is deterministic).
     */
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a deterministic timestamp: 2023‑01‑01 12:34:56.789 UTC
        // epoch millis
        long millis = 1672578896789L;
        testTimestamp = new Timestamp(millis);
        // Configure the mock hierarchy:
        // converter.getOptions() -> mockOptions
        // mockOptions.getTimeZone() -> UTC
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getTimeZone()).thenReturn(UTC);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks after each test to avoid cross‑test interference.
        clearInvocations(mockConverter, mockOptions);
    }

    private static Stream<Arguments> provideSuccessfulConversions() {
        return Stream.of(// Standard timestamp conversion using UTC
        Arguments.of("Standard timestamp conversion to Calendar using UTC timezone", Timestamp.valueOf("2023-03-15 10:20:30.123"), UTC, 1678879230123L, UTC), // Epoch timestamp (zero milliseconds) using UTC
        Arguments.of("Epoch timestamp (zero milliseconds) conversion using UTC", Timestamp.valueOf("1970-01-01 00:00:00.000"), UTC, 0L, UTC), // Far‑future timestamp using UTC
        Arguments.of("Far‑future timestamp conversion using UTC", Timestamp.valueOf("9999-12-31 23:59:59.999"), UTC, 253402300799999L, UTC), // Same instant but with a non‑UTC zone (America/New_York)
        Arguments.of("Timestamp conversion with a non‑UTC timezone (America/New_York)", Timestamp.valueOf("2023-03-15 10:20:30.123"), TimeZone.getTimeZone("America/New_York"), 1678879230123L, TimeZone.getTimeZone("America/New_York")), // Null time‑zone – Calendar.getInstance(null) falls back to system default
        Arguments.of("Converter returns null timezone – fallback to system default", Timestamp.valueOf("2023-03-15 10:20:30.123"), null, 1678879230123L, TimeZone.getDefault()));
    }

    private static Stream<Arguments> provideExceptionScenarios() {
        // Mocks needed for specific scenarios are created inside the arguments
        // to keep the provider pure and avoid sharing mutable state.
        Converter nullConverter = null;
        // Converter that returns null options
        Converter converterWithNullOptions = mock(Converter.class);
        when(converterWithNullOptions.getOptions()).thenReturn(null);
        // Converter whose options throw a RuntimeException when getTimeZone() is called
        ConverterOptions throwingOptions = mock(ConverterOptions.class);
        when(throwingOptions.getTimeZone()).thenThrow(new RuntimeException("bad zone"));
        Converter converterWithThrowingOptions = mock(Converter.class);
        when(converterWithThrowingOptions.getOptions()).thenReturn(throwingOptions);
        return Stream.of(// Null source object
        Arguments.of("Passing null as the source object should throw NullPointerException", null, // a regular mock – its methods won't be reached
        mock(Converter.class), NullPointerException.class), // Wrong source type (java.util.Date instead of java.sql.Timestamp)
        Arguments.of("Providing a non‑Timestamp object should throw ClassCastException", new java.util.Date(), mock(Converter.class), ClassCastException.class), // Null converter argument
        Arguments.of("Converter argument is null – should throw NullPointerException", Timestamp.valueOf("2023-01-01 00:00:00.000"), nullConverter, NullPointerException.class), // Converter returns null options
        Arguments.of("Converter returns null options – should throw NullPointerException", Timestamp.valueOf("2023-01-01 00:00:00.000"), converterWithNullOptions, NullPointerException.class), // Options throw RuntimeException on getTimeZone()
        Arguments.of("Options.getTimeZone() throws RuntimeException – should propagate", Timestamp.valueOf("2023-01-01 00:00:00.000"), converterWithThrowingOptions, RuntimeException.class));
    }

    @Test
    void toCalendar_convertsTimestampUsingConverterTimeZone() {
        // GIVEN – a Timestamp and a Converter whose options specify UTC
        // (setup performed in @BeforeEach)
        // WHEN – the static method is invoked
        Calendar result = TimestampConversions.toCalendar(testTimestamp, mockConverter);
        // THEN – a Calendar instance is returned with the same instant and the expected time‑zone
        assertNotNull(result, "Resulting Calendar should not be null");
        assertEquals(UTC, result.getTimeZone(), "Calendar should use the Converter's time‑zone");
        assertEquals(testTimestamp.getTime(), result.getTimeInMillis(), "Calendar milliseconds must match the original Timestamp");
    }

    @Test
    void toCalendar_preservesMillisecondPrecision() {
        // GIVEN – a Timestamp with a non‑zero millisecond component
        // includes milliseconds
        Timestamp ts = new Timestamp(1234567890123L);
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getTimeZone()).thenReturn(TimeZone.getTimeZone("America/New_York"));
        // WHEN
        Calendar cal = TimestampConversions.toCalendar(ts, mockConverter);
        // THEN
        assertEquals(ts.getTime(), cal.getTimeInMillis(), "Calendar must preserve the exact millisecond value from the Timestamp");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideSuccessfulConversions")
    void testToCalendar_success(String description, Timestamp from, TimeZone zoneForConverter, long expectedMillis, TimeZone expectedZone) {
        // Arrange – configure the converter to return the desired time‑zone
        when(mockOptions.getTimeZone()).thenReturn(zoneForConverter);
        // Act
        Calendar result = TimestampConversions.toCalendar(from, mockConverter);
        // Assert
        assertNotNull(result, description + " – result must not be null");
        assertEquals(expectedMillis, result.getTimeInMillis(), description + " – milliseconds must match");
        assertEquals(expectedZone, result.getTimeZone(), description + " – time‑zone must match");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideExceptionScenarios")
    void testToCalendar_exceptions(String description, Object from, Converter converter, Class<? extends Throwable> expectedException) {
        // Act & Assert
        Throwable thrown = assertThrows(expectedException, () -> TimestampConversions.toCalendar(from, converter), description + " – expected exception");
        // Optional: verify the exception message contains useful information
        assertNotNull(thrown.getMessage());
    }
}
