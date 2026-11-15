package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

class ThreadedLRUCacheStrategy_remove_Test {

    private ThreadedLRUCacheStrategy<String, Integer> cache;

    @BeforeEach
    void setupBeforeEach() {
        // capacity = 5, cleanup delay = 50 ms (any values > the minimum are fine)
        cache = new ThreadedLRUCacheStrategy<>(5, 50);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache is cleared so that each test starts with a clean state.
        cache.clear();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the shared scheduler used by all ThreadedLRUCacheStrategy instances.
        ThreadedLRUCacheStrategy.shutdown();
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> provideRemoveScenarios() {
        return Stream.of(org.junit.jupiter.params.provider.Arguments.of("alpha", 42, "When the key \"alpha\" is present, remove returns its value."), org.junit.jupiter.params.provider.Arguments.of("beta", null, "When the key \"beta\" is absent, remove returns null."));
    }

    @Test
    void remove_existingKey_returnsValueAndRemovesEntry() {
        // given
        String key = "one";
        Integer value = 1;
        cache.put(key, value);
        assertEquals(1, cache.size(), "Cache should contain one entry before removal");
        // when
        Integer removed = cache.remove(key);
        // then
        assertEquals(value, removed, "remove should return the value associated with the key");
        assertFalse(cache.containsKey(key), "Cache should no longer contain the removed key");
        assertEquals(0, cache.size(), "Cache size should be zero after removal");
    }

    @Test
    void remove_nonExistingKey_returnsNull() {
        // given
        String existingKey = "exists";
        cache.put(existingKey, 42);
        assertEquals(1, cache.size());
        // when
        Integer removed = cache.remove("does-not-exist");
        // then
        assertNull(removed, "remove should return null for a key that is not present");
        assertEquals(1, cache.size(), "Cache size should be unchanged when removing a non‑existent key");
        assertTrue(cache.containsKey(existingKey), "Existing entry should remain untouched");
    }

    @Test
    void remove_nullKey_whenNullKeyPresent_returnsValue() {
        // given
        Integer nullValue = 999;
        cache.put(null, nullValue);
        assertTrue(cache.containsKey(null), "Cache should contain a null key before removal");
        // when
        Integer removed = cache.remove(null);
        // then
        assertEquals(nullValue, removed, "remove should return the value associated with the null key");
        assertFalse(cache.containsKey(null), "Cache should no longer contain the null key");
        assertEquals(0, cache.size(), "Cache should be empty after removing the null key");
    }

    @Test
    void remove_nullKey_whenNotPresent_returnsNull() {
        // given
        cache.put("a", 1);
        assertFalse(cache.containsKey(null), "Cache does not contain a null key");
        // when
        Integer removed = cache.remove(null);
        // then
        assertNull(removed, "remove should return null when null key is absent");
        assertEquals(1, cache.size(), "Cache size should stay the same");
    }

    @Test
    void remove_nullKey_throwsNullPointerException() {
        // According to the JSON spec, the underlying ConcurrentMap may throw NPE for null keys.
        // The cache does not guard against this, so we expect the exception to propagate.
        assertThrows(NullPointerException.class, () -> cache.remove(null), "Removing a null key should propagate NullPointerException");
    }

    @Test
    void remove_afterPreviousRemoval_returnsNullOnSecondCall() {
        // given
        String key = "gamma";
        cache.put(key, 33);
        assertEquals(1, cache.size());
        // first removal – should succeed
        Integer first = cache.remove(key);
        assertEquals(33, first);
        assertEquals(0, cache.size());
        // second removal – key no longer present
        Integer second = cache.remove(key);
        assertNull(second, "Second removal of the same key should return null");
        assertEquals(0, cache.size(), "Cache size must stay zero after second removal");
    }

    @Test
    void remove_fromEmptyCache_returnsNull() {
        // Cache is freshly created and therefore empty.
        assertEquals(0, cache.size(), "Pre‑condition: cache should be empty");
        Integer removed = cache.remove("delta");
        assertNull(removed, "Removing any key from an empty cache must return null");
        assertEquals(0, cache.size(), "Cache must remain empty");
    }

    @Test
    void remove_keyWithNullValue_returnsNullAndRemovesEntry() {
        // given
        cache.put("epsilon", null);
        assertTrue(cache.containsKey("epsilon"));
        assertEquals(1, cache.size());
        // when
        Integer removed = cache.remove("epsilon");
        // then
        assertNull(removed, "Stored null value should be returned as null");
        assertFalse(cache.containsKey("epsilon"), "Key should be removed from the cache");
        assertEquals(0, cache.size(), "Cache size should decrement to zero");
    }

    @Test
    void remove_keyOfDifferentType_returnsNullWithoutException() {
        // Cache is keyed by String; passing an Integer should simply not match.
        cache.put("stringKey", 123);
        assertEquals(1, cache.size());
        // 123 is an Integer, not a String
        Integer removed = cache.remove(123);
        assertNull(removed, "Removing with a key of a different type must return null");
        assertEquals(1, cache.size(), "Cache size must stay unchanged");
    }

    @Test
    void remove_afterClear_returnsNull() {
        // given
        cache.put("alpha", 10);
        cache.put("beta", 20);
        assertEquals(2, cache.size());
        // when
        cache.clear();
        assertEquals(0, cache.size(), "Cache should be empty after clear");
        Integer removed = cache.remove("alpha");
        assertNull(removed, "Removing after clear must return null");
        assertEquals(0, cache.size(), "Cache must remain empty");
    }

    @Test
    void remove_reducesCacheSize_correctly() {
        // given three entries
        cache.put("alpha", 1);
        cache.put("theta", 2);
        cache.put("zeta", 3);
        assertEquals(3, cache.size());
        // when removing "theta"
        Integer removed = cache.remove("theta");
        // then
        assertEquals(2, removed, "Returned value must match stored value");
        assertEquals(2, cache.size(), "Cache size should decrement from 3 to 2");
        assertFalse(cache.containsKey("theta"), "Removed key must no longer be present");
    }

    @Test
    void remove_returnsComplexObject_exactInstance() {
        // given a complex object (ArrayList<Integer>) as value
        List<Integer> complexValue = new ArrayList<>(Arrays.asList(7, 8, 9));
        cache.put("zeta", complexValue);
        assertEquals(1, cache.size());
        // when
        @SuppressWarnings("unchecked")
        List<Integer> removed = (List<Integer>) cache.remove("zeta");
        // then
        assertSame(complexValue, removed, "remove must return the exact stored object instance");
        assertEquals(0, cache.size(), "Cache should be empty after removal");
    }

    @Test
    void remove_withEqualButDistinctKeyInstance_removesEntry() {
        // given a key stored as a String literal
        cache.put("alpha", 100);
        assertTrue(cache.containsKey("alpha"));
        assertEquals(1, cache.size());
        // when removing with a new String instance that is equal
        String distinctKey = new String("alpha");
        Integer removed = cache.remove(distinctKey);
        // then
        assertEquals(100, removed, "remove must locate entry despite distinct key instance");
        assertFalse(cache.containsKey("alpha"), "Key should be removed");
        assertEquals(0, cache.size(), "Cache should be empty after removal");
    }

    @ParameterizedTest(name = "{index} => key=''{0}'', expected=''{1}''")
    @MethodSource("provideRemoveScenarios")
    void remove_parameterizedScenarios(String key, Integer expectedValue, String description) {
        // Populate the cache according to the description (if needed)
        if ("valueAlpha".equals(expectedValue != null ? expectedValue.toString() : null)) {
            // arbitrary non‑null value
            cache.put(key, 42);
        } else if (expectedValue == null && "null".equals(description)) {
            cache.put(key, null);
        }
        Integer result = cache.remove(key);
        if (expectedValue != null) {
            assertEquals(expectedValue, result, description);
        } else {
            assertNull(result, description);
        }
    }
}
