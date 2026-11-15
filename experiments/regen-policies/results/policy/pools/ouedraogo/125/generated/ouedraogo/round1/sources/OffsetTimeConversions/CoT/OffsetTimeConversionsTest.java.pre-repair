package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class OffsetTimeConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // Converter has a public constructor that requires ConverterOptions.
        // Use a default instance (null options are allowed for testing purposes).
        this.converter = new Converter(null);
    }

    // ----------------------------------------------------------------------
    //  List of public (package‑private static) methods in OffsetTimeConversions
    // ----------------------------------------------------------------------
    // static String toString(Object from, Converter converter)
    // static Map<String,Object> toMap(Object from, Converter converter)

    // ----------------------------------------------------------------------
    //  Basic functionality tests
    // ----------------------------------------------------------------------
    @Test
    public void testToString_basic() {
        OffsetTime now = OffsetTime.of(13, 45, 30, 123_456_789, ZoneOffset.ofHoursMinutes(2, 30));
        String expected = now.format(DateTimeFormatter.ISO_OFFSET_TIME);
        String actual = OffsetTimeConversions.toString(now, converter);
        assertEquals("toString should format OffsetTime using ISO_OFFSET_TIME", expected, actual);
    }

    @Test
    public void testToMap_basic() {
        OffsetTime time = OffsetTime.of(9, 15, 0, 0, ZoneOffset.UTC);
        Map<String, Object> map = OffsetTimeConversions.toMap(time, converter);
        assertNotNull("Map should not be null", map);
        assertTrue("Map should contain the TIME key", map.containsKey(MapConversions.TIME));
        assertEquals("Map value should be the same string as OffsetTime.toString()", time.toString(),
                map.get(MapConversions.TIME));
    }

    // ----------------------------------------------------------------------
    //  Edge case / exception handling tests
    // ----------------------------------------------------------------------
    @Test(expected = ClassCastException.class)
    public void testToString_withInvalidObject() {
        // Passing an object that is not an OffsetTime should cause a ClassCastException
        OffsetTimeConversions.toString("Not an OffsetTime", converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_withInvalidObject() {
        // Passing an object that is not an OffsetTime should cause a ClassCastException
        OffsetTimeConversions.toMap(12345, converter);
    }

    @Test
    public void testToString_withNull() {
        try {
            OffsetTimeConversions.toString(null, converter);
            fail("Expected NullPointerException when passing null to toString");
        } catch (NullPointerException e) {
            // Expected – the method casts null to OffsetTime and then calls format()
        }
    }

    @Test
    public void testToMap_withNull() {
        try {
            OffsetTimeConversions.toMap(null, converter);
            fail("Expected NullPointerException when passing null to toMap");
        } catch (NullPointerException e) {
            // Expected – the method casts null to OffsetTime and then calls toString()
        }
    }

    @Test
    public void testToString_withMaximumOffset() {
        OffsetTime maxOffset = OffsetTime.of(23, 59, 59, 999_999_999, ZoneOffset.ofHours(18));
        String result = OffsetTimeConversions.toString(maxOffset, converter);
        assertEquals("Should correctly format maximum offset time",
                maxOffset.format(DateTimeFormatter.ISO_OFFSET_TIME), result);
    }

    @Test
    public void testToString_withMinimumOffset() {
        OffsetTime minOffset = OffsetTime.of(0, 0, 0, 0, ZoneOffset.ofHoursMinutes(-18, -0));
        String result = OffsetTimeConversions.toString(minOffset, converter);
        assertEquals("Should correctly format minimum offset time",
                minOffset.format(DateTimeFormatter.ISO_OFFSET_TIME), result);
    }

    @Test
    public void testToMap_preservesInsertionOrder() {
        // Build two different OffsetTime values and ensure the map produced
        // preserves insertion order (the builder is configured with insertionOrder()).
        OffsetTime t1 = OffsetTime.of(1, 2, 3, 0, ZoneOffset.UTC);
        OffsetTime t2 = OffsetTime.of(4, 5, 6, 0, ZoneOffset.UTC);

        Map<String, Object> map1 = OffsetTimeConversions.toMap(t1, converter);
        Map<String, Object> map2 = OffsetTimeConversions.toMap(t2, converter);

        // Both maps should have exactly one entry, and the entry key should be the same.
        assertEquals(1, map1.size());
        assertEquals(1, map2.size());

        // Verify that the iteration order of the single entry matches insertion order.
        String firstKeyMap1 = map1.keySet().iterator().next();
        String firstKeyMap2 = map2.keySet().iterator().next();
        assertEquals(firstKeyMap1, firstKeyMap2);
        assertEquals(MapConversions.TIME, firstKeyMap1);
    }

    @Test
    public void testToMap_containsOnlyExpectedKey() {
        OffsetTime time = OffsetTime.of(12, 0, 0, 0, ZoneOffset.ofHours(1));
        Map<String, Object> map = OffsetTimeConversions.toMap(time, converter);
        assertEquals("Map should contain exactly one entry", 1, map.size());
        assertTrue("Map should contain the TIME key", map.containsKey(MapConversions.TIME));
        assertFalse("Map should not contain any other keys", map.keySet().stream()
                .anyMatch(k -> !MapConversions.TIME.equals(k)));
    }
}
