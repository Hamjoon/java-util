package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.time.format.DateTimeException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DateConversions_toString_Test {

    /**
     * Mocked {@link Converter} used in all test cases.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Mocked {@link ConverterOptions} returned by {@code mockConverter.getOptions()}.
     */
    @Mock
    private ConverterOptions mockOptions;

    /**
     * Auto‑closeable for Mockito annotations – opened in {@code @BeforeEach}.
     */
    private AutoCloseable mocks;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise @Mock fields.
        mocks = MockitoAnnotations.openMocks(this);
        // Default behaviour: the converter returns the mocked options.
        when(mockConverter.getOptions()).thenReturn(mockOptions);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Release Mockito resources.
        if (mocks != null) {
            mocks.close();
        }
    }

    /**
     * Configures the shared {@code mockConverter} to return the supplied {@code zone}.
     */
    private void configureConverterZone(ZoneId zone) {
        when(mockOptions.getZoneId()).thenReturn(zone);
    }

    private static Stream<Arguments> validDateWithZoneOffsetProvider() {
        return Stream.of(Arguments.of("Epoch converted using America/New_York (UTC‑05:00)", new Date(0L), ZoneId.of("America/New_York"), "1969-12-31T19:00:00.000-05:00"));
    }

    private static Stream<Arguments> validDateUTCProvider() {
        return Stream.of(Arguments.of("Epoch converted using UTC – expects literal Z", new Date(0L), ZoneOffset.UTC, "1970-01-01T00:00:00.000Z"));
    }

    private static Stream<Arguments> dateWithNonZeroMillisProvider() {
        return Stream.of(Arguments.of("Date with 1234 ms (i.e. .234) using UTC", // 2021‑06‑01T02:30:01.234Z
        new Date(1622547801234L), ZoneOffset.UTC, "2021-06-01T02:30:01.234Z"));
    }

    private static Stream<Arguments> dateBeforeEpochProvider() {
        return Stream.of(Arguments.of("One day before the Unix epoch using UTC", // 1969‑12‑31T00:00:00Z
        new Date(-86_400_000L), ZoneOffset.UTC, "1969-12-31T00:00:00.000Z"));
    }

    /**
     * Verify that {@code toString} formats a Date at the epoch (0 ms) correctly
     * when the converter's zone is UTC.
     */
    @Test
    void toString_formatsEpochInUtc() {
        // GIVEN
        // 1970‑01‑01T00:00:00Z
        Date epoch = new Date(0L);
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
        // WHEN
        String result = DateConversions.toString(epoch, mockConverter);
        // THEN
        assertEquals("1970-01-01T00:00:00.000+00:00", result, "The formatted string should contain milliseconds and a UTC offset");
    }

    /**
     * Verify that a non‑UTC zone is honoured and the offset is reflected in the output.
     */
    @Test
    void toString_respectsConverterZoneId() {
        // GIVEN
        // 2023‑03‑15 12:34:56.789 in the system default zone (irrelevant – we force the zone)
        // corresponds to 2023‑03‑15T12:34:56.789Z
        long millis = 1678880096789L;
        Date date = new Date(millis);
        // Use a zone with a +02:00 offset (e.g., Europe/Paris in standard time)
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("Europe/Paris"));
        // WHEN
        String result = DateConversions.toString(date, mockConverter);
        // THEN
        // The same instant expressed in Europe/Paris (UTC+01:00 in March, but DST may apply;
        // we rely on the formatter to include the actual offset at that instant).
        // For 2023‑03‑15, Europe/Paris is UTC+01:00 (standard time), so expected offset +01:00.
        assertEquals("2023-03-15T13:34:56.789+01:00", result, "The formatted string should reflect the converter's zone offset");
    }

    /**
     * Verify that milliseconds are optional in the formatter – when the Date has zero
     * milliseconds, the formatter still prints “.000”.
     */
    @Test
    void toString_alwaysShowsMilliseconds() {
        // GIVEN
        // 1970‑01‑01T00:00:01.000Z
        Date date = new Date(1_000L);
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
        // WHEN
        String result = DateConversions.toString(date, mockConverter);
        // THEN
        assertTrue(result.endsWith(".000+00:00"), "Even when milliseconds are zero, the pattern should emit .000");
    }

    /**
     * Group: toString_validDateWithZoneOffset
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validDateWithZoneOffsetProvider")
    void toString_validDateWithZoneOffset(String description, Date from, ZoneId zone, String expected) {
        // GIVEN
        configureConverterZone(zone);
        // WHEN
        String result = DateConversions.toString(from, mockConverter);
        // THEN
        assertEquals(expected, result, description);
    }

    /**
     * Group: toString_validDateUTC
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validDateUTCProvider")
    void toString_validDateUTC(String description, Date from, ZoneId zone, String expected) {
        // GIVEN
        configureConverterZone(zone);
        // WHEN
        String result = DateConversions.toString(from, mockConverter);
        // THEN
        assertEquals(expected, result, description);
    }

    /**
     * Group: toString_dateWithNonZeroMilliseconds
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("dateWithNonZeroMillisProvider")
    void toString_dateWithNonZeroMilliseconds(String description, Date from, ZoneId zone, String expected) {
        // GIVEN
        configureConverterZone(zone);
        // WHEN
        String result = DateConversions.toString(from, mockConverter);
        // THEN
        assertEquals(expected, result, description);
    }

    /**
     * Group: toString_dateBeforeEpoch
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("dateBeforeEpochProvider")
    void toString_dateBeforeEpoch(String description, Date from, ZoneId zone, String expected) {
        // GIVEN
        configureConverterZone(zone);
        // WHEN
        String result = DateConversions.toString(from, mockConverter);
        // THEN
        assertEquals(expected, result, description);
    }

    /**
     * Group: toString_nullInput
     */
    @Test
    void toString_nullInput() {
        // GIVEN
        configureConverterZone(ZoneOffset.UTC);
        Date nullDate = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> DateConversions.toString(nullDate, mockConverter), "Passing a null `from` argument should throw NullPointerException");
    }

    /**
     * Group: toString_invalidType
     */
    @Test
    void toString_invalidType() {
        // GIVEN
        configureConverterZone(ZoneOffset.UTC);
        Object notADate = "2021-01-01";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> DateConversions.toString(notADate, mockConverter), "Providing a non‑Date object should cause a ClassCastException");
    }

    /**
     * Group: toString_nullConverter
     */
    @Test
    void toString_nullConverter() {
        // GIVEN
        Date date = new Date(0L);
        Converter nullConverter = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> DateConversions.toString(date, nullConverter), "Passing a null `converter` should result in NullPointerException");
    }

    /**
     * Group: toString_converterWithNullOptions
     */
    @Test
    void toString_converterWithNullOptions() {
        // GIVEN
        Date date = new Date(0L);
        // options are null
        when(mockConverter.getOptions()).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> DateConversions.toString(date, mockConverter), "Null options should cause NullPointerException when accessed");
    }

    /**
     * Group: toString_converterWithNullZoneId
     */
    @Test
    void toString_converterWithNullZoneId() {
        // GIVEN
        Date date = new Date(0L);
        // zoneId is null
        when(mockOptions.getZoneId()).thenReturn(null);
        // WHEN / THEN
        assertThrows(DateTimeException.class, () -> DateConversions.toString(date, mockConverter), "Null ZoneId should cause DateTimeException");
    }
}
