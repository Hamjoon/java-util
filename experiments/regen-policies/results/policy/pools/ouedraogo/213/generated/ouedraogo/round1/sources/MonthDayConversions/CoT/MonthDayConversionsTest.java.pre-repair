package com.cedarsoftware.util.convert;

import java.time.MonthDay;
import java.util.Map;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * JUnit 4 test suite for {@link MonthDayConversions}.
 *
 * The tests cover:
 *   1. Normal operation with a valid {@link MonthDay}.
 *   2. Handling of {@code null} arguments.
 *   3. Behaviour when the {@code from} argument is not a {@code MonthDay}.
 *   4. Edge‑case {@code MonthDay} values (e.g., February 29).
 *
 * The {@code Converter} argument is not used by {@code MonthDayConversions.toMap},
 * therefore {@code null} is passed in most tests to avoid dependence on the
 * {@code ConverterOptions} class, which is not part of the provided API.
 */
public class MonthDayConversionsTest {

    /** --------------------------------------------------------------------
     *  LIST OF PUBLIC (or package‑visible) METHODS IN MonthDayConversions
     *  --------------------------------------------------------------------
     *
     *  1. static Map toMap(Object from, Converter converter)
     *
     *  The method casts {@code from} to {@link MonthDay}, creates a
     *  {@link java.util.LinkedHashMap}, stores the string representation of the
     *  {@code MonthDay} under the key {@link MapConversions#MONTH_DAY},
     *  and returns the map.
     */
    
    // --------------------------------------------------------------------
    //  BASIC FUNCTIONALITY TESTS
    // --------------------------------------------------------------------
    
    @Test
    public void testToMapWithValidMonthDay() {
        MonthDay monthDay = MonthDay.of(12, 31);               // standard date
        Map<String, Object> result = MonthDayConversions.toMap(monthDay, null);
        
        assertNotNull("Result map must not be null", result);
        assertEquals("Map should contain exactly one entry", 1, result.size());
        assertTrue("Map must contain the MONTH_DAY key",
                result.containsKey(MapConversions.MONTH_DAY));
        assertEquals("Value must be the ISO‑8601 string of the MonthDay",
                monthDay.toString(), result.get(MapConversions.MONTH_DAY));
    }

    @Test
    public void testToMapWithLeapYearFebruary29() {
        MonthDay feb29 = MonthDay.of(2, 29);                  // valid for MonthDay (no year)
        Map<String, Object> result = MonthDayConversions.toMap(feb29, null);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(feb29.toString(), result.get(MapConversions.MONTH_DAY));
    }

    @Test
    public void testToMapWithNullConverterDoesNotAffectResult() {
        MonthDay monthDay = MonthDay.of(1, 1);
        Map<String, Object> result = MonthDayConversions.toMap(monthDay, null);
        
        assertNotNull(result);
        assertEquals(monthDay.toString(), result.get(MapConversions.MONTH_DAY));
    }

    // --------------------------------------------------------------------
    //  EDGE CASE & EXCEPTION TESTS
    // --------------------------------------------------------------------
    
    @Test(expected = NullPointerException.class)
    public void testToMapWithNullFromThrowsNPE() {
        // The method casts the null to MonthDay and then calls toString(),
        // which results in a NullPointerException.
        MonthDayConversions.toMap(null, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToMapWithInvalidFromTypeThrowsClassCastException() {
        // Passing an object that is not a MonthDay should cause a ClassCastException
        String notAMonthDay = "2023-01-01";
        MonthDayConversions.toMap(notAMonthDay, null);
    }

    @Test
    public void testToMapDoesNotUseConverterParameter() {
        // Even when a fully‑initialized Converter is supplied, the method should
        // ignore it and produce the same result.
        MonthDay monthDay = MonthDay.of(5, 15);
        Converter dummyConverter = null; // We cannot instantiate Converter without ConverterOptions
        Map<String, Object> result = MonthDayConversions.toMap(monthDay, dummyConverter);
        assertEquals(monthDay.toString(), result.get(MapConversions.MONTH_DAY));
    }
}
