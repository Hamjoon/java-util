package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.concurrent.*;

@ExtendWith(MockitoExtension.class)
class ThreadedLRUCacheStrategy_toString_Test {

    private ThreadedLRUCacheStrategy<String, String> cache;

    class CmeCache extends ThreadedLRUCacheStrategy<String, String> {

        final Map<String, String> backing = new HashMap<>();

        CmeCache() {
            super(5, 100);
            backing.put("x", "1");
        }

        @Override
        public Set<Entry<String, String>> entrySet() {
            // Return the entrySet of a plain HashMap – its iterator is fail‑fast.
            return backing.entrySet();
        }
    }

    class BadKey {

        @Override
        public String toString() {
            throw new RuntimeException("bad key");
        }
    }

    class BadValue {

        @Override
        public String toString() {
            throw new RuntimeException("bad value");
        }
    }

    class NullEntrySetCache extends ThreadedLRUCacheStrategy<String, String> {

        NullEntrySetCache() {
            super(5, 100);
        }

        @Override
        public Set<Entry<String, String>> entrySet() {
            // break the contract intentionally
            return null;
        }
    }

    class CmeCache extends ThreadedLRUCacheStrategy<String, String> {

        final Map<String, String> backing = new HashMap<>();

        CmeCache() {
            super(5, 100);
            backing.put("x", "1");
        }

        @Override
        public Set<Entry<String, String>> entrySet() {
            // Return the entrySet of a plain HashMap – its iterator is fail‑fast.
            return backing.entrySet();
        }
    }

    class BadKey {

        @Override
        public String toString() {
            throw new RuntimeException("bad key");
        }
    }

    class BadValue {

        @Override
        public String toString() {
            throw new RuntimeException("bad value");
        }
    }

    class NullEntrySetCache extends ThreadedLRUCacheStrategy<String, String> {

        NullEntrySetCache() {
            super(5, 100);
        }

        @Override
        public Set<Entry<String, String>> entrySet() {
            // break the contract intentionally
            return null;
        }
    }

    /**
     * Runs before each test method.
     */
    @BeforeEach
    void setupBeforeEach() {
        // capacity = 5, cleanupDelayMillis = 100 (both valid values)
        cache = new ThreadedLRUCacheStrategy<>(5, 100);
    }

    /**
     * Runs after each test method.
     */
    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache is cleared so that one test does not affect another.
        cache.clear();
    }

    /**
     * Runs once after all tests.
     */
    @AfterAll
    static void teardownAfterAll() {
        // Shut down the shared scheduler used by ThreadedLRUCacheStrategy.
        ThreadedLRUCacheStrategy.shutdown();
    }

    @Test
    void toString_EmptyCache_ReturnsBraces() {
        assertEquals("{}", cache.toString(), "An empty cache should be represented as {}");
    }

    @Test
    void toString_SingleEntry_FormatsCorrectly() {
        cache.put("key", "value");
        assertEquals("{key=value}", cache.toString(), "A cache with a single entry should be formatted as {key=value}");
    }

    @Test
    void toString_MultipleEntries_OrderIsIrrelevant() {
        cache.put("a", "1");
        cache.put("b", "2");
        String result = cache.toString();
        // The order of entries in a ConcurrentHashMap is not guaranteed,
        // so accept either ordering.
        Set<String> acceptable = new HashSet<>();
        acceptable.add("{a=1, b=2}");
        acceptable.add("{b=2, a=1}");
        assertTrue(acceptable.contains(result), "Result was: " + result + ". Expected one of " + acceptable);
    }

    @Test
    void toString_NullKeyAndValue_AreHandled() {
        cache.put(null, "nullValue");
        cache.put("nullKey", null);
        String result = cache.toString();
        // The string should contain both formatted elements, order does not matter.
        assertTrue(result.contains("null=nullValue"), "Result should contain the null key entry: " + result);
        assertTrue(result.contains("nullKey=null"), "Result should contain the null value entry: " + result);
        assertTrue(result.startsWith("{") && result.endsWith("}"), "Result should be wrapped in braces");
    }

    @Test
    void toString_SpecialCharactersInKeyAndValue_AreRenderedVerbatim() {
        // Keys/values that contain commas, braces or equals signs.
        cache.put("{a,b}", "v=1");
        assertEquals("{{a,b}=v=1}", cache.toString(), "Special characters must appear verbatim in the output");
    }

    @Test
    void toString_EmptyStringKeyAndValue_ProducesEmptyPair() {
        cache.put("", "");
        assertEquals("{=}", cache.toString(), "Both empty key and empty value should result in \"{=}\"");
    }

    @Test
    void toString_NonStringObjects_UseTheirOwnToString() {
        // Integer key and List value – rely on their own toString implementations.
        // store the list's string representation
        cache.put(5, Arrays.asList(1, 2).toString());
        // The cache is declared with <String,String> but Java's type erasure allows us to put
        // non‑String objects; the toString() method will call String.valueOf on them.
        assertEquals("{5=[1, 2]}", cache.toString(), "Non‑String objects must be rendered via their toString()");
    }

    @Test
    void toString_AfterRemoval_ReflectsCurrentState() {
        cache.put("alpha", "1");
        cache.put("beta", "2");
        cache.remove("alpha");
        assertEquals("{beta=2}", cache.toString(), "After removal only the remaining entry should be shown");
    }

    @Test
    void toString_PutAllWithOverwrite_UpdatesValues() {
        cache.put("key1", "old");
        cache.put("key2", "val2");
        Map<String, String> other = new HashMap<>();
        // overwrite
        other.put("key1", "new");
        // same value
        other.put("key2", "val2");
        cache.putAll(other);
        // Order is not guaranteed – accept both possible orderings.
        Set<String> acceptable = new HashSet<>();
        acceptable.add("{key1=new, key2=val2}");
        acceptable.add("{key2=val2, key1=new}");
        assertTrue(acceptable.contains(cache.toString()), "Result after putAll should reflect the overwritten value");
    }

    @Test
    void toString_ConcurrentModificationDuringIteration_ThrowsCME() throws Exception {
        // Use a plain HashMap (which throws CME) as the backing collection.
        class CmeCache extends ThreadedLRUCacheStrategy<String, String> {

            final Map<String, String> backing = new HashMap<>();

            CmeCache() {
                super(5, 100);
                backing.put("x", "1");
            }

            @Override
            public Set<Entry<String, String>> entrySet() {
                // Return the entrySet of a plain HashMap – its iterator is fail‑fast.
                return backing.entrySet();
            }
        }
        ThreadedLRUCacheStrategy<String, String> cmeCache = new CmeCache();
        // Thread that mutates the backing map while toString() iterates.
        Thread modifier = new Thread(() -> {
            try {
                // Small pause to let toString() start iterating.
                Thread.sleep(10);
                ((HashMap<String, String>) ((CmeCache) cmeCache).backing).put("y", "2");
            } catch (InterruptedException ignored) {
            }
        });
        modifier.start();
        assertThrows(ConcurrentModificationException.class, cmeCache::toString, "A structural modification during iteration should cause CME");
        // clean up
        modifier.join();
    }

    @Test
    void toString_KeyToStringThrowsException_PropagatesRuntimeException() {
        class BadKey {

            @Override
            public String toString() {
                throw new RuntimeException("bad key");
            }
        }
        // we need to store the object itself as key
        cache.put(new BadKey().toString(), "value");
        // Directly use the raw map to insert the problematic key object.
        @SuppressWarnings("unchecked")
        ThreadedLRUCacheStrategy<Object, String> rawCache = (ThreadedLRUCacheStrategy<Object, String>) cache;
        rawCache.put(new BadKey(), "value");
        RuntimeException ex = assertThrows(RuntimeException.class, cache::toString, "Exception from key.toString() should propagate");
        assertEquals("bad key", ex.getMessage());
    }

    @Test
    void toString_ValueToStringThrowsException_PropagatesRuntimeException() {
        class BadValue {

            @Override
            public String toString() {
                throw new RuntimeException("bad value");
            }
        }
        @SuppressWarnings("unchecked")
        ThreadedLRUCacheStrategy<String, Object> rawCache = (ThreadedLRUCacheStrategy<String, Object>) cache;
        rawCache.put("key", new BadValue());
        RuntimeException ex = assertThrows(RuntimeException.class, cache::toString, "Exception from value.toString() should propagate");
        assertEquals("bad value", ex.getMessage());
    }

    @Test
    void toString_NullEntrySet_ThrowsNullPointerException() {
        // Subclass that deliberately returns null from entrySet()
        class NullEntrySetCache extends ThreadedLRUCacheStrategy<String, String> {

            NullEntrySetCache() {
                super(5, 100);
            }

            @Override
            public Set<Entry<String, String>> entrySet() {
                // break the contract intentionally
                return null;
            }
        }
        ThreadedLRUCacheStrategy<String, String> broken = new NullEntrySetCache();
        assertThrows(NullPointerException.class, broken::toString, "A null entrySet should cause NPE when toString() tries to iterate");
    }
}
