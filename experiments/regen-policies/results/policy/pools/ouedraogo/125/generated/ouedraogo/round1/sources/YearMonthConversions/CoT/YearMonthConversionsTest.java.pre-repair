package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import java.time.YearMonth;
import java.util.Map;

/**
 * Test suite for {@link YearMonthConversions}.
 *
 * The tests are written for JUnit 4 and cover:
 * <ul>
 *   <li>Extraction of the public API of the class.</li>
 *   <li>Basic functional verification of {@code toMap}.</li>
 *   <li>Edge‑case and exceptional scenarios such as null arguments and wrong types.</li>
 * </ul>
 *
 * The class under test is package‑private and has a private constructor, therefore the test
 * resides in the same package to gain access to the static method.
 */
public class YearMonthConversionsTest
{
    /** The Converter instance required by the API.  The implementation does not use it,
     *  so a simple stub (null) is sufficient for the tests. */
    private Converter stubConverter;

    @Before
    public void setUp() {
        // The actual Converter constructor requires a ConverterOptions instance.
        // For the purpose of these tests the converter is never used, therefore we can safely
        // pass a null reference (the method under test does not dereference it).
        stubConverter = null;
    }

    @After
    public void tearDown() {
        stubConverter = null;
    }

    // -----------------------------------------------------------------------
    // 1.  Public method extraction (documented for reference)
    // -----------------------------------------------------------------------
    /*
     * The only public (package‑private static) method in YearMonthConversions is:
     *
     *   static Map toMap(Object from, Converter converter)
     *
     * It returns a Map<String,Object> containing two entries:
     *   "year"  -> Integer (the year component)
     *   "month" -> Integer (the month value 1‑12)
     *
     * The method casts the first argument to java.time.YearMonth without any
     * validation, so a ClassCastException is thrown if the argument is not a
     * YearMonth instance.  If the argument is null, a NullPointerException is
     * thrown at the cast site.
     *
     * The second argument (Converter) is not used at all; it may be null.
     */

    // -----------------------------------------------------------------------
    // 2.  Basic functional test
    // -----------------------------------------------------------------------
    @Test
    public void testToMapBasicFunctionality() {
        YearMonth ym = YearMonth.of(2023, 5); // May 2023
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) YearMonthConversions.toMap(ym, stubConverter);

        assertNotNull("Result map must not be null", result);
        assertEquals("Map must contain exactly two entries", 2, result.size());

        assertTrue("Map must contain key 'year'", result.containsKey("year"));
        assertTrue("Map must contain key 'month'", result.containsKey("month"));

        assertEquals("Year value must match", 2023, result.get("year"));
        assertEquals("Month value must match", 5, result.get("month"));
    }

    // -----------------------------------------------------------------------
    // 3.  Edge‑case tests
    // -----------------------------------------------------------------------
    @Test(expected = NullPointerException.class)
    public void testToMapNullFromThrowsNPE() {
        // The cast (YearMonth) null triggers a NullPointerException before any
        // logic is executed.
        YearMonthConversions.toMap(null, stubConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToMapWrongTypeThrowsClassCastException() {
        // Passing an object that is not a YearMonth should cause a ClassCastException.
        String notAYearMonth = "2023-05";
        YearMonthConversions.toMap(notAYearMonth, stubConverter);
    }

    @Test
    public void testToMapNullConverterIsAccepted() {
        // The converter argument is not used; passing null must not affect the result.
        YearMonth ym = YearMonth.of(1999, 12);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) YearMonthConversions.toMap(ym, null);

        assertNotNull(result);
        assertEquals(1999, result.get("year"));
        assertEquals(12, result.get("month"));
    }

    // -----------------------------------------------------------------------
    // 4.  Additional sanity checks (immutability of returned map)
    // -----------------------------------------------------------------------
    @Test
    public void testReturnedMapIsMutableAndIndependent() {
        YearMonth ym = YearMonth.of(2000, 1);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) YearMonthConversions.toMap(ym, stubConverter);

        // Modify the map and ensure it does not affect a new conversion
        map.put("year", 1234);
        map.put("extra", "value");

        assertEquals(1234, map.get("year"));
        assertEquals("value", map.get("extra"));
        assertEquals(3, map.size());

        // New conversion should give original values again
        @SuppressWarnings("unchecked")
        Map<String, Object> newMap = (Map<String, Object>) YearMonthConversions.toMap(ym, stubConverter);
        assertEquals(2000, newMap.get("year"));
        assertEquals(1, newMap.get("month"));
        assertFalse(newMap.containsKey("extra"));
    }
}
