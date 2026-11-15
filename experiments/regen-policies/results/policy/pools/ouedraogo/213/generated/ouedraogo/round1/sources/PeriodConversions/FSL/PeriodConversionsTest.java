package com.cedarsoftware.util.convert;

import static com.cedarsoftware.util.convert.MapConversions.PERIOD;
import static org.junit.Assert.*;

import java.time.Period;
import java.util.Iterator;
import java.util.Map;

import org.junit.Test;

/**
 * Unit tests for {@link PeriodConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical positive periods</li>
 *   <li>Zero period</li>
 *   <li>Negative periods</li>
 *   <li>Null source value (expected NPE)</li>
 *   <li>Invalid source type (expected ClassCastException)</li>
 *   <li>Map characteristics (size, key, value, iteration order)</li>
 * </ul>
 *
 * The {@code Converter} argument of {@code PeriodConversions.toMap} is not used by the
 * implementation, therefore the tests pass {@code null} for simplicity.
 */
public class PeriodConversionsTest {

    /**
     * Helper that invokes the method under test with a {@code null} Converter.
     */
    private static Map<String, Object> invokeToMap(Object source) {
        return PeriodConversions.toMap(source, null);
    }

    @Test
    public void testToMapWithPositivePeriod() {
        Period period = Period.of(1, 2, 3); // P1Y2M3D
        Map<String, Object> map = invokeToMap(period);

        assertNotNull("Resulting map must not be null", map);
        assertEquals("Map must contain exactly one entry", 1, map.size());

        assertTrue("Map must contain the expected key", map.containsKey(PERIOD));
        assertEquals("Value must be the ISO‑8601 representation of the period",
                period.toString(), map.get(PERIOD));
    }

    @Test
    public void testToMapWithZeroPeriod() {
        Period period = Period.ZERO; // P0D
        Map<String, Object> map = invokeToMap(period);

        assertEquals("P0D", map.get(PERIOD));
    }

    @Test
    public void testToMapWithNegativePeriod() {
        // Period.of allows negative components.
        Period period = Period.of(-1, -2, -3); // P-1Y-2M-3D
        Map<String, Object> map = invokeToMap(period);

        assertEquals("Negative period string must be preserved",
                period.toString(), map.get(PERIOD));
    }

    @Test
    public void testMapIterationOrderPreservesInsertion() {
        Period period = Period.of(5, 0, 10);
        Map<String, Object> map = invokeToMap(period);

        Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator();
        assertTrue("Map should have at least one entry", it.hasNext());

        Map.Entry<String, Object> entry = it.next();
        assertEquals("First (and only) key must be PERIOD constant", PERIOD, entry.getKey());
        assertEquals("First (and only) value must be period string", period.toString(), entry.getValue());

        assertFalse("No more entries should exist", it.hasNext());
    }

    @Test(expected = NullPointerException.class)
    public void testToMapWithNullSourceThrowsNPE() {
        invokeToMap(null);
    }

    @Test(expected = ClassCastException.class)
    public void testToMapWithInvalidSourceTypeThrowsClassCastException() {
        invokeToMap("not a Period");
    }

    @Test
    public void testToMapIgnoresConverterArgument() {
        Period period = Period.of(2, 4, 6);
        // Pass a non‑null Converter instance (the class has a public constructor that takes ConverterOptions).
        // Since ConverterOptions is not part of the public test contract, we create one via the default constructor.
        // The method does not use it, so any instance is acceptable.
        Converter dummyConverter = new Converter(new ConverterOptions());

        Map<String, Object> map = PeriodConversions.toMap(period, dummyConverter);

        assertEquals("Map must still contain the correct period string when a Converter is supplied",
                period.toString(), map.get(PERIOD));
    }
}
