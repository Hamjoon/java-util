package com.cedarsoftware.util.convert;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.Instant;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.DateTimeException;
import java.time.zone.ZoneRules;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class ZoneIdConversions_toZoneOffset_Test {

    static Stream<Arguments> validZoneIdProvider() {
        return Stream.of(// UTC → zero offset
        Arguments.of(ZoneId.of("UTC")), // region with DST
        Arguments.of(ZoneId.of("America/New_York")), // fixed offset (+05:30)
        Arguments.of(ZoneOffset.ofHoursMinutes(5, 30)));
    }

    static Stream<Arguments> additionalZoneIdProvider() {
        return Stream.of(// +05:30 fixed offset
        Arguments.of(ZoneId.of("Asia/Kolkata")), // DST aware
        Arguments.of(ZoneId.of("Europe/Paris")), // negative fixed offset (GMT‑3)
        Arguments.of(ZoneId.of("Etc/GMT+3")), // +02:00 fixed offset
        Arguments.of(ZoneOffset.ofHours(2)));
    }

    /**
     * Verify that a UTC {@link ZoneId} yields {@link ZoneOffset#UTC}.
     */
    @Test
    void toZoneOffset_UtcReturnsZeroOffset() {
        // given
        ZoneId utcZone = ZoneId.of("UTC");
        // converter is not used
        Converter mockConverter = mock(Converter.class);
        // when
        ZoneOffset actual = ZoneIdConversions.toZoneOffset(utcZone, mockConverter);
        // then
        assertEquals(ZoneOffset.UTC, actual, "UTC zone should always produce a zero offset");
    }

    /**
     * Verify that a non‑UTC zone returns the same offset that the JDK
     * calculates for the current instant.
     */
    @Test
    void toZoneOffset_NonUtcMatchesJdkCalculation() {
        // given
        ZoneId newYork = ZoneId.of("America/New_York");
        // converter is not used
        Converter mockConverter = mock(Converter.class);
        // capture the instant used for expectation
        Instant now = Instant.now();
        // when
        ZoneOffset actual = ZoneIdConversions.toZoneOffset(newYork, mockConverter);
        // then
        ZoneOffset expected = newYork.getRules().getOffset(now);
        assertEquals(expected, actual, "Offset returned by toZoneOffset must match JDK calculation for the same instant");
    }

    /**
     * Verify that passing a {@link ZoneId} that is not a standard ID (e.g., a fixed offset)
     * still yields the correct offset.
     */
    @Test
    void toZoneOffset_FixedOffsetZone() {
        // given
        // +05:30
        ZoneOffset fixed = ZoneOffset.ofHoursMinutes(5, 30);
        // ZoneOffset implements ZoneId
        ZoneId fixedZone = fixed;
        Converter mockConverter = mock(Converter.class);
        // when
        ZoneOffset actual = ZoneIdConversions.toZoneOffset(fixedZone, mockConverter);
        // then
        // For a fixed offset zone the offset is constant regardless of instant
        assertEquals(fixed, actual, "A fixed offset ZoneId should return its own offset");
    }

    /**
     * Verify that a {@code null} {@code from} argument results in a {@code NullPointerException}
     * because the implementation casts directly to {@link ZoneId}.
     */
    @Test
    void toZoneOffset_NullFromThrowsNpe() {
        // given
        Converter mockConverter = mock(Converter.class);
        // when / then
        assertThrows(NullPointerException.class, () -> ZoneIdConversions.toZoneOffset(null, mockConverter), "Passing null as the source object should throw NPE");
    }

    /* --------------------------------------------------------------
     *  Valid ZoneId conversions (including the original three cases)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] ZoneId: {0}")
    @MethodSource("validZoneIdProvider")
    void toZoneOffset_ValidZoneIds_ReturnExpectedOffset(ZoneId zone) {
        // given
        // not used
        Converter mockConverter = mock(Converter.class);
        // capture instant for expected value
        Instant now = Instant.now();
        // when
        ZoneOffset actual = ZoneIdConversions.toZoneOffset(zone, mockConverter);
        // then
        ZoneOffset expected = zone.getRules().getOffset(now);
        assertEquals(expected, actual, () -> "Offset for zone " + zone + " should match JDK calculation");
    }

    /* --------------------------------------------------------------
     *  Additional ZoneId variations (Asia/Kolkata, Europe/Paris, Etc/GMT+3, +02:00)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] ZoneId: {0}")
    @MethodSource("additionalZoneIdProvider")
    void toZoneOffset_AdditionalZoneIds_ReturnExpectedOffset(ZoneId zone) {
        // given
        Converter mockConverter = mock(Converter.class);
        Instant now = Instant.now();
        // when
        ZoneOffset actual = ZoneIdConversions.toZoneOffset(zone, mockConverter);
        // then
        ZoneOffset expected = zone.getRules().getOffset(now);
        assertEquals(expected, actual, () -> "Offset for zone " + zone + " should match JDK calculation");
    }

    @Test
    void toZoneOffset_IncompatibleFromThrowsClassCastException() {
        // given
        Converter mockConverter = mock(Converter.class);
        String notAZone = "not a zone";
        // when / then
        assertThrows(ClassCastException.class, () -> ZoneIdConversions.toZoneOffset(notAZone, mockConverter), "Passing an object that is not a ZoneId should cause ClassCastException");
    }

    /* --------------------------------------------------------------
     *  Converter parameter usage (non‑null Converter)
     * -------------------------------------------------------------- */
    @Test
    void toZoneOffset_NonNullConverterIsIgnored() {
        // given
        ZoneId utc = ZoneId.of("UTC");
        // create a concrete (anonymous) instance of Converter – the class is empty in the library
        Converter realConverter = new Converter() {
        };
        // when
        ZoneOffset actual = ZoneIdConversions.toZoneOffset(utc, realConverter);
        // then
        assertEquals(ZoneOffset.UTC, actual, "Converter argument is ignored; result must be same as with a mock");
    }

    /* --------------------------------------------------------------
     *  Custom ZoneId implementations causing internal errors
     * -------------------------------------------------------------- */
    @Test
    void toZoneOffset_CustomZoneIdWithNullRules_ThrowsNpe() {
        // given
        ZoneId custom = mock(ZoneId.class);
        when(custom.getRules()).thenReturn(null);
        when(custom.getId()).thenReturn("CustomNullRules");
        Converter mockConverter = mock(Converter.class);
        // when / then
        assertThrows(NullPointerException.class, () -> ZoneIdConversions.toZoneOffset(custom, mockConverter), "Custom ZoneId with null rules should cause NPE when getOffset is invoked");
    }

    @Test
    void toZoneOffset_CustomZoneIdWithThrowingRules_PropagatesDateTimeException() {
        // given
        ZoneId custom = mock(ZoneId.class);
        when(custom.getId()).thenReturn("CustomThrowingRules");
        ZoneRules throwingRules = mock(ZoneRules.class);
        when(throwingRules.getOffset(any(Instant.class))).thenThrow(new DateTimeException("forced exception"));
        when(custom.getRules()).thenReturn(throwingRules);
        Converter mockConverter = mock(Converter.class);
        // when / then
        assertThrows(DateTimeException.class, () -> ZoneIdConversions.toZoneOffset(custom, mockConverter), "DateTimeException from custom ZoneRules should be propagated");
    }
}
