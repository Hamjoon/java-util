package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@ExtendWith(MockitoExtension.class)
class CompactMap_getSingleValueKey_Test {

    /**
     * A plain CompactMap used for the default‑behavior test.
     */
    private CompactMap<String, Integer> defaultMap;

    private CustomKeyMap customMap;

    /**
     * A CompactMap subclass that overrides {@code getSingleValueKey()}.
     */
    private static class CustomKeyMap extends CompactMap<String, Integer> {

        @Override
        protected String getSingleValueKey() {
            return "customKey";
        }
    }

    /**
     * Subclass that does *not* override the method but exposes it as {@code public}
     * so that tests can call it without reflection.
     */
    private static class PublicExposeMap<K, V> extends CompactMap<K, V> {

        @SuppressWarnings("unchecked")
        public K exposeSingleValueKey() {
            return super.getSingleValueKey();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh instances for each test to avoid cross‑test contamination.
        defaultMap = new CompactMap<>();
        customMap = new CustomKeyMap();
    }

    @Test
    void defaultSingleValueKey_isId() {
        // The default implementation should return the constant "id".
        String key = defaultMap.getSingleValueKey();
        assertEquals("id", key, "Default single value key should be \"id\"");
    }

    @Test
    void defaultSingleValueKey_isConsistentAcrossCalls() {
        // Repeated calls must return an equal (and ideally identical) value.
        String first = defaultMap.getSingleValueKey();
        String second = defaultMap.getSingleValueKey();
        assertEquals(first, second, "Repeated calls must return the same key");
        assertSame(first, second, "Repeated calls should return the same instance");
    }

    @Test
    void overriddenSingleValueKey_returnsCustomKey() {
        // The subclass overrides the method; ensure the custom value is returned.
        String key = customMap.getSingleValueKey();
        assertEquals("customKey", key, "Overridden key should be \"customKey\"");
    }

    @Test
    void overriddenSingleValueKey_isStable() {
        // Ensure stability of the overridden value across multiple invocations.
        String first = customMap.getSingleValueKey();
        String second = customMap.getSingleValueKey();
        assertEquals(first, second, "Overridden key should be stable across calls");
    }

    @Test
    void singleValueKey_isAssignableToGenericType() {
        // The method returns a value of type K; confirm that it can be assigned
        // to a variable of the generic type without a ClassCastException.
        // K is String in this instance
        String key = defaultMap.getSingleValueKey();
        assertNotNull(key, "Key should not be null");
        assertTrue(key instanceof String, "Key should be an instance of String");
    }

    @Test
    void defaultSingleValueKey_isAssignableToGenericType() {
        // Ensure the returned value can be assigned to K without a cast exception.
        // K is String here
        String key = defaultMap.getSingleValueKey();
        assertNotNull(key);
        assertTrue(key instanceof String);
    }

    @Test
    void exposeSingleValueKey_viaPublicSubclass_returnsDefault() {
        PublicExposeMap<String, Integer> map = new PublicExposeMap<>();
        String key = map.exposeSingleValueKey();
        assertEquals(CompactMap.DEFAULT_SINGLE_KEY, key);
    }

    @Test
    void exposeSingleValueKey_referenceEqualityWithConstant() {
        PublicExposeMap<String, Integer> map = new PublicExposeMap<>();
        String key = map.exposeSingleValueKey();
        // The constant is a literal, thus interned – == is expected.
        assertSame(CompactMap.DEFAULT_SINGLE_KEY, key, "Returned key should be the exact same instance as the static constant");
    }

    @Test
    void singleValueKey_withGenericObject_returnsId() {
        CompactMap<Object, String> map = new CompactMap<>();
        Object key = map.getSingleValueKey();
        assertEquals("id", key);
    }

    @Test
    void singleValueKey_withGenericInteger_throwsClassCastException() {
        // CompactMap<Integer, String> still returns a String ("id").
        CompactMap<Integer, String> map = new CompactMap<>();
        // Attempting to assign the result to Integer forces a ClassCastException.
        assertThrows(ClassCastException.class, () -> {
            @SuppressWarnings("unused")
            Integer // illegal cast
            casted = (Integer) map.getSingleValueKey();
        });
    }

    @Test
    void singleValueKey_afterMapModifications_stillId() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertEquals("id", map.getSingleValueKey(), "Method must ignore current map contents");
    }

    @Test
    void reflectionInvoke_getSingleValueKey_returnsId() throws Exception {
        Method m = CompactMap.class.getDeclaredMethod("getSingleValueKey");
        m.setAccessible(true);
        CompactMap<String, Integer> map = new CompactMap<>();
        Object result = m.invoke(map);
        assertEquals("id", result);
        assertSame(CompactMap.DEFAULT_SINGLE_KEY, result, "Reflection call should return the same constant instance");
    }

    @Test
    void reflectionInvoke_withNullTarget_throwsNullPointerException() throws Exception {
        Method m = CompactMap.class.getDeclaredMethod("getSingleValueKey");
        m.setAccessible(true);
        assertThrows(NullPointerException.class, () -> m.invoke(null));
    }

    @ParameterizedTest(name = "Repeated call #{index} returns same key")
    @ValueSource(ints = { 1, 2, 3, 4, 5 })
    void repeatedCalls_consistency(int iteration) {
        // The iteration value is irrelevant; we just call the method twice.
        String first = defaultMap.getSingleValueKey();
        String second = defaultMap.getSingleValueKey();
        assertEquals(first, second);
        assertSame(first, second);
    }
}
