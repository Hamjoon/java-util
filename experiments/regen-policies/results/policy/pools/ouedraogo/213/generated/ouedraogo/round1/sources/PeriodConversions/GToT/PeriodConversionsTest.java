package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

import java.time.Period;
import java.util.Map;

/**
 * Unit tests for {@link PeriodConversions}.
 *
 * The tests cover:
 *   • normal conversion of a {@link Period} to a {@code Map}
 *   • handling of {@code null} source objects
 *   • handling of source objects that are not {@code Period}
 *   • behaviour when the {@code Converter} argument is {@code null}
 *   • edge‑case periods (zero and negative values)
 *
 * The class {@code PeriodConversions} has package‑private visibility, therefore the test class
 * resides in the same package so it can invoke the static {@code toMap} method directly.
 */
public class PeriodConversionsTest {

    /** The key used by {@code PeriodConversions} to store the ISO‑8601 string. */
    private static final String KEY = MapConversions.PERIOD;

    /* --------------------------------------------------------------------- */
    /*  Normal usage                                                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void toMap_basicPeriod_returnsCorrectMap() {
        Period period = Period.of(1, 2, 3);               // P1Y2M3D
        Map<String, Object> result = PeriodConversions.toMap(period, null);

        Assert.assertNotNull("Result map must not be null", result);
        Assert.assertEquals("Map must contain exactly one entry", 1, result.size());
        Assert.assertTrue("Map must contain the expected key", result.containsKey(KEY));
        Assert.assertEquals("Value must be the ISO‑8601 representation",
                period.toString(), result.get(KEY));
    }

    @Test
    public void toMap_nullConverter_behavesLikeNormal() {
        Period period = Period.of(0, 0, 5);              // P5D
        Map<String, Object> result = PeriodConversions.toMap(period, null);

        Assert.assertEquals("ISO‑8601 string must match", period.toString(), result.get(KEY));
    }

    /* --------------------------------------------------------------------- */
    /*  Edge cases                                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void toMap_zeroPeriod_returnsP0D() {
        Period zero = Period.ZERO;                       // P0D
        Map<String, Object> result = PeriodConversions.toMap(zero, null);

        Assert.assertEquals("Zero period must be represented as P0D",
                "P0D", result.get(KEY));
    }

    @Test
    public void toMap_negativePeriod_returnsNegativeIsoString() {
        Period negative = Period.of(-1, -2, -3);          // P-1Y-2M-3D
        Map<String, Object> result = PeriodConversions.toMap(negative, null);

        Assert.assertEquals("Negative period must keep sign in ISO‑8601 string",
                negative.toString(), result.get(KEY));
    }

    /* --------------------------------------------------------------------- */
    /*  Exception handling                                                   */
    /* --------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void toMap_nullFrom_throwsNullPointerException() {
        // The cast to Period yields null; calling toString() on it triggers NPE
        PeriodConversions.toMap(null, null);
    }

    @Test(expected = ClassCastException.class)
    public void toMap_nonPeriodFrom_throwsClassCastException() {
        // Passing an object that is not a Period should fail at the cast
        PeriodConversions.toMap("not a period", null);
    }
}
