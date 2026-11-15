package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;

@ExtendWith(MockitoExtension.class)
class LRUCache_entrySet_Test {

    private LRUCache<Integer, String> cache;

    /**
     * Creates a fresh LRUCache instance before each test. The default strategy
     * (LOCKING) is sufficient for exercising {@code entrySet()} because the
     * contract of {@code entrySet()} is independent of the eviction strategy.
     */
    @BeforeEach
    void setupBeforeEach() {
        cache = new LRUCache<>(5, LRUCache.StrategyType.LOCKING);
    }

    /**
     * Clears the cache after each test to avoid cross‑test contamination.
     */
    @AfterEach
    void teardownAfterEach() {
        if (cache != null) {
            cache.clear();
        }
    }

    /**
     * Shut down any thread pools created by the threaded strategy. This is safe
     * even when the tests use only the locking strategy.
     */
    @AfterAll
    static void teardownAfterAll() {
        ThreadedLRUCacheStrategy.shutdown();
    }

    private void replaceStrategyWithUnmodifiable() throws Exception {
        // Build a normal cache, then swap its strategy with an unmodifiable map.
        Map<Integer, String> backing = new HashMap<>();
        backing.put(1, "A");
        backing.put(2, "B");
        Map<Integer, String> unmodifiable = Collections.unmodifiableMap(backing);
        Field strategyField = LRUCache.class.getDeclaredField("strategy");
        strategyField.setAccessible(true);
        strategyField.set(cache, unmodifiable);
    }

    /**
     * Verify that {@code entrySet()} reflects the current contents of the cache.
     */
    @Test
    void testEntrySetReflectsCacheContents() {
        cache.put(1, "A");
        cache.put(2, "B");
        cache.put(3, "C");
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        assertEquals(3, entrySet.size(), "Entry set size should match number of entries");
        // Ensure each expected entry is present.
        assertTrue(entrySet.stream().anyMatch(e -> e.getKey().equals(1) && e.getValue().equals("A")));
        assertTrue(entrySet.stream().anyMatch(e -> e.getKey().equals(2) && e.getValue().equals("B")));
        assertTrue(entrySet.stream().anyMatch(e -> e.getKey().equals(3) && e.getValue().equals("C")));
    }

    /**
     * The {@code entrySet()} view must be live – changes to the cache after obtaining
     * the set should be visible in the set.
     */
    @Test
    void testEntrySetIsLiveView() {
        cache.put(1, "A");
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        // Add a new entry after the set was obtained.
        cache.put(2, "B");
        assertEquals(2, entrySet.size(), "Entry set should reflect newly added entry");
        assertTrue(entrySet.stream().anyMatch(e -> e.getKey().equals(2) && e.getValue().equals("B")));
    }

    /**
     * Removing an entry via the iterator of {@code entrySet()} must remove the
     * corresponding mapping from the cache.
     */
    @Test
    void testEntrySetRemovalAffectsCache() {
        cache.put(1, "A");
        cache.put(2, "B");
        cache.put(3, "C");
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        Iterator<Map.Entry<Integer, String>> it = entrySet.iterator();
        // Remove the entry with key 2 using the iterator.
        while (it.hasNext()) {
            Map.Entry<Integer, String> e = it.next();
            if (e.getKey().equals(2)) {
                it.remove();
                break;
            }
        }
        assertEquals(2, cache.size(), "Cache size should decrease after removal via entrySet");
        assertNull(cache.get(2), "Key 2 should no longer be present in the cache");
        assertTrue(cache.containsKey(1));
        assertTrue(cache.containsKey(3));
    }

    /**
     * The entry set must correctly represent entries with {@code null} keys
     * and/or values.
     */
    @Test
    void testEntrySetHandlesNullKeyAndValue() {
        cache.put(null, null);
        cache.put(1, null);
        cache.put(null, "value");
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        // Expect three distinct entries: (null,null), (1,null), (null,"value")
        assertEquals(3, entrySet.size());
        assertTrue(entrySet.stream().anyMatch(e -> e.getKey() == null && e.getValue() == null));
        assertTrue(entrySet.stream().anyMatch(e -> e.getKey() != null && e.getKey().equals(1) && e.getValue() == null));
        assertTrue(entrySet.stream().anyMatch(e -> e.getKey() == null && "value".equals(e.getValue())));
    }

    /**
     * Ensure that the {@code entrySet()} returned by the cache is the same
     * instance on successive calls (as required by {@link Map#entrySet()}).
     */
    @Test
    void testEntrySetIdentityConsistency() {
        Set<Map.Entry<Integer, String>> first = cache.entrySet();
        Set<Map.Entry<Integer, String>> second = cache.entrySet();
        assertSame(first, second, "Multiple calls to entrySet() should return the same Set instance");
    }

    @Test
    @DisplayName("entrySet() returns the exact Set instance from the underlying strategy")
    void testEntrySetDelegatesToStrategyAndIsSameInstance() throws Exception {
        // Create a real map, wrap it with a Mockito spy to observe delegation.
        Map<Integer, String> realMap = new HashMap<>();
        realMap.put(10, "X");
        realMap.put(20, "Y");
        @SuppressWarnings("unchecked")
        Map<Integer, String> spyMap = Mockito.spy(realMap);
        // Replace the private 'strategy' field with our spy.
        Field strategyField = LRUCache.class.getDeclaredField("strategy");
        strategyField.setAccessible(true);
        strategyField.set(cache, spyMap);
        // Call entrySet() on the cache.
        Set<Map.Entry<Integer, String>> returnedSet = cache.entrySet();
        // Verify that the returned set is exactly the one from the spy map.
        assertSame(spyMap.entrySet(), returnedSet, "entrySet() should return the exact Set instance from the underlying strategy");
        // Mutate the set and ensure the change is reflected in the cache.
        Iterator<Map.Entry<Integer, String>> it = returnedSet.iterator();
        // advance to first entry
        it.next();
        // remove via iterator
        it.remove();
        assertEquals(1, cache.size(), "Cache size must reflect removal performed on the entry set");
        assertFalse(cache.containsKey(10), "Key 10 should have been removed via the entry set iterator");
    }

    @Test
    @DisplayName("entrySet() is empty when the underlying strategy map is empty")
    void testEntrySetEmptyWhenStrategyEmpty() {
        // The cache is freshly created and empty.
        Set<Map.Entry<Integer, String>> set = cache.entrySet();
        assertTrue(set.isEmpty(), "Entry set should be empty for an empty underlying strategy");
    }

    @Test
    @DisplayName("entrySet() reflects direct modifications to the underlying strategy")
    void testEntrySetReflectsDirectStrategyChanges() throws Exception {
        // Populate cache normally.
        cache.put(1, "A");
        cache.put(2, "B");
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        // Directly modify the private strategy map via reflection.
        Field strategyField = LRUCache.class.getDeclaredField("strategy");
        strategyField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Integer, String> strategy = (Map<Integer, String>) strategyField.get(cache);
        // add new entry directly
        strategy.put(3, "C");
        // The previously obtained entrySet must now contain the new entry.
        assertEquals(3, entrySet.size(), "Live view must reflect direct changes to the underlying map");
        assertTrue(entrySet.stream().anyMatch(e -> e.getKey().equals(3) && e.getValue().equals("C")));
    }

    @Test
    @DisplayName("entrySet() throws NullPointerException when internal strategy is null")
    void testEntrySetThrowsWhenStrategyNull() throws Exception {
        // Force the private 'strategy' field to null.
        Field strategyField = LRUCache.class.getDeclaredField("strategy");
        strategyField.setAccessible(true);
        strategyField.set(cache, null);
        assertThrows(NullPointerException.class, () -> cache.entrySet(), "Calling entrySet() with a null strategy should throw NullPointerException");
    }

    @Test
    @DisplayName("Set.remove(entry) removes the mapping from the cache")
    void testEntrySetRemoveObjectSuccess() {
        cache.put(1, "A");
        cache.put(2, "B");
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        SimpleEntry<Integer, String> toRemove = new SimpleEntry<>(2, "B");
        boolean removed = entrySet.remove(toRemove);
        assertTrue(removed, "remove(existingEntry) must return true");
        assertEquals(1, cache.size(), "Cache size should decrease after successful removal via Set.remove");
        assertNull(cache.get(2), "Removed key must no longer be present in the cache");
    }

    @Test
    @DisplayName("Set.remove(entry) returns false for a non‑existent entry")
    void testEntrySetRemoveObjectFailure() {
        cache.put(1, "A");
        cache.put(2, "B");
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        SimpleEntry<Integer, String> nonExisting = new SimpleEntry<>(3, "C");
        boolean removed = entrySet.remove(nonExisting);
        assertFalse(removed, "remove(nonExistingEntry) must return false");
        assertEquals(2, cache.size(), "Cache size must remain unchanged after failed removal");
        assertTrue(cache.containsKey(1));
        assertTrue(cache.containsKey(2));
    }

    @Test
    @DisplayName("Calling setValue on an entry updates the cache")
    void testEntrySetSetValueUpdatesCache() {
        cache.put(1, "A");
        cache.put(2, "B");
        Iterator<Map.Entry<Integer, String>> it = cache.entrySet().iterator();
        // first entry (key 1)
        Map.Entry<Integer, String> entry = it.next();
        entry.setValue("Z");
        assertEquals("Z", cache.get(entry.getKey()), "Cache must reflect the new value after entry.setValue");
        // also verify the entry set reflects the change
        assertTrue(cache.entrySet().stream().anyMatch(e -> e.getKey().equals(entry.getKey()) && e.getValue().equals("Z")));
    }

    @Test
    @DisplayName("setValue(null) updates mapping to a null value while keeping the key")
    void testEntrySetSetValueToNull() {
        cache.put(1, "A");
        cache.put(2, "B");
        // Get entry for key 2
        Map.Entry<Integer, String> entry = cache.entrySet().stream().filter(e -> e.getKey().equals(2)).findFirst().orElseThrow();
        entry.setValue(null);
        assertTrue(cache.containsKey(2), "Key must still be present after setting value to null");
        assertNull(cache.get(2), "Value should become null after setValue(null)");
    }

    @Test
    @DisplayName("clear() on entrySet removes all entries from the cache")
    void testEntrySetClearRemovesAll() {
        cache.put(1, "A");
        cache.put(2, "B");
        cache.put(3, "C");
        cache.entrySet().clear();
        assertTrue(cache.isEmpty(), "Cache must be empty after entrySet().clear()");
        assertEquals(0, cache.entrySet().size(), "Entry set size must be zero after clear");
    }

    @Test
    @DisplayName("clear() on an already empty cache does not throw")
    void testEntrySetClearOnEmptyCache() {
        assertDoesNotThrow(() -> cache.entrySet().clear(), "Calling clear() on an empty entry set should not throw");
        assertTrue(cache.isEmpty(), "Cache remains empty after clear on empty entry set");
    }

    @Test
    @DisplayName("entrySet().contains works for existing and non‑existing entries")
    void testEntrySetContains() {
        cache.put(1, "A");
        cache.put(2, "B");
        SimpleEntry<Integer, String> existing = new SimpleEntry<>(1, "A");
        SimpleEntry<Integer, String> nonExisting = new SimpleEntry<>(3, "C");
        assertTrue(cache.entrySet().contains(existing), "contains(existingEntry) must be true");
        assertFalse(cache.entrySet().contains(nonExisting), "contains(nonExistingEntry) must be false");
    }

    @Test
    @DisplayName("entrySet().equals respects set equality irrespective of order")
    void testEntrySetEquals() {
        cache.put(1, "A");
        cache.put(2, "B");
        Set<Map.Entry<Integer, String>> other = new HashSet<>(cache.entrySet());
        assertEquals(other, cache.entrySet(), "Two sets containing the same entries must be equal");
    }

    @Test
    @DisplayName("entrySet().toArray returns an array of Map.Entry objects")
    void testEntrySetToArray() {
        cache.put(1, "A");
        cache.put(2, "B");
        cache.put(3, "C");
        Object[] array = cache.entrySet().toArray();
        assertEquals(3, array.length, "Array length must match cache size");
        for (Object obj : array) {
            assertTrue(obj instanceof Map.Entry, "Array elements must be Map.Entry instances");
            @SuppressWarnings("unchecked")
            Map.Entry<Integer, String> e = (Map.Entry<Integer, String>) obj;
            assertEquals(cache.get(e.getKey()), e.getValue(), "Entry key/value pair must match cache contents");
        }
    }

    @Test
    @DisplayName("entrySet().add throws UnsupportedOperationException on unmodifiable strategy")
    void testEntrySetAddUnsupported() throws Exception {
        replaceStrategyWithUnmodifiable();
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        SimpleEntry<Integer, String> newEntry = new SimpleEntry<>(3, "C");
        assertThrows(UnsupportedOperationException.class, () -> entrySet.add(newEntry), "add() must throw UnsupportedOperationException for unmodifiable strategy");
    }

    @Test
    @DisplayName("entrySet().addAll throws UnsupportedOperationException on unmodifiable strategy")
    void testEntrySetAddAllUnsupported() throws Exception {
        replaceStrategyWithUnmodifiable();
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        List<Map.Entry<Integer, String>> toAdd = List.of(new SimpleEntry<>(3, "C"));
        assertThrows(UnsupportedOperationException.class, () -> entrySet.addAll(toAdd), "addAll() must throw UnsupportedOperationException for unmodifiable strategy");
    }

    @Test
    @DisplayName("entrySet().remove throws UnsupportedOperationException on unmodifiable strategy")
    void testEntrySetRemoveUnsupported() throws Exception {
        replaceStrategyWithUnmodifiable();
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        SimpleEntry<Integer, String> existing = new SimpleEntry<>(1, "A");
        assertThrows(UnsupportedOperationException.class, () -> entrySet.remove(existing), "remove() must throw UnsupportedOperationException for unmodifiable strategy");
    }

    @Test
    @DisplayName("entrySet().removeAll throws UnsupportedOperationException on unmodifiable strategy")
    void testEntrySetRemoveAllUnsupported() throws Exception {
        replaceStrategyWithUnmodifiable();
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        List<Map.Entry<Integer, String>> toRemove = List.of(new SimpleEntry<>(1, "A"));
        assertThrows(UnsupportedOperationException.class, () -> entrySet.removeAll(toRemove), "removeAll() must throw UnsupportedOperationException for unmodifiable strategy");
    }

    @Test
    @DisplayName("entrySet().retainAll throws UnsupportedOperationException on unmodifiable strategy")
    void testEntrySetRetainAllUnsupported() throws Exception {
        replaceStrategyWithUnmodifiable();
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        List<Map.Entry<Integer, String>> toRetain = List.of(new SimpleEntry<>(1, "A"));
        assertThrows(UnsupportedOperationException.class, () -> entrySet.retainAll(toRetain), "retainAll() must throw UnsupportedOperationException for unmodifiable strategy");
    }

    @Test
    @DisplayName("entrySet().clear throws UnsupportedOperationException on unmodifiable strategy")
    void testEntrySetClearUnsupported() throws Exception {
        replaceStrategyWithUnmodifiable();
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        assertThrows(UnsupportedOperationException.class, entrySet::clear, "clear() must throw UnsupportedOperationException for unmodifiable strategy");
    }

    @Test
    @DisplayName("Calling iterator.remove() before next() throws IllegalStateException")
    void testIteratorRemoveWithoutNextThrows() {
        cache.put(1, "A");
        cache.put(2, "B");
        Iterator<Map.Entry<Integer, String>> it = cache.entrySet().iterator();
        assertThrows(IllegalStateException.class, it::remove, "remove() without a preceding next() must throw IllegalStateException");
        // Verify cache unchanged
        assertEquals(2, cache.size(), "Cache size must remain unchanged after illegal iterator.remove()");
    }
}
