package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

import java.time.YearMonth;
import java.util.Map;
import java.util.LinkedHashMap;

import static com.cedarsoftware.util.convert.MapConversions.YEAR_MONTH;

/**
 * Unit tests for {@link YearMonthConversions}.
 * These tests exercise the {@code toMap} method with a variety of inputs
 * and verify that the returned map conforms to the contract:
 * <ul>
 *   <li>Contains exactly one entry.</li>
 *   <li>The key is {@link MapConversions#YEAR_MONTH}.</li>
 *   <li>The value is the {@code YearMonth} formatted with {@code YearMonth#toString()}.</li>
 *   <li>The concrete map implementation is {@link LinkedHashMap} (preserves insertion order).</li>
 *   <li>The supplied {@link Converter} argument is not used.</li>
 *   <li>Invalid or {@code null} inputs result in the expected exceptions.</li>
 * </ul>
 */
public class YearMonthConversionsTest {

    /**
     * Verify that a valid {@link YearMonth} is converted to a {@link LinkedHashMap}
     * containing the correct key/value pair.
     */
    @Test
    public void testToMapProducesCorrectEntry() {
        YearMonth ym = YearMonth.of(2023, 5); // "2023-05"
        Map<String, Object> result = YearMonthConversions.toMap(ym, null);

        // The method must return a map with exactly one entry.
        assertEquals("Map should contain exactly one entry", 1, result.size());

        // The concrete type must be LinkedHashMap (preserves order).
        assertTrue("Result map should be a LinkedHashMap", result instanceof LinkedHashMap);

        // Verify the key and value.
        assertTrue("Result map should contain the YEAR_MONTH key", result.containsKey(YEAR_MONTH));
        assertEquals("Value should be the YearMonth's toString()", ym.toString(), result.get(YEAR_MONTH));
    }

    /**
     * The {@code Converter} argument is not used by the implementation.
     * Supplying a non‑null instance should not affect the result.
     */
    @Test
    public void testConverterArgumentIsIgnored() {
        YearMonth ym = YearMonth.of(1999, 12);
        // Create a minimal Converter instance (options can be null – the method never touches it)
        Converter dummyConverter = new Converter(null);

        Map<String, Object> result = YearMonthConversions.toMap(ym, dummyConverter);
        assertEquals("Map should still contain the correct string representation",
                ym.toString(), result.get(YEAR_MONTH));
    }

    /**
     * Passing {@code null} as the source object should result in a {@link NullPointerException}
     * because the implementation casts the argument to {@link YearMonth} and then dereferences it.
     */
    @Test(expected = NullPointerException.class)
    public void testToMapWithNullSourceThrowsNPE() {
        YearMonthConversions.toMap(null, null);
    }

    /**
     * Passing an object that is not a {@link YearMonth} should cause a {@link ClassCastException}
     * when the method attempts to cast the argument.
     */
    @Test(expected = ClassCastException.class)
    public void testToMapWithInvalidSourceThrowsClassCastException() {
        String notAYearMonth = "2023-05";
        YearMonthConversions.toMap(notAYearMonth, null);
    }

    /**
     * Ensure that the map preserves insertion order when more than one entry would exist.
     * Although {@code YearMonthConversions} currently adds only a single entry,
     * this test documents the expected map implementation for future extensions.
     */
    @Test
    public void testMapPreservesInsertionOrder() {
        YearMonth ym = YearMonth.of(2020, 1);
        Map<String, Object> result = YearMonthConversions.toMap(ym, null);

        // Because the map is a LinkedHashMap, its iterator order matches insertion order.
        // With a single entry, we simply verify that the iterator returns the expected key first.
        String firstKey = result.keySet().iterator().next();
        assertEquals("The first (and only) key should be YEAR_MONTH", YEAR_MONTH, firstKey);
    }
}
