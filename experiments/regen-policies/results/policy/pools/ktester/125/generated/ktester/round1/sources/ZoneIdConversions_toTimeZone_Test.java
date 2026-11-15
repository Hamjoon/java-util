package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ZoneIdConversions_toTimeZone_Test {

    @Mock
    private Converter // Converter is not used by the method but required by signature
    mockConverter;

    private static Stream<Arguments> validZoneIdProvider() {
        return Stream.of(// standard UTC
        Arguments.of(ZoneId.of("UTC"), "UTC"), // regional zone
        Arguments.of(ZoneId.of("America/New_York"), "America/New_York"), // fixed‑offset ZoneId expressed as a string offset
        Arguments.of(ZoneId.of("+02:00"), "+02:00"));
    }

    private static Stream<Arguments> invalidTypeProvider() {
        return Stream.of(Arguments.of("UTC", "String"), Arguments.of(new java.util.Date(), "java.util.Date"));
    }

    private static Stream<Arguments> fixedOffsetProvider() {
        return Stream.of(// +05:30 → 5*3600*1000 + 30*60*1000 = 19800000 ms
        Arguments.of(ZoneOffset.ofHoursMinutes(5, 30), "+05:30", 5 * 3600_000 + 30 * 60_000), // -04:00 → -4*3600*1000 = -14400000 ms
        Arguments.of(ZoneOffset.ofHours(-4), "-04:00", -4 * 3600_000));
    }

    @Test
    void toTimeZone_givenUtcZoneId_returnsUtcTimeZone() {
        // GIVEN
        ZoneId zoneId = ZoneId.of("UTC");
        // WHEN
        TimeZone tz = ZoneIdConversions.toTimeZone(zoneId, mockConverter);
        // THEN
        assertNotNull(tz, "TimeZone should not be null");
        assertEquals("UTC", tz.getID(), "TimeZone ID should match the ZoneId");
    }

    @Test
    void toTimeZone_givenNewYorkZoneId_returnsCorrespondingTimeZone() {
        // GIVEN
        ZoneId zoneId = ZoneId.of("America/New_York");
        // WHEN
        TimeZone tz = ZoneIdConversions.toTimeZone(zoneId, mockConverter);
        // THEN
        assertNotNull(tz, "TimeZone should not be null");
        assertEquals("America/New_York", tz.getID(), "TimeZone ID should match the ZoneId for New York");
    }

    @Test
    void toTimeZone_givenNull_returnsNullPointerException() {
        // GIVEN
        Object nullObj = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ZoneIdConversions.toTimeZone(nullObj, mockConverter), "Calling toTimeZone with null should throw NullPointerException");
    }

    @Test
    void toTimeZone_givenInvalidType_throwsClassCastException() {
        // GIVEN
        Object notAZoneId = "UTC";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> ZoneIdConversions.toTimeZone(notAZoneId, mockConverter), "Passing a non‑ZoneId object should result in ClassCastException");
    }

    /* ---------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0} → expected ID={1}")
    @MethodSource("validZoneIdProvider")
    void toTimeZone_givenValidZoneId_returnsCorrespondingTimeZone(Object from, String expectedId) {
        // WHEN
        TimeZone tz = ZoneIdConversions.toTimeZone(from, mockConverter);
        // THEN
        assertNotNull(tz, "TimeZone must not be null");
        assertEquals(expectedId, tz.getID(), () -> "TimeZone ID should match the supplied ZoneId (expected: " + expectedId + ")");
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void toTimeZone_givenNullFrom_throwsNullPointerException() {
        // GIVEN
        Object nullFrom = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ZoneIdConversions.toTimeZone(nullFrom, mockConverter), "Passing null as the 'from' argument must throw NullPointerException");
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void toTimeZone_givenNonNullConverter_ignoresConverterAndReturnsCorrectTimeZone() {
        // GIVEN a real (non‑mock) Converter instance – the method does not use it
        Converter realConverter = new Converter();
        ZoneId zoneId = ZoneId.of("UTC");
        // WHEN
        TimeZone tz = ZoneIdConversions.toTimeZone(zoneId, realConverter);
        // THEN
        assertNotNull(tz, "TimeZone must not be null even when a real Converter is supplied");
        assertEquals("UTC", tz.getID(), "TimeZone ID should still be 'UTC'");
    }

    /* ---------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] offset={0} → ID={1}, rawOffset={2}")
    @MethodSource("fixedOffsetProvider")
    void toTimeZone_givenZoneOffset_returnsMatchingTimeZone(ZoneOffset offset, String expectedId, int expectedRawOffsetMs) {
        // WHEN
        TimeZone tz = ZoneIdConversions.toTimeZone(offset, mockConverter);
        // THEN
        assertNotNull(tz, "TimeZone must not be null for ZoneOffset input");
        assertEquals(expectedId, tz.getID(), () -> "TimeZone ID should match the offset string");
        assertEquals(expectedRawOffsetMs, tz.getRawOffset(), () -> "Raw offset (ms) should correspond to the supplied ZoneOffset");
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void toTimeZone_givenCustomZoneIdThatThrows_propagatesIllegalArgumentException() {
        // GIVEN a custom ZoneId whose getId() throws IllegalArgumentException
        ZoneId badZoneId = new ZoneId() {

            @Override
            public String getId() {
                throw new IllegalArgumentException("invalid ID");
            }

            @Override
            public ZoneRules getRules() {
                // Return a harmless set of rules – they are never used because getId() fails first
                return ZoneId.of("UTC").getRules();
            }
        };
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> ZoneIdConversions.toTimeZone(badZoneId, mockConverter), "The IllegalArgumentException from getId() should be propagated");
    }
}
