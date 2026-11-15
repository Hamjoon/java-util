package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.anyLong;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toOffsetDateTime_Test {

    @Mock
    private Converter converter;

    @Mock
    private ConverterOptions options;

    @Mock
    private TimeZone timeZone;

    private AutoCloseable mocks;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the Mockito annotations (needed for the @Mock fields when not using field injection)
        mocks = MockitoAnnotations.openMocks(this);
        // Default stubbing – individual tests can override as needed
        when(converter.getOptions()).thenReturn(options);
        when(options.getTimeZone()).thenReturn(timeZone);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Close the Mockito resources
        mocks.close();
    }

    private static Stream<Arguments> normalConversionProvider() {
        return Stream.of(// Positive offset (+01:00)
        Arguments.of(LocalDateTime.of(2023, 5, 10, 12, 30, 45), 3600L, OffsetDateTime.of(2023, 5, 10, 12, 30, 45, 0, ZoneOffset.ofTotalSeconds(3600)), "Positive offset +01:00"), // Negative offset (‑05:00)
        Arguments.of(LocalDateTime.of(2021, 12, 31, 23, 59, 59), -18000L, OffsetDateTime.of(2021, 12, 31, 23, 59, 59, 0, ZoneOffset.ofTotalSeconds(-18000)), "Negative offset -05:00"), // Zero offset (UTC)
        Arguments.of(LocalDateTime.of(2020, 1, 1, 0, 0, 0), 0L, OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), "Zero offset (UTC)"), // Maximum positive offset (+18:00)
        Arguments.of(LocalDateTime.of(2022, 6, 15, 8, 0, 0), 64800L, OffsetDateTime.of(2022, 6, 15, 8, 0, 0, 0, ZoneOffset.ofTotalSeconds(64800)), "Maximum positive offset +18:00"), // Maximum negative offset (‑18:00)
        Arguments.of(LocalDateTime.of(2022, 12, 31, 23, 59, 59), -64800L, OffsetDateTime.of(2022, 12, 31, 23, 59, 59, 0, ZoneOffset.ofTotalSeconds(-64800)), "Maximum negative offset -18:00"), // Non‑standard offset (+05:30)
        Arguments.of(LocalDateTime.of(2021, 3, 14, 12, 0, 0), 19800L, OffsetDateTime.of(2021, 3, 14, 12, 0, 0, 0, ZoneOffset.ofTotalSeconds(19800)), "Non‑standard offset +05:30"), // Odd‑minute offset (+02:45)
        Arguments.of(LocalDateTime.of(2020, 11, 5, 6, 15, 30), 9900L, OffsetDateTime.of(2020, 11, 5, 6, 15, 30, 0, ZoneOffset.ofTotalSeconds(9900)), "Odd‑minute offset +02:45"), // Nanosecond precision preservation (offset 0)
        Arguments.of(LocalDateTime.of(2023, 8, 20, 14, 22, 33, 123_456_789), 0L, OffsetDateTime.of(2023, 8, 20, 14, 22, 33, 123_456_789, ZoneOffset.UTC), "Nanosecond precision with UTC offset"));
    }

    @Test
    void toOffsetDateTime_positiveOffset() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2023, 3, 15, 10, 20, 30);
        // Simulate a +02:00 time‑zone (offset = 2 * 3600 seconds = 7200 seconds)
        // milliseconds
        when(timeZone.getOffset(anyLong())).thenReturn(7200_000);
        // WHEN
        OffsetDateTime result = LocalDateTimeConversions.toOffsetDateTime(ldt, converter);
        // THEN
        ZoneOffset expectedOffset = ZoneOffset.ofTotalSeconds(7200);
        OffsetDateTime expected = ldt.atOffset(expectedOffset);
        assertEquals(expected, result);
    }

    @Test
    void toOffsetDateTime_negativeOffset() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2021, 12, 31, 23, 45, 0);
        // Simulate a -05:30 time‑zone (offset = -(5*3600 + 30*60) = -19800 seconds)
        // milliseconds
        when(timeZone.getOffset(anyLong())).thenReturn(-19_800_000);
        // WHEN
        OffsetDateTime result = LocalDateTimeConversions.toOffsetDateTime(ldt, converter);
        // THEN
        ZoneOffset expectedOffset = ZoneOffset.ofTotalSeconds(-19_800);
        OffsetDateTime expected = ldt.atOffset(expectedOffset);
        assertEquals(expected, result);
    }

    @Test
    void toOffsetDateTime_zeroOffset() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2020, 6, 1, 0, 0, 0);
        // Simulate UTC (offset zero)
        // milliseconds
        when(timeZone.getOffset(anyLong())).thenReturn(0);
        // WHEN
        OffsetDateTime result = LocalDateTimeConversions.toOffsetDateTime(ldt, converter);
        // THEN
        ZoneOffset expectedOffset = ZoneOffset.UTC;
        OffsetDateTime expected = ldt.atOffset(expectedOffset);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "{index} => {2}")
    @MethodSource("normalConversionProvider")
    void toOffsetDateTime_normalConversions(LocalDateTime source, long offsetSeconds, OffsetDateTime expected, String description) {
        // GIVEN – configure the mock TimeZone to return the required offset in *milliseconds*
        when(timeZone.getOffset(anyLong())).thenReturn(offsetSeconds * 1000L);
        // WHEN
        OffsetDateTime result = LocalDateTimeConversions.toOffsetDateTime(source, converter);
        // THEN
        assertEquals(expected, result, description);
    }

    @Test
    void toOffsetDateTime_offsetTruncationFromMillis() {
        // GIVEN – TimeZone returns 1 234 ms → 1 second after integer division
        LocalDateTime ldt = LocalDateTime.of(2024, 2, 29, 0, 0, 0);
        // milliseconds
        when(timeZone.getOffset(anyLong())).thenReturn(1_234L);
        // WHEN
        OffsetDateTime result = LocalDateTimeConversions.toOffsetDateTime(ldt, converter);
        // THEN – expected offset +00:01
        ZoneOffset expectedOffset = ZoneOffset.ofTotalSeconds(1);
        OffsetDateTime expected = ldt.atOffset(expectedOffset);
        assertEquals(expected, result);
    }

    @Test
    void toOffsetDateTime_nonLocalDateTimeSource_throwsClassCastException() {
        // GIVEN
        Object notALdt = "2023-05-10T12:30:45";
        // any offset – not used
        when(timeZone.getOffset(anyLong())).thenReturn(3600_000L);
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> LocalDateTimeConversions.toOffsetDateTime(notALdt, converter));
    }

    @Test
    void toOffsetDateTime_nullConverter_throwsNullPointerException() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.now();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toOffsetDateTime(ldt, null));
    }

    @Test
    void toOffsetDateTime_nullOptions_throwsNullPointerException() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.now();
        // Options is null
        when(converter.getOptions()).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toOffsetDateTime(ldt, converter));
    }

    @Test
    void toOffsetDateTime_nullTimeZone_throwsNullPointerException() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.now();
        // TimeZone is null
        when(options.getTimeZone()).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toOffsetDateTime(ldt, converter));
    }

    @Test
    void toOffsetDateTime_nullSource_throwsNullPointerException() {
        // GIVEN
        // any offset
        when(timeZone.getOffset(anyLong())).thenReturn(3600_000L);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toOffsetDateTime(null, converter));
    }

    @Test
    void toOffsetDateTime_timeZoneThrowsRuntimeException_propagates() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2025, 7, 1, 12, 0, 0);
        when(timeZone.getOffset(anyLong())).thenThrow(new RuntimeException("boom"));
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> LocalDateTimeConversions.toOffsetDateTime(ldt, converter));
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void toOffsetDateTime_offsetExceedsMaximumPositive_throwsDateTimeException() {
        // GIVEN – offset 64 801 seconds (> +18:00)
        LocalDateTime ldt = LocalDateTime.of(2023, 5, 10, 12, 30, 45);
        when(timeZone.getOffset(anyLong())).thenReturn(64_801L * 1000L);
        // WHEN / THEN
        assertThrows(DateTimeException.class, () -> LocalDateTimeConversions.toOffsetDateTime(ldt, converter));
    }

    @Test
    void toOffsetDateTime_offsetExceedsMaximumNegative_throwsDateTimeException() {
        // GIVEN – offset -64 801 seconds (< -18:00)
        LocalDateTime ldt = LocalDateTime.of(2023, 5, 10, 12, 30, 45);
        when(timeZone.getOffset(anyLong())).thenReturn(-64_801L * 1000L);
        // WHEN / THEN
        assertThrows(DateTimeException.class, () -> LocalDateTimeConversions.toOffsetDateTime(ldt, converter));
    }

    @Test
    void toOffsetDateTime_positiveOffset_legacy() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2023, 3, 15, 10, 20, 30);
        // +02:00 in ms
        when(timeZone.getOffset(anyLong())).thenReturn(7_200_000);
        // WHEN
        OffsetDateTime result = LocalDateTimeConversions.toOffsetDateTime(ldt, converter);
        // THEN
        ZoneOffset expectedOffset = ZoneOffset.ofTotalSeconds(7_200);
        OffsetDateTime expected = ldt.atOffset(expectedOffset);
        assertEquals(expected, result);
    }

    @Test
    void toOffsetDateTime_negativeOffset_legacy() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2021, 12, 31, 23, 45, 0);
        // -05:30 in ms
        when(timeZone.getOffset(anyLong())).thenReturn(-19_800_000);
        // WHEN
        OffsetDateTime result = LocalDateTimeConversions.toOffsetDateTime(ldt, converter);
        // THEN
        ZoneOffset expectedOffset = ZoneOffset.ofTotalSeconds(-19_800);
        OffsetDateTime expected = ldt.atOffset(expectedOffset);
        assertEquals(expected, result);
    }

    @Test
    void toOffsetDateTime_zeroOffset_legacy() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2020, 6, 1, 0, 0, 0);
        // UTC in ms
        when(timeZone.getOffset(anyLong())).thenReturn(0);
        // WHEN
        OffsetDateTime result = LocalDateTimeConversions.toOffsetDateTime(ldt, converter);
        // THEN
        ZoneOffset expectedOffset = ZoneOffset.UTC;
        OffsetDateTime expected = ldt.atOffset(expectedOffset);
        assertEquals(expected, result);
    }
}
