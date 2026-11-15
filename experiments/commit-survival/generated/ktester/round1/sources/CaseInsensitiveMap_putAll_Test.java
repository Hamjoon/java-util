package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.ConcurrentModificationException;
import java.util.stream.Stream;

class CaseInsensitiveMap_putAll_Test {

    class UnmodifiableCIMap<K, V> extends CaseInsensitiveMap<K, V> {

        @Override
        public V put(K key, V value) {
            throw new UnsupportedOperationException("put not supported");
        }
    }

    class UnmodifiableCIMap<K, V> extends CaseInsensitiveMap<K, V> {

        @Override
        public V put(K key, V value) {
            throw new UnsupportedOperationException("put not supported");
        }
    }

    /**
     * Empty source map.
     */
    private static Stream<Map<String, Integer>> emptyMapProvider() {
        return Stream.of(Collections.emptyMap());
    }

    /**
     * Regular map with only normal entries (no CaseInsensitiveEntry).
     */
    private static Stream<Map<String, String>> nonCaseInsensitiveEntriesProvider() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("Apple", "Red");
        m.put("Banana", "Yellow");
        m.put("Cherry", "Red");
        return Stream.of(m);
    }

    /**
     * Source map that contains only {@link CaseInsensitiveMap.CaseInsensitiveEntry}
     *  objects – created by using another {@code CaseInsensitiveMap}.
     */
    private static Stream<CaseInsensitiveMap<String, String>> caseInsensitiveEntriesProvider() {
        CaseInsensitiveMap<String, String> src = new CaseInsensitiveMap<>();
        src.put("TeSt", "value");
        src.put("Another", "val2");
        return Stream.of(src);
    }

    /**
     * Map that mixes a normal {@link java.util.Map.Entry} with a
     *  {@link CaseInsensitiveMap.CaseInsensitiveEntry}.
     */
    private static Stream<Map<String, String>> mixedEntriesProvider() {
        // Normal entry (SimpleEntry)
        Map.Entry<String, String> normal = new AbstractMap.SimpleEntry<>("normalKey", "normalVal");
        // CaseInsensitiveEntry obtained from a CI map
        CaseInsensitiveMap<String, String> ciMap = new CaseInsensitiveMap<>();
        ciMap.put("MixedKey", "mixedVal");
        CaseInsensitiveMap.CaseInsensitiveEntry ciEntry = (CaseInsensitiveMap.CaseInsensitiveEntry) ciMap.entrySet().iterator().next();
        // Build a custom map that returns a Set containing both entries
        Map<String, String> mixed = mock(Map.class);
        Set<Map.Entry<String, String>> entrySet = new LinkedHashSet<>();
        entrySet.add(normal);
        entrySet.add(ciEntry);
        when(mixed.entrySet()).thenReturn(entrySet);
        // The map itself is not used for anything else, so other methods can stay default.
        return Stream.of(mixed);
    }

    /**
     * Source map that is {@code null}.
     */
    private static Stream<Map<String, String>> nullMapProvider() {
        return Stream.of((Map<String, String>) null);
    }

    /**
     * Source map that contains a {@code null} key.
     */
    private static Stream<Map<String, String>> nullKeyProvider() {
        Map<String, String> m = new HashMap<>();
        m.put(null, "value");
        return Stream.of(m);
    }

    /**
     * Source map that contains a {@code null} value.
     */
    private static Stream<Map<String, String>> nullValueProvider() {
        Map<String, String> m = new HashMap<>();
        m.put("keyWithNull", null);
        return Stream.of(m);
    }

    /**
     * Source map with duplicate keys differing only by case.
     */
    private static Stream<Map<String, String>> duplicateCaseKeysProvider() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("test", "First");
        // later entry should win
        m.put("TEST", "Second");
        return Stream.of(m);
    }

    /**
     * Source map that will be mutated after {@code putAll} returns.
     */
    private static Stream<CaseInsensitiveMap<String, String>> mutableSourceMapProvider() {
        CaseInsensitiveMap<String, String> src = new CaseInsensitiveMap<>();
        src.put("initial", "value");
        return Stream.of(src);
    }

    /**
     * Source map that contains a key of the wrong type (Integer instead of String).
     */
    private static Stream<Map<Integer, String>> keyTypeMismatchProvider() {
        Map<Integer, String> m = new HashMap<>();
        m.put(123, "StringValue");
        return Stream.of(m);
    }

    /**
     * Source map whose {@code entrySet()} contains a single {@code null} entry.
     */
    private static Stream<Map<String, String>> nullEntryProvider() {
        Map<String, String> mockMap = mock(Map.class);
        Set<Map.Entry<String, String>> entrySet = new HashSet<>();
        // deliberately add a null entry
        entrySet.add(null);
        when(mockMap.entrySet()).thenReturn(entrySet);
        return Stream.of(mockMap);
    }

    @Test
    void putAllCopiesAllEntriesFromRegularMap() {
        // GIVEN: a target map with one entry and a source map with two entries
        CaseInsensitiveMap<String, Integer> target = new CaseInsensitiveMap<>();
        target.put("a", 1);
        Map<String, Integer> source = new HashMap<>();
        source.put("b", 2);
        source.put("c", 3);
        // WHEN: putAll is invoked
        target.putAll(source);
        // THEN: all entries are present and values are correct
        assertEquals(3, target.size(), "Target map should contain three entries");
        assertEquals(1, target.get("a"));
        assertEquals(2, target.get("b"));
        assertEquals(3, target.get("c"));
    }

    @Test
    void putAllHandlesCaseInsensitiveEntryCorrectly() {
        // GIVEN: a source CaseInsensitiveMap (its entrySet yields CaseInsensitiveEntry objects)
        CaseInsensitiveMap<String, String> source = new CaseInsensitiveMap<>();
        source.put("KeyOne", "value1");
        source.put("KEYTWO", "value2");
        // AND: a target map that already contains a key differing only by case
        CaseInsensitiveMap<String, String> target = new CaseInsensitiveMap<>();
        // same logical key as "KeyOne"
        target.put("keyone", "oldValue");
        // WHEN: putAll is called with the source map
        target.putAll(source);
        // THEN: the existing key is overwritten (case‑insensitive) and the new key is added
        assertEquals(2, target.size(), "Target map should contain two logical keys");
        assertEquals("value1", target.get("KeyOne"), "Existing key should be overwritten");
        assertEquals("value2", target.get("keytwo"), "New key should be added (case‑insensitive)");
    }

    @Test
    void putAllWithEmptyMapDoesNotChangeTarget() {
        // GIVEN: a non‑empty target map and an empty source map
        CaseInsensitiveMap<String, Integer> target = new CaseInsensitiveMap<>();
        target.put("x", 10);
        Map<String, Integer> emptySource = new HashMap<>();
        // WHEN: putAll is invoked with the empty map
        target.putAll(emptySource);
        // THEN: target map remains unchanged
        assertEquals(1, target.size(), "Target map size should stay the same");
        assertEquals(10, target.get("x"));
    }

    @ParameterizedTest
    @MethodSource("emptyMapProvider")
    void putAll_emptyMap(Map<String, Integer> source) {
        // Target starts empty
        CaseInsensitiveMap<String, Integer> target = new CaseInsensitiveMap<>();
        // Should be a no‑op and not throw
        assertDoesNotThrow(() -> target.putAll(source));
        assertEquals(0, target.size(), "Target should remain empty");
    }

    @ParameterizedTest
    @MethodSource("nonCaseInsensitiveEntriesProvider")
    void putAll_nonCaseInsensitiveEntries(Map<String, String> source) {
        CaseInsensitiveMap<String, String> target = new CaseInsensitiveMap<>();
        target.putAll(source);
        assertEquals(source.size(), target.size());
        source.forEach((k, v) -> assertEquals(v, target.get(k)));
    }

    @ParameterizedTest
    @MethodSource("caseInsensitiveEntriesProvider")
    void putAll_caseInsensitiveEntries(CaseInsensitiveMap<String, String> source) {
        CaseInsensitiveMap<String, String> target = new CaseInsensitiveMap<>();
        target.putAll(source);
        // Keys must be stored with their original case
        source.forEach((k, v) -> assertEquals(v, target.get(k)));
        assertEquals(source.size(), target.size());
    }

    @ParameterizedTest
    @MethodSource("mixedEntriesProvider")
    void putAll_mixedEntries(Map<String, String> source) {
        CaseInsensitiveMap<String, String> target = new CaseInsensitiveMap<>();
        target.putAll(source);
        // The map contains two entries: one from the normal entry, one from the CI entry
        assertEquals(2, target.size());
        assertEquals("normalVal", target.get("normalKey"));
        assertEquals("mixedVal", target.get("MixedKey"));
    }

    @ParameterizedTest
    @MethodSource("nullMapProvider")
    void putAll_nullMap(Map<String, String> source) {
        CaseInsensitiveMap<String, String> target = new CaseInsensitiveMap<>();
        // According to the implementation, MapUtilities.isEmpty(null) likely throws NPE.
        // The test asserts the observed behaviour.
        assertThrows(NullPointerException.class, () -> target.putAll(source));
    }

    @Test
    void putAll_overwriteExisting() {
        CaseInsensitiveMap<String, String> target = new CaseInsensitiveMap<>();
        target.put("apple", "Green");
        Map<String, String> source = new HashMap<>();
        // same logical key, different case
        source.put("Apple", "Red");
        target.putAll(source);
        assertEquals(1, target.size());
        assertEquals("Red", target.get("apple"));
    }

    @ParameterizedTest
    @MethodSource("nullKeyProvider")
    void putAll_nullKey(Map<String, String> source) {
        CaseInsensitiveMap<String, String> target = new CaseInsensitiveMap<>();
        // The map does not accept null keys – expect NPE
        assertThrows(NullPointerException.class, () -> target.putAll(source));
    }

    @ParameterizedTest
    @MethodSource("nullValueProvider")
    void putAll_nullValue(Map<String, String> source) {
        CaseInsensitiveMap<String, String> target = new CaseInsensitiveMap<>();
        target.putAll(source);
        assertEquals(1, target.size());
        assertTrue(target.containsKey("keyWithNull"));
        assertNull(target.get("keyWithNull"));
    }

    @ParameterizedTest
    @MethodSource("duplicateCaseKeysProvider")
    void putAll_duplicateCaseInsensitiveKeys(Map<String, String> source) {
        CaseInsensitiveMap<String, String> target = new CaseInsensitiveMap<>();
        target.putAll(source);
        assertEquals(1, target.size());
        // The later entry ("TEST") should have overwritten the first.
        assertEquals("Second", target.get("test"));
    }

    @ParameterizedTest
    @MethodSource("mutableSourceMapProvider")
    void putAll_sourceMapModifiedAfterCall(CaseInsensitiveMap<String, String> source) {
        CaseInsensitiveMap<String, String> target = new CaseInsensitiveMap<>();
        target.putAll(source);
        // Mutate source after putAll
        source.put("newKey", "newValue");
        // Target must not see the new entry
        assertFalse(target.containsKey("newKey"));
        assertEquals(1, target.size());
    }

    @Test
    void putAll_entryWithCaseInsensitiveStringKeyButNotCaseInsensitiveEntry() throws Exception {
        // Build a key that is an instance of the private inner CaseInsensitiveString.
        Class<?> ciStrClass = Class.forName("com.cedarsoftware.util.CaseInsensitiveMap$CaseInsensitiveString");
        Constructor<?> ctor = ciStrClass.getDeclaredConstructor(String.class);
        ctor.setAccessible(true);
        Object ciKey = ctor.newInstance("MiXeD");
        // Create a regular Map.Entry whose key is that CI string.
        Map.Entry<Object, String> entry = new AbstractMap.SimpleEntry<>(ciKey, "value");
        // Stub a Map to return a Set containing this entry.
        @SuppressWarnings("unchecked")
        Map<Object, String> source = (Map<Object, String>) mock(Map.class);
        Set<Map.Entry<Object, String>> entrySet = new LinkedHashSet<>();
        entrySet.add(entry);
        when(source.entrySet()).thenReturn(entrySet);
        // Target map expects String keys.
        CaseInsensitiveMap<String, String> target = new CaseInsensitiveMap<>();
        // putAll should treat the entry as a normal entry; the internal put()
        // will recognise the CI string and store the original case.
        target.putAll(source);
        // Verify that the key stored is the original string "MiXeD".
        assertEquals(1, target.size());
        assertEquals("value", target.get("MiXeD"));
    }

    @Test
    void putAll_unsupportedOperation() {
        // Subclass that throws on every put()
        class UnmodifiableCIMap<K, V> extends CaseInsensitiveMap<K, V> {

            @Override
            public V put(K key, V value) {
                throw new UnsupportedOperationException("put not supported");
            }
        }
        UnmodifiableCIMap<String, String> target = new UnmodifiableCIMap<>();
        Map<String, String> source = new HashMap<>();
        source.put("Key1", "Value1");
        // The exception from put() should propagate out of putAll().
        assertThrows(UnsupportedOperationException.class, () -> target.putAll(source));
    }

    @Test
    void putAll_concurrentModification() {
        // Custom map whose iterator modifies the map during iteration.
        Map<String, String> source = new AbstractMap<>() {

            private final Map<String, String> delegate = new LinkedHashMap<>();

            {
                delegate.put("a", "1");
                delegate.put("b", "2");
            }

            @Override
            public Set<Entry<String, String>> entrySet() {
                return new AbstractSet<>() {

                    @Override
                    public Iterator<Entry<String, String>> iterator() {
                        Iterator<Entry<String, String>> it = delegate.entrySet().iterator();
                        return new Iterator<>() {

                            private boolean first = true;

                            @Override
                            public boolean hasNext() {
                                return it.hasNext();
                            }

                            @Override
                            public Entry<String, String> next() {
                                if (first) {
                                    // Modify the underlying map on first iteration
                                    delegate.put("c", "3");
                                    first = false;
                                }
                                return it.next();
                            }
                        };
                    }

                    @Override
                    public int size() {
                        return delegate.size();
                    }
                };
            }

            // Minimal Map contract for this test
            @Override
            public int size() {
                return delegate.size();
            }

            @Override
            public boolean isEmpty() {
                return delegate.isEmpty();
            }

            @Override
            public boolean containsKey(Object key) {
                return delegate.containsKey(key);
            }

            @Override
            public boolean containsValue(Object value) {
                return delegate.containsValue(value);
            }

            @Override
            public String get(Object key) {
                return delegate.get(key);
            }

            @Override
            public String put(String key, String value) {
                return delegate.put(key, value);
            }

            @Override
            public String remove(Object key) {
                return delegate.remove(key);
            }

            @Override
            public void putAll(Map<? extends String, ? extends String> m) {
                delegate.putAll(m);
            }

            @Override
            public void clear() {
                delegate.clear();
            }

            @Override
            public Set<String> keySet() {
                return delegate.keySet();
            }

            @Override
            public Collection<String> values() {
                return delegate.values();
            }
        };
        CaseInsensitiveMap<String, String> target = new CaseInsensitiveMap<>();
        assertThrows(ConcurrentModificationException.class, () -> target.putAll(source));
    }

    @ParameterizedTest
    @MethodSource("keyTypeMismatchProvider")
    void putAll_keyTypeMismatch(Map<Integer, String> source) {
        CaseInsensitiveMap<String, String> target = new CaseInsensitiveMap<>();
        // The internal put() will attempt to cast Integer to String → ClassCastException
        assertThrows(ClassCastException.class, () -> target.putAll(source));
    }

    @ParameterizedTest
    @MethodSource("nullEntryProvider")
    void putAll_nullEntryInSourceMap(Map<String, String> source) {
        CaseInsensitiveMap<String, String> target = new CaseInsensitiveMap<>();
        // Dereferencing a null entry should cause NPE
        assertThrows(NullPointerException.class, () -> target.putAll(source));
    }
}
