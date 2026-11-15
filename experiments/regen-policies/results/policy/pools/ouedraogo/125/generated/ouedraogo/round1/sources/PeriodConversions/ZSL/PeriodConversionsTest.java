package com.cedarsoftware.util.convert;
import com.cedarsoftware.util.CompactMap;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.time.Period;
import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class PeriodConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // The toMap implementation does not use the Converter, but it must be non‑null.
        // The public API allows passing a null options object.
        this.converter = new Converter(null);
    }

    /**
     * Verify that a normal Period is converted to a map containing the expected keys
     * and values.
     */
    @Test
    public void testToMapBasic() {
        Period period = Period.of(1, 2, 3); // 1 year, 2 months, 3 days
        Map<String, Object> map = PeriodConversions.toMap(period, converter);

        assertNotNull("Resulting map must not be null", map);
        assertEquals("Map must contain exactly three entries", 3, map.size());

        // Expected keys – they are defined in MapConversions; using the literal values
        // because the constants are simple strings.
        assertTrue("Map must contain key 'years'", map.containsKey("years"));
        assertTrue("Map must contain key 'months'", map.containsKey("months"));
        assertTrue("Map must contain key 'days'", map.containsKey("days"));

        assertEquals("Year value must match", Integer.valueOf(1), map.get("years"));
        assertEquals("Month value must match", Integer.valueOf(2), map.get("months"));
        assertEquals("Day value must match", Integer.valueOf(3), map.get("days"));
    }

    /**
     * Verify that the map preserves insertion order (years → months → days).
     */
    @Test
    public void testInsertionOrderPreserved() {
        Period period = Period.of(10, 20, 30);
        Map<String, Object> map = PeriodConversions.toMap(period, converter);

        assertTrue("Returned map should be an instance of CompactMap",
                map instanceof CompactMap);

        // Iterate over the entry set and capture the order of keys.
        Iterator<String> keyIterator = map.keySet().iterator();

        assertTrue("Map must contain at least one key", keyIterator.hasNext());
        assertEquals("First key must be 'years'", "years", keyIterator.next());

        assertTrue("Map must contain a second key", keyIterator.hasNext());
        assertEquals("Second key must be 'months'", "months", keyIterator.next());

        assertTrue("Map must contain a third key", keyIterator.hasNext());
        assertEquals("Third key must be 'days'", "days", keyIterator.next());

        assertFalse("No more keys should be present", keyIterator.hasNext());
    }

    /**
     * Verify that negative values are handled correctly.
     */
    @Test
    public void testNegativeValues() {
        Period period = Period.of(-5, -6, -7);
        Map<String, Object> map = PeriodConversions.toMap(period, converter);

        assertEquals(Integer.valueOf(-5), map.get("years"));
        assertEquals(Integer.valueOf(-6), map.get("months"));
        assertEquals(Integer.valueOf(-7), map.get("days"));
    }

    /**
     * Passing a null Period should result in a NullPointerException because the
     * implementation dereferences the casted object.
     */
    @Test(expected = NullPointerException.class)
    public void testNullInputThrowsNPE() {
        PeriodConversions.toMap(null, converter);
    }

    /**
     * Passing an object that is not a java.time.Period should cause a ClassCastException.
     */
    @Test(expected = ClassCastException.class)
    public void testInvalidTypeThrowsClassCastException() {
        String notAPeriod = "I am not a Period";
        PeriodConversions.toMap(notAPeriod, converter);
    }
}
