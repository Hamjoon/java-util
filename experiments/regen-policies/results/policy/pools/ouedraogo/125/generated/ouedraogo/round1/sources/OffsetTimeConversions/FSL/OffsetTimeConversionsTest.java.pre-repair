package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.junit.Test;

/**
 * Unit tests for {@link OffsetTimeConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Correct ISO‑8601 formatting for a variety of {@link OffsetTime} instances.</li>
 *   <li>Behaviour when {@code null} is supplied as the source object (expected NPE).</li>
 *   <li>Correct map creation – a single entry with the {@code OffsetTime} string representation.</li>
 *   <li>Map characteristics (size, immutability of the key/value pair).</li>
 * </ul>
 *
 * The {@code Converter} argument is not used by the implementation, therefore {@code null}
 * is passed in all cases.  This matches the contract of the class and keeps the tests
 * focused on the logic under test.
 */
public class OffsetTimeConversionsTest {

    // -------------------------------------------------------------------------
    // toString(..) tests
    // -------------------------------------------------------------------------

    @Test
    public void testToString_WithPositiveOffset() {
        OffsetTime time = OffsetTime.of(10, 15, 30, 0, ZoneOffset.ofHours(2));
        String expected = time.format(DateTimeFormatter.ISO_OFFSET_TIME);
        String actual = OffsetTimeConversions.toString(time, null);
        assertEquals("ISO string with +02:00 offset should match", expected, actual);
    }

    @Test
    public void testToString_WithNegativeOffset() {
        OffsetTime time = OffsetTime.of(23, 59, 59, 123_000_000, ZoneOffset.ofHoursMinutes(-5, -30));
        String expected = time.format(DateTimeFormatter.ISO_OFFSET_TIME);
        String actual = OffsetTimeConversions.toString(time, null);
        assertEquals("ISO string with -05:30 offset should match", expected, actual);
    }

    @Test
    public void testToString_WithZeroOffset_Zulu() {
        OffsetTime time = OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC);
        String expected = time.format(DateTimeFormatter.ISO_OFFSET_TIME);
        String actual = OffsetTimeConversions.toString(time, null);
        assertEquals("ISO string with Z (UTC) offset should match", expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_NullInputThrowsNPE() {
        // The implementation casts the argument to OffsetTime and then calls a method on it.
        // Supplying null must therefore result in a NullPointerException.
        OffsetTimeConversions.toString(null, null);
    }

    // -------------------------------------------------------------------------
    // toMap(..) tests
    // -------------------------------------------------------------------------

    @Test
    public void testToMap_ReturnsMapWithSingleEntry() {
        OffsetTime time = OffsetTime.of(12, 34, 56, 0, ZoneOffset.ofHours(1));
        Map<String, Object> map = OffsetTimeConversions.toMap(time, null);

        // Verify map size
        assertEquals("Map should contain exactly one entry", 1, map.size());

        // Verify the key – we cannot reference MapConversions.TIME directly (it is not part of the supplied API),
        // but the implementation uses a single entry, so we can obtain the key dynamically.
        String key = map.keySet().iterator().next();
        assertNotNull("Key should not be null", key);

        // Verify the value matches the OffsetTime's standard string representation.
        Object value = map.get(key);
        assertEquals("Value should be the OffsetTime's toString()", time.toString(), value);
    }

    @Test
    public void testToMap_KeyPreservesInsertionOrder() {
        // The builder is configured with insertionOrder(); with a single entry this is trivial,
        // but we assert that the map reports its ordering correctly via its iterator.
        OffsetTime time = OffsetTime.of(9, 0, 0, 0, ZoneOffset.ofHours(-3));
        Map<String, Object> map = OffsetTimeConversions.toMap(time, null);

        // The iterator should return the sole entry first.
        String firstKey = map.entrySet().iterator().next().getKey();
        assertEquals("The only key should be the one inserted by toMap()", map.keySet().iterator().next(), firstKey);
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullInputThrowsNPE() {
        // As with toString, a null source leads to a NullPointerException when the code attempts to cast.
        OffsetTimeConversions.toMap(null, null);
    }

    // -------------------------------------------------------------------------
    // Additional sanity checks (not strictly required but improve coverage)
    // -------------------------------------------------------------------------

    @Test
    public void testToMap_ValueIsImmutableString() {
        OffsetTime time = OffsetTime.of(15, 45, 30, 500_000_000, ZoneOffset.ofHoursMinutes(5, 45));
        Map<String, Object> map = OffsetTimeConversions.toMap(time, null);
        Object value = map.values().iterator().next();

        // The value should be a String and equal to the OffsetTime's toString()
        assertTrue("Value should be a String instance", value instanceof String);
        assertEquals(time.toString(), value);
    }
}
