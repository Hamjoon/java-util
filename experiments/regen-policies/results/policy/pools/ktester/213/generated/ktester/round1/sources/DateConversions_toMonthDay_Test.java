package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.format.DateTimeException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DateConversions_toMonthDay_Test {

    /**
     * Mocked {@link Converter} used in all tests.
     */
    @Mock
    Converter mockConverter;

    /**
     * Mocked {@link ConverterOptions} returned by {@link Converter#getOptions()}.
     */
    @Mock
    ConverterOptions mockOptions;

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mock hierarchy: converter -> options -> zoneId.
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        // Use a deterministic zone (UTC) so the test is repeatable on any machine.
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions to avoid cross‑test contamination.
        clearInvocations(mockConverter, mockOptions);
    }

    private static Stream<Arguments> successCases() {
        return Stream.of(// Normal conversion – UTC zone
        Arguments.of("Normal conversion - UTC zone", Date.from(Instant.parse("2023-07-15T12:34:56Z")), ZoneId.of("UTC"), MonthDay.of(7, 15)), // Time‑zone conversion affecting the day (America/New_York)
        Arguments.of("Time‑zone conversion – shift to previous day (NY)", Date.from(Instant.parse("2023-03-01T01:00:00Z")), ZoneId.of("America/New_York"), MonthDay.of(2, 28)), // Time‑zone conversion affecting the day (Asia/Tokyo)
        Arguments.of("Time‑zone conversion – shift to next day (Tokyo)", Date.from(Instant.parse("2023-12-31T23:30:00Z")), ZoneId.of("Asia/Tokyo"), MonthDay.of(1, 1)), // Leap day – same zone (UTC)
        Arguments.of("Leap day conversion - same zone (UTC)", Date.from(Instant.parse("2020-02-29T12:00:00Z")), ZoneId.of("UTC"), MonthDay.of(2, 29)), // Leap day – shift to next day (Europe/Paris)
        Arguments.of("Leap day conversion - timezone shift to next day", Date.from(Instant.parse("2020-02-29T23:30:00Z")), ZoneId.of("Europe/Paris"), MonthDay.of(3, 1)), // Leap day – shift to previous day (America/New_York)
        Arguments.of("Leap day conversion - timezone shift to previous day (non‑leap year)", Date.from(Instant.parse("2020-02-29T01:00:00Z")), ZoneId.of("America/New_York"), MonthDay.of(2, 28)), // Epoch start (1970‑01‑01) UTC
        Arguments.of("Epoch start conversion (1970‑01‑01)", new Date(0L), ZoneId.of("UTC"), MonthDay.of(1, 1)), // Pre‑epoch conversion crossing day boundary (+02:00)
        Arguments.of("Pre‑epoch conversion crossing day boundary", Date.from(Instant.parse("1969-12-31T23:00:00Z")), ZoneId.of("+02:00"), MonthDay.of(1, 1)), // SQL Date subclass conversion
        Arguments.of("SQL Date subclass conversion", java.sql.Date.valueOf("2023-08-15"), ZoneId.of("UTC"), MonthDay.of(8, 15)), // ZoneOffset.UTC (same as UTC) conversion
        Arguments.of("ZoneOffset.UTC (same as UTC) conversion", Date.from(Instant.parse("2023-11-05T10:15:30Z")), ZoneOffset.UTC, MonthDay.of(11, 5)));
    }

    private static Stream<Arguments> exceptionCases() {
        // Helper converter that returns null options (used in one test case)
        Converter converterWithNullOptions = mock(Converter.class);
        when(converterWithNullOptions.getOptions()).thenReturn(null);
        return Stream.of(// Null Date argument
        Arguments.of("Null Date argument", null, ZoneId.of("UTC"), // any non‑null converter
        mock(Converter.class), NullPointerException.class), // Non‑Date source (ClassCastException)
        Arguments.of("Non‑Date source (ClassCastException)", // Using a Date reference that actually holds a String – we cannot create that,
        // so we pass a dummy object and rely on the method's unchecked cast.
        // The method casts the argument to java.util.Date, causing ClassCastException.
        (Date) (Object) "2023-07-15", ZoneId.of("UTC"), mock(Converter.class), ClassCastException.class), // Null Converter argument
        Arguments.of("Null Converter argument", new Date(), ZoneId.of("UTC"), null, NullPointerException.class), // Converter with null options (getOptions() returns null)
        Arguments.of("Converter options null", new Date(), ZoneId.of("UTC"), converterWithNullOptions, NullPointerException.class), // Extreme future date causing DateTimeException
        Arguments.of("Extreme future date causing DateTimeException", new Date(Long.MAX_VALUE), ZoneId.of("UTC"), mock(Converter.class), DateTimeException.class));
    }

    @Test
    void toMonthDay_convertsDateToMonthDay_usingConverterZone() {
        // GIVEN – a known date (2023‑05‑17) at start of day in UTC.
        LocalDate expectedLocalDate = LocalDate.of(2023, 5, 17);
        Instant instant = expectedLocalDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Date date = Date.from(instant);
        // WHEN – the conversion method is invoked.
        MonthDay result = DateConversions.toMonthDay(date, mockConverter);
        // THEN – the result should represent the same month and day.
        assertNotNull(result, "Resulting MonthDay must not be null");
        assertEquals(MonthDay.from(expectedLocalDate), result, "MonthDay should match the month and day of the source Date");
    }

    @Test
    void toMonthDay_handlesLeapDayCorrectly() {
        // GIVEN – February 29, 2020 (a leap day) in UTC.
        LocalDate leapDate = LocalDate.of(2020, 2, 29);
        Instant instant = leapDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Date date = Date.from(instant);
        // WHEN
        MonthDay result = DateConversions.toMonthDay(date, mockConverter);
        // THEN
        assertEquals(MonthDay.of(2, 29), result, "MonthDay should correctly represent the leap day");
    }

    @Test
    void toMonthDay_throwsClassCastException_whenSourceIsNotDate() {
        // GIVEN – an object that is not a java.util.Date.
        Object notADate = "2023-01-01";
        // WHEN / THEN – a ClassCastException is expected because the method
        // performs an unchecked cast to Date.
        assertThrows(ClassCastException.class, () -> DateConversions.toMonthDay(notADate, mockConverter), "Providing a non‑Date source should result in ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("successCases")
    void toMonthDay_successfulConversion(String description, Date from, ZoneId zoneId, MonthDay expected) {
        // GIVEN – a mock converter that returns the requested zone.
        when(mockOptions.getZoneId()).thenReturn(zoneId);
        // WHEN – conversion is performed.
        MonthDay result = DateConversions.toMonthDay(from, mockConverter);
        // THEN – the result matches the expected MonthDay.
        assertEquals(expected, result, () -> "Unexpected MonthDay for case: " + description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionCases")
    void toMonthDay_expectedExceptions(String description, Date from, ZoneId zoneId, Converter converter, Class<? extends Throwable> expectedException) {
        // WHEN / THEN – the conversion should throw the expected exception.
        Throwable thrown = assertThrows(expectedException, () -> DateConversions.toMonthDay(from, converter), () -> "Expected " + expectedException.getSimpleName() + " for case: " + description);
        // For NullPointerException we can optionally assert the message contains something useful.
        if (expectedException == NullPointerException.class && thrown.getMessage() != null) {
            assertTrue(thrown.getMessage().toLowerCase().contains("null"), "NullPointerException message should mention null");
        }
    }
}
