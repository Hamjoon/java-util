package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class SealableMapTest {

    private Supplier<Boolean> mutableSupplier;
    private Supplier<Boolean> sealedSupplier;
    private AtomicBoolean mutableFlag;
    private AtomicBoolean sealedFlag;

    @Before
    public void setUp() {
        mutableFlag = new AtomicBoolean(false); // false = mutable
        sealedFlag = new AtomicBoolean(true);   // true = sealed
        mutableSupplier = mutableFlag::get;
        sealedSupplier = sealedFlag::get;
    }

    /** Helper to create a map with a few entries */
    private Map<String, Integer> createSampleMap() {
        Map<String, Integer> m = new HashMap<>();
        m.put("one", 1);
        m.put("two", 2);
        m.put("three", 3);
        return m;
    }

    /* -------------------------------------------------------
     *  Tests for mutable behavior when the map is NOT sealed
     * ------------------------------------------------------*/

    @Test
    public void testMutableOperationsWhenUnsealed() {
        // start unsealed (mutable)
        mutableFlag.set(false);
        SealableMap<String, Integer> sm = new SealableMap<>(mutableSupplier);

        // put
        assertNull(sm.put("a", 10));
        assertEquals(Integer.valueOf(10), sm.get("a"));
        assertEquals(1, sm.size());

        // put another
        sm.put("b", 20);
        assertTrue(sm.containsKey("b"));
        assertTrue(sm.containsValue(20));

        // remove
        Integer removed = sm.remove("a");
        assertEquals(Integer.valueOf(10), removed);
        assertFalse(sm.containsKey("a"));
        assertEquals(1, sm.size());

        // putAll
        Map<String, Integer> another = new HashMap<>();
        another.put("c", 30);
        another.put("d", 40);
        sm.putAll(another);
        assertEquals(3, sm.size());
        assertTrue(sm.containsKey("c"));
        assertTrue(sm.containsKey("d"));

        // clear
        sm.clear();
        assertTrue(sm.isEmpty());
    }

    /* -------------------------------------------------------
     *  Tests for immutable behavior when the map is sealed
     * ------------------------------------------------------*/

    @Test(expected = UnsupportedOperationException.class)
    public void testPutWhenSealedThrows() {
        sealedFlag.set(true);
        SealableMap<String, Integer> sm = new SealableMap<>(sealedSupplier);
        sm.put("x", 99); // should throw
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveWhenSealedThrows() {
        sealedFlag.set(true);
        SealableMap<String, Integer> sm = new SealableMap<>(sealedSupplier);
        sm.remove("any"); // should throw
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPutAllWhenSealedThrows() {
        sealedFlag.set(true);
        SealableMap<String, Integer> sm = new SealableMap<>(sealedSupplier);
        sm.putAll(Collections.singletonMap("k", 1)); // should throw
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testClearWhenSealedThrows() {
        sealedFlag.set(true);
        SealableMap<String, Integer> sm = new SealableMap<>(sealedSupplier);
        sm.clear(); // should throw
    }

    /* -------------------------------------------------------
     *  Tests for view collections (keySet, values, entrySet)
     *  when the map is sealed – they must also reject modifications.
     * ------------------------------------------------------*/

    @Test
    public void testKeySetModificationWhenSealedThrows() {
        // Populate a normal map and wrap it
        Map<String, Integer> backing = createSampleMap();
        sealedFlag.set(true);
        SealableMap<String, Integer> sm = new SealableMap<>(backing, sealedSupplier);

        Set<String> keys = sm.keySet();

        // attempt to remove via Set#remove
        try {
            keys.remove("one");
            fail("Expected UnsupportedOperationException from keySet.remove when sealed");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        // attempt to remove via iterator
        try {
            Iterator<String> it = keys.iterator();
            it.next();
            it.remove();
            fail("Expected UnsupportedOperationException from iterator.remove when sealed");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void testValuesModificationWhenSealedThrows() {
        Map<String, Integer> backing = createSampleMap();
        sealedFlag.set(true);
        SealableMap<String, Integer> sm = new SealableMap<>(backing, sealedSupplier);

        Collection<Integer> values = sm.values();

        // attempt to add a new value
        try {
            // values is a SealableList which implements Collection; add should throw
            ((List<Integer>) values).add(99);
            fail("Expected UnsupportedOperationException from values.add when sealed");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        // attempt to remove via iterator
        try {
            Iterator<Integer> it = values.iterator();
            it.next();
            it.remove();
            fail("Expected UnsupportedOperationException from iterator.remove on values when sealed");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void testEntrySetModificationWhenSealedThrows() {
        Map<String, Integer> backing = createSampleMap();
        sealedFlag.set(true);
        SealableMap<String, Integer> sm = new SealableMap<>(backing, sealedSupplier);

        Set<Map.Entry<String, Integer>> entries = sm.entrySet();

        // attempt to remove via Set#remove
        Map.Entry<String, Integer> anyEntry = entries.iterator().next();
        try {
            entries.remove(anyEntry);
            fail("Expected UnsupportedOperationException from entrySet.remove when sealed");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        // attempt to remove via iterator
        try {
            Iterator<Map.Entry<String, Integer>> it = entries.iterator();
            it.next();
            it.remove();
            fail("Expected UnsupportedOperationException from iterator.remove on entrySet when sealed");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    /* -------------------------------------------------------
     *  Tests for read‑only methods – they should delegate to the
     *  underlying map regardless of sealed state.
     * ------------------------------------------------------*/

    @Test
    public void testReadOnlyMethodsUnsealed() {
        mutableFlag.set(false);
        SealableMap<String, Integer> sm = new SealableMap<>(mutableSupplier);
        sm.putAll(createSampleMap());

        assertEquals(3, sm.size());
        assertFalse(sm.isEmpty());
        assertTrue(sm.containsKey("two"));
        assertTrue(sm.containsValue(3));
        assertEquals(Integer.valueOf(2), sm.get("two"));
        assertEquals(createSampleMap().toString(), sm.toString());
    }

    @Test
    public void testReadOnlyMethodsSealed() {
        // Fill a backing map first
        Map<String, Integer> backing = createSampleMap();
        sealedFlag.set(true);
        SealableMap<String, Integer> sm = new SealableMap<>(backing, sealedSupplier);

        // read‑only queries must work
        assertEquals(3, sm.size());
        assertFalse(sm.isEmpty());
        assertTrue(sm.containsKey("three"));
        assertTrue(sm.containsValue(1));
        assertEquals(Integer.valueOf(1), sm.get("one"));
        assertEquals(backing.toString(), sm.toString());
    }

    /* -------------------------------------------------------
     *  Tests for equals() and hashCode() – they should delegate
     *  to the wrapped map.
     * ------------------------------------------------------*/

    @Test
    public void testEqualsAndHashCode() {
        Map<String, Integer> backing = createSampleMap();
        SealableMap<String, Integer> sm = new SealableMap<>(backing, mutableSupplier);

        // Equality with the original map
        assertTrue(sm.equals(backing));
        assertTrue(backing.equals(sm));

        // Equality with another SealableMap wrapping an equal map
        SealableMap<String, Integer> sm2 = new SealableMap<>(new HashMap<>(backing), mutableSupplier);
        assertTrue(sm.equals(sm2));
        assertTrue(sm2.equals(sm));

        // Hash codes must match
        assertEquals(backing.hashCode(), sm.hashCode());
        assertEquals(sm2.hashCode(), sm.hashCode());

        // Inequality check
        sm2.put("extra", 99);
        assertFalse(sm.equals(sm2));
    }

    /* -------------------------------------------------------
     *  Additional sanity checks for entrySet contents
     * ------------------------------------------------------*/

    @Test
    public void testEntrySetContents() {
        SealableMap<String, Integer> sm = new SealableMap<>(mutableSupplier);
        sm.putAll(createSampleMap());

        Set<Map.Entry<String, Integer>> entries = sm.entrySet();
        assertEquals(3, entries.size());

        // Verify that each entry matches the underlying map
        for (Map.Entry<String, Integer> e : entries) {
            assertEquals(sm.get(e.getKey()), e.getValue());
        }
    }
}
