package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LockingLRUCacheStrategy_keySet_Test {

    private static final int CAPACITY = 10;

    private LockingLRUCacheStrategy<Integer, String> cache;

    // ---------- CustomKeyObjects ----------
    static class CustomKey {

        private final String id;

        CustomKey(String id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            // all instances are considered equal
            return o instanceof CustomKey;
        }

        @Override
        public int hashCode() {
            // constant hash code to satisfy contract
            return 42;
        }
    }

    // ---------- KeyHashCodeFailure ----------
    static class BadHashKey {

        private final String name;

        BadHashKey(String name) {
            this.name = name;
        }

        @Override
        public int hashCode() {
            throw new RuntimeException("Deliberate hashCode failure");
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof BadHashKey && ((BadHashKey) o).name.equals(this.name);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh cache instance for each test to avoid cross‑test contamination.
        cache = new LockingLRUCacheStrategy<>(CAPACITY);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the cache to ensure any internal structures are released.
        cache.clear();
    }

    // Example of a method source for future expansion:
    static Stream<Arguments> dummyProvider() {
        return Stream.empty();
    }

    @Test
    void keySet_ShouldReturnEmptySet_WhenCacheIsEmpty() {
        // Given: a newly created cache (already set up in @BeforeEach)
        // When
        Set<Integer> keys = cache.keySet();
        // Then
        assertNotNull(keys, "keySet() must never return null");
        assertTrue(keys.isEmpty(), "keySet() should be empty for an empty cache");
    }

    @Test
    void keySet_ShouldReturnAllKeys_InsertionOrder() {
        // Given
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        // When
        Set<Integer> keys = cache.keySet();
        // Then
        assertEquals(3, keys.size(), "keySet() should contain exactly three keys");
        assertTrue(keys.contains(1), "keySet() should contain key 1");
        assertTrue(keys.contains(2), "keySet() should contain key 2");
        assertTrue(keys.contains(3), "keySet() should contain key 3");
    }

    @Test
    void keySet_ShouldReflectRemovals() {
        // Given
        cache.put(10, "ten");
        cache.put(20, "twenty");
        cache.put(30, "thirty");
        // remove one entry
        cache.remove(20);
        // When
        Set<Integer> keys = cache.keySet();
        // Then
        assertEquals(2, keys.size(), "After removal, keySet() should contain two keys");
        assertTrue(keys.contains(10), "keySet() should still contain key 10");
        assertTrue(keys.contains(30), "keySet() should still contain key 30");
        assertFalse(keys.contains(20), "keySet() should not contain the removed key 20");
    }

    @Test
    void keySet_ShouldHandleNullKeyAndValue() {
        // Given
        cache.put(null, null);
        cache.put(5, null);
        // overwrites previous null key entry
        cache.put(null, "valueForNullKey");
        // When
        Set<Integer> keys = cache.keySet();
        // Then
        assertEquals(2, keys.size(), "Cache should have two distinct keys (null and 5)");
        assertTrue(keys.contains(null), "keySet() should contain the null key");
        assertTrue(keys.contains(5), "keySet() should contain key 5");
    }

    // ---------- EmptyCache ----------
    @Test
    void emptyCache_ReturnsEmptySet() {
        Set<Integer> keys = cache.keySet();
        assertNotNull(keys);
        assertTrue(keys.isEmpty(), "When the cache is newly created it must return an empty Set");
    }

    // ---------- SingleEntry ----------
    @Test
    void singleEntry_ReturnsExactlyThatKey() {
        // using Integer key derived from string hash for simplicity
        cache.put("alpha".hashCode(), "alpha");
        Set<Integer> keys = cache.keySet();
        assertEquals(1, keys.size());
        assertTrue(keys.contains("alpha".hashCode()));
    }

    // ---------- MultipleEntries ----------
    @Test
    void multipleEntries_ReturnAllKeysPreserveInsertionOrder() {
        cache.put("first".hashCode(), "first");
        cache.put("second".hashCode(), "second");
        cache.put("third".hashCode(), "third");
        Set<Integer> keys = cache.keySet();
        List<Integer> expected = Arrays.asList("first".hashCode(), "second".hashCode(), "third".hashCode());
        assertEquals(expected, new ArrayList<>(keys), "Insertion order should be preserved");
    }

    @Test
    void multipleEntries_AccessDoesNotChangeKeySetOrder() {
        cache.put("first".hashCode(), "first");
        cache.put("second".hashCode(), "second");
        cache.put("third".hashCode(), "third");
        // Access "second" – LRU move does not affect keySet order
        cache.get("second".hashCode());
        Set<Integer> keys = cache.keySet();
        List<Integer> expected = Arrays.asList("first".hashCode(), "second".hashCode(), "third".hashCode());
        assertEquals(expected, new ArrayList<>(keys));
    }

    // ---------- ReturnedSetIndependence ----------
    @Test
    void returnedSet_IsIndependentFromCache() {
        cache.put("x".hashCode(), "x");
        cache.put("y".hashCode(), "y");
        Set<Integer> keys = cache.keySet();
        // Attempt to modify the returned set
        assertThrows(UnsupportedOperationException.class, () -> keys.remove("x".hashCode()));
        // Verify cache still contains both keys
        Set<Integer> keysAfter = cache.keySet();
        assertTrue(keysAfter.containsAll(Arrays.asList("x".hashCode(), "y".hashCode())));
    }

    // ---------- NullKeyAndValueHandling ----------
    @Test
    void nullKey_IncludedInKeySet() {
        cache.put(null, "nullVal");
        Set<Integer> keys = cache.keySet();
        assertEquals(1, keys.size());
        assertTrue(keys.contains(null));
    }

    @Test
    void nullValue_KeyStillPresent() {
        cache.put("k1".hashCode(), null);
        Set<Integer> keys = cache.keySet();
        assertEquals(1, keys.size());
        assertTrue(keys.contains("k1".hashCode()));
    }

    // ---------- CacheClear ----------
    @Test
    void clear_EmptiesKeySet() {
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        cache.clear();
        Set<Integer> keys = cache.keySet();
        assertTrue(keys.isEmpty(), "After clear() the key set must be empty");
    }

    // ---------- UpdateExistingKey ----------
    @Test
    void updateExistingKey_DoesNotDuplicateKey() {
        cache.put("dup".hashCode(), "first");
        // same logical key
        cache.put("dup".hashCode(), "second");
        Set<Integer> keys = cache.keySet();
        assertEquals(1, keys.size());
        assertTrue(keys.contains("dup".hashCode()));
    }

    // ---------- PutAllEntries ----------
    @Test
    void putAll_InsertsAllKeys() {
        Map<Integer, String> source = new LinkedHashMap<>();
        source.put("a".hashCode(), "a");
        source.put("b".hashCode(), "b");
        source.put("c".hashCode(), "c");
        source.put("d".hashCode(), "d");
        cache.putAll(source);
        Set<Integer> keys = cache.keySet();
        assertEquals(source.keySet(), keys);
    }

    // ---------- RemovalEffect ----------
    @Test
    void removalEffect_OnlyRemainingKeyIsReturned() {
        cache.put("keep".hashCode(), "keep");
        cache.put("remove".hashCode(), "remove");
        cache.remove("remove".hashCode());
        Set<Integer> keys = cache.keySet();
        assertEquals(1, keys.size());
        assertTrue(keys.contains("keep".hashCode()));
    }

    @Test
    void customKeyObjects_LogicalDuplicationHandled() {
        CustomKey k1 = new CustomKey("first");
        CustomKey k2 = new CustomKey("second");
        cache.put(k1, "value1");
        // same logical key according to equals()
        cache.put(k2, "value2");
        Set<CustomKey> keys = cache.keySet();
        assertEquals(1, keys.size());
        assertTrue(keys.contains(k1));
    }

    // ---------- LargeNumberOfEntries ----------
    @Test
    void largeNumberOfEntries_KeySetSizeAndBoundaries() {
        int count = 1000;
        for (int i = 0; i < count; i++) {
            cache.put(i, "v" + i);
        }
        Set<Integer> keys = cache.keySet();
        assertEquals(count, keys.size());
        assertTrue(keys.contains(0));
        assertTrue(keys.contains(count - 1));
    }

    // ---------- ReturnedSetIndependenceAfterModification ----------
    @Test
    void returnedSet_ModificationDoesNotAffectCache() {
        cache.put("p".hashCode(), "p");
        cache.put("q".hashCode(), "q");
        Set<Integer> first = cache.keySet();
        // Attempt to clear the returned set (it is an unmodifiable view)
        assertThrows(UnsupportedOperationException.class, first::clear);
        // Second call must still return both keys
        Set<Integer> second = cache.keySet();
        assertEquals(2, second.size());
        assertTrue(second.containsAll(Arrays.asList("p".hashCode(), "q".hashCode())));
    }

    // ---------- CorruptedInternalStructure ----------
    @Test
    void corruptedHead_NullPointerException() throws Exception {
        // head set to null via reflection
        Field headField = LockingLRUCacheStrategy.class.getDeclaredField("head");
        headField.setAccessible(true);
        headField.set(cache, null);
        assertThrows(NullPointerException.class, () -> cache.keySet());
    }

    @Test
    void corruptedHeadNext_NullPointerException() throws Exception {
        // Ensure head exists but its next is null
        Field headField = LockingLRUCacheStrategy.class.getDeclaredField("head");
        headField.setAccessible(true);
        Object head = headField.get(cache);
        Field nextField = head.getClass().getDeclaredField("next");
        nextField.setAccessible(true);
        nextField.set(head, null);
        assertThrows(NullPointerException.class, () -> cache.keySet());
    }

    @Test
    void corruptedTail_NullPointerException() throws Exception {
        // tail set to null via reflection
        Field tailField = LockingLRUCacheStrategy.class.getDeclaredField("tail");
        tailField.setAccessible(true);
        tailField.set(cache, null);
        assertThrows(NullPointerException.class, () -> cache.keySet());
    }

    @Test
    void keyHashCodeFailure_PropagatesRuntimeException() {
        BadHashKey badKey = new BadHashKey("boom");
        cache.put(badKey, "value");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> cache.keySet(), "Exception from key.hashCode() should propagate");
        assertEquals("Deliberate hashCode failure", ex.getMessage());
    }

    // ---------- LockUnlockMisuse ----------
    @Test
    void lockUnlockMisuse_IllegalMonitorStateException() throws Exception {
        // Create a mock Lock that throws when unlock() is called
        Lock mockLock = mock(Lock.class);
        doNothing().when(mockLock).lock();
        doThrow(new IllegalMonitorStateException("unlock without lock")).when(mockLock).unlock();
        // Inject the mock into the cache via reflection
        Field lockField = LockingLRUCacheStrategy.class.getDeclaredField("lock");
        lockField.setAccessible(true);
        lockField.set(cache, mockLock);
        // The method should attempt to unlock in the finally block, causing the exception
        IllegalMonitorStateException thrown = assertThrows(IllegalMonitorStateException.class, () -> cache.keySet(), "unlock() failure must be observed");
        assertEquals("unlock without lock", thrown.getMessage());
    }
}
