package com.cedarsoftware.util.convert;

import java.time.LocalTime;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class LongConversions_toLocalTime_Test {

    /**
     * The {@link Converter} instance required by the method signature.
     *  It is not used inside the method, so a mock is sufficient.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Create a mock Converter – the implementation does not touch it.
        converter = mock(Converter.class);
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// group: Valid conversions
        Arguments.of(0L, "Convert 0 milliseconds to LocalTime, expecting MIDNIGHT (00:00)", "00:00"), Arguments.of(43_200_000L, "Convert 43,200,000 ms (12 hours) to LocalTime, expecting 12:00", "12:00"), Arguments.of(86_399_999L, "Convert the maximum valid milliseconds (86,399,999) to LocalTime, expecting 23:59:59.999", "23:59:59.999"));
    }

    private static Stream<Arguments> boundaryConversionProvider() {
        return Stream.of(Arguments.of(1L, "Minimum positive millisecond should produce 00:00:00.001", "00:00:00.001"), Arguments.of(999L, "Just under one second (999 ms) should produce 00:00:00.999", "00:00:00.999"), Arguments.of(1_000L, "Exactly one second (1000 ms) should produce 00:00:01", "00:00:01"), Arguments.of(86_399_998L, "One millisecond before the maximum valid value should produce 23:59:59.998", "23:59:59.998"));
    }

    private static Stream<Arguments> invalidConversionProvider() {
        return Stream.of(Arguments.of(-1L, "Negative milliseconds should cause IllegalArgumentException."), Arguments.of(86_400_000L, "Milliseconds equal to 86,400,000 (24h) exceed the day range and should cause IllegalArgumentException."));
    }

    private static Stream<Arguments> converterIgnoredProvider() {
        // Use a separate mock to emphasise that the value is not consulted
        Converter dummy = mock(Converter.class);
        return Stream.of(Arguments.of(0L, dummy, "00:00"), Arguments.of(43_200_000L, dummy, "12:00"));
    }

    @Test
    void toLocalTime_zeroMillis_returnsMidnight() {
        Object from = Long.valueOf(0L);
        LocalTime expected = LocalTime.ofNanoOfDay(0L);
        LocalTime actual = LongConversions.toLocalTime(from, converter);
        assertEquals(expected, actual, "0 ms should map to 00:00:00.000");
    }

    @Test
    void toLocalTime_oneMillisecond_returnsOneMillisecondPastMidnight() {
        Object from = Long.valueOf(1L);
        // 1 ms = 1_000_000 ns
        LocalTime expected = LocalTime.ofNanoOfDay(1_000_000L);
        LocalTime actual = LongConversions.toLocalTime(from, converter);
        assertEquals(expected, actual, "1 ms should map to 00:00:00.001");
    }

    @Test
    void toLocalTime_maxValidMillis_returnsLastMomentOfDay() {
        // 86 399 999 ms = one millisecond before the next day
        Object from = Long.valueOf(86_399_999L);
        LocalTime expected = LocalTime.ofNanoOfDay(86_399_999L * 1_000_000L);
        LocalTime actual = LongConversions.toLocalTime(from, converter);
        assertEquals(expected, actual, "Maximum valid ms should map to 23:59:59.999");
    }

    @Test
    void toLocalTime_negativeMillis_throwsIllegalArgumentException() {
        Object from = Long.valueOf(-1L);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> LongConversions.toLocalTime(from, converter));
        assertTrue(ex.getMessage().contains("-1"), "Exception message should contain the offending value");
    }

    @Test
    void toLocalTime_tooLargeMillis_throwsIllegalArgumentException() {
        // 86 400 000 ms would be exactly 24 hours, which is out of range
        Object from = Long.valueOf(86_400_000L);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> LongConversions.toLocalTime(from, converter));
        assertTrue(ex.getMessage().contains("86_400_000"), "Exception message should contain the offending value");
    }

    @ParameterizedTest(name = "[{index}] {0} ms → {2}")
    @MethodSource("validConversionProvider")
    void toLocalTime_validConversions(long millis, String description, String expectedString) {
        Object from = Long.valueOf(millis);
        LocalTime actual = LongConversions.toLocalTime(from, converter);
        assertEquals(expectedString, actual.toString(), () -> "Failed conversion: " + description);
    }

    @ParameterizedTest(name = "[{index}] {0} ms → {2}")
    @MethodSource("boundaryConversionProvider")
    void toLocalTime_boundaryConversions(long millis, String description, String expectedString) {
        Object from = Long.valueOf(millis);
        LocalTime actual = LongConversions.toLocalTime(from, converter);
        assertEquals(expectedString, actual.toString(), () -> "Failed boundary conversion: " + description);
    }

    @ParameterizedTest(name = "[{index}] {0} ms → IllegalArgumentException")
    @MethodSource("invalidConversionProvider")
    void toLocalTime_invalidConversions(long millis, String description) {
        Object from = Long.valueOf(millis);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> LongConversions.toLocalTime(from, converter), () -> "Expected IllegalArgumentException for: " + description);
        // Verify that the offending value appears in the message
        assertTrue(ex.getMessage().contains(Long.toString(millis)), "Exception message should contain the offending value");
    }

    @Test
    void toLocalTime_nullFrom_throwsNullPointerException() {
        // The cast ((Long) from) will throw NPE when from is null
        assertThrows(NullPointerException.class, () -> LongConversions.toLocalTime(null, converter), "Passing null for `from` should trigger NullPointerException");
    }

    @Test
    void toLocalTime_integerFrom_throwsClassCastException() {
        // not a Long
        Object from = Integer.valueOf(1_000);
        assertThrows(ClassCastException.class, () -> LongConversions.toLocalTime(from, converter), "Providing an Integer instead of a Long should cause ClassCastException");
    }

    @Test
    void toLocalTime_stringFrom_throwsClassCastException() {
        // not a Long
        Object from = "12345";
        assertThrows(ClassCastException.class, () -> LongConversions.toLocalTime(from, converter), "Providing a String instead of a Long should cause ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] converter={1} → {2}")
    @MethodSource("converterIgnoredProvider")
    void toLocalTime_converterIsIgnored(long millis, Converter dummyConverter, String expectedString) {
        Object from = Long.valueOf(millis);
        LocalTime actual = LongConversions.toLocalTime(from, dummyConverter);
        assertEquals(expectedString, actual.toString(), "Method must ignore the provided Converter instance");
    }
}
