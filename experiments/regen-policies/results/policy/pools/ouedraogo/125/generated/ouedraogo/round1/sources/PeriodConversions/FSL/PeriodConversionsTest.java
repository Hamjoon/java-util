package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.time.Period;
import java.util.Map;

import org.junit.Test;

/**
 * Unit tests for {@link PeriodConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Conversion of a {@link Period} with positive values.</li>
 *   <li>Conversion of a {@link Period} with zero values.</li>
 *   <li>Conversion of a {@link Period} with negative values.</li>
 *   <li>Behaviour when {@code null} is passed as the {@link Converter} argument (the method does not use it).</li>
 *   <li>Verification that the resulting map contains exactly the three expected entries and respects insertion order.</li>
 * </ul>
 */
public class PeriodConversionsTest {

    /**
     * Helper that invokes {@link PeriodConversions#toMap(Object, Converter)} and returns the result.
     */
    private Map<String, Object> convert(Period period, Converter converter) {
        // The method under test expects an Object that is a Period.
        return PeriodConversions.toMap(period, converter);
    }

    @Test
    public void testToMap_WithPositiveValues() {
        Period period = Period.of(5, 3, 12); // 5 years, 3 months, 12 days
        Map<String, Object> map = convert(period, null);

        // Expected keys are defined in MapConversions (static constants). We use the same strings.
        assertEquals(3, map.size());
        assertTrue(map.containsKey(MapConversions.YEARS));
        assertTrue(map.containsKey(MapConversions.MONTHS));
        assertTrue(map.containsKey(MapConversions.DAYS));

        assertEquals(5, map.get(MapConversions.YEARS));
        assertEquals(3, map.get(MapConversions.MONTHS));
        assertEquals(12, map.get(MapConversions.DAYS));

        // Verify insertion order – the builder is configured with insertionOrder().
        // The iteration order of entrySet should match the order we inserted.
        String[] expectedOrder = {MapConversions.YEARS, MapConversions.MONTHS, MapConversions.DAYS};
        int i = 0;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            assertEquals(expectedOrder[i++], entry.getKey());
        }
    }

    @Test
    public void testToMap_WithZeroValues() {
        Period period = Period.of(0, 0, 0);
        Map<String, Object> map = convert(period, null);

        assertEquals(3, map.size());
        assertEquals(0, map.get(MapConversions.YEARS));
        assertEquals(0, map.get(MapConversions.MONTHS));
        assertEquals(0, map.get(MapConversions.DAYS));
    }

    @Test
    public void testToMap_WithNegativeValues() {
        // Period.of allows negative values; they are normalized internally.
        // For testing we construct a period with explicit negatives.
        Period period = Period.of(-2, -4, -10);
        Map<String, Object> map = convert(period, null);

        assertEquals(3, map.size());
        assertEquals(-2, map.get(MapConversions.YEARS));
        assertEquals(-4, map.get(MapConversions.MONTHS));
        assertEquals(-10, map.get(MapConversions.DAYS));
    }

    @Test
    public void testToMap_WithNonNullConverterArgument() {
        // The converter argument is not used by the implementation, but we still pass a real instance
        // to ensure the method works with a non‑null converter.
        ConverterOptions options = new ConverterOptions(); // assuming a public no‑arg ctor
        Converter converter = new Converter(options);

        Period period = Period.of(1, 2, 3);
        Map<String, Object> map = convert(period, converter);

        assertEquals(1, map.get(MapConversions.YEARS));
        assertEquals(2, map.get(MapConversions.MONTHS));
        assertEquals(3, map.get(MapConversions.DAYS));
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_WithInvalidFromObject() {
        // The method casts the first argument to Period without checking.
        // Supplying an object of another type should result in a ClassCastException.
        Object notAPeriod = "I am not a Period";
        PeriodConversions.toMap(notAPeriod, null);
    }
}
