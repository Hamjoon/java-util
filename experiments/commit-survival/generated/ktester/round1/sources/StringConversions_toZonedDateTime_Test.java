package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toZonedDateTime_Test {

    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    private AutoCloseable mocks;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations (handled by @ExtendWith, but kept for clarity)
        mocks = MockitoAnnotations.openMocks(this);
        // Default behaviour – can be overridden in individual tests.
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Close the Mockito session to release any resources.
        if (mocks != null) {
            mocks.close();
        }
    }

    private static Stream<Arguments> validParsingProvider() {
        return Stream.of(// ISO‑8601 with explicit offset – converter zone ignored
        Arguments.of("2023-07-15T14:30:00+02:00", ZoneId.of("Europe/Paris"), ZonedDateTime.of(2023, 7, 15, 14, 30, 0, 0, ZoneOffset.of("+02:00")), "String contains an offset; converter zone ignored."), // ISO‑8601 without zone – converter zone applied
        Arguments.of("2023-07-15T14:30:00", ZoneId.of("America/New_York"), ZonedDateTime.of(2023, 7, 15, 14, 30, 0, 0, ZoneId.of("America/New_York")), "String has no zone; converter zone applied."), // Slash‑separated pattern, UTC supplied by converter
        Arguments.of("2023/07/15 14:30:00", ZoneId.of("UTC"), ZonedDateTime.of(2023, 7, 15, 14, 30, 0, 0, ZoneOffset.UTC), "Slash‑separated date pattern, zone from converter (UTC)."), // Custom pattern “dd-MMM-yyyy HH:mm”
        Arguments.of("15-Jul-2023 14:30", ZoneId.of("Europe/London"), ZonedDateTime.of(2023, 7, 15, 14, 30, 0, 0, ZoneId.of("Europe/London")), "\"dd-MMM-yyyy HH:mm\" pattern with converter zone."), // ISO‑8601 with trailing ‘Z’ (UTC) – converter zone ignored
        Arguments.of("2023-12-01T08:15:30Z", ZoneId.of("America/Los_Angeles"), ZonedDateTime.of(2023, 12, 1, 8, 15, 30, 0, ZoneOffset.UTC), "String ends with 'Z' (UTC); converter zone ignored."), // Date‑only string – midnight in converter zone
        Arguments.of("2023-05-20", ZoneId.of("Asia/Tokyo"), ZonedDateTime.of(2023, 5, 20, 0, 0, 0, 0, ZoneId.of("Asia/Tokyo")), "Date‑only string interpreted as midnight in converter zone."), // Time‑only string – current date in converter zone
        Arguments.of("13:45:20", ZoneId.of("Europe/Berlin"), ZonedDateTime.of(LocalDate.now(ZoneId.of("Europe/Berlin")), LocalTime.of(13, 45, 20), ZoneId.of("Europe/Berlin")), "Time‑only string uses current date in converter zone."), // Space‑separated pattern with milliseconds
        Arguments.of("2023-07-15 14:30:15.123", ZoneId.of("UTC"), ZonedDateTime.of(2023, 7, 15, 14, 30, 15, 123_000_000, ZoneOffset.UTC), "Pattern with milliseconds; fraction preserved."), // Converter provides null ZoneId – fallback to system default
        Arguments.of("2023-07-15T14:30:00", null, ZonedDateTime.of(2023, 7, 15, 14, 30, 0, 0, ZoneId.systemDefault()), "Converter zone null → system default zone used."), // String with explicit offset, different from converter zone
        Arguments.of("2023-07-15T14:30:00+05:00", ZoneId.of("America/New_York"), ZonedDateTime.of(2023, 7, 15, 14, 30, 0, 0, ZoneOffset.of("+05:00")), "Explicit offset in string overrides converter zone."), // DST handling – standard time (Jan) in America/New_York
        Arguments.of("2023-01-15T12:00:00", ZoneId.of("America/New_York"), ZonedDateTime.of(2023, 1, 15, 12, 0, 0, 0, ZoneId.of("America/New_York")), "January (standard time) in New York – correct offset applied."), // DST handling – daylight‑saving time (Jul) in America/New_York
        Arguments.of("2023-07-15T12:00:00", ZoneId.of("America/New_York"), ZonedDateTime.of(2023, 7, 15, 12, 0, 0, 0, ZoneId.of("America/New_York")), "July (DST) in New York – correct offset applied."));
    }

    private static Stream<Arguments> invalidParsingProvider() {
        // Helper to create a converter mock with a given ZoneId (or null)
        java.util.function.Function<ZoneId, Converter> converterFactory = zoneId -> {
            Converter conv = mock(Converter.class);
            ConverterOptions opt = mock(ConverterOptions.class);
            when(conv.getOptions()).thenReturn(opt);
            when(opt.getZoneId()).thenReturn(zoneId);
            return conv;
        };
        // Converter with null options (i.e., getOptions() returns null)
        Converter converterWithNullOptions = mock(Converter.class);
        when(converterWithNullOptions.getOptions()).thenReturn(null);
        // Normal converter (zone not relevant for exception cases)
        Converter normalConverter = converterFactory.apply(ZoneId.of("UTC"));
        return Stream.of(// Type mismatch – Integer instead of String
        Arguments.of(12345, normalConverter, ClassCastException.class, "Non‑String input should cause ClassCastException."), // Null string input
        Arguments.of(null, normalConverter, NullPointerException.class, "Null string should cause NullPointerException."), // Malformed date string
        Arguments.of("not-a-date", normalConverter, DateTimeParseException.class, "Unparseable string should raise DateTimeParseException."), // Empty string
        Arguments.of("", normalConverter, DateTimeParseException.class, "Empty string should raise DateTimeParseException."), // Whitespace‑only string
        Arguments.of("   ", normalConverter, DateTimeParseException.class, "Whitespace‑only string should raise DateTimeParseException."), // Epoch seconds as plain number string – not supported
        Arguments.of("1689418200", normalConverter, DateTimeParseException.class, "Epoch seconds string not supported; expect DateTimeParseException."), // Impossible date values
        Arguments.of("2023-02-30", normalConverter, DateTimeParseException.class, "Invalid day (Feb 30) should raise DateTimeParseException."), Arguments.of("2023-13-01", normalConverter, DateTimeParseException.class, "Invalid month (13) should raise DateTimeParseException."), Arguments.of("2023-07-15T25:00:00", normalConverter, DateTimeParseException.class, "Hour 25 out of range → DateTimeParseException."), Arguments.of("2023-07-15T14:61:00", normalConverter, DateTimeParseException.class, "Minute 61 out of range → DateTimeParseException."), // Invalid offset formats
        Arguments.of("2023-07-15T14:30:00+25:00", normalConverter, DateTimeParseException.class, "Offset hour +25 invalid → DateTimeParseException."), Arguments.of("2023-07-15T14:30:00+02:60", normalConverter, DateTimeParseException.class, "Offset minute 60 invalid → DateTimeParseException."), // Unknown zone identifier inside brackets
        Arguments.of("2023-07-15T14:30:00[Invalid/Zone]", normalConverter, DateTimeException.class, "Unknown zone ID in string should raise DateTimeException."), // Converter with null options – NPE when dereferencing getZoneId()
        Arguments.of("2023-07-15T14:30:00", converterWithNullOptions, NullPointerException.class, "Converter with null options should cause NullPointerException."), // Null converter argument
        Arguments.of("2023-07-15T14:30:00", null, NullPointerException.class, "Null converter argument should cause NullPointerException."));
    }

    @Test
    void testToZonedDateTime_WithValidIsoString_ReturnsExpectedZonedDateTime() {
        // GIVEN
        // UTC time
        String isoString = "2023-08-15T12:34:56Z";
        ZonedDateTime expected = ZonedDateTime.parse(isoString);
        // WHEN
        ZonedDateTime actual = StringConversions.toZonedDateTime(isoString, mockConverter);
        // THEN
        assertEquals(expected, actual, "The parsed ZonedDateTime should match the expected value.");
    }

    @Test
    void testToZonedDateTime_WithNullInput_ThrowsNullPointerException() {
        // GIVEN
        Object nullInput = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> StringConversions.toZonedDateTime(nullInput, mockConverter), "Passing a null value should result in a NullPointerException.");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validParsingProvider")
    void testToZonedDateTime_ValidParsing(String from, ZoneId zoneId, ZonedDateTime expected, String description) {
        // Arrange – configure mock converter / options for this case
        Converter converter = mock(Converter.class);
        ConverterOptions options = mock(ConverterOptions.class);
        when(converter.getOptions()).thenReturn(options);
        // may be null
        when(options.getZoneId()).thenReturn(zoneId);
        // Act
        ZonedDateTime actual = StringConversions.toZonedDateTime(from, converter);
        // Assert
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("invalidParsingProvider")
    void testToZonedDateTime_InvalidParsing(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
        // Act & Assert
        assertThrows(expectedException, () -> StringConversions.toZonedDateTime(from, converter), description);
    }
}
