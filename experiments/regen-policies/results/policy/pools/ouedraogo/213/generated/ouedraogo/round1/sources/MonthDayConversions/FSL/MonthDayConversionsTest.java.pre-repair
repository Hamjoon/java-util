package com.cedarsoftware.util.convert;

import static com.cedarsoftware.util.convert.MapConversions.MONTH_DAY;
import static org.junit.Assert.*;

import java.time.MonthDay;
import java.util.Map;

import org.junit.Test;

/**
 * Unit tests for {@link MonthDayConversions}.
 * <p>
 * The tests cover:
 * <ul>
 *   <li>Correct conversion of a {@link MonthDay} to a {@link Map} with the proper key and value.</li>
 *   <li>Various valid {@code MonthDay} instances (regular dates, end‑of‑year, leap‑day).</li>
 *   <li>Behaviour when {@code null} is supplied as the source object (should throw {@link NullPointerException}).</li>
 *   <li>Behaviour when an object of an incompatible type is supplied (should throw {@link ClassCastException}).</li>
 *   <li>That the returned map is mutable and preserves insertion order (LinkedHashMap semantics).</li>
 * </ul>
 */
public class MonthDayConversionsTest {

    // Helper method to obtain a Converter instance. The implementation of MonthDayConversions does not
    // use the Converter, so we can safely pass a Converter constructed with a null options object.
    private static Converter getConverter() {
        return new Converter(null);
    }

    @Test
    public void testToMap_WithTypicalMonthDay() {
        MonthDay monthDay = MonthDay.of(1, 15);               // January 15th
        Map<String, Object> result = MonthDayConversions.toMap(monthDay, getConverter());

        assertNotNull("Resulting map must not be null", result);
        assertEquals("Map should contain exactly one entry", 1, result.size());
        assertTrue("Map must contain the MONTH_DAY key", result.containsKey(MONTH_DAY));
        assertEquals("MonthDay string representation must match '--MM-dd' format",
                     monthDay.toString(), result.get(MONTH_DAY));
    }

    @Test
    public void testToMap_WithEndOfYear() {
        MonthDay monthDay = MonthDay.of(12, 31);              // December 31st
        Map<String, Object> result = MonthDayConversions.toMap(monthDay, getConverter());

        assertEquals("12-31", result.get(MONTH_DAY));
    }

    @Test
    public void testToMap_WithLeapDay() {
        MonthDay monthDay = MonthDay.of(2, 29);               // February 29th (valid for MonthDay)
        Map<String, Object> result = MonthDayConversions.toMap(monthDay, getConverter());

        assertEquals("02-29", result.get(MONTH_DAY));
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullSourceThrowsNPE() {
        // The method casts the input to MonthDay without a null check, so a NPE is expected.
        MonthDayConversions.toMap(null, getConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_IncompatibleSourceThrowsClassCastException() {
        // Passing a String instead of a MonthDay should cause a ClassCastException.
        MonthDayConversions.toMap("not-a-monthday", getConverter());
    }

    @Test
    public void testReturnedMapIsMutableAndPreservesOrder() {
        MonthDay monthDay = MonthDay.of(3, 5);
        Map<String, Object> result = MonthDayConversions.toMap(monthDay, getConverter());

        // Verify mutability
        result.put("extraKey", "extraValue");
        assertEquals("Map should now contain two entries", 2, result.size());
        assertEquals("extraValue", result.get("extraKey"));

        // Verify iteration order (LinkedHashMap preserves insertion order)
        StringBuilder sb = new StringBuilder();
        for (String key : result.keySet()) {
            sb.append(key).append(';');
        }
        // Expected order: MONTH_DAY first, then "extraKey"
        assertEquals(MONTH_DAY + ";extraKey;", sb.toString());
    }
}
