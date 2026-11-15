package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.LocalDate;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalTimeConversions_toLong_Test {

    @Mock
    private Converter // Converter is not used by toLong, but we provide a mock.
    mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Mockito will inject the mockConverter field automatically.
        // Ensure there are no unexpected interactions before each test.
        clearInvocations(mockConverter);
    }

    @AfterEach
    void teardownAfterEach() {
        // Verify that the converter mock was not interacted with, as the method should ignore it.
        verifyNoInteractions(mockConverter);
    }

    /**
     * Provides a stream of arguments for successful conversions.
     * Each argument consists of:
     *   1. The {@link LocalTime} instance to convert.
     *   2. The expected millisecond value.
     *   3. A readable description (used as the display name).
     */
    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Valid LocalTime conversions (from the "Valid LocalTime conversions" group)
        Arguments.of(LocalTime.parse("01:02:03.004005006"), 3_723_004L, "Typical time with nanosecond precision: 01:02:03.004005006 → 3,723,004 ms"), Arguments.of(LocalTime.parse("00:00:00"), 0L, "Midnight should return 0 ms"), Arguments.of(LocalTime.parse("23:59:59.999999999"), 86_399_999_999L, "Last possible nanosecond of the day, truncating to 86,399,999,999 ms"), Arguments.of(LocalTime.parse("12:34:56.789"), 45_296_789L, "Time with millisecond precision only: 12:34:56.789 → 45,296,789 ms"), // Boundary rounding behaviour (from the "Boundary rounding behavior" group)
        Arguments.of(LocalTime.parse("00:00:00.000000001"), 0L, "One nanosecond after midnight truncates to 0 ms"), Arguments.of(LocalTime.parse("00:00:00.001"), 1L, "Exactly one millisecond after midnight → 1 ms"), Arguments.of(LocalTime.parse("00:00:00.999999999"), 999L, "Just under one second truncates to 999 ms"), Arguments.of(LocalTime.parse("12:00:00.500000000"), 43_200_500L, "Half a second past noon → 43,200,500 ms"));
    }

    /**
     * Provides arguments that should cause the method to throw.
     * Each argument consists of:
     *   1. The problematic input object.
     *   2. The expected exception class.
     *   3. A description for the test display name.
     */
    private static Stream<Arguments> invalidInputProvider() {
        return Stream.of(// Null input – should raise NPE when casting.
        Arguments.of(null, NullPointerException.class, "Null 'from' argument should throw NullPointerException"), // Wrong type – String instead of LocalTime.
        Arguments.of("13:45:30", ClassCastException.class, "String input should cause ClassCastException"), // Wrong type – LocalDate instead of LocalTime.
        Arguments.of(LocalDate.now(), ClassCastException.class, "LocalDate input should cause ClassCastException"));
    }

    @Test
    void toLong_convertsTypicalLocalTimeToMilliseconds() {
        // given
        // 12:34:56.789
        LocalTime time = LocalTime.of(12, 34, 56, 789_000_000);
        // calculate expected value
        long expectedMillis = time.toNanoOfDay() / 1_000_000L;
        // when
        long actual = LocalTimeConversions.toLong(time, mockConverter);
        // then
        assertEquals(expectedMillis, actual, "Conversion of typical LocalTime should yield correct milliseconds");
    }

    @Test
    void toLong_midnightReturnsZero() {
        // given
        LocalTime midnight = LocalTime.MIDNIGHT;
        long expected = 0L;
        // when
        long actual = LocalTimeConversions.toLong(midnight, mockConverter);
        // then
        assertEquals(expected, actual, "Midnight should convert to 0 milliseconds");
    }

    @Test
    void toLong_maxTimeRoundsDownToMilliseconds() {
        // given
        // 23:59:59.999999999
        LocalTime max = LocalTime.MAX;
        // should be 86_399_999 ms
        long expected = max.toNanoOfDay() / 1_000_000L;
        // when
        long actual = LocalTimeConversions.toLong(max, mockConverter);
        // then
        assertEquals(expected, actual, "Maximum LocalTime should be converted to milliseconds, truncating sub‑millisecond part");
    }

    @Test
    void toLong_nullFromThrowsNullPointerException() {
        // given
        Object nullObj = null;
        // when / then
        assertThrows(NullPointerException.class, () -> LocalTimeConversions.toLong(nullObj, mockConverter), "Passing null as the source object should throw NullPointerException");
    }

    @Test
    void toLong_ignoresConverterParameter() {
        // given
        LocalTime time = LocalTime.of(1, 2, 3, 0);
        // when
        long result = LocalTimeConversions.toLong(time, mockConverter);
        // then
        assertEquals(time.toNanoOfDay() / 1_000_000L, result);
        // Interaction verification is performed in @AfterEach via verifyNoInteractions.
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("validConversionProvider")
    void toLong_convertsLocalTimeCorrectly(LocalTime from, long expectedMillis, String description) {
        // When
        long actual = LocalTimeConversions.toLong(from, mockConverter);
        // Then
        assertEquals(expectedMillis, actual, description);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("invalidInputProvider")
    void toLong_invalidInputs_throwException(Object from, Class<? extends Throwable> expectedException, String description) {
        // When / Then
        assertThrows(expectedException, () -> LocalTimeConversions.toLong(from, mockConverter), description);
    }

    @Test
    void toLong_usesRealConverterButIgnoresIt() {
        // given: maximum LocalTime and a real (non‑mock) Converter instance
        LocalTime max = LocalTime.of(23, 59, 59, 999_999_999);
        // anonymous subclass, no behaviour needed
        Converter realConverter = new Converter() {
        };
        // 86_399_999_999
        long expected = max.toNanoOfDay() / 1_000_000L;
        // when
        long actual = LocalTimeConversions.toLong(max, realConverter);
        // then
        assertEquals(expected, actual, "Even with a real Converter supplied, conversion should produce correct milliseconds");
        // No verification of interactions is needed for the real converter.
    }
}
