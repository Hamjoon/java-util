package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveMap_copy_Test {

    private TestableCaseInsensitiveMap<String, String> testableMap;

    /**
     * Simple subclass that makes the protected copy method public for testing.
     */
    static class TestableCaseInsensitiveMap<K, V> extends CaseInsensitiveMap<K, V> {

        public Map<K, V> exposeCopy(Map<K, V> source, Map<K, V> dest) {
            return super.copy(source, dest);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        testableMap = new TestableCaseInsensitiveMap<>();
    }

    static Stream<Arguments> sourceDestProvider() {
        return Stream.of(// 1. String keys should be wrapped
        Arguments.of(mapOf("One", 1, "Two", 2), new HashMap<>(), (Runnable) () -> {
            // expectations are checked inside the test lambda
        }, "String keys should be wrapped into CaseInsensitiveString before being put into dest."), // 2. Already CaseInsensitiveString keys must not be double‑wrapped
        Arguments.of(mapOf(newCaseInsensitiveString(new TestableCaseInsensitiveMap<>(), "One"), 1), new HashMap<>(), (Runnable) () -> {
        }, "Keys already of type CaseInsensitiveString should not be double‑wrapped."), // 3. Non‑String keys unchanged
        Arguments.of(mapOf(10, "ten"), new HashMap<>(), (Runnable) () -> {
        }, "Non‑String keys must be copied as‑is."), // 4. Entry is a CaseInsensitiveEntry (source is a CaseInsensitiveMap)
        Arguments.of(new CaseInsensitiveMap<String, Integer>() {

            {
                put("Three", 3);
            }
        }, new HashMap<>(), (Runnable) () -> {
        }, "When the entry is a CaseInsensitiveEntry, the original key must be retrieved via getOriginalKey() before possible wrapping."), // 5. Mixed key types
        Arguments.of(mapOf("Four", 4, newCaseInsensitiveString(new TestableCaseInsensitiveMap<>(), "Five"), 5, 6, "six"), new HashMap<>(), (Runnable) () -> {
        }, "Mixed key types (String, existing CaseInsensitiveString, non‑String) are each handled according to their type."), // 6. Null source map -> NPE
        Arguments.of(null, new HashMap<>(), (Runnable) () -> {
        }, "Passing a null source map should result in NPE as the method iterates over source.entrySet()."), // 7. Empty source map -> dest unchanged
        Arguments.of(new HashMap<>(), new HashMap<>(), (Runnable) () -> {
        }, "Copying from an empty source should leave destination unchanged (still empty)."));
    }

    @Test
    void copyWrapsStringKeyWithCaseInsensitiveString() {
        // GIVEN
        Map<String, String> source = new HashMap<>();
        // normal String key
        source.put("TeStKey", "value1");
        Map<String, String> dest = new HashMap<>();
        // WHEN
        testableMap.exposeCopy(source, dest);
        // THEN
        assertEquals(1, dest.size(), "Destination map should contain exactly one entry");
        // Retrieval should be case‑insensitive because the key stored is a CaseInsensitiveString
        assertEquals("value1", dest.get("testkey"));
        assertEquals("value1", dest.get("TESTKEY"));
        // The actual key object stored is not the original String (it is wrapped), but the map
        // should still report the original key when iterating.
        assertTrue(dest.keySet().iterator().next() instanceof String, "KeySet should expose the original String, not the internal wrapper");
    }

    @Test
    void copyHandlesNonStringKeyUnchanged() {
        // GIVEN
        Map<Object, String> source = new HashMap<>();
        Integer intKey = 42;
        source.put(intKey, "intValue");
        Map<Object, String> dest = new HashMap<>();
        // WHEN
        // Use raw types to avoid generic warnings – the copy method works for any K, V.
        @SuppressWarnings("unchecked")
        Map<Object, String> result = testableMap.exposeCopy((Map) source, dest);
        // THEN
        assertEquals(1, result.size());
        assertTrue(result.containsKey(intKey), "Non‑String key must be copied unchanged");
        assertEquals("intValue", result.get(intKey));
    }

    @Test
    void copyPreservesCaseInsensitiveEntryOriginalKey() {
        // GIVEN
        // Populate a CaseInsensitiveMap so its entrySet returns a CaseInsensitiveEntry
        CaseInsensitiveMap<String, String> sourceMap = new CaseInsensitiveMap<>();
        sourceMap.put("MixedCase", "entryValue");
        Map<String, String> dest = new HashMap<>();
        // WHEN
        testableMap.exposeCopy(sourceMap, dest);
        // THEN
        assertEquals(1, dest.size());
        // Retrieval must be case‑insensitive
        assertEquals("entryValue", dest.get("mixedcase"));
        // The key stored in the destination map should be a CaseInsensitiveString (wrapped)
        // but the exposed keySet should present the original String.
        assertTrue(dest.keySet().iterator().next() instanceof String, "Destination keySet should expose the original String key");
    }

    /**
     * Helper to create an instance of the private inner class CaseInsensitiveString.
     */
    @SuppressWarnings("unchecked")
    private static <K> K newCaseInsensitiveString(TestableCaseInsensitiveMap<?, ?> map, String value) {
        try {
            Class<?> cisClass = Class.forName("com.cedarsoftware.util.CaseInsensitiveMap$CaseInsensitiveString");
            Constructor<?> ctor = cisClass.getDeclaredConstructor(CaseInsensitiveMap.class, String.class);
            ctor.setAccessible(true);
            return (K) ctor.newInstance(map, value);
        } catch (Exception e) {
            throw new RuntimeException("Unable to instantiate CaseInsensitiveString via reflection", e);
        }
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("sourceDestProvider")
    void copyVariousKeyTypes(Object sourceObj, Map<?, ?> dest, Runnable unused, String description) {
        // The test body handles each case via instanceof checks.
        if (sourceObj == null) {
            assertThrows(NullPointerException.class, () -> testableMap.exposeCopy(null, dest), "Null source should cause NullPointerException");
            return;
        }
        @SuppressWarnings("unchecked")
        Map<Object, Object> source = (Map<Object, Object>) sourceObj;
        @SuppressWarnings("unchecked")
        Map<Object, Object> destination = (Map<Object, Object>) dest;
        testableMap.exposeCopy(source, destination);
        // Verify expectations based on the description / input type
        if (source.keySet().stream().allMatch(k -> k instanceof String)) {
            // All keys are strings (or wrappers). Retrieval must be case‑insensitive.
            for (Object key : source.keySet()) {
                String original = (String) (key instanceof String ? key : ((CaseInsensitiveMap.CaseInsensitiveString) key).original);
                assertEquals(source.get(key), destination.get(original.toLowerCase()));
                assertEquals(source.get(key), destination.get(original.toUpperCase()));
            }
        } else if (source.keySet().stream().anyMatch(k -> k instanceof Integer)) {
            // Non‑String key case
            for (Object key : source.keySet()) {
                assertTrue(destination.containsKey(key));
                assertEquals(source.get(key), destination.get(key));
            }
        } else if (source instanceof CaseInsensitiveMap) {
            // Source map is a CaseInsensitiveMap – ensure case‑insensitive retrieval works.
            for (Object key : source.keySet()) {
                String strKey = (String) key;
                assertEquals(source.get(strKey), destination.get(strKey.toLowerCase()));
            }
        } else {
            // Mixed case – just ensure size matches and values are copied.
            assertEquals(source.size(), destination.size());
            source.forEach((k, v) -> assertEquals(v, destination.get(k)));
        }
    }

    @Test
    void copyPreservesInsertionOrderWhenDestIsLinkedHashMap() {
        // GIVEN
        TreeMap<String, Integer> source = new TreeMap<>();
        source.put("a", 1);
        source.put("b", 2);
        Map<String, Integer> dest = new LinkedHashMap<>();
        // WHEN
        testableMap.exposeCopy(source, dest);
        // THEN
        Iterator<Map.Entry<String, Integer>> it = dest.entrySet().iterator();
        Map.Entry<String, Integer> first = it.next();
        Map.Entry<String, Integer> second = it.next();
        assertEquals(1, first.getValue());
        assertEquals(2, second.getValue());
        // Keys are case‑insensitive wrappers, but iteration order must match insertion order.
        assertEquals("a", ((CaseInsensitiveMap.CaseInsensitiveString) first.getKey()).original);
        assertEquals("b", ((CaseInsensitiveMap.CaseInsensitiveString) second.getKey()).original);
    }

    @Test
    void copyHandlesConcurrentMapsAndMixedKeys() {
        // GIVEN
        ConcurrentHashMap<Object, Object> source = new ConcurrentHashMap<>();
        source.put("X", 10);
        source.put(newCaseInsensitiveString(new TestableCaseInsensitiveMap<>(), "Y"), 20);
        source.put(30L, "thirty");
        Map<Object, Object> dest = new ConcurrentSkipListMap<>();
        // WHEN
        testableMap.exposeCopy(source, dest);
        // THEN
        assertEquals(3, dest.size());
        assertEquals(10, dest.get("x"));
        assertEquals(20, dest.get("y"));
        assertEquals("thirty", dest.get(30L));
    }

    @Test
    void copyCopiesNullKeyAsIs() {
        // GIVEN
        Map<Object, Integer> source = new HashMap<>();
        source.put(null, 99);
        Map<Object, Integer> dest = new HashMap<>();
        // WHEN
        testableMap.exposeCopy(source, dest);
        // THEN
        assertEquals(1, dest.size());
        assertTrue(dest.containsKey(null));
        assertEquals(99, dest.get(null));
    }

    @Test
    void copyCopiesNullValueWithStringKeyWrapped() {
        // GIVEN
        Map<String, String> source = new HashMap<>();
        source.put("Nil", null);
        Map<String, String> dest = new HashMap<>();
        // WHEN
        testableMap.exposeCopy(source, dest);
        // THEN
        assertEquals(1, dest.size());
        assertTrue(dest.containsKey("Nil"));
        assertNull(dest.get("Nil"));
        // Retrieval must be case‑insensitive
        assertNull(dest.get("nil"));
    }

    @Test
    void copyWorksWithWeakHashMapSource() {
        // GIVEN
        WeakHashMap<String, Integer> source = new WeakHashMap<>();
        source.put("Weak", 1);
        Map<String, Integer> dest = new HashMap<>();
        // WHEN
        testableMap.exposeCopy(source, dest);
        // THEN
        assertEquals(1, dest.size());
        assertEquals(1, dest.get("weak"));
    }

    @Test
    void copyDuplicateCaseInsensitiveKeys_LastOneWins() {
        // GIVEN
        Map<String, Integer> source = new LinkedHashMap<>();
        source.put("Key", 1);
        // duplicate case‑insensitive key
        source.put("key", 2);
        Map<String, Integer> dest = new HashMap<>();
        // WHEN
        testableMap.exposeCopy(source, dest);
        // THEN
        assertEquals(1, dest.size(), "Only one entry should survive after wrapping");
        assertEquals(2, dest.get("key"));
    }

    @Test
    void copyCaseInsensitiveEntryWithPlainStringKey() {
        // GIVEN
        CaseInsensitiveMap<String, Integer> source = new CaseInsensitiveMap<>();
        // entry will be a CaseInsensitiveEntry with a plain String key
        source.put("Plain", 5);
        Map<String, Integer> dest = new HashMap<>();
        // WHEN
        testableMap.exposeCopy(source, dest);
        // THEN
        assertEquals(1, dest.size());
        assertEquals(5, dest.get("plain"));
    }

    @Test
    void copyCaseInsensitiveEntryWithAlreadyWrappedKey() {
        // GIVEN
        CaseInsensitiveMap<String, Integer> source = new CaseInsensitiveMap<>();
        // Directly insert a CaseInsensitiveString key to force the entry to hold that object
        source.put(newCaseInsensitiveString(new TestableCaseInsensitiveMap<>(), "Dup"), 7);
        Map<String, Integer> dest = new HashMap<>();
        // WHEN
        testableMap.exposeCopy(source, dest);
        // THEN
        assertEquals(1, dest.size());
        assertEquals(7, dest.get("dup"));
    }

    @Test
    void copyFromEmptyConcurrentNavigableMapLeavesDestEmpty() {
        // GIVEN
        ConcurrentNavigableMap<String, Integer> source = new ConcurrentSkipListMap<>();
        Map<String, Integer> dest = new HashMap<>();
        // WHEN
        testableMap.exposeCopy(source, dest);
        // THEN
        assertTrue(dest.isEmpty(), "Destination should remain empty when source is empty");
    }

    @Test
    void copyDoesNotWrapCharSequenceImplementation() {
        // GIVEN
        StringBuilder sb = new StringBuilder("Seq");
        Map<Object, Integer> source = new HashMap<>();
        source.put(sb, 123);
        Map<Object, Integer> dest = new HashMap<>();
        // WHEN
        testableMap.exposeCopy(source, dest);
        // THEN
        assertEquals(1, dest.size());
        assertTrue(dest.containsKey(sb));
        assertEquals(123, dest.get(sb));
    }

    @Test
    void copyNullDestinationThrowsNPE() {
        // GIVEN
        Map<String, Integer> source = new HashMap<>();
        source.put("Alpha", 1);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> testableMap.exposeCopy(source, null), "When the destination map is null the method should throw NullPointerException");
    }

    @Test
    void copyWithMismatchedKeyTypeThrowsClassCastException() {
        // GIVEN: Testable map declared with Integer keys, source provides String keys.
        TestableCaseInsensitiveMap<Integer, String> intKeyMap = new TestableCaseInsensitiveMap<>();
        Map<String, String> source = new HashMap<>();
        source.put("Key", "value");
        Map<Integer, String> dest = new HashMap<>();
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> intKeyMap.exposeCopy((Map) source, (Map) dest), "Mismatched key type should cause ClassCastException");
    }

    @Test
    void copyWithMismatchedKeyTypeLongThrowsClassCastException() {
        // GIVEN: Map declared with Long keys, source provides String keys.
        TestableCaseInsensitiveMap<Long, Integer> longKeyMap = new TestableCaseInsensitiveMap<>();
        Map<String, Integer> source = new HashMap<>();
        source.put("Key", 42);
        Map<Long, Integer> dest = new HashMap<>();
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> longKeyMap.exposeCopy((Map) source, (Map) dest), "Mismatched key type should cause ClassCastException");
    }

    @Test
    void copyToUnmodifiableMapThrowsUnsupportedOperationException() {
        // GIVEN
        Map<String, Integer> source = new HashMap<>();
        source.put("Beta", 2);
        Map<String, Integer> dest = Collections.unmodifiableMap(new HashMap<>());
        // WHEN / THEN
        assertThrows(UnsupportedOperationException.class, () -> testableMap.exposeCopy(source, dest), "Putting into an unmodifiable map should throw UnsupportedOperationException");
    }

    @Test
    void copyToEmptyMapFromCollectionsEmptyMapThrowsUnsupportedOperationException() {
        // GIVEN
        Map<String, Integer> source = new TreeMap<>();
        source.put("Gamma", 3);
        Map<String, Integer> dest = Collections.emptyMap();
        // WHEN / THEN
        assertThrows(UnsupportedOperationException.class, () -> testableMap.exposeCopy(source, dest), "Putting into Collections.emptyMap() should throw UnsupportedOperationException");
    }

    /**
     * Helper to build a map from alternating key/value arguments.
     */
    @SafeVarargs
    private static <K, V> Map<K, V> mapOf(Object... entries) {
        Map<K, V> map = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            @SuppressWarnings("unchecked")
            K key = (K) entries[i];
            @SuppressWarnings("unchecked")
            V value = (V) entries[i + 1];
            map.put(key, value);
        }
        return map;
    }
}
