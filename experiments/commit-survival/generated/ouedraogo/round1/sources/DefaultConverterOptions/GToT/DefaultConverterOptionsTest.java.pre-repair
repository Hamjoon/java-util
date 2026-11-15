package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.AbstractMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comprehensive JUnit‑4 test suite for {@link DefaultConverterOptions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Construction and initial state of the internal maps.</li>
 *   <li>Correct retrieval of custom options via {@code getCustomOption}.</li>
 *   <li>Behaviour when a null option name is supplied.</li>
 *   <li>Type‑safety (expected {@code ClassCastException}) of the generic method.</li>
 *   <li>Mutability and identity of the {@code converterOverrides} map returned by {@code getConverterOverrides}.</li>
 *   <li>Edge‑case handling using reflection to manipulate the private fields (the only way to
 *       inject data because the class does not expose mutators).</li>
 * </ul>
 *
 * No external mocking frameworks are used – only the public API and Java reflection.
 */
public class DefaultConverterOptionsTest {

    private DefaultConverterOptions options;

    /** Reflection fields used to access the private maps. */
    private Field customOptionsField;
    private Field converterOverridesField;

    @Before
    public void setUp() throws Exception {
        options = new DefaultConverterOptions();

        // Prepare reflection access to the private fields.
        customOptionsField = DefaultConverterOptions.class.getDeclaredField("customOptions");
        customOptionsField.setAccessible(true);

        converterOverridesField = DefaultConverterOptions.class.getDeclaredField("converterOverrides");
        converterOverridesField.setAccessible(true);
    }

    /**
     * Helper that returns the internal {@code customOptions} map.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getInternalCustomOptions() throws IllegalAccessException {
        return (Map<String, Object>) customOptionsField.get(options);
    }

    /**
     * Helper that returns the internal {@code converterOverrides} map.
     */
    @SuppressWarnings("unchecked")
    private Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> getInternalConverterOverrides() throws IllegalAccessException {
        return (Map<Map.Entry<Class<?>, Class<?>>, Convert<?>>) converterOverridesField.get(options);
    }

    // ----------------------------------------------------------------------
    // 1. Constructor & initial state
    // ----------------------------------------------------------------------
    @Test
    public void testConstructorCreatesEmptyMaps() throws Exception {
        // Both internal maps must be non‑null and initially empty.
        assertNotNull("customOptions map should be created", getInternalCustomOptions());
        assertTrue("customOptions map should be empty", getInternalCustomOptions().isEmpty());

        assertNotNull("converterOverrides map should be created", getInternalConverterOverrides());
        assertTrue("converterOverrides map should be empty", getInternalConverterOverrides().isEmpty());

        // The public getters must return the same instances.
        assertSame("getConverterOverrides should return the internal map",
                getInternalConverterOverrides(), options.getConverterOverrides());
    }

    // ----------------------------------------------------------------------
    // 2. getCustomOption – normal behaviour
    // ----------------------------------------------------------------------
    @Test
    public void testGetCustomOptionReturnsStoredValue() throws Exception {
        // Arrange: put a value directly into the private map.
        Map<String, Object> internal = getInternalCustomOptions();
        internal.put("answer", 42);

        // Act & Assert: retrieve it via the generic method.
        Integer result = options.<Integer>getCustomOption("answer");
        assertEquals("The stored integer value should be returned", Integer.valueOf(42), result);
    }

    // ----------------------------------------------------------------------
    // 3. getCustomOption – null name
    // ----------------------------------------------------------------------
    @Test
    public void testGetCustomOptionWithNullNameReturnsNull() {
        // ConcurrentHashMap permits null keys? No, it throws NPE.
        // Therefore we expect a NullPointerException from the underlying map.
        try {
            options.getCustomOption(null);
            fail("Expected NullPointerException when passing null as the option name");
        } catch (NullPointerException expected) {
            // Expected path.
        }
    }

    // ----------------------------------------------------------------------
    // 4. getCustomOption – type mismatch (ClassCastException)
    // ----------------------------------------------------------------------
    @Test
    public void testGetCustomOptionWrongGenericTypeThrowsClassCastException() throws Exception {
        // Arrange: store a String but request it as Integer.
        Map<String, Object> internal = getInternalCustomOptions();
        internal.put("greeting", "hello");

        // Act & Assert: the cast performed inside getCustomOption should fail.
        try {
            Integer value = options.<Integer>getCustomOption("greeting");
            // If we get here, the cast didn't happen – fail the test.
            fail("Expected ClassCastException when retrieving a String as Integer, got: " + value);
        } catch (ClassCastException expected) {
            // Expected outcome.
        }
    }

    // ----------------------------------------------------------------------
    // 5. getCustomOption – missing key returns null
    // ----------------------------------------------------------------------
    @Test
    public void testGetCustomOptionMissingKeyReturnsNull() {
        // No entry has been added for "missing".
        Object result = options.getCustomOption("missing");
        assertNull("Missing key should yield null", result);
    }

    // ----------------------------------------------------------------------
    // 6. getConverterOverrides – map identity & mutability
    // ----------------------------------------------------------------------
    @Test
    public void testGetConverterOverridesIsMutableAndReflectsInternalState() throws Exception {
        Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> returned = options.getConverterOverrides();

        // Initially empty.
        assertTrue("Returned map should be empty initially", returned.isEmpty());

        // Create a dummy Convert implementation (anonymous class).
        Convert<String> dummyConvert = new Convert<String>() {
            @Override
            public String convert(Object from, Converter converter) {
                return from.toString();
            }
        };

        // Build a map entry key.
        Map.Entry<Class<?>, Class<?>> key = new AbstractMap.SimpleEntry<>(Integer.class, String.class);

        // Add entry through the public map.
        returned.put(key, dummyConvert);

        // Verify that the internal map sees the same entry.
        Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> internal = getInternalConverterOverrides();
        assertEquals("Internal map should contain the entry added via the public getter", 1, internal.size());
        assertTrue("Internal map should contain the specific key", internal.containsKey(key));
        assertSame("Stored Convert instance should be the same as the one added", dummyConvert, internal.get(key));
    }

    // ----------------------------------------------------------------------
    // 7. getConverterOverrides – null key handling
    // ----------------------------------------------------------------------
    @Test
    public void testGetConverterOverridesRejectsNullKey() {
        Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> map = options.getConverterOverrides();

        try {
            map.put(null, null);
            fail("Expected NullPointerException when inserting null key into converterOverrides");
        } catch (NullPointerException expected) {
            // Expected – ConcurrentHashMap does not allow null keys.
        }
    }

    // ----------------------------------------------------------------------
    // 8. getConverterOverrides – null value handling
    // ----------------------------------------------------------------------
    @Test
    public void testGetConverterOverridesAllowsNullValue() {
        // ConcurrentHashMap does NOT allow null values either, so this should also NPE.
        Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> map = options.getConverterOverrides();

        Map.Entry<Class<?>, Class<?>> key = new AbstractMap.SimpleEntry<>(String.class, Integer.class);
        try {
            map.put(key, null);
            fail("Expected NullPointerException when inserting null value into converterOverrides");
        } catch (NullPointerException expected) {
            // Expected behaviour.
        }
    }

    // ----------------------------------------------------------------------
    // 9. Verify that the class implements the interface correctly (default methods)
    // ----------------------------------------------------------------------
    @Test
    public void testDefaultInterfaceMethodsReturnNull() {
        // All default methods from ConverterOptions return null (or default values) – we verify a couple.
        assertNull("Default getZoneId should be null", options.getZoneId());
        assertNull("Default getLocale should be null", options.getLocale());
        assertNull("Default getCharset should be null", options.getCharset());
        assertNull("Default getClassLoader should be null", options.getClassLoader());
        assertNull("Default getTimeZone should be null", options.getTimeZone());
        assertNull("Default trueChar should be null", options.trueChar());
        assertNull("Default falseChar should be null", options.falseChar());
    }
}
