package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LockingLRUCacheStrategy_containsKey_Test {

    /**
     * Instance under test.
     */
    private LockingLRUCacheStrategy<String, Integer> cache;

    /**
     * Capacity used for all tests – small enough for readability.
     */
    private static final int CAPACITY = 5;

    /**
     * Simple wrapper whose equality is based on an internal id.
     */
    private static final class KeyWrapper {

        private final int id;

        KeyWrapper(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof KeyWrapper) && ((KeyWrapper) o).id == this.id;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(id);
        }
    }

    /**
     * Key whose hashCode throws a RuntimeException.
     */
    private static final class BadHashKey {

        @Override
        public int hashCode() {
            throw new RuntimeException("hashCode failure");
        }

        @Override
        public boolean equals(Object obj) {
            return false;
        }
    }

    /**
     * Key with constant hash to force map to check equals, but equals throws.
     */
    private static final class BadEqualsKey {

        @Override
        public int hashCode() {
            // constant hash
            return 42;
        }

        @Override
        public boolean equals(Object obj) {
            throw new RuntimeException("equals failure");
        }
    }

    /**
     * Key with constant hash (used to ensure equals is invoked).
     */
    private static final class ConstantHashKey {

        private final int id;

        ConstantHashKey(int id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            // constant hash for all instances
            return 42;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof ConstantHashKey) && ((ConstantHashKey) o).id == this.id;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh cache for each test to guarantee isolation.
        cache = new LockingLRUCacheStrategy<>(CAPACITY);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the cache to aid garbage collection; not strictly required.
        if (cache != null) {
            cache.clear();
        }
        cache = null;
    }

    private static Stream<Arguments> basicScenarioProvider() throws Exception {
        return Stream.of(// 1. empty cache, any key → false
        Arguments.of("Cache is empty; containsKey should return false for any key.", 42, (Runnable) () -> {
            /* no setup */
        }, false), // 2. after putting ("alpha","value1") → true
        Arguments.of("After putting entry (\"alpha\" -> \"value1\"), containsKey should return true for the stored key.", "alpha", (Runnable) () -> cache.put("alpha", 1), true), // 3. key never added → false
        Arguments.of("Key \"beta\" was never added; containsKey should return false.", "beta", (Runnable) () -> {
            /* no extra put */
        }, false), // 4. null key not present → false
        Arguments.of("Cache does not contain a null key; containsKey should return false when queried with null.", null, (Runnable) () -> {
            /* no null put */
        }, false), // 5. null key added → true
        Arguments.of("After putting a null key with a non‑null value, containsKey should return true for null.", null, (Runnable) () -> cache.put(null, 999), true), // 6. key added then removed → false
        Arguments.of("Key 100 was added then removed; containsKey should return false after removal.", 100, (Runnable) () -> {
            // cache is <String,Integer> – we use a string representation of 100
            cache.put("100", 1);
            cache.remove("100");
        }, false), // 7. wrong type key → false (no ClassCastException)
        Arguments.of("Cache is typed <String,Integer>; querying with a String key should safely return false (no ClassCastException).", // Integer key
        123, (Runnable) () -> cache.put("someKey", 1), false));
    }

    private static Stream<Arguments> additionalScenarioProvider() {
        return Stream.of(// 1. second key after two inserts → true
        Arguments.of("After inserting two distinct entries (\"gamma\"→\"v3\" and \"delta\"→\"v4\"), containsKey should return true for the second key.", "delta", (Runnable) () -> {
            cache.put("gamma", 3);
            cache.put("delta", 4);
        }, true), // 2. same key put twice with different values → true
        Arguments.of("When the same key is put twice with different values, the key remains present; containsKey must return true.", "epsilon", (Runnable) () -> {
            cache.put("epsilon", 5);
            cache.put("epsilon", 55);
        }, true), // 3. putAll adds keys → true
        Arguments.of("After calling putAll with a map that contains keys \"zeta\" and \"eta\", containsKey should return true for a key that was added via putAll.", "zeta", (Runnable) () -> {
            Map<String, Integer> m = new HashMap<>();
            m.put("zeta", 7);
            m.put("eta", 8);
            cache.putAll(m);
        }, true), // 4. after clear → false
        Arguments.of("After populating the cache with several entries and then invoking clear(), the cache becomes empty; containsKey must return false for any previously stored key.", "theta", (Runnable) () -> {
            cache.put("theta", 9);
            cache.put("phi", 10);
            cache.clear();
        }, false), // 5. non‑null key with null value → true for key, false for null key
        Arguments.of("A non‑null key is stored with a null value; the key is still present, so containsKey(\"iota\") should be true.", "iota", (Runnable) () -> cache.put("iota", null), true), // 6. custom key class with equal ids → true
        Arguments.of("Using a custom key class whose equals/hashCode are based on an internal id. After inserting new KeyWrapper(1), a different instance with the same id is queried; containsKey must return true because the keys are equal.", new KeyWrapper(1), (Runnable) () -> cache.put(new KeyWrapper(1), 42), true));
    }

    @Test
    void containsKey_returnsTrue_whenKeyIsPresent() {
        // given
        cache.put("one", 1);
        cache.put("two", 2);
        cache.put("three", 3);
        // when
        boolean result = cache.containsKey("two");
        // then
        assertTrue(result, "containsKey should return true for an existing key");
    }

    @Test
    void containsKey_returnsFalse_whenKeyIsAbsent() {
        // given
        cache.put("alpha", 10);
        cache.put("beta", 20);
        // when
        boolean result = cache.containsKey("gamma");
        // then
        assertFalse(result, "containsKey should return false for a missing key");
    }

    @Test
    void containsKey_handlesNullKey_whenNullKeyWasAdded() {
        // given
        cache.put(null, 999);
        // when
        boolean result = cache.containsKey(null);
        // then
        assertTrue(result, "containsKey should return true for a null key that was added");
    }

    @Test
    void containsKey_handlesNullKey_whenNullKeyWasNotAdded() {
        // given
        cache.put("nonNull", 1);
        // when
        boolean result = cache.containsKey(null);
        // then
        assertFalse(result, "containsKey should return false for a null key that was not added");
    }

    @Test
    void containsKey_isConsistentAfterEviction() {
        // given a cache with capacity 3; add 4 entries to trigger eviction of the least‑recently‑used
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3);
        // Access "a" to make it recently used
        cache.get("a");
        // Adding fourth entry should evict "b" (the LRU)
        cache.put("d", 4);
        // when
        boolean containsB = cache.containsKey("b");
        boolean containsA = cache.containsKey("a");
        boolean containsC = cache.containsKey("c");
        boolean containsD = cache.containsKey("d");
        // then
        assertFalse(containsB, "Key 'b' should have been evicted");
        assertTrue(containsA, "Key 'a' should still be present");
        assertTrue(containsC, "Key 'c' should still be present");
        assertTrue(containsD, "Key 'd' should be present");
    }

    /* --------------------------------------------------------------
     *  Basic scenarios taken from the JSON test description
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("basicScenarioProvider")
    void basicScenarios(String description, Object key, Runnable setup, boolean expected) {
        // Arrange
        setup.run();
        // Act
        boolean result = cache.containsKey(key);
        // Assert
        assertEquals(expected, result, description);
    }

    /* --------------------------------------------------------------
     *  Additional scenarios from the JSON description
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("additionalScenarioProvider")
    void additionalScenarios(String description, Object key, Runnable setup, boolean expected) {
        // Arrange
        setup.run();
        // Act
        boolean result = cache.containsKey(key);
        // Assert
        assertEquals(expected, result, description);
    }

    /* --------------------------------------------------------------
     *  Exception scenarios
     * -------------------------------------------------------------- */
    @Test
    void containsKey_propagatesNullPointerException_whenInternalMapRejectsNullKey() throws Exception {
        // Arrange: mock the internal ConcurrentHashMapNullSafe to throw NPE on containsKey(null)
        @SuppressWarnings("unchecked")
        ConcurrentHashMapNullSafe<Object, LockingLRUCacheStrategy.Node<String, Integer>> mockMap = mock(ConcurrentHashMapNullSafe.class);
        when(mockMap.containsKey(null)).thenThrow(new NullPointerException("null keys not allowed"));
        // inject mock
        Field cacheField = LockingLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(cache, mockMap);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> cache.containsKey(null), "NullPointerException should be propagated when internal map rejects null keys");
    }

    @Test
    void containsKey_propagatesNullPointerException_whenInternalCacheIsNull() throws Exception {
        // Arrange: set the internal cache field to null via reflection
        Field cacheField = LockingLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(cache, null);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> cache.containsKey("anyKey"), "NullPointerException should be thrown when internal cache field is null");
    }

    @Test
    void containsKey_propagatesRuntimeException_fromKeyHashCode() {
        // Arrange: a key whose hashCode throws
        BadHashKey badKey = new BadHashKey();
        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> cache.containsKey(badKey), "RuntimeException from key.hashCode() should propagate");
        assertEquals("hashCode failure", ex.getMessage());
    }

    @Test
    void containsKey_propagatesRuntimeException_fromKeyEquals() {
        // Arrange: put a normal key with constant hash to force equals call,
        // then query with a key whose equals throws.
        ConstantHashKey normalKey = new ConstantHashKey(1);
        // any entry; its hash is not used
        cache.put("normal", 1);
        BadEqualsKey badKey = new BadEqualsKey();
        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> cache.containsKey(badKey), "RuntimeException from key.equals() should propagate");
        assertEquals("equals failure", ex.getMessage());
    }

    @Test
    void containsKey_propagatesUnsupportedOperationException_fromCustomMap() throws Exception {
        // Arrange: custom map that throws UOE on containsKey
        @SuppressWarnings("unchecked")
        ConcurrentHashMapNullSafe<Object, LockingLRUCacheStrategy.Node<String, Integer>> throwingMap = mock(ConcurrentHashMapNullSafe.class);
        when(throwingMap.containsKey(any())).thenThrow(new UnsupportedOperationException("unsupported"));
        // inject
        Field cacheField = LockingLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(cache, throwingMap);
        // Act & Assert
        assertThrows(UnsupportedOperationException.class, () -> cache.containsKey("anyKey"), "UnsupportedOperationException should be forwarded from the underlying map");
    }
}
