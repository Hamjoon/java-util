package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.sql.Date;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toSqlDate_Test {

    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise mocks and default stubbing.
        MockitoAnnotations.openMocks(this);
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        // Default to UTC – individual tests can override if needed.
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions to avoid cross‑test interference.
        clearInvocations(mockConverter, mockOptions);
    }

    private static Stream<Arguments> normalBehaviorProvider() {
        return Stream.of(Arguments.of(LocalDateTime.of(2023, 3, 15, 10, 30), ZoneId.of("UTC"), Date.valueOf("2023-03-15")), Arguments.of(LocalDateTime.of(2023, 12, 31, 23, 45), ZoneId.of("America/New_York"), Date.valueOf("2023-12-31")));
    }

    private static Stream<Arguments> errorHandlingProvider() {
        // Mock that returns null options
        Converter converterWithNullOptions = mock(Converter.class);
        when(converterWithNullOptions.getOptions()).thenReturn(null);
        // Mock that returns options whose zoneId is null
        ConverterOptions optionsWithNullZone = mock(ConverterOptions.class);
        when(optionsWithNullZone.getZoneId()).thenReturn(null);
        Converter converterWithNullZone = mock(Converter.class);
        when(converterWithNullZone.getOptions()).thenReturn(optionsWithNullZone);
        // Default mock (will be overridden by the test class @BeforeEach)
        Converter defaultMock = mock(Converter.class);
        ConverterOptions defaultOptions = mock(ConverterOptions.class);
        when(defaultMock.getOptions()).thenReturn(defaultOptions);
        when(defaultOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
        return Stream.of(Arguments.of("null source", null, defaultMock, NullPointerException.class), Arguments.of("non‑LocalDateTime source", "2023-03-15", defaultMock, ClassCastException.class), Arguments.of("null converter", LocalDateTime.now(), null, NullPointerException.class), Arguments.of("converter returns null options", LocalDateTime.now(), converterWithNullOptions, NullPointerException.class), Arguments.of("options return null ZoneId", LocalDateTime.now(), converterWithNullZone, NullPointerException.class));
    }

    private static Stream<Arguments> boundaryDatesProvider() {
        return Stream.of(Arguments.of(LocalDateTime.of(1970, 1, 1, 0, 0), ZoneId.of("UTC"), Date.valueOf("1970-01-01")), Arguments.of(LocalDateTime.of(9999, 12, 31, 23, 59, 59, 999_999_999), ZoneId.of("Asia/Tokyo"), Date.valueOf("9999-12-31")));
    }

    private static Stream<Arguments> sqlDateRangeExceptionProvider() {
        return Stream.of(Arguments.of(0, LocalDateTime.of(0, 1, 1, 0, 0), ZoneId.of("UTC"), IllegalArgumentException.class), Arguments.of(10000, LocalDateTime.of(10000, 1, 1, 0, 0), ZoneId.of("UTC"), IllegalArgumentException.class));
    }

    @Test
    void toSqlDate_convertsLocalDateTimeToSqlDate_usingDefaultZone() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2023, 3, 15, 10, 20, 30);
        // WHEN
        Date result = LocalDateTimeConversions.toSqlDate(ldt, mockConverter);
        // THEN
        Date expected = Date.valueOf(LocalDate.of(2023, 3, 15));
        assertEquals(expected, result);
        // Verify that the converter's options were consulted.
        verify(mockConverter).getOptions();
        verify(mockOptions).getZoneId();
    }

    @Test
    void toSqlDate_respectsProvidedZoneId() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2022, 12, 31, 23, 30);
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("America/New_York"));
        // WHEN
        Date result = LocalDateTimeConversions.toSqlDate(ldt, mockConverter);
        // THEN
        // The zone does not alter the local date part; it should still be 2022‑12‑31.
        Date expected = Date.valueOf(LocalDate.of(2022, 12, 31));
        assertEquals(expected, result);
        verify(mockConverter).getOptions();
        verify(mockOptions).getZoneId();
    }

    @Test
    void toSqlDate_throwsClassCastException_whenSourceIsNotLocalDateTime() {
        // GIVEN
        Object notLdt = "2023-01-01T00:00:00";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> LocalDateTimeConversions.toSqlDate(notLdt, mockConverter));
        // No interaction with options should occur because casting fails first.
        verify(mockConverter, never()).getOptions();
    }

    @Test
    void toSqlDate_throwsNullPointerException_whenSourceIsNull() {
        // GIVEN
        Object nullSource = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toSqlDate(nullSource, mockConverter));
        // The method attempts to cast null, which triggers NPE before any option lookup.
        verify(mockConverter, never()).getOptions();
    }

    /* -----------------------------------------------------------
     *  Normal behavior
     * ----------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("normalBehaviorProvider")
    void toSqlDate_normalBehavior(LocalDateTime from, ZoneId zone, Date expected) {
        // GIVEN – zone is stubbed on the default mock options
        when(mockOptions.getZoneId()).thenReturn(zone);
        // WHEN
        Date result = LocalDateTimeConversions.toSqlDate(from, mockConverter);
        // THEN
        assertEquals(expected, result);
        verify(mockConverter).getOptions();
        verify(mockOptions).getZoneId();
    }

    /* -----------------------------------------------------------
     *  Error handling / edge cases
     * ----------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("errorHandlingProvider")
    void toSqlDate_errorHandling(String description, Object from, Converter converter, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        assertThrows(expectedException, () -> LocalDateTimeConversions.toSqlDate(from, converter), description);
    }

    /* -----------------------------------------------------------
     *  Boundary dates
     * ----------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("boundaryDatesProvider")
    void toSqlDate_boundaryDates(LocalDateTime from, ZoneId zone, Date expected) {
        when(mockOptions.getZoneId()).thenReturn(zone);
        Date result = LocalDateTimeConversions.toSqlDate(from, mockConverter);
        assertEquals(expected, result);
    }

    /* -----------------------------------------------------------
     *  Time component ignored
     * ----------------------------------------------------------- */
    @Test
    void toSqlDate_timeComponentIgnored() {
        LocalDateTime ldt = LocalDateTime.of(2023, 3, 15, 23, 59, 59, 999_999_999);
        Date expected = Date.valueOf("2023-03-15");
        Date result = LocalDateTimeConversions.toSqlDate(ldt, mockConverter);
        assertEquals(expected, result);
    }

    /* -----------------------------------------------------------
     *  ZoneId with positive offset
     * ----------------------------------------------------------- */
    @Test
    void toSqlDate_zoneIdPositiveOffset() {
        LocalDateTime ldt = LocalDateTime.of(2023, 3, 15, 0, 0);
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC+02:00"));
        Date expected = Date.valueOf("2023-03-15");
        Date result = LocalDateTimeConversions.toSqlDate(ldt, mockConverter);
        assertEquals(expected, result);
    }

    /* -----------------------------------------------------------
     *  ZoneId with negative offset
     * ----------------------------------------------------------- */
    @Test
    void toSqlDate_zoneIdNegativeOffset() {
        LocalDateTime ldt = LocalDateTime.of(2023, 3, 15, 0, 0);
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC-05:00"));
        Date expected = Date.valueOf("2023-03-15");
        Date result = LocalDateTimeConversions.toSqlDate(ldt, mockConverter);
        assertEquals(expected, result);
    }

    /* -----------------------------------------------------------
     *  Converter with custom Options object returning same ZoneId
     * ----------------------------------------------------------- */
    @Test
    void toSqlDate_converterWithCustomOptions() {
        // Simulate a Converter whose Options are configured after construction.
        Converter customConverter = mock(Converter.class);
        ConverterOptions customOptions = mock(ConverterOptions.class);
        when(customConverter.getOptions()).thenReturn(customOptions);
        when(customOptions.getZoneId()).thenReturn(ZoneId.of("Europe/Paris"));
        LocalDateTime ldt = LocalDateTime.of(2022, 2, 28, 12, 0);
        Date expected = Date.valueOf("2022-02-28");
        Date result = LocalDateTimeConversions.toSqlDate(ldt, customConverter);
        assertEquals(expected, result);
        verify(customConverter).getOptions();
        verify(customOptions).getZoneId();
    }

    /* -----------------------------------------------------------
     *  DST gap exception
     * ----------------------------------------------------------- */
    @Test
    void toSqlDate_dstGapThrowsDateTimeException() {
        LocalDateTime ldt = LocalDateTime.of(2023, 3, 12, 2, 30);
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("America/New_York"));
        assertThrows(DateTimeException.class, () -> LocalDateTimeConversions.toSqlDate(ldt, mockConverter), "LocalDateTime in DST spring‑forward gap should throw DateTimeException");
    }

    /* -----------------------------------------------------------
     *  SQL Date range exception
     * ----------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] year {0}")
    @MethodSource("sqlDateRangeExceptionProvider")
    void toSqlDate_sqlDateRangeException(int year, LocalDateTime from, ZoneId zone, Class<? extends Throwable> expected) {
        when(mockOptions.getZoneId()).thenReturn(zone);
        assertThrows(expected, () -> LocalDateTimeConversions.toSqlDate(from, mockConverter), "Year " + year + " is out of java.sql.Date range");
    }
}
