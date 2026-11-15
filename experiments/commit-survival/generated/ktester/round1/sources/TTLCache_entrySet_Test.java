package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

@ExtendWith(MockitoExtension.class)
class TTLCache_entrySet_Test {

    /**
     * Cache used by the tests.
     */
    private TTLCache<Integer, String> ttlCache;

    /**
     * Initialise a fresh cache before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        // A TTL of 10 seconds and no size limit – long enough that expiration does not interfere.
        ttlCache = new TTLCache<>(10_000L, -1);
    }

    /**
     * Clean the cache after each test to avoid cross‑test contamination.
     */
    @AfterEach
    void teardownAfterEach() {
        ttlCache.clear();
    }

    /**
     * Global clean‑up – shut down the background purge thread used by TTLCache.
     */
    @AfterAll
    static void teardownAfterAll() {
        TTLCache.shutdown();
    }

    /**
     * Verify that {@code entrySet()} returns a live view of the map contents.
     */
    @Test
    void entrySetReflectsCurrentMappings() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.put(3, "C");
        Set<Map.Entry<Integer, String>> entrySet = ttlCache.entrySet();
        // Size must match the number of put entries.
        assertEquals(3, entrySet.size());
        // The set must contain the exact entries we inserted.
        assertTrue(entrySet.contains(Map.entry(1, "A")));
        assertTrue(entrySet.contains(Map.entry(2, "B")));
        assertTrue(entrySet.contains(Map.entry(3, "C")));
        // Iterating over the set should yield the same elements.
        int count = 0;
        for (Map.Entry<Integer, String> e : entrySet) {
            assertNotNull(e.getKey());
            assertNotNull(e.getValue());
            count++;
        }
        assertEquals(3, count);
    }

    /**
     * Removing an entry via the iterator must also remove it from the underlying cache.
     */
    @Test
    void entrySetIteratorRemoveUpdatesCache() {
        ttlCache.put(10, "X");
        ttlCache.put(20, "Y");
        ttlCache.put(30, "Z");
        Set<Map.Entry<Integer, String>> entrySet = ttlCache.entrySet();
        Iterator<Map.Entry<Integer, String>> it = entrySet.iterator();
        // Remove the entry with key 20 while iterating.
        while (it.hasNext()) {
            Map.Entry<Integer, String> e = it.next();
            if (e.getKey().equals(20)) {
                it.remove();
                break;
            }
        }
        // The cache must no longer contain the removed key.
        assertNull(ttlCache.get(20));
        assertEquals(2, ttlCache.size());
        // The entrySet view must also reflect the removal.
        assertFalse(entrySet.stream().anyMatch(e -> e.getKey().equals(20)));
    }

    /**
     * An entry set must become empty once all entries have expired.
     */
    @Test
    void entrySetBecomesEmptyAfterExpiration() throws InterruptedException {
        // TTL of 200 ms, cleanup interval of 50 ms – fast enough for the test.
        ttlCache = new TTLCache<>(200L, -1, 50L);
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        // Verify entries are present initially.
        assertEquals(2, ttlCache.entrySet().size());
        // Wait for the TTL to expire.
        Thread.sleep(350L);
        // After expiration the entry set should be empty.
        assertEquals(0, ttlCache.entrySet().size());
        assertTrue(ttlCache.isEmpty());
    }

    /**
     * The entry set should be equal to another map's entry set when the mappings are identical.
     */
    @Test
    void entrySetEqualsOtherMapEntrySet() {
        ttlCache.put(5, "foo");
        ttlCache.put(6, "bar");
        Map<Integer, String> other = Map.of(5, "foo", 6, "bar");
        // The two entry sets must be equal.
        assertEquals(ttlCache.entrySet(), other.entrySet());
        // Changing the other map should break equality.
        Map<Integer, String> different = Map.of(5, "foo", 6, "baz");
        assertNotEquals(ttlCache.entrySet(), different.entrySet());
    }

    /**
     * Basic contract: entrySet() returns a non‑null Set, implements Set, and creates distinct objects.
     */
    @Test
    void entrySetBasicContract() {
        Set<Map.Entry<Integer, String>> first = ttlCache.entrySet();
        Set<Map.Entry<Integer, String>> second = ttlCache.entrySet();
        assertNotNull(first, "entrySet() must not return null");
        assertTrue(first instanceof Set, "Returned object must implement java.util.Set");
        assertNotSame(first, second, "Successive calls must return distinct Set instances");
    }

    /**
     * After a put, entrySet must contain the newly added entry.
     */
    @Test
    void entrySetContainsNewlyAddedEntry() {
        ttlCache.put(42, "Answer");
        Set<Map.Entry<Integer, String>> entrySet = ttlCache.entrySet();
        assertTrue(entrySet.contains(Map.entry(42, "Answer")), "entrySet should contain the entry that was just put into the cache");
    }

    /**
     * entrySet size reflects the number of put entries.
     */
    @Test
    void entrySetSizeUpdatesAfterPut() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.put(3, "C");
        assertEquals(3, ttlCache.entrySet().size(), "entrySet().size() must equal the number of distinct entries inserted");
    }

    /**
     * entrySet no longer contains a removed mapping.
     */
    @Test
    void entrySetDoesNotContainRemovedEntry() {
        ttlCache.put(7, "seven");
        Set<Map.Entry<Integer, String>> entrySet = ttlCache.entrySet();
        // direct removal from the cache
        ttlCache.remove(7);
        assertFalse(entrySet.contains(Map.entry(7, "seven")), "After removal via TTLCache.remove(), the entrySet view must no longer contain the entry");
    }

    /**
     * Removing an entry via Set.remove(entry) deletes the mapping from the underlying cache.
     */
    @Test
    void entrySetRemoveEntryViaSetRemove() {
        ttlCache.put(11, "eleven");
        ttlCache.put(12, "twelve");
        Set<Map.Entry<Integer, String>> entrySet = ttlCache.entrySet();
        boolean removed = entrySet.remove(Map.entry(11, "eleven"));
        assertTrue(removed, "Set.remove(entry) should return true for an existing entry");
        assertNull(ttlCache.get(11), "The mapping must be removed from the cache");
        assertEquals(1, ttlCache.size(), "Cache size must reflect the removal");
    }

    /**
     * entrySet.clear() empties the cache.
     */
    @Test
    void entrySetClearEmptiesCache() {
        ttlCache.put(20, "twenty");
        ttlCache.put(21, "twenty‑one");
        ttlCache.put(22, "twenty‑two");
        Set<Map.Entry<Integer, String>> entrySet = ttlCache.entrySet();
        entrySet.clear();
        assertTrue(ttlCache.isEmpty(), "Cache must be empty after entrySet.clear()");
        assertEquals(0, ttlCache.size(), "Cache size must be zero after entrySet.clear()");
    }

    /**
     * entrySet.removeAll(Collection) removes all specified entries.
     */
    @Test
    void entrySetRemoveAllSpecifiedEntries() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.put(3, "C");
        Set<Map.Entry<Integer, String>> entrySet = ttlCache.entrySet();
        Collection<Map.Entry<Integer, String>> toRemove = List.of(Map.entry(1, "A"), Map.entry(3, "C"));
        boolean changed = entrySet.removeAll(toRemove);
        assertTrue(changed, "removeAll should report that the set was modified");
        assertNull(ttlCache.get(1));
        assertNull(ttlCache.get(3));
        assertEquals("B", ttlCache.get(2));
        assertEquals(1, ttlCache.size());
    }

    /**
     * Iterator traverses all entries and yields the expected keys.
     */
    @Test
    void entrySetIteratorTraversesAllEntries() {
        Map<Integer, String> expected = Map.of(100, "hundred", 200, "two‑hundred", 300, "three‑hundred");
        expected.forEach(ttlCache::put);
        Set<Map.Entry<Integer, String>> entrySet = ttlCache.entrySet();
        Set<Integer> iteratedKeys = new HashSet<>();
        for (Map.Entry<Integer, String> e : entrySet) {
            iteratedKeys.add(e.getKey());
        }
        assertEquals(expected.keySet(), iteratedKeys, "Iterator must visit every key that was put into the cache");
    }

    /**
     * iterator.remove() deletes the current entry from the map.
     */
    @Test
    void entrySetIteratorRemoveDeletesEntry() {
        ttlCache.put(5, "five");
        ttlCache.put(6, "six");
        ttlCache.put(7, "seven");
        Iterator<Map.Entry<Integer, String>> it = ttlCache.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, String> e = it.next();
            if (e.getKey().equals(6)) {
                // remove key 6
                it.remove();
                break;
            }
        }
        assertNull(ttlCache.get(6), "Iterator.remove() must delete the entry from the underlying cache");
        assertEquals(2, ttlCache.size());
    }

    /**
     * The iterator does not support add operation and throws UnsupportedOperationException.
     */
    @Test
    void entrySetIteratorAddUnsupported() {
        Iterator<Map.Entry<Integer, String>> it = ttlCache.entrySet().iterator();
        // The Iterator returned by entrySet() is not a ListIterator; it has no add method.
        // Attempting to invoke add via reflection should result in an UnsupportedOperationException.
        assertThrows(UnsupportedOperationException.class, () -> {
            // Use reflection to find a method named "add" (if any) and invoke it.
            // This mimics a caller mistakenly assuming the iterator supports add().
            it.getClass().getMethod("add", Object.class).invoke(it, Map.entry(1, "X"));
        }, "Iterator.add() must throw UnsupportedOperationException");
    }

    /**
     * entrySet().size() is zero for a newly created cache.
     */
    @Test
    void entrySetSizeZeroForNewCache() {
        assertEquals(0, ttlCache.entrySet().size(), "A fresh cache must report size 0 via entrySet().size()");
    }

    /**
     * entrySet().isEmpty() returns true for an empty cache.
     */
    @Test
    void entrySetIsEmptyForEmptyCache() {
        assertTrue(ttlCache.entrySet().isEmpty(), "entrySet().isEmpty() must be true when the cache contains no entries");
    }

    /**
     * entrySet size reflects removal of entries via iterator.
     */
    @Test
    void entrySetSizeReflectsIteratorRemoval() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.put(3, "C");
        Iterator<Map.Entry<Integer, String>> it = ttlCache.entrySet().iterator();
        // Remove two entries via iterator.remove()
        int removed = 0;
        while (it.hasNext() && removed < 2) {
            it.next();
            it.remove();
            removed++;
        }
        assertEquals(1, ttlCache.entrySet().size(), "After removing two entries via iterator, entrySet size must be original size minus two");
    }

    /**
     * toArray() returns an array of Map.Entry objects of the correct length and content.
     */
    @Test
    void entrySetToArrayContent() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.put(3, "C");
        ttlCache.put(4, "D");
        Object[] array = ttlCache.entrySet().toArray();
        assertEquals(4, array.length, "toArray() must return an array whose length equals the number of entries");
        for (Object obj : array) {
            assertTrue(obj instanceof Map.Entry, "Each element of the array must be a Map.Entry");
        }
        // Verify that each expected entry is present.
        Set<Map.Entry<Integer, String>> expected = Set.of(Map.entry(1, "A"), Map.entry(2, "B"), Map.entry(3, "C"), Map.entry(4, "D"));
        Set<Map.Entry<Integer, String>> actual = new HashSet<>();
        for (Object obj : array) {
            @SuppressWarnings("unchecked")
            Map.Entry<Integer, String> e = (Map.Entry<Integer, String>) obj;
            actual.add(e);
        }
        assertEquals(expected, actual, "Array content must match the entries in the cache");
    }

    /**
     * toArray(T[]) returns a correctly typed array containing all entries.
     */
    @Test
    void entrySetToTypedArray() {
        ttlCache.put(10, "ten");
        ttlCache.put(20, "twenty");
        ttlCache.put(30, "thirty");
        @SuppressWarnings("unchecked")
        Map.Entry<Integer, String>[] typedArray = ttlCache.entrySet().toArray(new Map.Entry[0]);
        assertEquals(3, typedArray.length, "Typed toArray must have length equal to the number of entries");
        Set<Map.Entry<Integer, String>> expected = Set.of(Map.entry(10, "ten"), Map.entry(20, "twenty"), Map.entry(30, "thirty"));
        Set<Map.Entry<Integer, String>> actual = new HashSet<>(Arrays.asList(typedArray));
        assertEquals(expected, actual, "Typed array must contain exactly the entries present in the cache");
    }
}
