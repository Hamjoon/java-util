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
 * The tests cover:
 * <ul>
 *   <li>Typical usage with positive, zero and negative values.</li>
 *   <li>Insertion‑order guarantee of the returned {@link CompactMap}.</li>
 *   <li>Edge‑case handling – {@code null} input and wrong input type.</li>
 *   <li>Large numeric values to ensure no overflow / truncation.</li>
 * </ul>
 *
 * The {@code Converter} argument of {@code PeriodConversions.toMap(..)} is not used
 * by the method implementation, therefore a {@code null} reference is sufficient
 * for the tests.
 */
public class PeriodConversionsTest {

    /* ----------------------------------------------------------------------
     * Helper constants – the keys used by PeriodConversions are defined in
     * MapConversions.  They are public static final strings, so we can refer to
     * them directly.
     * ---------------------------------------------------------------------- */
    private static final String YEARS_KEY   = MapConversions.YEARS;
    private static final String MONTHS_KEY  = MapConversions.MONTHS;
    private static final String DAYS_KEY    = MapConversions.DAYS;

    /**
     * Verify that a normal Period with positive values is converted correctly.
     */
    @Test
    public void testToMap_PositiveValues() {
        Period period = Period.of(2, 5, 12);               // 2 years, 5 months, 12 days
        Map<String, Object> map = PeriodConversions.toMap(period, null);

        assertNotNull("Resulting map must not be null", map);
        assertEquals("Map must contain exactly three entries", 3, map.size());

        assertEquals("Years must be stored correctly", 2, map.get(YEARS_KEY));
        assertEquals("Months must be stored correctly", 5, map.get(MONTHS_KEY));
        assertEquals("Days must be stored correctly", 12, map.get(DAYS_KEY));
    }

    /**
     * Verify handling of zero values – the map should still contain the keys
     * with a value of zero.
     */
    @Test
    public void testToMap_ZeroValues() {
        Period period = Period.of(0, 0, 0);
        Map<String, Object> map = PeriodConversions.toMap(period, null);

        assertEquals("Zero years expected", 0, map.get(YEARS_KEY));
        assertEquals("Zero months expected", 0, map.get(MONTHS_KEY));
        assertEquals("Zero days expected", 0, map.get(DAYS_KEY));
    }

    /**
     * Verify that negative components are preserved.
     */
    @Test
    public void testToMap_NegativeValues() {
        Period period = Period.of(-1, -3, -15);
        Map<String, Object> map = PeriodConversions.toMap(period, null);

        assertEquals("Negative years must be preserved", -1, map.get(YEARS_KEY));
        assertEquals("Negative months must be preserved", -3, map.get(MONTHS_KEY));
        assertEquals("Negative days must be preserved", -15, map.get(DAYS_KEY));
    }

    /**
     * Verify that the map produced by {@code toMap} respects insertion order
     * (years → months → days) as required by the builder call
     * {@code CompactMap.builder().insertionOrder()}.
     */
    @Test
    public void testToMap_InsertionOrder() {
        Period period = Period.of(1, 2, 3);
        Map<String, Object> map = PeriodConversions.toMap(period, null);

        // The iterator of entrySet reflects insertion order for CompactMap
        Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator();

        assertTrue("Map must contain at least one entry", it.hasNext());
        assertEquals("First key must be YEARS", YEARS_KEY, it.next().getKey());

        assertTrue("Map must contain a second entry", it.hasNext());
        assertEquals("Second key must be MONTHS", MONTHS_KEY, it.next().getKey());

        assertTrue("Map must contain a third entry", it.hasNext());
        assertEquals("Third key must be DAYS", DAYS_KEY, it.next().getKey());

        assertFalse("No more entries expected", it.hasNext());
    }

    /**
     * Verify that very large values are stored without loss.
     */
    @Test
    public void testToMap_LargeValues() {
        // Period does not normalize, so we can use large numbers directly.
        Period period = Period.of(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        Map<String, Object> map = PeriodConversions.toMap(period, null);

        assertEquals(Integer.MAX_VALUE, map.get(YEARS_KEY));
        assertEquals(Integer.MAX_VALUE, map.get(MONTHS_KEY));
        assertEquals(Integer.MAX_VALUE, map.get(DAYS_KEY));
    }

    /**
     * Passing {@code null} as the source object must result in a
     * {@link NullPointerException} because the method immediately dereferences
     * the casted Period.
     */
    @Test(expected = NullPointerException.class)
    public void testToMap_NullPeriod() {
        PeriodConversions.toMap(null, null);
    }

    /**
     * Passing an object that is not a {@link Period} must cause a
     * {@link ClassCastException} at the cast site.
     */
    @Test(expected = ClassCastException.class)
    public void testToMap_WrongSourceType() {
        String notAPeriod = "I am not a Period";
        PeriodConversions.toMap(notAPeriod, null);
    }

    /**
     * The returned map should be mutable – we can add a new entry without
     * affecting the original three keys.
     */
    @Test
    public void testToMap_MapIsMutable() {
        Period period = Period.of(0, 0, 1);
        Map<String, Object> map = PeriodConversions.toMap(period, null);

        assertNull("Custom key must not exist initially", map.get("extra"));
        map.put("extra", "value");
        assertEquals("Custom entry must be stored", "value", map.get("extra"));
        assertEquals("Original size must increase by one", 4, map.size());
    }
}
