package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class SealableSetTest {

    private AtomicBoolean sealedFlag;
    private Supplier<Boolean> sealedSupplier;

    @Before
    public void setUp() {
        sealedFlag = new AtomicBoolean(false);
        sealedSupplier = sealedFlag::get;
    }

    /** Helper to create a SealableSet that wraps a supplied HashSet */
    private <T> SealableSet<T> wrap(Set<T> backing) {
        return new SealableSet<>(backing, sealedSupplier);
    }

    /** Helper to create a SealableSet from a collection (uses internal ConcurrentSet) */
    private <T> SealableSet<T> fromCollection(Collection<T> col) {
        return new SealableSet<>(col, sealedSupplier);
    }

    @Test
    public void testImmutableApisDelegateToBackingSet() {
        Set<String> backing = new HashSet<>();
        backing.add("alpha");
        backing.add("beta");

        SealableSet<String> set = wrap(backing);

        // size / isEmpty / contains
        assertEquals(2, set.size());
        assertFalse(set.isEmpty());
        assertTrue(set.contains("alpha"));
        assertFalse(set.contains("gamma"));

        // toArray
        Object[] arr = set.toArray();
        assertEquals(2, arr.length);
        assertTrue(backing.contains(arr[0]) && backing.contains(arr[1]));

        // toArray(T[])
        String[] strArr = set.toArray(new String[0]);
        assertEquals(2, strArr.length);
        assertTrue(backing.contains(strArr[0]) && backing.contains(strArr[1]));

        // containsAll
        Collection<String> sub = new ArrayList<>();
        sub.add("alpha");
        assertTrue(set.containsAll(sub));

        // equals / hashCode / toString
        assertTrue(set.equals(backing));
        assertEquals(backing.hashCode(), set.hashCode());
        assertEquals(backing.toString(), set.toString());
    }

    @Test
    public void testMutableOperationsWhenUnsealed() {
        SealableSet<Integer> set = fromCollection(new ArrayList<Integer>());

        // add
        assertTrue(set.add(1));
        assertTrue(set.add(2));
        assertFalse(set.add(1)); // duplicate

        // remove
        assertTrue(set.remove(Integer.valueOf(1)));
        assertFalse(set.remove(Integer.valueOf(99)));

        // addAll
        Collection<Integer> more = new ArrayList<>();
        more.add(3);
        more.add(4);
        assertTrue(set.addAll(more));
        assertFalse(set.addAll(more)); // already present

        // removeAll
        Collection<Integer> toRemove = new ArrayList<>();
        toRemove.add(2);
        toRemove.add(99);
        assertTrue(set.removeAll(toRemove));
        assertFalse(set.removeAll(toRemove)); // nothing left to remove

        // retainAll
        Collection<Integer> retain = new ArrayList<>();
        retain.add(3);
        retain.add(5);
        assertTrue(set.retainAll(retain));
        assertEquals(1, set.size());
        assertTrue(set.contains(3));

        // clear
        set.clear();
        assertTrue(set.isEmpty());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddWhenSealedThrows() {
        sealedFlag.set(true);
        SealableSet<String> set = fromCollection(new ArrayList<>());
        set.add("shouldFail");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveWhenSealedThrows() {
        SealableSet<String> set = fromCollection(new ArrayList<>());
        set.add("a");
        sealedFlag.set(true);
        set.remove("a");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddAllWhenSealedThrows() {
        sealedFlag.set(true);
        SealableSet<String> set = fromCollection(new ArrayList<>());
        Collection<String> col = new ArrayList<>();
        col.add("x");
        set.addAll(col);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveAllWhenSealedThrows() {
        SealableSet<String> set = fromCollection(new ArrayList<>());
        set.add("y");
        sealedFlag.set(true);
        Collection<String> col = new ArrayList<>();
        col.add("y");
        set.removeAll(col);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRetainAllWhenSealedThrows() {
        SealableSet<String> set = fromCollection(new ArrayList<>());
        set.add("z");
        sealedFlag.set(true);
        Collection<String> col = new ArrayList<>();
        col.add("z");
        set.retainAll(col);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testClearWhenSealedThrows() {
        SealableSet<String> set = fromCollection(new ArrayList<>());
        set.add("c");
        sealedFlag.set(true);
        set.clear();
    }

    @Test
    public void testIteratorAndRemoveBehavior() {
        SealableSet<String> set = fromCollection(new ArrayList<>());
        set.add("one");
        set.add("two");
        set.add("three");

        // iterator should traverse all elements
        Iterator<String> it = set.iterator();
        Set<String> seen = new HashSet<>();
        while (it.hasNext()) {
            seen.add(it.next());
        }
        assertEquals(set.size(), seen.size());
        assertTrue(seen.containsAll(set));

        // iterator.remove works when unsealed
        Iterator<String> it2 = set.iterator();
        while (it2.hasNext()) {
            String v = it2.next();
            if ("two".equals(v)) {
                it2.remove();
            }
        }
        assertFalse(set.contains("two"));
        assertEquals(2, set.size());

        // iterator.remove throws when sealed
        sealedFlag.set(true);
        Iterator<String> it3 = set.iterator();
        while (it3.hasNext()) {
            it3.next(); // just advance
        }
        // Now attempt remove
        try {
            it3.remove();
            fail("Expected UnsupportedOperationException from iterator.remove() when sealed");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void testSealAwareEntrySetValueRespectsSeal() {
        // Create a backing set of Map.Entry objects
        Set<Map.Entry<String, Integer>> backing = new HashSet<>();
        backing.add(new AbstractMap.SimpleEntry<>("k1", 10));
        backing.add(new AbstractMap.SimpleEntry<>("k2", 20));

        SealableSet<Map.Entry<String, Integer>> set = wrap(backing);

        // Unsealed: setValue should succeed
        Iterator<Map.Entry<String, Integer>> it = set.iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> entry = it.next();
            if ("k1".equals(entry.getKey())) {
                entry.setValue(100);
            }
        }
        // Verify mutation propagated to backing set
        for (Map.Entry<String, Integer> e : backing) {
            if ("k1".equals(e.getKey())) {
                assertEquals(Integer.valueOf(100), e.getValue());
            }
        }

        // Now seal and verify setValue throws
        sealedFlag.set(true);
        Iterator<Map.Entry<String, Integer>> it2 = set.iterator();
        while (it2.hasNext()) {
            Map.Entry<String, Integer> entry = it2.next();
            if ("k2".equals(entry.getKey())) {
                try {
                    entry.setValue(200);
                    fail("Expected UnsupportedOperationException when setting value on sealed entry");
                } catch (UnsupportedOperationException e) {
                    // expected
                }
            }
        }
    }

    @Test
    public void testDynamicSealToggleAllowsAndPreventsMutations() {
        SealableSet<String> set = fromCollection(new ArrayList<>());

        // initially unsealed
        assertTrue(set.add("first"));
        assertTrue(set.contains("first"));

        // seal and verify mutation blocked
        sealedFlag.set(true);
        try {
            set.add("second");
            fail("Should have thrown when adding while sealed");
        } catch (UnsupportedOperationException ignored) {}

        // unseal again and verify mutation works
        sealedFlag.set(false);
        assertTrue(set.add("second"));
        assertTrue(set.contains("second"));
    }

    @Test
    public void testConstructorWithCollectionCopiesElements() {
        Collection<String> source = new ArrayList<>();
        source.add("a");
        source.add("b");
        source.add("c");

        SealableSet<String> set = new SealableSet<>(source, sealedSupplier);
        // Mutate source after construction – should NOT affect the SealableSet
        source.clear();
        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));
    }

    @Test
    public void testConstructorWithSetWrapsDirectly() {
        Set<String> backing = new HashSet<>();
        backing.add("x");
        backing.add("y");

        SealableSet<String> set = new SealableSet<>(backing, sealedSupplier);
        // Mutate backing while unsealed – changes should be reflected
        assertTrue(set.remove("x"));
        assertFalse(backing.contains("x"));
        assertEquals(1, set.size());

        // Seal and try to modify via SealableSet – should fail, but direct backing changes still work
        sealedFlag.set(true);
        try {
            set.add("z");
            fail("Expected exception when adding while sealed");
        } catch (UnsupportedOperationException ignored) {}

        // Directly modify backing set (simulating external mutable access)
        backing.add("z");
        assertTrue(set.contains("z"));
    }
}
