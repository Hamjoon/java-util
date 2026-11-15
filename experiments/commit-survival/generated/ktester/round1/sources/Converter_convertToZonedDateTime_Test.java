package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convertToZonedDateTime_Test {

    /**
     * Holds the original {@code Converter.instance} so we can restore it after the tests.
     */
    private static Converter originalInstance;

    /**
     * Mocked instance that will replace the real singleton during each test.
     */
    @Mock
    private Converter mockInstance;

    /**
     * Reflective access to the private static {@code instance} field.
     */
    private static Field instanceField;

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // Cache the original singleton.
        originalInstance = Converter.instance;
        // Obtain the private static field that holds the singleton.
        instanceField = Converter.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Replace the singleton with the Mockito mock.
        instanceField.set(null, mockInstance);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Reset the mock between tests (clear any stubbings/interaction history).
        Mockito.reset(mockInstance);
    }

    @AfterAll
    static void teardownAfterAll() throws Exception {
        // Restore the original singleton so other test classes are not affected.
        instanceField.set(null, originalInstance);
    }

    /**
     * Provides a stream of arguments representing every test case from the
     * supplied JSON.  Each {@link Arguments} consists of:
     * <ul>
     *   <li>the input object for {@code convertToZonedDateTime}</li>
     *   <li>the expected {@link ZonedDateTime} result (null if an exception is expected)</li>
     *   <li>the exception to be thrown by the mock (null if no exception)</li>
     *   <li>a textual description (used as the test name)</li>
     * </ul>
     */
    private static Stream<Arguments> provideConvertToZonedDateTimeCases() {
        ZoneId systemZone = ZoneId.systemDefault();
        // Helper values used in multiple cases
        ZonedDateTime zdtNow = ZonedDateTime.now(ZoneId.of("UTC"));
        ZonedDateTime zdtNowSystem = ZonedDateTime.now(systemZone);
        // 2023‑04‑12T00:00:00Z
        Date dateExample = new Date(1_680_000_000_000L);
        Timestamp tsExample = Timestamp.valueOf("2021-08-15 12:00:00");
        GregorianCalendar gregCal = new GregorianCalendar(TimeZone.getTimeZone("America/New_York"));
        // arbitrary instant
        gregCal.setTimeInMillis(1_600_000_000_000L);
        return Stream.of(// -----------------------------------------------------------------
        // convertToZonedDateTime - valid and edge cases
        // -----------------------------------------------------------------
        Arguments.of(null, null, null, "When the source object is null the conversion should return null rather than throwing an exception."), Arguments.of(ZonedDateTime.of(2023, 5, 15, 10, 30, 0, 0, ZoneId.of("UTC")), ZonedDateTime.of(2023, 5, 15, 10, 30, 0, 0, ZoneId.of("UTC")), null, "If the source object is already a ZonedDateTime the method should return it unchanged."), Arguments.of(dateExample, ZonedDateTime.ofInstant(dateExample.toInstant(), systemZone), null, "Convert a java.util.Date (which holds an instant) to a ZonedDateTime using the JVM default time‑zone."), Arguments.of(LocalDateTime.of(2023, 7, 20, 14, 45, 30), ZonedDateTime.of(LocalDateTime.of(2023, 7, 20, 14, 45, 30), systemZone), null, "Convert a LocalDateTime to ZonedDateTime by attaching the system default zone."), Arguments.of(LocalDate.of(2022, 12, 31), ZonedDateTime.of(LocalDate.of(2022, 12, 31).atStartOfDay(), systemZone), null, "Convert a LocalDate to ZonedDateTime at the start of the day in the default zone."), Arguments.of(tsExample, ZonedDateTime.ofInstant(tsExample.toInstant(), systemZone), null, "Convert a java.sql.Timestamp to ZonedDateTime using the default zone."), Arguments.of("2024-01-01T10:15:30+02:00[Europe/Paris]", ZonedDateTime.parse("2024-01-01T10:15:30+02:00[Europe/Paris]"), null, "Parse a fully qualified ISO‑8601 string that already contains a zone ID."), Arguments.of("2024-01-01T10:15:30", ZonedDateTime.of(LocalDateTime.parse("2024-01-01T10:15:30"), systemZone), null, "Parse an ISO‑8601 date‑time string without a zone; the converter should assume the system default zone."), Arguments.of(Long.valueOf(1_600_000_000_000L), ZonedDateTime.ofInstant(Instant.ofEpochMilli(1_600_000_000_000L), systemZone), null, "When the source is a Number (epoch‑millis) the converter should treat it as an instant and create a ZonedDateTime in the default zone."), Arguments.of(Boolean.TRUE, null, new IllegalArgumentException(), "Unsupported source types (e.g., Boolean) must cause the converter to throw an IllegalArgumentException."), // -----------------------------------------------------------------
        // convertToZonedDateTime - additional cases
        // -----------------------------------------------------------------
        Arguments.of(Instant.parse("2022-03-10T12:34:56Z"), ZonedDateTime.ofInstant(Instant.parse("2022-03-10T12:34:56Z"), systemZone), null, "Convert a java.time.Instant to ZonedDateTime using the system default zone."), Arguments.of("2022-03-10T12:34:56Z", ZonedDateTime.ofInstant(Instant.parse("2022-03-10T12:34:56Z"), systemZone), null, "Parse an ISO‑8601 instant string (with 'Z') and convert it to ZonedDateTime in the default zone."), Arguments.of("2022-03-10T12:34:56+01:00", ZonedDateTime.parse("2022-03-10T12:34:56+01:00"), null, "Parse an ISO‑8601 string that includes an offset but no zone ID; the converter should produce a ZonedDateTime retaining the offset."), Arguments.of(gregCal, ZonedDateTime.ofInstant(gregCal.toInstant(), ZoneId.of("America/New_York")), null, "Convert a java.util.Calendar (GregorianCalendar) with a specific time‑zone to ZonedDateTime preserving that zone."), Arguments.of(OffsetDateTime.parse("2022-03-10T12:34:56+02:00"), ZonedDateTime.ofInstant(OffsetDateTime.parse("2022-03-10T12:34:56+02:00").toInstant(), systemZone), null, "Convert an OffsetDateTime to ZonedDateTime using the system default zone (offset is applied to the instant)."), Arguments.of("2022-03-10", ZonedDateTime.of(LocalDate.parse("2022-03-10").atStartOfDay(), systemZone), null, "Parse a date‑only ISO‑8601 string and convert it to ZonedDateTime at start of day in the default zone."), Arguments.of(YearMonth.of(2022, 5), null, new IllegalArgumentException(), "Unsupported type YearMonth should cause an IllegalArgumentException."), Arguments.of(new BigDecimal("1620000000"), ZonedDateTime.ofInstant(Instant.ofEpochSecond(1_620_000_000L), systemZone), null, "Convert a BigDecimal representing epoch seconds to ZonedDateTime (treat as seconds, not milliseconds)."), Arguments.of(LocalTime.of(15, 30, 45), null, new IllegalArgumentException(), "LocalTime alone lacks date information; conversion should fail with IllegalArgumentException."), Arguments.of(Optional.of(zdtNowSystem), zdtNowSystem, null, "When the source is an Optional wrapping a ZonedDateTime, the converter should unwrap and return the contained ZonedDateTime."), // -----------------------------------------------------------------
        // convertToZonedDateTime - unsupported types
        // -----------------------------------------------------------------
        Arguments.of(new ArrayList<>(), null, new IllegalArgumentException(), "Passing a collection type (ArrayList) which the converter does not support should result in IllegalArgumentException."), Arguments.of(Collections.singletonMap("key", "value"), null, new IllegalArgumentException(), "Passing a Map which is not a recognised temporal source should cause IllegalArgumentException."), Arguments.of(OffsetTime.of(12, 30, 0, 0, ZoneOffset.UTC), null, new IllegalArgumentException(), "OffsetTime lacks date information; conversion to ZonedDateTime is unsupported and must throw IllegalArgumentException."), Arguments.of(Duration.ofHours(5), null, new IllegalArgumentException(), "Duration represents a time‑based amount, not an instant; the converter should reject it with IllegalArgumentException."), // -----------------------------------------------------------------
        // convertToZonedDateTime - malformed strings
        // -----------------------------------------------------------------
        Arguments.of("2024-02-30T10:00:00", null, new DateTimeParseException("Invalid date", "2024-02-30T10:00:00", 0), "String with an impossible date (Feb 30) should cause a DateTimeParseException during parsing."), Arguments.of("not-a-date", null, new DateTimeParseException("Invalid format", "not-a-date", 0), "Completely non‑date text should be rejected and result in a DateTimeParseException."), Arguments.of("10:15:30", null, new DateTimeParseException("Missing date component", "10:15:30", 0), "String containing only a time without a date component cannot be converted to ZonedDateTime and must throw DateTimeParseException."), // -----------------------------------------------------------------
        // convertToZonedDateTime - optional edge cases
        // -----------------------------------------------------------------
        Arguments.of(Optional.empty(), null, null, "An empty Optional should be treated as a null source and therefore the conversion returns null."), Arguments.of(Optional.of(Boolean.TRUE), null, new IllegalArgumentException(), "Optional wrapping an unsupported type (Boolean) should cause the converter to throw IllegalArgumentException."), Arguments.of(Optional.ofNullable(null), null, null, "Optional.ofNullable(null) yields an Optional containing null; the converter should interpret it as null and return null."));
    }

    @Test
    void convertToZonedDateTime_givenZonedDateTime_returnsSameInstance() {
        // GIVEN
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        Mockito.when(mockInstance.convert(eq(now), eq(ZonedDateTime.class))).thenReturn(now);
        // WHEN
        ZonedDateTime result = Converter.convertToZonedDateTime(now);
        // THEN
        assertSame(now, result, "The returned ZonedDateTime should be the exact same instance supplied by the mock.");
        Mockito.verify(mockInstance).convert(eq(now), eq(ZonedDateTime.class));
    }

    @Test
    void convertToZonedDateTime_givenString_parsesToZonedDateTime() {
        // GIVEN
        String isoString = "2023-01-02T15:30:45+02:00[Europe/Paris]";
        ZonedDateTime expected = ZonedDateTime.parse(isoString);
        Mockito.when(mockInstance.convert(eq(isoString), eq(ZonedDateTime.class))).thenReturn(expected);
        // WHEN
        ZonedDateTime result = Converter.convertToZonedDateTime(isoString);
        // THEN
        assertEquals(expected, result, "The conversion should return the ZonedDateTime parsed from the ISO string.");
        Mockito.verify(mockInstance).convert(eq(isoString), eq(ZonedDateTime.class));
    }

    @Test
    void convertToZonedDateTime_givenNull_returnsNull() {
        // GIVEN
        Mockito.when(mockInstance.convert(eq(null), eq(ZonedDateTime.class))).thenReturn(null);
        // WHEN
        ZonedDateTime result = Converter.convertToZonedDateTime(null);
        // THEN
        assertNull(result, "A null input should result in a null output.");
        Mockito.verify(mockInstance).convert(eq(null), eq(ZonedDateTime.class));
    }

    @Test
    void convertToZonedDateTime_whenConversionThrowsException_propagatesIt() {
        // GIVEN
        // not convertible
        Object badInput = new Object();
        RuntimeException conversionException = new IllegalArgumentException("Cannot convert");
        Mockito.when(mockInstance.convert(eq(badInput), eq(ZonedDateTime.class))).thenThrow(conversionException);
        // WHEN / THEN
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> Converter.convertToZonedDateTime(badInput), "The exception from the underlying conversion should be propagated.");
        assertSame(conversionException, thrown, "The same exception instance should be propagated.");
        Mockito.verify(mockInstance).convert(eq(badInput), eq(ZonedDateTime.class));
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("provideConvertToZonedDateTimeCases")
    void convertToZonedDateTime_parameterised(Object input, ZonedDateTime expectedResult, Throwable stubException, String description) throws Exception {
        // -----------------------------------------------------------------
        // Arrange – stub the mock according to the test case
        // -----------------------------------------------------------------
        if (stubException != null) {
            Mockito.when(mockInstance.convert(eq(input), eq(ZonedDateTime.class))).thenThrow(stubException);
        } else {
            Mockito.when(mockInstance.convert(eq(input), eq(ZonedDateTime.class))).thenReturn(expectedResult);
        }
        // -----------------------------------------------------------------
        // Act & Assert
        // -----------------------------------------------------------------
        if (stubException != null) {
            // Expect the same exception type to be propagated.
            Throwable thrown = assertThrows(stubException.getClass(), () -> Converter.convertToZonedDateTime(input), description);
            assertEquals(stubException.getClass(), thrown.getClass(), "The propagated exception type should match the stubbed one.");
        } else {
            ZonedDateTime actual = Converter.convertToZonedDateTime(input);
            if (expectedResult == null) {
                assertNull(actual, description);
            } else {
                // For the case where the input is already a ZonedDateTime we also
                // verify that the same instance is returned (identity check).
                if (input instanceof ZonedDateTime) {
                    assertSame(expectedResult, actual, description);
                } else {
                    assertEquals(expectedResult, actual, description);
                }
            }
        }
        // Verify that the delegate method was invoked exactly once.
        Mockito.verify(mockInstance).convert(eq(input), eq(ZonedDateTime.class));
    }
}
