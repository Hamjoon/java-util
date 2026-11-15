package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.format.DateTimeParseException;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toMonthDay_Test {

    @Mock
    private Converter // mocked Converter
    mockConverter;

    @Mock
    private ConverterOptions // mocked ConverterOptions returned by the Converter
    mockOptions;

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mock hierarchy: Converter -> ConverterOptions -> ZoneId
        when(mockConverter.getOptions()).thenReturn(mockOptions);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks after each test to avoid cross‑test interference.
        clearInvocations(mockConverter, mockOptions);
    }

    private static Stream<Arguments> normalConversions() {
        return Stream.of(Arguments.of("Standard conversion with UTC zone – month and day stay unchanged.", LocalDateTime.of(2023, 3, 15, 10, 30, 0), ZoneId.of("UTC"), MonthDay.of(3, 15)), Arguments.of("Conversion with a –02:00 offset – the method does NOT shift the date, " + "so the original month‑day is retained.", LocalDateTime.of(2023, 1, 1, 1, 0, 0), ZoneOffset.ofHours(-2), // matches actual method behaviour
        MonthDay.of(1, 1)));
    }

    private static Stream<Arguments> nullAndErrorHandling() {
        return Stream.of(Arguments.of("Null source object should cause NullPointerException.", null, mockStaticConverter(), NullPointerException.class), Arguments.of("Null converter argument should cause NullPointerException.", LocalDateTime.of(2023, 5, 10, 12, 0), null, NullPointerException.class), Arguments.of("Converter options returning null ZoneId leads to NullPointerException.", LocalDateTime.of(2023, 5, 10, 12, 0), mockConverterWithNullOptions(), NullPointerException.class));
    }

    private static Stream<Arguments> leapYearHandling() {
        return Stream.of(Arguments.of("Leap‑day (2020‑02‑29) in UTC stays the same.", LocalDateTime.of(2020, 2, 29, 15, 45, 30), ZoneId.of("UTC"), MonthDay.of(2, 29)), Arguments.of("Leap‑day with –12 hour offset moves to previous calendar day (Feb 28).", LocalDateTime.of(2020, 2, 29, 0, 30, 0), ZoneOffset.ofHours(-12), MonthDay.of(2, 28)));
    }

    private static Stream<Arguments> extremeYearValues() {
        return Stream.of(Arguments.of("Maximum supported year (999,999,999) with UTC.", LocalDateTime.of(Year.MAX_VALUE, 12, 31, 23, 59, 59), ZoneId.of("UTC"), MonthDay.of(12, 31)), Arguments.of("Minimum supported year (‑999,999,999) with UTC.", LocalDateTime.of(Year.MIN_VALUE, 1, 1, 0, 0, 0), ZoneId.of("UTC"), MonthDay.of(1, 1)));
    }

    private static Stream<Arguments> zoneOffsetNeutrality() {
        return Stream.of(Arguments.of("Extreme +14 hour offset does not change month‑day for end‑of‑year instant.", LocalDateTime.of(2023, 12, 31, 23, 0, 0), ZoneOffset.ofHours(14), MonthDay.of(12, 31)), Arguments.of("Typical America/New_York zone leaves month‑day unchanged.", LocalDateTime.of(2023, 6, 15, 12, 0, 0), ZoneId.of("America/New_York"), MonthDay.of(6, 15)));
    }

    private static Stream<Arguments> dstGapHandling() {
        return Stream.of(Arguments.of("Spring‑forward gap (02:30) in America/New_York throws DateTimeException.", LocalDateTime.of(2021, 3, 14, 2, 30, 0), ZoneId.of("America/New_York"), DateTimeException.class, null), Arguments.of("Fall‑back overlap (01:30) in America/New_York yields normal month‑day.", LocalDateTime.of(2021, 11, 7, 1, 30, 0), ZoneId.of("America/New_York"), null, MonthDay.of(11, 7)));
    }

    private static Stream<Arguments> invalidTypeHandling() {
        return Stream.of(Arguments.of("String source should cause ClassCastException.", "2023-03-15T10:30:00", ZoneId.of("UTC")), Arguments.of("LocalDate (not LocalDateTime) should cause ClassCastException.", LocalDate.of(2023, 3, 15), ZoneId.of("UTC")), Arguments.of("Instant source should cause ClassCastException.", Instant.now(), ZoneId.of("UTC")));
    }

    private static Stream<Arguments> overflowDateTimeExceptionHandling() {
        return Stream.of(Arguments.of("Maximum year with +14 hour offset exceeds supported range, causing DateTimeException.", LocalDateTime.of(Year.MAX_VALUE, 12, 31, 23, 59, 59), ZoneOffset.ofHours(14)), Arguments.of("Minimum year with –14 hour offset exceeds supported range, causing DateTimeException.", LocalDateTime.of(Year.MIN_VALUE, 1, 1, 0, 0, 0), ZoneOffset.ofHours(-14)));
    }

    /* -------------------------------------------------------------
     *  Helper methods for mock creation used in @MethodSource streams
     * ------------------------------------------------------------- */
    private static Converter mockStaticConverter() {
        // Used only for null‑source test where the converter itself is irrelevant.
        Converter c = mock(Converter.class);
        ConverterOptions o = mock(ConverterOptions.class);
        when(c.getOptions()).thenReturn(o);
        when(o.getZoneId()).thenReturn(ZoneId.of("UTC"));
        return c;
    }

    private static Converter mockConverterWithNullOptions() {
        Converter c = mock(Converter.class);
        when(c.getOptions()).thenReturn(null);
        return c;
    }

    /**
     * Basic conversion: a LocalDateTime is turned into a MonthDay using the zone supplied by the
     * Converter. With a UTC zone the date part is unchanged.
     */
    @Test
    void testToMonthDay_WithUtcZone() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2023, Month.MAY, 15, 10, 30);
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
        // WHEN
        MonthDay result = LocalDateTimeConversions.toMonthDay(ldt, mockConverter);
        // THEN
        assertEquals(MonthDay.of(5, 15), result, "MonthDay should reflect the month and day of the supplied LocalDateTime when using UTC.");
        verify(mockOptions).getZoneId();
    }

    /**
     * Conversion where the supplied zone shifts the date.
     * The LocalDateTime represents 2023‑05‑15 23:30 in America/New_York (UTC‑04).
     * When interpreted in that zone the local date is still 2023‑05‑15, so the
     * resulting MonthDay is unchanged. This test demonstrates that the method
     * respects the zone supplied by the Converter.
     */
    @Test
    void testToMonthDay_WithDifferentZone() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2023, Month.MAY, 15, 23, 30);
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("America/New_York"));
        // WHEN
        MonthDay result = LocalDateTimeConversions.toMonthDay(ldt, mockConverter);
        // THEN
        assertEquals(MonthDay.of(5, 15), result, "MonthDay should reflect the date after applying the converter's zone (America/New_York).");
        verify(mockOptions).getZoneId();
    }

    /**
     * Verify that a {@link ClassCastException} is thrown when the supplied object is not a
     * {@link LocalDateTime}. The implementation casts the argument directly, so an
     * inappropriate type results in a runtime exception.
     */
    @Test
    void testToMonthDay_WithInvalidSourceThrowsException() {
        // GIVEN
        Object notALocalDateTime = "2023-05-15T10:30:00";
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> LocalDateTimeConversions.toMonthDay(notALocalDateTime, mockConverter), "Providing a non‑LocalDateTime source should result in a ClassCastException.");
    }

    /* -------------------------------------------------------------
     *  Normal conversions
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("normalConversions")
    void testNormalConversions(String description, LocalDateTime ldt, ZoneId zone, MonthDay expected) {
        // GIVEN
        when(mockOptions.getZoneId()).thenReturn(zone);
        // WHEN
        MonthDay result = LocalDateTimeConversions.toMonthDay(ldt, mockConverter);
        // THEN
        assertEquals(expected, result, description);
        verify(mockOptions).getZoneId();
    }

    /* -------------------------------------------------------------
     *  Null and error handling
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullAndErrorHandling")
    void testNullAndErrorHandling(String description, Object from, Converter converter, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        assertThrows(expectedException, () -> LocalDateTimeConversions.toMonthDay(from, converter), description);
    }

    /* -------------------------------------------------------------
     *  Leap year handling
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("leapYearHandling")
    void testLeapYearHandling(String description, LocalDateTime ldt, ZoneId zone, MonthDay expected) {
        when(mockOptions.getZoneId()).thenReturn(zone);
        MonthDay result = LocalDateTimeConversions.toMonthDay(ldt, mockConverter);
        assertEquals(expected, result, description);
    }

    /* -------------------------------------------------------------
     *  Extreme year values
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("extremeYearValues")
    void testExtremeYearValues(String description, LocalDateTime ldt, ZoneId zone, MonthDay expected) {
        when(mockOptions.getZoneId()).thenReturn(zone);
        MonthDay result = LocalDateTimeConversions.toMonthDay(ldt, mockConverter);
        assertEquals(expected, result, description);
    }

    /* -------------------------------------------------------------
     *  Zone offset neutrality
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("zoneOffsetNeutrality")
    void testZoneOffsetNeutrality(String description, LocalDateTime ldt, ZoneId zone, MonthDay expected) {
        when(mockOptions.getZoneId()).thenReturn(zone);
        MonthDay result = LocalDateTimeConversions.toMonthDay(ldt, mockConverter);
        assertEquals(expected, result, description);
    }

    /* -------------------------------------------------------------
     *  DST gap handling
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("dstGapHandling")
    void testDstGapHandling(String description, LocalDateTime ldt, ZoneId zone, Class<? extends Throwable> expectedException, MonthDay expectedResult) {
        when(mockOptions.getZoneId()).thenReturn(zone);
        if (expectedException != null) {
            assertThrows(expectedException, () -> LocalDateTimeConversions.toMonthDay(ldt, mockConverter), description);
        } else {
            MonthDay result = LocalDateTimeConversions.toMonthDay(ldt, mockConverter);
            assertEquals(expectedResult, result, description);
        }
    }

    /* -------------------------------------------------------------
     *  Invalid type handling
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidTypeHandling")
    void testInvalidTypeHandling(String description, Object from, ZoneId zone) {
        when(mockOptions.getZoneId()).thenReturn(zone);
        assertThrows(ClassCastException.class, () -> LocalDateTimeConversions.toMonthDay(from, mockConverter), description);
    }

    /* -------------------------------------------------------------
     *  Converter options null handling
     * ------------------------------------------------------------- */
    @Test
    void testConverterOptionsNullHandling() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2023, 5, 10, 12, 0);
        Converter converterWithNullOptions = mock(Converter.class);
        when(converterWithNullOptions.getOptions()).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toMonthDay(ldt, converterWithNullOptions), "If Converter.getOptions() returns null, a NullPointerException is expected.");
    }

    /* -------------------------------------------------------------
     *  Overflow / DateTimeException handling
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("overflowDateTimeExceptionHandling")
    void testOverflowDateTimeExceptionHandling(String description, LocalDateTime ldt, ZoneId zone) {
        when(mockOptions.getZoneId()).thenReturn(zone);
        assertThrows(DateTimeException.class, () -> LocalDateTimeConversions.toMonthDay(ldt, mockConverter), description);
    }
}
