package com.cedarsoftware.util.convert;
import com.cedarsoftware.util.CompactMap;


import static org.junit.Assert.*;

import java.time.YearMonth;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

/**
 * Unit tests for {@link YearMonthConversions}.
 * <p>
 * The class under test is package‑private, therefore the test class lives in the same
 * package to gain access to the static methods.
 */
public class YearMonthConversionsTest {

    /**
     * Verify that a normal {@link YearMonth} instance is converted into a map
     * with the expected keys and values.
     */
    @Test
    public void testToMapNormalCase() {
        YearMonth ym = YearMonth.of(2023, 5); // May 2023
        // The converter argument is not used by the method; passing {@code null} is fine.
        Map<String, Object> result = YearMonthConversions.toMap(ym, null);

        // The result must be a non‑null map containing exactly two entries.
        assertNotNull("Result map must not be null", result);
        assertEquals("Map must contain exactly two entries", 2, result.size());

        // Verify the content.
        assertTrue("Map must contain key \"year\"", result.containsKey("year"));
        assertTrue("Map must contain key \"month\"", result.containsKey("month"));
        assertEquals("Year value must match", Integer.valueOf(2023), result.get("year"));
        assertEquals("Month value must match", Integer.valueOf(5), result.get("month"));

        // The concrete implementation should be a CompactMap (as used by the conversion code).
        assertTrue("Result should be an instance of CompactMap",
                result instanceof CompactMap);
    }

    /**
     * Verify that the map preserves insertion order (year first, month second).
     */
    @Test
    public void testMapInsertionOrder() {
        YearMonth ym = YearMonth.of(1999, 12);
        Map<String, Object> result = YearMonthConversions.toMap(ym, null);

        Iterator<Entry<String, Object>> it = result.entrySet().iterator();

        assertTrue("Map must have at least one entry", it.hasNext());
        Entry<String, Object> first = it.next();
        assertEquals("First entry key should be \"year\"", "year", first.getKey());

        assertTrue("Map must have a second entry", it.hasNext());
        Entry<String, Object> second = it.next();
        assertEquals("Second entry key should be \"month\"", "month", second.getKey());

        assertFalse("Map must contain exactly two entries", it.hasNext());
    }

    /**
     * YearMonth supports negative (BC) years. Ensure those values are handled correctly.
     */
    @Test
    public void testToMapWithNegativeYear() {
        YearMonth ym = YearMonth.of(-44, 3); // March 44 BC
        Map<String, Object> result = YearMonthConversions.toMap(ym, null);

        assertEquals("Negative year must be preserved", Integer.valueOf(-44), result.get("year"));
        assertEquals("Month must be preserved", Integer.valueOf(3), result.get("month"));
    }

    /**
     * Passing {@code null} as the source object should result in a {@link NullPointerException}
     * because the method casts the argument to {@link YearMonth} and then dereferences it.
     */
    @Test(expected = NullPointerException.class)
    public void testToMapNullSourceThrowsNPE() {
        YearMonthConversions.toMap(null, null);
    }

    /**
     * Passing an object that is not a {@link YearMonth} must cause a {@link ClassCastException}
     * when the method attempts to cast it.
     */
    @Test(expected = ClassCastException.class)
    public void testToMapInvalidSourceThrowsClassCastException() {
        String notAYearMonth = "2023-05";
        YearMonthConversions.toMap(notAYearMonth, null);
    }
}
