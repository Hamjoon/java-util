package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class SealableMapTest {

    /** Helper that allows us to flip the sealed state at will. */
    private static class SwitchableSeal implements Supplier<Boolean> {
        private final AtomicBoolean sealed = new AtomicBoolean(false);
        void setSealed(boolean s) { sealed.set(s); }
        @Override public Boolean get() { return sealed.get(); }
    }

    private SwitchableSeal seal;
    private SealableMap<String, Integer> map;

    @Before
    public void setUp() {
        seal = new SwitchableSeal();
        // start unsealed (mutable)
        seal.setSealed(false);
        map = new SealableMap<>(new HashMap<>(), seal);
    }

    /* ==================== Immutable operations (read‑only) ==================== */

    @Test
    public void testSizeAndIsEmpty() {
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());

        map.put("a", 1);
        assertFalse(map.isEmpty());
        assertEquals(1, map.size());

        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testContainsKeyAndValue() {
        map.put("k1", 10);
        map.put("k2", 20);

        assertTrue(map.containsKey("k1"));
        assertFalse(map.containsKey("k3"));

        assertTrue(map.containsValue(20));
        assertFalse(map.containsValue(30));
    }

    @Test
    public void testGetAndToString() {
        map.put("x", 99);
        assertEquals(Integer.valueOf(99), map.get("x"));
        assertNull(map.get("missing"));

        String expected = "{x=99}";
        assertEquals(expected, map.toString());
    }

    @Test
    public void testEqualsAndHashCode() {
        Map<String, Integer> other = new HashMap<>();
        other.put("a", 1);
        other.put("b", 2);

        map.putAll(other);

        assertTrue(map.equals(other));
        assertEquals(other.hashCode(), map.hashCode());

        // Different map should not be equal
        other.put("c", 3);
        assertFalse(map.equals(other));
    }

    @Test
    public void testKeySetViewHonorsSeal() {
        map.put("one", 1);
        map.put("two", 2);
        Set<String> keySet = map.keySet();

        // mutable state – removal should work
        assertTrue(keySet.remove("one"));
        assertFalse(map.containsKey("one"));
        assertEquals(1, map.size());

        // seal the map – further modifications must throw
        seal.setSealed(true);
        try {
            keySet.remove("two");
            fail("Expected UnsupportedOperationException when removing from sealed keySet");
        } catch (UnsupportedOperationException ignored) {}

        // iterator's remove should also be blocked when sealed
        try {
            keySet.iterator().remove();
            fail("Expected UnsupportedOperationException from iterator.remove() on sealed map");
        } catch (UnsupportedOperationException ignored) {}
    }

    @Test
    public void testValuesViewHonorsSeal() {
        map.put("a", 10);
        map.put("b", 20);
        // values() returns a SealableList backed by a copy of the values at call time
        // removal from this list should affect the underlying map while mutable
        assertTrue(map.values().remove(10));
        assertFalse(map.containsKey("a"));
        assertEquals(1, map.size());

        // seal and verify removal is prohibited
        seal.setSealed(true);
        try {
            map.values().remove(20);
            fail("Expected UnsupportedOperationException when removing from sealed values view");
        } catch (UnsupportedOperationException ignored) {}
    }

    @Test
    public void testEntrySetViewHonorsSeal() {
        map.put("k", 5);
        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();

        // mutable – can remove via iterator
        entrySet.iterator().next().setValue(6);
        assertEquals(Integer.valueOf(6), map.get("k"));

        // seal and verify modifications are blocked
        seal.setSealed(true);
        try {
            entrySet.iterator().remove();
            fail("Expected UnsupportedOperationException from entrySet iterator.remove() when sealed");
        } catch (UnsupportedOperationException ignored) {}

        try {
            entrySet.iterator().next().setValue(7);
            fail("Expected UnsupportedOperationException from entry.setValue() when sealed");
        } catch (UnsupportedOperationException ignored) {}
    }

    /* ==================== Mutable operations (write) ==================== */

    @Test
    public void testPutAndRemoveWhenUnsealed() {
        assertNull(map.put("alpha", 100));
        assertEquals(Integer.valueOf(100), map.get("alpha"));

        assertEquals(Integer.valueOf(100), map.remove("alpha"));
        assertNull(map.get("alpha"));
    }

    @Test
    public void testPutAllAndClearWhenUnsealed() {
        Map<String, Integer> source = new HashMap<>();
        source.put("x", 1);
        source.put("y", 2);
        map.putAll(source);
        assertEquals(2, map.size());

        map.clear();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testMutatingMethodsThrowWhenSealed() {
        // populate first while mutable
        map.put("p", 1);
        seal.setSealed(true);

        // put
        try {
            map.put("q", 2);
            fail("Expected UnsupportedOperationException on put when sealed");
        } catch (UnsupportedOperationException ignored) {}

        // remove
        try {
            map.remove("p");
            fail("Expected UnsupportedOperationException on remove when sealed");
        } catch (UnsupportedOperationException ignored) {}

        // putAll
        try {
            map.putAll(Collections.singletonMap("r", 3));
            fail("Expected UnsupportedOperationException on putAll when sealed");
        } catch (UnsupportedOperationException ignored) {}

        // clear
        try {
            map.clear();
            fail("Expected UnsupportedOperationException on clear when sealed");
        } catch (UnsupportedOperationException ignored) {}
    }

    @Test
    public void testSealedStateCanFlipMultipleTimes() {
        // first mutable
        map.put("a", 1);
        assertEquals(Integer.valueOf(1), map.get("a"));

        // seal
        seal.setSealed(true);
        try {
            map.put("b", 2);
            fail("Should not allow put when sealed");
        } catch (UnsupportedOperationException ignored) {}

        // unseal again
        seal.setSealed(false);
        map.put("b", 2);
        assertEquals(Integer.valueOf(2), map.get("b"));
        assertEquals(2, map.size());
    }

    /* ==================== Edge Cases ==================== */

    @Test
    public void testNullKeyAndValueWhenMutable() {
        // HashMap permits null keys and values
        map.put(null, null);
        assertTrue(map.containsKey(null));
        assertTrue(map.containsValue(null));
        assertNull(map.get(null));

        map.put("notNull", null);
        assertTrue(map.containsValue(null));
        assertNull(map.get("notNull"));
    }

    @Test
    public void testNullKeyAndValueWhenSealed() {
        map.put(null, null);
        seal.setSealed(true);
        try {
            map.put(null, 1);
            fail("Expected UnsupportedOperationException when modifying sealed map with null key");
        } catch (UnsupportedOperationException ignored) {}
        try {
            map.remove(null);
            fail("Expected UnsupportedOperationException when removing null key from sealed map");
        } catch (UnsupportedOperationException ignored) {}
    }

    @Test
    public void testEqualsWithNonMapObject() {
        map.put("x", 10);
        assertFalse(map.equals("not a map"));
        assertFalse(map.equals(null));
    }

    @Test
    public void testHashCodeConsistency() {
        int h1 = map.hashCode();
        map.put("a", 1);
        int h2 = map.hashCode();
        assertNotEquals(h1, h2);
        map.remove("a");
        assertEquals(h1, map.hashCode());
    }
}
