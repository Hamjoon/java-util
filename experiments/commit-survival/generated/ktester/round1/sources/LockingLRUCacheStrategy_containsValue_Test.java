package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

class LockingLRUCacheStrategy_containsValue_Test {

    private LockingLRUCacheStrategy<String, String> cache;

    /**
     * Simple factory used by the parameterised tests to create a cache
     * pre‑populated with the required entries.
     */
    @FunctionalInterface
    private interface CacheSetup {

        LockingLRUCacheStrategy<String, Object> create();
    }

    /**
     * Simple value object whose {@code equals} method is based on an {@code int} field.
     */
    private static final class EqObj {

        private final int id;

        EqObj(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof EqObj))
                return false;
            return id == ((EqObj) o).id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "EqObj{" + id + '}';
        }
    }

    /**
     * Value object whose {@code equals} method deliberately throws a
     * {@link RuntimeException}. Used to verify that {@code containsValue}
     * does not swallow such exceptions.
     */
    private static final class BadEquals {

        @Override
        public boolean equals(Object o) {
            throw new RuntimeException("equals is broken");
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // The default cache used by the simple tests in the original scaffold.
        cache = new LockingLRUCacheStrategy<>(3);
        cache.put("k1", "apple");
        cache.put("k2", "banana");
        // test handling of null values
        cache.put("k3", null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference for GC; the cache does not hold external resources
        cache = null;
    }

    /**
     * Supplies the normal test cases extracted from the JSON description.
     */
    private static Stream<Arguments> provideNormalCases() {
        return Stream.of(// 1. empty cache – any value → false
        Arguments.of("Cache is empty; containsValue should return false for any searched value.", (CacheSetup) () -> new LockingLRUCacheStrategy<>(2), "anyString", false), // 2. single entry – exact match → true
        Arguments.of("Cache contains a single entry (key=1, value=\"apple\"); searching for the exact value returns true.", (CacheSetup) () -> {
            LockingLRUCacheStrategy<String, Object> c = new LockingLRUCacheStrategy<>(2);
            c.put("1", "apple");
            return c;
        }, "apple", true), // 3. single entry – different value → false
        Arguments.of("Cache contains a single entry (key=1, value=\"apple\"); searching for a different value returns false.", (CacheSetup) () -> {
            LockingLRUCacheStrategy<String, Object> c = new LockingLRUCacheStrategy<>(2);
            c.put("1", "apple");
            return c;
        }, "banana", false), // 4. first node match → true
        Arguments.of("Cache has multiple entries; the first node after head holds value \"first\".", (CacheSetup) () -> {
            LockingLRUCacheStrategy<String, Object> c = new LockingLRUCacheStrategy<>(5);
            c.put("a", "first");
            c.put("b", "middle");
            c.put("c", "last");
            return c;
        }, "first", true), // 5. middle node match → true
        Arguments.of("Cache has several entries; the matching value \"middle\" is located in a middle node.", (CacheSetup) () -> {
            LockingLRUCacheStrategy<String, Object> c = new LockingLRUCacheStrategy<>(5);
            c.put("a", "first");
            c.put("b", "middle");
            c.put("c", "last");
            return c;
        }, "middle", true), // 6. last node match → true
        Arguments.of("Cache has several entries; the matching value \"last\" is stored in the node just before the tail sentinel.", (CacheSetup) () -> {
            LockingLRUCacheStrategy<String, Object> c = new LockingLRUCacheStrategy<>(5);
            c.put("a", "first");
            c.put("b", "middle");
            c.put("c", "last");
            return c;
        }, "last", true), // 7. stored null value → true
        Arguments.of("Cache contains a null value (e.g., key=2, value=null). Searching for null should succeed, returning true.", (CacheSetup) () -> {
            LockingLRUCacheStrategy<String, Object> c = new LockingLRUCacheStrategy<>(3);
            c.put("x", "foo");
            c.put("y", null);
            return c;
        }, null, true), // 8. null stored, non‑null search → false
        Arguments.of("Cache contains a null value but the searched value is non‑null; method should continue iterating and finally return false.", (CacheSetup) () -> {
            LockingLRUCacheStrategy<String, Object> c = new LockingLRUCacheStrategy<>(3);
            c.put("x", "foo");
            c.put("y", null);
            return c;
        }, "nonNull", false), // 9. no null entries, search null → false
        Arguments.of("Cache has no null entries; searching for null should traverse the whole list and return false.", (CacheSetup) () -> {
            LockingLRUCacheStrategy<String, Object> c = new LockingLRUCacheStrategy<>(2);
            c.put("a", "a1");
            c.put("b", "b1");
            return c;
        }, null, false), // 10. duplicate values → true (first match short‑circuits)
        Arguments.of("Cache contains duplicate values (keys 1 and 3 both map to \"dup\"). The method should return true as soon as it finds the first matching node.", (CacheSetup) () -> {
            LockingLRUCacheStrategy<String, Object> c = new LockingLRUCacheStrategy<>(4);
            c.put("1", "dup");
            c.put("2", "other");
            c.put("3", "dup");
            return c;
        }, "dup", true), // 11. value removed before call → false
        Arguments.of("An entry with value \"toRemove\" is removed before the call. After removal, containsValue must return false for that value.", (CacheSetup) () -> {
            LockingLRUCacheStrategy<String, Object> c = new LockingLRUCacheStrategy<>(3);
            c.put("a", "toRemove");
            c.put("b", "stay");
            c.remove("a");
            return c;
        }, "toRemove", false), // 12. cache cleared before call → false
        Arguments.of("Cache is populated, then clear() is invoked. After clearing, containsValue should behave like an empty cache and return false for any searched value.", (CacheSetup) () -> {
            LockingLRUCacheStrategy<String, Object> c = new LockingLRUCacheStrategy<>(5);
            c.put("a", "x");
            c.put("b", "y");
            c.clear();
            return c;
        }, "anyValue", false), // 13. custom object with proper equals → true
        Arguments.of("A custom object with overridden equals (EqObj) is stored (new EqObj(5)). Searching with a different instance but equal (new EqObj(5)) should succeed.", (CacheSetup) () -> {
            LockingLRUCacheStrategy<String, Object> c = new LockingLRUCacheStrategy<>(2);
            c.put("obj", new EqObj(5));
            return c;
        }, new EqObj(5), true), // 14. type‑different numeric values → false
        Arguments.of("Cache stores an Integer 5. Searching with a Long 5L (different runtime type) must return false because Objects.equals returns false for different types.", (CacheSetup) () -> {
            LockingLRUCacheStrategy<String, Object> c = new LockingLRUCacheStrategy<>(2);
            c.put("num", Integer.valueOf(5));
            return c;
        }, Long.valueOf(5L), false), // 15. large cache, value not present → false
        Arguments.of("Cache is filled with 1,000 distinct entries (\"val0\" … \"val999\"). The searched value does not exist, so the method must iterate through the whole list and return false.", (CacheSetup) () -> {
            LockingLRUCacheStrategy<String, Object> c = new LockingLRUCacheStrategy<>(2000);
            for (int i = 0; i < 1000; i++) {
                c.put("k" + i, "val" + i);
            }
            return c;
        }, "notPresent", false), // 16. large cache, value at last node → true
        Arguments.of("Cache is filled with 1,000 entries; the last node before the tail sentinel holds the value \"target\". The method must traverse the entire list and return true when it reaches the final matching node.", (CacheSetup) () -> {
            LockingLRUCacheStrategy<String, Object> c = new LockingLRUCacheStrategy<>(2000);
            for (int i = 0; i < 999; i++) {
                c.put("k" + i, "val" + i);
            }
            // this becomes the most‑recently used (last) node
            c.put("lastKey", "target");
            return c;
        }, "target", true));
    }

    @Test
    void containsValue_existingValue_returnsTrue() {
        assertTrue(cache.containsValue("apple"), "Cache should report that it contains the value \"apple\".");
    }

    @Test
    void containsValue_nonExistingValue_returnsFalse() {
        assertFalse(cache.containsValue("cherry"), "Cache should report that it does not contain the value \"cherry\".");
    }

    @Test
    void containsValue_nullValue_whenStored_returnsTrue() {
        assertTrue(cache.containsValue(null), "Cache should report that it contains a stored null value.");
    }

    @Test
    void containsValue_nullValue_whenNotStored_returnsFalse() {
        // Create a fresh cache without a null entry
        LockingLRUCacheStrategy<String, String> emptyCache = new LockingLRUCacheStrategy<>(2);
        emptyCache.put("x", "xray");
        assertFalse(emptyCache.containsValue(null), "Cache should report false when null is not stored.");
    }

    @Test
    void containsValue_onEmptyCache_returnsFalse() {
        LockingLRUCacheStrategy<String, String> emptyCache = new LockingLRUCacheStrategy<>(2);
        assertFalse(emptyCache.containsValue("anything"), "Empty cache should never contain any value.");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideNormalCases")
    void containsValue_normalCases(String description, CacheSetup setup, Object searchValue, boolean expectedResult) {
        // Arrange – create a fresh cache according to the supplied setup
        LockingLRUCacheStrategy<String, Object> testCache = setup.create();
        // Act
        boolean actual = testCache.containsValue(searchValue);
        // Assert
        assertEquals(expectedResult, actual, () -> "Failed case: " + description);
    }

    @Test
    void containsValue_exceptionFromEquals_propagatesRuntimeException() {
        // Arrange – a value whose equals() always throws
        BadEquals bad = new BadEquals();
        LockingLRUCacheStrategy<String, Object> badCache = new LockingLRUCacheStrategy<>(2);
        badCache.put("badKey", bad);
        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> badCache.containsValue("anyString"), "The RuntimeException thrown from equals() should propagate.");
        assertEquals("equals is broken", ex.getMessage());
    }

    @Test
    void containsValue_internalCorruption_throwsNullPointerException() throws Exception {
        // Arrange – a normal cache with a couple of entries
        LockingLRUCacheStrategy<String, Object> corruptCache = new LockingLRUCacheStrategy<>(3);
        corruptCache.put("a", "alpha");
        corruptCache.put("b", "beta");
        // Corrupt the internal 'head' field (set it to null)
        Field headField = LockingLRUCacheStrategy.class.getDeclaredField("head");
        headField.setAccessible(true);
        headField.set(corruptCache, null);
        // Act & Assert – NPE should be thrown when the method tries to dereference head.next
        assertThrows(NullPointerException.class, () -> corruptCache.containsValue("alpha"), "When the internal linked list is corrupted, containsValue should throw NPE.");
    }
}
