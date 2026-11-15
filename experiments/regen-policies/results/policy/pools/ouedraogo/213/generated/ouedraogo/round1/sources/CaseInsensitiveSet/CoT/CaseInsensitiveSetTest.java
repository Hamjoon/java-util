package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link CaseInsensitiveSet}.
 *
 * The tests are organized as follows:
 * <ul>
 *   <li>Listing of all public methods (including constructors) with their signatures.</li>
 *   <li>Basic functional tests for each method.</li>
 *   <li>Edge‑case and exception‑handling tests (null arguments, illegal capacities, case‑insensitivity, etc.).</li>
 *   <li>Combined test class {@code CaseInsensitiveSetTest} that can be run directly.</li>
 * </ul>
 */
public class CaseInsensitiveSetTest {

    /* ----------------------------------------------------------------------
     *  1.  Public API extracted from CaseInsensitiveSet
     * ---------------------------------------------------------------------- */
    // Constructors
    //   CaseInsensitiveSet()
    //   CaseInsensitiveSet(Collection<? extends E> collection)
    //   CaseInsensitiveSet(Collection<? extends E> source, Map backingMap)
    //   CaseInsensitiveSet(int initialCapacity)
    //   CaseInsensitiveSet(int initialCapacity, float loadFactor)

    // Set interface methods (public)
    //   int hashCode()
    //   boolean equals(Object other)
    //   int size()
    //   boolean isEmpty()
    //   boolean contains(Object o)
    //   Iterator<E> iterator()
    //   Object[] toArray()
    //   <T> T[] toArray(T[] a)
    //   boolean add(E e)
    //   boolean remove(Object o)
    //   boolean containsAll(Collection<?> c)
    //   boolean addAll(Collection<? extends E> c)
    //   boolean retainAll(Collection<?> c)
    //   boolean removeAll(Collection<?> c)
    //   void clear()
    //   String toString()

    // Deprecated convenience methods
    //   Set<E> minus(Iterable<E> removeMe)
    //   Set<E> minus(E removeMe)
    //   Set<E> plus(Iterable<E> right)
    //   Set<E> plus(Object right)

    /* ----------------------------------------------------------------------
     *  2.  Test fixtures
     * ---------------------------------------------------------------------- */
    private CaseInsensitiveSet<String> stringSet;
    private CaseInsensitiveSet<Object> mixedSet;

    @Before
    public void setUp() {
        stringSet = new CaseInsensitiveSet<>();
        mixedSet = new CaseInsensitiveSet<>();
    }

    /* ----------------------------------------------------------------------
     *  3.  Basic functional tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testAddAndContains_CaseInsensitive() {
        assertTrue(stringSet.add("Hello"));
        // Adding the same word with different case must not change the set
        assertFalse(stringSet.add("hello"));
        assertTrue(stringSet.contains("HELLO"));
        assertTrue(stringSet.contains("hello"));
        assertEquals(1, stringSet.size());
    }

    @Test
    public void testRemove_CaseInsensitive() {
        stringSet.add("World");
        assertTrue(stringSet.remove("world")); // different case
        assertFalse(stringSet.contains("World"));
        assertTrue(stringSet.isEmpty());
    }

    @Test
    public void testAddNonStringElements() {
        assertTrue(mixedSet.add(123));
        assertTrue(mixedSet.add("Apple"));
        assertTrue(mixedSet.add(45.6));
        assertEquals(3, mixedSet.size());
        assertTrue(mixedSet.contains(123));
        assertTrue(mixedSet.contains("Apple"));
        assertTrue(mixedSet.contains(45.6));
    }

    @Test
    public void testMixedCaseInsensitiveWithNonString() {
        mixedSet.add("Apple");
        mixedSet.add(999);
        // Adding another string differing only by case must be ignored
        assertFalse(mixedSet.add("apple"));
        assertEquals(2, mixedSet.size());
        assertTrue(mixedSet.contains("APPLE"));
        assertTrue(mixedSet.contains(999));
    }

    @Test
    public void testIteratorOrderPreservesInsertion() {
        stringSet.add("First");
        stringSet.add("Second");
        stringSet.add("Third");
        Iterator<String> it = stringSet.iterator();
        assertEquals("First", it.next());
        assertEquals("Second", it.next());
        assertEquals("Third", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testToArray_ObjectArray() {
        stringSet.addAll(Arrays.asList("a", "B", "c"));
        Object[] arr = stringSet.toArray();
        assertArrayEquals(new Object[]{"a", "B", "c"}, arr);
    }

    @Test
    public void testToArray_GenericArray_BigEnough() {
        stringSet.addAll(Arrays.asList("x", "Y"));
        String[] target = new String[5];
        String[] result = stringSet.toArray(target);
        // The returned array should be the same instance because it was big enough
        assertSame(target, result);
        assertArrayEquals(new String[]{"x", "Y", null, null, null}, result);
    }

    @Test
    public void testToArray_GenericArray_TooSmall() {
        stringSet.addAll(Arrays.asList("alpha", "beta"));
        String[] result = stringSet.toArray(new String[0]); // size 0, forces new array
        assertArrayEquals(new String[]{"alpha", "beta"}, result);
    }

    @Test
    public void testContainsAll() {
        stringSet.addAll(Arrays.asList("One", "Two", "Three"));
        assertTrue(stringSet.containsAll(Arrays.asList("one", "TWO")));
        assertFalse(stringSet.containsAll(Arrays.asList("one", "four")));
    }

    @Test
    public void testAddAll_ModificationFlag() {
        List<String> list = Arrays.asList("a", "b", "a"); // duplicate 'a'
        assertTrue(stringSet.addAll(list)); // at least one new element
        assertFalse(stringSet.addAll(list)); // no new elements after first addition
    }

    @Test
    public void testRetainAll() {
        stringSet.addAll(Arrays.asList("keep1", "keep2", "discard"));
        boolean changed = stringSet.retainAll(Arrays.asList("KEEP1", "keep2"));
        assertTrue(changed);
        assertEquals(2, stringSet.size());
        assertTrue(stringSet.contains("keep1"));
        assertTrue(stringSet.contains("keep2"));
        assertFalse(stringSet.contains("discard"));
    }

    @Test
    public void testRemoveAll() {
        stringSet.addAll(Arrays.asList("a", "b", "c"));
        boolean changed = stringSet.removeAll(Arrays.asList("A", "C"));
        assertTrue(changed);
        assertEquals(1, stringSet.size());
        assertTrue(stringSet.contains("b"));
    }

    @Test
    public void testClearAndIsEmpty() {
        stringSet.addAll(Arrays.asList("x", "y"));
        assertFalse(stringSet.isEmpty());
        stringSet.clear();
        assertTrue(stringSet.isEmpty());
        assertEquals(0, stringSet.size());
    }

    @Test
    public void testEqualsAndHashCode() {
        CaseInsensitiveSet<String> s1 = new CaseInsensitiveSet<>();
        CaseInsensitiveSet<String> s2 = new CaseInsensitiveSet<>();
        s1.addAll(Arrays.asList("a", "B"));
        s2.addAll(Arrays.asList("A", "b"));
        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());

        // Different content
        s2.add("c");
        assertNotEquals(s1, s2);
    }

    @Test
    public void testToStringPreservesOriginalCase() {
        stringSet.add("First");
        stringSet.add("second");
        assertEquals("[First, second]", stringSet.toString());
    }

    @Test
    public void testDeprecatedPlusAndMinus() {
        // plus(Iterable)
        List<String> list = Arrays.asList("one", "TWO");
        stringSet.plus(list);
        assertEquals(2, stringSet.size());
        assertTrue(stringSet.contains("ONE"));
        assertTrue(stringSet.contains("two"));

        // minus(Iterable)
        stringSet.minus(Arrays.asList("ONE"));
        assertEquals(1, stringSet.size());
        assertFalse(stringSet.contains("one"));

        // plus(Object)
        stringSet.plus("THREE");
        assertTrue(stringSet.contains("three"));
        // minus(Object)
        stringSet.minus("THREE");
        assertFalse(stringSet.contains("three"));
    }

    /* ----------------------------------------------------------------------
     *  4.  Edge‑case & exception tests
     * ---------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testConstructor_NullCollectionThrowsNPE() {
        new CaseInsensitiveSet<String>((Collection<String>) null);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructor_NullSourceAndMapThrowsNPE() {
        new CaseInsensitiveSet<String>((Collection<String>) null, new HashMap<>());
    }

    @Test(expected = NullPointerException.class)
    public void testConstructor_NullMapThrowsNPE() {
        List<String> src = Collections.singletonList("test");
        new CaseInsensitiveSet<String>(src, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_NegativeInitialCapacity() {
        new CaseInsensitiveSet<String>(-5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_NegativeInitialCapacityWithLoadFactor() {
        new CaseInsensitiveSet<String>(-1, 0.75f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_NonPositiveLoadFactor() {
        new CaseInsensitiveSet<String>(10, 0.0f);
    }

    @Test(expected = NullPointerException.class)
    public void testAddAll_NullCollectionThrowsNPE() {
        stringSet.addAll(null);
    }

    @Test(expected = NullPointerException.class)
    public void testContainsAll_NullCollectionThrowsNPE() {
        stringSet.containsAll(null);
    }

    @Test(expected = NullPointerException.class)
    public void testRetainAll_NullCollectionThrowsNPE() {
        stringSet.retainAll(null);
    }

    @Test(expected = NullPointerException.class)
    public void testRemoveAll_NullCollectionThrowsNPE() {
        stringSet.removeAll(null);
    }

    @Test(expected = NullPointerException.class)
    public void testToArray_GenericArray_NullArrayThrowsNPE() {
        stringSet.toArray((String[]) null);
    }

    @Test
    public void testAdd_NullElementBehavior() {
        // The underlying CaseInsensitiveMap (a LinkedHashMap) permits null keys,
        // so adding null should succeed and be reflected in the set.
        assertTrue(stringSet.add(null));
        assertTrue(stringSet.contains(null));
        assertEquals(1, stringSet.size());

        // Adding null again must return false because it is already present
        assertFalse(stringSet.add(null));
        assertEquals(1, stringSet.size());
    }

    @Test
    public void testRemove_NullElementBehavior() {
        stringSet.add(null);
        assertTrue(stringSet.remove(null));
        assertFalse(stringSet.contains(null));
        assertTrue(stringSet.isEmpty());
    }

    @Test
    public void testEquals_DifferentTypes() {
        // Comparing to a non‑Set should return false without throwing
        assertFalse(stringSet.equals("not a set"));
    }

    @Test
    public void testEquals_SelfReference() {
        assertTrue(stringSet.equals(stringSet));
    }

    @Test
    public void testHashCode_EmptySetConsistency() {
        CaseInsensitiveSet<String> empty = new CaseInsensitiveSet<>();
        assertEquals(empty.hashCode(), stringSet.hashCode());
    }

    /* ----------------------------------------------------------------------
     *  5.  Constructors with backing collections – behavior verification
     * ---------------------------------------------------------------------- */

    @Test
    public void testConstructor_WithCollection_CopiesElements() {
        List<String> source = Arrays.asList("Alpha", "beta", "ALPHA");
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(source);
        // "Alpha" and "ALPHA" collapse into a single entry
        assertEquals(2, set.size());
        assertTrue(set.contains("alpha"));
        assertTrue(set.contains("BETA"));
    }

    @Test
    public void testConstructor_WithCustomBackingMap() {
        Map<String, Object> customMap = new LinkedHashMap<>();
        List<String> src = Arrays.asList("x", "y");
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(src, customMap);
        // The custom map should now contain the entries
        assertEquals(2, customMap.size());
        assertTrue(set.containsAll(src));
    }
}
