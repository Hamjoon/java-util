package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@ExtendWith(MockitoExtension.class)
class LockingLRUCacheStrategy_entrySet_Test {

    private static final int CAPACITY = 10;

    private LockingLRUCacheStrategy<Integer, String> cache;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh cache for each test to avoid cross‑test contamination.
        cache = new LockingLRUCacheStrategy<>(CAPACITY);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the cache to aid garbage collection; not strictly required.
        cache.clear();
    }

    /**
     * Sets a private field on the given target object.
     *
     * @param target    the object whose field should be modified
     * @param fieldName the name of the field
     * @param value     the new value
     * @throws Exception if reflection fails
     */
    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field f = findField(target.getClass(), fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    /**
     * Retrieves the value of a private field from the given target object.
     *
     * @param target    the object to read from
     * @param fieldName the name of the field
     * @return the field value
     * @throws Exception if reflection fails
     */
    private static Object getPrivateField(Object target, String fieldName) throws Exception {
        Field f = findField(target.getClass(), fieldName);
        f.setAccessible(true);
        return f.get(target);
    }

    /**
     * Walks the class hierarchy to locate a declared field.
     *
     * @param clazz     the starting class
     * @param fieldName the field name
     * @return the {@link Field} object
     * @throws NoSuchFieldException if the field cannot be found
     */
    private static Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException("Field '" + fieldName + "' not found in class hierarchy of " + clazz);
    }

    @Test
    void entrySet_EmptyCache_ReturnsEmptySet() {
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        assertNotNull(entrySet, "entrySet() must never return null");
        assertTrue(entrySet.isEmpty(), "entrySet of an empty cache should be empty");
    }

    @Test
    void entrySet_WithEntries_ReturnsAllEntries() {
        // Populate the cache
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        // Build an expected map using the same insertion order
        Map<Integer, String> expected = new LinkedHashMap<>();
        expected.put(1, "one");
        expected.put(2, "two");
        expected.put(3, "three");
        Set<Map.Entry<Integer, String>> actualEntrySet = cache.entrySet();
        assertEquals(expected.entrySet(), actualEntrySet, "entrySet should contain exactly the entries that were put into the cache");
    }

    @Test
    void entrySet_IterationOrder_MatchesInsertionOrderWhenNoAccesses() {
        // Insert entries in a known order
        cache.put(10, "ten");
        cache.put(20, "twenty");
        cache.put(30, "thirty");
        // Capture the order produced by entrySet()
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        // Convert to a list to examine order
        Integer[] observedOrder = entrySet.stream().map(Map.Entry::getKey).toArray(Integer[]::new);
        Integer[] expectedOrder = new Integer[] { 10, 20, 30 };
        assertArrayEquals(expectedOrder, observedOrder, "When no explicit get() calls are made, the LRU order should be the same as insertion order");
    }

    @Test
    void entrySet_ModifyingReturnedSetDoesNotAffectCache() {
        cache.put(5, "five");
        cache.put(6, "six");
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        // Attempt to remove an entry via the returned set
        entrySet.removeIf(entry -> entry.getKey().equals(5));
        // The cache should still contain both entries because entrySet() returns a snapshot
        // of the internal state (a fresh LinkedHashMap each call).
        assertTrue(cache.containsKey(5), "Cache must still contain key 5 after modifying the snapshot set");
        assertTrue(cache.containsKey(6), "Cache must still contain key 6 after modifying the snapshot set");
    }

    @Test
    void entrySet_OrderAfterAccess_ReflectsLRUUpdate() {
        // Insert three entries in known order.
        cache.put(1, "a");
        cache.put(2, "b");
        cache.put(3, "c");
        // Access the first key to move it to the most‑recent position.
        cache.get(1);
        // Expected order: 2, 3, 1 (the accessed entry moves to the tail of the iteration)
        Integer[] expectedOrder = new Integer[] { 2, 3, 1 };
        Integer[] actualOrder = cache.entrySet().stream().map(Map.Entry::getKey).toArray(Integer[]::new);
        assertArrayEquals(expectedOrder, actualOrder, "After accessing key 1, entrySet order should reflect LRU update (2,3,1)");
    }

    @Test
    void entrySet_Modifications_AddEntryDoesNotAffectCache() {
        cache.put(1, "one");
        cache.put(2, "two");
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        // Add a new entry to the snapshot set.
        entrySet.add(new AbstractMap.SimpleEntry<>(3, "three"));
        // The original cache must remain unchanged.
        assertEquals(2, cache.size(), "Cache size must stay unchanged after adding to snapshot set");
        assertFalse(cache.containsKey(3), "Cache must not contain the newly added key from the snapshot");
    }

    @Test
    void entrySet_NullKeyAndNullValue_AreHandledCorrectly() {
        cache.put(null, "nullKey");
        cache.put(99, null);
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        assertEquals(2, entrySet.size(), "Entry set should contain both null key and null value entries");
        // Verify presence of the specific entries.
        boolean nullKeyFound = entrySet.stream().anyMatch(e -> e.getKey() == null && "nullKey".equals(e.getValue()));
        boolean nullValueFound = entrySet.stream().anyMatch(e -> Objects.equals(e.getKey(), 99) && e.getValue() == null);
        assertTrue(nullKeyFound, "Entry set must contain the (null=\"nullKey\") mapping");
        assertTrue(nullValueFound, "Entry set must contain the (99=null) mapping");
    }

    @Test
    void entrySet_ConcurrentAccessSafety_NoDeadlockAndCorrectResult() throws Exception {
        cache.put(42, "answer");
        Callable<Set<Map.Entry<Integer, String>>> task = () -> cache.entrySet();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Set<Map.Entry<Integer, String>>> f1 = executor.submit(task);
            Future<Set<Map.Entry<Integer, String>>> f2 = executor.submit(task);
            Set<Map.Entry<Integer, String>> set1 = f1.get(2, TimeUnit.SECONDS);
            Set<Map.Entry<Integer, String>> set2 = f2.get(2, TimeUnit.SECONDS);
            assertEquals(1, set1.size(), "First concurrent call should return a set of size 1");
            assertEquals(1, set2.size(), "Second concurrent call should return a set of size 1");
            Map.Entry<Integer, String> e1 = set1.iterator().next();
            Map.Entry<Integer, String> e2 = set2.iterator().next();
            assertEquals(42, e1.getKey());
            assertEquals("answer", e1.getValue());
            assertEquals(42, e2.getKey());
            assertEquals("answer", e2.getValue());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void entrySet_NullHead_ThrowsNullPointerException() throws Exception {
        // Corrupt the internal 'head' reference via reflection.
        setPrivateField(cache, "head", null);
        assertThrows(NullPointerException.class, () -> cache.entrySet(), "When head is null, entrySet() should throw NullPointerException");
    }

    @Test
    void entrySet_NullTail_ThrowsNullPointerException() throws Exception {
        // Corrupt the internal 'tail' reference via reflection.
        setPrivateField(cache, "tail", null);
        assertThrows(NullPointerException.class, () -> cache.entrySet(), "When tail is null, entrySet() should throw NullPointerException");
    }

    @Test
    void entrySet_BrokenLink_ThrowsNullPointerException() throws Exception {
        // Populate the cache with a couple of entries.
        cache.put(1, "one");
        cache.put(2, "two");
        // Obtain the first real node (head.next) via reflection.
        Object head = getPrivateField(cache, "head");
        Object firstNode = getPrivateField(head, "next");
        // Break the linked list by setting firstNode.next = null.
        setPrivateField(firstNode, "next", null);
        assertThrows(NullPointerException.class, () -> cache.entrySet(), "A broken link in the internal list should cause entrySet() to throw NullPointerException");
    }
}
