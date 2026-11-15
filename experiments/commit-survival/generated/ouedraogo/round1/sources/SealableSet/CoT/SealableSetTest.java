package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link SealableSet}.
 *
 * The suite is built in four logical parts:
 *
 * 1. **Public API extraction** – a comment block that lists every public method
 *    (including constructors) together with its exact signature.
 *
 * 2. **Basic functionality tests** – one simple test per method that verifies the
 *    normal (un‑sealed) behaviour.
 *
 * 3. **Edge‑case & exception analysis** – a description of the corner cases
 *    that the implementation can hit (sealed state, null values, empty
 *    collections, iterator removal, etc.).
 *
 * 4. **Comprehensive edge‑case tests** – tests that target the situations
 *    identified in step 3, especially the `UnsupportedOperationException`
 *    that must be thrown when the set (or a returned {@link SealAwareEntry})
 *    is sealed.
 *
 * All tests use only the public constructors and methods that appear in the
 * source code above; no mocking or external libraries are employed.
 */
public class SealableSetTest {

    /* --------------------------------------------------------------------- *
     * 1. PUBLIC METHODS (including constructors)                           *
     * --------------------------------------------------------------------- *
     *
     * Constructors
     *  - SealableSet(Supplier<Boolean> sealedSupplier)
     *  - SealableSet(Collection<T> col, Supplier<Boolean> sealedSupplier)
     *  - SealableSet(Set<T> set, Supplier<Boolean> sealedSupplier)
     *
     * Immutable (read‑only) APIs
     *  - boolean equals(Object o)
     *  - int hashCode()
     *  - String toString()
     *  - int size()
     *  - boolean isEmpty()
     *  - boolean contains(Object o)
     *  - Object[] toArray()
     *  - <T1> T1[] toArray(T1[] a)
     *  - boolean containsAll(Collection<?> col)
     *
     * Mutable APIs (throw UnsupportedOperationException when sealed)
     *  - boolean add(T t)
     *  - boolean remove(Object o)
     *  - boolean addAll(Collection<? extends T> col)
     *  - boolean removeAll(Collection<?> col)
     *  - boolean retainAll(Collection<?> col)
     *  - void clear()
     *  - Iterator<T> iterator()
     *
     * Nested class (used via iterator when the underlying set contains Map.Entry)
     *  - K getKey()
     *  - V getValue()
     *  - V setValue(V value)               // throws if sealed
     *  - boolean equals(Object o)
     *  - int hashCode()
     */

    /* --------------------------------------------------------------------- *
     * Helper fields used by many tests                                       *
     * --------------------------------------------------------------------- */
    private Supplier<Boolean> mutableSupplier;   // always returns false
    private Supplier<Boolean> sealedSupplier;    // always returns true
    private AtomicBoolean toggleSeal;            // can be switched during a test

    @Before
    public void setUp() {
        mutableSupplier = () -> false;
        sealedSupplier  = () -> true;
        toggleSeal = new AtomicBoolean(false);
    }

    private Supplier<Boolean> toggleableSupplier() {
        return () -> toggleSeal.get();
    }

    /* --------------------------------------------------------------------- *
     * 2. BASIC FUNCTIONALITY TESTS                                           *
     * --------------------------------------------------------------------- */

    @Test
    public void testConstructorEmptyMutable() {
        SealableSet<String> ss = new SealableSet<>(mutableSupplier);
        assertTrue(ss.isEmpty());
        assertEquals(0, ss.size());
    }

    @Test
    public void testConstructorFromCollection() {
        List<String> list = Arrays.asList("a", "b", "c");
        SealableSet<String> ss = new SealableSet<>(list, mutableSupplier);
        assertEquals(3, ss.size());
        assertTrue(ss.containsAll(list));
    }

    @Test
    public void testConstructorWrappingExistingSet() {
        Set<Integer> backing = new HashSet<>(Arrays.asList(1, 2, 3));
        SealableSet<Integer> ss = new SealableSet<>(backing, mutableSupplier);
        assertEquals(3, ss.size());
        assertTrue(ss.contains(2));
        // modify through original set – should be reflected
        backing.remove(2);
        assertFalse(ss.contains(2));
    }

    @Test
    public void testImmutableApis() {
        SealableSet<String> ss = new SealableSet<>(Arrays.asList("x", "y"), mutableSupplier);
        assertEquals(2, ss.size());
        assertFalse(ss.isEmpty());
        assertTrue(ss.contains("x"));
        assertFalse(ss.contains("z"));
        assertArrayEquals(new Object[]{"x", "y"}, ss.toArray());

        String[] arr = new String[2];
        assertArrayEquals(new String[]{"x", "y"}, ss.toArray(arr));

        assertEquals(ss, new HashSet<>(Arrays.asList("x", "y")));
        assertEquals(ss.hashCode(), new HashSet<>(Arrays.asList("x", "y")).hashCode());
        assertEquals("[x, y]", ss.toString());
    }

    @Test
    public void testAddRemoveMutable() {
        SealableSet<String> ss = new SealableSet<>(mutableSupplier);
        assertTrue(ss.add("one"));
        assertTrue(ss.add("two"));
        assertEquals(2, ss.size());

        assertTrue(ss.remove("one"));
        assertFalse(ss.contains("one"));
        assertEquals(1, ss.size());
    }

    @Test
    public void testAddAllRemoveAllRetainAllMutable() {
        SealableSet<Integer> ss = new SealableSet<>(mutableSupplier);
        assertTrue(ss.addAll(Arrays.asList(1, 2, 3, 4)));
        assertEquals(4, ss.size());

        assertTrue(ss.removeAll(Arrays.asList(2, 4)));
        assertFalse(ss.contains(2));
        assertEquals(2, ss.size());

        assertTrue(ss.retainAll(Collections.singleton(3)));
        assertTrue(ss.contains(3));
        assertEquals(1, ss.size());
    }

    @Test
    public void testClearMutable() {
        SealableSet<String> ss = new SealableSet<>(Arrays.asList("a", "b"), mutableSupplier);
        assertFalse(ss.isEmpty());
        ss.clear();
        assertTrue(ss.isEmpty());
    }

    @Test
    public void testIteratorBasic() {
        SealableSet<String> ss = new SealableSet<>(Arrays.asList("p", "q"), mutableSupplier);
        Iterator<String> it = ss.iterator();
        Set<String> seen = new HashSet<>();
        while (it.hasNext()) {
            seen.add(it.next());
        }
        assertEquals(new HashSet<>(Arrays.asList("p", "q")), seen);
    }

    @Test
    public void testIteratorRemoveWhenMutable() {
        SealableSet<String> ss = new SealableSet<>(Arrays.asList("a", "b", "c"), mutableSupplier);
        Iterator<String> it = ss.iterator();
        while (it.hasNext()) {
            String v = it.next();
            if ("b".equals(v)) {
                it.remove();
            }
        }
        assertFalse(ss.contains("b"));
        assertEquals(2, ss.size());
    }

    /* --------------------------------------------------------------------- *
     * 3. EDGE CASES & EXCEPTION SCENARIOS                                   *
     * --------------------------------------------------------------------- *
     *
     * • Adding/removing null – the underlying ConcurrentSet permits null?  The
     *   reference implementation does not forbid it, so we test both null
     *   insertion and removal.
     *
     * • Empty collections passed to addAll / removeAll / retainAll – should
     *   return false (no change) and not throw.
     *
     * • Sealed state – every mutating method must throw
     *   UnsupportedOperationException.  This includes iterator.remove().
     *
     * • Changing the sealed flag during iteration – the iterator should
     *   respect the *current* state when remove() is invoked.
     *
     * • The iterator may return a {@link SealAwareEntry} when the underlying
     *   set holds Map.Entry objects.  The entry's setValue() must also throw
     *   when the set is sealed.
     *
     * • equals() with a non‑Set object – must return false, not throw.
     *
     * • toArray(T[]) where the supplied array is too small – should allocate a
     *   new array of the proper size (standard Set contract).
     *
     * • retainAll with a collection that contains elements not present – the
     *   method must simply ignore them.
     *
     * • iterator.next() after the set has been cleared – should throw
     *   NoSuchElementException as per the underlying iterator.
     */

    /* --------------------------------------------------------------------- *
     * 4. COMPREHENSIVE EDGE‑CASE & EXCEPTION TESTS                           *
     * --------------------------------------------------------------------- */

    @Test(expected = UnsupportedOperationException.class)
    public void testAddWhenSealed() {
        SealableSet<String> ss = new SealableSet<>(sealedSupplier);
        ss.add("foo");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveWhenSealed() {
        SealableSet<String> ss = new SealableSet<>(Arrays.asList("a"), sealedSupplier);
        ss.remove("a");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddAllWhenSealed() {
        SealableSet<Integer> ss = new SealableSet<>(sealedSupplier);
        ss.addAll(Arrays.asList(1, 2));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveAllWhenSealed() {
        SealableSet<Integer> ss = new SealableSet<>(Arrays.asList(1, 2, 3), sealedSupplier);
        ss.removeAll(Arrays.asList(2));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRetainAllWhenSealed() {
        SealableSet<Integer> ss = new SealableSet<>(Arrays.asList(1, 2, 3), sealedSupplier);
        ss.retainAll(Collections.singleton(2));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testClearWhenSealed() {
        SealableSet<String> ss = new SealableSet<>(Arrays.asList("x"), sealedSupplier);
        ss.clear();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testIteratorRemoveWhenSealed() {
        SealableSet<String> ss = new SealableSet<>(Arrays.asList("a", "b"), sealedSupplier);
        Iterator<String> it = ss.iterator();
        it.next();
        it.remove(); // must throw
    }

    @Test
    public void testToggleSealDuringIteration() {
        // start unsealed, become sealed before calling remove()
        toggleSeal.set(false);
        SealableSet<String> ss = new SealableSet<>(Arrays.asList("x", "y", "z"), toggleableSupplier());
        Iterator<String> it = ss.iterator();
        assertTrue(it.hasNext());
        assertEquals("x", it.next());

        // now seal the set
        toggleSeal.set(true);
        try {
            it.remove();
            fail("Expected UnsupportedOperationException after sealing");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        // verify the element is still present because remove failed
        assertTrue(ss.contains("x"));
    }

    @Test
    public void testAddNullWhenMutable() {
        SealableSet<String> ss = new SealableSet<>(mutableSupplier);
        assertTrue(ss.add(null));
        assertTrue(ss.contains(null));
        assertTrue(ss.remove(null));
        assertFalse(ss.contains(null));
    }

    @Test
    public void testAddNullWhenSealed() {
        SealableSet<String> ss = new SealableSet<>(sealedSupplier);
        try {
            ss.add(null);
            fail("Expected UnsupportedOperationException for null add when sealed");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void testEmptyCollectionsInBulkOperations() {
        SealableSet<Integer> ss = new SealableSet<>(mutableSupplier);
        assertFalse(ss.addAll(Collections.emptyList()));
        assertFalse(ss.removeAll(Collections.emptySet()));
        assertFalse(ss.retainAll(Collections.emptyList()));
        assertEquals(0, ss.size());
    }

    @Test
    public void testEqualsWithNonSetObject() {
        SealableSet<String> ss = new SealableSet<>(Arrays.asList("a", "b"), mutableSupplier);
        assertFalse(ss.equals("not a set"));
        assertFalse(ss.equals(null));
    }

    @Test
    public void testToArrayWithSmallerArray() {
        SealableSet<String> ss = new SealableSet<>(Arrays.asList("one", "two"), mutableSupplier);
        String[] small = new String[1];
        String[] result = ss.toArray(small);
        // result must be a new array of exact size
        assertEquals(2, result.length);
        assertArrayEquals(new String[]{"one", "two"}, result);
        // original array must remain unchanged (per Set contract)
        assertEquals(1, small.length);
    }

    @Test
    public void testSealAwareEntrySetValueThrowsWhenSealed() {
        // Build a set that holds Map.Entry objects
        Map<String, Integer> map = new HashMap<>();
        map.put("k1", 10);
        map.put("k2", 20);
        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();

        // Wrap the entry set with a sealable set that can be toggled
        toggleSeal.set(false);
        SealableSet<Map.Entry<String, Integer>> ss = new SealableSet<>(entrySet, toggleableSupplier());

        Iterator<Map.Entry<String, Integer>> it = ss.iterator();
        Map.Entry<String, Integer> entry = it.next(); // returns a SealAwareEntry

        // Unsealed – setValue should work
        entry.setValue(99);
        assertEquals(Integer.valueOf(99), map.get(entry.getKey()));

        // Seal now and attempt modification
        toggleSeal.set(true);
        try {
            entry.setValue(100);
            fail("Expected UnsupportedOperationException from sealed entry");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void testIteratorNextAfterClear() {
        SealableSet<String> ss = new SealableSet<>(Arrays.asList("a", "b"), mutableSupplier);
        Iterator<String> it = ss.iterator();
        assertTrue(it.hasNext());
        assertEquals("a", it.next());
        ss.clear(); // underlying set now empty
        // The iterator is backed by the original set; calling next() now should
        // throw NoSuchElementException according to the iterator contract.
        try {
            it.next();
            fail("Expected NoSuchElementException after clearing underlying set");
        } catch (NoSuchElementException ignored) {
        }
    }
}
