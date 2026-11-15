package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StringConversions_toInstant_Test {

    /**
     * Shared {@link Converter} used by all test cases.
     */
    private static Converter sharedConverter;

    /**
     * Instance‑level {@link Converter} – created fresh for each test to avoid state leakage.
     */
    private Converter converter;

    /**
     * Original JVM default time‑zone – restored after the suite finishes.
     */
    private static TimeZone originalDefaultZone;

    @Mock
    Converter // Mockito will inject a mock instance
    customConverter;

    /**
     * Simple placeholder class used to test custom conversion logic.
     */
    static class MyCustomObject {
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Preserve the original default zone and enforce UTC for deterministic results.
        originalDefaultZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        // Initialise a static Converter that can be reused across test instances.
        sharedConverter = new Converter(new ConverterOptions());
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Converter for each test method. This mirrors the typical
        // usage pattern and guarantees that no mutable state from a previous test
        // interferes with the current one.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly release references – helps the garbage collector in long test suites.
        converter = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore the original default zone.
        TimeZone.setDefault(originalDefaultZone);
        // Clean up the static instance.
        sharedConverter = null;
    }

    /**
     * Provides arguments for the various conversion scenarios defined in the JSON spec.
     */
    private static Stream<Arguments> conversionScenarios() {
        return Stream.of(// Date input (epoch)
        Arguments.of(new Date(0L), null, Instant.ofEpochMilli(0L)), // Instant input (already an Instant)
        Arguments.of(Instant.parse("2021-01-01T00:00:00Z"), null, Instant.parse("2021-01-01T00:00:00Z")), // ZonedDateTime input (UTC)
        Arguments.of(ZonedDateTime.of(2021, 5, 20, 12, 30, 0, 0, ZoneId.of("UTC")), null, Instant.parse("2021-05-20T12:30:00Z")), // LocalDateTime input – default zone is forced to UTC in @BeforeAll
        Arguments.of(LocalDateTime.of(2022, 3, 15, 10, 15, 30), null, Instant.parse("2022-03-15T10:15:30Z")), // ISO‑8601 string with explicit offset
        Arguments.of("2022-12-31T23:59:59+01:00", null, Instant.parse("2022-12-31T22:59:59Z")), // java.sql.Timestamp input
        // 2021-06-01T00:00:00Z
        Arguments.// 2021-06-01T00:00:00Z
        of(// 2021-06-01T00:00:00Z
        new Timestamp(1_622_505_600_000L), null, Instant.parse("2021-06-01T00:00:00Z")));
    }

    @Test
    void shouldConvertIsoStringToInstant() {
        // GIVEN
        String isoString = "2023-01-02T03:04:05Z";
        Instant expected = Instant.parse(isoString);
        // WHEN
        Instant result = StringConversions.toInstant(isoString, converter);
        // THEN
        assertNotNull(result, "Resulting Instant should not be null");
        assertEquals(expected, result, "ISO‑8601 string should be parsed to the correct Instant");
    }

    @Test
    void shouldConvertDateToInstant() {
        // GIVEN
        // corresponds to 2022-01-01T00:00:45Z
        Date date = new Date(1_640_995_445_000L);
        Instant expected = date.toInstant();
        // WHEN
        Instant result = StringConversions.toInstant(date, converter);
        // THEN
        assertNotNull(result, "Resulting Instant should not be null");
        assertEquals(expected, result, "Date should be converted to the same Instant value");
    }

    @Test
    void shouldReturnNullWhenSourceIsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        Instant result = StringConversions.toInstant(source, converter);
        // THEN
        assertNull(result, "Conversion of null should yield null");
    }

    @Test
    void shouldReturnNullForUnsupportedType() {
        // GIVEN
        // No built‑in conversion to ZonedDateTime
        Object unsupported = new Object();
        // WHEN
        Instant result = StringConversions.toInstant(unsupported, converter);
        // THEN
        // The internal toZonedDateTime returns null for unsupported inputs,
        // consequently toInstant should also return null.
        assertNull(result, "Unsupported source type should result in null");
    }

    @ParameterizedTest
    @MethodSource("conversionScenarios")
    void shouldConvertVariousSupportedTypes(Object from, Converter conv, Instant expected) {
        // WHEN
        Instant result = StringConversions.toInstant(from, conv == null ? sharedConverter : conv);
        // THEN
        assertNotNull(result);
        assertEquals(expected, result);
    }

    @Test
    void shouldThrowDateTimeParseExceptionForMalformedString() {
        // GIVEN
        String malformed = "2022-13-40T25:61:61+99:99";
        // WHEN / THEN
        assertThrows(DateTimeParseException.class, () -> StringConversions.toInstant(malformed, sharedConverter));
    }

    @Test
    void shouldThrowDateTimeParseExceptionForNumericString() {
        // GIVEN
        String numeric = "1234567890";
        // WHEN / THEN
        assertThrows(DateTimeParseException.class, () -> StringConversions.toInstant(numeric, sharedConverter));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionForUnsupportedCollection() {
        // GIVEN
        Map<String, String> map = Collections.singletonMap("key", "value");
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toInstant(map, sharedConverter));
    }

    @Test
    void shouldReturnNullWhenSourceIsNullEvenIfConverterIsNull() {
        // GIVEN
        Object source = null;
        Converter nullConverter = null;
        // WHEN
        Instant result = StringConversions.toInstant(source, nullConverter);
        // THEN
        assertNull(result);
    }

    @Test
    void shouldThrowNullPointerExceptionWhenCustomTypeWithNullConverter() {
        // GIVEN
        MyCustomObject custom = new MyCustomObject();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> StringConversions.toInstant(custom, null));
    }

    @Test
    void shouldUseCustomConverterToConvertCustomObject() {
        // GIVEN
        MyCustomObject custom = new MyCustomObject();
        ZonedDateTime expectedZdt = ZonedDateTime.of(LocalDateTime.of(2000, 1, 1, 0, 0), ZoneOffset.UTC);
        // Stub the mock: when the converter is asked to convert MyCustomObject to ZonedDateTime,
        // return the predefined ZonedDateTime.
        when(customConverter.convert(eq(custom), eq(ZonedDateTime.class))).thenReturn(expectedZdt);
        // WHEN
        Instant result = StringConversions.toInstant(custom, customConverter);
        // THEN
        assertNotNull(result);
        assertEquals(expectedZdt.toInstant(), result);
        // Verify that the mock was indeed invoked.
        verify(customConverter, times(1)).convert(eq(custom), eq(ZonedDateTime.class));
    }

    @Test
    void shouldPropagateRuntimeExceptionFromCustomConverter() {
        // GIVEN
        Object source = new Object();
        when(customConverter.convert(eq(source), eq(ZonedDateTime.class))).thenThrow(new RuntimeException("Conversion failed"));
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> StringConversions.toInstant(source, customConverter));
        assertEquals("Conversion failed", ex.getMessage());
    }
}
