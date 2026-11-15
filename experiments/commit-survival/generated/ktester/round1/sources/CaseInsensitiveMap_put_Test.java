package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;

class CaseInsensitiveMap_put_Test {

    private CaseInsensitiveMap<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default constructor – it creates an internal LinkedHashMap.
        map = new CaseInsensitiveMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to ensure isolation between tests.
        map.clear();
    }

    @Test
    void put_StringKey_ShouldBeCaseInsensitive() {
        // given
        String keyLower = "hello";
        String keyUpper = "HELLO";
        // when
        Integer first = map.put(keyLower, 1);
        // same logical key, different case
        Integer second = map.put(keyUpper, 2);
        // then
        assertNull(first, "First put should return null because the map was empty");
        assertEquals(1, second, "Second put should return the previous value for the same logical key");
        assertEquals(1, map.size(), "Map should contain only one entry because keys are case‑insensitive");
        // The stored key should retain the original case of the first insertion.
        assertTrue(map.keySet().contains(keyLower), "Key set should contain the original key case");
        assertFalse(map.keySet().contains(keyUpper), "Key set should not contain the second case variant");
        // Retrieval with any case should work.
        assertEquals(2, map.get("HeLLo"));
    }

    @Test
    void put_NonStringKey_ShouldBehaveLikeNormalMap() {
        // given
        Integer intKey = 42;
        String strKey = "forty‑two";
        // when
        String first = map.put(intKey, "answer");
        String second = map.put(strKey, "string key");
        // then
        assertNull(first, "First put with Integer key should return null");
        assertNull(second, "First put with String key should return null (different logical key)");
        assertEquals(2, map.size(), "Both entries should be present because keys are of different types");
        assertEquals("answer", map.get(intKey));
        assertEquals("string key", map.get(strKey));
    }

    @Test
    void put_ReplaceExistingValue_ShouldReturnPreviousValue() {
        // given
        map.put("Test", 10);
        // when
        // same logical key, different case
        Integer previous = map.put("test", 20);
        // then
        assertEquals(10, previous, "put should return the previous value associated with the logical key");
        assertEquals(1, map.size(), "Map should still contain only one entry");
        assertEquals(20, map.get("TEST"));
    }

    @Test
    void put_NullKeyAndValue_ShouldDelegateToUnderlyingMap() {
        // given
        // LinkedHashMap (the underlying map) permits a single null key and null values.
        // when
        Integer previous = map.put(null, 123);
        Integer previous2 = map.put(null, 456);
        // then
        assertNull(previous, "First insertion of null key should return null");
        assertEquals(123, previous2, "Second insertion should return the previous value");
        assertEquals(1, map.size(), "Only one null key entry should exist");
        assertEquals(456, map.get(null));
    }

    /**
     * Helper that creates a {@code CaseInsensitiveMap} and injects the supplied
     * backing {@code Map} via reflection.
     */
    @SuppressWarnings("unchecked")
    private <K, V> CaseInsensitiveMap<K, V> createWithBacking(Map<K, V> backing) {
        CaseInsensitiveMap<K, V> ciMap = new CaseInsensitiveMap<>();
        try {
            Field f = CaseInsensitiveMap.class.getDeclaredField("map");
            f.setAccessible(true);
            f.set(ciMap, backing);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return ciMap;
    }

    @Test
    void put_StringKey_MultipleCaseVariations() {
        // first insertion
        Integer r1 = map.put("Hello", 100);
        assertNull(r1, "First put should return null");
        // second insertion – different case, should replace
        Integer r2 = map.put("HELLO", 200);
        assertEquals(100, r2, "Second put should return previous value");
        // third insertion – another case variant, should replace again
        Integer r3 = map.put("HeLLo", 300);
        assertEquals(200, r3, "Third put should return previous value");
        // final state checks
        assertEquals(1, map.size(), "Only one logical entry should exist");
        assertEquals(300, map.get("hello"));
        assertTrue(map.keySet().contains("Hello"));
        assertFalse(map.keySet().contains("HELLO"));
    }

    @Test
    void put_NonStringKey_IntegerKeyReplace() {
        Integer r1 = map.put(42, 1);
        assertNull(r1, "First put should return null");
        Integer r2 = map.put(42, 2);
        assertEquals(1, r2, "Second put should return previous value");
        assertEquals(1, map.size());
        assertEquals(2, map.get(42));
    }

    @Test
    void put_NullKey_WithLinkedHashMap() {
        Integer r1 = map.put(null, 10);
        assertNull(r1);
        Integer r2 = map.put(null, 20);
        assertEquals(10, r2);
        assertEquals(1, map.size());
        assertEquals(20, map.get(null));
    }

    @Test
    void put_StringKey_NullValueThenReplace() {
        Integer r1 = map.put("Nullable", null);
        assertNull(r1);
        assertNull(map.get("Nullable"));
        Integer r2 = map.put("Nullable", 99);
        assertNull(r2, "Replacing a null value should return null (previous value)");
        assertEquals(99, map.get("Nullable"));
    }

    @Test
    void put_CharSequenceNonStringKey_StringBuilder() {
        StringBuilder sb = new StringBuilder("builder");
        String r1 = map.put(sb, "SeqValue");
        assertNull(r1);
        String r2 = map.put(sb, "SeqValue2");
        assertEquals("SeqValue", r2);
        assertEquals(1, map.size());
        assertEquals("SeqValue2", map.get(sb));
    }

    @Test
    void put_StringKey_WithConcurrentHashMapBacking() {
        CaseInsensitiveMap<String, String> ciMap = createWithBacking(new ConcurrentHashMap<>());
        String r1 = ciMap.put("ConcurrentKey", "ConcurrentValue");
        assertNull(r1);
        assertEquals("ConcurrentValue", ciMap.get("concurrentkey"));
        assertEquals(1, ciMap.size());
    }

    @Test
    void put_NonStringKey_WithConcurrentHashMapBacking() {
        CaseInsensitiveMap<Integer, String> ciMap = createWithBacking(new ConcurrentHashMap<>());
        String r1 = ciMap.put(1001, "ConcurrentNonString");
        assertNull(r1);
        assertEquals("ConcurrentNonString", ciMap.get(1001));
        assertEquals(1, ciMap.size());
    }

    @Test
    void put_NullKey_WithConcurrentHashMap_ShouldThrowNPE() {
        CaseInsensitiveMap<String, String> ciMap = createWithBacking(new ConcurrentHashMap<>());
        assertThrows(NullPointerException.class, () -> ciMap.put(null, "someValue"));
    }

    @Test
    void put_NullValue_WithConcurrentHashMap_ShouldThrowNPE() {
        CaseInsensitiveMap<String, String> ciMap = createWithBacking(new ConcurrentHashMap<>());
        assertThrows(NullPointerException.class, () -> ciMap.put("nonNullKey", null));
    }

    @Test
    void put_StringKey_WithTreeMap_ShouldThrowClassCastException() {
        // TreeMap uses natural ordering; CaseInsensitiveString does NOT implement Comparable.
        CaseInsensitiveMap<String, String> ciMap = createWithBacking(new TreeMap<>());
        assertThrows(ClassCastException.class, () -> ciMap.put("CaseKey", "value1"));
    }

    @Test
    void put_StringKey_WithConcurrentSkipListMap_ShouldThrowClassCastException() {
        // ConcurrentSkipListMap also relies on natural ordering.
        CaseInsensitiveMap<String, String> ciMap = createWithBacking(new ConcurrentSkipListMap<>());
        assertThrows(ClassCastException.class, () -> ciMap.put("AnotherKey", "value2"));
    }

    @Test
    void put_WithNullBackingMap_ShouldThrowNPE() throws Exception {
        CaseInsensitiveMap<String, String> ciMap = new CaseInsensitiveMap<>();
        // Force the internal map reference to null via reflection.
        Field f = CaseInsensitiveMap.class.getDeclaredField("map");
        f.setAccessible(true);
        f.set(ciMap, null);
        assertThrows(NullPointerException.class, () -> ciMap.put("anyKey", "anyValue"));
    }

    @ParameterizedTest(name = "[{index}] put ''{0}'' => ''{1}'' returns {2}")
    @CsvSource({ // key, value, expectedPrevious (null means no previous mapping)
    "'Hello',   'World',    null", "'HELLO',   'Universe', 'World'", "'HeLLo',   'Galaxy',   'Universe'" })
    void put_StringKey_Parameterized(String key, String value, String expectedPrev) {
        // The map is freshly created for each invocation (handled by @BeforeEach).
        String previous = map.put(key, value);
        if ("null".equals(expectedPrev)) {
            assertNull(previous, "Expected no previous mapping for key '" + key + "'");
        } else {
            assertEquals(expectedPrev, previous, "Unexpected previous value for key '" + key + "'");
        }
        // Verify retrieval works regardless of case.
        assertEquals(value, map.get(key.toUpperCase()));
        assertEquals(value, map.get(key.toLowerCase()));
    }
}
