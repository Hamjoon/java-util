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

    /**
     * Mocked converter that will be passed to the method under test.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Mocked options returned by {@code mockConverter.getOptions()}.
     */
    @Mock
    private ConverterOptions mockOptions;

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mock hierarchy:
        // converter.getOptions() -> mockOptions
        // mockOptions.getZoneId() -> a deterministic ZoneId (UTC in this case)
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getZoneId()).thenReturn(ZoneOffset.UTC);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions to keep tests independent.
        clearInvocations(mockConverter, mockOptions);
    }

    /**
     * Supplies test data for {@link #toTimestamp_convertsLocalDateTimeUsingProvidedZone}.
     *
     * <p>Two cases are defined, matching the JSON input:
     * <ol>
     *   <li>UTC zone – expects epoch millis 1672574400000</li>
     *   <li>America/New_York – expects epoch millis 1672596000000</li>
     * </ol>
     *
     * @return a stream of arguments for the parameterised test
     */
    private static Stream<Arguments> validConversionProvider() {
        // Case 1 – default UTC zone
        LocalDateTime utcSource = LocalDateTime.of(2023, 1, 1, 12, 0, 0);
        ZoneId utcZone = ZoneId.of("UTC");
        // 2023‑01‑01T12:00:00Z
        Instant utcInstant = utcSource.atZone(utcZone).toInstant();
        // Case 2 – America/New_York (UTC‑5 at the given date, no DST)
        LocalDateTime nySource = LocalDateTime.of(2023, 1, 1, 12, 0, 0);
        ZoneId nyZone = ZoneId.of("America/New_York");
        // 2023‑01‑01T17:00:00Z
        Instant nyInstant = nySource.atZone(nyZone).toInstant();
        return Stream.of(Arguments.of(utcSource, utcZone, utcInstant), Arguments.of(nySource, nyZone, nyInstant));
    }

    @Test
    void toTimestamp_convertsLocalDateTimeUsingConverterZone() {
        // GIVEN
        LocalDateTime source = LocalDateTime.of(2023, Month.MAY.getValue(), 10, 12, 30, 45, // 123,456,789 nanoseconds
        123_456_789);
        // Expected instant based on the mocked zone (UTC)
        Instant expectedInstant = source.atZone(ZoneOffset.UTC).toInstant();
        Timestamp expectedTimestamp = Timestamp.from(expectedInstant);
        // WHEN
        Timestamp actual = LocalDateTimeConversions.toTimestamp(source, mockConverter);
        // THEN
        assertNotNull(actual, "Resulting Timestamp should not be null");
        assertEquals(expectedTimestamp, actual, () -> "Timestamp should represent the same instant as the source LocalDateTime in UTC");
        // Verify that the converter's options were consulted exactly once.
        verify(mockConverter, times(1)).getOptions();
        verify(mockOptions, times(1)).getZoneId();
    }

    @Test
    void toTimestamp_convertsLocalDateTimeUsingDifferentZone() {
        // GIVEN
        ZoneId zone = ZoneId.of("America/New_York");
        when(mockOptions.getZoneId()).thenReturn(zone);
        LocalDateTime source = LocalDateTime.of(2021, 12, 31, 23, 0, 0, 0);
        Instant expectedInstant = source.atZone(zone).toInstant();
        Timestamp expectedTimestamp = Timestamp.from(expectedInstant);
        // WHEN
        Timestamp actual = LocalDateTimeConversions.toTimestamp(source, mockConverter);
        // THEN
        assertEquals(expectedTimestamp, actual, () -> "Timestamp should be calculated using the provided America/New_York zone");
    }

    @Test
    void toTimestamp_throwsClassCastExceptionWhenSourceIsNotLocalDateTime() {
        // GIVEN – an object of an unrelated type
        Object notALocalDateTime = "2023-01-01T00:00:00";
        // WHEN / THEN – the method should fail fast with a ClassCastException
        assertThrows(ClassCastException.class, () -> LocalDateTimeConversions.toTimestamp(notALocalDateTime, mockConverter), "Method should throw ClassCastException when the source object is not a LocalDateTime");
        // Even though the conversion never proceeds, the method still accesses the converter
        verify(mockConverter, times(1)).getOptions();
        verify(mockOptions, times(1)).getZoneId();
    }

    /**
     * Parameterised test that validates correct conversion for a variety of
     * {@link LocalDateTime}/{@link ZoneId} pairs.
     *
     * <p>The data originates from the supplied JSON test cases.
     *
     * @param source          the {@code LocalDateTime} to convert
     * @param zoneId          the zone that the mocked {@code ConverterOptions} should return
     * @param expectedInstant the expected {@link Instant} after applying the zone
     */
    @ParameterizedTest(name = "[{index}] {0} with zone {1}")
    @MethodSource("validConversionProvider")
    void toTimestamp_convertsLocalDateTimeUsingProvidedZone(LocalDateTime source, ZoneId zoneId, Instant expectedInstant) {
        // GIVEN – configure the mock to return the specific zone for this iteration
        when(mockOptions.getZoneId()).thenReturn(zoneId);
        // Expected Timestamp built directly from the expected Instant
        Timestamp expectedTimestamp = Timestamp.from(expectedInstant);
        // WHEN – invoke the method under test
        Timestamp actual = LocalDateTimeConversions.toTimestamp(source, mockConverter);
        // THEN – verify the conversion result and mock interactions
        assertNotNull(actual, "Resulting Timestamp must not be null");
        assertEquals(expectedTimestamp, actual, () -> "Timestamp should represent the same instant as the source LocalDateTime in zone " + zoneId);
        verify(mockConverter, times(1)).getOptions();
        verify(mockOptions, times(1)).getZoneId();
    }
}
