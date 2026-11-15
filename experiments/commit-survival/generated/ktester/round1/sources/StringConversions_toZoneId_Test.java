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
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toZoneId_Test {

    /**
     * The Converter instance is not used by the implementation of {@code toZoneId},
     *  but the method signature requires it, so we provide a mock.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Valid identifiers that {@link ZoneId#of(String)} can parse directly.
     */
    private static Stream<Arguments> validZoneIdProvider() {
        return Stream.of(Arguments.of("America/New_York", ZoneId.of("America/New_York")), Arguments.of("UTC", ZoneId.of("UTC")), Arguments.of("+02:00", ZoneId.of("+02:00")), Arguments.of("GMT+2", ZoneId.of("GMT+2")));
    }

    /**
     * Legacy three‑letter or POSIX identifiers that are not understood by {@code ZoneId.of},
     * but are resolved by {@link TimeZone#getTimeZone(String)}.
     */
    private static Stream<Arguments> legacyTimeZoneProvider() {
        return Stream.of(Arguments.of("EST", TimeZone.getTimeZone("EST").toZoneId()), Arguments.of("PST8PDT", TimeZone.getTimeZone("PST8PDT").toZoneId()));
    }

    /**
     * Inputs that should cause {@link IllegalArgumentException} because the fallback
     * {@link TimeZone} resolves to the GMT zone (meaning the identifier is unknown).
     */
    private static Stream<String> illegalArgumentProvider() {
        return Stream.of("GMT", "Invalid/Zone", // whitespace only
        "   ", // case‑sensitive mismatch
        "america/new_york", "GMT+00:00", "GMT-00:00", "GMT0", "GMT+0", "GMT-0", "   GMT   ");
    }

    @Test
    void toZoneId_givenEmptyString_returnsNull() {
        // GIVEN
        String empty = "";
        // WHEN
        ZoneId result = StringConversions.toZoneId(empty, mockConverter);
        // THEN
        assertNull(result, "Empty string should result in null ZoneId");
    }

    @Test
    void toZoneId_givenValidZoneIdString_returnsCorrespondingZoneId() {
        // GIVEN
        String zone = "America/New_York";
        // WHEN
        ZoneId result = StringConversions.toZoneId(zone, mockConverter);
        // THEN
        assertNotNull(result, "Result should not be null for a valid zone ID");
        assertEquals(ZoneId.of(zone), result, "Returned ZoneId must match the expected value");
    }

    @Test
    void toZoneId_givenTimeZoneString_returnsTimeZoneConvertedToZoneId() {
        // GIVEN
        // "GMT+2" is not a ZoneId identifier but is understood by java.util.TimeZone
        String tzString = "GMT+2";
        TimeZone tz = TimeZone.getTimeZone(tzString);
        // Sanity‑check that the TimeZone is not the fallback "GMT"
        assertNotEquals("GMT", tz.getID(), "TimeZone.getTimeZone should not fall back to GMT for '" + tzString + "'");
        // WHEN
        ZoneId result = StringConversions.toZoneId(tzString, mockConverter);
        // THEN
        assertNotNull(result, "Result should not be null for a recognizable TimeZone string");
        assertEquals(tz.toZoneId(), result, "Returned ZoneId should be the one derived from the TimeZone");
    }

    @Test
    void toZoneId_givenUnknownZoneId_throwsIllegalArgumentException() {
        // GIVEN
        String unknown = "this/does_not_exist";
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toZoneId(unknown, mockConverter), "An unknown zone ID should cause IllegalArgumentException");
        // Verify that the message contains the problematic identifier
        assertTrue(ex.getMessage().contains(unknown), "Exception message should contain the unknown zone ID");
        // The cause should be the original exception from ZoneId.of()
        assertNotNull(ex.getCause(), "Exception should have a cause");
    }

    @ParameterizedTest(name = "toZoneId_validZoneId ''{0}'' returns expected ZoneId")
    @MethodSource("validZoneIdProvider")
    void toZoneId_validZoneId_returnsExpected(String input, ZoneId expected) {
        ZoneId result = StringConversions.toZoneId(input, mockConverter);
        assertEquals(expected, result, () -> "Expected ZoneId.of(\"" + input + "\") but got a different value");
    }

    @ParameterizedTest(name = "toZoneId_legacyZone ''{0}'' returns TimeZone‑derived ZoneId")
    @MethodSource("legacyTimeZoneProvider")
    void toZoneId_legacyZone_returnsTimeZoneDerived(String input, ZoneId expected) {
        ZoneId result = StringConversions.toZoneId(input, mockConverter);
        assertEquals(expected, result, () -> "Expected TimeZone.getTimeZone(\"" + input + "\").toZoneId() but got a different value");
    }

    @ParameterizedTest(name = "toZoneId_invalidZoneId ''{0}'' throws IllegalArgumentException")
    @MethodSource("illegalArgumentProvider")
    void toZoneId_invalidZoneId_throwsIllegalArgumentException(String input) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toZoneId(input, mockConverter), () -> "Input '" + input + "' should trigger IllegalArgumentException");
        // The exception message always contains the original string (even if it is whitespace)
        assertTrue(ex.getMessage().contains(input), () -> "Exception message should contain the original input: '" + input + "'");
    }

    @Test
    void toZoneId_nullFrom_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> StringConversions.toZoneId(null, mockConverter), "Passing null as the 'from' argument should cause NullPointerException");
    }

    @Test
    void toZoneId_nonStringFrom_throwsClassCastException() {
        assertThrows(ClassCastException.class, () -> StringConversions.toZoneId(12345, mockConverter), "Passing a non‑String object should cause ClassCastException");
    }
}
