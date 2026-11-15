package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentModificationException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapUtilities_cloneMapOfSets_Test {

    private Map<String, Set<Integer>> original;

    class FaultyHashKey {

        @Override
        public int hashCode() {
            throw new IllegalStateException("hashCode failure");
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }
    }

    class BadSet<E> extends AbstractSet<E> {

        @Override
        public Iterator<E> iterator() {
            return new Iterator<E>() {

                @Override
                public boolean hasNext() {
                    return true;
                }

                @Override
                public E next() {
                    throw new RuntimeException("iteration failure");
                }

                @Override
                public void remove() {
                }
            };
        }

        @Override
        public int size() {
            return 1;
        }
    }

    class ConcurrentModMap<K, V> extends HashMap<K, V> {

        @Override
        public Set<Entry<K, V>> entrySet() {
            Set<Entry<K, V>> original = super.entrySet();
            return new AbstractSet<Entry<K, V>>() {

                @Override
                public Iterator<Entry<K, V>> iterator() {
                    Iterator<Entry<K, V>> it = original.iterator();
                    return new Iterator<Entry<K, V>>() {

                        private boolean first = true;

                        @Override
                        public boolean hasNext() {
                            return it.hasNext();
                        }

                        @Override
                        public Entry<K, V> next() {
                            Entry<K, V> entry = it.next();
                            // On the first call, mutate the underlying map to trigger CME
                            if (first) {
                                first = false;
                                // This modification will be detected by the iterator of the original map
                                ConcurrentModMap.this.put((K) "c", (V) new HashSet<>(Arrays.asList(3)));
                            }
                            return entry;
                        }

                        @Override
                        public void remove() {
                            it.remove();
                        }
                    };
                }

                @Override
                public int size() {
                    return original.size();
                }
            };
        }
    }

    class FaultyHashKey {

        @Override
        public int hashCode() {
            throw new IllegalStateException("hashCode failure");
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }
    }

    class BadSet<E> extends AbstractSet<E> {

        @Override
        public Iterator<E> iterator() {
            return new Iterator<E>() {

                @Override
                public boolean hasNext() {
                    return true;
                }

                @Override
                public E next() {
                    throw new RuntimeException("iteration failure");
                }

                @Override
                public void remove() {
                }
            };
        }

        @Override
        public int size() {
            return 1;
        }
    }

    class ConcurrentModMap<K, V> extends HashMap<K, V> {

        @Override
        public Set<Entry<K, V>> entrySet() {
            Set<Entry<K, V>> original = super.entrySet();
            return new AbstractSet<Entry<K, V>>() {

                @Override
                public Iterator<Entry<K, V>> iterator() {
                    Iterator<Entry<K, V>> it = original.iterator();
                    return new Iterator<Entry<K, V>>() {

                        private boolean first = true;

                        @Override
                        public boolean hasNext() {
                            return it.hasNext();
                        }

                        @Override
                        public Entry<K, V> next() {
                            Entry<K, V> entry = it.next();
                            // On the first call, mutate the underlying map to trigger CME
                            if (first) {
                                first = false;
                                // This modification will be detected by the iterator of the original map
                                ConcurrentModMap.this.put((K) "c", (V) new HashSet<>(Arrays.asList(3)));
                            }
                            return entry;
                        }

                        @Override
                        public void remove() {
                            it.remove();
                        }
                    };
                }

                @Override
                public int size() {
                    return original.size();
                }
            };
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a reproducible map of sets for each test
        original = new HashMap<>();
        Set<Integer> setA = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> setB = new HashSet<>(Arrays.asList(4, 5));
        original.put("A", setA);
        original.put("B", setB);
    }

    @AfterEach
    void teardownAfterEach() {
        original = null;
    }

    /**
     * Data provider for normal maps (both mutable and immutable).
     */
    private static Stream<Arguments> normalMapProvider() {
        Map<String, Set<Integer>> map = new HashMap<>();
        map.put("a", new HashSet<>(Arrays.asList(1, 2)));
        map.put("b", new HashSet<>(Collections.singletonList(3)));
        return Stream.of(Arguments.of(map, false, "mutable"), Arguments.of(map, true, "immutable"));
    }

    /**
     * Empty map scenarios.
     */
    private static Stream<Arguments> emptyMapProvider() {
        return Stream.of(Arguments.of(Collections.emptyMap(), false, "mutable"), Arguments.of(Collections.emptyMap(), true, "immutable"));
    }

    @Test
    void testCloneMutableMapOfSets() {
        Map<String, Set<Integer>> clone = MapUtilities.cloneMapOfSets(original, false);
        // The cloned map must be a different instance
        assertNotSame(original, clone);
        assertEquals(original.size(), clone.size());
        // Each set must also be a different instance but contain the same elements
        for (String key : original.keySet()) {
            Set<Integer> originalSet = original.get(key);
            Set<Integer> clonedSet = clone.get(key);
            assertNotSame(originalSet, clonedSet, "Set for key '" + key + "' should be a new instance");
            assertEquals(originalSet, clonedSet, "Contents of set for key '" + key + "' should be equal");
        }
        // Modifying the original after cloning must not affect the clone
        original.get("A").add(99);
        assertFalse(clone.get("A").contains(99), "Clone must be independent of original modifications");
    }

    @Test
    void testCloneImmutableMapOfSets() {
        Map<String, Set<Integer>> immutableClone = MapUtilities.cloneMapOfSets(original, true);
        // The map itself must be unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> immutableClone.put("C", new HashSet<>()));
        // Each set inside the map must be unmodifiable and contain the same data
        for (String key : original.keySet()) {
            Set<Integer> originalSet = original.get(key);
            Set<Integer> clonedSet = immutableClone.get(key);
            // The set instances are expected to be the same as the original because the method
            // wraps the original set with Collections.unmodifiableSet when immutable == true.
            // However, we only need to verify immutability, not identity.
            assertEquals(originalSet, clonedSet, "Contents should match for key '" + key + "'");
            assertThrows(UnsupportedOperationException.class, () -> clonedSet.add(42));
        }
        // Verify that the map reports the correct size and contents
        assertEquals(original.size(), immutableClone.size());
        assertTrue(immutableClone.keySet().containsAll(original.keySet()));
    }

    @Test
    void testCloneEmptyMap() {
        Map<String, Set<Integer>> empty = Collections.emptyMap();
        Map<String, Set<Integer>> mutableClone = MapUtilities.cloneMapOfSets(empty, false);
        assertTrue(mutableClone.isEmpty());
        assertDoesNotThrow(() -> mutableClone.put("X", new HashSet<>(Arrays.asList(1))));
        Map<String, Set<Integer>> immutableClone = MapUtilities.cloneMapOfSets(empty, true);
        assertTrue(immutableClone.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> immutableClone.put("Y", new HashSet<>()));
    }

    @Test
    void testNullOriginalMapThrowsNPE() {
        assertThrows(NullPointerException.class, () -> MapUtilities.cloneMapOfSets(null, false));
    }

    @ParameterizedTest(name = "{2} map cloning – immutable={1}")
    @MethodSource("normalMapProvider")
    void testNormalMapCloning(Map<String, Set<Integer>> src, boolean immutable, String mode) {
        Map<String, Set<Integer>> clone = MapUtilities.cloneMapOfSets(src, immutable);
        // Map instance must be different
        assertNotSame(src, clone, "Result map should be a new instance");
        // Same size and same keys
        assertEquals(src.size(), clone.size());
        assertTrue(clone.keySet().containsAll(src.keySet()));
        // Verify each set
        for (String key : src.keySet()) {
            Set<Integer> originalSet = src.get(key);
            Set<Integer> clonedSet = clone.get(key);
            if (immutable) {
                // Immutable case – the set is wrapped, not a new instance
                assertEquals(originalSet, clonedSet);
                assertThrows(UnsupportedOperationException.class, () -> clonedSet.add(999));
            } else {
                // Mutable case – new instance with equal contents
                assertNotSame(originalSet, clonedSet);
                assertEquals(originalSet, clonedSet);
                // Mutating the clone must not affect the original
                clonedSet.add(999);
                assertFalse(originalSet.contains(999));
            }
        }
        // Map immutability check
        if (immutable) {
            assertThrows(UnsupportedOperationException.class, () -> clone.put("c", new HashSet<>()));
        } else {
            assertDoesNotThrow(() -> clone.put("c", new HashSet<>(Arrays.asList(7))));
        }
    }

    @ParameterizedTest(name = "Empty map – {2}")
    @MethodSource("emptyMapProvider")
    void testEmptyMapCloning(Map<String, Set<Integer>> src, boolean immutable, String mode) {
        Map<String, Set<Integer>> clone = MapUtilities.cloneMapOfSets(src, immutable);
        assertTrue(clone.isEmpty(), "Cloned map should be empty");
        if (immutable) {
            assertThrows(UnsupportedOperationException.class, () -> clone.put("x", new HashSet<>()));
        } else {
            assertDoesNotThrow(() -> clone.put("x", new HashSet<>(Arrays.asList(1))));
        }
    }

    /**
     * Null value handling (value Set is null).
     */
    @Test
    void testNullValueHandlingMutable() {
        Map<String, Set<Integer>> src = new HashMap<>();
        src.put("nullKey", null);
        assertThrows(NullPointerException.class, () -> MapUtilities.cloneMapOfSets(src, false));
    }

    @Test
    void testNullValueHandlingImmutable() {
        Map<String, Set<Integer>> src = new HashMap<>();
        src.put("keyWithNullSet", null);
        assertThrows(NullPointerException.class, () -> MapUtilities.cloneMapOfSets(src, true));
    }

    /**
     * Null key handling – HashMap permits null keys.
     */
    @Test
    void testNullKeyHandlingMutable() {
        Map<String, Set<Integer>> src = new HashMap<>();
        src.put(null, new HashSet<>(Arrays.asList(1, 2, 3)));
        Map<String, Set<Integer>> clone = MapUtilities.cloneMapOfSets(src, false);
        assertTrue(clone.containsKey(null), "Clone must contain the null key");
        Set<Integer> clonedSet = clone.get(null);
        assertNotSame(src.get(null), clonedSet);
        assertEquals(src.get(null), clonedSet);
        // Mutating the cloned set must not affect the original
        clonedSet.add(99);
        assertFalse(src.get(null).contains(99));
    }

    /**
     * Empty Set value – ensure a new empty Set instance is created.
     */
    @Test
    void testEmptySetValueMutable() {
        Map<String, Set<Integer>> src = new HashMap<>();
        src.put("k1", Collections.emptySet());
        Map<String, Set<Integer>> clone = MapUtilities.cloneMapOfSets(src, false);
        Set<Integer> clonedSet = clone.get("k1");
        assertNotSame(src.get("k1"), clonedSet);
        assertTrue(clonedSet.isEmpty());
        // Adding to cloned set must not affect original
        clonedSet.add(5);
        assertFalse(src.get("k1").contains(5));
    }

    /**
     * Set containing a null element.
     */
    @Test
    void testSetContainingNullElementMutable() {
        Set<Integer> setWithNull = new HashSet<>(Arrays.asList((Integer) null));
        Map<String, Set<Integer>> src = new HashMap<>();
        src.put("k2", setWithNull);
        Map<String, Set<Integer>> clone = MapUtilities.cloneMapOfSets(src, false);
        Set<Integer> clonedSet = clone.get("k2");
        assertNotSame(setWithNull, clonedSet);
        assertEquals(1, clonedSet.size());
        assertTrue(clonedSet.contains(null));
    }

    /**
     * Original map mutation after cloning – mutable result.
     */
    @Test
    void testOriginalMutationAfterMutableClone() {
        Map<String, Set<Integer>> src = new HashMap<>();
        src.put("a", new HashSet<>(Arrays.asList(1)));
        Map<String, Set<Integer>> clone = MapUtilities.cloneMapOfSets(src, false);
        // Mutate original after cloning
        src.put("b", new HashSet<>(Arrays.asList(2)));
        src.get("a").add(99);
        // Clone must stay unchanged
        assertFalse(clone.containsKey("b"));
        assertFalse(clone.get("a").contains(99));
    }

    /**
     * Original map mutation after cloning – immutable result.
     */
    @Test
    void testOriginalMutationAfterImmutableClone() {
        Map<String, Set<Integer>> src = new HashMap<>();
        src.put("b", new HashSet<>(Arrays.asList(2)));
        Map<String, Set<Integer>> clone = MapUtilities.cloneMapOfSets(src, true);
        // Mutate original after cloning
        src.put("c", new HashSet<>(Arrays.asList(3)));
        src.get("b").add(88);
        // Immutable clone must stay unchanged
        assertFalse(clone.containsKey("c"));
        assertFalse(clone.get("b").contains(88));
    }

    /**
     * Key with hashCode() that throws an exception.
     */
    @Test
    void testKeyHashCodeThrows() {
        class FaultyHashKey {

            @Override
            public int hashCode() {
                throw new IllegalStateException("hashCode failure");
            }

            @Override
            public boolean equals(Object o) {
                return this == o;
            }
        }
        Map<Object, Set<Integer>> src = new HashMap<>();
        src.put(new FaultyHashKey(), new HashSet<>(Arrays.asList(1, 2)));
        // The exception should propagate when putting into the result map
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> MapUtilities.cloneMapOfSets(src, false));
        assertEquals("hashCode failure", ex.getMessage());
    }

    /**
     * Value Set that throws during iteration (used by HashSet constructor).
     */
    @Test
    void testValueSetThrowsDuringConstruction() {
        class BadSet<E> extends AbstractSet<E> {

            @Override
            public Iterator<E> iterator() {
                return new Iterator<E>() {

                    @Override
                    public boolean hasNext() {
                        return true;
                    }

                    @Override
                    public E next() {
                        throw new RuntimeException("iteration failure");
                    }

                    @Override
                    public void remove() {
                    }
                };
            }

            @Override
            public int size() {
                return 1;
            }
        }
        Map<String, Set<Integer>> src = new HashMap<>();
        src.put("normalKey", new BadSet<>());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> MapUtilities.cloneMapOfSets(src, false));
        assertEquals("iteration failure", ex.getMessage());
    }

    /**
     * Concurrent modification during cloning.
     */
    @Test
    void testConcurrentModificationDuringCloning() {
        class ConcurrentModMap<K, V> extends HashMap<K, V> {

            @Override
            public Set<Entry<K, V>> entrySet() {
                Set<Entry<K, V>> original = super.entrySet();
                return new AbstractSet<Entry<K, V>>() {

                    @Override
                    public Iterator<Entry<K, V>> iterator() {
                        Iterator<Entry<K, V>> it = original.iterator();
                        return new Iterator<Entry<K, V>>() {

                            private boolean first = true;

                            @Override
                            public boolean hasNext() {
                                return it.hasNext();
                            }

                            @Override
                            public Entry<K, V> next() {
                                Entry<K, V> entry = it.next();
                                // On the first call, mutate the underlying map to trigger CME
                                if (first) {
                                    first = false;
                                    // This modification will be detected by the iterator of the original map
                                    ConcurrentModMap.this.put((K) "c", (V) new HashSet<>(Arrays.asList(3)));
                                }
                                return entry;
                            }

                            @Override
                            public void remove() {
                                it.remove();
                            }
                        };
                    }

                    @Override
                    public int size() {
                        return original.size();
                    }
                };
            }
        }
        ConcurrentModMap<String, Set<Integer>> src = new ConcurrentModMap<>();
        src.put("a", new HashSet<>(Arrays.asList(1)));
        src.put("b", new HashSet<>(Arrays.asList(2)));
        assertThrows(ConcurrentModificationException.class, () -> MapUtilities.cloneMapOfSets(src, false));
    }
}
