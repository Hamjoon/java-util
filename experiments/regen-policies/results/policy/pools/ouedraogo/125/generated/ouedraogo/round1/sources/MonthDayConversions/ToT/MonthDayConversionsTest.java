package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

import java.time.MonthDay;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Comprehensive JUnit‑4 test suite for {@link MonthDayConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical conversion of a {@link MonthDay} to a map.</li>
 *   <li>Edge case with a leap‑day (February 29).</li>
 *   <li>Verification that the returned {@link CompactMap} preserves insertion order.</li>
 *   <li>Behaviour when {@code null} is supplied as the source object.</li>
 *   <li>Behaviour when an object of the wrong type is supplied.</li>
 *   <li>Behaviour when {@code null} is supplied as the {@link Converter} (the method does not use it).</li>
 * </ul>
 *
 * Only APIs that appear in the supplied source code or in the listed dependency signatures
 * are used; no mocking frameworks or external libraries are required.
 */
public class MonthDayConversionsTest {

    /**
     * Helper that creates a {@link Converter} instance.
     * The {@code Converter} constructor requires a {@code ConverterOptions} argument,
     * but the {@code toMap} implementation does not use the converter at all.
     * To keep the test self‑contained we pass {@code null} – the method must tolerate it.
     */
    private static Converter dummyConverter() {
        return null; // acceptable because the method never dereferences the argument
    }

    @Test
    public void testToMapTypicalDate() {
        MonthDay md = MonthDay.of(5, 15); // May 15
        Map<String, Object> map = MonthDayConversions.toMap(md, dummyConverter());

        // Basic map contract checks
        Assert.assertEquals("Map should contain exactly two entries", 2, map.size());
        Assert.assertTrue("Map must contain key \"day\"", map.containsKey("day"));
        Assert.assertTrue("Map must contain key \"month\"", map.containsKey("month"));

        // Value correctness
        Assert.assertEquals("Day value must match", 15, map.get("day"));
        Assert.assertEquals("Month value must match", 5, map.get("month"));
    }

    @Test
    public void testToMapLeapDay() {
        MonthDay md = MonthDay.of(2, 29); // February 29 (leap year day)
        Map<String, Object> map = MonthDayConversions.toMap(md, dummyConverter());

        Assert.assertEquals("Day should be 29 for leap day", 29, map.get("day"));
        Assert.assertEquals("Month should be 2 for February", 2, map.get("month"));
    }

    @Test
    public void testInsertionOrderPreserved() {
        MonthDay md = MonthDay.of(12, 31);
        Map<String, Object> map = MonthDayConversions.toMap(md, dummyConverter());

        // The builder is configured with insertionOrder(), therefore iteration order must be:
        // first "day", then "month".
        Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator();

        Assert.assertTrue("Map must have at least one entry", it.hasNext());
        Map.Entry<String, Object> first = it.next();
        Assert.assertEquals("First entry key should be \"day\"", "day", first.getKey());

        Assert.assertTrue("Map must have a second entry", it.hasNext());
        Map.Entry<String, Object> second = it.next();
        Assert.assertEquals("Second entry key should be \"month\"", "month", second.getKey());

        Assert.assertFalse("Map should contain only two entries", it.hasNext());
    }

    @Test(expected = NullPointerException.class)
    public void testNullSourceThrowsNPE() {
        // Casting null to MonthDay yields null; the method then calls getDayOfMonth(),
        // which must throw NPE.
        MonthDayConversions.toMap(null, dummyConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testInvalidSourceTypeThrowsClassCastException() {
        // Pass an object that is not a MonthDay – the cast inside the method should fail.
        MonthDayConversions.toMap("not a MonthDay", dummyConverter());
    }

    @Test
    public void testNullConverterIsAccepted() {
        // The converter argument is not used; passing null must not cause any exception.
        MonthDay md = MonthDay.of(7, 4);
        Map<String, Object> map = MonthDayConversions.toMap(md, null);
        Assert.assertNotNull("Resulting map must not be null", map);
        Assert.assertEquals("Day must be 4", 4, map.get("day"));
        Assert.assertEquals("Month must be 7", 7, map.get("month"));
    }

    @Test
    public void testReturnedMapIsMutable() {
        MonthDay md = MonthDay.of(1, 1);
        Map<String, Object> map = MonthDayConversions.toMap(md, dummyConverter());

        // Verify we can add a new entry without throwing.
        map.put("extra", "value");
        Assert.assertEquals("Map should now contain three entries", 3, map.size());
        Assert.assertEquals("New entry must be stored correctly", "value", map.get("extra"));
    }

    @Test
    public void testKeySetAndValuesReflectContents() {
        MonthDay md = MonthDay.of(3, 10);
        Map<String, Object> map = MonthDayConversions.toMap(md, dummyConverter());

        Set<String> keys = map.keySet();
        Assert.assertTrue("Key set must contain \"day\"", keys.contains("day"));
        Assert.assertTrue("Key set must contain \"month\"", keys.contains("month"));
        Assert.assertEquals("Key set size must be 2", 2, keys.size());

        Assert.assertTrue("Values must contain day number", map.values().contains(10));
        Assert.assertTrue("Values must contain month number", map.values().contains(3));
    }
}
