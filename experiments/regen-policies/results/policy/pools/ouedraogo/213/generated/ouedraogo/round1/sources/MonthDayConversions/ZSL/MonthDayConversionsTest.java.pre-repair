package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.time.MonthDay;
import java.util.Map;
import java.util.LinkedHashMap;

import static com.cedarsoftware.util.convert.MapConversions.MONTH_DAY;
import static org.junit.Assert.*;

public class MonthDayConversionsTest {

    // A simple Converter instance – the implementation is not used by MonthDayConversions,
    // but the method signature requires it.
    private final Converter converter = new Converter(new ConverterOptions());

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Verify that a valid MonthDay is converted to a map containing a single entry.
     * The key must be {@link MapConversions#MONTH_DAY} and the value must be the
     * ISO‑8601 string produced by {@link MonthDay#toString()}.
     */
    @Test
    public void testToMapWithValidMonthDay() {
        MonthDay monthDay = MonthDay.of(12, 25); // Christmas
        Map<String, Object> result = MonthDayConversions.toMap(monthDay, converter);

        // The returned map must be a LinkedHashMap (preserves insertion order)
        assertTrue("Result should be a LinkedHashMap", result instanceof LinkedHashMap);

        // Exactly one entry should exist
        assertEquals("Map should contain exactly one entry", 1, result.size());

        // Verify key and value
        assertTrue("Map must contain the MONTH_DAY key", result.containsKey(MONTH_DAY));
        assertEquals("Value must match MonthDay.toString()", monthDay.toString(), result.get(MONTH_DAY));
    }

    /**
     * Passing {@code null} as the source object should result in a NullPointerException,
     * because the implementation casts the argument directly to {@link MonthDay}
     * without a null‑check.
     */
    @Test
    public void testToMapWithNullSourceThrowsNPE() {
        thrown.expect(NullPointerException.class);
        MonthDayConversions.toMap(null, converter);
    }

    /**
     * Passing an object that is not a {@link MonthDay} must cause a ClassCastException,
     * as the method performs an unchecked cast.
     */
    @Test
    public void testToMapWithInvalidTypeThrowsClassCastException() {
        thrown.expect(ClassCastException.class);
        MonthDayConversions.toMap("not-a-monthday", converter);
    }

    /**
     * Ensure that the method does not modify the supplied {@link Converter} instance.
     * This is a regression guard – the current implementation does not use the converter,
     * but future changes might inadvertently interact with it.
     */
    @Test
    public void testConverterIsUntouched() {
        // Capture a simple state from the converter (its options object) before the call.
        ConverterOptions beforeOptions = converter.getOptions();

        MonthDay monthDay = MonthDay.of(1, 1);
        MonthDayConversions.toMap(monthDay, converter);

        // After the call, the options reference should be the same instance,
        // indicating no replacement or mutation.
        assertSame("Converter options should remain unchanged", beforeOptions, converter.getOptions());
    }
}
