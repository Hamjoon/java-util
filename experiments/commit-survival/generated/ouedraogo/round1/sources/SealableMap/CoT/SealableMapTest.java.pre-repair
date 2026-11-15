package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class SealableMapTest {

    // ---------- Helper ----------
    private static Supplier<Boolean> supplierFrom(AtomicBoolean flag) {
        return flag::get;
    }

    // ---------- Fields ----------
    private AtomicBoolean sealedFlag;
    private Supplier<Boolean> sealedSupplier;
    private SealableMap<String, Integer> sealableMap;

    // ---------- Setup ----------
    @Before
    public void setUp() {
        sealedFlag = new AtomicBoolean(false); // start unsealed (mutable)
        sealedSupplier = supplierFrom(sealedFlag);
        sealableMap = new SealableMap<>(sealedSupplier);
    }

    // ---------- Public method signatures ----------
    // Map interface methods exposed by SealableMap
    // boolean equals(Object obj)
    // int hashCode()
    // String toString()
    // int size()
    // boolean isEmpty()
    // boolean containsKey(Object key)
    // boolean containsValue(Object value)
    // V get(Object key)
    // Set<K> keySet()
    // Collection<V> values()
    // Set<Map.Entry<K,V>> entrySet()
    // V put(K key, V value)
    // V remove(Object key)
    // void putAll(Map<? extends K, ? extends V> m)
    // void clear()

    // ---------- Basic functionality tests ----------
    @Test
    public void testPutAndGet() {
        assertNull(sealableMap.put("one", 1));
        assertEquals(Integer.valueOf(1), sealableMap.get("one"));
        // replace existing
        assertEquals(Integer.valueOf(1), sealableMap.put("one", 11));
        assertEquals(Integer.valueOf(11), sealableMap.get("one"));
    }

    @Test
    public void testSizeAndIsEmpty() {
        assertTrue(sealableMap.isEmpty());
        assertEquals(0, sealableMap.size());

        sealableMap.put("a", 10);
        sealableMap.put("b", 20);

        assertFalse(sealableMap.isEmpty());
        assertEquals(2, sealableMap.size());
    }

    @Test
    public void testContainsKeyAndContainsValue() {
        sealableMap.put("x", 100);
        assertTrue(sealableMap.containsKey("x"));
        assertTrue(sealableMap.containsValue(100));

        assertFalse(sealableMap.containsKey("y"));
        assertFalse(sealableMap.containsValue(200));
    }

    @Test
    public void testRemove() {
        sealableMap.put("k", 5);
        assertEquals(Integer.valueOf(5), sealableMap.remove("k"));
        assertNull(sealableMap.get("k"));
        assertEquals(0, sealableMap.size());
    }

    @Test
    public void testClear() {
        sealableMap.put("a", 1);
        sealableMap.put("b", 2);
        sealableMap.clear();
        assertTrue(sealableMap.isEmpty());
        assertEquals(0, sealableMap.size());
    }

    @Test
    public void testPutAll() {
        Map<String, Integer> other = new HashMap<>();
        other.put("p", 7);
        other.put("q", 8);
        sealableMap.putAll(other);
        assertEquals(Integer.valueOf(7), sealableMap.get("p"));
        assertEquals(Integer.valueOf(8), sealableMap.get("q"));
        assertEquals(2, sealableMap.size());
    }

    @Test
    public void testKeySetView() {
        sealableMap.put("k1", 1);
        sealableMap.put("k2", 2);
        Set<String> keys = sealableMap.keySet();
        assertTrue(keys.containsAll(Arrays.asList("k1", "k2")));
        assertEquals(2, keys.size());

        // modify through the view when unsealed
        keys.remove("k1");
        assertFalse(sealableMap.containsKey("k1"));
        assertEquals(1, sealableMap.size());
    }

    @Test
    public void testValuesView() {
        sealableMap.put("a", 10);
        sealableMap.put("b", 20);
        Collection<Integer> vals = sealableMap.values();
        assertTrue(vals.containsAll(Arrays.asList(10, 20)));
        assertEquals(2, vals.size());

        // remove via values collection when unsealed
        vals.remove(10);
        assertFalse(sealableMap.containsValue(10));
        assertEquals(1, sealableMap.size());
    }

    @Test
    public void testEntrySetView() {
        sealableMap.put("x", 100);
        sealableMap.put("y", 200);
        Set<Map.Entry<String, Integer>> entries = sealableMap.entrySet();
        assertEquals(2, entries.size());

        // remove via entrySet when unsealed
        Iterator<Map.Entry<String, Integer>> it = entries.iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> e = it.next();
            if (e.getKey().equals("x")) {
                it.remove();
            }
        }
        assertFalse(sealableMap.containsKey("x"));
        assertEquals(1, sealableMap.size());
    }

    @Test
    public void testEqualsAndHashCode() {
        sealableMap.put("a", 1);
        sealableMap.put("b", 2);

        Map<String, Integer> plain = new HashMap<>();
        plain.put("a", 1);
        plain.put("b", 2);

        assertTrue(sealableMap.equals(plain));
        assertEquals(plain.hashCode(), sealableMap.hashCode());
    }

    @Test
    public void testToString() {
        sealableMap.put("hello", 42);
        String s = sealableMap.toString();
        assertTrue(s.contains("hello"));
        assertTrue(s.contains("42"));
    }

    // ---------- Edge cases & exception handling ----------
    @Test(expected = UnsupportedOperationException.class)
    public void testPutWhenSealedThrows() {
        sealedFlag.set(true); // seal the map
        sealableMap.put("newKey", 123);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveWhenSealedThrows() {
        sealableMap.put("k", 1);
        sealedFlag.set(true);
        sealableMap.remove("k");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPutAllWhenSealedThrows() {
        sealedFlag.set(true);
        Map<String, Integer> other = Collections.singletonMap("x", 9);
        sealableMap.putAll(other);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testClearWhenSealedThrows() {
        sealableMap.put("a", 1);
        sealedFlag.set(true);
        sealableMap.clear();
    }

    @Test
    public void testModificationThroughViewsWhenSealed() {
        sealableMap.put("k1", 1);
        sealableMap.put("k2", 2);

        // seal
        sealedFlag.set(true);

        // keySet remove should throw UnsupportedOperationException
        Set<String> ks = sealableMap.keySet();
        try {
            ks.remove("k1");
            fail("Expected UnsupportedOperationException from keySet.remove when sealed");
        } catch (UnsupportedOperationException ignored) {}

        // values remove should throw
        Collection<Integer> vs = sealableMap.values();
        try {
            vs.remove(1);
            fail("Expected UnsupportedOperationException from values.remove when sealed");
        } catch (UnsupportedOperationException ignored) {}

        // entrySet remove should throw
        Set<Map.Entry<String, Integer>> es = sealableMap.entrySet();
        try {
            Iterator<Map.Entry<String, Integer>> it = es.iterator();
            if (it.hasNext()) {
                it.next();
                it.remove();
                fail("Expected UnsupportedOperationException from entrySet iterator.remove when sealed");
            }
        } catch (UnsupportedOperationException ignored) {}
    }

    @Test
    public void testSealUnsealToggle() {
        // start mutable
        sealableMap.put("a", 1);
        assertEquals(Integer.valueOf(1), sealableMap.get("a"));

        // seal
        sealedFlag.set(true);
        try {
            sealableMap.put("b", 2);
            fail("Expected UnsupportedOperationException while sealed");
        } catch (UnsupportedOperationException ignored) {}

        // unseal again
        sealedFlag.set(false);
        sealableMap.put("b", 2);
        assertEquals(Integer.valueOf(2), sealableMap.get("b"));
        assertEquals(2, sealableMap.size());
    }

    @Test(expected = NullPointerException.class)
    public void testPutAllWithNullMapThrows() {
        sealableMap.putAll(null);
    }

    @Test
    public void testEqualsWithDifferentType() {
        sealableMap.put("x", 10);
        assertFalse(sealableMap.equals("not a map"));
    }

    @Test
    public void testHashCodeConsistencyAfterMutations() {
        int initialHash = sealableMap.hashCode();
        sealableMap.put("k", 5);
        int afterPutHash = sealableMap.hashCode();
        assertNotEquals(initialHash, afterPutHash);

        sealableMap.remove("k");
        int afterRemoveHash = sealableMap.hashCode();
        assertEquals(initialHash, afterRemoveHash);
    }
}
