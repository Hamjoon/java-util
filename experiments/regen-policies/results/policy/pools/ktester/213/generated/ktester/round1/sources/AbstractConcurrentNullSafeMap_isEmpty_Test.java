package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mockito;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_isEmpty_Test {

    private TestConcurrentNullSafeMap<String, String> mapUnderTest;

    private ConcurrentMap<Object, Object> backingMap;

    /**
     * Simple concrete implementation used only for testing the abstract class.
     */
    private static class TestConcurrentNullSafeMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        TestConcurrentNullSafeMap(ConcurrentMap<Object, Object> internalMap) {
            super(internalMap);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh backing map for each test to guarantee isolation.
        backingMap = new ConcurrentHashMap<>();
        mapUnderTest = new TestConcurrentNullSafeMap<>(backingMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the backing map to avoid side‑effects if a test forgets to clean up.
        backingMap.clear();
    }

    @Test
    void isEmpty_returnsTrueWhenMapHasNoEntries() {
        // given: a newly created map (already empty)
        // when:
        boolean result = mapUnderTest.isEmpty();
        // then:
        assertTrue(result, "isEmpty() should return true for an empty map");
    }

    @Test
    void isEmpty_returnsFalseWhenMapContainsEntries() {
        // given:
        mapUnderTest.put("key1", "value1");
        // also test null‑key handling
        mapUnderTest.put(null, "nullKey");
        // and null‑value handling
        mapUnderTest.put("key2", null);
        // when:
        boolean result = mapUnderTest.isEmpty();
        // then:
        assertFalse(result, "isEmpty() should return false when the map contains entries");
    }

    @Test
    void isEmpty_returnsTrueAfterClearOnPreviouslyPopulatedMap() {
        // given: map with entries
        mapUnderTest.put("a", "1");
        mapUnderTest.put("b", "2");
        // when: clear the map
        mapUnderTest.clear();
        // then:
        assertTrue(mapUnderTest.isEmpty(), "isEmpty() should return true after clear() on a map that previously held entries");
    }

    @Test
    void isEmpty_returnsFalseAfterSinglePut() {
        mapUnderTest.put("onlyKey", "onlyValue");
        assertFalse(mapUnderTest.isEmpty(), "isEmpty() should return false after inserting a single entry");
    }

    @Test
    void isEmpty_returnsFalseWithMultipleEntriesIncludingNulls() {
        mapUnderTest.put("k1", "v1");
        // masked null key
        mapUnderTest.put(null, "nullKey");
        // masked null value
        mapUnderTest.put("k2", null);
        mapUnderTest.put("k3", "v3");
        assertFalse(mapUnderTest.isEmpty(), "isEmpty() must remain false when multiple entries (including masked nulls) exist");
    }

    @Test
    void isEmpty_returnsTrueAfterRemovingTheOnlyEntry() {
        mapUnderTest.put("x", "y");
        mapUnderTest.remove("x");
        assertTrue(mapUnderTest.isEmpty(), "After removing the sole entry, isEmpty() should return true");
    }

    @Test
    void isEmpty_returnsFalseWhenRemoveNonExistentKey() {
        mapUnderTest.put("a", "1");
        mapUnderTest.put("b", "2");
        // attempt to remove a key that does not exist
        mapUnderTest.remove("nonexistent");
        assertFalse(mapUnderTest.isEmpty(), "Removing a non‑existent key must not affect the non‑empty state of the map");
    }

    @Test
    void isEmpty_returnsTrueWhenPutAllWithEmptySourceMap() {
        Map<String, String> emptySource = Collections.emptyMap();
        mapUnderTest.putAll(emptySource);
        assertTrue(mapUnderTest.isEmpty(), "Putting an empty source map into an empty map should keep it empty");
    }

    @Test
    void isEmpty_returnsFalseWhenPutAllAddsEntries() {
        Map<String, String> source = new HashMap<>();
        source.put("a", "1");
        source.put("b", "2");
        mapUnderTest.putAll(source);
        assertFalse(mapUnderTest.isEmpty(), "After putAll with a non‑empty source, the map must be non‑empty");
    }

    @Test
    void isEmpty_returnsFalseAfterComputeIfAbsentCreatesMapping() {
        mapUnderTest.computeIfAbsent("newKey", k -> "generated");
        assertFalse(mapUnderTest.isEmpty(), "computeIfAbsent on an empty map should create a mapping, making it non‑empty");
    }

    @Test
    void isEmpty_returnsFalseWhenComputeIfAbsentFindsExistingKey() {
        mapUnderTest.put("existing", "value");
        mapUnderTest.computeIfAbsent("existing", k -> "newValue");
        assertFalse(mapUnderTest.isEmpty(), "computeIfAbsent on an existing key must not change emptiness (still non‑empty)");
    }

    @Test
    void isEmpty_returnsFalseWhenMergeAddsEntry() {
        mapUnderTest.merge("k", "v", (oldV, newV) -> oldV + newV);
        assertFalse(mapUnderTest.isEmpty(), "merge on an empty map should add the entry, resulting in non‑empty");
    }

    @Test
    void isEmpty_returnsTrueWhenMergeRemovesExistingEntry() {
        mapUnderTest.put("k", "v");
        // remapping function returns null → entry should be removed
        mapUnderTest.merge("k", "ignored", (oldV, newV) -> null);
        assertTrue(mapUnderTest.isEmpty(), "If merge's remapping function returns null, the entry is removed and map becomes empty");
    }

    @Test
    void isEmpty_returnsFalseAfterSuccessfulReplace() {
        mapUnderTest.put("k", "old");
        mapUnderTest.replace("k", "new");
        assertFalse(mapUnderTest.isEmpty(), "Replacing an existing key keeps the map non‑empty");
    }

    @Test
    void isEmpty_returnsTrueWhenReplaceFailsOnEmptyMap() {
        // replace on a key that does not exist; map stays empty
        mapUnderTest.replace("missing", "value");
        assertTrue(mapUnderTest.isEmpty(), "Attempting to replace a non‑existent key in an empty map should leave it empty");
    }

    @Test
    void isEmpty_returnsFalseAfterPuttingNullKeyAndNullValue() {
        // both key and value are masked internally
        mapUnderTest.put(null, null);
        assertFalse(mapUnderTest.isEmpty(), "Putting (null, null) creates a masked entry, so the map is not empty");
    }

    @Test
    void isEmpty_returnsTrueAfterRemovingMaskedNullEntry() {
        mapUnderTest.put(null, null);
        mapUnderTest.remove(null);
        assertTrue(mapUnderTest.isEmpty(), "Removing the masked null entry should make the map empty again");
    }

    @Test
    void isEmpty_returnsTrueWhenComputeReturnsNullAndRemovesMapping() {
        mapUnderTest.put("k", "v");
        // returning null removes the entry
        mapUnderTest.compute("k", (k, v) -> null);
        assertTrue(mapUnderTest.isEmpty(), "compute that returns null must delete the mapping, leaving the map empty");
    }

    @Test
    void isEmpty_throwsNullPointerExceptionWhenInternalMapIsNull() {
        // Construct a map with a null backing map
        TestConcurrentNullSafeMap<String, String> nullBackingMap = new TestConcurrentNullSafeMap<>(null);
        assertThrows(NullPointerException.class, nullBackingMap::isEmpty, "Calling isEmpty() when internalMap is null must throw NullPointerException");
    }

    @Test
    void isEmpty_propagatesUnsupportedOperationExceptionFromInternalMap() {
        @SuppressWarnings("unchecked")
        ConcurrentMap<Object, Object> mockMap = mock(ConcurrentMap.class);
        when(mockMap.isEmpty()).thenThrow(new UnsupportedOperationException("read‑only"));
        TestConcurrentNullSafeMap<String, String> map = new TestConcurrentNullSafeMap<>(mockMap);
        assertThrows(UnsupportedOperationException.class, map::isEmpty, "isEmpty() should propagate UnsupportedOperationException from the underlying map");
    }

    @Test
    void isEmpty_propagatesIllegalStateExceptionFromInternalMap() {
        @SuppressWarnings("unchecked")
        ConcurrentMap<Object, Object> mockMap = mock(ConcurrentMap.class);
        when(mockMap.isEmpty()).thenThrow(new IllegalStateException("boom"));
        TestConcurrentNullSafeMap<String, String> map = new TestConcurrentNullSafeMap<>(mockMap);
        assertThrows(IllegalStateException.class, map::isEmpty, "Exceptions thrown by internalMap.isEmpty() must bubble up unchanged");
    }
}
