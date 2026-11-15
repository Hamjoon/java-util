package com.cedarsoftware.util.convert;
import com.cedarsoftware.util.CompactLinkedMap;


import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.time.YearMonth;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link YearMonthConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical conversion of a {@link YearMonth} to a map.</li>
 *   <li>Boundary values such as the minimum and maximum supported years.</li>
 *   <li>Leap‑year handling (February month value).</li>
 *   <li>Verification that the returned map is a {@link CompactLinkedMap}.</li>
 *   <li>Behaviour when a {@code null} source is supplied (expected {@link NullPointerException}).</li>
 * </ul>
 *
 * The {@code Converter} argument is not used by {@code YearMonthConversions.toMap},
 * therefore {@code null} is passed safely.
 */
public class YearMonthConversionsTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private Converter dummyConverter;

    @Before
    public void setUp() {
        // The converter is not used by the method under test, so we can safely pass null.
        dummyConverter = null;
    }

    @Test
    public void testToMapTypicalYearMonth() {
        YearMonth ym = YearMonth.of(2023, 5); // May 2023
        Map<String, Object> map = YearMonthConversions.toMap(ym, dummyConverter);

        assertNotNull("Returned map should not be null", map);
        assertTrue("Returned map should be an instance of CompactLinkedMap",
                map instanceof CompactLinkedMap);

        assertEquals("Map should contain exactly two entries", 2, map.size());

        assertTrue("Map should contain key 'year'", map.containsKey("year"));
        assertTrue("Map should contain key 'month'", map.containsKey("month"));

        assertEquals("Year value should match", 2023, map.get("year"));
        assertEquals("Month value should match", 5, map.get("month"));
    }

    @Test
    public void testToMapLeapYearFebruary() {
        YearMonth ym = YearMonth.of(2020, 2); // February of a leap year
        Map<String, Object> map = YearMonthConversions.toMap(ym, dummyConverter);

        assertEquals("Year should be 2020", 2020, map.get("year"));
        assertEquals("Month should be 2 (February)", 2, map.get("month"));
    }

    @Test
    public void testToMapMinimumSupportedYear() {
        // Java's YearMonth supports years down to -999_999_999
        YearMonth ym = YearMonth.of(-999_999_999, 1);
        Map<String, Object> map = YearMonthConversions.toMap(ym, dummyConverter);

        assertEquals("Year should be the minimum supported", -999_999_999, map.get("year"));
        assertEquals("Month should be January (1)", 1, map.get("month"));
    }

    @Test
    public void testToMapMaximumSupportedYear() {
        // Java's YearMonth supports years up to 999_999_999
        YearMonth ym = YearMonth.of(999_999_999, 12);
        Map<String, Object> map = YearMonthConversions.toMap(ym, dummyConverter);

        assertEquals("Year should be the maximum supported", 999_999_999, map.get("year"));
        assertEquals("Month should be December (12)", 12, map.get("month"));
    }

    @Test
    public void testToMapNullSourceThrowsNPE() {
        thrown.expect(NullPointerException.class);
        YearMonthConversions.toMap(null, dummyConverter);
    }

    @Test
    public void testReturnedMapIsMutable() {
        YearMonth ym = YearMonth.of(2021, 7);
        Map<String, Object> map = YearMonthConversions.toMap(ym, dummyConverter);

        // Modify the map and verify changes are reflected
        map.put("year", 1999);
        map.put("month", 12);
        map.put("extra", "value");

        assertEquals("Modified year should be 1999", 1999, map.get("year"));
        assertEquals("Modified month should be 12", 12, map.get("month"));
        assertEquals("Extra entry should be present", "value", map.get("extra"));
        assertEquals("Map size should now be 3", 3, map.size());
    }
}
