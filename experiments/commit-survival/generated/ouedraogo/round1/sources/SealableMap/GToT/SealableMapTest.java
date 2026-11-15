package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class SealableMapTest {

    /** A mutable flag that the Supplier reads – allows us to switch between sealed / unsealed at runtime. */
    private AtomicBoolean sealedFlag;
    private Supplier<Boolean> sealedSupplier;

    @Before
    public void setUp() {
        sealedFlag = new AtomicBoolean(false);          // start un‑sealed
        sealedSupplier = sealedFlag::get;               // lambda supplier
    }

    /* --------------------------------------------------------------
     *  Public method list (extracted from SealableMap)
     *  --------------------------------------------------------------
     *  Constructors
     *      SealableMap(Supplier<Boolean> sealedSupplier)
     *      SealableMap(Map<K,V> map, Supplier<Boolean> sealedSupplier)
     *
     *  Immutable (read‑only) methods
     *      boolean equals(Object obj)
     *      int hashCode()
     *      String toString()
     *      int size()
     *      boolean isEmpty()
     *      boolean containsKey(Object key)
     *      boolean containsValue(Object value)
     *      V get(Object key)
     *      Set<K> keySet()
     *      Collection<V> values()
     *      Set<Map.Entry<K,V>> entrySet()
     *
     *  Mutable methods (throw if sealed)
     *      V put(K key, V value)
     *      V remove(Object key)
     *      void putAll(Map<? extends K,? extends V> m)
     *      void clear()
     * -------------------------------------------------------------- */

    /* --------------------------------------------------------------
     *  Basic functionality tests (one per public method)
     * -------------------------------------------------------------- */

    @Test
    public void testConstructorWithSupplierCreatesEmptyMap() {
        SealableMap<String,Integer> map = new SealableMap<>(sealedSupplier);
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testConstructorWithExistingMapWrapsIt() {
        Map<String,Integer> backing = new HashMap<>();
        backing.put("one", 1);
        SealableMap<String,Integer> map = new SealableMap<>(backing, sealedSupplier);
        assertEquals(1, map.size());
        assertTrue(map.containsKey("one"));
        assertEquals(Integer.valueOf(1), map.get("one"));
    }

    @Test
    public void testPutWhenUnsealed() {
        SealableMap<String,Integer> map = new SealableMap<>(sealedSupplier);
        map.put("k", 10);
        assertEquals(Integer.valueOf(10), map.get("k"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPutWhenSealedThrows() {
        sealedFlag.set(true);                               // seal the map
        SealableMap<String,Integer> map = new SealableMap<>(sealedSupplier);
        map.put("k", 10);                                   // should throw
    }

    @Test
    public void testRemoveWhenUnsealed() {
        SealableMap<String,Integer> map = new SealableMap<>(sealedSupplier);
        map.put("k", 5);
        Integer removed = map.remove("k");
        assertEquals(Integer.valueOf(5), removed);
        assertFalse(map.containsKey("k"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveWhenSealedThrows() {
        SealableMap<String,Integer> map = new SealableMap<>(sealedSupplier);
        map.put("k", 5);
        sealedFlag.set(true);
        map.remove("k");                                    // should throw
    }

    @Test
    public void testPutAllWhenUnsealed() {
        SealableMap<String,Integer> map = new SealableMap<>(sealedSupplier);
        Map<String,Integer> src = new HashMap<>();
        src.put("a", 1);
        src.put("b", 2);
        map.putAll(src);
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(1), map.get("a"));
        assertEquals(Integer.valueOf(2), map.get("b"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPutAllWhenSealedThrows() {
        SealableMap<String,Integer> map = new SealableMap<>(sealedSupplier);
        Map<String,Integer> src = new HashMap<>();
        src.put("a", 1);
        sealedFlag.set(true);
        map.putAll(src);                                    // should throw
    }

    @Test
    public void testClearWhenUnsealed() {
        SealableMap<String,Integer> map = new SealableMap<>(sealedSupplier);
        map.put("x", 1);
        map.clear();
        assertTrue(map.isEmpty());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testClearWhenSealedThrows() {
        SealableMap<String,Integer> map = new SealableMap<>(sealedSupplier);
        map.put("x", 1);
        sealedFlag.set(true);
        map.clear();                                        // should throw
    }

    @Test
    public void testImmutableMethodsAlwaysWork() {
        SealableMap<String,Integer> map = new SealableMap<>(sealedSupplier);
        map.put("a", 1);
        sealedFlag.set(true);                               // seal now

        assertEquals(1, map.size());
        assertFalse(map.isEmpty());
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsValue(1));
        assertEquals(Integer.valueOf(1), map.get("a"));
    }

    @Test
    public void testKeySetViewRespectsSealedState() {
        SealableMap<String,Integer> map = new SealableMap<>(sealedSupplier);
        map.put("k1", 1);
        Set<String> ks = map.keySet();
        assertTrue(ks.contains("k1"));

        // mutable when unsealed
        ks.remove("k1");
        assertFalse(map.containsKey("k1"));

        // add back, seal, then attempt mutation via view
        map.put("k1", 1);
        sealedFlag.set(true);
        Set<String> ksSealed = map.keySet();
        try {
            ksSealed.remove("k1");
            fail("Expected UnsupportedOperationException from sealed keySet");
        } catch (UnsupportedOperationException ignored) { }
    }

    @Test
    public void testValuesViewRespectsSealedState() {
        SealableMap<String,Integer> map = new SealableMap<>(sealedSupplier);
        map.put("a", 10);
        Collection<Integer> vals = map.values();
        assertTrue(vals.contains(10));

        // mutable when unsealed
        vals.remove(10);
        assertFalse(map.containsKey("a"));

        // add back, seal, then attempt mutation via view
        map.put("a", 10);
        sealedFlag.set(true);
        Collection<Integer> valsSealed = map.values();
        try {
            valsSealed.remove(10);
            fail("Expected UnsupportedOperationException from sealed values collection");
        } catch (UnsupportedOperationException ignored) { }
    }

    @Test
    public void testEntrySetViewRespectsSealedState() {
        SealableMap<String,Integer> map = new SealableMap<>(sealedSupplier);
        map.put("x", 5);
        Set<Map.Entry<String,Integer>> entries = map.entrySet();
        assertEquals(1, entries.size());

        Map.Entry<String,Integer> entry = entries.iterator().next();
        assertEquals("x", entry.getKey());
        assertEquals(Integer.valueOf(5), entry.getValue());

        // mutable when unsealed
        entries.remove(entry);
        assertFalse(map.containsKey("x"));

        // add back, seal, then attempt mutation via view
        map.put("x", 5);
        sealedFlag.set(true);
        Set<Map.Entry<String,Integer>> sealedEntries = map.entrySet();
        try {
            sealedEntries.clear();
            fail("Expected UnsupportedOperationException from sealed entrySet");
        } catch (UnsupportedOperationException ignored) { }
    }

    @Test
    public void testEqualsHashCodeAndToString() {
        SealableMap<String,Integer> m1 = new SealableMap<>(sealedSupplier);
        SealableMap<String,Integer> m2 = new SealableMap<>(sealedSupplier);
        m1.put("a", 1);
        m2.put("a", 1);

        assertTrue(m1.equals(m2));
        assertEquals(m1.hashCode(), m2.hashCode());
        // The underlying map's toString is used directly
        assertEquals("{a=1}", m1.toString());
    }

    @Test
    public void testNullKeyAndValueHandling() {
        SealableMap<String,Integer> map = new SealableMap<>(sealedSupplier);
        map.put(null, null);
        assertTrue(map.containsKey(null));
        assertTrue(map.containsValue(null));
        assertNull(map.get(null));
    }

    /* --------------------------------------------------------------
     *  Edge‑case / exception tests identified from source analysis
     * --------------------------------------------------------------
     *  1. Mutating through the collection views while the map is sealed.
     *  2. Re‑sealing and un‑sealing repeatedly – ensure state changes are honoured.
     *  3. Using a wrapped map that already contains entries.
     *  4. Null key / null value (ConcurrentHashMapNullSafe permits nulls – verify behaviour).
     * -------------------------------------------------------------- */

    @Test
    public void testRepeatedSealUnsealTransitions() {
        SealableMap<String,Integer> map = new SealableMap<>(sealedSupplier);
        // first mutation while unsealed
        map.put("a", 1);
        // seal and verify mutation is blocked
        sealedFlag.set(true);
        try {
            map.put("b", 2);
            fail("Expected exception while sealed");
        } catch (UnsupportedOperationException ignored) { }
        // unseal again and mutate
        sealedFlag.set(false);
        map.put("b", 2);
        assertEquals(Integer.valueOf(2), map.get("b"));
    }

    @Test
    public void testWrappedMapInitialContentsRemainAccessible() {
        Map<String,Integer> backing = new HashMap<>();
        backing.put("pre", 99);
        SealableMap<String,Integer> map = new SealableMap<>(backing, sealedSupplier);
        assertEquals(Integer.valueOf(99), map.get("pre"));
        assertTrue(map.containsKey("pre"));
        // mutate via sealable map and verify backing map sees change
        map.put("new", 1);
        assertEquals(Integer.valueOf(1), backing.get("new"));
    }
}
