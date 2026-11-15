package com.cedarsoftware.util.convert;
import com.cedarsoftware.util.CompactLinkedMap;


import static org.junit.Assert.*;

import java.time.MonthDay;
import java.util.Map;

import org.junit.Test;

/**
 * Unit tests for {@link MonthDayConversions}.
 * <p>
 * The tests cover:
 * <ul>
 *   <li>Successful conversion of a {@link MonthDay} to a {@link Map} with correct keys and values.</li>
 *   <li>Verification that the returned map is an instance of {@link CompactLinkedMap} and retains insertion order.</li>
 *   <li>Behaviour when {@code null} is supplied as the source object (expected {@link NullPointerException}).</li>
 *   <li>Behaviour when an object of an incompatible type is supplied (expected {@link ClassCastException}).</li>
 * </ul>
 */
public class MonthDayConversionsTest {

    /** Helper method to create a dummy {@link Converter} instance. */
    private static Converter dummyConverter() {
        // The Converter constructor accepts a ConverterOptions argument; passing null is acceptable for the tests
        // because the MonthDayConversions.toMap method never uses the Converter instance.
        return new Converter(null);
    }

    @Test
    public void testToMapWithValidMonthDay() {
        // Arrange: a known MonthDay value (February 29, a leap‑day)
        MonthDay monthDay = MonthDay.of(2, 29);
        Converter converter = dummyConverter();

        // Act
        Map<String, Object> result = MonthDayConversions.toMap(monthDay, converter);

        // Assert: map type
        assertTrue("Result should be an instance of CompactLinkedMap", result instanceof CompactLinkedMap);

        // Assert: size and content
        assertEquals("Map should contain exactly two entries", 2, result.size());
        assertTrue("Map should contain key 'day'", result.containsKey("day"));
        assertTrue("Map should contain key 'month'", result.containsKey("month"));
        assertEquals("Day value should match MonthDay", Integer.valueOf(29), result.get("day"));
        assertEquals("Month value should match MonthDay", Integer.valueOf(2), result.get("month"));

        // Assert: insertion order – CompactLinkedMap preserves the order of insertion.
        // The iterator should return "day" first, then "month".
        String[] expectedOrder = {"day", "month"};
        int index = 0;
        for (String key : result.keySet()) {
            assertEquals("Key order should be preserved", expectedOrder[index], key);
            index++;
        }
    }

    @Test
    public void testToMapResultIsMutable() {
        MonthDay monthDay = MonthDay.of(12, 31);
        Converter converter = dummyConverter();

        Map<String, Object> map = MonthDayConversions.toMap(monthDay, converter);

        // The map should be mutable – we can add a new entry without an exception.
        map.put("extra", "value");
        assertEquals("New entry should be present", "value", map.get("extra"));
        assertEquals("Map size should now be 3", 3, map.size());
    }

    @Test(expected = NullPointerException.class)
    public void testToMapWithNullSourceThrowsNPE() {
        Converter converter = dummyConverter();
        // Passing null should cause a NullPointerException when the method tries to invoke
        // monthDay.getDayOfMonth().
        MonthDayConversions.toMap(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToMapWithInvalidSourceTypeThrowsClassCastException() {
        Converter converter = dummyConverter();
        // Pass an object that is not a MonthDay – the cast inside the method will fail.
        MonthDayConversions.toMap("not a MonthDay", converter);
    }
}
