package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactLinkedMap;
import org.junit.Test;
import org.junit.Before;
import java.time.MonthDay;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link MonthDayConversions}.
 *
 * The class under test is package‑private with a package‑private static method,
 * therefore the test class resides in the same package so it can access the method.
 *
 * Test strategy:
 * 1. Verify normal conversion of a valid {@link MonthDay} instance.
 * 2. Verify that the returned map is a {@link CompactLinkedMap} and contains exactly
 *    the expected keys/values.
 * 3. Edge‑case / error handling:
 *    • {@code from} is {@code null}  → {@link NullPointerException}
 *    • {@code from} is not a {@link MonthDay} → {@link ClassCastException}
 *    • {@code converter} is {@code null} – the method does not use it, so it should
 *      still succeed.
 *
 * No mocking is required – the only dependency is {@link Converter}, which is not
 * used by the method; a simple instance (or {@code null}) suffices.
 */
public class MonthDayConversionsTest {

    private Converter dummyConverter;

    @Before
    public void setUp() {
        // Converter is not used inside toMap, but we create a minimal instance
        // to satisfy the method signature.
        dummyConverter = new Converter(new ConverterOptions());
    }

    /**
     * Basic happy‑path test – a regular MonthDay is converted to a map
     * containing the correct "day" and "month" entries.
     */
    @Test
    public void testToMapNormalCase() {
        MonthDay md = MonthDay.of(12, 25); // December 25
        Map<String, Object> result = MonthDayConversions.toMap(md, dummyConverter);

        // Verify the concrete map implementation
        assertTrue("Result should be a CompactLinkedMap",
                result instanceof CompactLinkedMap);

        // Verify size and content
        assertEquals("Map should contain exactly two entries", 2, result.size());
        assertTrue("Map should contain key 'day'", result.containsKey("day"));
        assertTrue("Map should contain key 'month'", result.containsKey("month"));
        assertEquals("Day value should match", 25, result.get("day"));
        assertEquals("Month value should match", 12, result.get("month"));
    }

    /**
     * The method does not depend on the Converter argument; passing {@code null}
     * must not affect the outcome.
     */
    @Test
    public void testToMapWithNullConverter() {
        MonthDay md = MonthDay.of(2, 29); // February 29 (leap year)
        Map<String, Object> result = MonthDayConversions.toMap(md, null);

        assertEquals(29, result.get("day"));
        assertEquals(2, result.get("month"));
    }

    /**
     * Passing {@code null} as the source object should result in a
     * {@link NullPointerException} because the method casts the argument to
     * {@link MonthDay} without a null‑check.
     */
    @Test(expected = NullPointerException.class)
    public void testToMapNullSource() {
        MonthDayConversions.toMap(null, dummyConverter);
    }

    /**
     * Passing an object that is not a {@link MonthDay} must trigger a
     * {@link ClassCastException} due to the explicit cast inside the method.
     */
    @Test(expected = ClassCastException.class)
    public void testToMapInvalidSourceType() {
        String notMonthDay = "2023-01-01";
        MonthDayConversions.toMap(notMonthDay, dummyConverter);
    }

    /**
     * Verify that the returned map is mutable – we can add a new entry without
     * throwing an exception.
     */
    @Test
    public void testResultMapIsMutable() {
        MonthDay md = MonthDay.of(7, 4); // July 4
        Map<String, Object> result = MonthDayConversions.toMap(md, dummyConverter);

        result.put("extra", "value");
        assertEquals("value", result.get("extra"));
        assertEquals(3, result.size()); // day, month, extra
    }
}
