package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LockingLRUCacheStrategy_containsKey_Test {

    private LockingLRUCacheStrategy<String, Integer> cacheStrategy;

    /**
     * Simple key whose {@code hashCode()} always returns the same value,
     * forcing a collision in the underlying map.
     */
    private static final class CollidingKey {

        private final int id;

        CollidingKey(int id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            // constant hash to cause collisions
            return 42;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof CollidingKey))
                return false;
            return this.id == ((CollidingKey) o).id;
        }

        @Override
        public String toString() {
            // The cache is typed as <String, Integer>, so we store the string representation.
            return "CollidingKey-" + id;
        }
    }

    /**
     * Key whose {@code hashCode()} deliberately throws a {@code RuntimeException}.
     */
    private static final class BadHashKey {

        @Override
        public int hashCode() {
            throw new RuntimeException("Bad hashCode");
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }
    }

    /**
     * Key whose {@code equals()} deliberately throws a {@code RuntimeException}.
     */
    private static final class BadEqualsKey {

        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public boolean equals(Object o) {
            throw new RuntimeException("Bad equals");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a small capacity; the capacity does not affect the containsKey logic.
        cacheStrategy = new LockingLRUCacheStrategy<>(5);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference to help GC and ensure a clean state for the next test.
        cacheStrategy = null;
    }

    @Test
    @DisplayName("containsKey returns true for an existing key")
    void testContainsKeyWhenKeyExists() {
        // given
        cacheStrategy.put("alpha", 1);
        cacheStrategy.put("beta", 2);
        // when
        boolean result = cacheStrategy.containsKey("alpha");
        // then
        assertTrue(result, "containsKey should return true for a key that was put into the cache");
    }

    @Test
    @DisplayName("containsKey returns false for a missing key")
    void testContainsKeyWhenKeyDoesNotExist() {
        // given
        cacheStrategy.put("gamma", 3);
        // when
        boolean result = cacheStrategy.containsKey("delta");
        // then
        assertFalse(result, "containsKey should return false for a key that has never been added");
    }

    @Test
    @DisplayName("containsKey works with null keys")
    void testContainsKeyWithNullKey() {
        // given
        cacheStrategy.put(null, 99);
        // when / then
        assertTrue(cacheStrategy.containsKey(null), "containsKey should return true for a null key that was added");
        // when removing the null key
        cacheStrategy.remove(null);
        // then
        assertFalse(cacheStrategy.containsKey(null), "containsKey should return false after the null key has been removed");
    }

    @Test
    @DisplayName("containsKey returns false after a key is removed")
    void testContainsKeyAfterRemoval() {
        // given
        cacheStrategy.put("key", 10);
        assertTrue(cacheStrategy.containsKey("key"), "pre‑condition: key must be present before removal");
        // when
        cacheStrategy.remove("key");
        // then
        assertFalse(cacheStrategy.containsKey("key"), "containsKey should return false after the key has been removed");
    }

    @Test
    @DisplayName("containsKey returns false for a key of a different type")
    void testContainsKeyDifferentTypeKey() {
        cacheStrategy.put("stringKey", 42);
        assertFalse(cacheStrategy.containsKey(123), "Cache holds String keys; querying with an Integer should return false");
    }

    @Test
    @DisplayName("containsKey works with equal but different String instance")
    void testContainsKeyEqualButDifferentInstance() {
        cacheStrategy.put("alpha", 1);
        // different object, same logical value
        String anotherAlpha = new String("alpha");
        assertTrue(cacheStrategy.containsKey(anotherAlpha), "containsKey should use logical equality, not reference equality");
    }

    @Test
    @DisplayName("containsKey does not modify cache size")
    void testContainsKeySizeUnchangedAfterQuery() {
        cacheStrategy.put("theta", 7);
        cacheStrategy.put("iota", 8);
        int sizeBefore = cacheStrategy.size();
        assertTrue(cacheStrategy.containsKey("theta"), "containsKey should return true for an existing key");
        assertEquals(sizeBefore, cacheStrategy.size(), "Cache size must remain unchanged after a containsKey query");
    }

    @Test
    @DisplayName("containsKey returns false after clear()")
    void testContainsKeyAfterClear() {
        cacheStrategy.put("delta", 4);
        cacheStrategy.clear();
        assertFalse(cacheStrategy.containsKey("delta"), "After clear(), the cache must be empty");
    }

    @Test
    @DisplayName("containsKey works after putAll()")
    void testContainsKeyAfterPutAll() {
        Map<String, Integer> source = new HashMap<>();
        source.put("epsilon", 5);
        source.put("zeta", 6);
        cacheStrategy.putAll(source);
        assertTrue(cacheStrategy.containsKey("epsilon"), "containsKey should reflect entries added via putAll");
    }

    @Test
    @DisplayName("containsKey succeeds for colliding key that is present")
    void testContainsKeyCollidingKeyPresent() {
        CollidingKey key1 = new CollidingKey(1);
        CollidingKey key2 = new CollidingKey(2);
        // use toString() as map key type (String)
        cacheStrategy.put(key1.toString(), 100);
        cacheStrategy.put(key2.toString(), 200);
        // Query using a new instance that is equal to key1
        CollidingKey query = new CollidingKey(1);
        assertTrue(cacheStrategy.containsKey(query.toString()), "containsKey must locate the entry despite hash collisions");
    }

    @Test
    @DisplayName("containsKey returns false for colliding key that is absent")
    void testContainsKeyCollidingKeyAbsent() {
        CollidingKey key1 = new CollidingKey(1);
        CollidingKey key2 = new CollidingKey(2);
        cacheStrategy.put(key1.toString(), 100);
        cacheStrategy.put(key2.toString(), 200);
        // same hash, different logical key
        CollidingKey query = new CollidingKey(3);
        assertFalse(cacheStrategy.containsKey(query.toString()), "containsKey must return false when colliding key is not present");
    }

    @Test
    @DisplayName("containsKey propagates RuntimeException from key.hashCode()")
    void testContainsKeyBadHashKeyException() {
        BadHashKey badKey = new BadHashKey();
        assertThrows(RuntimeException.class, () -> cacheStrategy.containsKey(badKey), "Exception from hashCode() should propagate out of containsKey");
    }

    @Test
    @DisplayName("containsKey propagates RuntimeException from key.equals()")
    void testContainsKeyBadEqualsKeyException() {
        cacheStrategy.put("normal", 1);
        BadEqualsKey badKey = new BadEqualsKey();
        assertThrows(RuntimeException.class, () -> cacheStrategy.containsKey(badKey), "Exception from equals() should propagate out of containsKey");
    }
}
