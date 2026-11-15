package com.cedarsoftware.util.convert;
import com.cedarsoftware.util.convert.Converter.ConversionPair;


import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Before;
import org.junit.Test;

/**
 * JUnit‑4 test suite for {@link DefaultConverterOptions}.
 */
public class DefaultConverterOptionsTest {

    private DefaultConverterOptions options;

    @Before
    public void setUp() {
        options = new DefaultConverterOptions();
    }

    /* ------------------------------------------------------------------------
     *  Tests for getCustomOption(String)
     * ------------------------------------------------------------------------ */

    @Test
    public void testGetCustomOptionReturnsNullWhenAbsent() {
        assertNull("Expect null for a key that has never been added", options.getCustomOption("missing"));
    }

    @Test
    public void testGetCustomOptionWithNullKey() {
        // Map#get(null) is legal for ConcurrentHashMap – it returns null (no entry)
        assertNull("Expect null when key is null", options.getCustomOption(null));
    }

    @Test
    public void testGetCustomOptionReturnsStoredValue() throws Exception {
        // Use reflection to put a value into the private map
        Field mapField = DefaultConverterOptions.class.getDeclaredField("customOptions");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> internalMap = (Map<String, Object>) mapField.get(options);
        internalMap.put("intKey", 42);

        Integer value = options.getCustomOption("intKey");
        assertNotNull("Value should be present", value);
        assertEquals(Integer.valueOf(42), value);
    }

    @Test
    public void testGetCustomOptionReturnsStoredNullValue() throws Exception {
        Field mapField = DefaultConverterOptions.class.getDeclaredField("customOptions");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> internalMap = (Map<String, Object>) mapField.get(options);
        internalMap.put("nullValue", null);

        Object result = options.getCustomOption("nullValue");
        assertNull("Stored null value should be returned as null", result);
    }

    @Test(expected = ClassCastException.class)
    public void testGetCustomOptionClassCastException() throws Exception {
        // Store an Integer but retrieve it as a String – unchecked cast should fail
        Field mapField = DefaultConverterOptions.class.getDeclaredField("customOptions");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> internalMap = (Map<String, Object>) mapField.get(options);
        internalMap.put("number", 123);

        // The following line must throw ClassCastException
        @SuppressWarnings("unused")
        String bogus = options.getCustomOption("number");
    }

    /* ------------------------------------------------------------------------
     *  Tests for getConverterOverrides()
     * ------------------------------------------------------------------------ */

    @Test
    public void testGetConverterOverridesInitiallyEmpty() {
        Map<ConversionPair, Convert<?>> map = options.getConverterOverrides();
        assertTrue("Initially the overrides map should be empty", map.isEmpty());
    }

    @Test
    public void testGetConverterOverridesIsMutable() {
        Map<ConversionPair, Convert<?>> map = options.getConverterOverrides();

        // Create a dummy ConversionPair and Convert implementation
        ConversionPair pair = new ConversionPair() {
            @Override public Class<?> getSource() { return String.class; }
            @Override public Class<?> getTarget() { return Integer.class; }
            @Override public boolean equals(Object o) {
                return (o instanceof ConversionPair) &&
                       ((ConversionPair) o).getSource().equals(getSource()) &&
                       ((ConversionPair) o).getTarget().equals(getTarget());
            }
            @Override public int hashCode() {
                return 31 * getSource().hashCode() + getTarget().hashCode();
            }
        };

        Convert<Integer> dummyConvert = new Convert<Integer>() {
            @Override public Integer convert(Object from, com.cedarsoftware.util.convert.Converter converter) {
                return Integer.valueOf(from.toString());
            }
        };

        map.put(pair, dummyConvert);
        assertFalse("Map should contain the newly added entry", map.isEmpty());
        assertSame("The stored convert instance should be the one we added", dummyConvert, map.get(pair));
    }

    @Test
    public void testGetConverterOverridesReferenceEquality() {
        Map<ConversionPair, Convert<?>> firstCall = options.getConverterOverrides();
        Map<ConversionPair, Convert<?>> secondCall = options.getConverterOverrides();
        assertSame("Multiple calls must return the same map instance", firstCall, secondCall);
    }

    @Test(expected = NullPointerException.class)
    public void testConverterOverridesRejectsNullKey() {
        Map<ConversionPair, Convert<?>> map = options.getConverterOverrides();
        map.put(null, (from, conv) -> null); // ConcurrentHashMap does not allow null keys
    }

    @Test(expected = NullPointerException.class)
    public void testConverterOverridesRejectsNullValue() {
        Map<ConversionPair, Convert<?>> map = options.getConverterOverrides();
        ConversionPair dummyPair = new ConversionPair() {
            @Override public Class<?> getSource() { return Object.class; }
            @Override public Class<?> getTarget() { return Object.class; }
            @Override public boolean equals(Object o) { return this == o; }
            @Override public int hashCode() { return System.identityHashCode(this); }
        };
        map.put(dummyPair, null); // ConcurrentHashMap does not allow null values
    }

    /* ------------------------------------------------------------------------
     *  Tests for default methods inherited from ConverterOptions (optional)
     * ------------------------------------------------------------------------ */

    @Test
    public void testDefaultMethodsReturnNull() {
        // The default methods are not overridden; they should return null per library contract
        assertNull(options.getZoneId());
        assertNull(options.getLocale());
        assertNull(options.getCharset());
        assertNull(options.getClassLoader());
        assertNull(options.getTimeZone());
        assertNull(options.trueChar());
        assertNull(options.falseChar());
    }
}
