package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toZonedDateTime_Test {

    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    /**
     * Common ZoneId used for all tests.
     */
    private static final ZoneId UTC = ZoneId.of("UTC");

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mock converter to return a mock options object whose zoneId is UTC.
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getZoneId()).thenReturn(UTC);
    }

    /**
     * Configures {@code mockConverter} to return a {@code ConverterOptions}
     * whose {@code getZoneId()} returns the supplied {@code zoneId}.
     *
     * @param zoneId the zone to be returned (may be {@code null})
     */
    private void configureConverterZoneId(ZoneId zoneId) {
        when(mockOptions.getZoneId()).thenReturn(zoneId);
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> validConversionsProvider() {
        return Stream.of(// Plain ISO_LOCAL_DATE_TIME, Converter zone = UTC
        Arguments.of("2023-08-15T12:30:00", ZoneId.of("UTC"), ZonedDateTime.of(2023, 8, 15, 12, 30, 0, 0, ZoneOffset.UTC), "Parse a plain ISO_LOCAL_DATE_TIME string when the Converter supplies UTC as the target ZoneId."), // ISO_OFFSET_DATE_TIME with offset, Converter zone = America/New_York
        Arguments.of("2023-08-15T12:30:00+02:00", ZoneId.of("America/New_York"), ZonedDateTime.ofInstant(Instant.parse("2023-08-15T10:30:00Z"), ZoneId.of("America/New_York")), "Parse an ISO_OFFSET_DATE_TIME string that already contains an offset. The supplied ZoneId (America/New_York) should be applied to the instant."), // UTC designator, Converter zone = Europe/Paris
        Arguments.of("2023-08-15T12:30:00Z", ZoneId.of("Europe/Paris"), ZonedDateTime.ofInstant(Instant.parse("2023-08-15T12:30:00Z"), ZoneId.of("Europe/Paris")), "Parse a string that already includes the UTC designator (Z). The resulting ZonedDateTime should keep the same instant but be expressed in the Converter's ZoneId (Europe/Paris)."));
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> invalidInputsProvider() {
        return Stream.of(// null String -> NullPointerException (DateUtilities will throw)
        Arguments.of(null, UTC, NullPointerException.class, "Passing a null String should cause a NullPointerException when the method attempts to parse it."), // empty string -> DateTimeParseException
        Arguments.of("", UTC, DateTimeParseException.class, "An empty string cannot be parsed as a date/time and should result in a DateTimeParseException."), // malformed date -> DateTimeParseException
        Arguments.of("2023-13-40", UTC, DateTimeParseException.class, "A malformed date (invalid month and day) should cause a DateTimeParseException."), // non‑String argument -> ClassCastException (no need to mock DateUtilities)
        Arguments.of(12345, UTC, ClassCastException.class, "Providing a non‑String object triggers a ClassCastException because the method casts the argument to String."));
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> edgeCasesProvider() {
        return Stream.of(// Date‑only string, UTC
        Arguments.of("2023-08-15", UTC, ZonedDateTime.of(2023, 8, 15, 0, 0, 0, 0, ZoneOffset.UTC), "A date‑only string should be interpreted as the start of the day (midnight) in the supplied ZoneId (UTC)."), // Offset matches ZoneId
        Arguments.of("2023-08-15T12:30:00-05:00", ZoneOffset.of("-05:00"), ZonedDateTime.ofInstant(Instant.parse("2023-08-15T17:30:00Z"), ZoneOffset.of("-05:00")), "When the input already contains an offset that matches the Converter's ZoneId, the resulting ZonedDateTime should represent the same instant with that offset."), // Ambiguous DST fallback (America/New_York)
        Arguments.of("2021-11-07T01:30:00", ZoneId.of("America/New_York"), ZonedDateTime.ofLocal(LocalDateTime.of(2021, 11, 7, 1, 30), ZoneId.of("America/New_York"), null), "During the fall‑back DST transition in New York, 01:30 occurs twice. The parser resolves the ambiguous time to the earlier offset (EDT, -04:00)."), // Nanosecond precision
        Arguments.of("2023-08-15T12:30:00.123456789", UTC, ZonedDateTime.of(2023, 8, 15, 12, 30, 0, 123_456_789, ZoneOffset.UTC), "String with nanosecond precision should be parsed preserving the full nanosecond component."));
    }

    @Test
    void toZonedDateTime_validString_returnsParsedValue() {
        // GIVEN
        String input = "2023-08-15T12:34:56Z";
        ZonedDateTime expected = ZonedDateTime.parse(input);
        try (MockedStatic<DateUtilities> dateUtilitiesMock = mockStatic(DateUtilities.class)) {
            // Mock static call
            dateUtilitiesMock.when(() -> DateUtilities.parseDate(input, UTC, true)).thenReturn(expected);
            // WHEN
            ZonedDateTime actual = StringConversions.toZonedDateTime(input, mockConverter);
            // THEN
            assertEquals(expected, actual, "The returned ZonedDateTime should be the one produced by DateUtilities");
            // Verify that the static method was invoked exactly once with the expected arguments.
            dateUtilitiesMock.verify(() -> DateUtilities.parseDate(input, UTC, true), times(1));
        }
    }

    @Test
    void toZonedDateTime_nullString_delegatesWithNull() {
        // GIVEN
        String input = null;
        ZonedDateTime expected = null;
        try (MockedStatic<DateUtilities> dateUtilitiesMock = mockStatic(DateUtilities.class)) {
            dateUtilitiesMock.when(() -> DateUtilities.parseDate(null, UTC, true)).thenReturn(expected);
            // WHEN
            ZonedDateTime actual = StringConversions.toZonedDateTime(input, mockConverter);
            // THEN
            assertNull(actual, "When the input string is null the method should return null");
            dateUtilitiesMock.verify(() -> DateUtilities.parseDate(null, UTC, true), times(1));
        }
    }

    @Test
    void toZonedDateTime_nonStringArgument_throwsClassCastException() {
        // GIVEN
        Integer notAString = 12345;
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> StringConversions.toZonedDateTime(notAString, mockConverter), "A non‑String argument should cause a ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("validConversionsProvider")
    void toZonedDateTime_validConversions(String from, ZoneId zoneId, ZonedDateTime expected, String description) {
        // Arrange
        configureConverterZoneId(zoneId);
        try (MockedStatic<DateUtilities> dateUtilitiesMock = mockStatic(DateUtilities.class)) {
            dateUtilitiesMock.when(() -> DateUtilities.parseDate(from, zoneId, true)).thenReturn(expected);
            // Act
            ZonedDateTime actual = StringConversions.toZonedDateTime(from, mockConverter);
            // Assert
            assertEquals(expected, actual, description);
            dateUtilitiesMock.verify(() -> DateUtilities.parseDate(from, zoneId, true), times(1));
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("invalidInputsProvider")
    void toZonedDateTime_invalidInputs(Object from, ZoneId zoneId, Class<? extends Throwable> expectedException, String description) {
        // Arrange
        configureConverterZoneId(zoneId);
        // For the non‑String case we expect the exception before DateUtilities is called.
        if (expectedException == ClassCastException.class) {
            assertThrows(expectedException, () -> StringConversions.toZonedDateTime(from, mockConverter), description);
            return;
        }
        // For the remaining cases we need to mock DateUtilities to throw the expected exception.
        try (MockedStatic<DateUtilities> dateUtilitiesMock = mockStatic(DateUtilities.class)) {
            dateUtilitiesMock.when(() -> DateUtilities.parseDate((String) from, zoneId, true)).thenThrow(expectedException);
            // Act & Assert
            assertThrows(expectedException, () -> StringConversions.toZonedDateTime(from, mockConverter), description);
            dateUtilitiesMock.verify(() -> DateUtilities.parseDate((String) from, zoneId, true), times(1));
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("edgeCasesProvider")
    void toZonedDateTime_edgeCases(String from, ZoneId zoneId, ZonedDateTime expected, String description) {
        // Arrange
        configureConverterZoneId(zoneId);
        try (MockedStatic<DateUtilities> dateUtilitiesMock = mockStatic(DateUtilities.class)) {
            dateUtilitiesMock.when(() -> DateUtilities.parseDate(from, zoneId, true)).thenReturn(expected);
            // Act
            ZonedDateTime actual = StringConversions.toZonedDateTime(from, mockConverter);
            // Assert
            assertEquals(expected, actual, description);
            dateUtilitiesMock.verify(() -> DateUtilities.parseDate(from, zoneId, true), times(1));
        }
    }

    // -----------------------------------------------------------------
    @Test
    void toZonedDateTime_nullZoneId_fallsBackToSystemDefault() {
        // GIVEN
        String input = "2023-08-15T12:30:00";
        // simulate missing ZoneId
        configureConverterZoneId(null);
        ZoneId systemDefault = ZoneId.systemDefault();
        ZonedDateTime expected = ZonedDateTime.of(2023, 8, 15, 12, 30, 0, 0, systemDefault);
        try (MockedStatic<DateUtilities> dateUtilitiesMock = mockStatic(DateUtilities.class)) {
            dateUtilitiesMock.when(() -> DateUtilities.parseDate(input, null, true)).thenReturn(expected);
            // WHEN
            ZonedDateTime actual = StringConversions.toZonedDateTime(input, mockConverter);
            // THEN
            assertEquals(expected, actual, "When Converter's ZoneId is null the system default zone should be used.");
            dateUtilitiesMock.verify(() -> DateUtilities.parseDate(input, null, true), times(1));
        }
    }

    // -----------------------------------------------------------------
    @Test
    void toZonedDateTime_nullConverter_throwsNullPointerException() {
        // GIVEN
        String input = "2023-08-15T12:30:00";
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> StringConversions.toZonedDateTime(input, null), "Passing a null Converter causes a NullPointerException when the method attempts to call converter.getOptions().");
    }

    @Test
    void toZonedDateTime_converterWithNullOptions_throwsNullPointerException() {
        // GIVEN
        String input = "2023-08-15T12:30:00";
        // mockConverter already exists; configure it to return null options
        when(mockConverter.getOptions()).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> StringConversions.toZonedDateTime(input, mockConverter), "If the Converter's getOptions() method returns null, a NullPointerException is thrown while accessing getZoneId().");
    }
}
