package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.ZoneId;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.Instant;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DateConversions_toString_Test {

    @Mock
    private Converter // the Converter used by the method under test
    converter;

    @Mock
    private ConverterOptions // options returned by converter.getOptions()
    options;

    @BeforeEach
    void setupBeforeEach() {
        // Wire the mock hierarchy: converter.getOptions() -> options
        when(converter.getOptions()).thenReturn(options);
    }

    @AfterEach
    void teardownAfterEach() {
        // Verify that no unexpected interactions occurred
        verifyNoMoreInteractions(converter, options);
    }

    private static Stream<Arguments> provideGroup1Cases() {
        return Stream.of(// UTC zone – preserve three‑digit millisecond fraction
        Arguments.of("2023-03-15T12:34:56.789Z", "UTC", "2023-03-15T12:34:56.789Z"), // Eastern Standard Time (UTC‑05:00) – leading zero millisecond
        Arguments.of("2023-03-15T12:34:56.001Z", "America/New_York", "2023-03-15T07:34:56.001-05:00"), // India Standard Time (UTC+05:30) – zero milliseconds
        Arguments.of("2023-03-15T12:34:56.000Z", "Asia/Kolkata", "2023-03-15T18:04:56.000+05:30"), // Cross‑year boundary with DST (+01:00)
        Arguments.of("2023-12-31T23:59:59.999Z", "Europe/Paris", "2024-01-01T00:59:59.999+01:00"));
    }

    private static Stream<Arguments> provideGroup2Cases() {
        return Stream.of(// Custom offset +00:45
        Arguments.of("2023-03-15T12:34:56.789Z", "+00:45", "2023-03-15T13:19:56.789+00:45"), // Maximum positive offset +14:00
        Arguments.of("2023-03-15T23:59:59.999Z", "Pacific/Kiritimati", "2023-03-16T13:59:59.999+14:00"), // Maximum negative offset -12:00
        Arguments.of("2023-01-01T00:00:00.000Z", "-12:00", "2022-12-31T12:00:00.000-12:00"), // London during standard time (UTC)
        Arguments.of("2023-03-15T12:34:56.123Z", "Europe/London", "2023-03-15T12:34:56.123Z"), // Pacific Daylight Time (UTC‑07:00) summer
        Arguments.of("2023-06-30T23:59:59.999Z", "America/Los_Angeles", "2023-06-30T16:59:59.999-07:00"), // Moscow +03:00 crossing year boundary, zero milliseconds
        Arguments.of("2023-12-31T23:59:59.000Z", "Europe/Moscow", "2024-01-01T02:59:59.000+03:00"), // Leap‑day with half‑second milliseconds, Tokyo (+09:00)
        Arguments.of("2020-02-29T12:00:00.500Z", "Asia/Tokyo", "2020-02-29T21:00:00.500+09:00"));
    }

    /**
     * Verify that a Date at epoch (0 ms) is rendered correctly when the zone is UTC.
     * Expected format: {@code 1970-01-01T00:00:00.000Z}
     */
    @Test
    void testToString_UtcZone() {
        // Arrange
        Date epoch = new Date(0L);
        when(options.getZoneId()).thenReturn(ZoneId.of("UTC"));
        // Act
        String result = DateConversions.toString(epoch, converter);
        // Assert
        assertEquals("1970-01-01T00:00:00.000Z", result);
        verify(options).getZoneId();
        verify(converter).getOptions();
    }

    /**
     * Verify that a Date at epoch (0 ms) is rendered correctly when the zone is
     * America/New_York (UTC‑05:00 at the epoch). Expected format:
     * {@code 1969-12-31T19:00:00.000-05:00}
     */
    @Test
    void testToString_NonUtcZone() {
        // Arrange
        Date epoch = new Date(0L);
        when(options.getZoneId()).thenReturn(ZoneId.of("America/New_York"));
        // Act
        String result = DateConversions.toString(epoch, converter);
        // Assert
        assertEquals("1969-12-31T19:00:00.000-05:00", result);
        verify(options).getZoneId();
        verify(converter).getOptions();
    }

    /**
     * Parameterised test covering the first group of JSON cases.
     *
     * Each argument consists of:
     *   1. An ISO‑8601 instant string (used to build the {@link Date} input)
     *   2. A zone‑id string (passed to the mocked {@link ConverterOptions})
     *   3. The expected formatted string (without surrounding quotes)
     */
    @ParameterizedTest(name = "[{index}] from={0} zone={1} => {2}")
    @MethodSource("provideGroup1Cases")
    void testToString_Group1(String instantIso, String zoneId, String expected) {
        // Arrange
        Date date = Date.from(Instant.parse(instantIso));
        when(options.getZoneId()).thenReturn(ZoneId.of(zoneId));
        // Act
        String result = DateConversions.toString(date, converter);
        // Assert
        assertEquals(expected, result, "Formatted date string does not match expected value");
        verify(options).getZoneId();
        verify(converter).getOptions();
    }

    @ParameterizedTest(name = "[{index}] from={0} zone={1} => {2}")
    @MethodSource("provideGroup2Cases")
    void testToString_Group2(String instantIso, String zoneId, String expected) {
        // Arrange
        Date date = Date.from(Instant.parse(instantIso));
        when(options.getZoneId()).thenReturn(ZoneId.of(zoneId));
        // Act
        String result = DateConversions.toString(date, converter);
        // Assert
        assertEquals(expected, result, "Formatted date string does not match expected value");
        verify(options).getZoneId();
        verify(converter).getOptions();
    }
}
