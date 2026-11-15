package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TrackingMap_putAll_Test {

    private TrackingMap<String, Integer> trackingMap;

    private Map<String, Integer> backingMap;

    /**
     * Simple Map implementation that records the argument passed to {@code putAll}.
     */
    private static class RecordingMap<K, V> extends HashMap<K, V> {

        private Map<? extends K, ? extends V> recordedArgument = null;

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            recordedArgument = m;
            super.putAll(m);
        }

        Map<? extends K, ? extends V> getRecordedArgument() {
            return recordedArgument;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a plain HashMap as the backing map for deterministic behaviour
        backingMap = new HashMap<>();
        trackingMap = new TrackingMap<>(backingMap);
        // Use a plain HashMap as the default backing map – deterministic and mutable.
        backingMap = new HashMap<>();
        trackingMap = new TrackingMap<>(backingMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – not strictly required but keeps the test tidy
        trackingMap = null;
        backingMap = null;
        trackingMap = null;
        backingMap = null;
    }

    /**
     * Data provider for the basic & empty map tests.
     */
    static Stream<Arguments> basicPutAllProvider() {
        Map<String, Integer> nonEmpty = new HashMap<>();
        nonEmpty.put("a", 1);
        nonEmpty.put("b", 2);
        Map<String, Integer> empty = Collections.emptyMap();
        return Stream.of(Arguments.of(nonEmpty, Map.of("a", 1, "b", 2), "non‑empty source"), Arguments.of(empty, Collections.emptyMap(), "empty source"));
    }

    @Test
    void putAllAddsAllEntries() {
        Map<String, Integer> source = new HashMap<>();
        source.put("one", 1);
        source.put("two", 2);
        source.put("three", 3);
        trackingMap.putAll(source);
        // Verify that the entries are now present in the TrackingMap
        assertEquals(3, trackingMap.size(), "TrackingMap should contain three entries after putAll");
        assertEquals(1, trackingMap.get("one"));
        assertEquals(2, trackingMap.get("two"));
        assertEquals(3, trackingMap.get("three"));
    }

    @Test
    void putAllDoesNotMarkKeysAsUsed() {
        Map<String, Integer> source = new HashMap<>();
        source.put("alpha", 10);
        source.put("beta", 20);
        trackingMap.putAll(source);
        // No read operation has been performed, so keysUsed should be empty
        assertTrue(trackingMap.keysUsed().isEmpty(), "keysUsed should remain empty after putAll because no get/containsKey has been called");
    }

    @Test
    void putAllWithNullMapThrowsNPE() {
        // According to Map contract, internalMap.putAll(null) throws NullPointerException
        assertThrows(NullPointerException.class, () -> trackingMap.putAll(null));
    }

    @Test
    void setWrappedMapClearsOldEntriesAndAddsNewOnes() {
        // Populate the original map with some entries
        trackingMap.put("oldKey1", 100);
        trackingMap.put("oldKey2", 200);
        assertEquals(2, trackingMap.size());
        // New map to replace the backing map
        Map<String, Integer> newMap = new HashMap<>();
        newMap.put("newKeyA", 1);
        newMap.put("newKeyB", 2);
        newMap.put("newKeyC", 3);
        // Replace the backing map
        trackingMap.setWrappedMap(newMap);
        // The old entries must be gone
        assertNull(trackingMap.get("oldKey1"));
        assertNull(trackingMap.get("oldKey2"));
        assertEquals(0, trackingMap.keysUsed().size(), "keysUsed should be cleared after setWrappedMap");
        // The new entries must be present
        assertEquals(3, trackingMap.size());
        assertEquals(1, trackingMap.get("newKeyA"));
        assertEquals(2, trackingMap.get("newKeyB"));
        assertEquals(3, trackingMap.get("newKeyC"));
    }

    /* -------------------------------------------------------------
     *  1. Basic putAll functionality (non‑empty & empty source)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "putAll with {2}")
    @MethodSource("basicPutAllProvider")
    void putAllCopiesAllEntries(Map<String, Integer> source, Map<String, Integer> expected, String description) {
        trackingMap.putAll(source);
        assertEquals(expected.size(), trackingMap.size(), "Size after putAll should match expected size");
        expected.forEach((k, v) -> assertEquals(v, trackingMap.get(k), "Value for key '" + k + "' must be copied"));
    }

    /* -------------------------------------------------------------
     *  2. Overwrite existing keys
     * ------------------------------------------------------------- */
    @Test
    void putAllOverwritesExistingKeys() {
        // initial content
        Map<String, Integer> initial = Map.of("x", 10, "y", 20);
        trackingMap.putAll(initial);
        assertEquals(2, trackingMap.size());
        // source that overwrites "y" and adds "z"
        Map<String, Integer> source = Map.of("y", 99, "z", 30);
        trackingMap.putAll(source);
        Map<String, Integer> expected = Map.of("x", 10, "y", 99, "z", 30);
        assertEquals(expected, trackingMap.entrySet().stream().collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap::putAll));
    }

    /* -------------------------------------------------------------
     *  4. Null keys and values handling (HashMap permits both)
     * ------------------------------------------------------------- */
    @Test
    void putAllAcceptsNullKeyAndNullValue() {
        Map<String, Integer> source = new HashMap<>();
        source.put(null, 42);
        source.put("k", null);
        trackingMap.putAll(source);
        assertTrue(trackingMap.containsKey(null));
        assertEquals(42, trackingMap.get(null));
        assertTrue(trackingMap.containsKey("k"));
        assertNull(trackingMap.get("k"));
    }

    /* -------------------------------------------------------------
     *  5. Source map mutation after putAll
     * ------------------------------------------------------------- */
    @Test
    void putAllIsDefensiveCopyOfSource() {
        Map<String, Integer> source = new HashMap<>();
        source.put("p", 5);
        trackingMap.putAll(source);
        // mutate source after the call
        source.put("p", 99);
        source.put("new", 1);
        // TrackingMap must retain the values as they were at call time
        assertEquals(5, trackingMap.get("p"));
        assertFalse(trackingMap.containsKey("new"));
    }

    /* -------------------------------------------------------------
     *  6. Self putAll behavior (no‑op)
     * ------------------------------------------------------------- */
    @Test
    void putAllWithSelfDoesNotChangeMap() {
        trackingMap.put("alpha", 1);
        trackingMap.put("beta", 2);
        Map<String, Integer> snapshot = new HashMap<>(trackingMap);
        // The call should not throw and must leave the map unchanged
        trackingMap.putAll(trackingMap);
        assertEquals(snapshot, trackingMap);
    }

    /* -------------------------------------------------------------
     *  7. Unmodifiable source map propagates UnsupportedOperationException
     * ------------------------------------------------------------- */
    @Test
    void putAllFromUnmodifiableSourcePropagatesUOE() {
        Map<String, Integer> source = Collections.unmodifiableMap(Map.of("x", 10));
        // HashMap's putAll will attempt to iterate; the unmodifiable map throws on modification,
        // not on iteration, so we need a source that throws on putAll – we simulate by using
        // a custom map that throws.
        Map<String, Integer> throwingMap = new AbstractMap<>() {

            @Override
            public Set<Entry<String, Integer>> entrySet() {
                return source.entrySet();
            }

            @Override
            public void putAll(Map<? extends String, ? extends Integer> m) {
                throw new UnsupportedOperationException("source is unmodifiable");
            }
        };
        assertThrows(UnsupportedOperationException.class, () -> trackingMap.putAll(throwingMap));
    }

    /* -------------------------------------------------------------
     *  8. Preserve insertion order with LinkedHashMap
     * ------------------------------------------------------------- */
    @Test
    void putAllPreservesInsertionOrderWhenUsingLinkedHashMap() {
        // Use a LinkedHashMap as the backing map to retain order
        Map<String, Integer> linkedBacking = new LinkedHashMap<>();
        trackingMap = new TrackingMap<>(linkedBacking);
        Map<String, Integer> source = new LinkedHashMap<>() {

            {
                put("first", 1);
                put("second", 2);
                put("third", 3);
            }
        };
        trackingMap.putAll(source);
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(trackingMap.entrySet());
        assertEquals(List.of("first", "second", "third"), entries.stream().map(Map.Entry::getKey).toList(), "Insertion order must be preserved");
    }

    /* -------------------------------------------------------------
     * 10. Large map boundary (10 000 entries)
     * ------------------------------------------------------------- */
    @Test
    void putAllHandlesLargeMapEfficiently() {
        Map<Integer, Integer> largeSource = new HashMap<>();
        for (int i = 0; i < 10_000; i++) {
            largeSource.put(i, i);
        }
        // TrackingMap with Integer keys/values
        TrackingMap<Integer, Integer> intMap = new TrackingMap<>(new HashMap<>());
        //noinspection unchecked
        // raw cast to satisfy generic signature for test
        intMap.putAll((Map) largeSource);
        assertEquals(10_000, intMap.size());
        assertEquals(5_000, intMap.get(5_000));
    }

    /* -------------------------------------------------------------
     * 11. Source map with self‑reference entry
     * ------------------------------------------------------------- */
    @Test
    void putAllAcceptsSelfReferenceEntry() {
        Map<Object, Object> source = new HashMap<>();
        // key and value reference the TrackingMap itself
        source.put(trackingMap, trackingMap);
        // The generic type of TrackingMap is <String,Integer>, but we bypass via raw types.
        @SuppressWarnings("unchecked")
        Map rawSource = source;
        //noinspection unchecked
        trackingMap.putAll(rawSource);
        assertTrue(trackingMap.containsKey(trackingMap));
        assertEquals(trackingMap, trackingMap.get(trackingMap));
    }

    /* -------------------------------------------------------------
     * 12. Delegation to custom wrapped map (recording putAll)
     * ------------------------------------------------------------- */
    @Test
    void putAllDelegatesToCustomWrappedMap() {
        RecordingMap<String, Integer> custom = new RecordingMap<>();
        trackingMap.setWrappedMap(custom);
        Map<String, Integer> source = Map.of("c1", 100);
        trackingMap.putAll(source);
        assertNotNull(custom.getRecordedArgument(), "Custom map should have recorded the putAll call");
        assertEquals(source, custom.getRecordedArgument(), "The argument passed to the custom map must equal the source map");
    }

    /* -------------------------------------------------------------
     * 13. Key type mismatch causes ClassCastException
     * ------------------------------------------------------------- */
    @Test
    void putAllWithMismatchedKeyTypeThrowsClassCastException() {
        // raw map with an Integer key
        Map raw = new HashMap<>();
        raw.put(123, "value");
        //noinspection unchecked
        assertThrows(ClassCastException.class, () -> trackingMap.putAll(raw), "Putting a map with a non‑String key into TrackingMap<String,?> must fail");
    }

    /* -------------------------------------------------------------
     * 14. Value type mismatch causes ClassCastException
     * ------------------------------------------------------------- */
    @Test
    void putAllWithMismatchedValueTypeThrowsClassCastException() {
        // raw map with a String value where Integer is expected
        Map raw = new HashMap<>();
        raw.put("key", "stringValue");
        //noinspection unchecked
        assertThrows(ClassCastException.class, () -> trackingMap.putAll(raw), "Putting a map with a non‑Integer value into TrackingMap<?,Integer> must fail");
    }

    /* -------------------------------------------------------------
     * 15. Unmodifiable internal map propagates UnsupportedOperationException
     * ------------------------------------------------------------- */
    @Test
    void putAllOnUnmodifiableInternalMapPropagatesUOE() {
        Map<String, Integer> unmodifiable = Collections.unmodifiableMap(new HashMap<>());
        trackingMap.setWrappedMap(unmodifiable);
        Map<String, Integer> source = Map.of("k", 1);
        assertThrows(UnsupportedOperationException.class, () -> trackingMap.putAll(source), "When internal map is unmodifiable, putAll must propagate the exception");
    }

    /* -------------------------------------------------------------
     * 16. Null key not allowed in TreeMap throws NullPointerException
     * ------------------------------------------------------------- */
    @Test
    void putAllWithNullKeyIntoTreeMapThrowsNPE() {
        // TreeMap does not allow null keys
        trackingMap.setWrappedMap(new TreeMap<>());
        Map<String, Integer> source = new HashMap<>();
        source.put(null, 42);
        assertThrows(NullPointerException.class, () -> trackingMap.putAll(source), "TreeMap should reject null keys with NPE");
    }

    /* -------------------------------------------------------------
     * 17. Null value not allowed in TreeMap throws NullPointerException
     * ------------------------------------------------------------- */
    @Test
    void putAllWithNullValueIntoTreeMapThrowsNPE() {
        // TreeMap also rejects null values for natural ordering
        trackingMap.setWrappedMap(new TreeMap<>());
        Map<String, Integer> source = new HashMap<>();
        source.put("validKey", null);
        assertThrows(NullPointerException.class, () -> trackingMap.putAll(source), "TreeMap should reject null values with NPE");
    }
}
