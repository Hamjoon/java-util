package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactMap;
import org.junit.Test;
import org.junit.Assert;

import java.time.MonthDay;
import java.util.Iterator;
import java.util.Map;

/**
 * Comprehensive JUnit‑4 test suite for {@code MonthDayConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Normal conversion of a {@link MonthDay} to a {@link Map}</li>
 *   <li>Insertion‑order guarantee of the produced {@code CompactMap}</li>
 *   <li>Behaviour when {@code null} is supplied for the {@code Converter} argument</li>
 *   <li>Null source object – expecting {@link NullPointerException}</li>
 *   <li>Incorrect source type – expecting {@link ClassCastException}</li>
 *   <li>Edge case values such as February 29 (leap day)</li>
 * </ul>
 *
 * The class under test has a private constructor and a single static method,
 * therefore no instance is created.
 */
public class MonthDayConversionsTest {

    /**
     * Helper that invokes the package‑private static method via reflection.
     * This avoids placing the test class in the same package as the class
     * under test (which would give direct access) and demonstrates that the
     * method works with normal visibility rules.
     */
    private static Map<String, Object> invokeToMap(Object from, Converter converter) throws Exception {
        java.lang.reflect.Method m = MonthDayConversions.class.getDeclaredMethod("toMap", Object.class, Converter.class);
        m.setAccessible(true);
        //noinspection unchecked
        return (Map<String, Object>) m.invoke(null, from, converter);
    }

    @Test
    public void testToMapNormalConversion() throws Exception {
        MonthDay md = MonthDay.of(12, 5); // month = 12, day = 5
        Map<String, Object> map = invokeToMap(md, null);

        Assert.assertEquals("Map should contain exactly 2 entries", 2, map.size());
        Assert.assertTrue("Map should contain key \"day\"", map.containsKey("day"));
        Assert.assertTrue("Map should contain key \"month\"", map.containsKey("month"));
        Assert.assertEquals("Day value should be 5", 5, map.get("day"));
        Assert.assertEquals("Month value should be 12", 12, map.get("month"));
    }

    @Test
    public void testToMapInsertionOrder() throws Exception {
        MonthDay md = MonthDay.of(3, 15);
        Map<String, Object> map = invokeToMap(md, null);

        // CompactMap.builder().insertionOrder() guarantees iteration order matches insertion order.
        Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator();

        Assert.assertTrue("Map should have first entry", it.hasNext());
        Map.Entry<String, Object> first = it.next();
        Assert.assertEquals("First key should be \"day\"", "day", first.getKey());
        Assert.assertEquals("First value should be 15", 15, first.getValue());

        Assert.assertTrue("Map should have second entry", it.hasNext());
        Map.Entry<String, Object> second = it.next();
        Assert.assertEquals("Second key should be \"month\"", "month", second.getKey());
        Assert.assertEquals("Second value should be 3", 3, second.getValue());

        Assert.assertFalse("Map should contain exactly two entries", it.hasNext());
    }

    @Test
    public void testToMapWithNullConverter() throws Exception {
        MonthDay md = MonthDay.of(1, 1);
        // Passing null for the Converter argument – the method does not use it.
        Map<String, Object> map = invokeToMap(md, null);

        Assert.assertEquals(1, map.get("day"));
        Assert.assertEquals(1, map.get("month"));
    }

    @Test(expected = NullPointerException.class)
    public void testToMapNullFromThrowsNPE() throws Exception {
        // The static method casts the argument to MonthDay and then dereferences it,
        // so a null source must raise NullPointerException.
        invokeToMap(null, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToMapInvalidFromThrowsClassCastException() throws Exception {
        // Supplying an object that is not a MonthDay should cause a ClassCastException.
        invokeToMap("not a MonthDay", null);
    }

    @Test
    public void testToMapLeapDay() throws Exception {
        // February 29 is a valid MonthDay (leap year concept is irrelevant for MonthDay)
        MonthDay md = MonthDay.of(2, 29);
        Map<String, Object> map = invokeToMap(md, null);

        Assert.assertEquals("Leap day should be stored correctly", 29, map.get("day"));
        Assert.assertEquals("Leap month should be stored correctly", 2, map.get("month"));
    }

    @Test
    public void testMapIsCompactMapImplementation() throws Exception {
        MonthDay md = MonthDay.of(7, 4);
        Map<String, Object> map = invokeToMap(md, null);
        // The concrete type should be CompactMap (or a subclass thereof)
        Assert.assertTrue("Returned map should be an instance of CompactMap",
                map instanceof CompactMap);
    }
}
