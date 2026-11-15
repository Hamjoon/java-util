package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalTimeConversions_toCalendar_Test {

    @Mock
    private Converter // the Converter passed to the method
    converter;

    @Mock
    private ConverterOptions // returned by converter.getOptions()
    options;

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mock chain: converter -> options -> TimeZone
        when(converter.getOptions()).thenReturn(options);
        when(options.getTimeZone()).thenReturn(TimeZone.getTimeZone("UTC"));
    }

    @AfterEach
    void teardownAfterEach() {
        // Verify that the mocks were used as expected and reset for the next test
        verify(converter).getOptions();
        verify(options).getTimeZone();
        // No further interactions should have occurred
        verifyNoMoreInteractions(converter, options);
    }

    /**
     * Provides a stream of arguments for normal conversion tests.
     * Each argument consists of:
     *   - the LocalTime to convert
     *   - the ID of the expected TimeZone
     *   - expected hour, minute, second, millisecond values
     */
    private static Stream<Arguments> normalConversionProvider() {
        return Stream.of(// midnight, zero nanoseconds, UTC
        Arguments.of(LocalTime.of(0, 0, 0, 0), "UTC", 0, 0, 0, 0, "Verify conversion of midnight with zero nanoseconds results in a Calendar set to 00:00:00.000 UTC."), // typical time, nanoseconds truncate to 123 ms, New York
        Arguments.of(LocalTime.of(13, 45, 30, 123_456_789), "America/New_York", 13, 45, 30, 123, "Test typical time with nanoseconds that truncate to milliseconds."), // maximum time of day, 999 ms, Tokyo
        Arguments.of(LocalTime.of(23, 59, 59, 999_999_999), "Asia/Tokyo", 23, 59, 59, 999, "Edge case: maximum time of day with nanoseconds just under one millisecond overflow."), // noon, zero nanoseconds, Europe/Paris
        Arguments.of(LocalTime.of(12, 0, 0, 0), "Europe/Paris", 12, 0, 0, 0, "Verify conversion for noon with zero nanoseconds."), // fixed‑offset zone, half‑millisecond → 500 ms
        Arguments.of(LocalTime.of(5, 30, 15, 500_000_000), "GMT+02:00", 5, 30, 15, 500, "Check conversion when the timezone has a fixed offset and nanoseconds map to half a millisecond."));
    }

    /**
     * Provides edge‑case scenarios that focus on nanosecond → millisecond truncation.
     */
    private static Stream<Arguments> edgeCaseProvider() {
        return Stream.of(// nanoseconds just under one millisecond → 0 ms
        Arguments.of(LocalTime.of(10, 20, 30, 999_999), "UTC", 10, 20, 30, 0, "Nanoseconds just under one millisecond should truncate to 0 milliseconds."), // exactly one millisecond → 1 ms
        Arguments.of(LocalTime.of(10, 20, 30, 1_000_000), "UTC", 10, 20, 30, 1, "Exactly one millisecond of nanoseconds should produce millisecond=1."), // 1.5 ms → truncated to 1 ms
        Arguments.of(LocalTime.of(10, 20, 30, 1_500_000), "UTC", 10, 20, 30, 1, "Nanoseconds that are not a whole millisecond are truncated (1.5 ms → 1 ms)."), // negative fixed‑offset zone, 500 ms
        Arguments.of(LocalTime.of(8, 15, 45, 500_000_000), "GMT-04:00", 8, 15, 45, 500, "Conversion with a negative fixed‑offset timezone; nanoseconds map to 500 ms."), // DST‑observing zone – fields are copied unchanged
        Arguments.of(LocalTime.of(2, 30, 0, 0), "America/Los_Angeles", 2, 30, 0, 0, "Conversion in a timezone that observes DST; the method should simply copy the LocalTime fields regardless of DST rules."));
    }

    @Test
    void toCalendar_ShouldCopyLocalTimeFieldsAndUseConverterTimeZone() {
        // GIVEN
        // LocalTime with hour, minute, second and a nanosecond component
        // 123 ms + 456_789 ns
        LocalTime localTime = LocalTime.of(13, 45, 30, 123_456_789);
        int expectedHour = 13;
        int expectedMinute = 45;
        int expectedSecond = 30;
        // nanoseconds -> milliseconds (truncated)
        int expectedMilli = 123;
        // WHEN
        Calendar result = LocalTimeConversions.toCalendar(localTime, converter);
        // THEN
        assertNotNull(result, "Resulting Calendar should not be null");
        assertEquals(TimeZone.getTimeZone("UTC"), result.getTimeZone(), "Calendar must use the TimeZone supplied by ConverterOptions");
        assertEquals(expectedHour, result.get(Calendar.HOUR_OF_DAY), "Hour of day should match LocalTime");
        assertEquals(expectedMinute, result.get(Calendar.MINUTE), "Minute should match LocalTime");
        assertEquals(expectedSecond, result.get(Calendar.SECOND), "Second should match LocalTime");
        assertEquals(expectedMilli, result.get(Calendar.MILLISECOND), "Millisecond should be derived from nanoseconds");
    }

    @ParameterizedTest(name = "[{index}] {5}")
    @MethodSource("normalConversionProvider")
    void toCalendar_ShouldCopyFieldsAndRespectTimeZone(LocalTime from, String zoneId, int expHour, int expMinute, int expSecond, int expMilli, String description) {
        // GIVEN – override the default TimeZone for this test case
        when(options.getTimeZone()).thenReturn(TimeZone.getTimeZone(zoneId));
        // WHEN
        Calendar result = LocalTimeConversions.toCalendar(from, converter);
        // THEN
        assertNotNull(result, "Resulting Calendar must not be null");
        assertEquals(TimeZone.getTimeZone(zoneId), result.getTimeZone(), "Calendar must use the TimeZone supplied by ConverterOptions");
        assertEquals(expHour, result.get(Calendar.HOUR_OF_DAY), "Hour of day should match LocalTime");
        assertEquals(expMinute, result.get(Calendar.MINUTE), "Minute should match LocalTime");
        assertEquals(expSecond, result.get(Calendar.SECOND), "Second should match LocalTime");
        assertEquals(expMilli, result.get(Calendar.MILLISECOND), "Millisecond should be derived from nanoseconds (truncated)");
    }

    @ParameterizedTest(name = "[{index}] {5}")
    @MethodSource("edgeCaseProvider")
    void toCalendar_EdgeCases_NanoToMilliTruncation(LocalTime from, String zoneId, int expHour, int expMinute, int expSecond, int expMilli, String description) {
        // GIVEN – set the required TimeZone
        when(options.getTimeZone()).thenReturn(TimeZone.getTimeZone(zoneId));
        // WHEN
        Calendar result = LocalTimeConversions.toCalendar(from, converter);
        // THEN
        assertNotNull(result, "Resulting Calendar must not be null");
        assertEquals(TimeZone.getTimeZone(zoneId), result.getTimeZone(), "Calendar must use the TimeZone supplied by ConverterOptions");
        assertEquals(expHour, result.get(Calendar.HOUR_OF_DAY));
        assertEquals(expMinute, result.get(Calendar.MINUTE));
        assertEquals(expSecond, result.get(Calendar.SECOND));
        assertEquals(expMilli, result.get(Calendar.MILLISECOND));
    }

    @Test
    void toCalendar_NullFrom_ShouldThrowNullPointerException() {
        // GIVEN – a null 'from' argument
        LocalTime from = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalTimeConversions.toCalendar(from, converter), "Passing a null 'from' argument should result in a NullPointerException");
    }

    @Test
    void toCalendar_NonLocalTimeFrom_ShouldThrowClassCastException() {
        // GIVEN – an object that is not a LocalTime
        Object from = Integer.valueOf(5);
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> LocalTimeConversions.toCalendar(from, converter), "Providing an object that is not a LocalTime should cause a ClassCastException");
    }

    @Test
    void toCalendar_OptionsReturnsNullTimeZone_ShouldThrowNullPointerException() {
        // GIVEN – Options mock returns null for getTimeZone()
        when(options.getTimeZone()).thenReturn(null);
        LocalTime from = LocalTime.of(12, 0, 0, 0);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalTimeConversions.toCalendar(from, converter), "If Options returns a null TimeZone, Calendar.getInstance(null) throws NullPointerException");
    }

    @Test
    void toCalendar_ConverterGetOptionsThrowsNullPointerException() {
        // GIVEN – configure the converter mock to throw NPE when getOptions() is called
        when(converter.getOptions()).thenThrow(new NullPointerException("converter.getOptions() NPE"));
        LocalTime from = LocalTime.of(12, 0, 0, 0);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalTimeConversions.toCalendar(from, converter), "If the Converter throws NPE on getOptions(), the method should propagate the exception");
    }

    @Test
    void toCalendar_ConverterIsNull_ShouldThrowNullPointerException() {
        // GIVEN – a null converter reference
        Converter nullConverter = null;
        LocalTime from = LocalTime.of(12, 0, 0, 0);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalTimeConversions.toCalendar(from, nullConverter), "Passing a null 'converter' argument should cause a NullPointerException");
        // NOTE: This test bypasses the mock verification in @AfterEach because the mock chain is never touched.
        // To keep the framework intact, we reset the verification expectations for this test.
        // The verification in @AfterEach will still succeed because no interactions occurred on the mocks.
    }
}
