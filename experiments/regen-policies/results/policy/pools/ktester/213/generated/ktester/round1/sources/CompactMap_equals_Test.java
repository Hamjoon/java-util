package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.concurrent.ConcurrentModificationException;

// keeps the original template annotation
@ExtendWith(MockitoExtension.class)
class CompactMap_equals_Test {

    /* --------------------------------------------------------------------- */
    private static class TestCompactMap<K, V> extends CompactMap<K, V> {

        private final int compactSize;

        private final Map<K, V> newMap;

        private final String singleKey;

        TestCompactMap(int compactSize, Map<K, V> newMap, String singleKey) {
            this.compactSize = compactSize;
            this.newMap = newMap;
            this.singleKey = singleKey;
        }

        @Override
        protected int compactSize() {
            return compactSize;
        }

        @Override
        protected Map<K, V> getNewMap() {
            return newMap;
        }

        @Override
        protected String getSingleValueKey() {
            return singleKey;
        }
    }

    @Test
    void equalsSameReference() {
        CompactMap<String, Integer> map = new CompactMap<>();
        assertTrue(map.equals(map), "Map should be equal to itself");
    }

    @Test
    void equalsWithNonMap() {
        CompactMap<String, Integer> map = new CompactMap<>();
        assertFalse(map.equals("not a map"), "Map must not be equal to a non‑Map object");
    }

    @Test
    void emptyMapsAreEqual() {
        CompactMap<String, Integer> map1 = new CompactMap<>();
        CompactMap<String, Integer> map2 = new CompactMap<>();
        assertTrue(map1.equals(map2), "Two empty CompactMaps must be equal");
        assertTrue(map1.equals(new HashMap<>()), "Empty CompactMap must equal empty HashMap");
    }

    @Test
    void singleEntryEquality() {
        CompactMap<String, Integer> map = new CompactMap<>() {

            @Override
            protected String getSingleValueKey() {
                return "value";
            }

            @Override
            protected int compactSize() {
                // forces single‑entry optimisation
                return 3;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        map.put("value", 42);
        Map<String, Integer> other = new HashMap<>();
        other.put("value", 42);
        assertTrue(map.equals(other), "Single‑entry CompactMap should equal a regular map with the same entry");
        assertTrue(other.equals(map), "Equality must be symmetric");
    }

    @Test
    void compactArrayEquality() {
        // compactSize = 3 → size 2 will be stored in the internal Object[] array
        CompactMap<String, Integer> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 3;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        map.put("a", 1);
        map.put("b", 2);
        Map<String, Integer> other = new HashMap<>();
        other.put("a", 1);
        other.put("b", 2);
        assertTrue(map.equals(other), "CompactMap in compact‑array state must equal a map with same entries");
        assertTrue(other.equals(map), "Equality must be symmetric");
        // change a value – should become unequal
        other.put("b", 99);
        assertFalse(map.equals(other), "Maps with different values must not be equal");
    }

    @Test
    void backingMapEquality() {
        // compactSize = 2 → 3 entries forces transition to a backing Map
        CompactMap<String, Integer> map = new TestCompactMap<>(2, new HashMap<>(), "id");
        map.put("x", 10);
        map.put("y", 20);
        // now internal storage is a real Map
        map.put("z", 30);
        Map<String, Integer> other = new LinkedHashMap<>();
        other.put("x", 10);
        other.put("y", 20);
        other.put("z", 30);
        assertTrue(map.equals(other), "CompactMap with backing map must equal a map with the same entries");
        assertTrue(other.equals(map), "Equality must be symmetric");
    }

    @Test
    void equalityWithNullValues() {
        CompactMap<String, Integer> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 3;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        map.put("k1", null);
        map.put("k2", 5);
        Map<String, Integer> other = new HashMap<>();
        other.put("k1", null);
        other.put("k2", 5);
        assertTrue(map.equals(other), "Maps containing null values must compare correctly");
    }

    @Test
    void differentSizeNotEqual() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Map<String, Integer> other = new HashMap<>();
        // only one entry
        other.put("a", 1);
        assertFalse(map.equals(other), "Maps with different sizes must not be equal");
    }

    @Test
    void orderIrrelevantForEquality() {
        // insertion order vs. unordered should not affect equality
        CompactMap<String, Integer> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 3;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        map.put("first", 1);
        map.put("second", 2);
        Map<String, Integer> other = new LinkedHashMap<>();
        other.put("second", 2);
        other.put("first", 1);
        assertTrue(map.equals(other), "Equality must ignore iteration order");
    }

    /**
     * ReferenceEquality – the same instance must be equal to itself.
     */
    @Test
    void referenceEquality() {
        CompactMap<String, Integer> map = new CompactMap<>();
        assertTrue(map.equals(map), "Map should be equal to itself (this == obj)");
    }

    /**
     * NonMapArgument – any non‑Map argument must result in false.
     */
    @Test
    void nonMapArgument() {
        CompactMap<String, Integer> map = new CompactMap<>();
        assertFalse(map.equals("not a map"), "Map must not be equal to a non‑Map object");
    }

    /**
     * NullArgument – equals(null) must return false.
     */
    @Test
    void nullArgument() {
        CompactMap<String, Integer> map = new CompactMap<>();
        assertFalse(map.equals(null), "equals(null) must return false");
    }

    /**
     * SizeMismatch – differing sizes cause early false.
     */
    @Test
    void sizeMismatch() {
        CompactMap<String, Integer> map = new CompactMap<>();
        // size = 1
        map.put("only", 1);
        Map<String, Integer> other = new HashMap<>();
        other.put("a", 1);
        // size = 2
        other.put("b", 2);
        assertFalse(map.equals(other), "Maps with different sizes must not be equal");
    }

    /**
     * EmptyMapEquality – two empty maps must be equal.
     */
    @Test
    void emptyMapEquality() {
        CompactMap<String, Integer> map = new CompactMap<>();
        assertTrue(map.equals(new HashMap<>()), "Empty CompactMap must equal empty HashMap");
    }

    /**
     * EmptyMapInequality – empty CompactMap vs non‑empty map must be false.
     */
    @Test
    void emptyMapInequality() {
        CompactMap<String, Integer> map = new CompactMap<>();
        Map<String, Integer> other = new HashMap<>();
        other.put("k", 1);
        assertFalse(map.equals(other), "Empty CompactMap must not equal a non‑empty map");
    }

    /**
     * SingleEntryKeyMissing – same size but the other map lacks the key.
     */
    @Test
    void singleEntryKeyMissing() {
        CompactMap<String, Integer> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 3;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        map.put("key1", 100);
        Map<String, Integer> other = new HashMap<>();
        other.put("differentKey", 100);
        assertFalse(map.equals(other), "Missing key in the other map must cause inequality");
    }

    /**
     * SingleEntryValueMismatch – same key but different non‑null values.
     */
    @Test
    void singleEntryValueMismatch() {
        CompactMap<String, Integer> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 3;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        map.put("key1", 100);
        Map<String, Integer> other = new HashMap<>();
        // different value
        other.put("key1", 999);
        assertFalse(map.equals(other), "Different values for the same key must cause inequality");
    }

    /**
     * NullValueEquality – both maps store null for the same key.
     */
    @Test
    void nullValueEquality() {
        CompactMap<String, Integer> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 3;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        map.put("k1", null);
        Map<String, Integer> other = new HashMap<>();
        other.put("k1", null);
        assertTrue(map.equals(other), "Maps with null values for the same key must be equal");
    }

    /**
     * NullValueInequality – one side null, the other non‑null.
     */
    @Test
    void nullValueInequality() {
        CompactMap<String, Integer> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 3;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        map.put("k1", null);
        Map<String, Integer> other = new HashMap<>();
        // non‑null
        other.put("k1", 5);
        assertFalse(map.equals(other), "Null vs non‑null value for the same key must be unequal");
    }

    /**
     * LargeMapDelegateInequality – backing map differs by one value.
     */
    @Test
    void largeMapDelegateInequality() {
        CompactMap<String, Integer> map = new TestCompactMap<>(2, new HashMap<>(), "id");
        map.put("a", 1);
        map.put("b", 2);
        // internal Map now
        map.put("c", 3);
        Map<String, Integer> other = new HashMap<>();
        other.put("a", 1);
        other.put("b", 2);
        // differing value
        other.put("c", 999);
        assertFalse(map.equals(other), "Backing‑map equality must detect differing values");
    }

    /**
     * ConcurrentModificationDuringEquals – the other map mutates while being iterated.
     */
    @Test
    void concurrentModificationDuringEquals() {
        // CompactMap with a single entry (stored in array)
        CompactMap<String, Integer> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 3;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        map.put("k", 1);
        // Map that adds a new entry during iteration, causing CME from HashMap iterator
        Map<String, Integer> mutatingMap = new HashMap<>();
        mutatingMap.put("k", 1);
        // placeholder to ensure iterator sees change
        mutatingMap.put("trigger", 0);
        // Wrap the entrySet iterator so that the first call to next() mutates the map
        Map<String, Integer> wrapper = new AbstractMap<>() {

            @Override
            public Set<Entry<String, Integer>> entrySet() {
                Set<Entry<String, Integer>> original = mutatingMap.entrySet();
                return new AbstractSet<>() {

                    @Override
                    public Iterator<Entry<String, Integer>> iterator() {
                        Iterator<Entry<String, Integer>> it = original.iterator();
                        return new Iterator<>() {

                            private boolean mutated = false;

                            @Override
                            public boolean hasNext() {
                                return it.hasNext();
                            }

                            @Override
                            public Entry<String, Integer> next() {
                                // mutate the map on the first element
                                if (!mutated) {
                                    mutatingMap.put("newKey", 42);
                                    mutated = true;
                                }
                                return it.next();
                            }
                        };
                    }

                    @Override
                    public int size() {
                        return original.size();
                    }
                };
            }

            @Override
            public int size() {
                return mutatingMap.size();
            }

            @Override
            public boolean isEmpty() {
                return mutatingMap.isEmpty();
            }

            @Override
            public boolean containsKey(Object key) {
                return mutatingMap.containsKey(key);
            }

            @Override
            public boolean containsValue(Object value) {
                return mutatingMap.containsValue(value);
            }

            @Override
            public Integer get(Object key) {
                return mutatingMap.get(key);
            }

            @Override
            public Integer put(String key, Integer value) {
                return mutatingMap.put(key, value);
            }

            @Override
            public Integer remove(Object key) {
                return mutatingMap.remove(key);
            }

            @Override
            public void putAll(Map<? extends String, ? extends Integer> m) {
                mutatingMap.putAll(m);
            }

            @Override
            public void clear() {
                mutatingMap.clear();
            }

            @Override
            public Set<String> keySet() {
                return mutatingMap.keySet();
            }

            @Override
            public Collection<Integer> values() {
                return mutatingMap.values();
            }
        };
        assertThrows(ConcurrentModificationException.class, () -> map.equals(wrapper), "Modifying the other map during iteration must throw CME");
    }

    /**
     * NullEntryInOtherMap – entrySet contains a null element, leading to NPE.
     */
    @Test
    void nullEntryInOtherMap() {
        CompactMap<String, Integer> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 3;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        map.put("k", 1);
        Map<String, Integer> mapWithNullEntry = new AbstractMap<>() {

            @Override
            public Set<Entry<String, Integer>> entrySet() {
                // A set with a single null element
                return new AbstractSet<>() {

                    @Override
                    public Iterator<Entry<String, Integer>> iterator() {
                        return new Iterator<>() {

                            private boolean delivered = false;

                            @Override
                            public boolean hasNext() {
                                return !delivered;
                            }

                            @Override
                            public Entry<String, Integer> next() {
                                if (delivered) {
                                    throw new NoSuchElementException();
                                }
                                delivered = true;
                                // null entry
                                return null;
                            }
                        };
                    }

                    @Override
                    public int size() {
                        return 1;
                    }
                };
            }

            @Override
            public int size() {
                return 1;
            }

            @Override
            public boolean isEmpty() {
                return false;
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
            public Integer get(Object key) {
                return null;
            }

            @Override
            public Integer put(String key, Integer value) {
                return null;
            }

            @Override
            public Integer remove(Object key) {
                return null;
            }

            @Override
            public void putAll(Map<? extends String, ? extends Integer> m) {
            }

            @Override
            public void clear() {
            }

            @Override
            public Set<String> keySet() {
                return Collections.emptySet();
            }

            @Override
            public Collection<Integer> values() {
                return Collections.emptyList();
            }
        };
        assertThrows(NullPointerException.class, () -> map.equals(mapWithNullEntry), "A null entry in the other map's entrySet must cause NPE");
    }

    /**
     * EntrySetReturnsNull – other.map.entrySet() returns null, leading to NPE.
     */
    @Test
    void entrySetReturnsNull() {
        CompactMap<String, Integer> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 3;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        map.put("k", 1);
        Map<String, Integer> mapWithNullEntrySet = new AbstractMap<>() {

            @Override
            public Set<Entry<String, Integer>> entrySet() {
                // deliberately broken
                return null;
            }

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
            public Integer get(Object key) {
                return null;
            }

            @Override
            public Integer put(String key, Integer value) {
                return null;
            }

            @Override
            public Integer remove(Object key) {
                return null;
            }

            @Override
            public void putAll(Map<? extends String, ? extends Integer> m) {
            }

            @Override
            public void clear() {
            }

            @Override
            public Set<String> keySet() {
                return Collections.emptySet();
            }

            @Override
            public Collection<Integer> values() {
                return Collections.emptyList();
            }
        };
        assertThrows(NullPointerException.class, () -> map.equals(mapWithNullEntrySet), "A null entrySet from the other map must cause NPE");
    }
}
