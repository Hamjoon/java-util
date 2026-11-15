package com.cedarsoftware.util.convert;

import java.time.Period;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link PeriodConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Normal conversion of a {@link Period} to a {@link Map}</li>
 *   <li>Conversion of {@link Period#ZERO}</li>
 *   <li>Behaviour when {@code null} is supplied as the source object</li>
 *   <li>Behaviour when an object of an unsupported type is supplied</li>
 * </ul>
 *
 * The class under test is package‑private, therefore the test class lives in the same package.
 */
public class PeriodConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // Converter requires a ConverterOptions instance.  An empty options object is sufficient for these tests.
        converter = new Converter(new ConverterOptions());
    }

    /**
     * Verify that a regular {@link Period} is converted to a map containing a single entry.
     * The key must be {@code MapConversions.PERIOD} and the value must be the ISO‑8601 string
     * produced by {@link Period#toString()}.
     */
    @Test
    public void testToMap_WithTypicalPeriod() {
        Period period = Period.of(2, 5, 10); // P2Y5M10D
        Map<String, Object> result = PeriodConversions.toMap(period, converter);

        assertNotNull("Result map must not be null", result);
        assertEquals("Result map must contain exactly one entry", 1, result.size());

        assertTrue("Result map must contain the PERIOD key", result.containsKey(PERIOD));
        assertEquals("String representation must match Period.toString()", period.toString(),
                result.get(PERIOD));
    }

    /**
     * Verify that {@link Period#ZERO} is handled correctly.
     * The ISO‑8601 representation of a zero period is {@code "P0D"}.
     */
    @Test
    public void testToMap_WithZeroPeriod() {
        Period zero = Period.ZERO; // P0D
        Map<String, Object> result = PeriodConversions.toMap(zero, converter);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("P0D", result.get(PERIOD));
    }

    /**
     * Passing {@code null} as the source object should result in a {@link NullPointerException}
     * because the implementation dereferences the casted {@link Period}.
     */
    @Test(expected = NullPointerException.class)
    public void testToMap_WithNullSource() {
        PeriodConversions.toMap(null, converter);
    }

    /**
     * Supplying an object that is not a {@link Period} must cause a {@link ClassCastException}
     * (the method performs an unchecked cast).
     */
    @Test(expected = ClassCastException.class)
    public void testToMap_WithInvalidSourceType() {
        String notAPeriod = "I am not a period";
        PeriodConversions.toMap(notAPeriod, converter);
    }
}
