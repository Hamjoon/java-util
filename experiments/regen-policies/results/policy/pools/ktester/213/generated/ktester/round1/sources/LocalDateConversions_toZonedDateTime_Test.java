package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toZonedDateTime_Test {

    // shared for all tests
    private static Converter converter;

    private static final ZoneId UTC = ZoneId.of("UTC");

    @Mock
    private Converter // will be configured per test
    mockConverter;

    /* -------------------------------------------------------------
     *  Helper holder for parameterised arguments
     * ------------------------------------------------------------- */
    private static final class ArgumentsHolder {

        final LocalDate inputDate;

        final ZoneId zone;

        final ZonedDateTime expected;

        ArgumentsHolder(LocalDate inputDate, ZoneId zone, ZonedDateTime expected) {
            this.inputDate = inputDate;
            this.zone = zone;
            this.expected = expected;
        }

        @Override
        public String toString() {
            return "ArgumentsHolder{" + "inputDate=" + inputDate + ", zone=" + zone + ", expected=" + expected + '}';
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Prepare a Converter with a known ZoneId so the result can be asserted.
        ConverterOptions options = new ConverterOptions();
        // use UTC for deterministic testing
        options.setZoneId(ZoneId.of("UTC"));
        converter = new Converter(options);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference – helps GC and isolates tests
        converter = null;
    }

    /**
     * Helper to create a real {@link Converter} that holds the supplied {@link ZoneId}.
     * Used for the few tests that prefer a concrete instance over a mock.
     */
    private static Converter realConverterWithZone(ZoneId zone) {
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(zone);
        return new Converter(options);
    }

    /* -------------------------------------------------------------
     *  Successful conversion test cases – parameterised
     * ------------------------------------------------------------- */
    private static Stream<ArgumentsHolder> successfulCases() {
        return Stream.of(// standard UTC
        new ArgumentsHolder(LocalDate.of(2023, 5, 10), UTC, ZonedDateTime.of(LocalDate.of(2023, 5, 10), LocalTime.MIDNIGHT, UTC)), // New York (DST in July, offset -04:00)
        new ArgumentsHolder(LocalDate.of(2023, 7, 1), ZoneId.of("America/New_York"), ZonedDateTime.of(LocalDate.of(2023, 7, 1), LocalTime.MIDNIGHT, ZoneId.of("America/New_York"))), // ZoneOffset -05:00
        new ArgumentsHolder(LocalDate.of(2022, 12, 25), ZoneOffset.ofHours(-5), ZonedDateTime.of(LocalDate.of(2022, 12, 25), LocalTime.MIDNIGHT, ZoneOffset.ofHours(-5))), // Leap day with Europe/Paris (+01:00 standard offset)
        new ArgumentsHolder(LocalDate.of(2020, 2, 29), ZoneId.of("Europe/Paris"), ZonedDateTime.of(LocalDate.of(2020, 2, 29), LocalTime.MIDNIGHT, ZoneId.of("Europe/Paris"))), // Maximum LocalDate
        new ArgumentsHolder(LocalDate.MAX, UTC, ZonedDateTime.of(LocalDate.MAX, LocalTime.MIDNIGHT, UTC)), // Minimum LocalDate
        new ArgumentsHolder(LocalDate.MIN, UTC, ZonedDateTime.of(LocalDate.MIN, LocalTime.MIDNIGHT, UTC)), // DST start day in New York (midnight still -05:00)
        new ArgumentsHolder(LocalDate.of(2023, 3, 12), ZoneId.of("America/New_York"), ZonedDateTime.of(LocalDate.of(2023, 3, 12), LocalTime.MIDNIGHT, ZoneId.of("America/New_York"))), // DST end day in New York (midnight still -04:00 – still DST)
        new ArgumentsHolder(LocalDate.of(2023, 11, 5), ZoneId.of("America/New_York"), ZonedDateTime.of(LocalDate.of(2023, 11, 5), LocalTime.MIDNIGHT, ZoneId.of("America/New_York"))), // Asia/Kolkata (+05:30 minute‑level offset)
        new ArgumentsHolder(LocalDate.of(2023, 5, 10), ZoneId.of("Asia/Kolkata"), ZonedDateTime.of(LocalDate.of(2023, 5, 10), LocalTime.MIDNIGHT, ZoneId.of("Asia/Kolkata"))), // Explicit ZoneOffset.UTC
        new ArgumentsHolder(LocalDate.of(2023, 5, 10), ZoneOffset.UTC, ZonedDateTime.of(LocalDate.of(2023, 5, 10), LocalTime.MIDNIGHT, ZoneOffset.UTC)));
    }

    @Test
    void toZonedDateTime_convertsLocalDateUsingConverterZone() {
        // GIVEN
        // arbitrary date
        LocalDate inputDate = LocalDate.of(2023, 5, 10);
        // WHEN
        ZonedDateTime result = LocalDateConversions.toZonedDateTime(inputDate, converter);
        // THEN
        assertNotNull(result, "Result should not be null");
        assertEquals(inputDate, result.toLocalDate(), "The LocalDate part must match the input");
        assertEquals(LocalTime.MIDNIGHT, result.toLocalTime(), "Time should be midnight (00:00:00)");
        assertEquals(ZoneId.of("UTC"), result.getZone(), "ZoneId must come from ConverterOptions");
    }

    @ParameterizedTest(name = "[{index}] {0} with zone {1}")
    @MethodSource("successfulCases")
    void toZonedDateTime_successfulConversions(ArgumentsHolder holder) {
        // GIVEN
        Converter converter = realConverterWithZone(holder.zone);
        // WHEN
        ZonedDateTime result = LocalDateConversions.toZonedDateTime(holder.inputDate, converter);
        // THEN
        assertNotNull(result, "Result must not be null");
        assertEquals(holder.expected, result, () -> "Expected ZonedDateTime does not match for input " + holder.inputDate);
    }

    @Test
    void toZonedDateTime_nullFrom_throwsNullPointerException() {
        // GIVEN
        Converter converter = realConverterWithZone(UTC);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toZonedDateTime(null, converter), "Null `from` should cause NullPointerException (ZonedDateTime.of receives null)");
    }

    @Test
    void toZonedDateTime_wrongTypeFrom_throwsClassCastException() {
        // GIVEN
        Converter converter = realConverterWithZone(UTC);
        String notALocalDate = "2023-05-10";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> LocalDateConversions.toZonedDateTime(notALocalDate, converter), "Passing a non‑LocalDate should raise ClassCastException");
    }

    @Test
    void toZonedDateTime_nullConverter_throwsNullPointerException() {
        // GIVEN
        LocalDate date = LocalDate.of(2023, 5, 10);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toZonedDateTime(date, null), "Null converter must cause NullPointerException when getOptions() is invoked");
    }

    @Test
    void toZonedDateTime_converterWithNullOptions_throwsNullPointerException() {
        // GIVEN
        LocalDate date = LocalDate.of(2023, 5, 10);
        // Mock a Converter that returns null for getOptions()
        when(mockConverter.getOptions()).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toZonedDateTime(date, mockConverter), "Null options should cause NullPointerException before ZonedDateTime creation");
    }

    @Test
    void toZonedDateTime_converterWithNullZoneId_throwsNullPointerException() {
        // GIVEN
        LocalDate date = LocalDate.of(2023, 5, 10);
        // zoneId left null
        ConverterOptions options = new ConverterOptions();
        Converter converter = new Converter(options);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toZonedDateTime(date, converter), "Null ZoneId inside options must cause NullPointerException");
    }
}
