package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.time.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toTimestamp_Test {

    // the Converter used for all tests
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Create a Converter that uses UTC – this makes the expected Timestamp deterministic
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(ZoneOffset.UTC);
        converter = new Converter(options);
    }

    @AfterEach
    void teardownAfterEach() {
        // Release reference – not strictly required but keeps the test tidy
        converter = null;
    }

    /**
     * Provides a stream of arguments for {@link #toTimestamp_successfulConversions}.
     * Each entry contains:
     * <ul>
     *   <li>Human‑readable description (used as test name)</li>
     *   <li>LocalDateTime input</li>
     *   <li>ZoneId supplied by the converter</li>
     *   <li>Expected epoch milliseconds</li>
     *   <li>Expected nanosecond remainder (0‑999_999)</li>
     * </ul>
     */
    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// UTC – simple case
        Arguments.of("Successful conversion - UTC", LocalDateTime.of(2023, 1, 2, 15, 30, 45), ZoneId.of("UTC"), 1672672245000L, 0), // America/New_York – winter offset (UTC‑5)
        Arguments.of("Successful conversion - America/New_York (offset handling)", LocalDateTime.of(2023, 1, 2, 15, 30, 45), ZoneId.of("America/New_York"), 1672693845000L, 0), // UTC with nanoseconds
        Arguments.of("Successful conversion - UTC with nanoseconds", LocalDateTime.of(2023, 1, 2, 15, 30, 45, 123_456_789), ZoneId.of("UTC"), 1672672245123L, 456_789), // Asia/Kathmandu (+05:45)
        Arguments.of("Successful conversion - Asia/Kathmandu (offset +5:45)", LocalDateTime.of(2023, 1, 2, 15, 30, 45), ZoneId.of("Asia/Kathmandu"), 1672651545000L, 0), // DST spring‑forward gap (America/New_York)
        Arguments.of("Successful conversion - DST gap (America/New_York spring forward)", LocalDateTime.of(2023, 3, 12, 2, 30, 0), ZoneId.of("America/New_York"), 1678608000000L, 0), // DST fall‑back overlap (America/New_York)
        Arguments.of("Successful conversion - DST overlap (America/New_York fall back)", LocalDateTime.of(2023, 11, 5, 1, 30, 0), ZoneId.of("America/New_York"), 1699162200000L, 0), // Extreme positive offset (Pacific/Kiritimati UTC+14)
        Arguments.of("Successful conversion - Extreme positive offset (Pacific/Kiritimati UTC+14)", LocalDateTime.of(2023, 1, 2, 15, 30, 45), ZoneId.of("Pacific/Kiritimati"), 1672621845000L, 0), // Extreme negative offset (Etc/GMT+12 == UTC‑12)
        Arguments.of("Successful conversion - Extreme negative offset (Etc/GMT+12, i.e., UTC‑12)", LocalDateTime.of(2023, 1, 2, 15, 30, 45), ZoneId.of("Etc/GMT+12"), 1672715445000L, 0), // Epoch start
        Arguments.of("Successful conversion - Epoch start (1970-01-01T00:00:00 UTC)", LocalDateTime.of(1970, 1, 1, 0, 0, 0), ZoneId.of("UTC"), 0L, 0), // Pre‑epoch date (1960-01-01)
        Arguments.of("Successful conversion - Pre‑epoch date (1960-01-01T00:00:00 UTC)", LocalDateTime.of(1960, 1, 1, 0, 0, 0), ZoneId.of("UTC"), -315619200000L, 0));
    }

    @Test
    void toTimestamp_convertsLocalDateTimeUsingConverterZone() {
        // GIVEN – a known LocalDateTime
        // 123 ms
        LocalDateTime ldt = LocalDateTime.of(2023, 5, 10, 12, 34, 56, 123_000_000);
        // AND – the expected Timestamp built from the same instant in UTC
        Instant expectedInstant = ldt.atZone(ZoneOffset.UTC).toInstant();
        Timestamp expected = Timestamp.from(expectedInstant);
        // WHEN – the static conversion method is invoked
        Timestamp actual = LocalDateTimeConversions.toTimestamp(ldt, converter);
        // THEN – the result matches the expected Timestamp
        assertEquals(expected, actual, "Timestamp should represent the same instant as the LocalDateTime in UTC");
    }

    @Test
    void toTimestamp_respectsCustomZoneId() {
        // GIVEN – a Converter configured with a non‑UTC zone (e.g., America/New_York)
        ConverterOptions customOptions = new ConverterOptions();
        // UTC‑5
        customOptions.setZoneId(ZoneOffset.ofHours(-5));
        Converter customConverter = new Converter(customOptions);
        LocalDateTime ldt = LocalDateTime.of(2023, 1, 1, 0, 0, 0, 0);
        // Expected instant is the LocalDateTime interpreted in UTC‑5
        Instant expectedInstant = ldt.atZone(ZoneOffset.ofHours(-5)).toInstant();
        Timestamp expected = Timestamp.from(expectedInstant);
        // WHEN
        Timestamp actual = LocalDateTimeConversions.toTimestamp(ldt, customConverter);
        // THEN
        assertEquals(expected, actual, "Timestamp should be calculated using the Converter's ZoneId (UTC‑5 in this case)");
    }

    @Test
    void toTimestamp_nullInputThrowsClassCastException() {
        // GIVEN a null source
        Object nullSource = null;
        // WHEN / THEN – a ClassCastException is expected because the method casts directly to LocalDateTime
        assertThrows(ClassCastException.class, () -> LocalDateTimeConversions.toTimestamp(nullSource, converter));
    }

    /* --------------------------------------------------------------
     *  Successful conversions – parameterized
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("successfulConversionProvider")
    void toTimestamp_successfulConversions(String description, LocalDateTime ldt, ZoneId zoneId, long expectedEpochMilli, int expectedNanos) {
        // GIVEN a converter configured with the supplied zone
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(zoneId);
        Converter testConverter = new Converter(options);
        // WHEN the conversion method is invoked
        Timestamp actual = LocalDateTimeConversions.toTimestamp(ldt, testConverter);
        // THEN the Timestamp matches the expected instant (millis + nanos)
        Timestamp expected = Timestamp.from(ldt.atZone(zoneId).toInstant());
        assertEquals(expectedEpochMilli, actual.getTime(), "Epoch milliseconds should match expected value");
        assertEquals(expectedNanos, actual.getNanos() % 1_000_000, "Sub‑millisecond nanosecond part should match expected value");
        assertEquals(expected, actual, description + " – full Timestamp equality");
    }

    @Test
    void toTimestamp_nullFromThrowsNullPointerException() {
        // GIVEN a null source object
        Object nullSource = null;
        // WHEN / THEN – NPE because the method dereferences the casted null
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toTimestamp(nullSource, converter), "Null 'from' should result in NullPointerException");
    }

    @Test
    void toTimestamp_invalidTypeThrowsClassCastException() {
        // GIVEN a String instead of a LocalDateTime
        Object badSource = "2023-01-02T15:30:45";
        // WHEN / THEN – the explicit cast triggers a ClassCastException
        assertThrows(ClassCastException.class, () -> LocalDateTimeConversions.toTimestamp(badSource, converter), "Invalid type for 'from' should cause ClassCastException");
    }

    @Test
    void toTimestamp_nullConverterThrowsNullPointerException() {
        // GIVEN a valid LocalDateTime but a null Converter
        LocalDateTime ldt = LocalDateTime.of(2023, 1, 2, 15, 30, 45);
        Converter nullConverter = null;
        // WHEN / THEN – NPE when the method attempts to call getOptions()
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toTimestamp(ldt, nullConverter), "Null Converter should cause NullPointerException");
    }

    @Test
    void toTimestamp_converterWithNullOptionsThrowsNullPointerException() {
        // GIVEN a Converter whose getOptions() returns null
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.getOptions()).thenReturn(null);
        LocalDateTime ldt = LocalDateTime.of(2023, 1, 2, 15, 30, 45);
        // WHEN / THEN – NPE when trying to call getZoneId() on null Options
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toTimestamp(ldt, mockConverter), "Converter with null Options should cause NullPointerException");
    }

    @Test
    void toTimestamp_converterOptionsWithNullZoneIdThrowsNullPointerException() {
        // GIVEN a ConverterOptions with a null ZoneId
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(null);
        Converter badConverter = new Converter(options);
        LocalDateTime ldt = LocalDateTime.of(2023, 1, 2, 15, 30, 45);
        // WHEN / THEN – NPE because atZone(null) is illegal
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toTimestamp(ldt, badConverter), "Converter Options with null ZoneId should cause NullPointerException");
    }
}
