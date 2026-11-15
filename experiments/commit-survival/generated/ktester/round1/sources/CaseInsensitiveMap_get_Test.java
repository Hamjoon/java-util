package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

class CaseInsensitiveMap_get_Test {

    private CaseInsensitiveMap<String, Integer> ciMap;

    private Map<String, Integer> referenceMap;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default constructor (LinkedHashMap) for deterministic ordering.
        ciMap = new CaseInsensitiveMap<>();
        // Populate the map with a mixture of keys.
        ciMap.put("Alpha", 1);
        ciMap.put("Beta", 2);
        ciMap.put("Gamma", 3);
        // Keep a reference map to compare expected values.
        referenceMap = new LinkedHashMap<>();
        referenceMap.put("Alpha", 1);
        referenceMap.put("Beta", 2);
        referenceMap.put("Gamma", 3);
        // Default constructor uses a LinkedHashMap internally – deterministic order.
        ciMap = new CaseInsensitiveMap<>();
        // Populate with a few distinct keys.
        ciMap.put("Alpha", 1);
        ciMap.put("Beta", 2);
        ciMap.put("Gamma", 3);
        referenceMap = new LinkedHashMap<>();
        referenceMap.put("Alpha", 1);
        referenceMap.put("Beta", 2);
        referenceMap.put("Gamma", 3);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure each test starts with a clean map.
        ciMap.clear();
        referenceMap.clear();
        ciMap.clear();
        referenceMap.clear();
    }

    /**
     * Uses reflection to replace the private {@code map} field of a
     * {@link CaseInsensitiveMap} instance.
     */
    private static <K, V> void setInternalMap(CaseInsensitiveMap<K, V> target, Map<K, V> newMap) {
        try {
            Field f = CaseInsensitiveMap.class.getDeclaredField("map");
            f.setAccessible(true);
            f.set(target, newMap);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a fresh {@link CaseInsensitiveMap} whose internal map is the
     * supplied {@code backingMap}. The map is populated with the given
     * entries using the public {@code put} method so that any internal
     * wrapping (e.g., {@code CaseInsensitiveString}) occurs as in normal usage.
     */
    private static <K, V> CaseInsensitiveMap<K, V> createWithBackingMap(Map<K, V> backingMap, Map<K, V> entries) {
        CaseInsensitiveMap<K, V> ci = new CaseInsensitiveMap<>();
        setInternalMap(ci, backingMap);
        // Populate via public API – this ensures the same wrapping logic as
        // production code.
        entries.forEach(ci::put);
        return ci;
    }

    @Test
    void get_StringKeyExactCase_ReturnsValue() {
        // Given
        String key = "Alpha";
        // When
        Integer value = ciMap.get(key);
        // Then
        assertEquals(referenceMap.get(key), value, "Exact‑case lookup should return the stored value.");
    }

    @Test
    void get_StringKeyDifferentCase_ReturnsValue() {
        // Given
        // different case from the stored key "Alpha"
        String key = "alpha";
        // When
        Integer value = ciMap.get(key);
        // Then
        assertEquals(referenceMap.get("Alpha"), value, "Lookup should be case‑insensitive for String keys.");
    }

    @Test
    void get_StringKeyMixedCase_ReturnsValue() {
        // Given
        String key = "AlPhA";
        // When
        Integer value = ciMap.get(key);
        // Then
        assertEquals(referenceMap.get("Alpha"), value, "Lookup should be case‑insensitive regardless of mixed case.");
    }

    @Test
    void get_NonStringKey_ReturnsValue() {
        // Given
        CaseInsensitiveMap<Integer, String> intKeyMap = new CaseInsensitiveMap<>();
        intKeyMap.put(10, "ten");
        intKeyMap.put(20, "twenty");
        // When
        String value = intKeyMap.get(10);
        // Then
        assertEquals("ten", value, "Non‑String keys must be handled by the underlying map without case conversion.");
    }

    @Test
    void get_NonExistingStringKey_ReturnsNull() {
        // When
        // not present
        Integer missing = ciMap.get("delta");
        // Then
        assertNull(missing, "A missing key should result in null.");
    }

    @Test
    void get_NullKey_DelegatesToUnderlyingMap() {
        // Given
        // LinkedHashMap permits a single null key
        ciMap.put(null, 99);
        // When
        Integer value = ciMap.get(null);
        // Then
        assertEquals(99, value, "Null key should be delegated to the wrapped map and return the associated value.");
    }

    @Test
    void get_KeyIsNotStringButEqualsStringInMap_ReturnsNull() {
        // Given a non‑String key that equals a stored String key via .equals()
        // (e.g., a custom CharSequence implementation). For simplicity we use StringBuilder.
        StringBuilder sbKey = new StringBuilder("Alpha");
        // The map currently has a String key "Alpha". The get method should treat sbKey as non‑String.
        // Therefore it should not find the entry and return null.
        // When
        Integer result = ciMap.get(sbKey);
        // Then
        assertNull(result, "Non‑String objects, even if equal to a stored String, must not be case‑insensitively matched.");
    }

    @Test
    void get_StringKey_DifferentCase_TrueBranch() {
        ciMap.put("test", 42);
        assertEquals(42, ciMap.get("Test"));
    }

    @Test
    void get_StringKey_NotPresent_TrueBranchReturnsNull() {
        ciMap.put("test", 42);
        // same as above but key not present after case change
        assertNull(ciMap.get("TeSt"));
    }

    @Test
    void get_NullKey_WithNullEntry_TrueBranch() {
        ciMap.put(null, 777);
        assertEquals(777, ciMap.get(null));
    }

    @Test
    void get_IntegerKey_FalseBranch() {
        ciMap.put("one", 1);
        ciMap.put("two", 2);
        // Insert an Integer key directly via reflection to bypass wrapping.
        Map<Object, Integer> raw = new LinkedHashMap<>();
        raw.put(42, 999);
        setInternalMap(ciMap, raw);
        assertEquals(999, ciMap.get(42));
    }

    @Test
    void get_StringKey_Missing_TrueBranchReturnsNull() {
        assertNull(ciMap.get("Missing"));
    }

    @Test
    void get_NonStringObject_NotPresent_FalseBranchReturnsNull() {
        assertNull(ciMap.get(new Object()));
    }

    @Test
    void get_ExactCaseString_ReturnsValue() {
        ciMap.put("test", 123);
        assertEquals(123, ciMap.get("test"));
    }

    @Test
    void get_CaseInsensitiveMatch_ReturnsValue() {
        ciMap.put("test", 123);
        assertEquals(123, ciMap.get("TeSt"));
    }

    @Test
    void get_KeyIsCaseInsensitiveString_FalseBranch() {
        // The map stores a CaseInsensitiveString instance when we use put().
        ciMap.put("test", 555);
        // Retrieve the internal key instance via reflection for direct use.
        CaseInsensitiveString cis = null;
        try {
            Field f = CaseInsensitiveMap.class.getDeclaredField("map");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<Object, Object> internal = (Map<Object, Object>) f.get(ciMap);
            cis = (CaseInsensitiveString) internal.keySet().iterator().next();
        } catch (Exception e) {
            fail("Reflection failed while extracting CaseInsensitiveString key", e);
        }
        assertNotNull(cis, "Internal CaseInsensitiveString key should exist");
        // Because cis is NOT a java.lang.String, the false branch is taken.
        assertEquals(555, ciMap.get(cis));
    }

    @Test
    void get_StringBuilderKey_FalseBranchReturnsNull() {
        ciMap.put("test", 111);
        assertNull(ciMap.get(new StringBuilder("test")));
    }

    @Test
    void get_KeyWithNullValue_ReturnsNullButKeyExists() {
        ciMap.put("nullValueKey", null);
        assertNull(ciMap.get("nullValueKey"));
        // Verify that the key is indeed present.
        assertTrue(ciMap.containsKey("nullValueKey"));
    }

    @Test
    void get_AnyKey_OnEmptyMap_ReturnsNull() {
        CaseInsensitiveMap<String, String> empty = new CaseInsensitiveMap<>();
        assertNull(empty.get("any"));
        assertNull(empty.get(12345));
        assertNull(empty.get(null));
    }

    @Test
    void get_NullKey_OnConcurrentHashMap_ThrowsNPE() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        setInternalMap(map, new ConcurrentHashMap<>());
        assertThrows(NullPointerException.class, () -> map.get(null));
    }

    @Test
    void get_NullKey_OnHashtable_ThrowsNPE() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        setInternalMap(map, new Hashtable<>());
        assertThrows(NullPointerException.class, () -> map.get(null));
    }

    @Test
    void get_StringKey_OnTreeMapWithIntegerKeys_ThrowsClassCastException() {
        // TreeMap with Integer keys – natural ordering expects comparable Integers.
        TreeMap<Integer, String> tree = new TreeMap<>();
        tree.put(1, "one");
        CaseInsensitiveMap<String, String> map = createWithBackingMap(tree, Collections.emptyMap());
        assertThrows(ClassCastException.class, () -> map.get("Test"));
    }

    @Test
    void get_StringBuilderKey_OnTreeMapWithIntegerKeys_ThrowsClassCastException() {
        TreeMap<Integer, String> tree = new TreeMap<>();
        tree.put(1, "one");
        CaseInsensitiveMap<String, String> map = createWithBackingMap(tree, Collections.emptyMap());
        assertThrows(ClassCastException.class, () -> map.get(new StringBuilder("test")));
    }

    @Test
    void get_WithInternalMapNull_ThrowsNullPointerException() throws Exception {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        // Force the private field to null.
        setInternalMap(map, null);
        assertThrows(NullPointerException.class, () -> map.get("any"));
    }
}
