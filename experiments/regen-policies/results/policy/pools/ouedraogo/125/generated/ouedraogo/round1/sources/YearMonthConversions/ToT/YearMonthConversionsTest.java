package com.cedarsoftware.util.convert;
import com.cedarsoftware.util.CompactMap;


import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.time.YearMonth;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Comprehensive JUnit‑4 test suite for {@link YearMonthConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical conversion of a valid {@link YearMonth} instance.</li>
 *   <li>Preservation of insertion order (year first, month second).</li>
 *   <li>Correct handling of edge‑case values such as February of a leap year.</li>
 *   <li>Behaviour when {@code null} is supplied as the source object.</li>
 *   <li>Behaviour when an object of an unexpected type is supplied.</li>
 * </ul>
 *
 * The {@code Converter} argument of {@code YearMonthConversions.toMap} is not used by the
 * implementation, therefore a {@code null} reference is sufficient for the tests.
 */
public class YearMonthConversionsTest {

    private Converter dummyConverter;

    @Before
    public void setUp() {
        // The implementation does not use the Converter, so we can safely pass null.
        // If a non‑null instance is desired, the class provides a public constructor that
        // requires a ConverterOptions instance, which is not part of the public API here.
        dummyConverter = null;
    }

    /**
     * Verify that a typical {@link YearMonth} is converted to a {@link Map}
     * containing the expected keys and values, and that the map preserves
     * insertion order (year first, month second).
     */
    @Test
    public void testToMapTypical() {
        YearMonth ym = YearMonth.of(2023, 7); // July 2023
        Map<String, Object> result = YearMonthConversions.toMap(ym, dummyConverter);

        // Basic sanity checks
        assertNotNull("Result map must not be null", result);
        assertEquals("Map must contain exactly two entries", 2, result.size());

        // Verify content
        assertTrue("Map must contain key 'year'", result.containsKey("year"));
        assertTrue("Map must contain key 'month'", result.containsKey("month"));
        assertEquals("Year value must match", Integer.valueOf(2023), result.get("year"));
        assertEquals("Month value must match", Integer.valueOf(7), result.get("month"));

        // Verify the concrete type is a CompactMap (implementation detail)
        assertTrue("Result should be an instance of CompactMap",
                result instanceof CompactMap);

        // Verify insertion order: year entry should appear before month entry
        Iterator<Entry<String, Object>> it = result.entrySet().iterator();
        assertTrue("Map should have at least one entry", it.hasNext());
        Entry<String, Object> first = it.next();
        assertEquals("First entry key must be 'year'", "year", first.getKey());

        assertTrue("Map should have a second entry", it.hasNext());
        Entry<String, Object> second = it.next();
        assertEquals("Second entry key must be 'month'", "month", second.getKey());

        assertFalse("Map must contain exactly two entries", it.hasNext());
    }

    /**
     * Verify conversion of an edge‑case {@link YearMonth}: February in a leap year.
     * This ensures that month values are not altered by any hidden calendar logic.
     */
    @Test
    public void testToMapLeapYearFebruary() {
        YearMonth ym = YearMonth.of(2020, 2); // February 2020 (leap year)
        Map<String, Object> result = YearMonthConversions.toMap(ym, dummyConverter);

        assertEquals("Year must be 2020", Integer.valueOf(2020), result.get("year"));
        assertEquals("Month must be 2", Integer.valueOf(2), result.get("month"));
    }

    /**
     * Passing {@code null} as the source object should result in a
     * {@link NullPointerException} because the implementation immediately
     * casts the argument to {@link YearMonth} and dereferences it.
     */
    @Test(expected = NullPointerException.class)
    public void testToMapNullSource() {
        YearMonthConversions.toMap(null, dummyConverter);
    }

    /**
     * Passing an object that is not a {@link YearMonth} must cause a
     * {@link ClassCastException}. The method does an unchecked cast,
     * so the exception is thrown before any map is created.
     */
    @Test(expected = ClassCastException.class)
    public void testToMapInvalidSourceType() {
        String notAYearMonth = "2023-07";
        YearMonthConversions.toMap(notAYearMonth, dummyConverter);
    }
}
