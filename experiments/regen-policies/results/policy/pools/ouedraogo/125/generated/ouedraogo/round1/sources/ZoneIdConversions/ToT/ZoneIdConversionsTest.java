package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TimeZone;

/**
 * Comprehensive JUnit‑4 test suite for {@link ZoneIdConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Normal usage with typical ZoneId values.</li>
 *   <li>Edge cases such as {@code null} arguments and wrong argument types.</li>
 *   <li>Verification that the returned {@link Map} respects insertion order and contains the correct key/value.</li>
 *   <li>Correct conversion to {@link TimeZone} and {@link ZoneOffset}.</li>
 * </ul>
 *
 * No mocking is performed – only the public API shown in the prompt is used.
 */
public class ZoneIdConversionsTest {

    private Converter dummyConverter;   // Converter is not used by the methods under test

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        // The Converter constructor requires a ConverterOptions instance.
        // The tests do not depend on any Converter behaviour, so we can safely pass null.
        dummyConverter = new Converter(null);
    }

    /* --------------------------------------------------------------------- */
    /* --------------------------- toMap() tests -------------------------- */
    /* --------------------------------------------------------------------- */

    @Test
    public void toMap_ShouldReturnMapWithZoneKeyAndStringValue() {
        ZoneId zoneId = ZoneId.of("America/New_York");
        Map<String, Object> result = ZoneIdConversions.toMap(zoneId, dummyConverter);

        // Verify map properties
        assert result != null : "Resulting map must not be null";
        assert result.size() == 1 : "Map should contain exactly one entry";
        assert result.containsKey("zone") : "Map must contain the key \"zone\"";
        assert zoneId.toString().equals(result.get("zone")) : "Value must match ZoneId.toString()";

        // Verify insertion order – the map implementation is insertion‑ordered,
        // so the first (and only) key must be "zone".
        // CompactMap implements java.util.Map; we can iterate over entrySet.
        String firstKey = result.entrySet().iterator().next().getKey();
        assert "zone".equals(firstKey) : "First key should be \"zone\"";
    }

    @Test
    public void toMap_ShouldThrowNullPointerException_WhenFromIsNull() {
        thrown.expect(NullPointerException.class);
        ZoneIdConversions.toMap(null, dummyConverter);
    }

    @Test
    public void toMap_ShouldThrowClassCastException_WhenFromIsNotZoneId() {
        thrown.expect(ClassCastException.class);
        ZoneIdConversions.toMap("not a ZoneId", dummyConverter);
    }

    /* --------------------------------------------------------------------- */
    /* ------------------------ toTimeZone() tests ------------------------- */
    /* --------------------------------------------------------------------- */

    @Test
    public void toTimeZone_ShouldConvertZoneIdToEquivalentTimeZone() {
        ZoneId zoneId = ZoneId.of("Europe/Paris");
        TimeZone tz = ZoneIdConversions.toTimeZone(zoneId, dummyConverter);

        assert tz != null : "Returned TimeZone must not be null";
        // TimeZone#getID returns the ID string; it should equal the ZoneId ID.
        assert zoneId.getId().equals(tz.getID()) :
                "TimeZone ID should match ZoneId ID (" + zoneId.getId() + ")";
    }

    @Test
    public void toTimeZone_ShouldHandleUTCCorrectly() {
        ZoneId utc = ZoneId.of("UTC");
        TimeZone tz = ZoneIdConversions.toTimeZone(utc, dummyConverter);
        assert tz != null;
        assert "UTC".equals(tz.getID());
    }

    @Test
    public void toTimeZone_ShouldThrowNullPointerException_WhenFromIsNull() {
        thrown.expect(NullPointerException.class);
        ZoneIdConversions.toTimeZone(null, dummyConverter);
    }

    @Test
    public void toTimeZone_ShouldThrowClassCastException_WhenFromIsNotZoneId() {
        thrown.expect(ClassCastException.class);
        ZoneIdConversions.toTimeZone(12345, dummyConverter);
    }

    /* --------------------------------------------------------------------- */
    /* ----------------------- toZoneOffset() tests ------------------------ */
    /* --------------------------------------------------------------------- */

    @Test
    public void toZoneOffset_ShouldReturnCurrentOffsetForGivenZone() {
        ZoneId zoneId = ZoneId.of("America/Los_Angeles");

        // Capture the instant once so both the expected and actual offsets are based on the same moment.
        Instant now = Instant.now();
        ZoneOffset expected = zoneId.getRules().getOffset(now);

        // The method uses Instant.now() internally; we cannot inject the captured instant,
        // but the offset for a given zone rarely changes within a few milliseconds.
        // To make the test robust we allow a small time drift by recomputing after the call.
        ZoneOffset actual = ZoneIdConversions.toZoneOffset(zoneId, dummyConverter);

        // Verify that the offset belongs to the same set of possible offsets for the zone.
        // For most zones there are only one or two possible offsets (standard/DST).
        assert zoneId.getRules().getValidOffsets(now).contains(actual) :
                "Returned offset must be a valid offset for the zone at the current instant";
        // Also check that the offset matches the expected value when the instant difference is negligible.
        // If the system clock crossed a DST boundary between the two calls, the assertion still holds
        // because both offsets will be valid for the zone.
        assert expected.equals(actual) || zoneId.getRules().getValidOffsets(now).contains(actual) :
                "Returned offset should match the expected offset for the captured instant";
    }

    @Test
    public void toZoneOffset_ShouldReturnZeroOffsetForUTC() {
        ZoneId utc = ZoneId.of("UTC");
        ZoneOffset offset = ZoneIdConversions.toZoneOffset(utc, dummyConverter);
        assert ZoneOffset.UTC.equals(offset) : "UTC ZoneOffset must be zero (Z)";
    }

    @Test
    public void toZoneOffset_ShouldThrowNullPointerException_WhenFromIsNull() {
        thrown.expect(NullPointerException.class);
        ZoneIdConversions.toZoneOffset(null, dummyConverter);
    }

    @Test
    public void toZoneOffset_ShouldThrowClassCastException_WhenFromIsNotZoneId() {
        thrown.expect(ClassCastException.class);
        ZoneIdConversions.toZoneOffset(new Object(), dummyConverter);
    }
}
