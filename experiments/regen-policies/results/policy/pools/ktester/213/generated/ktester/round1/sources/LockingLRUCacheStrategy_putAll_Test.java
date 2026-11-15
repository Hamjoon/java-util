package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.InOrder;
import org.mockito.Mock;
import java.util.*;
import java.util.concurrent.locks.Lock;

@ExtendWith(MockitoExtension.class)
class LockingLRUCacheStrategy_putAll_Test {

    private LockingLRUCacheStrategy<String, Integer> cache;

    @BeforeEach
    void setupBeforeEach() {
        // capacity larger than the number of entries we will use in the tests
        cache = new LockingLRUCacheStrategy<>(10);
    }

    @Test
    void putAllAddsAllEntries() {
        Map<String, Integer> source = new HashMap<>();
        source.put("one", 1);
        source.put("two", 2);
        source.put("three", 3);
        cache.putAll(source);
        assertEquals(source.size(), cache.size(), "Cache size should match source map size");
        source.forEach((k, v) -> assertEquals(v, cache.get(k), "Value for key '" + k + "' should be stored"));
    }

    @Test
    void putAllInvokesPutForEachEntry() {
        // Use a spy to verify that the public put method is called for each entry
        LockingLRUCacheStrategy<String, Integer> spyCache = spy(new LockingLRUCacheStrategy<>(10));
        Map<String, Integer> source = new HashMap<>();
        source.put("alpha", 10);
        source.put("beta", 20);
        spyCache.putAll(source);
        // Verify that put was called exactly once for each key/value pair
        verify(spyCache, times(1)).put("alpha", 10);
        verify(spyCache, times(1)).put("beta", 20);
        // No extra invocations
        verifyNoMoreInteractions(spyCache);
    }

    @Test
    void putAllWithEmptyMapDoesNotChangeCache() {
        // Populate cache with a single entry first
        cache.put("initial", 99);
        int sizeBefore = cache.size();
        Map<String, Integer> empty = new HashMap<>();
        cache.putAll(empty);
        assertEquals(sizeBefore, cache.size(), "Cache size should remain unchanged when putAll receives an empty map");
        assertEquals(99, cache.get("initial"), "Existing entry should remain untouched");
    }

    @Test
    void putAll_nullMap_throwsNullPointerException_andLeavesCacheUnchanged() {
        // Fill cache with a known entry
        cache.put("existing", 42);
        int sizeBefore = cache.size();
        assertThrows(NullPointerException.class, () -> cache.putAll(null), "putAll(null) must throw NullPointerException");
        // Ensure cache state is unchanged
        assertEquals(sizeBefore, cache.size(), "Cache size must stay the same after NullPointerException");
        assertEquals(42, cache.get("existing"), "Existing entry must remain untouched");
    }

    @Test
    void putAll_nullKey_isAccepted_andStored() {
        Map<String, Integer> source = new HashMap<>();
        source.put(null, 0);
        source.put("one", 1);
        cache.putAll(source);
        assertEquals(2, cache.size(), "Cache should contain two entries");
        assertTrue(cache.containsKey(null), "Cache must contain the null key");
        assertEquals(0, cache.get(null), "Value for null key should be stored");
        assertEquals(1, cache.get("one"));
    }

    @Test
    void putAll_nullValue_isAccepted_andStored() {
        Map<String, Integer> source = new HashMap<>();
        source.put("nullVal", null);
        source.put("valid", 5);
        cache.putAll(source);
        assertEquals(2, cache.size(), "Cache should contain two entries");
        assertTrue(cache.containsKey("nullVal"), "Cache must contain the key with null value");
        assertNull(cache.get("nullVal"), "Value for key 'nullVal' should be null");
        assertEquals(5, cache.get("valid"));
    }

    @Test
    void putAll_duplicateKeys_lastValueWins() {
        // A normal Map cannot contain duplicate keys; we simulate by putting the same key twice
        Map<Integer, String> source = new LinkedHashMap<>();
        source.put(1, "one");
        // overwrites previous entry
        source.put(1, "uno");
        // Use a cache with Integer keys to match the map
        LockingLRUCacheStrategy<Integer, String> intCache = new LockingLRUCacheStrategy<>(10);
        intCache.putAll(source);
        assertEquals(1, intCache.size(), "Only one entry should be present");
        assertEquals("uno", intCache.get(1), "The last value for the duplicate key should be stored");
    }

    @Test
    void putAll_exceedsCapacity_respectsLRUEviction() {
        // capacity = 2, insert three entries via putAll
        LockingLRUCacheStrategy<Integer, String> smallCache = new LockingLRUCacheStrategy<>(2);
        Map<Integer, String> source = new LinkedHashMap<>();
        // oldest
        source.put(1, "one");
        source.put(2, "two");
        // newest
        source.put(3, "three");
        smallCache.putAll(source);
        // Expect the LRU (key 1) to have been evicted
        assertEquals(2, smallCache.size(), "Cache size must respect its capacity");
        assertNull(smallCache.get(1), "Key 1 should have been evicted");
        assertEquals("two", smallCache.get(2));
        assertEquals("three", smallCache.get(3));
    }

    @Test
    void putAll_iterationException_propagatesAndLockIsReleased() {
        // Mock a Map whose entrySet iterator throws a RuntimeException
        @SuppressWarnings("unchecked")
        Map<String, Integer> faultyMap = mock(Map.class);
        Set<Map.Entry<String, Integer>> entrySet = mock(Set.class);
        Iterator<Map.Entry<String, Integer>> iterator = mock(Iterator.class);
        when(faultyMap.entrySet()).thenReturn(entrySet);
        when(entrySet.iterator()).thenReturn(iterator);
        when(iterator.hasNext()).thenThrow(new RuntimeException("iteration failure"));
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> cache.putAll(faultyMap), "putAll should propagate the RuntimeException from iterator");
        assertEquals("iteration failure", thrown.getMessage());
        // Verify that the cache is still usable after the exception (lock released)
        assertDoesNotThrow(() -> cache.put("after", 1), "Cache must remain functional after putAll exception");
        assertEquals(1, cache.get("after"));
    }

    @Test
    void putAll_entrySetReturnsNull_throwsNullPointerException_andLeavesCacheUnchanged() {
        @SuppressWarnings("unchecked")
        Map<String, Integer> badMap = mock(Map.class);
        // simulate null entrySet
        when(badMap.entrySet()).thenReturn(null);
        // Pre‑populate cache to verify it stays unchanged
        cache.put("pre", 100);
        int sizeBefore = cache.size();
        assertThrows(NullPointerException.class, () -> cache.putAll(badMap), "putAll should throw NPE when entrySet() returns null");
        assertEquals(sizeBefore, cache.size(), "Cache size must stay the same after NPE");
        assertEquals(100, cache.get("pre"));
    }

    @Test
    void putAll_normal_mapInsertion_verifiesContentAndSize() {
        // This mirrors the JSON group 'putAll_normal'
        Map<Integer, String> source = new LinkedHashMap<>();
        source.put(1, "one");
        source.put(2, "two");
        source.put(3, "three");
        LockingLRUCacheStrategy<Integer, String> intCache = new LockingLRUCacheStrategy<>(10);
        intCache.putAll(source);
        assertEquals(3, intCache.size(), "Cache should contain three entries");
        source.forEach((k, v) -> assertEquals(v, intCache.get(k), "Cache must contain entry {" + k + "='" + v + "'}"));
    }
}
