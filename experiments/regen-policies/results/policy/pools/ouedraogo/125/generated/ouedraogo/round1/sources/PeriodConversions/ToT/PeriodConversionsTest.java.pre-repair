package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactMap;
import org.junit.Test;

import java.time.Period;
import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link PeriodConversions}.
 *
 * <p>This suite covers:
 * <ul>
 *   <li>Typical conversion of a {@link Period} into a {@link Map} with the expected keys and values.</li>
 *   <li>Verification that the {@link CompactMap} preserves insertion order (years → months → days).</li>
 *   <li>Edge cases such as zero, negative and very large component values.</li>
 *   <li>Failure scenarios – passing an object that is not a {@code Period} and passing {@code null}
 *       as the source object, both of which should result in a {@link ClassCastException} or
 *       {@link NullPointerException} respectively.</li>
 * </ul>
 *
 * <p>Only the public API shown in the prompt is used; no mocking frameworks or external libraries are required.
 */
public class PeriodConversionsTest {

    /**
     * Helper that invokes {@link PeriodConversions#toMap(Object, Converter)}.
     * The {@code Converter} argument is not used by the method, therefore {@code null}
     * is supplied to keep the test simple.
     */
    private static Map<String, Object> convert(Period period) {
        // The method expects an Object, so we pass the Period directly.
        // The second argument is unused, so null is acceptable.
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) PeriodConversions.toMap(period, null);
        return result;
    }

    @Test
    public void testTypicalPeriodConversion() {
        Period period = Period.of(3, 5, 12); // 3 years, 5 months, 12 days
        Map<String, Object> map = convert(period);

        // Verify the map contains exactly three entries with the correct keys
        assertEquals("Map should contain exactly three entries", 3, map.size());
        assertTrue("Map should contain key YEARS", map.containsKey(MapConversions.YEARS));
        assertTrue("Map should contain key MONTHS", map.containsKey(MapConversions.MONTHS));
        assertTrue("Map should contain key DAYS", map.containsKey(MapConversions.DAYS));

        // Verify the values
        assertEquals(Integer.valueOf(3), map.get(MapConversions.YEARS));
        assertEquals(Integer.valueOf(5), map.get(MapConversions.MONTHS));
        assertEquals(Integer.valueOf(12), map.get(MapConversions.DAYS));
    }

    @Test
    public void testInsertionOrderIsPreserved() {
        Period period = Period.of(1, 2, 3);
        Map<String, Object> map = convert(period);

        // CompactMap with insertionOrder should return entries in the order they were added:
        // YEARS, MONTHS, DAYS.
        Iterator<String> keyIterator = map.keySet().iterator();

        assertTrue("First key should be YEARS", keyIterator.hasNext());
        assertEquals(MapConversions.YEARS, keyIterator.next());

        assertTrue("Second key should be MONTHS", keyIterator.hasNext());
        assertEquals(MapConversions.MONTHS, keyIterator.next());

        assertTrue("Third key should be DAYS", keyIterator.hasNext());
        assertEquals(MapConversions.DAYS, keyIterator.next());

        assertFalse("No more keys should be present", keyIterator.hasNext());
    }

    @Test
    public void testZeroValues() {
        Period period = Period.of(0, 0, 0);
        Map<String, Object> map = convert(period);

        assertEquals(Integer.valueOf(0), map.get(MapConversions.YEARS));
        assertEquals(Integer.valueOf(0), map.get(MapConversions.MONTHS));
        assertEquals(Integer.valueOf(0), map.get(MapConversions.DAYS));
    }

    @Test
    public void testNegativeValues() {
        // Period allows negative components; they are retained as supplied.
        Period period = Period.of(-2, -4, -7);
        Map<String, Object> map = convert(period);

        assertEquals(Integer.valueOf(-2), map.get(MapConversions.YEARS));
        assertEquals(Integer.valueOf(-4), map.get(MapConversions.MONTHS));
        assertEquals(Integer.valueOf(-7), map.get(MapConversions.DAYS));
    }

    @Test
    public void testLargeValues() {
        // Use values that are far beyond typical ranges.
        Period period = Period.of(Integer.MAX_VALUE, 12, 31);
        Map<String, Object> map = convert(period);

        assertEquals(Integer.valueOf(Integer.MAX_VALUE), map.get(MapConversions.YEARS));
        assertEquals(Integer.valueOf(12), map.get(MapConversions.MONTHS));
        assertEquals(Integer.valueOf(31), map.get(MapConversions.DAYS));
    }

    @Test(expected = ClassCastException.class)
    public void testNonPeriodObjectThrowsClassCastException() {
        // Passing a String (or any non‑Period) should cause a ClassCastException
        // because the method casts the argument to Period without checking.
        PeriodConversions.toMap("I am not a period", null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullSourceThrowsNullPointerException() {
        // Casting null to Period yields null; subsequent calls on the null reference
        // will throw a NullPointerException.
        PeriodConversions.toMap(null, null);
    }

    @Test
    public void testMapIsMutableAndIndependent() {
        Period period = Period.of(4, 8, 15);
        Map<String, Object> map = convert(period);

        // Modify the map and verify that it does not affect the original Period object.
        map.put(MapConversions.YEARS, 99);
        assertEquals(Integer.valueOf(99), map.get(MapConversions.YEARS));

        // Ensure the Period instance is unchanged.
        assertEquals(4, period.getYears());
    }

    @Test
    public void testCompactMapImplementation() {
        Period period = Period.of(2, 3, 4);
        Map<String, Object> map = convert(period);

        // The returned map must be an instance of CompactMap.
        assertTrue("Returned map should be a CompactMap", map instanceof CompactMap);
    }
}
