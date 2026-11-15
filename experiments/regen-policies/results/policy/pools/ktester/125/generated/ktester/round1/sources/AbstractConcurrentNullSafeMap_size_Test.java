package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_size_Test {

    private TestMap map;

    /**
     * Concrete implementation used only for testing the abstract class.
     */
    private static class TestMap extends AbstractConcurrentNullSafeMap<Object, Object> {

        TestMap(ConcurrentMap<Object, Object> internal) {
            super(internal);
        }
    }

    class NullBackingMap extends AbstractConcurrentNullSafeMap<Object, Object> {

        NullBackingMap() {
            super(null);
        }
    }

    class NullBackingMap extends AbstractConcurrentNullSafeMap<Object, Object> {

        NullBackingMap() {
            super(null);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a real ConcurrentHashMap for most tests.
        ConcurrentMap<Object, Object> backing = new ConcurrentHashMap<>();
        map = new TestMap(backing);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to avoid cross‑test contamination.
        map.clear();
    }

    private static Stream<Arguments> sizeScenarios() {
        return Stream.of(Arguments.of(List.of("a", "b", "c"), 3, "Three distinct puts"), Arguments.of(List.of(null, "x", null), 2, "Null key and a regular key"), Arguments.of(List.of("k1", "k1", "k1"), 1, "Duplicate puts of same key"));
    }

    @Test
    void sizeReturnsZeroForEmptyMap() {
        assertEquals(0, map.size(), "Size of a newly created map should be 0");
    }

    @Test
    void sizeReflectsNumberOfEntriesIncludingNullKeyAndValue() {
        map.put("one", 1);
        // null key is supported
        map.put(null, "nullKey");
        // null value is supported
        map.put("two", null);
        assertEquals(3, map.size(), "Size should count all entries, even those with null key/value");
    }

    @Test
    void sizeUpdatesAfterRemoval() {
        map.put("a", "A");
        map.put("b", "B");
        assertEquals(2, map.size());
        map.remove("a");
        assertEquals(1, map.size(), "Size should decrement after a removal");
    }

    @Test
    void sizeDelegatesToUnderlyingConcurrentMap() {
        // Create a mock ConcurrentMap to verify delegation.
        @SuppressWarnings("unchecked")
        ConcurrentMap<Object, Object> mockInternal = mock(ConcurrentMap.class);
        when(mockInternal.size()).thenReturn(42);
        TestMap mockMap = new TestMap(mockInternal);
        int size = mockMap.size();
        assertEquals(42, size, "Returned size should be whatever the internal map reports");
        verify(mockInternal, times(1)).size();
    }

    @Test
    void sizeAfterSinglePut() {
        map.put("k", "v");
        assertEquals(1, map.size(), "After putting a single entry, size must be 1");
    }

    @Test
    void sizeAfterThreeDistinctPuts() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertEquals(3, map.size(), "Size should reflect three distinct entries");
    }

    @Test
    void sizeAfterClear() {
        map.put("x", "X");
        map.clear();
        assertEquals(0, map.size(), "Size must be 0 after clear()");
    }

    @Test
    void sizeAfterAddThreeRemoveOne() {
        map.put("1", "one");
        map.put("2", "two");
        map.put("3", "three");
        map.remove("2");
        assertEquals(2, map.size(), "Remaining count after removal should be 2");
    }

    @Test
    void sizeCountsNullKeyAndNullValue() {
        map.put(null, "nullKey");
        map.put("regular", null);
        assertEquals(2, map.size(), "Null key and null value should each count as an entry");
    }

    @Test
    void sizeAfterClearWhenOnlyNullEntriesPresent() {
        map.put(null, null);
        map.clear();
        assertEquals(0, map.size(), "Clearing map with only null entries should result in size 0");
    }

    @Test
    void sizeAfterReplaceExistingKey() {
        map.put("k", "v1");
        map.replace("k", "v2");
        assertEquals(1, map.size(), "Replacing value for existing key must not change size");
    }

    @Test
    void sizeAfterReplaceNonExistingKey() {
        map.replace("missing", "value");
        assertEquals(0, map.size(), "replace on absent key must not insert entry");
    }

    @Test
    void sizeAfterComputeIfAbsentAddsNewKey() {
        map.computeIfAbsent("newKey", k -> "newValue");
        assertEquals(1, map.size(), "computeIfAbsent should add a new entry when key is missing");
    }

    @Test
    void sizeAfterComputeIfAbsentOnExistingKey() {
        map.put("existing", "old");
        // mapping function should NOT be called
        map.computeIfAbsent("existing", k -> "new");
        assertEquals(1, map.size(), "computeIfAbsent on existing key must not change size");
    }

    @Test
    void sizeAfterComputeReplacesValue() {
        map.put("k", "old");
        map.compute("k", (k, v) -> "new");
        assertEquals(1, map.size(), "compute that returns new non‑null value should keep size unchanged");
    }

    @Test
    void sizeAfterComputeRemovesEntryWhenNullReturned() {
        map.put("k", "value");
        // returning null removes the entry
        map.compute("k", (k, v) -> null);
        assertEquals(0, map.size(), "compute returning null must remove the entry");
    }

    @Test
    void sizeAfterPutAllWithNullKeyAndValue() {
        Map<Object, Object> source = new HashMap<>();
        source.put(null, "nullKey");
        source.put("regular", null);
        source.put("normal", "value");
        map.putAll(source);
        assertEquals(3, map.size(), "putAll must store null key/value entries as regular entries");
    }

    @Test
    void sizeAfterPutIfAbsentOnExistingKey() {
        map.put("k", "v1");
        map.putIfAbsent("k", "v2");
        assertEquals(1, map.size(), "putIfAbsent on existing key must not change size");
    }

    @Test
    void sizeAfterPutIfAbsentOnNewKey() {
        map.putIfAbsent("new", "val");
        assertEquals(1, map.size(), "putIfAbsent on missing key should insert a new entry");
    }

    @Test
    void sizeAfterRemovingNonExistentKey() {
        map.remove("absent");
        assertEquals(0, map.size(), "Removing a non‑existent key must leave size unchanged");
    }

    @Test
    void sizeAfterDuplicatePutsSameKey() {
        map.put("dup", "first");
        map.put("dup", "second");
        assertEquals(1, map.size(), "Putting the same key twice replaces value without increasing size");
    }

    @Test
    void sizeAfterLargeNumberOfEntries() {
        for (int i = 0; i < 1000; i++) {
            map.put("k" + i, i);
        }
        assertEquals(1000, map.size(), "Size must correctly report 1,000 distinct entries");
    }

    @Test
    void sizeAfterMixedAddRemoveSequence() {
        // Add 5 entries
        for (int i = 0; i < 5; i++) {
            map.put("a" + i, i);
        }
        // Remove 2 of them
        map.remove("a0");
        map.remove("a3");
        // Add 3 more
        map.put("b1", "B1");
        map.put("b2", "B2");
        map.put("b3", "B3");
        assertEquals(6, map.size(), "Final size after mixed operations should be 6");
    }

    @Test
    void sizeWhenInternalMapIsNullThrowsNPE() {
        // Subclass that deliberately passes null as the backing map
        class NullBackingMap extends AbstractConcurrentNullSafeMap<Object, Object> {

            NullBackingMap() {
                super(null);
            }
        }
        NullBackingMap nullMap = new NullBackingMap();
        assertThrows(NullPointerException.class, nullMap::size, "Calling size() with a null internalMap should throw NPE");
    }

    @Test
    void sizePropagatesExceptionFromInternalMap() {
        @SuppressWarnings("unchecked")
        ConcurrentMap<Object, Object> mockInternal = mock(ConcurrentMap.class);
        when(mockInternal.size()).thenThrow(new IllegalStateException("boom"));
        TestMap exceptionMap = new TestMap(mockInternal);
        IllegalStateException ex = assertThrows(IllegalStateException.class, exceptionMap::size, "Exception from internalMap.size() must be propagated");
        assertEquals("boom", ex.getMessage());
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("sizeScenarios")
    void sizeParameterized(List<Object> keys, int expectedSize, String description) {
        for (Object k : keys) {
            map.put(k, "value");
        }
        assertEquals(expectedSize, map.size(), description);
    }
}
