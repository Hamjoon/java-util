package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Calendar;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_create_Test {

    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    private AutoCloseable mocks;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise @Mock fields.
        mocks = MockitoAnnotations.openMocks(this);
        // Default stubbing – can be overridden inside individual tests.
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getTimeZone()).thenReturn(TimeZone.getTimeZone("UTC"));
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Close the mock context to avoid memory leaks.
        if (mocks != null) {
            mocks.close();
        }
    }

    private static Stream<Arguments> provideNormalCases() {
        return Stream.of(// Zero epoch with UTC
        Arguments.of(0L, "UTC"), // Known timestamp with a Pacific Time zone
        Arguments.of(1_622_548_800_000L, "America/Los_Angeles"));
    }

    private static Stream<Arguments> provideBoundaryCases() {
        return Stream.of(// Just before Unix epoch
        Arguments.of(-1L), // Far future
        Arguments.of(Long.MAX_VALUE), // Far past
        Arguments.of(Long.MIN_VALUE));
    }

    @Test
    void create_ShouldReturnCalendarWithGivenEpochAndTimeZone() {
        // GIVEN
        // 2021‑06‑01T00:00:00Z
        long epochMilli = 1_622_548_800_000L;
        // WHEN
        Calendar result = CalendarConversions.create(epochMilli, mockConverter);
        // THEN
        assertNotNull(result, "Calendar should not be null");
        assertEquals(epochMilli, result.getTimeInMillis(), "Calendar should hold the exact epoch milli supplied");
        assertEquals(TimeZone.getTimeZone("UTC"), result.getTimeZone(), "Calendar should use the TimeZone from the Converter options");
    }

    /* --------------------------------------------------------------------- *
     *  Normal behaviour – parameterised tests for typical inputs.
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] epoch={0}, tz={1}")
    @MethodSource("provideNormalCases")
    void create_NormalBehavior_ReturnsCalendarWithCorrectValues(long epochMilli, String tzId) {
        // Arrange – override the default TimeZone stub for this iteration.
        TimeZone tz = TimeZone.getTimeZone(tzId);
        when(mockOptions.getTimeZone()).thenReturn(tz);
        // Act
        Calendar cal = CalendarConversions.create(epochMilli, mockConverter);
        // Assert
        assertNotNull(cal, "Calendar must not be null");
        assertEquals(epochMilli, cal.getTimeInMillis(), "Calendar should contain the supplied epoch milliseconds");
        assertEquals(tz, cal.getTimeZone(), "Calendar should use the TimeZone supplied by the Converter");
    }

    /* --------------------------------------------------------------------- *
     *  Boundary conditions – very small / very large epoch values.
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] epoch={0}")
    @MethodSource("provideBoundaryCases")
    void create_BoundaryValues_ReturnsCalendar(long epochMilli) {
        // Use UTC for all boundary tests.
        when(mockOptions.getTimeZone()).thenReturn(TimeZone.getTimeZone("UTC"));
        Calendar cal = CalendarConversions.create(epochMilli, mockConverter);
        assertNotNull(cal, "Calendar must not be null for boundary value");
        assertEquals(epochMilli, cal.getTimeInMillis(), "Calendar should store the exact boundary epoch value");
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone(), "Calendar should retain the UTC TimeZone");
    }

    /* --------------------------------------------------------------------- *
     *  Exception scenarios – verify NullPointerException is thrown when
     *  required objects are missing.
     * --------------------------------------------------------------------- */
    @Test
    void create_NullConverter_ThrowsNullPointerException() {
        // GIVEN
        long epochMilli = 0L;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> CalendarConversions.create(epochMilli, null), "Passing a null Converter should throw NullPointerException");
    }

    @Test
    void create_ConverterReturnsNullOptions_ThrowsNullPointerException() {
        // GIVEN
        long epochMilli = 0L;
        // options == null
        when(mockConverter.getOptions()).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> CalendarConversions.create(epochMilli, mockConverter), "Null options from Converter should cause NullPointerException");
    }

    @Test
    void create_OptionsReturnNullTimeZone_ThrowsNullPointerException() {
        // GIVEN
        long epochMilli = 0L;
        // timezone == null
        when(mockOptions.getTimeZone()).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> CalendarConversions.create(epochMilli, mockConverter), "Null TimeZone from options should cause NullPointerException");
    }
}
