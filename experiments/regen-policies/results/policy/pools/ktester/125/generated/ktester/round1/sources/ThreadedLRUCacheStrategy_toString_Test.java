package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.TimeUnit;

@ExtendWith(MockitoExtension.class)
class ThreadedLRUCacheStrategy_toString_Test {

    /**
     * Instance used in each test.
     */
    private ThreadedLRUCacheStrategy<Integer, String> cache;

    /**
     * Simple class whose toString returns a constant string.
     */
    private static final class Foo {

        @Override
        public String toString() {
            return "Foo";
        }
    }

    /**
     * Class that deliberately throws a RuntimeException from its toString().
     * Used to verify exception propagation from ThreadedLRUCacheStrategy.toString().
     */
    private static final class BadToString {

        @Override
        public String toString() {
            throw new RuntimeException("Intentional toString failure");
        }
    }

    /**
     * Create a fresh cache before each test case.
     */
    @BeforeEach
    void setupBeforeEach() {
        // capacity = 10, cleanup delay = 50 ms (arbitrary but valid values)
        cache = new ThreadedLRUCacheStrategy<>(10, 50);
    }

    /**
     * Clear the cache after each test to avoid cross‑test contamination.
     */
    @AfterEach
    void teardownAfterEach() {
        if (cache != null) {
            cache.clear();
        }
    }

    /**
     * Shut down the shared scheduler used by all cache instances.
     */
    @AfterAll
    static void teardownAfterAll() {
        ThreadedLRUCacheStrategy.shutdown();
    }

    /**
     * Verify that an empty cache produces the expected string representation.
     */
    @Test
    void toString_EmptyCache_ReturnsEmptyBraces() {
        // given – an empty cache (created in @BeforeEach)
        // when
        String result = cache.toString();
        // then
        assertEquals("{}", result, "An empty ThreadedLRUCacheStrategy should be represented by {}");
    }

    /**
     * Verify that a cache with entries includes each entry in the string.
     */
    @Test
    void toString_NonEmptyCache_ContainsAllEntries() {
        // given
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        // when
        String result = cache.toString();
        // then – the exact order is not guaranteed, so we check for presence of each pair
        assertTrue(result.startsWith("{") && result.endsWith("}"), "Result should start with '{' and end with '}'");
        assertTrue(result.contains("1=one"), "Result should contain the entry 1=one");
        assertTrue(result.contains("2=two"), "Result should contain the entry 2=two");
        assertTrue(result.contains("3=three"), "Result should contain the entry 3=three");
    }

    /**
     * Verify that null keys and values are handled correctly in the string output.
     */
    @Test
    void toString_NullKeyAndValue_AreRepresented() {
        // given
        cache.put(null, null);
        cache.put(42, null);
        cache.put(null, "value");
        // when
        String result = cache.toString();
        // then – check that the textual representation includes the nulls
        assertTrue(result.contains("null=null"), "Result should contain null=null");
        assertTrue(result.contains("42=null"), "Result should contain 42=null");
        assertTrue(result.contains("null=value"), "Result should contain null=value");
    }

    /**
     * Single entry – verify exact string representation.
     */
    @Test
    void toString_SingleEntry_ReturnsExactPair() {
        ThreadedLRUCacheStrategy<String, Integer> c = new ThreadedLRUCacheStrategy<>(5, 50);
        c.put("one", 1);
        assertEquals("{one=1}", c.toString(), "A cache with a single entry should be represented exactly as {one=1}");
    }

    /**
     * Multiple entries – order is not significant, but all pairs must appear.
     */
    @Test
    void toString_MultipleEntries_AllPairsPresent() {
        ThreadedLRUCacheStrategy<String, String> c = new ThreadedLRUCacheStrategy<>(5, 50);
        c.put("alpha", "alphaValue");
        c.put("beta", "betaValue");
        String result = c.toString();
        assertTrue(result.startsWith("{") && result.endsWith("}"), "Result must be wrapped in braces");
        assertTrue(result.contains("alpha=alphaValue"), "Result must contain alpha=alphaValue");
        assertTrue(result.contains("beta=betaValue"), "Result must contain beta=betaValue");
    }

    /**
     * Duplicate key – the later value replaces the earlier one.
     */
    @Test
    void toString_DuplicateKey_OnlyLatestValueVisible() {
        ThreadedLRUCacheStrategy<String, Integer> c = new ThreadedLRUCacheStrategy<>(5, 50);
        c.put("dup", 1);
        // replacement
        c.put("dup", 2);
        String result = c.toString();
        assertEquals("{dup=2}", result, "When a key is inserted twice, only the latest mapping should appear");
    }

    /**
     * Null key with a non‑null value.
     */
    @Test
    void toString_NullKeyNonNullValue_RepresentedCorrectly() {
        ThreadedLRUCacheStrategy<String, String> c = new ThreadedLRUCacheStrategy<>(5, 50);
        c.put(null, "nonNullValue");
        assertEquals("{null=nonNullValue}", c.toString(), "null key with a non‑null value must be shown as null=nonNullValue");
    }

    /**
     * Non‑null key with a null value.
     */
    @Test
    void toString_NonNullKeyNullValue_RepresentedCorrectly() {
        ThreadedLRUCacheStrategy<String, String> c = new ThreadedLRUCacheStrategy<>(5, 50);
        c.put("someKey", null);
        assertEquals("{someKey=null}", c.toString(), "Non‑null key with a null value must be shown as someKey=null");
    }

    /**
     * Mixed entries – regular, null key/value combinations.
     */
    @Test
    void toString_MixedEntries_AllPairsPresent() {
        ThreadedLRUCacheStrategy<String, String> c = new ThreadedLRUCacheStrategy<>(10, 50);
        c.put("alpha", "alphaValue");
        c.put("beta", "betaValue");
        c.put(null, "val");
        c.put("key", null);
        String result = c.toString();
        assertTrue(result.contains("alpha=alphaValue"));
        assertTrue(result.contains("beta=betaValue"));
        assertTrue(result.contains("null=val"));
        assertTrue(result.contains("key=null"));
    }

    /**
     * Custom objects with their own toString implementation.
     */
    @Test
    void toString_CustomObjects_UsesTheirToString() {
        ThreadedLRUCacheStrategy<Foo, Foo> c = new ThreadedLRUCacheStrategy<>(5, 50);
        Foo foo = new Foo();
        c.put(foo, foo);
        assertEquals("{Foo=Foo}", c.toString(), "Custom objects should be rendered using their toString output");
    }

    /**
     * When a key's toString throws, the exception propagates.
     */
    @Test
    void toString_KeyToStringThrows_PropagatesException() {
        ThreadedLRUCacheStrategy<BadToString, String> c = new ThreadedLRUCacheStrategy<>(5, 50);
        c.put(new BadToString(), "value");
        assertThrows(RuntimeException.class, c::toString, "A RuntimeException from a key's toString must propagate");
    }

    /**
     * When a value's toString throws, the exception propagates.
     */
    @Test
    void toString_ValueToStringThrows_PropagatesException() {
        ThreadedLRUCacheStrategy<String, BadToString> c = new ThreadedLRUCacheStrategy<>(5, 50);
        c.put("key", new BadToString());
        assertThrows(RuntimeException.class, c::toString, "A RuntimeException from a value's toString must propagate");
    }

    /**
     * When both key and value toString throw, the first encountered exception propagates.
     */
    @Test
    void toString_BothKeyAndValueToStringThrow_PropagatesException() {
        ThreadedLRUCacheStrategy<BadToString, BadToString> c = new ThreadedLRUCacheStrategy<>(5, 50);
        c.put(new BadToString(), new BadToString());
        assertThrows(RuntimeException.class, c::toString, "If both key and value toString throw, a RuntimeException must be propagated");
    }
}
