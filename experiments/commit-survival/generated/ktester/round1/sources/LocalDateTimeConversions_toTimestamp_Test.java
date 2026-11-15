package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toTimestamp_Test {

    @Mock
    private Converter converter;

    @Mock
    private ConverterOptions options;

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mock Converter to return the mocked options
        when(converter.getOptions()).thenReturn(options);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks after each test to avoid cross‑test interference
        reset(converter, options);
    }

    private static Stream<Arguments> provideSuccessfulConversions() {
        return Stream.of(// Standard UTC conversion
        Arguments.of("Standard conversion (UTC)", LocalDateTime.of(2023, 5, 15, 10, 30, 45, 123_000_000), ZoneOffset.UTC, Timestamp.from(LocalDateTime.of(2023, 5, 15, 10, 30, 45, 123_000_000).atZone(ZoneOffset.UTC).toInstant())), // Non‑UTC zone (America/New_York) – DST in December => UTC‑5
        Arguments.of("Non‑UTC zone (America/New_York) – winter offset", LocalDateTime.of(2023, 12, 25, 8, 15, 30, 0), ZoneId.of("America/New_York"), Timestamp.from(LocalDateTime.of(2023, 12, 25, 8, 15, 30, 0).atZone(ZoneId.of("America/New_York")).toInstant())), // Epoch start
        Arguments.of("Epoch start (1970‑01‑01T00:00:00Z)", LocalDateTime.of(1970, 1, 1, 0, 0, 0, 0), ZoneOffset.UTC, Timestamp.from(Instant.EPOCH)), // Upper bound (year 9999)
        Arguments.of("Upper bound year 9999", LocalDateTime.of(9999, 12, 31, 23, 59, 59, 999_000_000), ZoneOffset.UTC, Timestamp.from(LocalDateTime.of(9999, 12, 31, 23, 59, 59, 999_000_000).atZone(ZoneOffset.UTC).toInstant())), // Half‑hour offset (Asia/Kolkata, UTC+5:30)
        Arguments.of("Half‑hour offset zone (Asia/Kolkata)", LocalDateTime.of(2023, 5, 15, 10, 30, 45, 0), ZoneId.of("Asia/Kolkata"), Timestamp.from(LocalDateTime.of(2023, 5, 15, 10, 30, 45, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant())), // DST gap (Europe/Paris) – 2023‑03‑26 02:30 does not exist, Java resolves to +02:00
        Arguments.of("DST gap handling (Europe/Paris)", LocalDateTime.of(2023, 3, 26, 2, 30, 0, 0), ZoneId.of("Europe/Paris"), Timestamp.from(LocalDateTime.of(2023, 3, 26, 2, 30, 0, 0).atZone(ZoneId.of("Europe/Paris")).toInstant())), // Nanosecond precision beyond milliseconds
        Arguments.of("Nanosecond precision (123 456 789 ns)", LocalDateTime.of(2023, 5, 15, 10, 30, 45, 123_456_789), ZoneOffset.UTC, Timestamp.from(LocalDateTime.of(2023, 5, 15, 10, 30, 45, 123_456_789).atZone(ZoneOffset.UTC).toInstant())), // Pre‑epoch year -1 (i.e. 2 BC)
        Arguments.of("Pre‑epoch year -1", LocalDateTime.of(-1, 12, 31, 23, 59, 59, 0), ZoneOffset.UTC, Timestamp.from(LocalDateTime.of(-1, 12, 31, 23, 59, 59, 0).atZone(ZoneOffset.UTC).toInstant())), // Short UTC identifier "Z"
        Arguments.of("Short UTC identifier \"Z\"", LocalDateTime.of(2023, 5, 15, 10, 30, 45, 0), ZoneId.of("Z"), Timestamp.from(LocalDateTime.of(2023, 5, 15, 10, 30, 45, 0).atZone(ZoneId.of("Z")).toInstant())));
    }

    private static Stream<Arguments> provideExceptionalScenarios() {
        // Helper mock for a converter whose options are null
        Converter converterWithNullOptions = mock(Converter.class);
        when(converterWithNullOptions.getOptions()).thenReturn(null);
        // Helper mock for a converter whose options return null ZoneId
        ConverterOptions nullZoneOptions = mock(ConverterOptions.class);
        when(nullZoneOptions.getZoneId()).thenReturn(null);
        Converter converterWithNullZone = mock(Converter.class);
        when(converterWithNullZone.getOptions()).thenReturn(nullZoneOptions);
        return Stream.of(// Null input LocalDateTime
        Arguments.of("Null LocalDateTime input", null, ZoneOffset.UTC, // normal mock, will not be used because NPE occurs earlier
        mock(Converter.class), NullPointerException.class), // Wrong type (LocalDate) – will cause ClassCastException during cast
        Arguments.of("Invalid type (LocalDate) instead of LocalDateTime", // we cannot pass LocalDate as LocalDateTime, so we use Object cast later
        null, ZoneOffset.UTC, mock(Converter.class), ClassCastException.class), // Null converter argument
        Arguments.of("Null converter argument", LocalDateTime.of(2023, 5, 15, 12, 0, 0, 0), ZoneOffset.UTC, null, NullPointerException.class), // Converter with null options
        Arguments.of("Converter with null options", LocalDateTime.of(2023, 5, 15, 12, 0, 0, 0), ZoneOffset.UTC, converterWithNullOptions, NullPointerException.class), // Options with null ZoneId
        Arguments.of("Converter options with null ZoneId", LocalDateTime.of(2023, 5, 15, 12, 0, 0, 0), null, converterWithNullZone, NullPointerException.class), // LocalDateTime beyond Timestamp max instant
        Arguments.of("LocalDateTime beyond max Timestamp instant", LocalDateTime.of(292_278_994, 8, 18, 0, 0, 0, 0), ZoneOffset.UTC, mock(Converter.class), IllegalArgumentException.class), // LocalDateTime before min Timestamp instant
        Arguments.of("LocalDateTime before min Timestamp instant", LocalDateTime.of(-292_278_994, 8, 17, 0, 0, 0, 0), ZoneOffset.UTC, mock(Converter.class), IllegalArgumentException.class));
    }

    @Test
    void toTimestamp_WithUtcZone_ReturnsCorrectTimestamp() {
        // GIVEN
        ZoneId zone = ZoneOffset.UTC;
        when(options.getZoneId()).thenReturn(zone);
        LocalDateTime ldt = LocalDateTime.of(2023, 5, 10, 12, 30, 45, 123_000_000);
        Timestamp expected = Timestamp.from(ldt.atZone(zone).toInstant());
        // WHEN
        Timestamp actual = LocalDateTimeConversions.toTimestamp(ldt, converter);
        // THEN
        assertEquals(expected, actual);
    }

    @Test
    void toTimestamp_WithNonUtcZone_ReturnsCorrectTimestamp() {
        // GIVEN
        // UTC‑5 (or UTC‑4 with DST)
        ZoneId zone = ZoneId.of("America/New_York");
        when(options.getZoneId()).thenReturn(zone);
        LocalDateTime ldt = LocalDateTime.of(2023, 12, 25, 8, 15, 30, 0);
        Timestamp expected = Timestamp.from(ldt.atZone(zone).toInstant());
        // WHEN
        Timestamp actual = LocalDateTimeConversions.toTimestamp(ldt, converter);
        // THEN
        assertEquals(expected, actual);
    }

    @Test
    void toTimestamp_WithNonLocalDateTimeInput_throwsClassCastException() {
        // GIVEN
        when(options.getZoneId()).thenReturn(ZoneOffset.UTC);
        Object notALocalDateTime = "2023-01-01T00:00:00";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> LocalDateTimeConversions.toTimestamp(notALocalDateTime, converter));
    }

    /* --------------------------------------------------------------
     *  Parameterized standard & additional conversion test cases
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideSuccessfulConversions")
    void toTimestamp_SuccessfulConversions(String description, LocalDateTime from, ZoneId zone, Timestamp expected) {
        // GIVEN
        when(options.getZoneId()).thenReturn(zone);
        // WHEN
        Timestamp actual = LocalDateTimeConversions.toTimestamp(from, converter);
        // THEN
        assertEquals(expected, actual, "Timestamp mismatch for: " + description);
        // For the nanosecond‑precision case also verify the nanos field
        if (expected.getNanos() != 0) {
            assertEquals(expected.getNanos(), actual.getNanos(), "Nanosecond field mismatch for: " + description);
        }
    }

    /* --------------------------------------------------------------
     *  Parameterized exceptional test cases
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideExceptionalScenarios")
    void toTimestamp_ExceptionalScenarios(String description, LocalDateTime from, ZoneId zone, Converter providedConverter, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        assertThrows(expectedException, () -> LocalDateTimeConversions.toTimestamp(from, providedConverter), "Expected exception for: " + description);
    }

    @Test
    void toTimestamp_NullFrom_throwsNullPointerException() {
        // GIVEN
        when(options.getZoneId()).thenReturn(ZoneOffset.UTC);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toTimestamp(null, converter));
    }
}
