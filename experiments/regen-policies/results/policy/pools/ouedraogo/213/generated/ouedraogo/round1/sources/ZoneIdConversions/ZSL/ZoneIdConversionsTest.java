package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.Instant;
import java.util.Map;
import java.util.TimeZone;

/**
 * Unit tests for {@link ZoneIdConversions}.
 *
 * The tests exercise all public static conversion methods with a variety of
 * inputs, including normal values, {@code null}, and values of an incorrect type.
 *
 * No external mocking frameworks are used – only real objects created via the
 * standard Java APIs that appear in the source code or its declared dependencies.
 */
public class ZoneIdConversionsTest {

    /**
     * Verify that {@code toMap} returns a map containing a single entry
     * with key "zone" and the string representation of the supplied {@link ZoneId}.
     */
    @Test
    public void testToMapWithStandardZoneId() {
        ZoneId zone = ZoneId.of("UTC");
        Map<String, Object> result = ZoneIdConversions.toMap(zone, null);
        Assert.assertNotNull("Result map must not be null", result);
        Assert.assertEquals("Map should contain exactly one entry", 1, result.size());
        Assert.assertTrue("Map should contain key \"zone\"", result.containsKey("zone"));
        Assert.assertEquals("Value should be the ZoneId's string representation",
                "UTC", result.get("zone"));
    }

    /**
     * Verify that {@code toMap} works with a {@link ZoneOffset}, which is a concrete
     * subclass of {@link ZoneId}. The string representation of a ZoneOffset includes
     * the offset sign and hours/minutes.
     */
    @Test
    public void testToMapWithZoneOffset() {
        ZoneOffset offset = ZoneOffset.ofHours(-5);
        Map<String, Object> result = ZoneIdConversions.toMap(offset, null);
        Assert.assertEquals("-05:00", result.get("zone"));
    }

    /**
     * Verify that {@code toMap} throws a {@link NullPointerException} when the source
     * argument is {@code null}. The method casts the argument directly to {@link ZoneId},
     * so a null reference leads to NPE at the cast site.
     */
    @Test(expected = NullPointerException.class)
    public void testToMapWithNull() {
        ZoneIdConversions.toMap(null, null);
    }

    /**
     * Verify that {@code toMap} throws a {@link ClassCastException} when the supplied
     * object is not a {@link ZoneId}.
     */
    @Test(expected = ClassCastException.class)
    public void testToMapWithInvalidType() {
        ZoneIdConversions.toMap("not-a-zone-id", null);
    }

    /**
     * Verify that {@code toTimeZone} returns a {@link TimeZone} whose ID matches the
     * supplied {@link ZoneId}. For a standard ZoneId the ID is the same string.
     */
    @Test
    public void testToTimeZoneWithStandardZoneId() {
        ZoneId zone = ZoneId.of("UTC");
        TimeZone tz = ZoneIdConversions.toTimeZone(zone, null);
        Assert.assertNotNull("TimeZone must not be null", tz);
        Assert.assertEquals("UTC", tz.getID());
    }

    /**
     * Verify that {@code toTimeZone} works with a fixed offset {@link ZoneOffset}.
     * The resulting {@link TimeZone} ID follows the "GMT[+|-]hh:mm" pattern.
     */
    @Test
    public void testToTimeZoneWithZoneOffset() {
        ZoneOffset offset = ZoneOffset.ofHours(3);
        TimeZone tz = ZoneIdConversions.toTimeZone(offset, null);
        Assert.assertEquals("GMT+03:00", tz.getID());
    }

    /**
     * Verify that {@code toTimeZone} throws {@link NullPointerException} when passed
     * a {@code null} source.
     */
    @Test(expected = NullPointerException.class)
    public void testToTimeZoneWithNull() {
        ZoneIdConversions.toTimeZone(null, null);
    }

    /**
     * Verify that {@code toTimeZone} throws {@link ClassCastException} when the source
     * is not a {@link ZoneId}.
     */
    @Test(expected = ClassCastException.class)
    public void testToTimeZoneWithInvalidType() {
        ZoneIdConversions.toTimeZone(12345, null);
    }

    /**
     * Verify that {@code toZoneOffset} returns the current offset for the supplied
     * {@link ZoneId}. For a fixed offset zone the result must equal the original offset.
     */
    @Test
    public void testToZoneOffsetWithFixedOffset() {
        ZoneOffset expected = ZoneOffset.ofHoursMinutes(2, 30);
        ZoneOffset actual = ZoneIdConversions.toZoneOffset(expected, null);
        Assert.assertEquals("Offset should be unchanged for a fixed ZoneOffset", expected, actual);
    }

    /**
     * Verify that {@code toZoneOffset} works with a normal region-based ZoneId.
     * The test uses UTC, which has a known offset of zero at any instant.
     */
    @Test
    public void testToZoneOffsetWithUtc() {
        ZoneId utc = ZoneId.of("UTC");
        ZoneOffset offset = ZoneIdConversions.toZoneOffset(utc, null);
        Assert.assertEquals("UTC should have zero offset", ZoneOffset.UTC, offset);
    }

    /**
     * Verify that {@code toZoneOffset} throws {@link NullPointerException} when the
     * source argument is {@code null}.
     */
    @Test(expected = NullPointerException.class)
    public void testToZoneOffsetWithNull() {
        ZoneIdConversions.toZoneOffset(null, null);
    }

    /**
     * Verify that {@code toZoneOffset} throws {@link ClassCastException} when the
     * supplied object is not a {@link ZoneId}.
     */
    @Test(expected = ClassCastException.class)
    public void testToZoneOffsetWithInvalidType() {
        ZoneIdConversions.toZoneOffset(new Object(), null);
    }

    /**
     * Additional sanity check: ensure that the offset returned by {@code toZoneOffset}
     * is consistent with the offset obtained directly from the ZoneId rules for the
     * same instant.
     */
    @Test
    public void testToZoneOffsetConsistency() {
        ZoneId zone = ZoneId.of("America/Los_Angeles");
        Instant now = Instant.now();
        ZoneOffset expected = zone.getRules().getOffset(now);
        ZoneOffset actual = ZoneIdConversions.toZoneOffset(zone, null);
        // The method uses Instant.now() internally; the two instants may differ slightly,
        // but for most zones the offset does not change within a few milliseconds.
        Assert.assertEquals("Offsets should match the zone's current rules", expected, actual);
    }
}
