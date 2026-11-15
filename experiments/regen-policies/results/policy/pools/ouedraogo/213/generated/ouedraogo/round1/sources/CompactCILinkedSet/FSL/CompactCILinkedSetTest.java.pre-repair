package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

/**
 * Unit tests for {@link CompactCILinkedSet}.
 *
 * The tests focus on the behaviour that is specific to the
 * case‑insensitive linked implementation:
 *
 * <ul>
 *   <li>Construction (empty and from a collection)</li>
 *   <li>Case‑insensitive {@code add}, {@code contains} and {@code remove}</li>
 *   <li>Preservation of insertion order (linked semantics)</li>
 *   <li>Standard {@code Set} contract methods such as {@code clear},
 *       {@code isEmpty}, {@code size}, {@code equals} and {@code hashCode}</li>
 *   <li>Exception handling for a {@code null} collection passed to the
 *       collection‑based constructor</li>
 * </ul>
 *
 * Only the public API of {@link CompactCILinkedSet} and the
 * {@link CompactSet} methods listed in the supplied signature are used.
 */
public class CompactCILinkedSetTest {

    /** Verify that the no‑arg constructor creates an empty set. */
    @Test
    public void testEmptyConstructor() {
        CompactCILinkedSet<String> set = new CompactCILinkedSet<>();
        assertTrue("Set should be empty after construction", set.isEmpty());
        assertEquals("Size should be 0 after construction", 0, set.size());
    }

    /** Verify that the collection‑based constructor copies elements correctly
     *  and respects case‑insensitivity. */
    @Test
    public void testCollectionConstructorCopiesElements() {
        List<String> source = Arrays.asList("Alpha", "beta", "ALPHA", "Gamma");
        CompactCILinkedSet<String> set = new CompactCILinkedSet<>(source);

        // "Alpha" and "ALPHA" are the same element case‑insensitively
        assertEquals("Duplicate elements (different case) should be merged", 3, set.size());
        assertTrue(set.contains("alpha"));
        assertTrue(set.contains("BETA"));
        assertTrue(set.contains("gamma"));
    }

    /** The collection‑based constructor must throw NPE when passed {@code null}. */
    @Test(expected = NullPointerException.class)
    public void testCollectionConstructorRejectsNull() {
        new CompactCILinkedSet<String>(null);
    }

    /** Adding elements is case‑insensitive; duplicate (different case) is ignored. */
    @Test
    public void testAddAndContainsCaseInsensitive() {
        CompactCILinkedSet<String> set = new CompactCILinkedSet<>();

        assertTrue("First add should succeed", set.add("Foo"));
        assertFalse("Adding the same value with different case should be ignored", set.add("fOO"));
        assertEquals("Size must be 1 after duplicate add", 1, set.size());

        assertTrue("Contains should be case‑insensitive (lower case)", set.contains("foo"));
        assertTrue("Contains should be case‑insensitive (upper case)", set.contains("FOO"));
    }

    /** Removing an element works case‑insensitively. */
    @Test
    public void testRemoveCaseInsensitive() {
        CompactCILinkedSet<String> set = new CompactCILinkedSet<>();
        set.add("TestValue");

        assertTrue("Remove with different case should succeed", set.remove("testvalue"));
        assertFalse("Set should be empty after removal", set.contains("TestValue"));
        assertEquals("Size must be 0 after removal", 0, set.size());
    }

    /** The iterator must return elements in insertion order (linked behaviour). */
    @Test
    public void testIteratorPreservesInsertionOrder() {
        CompactCILinkedSet<String> set = new CompactCILinkedSet<>();
        set.add("first");
        set.add("Second");
        set.add("THIRD");

        List<String> iterated = new ArrayList<>();
        for (Iterator<String> it = set.iterator(); it.hasNext(); ) {
            iterated.add(it.next());
        }

        List<String> expected = Arrays.asList("first", "Second", "THIRD");
        assertEquals("Iterator should preserve insertion order", expected, iterated);
    }

    /** addAll should respect case‑insensitivity and linked order. */
    @Test
    public void testAddAllFromCollection() {
        Collection<String> source = Arrays.asList("one", "TWO", "One", "three");
        CompactCILinkedSet<String> set = new CompactCILinkedSet<>();
        set.addAll(source);

        // "one" and "One" collapse to a single element
        assertEquals("Size after addAll with case‑duplicates", 3, set.size());
        assertTrue(set.contains("ONE"));
        assertTrue(set.contains("two"));
        assertTrue(set.contains("THREE"));

        // Verify order: first appearance of each distinct (case‑insensitive) element
        List<String> iterated = new ArrayList<>();
        for (String s : set) {
            iterated.add(s);
        }
        List<String> expectedOrder = Arrays.asList("one", "TWO", "three");
        assertEquals("Insertion order after addAll", expectedOrder, iterated);
    }

    /** clear() should empty the set and isEmpty() must reflect that. */
    @Test
    public void testClearEmptiesSet() {
        CompactCILinkedSet<String> set = new CompactCILinkedSet<>();
        set.add("a");
        set.add("b");
        set.clear();

        assertTrue("Set should be empty after clear()", set.isEmpty());
        assertEquals("Size must be 0 after clear()", 0, set.size());
    }

    /** equals() and hashCode() must be consistent for case‑insensitive sets. */
    @Test
    public void testEqualsAndHashCodeCaseInsensitive() {
        CompactCILinkedSet<String> set1 = new CompactCILinkedSet<>();
        CompactCILinkedSet<String> set2 = new CompactCILinkedSet<>();

        set1.add("Alpha");
        set1.add("beta");

        set2.add("ALPHA");
        set2.add("BETA");

        assertEquals("Sets with same elements (different case) must be equal", set1, set2);
        assertEquals("Hash codes must be equal for equal sets", set1.hashCode(), set2.hashCode());

        // Also verify equality against a regular HashSet containing the same
        // logical elements (case‑insensitive view)
        Set<String> hashSet = new HashSet<>(Arrays.asList("alpha", "BETA"));
        assertTrue("CompactCILinkedSet should be equal to a standard Set with same logical elements",
                set1.equals(hashSet));
    }

    /** containsAll should be case‑insensitive. */
    @Test
    public void testContainsAllCaseInsensitive() {
        CompactCILinkedSet<String> set = new CompactCILinkedSet<>();
        set.addAll(Arrays.asList("One", "Two", "Three"));

        Collection<String> query = Arrays.asList("one", "TWO");
        assertTrue("containsAll must be case‑insensitive", set.containsAll(query));

        Collection<String> queryMissing = Arrays.asList("one", "four");
        assertFalse("containsAll must return false when any element is missing", set.containsAll(queryMissing));
    }

    /** retainAll should keep only the elements present in the supplied collection,
     *  using case‑insensitive comparison. */
    @Test
    public void testRetainAllCaseInsensitive() {
        CompactCILinkedSet<String> set = new CompactCILinkedSet<>();
        set.addAll(Arrays.asList("A", "b", "C", "d"));

        Collection<String> retain = Arrays.asList("a", "D");
        boolean changed = set.retainAll(retain);

        assertTrue("retainAll should report a change when elements are removed", changed);
        assertEquals("Size after retainAll", 2, set.size());
        assertTrue(set.contains("A"));
        assertTrue(set.contains("d"));
        assertFalse(set.contains("b"));
        assertFalse(set.contains("C"));
    }

    /** removeAll should delete all matching elements irrespective of case. */
    @Test
    public void testRemoveAllCaseInsensitive() {
        CompactCILinkedSet<String> set = new CompactCILinkedSet<>();
        set.addAll(Arrays.asList("x", "Y", "z"));

        Collection<String> toRemove = Arrays.asList("X", "y");
        boolean changed = set.removeAll(toRemove);

        assertTrue("removeAll should report a change when elements are removed", changed);
        assertEquals("Size after removeAll", 1, set.size());
        assertTrue(set.contains("z"));
    }

    /** toArray() should return an array containing all elements in insertion order. */
    @Test
    public void testToArrayPreservesOrder() {
        CompactCILinkedSet<String> set = new CompactCILinkedSet<>();
        set.addAll(Arrays.asList("first", "Second", "THIRD"));

        Object[] array = set.toArray();
        assertArrayEquals("toArray must preserve insertion order",
                new Object[]{"first", "Second", "THIRD"}, array);
    }

    /** toArray(T[]) overload should correctly size the returned array. */
    @Test
    public void testToArrayTyped() {
        CompactCILinkedSet<String> set = new CompactCILinkedSet<>();
        set.addAll(Arrays.asList("a", "b"));

        String[] dest = new String[0];
        String[] result = set.toArray(dest);

        assertArrayEquals("Typed toArray must contain elements in order", new String[]{"a", "b"}, result);
        assertEquals("Returned array should be of the same runtime type as the argument",
                dest.getClass(), result.getClass());
    }
}
