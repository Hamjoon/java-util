package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.*;

class LockingLRUCacheStrategy_equals_Test {

    private LockingLRUCacheStrategy<String, Integer> cacheA;

    private LockingLRUCacheStrategy<String, Integer> cacheB;

    /**
     * Subclass that deliberately returns {@code null} from {@code entrySet()}.
     * This forces a {@link NullPointerException} when {@code equals} tries to
     * invoke {@code entrySet().equals(...)}.
     */
    private static class NullEntrySetCache<K, V> extends LockingLRUCacheStrategy<K, V> {

        NullEntrySetCache(int capacity) {
            super(capacity);
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            return null;
        }
    }

    /**
     * Subclass whose {@code entrySet()} returns a Set whose iterator throws
     * {@link ConcurrentModificationException} after the first element.
     * The standard {@code Set.equals} implementation iterates over both sets,
     * causing the exception to surface from {@code equals}.
     */
    private static class CmeEntrySetCache<K, V> extends LockingLRUCacheStrategy<K, V> {

        CmeEntrySetCache(int capacity) {
            super(capacity);
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            return new AbstractSet<>() {

                @Override
                public Iterator<Map.Entry<K, V>> iterator() {
                    return new Iterator<>() {

                        private boolean first = true;

                        @Override
                        public boolean hasNext() {
                            return true;
                        }

                        @Override
                        public Map.Entry<K, V> next() {
                            if (first) {
                                first = false;
                                return new AbstractMap.SimpleEntry<>(null, null);
                            }
                            throw new ConcurrentModificationException("Simulated concurrent change");
                        }
                    };
                }

                @Override
                public int size() {
                    return 1;
                }
            };
        }
    }

    /**
     * A custom {@link Map} whose {@code entrySet().equals(...)} deliberately
     * throws a {@link RuntimeException}. This verifies that {@code equals}
     * does not swallow unchecked exceptions coming from the other map.
     */
    private static class RuntimeExceptionMap implements Map<Object, Object> {

        @Override
        public Set<Entry<Object, Object>> entrySet() {
            return new AbstractSet<>() {

                @Override
                public Iterator<Entry<Object, Object>> iterator() {
                    return Collections.emptyIterator();
                }

                @Override
                public int size() {
                    return 0;
                }

                @Override
                public boolean equals(Object o) {
                    throw new RuntimeException("Deliberate exception from entrySet.equals");
                }
            };
        }

        // Minimal stub implementations – they are never used in the test.
        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean containsKey(Object key) {
            return false;
        }

        @Override
        public boolean containsValue(Object value) {
            return false;
        }

        @Override
        public Object get(Object key) {
            return null;
        }

        @Override
        public Object put(Object key, Object value) {
            return null;
        }

        @Override
        public Object remove(Object key) {
            return null;
        }

        @Override
        public void putAll(Map<?, ?> m) {
        }

        @Override
        public void clear() {
        }

        @Override
        public Set<Object> keySet() {
            return Collections.emptySet();
        }

        @Override
        public Collection<Object> values() {
            return Collections.emptyList();
        }

        @Override
        public boolean equals(Object o) {
            return o == this;
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create two caches with the same capacity for each test
        cacheA = new LockingLRUCacheStrategy<>(10);
        cacheB = new LockingLRUCacheStrategy<>(10);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear caches to avoid side‑effects between tests
        cacheA.clear();
        cacheB.clear();
    }

    @Test
    void equals_sameInstance_returnsTrue() {
        assertTrue(cacheA.equals(cacheA), "An object must be equal to itself");
    }

    @Test
    void equals_null_returnsFalse() {
        assertFalse(cacheA.equals(null), "Cache should not be equal to null");
    }

    @Test
    void equals_nonMapObject_returnsFalse() {
        Object notAMap = "I am not a map";
        assertFalse(cacheA.equals(notAMap), "Cache should not be equal to a non‑Map object");
    }

    @Test
    void equals_identicalContentsDifferentInstances_returnsTrue() {
        // Populate both caches with identical entries
        cacheA.put("one", 1);
        cacheA.put("two", 2);
        // test null key handling
        cacheA.put(null, 0);
        cacheB.put("one", 1);
        cacheB.put("two", 2);
        cacheB.put(null, 0);
        assertTrue(cacheA.equals(cacheB), "Caches with identical entries should be equal");
        assertTrue(cacheB.equals(cacheA), "Equality must be symmetric");
    }

    @Test
    void equals_differentContents_returnsFalse() {
        cacheA.put("one", 1);
        cacheA.put("two", 2);
        cacheB.put("one", 1);
        // different value
        cacheB.put("two", 99);
        assertFalse(cacheA.equals(cacheB), "Caches with different values should not be equal");
    }

    @Test
    void equals_sameEntriesDifferentOrder_returnsTrue() {
        // Insertion order should not affect equality because entrySet() is compared
        cacheA.put("first", 1);
        cacheA.put("second", 2);
        cacheA.put("third", 3);
        cacheB.put("third", 3);
        cacheB.put("first", 1);
        cacheB.put("second", 2);
        assertTrue(cacheA.equals(cacheB), "Order of insertion must not affect equality");
    }

    @Test
    void equals_compareWithStandardHashMap_returnsTrueWhenContentsMatch() {
        java.util.Map<String, Integer> stdMap = new java.util.HashMap<>();
        stdMap.put("alpha", 10);
        stdMap.put("beta", 20);
        cacheA.put("alpha", 10);
        cacheA.put("beta", 20);
        assertTrue(cacheA.equals(stdMap), "LockingLRUCacheStrategy should be equal to any Map with same entries");
        assertTrue(stdMap.equals(cacheA), "Standard Map should also consider the cache equal");
    }

    @Test
    void equals_emptyMapsEquality() {
        Map<String, Integer> emptyMap = new HashMap<>();
        assertTrue(cacheA.equals(emptyMap), "Empty cache should be equal to an empty HashMap");
    }

    @Test
    void equals_emptyVsNonEmptyInequality() {
        Map<String, String> nonEmpty = Map.of("key", "value");
        assertFalse(cacheA.equals(nonEmpty), "Empty cache must not be equal to a non‑empty map");
    }

    @Test
    void equals_nonEmptyMapsEquality() {
        cacheA.putAll(Map.of("a", 1, "b", 2));
        Map<String, Integer> other = Map.of("a", 1, "b", 2);
        assertTrue(cacheA.equals(other), "Caches with identical non‑empty contents should be equal");
    }

    @Test
    void equals_differentValuesInequality() {
        cacheA.putAll(Map.of("a", 1, "b", 2));
        Map<String, Integer> other = Map.of("a", 99, "b", 2);
        assertFalse(cacheA.equals(other), "Different values for the same keys must yield inequality");
    }

    @Test
    void equals_differentKeysInequality() {
        cacheA.putAll(Map.of("a", 1, "b", 2));
        Map<String, Integer> other = Map.of("x", 1, "y", 2);
        assertFalse(cacheA.equals(other), "Different key sets must yield inequality");
    }

    @Test
    void equals_mapWithDifferentInsertionOrder() {
        Map<String, Integer> linked = new LinkedHashMap<>() {

            {
                put("b", 2);
                put("a", 1);
            }
        };
        cacheA.putAll(Map.of("a", 1, "b", 2));
        assertTrue(cacheA.equals(linked), "EntrySet equality is order‑independent");
    }

    @Test
    void equals_bothMapsContainNullKeyAndNullValue() {
        cacheA.put(null, null);
        Map<String, Integer> other = new HashMap<>() {

            {
                put(null, null);
            }
        };
        assertTrue(cacheA.equals(other), "Both maps containing a null key/value pair should be equal");
    }

    @Test
    void equals_nullKeyMismatch() {
        cacheA.put(null, null);
        Map<String, Integer> other = new HashMap<>() {

            {
                put("a", 1);
            }
        };
        assertFalse(cacheA.equals(other), "Presence of a null key/value in one map only must cause inequality");
    }

    @Test
    void equals_nullEntrySet_throwsNullPointerException() {
        LockingLRUCacheStrategy<String, Integer> brokenCache = new NullEntrySetCache<>(10);
        Map<String, Integer> other = new HashMap<>();
        assertThrows(NullPointerException.class, () -> brokenCache.equals(other), "If entrySet() returns null, equals should throw NPE");
    }

    @Test
    void equals_entrySetIterationConcurrentModification_throwsCME() {
        LockingLRUCacheStrategy<String, Integer> cmeCache = new CmeEntrySetCache<>(10);
        Map<String, Integer> other = new HashMap<>() {

            {
                put("a", 1);
            }
        };
        assertThrows(ConcurrentModificationException.class, () -> cmeCache.equals(other), "A ConcurrentModificationException during entrySet iteration must propagate");
    }

    @Test
    void equals_otherMapEntrySetEqualsThrowsRuntimeException() {
        RuntimeExceptionMap badMap = new RuntimeExceptionMap();
        assertThrows(RuntimeException.class, () -> cacheA.equals(badMap), "RuntimeException from other.map.entrySet().equals must propagate");
    }
}
