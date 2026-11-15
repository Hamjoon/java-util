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
 * JUnit‑4 test suite for {@code YearMonthConversions}.
 *
 * The tests cover:
 *   1. Normal operation – a valid {@code YearMonth} is converted to a map containing
 *      the correct {@code "year"} and {@code "month"} entries.
 *   2. Edge cases and error handling:
 *        • {@code null} as the source object (expected {@code NullPointerException}).
 *        • An object of an incompatible type (expected {@code ClassCastException}).
 *        • {@code null} for the {@code Converter} argument (the method does not use the
 *          converter, so conversion should still succeed).
 *   3. Verification that the returned map is a {@code CompactMap} built with insertion‑order
 *      semantics (the iteration order of entries is year → month).
 */
public class YearMonthConversionsTest {

    private YearMonth sampleYearMonth;
    private Converter dummyConverter;   // not used by the method but kept for API completeness

    @Before
    public void setUp() {
        // Example value: April 2023
        sampleYearMonth = YearMonth.of(2023, 4);
        // The method never touches the Converter, so we can safely use null.
        dummyConverter = null;
    }

    /**
     * Basic happy‑path test: a proper {@code YearMonth} should be converted to a map
     * containing exactly two entries – "year" and "month" – with the correct values.
     */
    @Test
    public void testToMap_BasicConversion() {
        Map<String, Object> result = YearMonthConversions.toMap(sampleYearMonth, dummyConverter);

        assertNotNull("Resulting map must not be null", result);
        assertEquals("Map must contain exactly two entries", 2, result.size());

        // Verify contents
        assertTrue("Map must contain key \"year\"", result.containsKey("year"));
        assertTrue("Map must contain key \"month\"", result.containsKey("month"));
        assertEquals("Year value must match the source YearMonth",
                Integer.valueOf(2023), result.get("year"));
        assertEquals("Month value must match the source YearMonth",
                Integer.valueOf(4), result.get("month"));

        // Verify insertion order (year first, month second)
        Iterator<Entry<String, Object>> it = result.entrySet().iterator();
        Entry<String, Object> first = it.next();
        Entry<String, Object> second = it.next();
        assertEquals("First entry key should be \"year\"", "year", first.getKey());
        assertEquals("Second entry key should be \"month\"", "month", second.getKey());

        // Ensure the implementation is a CompactMap (builder returns that type)
        assertTrue("Returned map should be an instance of CompactMap",
                result instanceof CompactMap);
    }

    /**
     * Passing {@code null} as the source object should result in a {@code NullPointerException}
     * because the method performs an unchecked cast to {@code YearMonth}.
     */
    @Test(expected = NullPointerException.class)
    public void testToMap_NullSource() {
        YearMonthConversions.toMap(null, dummyConverter);
    }

    /**
     * Passing an object that is not a {@code YearMonth} must cause a {@code ClassCastException}
     * due to the direct cast performed inside the method.
     */
    @Test(expected = ClassCastException.class)
    public void testToMap_IncompatibleSource() {
        Integer notAYearMonth = Integer.valueOf(42);
        YearMonthConversions.toMap(notAYearMonth, dummyConverter);
    }

    /**
     * The {@code Converter} argument is unused by the implementation; therefore a {@code null}
     * value should be tolerated and the conversion should still succeed.
     */
    @Test
    public void testToMap_NullConverter() {
        Map<String, Object> result = YearMonthConversions.toMap(sampleYearMonth, null);

        assertNotNull("Resulting map must not be null even when converter is null", result);
        assertEquals("Map must still contain exactly two entries", 2, result.size());
        assertEquals("Year entry must be correct", Integer.valueOf(2023), result.get("year"));
        assertEquals("Month entry must be correct", Integer.valueOf(4), result.get("month"));
    }
}
