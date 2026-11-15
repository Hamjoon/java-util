package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

@ExtendWith(MockitoExtension.class)
class TTLCache_entrySet_Test {

    private TTLCache<Integer, String> ttlCache;

    @BeforeEach
    void setupBeforeEach() {
        // Use a relatively long TTL so that normal tests are not affected by expiration.
        // Cleanup interval is short enough to keep the background thread alive but does not interfere.
        ttlCache = new TTLCache<>(10_000L, -1, 5_000L);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache is cleared between tests to avoid cross‑test contamination.
        ttlCache.clear();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the shared background executor used by TTLCache.
        TTLCache.shutdown();
    }

    @Test
    void entrySetIsNotNullAndReflectsCurrentSize() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.put(3, "C");
        Set<Map.Entry<Integer, String>> entrySet = ttlCache.entrySet();
        assertNotNull(entrySet, "entrySet() must never return null");
        assertEquals(3, entrySet.size(), "entrySet size must match number of entries");
    }

    @Test
    void entrySetIteratesOverAllEntries() {
        ttlCache.put(10, "X");
        ttlCache.put(20, "Y");
        ttlCache.put(30, "Z");
        Set<Map.Entry<Integer, String>> entrySet = ttlCache.entrySet();
        assertEquals(3, entrySet.size());
        // Collect entries via iterator
        boolean found10 = false, found20 = false, found30 = false;
        for (Map.Entry<Integer, String> e : entrySet) {
            if (e.getKey() == 10 && "X".equals(e.getValue()))
                found10 = true;
            if (e.getKey() == 20 && "Y".equals(e.getValue()))
                found20 = true;
            if (e.getKey() == 30 && "Z".equals(e.getValue()))
                found30 = true;
        }
        assertTrue(found10 && found20 && found30, "Iterator must visit every entry that was put into the cache");
    }

    @Test
    void entrySetRemoveUpdatesCache() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.put(3, "C");
        Set<Map.Entry<Integer, String>> entrySet = ttlCache.entrySet();
        // Remove an entry via the Set's remove(Object) method
        Map.Entry<Integer, String> toRemove = null;
        for (Map.Entry<Integer, String> e : entrySet) {
            if (e.getKey().equals(2)) {
                toRemove = e;
                break;
            }
        }
        assertNotNull(toRemove, "Entry with key 2 must be present before removal");
        assertTrue(entrySet.remove(toRemove), "remove(entry) should return true");
        // Verify cache state
        assertNull(ttlCache.get(2), "Removed key must no longer be present in the cache");
        assertEquals(2, ttlCache.size(), "Cache size must shrink after removal");
        assertFalse(entrySet.contains(toRemove), "EntrySet must no longer contain the removed entry");
    }

    @Test
    void entrySetIteratorRemoveUpdatesCache() {
        ttlCache.put(5, "V");
        ttlCache.put(6, "W");
        Set<Map.Entry<Integer, String>> entrySet = ttlCache.entrySet();
        Iterator<Map.Entry<Integer, String>> it = entrySet.iterator();
        // Remove the first element using iterator.remove()
        assertTrue(it.hasNext(), "Iterator should have at least one element");
        // advance to first entry
        it.next();
        // remove it
        it.remove();
        // After removal, the cache must reflect the change
        assertEquals(1, ttlCache.size(), "Cache size must be 1 after iterator removal");
        // The remaining key is either 5 or 6 depending on iteration order
        assertTrue(ttlCache.containsKey(5) ^ ttlCache.containsKey(6), "Exactly one of the original keys must remain");
    }

    @Test
    void entrySetBecomesEmptyAfterExpiration() throws InterruptedException {
        // Short TTL (200 ms) and a cleanup interval that runs frequently (50 ms)
        ttlCache = new TTLCache<>(200L, -1, 50L);
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        // Verify entries are present initially
        assertEquals(2, ttlCache.size());
        assertTrue(ttlCache.entrySet().containsAll(ttlCache.entrySet()));
        // Wait long enough for the entries to expire
        Thread.sleep(350L);
        // After expiration the cache should be empty and entrySet should reflect that
        assertEquals(0, ttlCache.size(), "Cache size must be zero after TTL expiry");
        assertTrue(ttlCache.entrySet().isEmpty(), "entrySet() must be empty after all entries have expired");
    }

    @Test
    void entrySetEqualsOtherMapEntrySet() {
        ttlCache.put(100, "Alpha");
        ttlCache.put(200, "Beta");
        // Build a plain HashMap with the same content
        java.util.Map<Integer, String> other = new java.util.HashMap<>();
        other.put(100, "Alpha");
        other.put(200, "Beta");
        // entrySet().equals(other.entrySet()) is used by TTLCache.equals()
        assertEquals(other.entrySet(), ttlCache.entrySet(), "entrySet of TTLCache must be equal to entrySet of a map with identical entries");
    }

    @Test
    void entrySetOnEmptyCacheIsEmpty() {
        Set<Map.Entry<Integer, String>> entrySet = ttlCache.entrySet();
        assertNotNull(entrySet, "entrySet() must never return null");
        assertTrue(entrySet.isEmpty(), "Newly constructed cache should have an empty entrySet");
        assertEquals(0, entrySet.size(), "Size of entrySet for empty cache must be 0");
    }

    @Test
    void entrySetContainsSingleEntryAfterPut() {
        ttlCache.put(42, "answer");
        Set<Map.Entry<Integer, String>> entrySet = ttlCache.entrySet();
        assertEquals(1, entrySet.size(), "entrySet size must be 1 after a single put");
        Map.Entry<Integer, String> expected = new AbstractMap.SimpleEntry<>(42, "answer");
        assertTrue(entrySet.contains(expected), "entrySet must contain the entry just put");
    }

    @Test
    void entrySetReflectsSubsequentModifications() {
        ttlCache.put(1, "A");
        Set<Map.Entry<Integer, String>> entrySet = ttlCache.entrySet();
        assertEquals(1, entrySet.size());
        ttlCache.put(2, "B");
        assertEquals(2, entrySet.size(), "entrySet must reflect new entries added after the view was obtained");
    }

    @Test
    void iteratorRemoveAllEntriesEmptiesCache() {
        ttlCache.put(10, "X");
        ttlCache.put(20, "Y");
        Iterator<Map.Entry<Integer, String>> it = ttlCache.entrySet().iterator();
        while (it.hasNext()) {
            it.next();
            it.remove();
        }
        assertEquals(0, ttlCache.size(), "Cache must be empty after iterator.remove() on all entries");
        assertTrue(ttlCache.entrySet().isEmpty(), "entrySet must be empty after all removals");
    }

    @Test
    void entrySetCallsReturnDistinctButEqualSets() {
        ttlCache.put(5, "five");
        ttlCache.put(6, "six");
        Set<Map.Entry<Integer, String>> set1 = ttlCache.entrySet();
        Set<Map.Entry<Integer, String>> set2 = ttlCache.entrySet();
        assertNotSame(set1, set2, "Two calls to entrySet() should return distinct Set instances");
        assertEquals(set1, set2, "Both Set instances must be equal in content");
        // Mutate via one view and verify the other reflects the change
        set1.removeIf(e -> e.getKey().equals(5));
        assertEquals(1, set2.size(), "Both views must stay in sync after mutation");
    }

    @Test
    void entrySetSupportsContainsWithMatchingEntry() {
        ttlCache.put(7, "seven");
        Map.Entry<Integer, String> probe = new AbstractMap.SimpleEntry<>(7, "seven");
        assertTrue(ttlCache.entrySet().contains(probe), "contains(entry) must return true for a matching key/value pair");
    }

    @Test
    void entrySetToArrayMatchesCacheContents() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.put(3, "C");
        Object[] array = ttlCache.entrySet().toArray();
        assertEquals(3, array.length, "toArray() length must equal cache size");
        Set<Map.Entry<Integer, String>> expected = new HashSet<>(ttlCache.entrySet());
        for (Object o : array) {
            assertTrue(o instanceof Map.Entry, "Elements returned by toArray() must be Map.Entry");
            assertTrue(expected.remove(o), "Each entry in array must be present in the cache");
        }
        assertTrue(expected.isEmpty(), "All entries must be accounted for in the array");
    }

    @Test
    void setRemoveExistingEntryDecreasesSize() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        Map.Entry<Integer, String> entry = new AbstractMap.SimpleEntry<>(1, "A");
        boolean removed = ttlCache.entrySet().remove(entry);
        assertTrue(removed, "remove(entry) should return true for existing entry");
        assertEquals(1, ttlCache.size(), "Cache size must decrease after successful removal");
    }

    @Test
    void setRemoveNonExistingEntryReturnsFalse() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        Map.Entry<Integer, String> nonExisting = new AbstractMap.SimpleEntry<>(3, "C");
        boolean removed = ttlCache.entrySet().remove(nonExisting);
        assertFalse(removed, "remove(entry) should return false when entry is absent");
        assertEquals(2, ttlCache.size(), "Cache size must remain unchanged");
    }

    @Test
    void setRemoveEntryWithSameKeyDifferentValueFails() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        Map.Entry<Integer, String> mismatched = new AbstractMap.SimpleEntry<>(1, "X");
        boolean removed = ttlCache.entrySet().remove(mismatched);
        assertFalse(removed, "remove(entry) must consider both key and value");
        assertEquals(2, ttlCache.size(), "Cache size must stay the same when removal fails");
    }

    @Test
    void entrySetClearEmptiesCache() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.entrySet().clear();
        assertEquals(0, ttlCache.size(), "Cache size must be 0 after entrySet().clear()");
        assertTrue(ttlCache.entrySet().isEmpty(), "entrySet must be empty after clear()");
    }

    @Test
    void entrySetClearOnEmptyCacheIsNoOp() {
        assertTrue(ttlCache.entrySet().isEmpty(), "Pre‑condition: cache is empty");
        assertDoesNotThrow(() -> ttlCache.entrySet().clear(), "Calling clear() on an empty entrySet must not throw");
        assertEquals(0, ttlCache.size(), "Size must remain 0 after clear on empty cache");
    }

    @Test
    void entrySetAddThrowsUnsupportedOperationException() {
        Set<Map.Entry<Integer, String>> set = ttlCache.entrySet();
        Map.Entry<Integer, String> newEntry = new AbstractMap.SimpleEntry<>(99, "X");
        assertThrows(UnsupportedOperationException.class, () -> set.add(newEntry), "add() on entrySet must throw UnsupportedOperationException");
    }

    @Test
    void entrySetAddAllThrowsUnsupportedOperationException() {
        Set<Map.Entry<Integer, String>> set = ttlCache.entrySet();
        Collection<Map.Entry<Integer, String>> coll = Collections.singleton(new AbstractMap.SimpleEntry<>(100, "Y"));
        assertThrows(UnsupportedOperationException.class, () -> set.addAll(coll), "addAll() on entrySet must throw UnsupportedOperationException");
    }

    @Test
    void iteratorRemoveBeforeNextThrowsIllegalStateException() {
        ttlCache.put(1, "A");
        Iterator<Map.Entry<Integer, String>> it = ttlCache.entrySet().iterator();
        assertThrows(IllegalStateException.class, it::remove, "Calling iterator.remove() before next() must throw IllegalStateException");
    }

    @Test
    void iteratorHasNextFalseAfterExhaustionAndNextThrowsNoSuchElementException() {
        ttlCache.put(1, "A");
        Iterator<Map.Entry<Integer, String>> it = ttlCache.entrySet().iterator();
        assertTrue(it.hasNext());
        // consume the only element
        it.next();
        assertFalse(it.hasNext(), "hasNext() must be false after the last element");
        assertThrows(NoSuchElementException.class, it::next, "next() after exhaustion must throw NoSuchElementException");
    }

    @Test
    void iteratorFailsFastWhenCacheClearedDuringIteration() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        Iterator<Map.Entry<Integer, String>> it = ttlCache.entrySet().iterator();
        assertTrue(it.hasNext());
        // retrieve first element
        it.next();
        // clear underlying cache while iterator is still alive
        ttlCache.clear();
        assertThrows(NoSuchElementException.class, it::next, "Iterator must throw NoSuchElementException if underlying cache is cleared");
    }

    @Test
    void entrySetContainsAllWithMatchingCollection() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        Collection<Map.Entry<Integer, String>> coll = new ArrayList<>(ttlCache.entrySet());
        assertTrue(ttlCache.entrySet().containsAll(coll), "containsAll should return true for a collection matching the entrySet");
    }

    @Test
    void entrySetRemoveAllDeletesCorrespondingEntries() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.put(3, "C");
        Collection<Map.Entry<Integer, String>> toRemove = Arrays.asList(new AbstractMap.SimpleEntry<>(1, "A"), new AbstractMap.SimpleEntry<>(3, "C"));
        boolean changed = ttlCache.entrySet().removeAll(toRemove);
        assertTrue(changed, "removeAll should return true when entries are removed");
        assertEquals(1, ttlCache.size(), "Cache size must reflect removed entries");
        assertTrue(ttlCache.containsKey(2) && !ttlCache.containsKey(1) && !ttlCache.containsKey(3));
    }

    @Test
    void entrySetRetainAllKeepsOnlySpecifiedEntries() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.put(3, "C");
        Collection<Map.Entry<Integer, String>> toRetain = Collections.singleton(new AbstractMap.SimpleEntry<>(2, "B"));
        boolean changed = ttlCache.entrySet().retainAll(toRetain);
        assertTrue(changed, "retainAll should return true when the set is reduced");
        assertEquals(1, ttlCache.size(), "Only the retained entry should remain");
        assertTrue(ttlCache.containsKey(2) && !ttlCache.containsKey(1) && !ttlCache.containsKey(3));
    }

    @Test
    void entrySetTypedToArrayMatchesCacheSizeAndContent() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        Map.Entry<Integer, String>[] array = ttlCache.entrySet().toArray(new Map.Entry[0]);
        assertEquals(2, array.length, "Typed toArray(T[]) length must equal cache size");
        Set<Map.Entry<Integer, String>> expected = new HashSet<>(ttlCache.entrySet());
        for (Map.Entry<Integer, String> e : array) {
            assertTrue(expected.remove(e), "Each element of the typed array must be present in the cache");
        }
        assertTrue(expected.isEmpty(), "All entries must be accounted for in the typed array");
    }

    @Test
    void entrySetContainsNullReturnsFalse() {
        ttlCache.put(1, "A");
        assertFalse(ttlCache.entrySet().contains(null), "contains(null) must safely return false");
    }

    @Test
    void putNullKeyThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ttlCache.put(null, "value"), "Putting a null key must throw NullPointerException");
        assertEquals(0, ttlCache.size(), "Cache must remain unchanged after failed put");
    }

    @Test
    void putNullValueIsAllowedAndAppearsInEntrySet() {
        ttlCache.put(42, null);
        assertEquals(1, ttlCache.size(), "Cache should accept null values");
        Map.Entry<Integer, String> probe = new AbstractMap.SimpleEntry<>(42, null);
        assertTrue(ttlCache.entrySet().contains(probe), "EntrySet must contain the entry with a null value");
    }

    @Test
    void entrySetAfterShutdownThrowsIllegalStateException() {
        ttlCache.put(1, "A");
        // global shutdown
        TTLCache.shutdown();
        assertThrows(IllegalStateException.class, ttlCache::entrySet, "Calling entrySet() after shutdown must throw IllegalStateException");
        // Verify subsequent calls also throw
        assertThrows(IllegalStateException.class, ttlCache::entrySet, "Subsequent entrySet() calls after shutdown must continue to throw");
    }
}
