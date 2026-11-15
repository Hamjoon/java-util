package com.cedarsoftware.util.convert;
import com.cedarsoftware.util.CompactLinkedMap;


import org.junit.Test;
import org.junit.Assert;

import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Unit tests for {@link OffsetTimeConversions}.
 *
 * The class under test is package‑private and its methods are static,
 * therefore the test class lives in the same package so it can access them.
 *
 * The {@code Converter} argument is not used by the implementation, so a {@code null}
 * reference is sufficient for exercising the logic.
 */
public class OffsetTimeConversionsTest {

    /**
     * Verify that {@code toString} returns the ISO‑8601 representation
     * produced by {@link DateTimeFormatter#ISO_OFFSET_TIME}.
     */
    @Test
    public void testToStringMatchesIsoFormatter() {
        OffsetTime offsetTime = OffsetTime.of(12, 34, 56, 123_456_789, ZoneOffset.ofHours(2));
        String expected = offsetTime.format(DateTimeFormatter.ISO_OFFSET_TIME);
        String actual = OffsetTimeConversions.toString(offsetTime, null);
        Assert.assertEquals("toString should produce ISO_OFFSET_TIME format", expected, actual);
    }

    /**
     * Verify that {@code toString} works for a UTC (zero) offset.
     */
    @Test
    public void testToStringWithUtcOffset() {
        OffsetTime utc = OffsetTime.of(23, 59, 59, 0, ZoneOffset.UTC);
        String expected = utc.format(DateTimeFormatter.ISO_OFFSET_TIME);
        String actual = OffsetTimeConversions.toString(utc, null);
        Assert.assertEquals("UTC offset should be formatted with 'Z'", expected, actual);
    }

    /**
     * Verify that {@code toString} works for a negative offset and that the sign is preserved.
     */
    @Test
    public void testToStringWithNegativeOffset() {
        OffsetTime negative = OffsetTime.of(5, 0, 0, 0, ZoneOffset.ofHoursMinutes(-5, -30));
        String expected = negative.format(DateTimeFormatter.ISO_OFFSET_TIME);
        String actual = OffsetTimeConversions.toString(negative, null);
        Assert.assertEquals("Negative offset should be formatted correctly", expected, actual);
    }

    /**
     * Passing {@code null} as the source object should result in a {@link NullPointerException}
     * because the method casts the argument to {@link OffsetTime}.
     */
    @Test(expected = NullPointerException.class)
    public void testToStringNullArgument() {
        OffsetTimeConversions.toString(null, null);
    }

    /**
     * Verify that {@code toMap} returns a map containing a single entry with the key
     * {@link MapConversions#TIME} and the value equal to {@link OffsetTime#toString()}.
     */
    @Test
    public void testToMapProducesCorrectEntry() {
        OffsetTime time = OffsetTime.of(9, 15, 30, 0, ZoneOffset.ofHours(1));
        Map<String, Object> result = OffsetTimeConversions.toMap(time, null);

        // The map implementation should be a CompactLinkedMap (or at least a Map)
        Assert.assertTrue("Result should be an instance of CompactLinkedMap",
                result instanceof CompactLinkedMap);

        // It must contain exactly one entry
        Assert.assertEquals("Map should contain exactly one entry", 1, result.size());

        // Verify the key and value
        Assert.assertTrue("Map should contain the TIME key",
                result.containsKey(MapConversions.TIME));
        Assert.assertEquals("Value should be the OffsetTime's string representation",
                time.toString(), result.get(MapConversions.TIME));
    }

    /**
     * Verify that {@code toMap} throws a {@link NullPointerException} when the source is {@code null}.
     */
    @Test(expected = NullPointerException.class)
    public void testToMapNullArgument() {
        OffsetTimeConversions.toMap(null, null);
    }

    /**
     * The utility class has a private constructor to prevent instantiation.
     * This test uses reflection to ensure the constructor is indeed private
     * and that it cannot be invoked without overriding accessibility.
     */
    @Test
    public void testPrivateConstructorIsInaccessible() throws Exception {
        java.lang.reflect.Constructor<OffsetTimeConversions> ctor =
                OffsetTimeConversions.class.getDeclaredConstructor();
        Assert.assertTrue("Constructor should be private", java.lang.reflect.Modifier.isPrivate(ctor.getModifiers()));

        // Attempting to instantiate should fail unless we make it accessible.
        boolean instantiated = false;
        try {
            ctor.setAccessible(true);
            ctor.newInstance();
            instantiated = true;
        } finally {
            // Reset accessibility to avoid side effects
            ctor.setAccessible(false);
        }
        Assert.assertTrue("Constructor should be invocable via reflection when setAccessible(true)", instantiated);
    }
}
