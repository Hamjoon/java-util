package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class InstantConversions_toZonedDateTime_Test {

    /**
     * The mock Converter that will be supplied to the method under test.
     */
    @Mock
    private Converter mockConverter;

    /**
     * The mock options returned from {@code Converter#getOptions()}.
     */
    @Mock
    private ConverterOptions mockOptions;

    /**
     * A fixed instant used for all test cases.
     */
    private static final Instant FIXED_INSTANT = Instant.parse("2023-03-15T12:34:56Z");

    /**
     * The zone that the mock options will report.
     */
    private static final ZoneId TEST_ZONE = ZoneId.of("America/New_York");

    /**
     * Default zone used for the majority of tests – can be overridden per‑case.
     */
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("America/New_York");

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mock hierarchy so that
        // mockConverter.getOptions().getZoneId() returns TEST_ZONE.
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getZoneId()).thenReturn(TEST_ZONE);
        // By default the converter returns an options object that yields DEFAULT_ZONE.
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getZoneId()).thenReturn(DEFAULT_ZONE);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks after each test to avoid cross‑test interference.
        reset(mockConverter, mockOptions);
        // Ensure a clean state for the next test.
        reset(mockConverter, mockOptions);
    }

    private static Stream<Arguments> validInstantProvider() {
        return Stream.of(// Simple conversion – New York is UTC‑5 (standard time)
        Arguments.of(Instant.parse("2023-01-15T10:15:30Z"), ZoneId.of("America/New_York"), ZonedDateTime.of(2023, 1, 15, 5, 15, 30, 0, ZoneId.of("America/New_York")), "Converts a proper Instant to ZonedDateTime using the zone from converter options."), // Epoch instant with UTC zone
        Arguments.of(Instant.EPOCH, ZoneId.of("UTC"), ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC")), "Edge case: Instant at epoch with UTC zone."), // Crossing date boundary – Tokyo (+9) turns 23:00 UTC on June 30 into 08:00 on July 1
        Arguments.of(Instant.parse("2023-06-30T23:00:00Z"), ZoneId.of("Asia/Tokyo"), ZonedDateTime.of(2023, 7, 1, 8, 0, 0, 0, ZoneId.of("Asia/Tokyo")), "Conversion crossing date boundary."), // Leap day – Paris (+1) should keep the same calendar day
        Arguments.of(Instant.parse("2020-02-29T12:00:00Z"), ZoneId.of("Europe/Paris"), ZonedDateTime.of(2020, 2, 29, 13, 0, 0, 0, ZoneId.of("Europe/Paris")), "Leap day conversion."), // Epoch with maximum positive offset (+14)
        Arguments.of(Instant.parse("1970-01-01T00:00:00Z"), ZoneId.of("Pacific/Kiritimati"), ZonedDateTime.of(1970, 1, 1, 14, 0, 0, 0, ZoneId.of("Pacific/Kiritimati")), "Earliest representable instant with +14:00 offset."), // Pre‑epoch instant with negative offset (Etc/GMT+12 = -12)
        Arguments.of(Instant.parse("1969-12-31T23:59:59Z"), ZoneId.of("Etc/GMT+12"), ZonedDateTime.of(1969, 12, 31, 11, 59, 59, 0, ZoneId.of("Etc/GMT+12")), "Instant just before epoch with -12:00 offset."), // Near 32‑bit overflow – Los Angeles (UTC‑8)
        Arguments.of(Instant.parse("2038-01-19T03:14:07Z"), ZoneId.of("America/Los_Angeles"), ZonedDateTime.of(2038, 1, 18, 19, 14, 7, 0, ZoneId.of("America/Los_Angeles")), "Near 32‑bit Unix time overflow."));
    }

    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// Null source – casting succeeds (null) but atZone throws NPE
        Arguments.of(null, DEFAULT_ZONE, NullPointerException.class, "Passing a null Instant should result in a NullPointerException."), // Wrong type – String leads to ClassCastException on cast
        Arguments.of("2023-01-01T00:00:00", DEFAULT_ZONE, ClassCastException.class, "Passing a String should cause a ClassCastException."), // Wrong type – java.util.Date (not an Instant) leads to ClassCastException
        Arguments.of(Date.from(Instant.now()), DEFAULT_ZONE, ClassCastException.class, "Passing a java.util.Date should cause a ClassCastException."), // Null converter argument – NPE when calling getOptions()
        Arguments.of(Instant.now(), DEFAULT_ZONE, NullPointerException.class, "Null converter argument should cause NullPointerException."), // Converter with null Options – NPE when invoking getZoneId()
        Arguments.of(Instant.now(), // we will make mockConverter.getOptions() return null
        null, NullPointerException.class, "Converter with null Options should cause NullPointerException."), // Options with null ZoneId – NPE when atZone(null) is called
        Arguments.of(Instant.parse("2023-01-15T10:15:30Z"), // zone is irrelevant because we will stub getZoneId() -> null
        ZoneId.of("UTC"), NullPointerException.class, "Options with null ZoneId should cause NullPointerException."));
    }

    @Test
    void toZonedDateTime_convertsInstantUsingConverterZone() {
        // GIVEN
        // the method expects an Object that is an Instant
        Object source = FIXED_INSTANT;
        // WHEN
        ZonedDateTime result = InstantConversions.toZonedDateTime(source, mockConverter);
        // THEN
        assertNotNull(result, "Result must not be null");
        assertEquals(TEST_ZONE, result.getZone(), "The zone must be the one supplied by ConverterOptions");
        // The instant part should be identical to the original instant.
        assertEquals(FIXED_INSTANT, result.toInstant(), "The instant part of the ZonedDateTime must match the original Instant");
    }

    @Test
    void toZonedDateTime_withDifferentZone_returnsCorrespondingZonedDateTime() {
        // GIVEN
        ZoneId anotherZone = ZoneId.of("Europe/Paris");
        when(mockOptions.getZoneId()).thenReturn(anotherZone);
        Object source = FIXED_INSTANT;
        // WHEN
        ZonedDateTime result = InstantConversions.toZonedDateTime(source, mockConverter);
        // THEN
        assertEquals(anotherZone, result.getZone(), "The ZonedDateTime should reflect the zone provided by the mocked options");
        assertEquals(FIXED_INSTANT, result.toInstant(), "The underlying instant must stay unchanged regardless of the zone");
    }

    @Test
    void toZonedDateTime_nullSourceReturnsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        ZonedDateTime result = InstantConversions.toZonedDateTime(source, mockConverter);
        // THEN
        // The implementation casts the object to Instant, which will throw NPE.
        // We verify that a NullPointerException is thrown rather than returning a value.
        assertThrows(NullPointerException.class, () -> InstantConversions.toZonedDateTime(source, mockConverter));
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("validInstantProvider")
    void toZonedDateTime_validInstantConversions(Instant instant, ZoneId zone, ZonedDateTime expected, String description) {
        // Arrange – configure the mock to return the zone required for this case
        when(mockOptions.getZoneId()).thenReturn(zone);
        // the method expects an Object that is an Instant
        Object source = instant;
        // Act
        ZonedDateTime result = InstantConversions.toZonedDateTime(source, mockConverter);
        // Assert
        assertNotNull(result, "Result must not be null");
        assertEquals(expected, result, "ZonedDateTime does not match expected value");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("exceptionProvider")
    void toZonedDateTime_exceptionScenarios(Object source, ZoneId zoneToReturn, Class<? extends Throwable> expectedException, String description) {
        // Arrange
        // default mock
        Converter converterUnderTest = mockConverter;
        // Special handling for cases that require a different mock configuration
        if (expectedException == NullPointerException.class && source == null) {
            // Null source – keep the default mock (zone not used)
        } else if (expectedException == NullPointerException.class && zoneToReturn == null) {
            // Converter with null Options
            when(mockConverter.getOptions()).thenReturn(null);
        } else if (expectedException == NullPointerException.class && description.contains("null ZoneId")) {
            // Options with null ZoneId
            when(mockOptions.getZoneId()).thenReturn(null);
        } else if (expectedException == NullPointerException.class && source != null && zoneToReturn != null && description.contains("Null converter")) {
            // Null converter argument – we will pass null instead of the mock
            converterUnderTest = null;
        } else {
            // Normal case – configure the zone returned by Options
            when(mockOptions.getZoneId()).thenReturn(zoneToReturn);
        }
        // Act & Assert
        assertThrows(expectedException, () -> InstantConversions.toZonedDateTime(source, converterUnderTest), description);
    }

    @Test
    void toZonedDateTime_nullConverterArgument() {
        // GIVEN
        Object source = Instant.now();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> InstantConversions.toZonedDateTime(source, null), "Passing null as the Converter must throw NullPointerException");
    }

    @Test
    void toZonedDateTime_converterWithNullOptions() {
        // GIVEN
        when(mockConverter.getOptions()).thenReturn(null);
        Object source = Instant.now();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> InstantConversions.toZonedDateTime(source, mockConverter), "Converter with null Options must throw NullPointerException");
    }

    @Test
    void toZonedDateTime_optionsWithNullZoneId() {
        // GIVEN
        when(mockOptions.getZoneId()).thenReturn(null);
        Object source = Instant.parse("2023-01-15T10:15:30Z");
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> InstantConversions.toZonedDateTime(source, mockConverter), "Options with null ZoneId must cause NullPointerException");
    }
}
