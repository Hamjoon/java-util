package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

import java.time.MonthDay;
import java.util.Map;

import com.cedarsoftware.util.CompactMap;

/**
 * Tests for {@link MonthDayConversions}.
 *
 * The test class is placed in the same package as {@code MonthDayConversions}
 * so that it can access the package‑private class.
 */
public class MonthDayConversionsTest {

    /* -------------------------------------------------
     *  Basic functionality tests
     * ------------------------------------------------- */

    @Test
    public void testToMapBasic() {
        MonthDay md = MonthDay.of(12, 5); // December 5
        Map<String, Object> result = MonthDayConversions.toMap(md, null);

        // Verify map content
        Assert.assertEquals("Map should contain exactly two entries", 2, result.size());
        Assert.assertTrue("Map should contain key \"day\"", result.containsKey("day"));
        Assert.assertTrue("Map should contain key \"month\"", result.containsKey("month"));
        Assert.assertEquals("Day value should be 5", 5, result.get("day"));
        Assert.assertEquals("Month value should be 12", 12, result.get("month"));
    }

    @Test
    public void testToMapLeapDay() {
        MonthDay md = MonthDay.of(2, 29); // Leap day
        Map<String, Object> result = MonthDayConversions.toMap(md, null);

        Assert.assertEquals(2, result.size());
        Assert.assertEquals(29, result.get("day"));
        Assert.assertEquals(2, result.get("month"));
    }

    @Test
    public void testToMapReturnsCompactMap() {
        MonthDay md = MonthDay.of(1, 1);
        Map<String, Object> result = MonthDayConversions.toMap(md, null);

        Assert.assertTrue("Result should be an instance of CompactMap",
                result instanceof CompactMap);
    }

    /* -------------------------------------------------
     *  Edge‑case / exception tests
     * ------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToMapNullFrom() {
        // ‘from’ is null – should throw NPE when the method tries to call getDayOfMonth()
        MonthDayConversions.toMap(null, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToMapInvalidFromType() {
        // Passing an object that is not a MonthDay – cast should fail
        MonthDayConversions.toMap("not a month day", null);
    }

    @Test
    public void testToMapWithNullConverterDoesNotFail() {
        // The converter argument is not used; passing null must be safe.
        MonthDay md = MonthDay.of(7, 15);
        Map<String, Object> result = MonthDayConversions.toMap(md, null);
        Assert.assertEquals(15, result.get("day"));
        Assert.assertEquals(7, result.get("month"));
    }
}
