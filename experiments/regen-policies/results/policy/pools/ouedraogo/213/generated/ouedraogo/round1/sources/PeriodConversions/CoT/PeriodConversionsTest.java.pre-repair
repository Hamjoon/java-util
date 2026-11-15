package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.time.Period;
import java.util.Map;

/**
 * Unit tests for {@link PeriodConversions}.
 *
 * The tests are placed in the same package as {@code PeriodConversions}
 * so that the package‑private static method {@code toMap} is accessible.
 *
 * The test suite covers:
 *   • Normal conversion of a positive Period
 *   • Conversion of {@code Period.ZERO}
 *   • Conversion of a negative Period
 *   • Behaviour when the source object is {@code null}
 *   • Behaviour when the source object is of an incompatible type
 *   • Behaviour when the {@code Converter} argument is {@code null}
 *
 * The {@code Converter} argument is not used by {@code toMap}, therefore
 * we pass {@code null} in most tests.  This also demonstrates that the
 * method does not depend on any state of the {@code Converter} instance.
 */
public class PeriodConversionsTest {

    // -----------------------------------------------------------------
    // Helper constants
    // -----------------------------------------------------------------
    private static final String PERIOD_KEY = MapConversions.PERIOD; // "period"

    // -----------------------------------------------------------------
    // 1. List of public (exposed) methods in PeriodConversions
    // -----------------------------------------------------------------
    // The class itself does not expose any public methods, but it contains:
    //   • private PeriodConversions()                     – private constructor
    //   • static Map<String,Object> toMap(Object, Converter) – package‑private static method
    //
    // The tests exercise the static toMap method directly.

    // -----------------------------------------------------------------
    // 2. Basic functionality tests
    // -----------------------------------------------------------------
    @Test
    public void testToMapWithPositivePeriod() {
        Period period = Period.of(1, 2, 3);               // P1Y2M3D
        Map<String, Object> result = PeriodConversions.toMap(period, null);

        assertNotNull("Result map must not be null", result);
        assertEquals("Map should contain exactly one entry", 1, result.size());
        assertTrue("Map must contain the key defined in MapConversions.PERIOD",
                result.containsKey(PERIOD_KEY));
        assertEquals("String representation must follow ISO‑8601 format",
                "P1Y2M3D", result.get(PERIOD_KEY));
    }

    @Test
    public void testToMapWithZeroPeriod() {
        Period period = Period.ZERO;                     // P0D
        Map<String, Object> result = PeriodConversions.toMap(period, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("P0D", result.get(PERIOD_KEY));
    }

    @Test
    public void testToMapWithNegativePeriod() {
        // Negative values are allowed; toString produces ISO‑8601 with sign
        Period period = Period.of(-1, -2, -3);            // P-1Y-2M-3D
        Map<String, Object> result = PeriodConversions.toMap(period, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("P-1Y-2M-3D", result.get(PERIOD_KEY));
    }

    // -----------------------------------------------------------------
    // 3. Edge‑case / exception handling tests
    // -----------------------------------------------------------------
    @Test(expected = NullPointerException.class)
    public void testToMapWithNullFromThrowsNPE() {
        // The method casts the argument to Period and then calls toString(),
        // which will throw NPE when the casted reference is null.
        PeriodConversions.toMap(null, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToMapWithInvalidFromTypeThrowsClassCastException() {
        // Passing an object that is not a Period should result in a
        // ClassCastException at the cast site.
        PeriodConversions.toMap(Integer.valueOf(42), null);
    }

    @Test
    public void testToMapWithNullConverterDoesNotAffectResult() {
        // The Converter argument is not used; passing null should behave
        // identically to passing a real Converter instance.
        Period period = Period.of(5, 0, 0);
        Map<String, Object> result = PeriodConversions.toMap(period, null);

        assertNotNull(result);
        assertEquals("P5Y", result.get(PERIOD_KEY));
    }

    // -----------------------------------------------------------------
    // 4. Private constructor coverage (optional)
    // -----------------------------------------------------------------
    @Test
    public void testPrivateConstructorIsInaccessible() throws Exception {
        // Use reflection to ensure the private constructor exists and is not
        // accidentally made public. The test simply invokes it to achieve
        // 100 % code‑coverage for the constructor.
        java.lang.reflect.Constructor<PeriodConversions> ctor =
                PeriodConversions.class.getDeclaredConstructor();
        assertTrue("Constructor must be private", java.lang.reflect.Modifier.isPrivate(ctor.getModifiers()));
        ctor.setAccessible(true);
        // The constructor does nothing; just instantiate.
        Object instance = ctor.newInstance();
        assertNull("Utility class should not expose state", instance);
    }
}
