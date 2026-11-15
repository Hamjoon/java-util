package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.LocalDate;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class OffsetTimeConversions_toLong_Test {

    /**
     * The Converter is not used by the method under test, but it must be supplied.
     *  We provide a mock to keep the test lightweight.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Supplies the normal conversion arguments.
     */
    private static Stream<Arguments> normalConversionProvider() {
        return Stream.of(// OffsetTime at epoch (1970‑01‑01T00:00:00+00:00) → 0 ms
        Arguments.of(OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC), 0L), // 01:00 with +01:00 offset is the same instant as 00:00 UTC → 0 ms
        Arguments.of(OffsetTime.of(1, 0, 0, 0, ZoneOffset.ofHours(1)), 0L), // 00:00 with -02:00 offset corresponds to 02:00 UTC → 2 hours = 7 200 000 ms
        Arguments.of(OffsetTime.of(0, 0, 0, 0, ZoneOffset.ofHours(-2)), 7_200_000L), // 12:34:56+03:00 → UTC 09:34:56 → 34 496 000 ms
        Arguments.of(OffsetTime.of(12, 34, 56, 0, ZoneOffset.ofHours(3)), 34_496_000L));
    }

    private static Stream<Arguments> boundaryConversionProvider() {
        return Stream.of(// 23:59:59.999_999_999 UTC → 86 399 999 ms (one millisecond before next day)
        Arguments.of(OffsetTime.of(23, 59, 59, 999_999_999, ZoneOffset.UTC), 86_399_999L), // 00:00 with +14 offset → UTC 10:00 of previous day → -50_400_000 ms
        Arguments.of(OffsetTime.of(0, 0, 0, 0, ZoneOffset.ofHours(14)), -50_400_000L), // 00:00 with -12 offset → UTC 12:00 of same day → 43_200_000 ms
        Arguments.of(OffsetTime.of(0, 0, 0, 0, ZoneOffset.ofHours(-12)), 43_200_000L));
    }

    @Test
    void toLong_WithPositiveOffset_ReturnsCorrectEpochMilli() {
        // given
        OffsetTime offsetTime = OffsetTime.of(12, 34, 56, 789_000_000, ZoneOffset.ofHours(2));
        // expected: 1970‑01‑01T10:34:56.789Z  => 38_096_789 ms
        long expected = 38_096_789L;
        // when
        long actual = OffsetTimeConversions.toLong(offsetTime, mockConverter);
        // then
        assertEquals(expected, actual, "Epoch milliseconds for a positive offset should match the calculated value.");
    }

    @Test
    void toLong_WithNegativeOffset_ReturnsCorrectEpochMilli() {
        // given
        OffsetTime offsetTime = OffsetTime.of(0, 0, 0, 0, ZoneOffset.ofHours(-5));
        // expected: 1970‑01‑01T05:00:00Z  => 18_000_000 ms
        long expected = 18_000_000L;
        // when
        long actual = OffsetTimeConversions.toLong(offsetTime, mockConverter);
        // then
        assertEquals(expected, actual, "Epoch milliseconds for a negative offset should match the calculated value.");
    }

    @Test
    void toLong_WithNullSource_throwsNullPointerException() {
        // given
        OffsetTime nullOffsetTime = null;
        // when / then
        assertThrows(NullPointerException.class, () -> OffsetTimeConversions.toLong(nullOffsetTime, mockConverter), "Calling toLong with a null source should throw NullPointerException.");
    }

    @Test
    void toLong_WithInvalidType_throwsClassCastException() {
        // given
        Object notAnOffsetTime = "2023-01-01T12:00:00Z";
        // when / then
        assertThrows(ClassCastException.class, () -> OffsetTimeConversions.toLong(notAnOffsetTime, mockConverter), "Calling toLong with a non‑OffsetTime source should throw ClassCastException.");
    }

    /**
     * Parameterized test covering all “normal” conversion cases described in the
     * JSON test data under the group “Normal conversions of OffsetTime to epoch milliseconds”.
     *
     * Each argument consists of:
     * <ul>
     *   <li>the {@link OffsetTime} input</li>
     *   <li>the expected epoch‑millisecond result</li>
     *   <li>a description used as the display name for readability</li>
     * </ul>
     */
    @ParameterizedTest(name = "{2}")
    @MethodSource("normalConversionProvider")
    void toLong_NormalConversions_ReturnsExpectedEpochMilli(OffsetTime from, long expected) {
        // when
        long actual = OffsetTimeConversions.toLong(from, mockConverter);
        // then
        assertEquals(expected, actual, () -> "Epoch milliseconds for OffsetTime " + from + " should be " + expected);
    }

    /**
     * Parameterized test for boundary values that still return a long.
     * These cases are taken from the “Boundary and error handling for OffsetTime to long conversion”
     * group where the expected result is a numeric value.
     */
    @ParameterizedTest(name = "{2}")
    @MethodSource("boundaryConversionProvider")
    void toLong_BoundaryValues_ReturnsExpectedEpochMilli(OffsetTime from, long expected) {
        long actual = OffsetTimeConversions.toLong(from, mockConverter);
        assertEquals(expected, actual, () -> "Boundary epoch milliseconds for OffsetTime " + from + " should be " + expected);
    }

    /**
     * Tests that passing {@code null} as the source object results in a {@link NullPointerException}.
     */
    @Test
    void toLong_NullSource_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> OffsetTimeConversions.toLong(null, mockConverter), "Calling toLong with a null source should throw NullPointerException.");
    }

    /**
     * Tests that passing an object of an incompatible type triggers a {@link ClassCastException}.
     */
    @Test
    void toLong_WrongType_ThrowsClassCastException() {
        Object notAnOffsetTime = LocalDate.of(1970, 1, 1);
        assertThrows(ClassCastException.class, () -> OffsetTimeConversions.toLong(notAnOffsetTime, mockConverter), "Calling toLong with a non‑OffsetTime source should throw ClassCastException.");
    }
}
