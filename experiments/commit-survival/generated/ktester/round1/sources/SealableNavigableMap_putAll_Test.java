package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableNavigableMap_putAll_Test {

    private SealableNavigableMap<String, Integer> sealableMap;

    private Supplier<Boolean> sealedSupplier;

    private boolean sealed;

    /**
     * Map used as the source for putAll calls
     */
    private Map<String, Integer> sourceMap;

    @BeforeEach
    void setupBeforeEach() {
        // start each test un‑sealed
        sealed = false;
        sealedSupplier = () -> sealed;
        // underlying mutable map – the concrete implementation used by SealableNavigableMap
        NavigableMap<String, Integer> backing = new ConcurrentNavigableMapNullSafe<>();
        // pre‑populate with a few entries to verify that putAll merges correctly
        backing.put("alpha", 1);
        backing.put("beta", 2);
        sealableMap = new SealableNavigableMap<>(backing, sealedSupplier);
        // default source map – individual tests may replace its contents
        sourceMap = new HashMap<>();
        sourceMap.put("gamma", 3);
        sourceMap.put("delta", 4);
    }

    private static Stream<Arguments> provideMapsForUnsealedPutAll() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("one", 1);
        // both new keys
        map1.put("two", 2);
        // empty map
        Map<String, Integer> map2 = Collections.emptyMap();
        Map<String, Integer> map3 = new HashMap<>();
        // overwrites existing key
        map3.put("alpha", 99);
        return Stream.of(// original 2 + 2 new = 4
        Arguments.of(map1, 4), // unchanged
        Arguments.of(map2, 2), // size stays 2 because key is overwritten
        Arguments.of(map3, 2));
    }

    @Test
    void putAllAddsAllEntriesWhenUnsealed() {
        assertFalse(sealedSupplier.get(), "Map should start unsealed");
        // should be 2 (alpha, beta)
        int originalSize = sealableMap.size();
        sealableMap.putAll(sourceMap);
        // size must increase by the number of new keys (2)
        assertEquals(originalSize + sourceMap.size(), sealableMap.size(), "Size after putAll should reflect added entries");
        // each entry from sourceMap must be present
        sourceMap.forEach((k, v) -> assertEquals(v, sealableMap.get(k), "Entry for key '" + k + "' should have been added"));
    }

    @Test
    void putAllOverwritesExistingKeyWhenUnsealed() {
        // sourceMap contains a key that already exists in the backing map
        // "alpha" was 1 originally
        sourceMap.put("alpha", 99);
        sealableMap.putAll(sourceMap);
        // "alpha" should now hold the new value
        assertEquals(99, sealableMap.get("alpha"));
        // other entries are still present
        assertEquals(2, sealableMap.get("beta"));
        assertEquals(3, sealableMap.get("gamma"));
        assertEquals(4, sealableMap.get("delta"));
    }

    @Test
    void putAllThrowsWhenSealed() {
        // seal the map
        sealed = true;
        assertTrue(sealedSupplier.get(), "Map should be sealed");
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> sealableMap.putAll(sourceMap), "putAll must throw when the map is sealed");
        assertNotNull(ex.getMessage());
    }

    @Test
    void putAllDoesNotModifyMapWhenSealed() {
        sealed = true;
        int sizeBefore = sealableMap.size();
        try {
            sealableMap.putAll(sourceMap);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
            // expected
        }
        // map must remain unchanged
        assertEquals(sizeBefore, sealableMap.size(), "Size must be unchanged after failed putAll");
        sourceMap.keySet().forEach(k -> assertNull(sealableMap.get(k), "No new key should have been added"));
    }

    @Test
    void putAllAcceptsNullKeyAndNullValueWhenUnsealed() {
        sourceMap.clear();
        sourceMap.put(null, null);
        sourceMap.put("epsilon", null);
        // overwrite null key with a non‑null value
        sourceMap.put(null, 5);
        sealableMap.putAll(sourceMap);
        assertEquals(5, sealableMap.get(null), "Null key should map to the last supplied value");
        assertNull(sealableMap.get("epsilon"), "Null value should be stored correctly");
    }

    @Test
    void putAllWithEmptyMapIsNoOp() {
        // empty map
        sourceMap.clear();
        int sizeBefore = sealableMap.size();
        sealableMap.putAll(sourceMap);
        assertEquals(sizeBefore, sealableMap.size(), "Putting an empty map must not change the size");
    }

    @Test
    void putAllWithNullArgumentThrowsNPE() {
        assertThrows(NullPointerException.class, () -> sealableMap.putAll(null), "putAll should throw NullPointerException when supplied map is null");
    }

    @Test
    void putAllWithNullKeyOnTreeMapPropagatesException() {
        // TreeMap does not allow null keys – we expect a NullPointerException from the underlying map
        NavigableMap<Integer, String> treeBacking = new TreeMap<>();
        treeBacking.put(1, "one");
        SealableNavigableMap<Integer, String> intStringMap = new SealableNavigableMap<>(treeBacking, sealedSupplier);
        Map<Integer, String> badMap = new HashMap<>();
        badMap.put(null, "nullKey");
        assertThrows(NullPointerException.class, () -> intStringMap.putAll(badMap), "Underlying TreeMap should reject null keys");
    }

    @Test
    void putAllAcceptsNullValueOnTreeMap() {
        // TreeMap permits null values
        NavigableMap<Integer, String> treeBacking = new TreeMap<>();
        treeBacking.put(1, "one");
        SealableNavigableMap<Integer, String> intStringMap = new SealableNavigableMap<>(treeBacking, sealedSupplier);
        Map<Integer, String> withNullValue = new HashMap<>();
        withNullValue.put(2, null);
        intStringMap.putAll(withNullValue);
        assertNull(intStringMap.get(2), "Null value should be stored correctly");
        assertEquals(2, intStringMap.size(), "Size must reflect the newly added entry");
    }

    @Test
    void putAllLargeNumberOfEntries() {
        Map<Integer, String> bigMap = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            bigMap.put(i, "v" + i);
        }
        // Use a fresh SealableNavigableMap with an empty backing map
        NavigableMap<Integer, String> backing = new ConcurrentNavigableMapNullSafe<>();
        SealableNavigableMap<Integer, String> largeMap = new SealableNavigableMap<>(backing, sealedSupplier);
        largeMap.putAll(bigMap);
        assertEquals(1000, largeMap.size(), "Map size should be 1000 after bulk insertion");
        // Spot‑check a few entries
        assertEquals("v0", largeMap.get(0));
        assertEquals("v500", largeMap.get(500));
        assertEquals("v999", largeMap.get(999));
    }

    @Test
    void putAllPreservesNavigableOrderWithTreeMapBacking() {
        NavigableMap<Integer, String> treeBacking = new TreeMap<>();
        treeBacking.put(10, "ten");
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(treeBacking, sealedSupplier);
        Map<Integer, String> toAdd = new HashMap<>();
        toAdd.put(5, "five");
        toAdd.put(20, "twenty");
        map.putAll(toAdd);
        // Verify natural ordering is respected
        assertEquals(5, map.firstKey(), "firstKey should be the smallest key after putAll");
        assertEquals(20, map.lastKey(), "lastKey should be the largest key after putAll");
        assertEquals("five", map.get(5));
        assertEquals("ten", map.get(10));
        assertEquals("twenty", map.get(20));
    }

    @Test
    void putAllWithUnderlyingUnmodifiableMapThrowsUnsupportedOperationException() {
        // Wrap a mutable TreeMap with Collections.unmodifiableNavigableMap
        NavigableMap<Integer, String> mutable = new TreeMap<>();
        mutable.put(1, "one");
        NavigableMap<Integer, String> unmodifiable = Collections.unmodifiableNavigableMap(mutable);
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(unmodifiable, sealedSupplier);
        Map<Integer, String> toAdd = new HashMap<>();
        toAdd.put(2, "two");
        assertThrows(UnsupportedOperationException.class, () -> map.putAll(toAdd), "putAll must propagate UnsupportedOperationException from an unmodifiable backing map");
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    void putAllKeyTypeMismatchCausesClassCastException() {
        // Backing map expects Integer keys
        NavigableMap<Integer, String> backing = new TreeMap<>();
        backing.put(1, "one");
        SealableNavigableMap map = new SealableNavigableMap(backing, sealedSupplier);
        // Supplying a map with String keys – runtime should fail when TreeMap tries to compare
        Map<String, String> badKeys = new HashMap<>();
        badKeys.put("key", "value");
        assertThrows(ClassCastException.class, () -> map.putAll(badKeys), "Mismatched key types should result in ClassCastException");
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    void putAllValueTypeMismatchCausesClassCastException() {
        // Backing map expects String values
        NavigableMap<Integer, String> backing = new TreeMap<>();
        backing.put(1, "one");
        SealableNavigableMap map = new SealableNavigableMap(backing, sealedSupplier);
        // Supplying Integer values – TreeMap will accept the key but the put operation will fail on cast
        Map<Integer, Integer> badValues = new HashMap<>();
        badValues.put(2, 2);
        assertThrows(ClassCastException.class, () -> map.putAll(badValues), "Mismatched value types should result in ClassCastException");
    }

    @ParameterizedTest(name = "{index} => mapSize={0}, expectedSize={1}")
    @MethodSource("provideMapsForUnsealedPutAll")
    void putAllParameterized_UnsealedScenarios(Map<String, Integer> inputMap, int expectedSizeAfterPutAll) {
        int sizeBefore = sealableMap.size();
        sealableMap.putAll(inputMap);
        assertEquals(expectedSizeAfterPutAll, sealableMap.size(), "Size after putAll does not match expectation");
        // verify that all supplied entries are present
        inputMap.forEach((k, v) -> assertEquals(v, sealableMap.get(k)));
    }
}
