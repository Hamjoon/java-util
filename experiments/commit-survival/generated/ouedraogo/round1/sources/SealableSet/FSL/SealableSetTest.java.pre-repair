package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link SealableSet}.
 *
 * The tests cover:
 * <ul>
 *   <li>behaviour of mutable operations when the set is sealed vs. unsealed</li>
 *   <li>iterator semantics, including {@code remove()} and the {@link SealableSet.SealAwareEntry} wrapper</li>
 *   <li>delegated immutable methods (equals, hashCode, toString, contains, etc.)</li>
 *   <li>edge‑cases such as null elements and empty collections</li>
 * </ul>
 *
 * The {@link MutableSupplier} is a tiny mutable {@link Supplier} implementation used to flip the
 * sealed state during a test.
 */
public class SealableSetTest {

    /** Simple mutable Supplier<Boolean> used to toggle the sealed state. */
    private static class MutableSupplier implements Supplier<Boolean> {
        private volatile boolean value;

        MutableSupplier(boolean initial) { this.value = initial; }

        @Override public Boolean get() { return value; }

        void set(boolean v) { this.value = v; }
    }

    private MutableSupplier mutableSupplier;

    @Before
    public void setUp() {
        // start each test with the set *unsealed* (mutable)
        mutableSupplier = new MutableSupplier(false);
    }

    /* --------------------------------------------------------------
     *  Immutable delegated methods
     * -------------------------------------------------------------- */
    @Test
    public void testEqualsHashCodeAndToString() {
        Set<String> backing = new HashSet<>();
        backing.add("alpha");
        backing.add("beta");

        SealableSet<String> sealable = new SealableSet<>(backing, mutableSupplier);

        // equals should delegate to the underlying set
        assertTrue(sealable.equals(backing));
        assertTrue(backing.equals(sealable));

        // hashCode should be identical
        assertEquals(backing.hashCode(), sealable.hashCode());

        // toString should reflect the backing set's toString()
        assertEquals(backing.toString(), sealable.toString());
    }

    @Test
    public void testContainsAndContainsAll() {
        Set<Integer> backing = new HashSet<>();
        backing.add(1);
        backing.add(2);
        backing.add(3);

        SealableSet<Integer> sealable = new SealableSet<>(backing, mutableSupplier);

        assertTrue(sealable.contains(2));
        assertFalse(sealable.contains(99));

        Collection<Integer> subset = new ArrayList<>();
        subset.add(1);
        subset.add(3);
        assertTrue(sealable.containsAll(subset));

        subset.add(42);
        assertFalse(sealable.containsAll(subset));
    }

    @Test
    public void testSizeIsEmptyAndToArray() {
        Set<String> backing = new HashSet<>();
        SealableSet<String> sealable = new SealableSet<>(backing, mutableSupplier);

        assertTrue(sealable.isEmpty());
        assertEquals(0, sealable.size());

        backing.add("x");
        backing.add("y");

        assertFalse(sealable.isEmpty());
        assertEquals(2, sealable.size());

        Object[] arr = sealable.toArray();
        assertEquals(2, arr.length);
        assertTrue(backing.contains(arr[0]));
        assertTrue(backing.contains(arr[1]));

        String[] typed = sealable.toArray(new String[0]);
        assertEquals(2, typed.length);
        assertTrue(backing.contains(typed[0]));
        assertTrue(backing.contains(typed[1]));
    }

    /* --------------------------------------------------------------
     *  Mutable operations when the set is *unsealed* (mutable)
     * -------------------------------------------------------------- */
    @Test
    public void testAddRemoveWhenUnsealed() {
        SealableSet<String> sealable = new SealableSet<>(mutableSupplier);
        assertTrue(sealable.add("one"));
        assertTrue(sealable.contains("one"));
        assertEquals(1, sealable.size());

        // add duplicate – Set semantics return false
        assertFalse(sealable.add("one"));
        assertEquals(1, sealable.size());

        // remove existing element
        assertTrue(sealable.remove("one"));
        assertFalse(sealable.contains("one"));
        assertTrue(sealable.isEmpty());

        // remove non‑existent element
        assertFalse(sealable.remove("missing"));
    }

    @Test
    public void testAddAllRemoveAllRetainAllClearWhenUnsealed() {
        SealableSet<Integer> sealable = new SealableSet<>(mutableSupplier);
        Collection<Integer> initial = java.util.Arrays.asList(1, 2, 3);
        assertTrue(sealable.addAll(initial));
        assertEquals(3, sealable.size());

        // addAll with overlapping collection – should return false because no new elements
        assertFalse(sealable.addAll(java.util.Arrays.asList(2, 3)));

        // removeAll some elements
        assertTrue(sealable.removeAll(java.util.Arrays.asList(2, 4)));
        assertEquals(2, sealable.size());
        assertFalse(sealable.contains(2));

        // retainAll – keep only 1
        assertTrue(sealable.retainAll(java.util.Collections.singleton(1)));
        assertEquals(1, sealable.size());
        assertTrue(sealable.contains(1));

        // clear
        sealable.clear();
        assertTrue(sealable.isEmpty());
    }

    /* --------------------------------------------------------------
     *  Mutable operations when the set is *sealed* (immutable)
     * -------------------------------------------------------------- */
    @Test(expected = UnsupportedOperationException.class)
    public void testAddWhenSealedThrows() {
        mutableSupplier.set(true); // seal
        SealableSet<String> sealable = new SealableSet<>(mutableSupplier);
        sealable.add("should-fail");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveWhenSealedThrows() {
        SealableSet<String> sealable = new SealableSet<>(mutableSupplier);
        sealable.add("temp");
        mutableSupplier.set(true); // seal now
        sealable.remove("temp");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddAllWhenSealedThrows() {
        mutableSupplier.set(true);
        SealableSet<Integer> sealable = new SealableSet<>(mutableSupplier);
        sealable.addAll(java.util.Collections.singleton(1));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveAllWhenSealedThrows() {
        SealableSet<Integer> sealable = new SealableSet<>(mutableSupplier);
        sealable.add(5);
        mutableSupplier.set(true);
        sealable.removeAll(java.util.Collections.singleton(5));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRetainAllWhenSealedThrows() {
        SealableSet<Integer> sealable = new SealableSet<>(mutableSupplier);
        sealable.add(10);
        mutableSupplier.set(true);
        sealable.retainAll(java.util.Collections.singleton(10));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testClearWhenSealedThrows() {
        SealableSet<String> sealable = new SealableSet<>(mutableSupplier);
        sealable.add("a");
        mutableSupplier.set(true);
        sealable.clear();
    }

    /* --------------------------------------------------------------
     *  Iterator behaviour
     * -------------------------------------------------------------- */
    @Test
    public void testIteratorHasNextNextAndRemoveWhenUnsealed() {
        SealableSet<String> sealable = new SealableSet<>(mutableSupplier);
        sealable.add("alpha");
        sealable.add("beta");
        sealable.add("gamma");

        Iterator<String> it = sealable.iterator();

        // iterate and remove each element
        int count = 0;
        while (it.hasNext()) {
            String val = it.next();
            assertNotNull(val);
            it.remove(); // should succeed while unsealed
            count++;
        }
        assertEquals(3, count);
        assertTrue(sealable.isEmpty());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testIteratorRemoveWhenSealedThrows() {
        SealableSet<String> sealable = new SealableSet<>(mutableSupplier);
        sealable.add("only");
        mutableSupplier.set(true); // seal before obtaining iterator
        Iterator<String> it = sealable.iterator();
        assertTrue(it.hasNext());
        it.next();
        it.remove(); // must throw
    }

    @Test
    public void testIteratorReturnsSealAwareEntryForMapEntries() {
        // Prepare a backing Set containing Map.Entry objects
        Set<Map.Entry<String, Integer>> backing = new HashSet<>();
        Map.Entry<String, Integer> entry1 = new AbstractMap.SimpleEntry<>("k1", 100);
        Map.Entry<String, Integer> entry2 = new AbstractMap.SimpleEntry<>("k2", 200);
        backing.add(entry1);
        backing.add(entry2);

        SealableSet<Map.Entry<String, Integer>> sealable =
                new SealableSet<>(backing, mutableSupplier);

        // Verify that iterator wraps each entry in SealAwareEntry
        int wrappedCount = 0;
        for (Iterator<Map.Entry<String, Integer>> it = sealable.iterator(); it.hasNext(); ) {
            Map.Entry<String, Integer> e = it.next();
            assertTrue("Iterator must return a SealAwareEntry", e instanceof SealableSet.SealAwareEntry);
            SealableSet.SealAwareEntry<String, Integer> sealAware = (SealableSet.SealAwareEntry<String, Integer>) e;

            // read operations must delegate correctly
            assertTrue(e.getKey().startsWith("k"));
            assertNotNull(e.getValue());

            // modify value while unsealed – should succeed
            Integer old = sealAware.setValue(e.getValue() + 1);
            assertEquals(e.getValue() - 1, old);
            assertEquals(e.getValue(), Integer.valueOf(e.getValue()));

            // now seal and verify setValue throws
            mutableSupplier.set(true);
            try {
                sealAware.setValue(0);
                fail("Expected UnsupportedOperationException when setValue on sealed entry");
            } catch (UnsupportedOperationException ex) {
                // expected
            }
            // reset to unsealed for next iteration
            mutableSupplier.set(false);
            wrappedCount++;
        }
        assertEquals(2, wrappedCount);
    }

    /* --------------------------------------------------------------
     *  Null handling (if underlying Set permits it)
     * -------------------------------------------------------------- */
    @Test
    public void testAddNullWhenUnsealed() {
        SealableSet<String> sealable = new SealableSet<>(mutableSupplier);
        // ConcurrentSet (the default backing) permits null – this test simply asserts behaviour
        sealable.add(null);
        assertTrue(sealable.contains(null));
        assertEquals(1, sealable.size());

        sealable.remove(null);
        assertFalse(sealable.contains(null));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddNullWhenSealedThrows() {
        mutableSupplier.set(true);
        SealableSet<String> sealable = new SealableSet<>(mutableSupplier);
        sealable.add(null);
    }
}
