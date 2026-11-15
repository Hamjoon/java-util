package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalTimeConversions_toCalendar_Test {

    @Mock
    private Converter // injected by MockitoExtension
    mockConverter;

    private TimeZone testTimeZone;

    @BeforeEach
    void setupBeforeEach() {
        // Use a deterministic time‑zone so the test is repeatable on any machine
        testTimeZone = TimeZone.getTimeZone("UTC");
        // Configure the deep‑stubbed call chain: converter.getOptions().getTimeZone()
        when(mockConverter.getOptions().getTimeZone()).thenReturn(testTimeZone);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the mock to avoid interaction bleed‑over between tests
        reset(mockConverter);
    }

    private static Stream<Arguments> basicTimeValues() {
        return Stream.of(Arguments.of("Midnight with no nanoseconds (UTC)", LocalTime.of(0, 0, 0, 0), TimeZone.getTimeZone("UTC"), 0, 0, 0, 0), Arguments.of("Typical time without nanoseconds (UTC)", LocalTime.of(12, 34, 56, 0), TimeZone.getTimeZone("UTC"), 12, 34, 56, 0), Arguments.of("Maximum LocalTime value (UTC)", LocalTime.of(23, 59, 59, 999_999_999), TimeZone.getTimeZone("UTC"), 23, 59, 59, 999), Arguments.of("Nanoseconds truncated to whole milliseconds (UTC)", LocalTime.of(8, 15, 30, 123_456_789), TimeZone.getTimeZone("UTC"), 8, 15, 30, 123));
    }

    private static Stream<Arguments> timezoneHandling() {
        return Stream.of(Arguments.of("Pacific Standard Time (America/Los_Angeles)", LocalTime.of(10, 0, 0, 0), TimeZone.getTimeZone("America/Los_Angeles"), 10, 0, 0, 0), Arguments.of("Tokyo (Asia/Tokyo) with 500 ms", LocalTime.of(22, 45, 15, 500_000_000), TimeZone.getTimeZone("Asia/Tokyo"), 22, 45, 15, 500));
    }

    private static Stream<Arguments> millisecondEdgeCases() {
        return Stream.of(Arguments.of("Nanoseconds just under 2 ms (UTC)", LocalTime.of(1, 2, 3, 1_999_999), TimeZone.getTimeZone("UTC"), 1, 2, 3, 1), Arguments.of("Nanoseconds exactly 2 ms (UTC)", LocalTime.of(1, 2, 3, 2_000_000), TimeZone.getTimeZone("UTC"), 1, 2, 3, 2));
    }

    @Test
    void toCalendar_ShouldCopyLocalTimeFieldsAndPreserveTimeZone() {
        // GIVEN
        // 123 ms + 456 µs + 789 ns
        LocalTime localTime = LocalTime.of(13, 45, 30, 123_456_789);
        Calendar nowInZone = Calendar.getInstance(testTimeZone);
        int expectedYear = nowInZone.get(Calendar.YEAR);
        int expectedMonth = nowInZone.get(Calendar.MONTH);
        int expectedDay = nowInZone.get(Calendar.DAY_OF_MONTH);
        // WHEN
        Calendar result = LocalTimeConversions.toCalendar(localTime, mockConverter);
        // THEN
        assertNotNull(result, "Resulting Calendar must not be null");
        assertEquals(testTimeZone, result.getTimeZone(), "Calendar must use the Converter's TimeZone");
        // Date part – should be the same as “now” in the same zone
        assertEquals(expectedYear, result.get(Calendar.YEAR), "Year should match current year");
        assertEquals(expectedMonth, result.get(Calendar.MONTH), "Month should match current month");
        assertEquals(expectedDay, result.get(Calendar.DAY_OF_MONTH), "Day should match current day");
        // Time part – should reflect the supplied LocalTime
        assertEquals(13, result.get(Calendar.HOUR_OF_DAY), "Hour should be copied from LocalTime");
        assertEquals(45, result.get(Calendar.MINUTE), "Minute should be copied from LocalTime");
        assertEquals(30, result.get(Calendar.SECOND), "Second should be copied from LocalTime");
        // Nano‑seconds are truncated to milliseconds
        assertEquals(123, result.get(Calendar.MILLISECOND), "Millisecond should be derived from nanoseconds");
    }

    @Test
    void toCalendar_ShouldThrowClassCastException_WhenSourceIsNotLocalTime() {
        // GIVEN
        Object notALocalTime = "12:34:56";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> LocalTimeConversions.toCalendar(notALocalTime, mockConverter), "A non‑LocalTime source must result in a ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("basicTimeValues")
    void toCalendar_BasicTimeValues(String description, LocalTime from, TimeZone tz, int expHour, int expMinute, int expSecond, int expMilli) {
        // arrange – configure the converter to return the requested zone
        when(mockConverter.getOptions().getTimeZone()).thenReturn(tz);
        // act
        Calendar result = LocalTimeConversions.toCalendar(from, mockConverter);
        // assert – zone and time fields
        assertEquals(tz, result.getTimeZone(), "Time‑zone must match the converter's zone");
        assertEquals(expHour, result.get(Calendar.HOUR_OF_DAY), "Hour must be copied");
        assertEquals(expMinute, result.get(Calendar.MINUTE), "Minute must be copied");
        assertEquals(expSecond, result.get(Calendar.SECOND), "Second must be copied");
        assertEquals(expMilli, result.get(Calendar.MILLISECOND), "Millisecond must be derived from nanoseconds");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("timezoneHandling")
    void toCalendar_TimezoneHandling(String description, LocalTime from, TimeZone tz, int expHour, int expMinute, int expSecond, int expMilli) {
        when(mockConverter.getOptions().getTimeZone()).thenReturn(tz);
        Calendar result = LocalTimeConversions.toCalendar(from, mockConverter);
        assertEquals(tz, result.getTimeZone(), "Calendar must use the converter's time‑zone");
        assertEquals(expHour, result.get(Calendar.HOUR_OF_DAY));
        assertEquals(expMinute, result.get(Calendar.MINUTE));
        assertEquals(expSecond, result.get(Calendar.SECOND));
        assertEquals(expMilli, result.get(Calendar.MILLISECOND));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("millisecondEdgeCases")
    void toCalendar_MillisecondEdgeCases(String description, LocalTime from, TimeZone tz, int expHour, int expMinute, int expSecond, int expMilli) {
        when(mockConverter.getOptions().getTimeZone()).thenReturn(tz);
        Calendar result = LocalTimeConversions.toCalendar(from, mockConverter);
        assertEquals(expMilli, result.get(Calendar.MILLISECOND), "Milliseconds must be floor‑truncated from nanoseconds");
    }

    @Test
    void toCalendar_TimezoneWithNonHourOffset() {
        // Kolkata (+05:30) – the LocalTime fields must stay unchanged
        LocalTime lt = LocalTime.of(5, 30, 0, 0);
        TimeZone tz = TimeZone.getTimeZone("Asia/Kolkata");
        when(mockConverter.getOptions().getTimeZone()).thenReturn(tz);
        Calendar cal = LocalTimeConversions.toCalendar(lt, mockConverter);
        assertEquals(tz, cal.getTimeZone());
        assertEquals(5, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(30, cal.get(Calendar.MINUTE));
        assertEquals(0, cal.get(Calendar.SECOND));
        assertEquals(0, cal.get(Calendar.MILLISECOND));
    }

    @Test
    void toCalendar_ConverterWithNullTimezone_FallsBackToSystemDefault() {
        // Configure the converter to return null for the time‑zone
        when(mockConverter.getOptions().getTimeZone()).thenReturn(null);
        LocalTime lt = LocalTime.of(9, 15, 45, 250_000_000);
        Calendar result = LocalTimeConversions.toCalendar(lt, mockConverter);
        // Calendar.getInstance(null) uses the JVM default zone
        TimeZone systemDefault = TimeZone.getDefault();
        assertEquals(systemDefault, result.getTimeZone(), "When converter returns null, the system default zone must be used");
        assertEquals(9, result.get(Calendar.HOUR_OF_DAY));
        assertEquals(15, result.get(Calendar.MINUTE));
        assertEquals(45, result.get(Calendar.SECOND));
        assertEquals(250, result.get(Calendar.MILLISECOND));
    }

    @Test
    void toCalendar_DateFieldsReflectCurrentDate() {
        LocalTime lt = LocalTime.of(13, 0, 0, 0);
        Calendar now = Calendar.getInstance(testTimeZone);
        Calendar result = LocalTimeConversions.toCalendar(lt, mockConverter);
        assertEquals(now.get(Calendar.YEAR), result.get(Calendar.YEAR));
        assertEquals(now.get(Calendar.MONTH), result.get(Calendar.MONTH));
        assertEquals(now.get(Calendar.DAY_OF_MONTH), result.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    void toCalendar_ShouldThrowNullPointerException_WhenSourceIsNull() {
        assertThrows(NullPointerException.class, () -> LocalTimeConversions.toCalendar(null, mockConverter));
    }

    @Test
    void toCalendar_ShouldThrowNullPointerException_WhenConverterIsNull() {
        LocalTime lt = LocalTime.of(12, 0, 0, 0);
        assertThrows(NullPointerException.class, () -> LocalTimeConversions.toCalendar(lt, null));
    }
}
