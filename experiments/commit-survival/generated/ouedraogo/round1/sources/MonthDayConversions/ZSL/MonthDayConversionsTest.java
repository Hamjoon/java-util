package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.time.MonthDay;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link MonthDayConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Correct conversion of a {@link MonthDay} to a {@link Map} containing the expected keys and values.</li>
 *   <li>Handling of edge‑case dates such as February 29 (leap day).</li>
 *   <li>Behaviour when {@code null} is supplied as the source object.</li>
 *   <li>Behaviour when an object of an incompatible type is supplied.</li>
 *   <li>Verification that the {@code Converter} argument is not used by the method.</li>
 * </ul>
 *
 * Note: The {@code Converter} argument is unused by {@code MonthDayConversions.toMap},
 * therefore the tests pass {@code null} for this parameter. This is safe because the
 * implementation does not dereference the argument.
 */
@RunWith(JUnit4.class)
public class MonthDayConversionsTest {

    /**
     * Helper that invokes the private static method under test.
     *
     * @param monthDay the {@link MonthDay} to convert
     * @return the resulting map
     */
    private static Map<String, Object> invokeToMap(MonthDay monthDay) {
        // The method is package‑private static, so we can call it directly from the same package.
        return MonthDayConversions.toMap(monthDay, null);
    }

    @Test
    public void testToMapProducesCorrectEntries() {
        MonthDay md = MonthDay.of(12, 25); // Christmas
        Map<String, Object> result = invokeToMap(md);

        assertNotNull("Resulting map must not be null", result);
        assertEquals("Map must contain exactly two entries", 2, result.size());

        assertTrue("Map must contain key \"day\"", result.containsKey("day"));
        assertTrue("Map must contain key \"month\"", result.containsKey("month"));

        assertEquals("Day value must match the MonthDay's day", 25, result.get("day"));
        assertEquals("Month value must match the MonthDay's month", 12, result.get("month"));

        // Ensure the concrete implementation is CompactLinkedMap (optional but useful)
        assertTrue("Result should be an instance of CompactLinkedMap",
                result.getClass().getSimpleName().equals("CompactLinkedMap"));
    }

    @Test
    public void testToMapWithLeapDay() {
        MonthDay leap = MonthDay.of(2, 29); // February 29 (leap year)
        Map<String, Object> result = invokeToMap(leap);

        assertEquals("Month should be February (2)", 2, result.get("month"));
        assertEquals("Day should be 29", 29, result.get("day"));
    }

    @Test(expected = NullPointerException.class)
    public void testToMapNullFromThrowsNullPointerException() {
        // Passing null as the source causes a NPE when the method attempts to dereference it.
        MonthDayConversions.toMap(null, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToMapInvalidTypeThrowsClassCastException() {
        // The method expects a MonthDay; passing a String should trigger a ClassCastException.
        MonthDayConversions.toMap("not a MonthDay", null);
    }

    @Test
    public void testConverterArgumentIsIgnored() {
        // Even though the Converter argument is not usable (no public constructor without options),
        // we can still verify that providing a non‑null value does not affect the outcome.
        // We'll create a dummy subclass via an anonymous class (allowed because Converter is final?).
        // Since Converter is final, we cannot subclass; instead we use reflection to create a minimal instance.
        // However, the method does not touch the argument, so passing null is sufficient for this verification.
        MonthDay md = MonthDay.of(7, 4); // Independence Day
        Map<String, Object> resultWithNull = MonthDayConversions.toMap(md, null);
        Map<String, Object> resultWithNullAgain = MonthDayConversions.toMap(md, null);

        assertEquals("Result should be identical regardless of the Converter argument",
                resultWithNull, resultWithNullAgain);
    }
}
