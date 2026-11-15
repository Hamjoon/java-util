package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import static org.junit.Assert.*;

public class CaseInsensitiveSetTest {

    private CaseInsensitiveSet<String> stringSet;
    private CaseInsensitiveSet<Object> mixedSet;

    @Before
    public void setUp() {
        stringSet = new CaseInsensitiveSet<>();
        mixedSet = new CaseInsensitiveSet<>();
    }

    /* ----------------------------------------------------------------------
     * Basic add / contains / size behavior (case‑insensitive for Strings)
     * ---------------------------------------------------------------------- */
    @Test
    public void testAddAndContainsCaseInsensitive() {
        assertTrue(stringSet.add("Hello"));
        // duplicate with different case must not be added
        assertFalse(stringSet.add("HELLO"));
        assertEquals(1, stringSet.size());

        // contains should be case‑insensitive
        assertTrue(stringSet.contains("hello"));
        assertTrue(stringSet.contains("HeLLo"));
    }

    @Test
    public void testAddNonStringElements() {
        assertTrue(mixedSet.add(123));
        assertTrue(mixedSet.add(456));
        assertEquals(2, mixedSet.size());

        // non‑String elements are compared normally
        assertTrue(mixedSet.contains(123));
        assertFalse(mixedSet.contains(789));
    }

    @Test
    public void testRemoveCaseInsensitive() {
        stringSet.add("World");
        assertTrue(stringSet.remove("world"));
        assertTrue(stringSet.isEmpty());

        // removing a non‑existing element returns false
        assertFalse(stringSet.remove("world"));
    }

    /* ----------------------------------------------------------------------
     * Bulk operations: addAll, containsAll, retainAll, removeAll
     * ---------------------------------------------------------------------- */
    @Test
    public void testAddAllAndContainsAll() {
        List<String> list = Arrays.asList("a", "A", "b");
        assertTrue(stringSet.addAll(list));
        // "a" and "A" are considered the same element
        assertEquals(2, stringSet.size());
        assertTrue(stringSet.containsAll(list));
    }

    @Test
    public void testRetainAllCaseInsensitive() {
        stringSet.addAll(Arrays.asList("x", "Y", "z"));
        Collection<String> toRetain = Arrays.asList("X", "Z");
        assertTrue(stringSet.retainAll(toRetain));
        assertEquals(2, stringSet.size());
        assertTrue(stringSet.contains("x"));
        assertTrue(stringSet.contains("z"));
        assertFalse(stringSet.contains("Y"));
    }

    @Test
    public void testRemoveAllCaseInsensitive() {
        stringSet.addAll(Arrays.asList("a", "b", "c"));
        Collection<String> toRemove = Arrays.asList("A", "D");
        assertTrue(stringSet.removeAll(toRemove));
        assertEquals(2, stringSet.size());
        assertFalse(stringSet.contains("a"));
        assertTrue(stringSet.contains("b"));
        assertTrue(stringSet.contains("c"));
    }

    @Test
    public void testClear() {
        stringSet.addAll(Arrays.asList("one", "two"));
        assertFalse(stringSet.isEmpty());
        stringSet.clear();
        assertTrue(stringSet.isEmpty());
        assertEquals(0, stringSet.size());
    }

    /* ----------------------------------------------------------------------
     * Equality and hashCode (case‑insensitive)
     * ---------------------------------------------------------------------- */
    @Test
    public void testEqualsAndHashCode() {
        CaseInsensitiveSet<String> set1 = new CaseInsensitiveSet<>();
        CaseInsensitiveSet<String> set2 = new CaseInsensitiveSet<>();

        set1.add("Foo");
        set1.add("Bar");

        set2.add("foo");
        set2.add("BAR");

        assertTrue(set1.equals(set2));
        assertTrue(set2.equals(set1));
        assertEquals(set1.hashCode(), set2.hashCode());

        // Equality with a standard HashSet (case‑sensitive) must be false
        Set<String> hashSet = new HashSet<>(Arrays.asList("Foo", "Bar"));
        assertFalse(set1.equals(hashSet));
    }

    /* ----------------------------------------------------------------------
     * Iterator order (insertion order for default backing map)
     * ---------------------------------------------------------------------- */
    @Test
    public void testIteratorPreservesInsertionOrder() {
        stringSet.addAll(Arrays.asList("first", "second", "third"));
        Iterator<String> it = stringSet.iterator();

        assertTrue(it.hasNext());
        assertEquals("first", it.next());
        assertEquals("second", it.next());
        assertEquals("third", it.next());
        assertFalse(it.hasNext());
    }

    /* ----------------------------------------------------------------------
     * toArray variants
     * ---------------------------------------------------------------------- */
    @Test
    public void testToArray() {
        stringSet.addAll(Arrays.asList("alpha", "beta"));
        Object[] arr = stringSet.toArray();
        assertArrayEquals(new Object[]{"alpha", "beta"}, arr);
    }

    @Test
    public void testToArrayTyped() {
        stringSet.addAll(Arrays.asList("one", "two"));
        String[] arr = stringSet.toArray(new String[0]);
        assertArrayEquals(new String[]{"one", "two"}, arr);
    }

    /* ----------------------------------------------------------------------
     * toString representation (original case preserved)
     * ---------------------------------------------------------------------- */
    @Test
    public void testToStringPreservesOriginalCase() {
        stringSet.addAll(Arrays.asList("Apple", "banana"));
        assertEquals("[Apple, banana]", stringSet.toString());
    }

    /* ----------------------------------------------------------------------
     * Constructors – collection, capacity, and error handling
     * ---------------------------------------------------------------------- */
    @Test
    public void testConstructorWithCollection() {
        List<String> source = Arrays.asList("One", "two", "ONE");
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(source);
        // "One" and "ONE" collapse to a single entry
        assertEquals(2, set.size());
        assertTrue(set.contains("one"));
        assertTrue(set.contains("two"));
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNullCollectionThrowsNPE() {
        new CaseInsensitiveSet<String>((Collection<String>) null);
    }

    @Test
    public void testConstructorWithInitialCapacity() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(10);
        assertTrue(set.isEmpty());
        set.add("test");
        assertTrue(set.contains("TEST"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNegativeCapacityThrowsIAE() {
        new CaseInsensitiveSet<String>(-5);
    }

    @Test
    public void testConstructorWithInitialCapacityAndLoadFactor() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(5, 0.75f);
        set.add("load");
        assertTrue(set.contains("LOAD"));
    }

    /* ----------------------------------------------------------------------
     * Deprecated plus/minus methods (behave like addAll / remove)
     * ---------------------------------------------------------------------- */
    @Test
    public void testDeprecatedPlusAndMinus() {
        // plus with an Iterable
        List<String> items = Arrays.asList("A", "b", "C");
        stringSet.plus(items);
        assertEquals(3, stringSet.size());
        assertTrue(stringSet.contains("a"));
        assertTrue(stringSet.contains("B"));
        assertTrue(stringSet.contains("c"));

        // plus with a single Object
        stringSet.plus("d");
        assertTrue(stringSet.contains("D"));
        assertEquals(4, stringSet.size());

        // minus with an Iterable
        List<String> toRemove = Arrays.asList("B", "x");
        stringSet.minus(toRemove);
        assertFalse(stringSet.contains("b"));
        assertEquals(3, stringSet.size());

        // minus with a single element
        stringSet.minus("c");
        assertFalse(stringSet.contains("C"));
        assertEquals(2, stringSet.size());
    }
}
