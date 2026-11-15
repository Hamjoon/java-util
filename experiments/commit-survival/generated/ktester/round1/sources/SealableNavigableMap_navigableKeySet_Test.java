package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableNavigableMap_navigableKeySet_Test {

    private NavigableMap<String, Integer> backingMap;

    private SealableNavigableMap<String, Integer> sealableMap;

    private Supplier<Boolean> sealedSupplier;

    private boolean sealed;

    /**
     * Simple thread‑safe NavigableMap implementation used by the test suite.
     * It extends {@link ConcurrentSkipListMap} and adds null‑key support for the library's
     * specific use‑case (the real library uses a custom class, but this suffices for testing).
     */
    class ConcurrentNavigableMapNullSafe<K, V> extends ConcurrentSkipListMap<K, V> {

        @Override
        public V put(K key, V value) {
            // Allow null keys for test purposes (skiplist normally rejects them)
            if (key == null) {
                // store a special entry for null key
                return super.put((K) NullKey.INSTANCE, value);
            }
            return super.put(key, value);
        }

        @Override
        public boolean containsKey(Object key) {
            return super.containsKey(key == null ? NullKey.INSTANCE : key);
        }

        @Override
        public V get(Object key) {
            return super.get(key == null ? NullKey.INSTANCE : key);
        }

        @Override
        public V remove(Object key) {
            return super.remove(key == null ? NullKey.INSTANCE : key);
        }

        @Override
        public NavigableSet<K> navigableKeySet() {
            // Return a view that translates the internal NullKey placeholder back to null
            return new AbstractSet<K>() {

                @Override
                public Iterator<K> iterator() {
                    Iterator<K> it = ConcurrentNavigableMapNullSafe.super.navigableKeySet().iterator();
                    return new Iterator<K>() {

                        @Override
                        public boolean hasNext() {
                            return it.hasNext();
                        }

                        @Override
                        public K next() {
                            K k = it.next();
                            return k == NullKey.INSTANCE ? null : k;
                        }

                        @Override
                        public void remove() {
                            it.remove();
                        }
                    };
                }

                @Override
                public int size() {
                    return ConcurrentNavigableMapNullSafe.super.navigableKeySet().size();
                }

                @Override
                public boolean contains(Object o) {
                    return ConcurrentNavigableMapNullSafe.this.containsKey(o);
                }
            };
        }

        /**
         * Marker object used to store a null key internally.
         */
        private enum NullKey {

            INSTANCE
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        sealed = false;
        sealedSupplier = () -> sealed;
        // Use a concrete NavigableMap implementation that permits null keys for some tests.
        backingMap = new ConcurrentNavigableMapNullSafe<>();
        backingMap.put("a", 1);
        backingMap.put("b", 2);
        backingMap.put("c", 3);
        // ensure null key handling works for the library's map
        backingMap.put(null, null);
        sealableMap = new SealableNavigableMap<>(backingMap, sealedSupplier);
    }

    /* --------------------------------------------------------------
     *  Parameterized test source for groups that share similar logic
     * -------------------------------------------------------------- */
    private static Stream<Arguments> emptyMapProvider() {
        return Stream.of(Arguments.of(new ConcurrentNavigableMapNullSafe<>()));
    }

    @Test
    void navigableKeySetReflectsUnderlyingMapChanges() {
        NavigableSet<String> keySet = sealableMap.navigableKeySet();
        // initial contents
        assertTrue(keySet.contains("one"));
        assertTrue(keySet.contains("two"));
        assertTrue(keySet.contains("three"));
        assertTrue(keySet.contains(null));
        // add a new entry through the sealable map and verify the view sees it
        sealableMap.put("four", 4);
        assertTrue(keySet.contains("four"));
        assertEquals(5, keySet.size());
        // remove an entry through the sealable map and verify the view updates
        sealableMap.remove("two");
        assertFalse(keySet.contains("two"));
        assertEquals(4, keySet.size());
    }

    @Test
    void navigableKeySetIsImmutableWhenSealed() {
        NavigableSet<String> keySet = sealableMap.navigableKeySet();
        // seal the map
        sealed = true;
        assertTrue(sealedSupplier.get());
        // attempts to modify the set must throw
        assertThrows(UnsupportedOperationException.class, () -> keySet.remove("one"));
        assertThrows(UnsupportedOperationException.class, () -> keySet.add("five"));
        assertThrows(UnsupportedOperationException.class, () -> keySet.clear());
    }

    @Test
    void iteratorRemoveRespectsSealedState() {
        NavigableSet<String> keySet = sealableMap.navigableKeySet();
        Iterator<String> it = keySet.iterator();
        // iterate once while unsealed – removal should be allowed
        assertTrue(it.hasNext());
        String first = it.next();
        // ensure unsealed
        sealed = false;
        // should succeed
        it.remove();
        assertFalse(keySet.contains(first));
        assertEquals(3, keySet.size());
        // iterate again, seal the map before calling remove
        it = keySet.iterator();
        assertTrue(it.hasNext());
        // advance
        it.next();
        sealed = true;
        assertThrows(UnsupportedOperationException.class, it::remove);
    }

    @Test
    void iteratorRemoveWorksWhenUnsealed() {
        NavigableSet<String> keySet = sealableMap.navigableKeySet();
        Iterator<String> it = keySet.iterator();
        // remove each element while the map stays unsealed
        while (it.hasNext()) {
            it.next();
            sealed = false;
            it.remove();
        }
        assertTrue(keySet.isEmpty());
        assertEquals(0, sealableMap.size());
    }

    @Test
    void navigableKeySetHandlesNullKey() {
        NavigableSet<String> keySet = sealableMap.navigableKeySet();
        // The map was initialised with a null key; the set view should contain it
        assertTrue(keySet.contains(null));
        // Removing the null key when unsealed should succeed
        sealed = false;
        assertTrue(keySet.remove(null));
        assertFalse(keySet.contains(null));
        assertFalse(sealableMap.containsKey(null));
    }

    /**
     * Empty backing map should yield an empty set.
     */
    @Test
    void emptyBackingMapReturnsEmptyNavigableSet() {
        NavigableMap<String, Integer> emptyMap = new ConcurrentNavigableMapNullSafe<>();
        SealableNavigableMap<String, Integer> emptySealable = new SealableNavigableMap<>(emptyMap, sealedSupplier);
        NavigableSet<String> set = emptySealable.navigableKeySet();
        assertTrue(set.isEmpty(), "Set should be empty for empty backing map");
        assertEquals(0, set.size());
    }

    /**
     * When unsealed, the set is mutable – add and remove affect the underlying map.
     */
    @Test
    void unsealedSetAllowsAddAndRemove() {
        sealed = false;
        NavigableSet<String> set = sealableMap.navigableKeySet();
        // add a new key – map should contain the key with a null value
        assertTrue(set.add("x"));
        assertTrue(set.contains("x"));
        assertTrue(sealableMap.containsKey("x"));
        assertNull(sealableMap.get("x"));
        // remove an existing key
        assertTrue(set.remove("a"));
        assertFalse(set.contains("a"));
        assertFalse(sealableMap.containsKey("a"));
    }

    /**
     * When sealed, any mutating operation must throw UnsupportedOperationException.
     */
    @Test
    void sealedSetMutationsThrow() {
        sealed = true;
        NavigableSet<String> set = sealableMap.navigableKeySet();
        assertAll(() -> assertThrows(UnsupportedOperationException.class, () -> set.add("new")), () -> assertThrows(UnsupportedOperationException.class, () -> set.remove("a")), () -> assertThrows(UnsupportedOperationException.class, set::clear));
    }

    /**
     * Read‑only operations must work even when sealed.
     */
    @Test
    void sealedSetReadOnlyOperationsWork() {
        sealed = true;
        NavigableSet<String> set = sealableMap.navigableKeySet();
        assertTrue(set.contains("b"));
        // a,b,c,null
        assertEquals(4, set.size());
        Iterator<String> it = set.iterator();
        List<String> iterated = new ArrayList<>();
        it.forEachRemaining(iterated::add);
        // natural order with null first (TreeMap permits null as first key)
        List<String> expected = new ArrayList<>(Arrays.asList(null, "a", "b", "c"));
        assertEquals(expected, iterated);
    }

    /**
     * Iterator remove works when map is unsealed.
     */
    @Test
    void iteratorRemoveWhenUnsealedDeletesFromMap() {
        sealed = false;
        NavigableSet<String> set = sealableMap.navigableKeySet();
        Iterator<String> it = set.iterator();
        while (it.hasNext()) {
            String key = it.next();
            // should delete from map
            it.remove();
            assertFalse(set.contains(key));
            assertFalse(sealableMap.containsKey(key));
        }
        assertTrue(set.isEmpty());
        assertTrue(sealableMap.isEmpty());
    }

    /**
     * Bulk operations when unsealed.
     */
    @Test
    void bulkOperationsWhenUnsealed() {
        sealed = false;
        NavigableSet<String> set = sealableMap.navigableKeySet();
        // addAll
        assertTrue(set.addAll(Arrays.asList("x", "y")));
        assertTrue(set.containsAll(Arrays.asList("x", "y")));
        // original 4 + 2
        assertEquals(6, set.size());
        assertNull(sealableMap.get("x"));
        assertNull(sealableMap.get("y"));
        // removeAll
        assertTrue(set.removeAll(Arrays.asList("a", "b")));
        assertFalse(set.contains("a"));
        assertFalse(set.contains("b"));
        assertEquals(4, set.size());
        // retainAll
        assertTrue(set.retainAll(Arrays.asList("c", "x")));
        assertEquals(2, set.size());
        assertTrue(set.containsAll(Arrays.asList("c", "x")));
        assertFalse(set.contains("y"));
    }

    /**
     * clear() on sealed set must throw.
     */
    @Test
    void clearOnSealedSetThrows() {
        sealed = true;
        NavigableSet<String> set = sealableMap.navigableKeySet();
        assertThrows(UnsupportedOperationException.class, set::clear);
    }

    /**
     * Iterator edge cases – empty set next() throws NoSuchElementException.
     */
    @Test
    void iteratorNextOnEmptySetThrows() {
        NavigableMap<String, Integer> emptyMap = new ConcurrentNavigableMapNullSafe<>();
        SealableNavigableMap<String, Integer> emptySealable = new SealableNavigableMap<>(emptyMap, sealedSupplier);
        Iterator<String> it = emptySealable.navigableKeySet().iterator();
        assertThrows(NoSuchElementException.class, it::next);
    }

    /**
     * Iterator remove before next() throws IllegalStateException when unsealed.
     */
    @Test
    void iteratorRemoveBeforeNextThrows() {
        sealed = false;
        NavigableSet<String> set = sealableMap.navigableKeySet();
        Iterator<String> it = set.iterator();
        assertThrows(IllegalStateException.class, it::remove);
    }

    /**
     * Iterator remove when sealed throws UnsupportedOperationException.
     */
    @Test
    void iteratorRemoveWhenSealedThrows() {
        sealed = true;
        NavigableSet<String> set = sealableMap.navigableKeySet();
        Iterator<String> it = set.iterator();
        assertTrue(it.hasNext());
        // advance
        it.next();
        assertThrows(UnsupportedOperationException.class, it::remove);
    }

    /**
     * Comparator preservation – the set should expose the same comparator as the backing map.
     */
    @Test
    void comparatorIsPreserved() {
        // Use a TreeMap with a custom comparator (reverse order)
        Comparator<String> reverse = Comparator.reverseOrder();
        NavigableMap<String, Integer> customMap = new TreeMap<>(reverse);
        customMap.put("a", 1);
        customMap.put("b", 2);
        customMap.put("c", 3);
        SealableNavigableMap<String, Integer> customSealable = new SealableNavigableMap<>(customMap, sealedSupplier);
        NavigableSet<String> set = customSealable.navigableKeySet();
        assertEquals(reverse, set.comparator(), "Comparator should be the same as backing map's comparator");
        // Verify iteration order is reverse
        List<String> iterated = new ArrayList<>();
        set.forEach(iterated::add);
        assertEquals(Arrays.asList("c", "b", "a"), iterated);
    }

    /**
     * Concurrent modification – external put after obtaining set is visible via iterator.
     */
    @Test
    void concurrentModificationIsReflected() {
        NavigableSet<String> set = sealableMap.navigableKeySet();
        // modify underlying map directly
        backingMap.put("z", 99);
        assertTrue(set.contains("z"));
        // iterator should see the new element without CME
        Iterator<String> it = set.iterator();
        List<String> collected = new ArrayList<>();
        it.forEachRemaining(collected::add);
        assertTrue(collected.contains("z"));
    }

    /**
     * Adding null key to a map that does not permit nulls should throw NPE.
     */
    @Test
    void addNullKeyToNonNullPermittingMapThrows() {
        // TreeMap with natural ordering does not allow null keys
        NavigableMap<String, Integer> treeMap = new TreeMap<>();
        treeMap.put("a", 1);
        SealableNavigableMap<String, Integer> sealable = new SealableNavigableMap<>(treeMap, sealedSupplier);
        NavigableSet<String> set = sealable.navigableKeySet();
        assertThrows(NullPointerException.class, () -> set.add(null));
    }

    /**
     * Adding element of wrong type should throw ClassCastException.
     */
    @Test
    void addWrongTypeThrowsClassCastException() {
        NavigableSet<String> set = sealableMap.navigableKeySet();
        // The raw type is used to bypass compile‑time checks
        @SuppressWarnings("unchecked")
        Set rawSet = set;
        assertThrows(ClassCastException.class, () -> rawSet.add(123));
    }

    /**
     * Bulk addAll with a null element should propagate NullPointerException.
     */
    @Test
    void bulkAddAllWithNullElementThrows() {
        // Use a TreeMap (no null keys allowed)
        NavigableMap<String, Integer> treeMap = new TreeMap<>();
        treeMap.put("a", 1);
        SealableNavigableMap<String, Integer> sealable = new SealableNavigableMap<>(treeMap, sealedSupplier);
        NavigableSet<String> set = sealable.navigableKeySet();
        assertThrows(NullPointerException.class, () -> set.addAll(Collections.singleton(null)));
    }

    /**
     * Bulk addAll with incompatible type should throw ClassCastException.
     */
    @Test
    void bulkAddAllWithIncompatibleTypeThrows() {
        NavigableSet<String> set = sealableMap.navigableKeySet();
        List<Object> mixed = Arrays.asList("valid", 42);
        // raw set to bypass generics
        @SuppressWarnings("unchecked")
        Set rawSet = set;
        assertThrows(ClassCastException.class, () -> rawSet.addAll(mixed));
    }

    /**
     * Constructor with null backing map should throw NPE on navigableKeySet().
     */
    @Test
    void nullBackingMapThrowsOnNavigableKeySet() {
        SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(null, sealedSupplier);
        assertThrows(NullPointerException.class, map::navigableKeySet);
    }

    /**
     * Constructor with null sealedSupplier should throw NPE on navigableKeySet().
     */
    @Test
    void nullSealedSupplierThrowsOnNavigableKeySet() {
        SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(backingMap, null);
        assertThrows(NullPointerException.class, map::navigableKeySet);
    }

    /**
     * Backing map that throws from navigableKeySet propagates the exception.
     */
    @Test
    void backingMapThrowsPropagated() {
        NavigableMap<String, Integer> throwingMap = new NavigableMap<String, Integer>() {

            @Override
            public NavigableSet<String> navigableKeySet() {
                throw new UnsupportedOperationException("forced");
            }

            // stub other methods minimally
            @Override
            public Entry<String, Integer> lowerEntry(String key) {
                return null;
            }

            @Override
            public String lowerKey(String key) {
                return null;
            }

            @Override
            public Entry<String, Integer> floorEntry(String key) {
                return null;
            }

            @Override
            public String floorKey(String key) {
                return null;
            }

            @Override
            public Entry<String, Integer> ceilingEntry(String key) {
                return null;
            }

            @Override
            public String ceilingKey(String key) {
                return null;
            }

            @Override
            public Entry<String, Integer> higherEntry(String key) {
                return null;
            }

            @Override
            public String higherKey(String key) {
                return null;
            }

            @Override
            public Entry<String, Integer> firstEntry() {
                return null;
            }

            @Override
            public Entry<String, Integer> lastEntry() {
                return null;
            }

            @Override
            public Entry<String, Integer> pollFirstEntry() {
                return null;
            }

            @Override
            public Entry<String, Integer> pollLastEntry() {
                return null;
            }

            @Override
            public Comparator<? super String> comparator() {
                return null;
            }

            @Override
            public String firstKey() {
                return null;
            }

            @Override
            public String lastKey() {
                return null;
            }

            @Override
            public NavigableMap<String, Integer> descendingMap() {
                return null;
            }

            @Override
            public NavigableSet<String> keySet() {
                return null;
            }

            @Override
            public NavigableSet<String> descendingKeySet() {
                return null;
            }

            @Override
            public NavigableMap<String, Integer> subMap(String fromKey, boolean fromInclusive, String toKey, boolean toInclusive) {
                return null;
            }

            @Override
            public NavigableMap<String, Integer> headMap(String toKey, boolean inclusive) {
                return null;
            }

            @Override
            public NavigableMap<String, Integer> tailMap(String fromKey, boolean inclusive) {
                return null;
            }

            @Override
            public SortedMap<String, Integer> subMap(String fromKey, String toKey) {
                return null;
            }

            @Override
            public SortedMap<String, Integer> headMap(String toKey) {
                return null;
            }

            @Override
            public SortedMap<String, Integer> tailMap(String fromKey) {
                return null;
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
                return null;
            }

            @Override
            public Collection<Integer> values() {
                return null;
            }

            @Override
            public Set<Entry<String, Integer>> entrySet() {
                return null;
            }

            @Override
            public boolean isEmpty() {
                return true;
            }

            @Override
            public int size() {
                return 0;
            }
        };
        SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(throwingMap, sealedSupplier);
        assertThrows(UnsupportedOperationException.class, map::navigableKeySet);
    }

    @ParameterizedTest(name = "Empty map {index}")
    @MethodSource("emptyMapProvider")
    void parameterizedEmptyMapTest(NavigableMap<String, Integer> emptyMap) {
        SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(emptyMap, () -> false);
        NavigableSet<String> set = map.navigableKeySet();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }
}
