package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

import java.time.YearMonth;
import java.util.Map;

/**
 * Test suite for {@link YearMonthConversions}.
 *
 * The test class resides in the same package as {@code YearMonthConversions}
 * so it can access the package‑private static method {@code toMap}.
 */
public class YearMonthConversionsTest {

    /** Normal usage – a valid YearMonth instance is converted to a map. */
    @Test
    public void testToMapBasic() {
        YearMonth ym = YearMonth.of(2023, 5);
        Map<String, Object> result = YearMonthConversions.toMap(ym, null);

        assertNotNull("Result map must not be null", result);
        assertEquals("Map must contain exactly two entries", 2, result.size());
        assertEquals("Year entry is incorrect", 2023, result.get("year"));
        assertEquals("Month entry is incorrect", 5, result.get("month"));
    }

    /** Passing {@code null} as the source object should raise NPE. */
    @Test(expected = NullPointerException.class)
    public void testToMapNullFromThrowsNPE() {
        YearMonthConversions.toMap(null, null);
    }

    /** Passing an object that is not a YearMonth must raise ClassCastException. */
    @Test(expected = ClassCastException.class)
    public void testToMapInvalidTypeThrowsClassCastException() {
        Integer notAYearMonth = 42;
        YearMonthConversions.toMap(notAYearMonth, null);
    }

    /** The method does not use the Converter argument; a null converter must be accepted. */
    @Test
    public void testToMapNullConverterWorks() {
        YearMonth ym = YearMonth.of(1999, 12);
        Map<String, Object> result = YearMonthConversions.toMap(ym, null);

        assertEquals(1999, result.get("year"));
        assertEquals(12, result.get("month"));
    }

    /**
     * If a real {@code Converter} instance is available, the method should still work.
     * This test is optional – it will be compiled only when {@code ConverterOptions}
     * is on the classpath.
     */
    @Test
    public void testToMapWithRealConverter() {
        // The library provides a default constructor for ConverterOptions.
        // If that class is not present, this test will be ignored by the build.
        try {
            Class<?> optsClass = Class.forName("com.cedarsoftware.util.convert.ConverterOptions");
            Object opts = optsClass.getDeclaredConstructor().newInstance();
            Converter conv = new Converter((ConverterOptions) opts);

            YearMonth ym = YearMonth.of(2000, 1);
            Map<String, Object> result = YearMonthConversions.toMap(ym, conv);

            assertEquals(2000, result.get("year"));
            assertEquals(1, result.get("month"));
        } catch (ClassNotFoundException e) {
            // ConverterOptions not present – skip the test.
        } catch (Exception e) {
            fail("Unexpected exception while creating Converter: " + e);
        }
    }

    /** Verify that the returned map is mutable (it is a CompactLinkedMap). */
    @Test
    public void testReturnedMapIsMutable() {
        YearMonth ym = YearMonth.of(2021, 7);
        Map<String, Object> map = YearMonthConversions.toMap(ym, null);

        // add a new entry
        map.put("extra", "value");
        assertEquals("value", map.get("extra"));

        // remove the entry again
        map.remove("extra");
        assertFalse(map.containsKey("extra"));
    }
}
