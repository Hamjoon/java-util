package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toOffsetDateTime_Test {

    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    @Mock
    private TimeZone mockTimeZone;

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mock hierarchy used by the method under test.
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getTimeZone()).thenReturn(mockTimeZone);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions to avoid cross‑test contamination.
        clearInvocations(mockConverter, mockOptions, mockTimeZone);
    }

    /* -------------------------------------------------------------
     *  Parameterized tests for valid offset conversions
     * ------------------------------------------------------------- */
    private static Stream<Arguments> validOffsetProvider() {
        return Stream.of(// zero offset (UTC)
        Arguments.of(LocalDateTime.of(2023, 3, 15, 10, 30, 45), 0L, ZoneOffset.UTC), // positive whole‑hour offset (+02:00)
        Arguments.of(LocalDateTime.of(2023, 7, 1, 8, 0, 0), 2L * 60 * 60 * 1000, ZoneOffset.ofHours(2)), // negative whole‑hour offset (-05:00)
        Arguments.of(LocalDateTime.of(2022, 12, 31, 23, 45, 0), -5L * 60 * 60 * 1000, ZoneOffset.ofHours(-5)), // positive offset with minutes (+05:30)
        Arguments.of(LocalDateTime.of(2023, 1, 1, 12, 0, 0), (5L * 60 * 60 + 30L * 60) * 1000, ZoneOffset.ofHoursMinutes(5, 30)), // negative offset with minutes (-03:45)
        Arguments.of(LocalDateTime.of(2023, 6, 30, 23, 59, 59, 123_456_789), -(3L * 60 * 60 + 45L * 60) * 1000, ZoneOffset.ofHoursMinutes(-3, -45)), // maximum positive offset (+18:00)
        Arguments.of(LocalDateTime.of(2025, 12, 31, 23, 0, 0), 18L * 60 * 60 * 1000, ZoneOffset.ofHours(18)), // maximum negative offset (-18:00)
        Arguments.of(LocalDateTime.of(2025, 12, 31, 23, 0, 0), -18L * 60 * 60 * 1000, ZoneOffset.ofHours(-18)), // edge case: LocalDateTime.MIN with zero offset
        Arguments.of(LocalDateTime.MIN, 0L, ZoneOffset.UTC), // edge case: LocalDateTime.MAX with zero offset
        Arguments.of(LocalDateTime.MAX, 0L, ZoneOffset.UTC));
    }

    @Test
    void toOffsetDateTime_PositiveTwoHourOffset() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2023, Month.JANUARY, 15, 10, 20, 30);
        // TimeZone offset of +02:00 expressed in milliseconds.
        when(mockTimeZone.getOffset(anyLong())).thenReturn(2 * 60 * 60 * 1000);
        // WHEN
        OffsetDateTime result = LocalDateTimeConversions.toOffsetDateTime(ldt, mockConverter);
        // THEN
        ZoneOffset expectedOffset = ZoneOffset.ofHours(2);
        OffsetDateTime expected = ldt.atOffset(expectedOffset);
        assertEquals(expected, result);
        verify(mockTimeZone).getOffset(anyLong());
    }

    @Test
    void toOffsetDateTime_NegativeThreeHourOffset() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2021, Month.DECEMBER, 31, 23, 45, 0);
        // TimeZone offset of -03:00 expressed in milliseconds.
        when(mockTimeZone.getOffset(anyLong())).thenReturn(-3 * 60 * 60 * 1000);
        // WHEN
        OffsetDateTime result = LocalDateTimeConversions.toOffsetDateTime(ldt, mockConverter);
        // THEN
        ZoneOffset expectedOffset = ZoneOffset.ofHours(-3);
        OffsetDateTime expected = ldt.atOffset(expectedOffset);
        assertEquals(expected, result);
        verify(mockTimeZone).getOffset(anyLong());
    }

    @Test
    void toOffsetDateTime_ZeroOffset() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2020, Month.FEBRUARY, 29, 12, 0, 0);
        when(mockTimeZone.getOffset(anyLong())).thenReturn(0);
        // WHEN
        OffsetDateTime result = LocalDateTimeConversions.toOffsetDateTime(ldt, mockConverter);
        // THEN
        ZoneOffset expectedOffset = ZoneOffset.UTC;
        OffsetDateTime expected = ldt.atOffset(expectedOffset);
        assertEquals(expected, result);
        verify(mockTimeZone).getOffset(anyLong());
    }

    @Test
    void toOffsetDateTime_InvalidSourceObject_ThrowsClassCastException() {
        // GIVEN
        String notALocalDateTime = "2023-01-01T10:00:00";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> LocalDateTimeConversions.toOffsetDateTime(notALocalDateTime, mockConverter), "Non‑LocalDateTime source must trigger ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] {0} with offsetMillis={1}")
    @MethodSource("validOffsetProvider")
    void toOffsetDateTime_ValidOffsets(LocalDateTime input, long offsetMillis, ZoneOffset expectedOffset) {
        // GIVEN
        when(mockTimeZone.getOffset(anyLong())).thenReturn(offsetMillis);
        // WHEN
        OffsetDateTime result = LocalDateTimeConversions.toOffsetDateTime(input, mockConverter);
        // THEN
        OffsetDateTime expected = input.atOffset(expectedOffset);
        assertEquals(expected, result, "OffsetDateTime does not match expected value");
        verify(mockTimeZone).getOffset(anyLong());
        // Immutability check – the original LocalDateTime must remain unchanged.
        assertSame(input, result.toLocalDateTime(), "The method should not mutate the supplied LocalDateTime instance");
    }

    @Test
    void toOffsetDateTime_NullSourceObject_ThrowsNullPointerException() {
        // GIVEN
        // offset irrelevant
        when(mockTimeZone.getOffset(anyLong())).thenReturn(0L);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toOffsetDateTime(null, mockConverter), "Null source should result in NullPointerException");
    }

    @Test
    void toOffsetDateTime_NullConverter_ThrowsNullPointerException() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.now();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toOffsetDateTime(ldt, null), "Null converter should result in NullPointerException");
    }

    @Test
    void toOffsetDateTime_ConverterOptionsNull_ThrowsNullPointerException() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.now();
        // options are null
        when(mockConverter.getOptions()).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toOffsetDateTime(ldt, mockConverter), "Null options should cause NullPointerException");
    }

    @Test
    void toOffsetDateTime_TimeZoneNull_ThrowsNullPointerException() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.now();
        // timezone is null
        when(mockOptions.getTimeZone()).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toOffsetDateTime(ldt, mockConverter), "Null TimeZone should cause NullPointerException");
    }

    @Test
    void toOffsetDateTime_OffsetBeyondMaximum_ThrowsDateTimeException() {
        // GIVEN: +19:00 exceeds ZoneOffset's allowed range (+18:00)
        LocalDateTime ldt = LocalDateTime.of(2023, 5, 10, 12, 0, 0);
        // 68,400,000 ms
        long offsetMillis = 19L * 60 * 60 * 1000;
        when(mockTimeZone.getOffset(anyLong())).thenReturn(offsetMillis);
        // WHEN / THEN
        assertThrows(DateTimeException.class, () -> LocalDateTimeConversions.toOffsetDateTime(ldt, mockConverter), "Offset +19:00 should cause DateTimeException");
    }

    @Test
    void toOffsetDateTime_OffsetBeyondMinimum_ThrowsDateTimeException() {
        // GIVEN: -19:00 exceeds the minimum allowed (-18:00)
        LocalDateTime ldt = LocalDateTime.of(2023, 5, 10, 12, 0, 0);
        // -68,400,000 ms
        long offsetMillis = -19L * 60 * 60 * 1000;
        when(mockTimeZone.getOffset(anyLong())).thenReturn(offsetMillis);
        // WHEN / THEN
        assertThrows(DateTimeException.class, () -> LocalDateTimeConversions.toOffsetDateTime(ldt, mockConverter), "Offset -19:00 should cause DateTimeException");
    }
}
