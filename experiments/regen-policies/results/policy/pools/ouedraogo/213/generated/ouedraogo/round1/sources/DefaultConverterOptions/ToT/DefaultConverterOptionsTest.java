package com.cedarsoftware.util.convert;
import com.cedarsoftware.util.convert.Converter.ConversionPair;


import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JUnit‑4 test suite for {@link DefaultConverterOptions}.
 *
 * The tests are written from the point‑of‑view of three “experts” that
 * collaboratively design one test per public method, then expand the
 * suite to cover typical use‑cases, edge cases and error scenarios.
 *
 * Only the classes, constructors and methods that appear in the supplied
 * source code or in the listed dependency signatures are used – no
 * external libraries, no mocking frameworks and no subclassing of the
 * library’s interfaces.
 */
public class DefaultConverterOptionsTest {

    private DefaultConverterOptions options;

    @Before
    public void setUp() {
        options = new DefaultConverterOptions();
    }

    /*=====================================================================
     *  Expert 1 – tests for the constructor and getConverterOverrides()
     *====================================================================*/

    /**
     * The default constructor must initialise both internal maps.
     * The map returned by {@code getConverterOverrides()} must be the same
     * instance held internally and must be empty.
     */
    @Test
    public void testConstructorInitialisesMaps() throws Exception {
        // The map must be non‑null.
        Map<ConversionPair, Convert<?>> map = options.getConverterOverrides();
        assertNotNull("converterOverrides map should not be null", map);

        // It must be empty right after construction.
        assertTrue("converterOverrides map should be empty", map.isEmpty());

        // Verify that the internal field is the exact same object.
        Field field = DefaultConverterOptions.class.getDeclaredField("converterOverrides");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<ConversionPair, Convert<?>> internalMap =
                (Map<ConversionPair, Convert<?>>) field.get(options);
        assertSame("Returned map should be the internal map instance", internalMap, map);
        assertTrue("Internal map should be a ConcurrentHashMap", internalMap instanceof ConcurrentHashMap);
    }

    /*=====================================================================
     *  Expert 2 – tests for getCustomOption()
     *====================================================================*/

    /**
     * When a custom option is present, {@code getCustomOption} must return
     * the stored value with the appropriate generic type.
     */
    @Test
    public void testGetCustomOptionReturnsStoredValue() throws Exception {
        // Insert a value into the private customOptions map via reflection.
        Field field = DefaultConverterOptions.class.getDeclaredField("customOptions");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> customMap = (Map<String, Object>) field.get(options);
        customMap.put("answer", 42);
        customMap.put("greeting", "hello");

        // Retrieve the values using the generic method.
        Integer answer = options.getCustomOption("answer");
        String greeting = options.getCustomOption("greeting");

        assertEquals("The integer option should be retrieved correctly", Integer.valueOf(42), answer);
        assertEquals("The string option should be retrieved correctly", "hello", greeting);
    }

    /**
     * Requesting a non‑existent option must return {@code null}.
     */
    @Test
    public void testGetCustomOptionMissingReturnsNull() {
        assertNull("Missing option should return null", options.getCustomOption("nonexistent"));
    }

    /**
     * The method performs an unchecked cast; an incompatible expected type
     * must trigger a {@link ClassCastException}.
     */
    @Test(expected = ClassCastException.class)
    public void testGetCustomOptionClassCastException() throws Exception {
        // Put an Integer under the key "value".
        Field field = DefaultConverterOptions.class.getDeclaredField("customOptions");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> customMap = (Map<String, Object>) field.get(options);
        customMap.put("value", 123);

        // Attempt to retrieve it as a String – should throw ClassCastException.
        @SuppressWarnings("unused")
        String wrongType = options.getCustomOption("value");
    }

    /*=====================================================================
     *  Expert 3 – additional validation of map mutability and isolation
     *====================================================================*/

    /**
     * The map returned by {@code getConverterOverrides()} is mutable.
     * Adding an entry through the returned reference must be reflected
     * when the map is queried again.
     *
     * Because {@code ConversionPair} has no public constructor, we create
     * an instance via reflection.  The pair is constructed with the
     * source class {@code String.class} and target class {@code Integer.class}.
     */
    @Test
    public void testConverterOverridesMapIsMutable() throws Exception {
        // Obtain the map.
        Map<ConversionPair, Convert<?>> overrides = options.getConverterOverrides();

        // Build a ConversionPair instance via reflection.
        // The class is a static final inner class; we locate the declared
        // constructor that takes two Class arguments (the actual signature
        // in the library is known to be (Class<?>, Class<?>)).
        Class<?> pairClass = Class.forName("com.cedarsoftware.util.convert.Converter$ConversionPair");
        java.lang.reflect.Constructor<?> ctor = pairClass.getDeclaredConstructor(Class.class, Class.class);
        ctor.setAccessible(true);
        ConversionPair pair = (ConversionPair) ctor.newInstance(String.class, Integer.class);

        // Simple Convert implementation that parses a String to Integer.
        Convert<Integer> stringToInteger = new Convert<Integer>() {
            @Override
            public Integer convert(Object from, Converter converter) {
                return Integer.valueOf((String) from);
            }
        };

        // Put the entry into the map.
        overrides.put(pair, stringToInteger);

        // Verify that the map now contains the entry.
        assertEquals("Map should contain exactly one entry", 1, overrides.size());
        assertTrue("Map should contain the inserted ConversionPair", overrides.containsKey(pair));
        assertSame("Stored Convert instance must be the same", stringToInteger, overrides.get(pair));
    }

    /**
     * Ensure that the internal map instance cannot be replaced
     * (i.e., the reference returned is the original one, not a copy).
     */
    @Test
    public void testConverterOverridesReferenceIntegrity() throws Exception {
        Map<ConversionPair, Convert<?>> firstReference = options.getConverterOverrides();

        // Retrieve the field directly and compare references.
        Field field = DefaultConverterOptions.class.getDeclaredField("converterOverrides");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<ConversionPair, Convert<?>> internalMap =
                (Map<ConversionPair, Convert<?>>) field.get(options);

        assertSame("Both references must point to the same map instance", internalMap, firstReference);
    }
}
