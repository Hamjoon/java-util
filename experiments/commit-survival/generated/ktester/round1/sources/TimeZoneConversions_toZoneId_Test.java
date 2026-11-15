package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.ZoneId;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.Date;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TimeZoneConversions_toZoneId_Test {

    /**
     * Mocked dependency – not used by the method but required by the signature **
     */
    @Mock
    private Converter mockConverter;

    private static Stream<Arguments> validTimeZoneProvider() {
        return Stream.of(// UTC – the JDK maps "UTC" to the short ID "Z"
        Arguments.of(TimeZone.getTimeZone("UTC"), ZoneId.of("Z")), // Named region
        Arguments.of(TimeZone.getTimeZone("America/New_York"), ZoneId.of("America/New_York")), // GMT offset style
        Arguments.of(TimeZone.getTimeZone("GMT+02:00"), ZoneId.of("GMT+02:00")));
    }

    private static Stream<Arguments> invalidObjectProvider() {
        return Stream.of(Arguments.of("not a TimeZone"), Arguments.of(new Date()));
    }

    /**
     * GIVEN a valid {@link TimeZone} instance,
     * WHEN {@code TimeZoneConversions.toZoneId} is invoked,
     * THEN the corresponding {@link ZoneId} is returned.
     */
    @Test
    void toZoneId_returnsCorrespondingZoneId() {
        // GIVEN
        TimeZone tz = TimeZone.getTimeZone("America/New_York");
        // WHEN
        ZoneId result = TimeZoneConversions.toZoneId(tz, mockConverter);
        // THEN
        assertNotNull(result, "Resulting ZoneId should not be null");
        assertEquals(ZoneId.of("America/New_York"), result, "ZoneId should match the ID of the supplied TimeZone");
    }

    /**
     * GIVEN an object that is *not* a {@link TimeZone},
     * WHEN {@code TimeZoneConversions.toZoneId} is invoked,
     * THEN a {@link ClassCastException} is thrown.
     */
    @Test
    void toZoneId_withInvalidObject_throwsClassCastException() {
        // GIVEN
        Object notATimeZone = new Object();
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> TimeZoneConversions.toZoneId(notATimeZone, mockConverter), "Calling toZoneId with a non‑TimeZone object should throw ClassCastException");
    }

    /**
     * GIVEN a {@link TimeZone} instance,
     * WHEN {@code TimeZoneConversions.toZoneId} is invoked,
     * THEN the corresponding {@link ZoneId} is returned.
     *
     * Covers the three scenarios from the JSON:
     *   • UTC (expected ZoneId "Z")
     *   • America/New_York
     *   • GMT+02:00 offset
     */
    @ParameterizedTest(name = "{index} ⇒ from={0}, expected={1}")
    @MethodSource("validTimeZoneProvider")
    void toZoneId_validTimeZone_returnsCorrespondingZoneId(TimeZone from, ZoneId expected) {
        // WHEN
        // converter not used
        ZoneId result = TimeZoneConversions.toZoneId(from, null);
        // THEN
        assertNotNull(result, "Resulting ZoneId should not be null");
        assertEquals(expected, result, () -> "ZoneId should match expected value for TimeZone ID '" + from.getID() + "'");
    }

    /**
     * GIVEN a {@code null} {@code from} argument,
     * WHEN {@code TimeZoneConversions.toZoneId} is invoked,
     * THEN a {@link NullPointerException} is thrown.
     */
    @Test
    void toZoneId_nullFrom_throwsNullPointerException() {
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> TimeZoneConversions.toZoneId(null, null), "Passing null as the 'from' argument should throw NullPointerException");
    }

    /**
     * GIVEN an object that is not a {@link TimeZone},
     * WHEN {@code TimeZoneConversions.toZoneId} is invoked,
     * THEN a {@link ClassCastException} is thrown.
     *
     * Covers both a plain {@link String} and a {@link Date} instance.
     */
    @ParameterizedTest(name = "{index} ⇒ from={0}")
    @MethodSource("invalidObjectProvider")
    void toZoneId_invalidObject_throwsClassCastException(Object from) {
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> TimeZoneConversions.toZoneId(from, null), "Calling toZoneId with a non‑TimeZone object should throw ClassCastException");
    }
}
