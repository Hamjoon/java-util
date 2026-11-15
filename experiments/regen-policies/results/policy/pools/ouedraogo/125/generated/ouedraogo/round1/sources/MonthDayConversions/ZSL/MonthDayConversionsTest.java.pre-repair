package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.time.MonthDay;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.cedarsoftware.util.CompactMap;

/**
 * Unit tests for {@link MonthDayConversions}.
 *
 * These tests cover:
 * <ul>
 *   <li>Correct conversion of a {@link MonthDay} to a map with expected keys and values.</li>
 *   <li>Insertion order preservation (the builder is configured with {@code insertionOrder()}).</li>
 *   <li>Handling of edge‑case dates such as February 29 (leap year).</li>
 *   <li>Behaviour when {@code null} or an incompatible type is supplied.</li>
 *   <li>Mutability of the returned map (it should be a {@link CompactMap}).</li>
 * </ul>
 */
public class MonthDayConversionsTest {

    private Converter dummyConverter;

    @Before
    public void setUp() {
        // The toMap implementation does not use the Converter instance.
        // Passing {@code null} is acceptable, but we also create a dummy instance
        // in case future changes require a non‑null value.
        dummyConverter = null; // No need for a real Converter in current implementation.
    }

    @Test
    public void testToMapReturnsCorrectEntries() {
        MonthDay md = MonthDay.of(12, 25); // Christmas
        Map<String, Object> result = MonthDayConversions.toMap(md, dummyConverter);

        // Verify map type
        assertTrue("Result should be an instance of CompactMap", result instanceof CompactMap);

        // Verify size and content
        assertEquals("Map should contain exactly two entries", 2, result.size());
        assertTrue("Map should contain key 'day'", result.containsKey("day"));
        assertTrue("Map should contain key 'month'", result.containsKey("month"));
        assertEquals("Day value should match", 25, result.get("day"));
        assertEquals("Month value should match", 12, result.get("month"));
    }

    @Test
    public void testInsertionOrderIsPreserved() {
        MonthDay md = MonthDay.of(7, 4); // Independence Day
        Map<String, Object> result = MonthDayConversions.toMap(md, dummyConverter);

        // The builder is configured with insertionOrder(), so iteration should follow the insertion order:
        // first "day", then "month".
        Iterator<Entry<String, Object>> it = result.entrySet().iterator();

        assertTrue("Map should have at least one entry", it.hasNext());
        Entry<String, Object> first = it.next();
        assertEquals("First entry key should be 'day'", "day", first.getKey());

        assertTrue("Map should have a second entry", it.hasNext());
        Entry<String, Object> second = it.next();
        assertEquals("Second entry key should be 'month'", "month", second.getKey());

        assertFalse("Map should contain exactly two entries", it.hasNext());
    }

    @Test
    public void testLeapYearDayConversion() {
        // February 29 is valid for a leap year. MonthDay does not store year information,
        // but it validates the day/month combination.
        MonthDay md = MonthDay.of(2, 29);
        Map<String, Object> result = MonthDayConversions.toMap(md, dummyConverter);

        assertEquals("Day value for leap day should be 29", 29, result.get("day"));
        assertEquals("Month value for leap day should be 2", 2, result.get("month"));
    }

    @Test
    public void testReturnedMapIsMutable() {
        MonthDay md = MonthDay.of(1, 1);
        Map<String, Object> result = MonthDayConversions.toMap(md, dummyConverter);

        // Verify we can add a new entry without throwing.
        result.put("extra", "value");
        assertEquals("Map should contain the newly added entry", "value", result.get("extra"));
        assertEquals("Map size should now be three", 3, result.size());
    }

    @Test(expected = NullPointerException.class)
    public void testToMapWithNullThrowsNPE() {
        // The implementation casts the input to MonthDay, which will throw NPE when the argument is null.
        MonthDayConversions.toMap(null, dummyConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToMapWithInvalidTypeThrowsClassCastException() {
        // Supplying an object that is not a MonthDay should result in a ClassCastException.
        MonthDayConversions.toMap("not a MonthDay", dummyConverter);
    }
}
