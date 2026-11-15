package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ExtendWith(MockitoExtension.class)
class ThreadedLRUCacheStrategy_remove_Test {

    /**
     * Cache instance used in each test.
     */
    private ThreadedLRUCacheStrategy<Integer, String> cache;

    /**
     * Key implementation whose {@code equals} method always throws a {@link RuntimeException}.
     * The {@code hashCode} is constant so that the map will locate the bucket and invoke {@code equals}.
     */
    private static final class FaultyKey {

        @Override
        public int hashCode() {
            // constant hash to ensure bucket match
            return 1234;
        }

        @Override
        public boolean equals(Object obj) {
            throw new RuntimeException("FaultyKey equals invoked");
        }
    }

    /**
     * Creates a fresh cache before each test. The capacity and cleanup delay are
     * chosen arbitrarily – they only need to satisfy the constructor’s validation.
     */
    @BeforeEach
    void setupBeforeEach() {
        cache = new ThreadedLRUCacheStrategy<>(5, 50);
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
     * Shuts down the shared scheduler used by {@link ThreadedLRUCacheStrategy}
     * after all tests have run.
     */
    @AfterAll
    static void teardownAfterAll() {
        ThreadedLRUCacheStrategy.shutdown();
    }

    /**
     * Verify that removing an existing key returns the associated value and
     * actually removes the entry from the cache.
     */
    @Test
    void removeExistingKey_returnsValueAndRemovesEntry() {
        // given
        cache.put(1, "one");
        assertTrue(cache.containsKey(1), "Pre‑condition: key must be present");
        // when
        String removed = cache.remove(1);
        // then
        assertEquals("one", removed, "remove should return the stored value");
        assertFalse(cache.containsKey(1), "Key should no longer be present after removal");
        assertNull(cache.get(1), "Cache.get should return null for a removed key");
    }

    /**
     * Verify that attempting to remove a key that is not present returns {@code null}
     * and leaves the cache unchanged.
     */
    @Test
    void removeMissingKey_returnsNull() {
        // given
        cache.put(10, "ten");
        assertTrue(cache.containsKey(10));
        // when
        // 99 was never added
        String result = cache.remove(99);
        // then
        assertNull(result, "remove of a non‑existent key must return null");
        assertTrue(cache.containsKey(10), "Existing entries must remain untouched");
        assertEquals("ten", cache.get(10));
    }

    /**
     * Verify that {@code null} keys are supported – they can be inserted and removed.
     */
    @Test
    void removeNullKey_behavesCorrectly() {
        // given
        cache.put(null, "nullValue");
        assertTrue(cache.containsKey(null));
        // when
        String removed = cache.remove(null);
        // then
        assertEquals("nullValue", removed, "remove should return the value associated with null key");
        assertFalse(cache.containsKey(null), "Null key should be removed");
    }

    /**
     * Verify that removing a null key when it has never been added returns {@code null}.
     */
    @Test
    void removeNullKey_whenNotPresent_returnsNull() {
        assertFalse(cache.containsKey(null), "Pre‑condition: null key must not be present");
        assertNull(cache.remove(null), "Removing a non‑existent null key should return null");
    }

    /**
     * Removing a key that was never added should return {@code null} without side effects.
     */
    @Test
    void removeNeverAddedKey_returnsNull() {
        assertNull(cache.remove("gamma"));
        assertTrue(cache.isEmpty(), "Cache should still be empty");
    }

    /**
     * Remove a key, then verify a second removal returns {@code null}.
     */
    @Test
    void removeKeyThenRemoveAgain_returnsNullSecondTime() {
        cache.put("delta", "DeltaValue");
        assertEquals("DeltaValue", cache.remove("delta"));
        assertNull(cache.remove("delta"), "Second removal must return null");
    }

    /**
     * Remove a key whose associated value is {@code null}.
     */
    @Test
    void removeKeyWithNullValue_returnsNullButRemovesEntry() {
        cache.put("epsilon", null);
        assertTrue(cache.containsKey("epsilon"));
        assertNull(cache.remove("epsilon"), "Value stored was null, so remove should return null");
        assertFalse(cache.containsKey("epsilon"), "Entry must be removed even if its value was null");
    }

    /**
     * Remove a key while other entries exist; verify size decrement.
     */
    @Test
    void removeKeyWithOtherEntries_sizeDecrements() {
        cache.put("zeta", "ZetaValue");
        cache.put("other1", "value1");
        cache.put("other2", "value2");
        int sizeBefore = cache.size();
        assertEquals("ZetaValue", cache.remove("zeta"));
        assertEquals(sizeBefore - 1, cache.size(), "Cache size should decrement by one after removal");
    }

    /**
     * Remove a key whose value is a custom object; ensure the exact reference is returned.
     */
    @Test
    void removeKeyWithCustomObjectValue_returnsSameReference() {
        Object customValue = new Object();
        cache.put("eta", customValue);
        Object removed = cache.remove("eta");
        assertSame(customValue, removed, "Returned value must be the exact same object reference");
    }

    /**
     * Simulate a broken internal cache (null) and expect a NullPointerException.
     */
    @Test
    void removeWhenInternalCacheIsNull_throwsNPE() throws Exception {
        // Use reflection to nullify the private 'cache' field.
        Field cacheField = ThreadedLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(cache, null);
        assertThrows(NullPointerException.class, () -> cache.remove("anyKey"), "When internal cache is null, remove should throw NPE");
    }

    /**
     * Use an unmodifiable map as the backing cache; remove should propagate UnsupportedOperationException.
     */
    @Test
    void removeWhenBackingMapIsUnmodifiable_throwsUOE() throws Exception {
        // Create an unmodifiable map and inject it via reflection.
        Map<Integer, ThreadedLRUCacheStrategy.Node<Integer, String>> unmodifiable = Collections.unmodifiableMap(new ConcurrentHashMap<>());
        Field cacheField = ThreadedLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(cache, unmodifiable);
        assertThrows(UnsupportedOperationException.class, () -> cache.remove(1), "Removing from an unmodifiable map must propagate UnsupportedOperationException");
    }

    /**
     * A key whose equals() throws a RuntimeException should cause that exception to propagate.
     */
    @Test
    void removeKeyWithFaultyEquals_throwsRuntimeException() {
        // Insert a normal entry so that the map will attempt to compare keys during removal.
        cache.put(42, "answer");
        FaultyKey faultyKey = new FaultyKey();
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> cache.remove(faultyKey), "Exception from key.equals() must propagate");
        assertEquals("FaultyKey equals invoked", thrown.getMessage());
    }
}
