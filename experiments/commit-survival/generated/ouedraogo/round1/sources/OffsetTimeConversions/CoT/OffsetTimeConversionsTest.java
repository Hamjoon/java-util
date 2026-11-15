package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * JUnit 4 test suite for {@link OffsetTimeConversions}.
 *
 * The tests are placed in the same package as {@code OffsetTimeConversions}
 * so that the package‑private static methods can be accessed directly.
 */
public class OffsetTimeConversionsTest {

    /* ---------------------------------------------------------------------- */
    /*  1. List of the package‑private static methods (treated as public for testing) */
    /* ---------------------------------------------------------------------- */
    // static String toString(Object from, Converter converter)
    // static Map<String,Object> toMap(Object from, Converter converter)

    /* ---------------------------------------------------------------------- */
    /*  2. Basic functionality tests */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToString_WithUtcOffset() {
        OffsetTime ot = OffsetTime.of(13, 45, 30, 0, ZoneOffset.UTC);
        String expected = ot.format(DateTimeFormatter.ISO_OFFSET_TIME);
        String actual = OffsetTimeConversions.toString(ot, null);
        Assert.assertEquals("ISO_OFFSET_TIME string should match", expected, actual);
    }

    @Test
    public void testToString_WithPositiveOffset() {
        OffsetTime ot = OffsetTime.of(9, 15, 0, 0, ZoneOffset.ofHoursMinutes(2, 30));
        String expected = ot.format(DateTimeFormatter.ISO_OFFSET_TIME);
        String actual = OffsetTimeConversions.toString(ot, null);
        Assert.assertEquals("ISO_OFFSET_TIME string with +02:30 offset should match", expected, actual);
    }

    @Test
    public void testToString_WithNegativeOffset() {
        OffsetTime ot = OffsetTime.of(23, 5, 12, 0, ZoneOffset.ofHours(-5));
        String expected = ot.format(DateTimeFormatter.ISO_OFFSET_TIME);
        String actual = OffsetTimeConversions.toString(ot, null);
        Assert.assertEquals("ISO_OFFSET_TIME string with -05:00 offset should match", expected, actual);
    }

    @Test
    public void testToMap_Basic() {
        OffsetTime ot = OffsetTime.of(8, 0, 0, 0, ZoneOffset.ofHoursMinutes(1, 0));
        Map<String, Object> map = OffsetTimeConversions.toMap(ot, null);

        Assert.assertNotNull("Returned map must not be null", map);
        Assert.assertEquals("Map must contain exactly one entry", 1, map.size());

        // The key is defined in MapConversions.TIME – we don't have the constant value,
        // so we just verify that the single entry's value equals ot.toString().
        Object value = map.values().iterator().next();
        Assert.assertEquals("Map value must be the same string representation as OffsetTime.toString()",
                ot.toString(), value);
    }

    /* ---------------------------------------------------------------------- */
    /*  3. Edge‑case and exception handling tests */
    /* ---------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToString_NullFromObject() {
        // The method casts the argument to OffsetTime; a null should trigger NPE.
        OffsetTimeConversions.toString(null, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToString_WrongObjectType() {
        // Passing a non‑OffsetTime object should cause a ClassCastException.
        OffsetTimeConversions.toString("NotAnOffsetTime", null);
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullFromObject() {
        // Same behaviour as toString – casting null leads to NPE.
        OffsetTimeConversions.toMap(null, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_WrongObjectType() {
        // Passing an incompatible type should result in ClassCastException.
        OffsetTimeConversions.toMap(12345, null);
    }

    @Test
    public void testToString_NullConverterIsIgnored() {
        // The converter argument is not used; passing null must not affect the result.
        OffsetTime ot = OffsetTime.of(12, 30, 45, 123_456_789, ZoneOffset.ofHours(3));
        String expected = ot.format(DateTimeFormatter.ISO_OFFSET_TIME);
        String actual = OffsetTimeConversions.toString(ot, null);
        Assert.assertEquals("Result should be identical when converter is null", expected, actual);
    }

    @Test
    public void testToMap_NullConverterIsIgnored() {
        // The converter argument is not used; passing null must not affect the result.
        OffsetTime ot = OffsetTime.of(6, 20, 15, 0, ZoneOffset.ofHoursMinutes(-4, -45));
        Map<String, Object> map = OffsetTimeConversions.toMap(ot, null);
        Assert.assertEquals("Map value must still match OffsetTime.toString()", ot.toString(),
                map.values().iterator().next());
    }
}
