package com.cedarsoftware.util.convert;
import com.cedarsoftware.util.CompactLinkedMap;


import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;

/**
 * Unit tests for {@link EnumConversions}.
 * These tests cover normal operation, handling of {@code null},
 * and misuse (passing a non‑enum object). They also verify that the
 * optional {@code Converter} argument is truly ignored.
 */
public class EnumConversionsTest {

    /** Simple enum used for the tests. */
    private enum SampleEnum {
        ONE,
        TWO,
        THREE
    }

    /**
     * Helper that creates a {@link Converter} instance.
     * The {@code Converter} is not used by {@code EnumConversions.toMap},
     * but providing a real instance guarantees the method works with a
     * non‑null argument.
     */
    private static Converter createConverter() {
        // ConverterOptions is part of the public API; using the default constructor.
        // The actual options are irrelevant for these tests.
        return new Converter(new ConverterOptions());
    }

    @Test
    public void testToMapReturnsCorrectEntry() {
        Converter converter = createConverter();
        Map<String, Object> result = EnumConversions.toMap(SampleEnum.TWO, converter);

        // The result must be a map containing exactly one entry: name -> "TWO"
        assertNotNull("Resulting map should not be null", result);
        assertEquals("Map should contain exactly one entry", 1, result.size());
        assertTrue("Map should contain key 'name'", result.containsKey("name"));
        assertEquals("Enum name should be stored", "TWO", result.get("name"));

        // Verify that the concrete type is CompactLinkedMap (the implementation used)
        assertTrue("Result should be an instance of CompactLinkedMap",
                result instanceof CompactLinkedMap);
    }

    @Test
    public void testToMapIgnoresConverterArgument() {
        // Pass a null converter – the method does not touch it, so it must still work.
        Map<String, Object> result = EnumConversions.toMap(SampleEnum.ONE, null);

        assertNotNull("Resulting map should not be null when converter is null", result);
        assertEquals("Enum name should be stored correctly", "ONE", result.get("name"));
    }

    @Test
    public void testToMapWithNullEnumInstance() {
        Converter converter = createConverter();
        try {
            EnumConversions.toMap(null, converter);
            fail("Expected NullPointerException when 'from' argument is null");
        } catch (NullPointerException expected) {
            // Expected exception – test passes.
        }
    }

    @Test
    public void testToMapWithNonEnumObject() {
        Converter converter = createConverter();
        String notAnEnum = "I am not an enum";

        try {
            EnumConversions.toMap(notAnEnum, converter);
            fail("Expected ClassCastException when 'from' is not an Enum");
        } catch (ClassCastException expected) {
            // Expected exception – test passes.
        }
    }

    @Test
    public void testReturnedMapIsMutable() {
        Converter converter = createConverter();
        Map<String, Object> map = EnumConversions.toMap(SampleEnum.THREE, converter);

        // The map should be mutable because CompactLinkedMap extends Map.
        map.put("extra", 123);
        assertEquals("Map should contain the extra entry", Integer.valueOf(123), map.get("extra"));
        assertEquals("Map size should now be 2", 2, map.size());
    }
}
