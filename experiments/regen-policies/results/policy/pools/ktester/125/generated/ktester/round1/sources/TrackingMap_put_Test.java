package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TrackingMap_put_Test {

    @Mock
    private Map<String, Integer> mockBackingMap;

    private AutoCloseable mocks;

    /**
     * Simple spy map that records the last key/value passed to put and counts calls.
     */
    private static class SpyMap<K, V> extends HashMap<K, V> {

        K lastPutKey;

        V lastPutValue;

        int putCallCount = 0;

        @Override
        public V put(K key, V value) {
            putCallCount++;
            lastPutKey = key;
            lastPutValue = value;
            return super.put(key, value);
        }
    }

    /**
     * Map implementation that always throws IllegalStateException from put().
     */
    private static class ThrowingMap<K, V> extends HashMap<K, V> {

        @Override
        public V put(K key, V value) {
            throw new IllegalStateException("forced exception");
        }
    }

    /**
     * Mutable key whose hashCode/equals depend on an internal id field.
     */
    private static class MutableKey {

        private String id;

        MutableKey(String id) {
            this.id = id;
        }

        void setId(String newId) {
            this.id = newId;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(id);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof MutableKey))
                return false;
            MutableKey that = (MutableKey) o;
            return Objects.equals(id, that.id);
        }

        @Override
        public String toString() {
            return "MutableKey{" + id + '}';
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations for each test
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Release Mockito resources
        mocks.close();
    }

    private static Stream<Arguments> basicPutProvider() {
        // The sequence mirrors the JSON "Basic put operations" group.
        // 1. Insert "alpha" -> 123 (no previous)
        // 2. Replace "alpha" -> 456 (previous 123)
        // 3. Insert "beta" -> null (no previous)
        // 4. Insert null -> 789 (no previous)
        // 5. Replace null -> null (previous 789)
        return Stream.of(// key, value, expectedPrev, expectedSize, expectedGetAfterPut
        Arguments.of("alpha", 123, null, 1, 123), Arguments.of("alpha", 456, 123, 1, 456), Arguments.of("beta", null, null, 2, null), Arguments.of(null, 789, null, 3, 789), Arguments.of(null, null, 789, 3, null));
    }

    @Test
    @DisplayName("put(K,V) delegates to the wrapped map and returns its result")
    void testPutDelegatesAndReturnsResult() {
        // Arrange
        when(mockBackingMap.put("one", 1)).thenReturn(null);
        // simulate replacement
        when(mockBackingMap.put("two", 2)).thenReturn(1);
        TrackingMap<String, Integer> trackingMap = new TrackingMap<>(mockBackingMap);
        // Act & Assert – first insertion (no previous value)
        Integer result1 = trackingMap.put("one", 1);
        assertNull(result1, "First insertion should return null (no previous mapping)");
        verify(mockBackingMap, times(1)).put("one", 1);
        // Act & Assert – second insertion (replaces previous value)
        Integer result2 = trackingMap.put("two", 2);
        assertEquals(1, result2, "Second insertion should return the previous value (1)");
        verify(mockBackingMap, times(1)).put("two", 2);
    }

    @Test
    @DisplayName("put(K,V) works with null key and null value when the backing map permits it")
    void testPutWithNullKeyAndValue() {
        // Arrange
        when(mockBackingMap.put(null, null)).thenReturn(null);
        when(mockBackingMap.put("key", null)).thenReturn(null);
        when(mockBackingMap.put(null, 5)).thenReturn(null);
        TrackingMap<String, Integer> trackingMap = new TrackingMap<>(mockBackingMap);
        // Act & Assert
        assertNull(trackingMap.put(null, null));
        assertNull(trackingMap.put("key", null));
        assertNull(trackingMap.put(null, 5));
        verify(mockBackingMap).put(null, null);
        verify(mockBackingMap).put("key", null);
        verify(mockBackingMap).put(null, 5);
    }

    @Test
    @DisplayName("constructor throws IllegalArgumentException when supplied a null backing map")
    void testConstructorRejectsNullBackingMap() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new TrackingMap<String, Integer>(null));
        assertEquals("Cannot construct a TrackingMap() with null", ex.getMessage());
    }

    @Test
    @DisplayName("put does not affect the set of accessed keys used by expungeUnused")
    void testPutDoesNotMarkKeyAsAccessed() {
        // Use a real backing map to observe behavior of keysUsed()
        Map<String, String> realMap = new java.util.HashMap<>();
        realMap.put("a", "alpha");
        realMap.put("b", "beta");
        TrackingMap<String, String> trackingMap = new TrackingMap<>(realMap);
        // No get/containsKey yet – keysUsed should be empty
        assertTrue(trackingMap.keysUsed().isEmpty(), "No keys should be marked as used initially");
        // Put a new entry; this should not count as usage
        trackingMap.put("c", "gamma");
        assertTrue(trackingMap.keysUsed().isEmpty(), "Put should not mark the key as used");
        // Access a key via get – now it should be recorded
        trackingMap.get("a");
        assertEquals(1, trackingMap.keysUsed().size());
        assertTrue(trackingMap.keysUsed().contains("a"));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: put({0}, {1}) → expectedPrev={2}")
    @MethodSource("basicPutProvider")
    @DisplayName("Basic put operations")
    void testBasicPutOperations(String key, Integer value, Integer expectedPrev, Integer expectedSize, Integer expectedGet) {
        // Use a real HashMap as the backing store to observe size/get behaviour.
        Map<String, Integer> backing = new HashMap<>();
        TrackingMap<String, Integer> tm = new TrackingMap<>(backing);
        // Pre‑populate with entries required for the test sequence.
        // The provider supplies the cumulative state after each call.
        Integer actualPrev = tm.put(key, value);
        assertEquals(expectedPrev, actualPrev, () -> "put(" + key + "," + value + ") should return previous value");
        assertEquals(expectedSize.intValue(), tm.size(), () -> "size after put(" + key + "," + value + ")");
        assertEquals(expectedGet, tm.get(key), () -> "get(" + key + ") after put");
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("Delegation verification (spy map) – first put")
    void testDelegationFirstPutWithSpyMap() {
        SpyMap<String, Integer> spy = new SpyMap<>();
        TrackingMap<String, Integer> tm = new TrackingMap<>(spy);
        Integer prev = tm.put("gamma", 111);
        assertNull(prev, "First insertion should return null");
        assertEquals(1, spy.putCallCount, "SpyMap should have recorded one put call");
        assertEquals("gamma", spy.lastPutKey);
        assertEquals(111, spy.lastPutValue);
        assertEquals(111, spy.get("gamma"));
    }

    @Test
    @DisplayName("Delegation verification (spy map) – second put on same key")
    void testDelegationSecondPutWithSpyMap() {
        SpyMap<String, Integer> spy = new SpyMap<>();
        TrackingMap<String, Integer> tm = new TrackingMap<>(spy);
        // first insertion
        tm.put("gamma", 111);
        // second insertion (replacement)
        Integer prev = tm.put("gamma", 222);
        assertEquals(111, prev, "Second insertion should return previous value");
        assertEquals(2, spy.putCallCount, "Two put calls should be recorded");
        assertEquals("gamma", spy.lastPutKey);
        assertEquals(222, spy.lastPutValue);
        assertEquals(222, spy.get("gamma"));
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("Stress test – inserting 1000 distinct keys")
    void testLargeNumberOfEntries() {
        Map<String, Integer> backing = new HashMap<>();
        TrackingMap<String, Integer> tm = new TrackingMap<>(backing);
        // Insert 1000 distinct keys
        for (int i = 0; i < 1000; i++) {
            String k = "key" + i;
            Integer v = i;
            assertNull(tm.put(k, v), "First insertion of " + k + " should return null");
        }
        assertEquals(1000, tm.size(), "Size after 1000 insertions");
        // Verify a few random entries
        assertEquals(0, tm.get("key0"));
        assertEquals(999, tm.get("key999"));
        assertEquals(500, tm.get("key500"));
    }

    @Test
    @DisplayName("Replace a value deep inside a large map")
    void testReplaceInLargeMap() {
        Map<String, Integer> backing = new HashMap<>();
        TrackingMap<String, Integer> tm = new TrackingMap<>(backing);
        // Populate 1000 entries
        for (int i = 0; i < 1000; i++) {
            tm.put("key" + i, i);
        }
        // Replace value for key500
        Integer prev = tm.put("key500", 9999);
        assertEquals(500, prev, "Previous value for key500 should be 500");
        assertEquals(9999, tm.get("key500"));
        assertEquals(1000, tm.size(), "Size should remain unchanged after replacement");
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("Putting null key into a TreeMap should propagate NullPointerException")
    void testNullKeyOnTreeMap() {
        TreeMap<String, Integer> treeMap = new TreeMap<>();
        TrackingMap<String, Integer> tm = new TrackingMap<>(treeMap);
        assertThrows(NullPointerException.class, () -> tm.put(null, 42), "TreeMap does not allow null keys, exception must propagate");
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("Putting a String key into a TreeMap<Integer,Integer> should propagate ClassCastException")
    void testClassCastExceptionOnTreeMap() {
        TreeMap<Integer, Integer> intMap = new TreeMap<>();
        // The raw type is required because the generic type does not match the key we will use.
        @SuppressWarnings("unchecked")
        TrackingMap<String, Integer> tm = new TrackingMap((Map) intMap);
        assertThrows(ClassCastException.class, () -> tm.put("stringKey", 123), "TreeMap<Integer,Integer> should throw ClassCastException for a String key");
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("Putting into an unmodifiable map should propagate UnsupportedOperationException")
    void testPutIntoUnmodifiableMap() {
        Map<String, Integer> unmod = Collections.unmodifiableMap(new HashMap<>());
        TrackingMap<String, Integer> tm = new TrackingMap<>(unmod);
        assertThrows(UnsupportedOperationException.class, () -> tm.put("anyKey", 456));
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("Custom map that throws IllegalStateException should let the exception propagate")
    void testCustomMapThrowsRuntimeException() {
        ThrowingMap<String, Integer> throwingMap = new ThrowingMap<>();
        TrackingMap<String, Integer> tm = new TrackingMap<>(throwingMap);
        assertThrows(IllegalStateException.class, () -> tm.put("key", 789));
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("ConcurrentHashMap does not allow null values – NullPointerException should propagate")
    void testNullValueInConcurrentHashMap() {
        ConcurrentHashMap<String, Integer> chm = new ConcurrentHashMap<>();
        TrackingMap<String, Integer> tm = new TrackingMap<>(chm);
        assertThrows(NullPointerException.class, () -> tm.put("concurrentKey", null));
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("Mutable key after mutation becomes unreachable")
    void testMutableKeyBehavior() {
        MutableKey mutableKey = new MutableKey("key1");
        Map<MutableKey, String> backing = new HashMap<>();
        TrackingMap<MutableKey, String> tm = new TrackingMap<>(backing);
        // Insert with original key state
        assertNull(tm.put(mutableKey, "value1"));
        assertEquals("value1", tm.get(mutableKey));
        assertEquals(1, tm.size());
        // Mutate the key – its hashCode changes
        mutableKey.setId("changed");
        // The map can no longer find the entry using the mutated key
        assertNull(tm.get(mutableKey), "After mutation, get should return null");
        // Adding a new entry with the mutated key creates a second entry
        assertNull(tm.put(mutableKey, "value2"));
        assertEquals(2, tm.size(), "Size should be 2 after inserting with mutated key");
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("Putting null value does not remove the key – size stays the same")
    void testPutNullDoesNotRemoveKey() {
        Map<String, Integer> backing = new HashMap<>();
        TrackingMap<String, Integer> tm = new TrackingMap<>(backing);
        // Insert initial entry
        assertNull(tm.put("zeta", 111));
        assertEquals(1, tm.size());
        // Replace with null value – entry still exists (value is null)
        Integer prev = tm.put("zeta", null);
        assertEquals(111, prev);
        assertEquals(1, tm.size(), "Size should remain 1 after putting null value");
        assertNull(tm.get("zeta"), "Value should now be null");
        // Re‑insert non‑null value
        assertNull(tm.put("zeta", 222));
        assertEquals(1, tm.size());
        assertEquals(222, tm.get("zeta"));
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("Putting the same value for the same key returns the previous value")
    void testPutSameValueAgain() {
        TrackingMap<String, Integer> tm = new TrackingMap<>(new HashMap<>());
        // First insertion
        assertNull(tm.put("delta", 555));
        assertEquals(555, tm.get("delta"));
        assertEquals(1, tm.size());
        // Second insertion with identical value
        Integer prev = tm.put("delta", 555);
        assertEquals(555, prev, "Previous value should be returned even if identical");
        assertEquals(555, tm.get("delta"));
        assertEquals(1, tm.size(), "Size must stay unchanged");
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("Different String objects with same logical value are treated as the same key")
    void testKeyEqualityHandling() {
        TrackingMap<String, Integer> tm = new TrackingMap<>(new HashMap<>());
        String keyObj = new String("epsilon");
        assertNull(tm.put(keyObj, 100));
        assertEquals(100, tm.get("epsilon"));
        assertEquals(1, tm.size());
        // Insert using a literal that is equal to the previous key object
        Integer prev = tm.put("epsilon", 200);
        assertEquals(100, prev);
        assertEquals(200, tm.get("epsilon"));
        assertEquals(1, tm.size());
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("First insertion of null value returns null, replacement returns previous null")
    void testReturnPreviousNullValue() {
        TrackingMap<String, Integer> tm = new TrackingMap<>(new HashMap<>());
        // First insertion with null value
        assertNull(tm.put("gamma", null));
        assertNull(tm.get("gamma"));
        assertEquals(1, tm.size());
        // Replace null with a non‑null value
        Integer prev = tm.put("gamma", 777);
        assertNull(prev, "Previous value was null");
        assertEquals(777, tm.get("gamma"));
        assertEquals(1, tm.size());
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("keysUsed set is unchanged after put operations")
    void testKeysUsedUnaffectedByPut() {
        TrackingMap<String, String> tm = new TrackingMap<>(new HashMap<>());
        assertTrue(tm.keysUsed().isEmpty());
        tm.put("a", "A");
        assertTrue(tm.keysUsed().isEmpty(), "Put should not mark key as used");
        tm.get("a");
        assertEquals(Set.of("a"), tm.keysUsed(), "After get, key should be recorded");
    }
}
