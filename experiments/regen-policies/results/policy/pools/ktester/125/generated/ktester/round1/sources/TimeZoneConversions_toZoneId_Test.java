package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Map;
import java.util.TimeZone;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.DateTimeException;
import java.time.ZoneOffset;
import java.util.SimpleTimeZone;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TimeZoneConversions_toZoneId_Test {

    /**
     * A shared {@link Converter} instance used for all tests.
     */
    private static Converter converter;

    /**
     * Mocked {@link ConverterOptions} used to construct the {@link Converter}.
     */
    @Mock
    private static ConverterOptions mockOptions;

    /**
     * A mock {@link Converter} used for the few cases where a non‑null converter is supplied.
     */
    @Mock
    private Converter mockConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Stub the options to return an empty map for overrides – this matches the
        // real constructor contract without pulling in any external state.
        when(mockOptions.getConverterOverrides()).thenReturn(Collections.emptyMap());
        // Create the Converter that will be passed to the method under test.
        converter = new Converter(mockOptions);
    }

    @BeforeEach
    void resetMocks() {
        // Ensure the mockConverter has no prior interactions.
        reset(mockConverter);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static references (good practice for large test suites).
        converter = null;
    }

    /**
     * Supplies all successful conversion cases described in the JSON test matrix.
     */
    private static Stream<Arguments> validZoneIdProvider() {
        return Stream.of(// Region ID
        Arguments.of(TimeZone.getTimeZone("America/New_York"), ZoneId.of("America/New_York"), false, "Valid region ID should map to the same ZoneId."), // GMT+02:00  → offset +02:00
        Arguments.of(TimeZone.getTimeZone("GMT+02:00"), ZoneOffset.ofHours(2), false, "GMT+02:00 should become ZoneOffset +02:00."), // UTC with a real (non‑null) converter – the converter is ignored.
        Arguments.of(TimeZone.getTimeZone("UTC"), ZoneOffset.UTC, true, "UTC should return ZoneOffset.UTC even when a non‑null Converter is supplied."), // GMT-05:00 → offset -05:00
        Arguments.of(TimeZone.getTimeZone("GMT-05:00"), ZoneOffset.ofHours(-5), false, "Negative GMT offset should become ZoneOffset -05:00."), // GMT+05:30 → offset +05:30
        Arguments.of(TimeZone.getTimeZone("GMT+05:30"), ZoneOffset.ofHoursMinutes(5, 30), false, "Half‑hour offset should preserve minutes (+05:30)."), // GMT (zero offset) → ZoneOffset.UTC (ID "Z")
        Arguments.of(TimeZone.getTimeZone("GMT"), ZoneOffset.UTC, false, "GMT (zero offset) should be the UTC ZoneId (\"Z\")."), // Legacy short ID "EST" → fixed -05:00
        Arguments.of(TimeZone.getTimeZone("EST"), ZoneOffset.ofHours(-5), false, "Legacy short ID EST maps to fixed offset -05:00."), // Unknown ID falls back to GMT → UTC
        Arguments.of(TimeZone.getTimeZone("Foo/Bar"), ZoneOffset.UTC, false, "Unknown ID falls back to GMT, resulting in UTC ZoneId."), // SimpleTimeZone with -3h raw offset
        Arguments.of(new SimpleTimeZone(-3 * 60 * 60 * 1000, "Custom"), ZoneOffset.ofHours(-3), false, "SimpleTimeZone with -3h offset should become ZoneOffset -03:00."));
    }

    @Test
    void toZoneId_WithValidTimeZone_ReturnsMatchingZoneId() {
        // GIVEN
        TimeZone tz = TimeZone.getTimeZone("America/New_York");
        ZoneId expected = tz.toZoneId();
        // WHEN
        ZoneId actual = TimeZoneConversions.toZoneId(tz, converter);
        // THEN
        assertEquals(expected, actual, "The ZoneId returned should match the TimeZone's ZoneId");
        // Verify that the converter is not interacted with.
        verifyNoInteractions(converter);
    }

    @Test
    void toZoneId_WithUtcTimeZone_ReturnsUtcZoneId() {
        // GIVEN
        TimeZone tz = TimeZone.getTimeZone("UTC");
        ZoneId expected = tz.toZoneId();
        // WHEN
        ZoneId actual = TimeZoneConversions.toZoneId(tz, converter);
        // THEN
        assertEquals(expected, actual, "UTC TimeZone should produce the UTC ZoneId");
        verifyNoInteractions(converter);
    }

    @Test
    void toZoneId_WithNullFrom_ThrowsNullPointerException() {
        // GIVEN
        Object nullObj = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> TimeZoneConversions.toZoneId(nullObj, null), "Passing null as the source must throw NPE.");
        // No interaction with any converter (null in this case) – nothing to verify.
    }

    @Test
    void toZoneId_WithInvalidType_ThrowsClassCastException() {
        // GIVEN
        Object notATimeZone = "I am not a TimeZone";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> TimeZoneConversions.toZoneId(notATimeZone, converter), "Calling toZoneId with a non‑TimeZone object should throw ClassCastException");
        verifyNoInteractions(converter);
    }

    /* --------------------------------------------------------------
     *  Parameterized tests for all *successful* conversion scenarios.
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validZoneIdProvider")
    void toZoneId_ValidConversions(Object from, ZoneId expected, boolean useMockConverter, String description) {
        // GIVEN
        Converter usedConverter = useMockConverter ? mockConverter : null;
        // WHEN
        ZoneId actual = TimeZoneConversions.toZoneId(from, usedConverter);
        // THEN
        assertEquals(expected, actual, description);
        // The method must never touch the converter, regardless of whether we passed a mock.
        verifyNoInteractions(usedConverter);
    }

    @Test
    void toZoneId_WithTooLargeOffset_ThrowsDateTimeException() {
        // A SimpleTimeZone with a raw offset of +20 hours exceeds the maximum ZoneOffset (+18:00).
        SimpleTimeZone tooBig = new SimpleTimeZone(20 * 60 * 60 * 1000, "TooBig");
        assertThrows(DateTimeException.class, () -> TimeZoneConversions.toZoneId(tooBig, null), "Offset beyond +18:00 must cause DateTimeException.");
    }
}
