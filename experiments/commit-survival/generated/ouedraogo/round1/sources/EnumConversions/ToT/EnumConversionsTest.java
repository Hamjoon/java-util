package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactLinkedMap;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * JUnit 4 test suite for {@link EnumConversions}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Typical conversion of an {@code Enum} to a {@code Map}</li>
 *   <li>Behaviour when {@code null} is supplied as the source object</li>
 *   <li>Behaviour when a non‑enum object is supplied</li>
 *   <li>Behaviour when a {@code null} {@link Converter} is supplied</li>
 *   <li>Mutability of the returned map and independence of successive calls</li>
 * </ul>
 *
 * <p>Only the public API listed in the prompt is used; no mocking frameworks are employed.
 */
public class EnumConversionsTest {

    /** Simple enum used for the tests. */
    private enum SampleEnum { ONE, TWO }

    /**
     * Helper to create a {@link Converter} instance without needing a real
     * {@code ConverterOptions} object (the {@code toMap} method does not use it).
     */
    private static Converter createConverter() {
        // The constructor accepts a ConverterOptions; passing {@code null} is legal for this test.
        return new Converter(null);
    }

    /** Typical usage – an enum is converted to a map containing its name. */
    @Test
    public void testToMapTypical() {
        SampleEnum source = SampleEnum.ONE;
        Converter converter = createConverter();

        Map<String, Object> result = EnumConversions.toMap(source, converter);

        // Verify the concrete implementation is CompactLinkedMap
        assertTrue("Result should be an instance of CompactLinkedMap",
                result instanceof CompactLinkedMap);

        // Verify map content
        assertEquals("Map should contain exactly one entry", 1, result.size());
        assertTrue("Map should contain the key 'name'", result.containsKey("name"));
        assertEquals("The value for 'name' should be the enum constant name",
                "ONE", result.get("name"));
    }

    /** The returned map should be mutable – callers can add additional entries. */
    @Test
    public void testReturnedMapIsMutable() {
        SampleEnum source = SampleEnum.TWO;
        Map<String, Object> map = EnumConversions.toMap(source, createConverter());

        // Add a new entry
        map.put("ordinal", source.ordinal());

        assertEquals("Map should now contain two entries", 2, map.size());
        assertEquals("Ordinal entry should be correct", Integer.valueOf(1), map.get("ordinal"));
    }

    /** Two consecutive calls must return distinct map instances (no shared state). */
    @Test
    public void testDistinctMapsOnEachCall() {
        SampleEnum first = SampleEnum.ONE;
        SampleEnum second = SampleEnum.TWO;

        Map<String, Object> map1 = EnumConversions.toMap(first, createConverter());
        Map<String, Object> map2 = EnumConversions.toMap(second, createConverter());

        assertNotSame("Each conversion should produce a new map instance", map1, map2);
        assertEquals("First map should hold 'ONE'", "ONE", map1.get("name"));
        assertEquals("Second map should hold 'TWO'", "TWO", map2.get("name"));
    }

    /** Passing {@code null} as the source enum should result in a NullPointerException
     *  when the method attempts to call {@code name()} on the null reference. */
    @Test(expected = NullPointerException.class)
    public void testToMapNullSource() {
        EnumConversions.toMap(null, createConverter());
    }

    /** Passing a non‑enum object should cause a ClassCastException because of the cast
     *  inside {@code toMap}. */
    @Test(expected = ClassCastException.class)
    public void testToMapNonEnumSource() {
        String notAnEnum = "I am not an enum";
        EnumConversions.toMap(notAnEnum, createConverter());
    }

    /** The {@code Converter} argument is unused; supplying {@code null} must not affect
     *  the conversion outcome. */
    @Test
    public void testToMapNullConverter() {
        SampleEnum source = SampleEnum.ONE;
        Map<String, Object> result = EnumConversions.toMap(source, null);

        assertEquals("Map should still contain the correct enum name",
                "ONE", result.get("name"));
        assertEquals("Map size should be exactly one", 1, result.size());
    }
}
