package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SealableNavigableMap_size_Test {

    private NavigableMap<String, Integer> backingMap;

    private SealableNavigableMap<String, Integer> sealableMap;

    private Supplier<Boolean> sealedSupplier;

    private boolean sealed;

    @BeforeEach
    void setupBeforeEach() {
        // start un‑sealed
        sealed = false;
        sealedSupplier = () -> sealed;
        // a simple mutable backing map (used by the default tests)
        backingMap = new ConcurrentNavigableMapNullSafe<>();
        backingMap.put("one", 1);
        backingMap.put("two", 2);
        backingMap.put("three", 3);
        // null key/value are allowed by the backing map
        backingMap.put(null, null);
        // the class under test
        sealableMap = new SealableNavigableMap<>(backingMap, sealedSupplier);
    }

    @AfterEach
    void teardownAfterEach() {
        // clear references – helps the GC when many tests run
        backingMap = null;
        sealableMap = null;
        sealedSupplier = null;
    }

    /**
     * Parameter source for simple size expectations on a mocked backing map.
     */
    private static Stream<Arguments> sizeMockProvider() {
        return Stream.of(// empty map → size 0
        Arguments.of(Collections.emptyNavigableMap(), 0), // map with three entries → size 3
        Arguments.of(new TreeMap<>(Map.of("a", 1, "b", 2, "c", 3)), 3), // map that pretends to have Integer.MAX_VALUE entries (mocked)
        Arguments.of(mock(NavigableMap.class), Integer.MAX_VALUE));
    }

    @Test
    void sizeReflectsInitialBackingMap() {
        // The map was populated with four entries (including the null entry)
        assertEquals(4, sealableMap.size(), "size() should return the number of entries present in the backing map");
    }

    @Test
    void sizeChangesWhenMutatingWhileUnsealed() {
        assertFalse(sealedSupplier.get(), "Map should start unsealed");
        assertEquals(4, sealableMap.size());
        sealableMap.put("four", 4);
        assertEquals(5, sealableMap.size(), "size() should increase after a successful put while unsealed");
        sealableMap.remove("two");
        assertEquals(4, sealableMap.size(), "size() should decrease after a successful remove while unsealed");
    }

    @Test
    void sizeRemainsAccurateWhenSealed() {
        // Mutate the map first, then seal it
        sealableMap.put("four", 4);
        assertEquals(5, sealableMap.size());
        // now the map is sealed (read‑only)
        sealed = true;
        assertTrue(sealedSupplier.get(), "Map should be sealed");
        // size() is a read‑only operation; it must still report the correct count
        assertEquals(5, sealableMap.size(), "size() must return the current number of entries even when the map is sealed");
        // Attempting a mutation should throw, but size should stay unchanged
        assertThrows(UnsupportedOperationException.class, () -> sealableMap.put("five", 5));
        assertEquals(5, sealableMap.size(), "size() must not change after a failed mutation attempt while sealed");
    }

    @Test
    void sizeReflectsChangesThroughViews() {
        // The keySet view is a live view; adding through the map must be visible via size()
        sealableMap.put("alpha", 10);
        assertTrue(sealableMap.keySet().contains("alpha"));
        assertEquals(5, sealableMap.size());
        // Removing via the entrySet view also affects size()
        sealableMap.entrySet().removeIf(e -> "one".equals(e.getKey()));
        assertEquals(4, sealableMap.size());
    }

    @ParameterizedTest(name = "{index}: size() should return {1}")
    @MethodSource("sizeMockProvider")
    void sizeWithMockedBackingMap(NavigableMap<String, Integer> mockMap, int expectedSize) {
        // For the third case we need to stub the mock's size() method.
        if (mockMap instanceof NavigableMap && Mockito.mockingDetails(mockMap).isMock()) {
            when(mockMap.size()).thenReturn(expectedSize);
        }
        // Build a SealableNavigableMap that wraps the (real or mocked) map.
        SealableNavigableMap<String, Integer> mapUnderTest = new SealableNavigableMap<>(mockMap, () -> false);
        assertEquals(expectedSize, mapUnderTest.size(), "size() should delegate to the underlying navMap");
    }

    @Test
    void sizeThrowsWhenInternalNavMapIsNull() throws Exception {
        // Use the normal instance created in @BeforeEach and then null out the private field.
        Field navMapField = SealableNavigableMap.class.getDeclaredField("navMap");
        navMapField.setAccessible(true);
        navMapField.set(sealableMap, null);
        // The contract states a NullPointerException is expected.
        assertThrows(NullPointerException.class, sealableMap::size, "Calling size() when internal navMap is null should throw NPE");
    }

    @Test
    void sizeAfterSingleInsertionIntoEmptyMap() {
        // Create an empty backing map for this test.
        NavigableMap<String, Integer> emptyMap = new TreeMap<>();
        SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(emptyMap, () -> false);
        assertEquals(0, map.size(), "Initially empty map should report size 0");
        map.put("only", 42);
        assertEquals(1, map.size(), "After inserting a single entry size should be 1");
    }

    @Test
    void sizeUnchangedWhenInsertingDuplicateKey() {
        // Start with a map that already contains one entry.
        NavigableMap<String, Integer> base = new TreeMap<>();
        base.put("dup", 1);
        SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(base, () -> false);
        assertEquals(1, map.size(), "Pre‑condition: map size is 1");
        // Inserting a second entry with the same key should replace the value, not increase size.
        map.put("dup", 2);
        assertEquals(1, map.size(), "Size must remain 1 when a duplicate key is inserted");
        assertEquals(2, map.get("dup"), "Value should be replaced with the new one");
    }

    @Test
    void sizeDecreasesWhenRemovingOnlyEntry() {
        NavigableMap<String, Integer> base = new TreeMap<>();
        base.put("solo", 99);
        SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(base, () -> false);
        assertEquals(1, map.size(), "Map should start with size 1");
        map.remove("solo");
        assertEquals(0, map.size(), "After removing the only entry size should be 0");
    }

    @Test
    void sizeResetsToZeroAfterClear() {
        // Use the default backing map (four entries) then clear it.
        assertEquals(4, sealableMap.size(), "Pre‑condition: map has 4 entries");
        sealableMap.clear();
        assertEquals(0, sealableMap.size(), "clear() must reset size to 0");
    }

    @Test
    void sizeAfterPutAllWithDuplicates() {
        // Start with a map that already contains two keys.
        NavigableMap<String, Integer> base = new TreeMap<>();
        base.put("k1", 1);
        base.put("k2", 2);
        SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(base, () -> false);
        // Map to putAll – contains four keys, two of which already exist.
        Map<String, Integer> toPut = Map.of(// duplicate key
        "k2", // duplicate key
        22, "k3", 3, "k4", 4, "k5", 5);
        map.putAll(toPut);
        // Unique keys after putAll: k1, k2, k3, k4, k5 → total 5
        assertEquals(5, map.size(), "size() must reflect the total number of distinct keys after putAll");
    }
}
