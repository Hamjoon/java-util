package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.AbstractMap;
import java.util.Locale;
import java.util.TimeZone;
import java.nio.charset.Charset;
import java.time.ZoneId;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link DefaultConverterOptions}.
 *
 * The tests exercise:
 * <ul>
 *   <li>Default construction (empty internal maps).</li>
 *   <li>Retrieval of custom options, including generic type handling,
 *       null keys and missing entries.</li>
 *   <li>Behaviour of the {@code converterOverrides} map exposed via
 *       {@link ConverterOptions#getConverterOverrides()} – mutability,
 *       null‑key/value rejection (ConcurrentHashMap contract) and identity
 *       of the returned map.</li>
 *   <li>Verification that the default methods defined in {@link ConverterOptions}
 *       (e.g., {@code getLocale()}, {@code getCharset()}, etc.) return {@code null}
 *       because {@code DefaultConverterOptions} does not override them.</li>
 * </ul>
 *
 * No mocking frameworks are used; all objects are instantiated directly,
 * and private fields are accessed only via reflection where the API does
 * not expose a mutator (custom options map).
 */
public class DefaultConverterOptionsTest {

    private DefaultConverterOptions options;

    @Before
    public void setUp() {
        options = new DefaultConverterOptions();
    }

    /* -------------------------------------------------------------
     * Helper – reflectively obtain the private {@code customOptions} map.
     * ------------------------------------------------------------- */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getCustomOptionsMap() throws Exception {
        Field field = DefaultConverterOptions.class.getDeclaredField("customOptions");
        field.setAccessible(true);
        return (Map<String, Object>) field.get(options);
    }

    /* -------------------------------------------------------------
     * 1. Construction – internal maps must be empty and of expected type.
     * ------------------------------------------------------------- */
    @Test
    public void testConstructorInitialState() {
        // converterOverrides exposed via the public API
        Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> overrides = options.getConverterOverrides();

        assertNotNull("converterOverrides map should never be null", overrides);
        assertTrue("converterOverrides map should be empty initially", overrides.isEmpty());
        assertTrue("converterOverrides should be a ConcurrentHashMap",
                overrides instanceof ConcurrentHashMap);

        // customOptions is not directly accessible – use reflection
        try {
            Map<String, Object> custom = getCustomOptionsMap();
            assertNotNull("customOptions map should never be null", custom);
            assertTrue("customOptions map should be empty initially", custom.isEmpty());
            assertTrue("customOptions should be a ConcurrentHashMap",
                    custom instanceof ConcurrentHashMap);
        } catch (Exception e) {
            fail("Reflection failed while checking customOptions map: " + e);
        }
    }

    /* -------------------------------------------------------------
     * 2. getCustomOption – normal retrieval.
     * ------------------------------------------------------------- */
    @Test
    public void testGetCustomOptionNormal() throws Exception {
        Map<String, Object> custom = getCustomOptionsMap();
        custom.put("answer", 42);
        custom.put("greeting", "hello");

        Integer answer = options.getCustomOption("answer");
        String greeting = options.getCustomOption("greeting");

        assertEquals(Integer.valueOf(42), answer);
        assertEquals("hello", greeting);
    }

    /* -------------------------------------------------------------
     * 3. getCustomOption – missing key returns null.
     * ------------------------------------------------------------- */
    @Test
    public void testGetCustomOptionMissingKey() {
        assertNull("Missing key should yield null", options.getCustomOption("nonexistent"));
    }

    /* -------------------------------------------------------------
     * 4. getCustomOption – null key is permitted by Map#get (returns null).
     * ------------------------------------------------------------- */
    @Test
    public void testGetCustomOptionNullKey() throws Exception {
        Map<String, Object> custom = getCustomOptionsMap();
        // ConcurrentHashMap does NOT allow null keys – we must not put a null key.
        // The test only verifies that asking for a null key does not throw.
        assertNull("Requesting a null key should return null and not throw", options.getCustomOption(null));
    }

    /* -------------------------------------------------------------
     * 5. getCustomOption – type mismatch results in ClassCastException
     *    at the call site (the method itself only performs an unchecked cast).
     * ------------------------------------------------------------- */
    @Test(expected = ClassCastException.class)
    public void testGetCustomOptionTypeMismatch() throws Exception {
        Map<String, Object> custom = getCustomOptionsMap();
        custom.put("number", 123); // stored as Integer

        // Attempt to retrieve as String – should throw ClassCastException
        @SuppressWarnings("unused")
        String wrong = options.getCustomOption("number");
    }

    /* -------------------------------------------------------------
     * 6. getConverterOverrides – map identity (same instance on each call).
     * ------------------------------------------------------------- */
    @Test
    public void testGetConverterOverridesIdentity() {
        Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> first = options.getConverterOverrides();
        Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> second = options.getConverterOverrides();

        assertSame("Multiple calls must return the same map instance", first, second);
    }

    /* -------------------------------------------------------------
     * 7. getConverterOverrides – mutability (adding entries affects the
     *    internal state and is reflected on subsequent reads).
     * ------------------------------------------------------------- */
    @Test
    public void testGetConverterOverridesMutability() {
        Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> overrides = options.getConverterOverrides();

        // Simple Convert implementation for testing
        Convert<String> toStringConvert = (from, converter) -> from == null ? null : from.toString();

        Map.Entry<Class<?>, Class<?>> key = new AbstractMap.SimpleEntry<>(Object.class, String.class);
        overrides.put(key, toStringConvert);

        // Verify that the map now contains the entry
        assertTrue("Overrides map should contain the newly added entry", overrides.containsKey(key));
        assertEquals("Stored Convert instance must be the same", toStringConvert, overrides.get(key));
    }

    /* -------------------------------------------------------------
     * 8. getConverterOverrides – ConcurrentHashMap rejects null key/value.
     * ------------------------------------------------------------- */
    @Test(expected = NullPointerException.class)
    public void testConverterOverridesRejectNullKey() {
        Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> overrides = options.getConverterOverrides();
        overrides.put(null, (from, converter) -> null);
    }

    @Test(expected = NullPointerException.class)
    public void testConverterOverridesRejectNullValue() {
        Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> overrides = options.getConverterOverrides();
        Map.Entry<Class<?>, Class<?>> key = new AbstractMap.SimpleEntry<>(Object.class, String.class);
        overrides.put(key, null);
    }

    /* -------------------------------------------------------------
     * 9. Default methods from ConverterOptions – they should return null
     *    because DefaultConverterOptions does not override them.
     * ------------------------------------------------------------- */
    @Test
    public void testDefaultConverterOptionsDefaultMethodsReturnNull() {
        assertNull("Default getZoneId() should return null", options.getZoneId());
        assertNull("Default getLocale() should return null", options.getLocale());
        assertNull("Default getCharset() should return null", options.getCharset());
        assertNull("Default getClassLoader() should return null", options.getClassLoader());
        assertNull("Default getTimeZone() should return null", options.getTimeZone());
        assertNull("Default trueChar() should return null", options.trueChar());
        assertNull("Default falseChar() should return null", options.falseChar());
    }
}
