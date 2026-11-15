package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toZonedDateTime_Test {

    @Mock
    private Converter converter;

    @Mock
    private ConverterOptions options;

    @BeforeEach
    void setupBeforeEach() {
        // Default mock behaviour – can be overridden inside each test case
        when(converter.getOptions()).thenReturn(options);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions to avoid cross‑test interference
        clearInvocations(converter, options);
    }

    private static Stream<Arguments> normalConversionProvider() {
        return Stream.of(Arguments.of(LocalDate.of(2023, 5, 10), ZoneId.of("America/New_York")));
    }

    private static Stream<Arguments> boundaryDatesProvider() {
        return Stream.of(Arguments.of(LocalDate.MIN), Arguments.of(LocalDate.MAX));
    }

    @Test
    @DisplayName("Convert LocalDate to ZonedDateTime using UTC zone")
    void testToZonedDateTime_UtcZone() {
        // given
        LocalDate localDate = LocalDate.of(2023, 5, 10);
        when(options.getZoneId()).thenReturn(ZoneId.of("UTC"));
        // when
        ZonedDateTime result = LocalDateConversions.toZonedDateTime(localDate, converter);
        // then
        assertNotNull(result, "Result should not be null");
        assertEquals(localDate, result.toLocalDate(), "Date part must match the input LocalDate");
        assertEquals(LocalTime.MIDNIGHT, result.toLocalTime(), "Time part must be midnight");
        assertEquals(ZoneId.of("UTC"), result.getZone(), "Zone must be UTC");
    }

    @Test
    @DisplayName("Convert LocalDate to ZonedDateTime using a non‑UTC zone")
    void testToZonedDateTime_NonUtcZone() {
        // given
        LocalDate localDate = LocalDate.of(2021, 12, 31);
        ZoneId zoneId = ZoneId.of("America/New_York");
        when(options.getZoneId()).thenReturn(zoneId);
        // when
        ZonedDateTime result = LocalDateConversions.toZonedDateTime(localDate, converter);
        // then
        assertNotNull(result);
        assertEquals(localDate, result.toLocalDate());
        assertEquals(LocalTime.MIDNIGHT, result.toLocalTime());
        assertEquals(zoneId, result.getZone());
    }

    @Test
    @DisplayName("Passing a non‑LocalDate should result in ClassCastException")
    void testToZonedDateTime_InvalidInput() {
        // given
        String notALocalDate = "2022-01-01";
        when(options.getZoneId()).thenReturn(ZoneId.of("UTC"));
        // when / then
        assertThrows(ClassCastException.class, () -> {
            LocalDateConversions.toZonedDateTime(notALocalDate, converter);
        });
    }

    /* --------------------------------------------------------------------- *
     *  Normal conversion – single case (parameterised for future extension) *
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => from={0}, zoneId={1}")
    @MethodSource("normalConversionProvider")
    @DisplayName("Normal conversion of LocalDate to ZonedDateTime")
    void testNormalConversion(LocalDate from, ZoneId zoneId) {
        // given
        when(options.getZoneId()).thenReturn(zoneId);
        // when
        ZonedDateTime result = LocalDateConversions.toZonedDateTime(from, converter);
        // then
        assertNotNull(result, "Result must not be null");
        assertEquals(from, result.toLocalDate(), "Date part must match the input LocalDate");
        assertEquals(LocalTime.MIDNIGHT, result.toLocalTime(), "Time part must be midnight");
        assertEquals(zoneId, result.getZone(), "Zone must match the provided ZoneId");
    }

    /* --------------------------------------------------------------------- *
     *  Boundary dates – LocalDate.MIN and LocalDate.MAX                     *
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => from={0}")
    @MethodSource("boundaryDatesProvider")
    @DisplayName("Conversion of boundary LocalDate values")
    void testBoundaryDates(LocalDate from) {
        // given
        when(options.getZoneId()).thenReturn(ZoneId.of("UTC"));
        // when
        ZonedDateTime result = LocalDateConversions.toZonedDateTime(from, converter);
        // then
        assertNotNull(result);
        assertEquals(from, result.toLocalDate());
        assertEquals(LocalTime.MIDNIGHT, result.toLocalTime());
        assertEquals(ZoneId.of("UTC"), result.getZone());
    }

    /* --------------------------------------------------------------------- *
     *  Null `from` argument                                                   *
     * --------------------------------------------------------------------- */
    @Test
    @DisplayName("Passing null as `from` should throw NullPointerException")
    void testNullFrom() {
        // given
        when(options.getZoneId()).thenReturn(ZoneId.of("UTC"));
        // when / then
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toZonedDateTime(null, converter), "Null `from` must cause NullPointerException");
    }

    /* --------------------------------------------------------------------- *
     *  Invalid `from` type (e.g., String)                                      *
     * --------------------------------------------------------------------- */
    @Test
    @DisplayName("Passing a non‑LocalDate should result in ClassCastException")
    void testInvalidFromType() {
        // given
        when(options.getZoneId()).thenReturn(ZoneId.of("UTC"));
        String notALocalDate = "2023-05-10";
        // when / then
        assertThrows(ClassCastException.class, () -> LocalDateConversions.toZonedDateTime(notALocalDate, converter), "Non‑LocalDate input must cause ClassCastException");
    }

    /* --------------------------------------------------------------------- *
     *  Converter returns null options                                           *
     * --------------------------------------------------------------------- */
    @Test
    @DisplayName("Null ConverterOptions should cause NullPointerException")
    void testNullOptions() {
        // given
        when(converter.getOptions()).thenReturn(null);
        LocalDate from = LocalDate.of(2023, 5, 10);
        // when / then
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toZonedDateTime(from, converter), "Null options must cause NullPointerException when accessing getZoneId()");
    }

    /* --------------------------------------------------------------------- *
     *  Options provide a null ZoneId                                            *
     * --------------------------------------------------------------------- */
    @Test
    @DisplayName("Null ZoneId in options should cause NullPointerException")
    void testNullZoneId() {
        // given
        when(options.getZoneId()).thenReturn(null);
        LocalDate from = LocalDate.of(2023, 5, 10);
        // when / then
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toZonedDateTime(from, converter), "Null ZoneId must cause NullPointerException in ZonedDateTime.of");
    }
}
