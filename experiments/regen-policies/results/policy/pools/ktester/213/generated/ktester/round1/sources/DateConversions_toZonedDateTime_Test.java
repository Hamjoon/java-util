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
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DateConversions_toZonedDateTime_Test {

    /**
     * Mocked {@link Converter} that will be passed to the static method under test.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Mocked {@link ConverterOptions} returned by {@code mockConverter.getOptions()}.
     */
    @Mock
    private ConverterOptions mockOptions;

    /**
     * The zone that will be used by the mocked options.
     */
    private static final ZoneId TEST_ZONE = ZoneId.of("UTC");

    /**
     * The default zone used by the framework – can be overridden per test.
     */
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("UTC");

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the Mockito annotations for this test instance.
        MockitoAnnotations.openMocks(this);
        // Default stubbing: converter returns options, options returns the default zone.
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getZoneId()).thenReturn(DEFAULT_ZONE);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks after each test to avoid cross‑test interference.
        reset(mockConverter, mockOptions);
    }

    /**
     * Provides the data for {@link #toZonedDateTime_convertsDateUsingConverterZone}.
     */
    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// epoch 0 (1970‑01‑01T00:00:00Z) with UTC
        Arguments.of(0L, "UTC"), // specific timestamp with America/New_York zone
        Arguments.of(1_622_548_800_000L, "America/New_York"));
    }

    @Test
    void toZonedDateTime_convertsDateUsingConverterZone() {
        // GIVEN
        Date date = new Date(epochMilli);
        ZoneId zone = ZoneId.of(zoneId);
        // Override the default zone for this invocation.
        when(mockOptions.getZoneId()).thenReturn(zone);
        // WHEN
        ZonedDateTime result = DateConversions.toZonedDateTime(date, mockConverter);
        // THEN
        ZonedDateTime expected = Instant.ofEpochMilli(epochMilli).atZone(zone);
        assertEquals(expected, result, "The ZonedDateTime should be created from the Date's epoch millis and the Converter's zone");
    }

    @Test
    void toZonedDateTime_nullSource_throwsNullPointerException() {
        // GIVEN
        Object nullSource = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> DateConversions.toZonedDateTime(nullSource, mockConverter), "Passing a null source should result in a NullPointerException because the method casts to Date");
    }

    /* --------------------------------------------------------------
     *  Non‑Date input – expect ClassCastException.
     * -------------------------------------------------------------- */
    @Test
    void toZonedDateTime_nonDateInput_throwsClassCastException() {
        // GIVEN
        Integer notADate = Integer.valueOf(123);
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> DateConversions.toZonedDateTime(notADate, mockConverter), "Passing a non‑Date object should cause a ClassCastException due to the explicit cast.");
    }

    /* --------------------------------------------------------------
     *  Null converter – expect NullPointerException.
     * -------------------------------------------------------------- */
    @Test
    void toZonedDateTime_nullConverter_throwsNullPointerException() {
        // GIVEN
        Date date = new Date(0L);
        Converter nullConverter = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> DateConversions.toZonedDateTime(date, nullConverter), "A null Converter should cause a NullPointerException when the method accesses getOptions().");
    }

    /* --------------------------------------------------------------
     *  Null options returned from the converter – expect NPE.
     * -------------------------------------------------------------- */
    @Test
    void toZonedDateTime_nullOptions_throwsNullPointerException() {
        // GIVEN
        Date date = new Date(0L);
        // options are null
        when(mockConverter.getOptions()).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> DateConversions.toZonedDateTime(date, mockConverter), "If Converter.getOptions() returns null, a NullPointerException should be thrown.");
    }

    /* --------------------------------------------------------------
     *  Null zoneId inside options – expect NPE from atZone(null).
     * -------------------------------------------------------------- */
    @Test
    void toZonedDateTime_nullZoneId_throwsNullPointerException() {
        // GIVEN
        Date date = new Date(0L);
        // zoneId is null
        when(mockOptions.getZoneId()).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> DateConversions.toZonedDateTime(date, mockConverter), "When Options.getZoneId() returns null, atZone(null) should trigger a NullPointerException.");
    }

    /* --------------------------------------------------------------
     *  Epoch milli overflow – Instant.ofEpochMilli(Long.MAX_VALUE) throws DateTimeException.
     * -------------------------------------------------------------- */
    @Test
    void toZonedDateTime_epochMilliOverflow_throwsDateTimeException() {
        // GIVEN
        // value beyond Instant range
        Date overflowDate = new Date(Long.MAX_VALUE);
        // WHEN / THEN
        assertThrows(DateTimeException.class, () -> DateConversions.toZonedDateTime(overflowDate, mockConverter), "Instant.ofEpochMilli(Long.MAX_VALUE) exceeds the supported range and should throw DateTimeException.");
    }
}
