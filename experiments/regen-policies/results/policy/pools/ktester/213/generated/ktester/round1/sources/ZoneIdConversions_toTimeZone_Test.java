package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.ZoneId;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.ZoneOffset;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ZoneIdConversions_toTimeZone_Test {

    @Mock
    private Converter // not used by the method but required by signature
    mockConverter;

    /**
     * Provides a stream of valid inputs together with the expected TimeZone ID.
     * The third argument indicates whether the test should supply a non‑null
     * {@link Converter} (the mock instance) or {@code null}.
     */
    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Standard region‑based ZoneId
        Arguments.of(ZoneId.of("America/New_York"), "America/New_York", false, "Converts a standard region‑based ZoneId to a matching TimeZone (converter = null)"), // ZoneOffset works because it implements ZoneId
        Arguments.of(ZoneOffset.ofHours(-5), "GMT-05:00", true, "Converts a ZoneOffset (UTC‑5) to GMT‑style TimeZone (converter = mock)"), // UTC edge case
        Arguments.of(ZoneId.of("UTC"), "UTC", false, "Edge case where the ZoneId is the universal UTC identifier (converter = null)"), // Positive offset with minutes (India Standard Time)
        Arguments.of(ZoneOffset.ofHoursMinutes(5, 30), "GMT+05:30", false, "Converts ZoneOffset +05:30 to GMT‑style identifier (converter = null)"), // Negative offset with minutes
        Arguments.of(ZoneOffset.ofHoursMinutes(-3, -30), "GMT-03:30", true, "Converts ZoneOffset -03:30 to GMT‑style identifier (converter = mock)"), // Offset defined by total seconds (90 s = 1 min 30 s)
        Arguments.of(ZoneOffset.ofTotalSeconds(90), "GMT+00:01:30", false, "Converts ZoneOffset of 90 seconds to GMT‑style identifier (converter = null)"));
    }

    /**
     * Provides a stream of invalid inputs together with the expected exception type.
     * The third argument controls whether a non‑null {@link Converter} is supplied.
     */
    private static Stream<Arguments> invalidInputProvider() {
        return Stream.of(// Null argument – the cast triggers NPE
        Arguments.of(null, NullPointerException.class, false, "Null 'from' argument should cause NullPointerException (converter = null)"), // Wrong type – String does not implement ZoneId
        Arguments.of("Europe/Paris", ClassCastException.class, false, "String argument should cause ClassCastException (converter = null)"), // Wrong type – generic Object
        Arguments.of(new Object(), ClassCastException.class, true, "Object argument should cause ClassCastException (converter = mock)"));
    }

    @Test
    void toTimeZone_WithValidZoneId_ReturnsCorrespondingTimeZone() {
        // given
        ZoneId zoneId = ZoneId.of("America/New_York");
        // when
        TimeZone tz = ZoneIdConversions.toTimeZone(zoneId, mockConverter);
        // then
        assertNotNull(tz, "Resulting TimeZone should not be null");
        // TimeZone.getTimeZone(ZoneId) returns a TimeZone with the same ID as the ZoneId
        assertEquals(zoneId.getId(), tz.getID(), "TimeZone ID should match the original ZoneId ID");
    }

    @Test
    void toTimeZone_WithUtcZoneId_ReturnsUtcTimeZone() {
        // given
        ZoneId utc = ZoneId.of("UTC");
        // when
        TimeZone tz = ZoneIdConversions.toTimeZone(utc, mockConverter);
        // then
        assertNotNull(tz);
        assertEquals("UTC", tz.getID());
    }

    @Test
    void toTimeZone_WithNullZoneId_ThrowsNullPointerException() {
        // given
        Object nullObj = null;
        // when / then
        assertThrows(NullPointerException.class, () -> ZoneIdConversions.toTimeZone(nullObj, mockConverter), "Passing null should result in NullPointerException");
    }

    @Test
    void toTimeZone_WithInvalidType_ThrowsClassCastException() {
        // given
        Object notAZoneId = "not-a-zone-id";
        // when / then
        assertThrows(ClassCastException.class, () -> ZoneIdConversions.toTimeZone(notAZoneId, mockConverter), "Passing a non‑ZoneId object should cause ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("validConversionProvider")
    void toTimeZone_ValidInputs_ReturnsExpectedTimeZone(Object from, String expectedId, boolean useMockConverter, String description) {
        // Arrange
        Converter converter = useMockConverter ? mockConverter : null;
        // Act
        TimeZone result = ZoneIdConversions.toTimeZone(from, converter);
        // Assert
        assertNotNull(result, "Resulting TimeZone must not be null");
        assertEquals(expectedId, result.getID(), () -> "Expected TimeZone ID '" + expectedId + "' but got '" + result.getID() + "'");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("invalidInputProvider")
    void toTimeZone_InvalidInputs_ThrowExpectedException(Object from, Class<? extends Throwable> expectedException, boolean useMockConverter, String description) {
        // Arrange
        Converter converter = useMockConverter ? mockConverter : null;
        // Act & Assert
        assertThrows(expectedException, () -> ZoneIdConversions.toTimeZone(from, converter), () -> "Expected " + expectedException.getSimpleName() + " for input: " + from);
    }
}
