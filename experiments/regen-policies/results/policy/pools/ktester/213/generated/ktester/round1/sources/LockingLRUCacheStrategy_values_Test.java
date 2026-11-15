package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.locks.Lock;

@ExtendWith(MockitoExtension.class)
class LockingLRUCacheStrategy_values_Test {

    private static final int CAPACITY = 5;

    private LockingLRUCacheStrategy<Integer, String> cache;

    private static final int DEFAULT_CAPACITY = 5;

    class BadKey {

        @Override
        public int hashCode() {
            throw new RuntimeException("bad hashCode");
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }
    }

    class BadKey {

        @Override
        public int hashCode() {
            throw new IllegalArgumentException("illegal hash");
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }
    }

    class BadKey {

        @Override
        public int hashCode() {
            throw new RuntimeException("bad hashCode");
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }
    }

    class BadKey {

        @Override
        public int hashCode() {
            throw new IllegalArgumentException("illegal hash");
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh cache before each test to guarantee isolation.
        cache = new LockingLRUCacheStrategy<>(CAPACITY);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache is cleared so that no leftover state leaks to the next test.
        cache.clear();
    }

    /**
     * Uses reflection to set a private field (even if final) on the given target object.
     *
     * @param target    the object whose field should be modified
     * @param fieldName the name of the field
     * @param value     the new value
     * @throws Exception if reflection fails
     */
    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        // For final fields we need to remove the final modifier
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
        field.set(target, value);
    }

    @Test
    void values_ReturnsEmptyCollection_WhenCacheIsEmpty() {
        // Given: a newly created cache (already set up in @BeforeEach)
        // When
        Collection<String> values = cache.values();
        // Then
        assertNotNull(values, "values() should never return null");
        assertTrue(values.isEmpty(), "values() should be empty for an empty cache");
    }

    @Test
    void values_ReturnsValuesInInsertionOrder() {
        // Given: a cache populated with three entries
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        // When
        Collection<String> values = cache.values();
        // Then
        assertEquals(3, values.size(), "values() should contain exactly three elements");
        // Convert to a list to assert order
        List<String> actual = new ArrayList<>(values);
        List<String> expected = List.of("one", "two", "three");
        assertIterableEquals(expected, actual, "values() should preserve insertion order (LRU order for untouched entries)");
    }

    @Test
    void values_ReflectsLatestStateAfterUpdates() {
        // Given: a cache with two entries, then an update to an existing key
        cache.put(10, "ten");
        cache.put(20, "twenty");
        // update value for key 10
        cache.put(10, "TEN");
        // When
        Collection<String> values = cache.values();
        // Then
        // The map should now contain the updated value for key 10, order stays insertion‑based:
        // key 10 (updated) first, then key 20.
        List<String> actual = new ArrayList<>(values);
        List<String> expected = List.of("TEN", "twenty");
        assertIterableEquals(expected, actual, "values() must reflect the most recent value for a key while preserving order");
    }

    @Test
    void values_IsIndependentFromReturnedCollectionModification() {
        // Given
        cache.put(100, "hundred");
        cache.put(200, "two hundred");
        // When
        Collection<String> values = cache.values();
        // Attempt to modify the returned collection (should throw UnsupportedOperationException
        // because LinkedHashMap.values() returns a view that does not support add/remove)
        assertThrows(UnsupportedOperationException.class, () -> values.add("new value"));
        // Ensure the cache itself is unchanged
        assertEquals(2, cache.size());
        assertTrue(cache.containsKey(100));
        assertTrue(cache.containsKey(200));
    }

    @Test
    void values_SingleEntry_ReturnsThatValue() {
        cache.put(1, "A");
        Collection<String> values = cache.values();
        assertEquals(List.of("A"), new ArrayList<>(values));
    }

    @Test
    void values_ThreeEntries_ReflectsInternalLRUReordering() {
        // According to the description, after three puts the internal order becomes B, C, A
        // k1
        cache.put(1, "A");
        // k2
        cache.put(2, "B");
        // k3
        cache.put(3, "C");
        Collection<String> values = cache.values();
        // The implementation iterates from head.next to tail, which yields insertion order.
        // The expected order in the spec is B, C, A, which matches the LRU order after three puts.
        assertEquals(List.of("B", "C", "A"), new ArrayList<>(values));
    }

    @Test
    void values_SnapshotRemainsUnchangedAfterCacheMutation() {
        cache.put(1, "X");
        cache.put(2, "Y");
        // capture snapshot
        Collection<String> snapshot = cache.values();
        // mutate cache after snapshot
        cache.put(3, "Z");
        // Snapshot must stay the same
        assertEquals(List.of("X", "Y"), new ArrayList<>(snapshot));
    }

    @Test
    void values_PropagatesLockAcquisitionException() throws Exception {
        // Replace the internal lock with a mock that throws IllegalStateException on lock()
        Lock mockLock = mock(Lock.class);
        doThrow(new IllegalStateException("cannot acquire lock")).when(mockLock).lock();
        setPrivateField(cache, "lock", mockLock);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> cache.values());
        assertEquals("cannot acquire lock", ex.getMessage());
    }

    @Test
    void values_UpdateExistingKey_MovesEntryToMostRecent() {
        // k1
        cache.put(1, "A");
        // k2
        cache.put(2, "B");
        // update k1 -> "C", should become most recent
        cache.put(1, "C");
        Collection<String> values = cache.values();
        // Expected order: B (least recent), C (most recent)
        assertEquals(List.of("B", "C"), new ArrayList<>(values));
    }

    @Test
    void values_EvictionWhenCapacityExceeded() {
        LockingLRUCacheStrategy<Integer, String> smallCache = new LockingLRUCacheStrategy<>(2);
        smallCache.put(1, "A");
        smallCache.put(2, "B");
        // should evict key 1 ("A")
        smallCache.put(3, "C");
        Collection<String> values = smallCache.values();
        assertEquals(List.of("B", "C"), new ArrayList<>(values));
    }

    @Test
    void values_NullValueIsIncludedInResult() {
        cache.put(1, null);
        cache.put(2, "A");
        Collection<String> values = cache.values();
        assertEquals(Arrays.asList(null, "A"), new ArrayList<>(values));
    }

    @Test
    void values_IteratorRemoveThrowsUnsupportedOperationException() {
        cache.put(1, "one");
        cache.put(2, "two");
        Collection<String> values = cache.values();
        Iterator<String> it = values.iterator();
        assertTrue(it.hasNext());
        it.next();
        // The underlying view does not support removal.
        assertThrows(UnsupportedOperationException.class, it::remove);
    }

    @Test
    void values_OrderChangesAfterGetAccess() {
        // k1
        cache.put(1, "A");
        // k2
        cache.put(2, "B");
        // k3 => internal order B, C, A (as per spec)
        cache.put(3, "C");
        // Access k2 to make it most recent
        cache.get(2);
        Collection<String> values = cache.values();
        // Expected order after get: C (least), A, B (most recent)
        assertEquals(List.of("C", "A", "B"), new ArrayList<>(values));
    }

    @Test
    void values_EmptyAfterClear() {
        cache.put(1, "A");
        cache.put(2, "B");
        cache.clear();
        Collection<String> values = cache.values();
        assertTrue(values.isEmpty(), "values() should be empty after clear()");
    }

    @Test
    void values_NullHeadThrowsNullPointerException() throws Exception {
        // Set private field 'head' to null via reflection
        setPrivateField(cache, "head", null);
        assertThrows(NullPointerException.class, () -> cache.values());
    }

    @Test
    void values_NullTailThrowsNullPointerException() throws Exception {
        // Set private field 'tail' to null via reflection
        setPrivateField(cache, "tail", null);
        assertThrows(NullPointerException.class, () -> cache.values());
    }

    @Test
    void values_UnlockThrowsIllegalMonitorStateException() throws Exception {
        // Mock lock where lock() does nothing, unlock() throws IllegalMonitorStateException
        Lock mockLock = mock(Lock.class);
        doNothing().when(mockLock).lock();
        doThrow(new IllegalMonitorStateException("unlock without lock")).when(mockLock).unlock();
        setPrivateField(cache, "lock", mockLock);
        IllegalMonitorStateException ex = assertThrows(IllegalMonitorStateException.class, () -> cache.values());
        assertEquals("unlock without lock", ex.getMessage());
    }

    @Test
    void values_KeyHashCodeThrowsRuntimeException() {
        // Key that throws on hashCode()
        class BadKey {

            @Override
            public int hashCode() {
                throw new RuntimeException("bad hashCode");
            }

            @Override
            public boolean equals(Object o) {
                return this == o;
            }
        }
        BadKey badKey = new BadKey();
        cache.put(badKey, "value");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> cache.values());
        assertEquals("bad hashCode", ex.getMessage());
    }

    @Test
    void values_KeyHashCodeThrowsIllegalArgumentException() {
        class BadKey {

            @Override
            public int hashCode() {
                throw new IllegalArgumentException("illegal hash");
            }

            @Override
            public boolean equals(Object o) {
                return this == o;
            }
        }
        BadKey badKey = new BadKey();
        cache.put(badKey, "value");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> cache.values());
        assertEquals("illegal hash", ex.getMessage());
    }
}
