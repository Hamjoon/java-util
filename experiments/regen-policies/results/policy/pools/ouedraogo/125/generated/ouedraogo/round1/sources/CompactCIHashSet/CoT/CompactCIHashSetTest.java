package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * JUnit 4 test cases for {@link CompactCIHashSet}.
 * The tests cover:
 * <ul>
 *   <li>Public constructors</li>
 *   <li>Inherited Set operations with case‑insensitive behavior</li>
 *   <li>Edge cases such as null arguments and duplicate values differing only by case</li>
 *   <li>Exception handling (NullPointerException from the collection constructor)</li>
 * </ul>
 */
public class CompactCIHashSetTest {

    private CompactCIHashSet<String> set;

    @Before
    public void setUp() {
        set = new CompactCIHashSet<>();
    }

    /*** PUBLIC CONSTRUCTORS ***/

    @Test
    public void testDefaultConstructorCreatesEmptySet() {
        assertEquals("New set should be empty", 0, set.size());
        assertTrue("New set should report isEmpty()", set.isEmpty());
    }

    @Test
    public void testConstructorWithCollectionCopiesElementsCaseInsensitively() {
        Collection<String> source = new ArrayList<>();
        source.add("Alpha");
        source.add("beta");
        source.add("GAMMA");

        CompactCIHashSet<String> cs = new CompactCIHashSet<>(source);

        assertEquals(3, cs.size());
        assertTrue(cs.contains("ALPHA"));
        assertTrue(cs.contains("BETA"));
        assertTrue(cs.contains("gamma"));
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullCollectionThrowsException() {
        new CompactCIHashSet<String>(null);
    }

    /*** BASIC FUNCTIONALITY ***/

    @Test
    public void testAddAndContainsCaseInsensitive() {
        assertTrue(set.add("Apple"));
        assertTrue(set.contains("APPLE"));
        assertEquals(1, set.size());
    }

    @Test
    public void testAddDuplicateDifferentCaseDoesNotIncreaseSize() {
        assertTrue(set.add("Apple"));
        assertFalse("Adding a duplicate value with different case should return false",
                    set.add("apple"));
        assertEquals(1, set.size());
    }

    @Test
    public void testRemoveCaseInsensitive() {
        set.add("Banana");
        assertTrue(set.remove("BANANA"));
        assertFalse(set.contains("banana"));
        assertEquals(0, set.size());
    }

    @Test
    public void testAddAllFromCollection() {
        List<String> list = new ArrayList<>();
        list.add("One");
        list.add("TWO");
        list.add("three");

        assertTrue(set.addAll(list));
        assertEquals(3, set.size());
        assertTrue(set.contains("one"));
        assertTrue(set.contains("two"));
        assertTrue(set.contains("THREE"));
    }

    /*** SET OPERATIONS WITH CASE‑INSENSITIVE SEMANTICS ***/

    @Test
    public void testContainsAllCaseInsensitive() {
        set.addAll(List.of("x", "Y", "z"));
        Collection<String> query = List.of("X", "y");
        assertTrue(set.containsAll(query));
    }

    @Test
    public void testRetainAllCaseInsensitive() {
        set.addAll(List.of("a", "B", "c"));
        Collection<String> retain = List.of("A", "b");
        assertTrue(set.retainAll(retain));
        assertEquals(2, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertFalse(set.contains("c"));
    }

    @Test
    public void testRemoveAllCaseInsensitive() {
        set.addAll(List.of("dog", "cat", "mouse"));
        Collection<String> toRemove = List.of("DOG", "MOUSE");
        assertTrue(set.removeAll(toRemove));
        assertEquals(1, set.size());
        assertTrue(set.contains("cat"));
        assertFalse(set.contains("dog"));
        assertFalse(set.contains("mouse"));
    }

    @Test
    public void testClearEmptiesSet() {
        set.addAll(List.of("one", "two"));
        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    public void testIteratorTraversesAllElementsCaseInsensitive() {
        set.addAll(List.of("Alpha", "beta"));
        Iterator<String> it = set.iterator();

        List<String> iterated = new ArrayList<>();
        while (it.hasNext()) {
            iterated.add(it.next());
        }

        assertEquals(2, iterated.size());
        assertTrue(iterated.contains("Alpha"));
        assertTrue(iterated.contains("beta"));
    }

    @Test
    public void testToArrayReturnsAllElements() {
        set.addAll(List.of("One", "Two"));
        Object[] arr = set.toArray();
        assertEquals(2, arr.length);
        // Order is not guaranteed; verify presence ignoring case
        List<String> asList = List.of(((String) arr[0]).toLowerCase(),
                                      ((String) arr[1]).toLowerCase());
        assertTrue(asList.contains("one"));
        assertTrue(asList.contains("two"));
    }

    @Test
    public void testToTypedArray() {
        set.addAll(List.of("Red", "Blue"));
        String[] arr = set.toArray(new String[0]);
        assertEquals(2, arr.length);
        List<String> lowered = List.of(arr[0].toLowerCase(), arr[1].toLowerCase());
        assertTrue(lowered.contains("red"));
        assertTrue(lowered.contains("blue"));
    }

    /*** EDGE CASES ***/

    @Test
    public void testAddNullElement() {
        // CompactSet permits null values (the underlying CompactMap does), so we verify that behaviour.
        assertTrue("Adding null should succeed", set.add(null));
        assertTrue("Set should contain null after addition", set.contains(null));
        assertEquals(1, set.size());

        // Adding null again must not increase size
        assertFalse("Adding null a second time should return false", set.add(null));
        assertEquals(1, set.size());
    }

    @Test
    public void testContainsNullWhenAbsent() {
        assertFalse(set.contains(null));
    }

    @Test
    public void testRemoveNullElement() {
        set.add(null);
        assertTrue(set.remove(null));
        assertFalse(set.contains(null));
        assertEquals(0, set.size());
    }

    /*** EQUALITY AND HASHCODE ***/

    @Test
    public void testEqualsAndHashCodeRespectCaseInsensitivity() {
        CompactCIHashSet<String> other = new CompactCIHashSet<>();
        set.add("Hello");
        other.add("hello");

        assertEquals("Sets with same elements differing only by case should be equal", set, other);
        assertEquals("Hash codes should be equal for equal sets", set.hashCode(), other.hashCode());
    }

    @Test
    public void testToStringContainsElements() {
        set.addAll(List.of("x", "Y"));
        String s = set.toString();
        // The exact format is implementation dependent; we only assert that the string contains the elements.
        assertTrue(s.contains("x"));
        assertTrue(s.contains("Y"));
    }
}
