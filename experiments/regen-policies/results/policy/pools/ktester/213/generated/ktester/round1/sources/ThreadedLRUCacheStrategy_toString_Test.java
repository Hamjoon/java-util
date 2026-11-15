package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ThreadedLRUCacheStrategy_toString_Test {

    private ThreadedLRUCacheStrategy<String, Integer> cache;

    class BadKey {

        @Override
        public String toString() {
            throw new RuntimeException("Bad key toString");
        }
    }

    class BadKey {

        @Override
        public String toString() {
            throw new RuntimeException("Bad key toString");
        }
    }

    /**
     * Creates a fresh cache instance before each test and populates it with a
     * deterministic set of entries. The chosen capacity and cleanup delay are
     * arbitrary but valid (capacity ≥ 1, cleanupDelayMillis ≥ 10).
     */
    @BeforeEach
    void setupBeforeEach() {
        // capacity = 5, cleanup delay = 50ms – arbitrary but valid.
        cache = new ThreadedLRUCacheStrategy<>(5, 50);
        cache.put("one", 1);
        cache.put("two", 2);
        cache.put("three", 3);
    }

    /**
     * Clears the cache after each test to avoid cross‑test contamination.
     */
    @AfterEach
    void teardownAfterEach() {
        cache.clear();
    }

    /**
     * Parameter provider that creates a fresh cache for each scenario, performs
     * the required operations and returns the expected string representation.
     */
    private static Stream<Arguments> cacheScenarios() {
        return Stream.of(// empty cache
        Arguments.of((Runnable) () -> {
            ThreadedLRUCacheStrategy<String, Integer> c = new ThreadedLRUCacheStrategy<>(5, 50);
            // no entries
        }, "{}", "A newly created ThreadedLRUCacheStrategy contains no entries, so toString() should return the empty map representation."), // single entry
        Arguments.of((Runnable) () -> {
            ThreadedLRUCacheStrategy<String, Integer> c = new ThreadedLRUCacheStrategy<>(5, 50);
            c.put("a", 1);
        }, "{a=1}", "A cache containing exactly one mapping (key \"a\" -> value 1) should render that single entry in the standard map string format."), // multiple entries
        Arguments.of((Runnable) () -> {
            ThreadedLRUCacheStrategy<String, Integer> c = new ThreadedLRUCacheStrategy<>(5, 50);
            c.put("a", 1);
            c.put("b", 2);
        }, "{a=1, b=2}", "A cache with two distinct entries should list both entries separated by a comma and a space, preserving the insertion order."), // null key and null value
        Arguments.of((Runnable) () -> {
            ThreadedLRUCacheStrategy<String, Integer> c = new ThreadedLRUCacheStrategy<>(5, 50);
            // null -> null
            c.put(null, null);
            // overwrite with non‑null value (not needed)
            c.put(null, 0);
            // key "nullKey" -> null
            c.put("nullKey", null);
            // null key -> 1 (overwrite again)
            c.put(null, 1);
            // finally null key -> null again
            c.put(null, null);
            // ensure we have both null key and null value cases
            c.put(null, 0);
            // final state: null key maps to null
            c.put(null, null);
            // key "nullValue" -> null
            c.put("nullValue", null);
        }, "{null=null, nullValue=null}", "When the cache stores a null key and a null value, the string representation must display the literal word \"null\" for those components."), // after removal
        Arguments.of((Runnable) () -> {
            ThreadedLRUCacheStrategy<String, Integer> c = new ThreadedLRUCacheStrategy<>(5, 50);
            c.put("x", 10);
            c.put("y", 20);
            c.remove("x");
        }, "{y=20}", "After inserting two entries and removing one of them, toString() should reflect only the remaining entry."), // after putAll
        Arguments.of((Runnable) () -> {
            ThreadedLRUCacheStrategy<String, Integer> c = new ThreadedLRUCacheStrategy<>(5, 50);
            Map<String, Integer> m = new LinkedHashMap<>();
            m.put("c", 3);
            m.put("d", 4);
            c.putAll(m);
        }, "{c=3, d=4}", "Calling putAll with a map that contains two entries should add those entries to the cache, and toString() must show them."), // after clear
        Arguments.of((Runnable) () -> {
            ThreadedLRUCacheStrategy<String, Integer> c = new ThreadedLRUCacheStrategy<>(5, 50);
            c.put("p", 5);
            c.put("q", 6);
            c.clear();
        }, "{}", "Clearing a previously populated cache should result in an empty map representation."));
    }

    /**
     * Verifies that {@code ThreadedLRUCacheStrategy.toString()} returns the same
     * string as {@link MapUtilities#mapToString(Map)} for the current cache state.
     */
    @Test
    void toString_ShouldDelegateToMapUtilities() {
        // GIVEN – the cache is already populated in @BeforeEach
        // WHEN
        String actual = cache.toString();
        // THEN – compute the expected representation using the utility directly
        String expected = MapUtilities.mapToString(cache);
        assertEquals(expected, actual, "ThreadedLRUCacheStrategy.toString() should delegate to MapUtilities.mapToString()");
    }

    /**
     * Additional sanity check: an empty cache should produce the same empty‑map
     * representation as the utility.
     */
    @Test
    void toString_EmptyCache_ShouldMatchUtility() {
        // GIVEN
        // ensure empty
        cache.clear();
        // WHEN
        String actual = cache.toString();
        // THEN
        String expected = MapUtilities.mapToString(cache);
        assertEquals(expected, actual, "Empty ThreadedLRUCacheStrategy should produce the same string as MapUtilities for an empty map");
    }

    /**
     * Verify that modifications after the initial call are reflected in subsequent
     * {@code toString()} results.
     */
    @Test
    void toString_AfterModification_ShouldReflectChanges() {
        // GIVEN
        cache.put("four", 4);
        // WHEN
        String afterPut = cache.toString();
        // THEN
        String expectedAfterPut = MapUtilities.mapToString(cache);
        assertEquals(expectedAfterPut, afterPut, "toString() should reflect entries added after the cache was created");
        // Modify again
        cache.remove("two");
        String afterRemove = cache.toString();
        String expectedAfterRemove = MapUtilities.mapToString(cache);
        assertEquals(expectedAfterRemove, afterRemove, "toString() should reflect entries removed from the cache");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("cacheScenarios")
    void toString_VariousScenarios_ShouldMatchUtility(Runnable cacheSetup, String expectedString, String description) {
        // GIVEN – create a fresh cache instance and apply the scenario‑specific setup
        ThreadedLRUCacheStrategy<String, Integer> scenarioCache = new ThreadedLRUCacheStrategy<>(5, 50);
        // The lambda captures its own cache instance; we need to execute it on the scenarioCache.
        // To keep the lambda simple, we re‑create the cache inside the lambda; therefore we ignore the
        // passed instance and instead retrieve the cache from the lambda via reflection.
        // However, for clarity we will just re‑execute the lambda on a new cache and then
        // compute the expected string using MapUtilities (which will match the lambda’s internal cache).
        // To avoid confusion, we let the lambda operate on a thread‑local variable:
        ThreadLocal<ThreadedLRUCacheStrategy<String, Integer>> holder = new ThreadLocal<>();
        holder.set(scenarioCache);
        // the lambda uses its own cache instance internally
        cacheSetup.run();
        // The lambda has already populated its own cache; we now retrieve that cache:
        ThreadedLRUCacheStrategy<String, Integer> actualCache = holder.get();
        // WHEN
        String actual = actualCache.toString();
        // THEN – compare with the expected string supplied in the test data.
        // For deterministic comparison we also compute the utility result and assert equality.
        String utilityResult = MapUtilities.mapToString(actualCache);
        assertEquals(expectedString, utilityResult, "Utility representation does not match the expected literal for scenario: " + description);
        assertEquals(utilityResult, actual, "toString() should delegate to MapUtilities.mapToString() for scenario: " + description);
    }

    /**
     * Verifies that if a key's {@code toString()} throws a {@link RuntimeException},
     * the exception propagates out of {@code ThreadedLRUCacheStrategy.toString()}.
     */
    @Test
    void toString_KeyToStringThrowsRuntimeException_ShouldPropagate() {
        // GIVEN – a key whose toString() deliberately throws
        class BadKey {

            @Override
            public String toString() {
                throw new RuntimeException("Bad key toString");
            }
        }
        BadKey badKey = new BadKey();
        // store any value
        cache.put(badKey, 42);
        // WHEN / THEN – the exception should bubble up
        assertThrows(RuntimeException.class, cache::toString, "toString() should propagate RuntimeException thrown by a key's toString()");
    }
}
