package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link YearMonthConversions}.
 *
 * These tests cover:
 * <ul>
 *   <li>Normal conversion of a {@link YearMonth} to a {@link Map}.</li>
 *   <li>Verification that the returned map is a {@link LinkedHashMap} and contains the expected key/value.</li>
 *   <li>Ensuring a fresh map instance is created on each invocation.</li>
 *   <li>Behaviour when {@code null} is supplied as the source object.</li>
 *   <li>Behaviour when an object of an incompatible type is supplied.</li>
 * </ul>
 *
 * The {@code Converter} argument is not used by the {@code toMap} method, but a real instance is supplied
 * to keep the contract intact.
 */
public class YearMonthConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // The ConverterOptions class is part of the library; using its default constructor.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @Test
    public void testToMapReturnsCorrectLinkedHashMap() {
        YearMonth ym = YearMonth.of(2023, 12);
        Map<String, Object> result = YearMonthConversions.toMap(ym, converter);

        // Verify the concrete type
        assertTrue("Result should be a LinkedHashMap", result instanceof LinkedHashMap);

        // Verify map contents
        assertEquals("Map should contain exactly one entry", 1, result.size());
        assertTrue("Map should contain the YEAR_MONTH key", result.containsKey(MapConversions.YEAR_MONTH));
        assertEquals("Value should be the YearMonth string representation",
                ym.toString(), result.get(MapConversions.YEAR_MONTH));
    }

    @Test
    public void testToMapCreatesNewInstanceEachCall() {
        YearMonth ym = YearMonth.of(1999, 1);
        Map<String, Object> first = YearMonthConversions.toMap(ym, converter);
        Map<String, Object> second = YearMonthConversions.toMap(ym, converter);

        assertNotSame("Each call should return a new map instance", first, second);
        // Both maps should still hold the correct data
        assertEquals(first, second);
    }

    @Test(expected = NullPointerException.class)
    public void testToMapWithNullSourceThrowsNullPointerException() {
        // The cast succeeds (null), but calling toString() on the null reference triggers NPE.
        YearMonthConversions.toMap(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToMapWithIncompatibleSourceThrowsClassCastException() {
        // Passing a String instead of a YearMonth should cause a ClassCastException on the cast.
        YearMonthConversions.toMap("2023-01", converter);
    }
}
