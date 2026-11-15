package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

import java.time.YearMonth;
import java.util.Map;

/**
 * Unit tests for {@link YearMonthConversions}.
 *
 * The test class resides in the same package as the class under test
 * so that it can invoke the package‑private {@code toMap} method.
 */
public class YearMonthConversionsTest {

    /**
     * Verify normal conversion of a {@link YearMonth} to a Map.
     */
    @Test
    public void testToMapBasicFunctionality() {
        YearMonth ym = YearMonth.of(2023, 5);
        Map result = YearMonthConversions.toMap(ym, null);   // converter is unused
        assertNotNull("Resulting map must not be null", result);
        assertEquals("Map should contain exactly one entry", 1, result.size());
        assertTrue("Map must contain the YEAR_MONTH key", result.containsKey(YEAR_MONTH));
        assertEquals("Value must be the YearMonth's ISO‑8601 string",
                     ym.toString(), result.get(YEAR_MONTH));
    }

    /**
     * Passing {@code null} as the source object should cause a NullPointerException
     * because the method dereferences the casted value.
     */
    @Test(expected = NullPointerException.class)
    public void testToMap_NullFrom_ThrowsNPE() {
        YearMonthConversions.toMap(null, null);
    }

    /**
     * Supplying an object of the wrong type must result in a ClassCastException.
     */
    @Test(expected = ClassCastException.class)
    public void testToMap_WrongType_ThrowsClassCastException() {
        YearMonthConversions.toMap("2023-05", null);   // not a YearMonth
    }

    /**
     * The implementation creates a {@link java.util.LinkedHashMap}.  This test
     * confirms the concrete type and that the map preserves insertion order.
     */
    @Test
    public void testToMap_ReturnsLinkedHashMap() {
        YearMonth ym = YearMonth.of(2020, 12);
        Map result = YearMonthConversions.toMap(ym, null);
        assertTrue("Returned map should be a LinkedHashMap",
                   result instanceof java.util.LinkedHashMap);

        // Append a second entry and verify iteration order
        result.put("extra", "value");
        java.util.Iterator<String> iterator = result.keySet().iterator();
        assertEquals("First key must be YEAR_MONTH", YEAR_MONTH, iterator.next());
        assertEquals("Second key must be the newly added one", "extra", iterator.next());
    }

    /**
     * The converter argument is not used by the method; passing {@code null}
     * must not affect the result.
     */
    @Test
    public void testToMap_WithNullConverterWorks() {
        YearMonth ym = YearMonth.of(1999, 1);
        Map result = YearMonthConversions.toMap(ym, null);
        assertEquals(ym.toString(), result.get(YEAR_MONTH));
    }
}
