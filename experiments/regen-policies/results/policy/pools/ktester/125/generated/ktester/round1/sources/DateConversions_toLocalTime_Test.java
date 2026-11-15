package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.sql.Timestamp;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DateConversions_toLocalTime_Test {

    @Mock
    private Converter converter;

    @Mock
    private ConverterOptions options;

    private AutoCloseable mocks;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations (handled by MockitoExtension, but kept for clarity)
        mocks = MockitoAnnotations.openMocks(this);
        // Default behaviour for the converter's options – use UTC for deterministic results
        when(converter.getOptions()).thenReturn(options);
        when(options.getZoneId()).thenReturn(ZoneId.of("UTC"));
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Close the mock context to avoid leaking resources between tests
        mocks.close();
    }

    /**
     * Provides a stream of successful conversion arguments.
     *
     * Each argument consists of:
     * <ul>
     *   <li>source object (the value passed to {@code toLocalTime})</li>
     *   <li>zoneId to be returned by {@code ConverterOptions#getZoneId()}</li>
     *   <li>expected {@link LocalTime} result</li>
     *   <li>description for readability (used only in test name)</li>
     * </ul>
     */
    private static Stream<Arguments> successfulConversions() {
        return Stream.of(// 1. Instant (UTC)
        Arguments.of(Instant.parse("2023-03-15T08:45:30Z"), "UTC", LocalTime.of(8, 45, 30), "Instant (UTC)"), // 2. java.util.Date (system default zone)
        Arguments.of(new Date(Instant.parse("2023-03-15T15:20:10Z").toEpochMilli()), ZoneId.systemDefault().getId(), LocalTime.of(15, 20, 10), "java.util.Date (system default zone)"), // 3. java.sql.Timestamp (UTC)
        Arguments.of(Timestamp.from(Instant.parse("2023-03-15T23:59:59Z")), "UTC", LocalTime.of(23, 59, 59), "java.sql.Timestamp (UTC)"), // 4. epoch millis (Long) with Europe/Paris (+01:00)
        Arguments.of(Instant.parse("2023-03-15T12:00:00Z").toEpochMilli(), "Europe/Paris", LocalTime.of(13, 0, 0), "epoch millis with Europe/Paris"), // 5. LocalDateTime (no zone shift)
        Arguments.of(LocalDateTime.of(2023, 3, 15, 9, 30, 45), "UTC", LocalTime.of(9, 30, 45), "LocalDateTime (UTC)"), // 6. String with offset -05:00 (converted to UTC)
        Arguments.of("2023-03-15T18:15:00-05:00", "UTC", LocalTime.of(23, 15, 0), "String with offset -05:00"), // 7. LocalDate (midnight)
        Arguments.of(LocalDate.of(2023, 3, 15), "UTC", LocalTime.of(0, 0), "LocalDate (UTC)"), // 8. ZonedDateTime Europe/Paris (UTC+1) -> UTC
        Arguments.of(ZonedDateTime.of(2023, 3, 15, 10, 15, 30, 0, ZoneId.of("Europe/Paris")), "UTC", LocalTime.of(9, 15, 30), "ZonedDateTime Europe/Paris to UTC"), // 9. OffsetDateTime +03:00 -> UTC
        Arguments.of(OffsetDateTime.parse("2023-03-15T12:45:00+03:00"), "UTC", LocalTime.of(9, 45, 0), "OffsetDateTime +03:00 to UTC"), // 10. java.sql.Date (UTC)
        Arguments.of(java.sql.Date.valueOf(LocalDate.of(2023, 3, 15)), "UTC", LocalTime.of(0, 0), "java.sql.Date (UTC)"), // 11. Calendar (America/New_York) with UTC converter
        Arguments.of(createCalendar(TimeZone.getTimeZone("America/New_York"), Instant.parse("2023-03-15T15:00:00Z").toEpochMilli()), "UTC", LocalTime.of(15, 0, 0), "Calendar (America/New_York) with UTC converter"), // 12. epoch millis (Long) with Asia/Tokyo (+09:00)
        Arguments.of(Instant.parse("2023-03-15T00:00:00Z").toEpochMilli(), "Asia/Tokyo", LocalTime.of(9, 0, 0), "epoch millis with Asia/Tokyo"), // 13. ISO‑8601 string without offset (interpreted in UTC)
        Arguments.of("2023-03-15T12:30:00", "UTC", LocalTime.of(12, 30, 0), "ISO‑8601 string without offset (UTC)"), // 14. ISO‑8601 string with zone ID Europe/Paris
        Arguments.of("2023-03-15T12:30:00[Europe/Paris]", "UTC", LocalTime.of(11, 30, 0), "ISO‑8601 string with zone ID Europe/Paris"), // 15. String with fractional seconds
        Arguments.of("2023-03-15T12:30:45.123", "UTC", LocalTime.of(12, 30, 45, 123_000_000), "String with fractional seconds"), // 16. Negative epoch millis (pre‑1970) with UTC
        Arguments.of(Instant.parse("1965-01-01T08:15:00Z").toEpochMilli(), "UTC", LocalTime.of(8, 15, 0), "Negative epoch millis (pre‑1970) UTC"));
    }

    private static Stream<Arguments> exceptionScenarios() {
        return Stream.of(// Boolean – unsupported type
        Arguments.of(Boolean.TRUE, "UTC", IllegalArgumentException.class, "Unsupported Boolean source"), // Double – unsupported numeric type
        Arguments.of(12345.678, "UTC", IllegalArgumentException.class, "Unsupported Double source"), // Malformed ISO‑8601 string
        Arguments.of("2023-02-30T25:61:61", "UTC", DateTimeParseException.class, "Malformed ISO‑8601 string"), // Unknown zone identifier in string
        Arguments.of("2023-03-15T12:30:00[Invalid/Zone]", "UTC", DateTimeException.class, "String with unknown zone ID"), // Invalid offset (+25:00)
        Arguments.of("2023-03-15T12:30:00+25:00", "UTC", DateTimeException.class, "String with invalid offset"));
    }

    /**
     * Derives an {@link Instant} from the supplied source object using the same
     * logic that the real {@code DateConversions.toInstant} would apply.
     *
     * This helper is only used for test setup – it mirrors the expected
     * conversion behaviour for the supported types.
     */
    private static Instant toInstantFromSource(Object source) {
        if (source == null) {
            return null;
        }
        if (source instanceof Instant) {
            return (Instant) source;
        }
        if (source instanceof Date) {
            return Instant.ofEpochMilli(((Date) source).getTime());
        }
        if (source instanceof Timestamp) {
            return ((Timestamp) source).toInstant();
        }
        if (source instanceof Long) {
            return Instant.ofEpochMilli((Long) source);
        }
        if (source instanceof LocalDateTime) {
            // Assume the LocalDateTime is in UTC for test purposes; the zone will be applied later.
            return ((LocalDateTime) source).toInstant(ZoneOffset.UTC);
        }
        if (source instanceof String) {
            // Try parsing the string as an Instant (covers offset, zone‑id, fractional seconds, etc.).
            // If parsing fails, let the exception propagate – the test will stub the mock accordingly.
            return Instant.parse((String) source);
        }
        if (source instanceof LocalDate) {
            // Midnight at the start of the day in UTC.
            return ((LocalDate) source).atStartOfDay(ZoneOffset.UTC).toInstant();
        }
        if (source instanceof ZonedDateTime) {
            return ((ZonedDateTime) source).toInstant();
        }
        if (source instanceof OffsetDateTime) {
            return ((OffsetDateTime) source).toInstant();
        }
        if (source instanceof Calendar) {
            return Instant.ofEpochMilli(((Calendar) source).getTimeInMillis());
        }
        // Unsupported type – return null (the real implementation would throw).
        return null;
    }

    /**
     * Utility to create a {@link Calendar} instance with the supplied {@link TimeZone}
     * and epoch‑millisecond instant.
     */
    private static Calendar createCalendar(TimeZone tz, long epochMilli) {
        Calendar cal = Calendar.getInstance(tz);
        cal.setTimeInMillis(epochMilli);
        return cal;
    }

    @Test
    void toLocalTime_convertsInstantToLocalTime_usingUtcZone() {
        // GIVEN
        Instant fixedInstant = Instant.parse("2023-01-01T12:34:56Z");
        // The Converter is asked to convert the source object to an Instant.
        // We stub that conversion to return our fixed instant irrespective of the source value.
        when(converter.convert(any(), eq(Instant.class))).thenReturn(fixedInstant);
        // WHEN
        LocalTime result = DateConversions.toLocalTime("any source object", converter);
        // THEN
        assertNotNull(result, "Resulting LocalTime should not be null");
        assertEquals(LocalTime.of(12, 34, 56), result, "LocalTime should represent the time component of the fixed instant in UTC");
        // Verify that the converter was used as expected
        verify(converter).convert(any(), eq(Instant.class));
        verify(converter).getOptions();
        verify(options).getZoneId();
    }

    @Test
    void toLocalTime_respectsConverterZoneId() {
        // GIVEN
        Instant fixedInstant = Instant.parse("2023-01-01T12:34:56Z");
        when(converter.convert(any(), eq(Instant.class))).thenReturn(fixedInstant);
        // Change the zone to a non‑UTC zone (e.g., America/New_York, which is UTC‑5 in winter)
        when(options.getZoneId()).thenReturn(ZoneId.of("America/New_York"));
        // WHEN
        LocalTime result = DateConversions.toLocalTime("ignored", converter);
        // THEN
        // 12:34:56Z corresponds to 07:34:56 in America/New_York (UTC‑5)
        assertEquals(LocalTime.of(7, 34, 56), result, "LocalTime should be calculated using the Converter's ZoneId");
    }

    @Test
    void toLocalTime_nullSourceReturnsNullInstantHandledByConverter() {
        // GIVEN
        // Simulate the converter returning null when the source is null
        when(converter.convert(isNull(), eq(Instant.class))).thenReturn(null);
        when(options.getZoneId()).thenReturn(ZoneId.of("UTC"));
        // WHEN / THEN
        // DateConversions.toInstant will return null, which causes
        // LocalDateTime.ofInstant to throw NPE – we assert that the NPE propagates.
        assertThrows(NullPointerException.class, () -> DateConversions.toLocalTime(null, converter), "A null Instant should lead to NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("successfulConversions")
    void toLocalTime_successfulConversions(Object source, String zoneId, LocalTime expected, String _desc) {
        // -----------------------------------------------------------------
        // Arrange – compute the Instant that the mocked Converter should return.
        // -----------------------------------------------------------------
        Instant instant = toInstantFromSource(source);
        assertNotNull(instant, "Test setup error – could not derive Instant from source");
        // Stub the Converter to return the pre‑computed Instant for any conversion request.
        when(converter.convert(any(), eq(Instant.class))).thenReturn(instant);
        // Stub the zone that the ConverterOptions will provide.
        when(options.getZoneId()).thenReturn(ZoneId.of(zoneId));
        // -----------------------------------------------------------------
        // Act
        // -----------------------------------------------------------------
        LocalTime result = DateConversions.toLocalTime(source, converter);
        // -----------------------------------------------------------------
        // Assert
        // -----------------------------------------------------------------
        assertEquals(expected, result, () -> String.format("Expected LocalTime %s but got %s for source %s with zone %s", expected, result, source, zoneId));
        // Verify interaction with the mocks (helps guard against accidental changes).
        verify(converter).convert(any(), eq(Instant.class));
        verify(converter).getOptions();
        verify(options).getZoneId();
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("exceptionScenarios")
    void toLocalTime_exceptionScenarios(Object source, String zoneId, Class<? extends Throwable> expectedException, String _desc) {
        // -----------------------------------------------------------------
        // Arrange – stub the Converter to throw the appropriate exception
        // when it is asked to produce an Instant from the given source.
        // -----------------------------------------------------------------
        when(converter.getOptions()).thenReturn(options);
        when(options.getZoneId()).thenReturn(ZoneId.of(zoneId));
        // Configure the mock to throw the expected exception for the specific source.
        when(converter.convert(eq(source), eq(Instant.class))).thenThrow(expectedException);
        // -----------------------------------------------------------------
        // Act & Assert
        // -----------------------------------------------------------------
        assertThrows(expectedException, () -> DateConversions.toLocalTime(source, converter), () -> "Expected " + expectedException.getSimpleName() + " for source: " + source);
    }

    @Test
    void toLocalTime_nullSourceThrowsNullPointerException() {
        // The mocked Converter returns null Instant for a null source.
        when(converter.convert(isNull(), eq(Instant.class))).thenReturn(null);
        when(options.getZoneId()).thenReturn(ZoneId.of("UTC"));
        assertThrows(NullPointerException.class, () -> DateConversions.toLocalTime(null, converter), "A null Instant should cause NullPointerException");
    }

    @Test
    void toLocalTime_nullConverterThrowsNullPointerException() {
        // Directly pass a null converter – the method should NPE when accessing options.
        assertThrows(NullPointerException.class, () -> DateConversions.toLocalTime(Instant.now(), null), "Passing a null Converter must result in NullPointerException");
    }
}
