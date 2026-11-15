package com.cedarsoftware.util.convert;
import com.cedarsoftware.util.CompactMap;


import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.Map;

/**
 * Unit tests for {@link OffsetTimeConversions}.
 *
 * The class under test is package‑private, therefore this test class
 * resides in the same package to gain access to the static methods.
 *
 * Tests cover:
 *   • Normal conversion to String (ISO‑8601 offset time)
 *   • Normal conversion to Map (single entry with the string value)
 *   • Edge cases – null arguments, wrong argument type, extreme offsets,
 *     maximum nanosecond precision.
 */
public class OffsetTimeConversionsTest {

    // The Converter argument is not used by the methods under test.
    // We can safely pass {@code null} or a dummy instance.
    private Converter dummyConverter;

    @Before
    public void setUp() {
        // Passing null is sufficient because the implementation never dereferences it.
        dummyConverter = null;
    }

    /* -------------------------------------------------
     *  Tests for toString(Object, Converter)
     * ------------------------------------------------- */

    @Test
    public void testToString_NormalOffsetTime() {
        OffsetTime ot = OffsetTime.of(13, 45, 30, 0, ZoneOffset.ofHoursMinutes(2, 30));
        String result = OffsetTimeConversions.toString(ot, dummyConverter);
        assertEquals("13:45:30+02:30", result);
    }

    @Test
    public void testToString_MaxNanoseconds() {
        OffsetTime ot = OffsetTime.of(23, 59, 59, 999_999_999, ZoneOffset.UTC);
        String result = OffsetTimeConversions.toString(ot, dummyConverter);
        assertEquals("23:59:59.999999999Z", result);
    }

    @Test
    public void testToString_MinimumOffset() {
        OffsetTime ot = OffsetTime.of(0, 0, 0, 0, ZoneOffset.ofHours(-18));
        String result = OffsetTimeConversions.toString(ot, dummyConverter);
        assertEquals("00:00:00-18:00", result);
    }

    @Test
    public void testToString_MaximumOffset() {
        OffsetTime ot = OffsetTime.of(0, 0, 0, 0, ZoneOffset.ofHours(18));
        String result = OffsetTimeConversions.toString(ot, dummyConverter);
        assertEquals("00:00:00+18:00", result);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_NullArgument() {
        OffsetTimeConversions.toString(null, dummyConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToString_WrongArgumentType() {
        OffsetTimeConversions.toString("not an OffsetTime", dummyConverter);
    }

    /* -------------------------------------------------
     *  Tests for toMap(Object, Converter)
     * ------------------------------------------------- */

    @Test
    public void testToMap_NormalOffsetTime() {
        OffsetTime ot = OffsetTime.of(9, 15, 0, 0, ZoneOffset.ofHoursMinutes(-5, 0));
        Map<String, Object> map = OffsetTimeConversions.toMap(ot, dummyConverter);

        // The implementation uses CompactMap.builder().insertionOrder()
        // and puts a single entry with key MapConversions.TIME.
        // We verify the map size, the key/value, and that the map
        // is indeed a CompactMap.
        assertTrue("Result should be a CompactMap", map instanceof CompactMap);
        assertEquals("Map should contain exactly one entry", 1, map.size());

        // The constant MapConversions.TIME is not part of the supplied
        // source, but its value is defined as "time" in the library.
        // To avoid a hard dependency we simply check that the stored
        // value equals the OffsetTime's toString() representation.
        Object value = map.values().iterator().next();
        assertEquals(ot.toString(), value);
    }

    @Test
    public void testToMap_MaxNanoseconds() {
        OffsetTime ot = OffsetTime.of(12, 0, 0, 123_456_789, ZoneOffset.UTC);
        Map<String, Object> map = OffsetTimeConversions.toMap(ot, dummyConverter);
        assertEquals(ot.toString(), map.values().iterator().next());
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullArgument() {
        OffsetTimeConversions.toMap(null, dummyConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_WrongArgumentType() {
        OffsetTimeConversions.toMap(42, dummyConverter);
    }
}
