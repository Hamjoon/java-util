package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JUnit‑4 test suite for {@link YearMonthConversions}.
 *
 * Covers:
 *   • normal conversion to a map
 *   • handling of null input
 *   • handling of wrong input type
 *   • edge case of month values with a leading zero
 *   • verification that the returned map is a LinkedHashMap
 *
 * The {@code Converter} argument is not used by the implementation,
 * therefore {@code null} is supplied where appropriate.
 */
public class YearMonthConversionsTest {

    /** The constant key used by {@code YearMonthConversions}. */
    private static final String YEAR_MONTH = MapConversions.YEAR_MONTH;

    /** Normal usage – a typical YearMonth value. */
    @Test
    public void testToMapBasic() {
        YearMonth ym = YearMonth.of(2021, 5);
        Map<String, Object> result = YearMonthConversions.toMap(ym, null);

        assertNotNull("Result map must not be null", result);
        assertEquals("Result map should contain exactly one entry", 1, result.size());
        assertEquals("2021-05", result.get(YEAR_MONTH));
    }

    /** Passing {@code null} as the source object must raise NPE because the method calls {@code toString()} on it. */
    @Test(expected = NullPointerException.class)
    public void testToMapNullFrom() {
        YearMonthConversions.toMap(null, null);
    }

    /** Supplying an object that is not a {@code YearMonth} must raise a ClassCastException. */
    @Test(expected = ClassCastException.class)
    public void testToMapInvalidType() {
        YearMonthConversions.toMap("not a YearMonth", null);
    }

    /** Edge case – February of a leap year; ensures leading zero month is preserved. */
    @Test
    public void testToMapLeapYearFebruary() {
        YearMonth ym = YearMonth.of(2020, 2);
        Map<String, Object> result = YearMonthConversions.toMap(ym, null);

        assertEquals("2020-02", result.get(YEAR_MONTH));
    }

    /** Verify that the concrete map implementation returned is a LinkedHashMap. */
    @Test
    public void testMapImplementation() {
        YearMonth ym = YearMonth.of(2022, 12);
        Map<String, Object> result = YearMonthConversions.toMap(ym, null);

        assertTrue("Returned map should be a LinkedHashMap",
                result instanceof LinkedHashMap);
    }
}
