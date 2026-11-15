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
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class OffsetTimeConversions_toString_Test {

    @Mock
    private Converter // not used by the method but supplied for signature compliance
    mockConverter;

    private static Stream<Arguments> validCases() {
        return Stream.of(// Standard OffsetTime without fractional seconds
        Arguments.of(OffsetTime.of(13, 45, 30, 0, ZoneOffset.ofHours(2)), null, "13:45:30+02:00", "Standard OffsetTime without fractional seconds; should be formatted using ISO_OFFSET_TIME."), // OffsetTime with nanosecond precision, UTC (Z)
        Arguments.of(OffsetTime.of(13, 45, 30, 123_456_789, ZoneOffset.UTC), null, "13:45:30.123456789Z", "OffsetTime with nanosecond precision; ISO formatter must include the fractional part and the 'Z' designator."), // Negative hour‑minute offset
        Arguments.of(OffsetTime.of(0, 0, 0, 0, ZoneOffset.ofHoursMinutes(-5, -30)), null, "00:00:00-05:30", "OffsetTime with a negative hour‑minute offset; verifies correct handling of negative zones."), // Maximum time of the day with maximum nanoseconds
        Arguments.of(OffsetTime.of(23, 59, 59, 999_999_999, ZoneOffset.ofHours(0)), null, "23:59:59.999999999Z", "Maximum time of the day with maximum nanoseconds; ensures formatter does not truncate trailing digits."), // Valid OffsetTime with a non‑null Converter (ignored by the method)
        Arguments.of(OffsetTime.of(12, 0, 0, 0, ZoneOffset.ofHoursMinutes(0, 0)), new Converter(), "12:00:00Z", "Valid OffsetTime with a non‑null Converter (which is ignored by the method)."));
    }

    private static Stream<Arguments> invalidCases() {
        return Stream.of(// Null `from` argument
        Arguments.of(null, null, NullPointerException.class, "Passing a null `from` argument should cause a NullPointerException because the method unconditionally casts to OffsetTime."), // Object that is not an OffsetTime (e.g., String)
        Arguments.of("2023-01-01T12:00:00Z", null, ClassCastException.class, "Providing an object that is not an OffsetTime (e.g., a String) must result in a ClassCastException due to the unchecked cast."));
    }

    @Test
    void toString_formatsOffsetTimeUsingIsoOffsetTime() {
        // GIVEN
        OffsetTime offsetTime = OffsetTime.of(13, 45, 30, 123_456_789, ZoneOffset.ofHoursMinutes(2, 30));
        // WHEN
        String result = OffsetTimeConversions.toString(offsetTime, mockConverter);
        // THEN
        // Expected format: HH:mm:ss.SSSSSSSSS+02:30 (ISO_OFFSET_TIME)
        assertEquals("13:45:30.123456789+02:30", result);
    }

    @Test
    void toString_nullInput_throwsNullPointerException() {
        // GIVEN
        Object nullObj = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> OffsetTimeConversions.toString(nullObj, mockConverter));
    }

    /* -------------------------------------------------------------
     *  Valid cases – the method should return the ISO‑8601 representation
     *  of the supplied OffsetTime, regardless of the Converter argument.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("validCases")
    void toString_ValidCases(OffsetTime from, Converter converter, String expected, String description) {
        // WHEN
        String result = OffsetTimeConversions.toString(from, converter);
        // THEN
        assertEquals(expected, result, description);
    }

    /* -------------------------------------------------------------
     *  Null / Invalid cases – the method is expected to throw.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("invalidCases")
    void toString_NullAndInvalidCases(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
        // WHEN / THEN
        assertThrows(expectedException, () -> OffsetTimeConversions.toString(from, converter), description);
    }
}
