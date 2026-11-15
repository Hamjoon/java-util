package com.cedarsoftware.util.convert;
import com.cedarsoftware.util.convert.Converter.ConversionPair;


import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;

/**
 * Unit tests for {@link DefaultConverterOptions}.
 *
 * The class under test only exposes two public methods:
 * <ul>
 *   <li>{@code <T> T getCustomOption(String name)}</li>
 *   <li>{@code Map<ConversionPair, Convert<?>> getConverterOverrides()}</li>
 * </ul>
 *
 * Because there is no public API to add custom options we use reflection to
 * manipulate the internal {@code customOptions} map.  The {@code getConverterOverrides}
 * method returns the live map instance, so we can test its mutability directly.
 */
public class DefaultConverterOptionsTest {

    /**
     * Verify that a newly created {@code DefaultConverterOptions} returns {@code null}
     * for any unknown custom option name, including {@code null} and empty strings.
     */
    @Test
    public void testGetCustomOption_NotPresent() {
        DefaultConverterOptions opts = new DefaultConverterOptions();

        assertNull("Missing option should return null", opts.getCustomOption("nonexistent"));
        assertNull("Null name should return null", opts.getCustomOption(null));
        assertNull("Empty string name should return null", opts.getCustomOption(""));
    }

    /**
     * Verify that a custom option added via reflection can be retrieved with the
     * correct generic type.  Also verify that requesting the same option with an
     * incompatible type results in a {@link ClassCastException}.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testGetCustomOption_WithValueAndTypeSafety() throws Exception {
        DefaultConverterOptions opts = new DefaultConverterOptions();

        // Access the private 'customOptions' field
        Field customOptionsField = DefaultConverterOptions.class.getDeclaredField("customOptions");
        customOptionsField.setAccessible(true);
        Map<String, Object> customMap = (Map<String, Object>) customOptionsField.get(opts);

        // Populate the map with different value types
        customMap.put("stringKey", "stringValue");
        customMap.put("intKey", Integer.valueOf(42));

        // Correct generic retrieval
        String stringResult = opts.getCustomOption("stringKey");
        assertEquals("stringValue", stringResult);

        Integer intResult = opts.getCustomOption("intKey");
        assertEquals(Integer.valueOf(42), intResult);

        // Incorrect generic retrieval should throw ClassCastException
        try {
            // Attempt to retrieve a String as an Integer
            opts.getCustomOption("stringKey");
            // The line above compiles because of type erasure; we force a cast
            Integer bogus = (Integer) opts.getCustomOption("stringKey");
            fail("Expected ClassCastException when casting String to Integer");
        } catch (ClassCastException expected) {
            // Expected path
        }

        // Verify that a missing key still returns null, regardless of generic type
        Double missing = opts.getCustomOption("missingKey");
        assertNull(missing);
    }

    /**
     * Verify that {@code getConverterOverrides()} returns the same live map
     * instance each time and that the map can be mutated (add/remove entries).
     */
    @Test
    public void testGetConverterOverrides_MutableMap() {
        DefaultConverterOptions opts = new DefaultConverterOptions();

        // Initial map should be empty
        Map<ConversionPair, Convert<?>> firstCall = opts.getConverterOverrides();
        assertNotNull("Converter overrides map must not be null", firstCall);
        assertTrue("Initial overrides map should be empty", firstCall.isEmpty());

        // The same instance should be returned on subsequent calls
        Map<ConversionPair, Convert<?>> secondCall = opts.getConverterOverrides();
        assertSame("Subsequent calls must return the same map instance", firstCall, secondCall);

        // Prepare a ConversionPair and a simple Convert implementation
        ConversionPair pair = new Converter.ConversionPair(String.class, Integer.class);
        Convert<Integer> stringToInteger = new Convert<Integer>() {
            @Override
            public Integer convert(Object from, Converter converter) {
                return Integer.valueOf(from.toString());
            }
        };

        // Add an entry
        firstCall.put(pair, stringToInteger);
        assertEquals("Map should contain one entry after addition", 1, firstCall.size());
        assertTrue("Map must contain the added key", firstCall.containsKey(pair));
        assertSame("Stored Convert instance must be the same", stringToInteger, firstCall.get(pair));

        // Remove the entry and verify emptiness
        firstCall.remove(pair);
        assertTrue("Map should be empty after removal", firstCall.isEmpty());
    }

    /**
     * Verify that the map returned by {@code getConverterOverrides()} can hold
     * multiple entries with different source/target class combinations.
     */
    @Test
    public void testGetConverterOverrides_MultipleEntries() {
        DefaultConverterOptions opts = new DefaultConverterOptions();
        Map<ConversionPair, Convert<?>> overrides = opts.getConverterOverrides();

        // Pair 1: String -> Integer
        ConversionPair pair1 = new Converter.ConversionPair(String.class, Integer.class);
        Convert<Integer> conv1 = (from, converter) -> Integer.valueOf((String) from);
        overrides.put(pair1, conv1);

        // Pair 2: Integer -> String
        ConversionPair pair2 = new Converter.ConversionPair(Integer.class, String.class);
        Convert<String> conv2 = (from, converter) -> String.valueOf(from);
        overrides.put(pair2, conv2);

        // Pair 3: Double -> String
        ConversionPair pair3 = new Converter.ConversionPair(Double.class, String.class);
        Convert<String> conv3 = (from, converter) -> String.format("%.2f", (Double) from);
        overrides.put(pair3, conv3);

        assertEquals("Three distinct entries should be present", 3, overrides.size());
        assertTrue(overrides.containsKey(pair1));
        assertTrue(overrides.containsKey(pair2));
        assertTrue(overrides.containsKey(pair3));

        // Validate that each stored Convert works as expected
        Converter dummyConverter = null; // we don't need a real Converter for these tests
        assertEquals(Integer.valueOf(123), overrides.get(pair1).convert("123", dummyConverter));
        assertEquals("456", overrides.get(pair2).convert(456, dummyConverter));
        assertEquals("3.14", overrides.get(pair3).convert(3.14159, dummyConverter));
    }
}
