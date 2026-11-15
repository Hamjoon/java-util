package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactMap;
import org.junit.Test;

import java.time.MonthDay;
import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.*;

public class MonthDayConversionsTest {

    /**
     * Verify that a normal MonthDay is converted correctly into a map with
     * keys "day" and "month" containing the appropriate integer values.
     */
    @Test
    public void testToMapBasic() {
        MonthDay md = MonthDay.of(5, 15); // May 15
        Map<String, Object> result = MonthDayConversions.toMap(md, null);

        assertNotNull("Result map must not be null", result);
        assertEquals("Map must contain exactly two entries", 2, result.size());

        assertTrue("Map must contain key 'day'", result.containsKey("day"));
        assertTrue("Map must contain key 'month'", result.containsKey("month"));

        assertEquals("Day value must match MonthDay", 15, result.get("day"));
        assertEquals("Month value must match MonthDay", 5, result.get("month"));

        // Verify the concrete implementation is a CompactMap
        assertTrue("Result should be a CompactMap", result instanceof CompactMap);
    }

    /**
     * Verify conversion of a leap‑day (February 29) – an edge case for the
     * MonthDay API.
     */
    @Test
    public void testToMapLeapDay() {
        MonthDay md = MonthDay.of(2, 29); // Feb 29 (leap year)
        Map<String, Object> result = MonthDayConversions.toMap(md, null);

        assertEquals("Day value for leap day must be 29", 29, result.get("day"));
        assertEquals("Month value for leap day must be 2", 2, result.get("month"));
    }

    /**
     * The builder used in MonthDayConversions requests insertion order.
     * Ensure that the iteration order of the entry set reflects this order:
     * first "day", then "month".
     */
    @Test
    public void testInsertionOrderPreserved() {
        MonthDay md = MonthDay.of(12, 31);
        Map<String, Object> result = MonthDayConversions.toMap(md, null);

        Iterator<Map.Entry<String, Object>> it = result.entrySet().iterator();

        assertTrue("Map must have at least one entry", it.hasNext());
        Map.Entry<String, Object> first = it.next();
        assertEquals("First entry key should be 'day'", "day", first.getKey());

        assertTrue("Map must have a second entry", it.hasNext());
        Map.Entry<String, Object> second = it.next();
        assertEquals("Second entry key should be 'month'", "month", second.getKey());

        assertFalse("No more entries should be present", it.hasNext());
    }

    /**
     * Passing a null reference as the source object should result in a
     * NullPointerException because the method casts the argument without
     * a null check.
     */
    @Test(expected = NullPointerException.class)
    public void testNullSourceThrowsNPE() {
        MonthDayConversions.toMap(null, null);
    }

    /**
     * Passing an object that is not a MonthDay must cause a ClassCastException.
     */
    @Test(expected = ClassCastException.class)
    public void testInvalidSourceTypeThrowsClassCastException() {
        String notAMonthDay = "not a month day";
        MonthDayConversions.toMap(notAMonthDay, null);
    }

    /**
     * The returned map should be mutable – callers can add additional entries.
     */
    @Test
    public void testResultMapIsMutable() {
        MonthDay md = MonthDay.of(1, 1);
        Map<String, Object> result = MonthDayConversions.toMap(md, null);

        // Add a new entry
        result.put("extra", "value");

        assertEquals("Map size should increase after adding an entry", 3, result.size());
        assertEquals("Extra entry must be retrievable", "value", result.get("extra"));
    }
}
