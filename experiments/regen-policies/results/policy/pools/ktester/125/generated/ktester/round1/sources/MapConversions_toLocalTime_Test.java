package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class MapConversions_toLocalTime_Test {

    @Mock
    private Converter // injected by Mockito
    mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Reset mock before each test to avoid cross‑test interference.
        reset(mockConverter);
    }

    private static Stream<Arguments> provideSuccessfulHourMinuteCases() {
        return Stream.of(// full fields – already covered by the explicit test, but kept for completeness
        Arguments.of("All fields present (int values)", mapOf("hour", 13, "minute", 45, "second", 30, "nanos", 123456789), LocalTime.of(13, 45, 30, 123456789)), // missing second (null) → treated as 0
        Arguments.of("Missing second (null) – should become 0", mapOf("hour", 9, "minute", 15, "second", null, "nanos", 250_000_000), LocalTime.of(9, 15, 0, 250_000_000)), // missing nanos (null) → 0
        Arguments.of("Missing nanos (null) – should become 0", mapOf("hour", 22, "minute", 5, "second", 30, "nanos", null), LocalTime.of(22, 5, 30, 0)), // second null, nanos present
        Arguments.of("Second null, nanos present", mapOf("hour", 3, "minute", 40, "second", null, "nanos", 123), LocalTime.of(3, 40, 0, 123)), // values supplied as strings
        Arguments.of("All numeric fields as strings", mapOf("hour", "07", "minute", "45", "second", "15", "nanos", "500000000"), LocalTime.of(7, 45, 15, 500_000_000)), // extra irrelevant keys should be ignored
        Arguments.of("Extra irrelevant keys are ignored", mapOf("hour", 14, "minute", 30, "second", 10, "nanos", 0, "foo", "bar", "unused", 12345), LocalTime.of(14, 30, 10, 0)), // hour/minute present together with a time string – hour/minute take precedence
        Arguments.of("Hour/minute take precedence over time key", mapOf("hour", 8, "minute", 20, "second", 5, "nanos", 0, "time", "23:59:59.999999999"), LocalTime.of(8, 20, 5, 0)));
    }

    private static Stream<Arguments> provideInvalidNumericCases() {
        return Stream.of(Arguments.of("Hour out of range (25)", mapOf("hour", 25, "minute", 10, "second", 0, "nanos", 0), DateTimeException.class), Arguments.of("Minute out of range (61)", mapOf("hour", 12, "minute", 61, "second", 0, "nanos", 0), DateTimeException.class), Arguments.of("Second out of range (70)", mapOf("hour", 10, "minute", 15, "second", 70, "nanos", 0), DateTimeException.class), Arguments.of("Nanos out of range (1_000_000_000)", mapOf("hour", 8, "minute", 45, "second", 30, "nanos", 1_000_000_000), DateTimeException.class));
    }

    /* --------------------------------------------------------------------- */
    private static Map<String, Object> mapOf(Object... keyVals) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyVals.length; i += 2) {
            String key = (String) keyVals[i];
            Object val = keyVals[i + 1];
            map.put(key, val);
        }
        return map;
    }

    /**
     * Verifies that when the map contains {@code hour}, {@code minute},
     * {@code second} and {@code nanos} keys, the method uses the supplied
     * {@link Converter} to obtain integer values and returns the expected
     * {@link LocalTime}.
     */
    @Test
    void testToLocalTime_WithAllComponents_ReturnsExpectedLocalTime() {
        // GIVEN
        Map<String, Object> source = new HashMap<>();
        source.put("hour", 10);
        source.put("minute", 20);
        source.put("second", 30);
        source.put("nanos", 123_456_789);
        // Stub the converter so that each call returns the same integer it received.
        when(mockConverter.convert(eq(10), eq(int.class))).thenReturn(10);
        when(mockConverter.convert(eq(20), eq(int.class))).thenReturn(20);
        when(mockConverter.convert(eq(30), eq(int.class))).thenReturn(30);
        when(mockConverter.convert(eq(123_456_789), eq(int.class))).thenReturn(123_456_789);
        // WHEN
        LocalTime actual = MapConversions.toLocalTime(source, mockConverter);
        // THEN
        LocalTime expected = LocalTime.of(10, 20, 30, 123_456_789);
        assertEquals(expected, actual, "LocalTime should be built from the map values");
        // Verify that the converter was called exactly once for each component.
        verify(mockConverter, times(1)).convert(eq(10), eq(int.class));
        verify(mockConverter, times(1)).convert(eq(20), eq(int.class));
        verify(mockConverter, times(1)).convert(eq(30), eq(int.class));
        verify(mockConverter, times(1)).convert(eq(123_456_789), eq(int.class));
        verifyNoMoreInteractions(mockConverter);
    }

    /**
     * When either {@code hour} or {@code minute} is missing the method should
     * delegate to the private {@code fromMap} helper.  Because that helper is
     * private we cannot stub it; the most reliable observable behaviour is that
     * the method returns {@code null} for a map that does not contain the
     * required keys (the helper returns {@code null} for unknown structures).
     */
    @Test
    void testToLocalTime_DelegatesWhenHourOrMinuteMissing() {
        // GIVEN – a map that lacks the mandatory "hour" key
        Map<String, Object> source = new HashMap<>();
        source.put("minute", 15);
        // some alternative key that fromMap might understand
        source.put("time", "12:15:00");
        // The converter is not expected to be used because the early‑exit condition fails.
        // No stubbing is required.
        // WHEN
        LocalTime result = MapConversions.toLocalTime(source, mockConverter);
        // THEN
        assertNull(result, "When hour/minute are missing, the method should fall back to the private helper and return null for this simple map");
        // Ensure the converter was never invoked.
        verifyNoInteractions(mockConverter);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideSuccessfulHourMinuteCases")
    void testToLocalTime_HourMinutePresent_Success(String description, Map<String, Object> source, LocalTime expected) {
        // Generic converter behaviour: numbers → same int, strings → parsed int, null → 0
        when(mockConverter.convert(any(), eq(int.class))).thenAnswer(invocation -> {
            Object arg = invocation.getArgument(0);
            if (arg == null)
                return 0;
            if (arg instanceof Number)
                return ((Number) arg).intValue();
            if (arg instanceof String)
                return Integer.parseInt((String) arg);
            throw new IllegalArgumentException("Unsupported conversion");
        });
        LocalTime actual = MapConversions.toLocalTime(source, mockConverter);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideInvalidNumericCases")
    void testToLocalTime_InvalidNumericValues_ThrowsDateTimeException(String description, Map<String, Object> source, Class<? extends Throwable> expectedException) {
        // Same generic converter as the successful cases.
        when(mockConverter.convert(any(), eq(int.class))).thenAnswer(invocation -> {
            Object arg = invocation.getArgument(0);
            if (arg == null)
                return 0;
            if (arg instanceof Number)
                return ((Number) arg).intValue();
            if (arg instanceof String)
                return Integer.parseInt((String) arg);
            throw new IllegalArgumentException("Unsupported conversion");
        });
        assertThrows(expectedException, () -> MapConversions.toLocalTime(source, mockConverter), description);
    }

    @Test
    void testToLocalTime_ConverterThrowsDuringHourConversion() {
        Map<String, Object> source = mapOf("hour", 12, "minute", 30);
        // Simulate a converter that throws for the hour value.
        when(mockConverter.convert(eq(12), eq(int.class))).thenThrow(new IllegalArgumentException("converter failure on hour"));
        // Minute conversion can stay default (won't be reached).
        when(mockConverter.convert(eq(30), eq(int.class))).thenReturn(30);
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toLocalTime(source, mockConverter), "Exception from converter should propagate");
    }

    @Test
    void testToLocalTime_SecondNonNumericString_ThrowsNumberFormatException() {
        Map<String, Object> source = mapOf("hour", 9, "minute", 20, "second", "sixty", "nanos", 0);
        // Generic conversion for numbers/strings, but let Integer.parseInt throw for "sixty".
        when(mockConverter.convert(any(), eq(int.class))).thenAnswer(invocation -> {
            Object arg = invocation.getArgument(0);
            if (arg == null)
                return 0;
            if (arg instanceof Number)
                return ((Number) arg).intValue();
            // This will throw NumberFormatException for non‑numeric strings.
            return Integer.parseInt(arg.toString());
        });
        assertThrows(NumberFormatException.class, () -> MapConversions.toLocalTime(source, mockConverter), "Parsing a non‑numeric string for 'second' should throw NumberFormatException");
    }

    @Test
    void testToLocalTime_Fallback_ParsesTimeString() {
        Map<String, Object> source = mapOf("time", "15:20:35.123456789");
        // The converter is not used for the fallback path in the current implementation,
        // but we provide a harmless stub in case it is.
        when(mockConverter.convert(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        LocalTime result = MapConversions.toLocalTime(source, mockConverter);
        assertEquals(LocalTime.of(15, 20, 35, 123_456_789), result, "Fallback should parse ISO‑8601 time string");
    }

    @Test
    void testToLocalTime_Fallback_MissingHourThrowsIllegalArgumentException() {
        Map<String, Object> source = mapOf("minute", 15, "second", 20, "nanos", 5_000_000);
        // No special stubbing needed; we only care about the exception type.
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toLocalTime(source, mockConverter), "Missing 'hour' key should cause an IllegalArgumentException in the fallback path");
    }

    @Test
    void testToLocalTime_Fallback_EmptyMapThrowsIllegalArgumentException() {
        Map<String, Object> source = new HashMap<>();
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toLocalTime(source, mockConverter), "Empty map should cause an IllegalArgumentException in the fallback path");
    }

    @Test
    void testToLocalTime_Fallback_UnparsableTimeString_ThrowsDateTimeParseException() {
        Map<String, Object> source = mapOf("time", "not-a-time");
        assertThrows(DateTimeParseException.class, () -> MapConversions.toLocalTime(source, mockConverter), "Unparsable time string should result in DateTimeParseException");
    }

    @Test
    void testToLocalTime_Fallback_NullTimeValue_ThrowsIllegalArgumentException() {
        Map<String, Object> source = mapOf("time", null);
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toLocalTime(source, mockConverter), "Null time value should cause IllegalArgumentException");
    }

    @Test
    void testToLocalTime_NullFromArgument_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> MapConversions.toLocalTime(null, mockConverter), "Passing null as the source map should throw NullPointerException");
    }

    @Test
    void testToLocalTime_NonMapFromArgument_ThrowsClassCastException() {
        assertThrows(ClassCastException.class, () -> MapConversions.toLocalTime("I am not a map", mockConverter), "Passing a non‑map object should throw ClassCastException");
    }
}
