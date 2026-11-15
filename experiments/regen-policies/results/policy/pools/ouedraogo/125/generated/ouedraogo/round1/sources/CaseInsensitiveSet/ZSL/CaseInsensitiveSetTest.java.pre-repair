package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link CaseInsensitiveSet}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public mutating operations (add, remove, addAll, retainAll, removeAll, clear)</li>
 *   <li>Query operations (size, isEmpty, contains, containsAll, iterator, toArray, toString)</li>
 *   <li>Equality and hash‑code contracts</li>
 *   <li>Behavior of the three constructors (default, collection‑based, custom backing map)</li>
 *   <li>Case‑insensitive handling for {@link String} elements while preserving original case</li>
 *   <li>Handling of non‑String elements</li>
 *   <li>Deprecated {@code plus} / {@code minus} methods</li>
 * </ul>
 */
public class CaseInsensitiveSetTest
{
    private CaseInsensitiveSet<String> set;

    @Before
    public void setUp()
    {
        set = new CaseInsensitiveSet<>();
    }

    /** Helper to create a set pre‑populated with the given elements (preserving insertion order). */
    private CaseInsensitiveSet<String> createSet(String... elements)
    {
        CaseInsensitiveSet<String> s = new CaseInsensitiveSet<>();
        for (String e : elements)
        {
            s.add(e);
        }
        return s;
    }

    @Test
    public void testAddAndContainsCaseInsensitive()
    {
        assertTrue(set.add("Hello"));
        // same value, different case → should already be present
        assertFalse(set.add("hello"));
        assertTrue(set.contains("HELLO"));
        assertTrue(set.contains("hello"));
        assertEquals(1, set.size());
    }

    @Test
    public void testAddNonStringElements()
    {
        CaseInsensitiveSet<Object> mixed = new CaseInsensitiveSet<>();
        assertTrue(mixed.add(42));
        assertTrue(mixed.add("Apple"));
        // duplicate integer (exact same object/value) → false
        assertFalse(mixed.add(42));
        // duplicate string with different case → false
        assertFalse(mixed.add("apple"));
        assertTrue(mixed.contains(42));
        assertTrue(mixed.contains("APPLE"));
        assertEquals(2, mixed.size());
    }

    @Test
    public void testRemoveCaseInsensitive()
    {
        set.add("World");
        assertTrue(set.remove("world"));
        assertFalse(set.contains("WORLD"));
        assertTrue(set.isEmpty());
    }

    @Test
    public void testSizeAndIsEmpty()
    {
        assertTrue(set.isEmpty());
        set.add("One");
        set.add("Two");
        assertEquals(2, set.size());
        set.remove("ONE");
        assertEquals(1, set.size());
    }

    @Test
    public void testIteratorOrderPreserved()
    {
        CaseInsensitiveSet<String> ordered = createSet("first", "Second", "THIRD");
        Iterator<String> it = ordered.iterator();
        assertEquals("first", it.next());
        assertEquals("Second", it.next());
        assertEquals("THIRD", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testToArray()
    {
        set.add("Alpha");
        set.add("beta");
        Object[] arr = set.toArray();
        assertArrayEquals(new Object[] { "Alpha", "beta" }, arr);

        String[] strArr = set.toArray(new String[0]);
        assertArrayEquals(new String[] { "Alpha", "beta" }, strArr);
    }

    @Test
    public void testAddAllAndContainsAll()
    {
        Collection<String> source = new ArrayList<>();
        source.add("cat");
        source.add("Dog");
        source.add("CAT"); // duplicate case‑insensitive

        assertTrue(set.addAll(source));
        assertEquals(2, set.size());
        assertTrue(set.containsAll(source));
    }

    @Test
    public void testRetainAll()
    {
        set.addAll(java.util.Arrays.asList("a", "B", "c"));
        Collection<String> retain = java.util.Arrays.asList("A", "c", "D");
        assertTrue(set.retainAll(retain));
        assertEquals(2, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("c"));
        assertFalse(set.contains("B"));
    }

    @Test
    public void testRemoveAll()
    {
        set.addAll(java.util.Arrays.asList("x", "Y", "z"));
        Collection<String> remove = java.util.Arrays.asList("X", "y");
        assertTrue(set.removeAll(remove));
        assertEquals(1, set.size());
        assertTrue(set.contains("z"));
        assertFalse(set.contains("x"));
        assertFalse(set.contains("Y"));
    }

    @Test
    public void testClear()
    {
        set.addAll(java.util.Arrays.asList("one", "two"));
        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    public void testEqualsAndHashCode()
    {
        CaseInsensitiveSet<String> a = createSet("foo", "Bar");
        CaseInsensitiveSet<String> b = createSet("FOO", "bar");
        CaseInsensitiveSet<String> c = createSet("foo");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        // Equality with a regular HashSet should be false because case‑insensitivity differs
        java.util.HashSet<String> regular = new java.util.HashSet<>();
        regular.add("foo");
        regular.add("Bar");
        assertNotEquals(a, regular);
    }

    @Test
    public void testToStringPreservesOriginalCaseAndOrder()
    {
        set.addAll(java.util.Arrays.asList("x", "Y", "z"));
        assertEquals("[x, Y, z]", set.toString());
    }

    @Test
    public void testConstructorWithCollectionSortedSet()
    {
        SortedSet<String> sorted = new TreeSet<>();
        sorted.add("b");
        sorted.add("A");
        sorted.add("c");
        CaseInsensitiveSet<String> s = new CaseInsensitiveSet<>(sorted);
        // TreeSet provides natural ordering, so iteration should be alphabetical (case‑insensitive)
        Iterator<String> it = s.iterator();
        assertEquals("A", it.next());
        assertEquals("b", it.next());
        assertEquals("c", it.next());
        assertEquals(3, s.size());
    }

    @Test
    public void testConstructorWithInitialCapacity()
    {
        CaseInsensitiveSet<String> capSet = new CaseInsensitiveSet<>(10);
        assertTrue(capSet.isEmpty());
        capSet.add("test");
        assertTrue(capSet.contains("TEST"));
        assertEquals(1, capSet.size());
    }

    @Test
    public void testConstructorWithCustomBackingMapPreservesMapSemantics()
    {
        // Use a TreeMap as the backing map – this gives sorted order but *no* case‑insensitivity.
        java.util.TreeMap<String, Object> backing = new java.util.TreeMap<>();
        CaseInsensitiveSet<String> custom = new CaseInsensitiveSet<>(java.util.Arrays.asList("b", "A", "c"), backing);
        // Because we supplied a plain TreeMap, duplicate strings with different case are considered distinct.
        assertEquals(3, custom.size());
        // Verify ordering is alphabetical (TreeMap order)
        Iterator<String> it = custom.iterator();
        assertEquals("A", it.next());
        assertEquals("b", it.next());
        assertEquals("c", it.next());

        // Adding a case‑variant now should increase the size (TreeMap is case‑sensitive)
        custom.add("B");
        assertEquals(4, custom.size());
        assertTrue(custom.contains("B"));
    }

    @Test
    public void testDeprecatedPlusAndMinus()
    {
        // plus(Iterable)
        CaseInsensitiveSet<String> plusSet = new CaseInsensitiveSet<>();
        plusSet.plus(java.util.Arrays.asList("a", "B"));
        assertEquals(2, plusSet.size());
        assertTrue(plusSet.contains("A"));
        assertTrue(plusSet.contains("b"));

        // plus(Object)
        plusSet.plus("c");
        assertTrue(plusSet.contains("C"));
        assertEquals(3, plusSet.size());

        // minus(Iterable)
        plusSet.minus(java.util.Arrays.asList("A", "c"));
        assertFalse(plusSet.contains("a"));
        assertFalse(plusSet.contains("C"));
        assertTrue(plusSet.contains("B"));
        assertEquals(1, plusSet.size());

        // minus(Object)
        plusSet.minus("b");
        assertTrue(plusSet.isEmpty());
    }

    @Test
    public void testContainsAllWithMixedTypes()
    {
        CaseInsensitiveSet<Object> mixed = new CaseInsensitiveSet<>();
        mixed.add("Apple");
        mixed.add(123);
        mixed.add("banana");

        Collection<Object> superset = java.util.Arrays.asList("APPLE", 123, "BANANA");
        assertTrue(mixed.containsAll(superset));

        Collection<Object> notSuperset = java.util.Arrays.asList("apple", 123, "cherry");
        assertFalse(mixed.containsAll(notSuperset));
    }
}
