package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.ZoneOffset;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.DateTimeException;
import java.util.SimpleTimeZone;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TimeZoneConversions_toZoneOffset_Test {

    @Mock
    private Converter // Converter is not used by the method, a mock is sufficient
    mockConverter;

    /**
     * Provides a stream of arguments: a {@link TimeZone} instance and the
     * expected {@link ZoneOffset} string representation.
     */
    private static Stream<Arguments> validOffsetProvider() {
        return Stream.of(// Zero offset (UTC)
        Arguments.of(new SimpleTimeZone(0, "UTC"), "Z"), // Positive whole‑hour offset (+1 hour)
        Arguments.of(new SimpleTimeZone(3_600_000, "GMT+1"), "+01:00"), // Negative whole‑hour offset (‑2 hours)
        Arguments.of(new SimpleTimeZone(-7_200_000, "GMT-2"), "-02:00"), // Positive non‑standard offset (33 minutes)
        Arguments.of(new SimpleTimeZone(1_980_000, "GMT+00:33"), "+00:33"), // Negative non‑standard offset (‑1 hour 30 minutes)
        Arguments.of(new SimpleTimeZone(-5_400_000, "GMT-01:30"), "-01:30"), // Raw offset not a whole second (1234 ms → 1 s after truncation)
        Arguments.of(new SimpleTimeZone(1_234, "CUSTOM_MS"), "+00:00:01"));
    }

    @Test
    void toZoneOffset_Utc_ReturnsUtcOffset() {
        // given
        TimeZone tz = TimeZone.getTimeZone("UTC");
        // when
        ZoneOffset result = TimeZoneConversions.toZoneOffset(tz, mockConverter);
        // then
        assertEquals(ZoneOffset.UTC, result);
    }

    @Test
    void toZoneOffset_PositiveOffset_ReturnsCorrectZoneOffset() {
        // given: GMT+02:30 (2 hours 30 minutes = 9,000 seconds)
        TimeZone tz = TimeZone.getTimeZone("GMT+02:30");
        // when
        ZoneOffset result = TimeZoneConversions.toZoneOffset(tz, mockConverter);
        // then
        ZoneOffset expected = ZoneOffset.ofHoursMinutes(2, 30);
        assertEquals(expected, result);
    }

    @Test
    void toZoneOffset_NegativeOffset_ReturnsCorrectZoneOffset() {
        // given: GMT-05:00 (−5 hours = −18,000 seconds)
        TimeZone tz = TimeZone.getTimeZone("GMT-05:00");
        // when
        ZoneOffset result = TimeZoneConversions.toZoneOffset(tz, mockConverter);
        // then
        ZoneOffset expected = ZoneOffset.ofHours(-5);
        assertEquals(expected, result);
    }

    @Test
    void toZoneOffset_ZeroOffset_ReturnsZero() {
        // given: GMT+00:00 explicitly
        TimeZone tz = TimeZone.getTimeZone("GMT+00:00");
        // when
        ZoneOffset result = TimeZoneConversions.toZoneOffset(tz, mockConverter);
        // then
        assertEquals(ZoneOffset.UTC, result);
    }

    @Test
    void toZoneOffset_HourAndMinuteOffset_ReturnsExactSeconds() {
        // given: GMT+01:45 (1 hour 45 minutes = 6300 seconds)
        TimeZone tz = TimeZone.getTimeZone("GMT+01:45");
        // when
        ZoneOffset result = TimeZoneConversions.toZoneOffset(tz, mockConverter);
        // then
        ZoneOffset expected = ZoneOffset.ofHoursMinutes(1, 45);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] {1} offset")
    @MethodSource("validOffsetProvider")
    @DisplayName("toZoneOffset – valid offsets produce expected ZoneOffset")
    void toZoneOffset_ValidOffsets(TimeZone tz, String expectedString) {
        // when
        ZoneOffset result = TimeZoneConversions.toZoneOffset(tz, mockConverter);
        // then – compare the string form (e.g., \"Z\", \"+01:00\", \"+00:00:01\")
        assertEquals(expectedString, result.toString(), () -> "Expected ZoneOffset string \"" + expectedString + "\" but got \"" + result + "\"");
    }

    @Test
    @DisplayName("toZoneOffset – offset > +18:00 throws DateTimeException")
    void toZoneOffset_TooPositive_ThrowsDateTimeException() {
        // ≈ +19:26
        TimeZone tz = new SimpleTimeZone(70_000_000, "TOO_POSITIVE");
        assertThrows(DateTimeException.class, () -> TimeZoneConversions.toZoneOffset(tz, mockConverter), "Expected DateTimeException for offset greater than +18:00");
    }

    @Test
    @DisplayName("toZoneOffset – offset < -18:00 throws DateTimeException")
    void toZoneOffset_TooNegative_ThrowsDateTimeException() {
        // ≈ -19:26
        TimeZone tz = new SimpleTimeZone(-70_000_000, "TOO_NEGATIVE");
        assertThrows(DateTimeException.class, () -> TimeZoneConversions.toZoneOffset(tz, mockConverter), "Expected DateTimeException for offset less than -18:00");
    }

    @Test
    @DisplayName("toZoneOffset – null source object throws NullPointerException")
    void toZoneOffset_NullInput_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> TimeZoneConversions.toZoneOffset(null, mockConverter), "Casting null to TimeZone should raise NullPointerException");
    }

    @Test
    @DisplayName("toZoneOffset – non‑TimeZone input throws ClassCastException")
    void toZoneOffset_InvalidType_ThrowsClassCastException() {
        String notATimeZone = "not a TimeZone";
        assertThrows(ClassCastException.class, () -> TimeZoneConversions.toZoneOffset(notATimeZone, mockConverter), "Casting a non‑TimeZone object should raise ClassCastException");
    }
}
