package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalTime;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalTimeConversions_toInteger_Test {

    /**
     * Converter instance required by the method signature (not used internally).
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The Converter has a public constructor that requires options.
        // An empty options object is sufficient for the tests.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and avoid accidental reuse.
        converter = null;
    }

    private static Stream<Arguments> standardConversions() {
        return Stream.of(Arguments.of(LocalTime.of(0, 0), null, 0, "Midnight should be converted to 0 milliseconds."), Arguments.of(LocalTime.of(12, 0), null, 43_200_000, "Noon (12:00) should be converted to 43 200 000 ms."), Arguments.of(LocalTime.of(23, 59, 59, 999_999_999), null, 86_399_999, "One nanosecond before midnight should be truncated to 86 399 999 ms."), Arguments.of(LocalTime.of(6, 30, 15, 250_000_000), null, 23_415_125, "Random time 06:30:15.250 should be converted to 23 415 125 ms."));
    }

    private static Stream<Arguments> truncationEdgeCases() {
        return Stream.of(Arguments.of(LocalTime.of(0, 0, 0, 1), null, 0, "A single nanosecond should be truncated to 0 ms."), Arguments.of(LocalTime.of(0, 0, 0, 1_000_000), null, 1, "Exactly one millisecond (1 000 000 ns) should convert to 1 ms."), Arguments.of(LocalTime.of(0, 0, 0, 1_999_999), null, 1, "1 999 999 ns should be truncated to 1 ms."), Arguments.of(LocalTime.of(23, 59, 59, 999_000_000), null, 86_399_999, "Exact maximum whole‑millisecond value of the day (23:59:59.999) → 86 399 999 ms."), Arguments.of(LocalTime.of(0, 0, 0, 999_999_999), null, 999, "999 999 999 ns (just under one second) should truncate to 999 ms."));
    }

    private static Stream<Arguments> converterVariations() {
        // Create a non‑null Converter once for reuse.
        Converter nonNull = new Converter(new ConverterOptions());
        return Stream.of(Arguments.of(LocalTime.of(2, 30), nonNull, 9_000_000, "Non‑null Converter supplied; 2 h 30 m → 9 000 000 ms."), Arguments.of(LocalTime.of(5, 45, 30), nonNull, 20_730_000, "Non‑null Converter supplied; 5 h 45 m 30 s → 20 730 000 ms."));
    }

    private static Stream<Arguments> exceptionCases() {
        return Stream.of(Arguments.of(null, null, NullPointerException.class, "Passing null `from` should raise NullPointerException."), Arguments.of("not a LocalTime", null, ClassCastException.class, "Passing a non‑LocalTime object should raise ClassCastException."), // This case verifies that a null converter does NOT cause an exception.
        Arguments.of(LocalTime.of(1, 1), null, null, "Providing null converter should not affect conversion."));
    }

    @Test
    void toInteger_midnight_returnsZero() {
        // given
        // 00:00:00.000
        LocalTime time = LocalTime.MIDNIGHT;
        // when
        int result = LocalTimeConversions.toInteger(time, converter);
        // then
        assertEquals(0, result, "Midnight should be converted to 0 milliseconds");
    }

    @Test
    void toInteger_oneSecond_returnsOneThousand() {
        // given
        // 00:00:01.000
        LocalTime time = LocalTime.of(0, 0, 1, 0);
        // when
        int result = LocalTimeConversions.toInteger(time, converter);
        // then
        assertEquals(1_000, result, "One second should be 1,000 milliseconds");
    }

    @Test
    void toInteger_complexTime_returnsCorrectMilliseconds() {
        // given
        // 12:34:56.789
        LocalTime time = LocalTime.of(12, 34, 56, 789_000_000);
        // Expected: (12*60*60 + 34*60 + 56) seconds = 45296 seconds
        // Milliseconds = 45296_000 + 789 = 45_296_789
        int expected = 45_296_789;
        // when
        int result = LocalTimeConversions.toInteger(time, converter);
        // then
        assertEquals(expected, result, "12:34:56.789 should be converted to 45,296,789 milliseconds");
    }

    @Test
    void toInteger_nullSource_throwsNullPointerException() {
        // given
        Object nullSource = null;
        // when / then
        assertThrows(NullPointerException.class, () -> LocalTimeConversions.toInteger(nullSource, converter), "Passing null should result in NullPointerException");
    }

    @Test
    void toInteger_wrongType_throwsClassCastException() {
        // given
        Object notATime = "10:15:30";
        // when / then
        assertThrows(ClassCastException.class, () -> LocalTimeConversions.toInteger(notATime, converter), "Passing a non‑LocalTime object should result in ClassCastException");
    }

    /* --------------------------------------------------------------
     *  Standard conversion cases
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("standardConversions")
    void toInteger_standardConversions(LocalTime from, Converter conv, int expected, String description) {
        int result = LocalTimeConversions.toInteger(from, conv);
        assertEquals(expected, result, description);
    }

    /* --------------------------------------------------------------
     *  Truncation and rounding edge cases
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("truncationEdgeCases")
    void toInteger_truncationEdgeCases(LocalTime from, Converter conv, int expected, String description) {
        int result = LocalTimeConversions.toInteger(from, conv);
        assertEquals(expected, result, description);
    }

    /* --------------------------------------------------------------
     *  Converter argument variations (null vs non‑null)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("converterVariations")
    void toInteger_converterVariations(LocalTime from, Converter conv, int expected, String description) {
        int result = LocalTimeConversions.toInteger(from, conv);
        assertEquals(expected, result, description);
    }

    /* --------------------------------------------------------------
     *  Overflow behavior – result exceeds Integer.MAX_VALUE and wraps
     * -------------------------------------------------------------- */
    @Test
    void toInteger_overflowBehavior() {
        // LocalTime.MAX = 23:59:59.999999999
        LocalTime max = LocalTime.MAX;
        int result = LocalTimeConversions.toInteger(max, null);
        // Expected: (86_399_999_999 ms) cast to int => 500_654_079
        assertEquals(500_654_079, result, "LocalTime.MAX should overflow and wrap to 500 654 079.");
    }

    /* --------------------------------------------------------------
     *  Exception handling cases
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("exceptionCases")
    void toInteger_exceptionHandling(Object from, Converter conv, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> LocalTimeConversions.toInteger(from, conv), description);
    }

    // Helper method to handle the third case where no exception is expected.
    @Test
    void toInteger_nullConverterDoesNotThrow() {
        LocalTime time = LocalTime.of(1, 1);
        // No exception expected – just verify the conversion result.
        // 1 hour 1 minute = 3 660 000 ms
        int expected = 3_660_000;
        int result = LocalTimeConversions.toInteger(time, null);
        assertEquals(expected, result, "Conversion should succeed even when converter is null.");
    }
}
