package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class InstantConversions_toOffsetDateTime_Test {

    @Mock
    private Converter // the Converter instance passed to the method
    converter;

    @Mock
    private ConverterOptions // the options object returned by converter.getOptions()
    options;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the mock hierarchy: converter -> options
        when(converter.getOptions()).thenReturn(options);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks after each test to avoid cross‑test interference
        clearInvocations(converter, options);
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(Arguments.of("UTC zone – epoch instant", Instant.ofEpochSecond(0), TimeZone.getTimeZone("UTC"), OffsetDateTime.ofInstant(Instant.ofEpochSecond(0), ZoneOffset.UTC)), Arguments.of("GMT+05:30 zone – epoch instant", Instant.ofEpochSecond(0), TimeZone.getTimeZone("GMT+05:30"), OffsetDateTime.ofInstant(Instant.ofEpochSecond(0), ZoneOffset.ofHoursMinutes(5, 30))), Arguments.of("GMT-04:00 zone – epoch instant", Instant.ofEpochSecond(0), TimeZone.getTimeZone("GMT-04:00"), OffsetDateTime.ofInstant(Instant.ofEpochSecond(0), ZoneOffset.ofHours(-4))), Arguments.of("UTC zone – instant with nanoseconds", Instant.ofEpochSecond(0, 123_456_789), TimeZone.getTimeZone("UTC"), OffsetDateTime.ofInstant(Instant.ofEpochSecond(0, 123_456_789), ZoneOffset.UTC)), Arguments.of("GMT+02:00 zone – far‑future instant", Instant.ofEpochSecond(9_000_000_000L), TimeZone.getTimeZone("GMT+02:00"), OffsetDateTime.ofInstant(Instant.ofEpochSecond(9_000_000_000L), ZoneOffset.ofHours(2))), Arguments.of("GMT-03:30 zone – pre‑1970 instant", Instant.ofEpochSecond(-1), TimeZone.getTimeZone("GMT-03:30"), OffsetDateTime.ofInstant(Instant.ofEpochSecond(-1), ZoneOffset.ofHoursMinutes(-3, -30))), Arguments.of("GMT+05:45 zone – epoch instant", Instant.ofEpochSecond(0), TimeZone.getTimeZone("GMT+05:45"), OffsetDateTime.ofInstant(Instant.ofEpochSecond(0), ZoneOffset.ofHoursMinutes(5, 45))), Arguments.of("GMT-02:15 zone – epoch instant", Instant.ofEpochSecond(0), TimeZone.getTimeZone("GMT-02:15"), OffsetDateTime.ofInstant(Instant.ofEpochSecond(0), ZoneOffset.ofHoursMinutes(-2, -15))), Arguments.of("America/New_York zone – epoch instant (dynamic offset)", Instant.ofEpochSecond(0), TimeZone.getTimeZone("America/New_York"), OffsetDateTime.ofInstant(Instant.ofEpochSecond(0), ZoneOffset.ofTotalSeconds(TimeZone.getTimeZone("America/New_York").getOffset(System.currentTimeMillis()) / 1000))));
    }

    private static Stream<Arguments> exceptionProvider() {
        ???;
    }

    /**
     * Verify that an {@link Instant} is correctly converted to an {@link OffsetDateTime}
     * when the Converter's time‑zone option is UTC (offset 0).
     */
    @Test
    void toOffsetDateTime_UtcTimeZone() {
        // GIVEN
        Instant instant = Instant.parse("2023-01-01T12:34:56Z");
        TimeZone utc = TimeZone.getTimeZone("UTC");
        when(options.getTimeZone()).thenReturn(utc);
        // WHEN
        OffsetDateTime result = InstantConversions.toOffsetDateTime(instant, converter);
        // THEN
        OffsetDateTime expected = instant.atOffset(ZoneOffset.UTC);
        assertEquals(expected, result, "OffsetDateTime should be the instant at UTC offset when the converter's time zone is UTC");
        // Verify interaction
        verify(options).getTimeZone();
    }

    /**
     * Verify conversion when the Converter's time‑zone option is a fixed offset
     * (e.g., GMT+02:00). The method uses {@code System.currentTimeMillis()} to obtain the
     * offset, but for a fixed‑offset zone the result is deterministic.
     */
    @Test
    void toOffsetDateTime_FixedOffsetTimeZone() {
        // GIVEN
        Instant instant = Instant.parse("2023-06-15T08:00:00Z");
        // "GMT+02:00" is a fixed‑offset zone (no DST)
        TimeZone fixed = TimeZone.getTimeZone("GMT+02:00");
        when(options.getTimeZone()).thenReturn(fixed);
        // WHEN
        OffsetDateTime result = InstantConversions.toOffsetDateTime(instant, converter);
        // THEN
        ZoneOffset expectedOffset = ZoneOffset.ofTotalSeconds(fixed.getOffset(System.currentTimeMillis()) / 1000);
        OffsetDateTime expected = instant.atOffset(expectedOffset);
        assertEquals(expected, result, "OffsetDateTime should reflect the fixed GMT+02:00 offset");
        verify(options).getTimeZone();
    }

    /**
     * Guard against a {@code null} source object – the method casts the argument to {@link Instant},
     * so a {@code NullPointerException} is expected.
     */
    @Test
    void toOffsetDateTime_NullSourceThrowsNPE() {
        // GIVEN
        TimeZone utc = TimeZone.getTimeZone("UTC");
        when(options.getTimeZone()).thenReturn(utc);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> InstantConversions.toOffsetDateTime(null, converter), "Passing a null source should result in NullPointerException due to the cast to Instant");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionProvider")
    void toOffsetDateTime_ValidConversions(String description, Instant instant, TimeZone timeZone, OffsetDateTime expected) {
        when(options.getTimeZone()).thenReturn(timeZone);
        OffsetDateTime result = InstantConversions.toOffsetDateTime(instant, converter);
        assertEquals(expected, result, () -> "Failed conversion: " + description);
        verify(options).getTimeZone();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionProvider")
    void toOffsetDateTime_ExceptionScenarios(String description, Object from, Converter suppliedConverter, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> InstantConversions.toOffsetDateTime(from, suppliedConverter), () -> "Expected " + expectedException.getSimpleName() + " for: " + description);
    }
}
