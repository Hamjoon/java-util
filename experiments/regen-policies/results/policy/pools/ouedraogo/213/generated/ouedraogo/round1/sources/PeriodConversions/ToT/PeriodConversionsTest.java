package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

import java.time.Period;
import java.util.Map;

/**
 * Unit tests for {@link PeriodConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical conversion of a positive Period</li>
 *   <li>Conversion of a zero-length Period</li>
 *   <li>Conversion of a Period with negative components</li>
 *   <li>Behaviour when {@code null} is supplied as the source object</li>
 *   <li>Verification that the returned map contains exactly one entry with the expected key</li>
 * </ul>
 *
 * The {@code Converter} argument is not used by {@code PeriodConversions.toMap},
 * therefore a {@code null} reference is passed in all cases.
 */
public class PeriodConversionsTest {

    /**
     * Helper to assert the structure of the map returned by {@code PeriodConversions.toMap}.
     *
     * @param period the original Period instance
     * @param map    the map produced by {@code toMap}
     */
    private void assertMapMatchesPeriod(Period period, Map<String, Object> map) {
        // The map must contain exactly one entry
        assertEquals("Map should contain exactly one entry", 1, map.size());

        // The key must be the constant defined in MapConversions
        assertTrue("Map should contain the key PERIOD",
                map.containsKey(MapConversions.PERIOD));

        // The value must be the ISO‑8601 string representation of the period
        assertEquals("Map value does not match Period.toString()",
                period.toString(), map.get(MapConversions.PERIOD));
    }

    @Test
    public void testToMap_WithPositivePeriod() {
        // Typical case: 2 years, 3 months, 10 days
        Period period = Period.of(2, 3, 10);
        Map<String, Object> result = PeriodConversions.toMap(period, null);
        assertMapMatchesPeriod(period, result);
    }

    @Test
    public void testToMap_WithZeroPeriod() {
        // Edge case: zero period (all components are zero)
        Period period = Period.of(0, 0, 0);
        Map<String, Object> result = PeriodConversions.toMap(period, null);
        assertMapMatchesPeriod(period, result);
    }

    @Test
    public void testToMap_WithNegativeComponents() {
        // Period can contain negative values; ISO‑8601 format reflects the sign
        Period period = Period.of(-1, -2, -3);
        Map<String, Object> result = PeriodConversions.toMap(period, null);
        assertMapMatchesPeriod(period, result);
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_WithNullSourceThrowsNPE() {
        // The method casts the source object to Period without a null‑check,
        // so a NullPointerException is expected.
        PeriodConversions.toMap(null, null);
    }

    @Test
    public void testToMap_ReturnsLinkedHashMapPreservingInsertionOrder() {
        // Although the method only adds a single entry, we verify that the concrete
        // type is LinkedHashMap as documented, which guarantees deterministic ordering.
        Period period = Period.of(1, 1, 1);
        Map<String, Object> result = PeriodConversions.toMap(period, null);
        assertTrue("Resulting map should be an instance of LinkedHashMap",
                result instanceof java.util.LinkedHashMap);
        // The order test is trivial with one entry, but we assert that the key
        // appears as the first (and only) entry when iterating.
        String firstKey = result.keySet().iterator().next();
        assertEquals("First key should be the PERIOD constant", MapConversions.PERIOD, firstKey);
    }
}
