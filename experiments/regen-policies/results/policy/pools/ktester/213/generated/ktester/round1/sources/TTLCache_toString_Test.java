package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TTLCache_toString_Test {

    private TTLCache<Integer, String> ttlCache;

    /**
     * Simple subclass that allows us to inject a custom {@code entrySet}.
     */
    private static class TTLCacheWithEntrySet<K, V> extends TTLCache<K, V> {

        private final Set<Entry<K, V>> customSet;

        TTLCacheWithEntrySet(long ttl, int maxSize, Set<Entry<K, V>> customSet) {
            super(ttl, maxSize);
            this.customSet = customSet;
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return customSet;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a long TTL so entries do not expire during the test
        ttlCache = new TTLCache<>(60_000, -1);
    }

    @AfterEach
    void teardownAfterEach() {
        if (ttlCache != null) {
            ttlCache.clear();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the shared purge thread used by TTLCache
        TTLCache.shutdown();
    }

    static Stream<Arguments> nullKeyValueProvider() {
        return Stream.of(Arguments.of("Null key with non‑null value", null, "value", "{null=value}"), Arguments.of("Non‑null key with null value", "key", null, "{key=null}"), Arguments.of("Both key and value null", null, null, "{null=null}"));
    }

    static Stream<Arguments> specialCharactersProvider() {
        return Stream.of(Arguments.of("Comma in key, equals in value", "a,b", "c=d", "{a,b=c=d}"), Arguments.of("Braces in key, space in value", "{key}", "value with space", "{{key}=value with space}"));
    }

    /**
     * Extracts the individual "key=value" strings from a map-like string.
     * Example: "{a=1, b=2}" -> Set{"a=1","b=2"}
     */
    private static Set<String> extractEntries(String mapString) {
        if ("{}".equals(mapString)) {
            return Collections.emptySet();
        }
        String inner = mapString.substring(1, mapString.length() - 1);
        String[] parts = inner.split(", ");
        return new HashSet<>(Arrays.asList(parts));
    }

    /**
     * Uses reflection to verify that the internal ReentrantLock is not held.
     */
    private static boolean isLockFree(TTLCache<?, ?> cache) {
        try {
            Field lockField = TTLCache.class.getDeclaredField("lock");
            lockField.setAccessible(true);
            ReentrantLock lock = (ReentrantLock) lockField.get(cache);
            // tryLock returns true only if the lock is free
            boolean free = lock.tryLock();
            if (free) {
                // restore state
                lock.unlock();
            }
            return free;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testToStringEmptyCache() {
        String result = ttlCache.toString();
        assertEquals("{}", result, "Empty cache should be represented as {}");
    }

    @Test
    void testToStringSingleEntry() {
        ttlCache.put(1, "A");
        String result = ttlCache.toString();
        // Expected format: {1=A}
        assertTrue(result.startsWith("{") && result.endsWith("}"), "Result should be wrapped in braces");
        assertEquals("{1=A}", result, "Single entry should be formatted exactly");
    }

    @Test
    void testToStringMultipleEntries() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.put(3, "C");
        String result = ttlCache.toString();
        // Verify braces
        assertTrue(result.startsWith("{") && result.endsWith("}"), "Result should be wrapped in braces");
        // Strip braces and split entries
        String inner = result.substring(1, result.length() - 1);
        String[] parts = inner.split(", ");
        Set<String> actualEntries = new HashSet<>();
        for (String part : parts) {
            actualEntries.add(part);
        }
        Set<String> expectedEntries = Set.of("1=A", "2=B", "3=C");
        assertEquals(expectedEntries, actualEntries, "All entries must be present, order is not guaranteed");
    }

    @Test
    void testToStringAfterClear() {
        ttlCache.put(10, "X");
        ttlCache.put(20, "Y");
        ttlCache.clear();
        String result = ttlCache.toString();
        assertEquals("{}", result, "Cache should be empty after clear()");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("nullKeyValueProvider")
    void testToStringNullKeyOrValue(String description, Object key, Object value, String expected) {
        ttlCache.put(key, value);
        assertEquals(expected, ttlCache.toString(), description);
    }

    @Test
    void testLockReleasedWhenEntrySetThrowsRuntimeException() {
        TTLCache<Object, Object> brokenCache = new TTLCache<Object, Object>(60_000, -1) {

            @Override
            public Set<Entry<Object, Object>> entrySet() {
                throw new RuntimeException("forced failure");
            }
        };
        // invoke toString and expect the RuntimeException
        RuntimeException ex = assertThrows(RuntimeException.class, brokenCache::toString);
        assertEquals("forced failure", ex.getMessage());
        // verify the lock is unlocked
        assertTrue(isLockFree(brokenCache), "Lock must be released after exception");
    }

    @Test
    void testToStringAfterRemoval() {
        ttlCache.put("keep", "yes");
        ttlCache.put("remove", "no");
        ttlCache.remove("remove");
        assertEquals("{keep=yes}", ttlCache.toString(), "Cache should contain only the kept entry");
    }

    @Test
    void testToStringAfterPutAll() {
        ttlCache.put("base", 0);
        Map<String, Integer> extra = new LinkedHashMap<>();
        extra.put("one", 1);
        extra.put("two", 2);
        ttlCache.putAll(extra);
        // order is not guaranteed by the implementation, so compare as a set
        String result = ttlCache.toString();
        Set<String> expected = Set.of("base=0", "one=1", "two=2");
        assertEquals(expected, extractEntries(result), "All three entries must be present");
    }

    @Test
    void testToStringDuplicateKeyOverwrite() {
        ttlCache.put("dup", 1);
        // overwrite
        ttlCache.put("dup", 2);
        assertEquals("{dup=2}", ttlCache.toString(), "Latest value should be shown");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("specialCharactersProvider")
    void testToStringSpecialCharacters(String description, Object key, Object value, String expected) {
        ttlCache.put(key, value);
        assertEquals(expected, ttlCache.toString(), description);
    }

    @Test
    void testToStringLargeNumberOfEntries() {
        int count = 20;
        for (int i = 0; i < count; i++) {
            ttlCache.put("k" + i, i);
        }
        String result = ttlCache.toString();
        // Build expected set of entry strings
        Set<String> expected = new HashSet<>();
        for (int i = 0; i < count; i++) {
            expected.add("k" + i + "=" + i);
        }
        assertEquals(expected, extractEntries(result), "All 20 entries must be present");
    }

    @Test
    void testToStringWhenEntrySetIsNull() throws Exception {
        TTLCache<Object, Object> cache = new TTLCacheWithEntrySet<>(60_000, -1, null);
        NullPointerException npe = assertThrows(NullPointerException.class, cache::toString);
        assertTrue(isLockFree(cache), "Lock must be released after NullPointerException");
    }

    @Test
    void testToStringWhenIteratorIsNull() throws Exception {
        Set<Map.Entry<Object, Object>> set = new AbstractSet<>() {

            @Override
            public Iterator<Entry<Object, Object>> iterator() {
                // deliberately broken
                return null;
            }

            @Override
            public int size() {
                return 0;
            }
        };
        TTLCache<Object, Object> cache = new TTLCacheWithEntrySet<>(60_000, -1, set);
        NullPointerException npe = assertThrows(NullPointerException.class, cache::toString);
        assertTrue(isLockFree(cache), "Lock must be released after NullPointerException");
    }

    @Test
    void testToStringIteratorHasNextThrows() throws Exception {
        Iterator<Map.Entry<Object, Object>> badIter = new Iterator<>() {

            @Override
            public boolean hasNext() {
                throw new RuntimeException("hasNext failure");
            }

            @Override
            public Map.Entry<Object, Object> next() {
                return null;
            }
        };
        Set<Map.Entry<Object, Object>> set = Collections.singleton(new AbstractMap.SimpleEntry<>(1, "one"));
        Set<Map.Entry<Object, Object>> wrapper = new AbstractSet<>() {

            @Override
            public Iterator<Entry<Object, Object>> iterator() {
                return badIter;
            }

            @Override
            public int size() {
                return 1;
            }
        };
        TTLCache<Object, Object> cache = new TTLCacheWithEntrySet<>(60_000, -1, wrapper);
        RuntimeException ex = assertThrows(RuntimeException.class, cache::toString);
        assertEquals("hasNext failure", ex.getMessage());
        assertTrue(isLockFree(cache), "Lock must be released after iterator exception");
    }

    @Test
    void testToStringIteratorNextThrows() throws Exception {
        Iterator<Map.Entry<Object, Object>> badIter = new Iterator<>() {

            private boolean first = true;

            @Override
            public boolean hasNext() {
                return first;
            }

            @Override
            public Map.Entry<Object, Object> next() {
                first = false;
                throw new RuntimeException("next failure");
            }
        };
        Set<Map.Entry<Object, Object>> wrapper = new AbstractSet<>() {

            @Override
            public Iterator<Entry<Object, Object>> iterator() {
                return badIter;
            }

            @Override
            public int size() {
                return 1;
            }
        };
        TTLCache<Object, Object> cache = new TTLCacheWithEntrySet<>(60_000, -1, wrapper);
        RuntimeException ex = assertThrows(RuntimeException.class, cache::toString);
        assertEquals("next failure", ex.getMessage());
        assertTrue(isLockFree(cache), "Lock must be released after iterator exception");
    }

    @Test
    void testToStringEntryKeyThrows() throws Exception {
        Map.Entry<Object, Object> badEntry = new Map.Entry<>() {

            @Override
            public Object getKey() {
                throw new RuntimeException("key failure");
            }

            @Override
            public Object getValue() {
                return "value";
            }

            @Override
            public Object setValue(Object value) {
                return null;
            }
        };
        Set<Map.Entry<Object, Object>> set = Collections.singleton(badEntry);
        TTLCache<Object, Object> cache = new TTLCacheWithEntrySet<>(60_000, -1, set);
        RuntimeException ex = assertThrows(RuntimeException.class, cache::toString);
        assertEquals("key failure", ex.getMessage());
        assertTrue(isLockFree(cache), "Lock must be released after entry key exception");
    }

    @Test
    void testToStringEntryValueThrows() throws Exception {
        Map.Entry<Object, Object> badEntry = new Map.Entry<>() {

            @Override
            public Object getKey() {
                return "key";
            }

            @Override
            public Object getValue() {
                throw new RuntimeException("value failure");
            }

            @Override
            public Object setValue(Object value) {
                return null;
            }
        };
        Set<Map.Entry<Object, Object>> set = Collections.singleton(badEntry);
        TTLCache<Object, Object> cache = new TTLCacheWithEntrySet<>(60_000, -1, set);
        RuntimeException ex = assertThrows(RuntimeException.class, cache::toString);
        assertEquals("value failure", ex.getMessage());
        assertTrue(isLockFree(cache), "Lock must be released after entry value exception");
    }
}
