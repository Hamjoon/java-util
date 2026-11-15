package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.time.Period;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Test suite for {@link PeriodConversions}.
 * The test class resides in the same package so it can access the
 * package‑private static method {@code toMap}.
 */
public class PeriodConversionsTest {

    // -----------------------------------------------------------------
    //  Basic functionality test
    // -----------------------------------------------------------------
    @Test
    public void testToMapBasic() {
        Period period = Period.of(2, 5, 10);
        Map<String, Object> map = PeriodConversions.toMap(period, null);

        assertEquals("Map should contain exactly three entries", 3, map.size());
        assertEquals("Years value mismatch", 2, map.get(MapConversions.YEARS));
        assertEquals("Months value mismatch", 5, map.get(MapConversions.MONTHS));
        assertEquals("Days value mismatch", 10, map.get(MapConversions.DAYS));
    }

    // -----------------------------------------------------------------
    //  Edge‑case / exception tests
    // -----------------------------------------------------------------
    @Test(expected = NullPointerException.class)
    public void testToMapNullFromThrowsNPE() {
        // 'from' is null → cast succeeds (null) but period.getYears() triggers NPE
        PeriodConversions.toMap(null, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToMapInvalidTypeThrowsClassCastException() {
        // Passing a non‑Period object should cause a ClassCastException
        PeriodConversions.toMap("I am not a Period", null);
    }

    @Test
    public void testToMapNullConverterIsAccepted() {
        // The converter argument is not used; passing null must not affect the result
        Period period = Period.of(1, 2, 3);
        Map<String, Object> map = PeriodConversions.toMap(period, null);

        assertNotNull("Returned map must not be null", map);
        assertEquals(1, map.get(MapConversions.YEARS));
        assertEquals(2, map.get(MapConversions.MONTHS));
        assertEquals(3, map.get(MapConversions.DAYS));
    }

    @Test
    public void testToMapNegativeValues() {
        Period period = Period.of(-4, -7, -15);
        Map<String, Object> map = PeriodConversions.toMap(period, null);

        assertEquals(-4, map.get(MapConversions.YEARS));
        assertEquals(-7, map.get(MapConversions.MONTHS));
        assertEquals(-15, map.get(MapConversions.DAYS));
    }

    @Test
    public void testToMapExtremeValues() {
        Period period = Period.of(Integer.MAX_VALUE, 11, 31);
        Map<String, Object> map = PeriodConversions.toMap(period, null);

        assertEquals(Integer.MAX_VALUE, map.get(MapConversions.YEARS));
        assertEquals(11, map.get(MapConversions.MONTHS));
        assertEquals(31, map.get(MapConversions.DAYS));
    }

    // -----------------------------------------------------------------
    //  Order preservation test (CompactMap built with insertionOrder())
    // -----------------------------------------------------------------
    @Test
    public void testInsertionOrderIsPreserved() {
        Period period = Period.of(9, 8, 7);
        Map<String, Object> map = PeriodConversions.toMap(period, null);

        // Collect the iteration order of the keys
        List<String> actualOrder = new ArrayList<>();
        for (String key : map.keySet()) {
            actualOrder.add(key);
        }

        List<String> expectedOrder = Arrays.asList(
                MapConversions.YEARS,
                MapConversions.MONTHS,
                MapConversions.DAYS
        );

        assertEquals("Keys should be iterated in insertion order", expectedOrder, actualOrder);
    }
}
