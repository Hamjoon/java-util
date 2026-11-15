package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.ConcurrentModificationException;

@ExtendWith(MockitoExtension.class)
class TTLCache_toString_Test {

    private TTLCache<Integer, String> ttlCache;

    /**
     * Subclass that forces {@code entrySet()} to return {@code null}.
     * The {@code toString()} implementation will attempt to call {@code iterator()}
     * on this null reference, resulting in a {@link NullPointerException}.
     */
    static class NullEntrySetCache extends TTLCache<Integer, String> {

        NullEntrySetCache() {
            super(10_000L, -1);
        }

        @Override
        public Set<Entry<Integer, String>> entrySet() {
            // deliberately break the contract
            return null;
        }
    }

    /**
     * Set implementation whose {@code iterator()} method always throws a
     * {@link RuntimeException}.  This simulates a badly‑behaving collection.
     */
    static class BadIteratorSet extends AbstractSet<Map.Entry<Integer, String>> {

        @Override
        public Iterator<Entry<Integer, String>> iterator() {
            throw new RuntimeException("iterator failure");
        }

        @Override
        public int size() {
            // size is irrelevant; iterator() never succeeds
            return 1;
        }
    }

    static class BadIteratorCache extends TTLCache<Integer, String> {

        BadIteratorCache() {
            super(10_000L, -1);
        }

        @Override
        public Set<Entry<Integer, String>> entrySet() {
            return new BadIteratorSet();
        }
    }

    /**
     * Set whose iterator throws {@link ConcurrentModificationException} on {@code next()}.
     */
    static class CMEIteratorSet extends AbstractSet<Map.Entry<Integer, String>> {

        private final Set<Entry<Integer, String>> backing = new HashSet<>();

        CMEIteratorSet() {
            // Populate with a single dummy entry so that hasNext() returns true.
            backing.add(new AbstractMap.SimpleEntry<>(1, "A"));
        }

        @Override
        public Iterator<Entry<Integer, String>> iterator() {
            final Iterator<Entry<Integer, String>> it = backing.iterator();
            return new Iterator<>() {

                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public Entry<Integer, String> next() {
                    throw new ConcurrentModificationException("forced CME");
                }
            };
        }

        @Override
        public int size() {
            return backing.size();
        }
    }

    static class CMECache extends TTLCache<Integer, String> {

        CMECache() {
            super(10_000L, -1);
        }

        @Override
        public Set<Entry<Integer, String>> entrySet() {
            return new CMEIteratorSet();
        }
    }

    /**
     * Entry implementation whose {@code getKey()} throws {@link IllegalStateException}.
     */
    static class BadKeyEntry implements Map.Entry<Integer, String> {

        @Override
        public Integer getKey() {
            throw new IllegalStateException("key failure");
        }

        @Override
        public String getValue() {
            return "value";
        }

        @Override
        public String setValue(String value) {
            return null;
        }
    }

    static class BadKeyEntryCache extends TTLCache<Integer, String> {

        BadKeyEntryCache() {
            super(10_000L, -1);
        }

        @Override
        public Set<Entry<Integer, String>> entrySet() {
            return Set.of(new BadKeyEntry());
        }
    }

    /**
     * Entry implementation whose {@code getValue()} throws {@link IllegalArgumentException}.
     */
    static class BadValueEntry implements Map.Entry<Integer, String> {

        @Override
        public Integer getKey() {
            return 42;
        }

        @Override
        public String getValue() {
            throw new IllegalArgumentException("value failure");
        }

        @Override
        public String setValue(String value) {
            return null;
        }
    }

    static class BadValueEntryCache extends TTLCache<Integer, String> {

        BadValueEntryCache() {
            super(10_000L, -1);
        }

        @Override
        public Set<Entry<Integer, String>> entrySet() {
            return Set.of(new BadValueEntry());
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a relatively large TTL so entries do not expire during the test.
        ttlCache = new TTLCache<>(10_000L, -1);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache is cleared between tests to avoid cross‑test interference.
        ttlCache.clear();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the shared background purge thread used by TTLCache.
        TTLCache.shutdown();
    }

    /**
     * Parses a string produced by {@link TTLCache#toString()} into a {@link Set}
     * of "key=value" tokens, ignoring ordering.
     *
     * @param s the string representation, e.g. "{1=A, 2=B}"
     * @return a set containing each "key=value" token
     */
    private static Set<String> parseToString(String s) {
        assertNotNull(s, "toString() returned null");
        assertTrue(s.startsWith("{") && s.endsWith("}"), "toString() should start with '{' and end with '}'");
        String inner = s.substring(1, s.length() - 1).trim();
        Set<String> result = new HashSet<>();
        if (inner.isEmpty()) {
            // empty cache
            return result;
        }
        for (String token : inner.split(",")) {
            result.add(token.trim());
        }
        return result;
    }

    /* -------------------------------------------------------------
     *  Tests for toString()
     * ------------------------------------------------------------- */
    @Test
    void testToStringEmptyCache() {
        assertEquals("{}", ttlCache.toString(), "Empty cache should be represented as {}");
    }

    @Test
    void testToStringSingleEntry() {
        ttlCache.put(1, "A");
        assertEquals("{1=A}", ttlCache.toString(), "Cache with a single entry should be represented exactly as {key=value}");
    }

    @Test
    void testToStringMultipleEntries() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.put(3, "C");
        String str = ttlCache.toString();
        // Verify the format contains the surrounding braces.
        assertTrue(str.startsWith("{") && str.endsWith("}"), "String representation must be wrapped in braces");
        // Verify that all expected key=value pairs are present, regardless of order.
        Set<String> expected = Set.of("1=A", "2=B", "3=C");
        Set<String> actual = parseToString(str);
        assertEquals(expected, actual, "All entries must appear in the string representation");
        // Ensure that calling toString() does not modify the cache.
        assertEquals(3, ttlCache.size(), "Cache size must remain unchanged after toString()");
    }

    @Test
    void testToStringDoesNotThrowWhenConcurrentModificationOccurs() throws InterruptedException {
        // Populate the cache.
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        // Start a thread that modifies the cache while toString() is executing.
        Thread modifier = new Thread(() -> {
            try {
                // Small sleep to increase the chance of overlap.
                Thread.sleep(5);
                ttlCache.put(3, "C");
                ttlCache.remove(1);
            } catch (InterruptedException ignored) {
            }
        });
        modifier.start();
        // The toString implementation locks the whole map, so it should not throw
        // ConcurrentModificationException or any other exception.
        assertDoesNotThrow(() -> ttlCache.toString(), "toString() should be safe against concurrent modifications due to internal locking");
        // ensure the modifier thread finishes before the test ends
        modifier.join();
    }

    @Test
    void testToStringNullEntrySet() {
        TTLCache<Integer, String> cache = new NullEntrySetCache();
        assertThrows(NullPointerException.class, cache::toString, "When entrySet() returns null, toString() should propagate NPE");
    }

    @Test
    void testToStringIteratorThrowsRuntimeException() {
        TTLCache<Integer, String> cache = new BadIteratorCache();
        RuntimeException ex = assertThrows(RuntimeException.class, cache::toString, "Iterator throwing RuntimeException should propagate");
        assertEquals("iterator failure", ex.getMessage());
    }

    @Test
    void testToStringIteratorThrowsConcurrentModificationException() {
        TTLCache<Integer, String> cache = new CMECache();
        assertThrows(ConcurrentModificationException.class, cache::toString, "Iterator throwing CME should propagate");
    }

    @Test
    void testToStringEntryGetKeyThrowsException() {
        TTLCache<Integer, String> cache = new BadKeyEntryCache();
        IllegalStateException ex = assertThrows(IllegalStateException.class, cache::toString, "Entry.getKey() throwing should propagate");
        assertEquals("key failure", ex.getMessage());
    }

    @Test
    void testToStringEntryGetValueThrowsException() {
        TTLCache<Integer, String> cache = new BadValueEntryCache();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, cache::toString, "Entry.getValue() throwing should propagate");
        assertEquals("value failure", ex.getMessage());
    }
}
