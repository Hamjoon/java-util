package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
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
    Converter // mock of the Converter passed to the method
    mockConverter;

    @Mock
    ConverterOptions // returned by Converter.getOptions()
    mockOptions;

    @Mock
    TimeZone // returned by ConverterOptions.getTimeZone()
    mockTimeZone;

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mock chain so that:
        // mockConverter.getOptions().getTimeZone().getOffset(anyLong())
        // returns a deterministic offset in milliseconds.
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getTimeZone()).thenReturn(mockTimeZone);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks after each test to avoid cross‑test interference
        reset(mockConverter, mockOptions, mockTimeZone);
    }

    private static Stream<Arguments> normalOffsetProvider() {
        return Stream.of(// UTC (zero offset)
        Arguments.of("Normal offsets - UTC", LocalDateTime.of(2023, 3, 25, 12, 0, 0), 0, "2023-03-25T12:00:00Z"), // Positive whole hour (+02:00)
        Arguments.of("Normal offsets - Positive whole hour", LocalDateTime.of(2021, 12, 31, 23, 59, 59), 7200, "2021-12-31T23:59:59+02:00"), // Negative whole hour (-05:00)
        Arguments.of("Normal offsets - Negative whole hour", LocalDateTime.of(2022, 6, 15, 8, 30, 0), -18000, "2022-06-15T08:30:00-05:00"), // Positive hour with minutes (+05:30)
        Arguments.of("Normal offsets - Positive hour with minutes", LocalDateTime.of(2020, 1, 1, 0, 0, 0), 19800, "2020-01-01T00:00:00+05:30"));
    }

    @Test
    void toOffsetDateTime_appliesCorrectZoneOffset() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2023, 5, 10, 12, 30, 45);
        // Simulate a timezone offset of +02:00 (i.e., 2 hours * 3600 seconds * 1000 ms)
        int offsetMillis = 2 * 3600 * 1000;
        when(mockTimeZone.getOffset(anyLong())).thenReturn(offsetMillis);
        // WHEN
        OffsetDateTime result = LocalDateTimeConversions.toOffsetDateTime(ldt, mockConverter);
        // THEN
        ZoneOffset expectedOffset = ZoneOffset.ofHours(2);
        assertEquals(expectedOffset, result.getOffset(), "The OffsetDateTime should carry the mocked +02:00 offset");
        assertEquals(ldt, result.toLocalDateTime(), "The LocalDateTime component must be unchanged");
    }

    @Test
    void toOffsetDateTime_withNegativeOffset() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2021, 12, 31, 23, 0, 0);
        // Simulate a timezone offset of -05:30 (i.e., -(5*3600 + 30*60) seconds * 1000 ms)
        int offsetMillis = -(5 * 3600 + 30 * 60) * 1000;
        when(mockTimeZone.getOffset(anyLong())).thenReturn(offsetMillis);
        // WHEN
        OffsetDateTime result = LocalDateTimeConversions.toOffsetDateTime(ldt, mockConverter);
        // THEN
        ZoneOffset expectedOffset = ZoneOffset.ofHoursMinutes(-5, -30);
        assertEquals(expectedOffset, result.getOffset(), "The OffsetDateTime should carry the mocked -05:30 offset");
        assertEquals(ldt, result.toLocalDateTime(), "The LocalDateTime component must be unchanged");
    }

    @Test
    void toOffsetDateTime_nullLocalDateTime_throwsNPE() {
        // GIVEN
        // offset does not matter
        when(mockTimeZone.getOffset(anyLong())).thenReturn(0);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toOffsetDateTime(null, mockConverter), "Passing null as the source should result in a NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("normalOffsetProvider")
    void toOffsetDateTime_normalOffsets(String description, LocalDateTime source, int offsetSeconds, String expectedIso) {
        // GIVEN
        int offsetMillis = offsetSeconds * 1000;
        when(mockTimeZone.getOffset(anyLong())).thenReturn(offsetMillis);
        // WHEN
        OffsetDateTime result = LocalDateTimeConversions.toOffsetDateTime(source, mockConverter);
        // THEN
        OffsetDateTime expected = OffsetDateTime.parse(expectedIso);
        assertEquals(expected, result, description);
    }

    @Test
    void toOffsetDateTime_boundaryMaxValue() {
        // GIVEN
        LocalDateTime max = LocalDateTime.MAX;
        // +01:00
        int offsetSeconds = 3600;
        when(mockTimeZone.getOffset(anyLong())).thenReturn(offsetSeconds * 1000);
        // WHEN
        OffsetDateTime result = LocalDateTimeConversions.toOffsetDateTime(max, mockConverter);
        // THEN
        assertEquals(max, result.toLocalDateTime(), "Maximum LocalDateTime should be preserved");
        assertEquals(ZoneOffset.ofHours(1), result.getOffset(), "Offset should be +01:00");
    }

    @Test
    void toOffsetDateTime_boundaryMinValue() {
        // GIVEN
        LocalDateTime min = LocalDateTime.MIN;
        // -01:00
        int offsetSeconds = -3600;
        when(mockTimeZone.getOffset(anyLong())).thenReturn(offsetSeconds * 1000);
        // WHEN
        OffsetDateTime result = LocalDateTimeConversions.toOffsetDateTime(min, mockConverter);
        // THEN
        assertEquals(min, result.toLocalDateTime(), "Minimum LocalDateTime should be preserved");
        assertEquals(ZoneOffset.ofHours(-1), result.getOffset(), "Offset should be -01:00");
    }

    @Test
    void toOffsetDateTime_nullConverter_throwsNPE() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2023, 3, 25, 12, 0, 0);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toOffsetDateTime(ldt, null), "Passing a null converter should cause NullPointerException");
    }

    @Test
    void toOffsetDateTime_nullOptions_throwsNPE() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2023, 3, 25, 12, 0, 0);
        // options are null
        when(mockConverter.getOptions()).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toOffsetDateTime(ldt, mockConverter), "Null options should cause NullPointerException");
    }

    @Test
    void toOffsetDateTime_nullTimeZone_throwsNPE() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2023, 3, 25, 12, 0, 0);
        // timezone is null
        when(mockOptions.getTimeZone()).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toOffsetDateTime(ldt, mockConverter), "Null TimeZone should cause NullPointerException");
    }

    @Test
    void toOffsetDateTime_invalidSourceType_throwsClassCastException() {
        // GIVEN
        when(mockTimeZone.getOffset(anyLong())).thenReturn(0);
        String invalidSource = "2022-01-01T00:00";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> LocalDateTimeConversions.toOffsetDateTime(invalidSource, mockConverter), "Passing a non‑LocalDateTime source should cause ClassCastException");
    }

    @Test
    void toOffsetDateTime_outOfRangePositiveOffset_throwsDateTimeException() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2023, 1, 1, 0, 0, 0);
        // exceeds +18:00 (64800 seconds)
        int offsetSeconds = 70_000;
        when(mockTimeZone.getOffset(anyLong())).thenReturn(offsetSeconds * 1000);
        // WHEN / THEN
        assertThrows(DateTimeException.class, () -> LocalDateTimeConversions.toOffsetDateTime(ldt, mockConverter), "Offset > +18:00 should cause DateTimeException");
    }

    @Test
    void toOffsetDateTime_outOfRangeNegativeOffset_throwsDateTimeException() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2023, 1, 1, 0, 0, 0);
        // exceeds -18:00
        int offsetSeconds = -70_000;
        when(mockTimeZone.getOffset(anyLong())).thenReturn(offsetSeconds * 1000);
        // WHEN / THEN
        assertThrows(DateTimeException.class, () -> LocalDateTimeConversions.toOffsetDateTime(ldt, mockConverter), "Offset < -18:00 should cause DateTimeException");
    }
}
