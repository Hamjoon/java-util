package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link SealableSet}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public methods (basic happy‑path behavior)</li>
 *   <li>Edge‑cases such as null elements, empty collections, and duplicate additions</li>
 *   <li>Correct enforcement of the sealed/immutable contract</li>
 *   <li>Behaviour of the {@code SealAwareEntry} wrapper returned from the iterator</li>
 *   <li>Delegation of {@code equals}, {@code hashCode} and {@code toString}</li>
 * </ul>
 */
public class SealableSetTest {

    /** Supplier that can be toggled to emulate sealed / mutable states. */
    private AtomicBoolean sealedFlag;
    private Supplier<Boolean> sealedSupplier;

    @Before
    public void setUp() {
        sealedFlag = new AtomicBoolean(false);
        sealedSupplier = sealedFlag::get;
    }

    /* --------------------------------------------------------------------- */
    /* 1.  Extraction of public method signatures (for reference)           */
    /* --------------------------------------------------------------------- */
    // Constructors
    //   SealableSet(Supplier<Boolean> sealedSupplier)
    //   SealableSet(Collection<T> col, Supplier<Boolean> sealedSupplier)
    //   SealableSet(Set<T> set, Supplier<Boolean> sealedSupplier)

    // Immutable APIs
    //   boolean equals(Object o)
    //   int hashCode()
    //   String toString()
    //   int size()
    //   boolean isEmpty()
    //   boolean contains(Object o)
    //   Object[] toArray()
    //   <T1> T1[] toArray(T1[] a)
    //   boolean containsAll(Collection<?> col)

    // Mutable APIs
    //   boolean add(T t)
    //   boolean remove(Object o)
    //   boolean addAll(Collection<? extends T> col)
    //   boolean removeAll(Collection<?> col)
    //   boolean retainAll(Collection<?> col)
    //   void clear()
    //   Iterator<T> iterator()

    // Nested class (public static)
    //   SealAwareEntry<K,V> implements Map.Entry<K,V>
    //       K getKey()
    //       V getValue()
    //       V setValue(V value)
    //       boolean equals(Object o)
    //       int hashCode()

    /* --------------------------------------------------------------------- */
    /* 2.  Basic functionality tests (happy path)                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testEmptyConstructorAndBasicImmutableOps() {
        SealableSet<String> set = new SealableSet<>(sealedSupplier);
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
        assertFalse(set.contains("anything"));
        assertEquals("[]", set.toString());

        // toArray on empty set
        Object[] arr = set.toArray();
        assertEquals(0, arr.length);
    }

    @Test
    public void testAddAndContainsWhenUnsealed() {
        SealableSet<Integer> set = new SealableSet<>(sealedSupplier);
        assertTrue(set.add(10));
        assertTrue(set.contains(10));
        assertEquals(1, set.size());
    }

    @Test
    public void testAddAllAndRemoveAllWhenUnsealed() {
        SealableSet<String> set = new SealableSet<>(sealedSupplier);
        List<String> toAdd = Arrays.asList("a", "b", "c");
        assertTrue(set.addAll(toAdd));
        assertEquals(3, set.size());

        List<String> toRemove = Arrays.asList("b", "c");
        assertTrue(set.removeAll(toRemove));
        assertEquals(1, set.size());
        assertTrue(set.contains("a"));
    }

    @Test
    public void testRetainAllWhenUnsealed() {
        SealableSet<String> set = new SealableSet<>(sealedSupplier);
        set.addAll(Arrays.asList("x", "y", "z"));
        assertTrue(set.retainAll(Arrays.asList("x", "y")));
        assertEquals(2, set.size());
        assertTrue(set.contains("x"));
        assertTrue(set.contains("y"));
        assertFalse(set.contains("z"));
    }

    @Test
    public void testClearWhenUnsealed() {
        SealableSet<String> set = new SealableSet<>(sealedSupplier);
        set.addAll(Arrays.asList("one", "two"));
        assertFalse(set.isEmpty());
        set.clear();
        assertTrue(set.isEmpty());
    }

    @Test
    public void testIteratorAndRemoveWhenUnsealed() {
        SealableSet<String> set = new SealableSet<>(sealedSupplier);
        set.addAll(Arrays.asList("alpha", "beta"));
        Iterator<String> it = set.iterator();

        List<String> visited = new ArrayList<>();
        while (it.hasNext()) {
            String val = it.next();
            visited.add(val);
            if ("alpha".equals(val)) {
                it.remove(); // should succeed while unsealed
            }
        }
        assertEquals(Arrays.asList("alpha", "beta"), visited);
        assertFalse(set.contains("alpha"));
        assertTrue(set.contains("beta"));
    }

    @Test
    public void testEqualsHashCodeAndToStringDelegate() {
        Set<String> backing = new HashSet<>(Arrays.asList("p", "q"));
        SealableSet<String> set = new SealableSet<>(backing, sealedSupplier);

        // Equality with the original set
        assertEquals(backing, set);
        assertEquals(set, backing);

        // Equality with another SealableSet wrapping an equal collection
        SealableSet<String> other = new SealableSet<>(Arrays.asList("p", "q"), sealedSupplier);
        assertEquals(set, other);
        assertEquals(set.hashCode(), other.hashCode());

        // toString should match backing set's toString (order not guaranteed)
        String s = set.toString();
        assertTrue(s.contains("p"));
        assertTrue(s.contains("q"));
    }

    /* --------------------------------------------------------------------- */
    /* 3.  Edge‑case and exception handling tests                             */
    /* --------------------------------------------------------------------- */

    @Test(expected = UnsupportedOperationException.class)
    public void testAddWhenSealedThrows() {
        sealedFlag.set(true);
        SealableSet<Integer> set = new SealableSet<>(sealedSupplier);
        set.add(1); // should throw
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveWhenSealedThrows() {
        SealableSet<Integer> set = new SealableSet<>(sealedSupplier);
        set.add(5);
        sealedFlag.set(true);
        set.remove(5); // should throw
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddAllWhenSealedThrows() {
        sealedFlag.set(true);
        SealableSet<String> set = new SealableSet<>(sealedSupplier);
        set.addAll(Arrays.asList("a", "b"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveAllWhenSealedThrows() {
        SealableSet<String> set = new SealableSet<>(sealedSupplier);
        set.addAll(Arrays.asList("a", "b"));
        sealedFlag.set(true);
        set.removeAll(Arrays.asList("a"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRetainAllWhenSealedThrows() {
        SealableSet<String> set = new SealableSet<>(sealedSupplier);
        set.addAll(Arrays.asList("a", "b"));
        sealedFlag.set(true);
        set.retainAll(Arrays.asList("a"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testClearWhenSealedThrows() {
        SealableSet<String> set = new SealableSet<>(sealedSupplier);
        set.add("x");
        sealedFlag.set(true);
        set.clear();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testIteratorRemoveWhenSealedThrows() {
        SealableSet<String> set = new SealableSet<>(sealedSupplier);
        set.addAll(Arrays.asList("first", "second"));
        Iterator<String> it = set.iterator();
        it.next(); // advance
        sealedFlag.set(true);
        it.remove(); // should throw
    }

    @Test
    public void testNullElementAddAndContains() {
        SealableSet<String> set = new SealableSet<>(sealedSupplier);
        assertTrue(set.add(null));
        assertTrue(set.contains(null));
        assertEquals(1, set.size());
        assertTrue(set.remove(null));
        assertFalse(set.contains(null));
    }

    @Test
    public void testAddDuplicateElements() {
        SealableSet<Integer> set = new SealableSet<>(sealedSupplier);
        assertTrue(set.add(42));
        assertFalse(set.add(42)); // Set semantics: second add returns false
        assertEquals(1, set.size());
    }

    @Test
    public void testIteratorReturnsSealAwareEntryWhenUnderlyingSetContainsMapEntries() {
        // Prepare a backing set that holds Map.Entry objects
        Set<Map.Entry<String, Integer>> backing = new HashSet<>();
        backing.add(new AbstractMap.SimpleEntry<>("k1", 1));
        backing.add(new AbstractMap.SimpleEntry<>("k2", 2));

        SealableSet<Map.Entry<String, Integer>> set = new SealableSet<>(backing, sealedSupplier);
        Iterator<Map.Entry<String, Integer>> it = set.iterator();

        // First entry should be wrapped in SealAwareEntry
        Map.Entry<String, Integer> entry = it.next();
        assertTrue(entry instanceof SealableSet.SealAwareEntry);
        assertEquals("k1", entry.getKey());
        assertEquals(Integer.valueOf(1), entry.getValue());

        // Modify value while unsealed – should succeed
        ((SealableSet.SealAwareEntry<String, Integer>) entry).setValue(10);
        assertEquals(Integer.valueOf(10), entry.getValue());

        // Now seal and verify that setValue throws
        sealedFlag.set(true);
        try {
            ((SealableSet.SealAwareEntry<String, Integer>) entry).setValue(20);
            fail("Expected UnsupportedOperationException when sealed");
        } catch (UnsupportedOperationException ex) {
            // expected
        }
    }

    @Test
    public void testSealAwareEntryEqualsAndHashCodeDelegate() {
        Map.Entry<String, Integer> raw = new AbstractMap.SimpleEntry<>("key", 100);
        SealableSet.SealAwareEntry<String, Integer> wrapper =
                new SealableSet.SealAwareEntry<>(raw, sealedSupplier);

        // Equality with original entry
        assertEquals(raw, wrapper);
        assertEquals(wrapper, raw);

        // Equality with another wrapper around an equal entry
        Map.Entry<String, Integer> raw2 = new AbstractMap.SimpleEntry<>("key", 100);
        SealableSet.SealAwareEntry<String, Integer> wrapper2 =
                new SealableSet.SealAwareEntry<>(raw2, sealedSupplier);
        assertEquals(wrapper, wrapper2);
        assertEquals(wrapper.hashCode(), wrapper2.hashCode());
    }

    /* --------------------------------------------------------------------- */
    /* 4.  Constructor variants and collection‑copy semantics               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testConstructorWithCollectionCopiesElements() {
        List<String> src = Arrays.asList("a", "b", "c");
        SealableSet<String> set = new SealableSet<>(src, sealedSupplier);
        assertEquals(3, set.size());
        assertTrue(set.containsAll(src));

        // Modifying the original collection should NOT affect the SealableSet
        src = new ArrayList<>(src);
        src.remove("a");
        assertEquals(3, set.size()); // still three elements
    }

    @Test
    public void testConstructorWithExistingSetWrapsDirectly() {
        Set<Integer> backing = new HashSet<>(Arrays.asList(1, 2, 3));
        SealableSet<Integer> set = new SealableSet<>(backing, sealedSupplier);

        // Mutations affect the original backing set when unsealed
        set.add(4);
        assertTrue(backing.contains(4));

        sealedFlag.set(true);
        try {
            set.add(5);
            fail("Expected exception when sealed");
        } catch (UnsupportedOperationException ignored) {
        }
        assertFalse(backing.contains(5));
    }
}
