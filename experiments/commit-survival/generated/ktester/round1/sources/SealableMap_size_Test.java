package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class SealableMap_size_Test {

    private SealableMap<String, Integer> map;

    private volatile boolean sealedState = false;

    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    @BeforeEach
    void setupBeforeEach() {
        // Ensure the map starts in an unsealed state for each test.
        sealedState = false;
        map = new SealableMap<>(sealedSupplier);
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        map.put(null, null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to avoid cross‑test contamination (optional).
        map.clear();
    }

    /* --------------------------------------------------------------------- *
     * Helper method to inject a custom backing map via reflection.        *
     * --------------------------------------------------------------------- */
    private void setUnderlyingMap(SealableMap<?, ?> target, Map<?, ?> newMap) throws Exception {
        Field field = SealableMap.class.getDeclaredField("map");
        field.setAccessible(true);
        field.set(target, newMap);
    }

    @Test
    void sizeReturnsNumberOfEntriesWhenUnsealed() {
        // given: map contains four entries (including a null key/value)
        assertEquals(4, map.size(), "Size should reflect the number of entries added in setUp");
    }

    @Test
    void sizeReflectsAddedEntriesWhenUnsealed() {
        // when: add a new entry
        map.put("four", 4);
        // then: size increases by one
        assertEquals(5, map.size(), "Size should increase after a successful put operation");
    }

    @Test
    void sizeReflectsRemovedEntriesWhenUnsealed() {
        // when: remove an existing entry
        map.remove("two");
        // then: size decreases by one
        assertEquals(3, map.size(), "Size should decrease after a successful remove operation");
    }

    @Test
    void sizeDoesNotChangeWhenSealed() {
        // given: seal the map
        sealedState = true;
        // when: attempt mutating operations (they will throw)
        assertThrows(UnsupportedOperationException.class, () -> map.put("five", 5));
        assertThrows(UnsupportedOperationException.class, () -> map.remove("one"));
        // then: size remains the same as before sealing
        assertEquals(4, map.size(), "Size should remain unchanged when the map is sealed");
    }

    @Test
    void sizeOnEmptyMap() {
        // given: an empty SealableMap
        SealableMap<String, Integer> emptyMap = new SealableMap<>(sealedSupplier);
        // then: size is zero
        assertEquals(0, emptyMap.size(), "A newly created map should have size 0");
    }

    @Test
    void sizeConsidersNullKeyAndNullValueAsEntries() {
        // map already contains a null key with a null value from setUp
        assertTrue(map.containsKey(null), "Map should contain the null key");
        assertTrue(map.containsValue(null), "Map should contain the null value");
        assertEquals(4, map.size(), "Null key/value pair counts as an entry in size");
    }

    @Test
    void sizeEmptyUnderlyingMap() {
        // When the SealableMap wraps an empty HashMap, size() should return 0.
        SealableMap<String, Integer> empty = new SealableMap<>(sealedSupplier);
        assertEquals(0, empty.size(), "size() must return 0 for a freshly created empty map");
    }

    @Test
    void sizeAfterAddingThreeEntries() {
        // Populate a fresh map with three distinct entries.
        SealableMap<String, Integer> fresh = new SealableMap<>(sealedSupplier);
        fresh.put("a", 10);
        fresh.put("b", 20);
        fresh.put("c", 30);
        assertEquals(3, fresh.size(), "size() should reflect three added entries");
    }

    @Test
    void sizeAfterRemovingTwoOfThreeEntries() {
        // Start with three entries, remove two, expect size == 1.
        SealableMap<String, Integer> fresh = new SealableMap<>(sealedSupplier);
        fresh.put("x", 1);
        fresh.put("y", 2);
        fresh.put("z", 3);
        fresh.remove("x");
        fresh.remove("y");
        assertEquals(1, fresh.size(), "size() should be 1 after removing two of three entries");
    }

    @Test
    void sizeAfterClearOnNonEmptyMap() {
        // Populate, then clear – size must become 0.
        SealableMap<String, Integer> fresh = new SealableMap<>(sealedSupplier);
        fresh.put("p", 5);
        fresh.put("q", 6);
        fresh.clear();
        assertEquals(0, fresh.size(), "size() should be 0 after clear() on a non‑empty map");
    }

    @Test
    void sizeAfterPutAllWithOverlappingKeys() {
        // Start with two entries.
        SealableMap<String, Integer> fresh = new SealableMap<>(sealedSupplier);
        fresh.put("k1", 1);
        fresh.put("k2", 2);
        // Map to be merged: three entries, one overlapping key ("k2").
        Map<String, Integer> toMerge = new HashMap<>();
        // replaces existing value
        toMerge.put("k2", 22);
        toMerge.put("k3", 3);
        toMerge.put("k4", 4);
        fresh.putAll(toMerge);
        assertEquals(4, fresh.size(), "size() should count distinct keys after putAll with one duplicate");
    }

    @Test
    void sizeAfterReplacingExistingKey() {
        // Two entries, then replace value for an existing key.
        SealableMap<String, Integer> fresh = new SealableMap<>(sealedSupplier);
        fresh.put("alpha", 100);
        fresh.put("beta", 200);
        // Replace value for "alpha"
        fresh.put("alpha", 999);
        assertEquals(2, fresh.size(), "size() must remain unchanged when an existing key is replaced");
    }

    @Test
    void sizeCountsNullKeyAndNullValueSeparately() {
        // Null key -> non‑null value, non‑null key -> null value.
        SealableMap<String, Integer> fresh = new SealableMap<>(sealedSupplier);
        fresh.put(null, 42);
        fresh.put("notNull", null);
        assertEquals(2, fresh.size(), "Both null key and null value entries should be counted");
    }

    @Test
    void sizeWhenUnderlyingMapIsNull() throws Exception {
        // Construct a normal map then replace the delegate with null via reflection.
        SealableMap<String, Integer> faulty = new SealableMap<>(sealedSupplier);
        setUnderlyingMap(faulty, null);
        assertThrows(NullPointerException.class, faulty::size, "Calling size() when the backing map is null must throw NPE");
    }

    @Test
    void sizePropagatesUnsupportedOperationExceptionFromDelegate() throws Exception {
        // Mock a Map whose size() throws UnsupportedOperationException.
        @SuppressWarnings("unchecked")
        Map<String, Integer> mockMap = mock(Map.class);
        when(mockMap.size()).thenThrow(new UnsupportedOperationException("delegate not supported"));
        SealableMap<String, Integer> wrapper = new SealableMap<>(sealedSupplier);
        setUnderlyingMap(wrapper, mockMap);
        assertThrows(UnsupportedOperationException.class, wrapper::size, "size() should propagate UnsupportedOperationException from the delegate");
    }

    @Test
    void sizePropagatesArbitraryRuntimeExceptionFromDelegate() throws Exception {
        // Mock a Map whose size() throws a generic RuntimeException.
        @SuppressWarnings("unchecked")
        Map<String, Integer> mockMap = mock(Map.class);
        when(mockMap.size()).thenThrow(new RuntimeException("delegate failure"));
        SealableMap<String, Integer> wrapper = new SealableMap<>(sealedSupplier);
        setUnderlyingMap(wrapper, mockMap);
        assertThrows(RuntimeException.class, wrapper::size, "size() should propagate arbitrary RuntimeException from the delegate");
    }
}
