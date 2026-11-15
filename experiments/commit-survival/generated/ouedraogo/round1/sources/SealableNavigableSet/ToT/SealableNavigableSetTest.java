package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link SealableNavigableSet}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Mutable operations when the set is sealed vs. unsealed.</li>
 *   <li>Iterator behaviour (including removal) respecting the sealed state.</li>
 *   <li>View methods (subSet, headSet, tailSet, descendingSet) and their mutability.</li>
 *   <li>Immutable contract methods (equals, hashCode, toString, contains, size, etc.).</li>
 *   <li>Edge cases such as empty set handling and poll operations.</li>
 * </ul>
 *
 * <p>All objects are created via the public constructors of the class under test; no mocking
 * or subclassing of library types is performed.
 */
public class SealableNavigableSetTest {

    /** Supplier that can be toggled to simulate sealing / unsealing. */
    private AtomicBoolean sealedFlag;
    private Supplier<Boolean> sealedSupplier;

    /** A fresh set used in each test (populated with a few integers). */
    private SealableNavigableSet<Integer> set;

    @Before
    public void setUp() {
        sealedFlag = new AtomicBoolean(false);               // start unsealed
        sealedSupplier = sealedFlag::get;
        set = new SealableNavigableSet<>(sealedSupplier);
        // Populate with deterministic data
        set.addAll(Arrays.asList(1, 3, 5, 7, 9));
    }

    /* --------------------------------------------------------------------- *
     *  Immutable API verification (equals, hashCode, toString, query ops) *
     * --------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCode() {
        // Same content, different implementation → should be equal
        NavigableSet<Integer> other = new TreeSet<>(Arrays.asList(1, 3, 5, 7, 9));
        assertTrue(set.equals(other));
        assertEquals(set.hashCode(), other.hashCode());

        // Different content → not equal
        other.add(11);
        assertFalse(set.equals(other));
    }

    @Test
    public void testToStringAndContains() {
        String expected = "[1, 3, 5, 7, 9]";   // TreeSet/ConcurrentNavigableSet order is natural
        assertEquals(expected, set.toString());

        assertTrue(set.contains(5));
        assertFalse(set.contains(2));

        assertTrue(set.containsAll(Arrays.asList(1, 7)));
        assertFalse(set.containsAll(Arrays.asList(1, 2)));
    }

    @Test
    public void testSizeAndIsEmpty() {
        assertEquals(5, set.size());
        assertFalse(set.isEmpty());

        set.clear();
        assertEquals(0, set.size());
        assertTrue(set.isEmpty());
    }

    @Test
    public void testFirstLastAndNavigationalMethods() {
        assertEquals(Integer.valueOf(1), set.first());
        assertEquals(Integer.valueOf(9), set.last());

        assertEquals(Integer.valueOf(3), set.higher(2));
        assertEquals(Integer.valueOf(3), set.ceiling(3));
        assertEquals(Integer.valueOf(5), set.lower(7));
        assertEquals(Integer.valueOf(5), set.floor(5));
    }

    /* --------------------------------------------------- *
     *  Mutating operations when the set is *unsealed*     *
     * --------------------------------------------------- */

    @Test
    public void testAddWhenUnsealed() {
        assertTrue(set.add(11));
        assertTrue(set.contains(11));
        assertEquals(6, set.size());
    }

    @Test
    public void testRemoveWhenUnsealed() {
        assertTrue(set.remove(3));
        assertFalse(set.contains(3));
        assertEquals(4, set.size());
    }

    @Test
    public void testClearWhenUnsealed() {
        set.clear();
        assertTrue(set.isEmpty());
    }

    @Test
    public void testPollFirstAndLastWhenUnsealed() {
        Integer first = set.pollFirst();
        Integer last = set.pollLast();

        assertEquals(Integer.valueOf(1), first);
        assertEquals(Integer.valueOf(9), last);
        assertFalse(set.contains(1));
        assertFalse(set.contains(9));
        assertEquals(3, set.size());
    }

    @Test
    public void testIteratorRemoveWhenUnsealed() {
        Iterator<Integer> it = set.iterator();
        while (it.hasNext()) {
            Integer v = it.next();
            if (v.equals(5)) {
                it.remove();          // should succeed
            }
        }
        assertFalse(set.contains(5));
        assertEquals(4, set.size());
    }

    /* --------------------------------------------------- *
     *  Mutating operations when the set is *sealed*       *
     * --------------------------------------------------- */

    private void seal() {
        sealedFlag.set(true);
    }

    @Test
    public void testAddWhenSealedThrows() {
        seal();
        assertThrows(UnsupportedOperationException.class, () -> set.add(11));
    }

    @Test
    public void testRemoveWhenSealedThrows() {
        seal();
        assertThrows(UnsupportedOperationException.class, () -> set.remove(3));
    }

    @Test
    public void testClearWhenSealedThrows() {
        seal();
        assertThrows(UnsupportedOperationException.class, () -> set.clear());
    }

    @Test
    public void testPollFirstWhenSealedThrows() {
        seal();
        assertThrows(UnsupportedOperationException.class, () -> set.pollFirst());
    }

    @Test
    public void testIteratorRemoveWhenSealedThrows() {
        seal();
        Iterator<Integer> it = set.iterator();
        assertTrue(it.hasNext());
        it.next();                                     // advance once
        assertThrows(UnsupportedOperationException.class, it::remove);
    }

    @Test
    public void testDescendingIteratorRemoveWhenSealedThrows() {
        seal();
        Iterator<Integer> it = set.descendingIterator();
        assertTrue(it.hasNext());
        it.next();
        assertThrows(UnsupportedOperationException.class, it::remove);
    }

    /* --------------------------------------------------- *
     *  View methods – they must respect the sealed flag *
     * --------------------------------------------------- */

    @Test
    public void testSubSetMutabilityRespectsSeal() {
        // Subset covering 3..7 (inclusive both ends)
        NavigableSet<Integer> sub = set.subSet(3, true, 7, true);
        assertEquals(new TreeSet<>(Arrays.asList(3,5,7)), sub);

        // Unsealed – removal works
        assertTrue(sub.remove(5));
        assertFalse(set.contains(5));

        // Re‑populate for sealed scenario
        set.add(5);
        seal();

        // Now any mutating call on the view must fail
        assertThrows(UnsupportedOperationException.class,
                () -> sub.add(6));
        assertThrows(UnsupportedOperationException.class,
                () -> sub.remove(3));
        assertThrows(UnsupportedOperationException.class,
                () -> sub.clear());
    }

    @Test
    public void testHeadSetAndTailSetSealedBehavior() {
        NavigableSet<Integer> head = set.headSet(5, false); // {1,3}
        NavigableSet<Integer> tail = set.tailSet(5, true);  // {5,7,9}

        // Unsealed – mutating head works
        assertTrue(head.add(2));
        assertTrue(set.contains(2));

        // Unsealed – mutating tail works
        assertTrue(tail.remove(9));
        assertFalse(set.contains(9));

        // Seal and verify that subsequent mutations on the views throw
        seal();

        assertThrows(UnsupportedOperationException.class,
                () -> head.add(0));
        assertThrows(UnsupportedOperationException.class,
                () -> tail.remove(5));
    }

    @Test
    public void testDescendingSetSealedBehavior() {
        NavigableSet<Integer> descending = set.descendingSet(); // {9,7,5,3,1}
        assertEquals(Arrays.asList(9,7,5,3,1), new ArrayList<>(descending));

        // Unsealed – removal works
        assertTrue(descending.remove(7));
        assertFalse(set.contains(7));

        // Seal and verify mutation attempts fail
        seal();
        assertThrows(UnsupportedOperationException.class,
                () -> descending.add(11));
        assertThrows(UnsupportedOperationException.class,
                () -> descending.remove(5));
    }

    /* --------------------------------------------------- *
     *  Verify that the sealed flag can change at runtime *
     * --------------------------------------------------- */

    @Test
    public void testDynamicSealToggle() {
        // Initially unsealed – add succeeds
        assertTrue(set.add(11));

        // Seal – further adds fail
        seal();
        assertThrows(UnsupportedOperationException.class, () -> set.add(13));

        // Unseal again – adds succeed again
        sealedFlag.set(false);
        assertTrue(set.add(13));
        assertTrue(set.containsAll(Arrays.asList(11,13)));
    }

    /* --------------------------------------------------- *
     *  Ensure iterator returns proper entry wrappers for Map.Entry elements *
     * --------------------------------------------------- */
    // The underlying ConcurrentNavigableSetNullSafe may store Map.Entry objects when used as a
    // set of entries (e.g., a Set<Map.Entry<K,V>>).  This test demonstrates that the iterator
    // correctly wraps such entries with SealableSet.SealAwareEntry when present.
    @Test
    public void testIteratorWrapsMapEntryWhenPresent() {
        // Build a set of Map.Entry<Integer,String>
        Set<Map.Entry<Integer,String>> entrySet = new HashSet<>();
        entrySet.add(new AbstractMap.SimpleEntry<>(1, "one"));
        entrySet.add(new AbstractMap.SimpleEntry<>(2, "two"));

        SealableNavigableSet<Map.Entry<Integer,String>> entryNavigableSet =
                new SealableNavigableSet<>(entrySet, sealedSupplier);

        // Unsealed – iterator should return SealAwareEntry instances
        Iterator<Map.Entry<Integer,String>> it = entryNavigableSet.iterator();
        while (it.hasNext()) {
            Map.Entry<Integer,String> e = it.next();
            assertTrue("Iterator should wrap entry with SealAwareEntry",
                    e instanceof SealableSet.SealAwareEntry);
        }

        // Seal – attempts to remove via iterator must still throw
        seal();
        Iterator<Map.Entry<Integer,String>> it2 = entryNavigableSet.iterator();
        assertTrue(it2.hasNext());
        it2.next();
        assertThrows(UnsupportedOperationException.class, it2::remove);
    }
}
