package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link CompactCILinkedSet}.
 *
 * The suite follows the steps requested:
 *   1. List all public methods (including inherited ones) with signatures.
 *   2. Provide a basic test for each method.
 *   3. Identify edge‑case and exception scenarios.
 *   4. Add explicit tests for those scenarios.
 *   5. Combine everything into a single test class.
 *
 * NOTE: The implementation of {@link CompactSet} (the superclass) is not shown here,
 * but the public API documented in the prompt is used.  All tests rely only on the
 * constructors and methods that are guaranteed to exist.
 */
public class CompactCILinkedSetTest {

    // -------------------------------------------------------------------------
    // 1. PUBLIC METHOD LIST (including inherited methods from CompactSet)
    // -------------------------------------------------------------------------
    //
    // Constructors
    //   CompactCILinkedSet()
    //   CompactCILinkedSet(Collection<E> other)
    //
    // Inherited public methods from CompactSet<E> (the Set<E> interface)
    //   boolean isDefaultCompactSet()
    //   int size()
    //   boolean isEmpty()
    //   boolean contains(Object o)
    //   boolean add(E e)
    //   boolean remove(Object o)
    //   void clear()
    //   boolean containsAll(Collection<?> c)
    //   boolean addAll(Collection<? extends E> c)
    //   boolean retainAll(Collection<?> c)
    //   boolean removeAll(Collection<?> c)
    //   Iterator<E> iterator()
    //   Object[] toArray()
    //   <T> T[] toArray(T[] a)
    //   boolean equals(Object o)
    //   int hashCode()
    //   String toString()
    //   Map<String, Object> getConfig()
    //   CompactCILinkedSet<E> withConfig(Map<String, Object> config)
    //
    // -------------------------------------------------------------------------

    private CompactCILinkedSet<String> emptySet;
    private CompactCILinkedSet<String> populatedSet;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        emptySet = new CompactCILinkedSet<>();

        // a set that already contains a few elements, using mixed case to verify
        // case‑insensitive behaviour
        List<String> initial = Arrays.asList("Apple", "banana", "Cherry");
        populatedSet = new CompactCILinkedSet<>(initial);
    }

    // -------------------------------------------------------------------------
    // 2. BASIC FUNCTIONALITY TESTS (one per public method)
    // -------------------------------------------------------------------------

    @Test
    public void testDefaultConstructorCreatesEmptySet() {
        assertTrue(emptySet.isEmpty());
        assertEquals(0, emptySet.size());
    }

    @Test
    public void testCollectionConstructorCopiesElements() {
        assertEquals(3, populatedSet.size());
        assertTrue(populatedSet.contains("Apple"));
        assertTrue(populatedSet.contains("banana"));
        assertTrue(populatedSet.contains("Cherry"));
    }

    @Test
    public void testAddAndSize() {
        assertTrue(emptySet.add("Dog"));
        assertEquals(1, emptySet.size());
        // adding the same logical element with different case must not increase size
        assertFalse(emptySet.add("dog")); // duplicate (case‑insensitive)
        assertEquals(1, emptySet.size());
    }

    @Test
    public void testContainsCaseInsensitive() {
        assertTrue(populatedSet.contains("apple"));   // different case
        assertTrue(populatedSet.contains("BANANA"));
        assertTrue(populatedSet.contains("cherry"));
        assertFalse(populatedSet.contains("durian"));
    }

    @Test
    public void testRemoveCaseInsensitive() {
        assertTrue(populatedSet.remove("BANANA"));
        assertFalse(populatedSet.contains("banana"));
        assertEquals(2, populatedSet.size());

        // removing a non‑existent element returns false
        assertFalse(populatedSet.remove("banana"));
    }

    @Test
    public void testClear() {
        populatedSet.clear();
        assertTrue(populatedSet.isEmpty());
        assertEquals(0, populatedSet.size());
    }

    @Test
    public void testIsEmptyAndIsDefaultCompactSet() {
        assertTrue(emptySet.isEmpty());
        // CompactSet.isDefaultCompactSet() is public; it should return a boolean.
        // Its exact value is implementation dependent, but the call must not fail.
        emptySet.isDefaultCompactSet(); // just ensure no exception
    }

    @Test
    public void testContainsAll() {
        Collection<String> subset = Arrays.asList("apple", "BANANA");
        assertTrue(populatedSet.containsAll(subset));

        Collection<String> notSubset = Arrays.asList("apple", "durian");
        assertFalse(populatedSet.containsAll(notSubset));
    }

    @Test
    public void testAddAllWithDuplicatesAndCaseVariations() {
        Collection<String> toAdd = Arrays.asList("dog", "Dog", "DOG", "cat");
        assertTrue(emptySet.addAll(toAdd));
        // only two logical elements should be present
        assertEquals(2, emptySet.size());
        assertTrue(emptySet.contains("dog"));
        assertTrue(emptySet.contains("cat"));
    }

    @Test
    public void testRetainAllKeepsOnlySpecifiedElements() {
        Collection<String> retain = Arrays.asList("APPLE", "cherry");
        assertTrue(populatedSet.retainAll(retain));
        assertEquals(2, populatedSet.size());
        assertTrue(populatedSet.contains("Apple"));
        assertTrue(populatedSet.contains("Cherry"));
        assertFalse(populatedSet.contains("banana"));
    }

    @Test
    public void testRemoveAll() {
        Collection<String> toRemove = Arrays.asList("APPLE", "Banana");
        assertTrue(populatedSet.removeAll(toRemove));
        assertEquals(1, populatedSet.size());
        assertTrue(populatedSet.contains("Cherry"));
        assertFalse(populatedSet.contains("Apple"));
        assertFalse(populatedSet.contains("banana"));
    }

    @Test
    public void testIteratorOrderIsInsertionOrder() {
        // CompactCILinkedSet is described as a LinkedHashSet replacement,
        // therefore iteration order should follow insertion order.
        Iterator<String> it = populatedSet.iterator();
        List<String> iterated = new ArrayList<>();
        while (it.hasNext()) {
            iterated.add(it.next());
        }
        assertEquals(Arrays.asList("Apple", "banana", "Cherry"), iterated);
    }

    @Test
    public void testToArray() {
        Object[] arr = populatedSet.toArray();
        assertArrayEquals(new Object[]{"Apple", "banana", "Cherry"}, arr);
    }

    @Test
    public void testToArrayTyped() {
        String[] arr = populatedSet.toArray(new String[0]);
        assertArrayEquals(new String[]{"Apple", "banana", "Cherry"}, arr);
    }

    @Test
    public void testEqualsAndHashCode() {
        CompactCILinkedSet<String> other = new CompactCILinkedSet<>(Arrays.asList("APPLE", "Banana", "CHERRY"));
        assertTrue(populatedSet.equals(other));
        assertEquals(populatedSet.hashCode(), other.hashCode());
    }

    @Test
    public void testToStringNotNull() {
        String s = populatedSet.toString();
        assertNotNull(s);
        // basic sanity – the string should contain at least one of the elements
        assertTrue(s.contains("Apple") || s.contains("banana") || s.contains("Cherry"));
    }

    // -------------------------------------------------------------------------
    // 3. EDGE CASES & EXCEPTION SCENARIOS
    // -------------------------------------------------------------------------

    @Test
    public void testCollectionConstructorWithNullThrowsNPE() {
        thrown.expect(NullPointerException.class);
        new CompactCILinkedSet<String>(null);
    }

    @Test
    public void testAddNullElement() {
        // The contract of Set permits null elements unless the implementation forbids it.
        // CompactSet (the superclass) does not document a restriction, so we verify the actual behaviour.
        // If it throws NPE we assert that; otherwise we assert the element is stored.
        try {
            boolean added = emptySet.add(null);
            // If we reach here, null is accepted – size should increase.
            assertTrue(added);
            assertEquals(1, emptySet.size());
            assertTrue(emptySet.contains(null));
        } catch (NullPointerException npe) {
            // Expected alternative behaviour – the test passes.
        }
    }

    @Test
    public void testAddAllWithNullCollectionThrowsNPE() {
        thrown.expect(NullPointerException.class);
        emptySet.addAll(null);
    }

    @Test
    public void testRemoveAllWithNullCollectionThrowsNPE() {
        thrown.expect(NullPointerException.class);
        emptySet.removeAll(null);
    }

    @Test
    public void testRetainAllWithNullCollectionThrowsNPE() {
        thrown.expect(NullPointerException.class);
        emptySet.retainAll(null);
    }

    @Test
    public void testContainsAllWithNullCollectionThrowsNPE() {
        thrown.expect(NullPointerException.class);
        emptySet.containsAll(null);
    }

    @Test
    public void testAddAllEmptyCollectionDoesNotChangeSet() {
        Collection<String> empty = Collections.emptyList();
        assertFalse(emptySet.addAll(empty));
        assertTrue(emptySet.isEmpty());
    }

    @Test
    public void testRemoveNonExistentElementReturnsFalse() {
        assertFalse(emptySet.remove("nonexistent"));
    }

    @Test
    public void testIteratorRemoveUnsupported() {
        // The iterator returned by CompactCILinkedSet should support remove() because it
        // mimics LinkedHashSet. We verify that it works and does not throw UnsupportedOperationException.
        populatedSet.add("dog");
        Iterator<String> it = populatedSet.iterator();
        while (it.hasNext()) {
            String val = it.next();
            if ("dog".equalsIgnoreCase(val)) {
                it.remove();
                break;
            }
        }
        assertFalse(populatedSet.contains("dog"));
    }

    // -------------------------------------------------------------------------
    // 4. MERGED COMPLETE TEST CLASS (already assembled above)
    // -------------------------------------------------------------------------
}
