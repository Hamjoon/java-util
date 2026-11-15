package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import java.time.YearMonth;
import java.util.Map;

import static org.junit.Assert.*;
import static com.cedarsoftware.util.convert.MapConversions.YEAR_MONTH;

/**
 * JUnit 4 test suite for {@link YearMonthConversions}.
 *
 * <p>All tests are written in the same package as {@code YearMonthConversions}
 * because the class has package‑private visibility.</p>
 *
 * <p>Test coverage includes:</p>
 * <ul>
 *   <li>Typical use – converting a valid {@link YearMonth} to a map.</li>
 *   <li>Edge case – passing {@code null} as the source object (expects {@link NullPointerException}).</li>
 *   <li>Error case – passing an object of the wrong type (expects {@link ClassCastException}).</li>
 * </ul>
 *
 * The {@code Converter} argument is not used by the implementation, therefore a {@code null}
 * reference is sufficient for all test scenarios.
 */
public class YearMonthConversionsTest {

    private YearMonth sampleYearMonth;
    private String expectedKey;

    @Before
    public void setUp() {
        // Choose a deterministic YearMonth for repeatable tests.
        sampleYearMonth = YearMonth.of(2023, 5); // results in string "2023-05"
        expectedKey = YEAR_MONTH; // the constant imported from MapConversions
    }

    /**
     * Verify that a normal {@link YearMonth} instance is converted to a map
     * containing a single entry whose key is {@code YEAR_MONTH} and whose
     * value is the string representation of the YearMonth.
     */
    @Test
    public void testToMapWithValidYearMonth() {
        Map<String, Object> result = YearMonthConversions.toMap(sampleYearMonth, null);

        // The result must be a non‑null map with exactly one entry
        assertNotNull("Resulting map should not be null", result);
        assertEquals("Map should contain exactly one entry", 1, result.size());

        // Verify key and value
        assertTrue("Map should contain the YEAR_MONTH key", result.containsKey(expectedKey));
        assertEquals("Value should be the YearMonth's toString() representation",
                     sampleYearMonth.toString(),
                     result.get(expectedKey));
    }

    /**
     * Passing {@code null} as the source object should result in a
     * {@link NullPointerException} because the implementation casts the
     * argument to {@link YearMonth} and then calls {@code toString()} on it.
     */
    @Test(expected = NullPointerException.class)
    public void testToMapWithNullSource() {
        YearMonthConversions.toMap(null, null);
    }

    /**
     * Passing an object that is not a {@link YearMonth} should cause a
     * {@link ClassCastException} at the cast site.
     */
    @Test(expected = ClassCastException.class)
    public void testToMapWithInvalidSourceType() {
        Object notAYearMonth = "2023-05"; // a plain String, not a YearMonth
        YearMonthConversions.toMap(notAYearMonth, null);
    }
}
